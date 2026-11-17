package distantlands.gameplay.chronicle.legacies

import distantlands.gameplay.{GrandMaskCard, OathRules, WalkerDiceFixture}
import distantlands.gameplay.chronicle.ChronicleWalk
import distantlands.gameplay.operations.TheGrandScepterCard
import distantlands.gameplay.powers.WalkerPowerCatalog
import distantlands.gameplay.powers.economy.CupOfPlentyCard
import distantlands.gameplay.setup.{ChronicleAccounting, GameStartRules,
  LaterGameFixture}
import distantlands.gameplay.setup.FirstGameSetupFixture.catalog
import distantlands.model._
import distantlands.testkit.Table
import distantlands.testkit.Table.{p1, p2}
import distantlands.testkit.Unwrap.accepted

/** Heirloom (L20): "Before Scatter Relics, secretly choose one of your
  * relics to keep facedown. You will start the next game with it."
  * (ruling 13; R17-R20). p1, the Chancellor, holds the Throne; p2 is an
  * Exile. */
class HeirloomSuite extends munit.FunSuite:
  private val rules = new OathRules(catalog,
    walkerPowerCatalog = WalkerPowerCatalog.default(catalog),
    walkerDice = WalkerDiceFixture.reversingShuffle)

  private def throne: Table = ChronicleWalk.throneHeldByChancellorP1

  private def through(table: Table,
      answers: PartialFunction[Decide, DecisionAnswer])(
      using munit.Location): ChronicleWalk.Run =
    ChronicleWalk.through(rules, table, TriggeredProcedureRef.ChronicleThrone,
      answers, after = TriggeredProcedureRef.ChronicleSun)

  private def keeping(relic: RelicId): PartialFunction[Decide, DecisionAnswer] =
    case decide if decide.decisionId == Heirloom.decisionId(p2) =>
      DecisionAnswer.ChooseOneAnswer(DecisionOptionRef.Relic(relic))

  private def lineage(ready: ReadyGame, player: PlayerId): LineageState =
    ready.game.campaign.lineages(ready.game.current.player(player).get.lineage)

  test("the holder keeps one relic of their choice, never the Grand " +
      "Scepter, before Scatter Relics (ruling 13; R18)"):
    val table = throne.relic(p2, CupOfPlentyCard).relic(p2, GrandMaskCard)
      .relic(p2, TheGrandScepterCard).legacy(p2, HeirloomCard)
    val run = through(table, keeping(GrandMaskCard.id))
    val asked = run.asked.find(_.decisionId == Heirloom.decisionId(p2)).get
    assertEquals(asked.owner, p2)
    assertEquals(asked.query.asInstanceOf[DecisionQuery.ChooseOne].options
      .map(_.ref).toSet, Set[DecisionOptionRef](
        DecisionOptionRef.Relic(CupOfPlentyCard.id),
        DecisionOptionRef.Relic(GrandMaskCard.id)))
    assertEquals(lineage(run.ready, p2).startingRelics,
      Vector(GrandMaskCard.id))
    // Scatter Relics took the rest; the Scepter stays with p2.
    assertEquals(run.ready.game.current.player(p2).get.relics.map(_.id),
      Vector(TheGrandScepterCard.id))

  test("a single relic is kept without a question, and none asks nothing " +
      "(R18)"):
    val one = through(throne.relic(p2, CupOfPlentyCard)
      .legacy(p2, HeirloomCard), PartialFunction.empty)
    assert(!one.asked.exists(_.decisionId == Heirloom.decisionId(p2)))
    assertEquals(lineage(one.ready, p2).startingRelics,
      Vector(CupOfPlentyCard.id))
    val none = through(throne.relic(p2, TheGrandScepterCard)
      .legacy(p2, HeirloomCard), PartialFunction.empty)
    assert(!none.asked.exists(_.decisionId == Heirloom.decisionId(p2)))
    assertEquals(lineage(none.ready, p2).startingRelics, Vector.empty)

  test("a Throne holder's relics are already in the Reliquary, so their " +
      "Heirloom keeps nothing (R17)"):
    val run = through(throne.relic(p1, CupOfPlentyCard)
      .legacy(p1, HeirloomCard), PartialFunction.empty)
    assertEquals(lineage(run.ready, p1).startingRelics, Vector.empty)

  test("a dormant Heirloom keeps nothing (ruling 1)"):
    val run = through(throne.relic(p2, CupOfPlentyCard)
      .legacy(p2, HeirloomCard, active = false), PartialFunction.empty)
    assertEquals(lineage(run.ready, p2).startingRelics, Vector.empty)

  test("a kept relic's secrets return to its holder first, facedown"):
    val table = throne.relic(p2, CupOfPlentyCard)
      .tokens(CupOfPlentyCard, secrets = 2).legacy(p2, HeirloomCard)
    val before = table.ready.game.current.player(p2).get.board
    val run = through(table, PartialFunction.empty)
    assertEquals(lineage(run.ready, p2).startingRelics,
      Vector(CupOfPlentyCard.id))
    val after = run.ready.game.current.player(p2).get.board
    assertEquals(after.faceDownSecrets, before.faceDownSecrets + 2)

  test("a walk resumed at Heirloom's question keeps the chosen relic"):
    val table = throne.relic(p2, CupOfPlentyCard).relic(p2, GrandMaskCard)
      .legacy(p2, HeirloomCard)
    val started = accepted(rules.next(OathTransition(table.state,
      Vector.empty), TriggeredProcedureRef.ChronicleSun))
    val parked = ChronicleWalk.walkedTo(rules, started,
      Heirloom.decisionId(p2))
    val decide = ChronicleWalk.parked(parked.state).get
    val replayed = accepted(parked.events.foldLeft[
      Either[OathViolation, OathState]](Right(table.state))((state, event) =>
        state.flatMap(rules.evolve(_, event))))
    assertEquals(ChronicleWalk.parked(replayed), Some(decide))
    // Answered on the replayed state, the walk keeps the chosen relic.
    val answered = accepted(rules.resolveWalker(replayed, p2,
      Heirloom.decisionId(p2), DecisionAnswer.ChooseOneAnswer(
        DecisionOptionRef.Relic(GrandMaskCard.id))))
    val ready = ChronicleWalk.endOf(rules, replayed,
      OathTransition(answered.state, answered.events),
      TriggeredProcedureRef.ChronicleThrone)
    assertEquals(lineage(ready, p2).startingRelics, Vector(GrandMaskCard.id))

  test("the kept relic starts the next game with its holder, facedown " +
      "(ruling 13)"):
    // p2 holds the Darkest Secret, so Heirloom stays active through the Sun.
    val run = ChronicleWalk.through(rules, ChronicleWalk.chronicleWonByP1
      .relic(p2, CupOfPlentyCard).relic(p2, GrandMaskCard)
      .darkestSecret(Some(p2), 1).legacy(p2, HeirloomCard),
      TriggeredProcedureRef.ChronicleFinish, keeping(CupOfPlentyCard.id))
    val record = run.ready.game.campaign.evolved.get
    assertEquals(ChronicleAccounting.knownAndUnique(catalog, record),
      Right(()))
    assertEquals(ChronicleAccounting.conserves(
      run.ready.game.campaign.baseline, record), Right(()))
    val kept = lineage(run.ready, p2).id
    assertEquals(record.lineages.find(_.id == kept).map(_.startingRelics),
      Some(Vector(CupOfPlentyCard.id)))
    val next = accepted(GameStartRules.evolve(catalog, record,
      LaterGameFixture.orders(record, p1)))
    val seat = next.game.current.players.find(_.lineage == kept).get
    assert(seat.relics.contains(RelicState(CupOfPlentyCard.id,
      Orientation.FaceDown, Tokens.empty)), seat.relics)
