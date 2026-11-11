package distantlands.application

import distantlands.catalog.FoundationCards
import distantlands.gameplay.setup.FirstGameSetupFixture.catalog
import distantlands.model.{FoundationFace, FoundationNumber, OathState}
import distantlands.protocol.projection.FoundationProjection
import distantlands.testkit.Table
import distantlands.testkit.Table.{p1, p2}

/** The six foundation setup cards every seat sees (Foundations design,
  * slice 1, "Projection and UI"). */
class FoundationProjectionSuite extends munit.FunSuite:
  private val projector = GameProjector(catalog)

  test("every seat and the public view see the six cards, I to VI, each by " +
      "its face in use"):
    val state = Table.start.state
    val expected = Vector("Imperial Maps", "Powerful Tribes",
      "Quiet Ambitions", "Ties of Fellowship", "Mob's Favor",
      "Wandering Flame")
    val views = Vector(p1, p2).map(seat =>
      projector.project("g", LoadedGame(state, 1L), seat)) :+
      projector.projectPublic("g", LoadedGame(state, 1L))
    views.foreach { view =>
      assertEquals(view.foundations.map(_.number), Vector(1, 2, 3, 4, 5, 6))
      assertEquals(view.foundations.map(_.face), Vector.fill(6)("normal"))
      assertEquals(view.foundations.map(_.name), expected)
    }

  test("an altered card shows its altered face's name and text"):
    val state = Table.start.foundation(FoundationNumber.III,
      FoundationFace.Altered).state
    val third = projector.project("g", LoadedGame(state, 1L), p1)
      .foundations(2)
    assertEquals(third, FoundationProjection(3, "altered", "Public Ambitions",
      FoundationCards.face(FoundationNumber.III, FoundationFace.Altered)
        .text))

  test("a game not yet started shows no foundations"):
    assertEquals(projector.project("g", LoadedGame(OathState.NoGame, 0L),
      p1).foundations, Vector.empty)
