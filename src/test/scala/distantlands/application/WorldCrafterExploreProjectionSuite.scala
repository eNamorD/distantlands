package distantlands.application

import distantlands.gameplay.OathRules
import distantlands.gameplay.WalkerDiceFixture.scripted
import distantlands.gameplay.cards.BuriedGiantSite
import distantlands.gameplay.powers.WalkerPowerCatalog
import distantlands.gameplay.setup.FirstGameSetupFixture.catalog
import distantlands.gameplay.setup.legacies.WorldCrafterCard
import distantlands.model._
import distantlands.model.OathState.Ready
import distantlands.testkit.Table
import distantlands.testkit.Table.{p1, p2}
import distantlands.testkit.Unwrap.accepted

/** World Crafter's holder answers another player's Explore end off turn
  * (spec ruling 11): the holder is shown the question, the explorer and
  * the public who it waits on. */
class WorldCrafterExploreProjectionSuite extends munit.FunSuite:
  private val projector = new GameProjector(catalog)

  test("the holder is shown the end question, the explorer waits on them"):
    import DefenseDieFace.TwoShields
    val rules = new OathRules(catalog,
      walkerPowerCatalog = WalkerPowerCatalog.default(catalog),
      walkerDice = scripted(Vector(TwoShields, TwoShields)))
    val ready = Table.start
      .foundation(FoundationNumber.I, FoundationFace.Altered)
      .mapCutIntoAtlas(3).pawn(p1, BuriedGiantSite).banditsRefilled
      .legacy(p2, WorldCrafterCard).ready
    val at = accepted(rules.startWalker(Ready(ready), ActionRef.Explore, p1))
    val loaded = LoadedGame(at.state, 1L)
    assert(projector.project("explore", loaded, p2).walkerDecision
      .exists(_.query.exists(_.offeredOptions.size == 2)))
    Vector(projector.project("explore", loaded, p1),
        projector.projectPublic("explore", loaded)).foreach(view =>
      assertEquals(view.walkerWaiting.map(_.playerIds), Some(Vector("p2"))))
