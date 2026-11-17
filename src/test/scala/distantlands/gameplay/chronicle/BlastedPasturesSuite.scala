package distantlands.gameplay.chronicle

import distantlands.gameplay.{OathRules, WalkerDiceFixture}
import distantlands.gameplay.cards.{AncientCitySite, BrokenPeaksSite,
  BuriedGiantSite, DeepWoodsSite}
import distantlands.gameplay.powers.{PowerImplementationStatus,
  WalkerPowerCatalog}
import distantlands.gameplay.powers.action.{EldersCard, GiantsPathCard}
import distantlands.gameplay.powers.economy.{CupOfPlentyCard,
  HallowedSpringCard, VillageIdiotCard, WildPasturesCard}
import distantlands.gameplay.powers.setup.GreatForgeCard
import distantlands.gameplay.powers.whenplayed.{ThreateningRoarCard,
  TwinBrotherCard}
import distantlands.gameplay.setup.FirstGameSetupFixture.catalog
import distantlands.gameplay.walker.LogNoted
import distantlands.model._
import distantlands.testkit.{Outcomes, Table}
import distantlands.testkit.Table.{p1, p2}

/** Blasted Pastures (E07 ruined): "CHRONICLE (START OF THRONE): Bury all
  * Nomad cards on the map unless the winner rules this card." (Last
  * Chronicle powers design, rulings 5 and 6). p1, the Chancellor, won and
  * holds the Throne; Blasted Pastures stands at Broken Peaks. Each test
  * reads the table where Offer Citizenship parks, after the start of the
  * Throne and before Shape Empire discards anything. */
