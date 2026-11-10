package distantlands.testkit

import distantlands.gameplay.powers.economy.HallowedSpringCard
import distantlands.application.{GameApplicationService, 
  InMemoryEventStreamRepository, ParkedServiceFixture}
import distantlands.gameplay.actions.challenge.MagiciansCodeCard
import distantlands.gameplay.StateBasedEvaluation
import distantlands.gameplay.actions.VisionRules
import distantlands.gameplay.cards.{AncientCitySite, BrokenPeaksSite,
  BuriedGiantSite, DunesSite, FairIsleSite}
import distantlands.gameplay.phases.rest.FinishRestProcedure
import distantlands.gameplay.powers.action.{AlchemistCard, BrassHorseCard,
  DowsingSticksCard, EldersCard, ScryerCard, WolvesCard}
import distantlands.gameplay.powers.campaign.StickyFireCard
import distantlands.gameplay.powers.search.ForcedLaborCard
import distantlands.gameplay.powers.targeting.CircletOfCommandCard
import distantlands.gameplay.powers.whenplayed.{DazzleCard,
  ThreateningRoarCard}
import distantlands.gameplay.setup.{FirstGameSetupFixture, GameStartRules}
import distantlands.gameplay.setup.FirstGameSetupFixture.catalog
import distantlands.model._
import Table.{p1, p2, p3}
import Unwrap.{accepted, present}

/** `Table` builds rule-test states from the real game start without the
  * setup walk. These tests pin what the quiet table holds, that every step
  * moves a card rather than copying it, and that the rules accept the
  * result as a real game. */
