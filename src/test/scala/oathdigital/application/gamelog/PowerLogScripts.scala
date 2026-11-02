package oathdigital.application.gamelog

import oathdigital.gameplay.actions.cardplay.CardPlayProcedure.PlayedAs
import oathdigital.application._
import oathdigital.gameplay.{ModifierPlay, TravelPlay}
import oathdigital.gameplay.actions.VisionRules
import oathdigital.gameplay.actions.travel.TravelProcedure
import oathdigital.gameplay.powers.action.{BarbedNet, BarbedNetCard,
  DowsingSticksCard, GamblingHall, GamblingHallCard, Oracle, OracleCard, Wolves,
  WolvesCard}
import oathdigital.gameplay.powers.campaign.StickyFireCard
import oathdigital.gameplay.powers.cardplay.{WildCry, WildCryCard}
import oathdigital.gameplay.powers.economy.BirdsongCard
import oathdigital.gameplay.powers.search.{Augury, AuguryCard, NewsFromAfar,
  NewsFromAfarCard}
import oathdigital.gameplay.powers.travel.{Tents, TentsCard}
import oathdigital.gameplay.powers.wake.{AncientCitySite, HungerCard}
import oathdigital.gameplay.powers.whenplayed.{FamilyHeirloom,
  FamilyHeirloomCard, WizardsConclaveCard}
import oathdigital.model._
import oathdigital.model.DecisionAnswer.ChooseOneAnswer
import oathdigital.testkit.{Answers, Dice, Look, MenuPlay, PlacementPicks}
import oathdigital.testkit.Table.{p1, p2, p3}
import LogScriptSupport.{atTable, tableWithOnePawnPerSite}

/** The scripts that play one named power: a modifier picked into an
  * action (Augury, News from Afar, Wild Cry, Tents), a power used in Act
  * (Silver Tongue, Gambling Hall, Wolves, Oracle, Barbed Net), a forced
  * step (Hunger) and a When Played draw (Family Heirloom). Reached through
  * `LogScripts`.
  */