class BlastedPasturesSuite extends munit.FunSuite:
  private val rules = new OathRules(catalog,
    walkerPowerCatalog = WalkerPowerCatalog.default(catalog),
    walkerDice = WalkerDiceFixture.reversingShuffle)

  private def pasturesAtBrokenPeaks: Table =
    ChronicleWalk.throneHeldByChancellorP1
      .edifice(WildPasturesCard, EdificeSide.Ruined, at = BrokenPeaksSite)

  /** `table`'s Throne walked to Offer Citizenship's question: the game
    * there and the events that led to it. */
  private def atOffer(table: Table)(using munit.Location)
      : (ReadyGame, Vector[OathEvent]) =
    val walked = ChronicleWalk.walkedTo(rules, ChronicleWalk.started(rules,
      table, TriggeredProcedureRef.ChronicleSun),
      OfferCitizenship.offerDecisionId)
    assertEquals(ChronicleWalk.parked(walked.state).map(_.decisionId),
      Some(OfferCitizenship.offerDecisionId))
    (Outcomes.ready(walked.state), walked.events)

  private def onMap(ready: ReadyGame, card: CardId): Boolean =
    ready.game.current.map.sites.values.exists(_.denizens.exists(_.id == card))

  /** The arguments of each Blasted Pastures line among `events`. */
  private def buriedLines(events: Vector[OathEvent]): Vector[Vector[NoteArg]] =
    events.collect {
      case LogNoted(_, note, _) if note.key == BlastedPastures.buried.name =>
        note.args
    }

  test("Blasted Pastures is implemented"):
    assert(PowerImplementationStatus.implemented(
      WildPasturesCard.ruined.power.id))
    assertEquals(BlastedPastures.id, WildPasturesCard.ruined.power.id)

  test("unless the Throne holder rules it, every Nomad card on the map is " +
      "buried: itself, both edifice faces, locked cards, and cards at the " +
      "holder's own and Enduring sites (ruling 5)"):
    // p2 rules Broken Peaks. The Empire, so p1, rules Ancient City, which
    // is also the Enduring site. Great Forge's intact face is locked.
    val table = pasturesAtBrokenPeaks.warbandsAt(BrokenPeaksSite, p2, 1)
      .warbandsAt(AncientCitySite, p1, 1)
      .edifice(GiantsPathCard, EdificeSide.Ruined, at = AncientCitySite)
      .denizen(ThreateningRoarCard, at = AncientCitySite)
      .edifice(GreatForgeCard, EdificeSide.Intact, at = BuriedGiantSite)
      .denizen(EldersCard, at = DeepWoodsSite)
      .denizen(VillageIdiotCard, at = DeepWoodsSite)
      .adviser(p2, TwinBrotherCard)
    val (ready, events) = atOffer(table)
    val edifices = Set(WildPasturesCard.id, GiantsPathCard.id,
      GreatForgeCard.id)
    val nomads: Set[CardId] = edifices ++ Set(EldersCard.id)
    nomads.foreach(card => assert(!onMap(ready, card), card))
    val common = ready.game.current.commonCards
    assertEquals(common.worldDeck.lastOption.map(_.value),
      Some(EldersCard.id.value))
    assertEquals(common.edificeDeck.takeRight(3).toSet, edifices)
    assert(onMap(ready, ThreateningRoarCard.id))
    assert(onMap(ready, VillageIdiotCard.id))
    assert(ready.game.current.player(p2).get.advisers
      .exists(_.id == TwinBrotherCard.id))
    val lines = buriedLines(events)
    assertEquals(lines.map(_.head),
      Vector[NoteArg](NoteArg.Card(WildPasturesCard.id)))
    assertEquals(lines.flatMap(_.lift(1)).collect {
      case NoteArg.Cards(ids) => ids.toSet }, Vector(nomads))

  test("favor on a buried card returns to the Nomad bank and its secrets " +
      "go to the Throne holder, facedown (ruling 5)"):
    val table = pasturesAtBrokenPeaks.denizen(EldersCard, at = DeepWoodsSite)
      .tokens(EldersCard, favor = 2, secrets = 1)
    val before = table.ready
    val (ready, _) = atOffer(table)
    assert(!onMap(ready, EldersCard.id))
    assertEquals(ready.banks.favor(Suit.Nomad),
      before.banks.favor(Suit.Nomad) + 2)
    def hidden(game: ReadyGame) =
      game.game.current.player(p1).get.board.faceDownSecrets
    assertEquals(hidden(ready), hidden(before) + 1)

  test("the Throne holder ruling its site keeps every card; an Imperial " +
      "holder rules the Empire's sites (ruling 5)"):
    // p1 is the Chancellor, so their warband at Broken Peaks is Imperial.
    val table = pasturesAtBrokenPeaks.warbandsAt(BrokenPeaksSite, p1, 1)
      .denizen(EldersCard, at = DeepWoodsSite)
    assert(ChronicleTasks.rules(table.ready, p1, BrokenPeaksSite.id))
    val (ready, events) = atOffer(table)
    assert(onMap(ready, WildPasturesCard.id))
    assert(onMap(ready, EldersCard.id))
    assertEquals(buriedLines(events), Vector.empty)

  /** `pasturesAtBrokenPeaks` with the Throne held by p2, an Exile. */
  private def throneHeldByExileP2: Table = pasturesAtBrokenPeaks.update(
    state => state.updateCurrent(_.copy(chronicleTasks = Map(
      ChronicleTask.Throne -> p2, ChronicleTask.World -> p1,
      ChronicleTask.Beacon -> Table.p3))))

  test("an Exile holder rules only their own sites (ruling 5)"):
    val exile = throneHeldByExileP2.denizen(EldersCard, at = DeepWoodsSite)
    val own = exile.warbandsAt(BrokenPeaksSite, p2, 1)
    assert(ChronicleTasks.rules(own.ready, p2, BrokenPeaksSite.id))
    val (kept, keptEvents) = atOffer(own)
    assert(onMap(kept, WildPasturesCard.id))
    assert(onMap(kept, EldersCard.id))
    assertEquals(buriedLines(keptEvents), Vector.empty)
    // The Empire's site is not an Exile's.
    val imperial = exile.warbandsAt(BrokenPeaksSite, p1, 1)
    assert(!ChronicleTasks.rules(imperial.ready, p2, BrokenPeaksSite.id))
    val (buried, buriedEvents) = atOffer(imperial)
    assert(!onMap(buried, WildPasturesCard.id))
    assert(!onMap(buried, EldersCard.id))
    assertEquals(buriedLines(buriedEvents).size, 1)

  test("a site another player, bandits or nobody rules still buries"):
    val withElders = pasturesAtBrokenPeaks
      .denizen(EldersCard, at = DeepWoodsSite)
    Vector(withElders.warbandsAt(BrokenPeaksSite, p2, 1),
      withElders.bandits(BrokenPeaksSite, 1), withElders).foreach { table =>
      val (ready, events) = atOffer(table)
      assert(!onMap(ready, WildPasturesCard.id))
      assert(!onMap(ready, EldersCard.id))
      assertEquals(buriedLines(events).size, 1)
    }

  test("the intact Wild Pastures buries nothing"):
    val table = ChronicleWalk.throneHeldByChancellorP1
      .edifice(WildPasturesCard, EdificeSide.Intact, at = BrokenPeaksSite)
      .denizen(EldersCard, at = DeepWoodsSite)
    val (ready, events) = atOffer(table)
    assert(onMap(ready, WildPasturesCard.id))
    assert(onMap(ready, EldersCard.id))
    assertEquals(buriedLines(events), Vector.empty)

  test("Blasted Pastures buries after Hiding Place asks (ruling 6)"):
    val table = pasturesAtBrokenPeaks
      .edifice(HallowedSpringCard, EdificeSide.Ruined, at = DeepWoodsSite)
      .warbandsAt(DeepWoodsSite, p2, 1).relic(p2, CupOfPlentyCard)
    val parked = ChronicleWalk.walkedTo(rules, ChronicleWalk.started(rules,
      table, TriggeredProcedureRef.ChronicleSun), HidingPlace.decisionId(p2))
    assertEquals(ChronicleWalk.parked(parked.state).map(_.decisionId),
      Some(HidingPlace.decisionId(p2)))
    assert(onMap(Outcomes.ready(parked.state), WildPasturesCard.id))
    val (ready, _) = atOffer(table)
    assert(!onMap(ready, WildPasturesCard.id))

  test("the whole Throne replays with the bury"):
    val table = pasturesAtBrokenPeaks.denizen(EldersCard, at = DeepWoodsSite)
    // `through` replays its events from the table and fails on a refusal.
    val run = ChronicleWalk.through(rules, table,
      TriggeredProcedureRef.ChronicleThrone,
      after = TriggeredProcedureRef.ChronicleSun)
    assert(!onMap(run.ready, WildPasturesCard.id))
    assert(run.ready.game.current.commonCards.worldDeck
      .exists(_.value == EldersCard.id.value))
