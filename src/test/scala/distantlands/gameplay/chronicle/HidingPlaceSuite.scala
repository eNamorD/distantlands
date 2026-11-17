package distantlands.gameplay.chronicle

import distantlands.gameplay.{GrandMaskCard, OathRules, WalkerDiceFixture}
import distantlands.gameplay.cards.{AncientCitySite, BrokenPeaksSite}
import distantlands.gameplay.chronicle.legacies.{Reformer, ReformerCard}
import distantlands.gameplay.operations.TheGrandScepterCard
import distantlands.gameplay.powers.{PlayerFacts, PowerImplementationStatus,
  WalkerPowerCatalog}
import distantlands.gameplay.powers.campaign.ObsidianCageCard
import distantlands.gameplay.powers.whenplayed.ThreateningRoarCard
import distantlands.gameplay.powers.economy.{CupOfPlentyCard,
  HallowedSpringCard}
import distantlands.gameplay.setup.{ChronicleAccounting, GameStartRules,
  LaterGameFixture}
import distantlands.gameplay.setup.FirstGameSetupFixture.catalog
import distantlands.model._
import distantlands.testkit.Table
import distantlands.testkit.Table.{p1, p2}
import distantlands.testkit.Unwrap.accepted

/** Hiding Place (E26 ruined): "CHRONICLE (THRONE): This site's ruler may
  * secretly keep a relic they have facedown. They start the next game
  * with it." (Last Chronicle powers design, rulings 3, 4 and 6). p1, the
  * Chancellor, won and holds the Throne; Hiding Place stands at Broken
  * Peaks. */
