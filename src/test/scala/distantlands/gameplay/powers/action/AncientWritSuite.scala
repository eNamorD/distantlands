package distantlands.gameplay.powers.action

import distantlands.gameplay.chronicle.legacies.RivalCard
import distantlands.gameplay.powers.{LegacyDraws, NoteText}
import distantlands.gameplay.setup.legacies.{PopulistCard, SeerCard,
  WorldCrafterCard}
import distantlands.model._
import distantlands.testkit.{Look, Outcomes, PhasePowerHarness, Table}
import distantlands.testkit.CardRefs.LegacyRef
import distantlands.testkit.Table.p1
import distantlands.testkit.Unwrap.accepted

/** Ancient Writ (relic R22), ACTION: bury it, draw 3 legacies, keep 1
  * dormant and discard the others to the bottom of the legacy deck
  * (catalog batch 13, rulings 1, 2, 6 and 8). */
class AncientWritSuite extends munit.FunSuite:
  import PhasePowerHarness.{after, answer, awaits, pick, use}

  private val writ = AncientWritCard.id
  private val source = DecisionOptionRef.Relic(writ)
  private val (seer, populist, crafter, rival) = (SeerCard.id,
    PopulistCard.id, WorldCrafterCard.id, RivalCard.id)

  /** p1's Act holding Ancient Writ faceup, the legacy deck holding `deck`,
    * first named on top. */
  private def holding(deck: LegacyRef*): Table =
    Table.start.turn(p1, Phase.Act).relic(p1, AncientWritCard)
      .legacyDeck(deck*)

  private def lineageOf(ready: ReadyGame): LineageId =
    ready.game.current.player(p1).get.lineage

  private def legaciesOf(ready: ReadyGame): Vector[LegacyState] =
    ready.game.campaign.lineages(lineageOf(ready)).legacies

  private def handOf(ready: ReadyGame): Vector[LegacyId] =
    LegacyDraws.hand(ready, p1)

  test("it buries itself and draws the top 3 legacies into the hand, then " +
      "asks which to keep"):
    val used = accepted(use(holding(SeerCard, PopulistCard, WorldCrafterCard,
      RivalCard).ready, AncientWrit, source))
    assert(awaits(used, AncientWrit.decisionId))
    val now = after(used)
    assert(!Look(now).player(p1).relics.exists(_.id == writ))
    assertEquals(now.game.current.commonCards.relicDeck.last, writ)
    assertEquals(handOf(now), Vector(seer, populist, crafter))
    assertEquals(now.game.current.commonCards.legacyDeck, Vector(rival))

  test("the kept legacy joins the lineage dormant and the others go to the " +
      "bottom of the deck"):
    val used = accepted(use(holding(SeerCard, PopulistCard, WorldCrafterCard,
      RivalCard).ready, AncientWrit, source))
    val kept = after(accepted(answer(used, p1, AncientWrit.decisionId,
      pick(DecisionOptionRef.Legacy(populist)))))
    assertEquals(legaciesOf(kept), Vector(LegacyState(populist, active = false)))
    assertEquals(handOf(kept), Vector.empty)
    assertEquals(kept.game.current.commonCards.legacyDeck,
      Vector(rival, seer, crafter))

  test("a deck of two draws both and asks between them"):
    val used = accepted(use(holding(SeerCard, PopulistCard).ready,
      AncientWrit, source))
    assert(awaits(used, AncientWrit.decisionId))
    assertEquals(handOf(after(used)), Vector(seer, populist))

  test("a deck of one is kept without a question"):
    val used = accepted(use(holding(SeerCard).ready, AncientWrit, source))
    assert(!awaits(used, AncientWrit.decisionId))
    assertEquals(legaciesOf(after(used)),
      Vector(LegacyState(seer, active = false)))
    assertEquals(after(used).game.current.commonCards.legacyDeck, Vector.empty)

  test("an empty deck draws nothing, asks nothing and says so"):
    val used = accepted(use(holding().ready, AncientWrit, source))
    assert(!awaits(used, AncientWrit.decisionId))
    assertEquals(legaciesOf(after(used)), Vector.empty)
    assertEquals(NoteText.said(AncientWrit, used.events),
      Vector(NoteText.Said("legacy-deck-empty", "The legacy deck was empty.",
        covers = false)))

  test("its lines say how many legacies were drawn and that one was kept, " +
      "never which"):
    val used = accepted(use(holding(SeerCard, PopulistCard, WorldCrafterCard)
      .ready, AncientWrit, source))
    val kept = accepted(answer(used, p1, AncientWrit.decisionId,
      pick(DecisionOptionRef.Legacy(seer))))
    assertEquals(NoteText.said(AncientWrit, used.events ++ kept.events)
      .map(_.text), Vector("p1 drew 3 legacies.", "p1 kept a legacy."))
    val single = accepted(use(holding(SeerCard).ready, AncientWrit, source))
    assertEquals(NoteText.said(AncientWrit, single.events).map(_.text),
      Vector("p1 drew 1 legacy.", "p1 kept a legacy."))

  test("the journal replays to the same state"):
    val start = holding(SeerCard, PopulistCard, WorldCrafterCard).ready
    val used = accepted(use(start, AncientWrit, source))
    val kept = accepted(answer(used, p1, AncientWrit.decisionId,
      pick(DecisionOptionRef.Legacy(crafter))))
    assertEquals(
      Outcomes.ready(accepted(PhasePowerHarness.replayed(start,
        used.events ++ kept.events))),
      after(kept))
