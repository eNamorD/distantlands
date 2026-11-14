package distantlands.catalog

import distantlands.catalog.holding._
import distantlands.gameplay.chronicle.legacies._
import distantlands.gameplay.powers.legacies._
import distantlands.gameplay.powers.travel.{AncestralLandsCard,
  PathfinderCard}
import distantlands.gameplay.powers.wake.LightFingersCard
import distantlands.gameplay.powers.economy.BelovedCard
import distantlands.gameplay.powers.campaign.KeeperOfOrderCard
import distantlands.gameplay.actions.negotiation.HighPriestCard
import distantlands.gameplay.setup.FirstGameSetupFixture
import distantlands.gameplay.setup.legacies._
import distantlands.model.{FoundationNumber, Suit}

/** The goal and suit each legacy prints (CR section 12; Legacies design,
  * "Card data"). The goal is the ribbon's icon and the suit its seal. */
class LegacyCardDataSuite extends munit.FunSuite:
  private val legacies = FirstGameSetupFixture.catalog.legacies

  test("each goal is printed on six legacies, one of each suit"):
    val byGoal = legacies.groupBy(_.goal)
    assertEquals(byGoal.keySet, LegacyGoal.values.toSet)
    byGoal.foreach((goal, cards) =>
      assertEquals(cards.map(_.suit).sortBy(_.ordinal), Suit.all.sortBy(_.ordinal),
        s"$goal"))

  test("each goal covers a run of six ids"):
    val ids = LegacyGoal.values.toVector.map(goal =>
      legacies.filter(_.goal == goal).map(_.id.value).sorted)
    assertEquals(ids, (1 to 36).map(n => f"L$n%02d").grouped(6)
      .map(_.toVector).toVector)

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

  /** The design's "Card data" tables, card by card: printed id order, the
    * goal of each run of six, the seal's suit and the foundation set. */
  private val printed: Vector[(Legacy, LegacyGoal, Suit,
      Option[FoundationNumber])] =
    import LegacyGoal._
    import Suit._
    def run(goal: LegacyGoal, cards: (Legacy, Suit)*) =
      cards.map((card, suit) => (card, goal, suit,
        Option.empty[FoundationNumber])).toVector
    run(MostSites, IronHandCard -> Order, ChroniclerCard -> Arcane,
      SteadfastCard -> Discord, PeacemakerCard -> Hearth,
      TheNeedleCard -> Beast, CircleOfSwordsCard -> Nomad) ++
    run(GrandScepter, GreatArchitectCard -> Hearth,
      AncestralLandsCard -> Nomad, ScepterBearerCard -> Discord,
      RoyalLineCard -> Beast, KeeperOfOrderCard -> Order,
      GoldenKeyCard -> Arcane) ++
    run(PeoplesFavor, WorldCrafterCard -> Nomad, MatchmakerCard -> Hearth,
      RevolutionaryCard -> Discord, PopulistCard -> Beast,
      RivalCard -> Order, TheStandardBearerCard -> Arcane) ++
    run(DarkestSecret, TheEarCard -> Hearth, HeirloomCard -> Beast,
      HighPriestCard -> Order, SeerCard -> Arcane, ArbiterCard -> Discord,
      TheBrightBeaconCard -> Nomad) ++
    run(HighestRelic, OathsOfLoyaltyCard -> Hearth,
      ConspiratorCard -> Discord, LightFingersCard -> Beast,
      SecretSocietyCard -> Arcane, RuthlessCard -> Order,
      QuestToDistantLandsCard -> Nomad) ++
    Vector(
      (WealthyCard, MostOfSuit, Hearth, Some(FoundationNumber.II)),
      (GatekeeperCard, MostOfSuit, Discord, Some(FoundationNumber.III)),
      (BelovedCard, MostOfSuit, Beast, Some(FoundationNumber.IV)),
      (TheMouthCard, MostOfSuit, Arcane, Some(FoundationNumber.VI)),
      (ReformerCard, MostOfSuit, Order, Some(FoundationNumber.V)),
      (PathfinderCard, MostOfSuit, Nomad, Some(FoundationNumber.I)))

  test("every legacy prints the goal, suit and foundation the design lists, " +
      "in id order"):
    assertEquals(printed.map(_._1.id.value),
      (1 to 36).map(n => f"L$n%02d").toVector)
    printed.foreach((card, goal, suit, alters) =>
      assertEquals((card.goal, card.suit, card.alters), (goal, suit, alters),
        card.name))
    assertEquals(legacies.map(_.id).sortBy(_.value),
      printed.map(_._1.id))

  test("each goal prints the rules' words for it (R42)"):
    assertEquals(LegacyGoal.values.toVector.map(_.printed), Vector(
      "Rule the most sites.", "Hold the Grand Scepter. If none, win.",
      "Hold the People's Favor.", "Hold the Darkest Secret.",
      "Hold the relic of the highest value (shown under name).",
      "Rule the most cards of this suit (even advisers)."))
