package oathdigital.application.gamelog

import oathdigital.gameplay.powers.economy.HallowedSpringCard
import oathdigital.application._
import oathdigital.gameplay.cards.{AncientCitySite, BrokenPeaksSite,
  DeepWoodsSite}
import oathdigital.gameplay.phases.PhaseMenu
import oathdigital.gameplay.phases.wake.TakeWealthProcedure
import oathdigital.gameplay.powers.campaign.StickyFireCard
import oathdigital.gameplay.powers.economy.BirdsongCard
import oathdigital.gameplay.powers.rest.InsomniaCard
import oathdigital.gameplay.powers.whenplayed.WizardsConclaveCard
import oathdigital.gameplay.setup.{FirstGameSetupFixture, SetupProcedure}
import oathdigital.gameplay.setup.FirstGameSetupFixture.catalog
import oathdigital.model._
import oathdigital.testkit.{Answers, Dice, Look, MenuPlay, Situation, Table}
import oathdigital.testkit.Unwrap.as
import oathdigital.testkit.Table.{p1, p2, p3}
import LogScriptSupport.{atTable, start, tableWithOnePawnPerSite}

/** The scripts that play Setup and the phases around a turn: Setup to the
  * first Wake, a whole round, the Wake and Rest menus, Take Wealth and the
  * Oathkeeper tie. Reached through `LogScripts`.
  */
object PhaseLogScripts:
  /** A short journal on a table, for the suites that append their own
    * recorded events to a real board and read the lines they post. The board
    * holds the facts those suites lean on, each stated here: a relic lying at
    * Broken Peaks, a homeland edifice at Deep Woods, and a facedown adviser
    * with each of p2 and p3. p1, the actor, wakes and ends Wake, and it is
    * their Act. */
  def recordedEventsBoard(using munit.Location): Script =
    val (service, waking) = atTable("board", tableWithOnePawnPerSite.turn(p1, Phase.Wake)
      .relicAt(StickyFireCard, BrokenPeaksSite)
      .edifice(HallowedSpringCard, EdificeSide.Ruined, DeepWoodsSite)
      .adviser(p2, BirdsongCard, facedown = true)
      .adviser(p3, WizardsConclaveCard, facedown = true)
      .banditsRefilled)
    waking.endingWake(p1)
    Script("board", service, p1)

  /** p1's Wake with one favor at Ancient City and nothing else to take. */
  private def wealthTable: Table = tableWithOnePawnPerSite
    .turn(p1, Phase.Wake).siteTokens(AncientCitySite, favor = 1, secrets = 0)
    .banditsRefilled

  /** p1 takes the favor by picking it from the Wake menu. */
  def wealthFromMenu(using munit.Location): Script =
    val (service, waking) = atTable("menu-wealth",
      wealthTable.phaseMenuParked)
    waking.after(MenuPlay.picking(p1,
      PhaseMenu.takeWealth(WakeResource.Favor)))
    Script("menu-wealth", service, p1)

  /** p1 rests holding Insomnia and uses it by picking it from the Rest menu,
    * which ends the Rest. */
  def restFromMenu(using munit.Location): Script =
    val (service, resting) = atTable("menu-rest", tableWithOnePawnPerSite
      .adviser(p1, InsomniaCard).turn(p1, Phase.Rest).banditsRefilled
      .phaseMenuParked)
    resting.after(MenuPlay.picking(p1,
      DecisionOptionRef.Denizen(InsomniaCard.id)))
    Script("menu-rest", service, p1)

  /** The same take, started directly, as before the menus. */
  def wealthStarted(using munit.Location): Script =
    val (service, waking) = atTable("started-wealth", wealthTable)
    waking.after(MenuPlay.start(p1, ActionRef.TakeWealth, TakeWealthProcedure.selection(WakeResource.Favor)))
    Script("started-wealth", service, p1)

  /** Setup to the first player's Wake, on the real start: the script that
    * replays Setup with every park answered by default, so its log carries
    * the setup lines. Pawns are spread one per site. */
  def woken(using munit.Location): Script =
    wokenAnswering("woken", PartialFunction.empty)

  /** As `woken`, with every Reveal Cards answered by revealing every card it
    * offers. */
  def wokenRevealingAll(using munit.Location): Script =
    wokenAnswering("revealed", {
      case park if park.decisionId ==
          SetupProcedure.revealDecisionId(park.awaiting) =>
        val offered = as[DecisionQuery.ChooseMany](park.decide.query).options
        DecisionAnswer.ChooseManyAnswer(offered.map(_.ref))
    })

  /** Setup to the first player's Wake on stream `name`, pawns spread one per
    * site, `answers` answering the parks they match first. */
  private def wokenAnswering(name: String, answers: Answers)(
      using munit.Location): Script =
    val repository = new InMemoryEventStreamRepository
    val service = new GameApplicationService(catalog, repository,
      campaignDicePort = Dice.steady)
    val situation = Situation.wake(Situation.journaled(service, catalog,
      repository, name).withAnswers(
        answers orElse Situation.pawnsAt(FirstGameSetupFixture.sites)))
    Script(name, service, Look(situation.ready).active)

  /** The first player wakes, travels and rests; every other player rests;
    * Round 2 begins. */
  def round(using munit.Location): Script =
    val (service, woken) = atTable("round",
      tableWithOnePawnPerSite.turn(p1, Phase.Wake).banditsRefilled)
    val first = Look(woken.ready).active
    val acting = woken.endingWake(first)
    val destination = acting.ready.game.current.map.inPlay
      .find(_ != Look(acting.ready).pawn(first)).get
    val travelled = acting
      .parkedAfter(ParkedServiceFixture.startTravel(first))
      .after(ParkedServiceFixture.pickTravelDestination(first, destination))
    val seats = woken.ready.game.current.players.size
    (1 to seats).foldLeft(travelled) { (situation, turn) =>
      val player = Look(situation.ready).active
      val awake = if turn == 1 then situation
        else situation.endingWake(player)
      // Begin Rest runs on into Finish Rest when Rest asks nothing.
      val resting = awake.after(MenuPlay.start(player, PhaseTransitionRef.BeginRest))
      if Look(resting.ready).active == player then
        resting.finishingRest(player)
      else resting
    }
    Script("round", service, first)

  /** The service suite's Oathkeeper tie: a table with two tied leaders, the
    * active player's Travel, and the holder's choice of the next
    * Oathkeeper, answered by default. ParkedServiceFixture.oathkeeperTiePark
    * states where each pawn stands. */
  def oathkeeperTie(using munit.Location): Script =
    val ParkedServiceFixture.OathkeeperTiePark(game, mover, _, _) =
      ParkedServiceFixture.oathkeeperTiePark("oathkeeper")
    game.situation.after()
    Script("oathkeeper", game.service, mover)

  /** One favor lies on p2's site, and p2 wakes there and takes it. That is
    * Wake's only option, so Wake ends with the take. */
  def takeWealth(using munit.Location): Script =
    val (service, waking) = atTable("take-wealth", tableWithOnePawnPerSite
      .turn(p2, Phase.Wake).pawn(p2, BrokenPeaksSite)
      .siteTokens(BrokenPeaksSite, favor = 1)
      .banditsRefilled)
    start(waking, ActionRef.TakeWealth, DecisionOptionRef.Button("favor"))
    Script("take-wealth", service, p2)
