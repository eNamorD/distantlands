package distantlands.application.gamelog

import distantlands.application.{GameApplicationService,
  InMemoryEventStreamRepository}
import distantlands.gameplay.chronicle.legacies.{IronHandCard, RivalCard}
import distantlands.gameplay.powers.setup.GreatClockCard
import distantlands.gameplay.setup.FirstGameSetupFixture.catalog
import distantlands.gameplay.setup.{AtlasPlacement, ImperialMaps,
  LaterGameFixture, SetupLegacyFixture}
import distantlands.gameplay.setup.legacies.{CircleOfSwordsCard,
  ScepterBearerCard}
import distantlands.model._
import distantlands.model.DecisionAnswer.ChooseOneAnswer
import distantlands.testkit.{Answers, Dice, Look, Situation}
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
    "later-game-forgotten-sites" -> (() => laterGameFilledFromForgotten),
    "later-game-after-oaths" -> (() => laterGameAfterOaths))
