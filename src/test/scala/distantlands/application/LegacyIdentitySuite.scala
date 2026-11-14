package distantlands.application

import distantlands.catalog.holding.{ArbiterCard, SeerCard}
import distantlands.gameplay.chronicle.legacies.RivalCard
import distantlands.gameplay.setup.FirstGameSetupFixture.catalog
import distantlands.model.{DecisionOption, DecisionOptionRef, Phase}
import distantlands.testkit.Table
import distantlands.testkit.Table.{p1, p2}

/** Who may identify a legacy (Legacies design, ruling 1): everyone an
  * active or public one, only its owner a dormant one. */
class LegacyIdentitySuite extends munit.FunSuite:
  private val presentation = new GamePresentationProjector(catalog)
  private val ready = Table.start.legacy(p1, SeerCard)
    .legacy(p1, ArbiterCard, active = false)
    .publicLegacies(RivalCard).ready
  private val viewers = Vector(Some(p1), Some(p2), None)

  test("every viewer identifies an active legacy and a public one"):
    assert(viewers.forall(viewer =>
      presentation.identifiesAt(ready, viewer, SeerCard.id) &&
        presentation.identifiesAt(ready, viewer, RivalCard.id)))

  test("only its owner identifies a dormant legacy"):
    assert(presentation.identifiesAt(ready, Some(p1), ArbiterCard.id))
    assert(!presentation.identifiesAt(ready, Some(p2), ArbiterCard.id))
    assert(!presentation.identifiesAt(ready, None, ArbiterCard.id))

  private val rows = new DecisionOptionRows(catalog, presentation)

  test("a dormant legacy option reads as its card to its owner only"):
    val index = ready.game.cardIndex.toOption
    val option = DecisionOption.Legacy(DecisionOptionRef.Legacy(ArbiterCard.id))
    val row = rows.project(ready, Some(p1), index, option)
    assertEquals(row.flatMap(_.card).map(card =>
      (card.cardKind, card.name, card.suit)),
      Some(("legacy", ArbiterCard.name, Some(ArbiterCard.suit.key))))
    assertEquals(rows.project(ready, Some(p2), index, option), None)

  test("a legacy option held by another seated player names its holder, " +
      "and its own holder reads no such line (R6)"):
    val index = ready.game.cardIndex.toOption
    val option = DecisionOption.Legacy(DecisionOptionRef.Legacy(SeerCard.id))
    assertEquals(rows.project(ready, Some(p2), index, option)
      .map(_.details), Some(Vector("Held by p1")))
    assertEquals(rows.project(ready, Some(p1), index, option)
      .map(_.details), Some(Vector.empty))

  test("a legacy's back is a legacy back"):
    assertEquals(presentation.cardKind(ArbiterCard.id), "legacy")

  test("in the Chronicle Phase every viewer identifies a dormant legacy, " +
      "and in play only its owner (R18)"):
    val chronicle = ready.updateCurrent(current => current.copy(turn =
      current.turn.copy(phase = Phase.Chronicle)))
    assert(viewers.forall(presentation.identifiesAt(chronicle, _,
      ArbiterCard.id)))
    assert(!presentation.identifiesAt(ready, Some(p2), ArbiterCard.id))

  test("a legacy's card details carry its goal's key and words (R35)"):
    assertEquals(presentation.cardDetails(SeerCard.id, None, hidden = false)
      .legacyGoal, Some(distantlands.protocol.projection.LegacyGoalProjection(
        "darkest-secret", "Hold the Darkest Secret.")))

  private val projector = new GameProjector(catalog)
  private def loaded(state: distantlands.model.ReadyGame): LoadedGame =
    LoadedGame(distantlands.model.OathState.Ready(state), 1L)
  private def legaciesOn(view: distantlands.protocol.projection.GameProjection,
      player: distantlands.model.PlayerId)
      : Vector[distantlands.protocol.projection.CardDetailsProjection] =
    view.playerBoards.find(_.playerId == player.value).get.legacies

  test("a board lists its lineage's legacies: the owner sees both faces, " +
      "anyone else the active one and a legacy back"):
    val own = legaciesOn(projector.project("g", loaded(ready), p1), p1)
    assertEquals(own.map(card => (card.cardId, card.orientation, card.hidden)),
      Vector((SeerCard.id.value, Some("face-up"), false),
        (ArbiterCard.id.value, Some("face-down"), false)))
    Vector(projector.project("g", loaded(ready), p2),
        projector.projectPublic("g", loaded(ready))).foreach { view =>
      val theirs = legaciesOn(view, p1)
      assertEquals(theirs.map(card => (card.cardKind, card.hidden)),
        Vector(("legacy", false), ("legacy", true)))
      assert(!distantlands.protocol.projection.GameProjectionCodec
        .encode(view).contains(s"\"${ArbiterCard.id.value}\""))
    }

  test("in the Chronicle Phase every view names a dormant legacy (R18)"):
    val chronicle = ready.updateCurrent(current => current.copy(turn =
      current.turn.copy(phase = Phase.Chronicle)))
    assertEquals(legaciesOn(projector.projectPublic("g", loaded(chronicle)),
      p1).map(_.hidden), Vector(false, false))

  test("every view shows the Public Ambitions row faceup"):
    Vector(projector.project("g", loaded(ready), p2),
        projector.projectPublic("g", loaded(ready))).foreach(view =>
      assertEquals(view.publicLegacies.map(card =>
        (card.cardId, card.orientation)),
        Vector((RivalCard.id.value, Some("face-up")))))

  test("the Chancellor's tie-break names its public legacy as its subject"):
    val decisions = new WalkerDecisionProjector(catalog, presentation)
    val subjects = decisions.subjectCards(ready, Some(p2),
      distantlands.gameplay.chronicle.ResolveLegacies.publicDecisionId(
        RivalCard.id))
    assertEquals(subjects.map(card => (card.cardId, card.name, card.hidden)),
      Vector((RivalCard.id.value, RivalCard.name, false)))

  test("another seat's JSON names neither a dormant legacy's name nor its " +
      "goal"):
    // Iron Hand's goal is the only one of its kind on this table.
    val dormant = Table.start.legacy(p1, SeerCard)
      .legacy(p1, distantlands.gameplay.chronicle.legacies.IronHandCard, active = false)
      .ready
    Vector(projector.project("g", loaded(dormant), p2),
        projector.projectPublic("g", loaded(dormant))).foreach { view =>
      val json = distantlands.protocol.projection.GameProjectionCodec
        .encode(view)
      Vector(distantlands.gameplay.chronicle.legacies.IronHandCard.id.value,
        distantlands.gameplay.chronicle.legacies.IronHandCard.name,
        distantlands.catalog.LegacyGoal.MostSites.printed).foreach(secret =>
        assert(!json.contains(secret), secret))
    }