object PowerLogScripts:
  /** Silver Tongue used in Rest, its bank choice answered by default.
    * ParkedServiceFixture.silverTonguePark states where each pawn stands. */
  def silverTongue(using munit.Location): Script =
    val ParkedServiceFixture.SilverTonguePark(game, actor, _) =
      ParkedServiceFixture.silverTonguePark("use-power", Dice.steady)
    game.situation.after()
    Script("use-power", game.service, actor)

  /** Augury, a free Search modifier, stands at p1's site; the Search picks
    * it. */
  def augury(using munit.Location): Script =
    val (service, act) = atTable("augury", tableWithOnePawnPerSite
      .denizen(AuguryCard, AncientCitySite).banditsRefilled)
    act.withAnswers(ModifierPlay.answers(PowerWindow.SearchModifierSelection,
      Vector(Augury.id))).after(MenuPlay.start(p1, ActionRef.Search))
    Script("augury", service, p1)

  /** Augury at p1's site and News from Afar as p1's adviser, with 2 favor
    * to pay for it. The Search picks News from Afar, then Augury; every
    * modifier is then picked, so it starts by itself. News from Afar spends
    * no Supply. */
  def twoPicks(using munit.Location): Script =
    val (service, act) = atTable("two-picks", tableWithOnePawnPerSite
      .denizen(AuguryCard, AncientCitySite).adviser(p1, NewsFromAfarCard)
      .favor(p1, 2).banditsRefilled)
    act.withAnswers(ModifierPlay.answers(PowerWindow.SearchModifierSelection,
      Vector(NewsFromAfar.id, Augury.id))).after(MenuPlay.start(p1, ActionRef.Search))
    Script("two-picks", service, p1)

  /** Wild Cry at p1's site and a facedown adviser: the play picks Wild Cry,
    * then plays the adviser by default. */
  def facedownWithCry(using munit.Location): Script =
    val conclave = WizardsConclaveCard.id
    val (service, act) = atTable("facedown-cry", tableWithOnePawnPerSite
      .denizen(WildCryCard, AncientCitySite)
      .adviser(p1, conclave, facedown = true).banditsRefilled)
    act.withAnswers(ModifierPlay.answers(PowerWindow.SearchModifierSelection,
      Vector(WildCry.id))).after(MenuPlay.start(p1, ActionRef.PlayFacedownAdviser))
    Script("facedown-cry", service, p1)

  /** Tents as p1's adviser, with 1 favor to pay for it. The Travel picks
    * Tents, its only modifier, so it starts by itself, then goes to the
    * other site of Ancient City's region, which Tents makes free. */
  def travelWithTents(using munit.Location): Script =
    val (service, act) = atTable("tents", tableWithOnePawnPerSite
      .adviser(p1, TentsCard).favor(p1, 1).banditsRefilled)
    val map = act.ready.game.current.map
    val home = Look(act.ready).pawn(p1)
    val nearby = map.inPlay.find(site => site != home &&
      map.regionOf(site) == map.regionOf(home)).getOrElse(
      munit.Assertions.fail("Ancient City's region has one site"))
    val destination: Answers = {
      case park if park.decisionId == TravelProcedure.destinationDecisionId =>
        TravelPlay.destination(nearby)
    }
    act.withAnswers(destination.orElse(ModifierPlay.answers(
      PowerWindow.TravelModifierSelection, Vector(Tents.id))))
      .after(ParkedServiceFixture.startTravel(p1))
    Script("tents", service, p1)

  /** Gambling Hall at p1's site, used in Act by a player holding two favor.
    * Gambling Hall rolls 4 defense dice and Dice.steady makes each two
    * shields, so they total 8, and the richest bank is chosen so the gain is
    * never empty. */
  def gamblingHall(using munit.Location): Script =
    val card = GamblingHallCard.id
    val table = tableWithOnePawnPerSite.denizen(GamblingHallCard, AncientCitySite)
      .favor(p1, 2).banditsRefilled
    val richest = Suit.all.maxBy(suit => table.ready.banks.favor(suit))
    val (service, act) = atTable("gambling-hall", table)
    act.withAnswers {
      case park if park.decisionId == GamblingHall.decisionId =>
        ChooseOneAnswer(DecisionOptionRef.FavorBank(richest))
    }.after(MenuPlay.start(p1, ActionRef.UsePower(GamblingHall.id), Vector(DecisionOptionRef.Denizen(card))))
    Script("gambling-hall", service, p1)

  /** Wolves at p1's site, used in Act by a player holding two secrets. The
    * board question parks; the answer kills one of the other player's
    * warbands with the most of them. */
  def wolves(using munit.Location): Script =
    val card = WolvesCard.id
    // p3 has fewer warbands on its board, so p2 is the one with the most.
    val table = tableWithOnePawnPerSite.denizen(WolvesCard, AncientCitySite)
      .secrets(p1, faceUp = 2).boardWarbands(p3, 2).banditsRefilled
    val victim = table.ready.game.current.players.filter(_.player != p1)
      .maxBy(_.board.warbands).player
    val (service, act) = atTable("wolves", table)
    act.withAnswers {
      case park if park.decisionId == Wolves.decisionId =>
        ChooseOneAnswer(DecisionOptionRef.Player(victim))
    }.after(MenuPlay.start(p1, ActionRef.UsePower(Wolves.id), Vector(DecisionOptionRef.Denizen(card))))
    Script("wolves", service, p1)

  /** Oracle at p1's site, used in Act by a player holding three secrets.
    * Vision of Sanctuary tops the world deck; Oracle draws it, and the actor
    * keeps it as a facedown adviser. */
  def oracle(using munit.Location): Script =
    oracle("oracle", PlayedAs.Facedown)

  /** Oracle, its drawn Vision of Sanctuary played as `placement` names. */
  def oracle(name: String, placement: PlayedAs)(using munit.Location): Script =
    val card = OracleCard.id
    val (service, act) = atTable(name, tableWithOnePawnPerSite
      .denizen(OracleCard, AncientCitySite).secrets(p1, faceUp = 3)
      .worldDeckTop(VisionRules.Sanctuary).banditsRefilled)
    act.withAnswers {
      case park if park.decisionId.startsWith(LogWords.PlacePrefix) =>
        ChooseOneAnswer(PlacementPicks.pick(park.decide, placement))
    }.after(MenuPlay.start(p1, ActionRef.UsePower(Oracle.id), Vector(DecisionOptionRef.Denizen(card))))
    Script(name, service, p1)

  /** Barbed Net in p1's play area and two relics at p1's site, used in Act
    * by a player holding four secrets. The relic question parks; the answer
    * takes Dowsing Sticks. */
  def barbedNet(using munit.Location): Script =
    val net = BarbedNetCard.id
    val target = DowsingSticksCard.id
    val (service, act) = atTable("barbed-net", tableWithOnePawnPerSite
      .relic(p1, BarbedNetCard)
      .relicAt(DowsingSticksCard, AncientCitySite)
      .relicAt(StickyFireCard, AncientCitySite)
      .secrets(p1, faceUp = 4).banditsRefilled)
    act.withAnswers {
      case park if park.decisionId == BarbedNet.decisionId =>
        ChooseOneAnswer(DecisionOptionRef.Relic(target))
    }.after(MenuPlay.start(p1, ActionRef.UsePower(BarbedNet.id), Vector(DecisionOptionRef.Relic(net))))
    Script("barbed-net", service, p1)

  /** Hunger faceup with p2, added after their facedown adviser (so that
    * adviser is slot 1), whose Wake begins when p1 rests. The forced step
    * parks for them. Their only candidate is their own facedown adviser,
    * which the default answer buries, and nothing else keeps Wake open, so
    * it ends and the log ends with Act. The script's actor is Hunger's
    * holder, p2. */
  def hunger(using munit.Location): Script =
    val (service, act) = atTable("hunger", tableWithOnePawnPerSite
      .adviser(p2, BirdsongCard, facedown = true).adviser(p2, HungerCard)
      .banditsRefilled)
    act.after(MenuPlay.start(p1, PhaseTransitionRef.BeginRest))
    Script("hunger", service, p2)

  /** Family Heirloom held by p1 as a facedown adviser, then played faceup as
    * an adviser: its When Played draw is kept or put on the bottom by
    * `choice`. Dowsing Sticks tops the relic deck. */
  def familyHeirloom(name: String, choice: DecisionOptionRef.Button)
      (using munit.Location): Script =
    val (service, act) = atTable(name, tableWithOnePawnPerSite
      .adviser(p1, FamilyHeirloomCard, facedown = true)
      .relicDeckTop(DowsingSticksCard).banditsRefilled)
    act.withAnswers {
      case park if park.decisionId.startsWith(LogWords.PlacePrefix) =>
        ChooseOneAnswer(PlacementPicks.pick(park.decide, PlayedAs.Faceup))
      case park if park.decisionId == FamilyHeirloom.decisionId =>
        ChooseOneAnswer(choice)
    }.after(MenuPlay.start(p1, ActionRef.PlayFacedownAdviser))
    Script(name, service, p1)

  def heirloomKept(using munit.Location): Script =
    familyHeirloom("heirloom-kept", FamilyHeirloom.keep)

  def heirloomReturned(using munit.Location): Script =
    familyHeirloom("heirloom-returned", FamilyHeirloom.bottom)
