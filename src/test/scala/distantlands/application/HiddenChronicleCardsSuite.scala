package distantlands.application

import distantlands.gameplay.{BanditCrownCard, GrandMaskCard, OathRules,
  WalkerDiceFixture}
import distantlands.gameplay.cards.{AncientCitySite, BrokenPeaksSite}
import distantlands.gameplay.chronicle.{ChronicleWalk, ReliquaryAndRelics}
import distantlands.gameplay.chronicle.legacies.{Heirloom, HeirloomCard,
  Reformer, ReformerCard, TheBrightBeacon, TheBrightBeaconCard, TheEar,
  TheEarCard}
import distantlands.gameplay.operations.TheGrandScepterCard
import distantlands.gameplay.powers.WalkerPowerCatalog
import distantlands.gameplay.powers.economy.CupOfPlentyCard
import distantlands.gameplay.powers.whenplayed.ThreateningRoarCard
import distantlands.gameplay.setup.FirstGameSetupFixture.catalog
import distantlands.model._
import distantlands.testkit.{Outcomes, Table}
import distantlands.testkit.Table.{p1, p2, p3}
import distantlands.testkit.Unwrap.accepted

/** Cards a legacy shows its holder alone in the Chronicle (Legacy Chronicle
  * powers design, "Hidden choices"; R22): the holder peeks them, and every
  * other seat and the public view still see them hidden. */
