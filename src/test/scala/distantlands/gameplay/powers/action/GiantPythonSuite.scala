package distantlands.gameplay.powers.action

import distantlands.gameplay.cards.{AncientCitySite, BrokenPeaksSite,
  DeepWoodsSite}
import distantlands.gameplay.operations.AncientBloodlineCard
import distantlands.gameplay.powers.NoteText
import distantlands.gameplay.powers.banner.YewStaffCard
import distantlands.gameplay.powers.search.ErrandBoyCard
import distantlands.model._
import distantlands.testkit.{Look, PhasePowerHarness, Table}
import distantlands.testkit.Outcomes.assertWireRoundTrips
import distantlands.testkit.Table.p1
import distantlands.testkit.Unwrap.accepted

/** Giant Python's ACTION (card 186): "If you hold the Darkest Secret, move
  * Giant Python to another site, and you may discard a card there first."
  * (catalog batch 8, slice 10, part 3). */
class GiantPythonSuite extends munit.FunSuite:
  import PhasePowerHarness._

  private val python = GiantPythonCard.id
  private val source = DecisionOptionRef.Denizen(python)
  private val power = GiantPython
  private val home = AncientCitySite.id
  /** Capacity 1. */
  private val peaks = BrokenPeaksSite.id
  /** Capacity 3. */
  private val woods = DeepWoodsSite.id
  /** A plain Beast denizen. */
  private val errandBoy = ErrandBoyCard.id

  /** p1's Act at Ancient City beside Giant Python, holding the Darkest
    * Secret and the start's faceup secret. Errand Boy fills Broken Peaks. */
  private def pythonBesideDarkestSecretHolder: Table =
    Table.start.turn(p1, Phase.Act).pawn(p1, at = home)
      .denizen(python, at = home).denizen(errandBoy, at = peaks)
      .darkestSecret(Some(p1), 1)

  private def slotOptions(t: OathTransition): Vector[(String, String)] =
    offered(t, p1).toVector.flatten

  test("without the Darkest Secret it is unusable"):
    val ready = pythonBesideDarkestSecretHolder.darkestSecret(None, 1).ready
    assert(!usableNow(ready).exists(_.power.id == power.id))

  test("Yew Staff's holder may use it as if they held the Darkest Secret"):
    val ready = pythonBesideDarkestSecretHolder.darkestSecret(None, 1)
      .relic(p1, YewStaffCard).ready
    assert(usableNow(ready).exists(_.power.id == power.id))

  test("it asks one slot question: an empty slot where there is room, a " +
      "card to replace, and nothing at its own site"):
    val t = accepted(use(pythonBesideDarkestSecretHolder.ready, power,
      source))
    assert(awaits(t, GiantPython.decisionId))
    val options = slotOptions(t)
    assert(options.contains("site-slot" ->
      DecisionOptionRef.SiteSlot(woods, 0).wireId))
    // Broken Peaks is full: only its card, to replace.
    assertEquals(options.filter(_._2.startsWith(peaks.value)),
      Vector("site-slot" -> DecisionOptionRef.SiteSlot(peaks, 0).wireId))
    assert(!options.exists(_._2.startsWith(home.value)))

  test("to an empty slot it moves with its secret, and the line says " +
      "where"):
    val ready = pythonBesideDarkestSecretHolder.ready
    val UsedAndAnswered(t, done) = useAndAnswer(ready, power, source,
      GiantPython.decisionId, DecisionOptionRef.SiteSlot(woods, 0))
    val end = Look(after(done))
    assertEquals(end.cardsAt(woods), Vector[CardId](python))
    assertEquals(end.cardsAt(home), Vector.empty[CardId])
    assertEquals(end.tokensOn(python), Tokens(0, 1))
    assertEquals(NoteText.said(power, done.events), Vector(NoteText.Said(
      NoteKey.Used, s"Moved ${python.value} to ${woods.value}.",
      covers = true)))
    assertEquals(replayed(ready, t.events ++ done.events), Right(done.state))
    assertWireRoundTrips(t.events ++ done.events)

  test("replacing a card discards it the standard way and takes its site"):
    val ready = pythonBesideDarkestSecretHolder
      .tokens(errandBoy, favor = 1, secrets = 0).bankFavor(Suit.Beast, 2)
      .ready
    val UsedAndAnswered(_, done) = useAndAnswer(ready, power, source,
      GiantPython.decisionId, DecisionOptionRef.SiteSlot(peaks, 0))
    val end = Look(after(done))
    assertEquals(end.cardsAt(peaks), Vector[CardId](python))
    assert(end.discardAfter(peaks).contains(errandBoy))
    assertEquals(end.bankFavor(Suit.Beast), 2 + 1)
    assertEquals(NoteText.said(power, done.events), Vector(NoteText.Said(
      "used.replaced", s"Replaced ${errandBoy.value} at ${peaks.value}.",
      covers = true)))

  test("a card Ancient Bloodline locks is askable of its holder as a " +
      "Replace"):
    // p2 rules Broken Peaks, where Errand Boy stands, and holds Ancient
    // Bloodline faceup.
    val ready = pythonBesideDarkestSecretHolder
      .warbandsAt(peaks, Table.p2, 1)
      .adviser(Table.p2, AncientBloodlineCard.id).ready
    val t = accepted(use(ready, power, source))
    assert(awaits(t, GiantPython.decisionId))
    val row = queryOf(t, p1).toVector.flatMap(_.offeredOptions)
      .find(_.id == DecisionOptionRef.SiteSlot(peaks, 0).wireId)
    assertEquals(row.map(_.state), Some("askable"))
    assertEquals(row.flatMap(_.reason), Some("Ancient Bloodline: Locked " +
      "and needs the holder's consent."))
