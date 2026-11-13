package distantlands.catalog

import distantlands.catalog.holding.{BelovedCard, GatekeeperCard,
  IronHandCard, PathfinderCard, ReformerCard, SeerCard, TheMouthCard,
  WealthyCard}
import distantlands.gameplay.setup.FirstGameSetupFixture
import distantlands.model.{FoundationNumber, Suit}

/** The goal and suit each legacy prints (CR section 12; Legacies design,
  * "Card data"). The goal is the ribbon's icon and the suit its seal. */
class LegacyCardDataSuite extends munit.FunSuite:
  private val legacies = FirstGameSetupFixture.catalog.legacies

  test("each goal is printed on six legacies, one of each suit"):
    val byGoal = legacies.groupBy(_.goal)
    assertEquals(byGoal.keySet, LegacyGoal.all.toSet)
    byGoal.foreach((goal, cards) =>
      assertEquals(cards.map(_.suit).sortBy(_.ordinal), Suit.all.sortBy(_.ordinal),
        s"$goal"))

  test("each goal covers a run of six ids"):
    val ids = LegacyGoal.all.map(goal =>
      legacies.filter(_.goal == goal).map(_.id.value).sorted)
    assertEquals(ids, (1 to 36).map(n => f"L$n%02d").grouped(6)
      .map(_.toVector).toVector)

  test("cards read from their seals"):
    assertEquals((IronHandCard.goal, IronHandCard.suit),
      (LegacyGoal.MostSites, Suit.Order))
    assertEquals((SeerCard.goal, SeerCard.suit),
      (LegacyGoal.DarkestSecret, Suit.Arcane))

  test("only the six suit-goal legacies alter a foundation, each a " +
      "different one"):
    val altering: Map[String, FoundationNumber] = legacies.flatMap(card =>
      card.alters.map(card.id.value -> _)).toMap
    assertEquals(altering, Map(
      WealthyCard.id.value -> FoundationNumber.II,
      GatekeeperCard.id.value -> FoundationNumber.III,
      BelovedCard.id.value -> FoundationNumber.IV,
      TheMouthCard.id.value -> FoundationNumber.VI,
      ReformerCard.id.value -> FoundationNumber.V,
      PathfinderCard.id.value -> FoundationNumber.I))
    assert(legacies.filter(_.alters.nonEmpty)
      .forall(_.goal == LegacyGoal.MostOfSuit))
