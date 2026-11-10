package oathdigital.application

import oathdigital.gameplay.powers.campaign.ObsidianCageCard
import oathdigital.gameplay.setup.FirstGameSetupFixture.catalog
import oathdigital.protocol.projection.SiteForcesProjection
import oathdigital.testkit.Table
import oathdigital.testkit.Table.{p1, p2}

/** Warbands standing on a card (Obsidian Cage) reach the table on the
  * card's details, one entry per force, in its owner's colour. */
class CardWarbandsProjectionSuite extends munit.FunSuite:
  private val presentation = new GamePresentationProjector(catalog)

  test("a held relic carries the warbands caged on it, in their owner's " +
      "colour"):
    val ready = Table.start.relic(p1, ObsidianCageCard)
      .warbandsOnCard(ObsidianCageCard, p2, 2).ready
    val color = ready.playerColors(p2)
    val relics = presentation.playerBoards(ready, Some(p1))
      .find(_.playerId == p1.value).get.relics
    assertEquals(relics.map(_.warbands), Vector(Vector(
      SiteForcesProjection.Exile(2, p2.value, color,
        s"${color.key.capitalize} Warbands"))))

  test("a relic with no warband on it carries none"):
    val ready = Table.start.relic(p1, ObsidianCageCard).ready
    val relics = presentation.playerBoards(ready, Some(p1))
      .find(_.playerId == p1.value).get.relics
    assertEquals(relics.map(_.warbands), Vector(Vector.empty))