class TableSuite extends munit.FunSuite:

  test("the quiet table: p1's Act, each pawn on its own site, nothing at the " +
      "sites, and no advisers, relics, hands, discards, Oathkeeper or banners"):
    val look = Look(Table.start.ready)
    assertEquals(look.active, p1)
    assertEquals(look.phase, Phase.Act)
    assertEquals(Vector(p1, p2, p3).map(look.pawn),
      Vector(AncientCitySite.id, BrokenPeaksSite.id, BuriedGiantSite.id))
    Table.start.ready.game.current.map.inPlay.foreach { site =>
      assertEquals(look.cardsAt(site), Vector.empty, site.value)
      assertEquals(look.relicsAt(site), Vector.empty, site.value)
      assertEquals(look.forces(site), SiteForces.Empty, site.value)
      assertEquals(look.siteTokens(site), Tokens.empty, site.value)
    }
    Vector(p1, p2, p3).foreach { p =>
      assertEquals(look.advisers(p), Vector.empty)
      assertEquals(look.relics(p), Vector.empty)
    }
    val current = Table.start.ready.game.current
    assertEquals(current.temporaryHands, Map.empty)
    assert(Region.all.forall(current.commonCards.discard(_).isEmpty))
    assertEquals(current.title.holder, None)
    assertEquals((current.banners.peoplesFavor.holder,
      current.banners.darkestSecret.holder), (None, None))

  test("banditsRefilled leaves the refill nothing to add and keeps the forces " +
      "already at a site"):
    val held = Table.start.warbandsAt(AncientCitySite, p1, 1)
      .boardWarbands(p1, 2)
    val table = held.banditsRefilled
    assertEquals(StateBasedEvaluation.banditRefill(catalog, table.state),
      Right(None))
    val look = Look(table.ready)
    assertEquals(look.forces(AncientCitySite.id),
      Look(held.ready).forces(AncientCitySite.id))
    assertEquals(look.forces(BrokenPeaksSite.id),
      SiteForces.Occupied(ForceKind.Bandit,
        present(catalog.site(BrokenPeaksSite.id),
          "Broken Peaks").capacity))

  test("relicDeckTop puts relics on top of the relic deck, first named on top"):
    val ready = Table.start.relicDeckTop(DowsingSticksCard, StickyFireCard)
      .ready
    assertEquals(ready.game.current.commonCards.relicDeck.take(2),
      Vector(DowsingSticksCard.id,
        StickyFireCard.id))

  test("the turn passes p1, p2, p3"):
    assertEquals(FinishRestProcedure.turnOrder(Table.start.ready),
      Vector(p1, p2, p3))

  test("boards keep their printed start: 1 favor, 1 faceup secret, 3 warbands, 7 Supply"):
    val look = Look(Table.start.ready)
    assertEquals((look.favor(p1), look.faceUpSecrets(p1), look.boardWarbands(p1),
      look.supply(p1)), (1, 1, 3, 7))

  test("the quiet table holds exactly the cards the game started with"):
    // Starts the game from the Chronicle: the start it was built from is the
    // thing compared.
    val started = accepted(GameStartRules.evolve(catalog,
      FirstGameSetupFixture.chronicle, FirstGameSetupFixture.orders))
    assertEquals(accepted(CardIndex.from(Table.start.ready.game)).ids,
      accepted(CardIndex.from(started.game)).ids)

  test("placing a card takes it out of the zone that held it"):
    // Threatening Roar tops the quiet table's world deck.
    val dealt = ThreateningRoarCard.id
    assertEquals(
      Table.start.ready.game.current.commonCards.worldDeck.headOption,
      Some(dealt))
    val advised = Table.start.adviser(p1, dealt)
    assert(!advised.ready.game.current.commonCards.worldDeck.contains(dealt))
    val moved = advised.denizen(dealt, at = DunesSite)
    assertEquals(Look(moved.ready).advisers(p1), Vector.empty)
    assertEquals(Look(moved.ready).cardsAt(DunesSite), Vector(dealt))

  test("placing a card takes it from a discard, a player's relics and every viewer's memory"):
    val discarded = Table.start.discarded(Region.Cradle, AlchemistCard)
    val placed = discarded.adviser(p1, AlchemistCard).ready
    assertEquals(placed.game.current.commonCards.discard(Region.Cradle), Vector.empty)
    // No Table step records knowledge of a held relic.
    val peeked = Table.start.relic(p2, BrassHorseCard, facedown = true)
      .update(r => r.copy(knowledge = r.knowledge.copy(heldRelics =
        Map(p1 -> Vector(BrassHorseCard.id)))))
    val moved = peeked.relicAt(BrassHorseCard, at = DunesSite).ready
    assertEquals(Look(moved).relics(p2), Vector.empty)
    assertEquals(moved.knowledge.heldRelics(p1), Vector.empty)

  test("a card the first game did not deal joins the table"):
    val inGame = accepted(CardIndex.from(Table.start.ready.game)).ids
    // The first game does not deal Forced Labor.
    val outside = ForcedLaborCard.id
    assert(!inGame(outside))
    val ready = Table.start.adviser(p1, outside).ready
    assertEquals(Look(ready).advisers(p1), Vector(outside))

  test("a card in two places is rejected when the state is read"):
    val card = AlchemistCard.id
    val twice = Table.start.adviser(p1, card).update(_.updateCurrent(c =>
      c.copy(players = c.players.map(p => if p.player == p2 then
        p.copy(advisers = Vector(DenizenState(card, Orientation.FaceUp,
          Tokens.empty))) else p))))
    val failure = intercept[munit.FailException](twice.ready)
    assert(failure.getMessage.contains("Alchemist"), failure.getMessage)

  test("a lost card is rejected when the state is read"):
    val lost = Table.start.update(_.updateCurrent(c => c.copy(commonCards =
      c.commonCards.copy(worldDeck = c.commonCards.worldDeck.tail))))
    val failure = intercept[munit.FailException](lost.ready)
    assert(failure.getMessage.contains("is missing"), failure.getMessage)

  test("unchecked reads a state a fixture is still assembling, which ready refuses"):
    val lost = Table.start.update(_.updateCurrent(c => c.copy(commonCards =
      c.commonCards.copy(worldDeck = c.commonCards.worldDeck.tail))))
    assertEquals(lost.unchecked.game.current.commonCards.worldDeck.size,
      Table.start.ready.game.current.commonCards.worldDeck.size - 1)
    intercept[munit.FailException](lost.ready)

  test("a table continued from a state loses a card and ready names it"):
    val joinedEarlier = AlchemistCard.id
    // The first game does not deal the Alchemist, so a check against the
    // first game's cards alone would not miss it.
    assert(!accepted(CardIndex.from(Table.start.ready.game)).ids(
      joinedEarlier))
    val base = Table.start.adviser(p1, joinedEarlier).ready
    val lost = Table.from(base).update(_.updateCurrent(c => c.copy(
      players = c.players.map(p =>
        if p.player == p1 then p.copy(advisers = Vector.empty) else p))))
    val failure = intercept[munit.FailException](lost.ready)
    assert(failure.getMessage.contains("Alchemist"), failure.getMessage)

  test("a card a step placed is expected from then on, and losing it is " +
      "named"):
    val joining = ForcedLaborCard.id
    // The first game does not deal Forced Labor, so only the step that
    // placed it makes the table expect it.
    assert(!accepted(CardIndex.from(Table.start.ready.game)).ids(joining))
    val lost = Table.start.adviser(p1, joining).update(_.updateCurrent(c =>
      c.copy(players = c.players.map(p =>
        if p.player == p1 then p.copy(advisers = Vector.empty) else p))))
    val failure = intercept[munit.FailException](lost.ready)
    assert(failure.getMessage.contains("Forced Labor"), failure.getMessage)

  test("a continued table is checked against the state it continued from, " +
      "not against the cards the first game dealt"):
    // A base this table did not assemble may hold fewer cards than the first
    // game dealt; what it holds is what its steps must keep.
    val short = Table.start.ready.updateCurrent(c => c.copy(commonCards =
      c.commonCards.copy(worldDeck = c.commonCards.worldDeck.tail)))
    Table.from(short).favor(p1, 2).ready
    val lostAfter = Table.from(short).update(_.updateCurrent(c => c.copy(
      commonCards = c.commonCards.copy(
        worldDeck = c.commonCards.worldDeck.tail))))
    assert(intercept[munit.FailException](lostAfter.ready).getMessage
      .contains("is missing"))

  test("a continued table whose state already holds a card twice is refused"):
    // The expected set seeds empty for a state that does not index, so this
    // pins that `from` reads that index without demanding it succeeded.
    val card = AlchemistCard.id
    val twice = Table.start.adviser(p1, card).ready.updateCurrent(c =>
      c.copy(players = c.players.map(p => if p.player == p2 then
        p.copy(advisers = Vector(DenizenState(card, Orientation.FaceUp,
          Tokens.empty))) else p)))
    val failure = intercept[munit.FailException](Table.from(twice).ready)
    assert(failure.getMessage.contains("Alchemist"), failure.getMessage)

  test("more warbands than the printed supply are rejected when the state is read"):
    // Bandits print 24, so 25 at one site are one too many.
    val bandits = intercept[munit.FailException](
      Table.start.bandits(DunesSite, MaterialBankState.BanditWarbands + 1)
        .ready)
    assert(bandits.getMessage.contains("Bandit"), bandits.getMessage)
    // A lineage prints 14: 12 on the board and 3 at a site are one too many.
    val exiles = intercept[munit.FailException](
      Table.start.boardWarbands(p1, 12).warbandsAt(DunesSite, p1, 3).ready)
    assert(exiles.getMessage.contains("15 warbands"), exiles.getMessage)

  test("banksHoldingOnly stocks the banks named and empties the rest"):
    val banks = Table.start.banksHoldingOnly(Suit.Order -> 5, Suit.Hearth -> 2)
      .ready.banks.favor
    assertEquals(Suit.all.map(banks(_)), Suit.all.map {
      case Suit.Order => 5
      case Suit.Hearth => 2
      case _ => 0
    })

  test("warbandsInBank fills the board until the bank holds exactly n"):
    // A lineage prints 14: 2 at a site and 4 left in the bank leave 8 for
    // the board.
    val table = Table.start.warbandsAt(AncientCitySite, p1, 2)
      .warbandsInBank(p1, 4)
    assertEquals(Look(table.ready).boardWarbands(p1), 8)
    // It gives warbands back too: 10 on the board leave 4 in the bank, and
    // a bank of 6 takes 2 of them.
    val returned = Table.start.boardWarbands(p1, 10).warbandsInBank(p1, 6)
    assertEquals(Look(returned.ready).boardWarbands(p1), 8)

  test("each step states one fact"):
    val ready = Table.start
      .turn(p2, Phase.Wake)
      .pawn(p1, at = DunesSite)
      .adviser(p1, AlchemistCard, facedown = true)
      .relic(p2, CircletOfCommandCard)
      .denizen(MagiciansCodeCard, at = DunesSite)
      .relicAt(BrassHorseCard, at = DunesSite)
      .edifice(HallowedSpringCard, EdificeSide.Intact, at = FairIsleSite)
      .bandits(FairIsleSite, 2)
      .warbandsAt(DunesSite, p1, 3)
      .favor(p1, 4).secrets(p1, faceUp = 2, faceDown = 1).supply(p1, 5)
      .boardWarbands(p1, 6)
      .tokens(MagiciansCodeCard, favor = 1)
      .siteTokens(DunesSite, secrets = 2)
      .peoplesFavor(Some(p3), favor = 2)
      .oathkeeper(Some(p2))
      .bankFavor(Suit.Arcane, 1)
      .ready
    val look = Look(ready)
    assertEquals((look.active, look.phase), (p2, Phase.Wake))
    assertEquals(look.pawn(p1), DunesSite.id)
    assertEquals(look.player(p1).advisers, Vector(DenizenState(
      AlchemistCard.id, Orientation.FaceDown, Tokens.empty)))
    assertEquals(look.relics(p2), Vector(CircletOfCommandCard.id))
    assertEquals(look.cardsAt(DunesSite),
      Vector(MagiciansCodeCard.id))
    assertEquals(look.relicsAt(DunesSite), Vector(BrassHorseCard.id))
    assertEquals(look.cardsAt(FairIsleSite),
      Vector(HallowedSpringCard.id))
    assertEquals(look.forces(FairIsleSite),
      SiteForces.Occupied(ForceKind.Bandit, 2))
    assertEquals(look.forces(DunesSite), SiteForces.Occupied(
      ForceKind.Exile(look.player(p1).lineage), 3))
    assertEquals((look.favor(p1), look.faceUpSecrets(p1),
      look.faceDownSecrets(p1), look.supply(p1), look.boardWarbands(p1)),
      (4, 2, 1, 5, 6))
    assertEquals(look.tokensOn(MagiciansCodeCard), Tokens(1, 0))
    assertEquals(look.siteTokens(DunesSite), Tokens(0, 2))
    assertEquals(ready.game.current.banners.peoplesFavor.holder, Some(p3))
    assertEquals(ready.game.current.title.holder, Some(p2))
    assertEquals(ready.banks.favor(Suit.Arcane), 1)

  test("a viewer can know a relic lying at a site"):
    val ready = Table.start.relicAt(StickyFireCard, at = DunesSite)
      .knowsRelicAt(p2, StickyFireCard, at = DunesSite).ready
    assertEquals(ready.knowledge.siteRelics(p2)(DunesSite.id),
      Vector(StickyFireCard.id))
    intercept[munit.FailException](
      Table.start.knowsRelicAt(p2, StickyFireCard, at = DunesSite))

  test("withoutAdvisers sends each adviser to the world deck's bottom, in " +
      "the order held, and leaves the other players' advisers alone"):
    val ready = Table.start
      .adviser(p1, AlchemistCard)
      .adviser(p1, MagiciansCodeCard, facedown = true)
      .adviser(p2, EldersCard)
      .withoutAdvisers(p1).ready
    assertEquals(Look(ready).advisers(p1), Vector.empty)
    assertEquals(ready.game.current.commonCards.worldDeck.takeRight(2), Vector(
      AlchemistCard.id, MagiciansCodeCard.id))
    assertEquals(Look(ready).advisers(p2), Vector(EldersCard.id))

  test("a card can be put in a player's hand"):
    val ready = Table.start.hand(p1, AlchemistCard, MagiciansCodeCard).ready
    assertEquals(ready.game.current.temporaryHands(p1), Vector(
      AlchemistCard.id, MagiciansCodeCard.id))
    assert(!ready.game.current.commonCards.worldDeck.contains(
      AlchemistCard.id))

  test("the world deck's top can be named"):
    val ready = Table.start.worldDeckTop(AlchemistCard, MagiciansCodeCard).ready
    assertEquals(ready.game.current.commonCards.worldDeck.take(2), Vector(
      AlchemistCard.id, MagiciansCodeCard.id))

  test("real commands run from a table: Rest passes the turn to p2"):
    val rested = Table.start.situation(Situation.rules(catalog))
      .after(MenuPlay.start(p1, PhaseTransitionRef.BeginRest))
    assertEquals(Look(rested.state).active, p2)

  test("real commands run from a table: Muster from a denizen at the pawn's site"):
    val mustered = Table.start.pawn(p1, AncientCitySite)
      .denizen(AlchemistCard, at = AncientCitySite)
      .situation(Situation.rules(catalog))
      .after(MenuPlay.start(p1, ActionRef.Muster))
    val look = Look(mustered.state)
    // From the quiet start of 1 favor, 3 warbands and 7 Supply: the favor
    // moves onto the card, one warband joins the board, and Muster costs
    // 1 Supply.
    assertEquals((look.favor(p1), look.boardWarbands(p1), look.supply(p1)), (0, 4, 6))
    assertEquals(look.tokensOn(AlchemistCard), Tokens(1, 0))

  test("real commands run from a table: Travel moves the pawn"):
    val travelled = Table.start.phaseMenuParked
      .situation(Situation.rules(catalog))
      .parkedAfter(ParkedServiceFixture.startTravel(p1))
      .after(ParkedServiceFixture.pickTravelDestination(p1,
        DunesSite.id))
    assertEquals(Look(travelled.state).pawn(p1), DunesSite.id)

  test("a journaled situation cannot start at a table, since a stream begins with GameStarted"):
    val repository = new InMemoryEventStreamRepository
    val service = new GameApplicationService(catalog, repository)
    val driver = Situation.journaled(service, catalog, repository, "table")
    intercept[munit.FailException](Table.start.situation(driver))

  test("Look.discardAfter reads the discard pile of the region after the site's"):
    val table = Table.start.discarded(Region.Provinces, WolvesCard)
    assertEquals(Look(table.ready).discardAfter(AncientCitySite),
      Vector(WolvesCard.id))

  test("Look.forceKind reads the kind of a player's own warbands"):
    val look = Look(Table.start.ready)
    assertNotEquals(look.forceKind(p1), look.forceKind(p2))
    assertNotEquals(look.forceKind(p1), ForceKind.Bandit)

  test("relicDeckEmptied moves the relic deck to the reliquary, losing no relic"):
    val before = Table.start.ready
    val after = Table.start.relicDeckEmptied.ready
    assertEquals(after.game.current.commonCards.relicDeck, Vector.empty)
    assertEquals(after.game.campaign.reliquary,
      before.game.campaign.reliquary ++ before.game.current.commonCards.relicDeck)

  test("parkedWalker parks the action's walker at the given step"):
    val current = Table.start.parkedWalker(ActionRef.Recover).ready.game.current
    assertEquals(current.walkerProcedure, Some(ActionRef.Recover))
    assertEquals(current.walkerPending,
      Some(PendingTree(Vector("0"), Vector.empty)))

  test("visionsDrawn sets the Visions Drawn track"):
    assertEquals(Table.start.visionsDrawn(3).ready.game.current.tracks
      .visionsDrawn, 3)

  test("revealedVision reveals the Vision faceup and takes it from the deck"):
    val current = Table.start.revealedVision(p1, VisionRules.Faith).ready
      .game.current
    assertEquals(current.player(p1).map(_.revealedVisions),
      Some(Vector(VisionState(VisionRules.Faith, Orientation.FaceUp))))
    assert(!current.commonCards.worldDeck.contains(VisionRules.Faith))

  test("revealedVision adds to the Visions already revealed"):
    val current = Table.start.revealedVision(p1, VisionRules.Faith)
      .revealedVision(p1, VisionRules.Conquest).ready.game.current
    assertEquals(current.player(p1).map(_.revealedVisions),
      Some(Vector(VisionState(VisionRules.Faith, Orientation.FaceUp),
        VisionState(VisionRules.Conquest, Orientation.FaceUp))))

  test("backInPhase changes the phase and keeps the powers used this turn"):
    val wolves = WolvesCard.id
    val use = PowerUseRef(PowerTiming.Act, PowerSourceRef.Card(wolves),
      PowerId("denizen.wolves"))
    // The used power is the subject: no step records a use.
    val used = Table.start.update(_.updateCurrent(c => c.copy(turn =
      c.turn.copy(usedPowers = Set(use)))))
    val turn = used.backInPhase(Phase.Wake).ready.game.current.turn
    assertEquals(turn.phase, Phase.Wake)
    assertEquals(turn.usedPowers, Set(use))

  test("the quiet table's banners lie on the faces the banner steps default to"):
    val banners = Table.start.ready.game.current.banners
    assertEquals(banners.peoplesFavor.active, PeoplesFavorFace.Mob)
    assertEquals(banners.darkestSecret.active, DarkestSecretFace.WanderingFlame)

  test("the banner steps set the face they are given"):
    val banners = Table.start
      .peoplesFavor(Some(p1), 2, face = PeoplesFavorFace.GrandCouncil)
      .darkestSecret(Some(p2), 1, face = DarkestSecretFace.Festival)
      .ready.game.current.banners
    assertEquals(banners.peoplesFavor.active, PeoplesFavorFace.GrandCouncil)
    assertEquals(banners.darkestSecret.active, DarkestSecretFace.Festival)

  test("Look.tokensOn fails for a card at no site and in no play area"):
    // The quiet table deals no denizen to a site or a player.
    intercept[munit.FailException](
      Look(Table.start.ready).tokensOn(AlchemistCard))

  test("dispossessed holds exactly the named cards, and moves the cards it " +
      "held to the bottom of the world deck"):
    // The quiet table's Dispossessed is empty, so Elders is put there first.
    val ready = Table.start.dispossessed(EldersCard)
      .dispossessed(DazzleCard, ScryerCard).ready
    assertEquals(ready.game.campaign.dispossessed,
      Vector[WorldCardId](DazzleCard.id, ScryerCard.id))
    assertEquals(ready.game.current.commonCards.worldDeck.lastOption,
      Some(EldersCard.id))
