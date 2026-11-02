package oathdigital.gameplay.powers.whenplayed

import oathdigital.gameplay.operations.HallOfMinistersCard
import oathdigital.gameplay.powers.NoteText
import oathdigital.gameplay.powers.action.HomesteadersCard
import oathdigital.gameplay.powers.campaign.WildMountsCard
import oathdigital.gameplay.powers.recover.RelicPatrolsCard
import oathdigital.gameplay.powers.search.{ErrandBoyCard, LandWardenCard}
import oathdigital.model._
import oathdigital.testkit.{Look, Outcomes, Table}
import oathdigital.testkit.Table.{p1, p2}

class RiotsSuite extends munit.FunSuite:
  import WhenPlayedHarness._

  private val power = registered[Riots]
  private val riots = power.cardId
  private val homesteaders = HomesteadersCard.id
  private val landWarden = LandWardenCard.id
  private val errandBoy = ErrandBoyCard.id
  private val wildMounts = WildMountsCard.id
  private val hallOfBandits = HallOfMinistersCard.id
  private val relicPatrols = RelicPatrolsCard.id

  private def discarded(cards: Vector[CardId]): NoteText.Said =
    NoteText.Said("discarded",
      s"Discarded ${cards.map(_.value).mkString(", ")}.", covers = true)

  private def burned(n: Int): NoteText.Said = NoteText.Said("burned",
    s"Burned $n favor from the People's Favor.", covers = false)

  /** Riots as p1's adviser, with four discardable cards in p1's region
    * (Homesteaders, Errand Boy, Wild Mounts and the ruined Hall of Bandits),
    * the intact Relic Patrols there and Land Warden in another region. */
  private def staged: Table = p1AtHomeSite.adviser(p1, riots)
    .denizen(homesteaders, at = homeSite).denizen(errandBoy, at = homeSite)
    .denizen(wildMounts, at = nearSite)
    .edifice(HallOfMinistersCard, EdificeSide.Ruined, at = nearSite)
    .edifice(RelicPatrolsCard, EdificeSide.Intact, at = nearSite)
    .denizen(landWarden, at = awaySite)

  private val targets: Vector[CardId] = inMapOrder(
    homeSite -> Vector(homesteaders, errandBoy),
    nearSite -> Vector(wildMounts, hallOfBandits))

  test("it discards every denizen and ruined edifice in the region, then " +
      "burns as many favor from a held People's Favor"):
    val ready = staged.peoplesFavor(Some(p2), 5).ready
    val done = Outcomes.finished(play(ready, power, riots))
    val after = Look(done.treeless)
    assertEquals(after.cardsAt(homeSite), Vector.empty)
    assertEquals(after.cardsAt(nearSite), Vector[CardId](relicPatrols))
    assertEquals(after.cardsAt(awaySite), Vector[CardId](landWarden))
    // 5 on the banner, less 1 per card discarded.
    assertEquals(Look(done.treeless).favorOnPeoplesFavor, 5 - 4)
    assertEquals(Look(done.treeless).peoplesFavorHolder, Some(p2))
    assertJournalRebuilds(ready, done.events, done.treeless)
    assertEquals(said(power, done.events),
      Vector(discarded(targets), burned(4)))

  test("played to a site it discards and counts itself"):
    val ready = p1AtHomeSite.denizen(riots, at = homeSite)
      .denizen(homesteaders, at = homeSite).peoplesFavor(None, 5).ready
    val done = Outcomes.finished(fire(ready, power, hookAt(riots, homeSite)))
    assertEquals(Look(done.treeless).cardsAt(homeSite), Vector.empty)
    // Riots and Homesteaders are discarded: 5 - 2.
    assertEquals(Look(done.treeless).favorOnPeoplesFavor, 5 - 2)
    assertEquals(said(power, done.events),
      Vector(discarded(Vector(riots, homesteaders)), burned(2)))

  test("the burn takes what the People's Favor holds"):
    val done = Outcomes.finished(
      play(staged.peoplesFavor(None, 1).ready, power, riots))
    assertEquals(Look(done.treeless).favorOnPeoplesFavor, 0)
    assertEquals(said(power, done.events),
      Vector(discarded(targets), burned(1)))

  test("an empty People's Favor burns nothing, and the line says so"):
    val done = Outcomes.finished(
      play(staged.peoplesFavor(None, 0).ready, power, riots))
    assertEquals(said(power, done.events), Vector(discarded(targets),
      NoteText.Said("unburned", "The People's Favor had no favor to burn.",
        covers = false)))

  test("with nothing to discard it burns nothing"):
    val ready = p1AtHomeSite.adviser(p1, riots)
      .denizen(landWarden, at = awaySite).peoplesFavor(None, 5).ready
    val done = Outcomes.finished(play(ready, power, riots))
    assertEquals(ops(done.events), Vector.empty)
    assertEquals(Look(done.treeless).favorOnPeoplesFavor, 5)
    assertEquals(said(power, done.events), Vector(NoteText.Said("none",
      "Nothing was discarded.", covers = true)))
