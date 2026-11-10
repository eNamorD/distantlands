package distantlands.gameplay.powers.search

import distantlands.gameplay.ModifierPlay
import distantlands.gameplay.cards.AncientCitySite
import distantlands.gameplay.powerresolver.ModifierPicks
import distantlands.gameplay.powers.{PowerFixture, SearchFixture}
import distantlands.model._
import distantlands.model.OathState.Ready
import distantlands.testkit.Unwrap.accepted

/** Bracken, Cracked Horn and Ring of Devotion exclude each other: once one
  * is picked, the other two show unavailable (catalog batch 10, slice 9,
  * "Readings"). */
class SearchDiscardsSuite extends munit.FunSuite:
  import PowerFixture.actor
  import SearchDiscardFixture.searchAtAncientCity
  import SearchFixture.rules

  /** p1 holding both relics, beside Bracken, with a secret to pay for it. */
  private val allThree: ReadyGame = searchAtAncientCity(table =>
    table.relic(actor, RingOfDevotionCard).relic(actor, CrackedHornCard)
      .denizen(BrackenCard, at = AncientCitySite).secrets(actor, faceUp = 1))

  /** How `card`'s option shows once `first` is picked: its state and its
    * reason. */
  private def shownAfterPicking(first: PowerId, card: CardId)
      : Option[(String, Option[String])] =
    val begun = accepted(rules.startWalker(Ready(allThree), ActionRef.Search,
      actor))
    val chosen = accepted(rules.resolveWalker(begun.state, actor,
      ModifierPicks.decisionId(PowerWindow.SearchModifierSelection),
      ModifierPlay.pick(first)))
    DecisionOptionRef.ofCard(card).flatMap(ref => ShownOptions.optionFor(
      ShownOptions.atParkedDecision(chosen.state), ref))
      .map(option => (option.state, option.reason))

  private val unavailable =
    Some(("unavailable", Some(SearchDiscards.anotherPicked)))

  test("with Ring of Devotion picked, Cracked Horn and Bracken are " +
      "unavailable"):
    assertEquals(shownAfterPicking(RingOfDevotion.id, CrackedHornCard.id),
      unavailable)
    assertEquals(shownAfterPicking(RingOfDevotion.id, BrackenCard.id),
      unavailable)

  test("with Cracked Horn picked, Ring of Devotion is unavailable"):
    assertEquals(shownAfterPicking(CrackedHorn.id, RingOfDevotionCard.id),
      unavailable)

  test("before any pick, all three are available"):
    val begun = accepted(rules.startWalker(Ready(allThree), ActionRef.Search,
      actor))
    val shown = ShownOptions.atParkedDecision(begun.state)
    Vector[CardId](RingOfDevotionCard.id, CrackedHornCard.id, BrackenCard.id)
      .foreach { card =>
        assertEquals(DecisionOptionRef.ofCard(card).flatMap(
          ShownOptions.optionFor(shown, _)).map(_.state), Some("available"),
          card.value)
      }
