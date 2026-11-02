package oathdigital.gameplay.powers.search

import oathdigital.gameplay.{ModifierPlay, SearchPlay}
import oathdigital.gameplay.powerresolver.ModifierPicks
import oathdigital.gameplay.powers.SearchFixture
import oathdigital.gameplay.powers.action.{FaeMerchantCard, HomesteadersCard,
  MessengerCard, ScryerCard, SecondChanceCard}
import oathdigital.gameplay.powers.campaign.{MercenariesCard, WrestlersCard}
import oathdigital.gameplay.powers.economy.DefameCard
import oathdigital.gameplay.powers.wake.AncientCitySite
import oathdigital.gameplay.powers.whenplayed.ThreateningRoarCard
import oathdigital.model._
import oathdigital.model.OathState.Ready
import oathdigital.testkit.{Look, Table}
import oathdigital.testkit.Table.{p1, p2}
import oathdigital.testkit.Unwrap.accepted

/** Painted Trumpet (relic R23): a Search of a discard pile draws the top
  * card of each discard pile instead, and nothing modifies that draw
  * (catalog batch 6, slice 3). */
class PaintedTrumpetSuite extends munit.FunSuite:
  import SearchFixture.rules

  private val modifiers = Vector(PaintedTrumpet.id)
  private val cradleTop = FaeMerchantCard.id
  private val cradleBelow = ThreateningRoarCard.id
  private val provincesTop = MercenariesCard.id
  private val hinterlandTop = ScryerCard.id
  private val deckTop = Vector(SecondChanceCard.id, HomesteadersCard.id,
    MessengerCard.id)

  /** p1 at Ancient City holding Painted Trumpet faceup, with 5 Supply and
    * the world deck topped by three plain cards. The Provinces and
    * Hinterland piles hold two cards each. The Cradle pile holds two
    * unless `emptyCradle`. */
  private def holdingTrumpet(emptyCradle: Boolean = false): Table =
    val table = Table.start.pawn(p1, AncientCitySite)
      .relic(p1, PaintedTrumpetCard).supply(p1, 5).worldDeckTop(deckTop*)
      .discarded(Region.Provinces, WrestlersCard, MercenariesCard)
      .discarded(Region.Hinterland, DefameCard, ScryerCard)
    if emptyCradle then table
    else table.discarded(Region.Cradle, cradleBelow, cradleTop)

  private def searched(table: Table, pile: SearchSource,
      picked: Vector[PowerId]): ReadyGame =
    SearchFixture.after(accepted(SearchPlay.searched(rules,
      Ready(table.ready), p1, pile, picked)))

  private val cradle = SearchSource.RegionalDiscard(Region.Cradle)

  test("a discard pile picked draws the top card of each pile, for 2 " +
      "Supply"):
    val after = searched(holdingTrumpet(), cradle, modifiers)
    assertEquals(after.game.current.temporaryHands(p1),
      Vector(cradleTop, provincesTop, hinterlandTop))
    assertEquals(Look(after).supply(p1), 5 - 2)
    assertEquals(after.game.current.commonCards.discard(Region.Cradle),
      Vector(cradleBelow))

  test("Augury adds no card to its draw"):
    val table = holdingTrumpet().denizen(AuguryCard, at = AncientCitySite)
    val after = searched(table, cradle,
      Vector(PaintedTrumpet.id, Augury.id))
    assertEquals(after.game.current.temporaryHands(p1),
      Vector(cradleTop, provincesTop, hinterlandTop))

  test("Truthful Harp still reveals the cards Painted Trumpet drew"):
    val table = holdingTrumpet().relic(p1, TruthfulHarpCard)
    val after = searched(table, cradle,
      Vector(PaintedTrumpet.id, TruthfulHarp.id))
    val drawn = Vector(cradleTop, provincesTop, hinterlandTop)
    assertEquals(after.game.current.temporaryHands(p1), drawn)
    assertEquals(after.knowledge.advisers.getOrElse(p2, Vector.empty).toSet,
      drawn.toSet[WorldCardId])

  test("an empty pile may be picked while another pile has a card"):
    val table = holdingTrumpet(emptyCradle = true)
    val shown = ShownOptions.sourcesAfterPicking(table.ready, modifiers)
    assertEquals(ShownOptions.optionFor(shown, SearchSource.ref(cradle))
      .map(option => (option.state, option.badge)),
      Some(("available", Some("2 Supply"))))
    assertEquals(searched(table, cradle, modifiers).game.current
      .temporaryHands(p1), Vector(provincesTop, hinterlandTop))

  test("a world Search is unchanged, and the world deck warns"):
    val after = searched(holdingTrumpet(), SearchSource.WorldDeck,
      modifiers)
    assertEquals(after.game.current.temporaryHands(p1), deckTop)
    val shown = ShownOptions.sourcesAfterPicking(holdingTrumpet().ready,
      modifiers)
    assertEquals(ShownOptions.optionFor(shown,
      SearchSource.ref(SearchSource.WorldDeck)).map(option =>
      (option.state, option.reason)),
      Some(("warning", Some(PaintedTrumpet.deckWarning))))

  test("Painted Trumpet and Mushrooms exclude each other"):
    val ready = holdingTrumpet().denizen(MushroomsCard, at = AncientCitySite)
      .ready
    val begun = accepted(rules.startWalker(Ready(ready), ActionRef.Search,
      p1))
    val decisionId =
      ModifierPicks.decisionId(PowerWindow.SearchModifierSelection)
    def shownAfterPicking(power: PowerId) = ShownOptions.atParkedDecision(
      accepted(rules.resolveWalker(begun.state, p1, decisionId,
        ModifierPlay.pick(power))).state)
    assertEquals(ShownOptions.optionFor(shownAfterPicking(Mushrooms.id),
      DecisionOptionRef.Relic(PaintedTrumpetCard.id)).map(option =>
      (option.state, option.reason)),
      Some(("unavailable", Some(PaintedTrumpet.notWithMushrooms))))
    assertEquals(ShownOptions.optionFor(shownAfterPicking(PaintedTrumpet.id),
      DecisionOptionRef.Denizen(MushroomsCard.id)).map(option =>
      (option.state, option.reason)),
      Some(("unavailable", Some(Mushrooms.notWithTrumpet))))
