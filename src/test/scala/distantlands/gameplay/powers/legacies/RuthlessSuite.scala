package distantlands.gameplay.powers.legacies

import distantlands.gameplay.VowOfKinshipCard
import distantlands.gameplay.cards.AncientCitySite
import distantlands.gameplay.powers.NoteText
import distantlands.gameplay.powers.economy.DefameCard
import distantlands.gameplay.powers.setup.HallOfDebateCard
import distantlands.model._
import distantlands.testkit.{Look, PhasePowerHarness, Table}
import distantlands.testkit.Unwrap.accepted

/** Ruthless (L29): burn 2 favor to bury a denizen at your site (Legacy
  * in-play powers rulings 1, 6, 7 and 13). */
class RuthlessSuite extends munit.FunSuite:
  import PhasePowerHarness._

  private val actor = Table.p1
  private val source = DecisionOptionRef.Legacy(RuthlessCard.id)
  private val defame = DecisionOptionRef.Denizen(DefameCard.id)
  private val hall = DecisionOptionRef.Edifice(HallOfDebateCard.id)

  /** p1's Act at Ancient City holding Ruthless active with `favor`, Defame
    * there with 1 favor on it and Hall of Debate intact beside it. */
  private def table(favor: Int = 2): Table =
    Table.start.turn(actor, Phase.Act).pawn(actor, AncientCitySite)
      .legacy(actor, RuthlessCard).favor(actor, favor)
      .denizen(DefameCard, AncientCitySite).tokens(DefameCard, favor = 1)
      .edifice(HallOfDebateCard, EdificeSide.Intact, AncientCitySite)

  private def usable(ready: ReadyGame): Boolean =
    usableNow(ready).exists(_.power.id == Ruthless.id)

  test("it is offered while the holder's site holds a denizen, and burns " +
      "2 favor"):
    val ready = table().ready
    assert(usable(ready))
    val t = accepted(use(ready, Ruthless, source))
    assert(awaits(t, Ruthless.decisionId))
    assertEquals(offered(t, actor), Some(Vector(
      "denizen" -> DefameCard.id.value, "edifice" -> HallOfDebateCard.id.value)))
    assertEquals(Look(after(t)).favor(actor), 0)

  test("it buries the chosen denizen, returning its favor, and writes one " +
      "line"):
    val ready = table().ready
    val done = useAndAnswer(ready, Ruthless, source, Ruthless.decisionId,
      defame)
    val now = Look(after(done.answered))
    assert(!now.cardsAt(AncientCitySite).contains(DefameCard.id))
    assertEquals(now.bankFavor(DefameCard.suit),
      Look(ready).bankFavor(DefameCard.suit) + 1)
    assertEquals(replayed(ready, done.events), Right(done.answered.state))
    assertEquals(NoteText.said(Ruthless, done.events), Vector(
      NoteText.Said(NoteKey.Used, s"${actor.value} buried " +
        s"${DefameCard.id.value} with ${RuthlessCard.id.value}",
        covers = true)))

  test("it buries a locked edifice (ruling 7)"):
    val done = useAndAnswer(table().ready, Ruthless, source,
      Ruthless.decisionId, hall)
    assert(!Look(after(done.answered)).cardsAt(AncientCitySite)
      .contains(HallOfDebateCard.id))

  test("it buries a locked denizen at the site (ruling 7)"):
    // Vow of Kinship, a locked card, stands at Ancient City beside Defame;
    // every locked denizen is adviser-only in play, so the table places it.
    val ready = table().denizen(VowOfKinshipCard, AncientCitySite).ready
    val kinship = DecisionOptionRef.Denizen(VowOfKinshipCard.id)
    val done = useAndAnswer(ready, Ruthless, source, Ruthless.decisionId,
      kinship)
    assert(!Look(after(done.answered)).cardsAt(AncientCitySite)
      .contains(VowOfKinshipCard.id))

  test("it is not offered at a site with no denizen (R4)"):
    val bare = Table.start.turn(actor, Phase.Act).pawn(actor, AncientCitySite)
      .legacy(actor, RuthlessCard).favor(actor, 2).ready
    assert(!usable(bare))
    assertEquals(use(bare, Ruthless, source).left.toOption,
      Some(OathViolation.StartUnavailable("No denizen at your site")))

  test("it is not offered without 2 favor"):
    assert(!usable(table(favor = 1).ready))

  test("a dormant Ruthless and a boxed lineage's offer nothing (ruling 1)"):
    val dormant = Table.start.turn(actor, Phase.Act)
      .pawn(actor, AncientCitySite).legacy(actor, RuthlessCard, active = false)
      .favor(actor, 2).denizen(DefameCard, AncientCitySite).ready
    assert(!usable(dormant))
    assert(!usable(LegacyActionFixture.boxed(table().ready, actor)))

  test("a walk resumed at Ruthless's question buries the chosen card once"):
    val ready = table().ready
    val started = accepted(use(ready, Ruthless, source))
    val resumed = accepted(replayed(ready, started.events))
    val done = accepted(answer(OathTransition(resumed, Vector.empty), actor,
      Ruthless.decisionId, pick(defame)))
    val now = Look(after(done))
    assertEquals(now.cardsAt(AncientCitySite), Vector(HallOfDebateCard.id))
    assertEquals(now.favor(actor), 0)
