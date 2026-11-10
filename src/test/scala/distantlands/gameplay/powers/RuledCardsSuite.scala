package distantlands.gameplay.powers

import distantlands.gameplay.VowOfKinshipCard
import distantlands.gameplay.CampaignFixture._
import distantlands.gameplay.cards.{AncientCitySite, BrokenPeaksSite}
import distantlands.gameplay.powers.action.ConvoysCard
import distantlands.gameplay.powers.campaign.MartialCultureCard
import distantlands.gameplay.powers.setup.GreatForgeCard
import distantlands.gameplay.setup.FirstGameSetupFixture.catalog
import distantlands.model._

/** The cards a ruler rules (catalog batch 3 rulings, "Cards a player
  * rules"). */
class RuledCardsSuite extends munit.FunSuite:
  // Two Nomad denizens, an Order one, and a Nomad edifice (Great Forge, on
  // its ruined face).
  private val convoys = ConvoysCard.id
  private val vowOfKinship = VowOfKinshipCard.id
  private val order = MartialCultureCard.id
  private val nomadEdifice = GreatForgeCard.id

  private def nomadsOf(b: Board, ruler: SiteRuler): Set[CardId] =
    RuledCards.of(catalog, b.ready, ruler, Suit.Nomad).toSet

  test("a player rules their faceup advisers, not their facedown ones"):
    val base = board(warbands = 5, supply = 7)
    val b = on(base)(_.adviser(base.actor, convoys)
      .adviser(base.actor, vowOfKinship, facedown = true))
    assertEquals(nomadsOf(b, SiteRuler.Player(b.actor)), Set[CardId](convoys))

  test("a player rules the faceup denizens and the edifices, on either " +
      "face, at the sites they rule"):
    val base = board(warbands = 5, supply = 7)
    val site = BrokenPeaksSite.id
    // A facedown denizen has no suit, and an order card is not a nomad card.
    val b = on(actorRules(base, site))(_
      .denizen(convoys, at = site)
      .denizen(vowOfKinship, at = site, facedown = true)
      .denizen(order, at = site)
      .edifice(nomadEdifice, EdificeSide.Ruined, at = site))
    assertEquals(nomadsOf(b, SiteRuler.Player(b.actor)),
      Set[CardId](convoys, nomadEdifice))

  test("bandits rule the cards at every site they rule, and nobody else " +
      "does"):
    // Two Bandits rule Ancient City, p1's site, and Broken Peaks; p1 rules
    // nothing.
    val base = board(extras = 1, warbands = 5, supply = 7)
    val b = on(base)(_.denizen(convoys, at = AncientCitySite)
      .denizen(vowOfKinship, at = BrokenPeaksSite))
    assertEquals(nomadsOf(b, SiteRuler.Bandits),
      Set[CardId](convoys, vowOfKinship))
    assertEquals(nomadsOf(b, SiteRuler.Player(b.actor)), Set.empty[CardId])
