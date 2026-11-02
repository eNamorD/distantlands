package oathdigital.application.gamelog

import oathdigital.gameplay.powers.economy.HallowedSpringCard
import oathdigital.catalog.Denizen
import oathdigital.gameplay.actions.cardplay.CardPlayProcedure.PlayedAs
import oathdigital.application._
import oathdigital.gameplay.ModifierPlay
import oathdigital.gameplay.actions.minor.MinorActionProcedures
import oathdigital.gameplay.actions.recover.RecoverProcedure
import oathdigital.gameplay.cards.{BuriedGiantSite, DeepWoodsSite}
import oathdigital.gameplay.powers.action.{FaeMerchantCard, SecondChanceCard}
import oathdigital.gameplay.powers.campaign.StickyFireCard
import oathdigital.gameplay.powers.economy.{GolemLegions, GolemLegionsCard,
  InitiationRiteCard, VowOfWanderingCard}
import oathdigital.gameplay.powers.recover.{RelicWorship, RelicWorshipCard}
import oathdigital.gameplay.powers.search.ForcedLaborCard
import oathdigital.gameplay.powers.travel.BrokenPeaksSite
import oathdigital.gameplay.powers.wake.AncientCitySite
import oathdigital.gameplay.powers.whenplayed.{ThreateningRoarCard,
  WizardsConclaveCard}
import oathdigital.model._
import oathdigital.model.DecisionAnswer.{ChooseAmountAnswer, ChooseOneAnswer}
import oathdigital.testkit.{Dice, Look, MenuPlay, PlacementPicks, Situation,
  Table}
import oathdigital.testkit.Table.{p1, p2, p3}
import LogScriptSupport.{arrangedForNext, atTable, start,
  tableWithOnePawnPerSite}

/** The scripts that play one major or minor action with no power in the
  * way: Search, a facedown adviser played, Muster, Trade, Recover and the
  * relic reveal, the site peek, warbands moved, the Forge and the banner
  * actions. Reached through `LogScripts`.
  */
