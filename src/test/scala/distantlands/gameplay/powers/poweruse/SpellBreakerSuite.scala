package distantlands.gameplay.powers.poweruse

import distantlands.gameplay.cards.DeepWoodsSite
import distantlands.gameplay.powerresolver.PowerUse
import distantlands.gameplay.powers.ConsentPowers
import distantlands.gameplay.powers.action.RovingTerrorCard
import distantlands.gameplay.setup.FirstGameSetupFixture.catalog
import distantlands.model._
import distantlands.testkit.Table
import distantlands.testkit.Table.{p1, p2}

/** Spell Breaker (card 162): "Enemies of Spell Breaker's ruler cannot use
  * powers that cost any [secret] or [secret-burnt]." (catalog batch 8,
  * slice 10). This part tests the restriction alone; the paths that read
  * it are tested in the suites after it. */
class SpellBreakerSuite extends munit.FunSuite:
  private val breaker = SpellBreakerCard.id
  private val woods = DeepWoodsSite.id
  private val terror = RovingTerrorCard.id

  /** p1's Act, with Spell Breaker at Deep Woods, which `ruled` gives its
    * ruler: p2's warband by default. */
  private def breakerAtDeepWoods(
      ruled: Table => Table = _.warbandsAt(woods, p2, 1)): ReadyGame =
    ruled(Table.start.turn(p1, Phase.Act).denizen(breaker, at = woods))
      .ready

  private def secretUse(by: PlayerId): PowerUse = PowerUse(by,
    PowerId("denizen.roving-terror"), Some(terror), persistent = false,
    costsSecret = true)

  test("it refuses an enemy of its ruler a power that costs a secret, " +
      "waivable by the ruler"):
    val refusal = SpellBreaker.refusal(catalog, breakerAtDeepWoods(),
      secretUse(p1))
    assertEquals(refusal.map(_.power), Some(SpellBreaker.id))
    assertEquals(refusal.flatMap(_.reason.waiver).flatMap(_.granter),
      Some(p2))
    assertEquals(refusal.flatMap(_.reason.waiver).map(_.covers),
      Some(ConsentKey(SpellBreaker.id, p1, "power.use",
        DecisionOptionRef.Denizen(terror))))

  test("its ruler, and a power that costs no secret, are not refused"):
    assertEquals(SpellBreaker.refusal(catalog, breakerAtDeepWoods(),
      secretUse(p2)),
      None)
    assertEquals(SpellBreaker.refusal(catalog, breakerAtDeepWoods(),
      secretUse(p1).copy(costsSecret = false)), None)

  test("under bandits every player is refused, and nobody may consent"):
    val refusal = SpellBreaker.refusal(catalog,
      breakerAtDeepWoods(_.bandits(woods, 2)), secretUse(p1))
    assertEquals(refusal.map(_.power), Some(SpellBreaker.id))
    // Its waiver names nobody, so the refusal shows its plain reason.
    assertEquals(refusal.flatMap(_.reason.waiver).map(_.granter), Some(None))

  test("at an unruled site it refuses nobody"):
    assertEquals(SpellBreaker.refusal(catalog, breakerAtDeepWoods(identity),
      secretUse(p1)), None)

  test("the consent grid lists it, granted by its ruler"):
    val powers = ConsentPowers.default
    assertEquals(powers.granter(breakerAtDeepWoods(), SpellBreaker.id),
      Some(p2))
    assertEquals(powers.source(SpellBreaker.id),
      Some(PowerSourceRef.Card(breaker)))
    assertEquals(powers.granter(breakerAtDeepWoods(_.bandits(woods, 2)),
      SpellBreaker.id), None)
