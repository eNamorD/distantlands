package distantlands.gameplay.powers

import distantlands.gameplay.VowOfKinshipCard
import distantlands.gameplay.actions.BannerRules
import distantlands.gameplay.actions.challenge.PlaceBannerResourceProcedure
import distantlands.gameplay.operations.Costs
import distantlands.gameplay.powers.economy.{VowOfPoverty, VowOfPovertyCard}
import distantlands.gameplay.powers.travel.TollRoads
import distantlands.gameplay.setup.FirstGameSetupFixture.catalog
import distantlands.model._
import distantlands.testkit.Table
import distantlands.testkit.Table.{p1, p2}

/** The checks of a spend read a Vow of Kinship holder's Nomad bank with
  * their board (catalog batch 11, rulings 9 and 26); Vow of Poverty's
  * check reads the board only (ruling 27). */
class SpendableFavorChecksSuite extends munit.FunSuite:
  /** p1 holds the Vow with an empty board and 3 favor in the Nomad bank. */
  private val vowHolderWithBankFavor: Table =
    Table.start.adviser(p1, VowOfKinshipCard).favor(p1, 0)
      .bankFavor(Suit.Nomad, 3)

  test("a cost and a toll the holder can pay only from the bank are " +
      "affordable"):
    val ready = vowHolderWithBankFavor.ready
    assert(Costs.affordable(ready, p1, Location.SharedBank,
      Cost(favorBurnt = 3)))
    assert(!Costs.affordable(ready, p1, Location.SharedBank,
      Cost(favorBurnt = 4)))
    assert(TollRoads.canPay(ready, p1))
    assert(RulerToll.payOption(ready, p1, SiteRuler.Player(p2))
      .isInstanceOf[DecisionOption.Button])

  test("the People's Favor counts the bank, for the challenge and for " +
      "the banner's own placement"):
    val ready = vowHolderWithBankFavor.ready
    val held = ready.game.current.player(p1).get
    assertEquals(BannerRules.playerResources(ready, held,
      Banner.PeoplesFavor), 3)
    val holding = vowHolderWithBankFavor.peoplesFavor(Some(p1), favor = 1)
      .ready
    assert(PlaceBannerResourceProcedure.heldBanners(holding, p1)
      .contains(Banner.PeoplesFavor))

  test("Vow of Poverty's REST reads the board only (ruling 27)"):
    val ready = vowHolderWithBankFavor.adviser(p1, VowOfPovertyCard)
      .bankFavor(Suit.Order, 2).turn(p1, Phase.Rest).ready
    assert(VowOfPoverty.usable(catalog, ready, p1,
      DecisionOptionRef.Denizen(VowOfPovertyCard.id)))
