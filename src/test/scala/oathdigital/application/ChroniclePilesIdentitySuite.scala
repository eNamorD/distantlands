package oathdigital.application

import oathdigital.gameplay.setup.FirstGameSetupFixture.catalog
import oathdigital.model._
import oathdigital.testkit.Table
import oathdigital.testkit.Table.{p1, p2}

/** Who may identify a card in the gathering pile or among a lineage's
  * starting advisers (Chronicle World and Beacon plan, Task 1). */
class ChroniclePilesIdentitySuite extends munit.FunSuite:
  private val presentation = new GamePresentationProjector(catalog)

  /** The quiet table with the top nine world cards dealt into the
    * gathering pile, in deck order. */
  private val nineGathered: Table = Table.start.update(ready =>
    val nine = ready.game.current.commonCards.worldDeck.take(9)
    ready.updateCurrent(current => current.copy(
      commonCards = current.commonCards.copy(
        worldDeck = current.commonCards.worldDeck.drop(9)),
      chronicleGathering = nine)))

  test("every player identifies the two faceup stacks, nobody the third"):
    val ready = nineGathered.ready
    val gathered = ready.game.current.chronicleGathering
    val viewers = Vector(Some(p1), Some(p2), None)
    assert(viewers.forall(viewer => gathered.take(6).forall(card =>
      presentation.identifiesAt(ready, viewer, card))))
    assert(viewers.forall(viewer => gathered.drop(6).forall(card =>
      !presentation.identifiesAt(ready, viewer, card))))

  test("once a stack leaves, the facedown stack is still the hidden last three"):
    // The first stack goes back to the world deck, so every card is still
    // somewhere and the card index stays whole.
    val ready = nineGathered.update(_.updateCurrent(current => current.copy(
      commonCards = current.commonCards.copy(worldDeck =
        current.commonCards.worldDeck ++ current.chronicleGathering.take(3)),
      chronicleGathering = current.chronicleGathering.drop(3)))).ready
    val six = ready.game.current.chronicleGathering
    assert(six.take(3).forall(card =>
      presentation.identifiesAt(ready, Some(p1), card)))
    assert(six.drop(3).forall(card =>
      !presentation.identifiesAt(ready, Some(p1), card)))

  test("only the lineage's player identifies a starting adviser"):
    val table = Table.start.update(state =>
      val lineage = state.game.current.player(p2).get.lineage
      val card = state.game.current.commonCards.worldDeck.collectFirst {
        case id: DenizenId => id }.get
      state.updateCurrent(current => current.copy(commonCards =
        current.commonCards.copy(worldDeck =
          current.commonCards.worldDeck.filterNot(_ == card))))
        .updateCampaign(campaign => campaign.copy(lineages =
          campaign.lineages.updatedWith(lineage)(_.map(_.copy(
            startingAdvisers = Vector(DenizenState(card,
              Orientation.FaceDown, Tokens.empty))))))))
    val ready = table.ready
    val lineage = ready.game.current.player(p2).get.lineage
    val card = ready.game.campaign.lineages(lineage).startingAdvisers.head.id
    assert(presentation.identifiesAt(ready, Some(p2), card))
    assert(!presentation.identifiesAt(ready, Some(p1), card))
