package distantlands.gameplay.powers.banner

import distantlands.gameplay.OathRules
import distantlands.gameplay.WalkerDiceFixture.scripted
import distantlands.gameplay.actions.dice.RerollChoice
import distantlands.gameplay.actions.economy.TradeProcedure
import distantlands.gameplay.actions.recover.RecoverProcedure
import distantlands.gameplay.cards.{AncientCitySite, BrokenPeaksSite,
  FairIsleSite}
import distantlands.gameplay.operations.{BoilingLakeCard, PowerUseRules}
import distantlands.gameplay.powerresolver.{PowerUse, PowerUseStep}
import distantlands.gameplay.powers.{ConsentPowers, NoteText,
  WalkerPowerCatalog}
import distantlands.gameplay.powers.action.{DowsingSticksCard, RovingTerror,
  RovingTerrorCard}
import distantlands.gameplay.powers.campaign.WrestlersCard
import distantlands.gameplay.powers.dice.JinxCard
import distantlands.gameplay.powers.setup.HallOfDebateCard
import distantlands.gameplay.powers.travel.{BoilingLakeTravel, TravelFixture,
  WayStation, WayStationCard}
import distantlands.gameplay.setup.FirstGameSetupFixture.catalog
import distantlands.model._
import distantlands.model.OathState.Ready
import distantlands.testkit.{Look, MenuPlay, Outcomes, PhasePowerHarness,
  Table}
import distantlands.testkit.Table.{p1, p2, p3}
import distantlands.testkit.Unwrap.accepted

/** The festival marker's persistent power: "Your enemies cannot place favor
  * or secrets on denizens at the site with the festival marker, and cannot
  * use their powers unless they're persistent powers. They can still peek
  * at and recover relics. You may give enemies consent to ignore the
  * festival" (rulebook, "Festival (Darkest Secret)"; Foundations design,
  * slice 6). */
