package oathdigital.gameplay.powers.poweruse

import oathdigital.gameplay.BanditCrownCard
import oathdigital.gameplay.cards.{AncientCitySite, DeepWoodsSite}
import oathdigital.gameplay.powerresolver.PowerUse
import oathdigital.gameplay.powers.action.{RovingTerror, RovingTerrorCard}
import oathdigital.gameplay.powers.travel.ForbiddenForestCard
import oathdigital.gameplay.setup.FirstGameSetupFixture.catalog
import oathdigital.model._
import oathdigital.testkit.{MenuPlay, Table}
import oathdigital.testkit.Table.{p1, p2, p3}

/** Bandit Forest (E29, ruined): "Imperials cannot place [favor]/[secret] on
  * cards ruled by bandits or use their non-persistent powers." (catalog
  * batch 10, slice 1). It binds Imperial players only, and only the Bandit
  * Crown's holder may waive it (catalog batch 11, ruling 6). */
class BanditForestSuite extends munit.FunSuite:
  private val forest = NonPersistentPowerRestriction.banditForest
  private val home = AncientCitySite.id
  private val woods = DeepWoodsSite.id
  private val terror = RovingTerrorCard.id
  private val source = PowerSourceRef.Card(ForbiddenForestCard.id)

  /** p2 is the Chancellor and p1 a Citizen; p3 is an Exile. Bandits rule
    * Ancient City, where Roving Terror stands, and Bandit Forest lies
    * ruined at Deep Woods. It is `actor`'s Act. */
  private def terrorUnderBanditsWithForestRuined(actor: PlayerId): Table =
    Table.start.imperial(p2, p1).turn(actor, Phase.Act)
      .bandits(home, 2).denizen(terror, at = home)
      .edifice(ForbiddenForestCard, EdificeSide.Ruined, at = woods)

  private def nonPersistentUseOf(card: DenizenId, by: PlayerId): PowerUse =
    PowerUse(by, PowerId("test.power"), Some(card), persistent = false,
      costsSecret = false)

  test("it refuses an Imperial player a non-persistent power of a card " +
      "bandits rule, and nobody may waive it without a Crown"):
    val ready = terrorUnderBanditsWithForestRuined(p1).ready
    val refusal = forest.refusal(catalog, ready, nonPersistentUseOf(terror,
      by = p1))
    assertEquals(refusal.map(_.note), Some(forest.usePlain(source)))
    assertEquals(refusal.flatMap(_.reason.waiver).flatMap(_.granter), None)

  test("an Exile may use the card"):
    val ready = terrorUnderBanditsWithForestRuined(p3).ready
    assertEquals(forest.refusal(catalog, ready, nonPersistentUseOf(terror,
      by = p3)), None)

  test("it refuses an Imperial player's payment onto a card bandits rule, " +
      "and lets an Exile's through"):
    val ban = forest.operationRestrictions(catalog).head
    val imperialPays = PayCost(p1, Location.OnCard(terror), Cost(favor = 1))
    assert(ban.reason(terrorUnderBanditsWithForestRuined(p1).ready,
      imperialPays).exists(_.waiver.exists(_.granter.isEmpty)))
    val exilePays = PayCost(p3, Location.OnCard(terror), Cost(favor = 1))
    assertEquals(ban.reason(terrorUnderBanditsWithForestRuined(p3).ready,
      exilePays), None)

  test("a card at a site bandits do not rule is free"):
    val ready = terrorUnderBanditsWithForestRuined(p1)
      .update(_.updateCurrent(current => current.copy(map =
        current.map.copy(sites = current.map.sites.updatedWith(home)(
          _.map(_.copy(forces = SiteForces.Empty)))))))
      .ready
    assertEquals(forest.refusal(catalog, ready, nonPersistentUseOf(terror,
      by = p1)), None)

  test("with the Forest's intact face on the map, nothing is refused"):
    val ready = Table.start.imperial(p2, p1).turn(p1, Phase.Act)
      .bandits(home, 2).denizen(terror, at = home)
      .edifice(ForbiddenForestCard, EdificeSide.Intact, at = woods).ready
    assertEquals(forest.refusal(catalog, ready, nonPersistentUseOf(terror,
      by = p1)), None)

  test("it binds the Chancellor too"):
    val ready = terrorUnderBanditsWithForestRuined(p2).ready
    assert(forest.refusal(catalog, ready, nonPersistentUseOf(terror,
      by = p2)).nonEmpty)

  test("it refuses an Imperial player's persistent use whose cost places " +
      "on a card bandits rule"):
    val ready = terrorUnderBanditsWithForestRuined(p1).ready
    val placing = PowerUse(p1, PowerId("test.power"), Some(terror),
      persistent = true, costsSecret = false, places = true)
    assertEquals(forest.refusal(catalog, ready, placing).map(_.note),
      Some(forest.placingPlain(source)))

  test("the phase menu shows a card bandits rule with its reason"):
    val option = MenuPlay.menuOptionFor(catalog,
      terrorUnderBanditsWithForestRuined(p1).ready,
      ActionRef.UsePower(RovingTerror.id),
      Vector(DecisionOptionRef.Denizen(terror)))
    assertEquals(option, DecisionOption.Unavailable(
      DecisionOption.Denizen(DecisionOptionRef.Denizen(terror)),
      OptionReason.Noted(forest.id, forest.usePlain(source))))

  test("the Crown's holder may waive it for an Imperial player"):
    val ready = terrorUnderBanditsWithForestRuined(p1)
      .relic(p3, BanditCrownCard).ready
    val option = MenuPlay.menuOptionFor(catalog, ready,
      ActionRef.UsePower(RovingTerror.id),
      Vector(DecisionOptionRef.Denizen(terror)))
    option match
      case DecisionOption.Unavailable(_, OptionReason.Askable(power, note,
          asks, _)) =>
        assertEquals(power, forest.id)
        assertEquals(note, forest.useAsked(source))
        assertEquals(asks, Vector(ConsentAsk(p3, Vector(forest.id))))
      case other => fail(s"not askable: $other")

  test("an Imperial holder is still bound, and cannot let themselves off"):
    val ready = terrorUnderBanditsWithForestRuined(p2)
      .relic(p2, BanditCrownCard).ready
    assertEquals(MenuPlay.menuOptionFor(catalog, ready,
      ActionRef.UsePower(RovingTerror.id),
      Vector(DecisionOptionRef.Denizen(terror))),
      DecisionOption.Unavailable(
        DecisionOption.Denizen(DecisionOptionRef.Denizen(terror)),
        OptionReason.Noted(forest.id, forest.usePlain(source))))
