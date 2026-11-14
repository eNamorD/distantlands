package distantlands.application.gamelog

import distantlands.application.{GameApplicationService,
  InMemoryEventStreamRepository}
import distantlands.gameplay.chronicle.legacies.{IronHandCard, RivalCard}
import distantlands.gameplay.powers.setup.GreatClockCard
import distantlands.gameplay.setup.FirstGameSetupFixture.catalog
import distantlands.gameplay.setup.{AtlasPlacement, ImperialMaps,
  LaterGameFixture, SetupLegacyFixture, SetupProcedure}
import distantlands.gameplay.setup.legacies.{CircleOfSwordsCard,
  MatchmakerCard, Populist, PopulistCard, QuestToDistantLands,
  QuestToDistantLandsCard, ScepterBearerCard, SecretSociety,
  SecretSocietyCard, Seer, SeerCard, TheStandardBearer,
  TheStandardBearerCard, Wealthy, WealthyCard, WorldCrafter, WorldCrafterCard}
import distantlands.model._
import distantlands.model.DecisionAnswer.ChooseOneAnswer
import distantlands.testkit.{Answers, Dice, Look, MenuPlay, Situation}
import distantlands.testkit.Table.p1

/** [[LogScripts]]' scripts for a later game. */
object LaterGameLogScripts:
  /** A later game's Setup: l1 the seated Chancellor, l2 a Citizen, the
    * Reliquary holding two relics, and every choice the first offered. */
  def laterGameWoken(using munit.Location): Script =
    laterGameWokenFrom("later-game-setup", identity)

  /** As [[laterGameWoken]], with Foundation III on Public Ambitions: game
    * start deals 4 legacies faceup to the public row, and nobody draws. */
  def laterGamePublicAmbitions(using munit.Location): Script =
    laterGameWokenFrom("later-game-public-ambitions", chronicle =>
      chronicle.copy(foundations = chronicle.foundations.updated(
        FoundationNumber.III,
        FoundationState(FoundationFace.Altered, Set.empty))))

  /** As [[laterGameWoken]], with the Great Clock stored at the first Atlas
    * site that keeps an edifice, a site that comes into play, so its ruined face, Broken Clock,
    * advances the round marker to round 2 at Setup's end. */
  def laterGameWithBrokenClock(using munit.Location): Script =
    laterGameWokenFrom("later-game-broken-clock", chronicle =>
      val first = chronicle.atlasBox.indexWhere(
        _.items.exists(_.isInstanceOf[EdificeId]))
      chronicle.copy(atlasBox = chronicle.atlasBox.updated(first,
        chronicle.atlasBox(first).copy(items = Vector(GreatClockCard.id)))))

  /** As [[laterGameWoken]], with the Chancellor filling the map from
    * Forgotten Sites: Golden Valley, the Atlas's last site, takes the top
    * slot of the Provinces. */
  def laterGameFilledFromForgotten(using munit.Location): Script =
    laterGameWokenFrom("later-game-forgotten-sites", identity, {
      case park if park.decisionId == ImperialMaps.decisionId =>
        ChooseOneAnswer(AtlasPlacement.endOption(AtlasEnd.Forgotten))
    })

  /** As [[laterGameWoken]], with game start's legacy powers: l3 an Exile
    * holding Circle of Swords, Iron Hand and Rival active, and l2 a
    * Citizen holding Scepter Bearer active (Legacy Setup powers, slice 1).
    * Setup's log opens with their lines. */
  def laterGameSetupLegacies(using munit.Location): Script =
    laterGameWokenFrom("later-game-setup-legacies", chronicle =>
      SetupLegacyFixture.holding(SetupLegacyFixture.holding(chronicle,
        LineageId("l3"), Role.Exile, CircleOfSwordsCard.id -> true,
        IronHandCard.id -> true, RivalCard.id -> true),
        LineageId("l2"), Role.Citizen, ScepterBearerCard.id -> true))

  /** As [[laterGameWoken]], with the foundations' legacy powers: l3 an
    * Exile holding World Crafter and Matchmaker active, l2 a Citizen
    * holding Populist active (Legacy Setup powers, slice 2). World Crafter
    * fills slot 2 from Forgotten Sites and every other slot from Recent
    * Sites, Populist divides, and Matchmaker keeps each draw's first
    * legacy for p1 and p2. */
  def laterGameSetupFoundations(using munit.Location): Script =
    laterGameWokenFrom("later-game-setup-foundations", chronicle =>
      SetupLegacyFixture.holding(SetupLegacyFixture.holding(chronicle,
        LineageId("l3"), Role.Exile, WorldCrafterCard.id -> true,
        MatchmakerCard.id -> true),
        LineageId("l2"), Role.Citizen, PopulistCard.id -> true), {
      case park if park.decisionId ==
          WorldCrafter.slotDecisionId(PlayerId("p3"), 2) =>
        ChooseOneAnswer(AtlasPlacement.endOption(AtlasEnd.Forgotten))
      case park if park.decisionId == Populist.decisionId(PlayerId("p2")) =>
        ChooseOneAnswer(Populist.divide)
    })

  /** As [[laterGameWoken]], with the world deck's legacy powers: l2 a
    * Citizen holding Secret Society and Seer active (Legacy Setup powers,
    * slice 3). Secret Society takes the first Vision it is offered; Seer
    * puts every Vision left on top of the deck, in its order. */
  def laterGameSetupWorldDeck(using munit.Location): Script =
    val p2 = PlayerId("p2")
    laterGameWokenFrom("later-game-setup-world-deck", chronicle =>
      SetupLegacyFixture.holding(chronicle, LineageId("l2"), Role.Citizen,
        SecretSocietyCard.id -> true, SeerCard.id -> true), {
      case park if park.decisionId == SecretSociety.decisionId(p2) =>
        park.decide.query match
          case one: DecisionQuery.ChooseOne => ChooseOneAnswer(one.options(1).ref)
          case other => throw new AssertionError(s"unexpected query $other")
      case park if park.decisionId == Seer.decisionId(p2) =>
        park.decide.query match
          case amounts: DecisionQuery.ChooseAmounts =>
            DecisionAnswer.DistributeAnswer(amounts.slots.zipWithIndex.map(
              (slot, n) => DistributeAmount(slot.ref, n + 1)))
          case other => throw new AssertionError(s"unexpected query $other")
    })

  /** A later game from Setup through a Quest holder's first Wake (Legacy
    * Setup powers, slice 4): l2, a Citizen, holds The Standard Bearer,
    * Wealthy and Quest to Distant Lands active, and two denizens wait in
    * the Dispossessed. p2 stays off the map, takes the People's Favor and
    * 3 favor from the bank, and swaps the first Dispossessed card for the
    * first card of their hand; p1 plays a turn; p2's first Wake places the
    * pawn at the first site, keeps an adviser and reveals nothing. */
  def laterGameQuestFirstWake(using munit.Location): Script =
    val name = "later-game-quest-first-wake"
    val p2 = PlayerId("p2")
    val base = LaterGameFixture.chronicle(LineageId("l1"), LineageId("l2"))
    val (waiting, deck) = base.worldDeck.splitAt(2)
    val record = SetupLegacyFixture.holding(base.copy(worldDeck = deck,
      dispossessed = waiting), LineageId("l2"), Role.Citizen,
      TheStandardBearerCard.id -> true, WealthyCard.id -> true,
      QuestToDistantLandsCard.id -> true)
    val answers: Answers =
      case park if park.decisionId == SetupProcedure.pawnDecisionId(p2) =>
        ChooseOneAnswer(QuestToDistantLands.stayOff)
      case park if park.decisionId == TheStandardBearer.decisionId(p2) =>
        ChooseOneAnswer(DecisionOptionRef.Banner(Banner.PeoplesFavor))
      case park if park.decisionId == Wealthy.decisionId(p2) =>
        ChooseOneAnswer(Wealthy.fromBank)
      case park if park.decisionId == QuestToDistantLands.swapDecisionId(p2) =>
        ChooseOneAnswer(DecisionOptionRef.Denizen(waiting.sortBy(_.value).head))
    val repository = new InMemoryEventStreamRepository
    val service = new GameApplicationService(catalog, repository,
      campaignDicePort = Dice.steady)
    Situation.wake(Situation.journaled(service, catalog, repository, name)
      .withAnswers(answers), record, LaterGameFixture.orders(record, p1))
      .endingWake(p1).after(MenuPlay.start(p1, PhaseTransitionRef.BeginRest))
    Script(name, service, p2)

  /** The six-power later game the whole-feature review played (Legacy
    * Setup powers, slice 4): l3, an Exile, holds Rival, Iron Hand, Circle
    * of Swords, Quest to Distant Lands, Secret Society and Wealthy, and
    * stays off the map; l2, a Citizen, holds Matchmaker, World Crafter,
    * Populist, Seer and The Standard Bearer; two denizens wait in the
    * Dispossessed. Secret Society takes the first Vision, Seer puts the
    * rest on top in order, Populist divides, The Standard Bearer takes the
    * People's Favor, and Quest swaps the first Dispossessed card. p1
    * campaigns against p3's Iron Hand site while p3 has no pawn; p1 and p2
    * then rest, and p3's first Wake places the pawn. */
  def laterGameCombinedLegacies(using munit.Location): Script =
    combinedLegacies._1

  /** [[laterGameCombinedLegacies]] and its Chronicle. */
  def combinedLegacies(using munit.Location): (Script, Chronicle) =
    val name = "later-game-combined-legacies"
    val (p2, p3) = (PlayerId("p2"), PlayerId("p3"))
    val base = LaterGameFixture.chronicle(LineageId("l1"), LineageId("l2"))
    val (waiting, deck) = base.worldDeck.splitAt(2)
    val record = SetupLegacyFixture.holding(SetupLegacyFixture.holding(
      base.copy(worldDeck = deck, dispossessed = waiting),
      LineageId("l3"), Role.Exile, RivalCard.id -> true,
      IronHandCard.id -> true, CircleOfSwordsCard.id -> true,
      QuestToDistantLandsCard.id -> true, SecretSocietyCard.id -> true,
      WealthyCard.id -> true),
      LineageId("l2"), Role.Citizen, MatchmakerCard.id -> true,
      WorldCrafterCard.id -> true, PopulistCard.id -> true,
      SeerCard.id -> true, TheStandardBearerCard.id -> true)
    val answers: Answers =
      case park if park.decisionId == SetupProcedure.pawnDecisionId(p3) =>
        ChooseOneAnswer(QuestToDistantLands.stayOff)
      case park if park.decisionId == SecretSociety.decisionId(p3) =>
        park.decide.query match
          case one: DecisionQuery.ChooseOne => ChooseOneAnswer(one.options(1).ref)
          case other => throw new AssertionError(s"unexpected query $other")
      case park if park.decisionId == Seer.decisionId(p2) =>
        park.decide.query match
          case amounts: DecisionQuery.ChooseAmounts =>
            DecisionAnswer.DistributeAnswer(amounts.slots.zipWithIndex.map(
              (slot, n) => DistributeAmount(slot.ref, n + 1)))
          case other => throw new AssertionError(s"unexpected query $other")
      case park if park.decisionId == Populist.decisionId(p2) =>
        ChooseOneAnswer(Populist.divide)
      case park if park.decisionId == TheStandardBearer.decisionId(p2) =>
        ChooseOneAnswer(DecisionOptionRef.Banner(Banner.PeoplesFavor))
      case park if park.decisionId == QuestToDistantLands.swapDecisionId(p3) =>
        ChooseOneAnswer(DecisionOptionRef.Denizen(waiting.sortBy(_.value).head))
    val repository = new InMemoryEventStreamRepository
    val service = new GameApplicationService(catalog, repository,
      campaignDicePort = Dice.steady)
    Situation.wake(Situation.journaled(service, catalog, repository, name)
      .withAnswers(answers), record, LaterGameFixture.orders(record, p1))
      .endingWake(p1).after(MenuPlay.start(p1, ActionRef.Campaign))
      .after(MenuPlay.start(p1, PhaseTransitionRef.BeginRest))
      .endingWake(p2).after(MenuPlay.start(p2, PhaseTransitionRef.BeginRest))
    (Script(name, service, p3), record)

  /** A later game's Setup on stream `name`, from the fixture's Chronicle as
    * `changed` leaves it, each park answered by `answers` where it names
    * one and as the situation does by default otherwise. */
  private def laterGameWokenFrom(name: String,
      changed: Chronicle => Chronicle,
      answers: Answers = PartialFunction.empty)(using munit.Location)
      : Script =
    val record = LaterGameFixture.chronicle(LineageId("l1"), LineageId("l2"))
    val chronicle = changed(record.copy(relicDeck = record.relicDeck.drop(2),
      reliquary = record.relicDeck.take(2)))
    val repository = new InMemoryEventStreamRepository
    val service = new GameApplicationService(catalog, repository,
      campaignDicePort = Dice.steady)
    val situation = Situation.wake(Situation.journaled(service, catalog,
      repository, name).withAnswers(answers), chronicle,
      LaterGameFixture.orders(chronicle, p1))
    Script(name, service, Look(situation.ready).active)

  /** The next game, from the Chronicle
    * [[ChronicleLogScripts.oathsHeirloomPlayed]] journals: p2's lineage
    * starts with its two kept advisers and Cup of Plenty, facedown, and
    * draws no hand (Legacy Chronicle powers, R27). */
  def laterGameAfterOaths(using munit.Location): Script =
    val record = ChronicleLogScripts.oathsHeirloomPlayed._2
    val repository = new InMemoryEventStreamRepository
    val service = new GameApplicationService(catalog, repository,
      campaignDicePort = Dice.steady)
    val situation = Situation.wake(Situation.journaled(service, catalog,
      repository, "later-game-after-oaths"), record,
      LaterGameFixture.orders(record, p1))
    Script("later-game-after-oaths", service, Look(situation.ready).active)

  val named: Vector[(String, () => Script)] = Vector(
    "later-game-setup" -> (() => laterGameWoken),
    "later-game-setup-legacies" -> (() => laterGameSetupLegacies),
    "later-game-setup-foundations" -> (() => laterGameSetupFoundations),
    "later-game-setup-world-deck" -> (() => laterGameSetupWorldDeck),
    "later-game-quest-first-wake" -> (() => laterGameQuestFirstWake),
    "later-game-combined-legacies" -> (() => laterGameCombinedLegacies),
    "later-game-forgotten-sites" -> (() => laterGameFilledFromForgotten),
    "later-game-after-oaths" -> (() => laterGameAfterOaths))