class FestivalRestrictionSuite extends munit.FunSuite:
  private val festival = DarkestSecretFestival
  private val home = AncientCitySite.id
  private val peaks = BrokenPeaksSite.id
  private val terror = RovingTerrorCard.id
  private val terrorRef = DecisionOptionRef.Denizen(terror)
  private val source = PowerSourceRef.Banner(Banner.DarkestSecret)

  /** p1's Act at Ancient City, where Roving Terror stands under the
    * festival marker; Wrestlers stand at Broken Peaks for the Terror to
    * replace. p2, at Broken Peaks, holds the Darkest Secret on its Festival
    * face. */
  private def terrorAtTheFestivalOfP2: Table =
    Table.start.turn(p1, Phase.Act).pawn(p1, at = home)
      .denizen(terror, at = home).denizen(WrestlersCard, at = peaks)
      .darkestSecret(Some(p2), 1, DarkestSecretFace.Festival).festival(home)

  private def useOf(card: CardId, by: PlayerId, persistent: Boolean = false,
      places: Boolean = false): PowerUse =
    PowerUse(by, PowerId("test.power"), Some(card), persistent = persistent,
      costsSecret = false, places = places)

  private def terrorOption(ready: ReadyGame)(using munit.Location)
      : DecisionOption = MenuPlay.menuOptionFor(catalog, ready,
    ActionRef.UsePower(RovingTerror.id), Vector(terrorRef))

  test("it refuses an enemy a non-persistent power of a denizen at the " +
      "festival, waivable by the holder"):
    val refusal = festival.refusal(catalog, terrorAtTheFestivalOfP2.ready,
      useOf(terror, by = p1))
    assertEquals(refusal.map(_.note), Some(festival.usePlain(source)))
    assertEquals(refusal.flatMap(_.reason.waiver).flatMap(_.granter),
      Some(p2))
    assertEquals(refusal.flatMap(_.reason.waiver).map(_.covers), Some(
      ConsentKey(festival.id, p1, PowerUseRules.useKey, terrorRef)))

  test("it binds every enemy whatever their role, and never the holder"):
    val imperial = terrorAtTheFestivalOfP2.imperial(p2, p1).ready
    assert(festival.refusal(catalog, imperial, useOf(terror, by = p1))
      .nonEmpty)
    assert(festival.refusal(catalog, terrorAtTheFestivalOfP2.ready,
      useOf(terror, by = p3)).nonEmpty)
    assertEquals(festival.refusal(catalog, terrorAtTheFestivalOfP2.ready,
      useOf(terror, by = p2)), None)

  test("an enemy may still use a persistent power at the festival, unless " +
      "its cost places on the card"):
    val ready = terrorAtTheFestivalOfP2.ready
    assertEquals(festival.refusal(catalog, ready,
      useOf(terror, by = p1, persistent = true)), None)
    assertEquals(festival.refusal(catalog, ready, useOf(terror, by = p1,
      persistent = true, places = true)).map(_.note),
      Some(festival.placingPlain(source)))

  test("it guards an edifice at the festival, and no card elsewhere, and " +
      "no relic"):
    val ready = terrorAtTheFestivalOfP2
      .edifice(HallOfDebateCard, EdificeSide.Intact, at = home)
      .relicAt(DowsingSticksCard, at = home).ready
    assert(festival.refusal(catalog, ready,
      useOf(HallOfDebateCard.id, by = p1)).nonEmpty)
    assertEquals(festival.refusal(catalog, ready,
      useOf(WrestlersCard.id, by = p1)), None)
    assertEquals(festival.refusal(catalog, ready,
      useOf(DowsingSticksCard.id, by = p1)), None)

  test("it refuses an enemy's payment onto a denizen at the festival, " +
      "waivable by the holder, and lets the holder's through"):
    val ban = festival.operationRestrictions(catalog).head
    val ready = terrorAtTheFestivalOfP2.ready
    val enemyPays = PayCost(p1, Location.OnCard(terror), Cost(favor = 1))
    val waiver = ban.reason(ready, enemyPays).flatMap(_.waiver)
    assertEquals(waiver.map(_.covers), Some(ConsentKey(festival.id, p1,
      PowerUseRules.useKey, terrorRef)))
    assertEquals(waiver.flatMap(_.granter), Some(p2))
    assertEquals(waiver.map(_.plain), Some(festival.placingPlain(source)))
    val holderTurn = Table.from(ready).turn(p2, Phase.Act).ready
    assertEquals(ban.reason(holderTurn,
      PayCost(p2, Location.OnCard(terror), Cost(favor = 1))), None)

  test("with the marker beside the map, the banner unheld or the Wandering " +
      "Flame up, nothing is refused"):
    val free: Vector[(String, Table)] = Vector(
      "beside the map" -> terrorAtTheFestivalOfP2.update(_.updateCurrent(c =>
        c.copy(banners = c.banners.copy(darkestSecret =
          c.banners.darkestSecret.copy(festival = None))))),
      "unheld" -> terrorAtTheFestivalOfP2
        .darkestSecret(None, 1, DarkestSecretFace.Festival),
      "Wandering Flame" -> terrorAtTheFestivalOfP2
        .darkestSecret(Some(p2), 1, DarkestSecretFace.WanderingFlame))
    free.foreach { case (label, table) =>
      assertEquals(festival.refusal(catalog, table.ready,
        useOf(terror, by = p1)), None, label)
    }

  test("the Act menu shows an enemy's ACTION at the festival disabled, " +
      "askable of the holder"):
    terrorOption(terrorAtTheFestivalOfP2.ready) match
      case DecisionOption.Unavailable(_, OptionReason.Askable(power, note,
          asks, _)) =>
        assertEquals(power, festival.id)
        assertEquals(note, festival.useAsked(source))
        assertEquals(asks, Vector(ConsentAsk(p2, Vector(festival.id))))
      case other => fail(s"not askable: $other")

  test("the holder's grant opens the ACTION for the turn"):
    val ready = terrorAtTheFestivalOfP2.ready
    val granted = ready.copy(consent = ConsentState(answers = Map(
      ConsentKey(festival.id, p1, PowerUseRules.useKey, terrorRef) ->
        ConsentAnswer.Granted.by(p2)), turn = Some(ConsentTurn.of(ready))))
    assert(terrorOption(granted).available)
    assert(PhasePowerHarness.awaits(accepted(PhasePowerHarness.use(granted,
      RovingTerror, terrorRef)), RovingTerror.decisionId))

  test("where nobody may be asked, as in a Campaign, it shows its plain " +
      "reason"):
    val ready = terrorAtTheFestivalOfP2.ready
    val use = useOf(terror, by = p1).copy(consent = false)
    val refusals = PowerUseRules.refusals(catalog, ready, use,
      Vector(festival))
    assert(!PowerUseRules.waivable(use, refusals))
    assertEquals(PowerUseRules.shown(ready, p1, refusals, use.consent),
      OptionReason.Noted(festival.id, festival.usePlain(source)))

  test("the holder's consent grid lists it, named for the banner"):
    val ready = terrorAtTheFestivalOfP2.ready
    val powers = ConsentPowers.default
    assert(powers.ruledBy(ready, p2).contains(festival.id))
    assert(!powers.ruledBy(ready, p1).contains(festival.id))
    assertEquals(powers.source(festival.id), Some(source))

  // ---- Muster and Trade at the festival ----------------------------------

  private val tradeForFavor = TradeProcedure.selection(TradeResource.Favor)

  /** p1 with 1 faceup secret and no favor at Ancient City, where Roving
    * Terror, the only card, stands under p2's festival marker. */
  private def onlyTerrorToTradeWith: Table =
    terrorAtTheFestivalOfP2.secrets(p1, faceUp = 1).favor(p1, 0)

  test("Trade for favor, with every card at the actor's site under the " +
      "festival, starts, and its source card is askable of the holder"):
    val ready = onlyTerrorToTradeWith.ready
    assert(MenuPlay.availableInMenu(catalog, ready, ActionRef.Trade,
      tradeForFavor))
    val game = new OathRules(catalog,
      walkerPowerCatalog = WalkerPowerCatalog.default(catalog))
    val started = accepted(game.startWalker(Ready(ready), ActionRef.Trade, p1,
      startArgs = tradeForFavor))
    assert(PhasePowerHarness.awaits(started, TradeProcedure.decisionId))
    val shown = PhasePowerHarness.queryOf(started, p1).flatMap(
      _.offeredOptions.find(_.id == terror.value))
    assertEquals(shown.map(option => option.state -> option.reason), Some(
      "askable" -> Some("Darkest Secret: Cannot place favor or secrets on " +
        "denizens at the festival without the Darkest Secret holder's " +
        "consent.")))

  test("Muster with no favor at the festival names the favor it needs"):
    assertEquals(MenuPlay.withoutBand(MenuPlay.menuOptionFor(catalog,
      onlyTerrorToTradeWith.ready, ActionRef.Muster)),
      DecisionOption.Unavailable(DecisionOption.Button(
        DecisionOptionRef.Button(ActionRef.Muster.key), "Muster"),
        OptionReason.Written("Needs 1 favor; you have 0")))

  test("once the holder refused this turn, Trade shows the festival's plain " +
      "placing reason"):
    val ready = onlyTerrorToTradeWith.ready
    val refused = ready.copy(consent = ConsentState(answers = Map(
      ConsentKey(festival.id, p1, PowerUseRules.useKey, terrorRef) ->
        ConsentAnswer.Refused.by(p2)), turn = Some(ConsentTurn.of(ready))))
    assertEquals(MenuPlay.withoutBand(MenuPlay.menuOptionFor(catalog, refused,
      ActionRef.Trade, tradeForFavor)) match
      case DecisionOption.Unavailable(_, reason) => Some(reason)
      case _ => None,
      Some(OptionReason.Noted(festival.id, festival.placingPlain(source))))

  // ---- the real powers ----------------------------------------------------

  test("an enemy's Jinx at the festival shows its reroll disabled, askable " +
      "of the holder, since its secret would go onto the card"):
    // p1 recovers at Fair Isle, which p1 rules and where Jinx stands under
    // p2's festival marker; the first roll fails and parks on the reroll.
    val isle = FairIsleSite.id
    val start = Table.start.turn(p1, Phase.Act).pawn(p1, at = isle)
      .relicAt(DowsingSticksCard.id, at = isle).denizen(JinxCard, at = isle)
      .warbandsAt(isle, p1, 1).secrets(p1, faceUp = 2)
      .darkestSecret(Some(p2), 1, DarkestSecretFace.Festival).festival(isle)
      .ready
    val game = new OathRules(catalog,
      walkerPowerCatalog = WalkerPowerCatalog.default(catalog),
      walkerDice = scripted(Vector(DefenseDieFace.Blank, DefenseDieFace.Blank)))
    val rolled = accepted(game.startWalker(Ready(start), ActionRef.Recover,
      p1))
    val firstReroll = RerollChoice.decisionId(RecoverProcedure.recoverPool, 1)
    assert(PhasePowerHarness.awaits(rolled, firstReroll))
    val shown = PhasePowerHarness.queryOf(rolled, p1).flatMap(_.offeredOptions
      .find(_.id == RerollChoice.optionRef(JinxCard.id).key))
    assertEquals(shown.map(option => option.state -> option.reason), Some(
      "askable" -> Some("Darkest Secret: Cannot place favor or secrets on " +
        "denizens at the festival without the Darkest Secret holder's " +
        "consent.")))

  /** The Travel board with the actor, p1, holding 1 favor at the Cradle's
    * plains, and `card` at the coast under the festival marker of p2, who
    * rules the coast and holds the Darkest Secret on its Festival face. */
  private def festivalOfP2AtTheCoastWith(card: Table => Table): ReadyGame =
    import TravelFixture.{board, coast, on}
    on(board(supply = 7))(table => card(table).warbandsAt(coast, p2, 1)
      .favor(p1, 1).boardWarbands(p1, 3)
      .darkestSecret(Some(p2), 1, DarkestSecretFace.Festival).festival(coast))

  test("an enemy may still use Way Station's persistent power at the " +
      "festival, giving its ruler favor and spending no Supply"):
    import TravelFixture.{coast, travel}
    val ready = festivalOfP2AtTheCoastWith(_.denizen(WayStationCard.id,
      at = coast))
    val asked = accepted(travel(ready, coast))
    assertEquals(TravelFixture.parkedDecision(asked.state).decisionId,
      WayStation.decisionId)
    val done = Outcomes.ready(accepted(TravelFixture.rules.resolveWalker(
      asked.state, p1, WayStation.decisionId,
      DecisionAnswer.ChooseOneAnswer(WayStation.favorOption))).state)
    assertEquals(Look(done).pawn(p1), coast)
    assertEquals(Look(done).supply(p1), 7)
    assertEquals(Look(done).favor(p2), Look(ready).favor(p2) + 1)

  test("an enemy's arrival at the festival waits at Boiling Lake's step, " +
      "and a refusal the holder gave this turn leaves the Lake deactivated"):
    import TravelFixture.{coast, travel}
    val lake = BoilingLakeCard.id
    val ready = festivalOfP2AtTheCoastWith(_.edifice(lake, EdificeSide.Intact,
      at = coast))
    val waiting = accepted(travel(ready, coast))
    assertEquals(TravelFixture.parkedDecision(waiting.state).decisionId,
      PowerUseStep.decisionId(BoilingLakeTravel.id))
    val refused = ready.copy(consent = ConsentState(answers = Map(
      ConsentKey(festival.id, p1, PowerUseRules.useKey, PowerUseRules.target(
        PowerUse.automatic(p1, BoilingLakeTravel.id, lake))) ->
        ConsentAnswer.Refused.by(p2)), turn = Some(ConsentTurn.of(ready))))
    val done = accepted(travel(refused, coast))
    assertEquals(Look(Outcomes.ready(done.state)).boardWarbands(p1), 3)
    assertEquals(NoteText.said(festival.id, festival.noteKeys, done.events),
      Vector(NoteText.Said("stays-deactivated",
        s"${lake.value} stays deactivated.", covers = false)))