class HiddenChronicleCardsSuite extends munit.FunSuite:
  private val rules = new OathRules(catalog,
    walkerPowerCatalog = WalkerPowerCatalog.default(catalog),
    walkerDice = WalkerDiceFixture.reversingShuffle)
  private val presentation = new GamePresentationProjector(catalog)
  private val projector = new GameProjector(catalog)

  /** As `TheEarSuite`'s table, but the three relics are p3's, so p2, who
    * holds The Ear, knows them only by its peeks. */
  private def earTable: Table = ChronicleWalk.throneHeldByChancellorP1
    .warbandsAt(AncientCitySite, p1, 1)
    .relic(p3, CupOfPlentyCard).relic(p3, GrandMaskCard)
    .relic(p3, BanditCrownCard).legacy(p2, TheEarCard)

  test("the examined relics are named to the holder alone, and the holder " +
      "still knows each where it lies; another seat and the public do not " +
      "(R22)"):
    val started = ChronicleWalk.started(rules, earTable,
      TriggeredProcedureRef.ChronicleSun)
    val atEar = ChronicleWalk.walkedTo(rules, started, TheEar.decisionId(p2))
    val ready = Outcomes.ready(atEar.state)
    val relics = Vector(CupOfPlentyCard.id, GrandMaskCard.id,
      BanditCrownCard.id)
    assert(relics.forall(presentation.identifiesAt(ready, Some(p2), _)))
    Vector(Some(p1), None).foreach(viewer =>
      assert(relics.forall(!presentation.identifiesAt(ready, viewer, _)),
        viewer))
    val loaded = LoadedGame(atEar.state, 1L)
    assert(projector.project("ear", loaded, p2).walkerDecision
      .exists(_.query.exists(_.offeredOptions.nonEmpty)))
    Vector(projector.project("ear", loaded, p1),
        projector.projectPublic("ear", loaded)).foreach(view =>
      assert(!view.walkerDecision.exists(_.query.nonEmpty)))
    val after = ChronicleWalk.endOf(rules, earTable.state, atEar,
      TriggeredProcedureRef.ChronicleThrone)
    // Every relic The Ear placed is known to the holder where it lies.
    val placed = ReliquaryAndRelics.emptySlots(catalog, earTable.ready)
      .flatMap(site => after.game.current.map.sites(site).relics.map(_.id))
      .filter(relics.contains)
    assertEquals(placed.toSet, relics.toSet)
    placed.foreach { relic =>
      assert(presentation.identifiesAt(after, Some(p2), relic), relic)
      assert(!presentation.identifiesAt(after, Some(p1), relic), relic)
      assert(!presentation.identifiesAt(after, None, relic), relic)
    }

  /** As `TheBrightBeaconSuite`'s table: p3 holds the Beacon task and The
    * Bright Beacon; five older and three newly Dispossessed denizens wait. */
  private def beaconTable: Table =
    val base = Table.from(ChronicleWalk.throneHeldByChancellorP1.ready
      .updateCurrent(current => current.copy(commonCards =
        current.commonCards.copy(worldDeck = current.commonCards.worldDeck
          .filterNot(_.isInstanceOf[VisionId])))))
    val denizens = base.ready.game.current.commonCards.worldDeck
      .collect { case id: DenizenId => id }
    val (old, fresh) = denizens.take(8).splitAt(5)
    base.dispossessed(old*).update(_.updateCurrent(current => current.copy(
      commonCards = current.commonCards.copy(worldDeck =
        current.commonCards.worldDeck.filterNot(fresh.contains)),
      newlyDispossessed = fresh))).legacy(p3, TheBrightBeaconCard)

  private def all(table: Table): Vector[DenizenId] =
    (table.ready.game.campaign.dispossessed ++
      table.ready.game.current.newlyDispossessed)
      .collect { case id: DenizenId => id }

  test("the Dispossessed are named to the holder only after choosing them " +
      "as the source, and never to another seat or the public (R22)"):
    val table = beaconTable
    val started = ChronicleWalk.started(rules, table,
      TriggeredProcedureRef.ChronicleWorld)
    val atSource = ChronicleWalk.walkedTo(rules, started,
      TheBrightBeacon.sourceId(p3))
    val older = table.ready.game.campaign.dispossessed
    assert(older.forall(card => !presentation.identifiesAt(
      Outcomes.ready(atSource.state), Some(p3), card)))
    val chosen = accepted(rules.resolveWalker(atSource.state, p3,
      TheBrightBeacon.sourceId(p3), DecisionAnswer.ChooseOneAnswer(
        TheBrightBeacon.dispossessed)))
    val ready = Outcomes.ready(chosen.state)
    assertEquals(ChronicleWalk.parked(chosen.state).map(_.decisionId),
      Some(TheBrightBeacon.cardsId(p3)))
    assert(all(table).forall(presentation.identifiesAt(ready, Some(p3), _)))
    Vector(Some(p1), Some(p2), None).foreach(viewer =>
      assert(all(table).forall(card =>
        !presentation.identifiesAt(ready, viewer, card)), viewer))

  test("the start of the Throne hides every held relic but the Grand " +
      "Scepter before Reformer's question: its holder still names their " +
      "own, and no other seat or the public does (ruling 3)"):
    // p1 won and is the Chancellor; the Empire rules Ancient City and
    // Broken Peaks, so Reformer has a card to move.
    val table = ChronicleWalk.throneHeldByChancellorP1
      .warbandsAt(AncientCitySite, p1, 1).warbandsAt(BrokenPeaksSite, p1, 1)
      .denizen(ThreateningRoarCard, at = AncientCitySite)
      .legacy(p1, ReformerCard)
      .relic(p2, CupOfPlentyCard).relic(p2, GrandMaskCard)
    val relics = Vector(CupOfPlentyCard.id, GrandMaskCard.id)
    assert(relics.forall(presentation.identifiesAt(table.ready, Some(p1), _)))
    val atReformer = ChronicleWalk.walkedTo(rules, ChronicleWalk.started(
      rules, table, TriggeredProcedureRef.ChronicleSun), Reformer.pickId(p1))
    assertEquals(ChronicleWalk.parked(atReformer.state).map(_.decisionId),
      Some(Reformer.pickId(p1)))
    val ready = Outcomes.ready(atReformer.state)
    assert(relics.forall(presentation.identifiesAt(ready, Some(p2), _)))
    Vector(Some(p1), Some(p3), None).foreach(viewer =>
      assert(relics.forall(!presentation.identifiesAt(ready, viewer, _)),
        viewer))

  test("the start of the Throne hides every held relic but the Grand " +
      "Scepter: at Heirloom's question its holder still names their own, " +
      "and no other seat or the public does (R28)"):
    val table = ChronicleWalk.throneHeldByChancellorP1
      .warbandsAt(AncientCitySite, p1, 1)
      .relic(p2, CupOfPlentyCard).relic(p2, GrandMaskCard)
      .relic(p3, TheGrandScepterCard).legacy(p2, HeirloomCard)
    val started = ChronicleWalk.started(rules, table,
      TriggeredProcedureRef.ChronicleSun)
    val atHeirloom = ChronicleWalk.walkedTo(rules, started,
      Heirloom.decisionId(p2))
    val ready = Outcomes.ready(atHeirloom.state)
    val relics = Vector(CupOfPlentyCard.id, GrandMaskCard.id)
    assert(relics.forall(presentation.identifiesAt(ready, Some(p2), _)))
    Vector(Some(p1), Some(p3), None).foreach(viewer =>
      assert(relics.forall(!presentation.identifiesAt(ready, viewer, _)),
        viewer))
    Vector(Some(p1), Some(p2), None).foreach(viewer =>
      assert(presentation.identifiesAt(ready, viewer,
        TheGrandScepterCard.id), viewer))
    assertEquals(ChronicleWalk.parked(atHeirloom.state).toVector.flatMap(
      _.query.asInstanceOf[DecisionQuery.ChooseOne].options.map(_.ref)).toSet,
      relics.map(id => DecisionOptionRef.Relic(id): DecisionOptionRef).toSet)

  test("the start of the Throne clears a seat's recorded knowledge of " +
      "another seat's relic: at Heirloom's question p1 no longer names it " +
      "(R28)"):
    // p1 knows p2's facedown Cup of Plenty from a record, as when p1 held
    // it before passing it on, not from the Sun's reveal.
    val table = ChronicleWalk.throneHeldByChancellorP1
      .warbandsAt(AncientCitySite, p1, 1)
      .relic(p2, CupOfPlentyCard, facedown = true)
      .relic(p2, GrandMaskCard).legacy(p2, HeirloomCard)
      .update(_.copy(knowledge = CardKnowledge(heldRelics =
        Map(p1 -> Vector(CupOfPlentyCard.id)))))
    assert(presentation.identifiesAt(table.ready, Some(p1), CupOfPlentyCard.id))
    assert(!presentation.identifiesAt(table.ready, Some(p3),
      CupOfPlentyCard.id))
    val atHeirloom = ChronicleWalk.walkedTo(rules, ChronicleWalk.started(rules,
      table, TriggeredProcedureRef.ChronicleSun), Heirloom.decisionId(p2))
    val ready = Outcomes.ready(atHeirloom.state)
    assert(!presentation.identifiesAt(ready, Some(p1), CupOfPlentyCard.id))
    assertEquals(ready.knowledge.heldRelics.getOrElse(p1, Vector.empty),
      Vector.empty)
    assert(presentation.identifiesAt(ready, Some(p2), CupOfPlentyCard.id))
