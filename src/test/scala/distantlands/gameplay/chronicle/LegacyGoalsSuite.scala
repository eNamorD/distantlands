package distantlands.gameplay.chronicle

import distantlands.catalog.LegacyGoal
import distantlands.catalog.holding.{IronHandCard, KeeperOfOrderCard,
  MatchmakerCard, SeerCard, WealthyCard}
import distantlands.gameplay.chronicle.legacies.OathsOfLoyaltyCard
import distantlands.gameplay.cards.{AncientCitySite, BrokenPeaksSite,
  BuriedGiantSite, DesolateShoreSite, DunesSite, FairIsleSite}
import distantlands.gameplay.operations.TheGrandScepterCard
import distantlands.gameplay.powers.economy.{CupOfPlentyCard, OldSongsCard,
  RowdyPubCard, VillageIdiotCard}
import distantlands.gameplay.powers.setup.ProvingGroundsCard
import distantlands.gameplay.GrandMaskCard
import distantlands.gameplay.setup.FirstGameSetupFixture.catalog
import distantlands.model._
import distantlands.testkit.Table
import distantlands.testkit.Table.{p1, p2, p3}

/** Who meets a legacy's goal in Resolve Legacies (Legacies design, slice 3;
  * rulings 6 and 7, R22 and R23). Every table is p1's won game. */
class LegacyGoalsSuite extends munit.FunSuite:
  private def won: Table = ChronicleWalk.chronicleWonByP1

  private def meeting(table: Table, legacy: distantlands.catalog.Legacy)(
      using munit.Location): Set[PlayerId] =
    LegacyGoals.meeting(table.ready, catalog, legacy)

  test("the most sites: one Exile alone leads"):
    val table = won.warbandsAt(AncientCitySite, p2, 1)
      .warbandsAt(BrokenPeaksSite, p2, 1).warbandsAt(BuriedGiantSite, p3, 1)
    assertEquals(meeting(table, IronHandCard), Set(p2))
    assertEquals(LegacyGoals.score(table.ready, catalog, LegacyGoal.MostSites,
      IronHandCard.suit, p3), 1)

  test("every Imperial is credited with each Empire site, and Imperials " +
      "alone tie friendly"):
    val table = won.imperial(p1, p3).warbandsAt(AncientCitySite, p1, 1)
      .warbandsAt(BrokenPeaksSite, p1, 1).warbandsAt(BuriedGiantSite, p1, 1)
      .warbandsAt(DesolateShoreSite, p2, 1).warbandsAt(DunesSite, p2, 1)
    assertEquals(meeting(table, IronHandCard), Set(p1, p3))

  test("a tie that includes an Exile meets nobody, even with the Empire"):
    val withEmpire = won.imperial(p1).warbandsAt(AncientCitySite, p1, 1)
      .warbandsAt(BrokenPeaksSite, p1, 1)
      .warbandsAt(DesolateShoreSite, p2, 1).warbandsAt(DunesSite, p2, 1)
    assertEquals(meeting(withEmpire, IronHandCard), Set.empty)
    val exiles = won.warbandsAt(AncientCitySite, p2, 1)
      .warbandsAt(BrokenPeaksSite, p3, 1)
    assertEquals(meeting(exiles, IronHandCard), Set.empty)

  test("a score of 0 never meets a goal, and bandits never compete"):
    assertEquals(meeting(won.bandits(AncientCitySite, 2), IronHandCard),
      Set.empty)
    assertEquals(meeting(won, OathsOfLoyaltyCard), Set.empty)

  test("cards of a suit: denizens and edifices, ruined too, at sites the " +
      "player rules, and their own advisers, facedown too (R23)"):
    val table = won.warbandsAt(AncientCitySite, p2, 1)
      .denizen(VillageIdiotCard, AncientCitySite)
      .edifice(ProvingGroundsCard, EdificeSide.Ruined, AncientCitySite)
      .denizen(RowdyPubCard, FairIsleSite)
      .adviser(p3, OldSongsCard, facedown = true)
    def score(player: PlayerId) = LegacyGoals.score(table.ready, catalog,
      LegacyGoal.MostOfSuit, WealthyCard.suit, player)
    assertEquals(Vector(p1, p2, p3).map(score), Vector(0, 2, 1))
    assertEquals(meeting(table, WealthyCard), Set(p2))

  test("Imperials share the cards at Imperial sites"):
    val table = won.imperial(p1, p3).warbandsAt(AncientCitySite, p1, 1)
      .denizen(VillageIdiotCard, AncientCitySite)
    assertEquals(meeting(table, WealthyCard), Set(p1, p3))

  test("the highest relic is the best printed value held, facedown too"):
    val table = won.relic(p2, CupOfPlentyCard, facedown = true)
      .relic(p3, GrandMaskCard)
    assertEquals(meeting(table, OathsOfLoyaltyCard), Set(p2))

  test("a banner goal is met by its holder, and by nobody when unheld"):
    val held = won.peoplesFavor(Some(p3), 1).darkestSecret(Some(p2), 1)
    assertEquals(meeting(held, MatchmakerCard), Set(p3))
    assertEquals(meeting(held, SeerCard), Set(p2))
    val unheld = won.peoplesFavor(None, 1).darkestSecret(None, 1)
    assertEquals(meeting(unheld, MatchmakerCard), Set.empty)
    assertEquals(meeting(unheld, SeerCard), Set.empty)

  test("the Grand Scepter goal: its holder; the winner with none in play " +
      "(ruling 7); nobody when it lies at a site (R22)"):
    assertEquals(meeting(won.relic(p2, TheGrandScepterCard),
      KeeperOfOrderCard), Set(p2))
    assertEquals(meeting(won, KeeperOfOrderCard), Set(p1))
    assertEquals(meeting(won.relicAt(TheGrandScepterCard, AncientCitySite),
      KeeperOfOrderCard), Set.empty)

  test("the turn order runs from the last round's starter (R21)"):
    assertEquals(LegacyGoals.turnOrder(won.roundStarter(p2).ready),
      Vector(p2, p3, p1))
