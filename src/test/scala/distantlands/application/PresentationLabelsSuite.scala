package distantlands.application

import distantlands.gameplay.powers.economy.HallowedSpringCard
import distantlands.gameplay.cards.DeepWoodsSite
import distantlands.gameplay.powers.campaign.WrestlersCard
import distantlands.gameplay.powers.search.ErrandBoyCard
import distantlands.gameplay.powers.whenplayed.{ThreateningRoarCard,
  WizardsConclaveCard}
import distantlands.gameplay.setup.FirstGameSetupFixture.catalog
import distantlands.model._
import distantlands.testkit.Table
import distantlands.testkit.Table.{p1, p2, p3}

class PresentationLabelsSuite extends munit.FunSuite:
  private val presentation = new GamePresentationProjector(catalog)

  /** The ruined Hiding Place and Wrestlers lie at Deep Woods, p1 holds
    * Wizard's Conclave facedown, and Threatening Roar tops the world deck. */
  private lazy val board: ReadyGame = Table.start
    .edifice(HallowedSpringCard, EdificeSide.Ruined, DeepWoodsSite)
    .denizen(WrestlersCard, DeepWoodsSite)
    .adviser(p1, WizardsConclaveCard, facedown = true)
    .worldDeckTop(ThreateningRoarCard).ready

  test("a player's label is the one the Players strip shows"):
    assertEquals(presentation.playerLabel(p1),
      presentation.readyPlayers(board).head.displayName)

  test("a player's label is their ID as typed"):
    Vector("roman", "red-team", "Pink", "a.b:c").foreach(id =>
      assertEquals(presentation.playerLabel(PlayerId(id)), id))

  test("a player option in a decision is labelled with the ID as typed"):
    val rows = new DecisionOptionRows(catalog, presentation)
    assertEquals(rows.project(board, None, None,
      DecisionOption.Player(DecisionOptionRef.Player(p1))).map(_.label),
      Some(p1.value))

  test("a site denizen is identified to everyone; a facedown adviser to its owner only"):
    val denizen = WrestlersCard.id
    Vector(None, Some(p1)).foreach { viewer =>
      assert(presentation.identifiesAt(board, viewer, denizen)) }
    val adviser = WizardsConclaveCard.id
    assert(presentation.identifiesAt(board, Some(p1), adviser))
    assert(!presentation.identifiesAt(board, Some(p2), adviser))
    assert(!presentation.identifiesAt(board, None, adviser))

  test("a card in the world deck is identified to nobody"):
    val top = ThreateningRoarCard.id
    Vector(p1, p2, p3).foreach { viewer =>
      assert(!presentation.identifiesAt(board, Some(viewer), top)) }

  test("a card's label is its printed name"):
    assertEquals(presentation.cardLabel(board,
      ErrandBoyCard.id), "Errand Boy")