class HidingPlaceSuite extends munit.FunSuite:
  private val rules = new OathRules(catalog,
    walkerPowerCatalog = WalkerPowerCatalog.default(catalog),
    walkerDice = WalkerDiceFixture.reversingShuffle)

  private def hidingPlaceAtBrokenPeaks: Table =
    ChronicleWalk.throneHeldByChancellorP1
      .edifice(HallowedSpringCard, EdificeSide.Ruined, at = BrokenPeaksSite)

  private def hidingPlaceRuledByP2: Table =
    hidingPlaceAtBrokenPeaks.warbandsAt(BrokenPeaksSite, p2, 1)

  private def through(table: Table,
      answers: PartialFunction[Decide, DecisionAnswer])(
      using munit.Location): ChronicleWalk.Run =
    ChronicleWalk.through(rules, table, TriggeredProcedureRef.ChronicleThrone,
      answers, after = TriggeredProcedureRef.ChronicleSun)

  private def answering(ruler: PlayerId, ref: DecisionOptionRef)
      : PartialFunction[Decide, DecisionAnswer] =
    case decide if decide.decisionId == HidingPlace.decisionId(ruler) =>
      DecisionAnswer.ChooseOneAnswer(ref)

  private def keeping(ruler: PlayerId, relic: RelicId)
      : PartialFunction[Decide, DecisionAnswer] =
    answering(ruler, DecisionOptionRef.Relic(relic))

  private def lineage(ready: ReadyGame, player: PlayerId): LineageState =
    ready.game.campaign.lineages(ready.game.current.player(player).get.lineage)

  private def askedHidingPlace(run: ChronicleWalk.Run): Vector[Decide] =
    run.asked.filter(_.decisionId.startsWith("chronicle.throne.hiding-place"))

  private def options(decide: Decide): Set[DecisionOptionRef] =
    decide.query.asInstanceOf[DecisionQuery.ChooseOne].options.map(_.ref).toSet

  test("Hiding Place is implemented"):
    assert(PowerImplementationStatus.implemented(
      HallowedSpringCard.ruined.power.id))
    assertEquals(HidingPlace.id, HallowedSpringCard.ruined.power.id)

  test("its ruler keeps one relic of their choice, never the Grand " +
      "Scepter, and may Decline (ruling 4)"):
    val table = hidingPlaceRuledByP2.relic(p2, CupOfPlentyCard)
      .relic(p2, GrandMaskCard).relic(p2, TheGrandScepterCard)
    val run = through(table, keeping(p2, GrandMaskCard.id))
    val asked = askedHidingPlace(run)
    assertEquals(asked.map(_.owner), Vector(p2))
    assertEquals(options(asked.head), Set[DecisionOptionRef](
      HidingPlace.decline, DecisionOptionRef.Relic(CupOfPlentyCard.id),
      DecisionOptionRef.Relic(GrandMaskCard.id)))
    assertEquals(lineage(run.ready, p2).startingRelics,
      Vector(GrandMaskCard.id))
    // Scatter Relics took the rest; the Scepter stays with p2.
    assertEquals(run.ready.game.current.player(p2).get.relics.map(_.id),
      Vector(TheGrandScepterCard.id))

  test("a single relic still asks, and Decline keeps nothing (ruling 4)"):
    val run = through(hidingPlaceRuledByP2.relic(p2, CupOfPlentyCard),
      answering(p2, HidingPlace.decline))
    assertEquals(askedHidingPlace(run).map(_.owner), Vector(p2))
    assertEquals(lineage(run.ready, p2).startingRelics, Vector.empty)

  test("a ruler with only the Grand Scepter is not asked"):
    val run = through(hidingPlaceRuledByP2.relic(p2, TheGrandScepterCard),
      PartialFunction.empty)
    assertEquals(askedHidingPlace(run), Vector.empty)

  test("the Chancellor keeps for an Imperial site, before Stock Reliquary " +
      "takes their relics (ruling 4)"):
    // p1 is the Chancellor, so their warband at Broken Peaks is Imperial.
    val table = hidingPlaceAtBrokenPeaks.warbandsAt(BrokenPeaksSite, p1, 1)
      .relic(p1, CupOfPlentyCard)
    assertEquals(SiteRule.rulingPlayer(table.ready, BrokenPeaksSite.id),
      Some(SiteRuler.Player(p1)))
    val run = through(table, keeping(p1, CupOfPlentyCard.id))
    assertEquals(askedHidingPlace(run).map(_.owner), Vector(p1))
    assertEquals(lineage(run.ready, p1).startingRelics,
      Vector(CupOfPlentyCard.id))
    assert(!run.ready.game.campaign.reliquary.contains(CupOfPlentyCard.id))

  test("a site bandits rule, or nobody rules, asks nobody (ruling 4)"):
    val bandits = hidingPlaceAtBrokenPeaks.bandits(BrokenPeaksSite, 2)
      .relic(p2, CupOfPlentyCard)
    assertEquals(askedHidingPlace(through(bandits, PartialFunction.empty)),
      Vector.empty)
    val unruled = hidingPlaceAtBrokenPeaks.relic(p2, CupOfPlentyCard)
    assertEquals(SiteRule.rulingPlayer(unruled.ready, BrokenPeaksSite.id),
      Some(SiteRuler.Unruled))
    assertEquals(askedHidingPlace(through(unruled, PartialFunction.empty)),
      Vector.empty)

  test("the intact Hallowed Spring asks nobody"):
    val table = ChronicleWalk.throneHeldByChancellorP1
      .edifice(HallowedSpringCard, EdificeSide.Intact, at = BrokenPeaksSite)
      .warbandsAt(BrokenPeaksSite, p2, 1).relic(p2, CupOfPlentyCard)
    assertEquals(askedHidingPlace(through(table, PartialFunction.empty)),
      Vector.empty)

  test("a kept relic's secrets return to its ruler first, facedown"):
    val table = hidingPlaceRuledByP2.relic(p2, CupOfPlentyCard)
      .tokens(CupOfPlentyCard, secrets = 2)
    val before = table.ready.game.current.player(p2).get.board
    val run = through(table, keeping(p2, CupOfPlentyCard.id))
    assertEquals(lineage(run.ready, p2).startingRelics,
      Vector(CupOfPlentyCard.id))
    val after = run.ready.game.current.player(p2).get.board
    assertEquals(after.faceDownSecrets, before.faceDownSecrets + 2)

  test("warbands on a kept relic go back to their supply first"):
    // p1 already rules Ancient City, so Shape Empire places no Imperial
    // warband from the supply this test counts.
    val cage = ObsidianCageCard.id
    val table = hidingPlaceRuledByP2.warbandsAt(AncientCitySite, p1, 1)
      .relic(p2, ObsidianCageCard).warbandsOnCard(ObsidianCageCard, p1, 1)
    val kinds = table.ready.game.current.warbandsOnCards(cage).keys.toVector
    val supply = (ready: ReadyGame) =>
      kinds.map(PlayerFacts.banked(ready, _)).sum
    val run = through(table, keeping(p2, cage))
    assertEquals(lineage(run.ready, p2).startingRelics, Vector(cage))
    assertEquals(run.ready.game.current.warbandsOnCards, Map.empty)
    assertEquals(supply(run.ready), supply(table.ready) + 1)

  test("Hiding Place asks after Reformer, once the relics are hidden " +
      "(rulings 3 and 6)"):
    // p1 won, holds Reformer and rules Ancient City and Broken Peaks, so
    // both powers ask p1.
    val table = hidingPlaceAtBrokenPeaks.warbandsAt(AncientCitySite, p1, 1)
      .warbandsAt(BrokenPeaksSite, p1, 1)
      .denizen(ThreateningRoarCard, at = AncientCitySite)
      .legacy(p1, ReformerCard).relic(p1, CupOfPlentyCard)
    val run = through(table, keeping(p1, CupOfPlentyCard.id))
    val ids = run.asked.map(_.decisionId)
    val reformer = ids.indexOf(Reformer.pickId(p1))
    val hiding = ids.indexOf(HidingPlace.decisionId(p1))
    assert(reformer >= 0 && hiding > reformer, ids)

  test("a walk resumed at Hiding Place's question keeps the chosen relic"):
    val table = hidingPlaceRuledByP2.relic(p2, CupOfPlentyCard)
      .relic(p2, GrandMaskCard)
    val started = accepted(rules.next(OathTransition(table.state,
      Vector.empty), TriggeredProcedureRef.ChronicleSun))
    val parked = ChronicleWalk.walkedTo(rules, started,
      HidingPlace.decisionId(p2))
    val decide = ChronicleWalk.parked(parked.state).get
    assertEquals(decide.decisionId, HidingPlace.decisionId(p2))
    val replayed = accepted(parked.events.foldLeft[
      Either[OathViolation, OathState]](Right(table.state))((state, event) =>
        state.flatMap(rules.evolve(_, event))))
    assertEquals(ChronicleWalk.parked(replayed), Some(decide))
    val answered = accepted(rules.resolveWalker(replayed, p2,
      HidingPlace.decisionId(p2), DecisionAnswer.ChooseOneAnswer(
        DecisionOptionRef.Relic(GrandMaskCard.id))))
    val ready = ChronicleWalk.endOf(rules, replayed,
      OathTransition(answered.state, answered.events),
      TriggeredProcedureRef.ChronicleThrone)
    assertEquals(lineage(ready, p2).startingRelics, Vector(GrandMaskCard.id))

  test("the kept relic starts the next game with its ruler, facedown"):
    val run = ChronicleWalk.through(rules, ChronicleWalk.chronicleWonByP1
      .edifice(HallowedSpringCard, EdificeSide.Ruined, at = BrokenPeaksSite)
      .warbandsAt(BrokenPeaksSite, p2, 1)
      .relic(p2, CupOfPlentyCard).relic(p2, GrandMaskCard),
      TriggeredProcedureRef.ChronicleFinish,
      keeping(p2, CupOfPlentyCard.id))
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
