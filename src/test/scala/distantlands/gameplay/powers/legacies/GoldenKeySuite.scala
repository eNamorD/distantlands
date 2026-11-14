package distantlands.gameplay.powers.legacies

import distantlands.gameplay.powers.NoteText
import distantlands.model._
import distantlands.testkit.{Look, PhasePowerHarness, Table}
import distantlands.testkit.Unwrap.accepted

/** Golden Key (L12): an Imperial holder burns a secret and a favor to take
  * a relic from the Reliquary (Legacy in-play powers rulings 2, 10 and
  * 13; implementation rulings R1, R4 and R5). */
class GoldenKeySuite extends munit.FunSuite:
  import PhasePowerHarness._

  private val actor = Table.p1
  private val source = DecisionOptionRef.Legacy(GoldenKeyCard.id)

  /** p1, the Chancellor, in Act holding Golden Key active with 1 favor and
    * 1 secret, and 2 relics in the Reliquary. */
  private def table: Table =
    Table.start.imperial(actor).turn(actor, Phase.Act)
      .legacy(actor, GoldenKeyCard).favor(actor, 1).secrets(actor, faceUp = 1)
      .reliquaryFromDeck(2)

  private def usable(ready: ReadyGame): Boolean =
    usableNow(ready).exists(_.power.id == GoldenKey.id)

  test("an Imperial holder takes the chosen relic facedown, paying a " +
      "favor and a secret"):
    val ready = table.ready
    val reliquary = ready.game.campaign.reliquaryInPlay
    assert(usable(ready))
    val chosen = reliquary.last
    val done = useAndAnswer(ready, GoldenKey, source, GoldenKey.decisionId,
      DecisionOptionRef.Relic(chosen))
    val now = after(done.answered)
    assertEquals(now.game.campaign.reliquary, reliquary.init)
    assertEquals(now.game.current.player(actor).map(_.relics.filter(
      _.id == chosen).map(_.orientation)), Some(Vector(Orientation.FaceDown)))
    assertEquals((Look(now).favor(actor), Look(now).faceUpSecrets(actor)),
      (0, 0))
    assertEquals(replayed(ready, done.events), Right(done.answered.state))
    assertEquals(NoteText.said(GoldenKey, done.events), Vector(
      NoteText.Said(NoteKey.Used, s"${actor.value} took a relic from the " +
        s"Reliquary with ${GoldenKeyCard.id.value}", covers = false)))

  test("a walk resumed at Golden Key's question takes the chosen relic"):
    val ready = table.ready
    val chosen = ready.game.campaign.reliquaryInPlay.head
    val started = accepted(use(ready, GoldenKey, source))
    val resumed = accepted(replayed(ready, started.events))
    val done = accepted(answer(OathTransition(resumed, Vector.empty), actor,
      GoldenKey.decisionId, pick(DecisionOptionRef.Relic(chosen))))
    assert(Look(after(done)).relics(actor).contains(chosen))

  test("it is not offered while the Reliquary is empty (R4)"):
    val empty = Table.start.imperial(actor).turn(actor, Phase.Act)
      .legacy(actor, GoldenKeyCard).favor(actor, 1)
      .secrets(actor, faceUp = 1).ready
    assert(!usable(empty))
    assertEquals(use(empty, GoldenKey, source).left.toOption,
      Some(OathViolation.StartUnavailable("The Reliquary is empty")))

  test("a set-aside Reliquary gives nothing (R5)"):
    val aside = table.reliquarySetAside.ready
    assertEquals(use(aside, GoldenKey, source).left.toOption,
      Some(OathViolation.StartUnavailable("The Reliquary is empty")))

  test("an Exile holder's menu leaves it out, and a use is refused (R1)"):
    val exile = Table.start.turn(actor, Phase.Act)
      .legacy(actor, GoldenKeyCard).favor(actor, 1)
      .secrets(actor, faceUp = 1).reliquaryFromDeck(2).ready
    assert(!usable(exile))
    assert(use(exile, GoldenKey, source).isLeft)
