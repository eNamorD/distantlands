package distantlands.gameplay.chronicle

import distantlands.gameplay.OathRules
import distantlands.gameplay.cards.{AncientCitySite, BrokenPeaksSite}
import distantlands.gameplay.chronicle.legacies.IronHandCard
import distantlands.gameplay.powers.banner.TheNeedleCard
import distantlands.gameplay.powers.WalkerPowerCatalog
import distantlands.gameplay.setup.FirstGameSetupFixture.catalog
import distantlands.gameplay.setup.legacies.SeerCard
import distantlands.model._
import distantlands.testkit.{RecordedOps, Table}
import distantlands.testkit.Table.{p1, p2, p3}

/** Stone Baths and Ruined Baths (E04), CHRONICLE (SUN): a player whose
  * pawn is at the site keeps their dormant legacies at Sun step 2 (Stone
  * Baths), or, as an Exile, their active ones at step 3 (Ruined Baths)
  * (catalog batch 13, rulings 14 to 16). Every game here is p1's win, in
  * the Chronicle Phase with Start Chronicle answered, the Baths at Broken
  * Peaks. Seer's goal is the Darkest Secret, which nobody holds unless a
  * test gives it; The Needle's is met only by a player who rules a site,
  * and p1 rules none. */
class BathsSuite extends munit.FunSuite:
  private val rules = new OathRules(catalog,
    walkerPowerCatalog = WalkerPowerCatalog.default(catalog))
  private def won: Table = ChronicleWalk.chronicleWonByP1

  private def sun(table: Table)(using munit.Location): ChronicleWalk.Run =
    ChronicleWalk.through(rules, table, TriggeredProcedureRef.ChronicleSun)

  /** `side` of the Baths at Broken Peaks, `bathing`'s pawn there and every
    * other pawn at Ancient City. */
  private def bathsWith(table: Table, side: EdificeSide,
      bathing: PlayerId*): Table =
    Vector(p1, p2, p3).foldLeft(table.edifice(StoneBathsCard.id, side,
      at = BrokenPeaksSite))((t, player) => t.pawn(player,
        if bathing.contains(player) then BrokenPeaksSite else AncientCitySite))

  private def legaciesOf(ready: ReadyGame, player: PlayerId)
      : Vector[LegacyState] =
    ready.game.campaign.lineages(ready.game.current.player(player).get.lineage)
      .legacies

  test("at Stone Baths an unmet dormant legacy stays dormant, and a met " +
      "one still activates"):
    val run = sun(bathsWith(won.darkestSecret(Some(p2), 1)
      .legacy(p2, SeerCard, active = false)
      .legacy(p2, IronHandCard, active = false), EdificeSide.Intact, p2))
    assertEquals(legaciesOf(run.ready, p2), Vector(
      LegacyState(SeerCard.id, active = true),
      LegacyState(IronHandCard.id, active = false)))
    assert(!run.ready.game.current.commonCards.legacyDeck
      .contains(IronHandCard.id))
    assertEquals(run.ready.game.current.keptDormant, Set(IronHandCard.id))

  test("a player away from Stone Baths still discards an unmet dormant " +
      "legacy, and nothing is recorded"):
    val run = sun(bathsWith(won.legacy(p3, SeerCard, active = false),
      EdificeSide.Intact, p2))
    assertEquals(legaciesOf(run.ready, p3), Vector.empty)
    assert(run.ready.game.current.commonCards.legacyDeck.contains(SeerCard.id))
    assert(!RecordedOps.all(run.events).exists(
      _.isInstanceOf[LegaciesKeptDormant]))

  test("a pawn off the map is at no site (ruling 16)"):
    val offMap = bathsWith(won.legacy(p2, SeerCard, active = false),
      EdificeSide.Intact, p2).update(_.updateCurrent(current => current.copy(
        players = current.players.map(player =>
          if player.player == p2 then player.copy(pawnSite = None)
          else player))))
    assertEquals(legaciesOf(sun(offMap).ready, p2), Vector.empty)

  test("an Exile winner at Stone Baths reactivates only the newly dormant " +
      "legacy at Crown Chancellor"):
    // The Needle goes dormant in step 3; Seer was kept in step 2.
    val run = sun(bathsWith(won.legacy(p1, TheNeedleCard)
      .legacy(p1, SeerCard, active = false), EdificeSide.Intact, p1))
    assertEquals(legaciesOf(run.ready, p1), Vector(
      LegacyState(TheNeedleCard.id, active = true),
      LegacyState(SeerCard.id, active = false)))

  test("at Ruined Baths an Exile keeps an unmet active legacy active, and " +
      "an Exile elsewhere does not"):
    val run = sun(bathsWith(won.legacy(p2, IronHandCard)
      .legacy(p3, SeerCard), EdificeSide.Ruined, p2))
    assertEquals(legaciesOf(run.ready, p2),
      Vector(LegacyState(IronHandCard.id, active = true)))
    assertEquals(legaciesOf(run.ready, p3),
      Vector(LegacyState(SeerCard.id, active = false)))

  test("Ruined Baths does not keep a dormant legacy at step 2"):
    val run = sun(bathsWith(won.legacy(p2, SeerCard, active = false),
      EdificeSide.Ruined, p2))
    assertEquals(legaciesOf(run.ready, p2), Vector.empty)
