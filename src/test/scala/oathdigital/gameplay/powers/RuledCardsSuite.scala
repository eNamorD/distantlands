package oathdigital.gameplay.powers

import oathdigital.catalog.holding.{MartialCultureCard, VowOfKinshipCard}
import oathdigital.gameplay.CampaignFixture._
import oathdigital.gameplay.powers.action.ConvoysCard
import oathdigital.gameplay.powers.setup.GreatForgeCard
import oathdigital.gameplay.powers.travel.BrokenPeaksSite
import oathdigital.gameplay.powers.wake.AncientCitySite
import oathdigital.gameplay.setup.FirstGameSetupFixture.catalog
import oathdigital.model._

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