object ActionLogScripts:
  /** Search of the world deck, every decision answered by default. */
  def search(using munit.Location): Script =
    // Three denizens on top of the world deck, so the Search offers a choice.
    val (service, act) = atTable("search", tableWithOnePawnPerSite
      .worldDeckTop(ThreateningRoarCard, FaeMerchantCard, SecondChanceCard)
      .banditsRefilled)
    start(act, ActionRef.Search)
    Script("search", service, Look(act.ready).active)

  /** Forced Labor at Broken Peaks, and p2 ruling both Broken Peaks and
    * Ancient City, where p1 stands: p1's Search of the world deck spends 2
    * Supply and, its step answered by paying, gives p2 1 favor. p1 holds 5
    * Supply and the start's 1 favor. */
  def forcedLaborSearch(using munit.Location): Script =
    val (service, act) = atTable("forced-labor", tableWithOnePawnPerSite
      .worldDeckTop(ThreateningRoarCard, FaeMerchantCard, SecondChanceCard)
      .supply(p1, 5).denizen(ForcedLaborCard, BrokenPeaksSite)
      .warbandsAt(BrokenPeaksSite, p2, 1).warbandsAt(AncientCitySite, p2, 1)
      .banditsRefilled)
    start(act, ActionRef.Search)
    Script("forced-labor", service, p1)

  /** p1's facedown adviser played from its slot. */
  def facedownAdviser(using munit.Location): Script =
    facedownAdviser("facedown-adviser", None)

  /** As above, the card played as `placement` names when one is given, else
    * by default (the first option, discard). */
  def facedownAdviser(name: String, placement: Option[PlayedAs])
      (using munit.Location): Script =
    val conclave = WizardsConclaveCard.id
    val (service, acted) = atTable(name, tableWithOnePawnPerSite
      .adviser(p1, conclave, facedown = true).banditsRefilled)
    val act = placement.fold(acted)(way => acted.withAnswers {
      case park if park.decisionId.startsWith(LogWords.PlacePrefix) =>
        ChooseOneAnswer(PlacementPicks.pick(park.decide, way))
    })
    start(act, ActionRef.PlayFacedownAdviser)
    Script(name, service, p1)

  /** Deep Woods with a card to Muster or Trade from: a first game deals no
    * denizen to a site, so on this board the card is a homeland edifice. The
    * actor's pawn must stand there. */
  private def source(table: Table): Table =
    table.edifice(HallowedSpringCard, EdificeSide.Ruined, DeepWoodsSite)

  /** p1 musters at Deep Woods holding `advisers` faceup. */
  private def musterHolding(name: String, advisers: Denizen*)(
      using munit.Location): Script =
    val (service, act) = atTable(name, source(advisers.foldLeft(
      tableWithOnePawnPerSite.pawn(p1, DeepWoodsSite))(_.adviser(p1, _)))
      .banditsRefilled)
    start(act, ActionRef.Muster)
    Script(name, service, Look(act.ready).active)

  /** p1 musters at Deep Woods. */
  def muster(using munit.Location): Script = musterHolding("muster")

  /** p1 musters at Deep Woods holding Initiation Rite, so the card mustered
    * on receives a secret instead of a favor. */
  def initiatedMuster(using munit.Location): Script =
    musterHolding("initiated-muster", InitiationRiteCard)

  /** p1 musters at Deep Woods holding Vow of Wandering. */
  def vowMuster(using munit.Location): Script =
    musterHolding("vow-muster", VowOfWanderingCard)

  /** p1 musters on Golem Legions, their adviser, with no card at their site.
    * The Muster picks Golem Legions, its only modifier, so it starts by
    * itself. */
  def golemMuster(using munit.Location): Script =
    val (service, act) = atTable("golem-muster", tableWithOnePawnPerSite
      .adviser(p1, GolemLegionsCard).banditsRefilled)
    act.withAnswers(ModifierPlay.answers(PowerWindow.MusterModifierSelection,
      Vector(GolemLegions.id)))
      .after(MenuPlay.start(p1, ActionRef.Muster))
    Script("golem-muster", service, p1)

  /** Trade for secrets, on the second player's turn, at the site with the
    * source. It costs two favor and a player starts with one, so a second
    * is arranged into their area. With no adviser matching the source's
    * suit, it gains nothing. */
  def trade(using munit.Location): Script =
    val (service, act) = atTable("trade",
      source(tableWithOnePawnPerSite.pawn(p2, DeepWoodsSite)).banditsRefilled)
    val (waking, actor) = arrangedForNext(act)((_, next) =>
      Vector(Move(Piece.Favor(1),
        PositionedLocation(Location.FavorBank(Suit.all.head)),
        PositionedLocation(Location.PlayArea(next)))))
    start(waking.endingWake(actor), ActionRef.Trade,
      DecisionOptionRef.Button("secret"))
    Script("trade", service, actor)

  private def recovering(name: String, dice: CampaignDicePort)
      (using munit.Location): (GameApplicationService, Situation) =
    // p2 is off Broken Peaks (see `recoverTable`), so the relics stay hidden
    // from the other seat.
    atTable(name, ParkedServiceFixture.recoverTable.banditsRefilled, dice)

  /** Dice that fail every Recover roll: continue once, then stop. */
  def recoverFailed(using munit.Location): Script =
    val (service, act) = recovering("recover-failed",
      Dice.blanks)
    val actor = Look(act.ready).active
    def answer(key: String) = GameCommand.ResolveWalker(actor, TreeDecision(
      RecoverProcedure.choiceDecisionId,
      ChooseOneAnswer(DecisionOptionRef.Button(key))))
    act.parkedAfter(MenuPlay.start(actor, ActionRef.Recover))
      .parkedAfter(answer("continue"))
      .after(answer("stop"))
    Script("recover-failed", service, actor)

  /** Steady dice succeed at once; the relic decision is answered by default. */
  def recoverSucceeded(using munit.Location): Script =
    val (service, act) = recovering("recover-succeeded", Dice.steady)
    start(act, ActionRef.Recover)
    Script("recover-succeeded", service, Look(act.ready).active)

  /** A successful Recover, then the recovered relic revealed with Reveal
    * Owned Relic. The relic choice is answered by default. */
  def revealRelic(using munit.Location): Script =
    val (service, act) = recovering("reveal-relic", Dice.steady)
    val actor = Look(act.ready).active
    start(act, ActionRef.Recover).after(
      MenuPlay.start(actor, ActionRef.RevealOwnedRelic))
    Script("reveal-relic", service, actor)

  /** p1 peeks at Broken Peaks, where Sticky Fire lies. p2 and p3 stand
    * elsewhere, so only p1 learns the relic. */
  def peekRelics(using munit.Location): Script =
    val (service, act) = atTable("peek-relics", Table.start
      .pawn(p1, at = BrokenPeaksSite).pawn(p2, at = AncientCitySite)
      .pawn(p3, at = BuriedGiantSite).relicAt(StickyFireCard, BrokenPeaksSite)
      .banditsRefilled)
    act.after(MenuPlay.start(p1, ActionRef.PeekSiteRelics))
    Script("peek-relics", service, p1)

  /** p1 rules Broken Peaks with 3 warbands and holds 4 more. Two minor
    * actions: 2 warbands to the site, then 1 back to the board. */
  def warbandsMoved(using munit.Location): Script =
    val (service, act) = atTable("warbands-moved", Table.start
      .pawn(p1, at = BrokenPeaksSite).boardWarbands(p1, 4)
      .warbandsAt(BrokenPeaksSite, p1, 3).banditsRefilled)
    def answer(decisionId: String, value: DecisionAnswer) =
      GameCommand.ResolveWalker(p1, TreeDecision(decisionId, value))
    def moved(from: Situation, way: DecisionOptionRef.Button,
        amount: Int): Situation =
      from.parkedAfter(MenuPlay.start(p1, ActionRef.MoveWarbands))
        .parkedAfter(answer(MinorActionProcedures.directionDecisionId,
          ChooseOneAnswer(way)))
        .after(answer(MinorActionProcedures.amountDecisionId,
          ChooseAmountAnswer(amount)))
    moved(moved(act, MinorActionProcedures.toSite, 2),
      MinorActionProcedures.toBoard, 1)
    Script("warbands-moved", service, p1)

  /** p1 recovers at Broken Peaks with Relic Worship, their adviser, and one
    * faceup secret. Relic Worship is the only modifier, so its pick starts
    * the Recover by itself. */
  def recoverWorship(using munit.Location): Script =
    val (service, act) = atTable("recover-worship",
      ParkedServiceFixture.recoverTable.adviser(p1, RelicWorshipCard)
        .secrets(p1, faceUp = 1).banditsRefilled, Dice.steady)
    act.withAnswers(ModifierPlay.answers(
      PowerWindow.RecoverModifierSelection, Vector(RelicWorship.id)))
      .after(MenuPlay.start(p1, ActionRef.Recover))
    Script("recover-worship", service, p1)

  /** The Forge fixture's table, then the Forge itself, which never parks at
    * a single-resource site. */
  def forge(using munit.Location): Script =
    val (service, _) = ForgeWalkerFixture.forgeService()
    val actor = Table.p1
    service.handle("forge", 0L, MenuPlay.starting(actor, ActionRef.Forge)).fold(
      error => munit.Assertions.fail(s"Forge refused: $error"), identity)
    Script("forge", service, actor)

  /** A Challenge for a banner from the bank, then resources placed on it.
    * A Challenge needs strictly more favor than the banner holds, and a
    * player starts with too little to challenge and still have favor to
    * place, so p1 holds three.
    */
  def banners(using munit.Location): Script =
    val (service, act) = atTable("banners",
      tableWithOnePawnPerSite.favor(p1, 3).banditsRefilled)
    start(start(act, ActionRef.Challenge), ActionRef.PlaceBannerResource)
    Script("banners", service, p1)
