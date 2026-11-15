package distantlands.gameplay.powers.setup

import distantlands.gameplay.OathRules
import distantlands.gameplay.actions.challenge.SchoolOfVinesCard
import distantlands.gameplay.actions.explore.ExploreProcedure
import distantlands.gameplay.cards.{AncientCitySite, BrokenPeaksSite,
  BuriedGiantSite, DeepWoodsSite}
import distantlands.gameplay.powers.{NoteText, WalkerPowerCatalog}
import distantlands.gameplay.powers.action.{DowsingSticksCard, EldersCard,
  MessengerCard, ScryerCard}
import distantlands.gameplay.powers.campaign.StickyFireCard
import distantlands.gameplay.powers.whenplayed.{DazzleCard,
  WizardsConclaveCard}
import distantlands.gameplay.setup.FirstGameSetupFixture.catalog
import distantlands.gameplay.walker.{LogNoted, WalkerCompleted, WalkerDice}
import distantlands.model._
import distantlands.model.DecisionAnswer.ChooseOneAnswer
import distantlands.model.OathState.Ready
import distantlands.testkit.{Look, Outcomes, Table}
import distantlands.testkit.Table.{p1, p2}
import distantlands.testkit.Unwrap.accepted

/** WHEN EXPLORED (rulebook, "Setup / When Explored"): the power of a card
  * an Explore places on the map resolves right after the site is placed,
  * for that site alone. An explored site's edifice is placed ruined, so
  * these are the ruined faces. p1 explores Deep Woods, the Recent end of
  * the Atlas, into the Provinces beside Buried Giant. */
