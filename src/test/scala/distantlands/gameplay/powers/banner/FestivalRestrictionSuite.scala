package distantlands.gameplay.powers.banner

import distantlands.gameplay.cards.{AncientCitySite, BrokenPeaksSite}
import distantlands.gameplay.operations.PowerUseRules
import distantlands.gameplay.powerresolver.PowerUse
import distantlands.gameplay.powers.ConsentPowers
import distantlands.gameplay.powers.action.{DowsingSticksCard, RovingTerror,
  RovingTerrorCard}
import distantlands.gameplay.powers.campaign.WrestlersCard
import distantlands.gameplay.powers.setup.HallOfDebateCard
import distantlands.gameplay.setup.FirstGameSetupFixture.catalog
import distantlands.model._
import distantlands.testkit.{MenuPlay, PhasePowerHarness, Table}
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
