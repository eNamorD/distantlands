package distantlands.application

import distantlands.gameplay.powers.legacies.GoldenKeyCard
import distantlands.gameplay.setup.FirstGameSetupFixture.catalog
import distantlands.model._
import distantlands.protocol.projection.GameProjectionCodec
import distantlands.testkit.Table
import distantlands.testkit.Table.{p1, p2}

/** The Reliquary in the shared bank, facedown, and Golden Key's peek
  * (Legacy in-play powers rulings 9 and 10; implementation rulings R1 and
  * R5). */
class ReliquaryViewSuite extends munit.FunSuite:
  private val projector = new GameProjector(catalog)
  private val presentation = new GamePresentationProjector(catalog)
  private def loaded(ready: ReadyGame) = LoadedGame(OathState.Ready(ready), 1L)
  private def view(ready: ReadyGame, viewer: Option[PlayerId]) =
    viewer.fold(projector.projectPublic("g", loaded(ready)))(player =>
      projector.project("g", loaded(ready), player))

  /** p1, the Chancellor, holds Golden Key active in Act, and the Reliquary
    * holds two relics. */
  private val keyed = Table.start.imperial(p1).turn(p1, Phase.Act)
    .legacy(p1, GoldenKeyCard).reliquaryFromDeck(2).ready
  private val relics = keyed.game.campaign.reliquaryInPlay

  test("every viewer sees a non-empty Reliquary in the shared bank, " +
      "facedown, and only Golden Key's Imperial holder sees its faces"):
    assertEquals(view(keyed, Some(p1)).reliquary.map(card =>
      (card.cardId, card.hidden)), relics.map(id => (id.value, false)))
    Vector(Some(p2), None).foreach { viewer =>
      val shown = view(keyed, viewer)
      assertEquals(shown.reliquary.map(_.hidden), Vector(true, true))
      val wire = GameProjectionCodec.encode(shown)
      relics.foreach(id => assert(!wire.contains(s"\"${id.value}\""),
        s"$viewer sees ${id.value}"))
    }

  test("a Citizen holder peeks too; the Chancellor beside them does not " +
      "outside Setup (ruling 2)"):
    val citizen = Table.start.imperial(p2, p1).turn(p1, Phase.Act)
      .legacy(p1, GoldenKeyCard).reliquaryFromDeck(2).ready
    assertEquals(view(citizen, Some(p1)).reliquary.map(_.hidden),
      Vector(false, false))
    assertEquals(view(citizen, Some(p2)).reliquary.map(_.hidden),
      Vector(true, true))

  test("the peek is gone once its holder becomes an Exile (ruling 2)"):
    val exiled = keyed.updateCampaign(campaign => campaign.copy(lineages =
      campaign.lineages.view.mapValues(lineage => lineage.copy(
        role = Role.Exile)).toMap))
    assertEquals(view(exiled, Some(p1)).reliquary.map(_.hidden),
      Vector(true, true))

  test("an empty or set-aside Reliquary is not shown (ruling 9; R5)"):
    val empty = Table.start.turn(p1, Phase.Act).ready
    assertEquals(view(empty, None).reliquary, Vector.empty)
    val setAside = Table.from(keyed).reliquarySetAside.ready
    assertEquals(view(setAside, Some(p1)).reliquary, Vector.empty)

  test("Golden Key's holder reads a Reliquary option as its relic"):
    val rows = new DecisionOptionRows(catalog, presentation)
    val option = DecisionOption.Relic(DecisionOptionRef.Relic(relics.head))
    val index = keyed.game.cardIndex.toOption
    assert(rows.project(keyed, Some(p1), index, option)
      .flatMap(_.card).exists(!_.hidden))
    assertEquals(rows.project(keyed, Some(p2), index, option)
      .flatMap(_.card).map(_.cardId), None)
