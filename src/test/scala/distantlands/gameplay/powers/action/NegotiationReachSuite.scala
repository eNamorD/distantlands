package distantlands.gameplay.powers.action

import distantlands.gameplay.actions.negotiation.{NegotiationReach,
  TheTribunalCard, TravelingNegotiatorCard}
import distantlands.gameplay.cards.{AncientCitySite, BrokenPeaksSite,
  DeepWoodsSite, DunesSite}
import distantlands.gameplay.powerresolver.PowerUseRestriction
import distantlands.gameplay.powers.WalkerPowerCatalog
import distantlands.gameplay.powers.poweruse.CityWallCard
import distantlands.gameplay.powers.travel.ForbiddenForestCard
import distantlands.gameplay.setup.FirstGameSetupFixture.catalog
import distantlands.model._
import distantlands.testkit.Table
import distantlands.testkit.Table.{p1, p2, p3}

/** Traveling Negotiator (242), The Tribunal (E19 intact) and Bandit
  * Tribunal (E19 ruined): who each lets a Negotiation reach beyond the
  * actor's site (catalog batch 12, rulings 7 to 9, 24 and 25). */
class NegotiationReachSuite extends munit.FunSuite:
  private val peaks = BrokenPeaksSite.id
  private val dunes = DunesSite.id
  private val city = AncientCitySite.id
  private val woods = DeepWoodsSite.id
  private val blockers: Vector[PowerUseRestriction] =
    WalkerPowerCatalog.useRestrictions

  /** p1 at Broken Peaks, p2 at Dunes, p3 at Ancient City: no two share a
    * site. */
  private def apart: Table = Table.start
    .pawn(p1, at = peaks).pawn(p2, at = dunes).pawn(p3, at = city)

  /** The players other than `actor` whom a power lets `actor` reach, in
    * table order. */
  private def reachedBy(table: Table, actor: PlayerId,
      restrictions: Vector[PowerUseRestriction] = Vector.empty)
      : Vector[PlayerId] =
    val ready = table.ready
    ready.game.current.players.map(_.player).filter(other => other != actor &&
      NegotiationReach.reaches(catalog, ready, actor, other, restrictions))

  test("with no power, nobody reaches a player at another site"):
    Vector(p1, p2, p3).foreach(actor =>
      assertEquals(reachedBy(apart, actor), Vector.empty[PlayerId], actor))

  test("Traveling Negotiator's holder reaches any player, and any player " +
      "reaches its holder"):
    val held = apart.adviser(p1, TravelingNegotiatorCard.id)
    assertEquals(reachedBy(held, p1), Vector(p2, p3))
    assertEquals(reachedBy(held, p2), Vector(p1))
    assertEquals(reachedBy(held, p3), Vector(p1))

  test("a facedown Traveling Negotiator reaches nobody"):
    val hidden = apart.adviser(p1, TravelingNegotiatorCard.id,
      facedown = true)
    assertEquals(reachedBy(hidden, p1), Vector.empty[PlayerId])

  test("The Tribunal reaches from its site, both ways"):
    val atDunes = apart.edifice(TheTribunalCard, EdificeSide.Intact,
      at = dunes)
    assertEquals(reachedBy(atDunes, p2), Vector(p1, p3))
    assertEquals(reachedBy(atDunes, p1), Vector(p2))
    assertEquals(reachedBy(atDunes, p3), Vector(p2))

  test("The Tribunal reaches from the site its user rules"):
    val ruled = apart.edifice(TheTribunalCard, EdificeSide.Intact, at = woods)
      .warbandsAt(woods, p1, 1)
    assertEquals(reachedBy(ruled, p1), Vector(p2, p3))
    assertEquals(NegotiationReach.tribunalUsers(catalog, ruled.ready,
      Vector(p1, p2, p3), Vector.empty), Vector(p1))

  test("the ruined face gives no Tribunal reach"):
    val ruined = apart.edifice(TheTribunalCard, EdificeSide.Ruined, at = dunes)
    assertEquals(reachedBy(ruined, p2), Vector.empty[PlayerId])
    assertEquals(NegotiationReach.tribunalUsers(catalog, ruined.ready,
      Vector(p1, p2, p3), Vector.empty), Vector.empty[PlayerId])

  test("Bandit Tribunal joins players whose pawns stand at bandit-ruled " +
      "sites, while it is on the map"):
    val bandits = apart.bandits(peaks, 1).bandits(dunes, 1)
    val ruined = bandits.edifice(TheTribunalCard, EdificeSide.Ruined,
      at = woods)
    assertEquals(reachedBy(ruined, p1), Vector(p2))
    assertEquals(reachedBy(ruined, p2), Vector(p1))
    assertEquals(reachedBy(ruined, p3), Vector.empty[PlayerId])
    assertEquals(reachedBy(bandits, p1), Vector.empty[PlayerId])
    // Persistent: no blocker touches it.
    assertEquals(reachedBy(ruined, p1, blockers), Vector(p2))

  test("City Wall stops its ruler's enemy at its site from using Traveling " +
      "Negotiator, whoever negotiates"):
    val walled = apart.adviser(p1, TravelingNegotiatorCard.id)
      .denizen(CityWallCard.id, at = peaks).warbandsAt(peaks, p2, 1)
    assertEquals(reachedBy(walled, p1), Vector(p2, p3))
    assertEquals(reachedBy(walled, p1, blockers), Vector.empty[PlayerId])
    assertEquals(reachedBy(walled, p3, blockers), Vector.empty[PlayerId])

  test("Giant Python at The Tribunal's site stops every use of it"):
    val python = apart.edifice(TheTribunalCard, EdificeSide.Intact, at = dunes)
      .denizen(GiantPythonCard.id, at = dunes)
    assertEquals(reachedBy(python, p1), Vector(p2))
    assertEquals(reachedBy(python, p1, blockers), Vector.empty[PlayerId])

  test("Bandit Forest stops an Imperial, not an Exile, from using a " +
      "Tribunal bandits rule"):
    // p2 is the Chancellor and p1 a Citizen. p1 and p3 stand at Ancient
    // City, where bandits rule The Tribunal; Bandit Forest lies at Deep
    // Woods; p2 is at Broken Peaks.
    val forest = Table.start.imperial(p2, p1)
      .pawn(p1, at = city).pawn(p3, at = city).pawn(p2, at = peaks)
      .bandits(city, 2)
      .edifice(TheTribunalCard, EdificeSide.Intact, at = city)
      .edifice(ForbiddenForestCard, EdificeSide.Ruined, at = woods)
    assertEquals(NegotiationReach.tribunalUsers(catalog, forest.ready,
      Vector(p1, p2, p3), blockers), Vector(p3))
    assertEquals(reachedBy(forest, p1, blockers), Vector(p3))
    assertEquals(reachedBy(forest, p3, blockers), Vector(p1, p2))