class WhenExploredSuite extends munit.FunSuite:
  /** Stub of WalkerDice: every defense die two shields, so an Explore
    * succeeds on its first roll, and every shuffle reverses its pile. */
  private val shieldsAndReversedShuffles: WalkerDice = new WalkerDice:
    def roll(kind: DiceKind, count: Int)
        : Either[OathViolation, Vector[DieFace]] =
      Right(Vector.fill(count)(DefenseDieFace.TwoShields))
    override def shuffle(count: Int): Either[OathViolation, Vector[Int]] =
      Right((0 until count).reverse.toVector)

  private val rules = new OathRules(catalog,
    walkerPowerCatalog = WalkerPowerCatalog.default(catalog),
    walkerDice = shieldsAndReversedShuffles)

  /** p1's Act under Wide Horizons at Buried Giant, the Hintermost of three
    * sites, each holding its bandits. The Atlas holds Deep Woods at its
    * Recent end and four more sites behind it. */
  private def p1AtHintermostOfThree: Table = Table.start
    .foundation(FoundationNumber.I, FoundationFace.Altered)
    .mapCutIntoAtlas(3).pawn(p1, BuriedGiantSite).banditsRefilled

  /** [[p1AtHintermostOfThree]] with `edifice` stored on Deep Woods. */
  private def deepWoodsStoring(edifice: EdificeId): Table =
    p1AtHintermostOfThree.storedInAtlas(DeepWoodsSite, edifice)

  /** p1's Explore of the Recent end, from its start to its end. */
  private def exploredDeepWoods(table: Table): OathTransition =
    val rolled = accepted(rules.startWalker(Ready(table.ready),
      ActionRef.Explore, p1))
    val placed = accepted(rules.resolveWalker(rolled.state, p1,
      ExploreProcedure.endDecisionId,
      ChooseOneAnswer(ExploreProcedure.endOption(AtlasEnd.Recent))))
    assert(placed.events.contains(WalkerCompleted(ActionRef.Explore)),
      placed.events)
    assertEquals(Look(placed.state).pawn(p1), DeepWoodsSite.id)
    placed.copy(events = rolled.events ++ placed.events)

  private def said(id: PowerId, keys: Vector[NoteKey],
      events: Vector[OathEvent]): Vector[String] =
    NoteText.said(id, keys, events).map(_.text)

  test("Bandit Market on the explored site places 1 favor on each bandit " +
      "site, the new one too, and burns 1 favor from each bank, after the " +
      "Explore's own line"):
    val table = deepWoodsStoring(GreatMarketCard.id)
    val banks = table.ready.banks.favor
    val explored = exploredDeepWoods(table)
    val look = Look(explored.state)
    assertEquals(look.cardsAt(DeepWoodsSite), Vector[CardId](GreatMarketCard.id))
    Vector(AncientCitySite, BrokenPeaksSite, BuriedGiantSite, DeepWoodsSite)
      .foreach(site => assertEquals(look.siteTokens(site).favor, 1, site.name))
    Suit.all.foreach(suit =>
      assertEquals(look.bankFavor(suit), banks(suit) - 1))
    assertEquals(said(BanditMarket.id, BanditMarket.noteKeys, explored.events),
      Vector("Placed 4 favor on the bandit sites and burned 6 favor from " +
        "the banks."))
    assertEquals(explored.events.collect { case LogNoted(power, note, _) =>
      power.value -> note.key }, Vector(
        "procedure.explore" -> "explored-recent",
        BanditMarket.id.value -> "placed-and-burned"))

  test("a Bandit Market that already stands on the map stays quiet when " +
      "another site is explored"):
    val table = p1AtHintermostOfThree
      .edifice(GreatMarketCard, EdificeSide.Ruined, at = AncientCitySite)
    val banks = table.ready.banks.favor
    val explored = exploredDeepWoods(table)
    assertEquals(Look(explored.state).siteTokens(AncientCitySite), Tokens.empty)
    assertEquals(Outcomes.ready(explored.state).banks.favor, banks)
    assertEquals(said(BanditMarket.id, BanditMarket.noteKeys, explored.events),
      Vector.empty)

  test("Broken Forge on the explored site: the explorer places their pawn " +
      "there, so every relic at a site in its region is discarded"):
    val table = deepWoodsStoring(GreatForgeCard.id)
      .relicAt(StickyFireCard, BuriedGiantSite)
      .relicAt(DowsingSticksCard, BrokenPeaksSite)
    val explored = exploredDeepWoods(table)
    val look = Look(explored.state)
    // Buried Giant shares the Provinces with Deep Woods; Broken Peaks is in
    // the Cradle.
    assertEquals(look.relicsAt(BuriedGiantSite), Vector.empty)
    assertEquals(look.relicsAt(BrokenPeaksSite), Vector(DowsingSticksCard.id))
    assertEquals(said(BrokenForge.id, BrokenForge.noteKeys, explored.events),
      Vector(s"Discarded ${StickyFireCard.id.value}."))

  test("a Proving Grounds that already stands where the explorer started " +
      "gives no warbands: the pawn is placed at the explored site"):
    val table = p1AtHintermostOfThree
      .edifice(ProvingGroundsCard, EdificeSide.Intact, at = BuriedGiantSite)
    val before = Look(table.ready).boardWarbands(p1)
    val explored = exploredDeepWoods(table)
    assertEquals(Look(explored.state).boardWarbands(p1), before)

  test("Empty Grounds on the explored site discards every other denizen " +
      "in its region and stays"):
    val table = deepWoodsStoring(ProvingGroundsCard.id)
      .storedInAtlas(DeepWoodsSite, WizardsConclaveCard)
      .denizen(EldersCard, at = BuriedGiantSite)
      .denizen(MessengerCard, at = AncientCitySite)
    val explored = exploredDeepWoods(table)
    val look = Look(explored.state)
    assertEquals(look.cardsAt(DeepWoodsSite),
      Vector[CardId](ProvingGroundsCard.id))
    assertEquals(look.cardsAt(BuriedGiantSite), Vector.empty)
    assertEquals(look.cardsAt(AncientCitySite), Vector[CardId](MessengerCard.id))

  test("Hall of Mockery on the explored site places 4 favor on the " +
      "People's Favor and burns 1 favor from each bank"):
    val explored = exploredDeepWoods(deepWoodsStoring(HallOfDebateCard.id))
    val look = Look(explored.state)
    assertEquals(look.favorOnPeoplesFavor, 1 + HallOfMockery.Favor)
    assertEquals(said(HallOfMockery.id, HallOfMockery.noteKeys,
      explored.events), Vector("Placed 4 favor on the People's Favor and " +
        "burned 6 favor from the banks."))

  test("Hall of Mockery on the explored site takes the People's Favor from " +
      "its holder back to the shared bank"):
    val table = deepWoodsStoring(HallOfDebateCard.id)
      .peoplesFavor(Some(p2), favor = 1)
    val explored = exploredDeepWoods(table)
    assertEquals(Look(explored.state).peoplesFavorHolder, None)
    assertEquals(said(HallOfMockery.id, HallOfMockery.noteKeys,
      explored.events), Vector("Placed 4 favor on the People's Favor, moved " +
        "it to the shared bank and burned 6 favor from the banks."))

  test("Lost Guardians on the explored site takes the Darkest Secret from " +
      "its holder, the explorer, back to the shared bank"):
    val table = deepWoodsStoring(TomeGuardiansCard.id)
      .darkestSecret(Some(p1), secrets = 1)
    val explored = exploredDeepWoods(table)
    assertEquals(Look(explored.state).darkestSecretHolder, None)
    assertEquals(said(LostGuardians.id, LostGuardians.noteKeys,
      explored.events), Vector("Placed 3 secrets on the Darkest Secret and " +
        "moved it to the shared bank."))

  test("Lost Guardians on the explored site places 3 secrets on the " +
      "Darkest Secret"):
    val explored = exploredDeepWoods(deepWoodsStoring(TomeGuardiansCard.id))
    assertEquals(Look(explored.state).secretsOnDarkestSecret,
      1 + LostGuardians.Secrets)

  test("Ruined School on the explored site places 1 secret on each bandit " +
      "site"):
    val explored = exploredDeepWoods(deepWoodsStoring(SchoolOfVinesCard.id))
    val look = Look(explored.state)
    Vector(AncientCitySite, BrokenPeaksSite, BuriedGiantSite, DeepWoodsSite)
      .foreach(site => assertEquals(look.siteTokens(site).secrets, 1,
        site.name))
    assertEquals(said(RuinedSchool.id, RuinedSchool.noteKeys, explored.events),
      Vector("Placed 4 secrets on the bandit sites."))

  test("Broken Horn on the explored site buries its region's discard " +
      "pile"):
    val table = deepWoodsStoring(ForestHornCard.id)
      .discarded(Region.Provinces, EldersCard, MessengerCard)
      .discarded(Region.Cradle, ScryerCard)
    val deck = table.ready.game.current.commonCards.worldDeck
    val explored = exploredDeepWoods(table)
    val look = Look(explored.state)
    assertEquals(look.discardPile(Region.Provinces), Vector.empty)
    assertEquals(look.discardPile(Region.Cradle),
      Vector[WorldCardId](ScryerCard.id))
    assertEquals(Outcomes.ready(explored.state).game.current.commonCards
      .worldDeck.size, deck.size + 2)
    assertEquals(said(BrokenHorn.id, BrokenHorn.noteKeys, explored.events),
      Vector("Buried the Provinces discard pile (2 cards)."))

  test("Broken Clock on the explored site advances the round marker once, " +
      "and on round 8 does nothing"):
    val third = exploredDeepWoods(deepWoodsStoring(GreatClockCard.id)
      .roundMarkerAt(3))
    assertEquals(Outcomes.ready(third.state).game.current.tracks.round, 4)
    assertEquals(said(BrokenClock.id, BrokenClock.noteKeys, third.events),
      Vector("Advanced the round marker to round 4."))
    val last = exploredDeepWoods(deepWoodsStoring(GreatClockCard.id)
      .roundMarkerAt(RoundMarker.LastRound))
    assertEquals(Outcomes.ready(last.state).game.current.tracks.round, 8)
    assertEquals(Outcomes.ready(last.state).game.current.result, None)
    assertEquals(said(BrokenClock.id, BrokenClock.noteKeys, last.events),
      Vector.empty)

  test("Fallen Spire on the explored site swaps the denizens of its " +
      "region's discard pile with the Dispossessed"):
    val table = deepWoodsStoring(GreatSpireCard.id)
      .discarded(Region.Provinces, EldersCard, MessengerCard)
      .dispossessed(DazzleCard, ScryerCard)
    val explored = exploredDeepWoods(table)
    val after = Outcomes.ready(explored.state)
    assertEquals(after.game.current.commonCards.discard(Region.Provinces)
      .toSet, Set[WorldCardId](DazzleCard.id, ScryerCard.id))
    assertEquals(after.game.campaign.dispossessed.toSet,
      Set[WorldCardId](EldersCard.id, MessengerCard.id))
    assertEquals(said(FallenSpire.id, FallenSpire.noteKeys, explored.events),
      Vector("Swapped 2 cards between the Dispossessed and the Provinces " +
        "discard pile, then shuffled the pile."))

  test("an explored site with no edifice resolves nothing"):
    val explored = exploredDeepWoods(p1AtHintermostOfThree)
    assertEquals(explored.events.collect { case LogNoted(power, _, _) =>
      power.value }, Vector("procedure.explore"))
