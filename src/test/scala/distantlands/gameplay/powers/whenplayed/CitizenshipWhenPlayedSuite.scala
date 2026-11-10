package distantlands.gameplay.powers.whenplayed

import distantlands.gameplay.WalkerDiceFixture
import distantlands.gameplay.powers.NoteText
import distantlands.gameplay.roles.Citizenship
import distantlands.model._
import distantlands.testkit.{Look, Table}
import distantlands.testkit.Table.{p1, p2, p3}

/** The four "become a Citizen" When Played denizens (rulings R21, R22). */
class CitizenshipWhenPlayedSuite extends munit.FunSuite:
  import WhenPlayedHarness._

  private val heir = registered[LongLostHeir.type]
  private val bewitch = registered[Bewitch.type]
  private val pact = registered[AncientPact.type]
  private val ambitions = registered[RoyalAmbitions.type]

  /** p1, an Exile in their Act at the home site, with p3 the Chancellor and
    * the first-game option off. */
  private def exileInEmpire: Table = p1AtHomeSite.turn(p1, Phase.Act)
    .imperial(p3).allowingCitizenshipPowers

  private def line(key: String, text: String): NoteText.Said =
    NoteText.Said(key, text, covers = false)

  private def played(ready: ReadyGame, power: CitizenshipWhenPlayed,
      answers: DecisionAnswer*): Walked =
    walkShuffling(ready, power, hook(power.cardId),
      WalkerDiceFixture.reversingShuffle,
      answers*)

  test("Long-Lost Heir: an Exile who accepts becomes a Citizen, and their " +
      "Act ends"):
    val start = exileInEmpire.ready
    val walked = played(start, heir, chooseOne(heir.become))
    assertEquals(walked.asked.map(_.decisionId),
      Vector("cardplay.long-lost-heir.citizenship"))
    assertEquals(Imperials.role(walked.end, p1), Some(Role.Citizen))
    assertEquals(walked.end.game.current.turn.phase, Phase.Rest)
    assertEquals(said(heir, walked.events),
      Vector(line("used", "Became a Citizen.")))
    assertJournalRebuilds(start, walked.events, walked.end)

  test("declining keeps the role and says so"):
    val walked = played(exileInEmpire.ready, heir, chooseOne(heir.decline))
    assertEquals(Imperials.role(walked.end, p1), Some(Role.Exile))
    assertEquals(said(heir, walked.events),
      Vector(line("declined", "Declined Citizenship.")))

  test("a player who is not an Exile is asked nothing"):
    val walked = played(p1AtHomeSite.turn(p1, Phase.Act).imperial(p3, p1)
      .allowingCitizenshipPowers.ready, heir)
    assertEquals(walked.asked, Vector.empty)
    assertEquals(said(heir, walked.events),
      Vector(line("not-exile", "Was not an Exile.")))

  test("with no Empire the player becomes the Chancellor"):
    val walked = played(p1AtHomeSite.turn(p1, Phase.Act)
      .allowingCitizenshipPowers.ready, heir, chooseOne(heir.become))
    assertEquals(Imperials.chancellor(walked.end), Some(p1))
    assertEquals(said(heir, walked.events),
      Vector(line("used.chancellor", "Became the Chancellor.")))

  test("under the first-game option becoming a Citizen is shown " +
      "unavailable with its reason"):
    val walked = played(p1AtHomeSite.turn(p1, Phase.Act).imperial(p3).ready,
      heir, chooseOne(heir.decline))
    assertEquals(chooseOneOptions(walked.asked.head), Vector(
      DecisionOption.Unavailable(
        DecisionOption.Button(heir.become, "Become a Citizen"),
        OptionReason.Written(Citizenship.ignoredReason)),
      DecisionOption.Button(heir.decline, "Stay an Exile")))

  test("Bewitch: with the most secrets the player takes a Reliquary relic " +
      "facedown and becomes a Citizen"):
    val start = exileInEmpire.secrets(p1, 3).secrets(p2, 1).secrets(p3, 1)
      .reliquaryFromDeck(3).ready
    val stocked = start.game.campaign.reliquary
    val walked = played(start, bewitch, chooseOne(bewitch.become))
    // The shuffle reverses the Reliquary, so its last relic comes first.
    val taken = Look(walked.end).player(p1).relics
      .filter(relic => stocked.contains(relic.id))
    assertEquals(taken.map(_.id), Vector(stocked.last))
    assertEquals(taken.map(_.orientation), Vector(Orientation.FaceDown))
    assertEquals(walked.end.game.campaign.reliquary.size, 2)
    assertEquals(Imperials.role(walked.end, p1), Some(Role.Citizen))
    assertJournalRebuilds(start, walked.events, walked.end)

  test("Bewitch: a tie for the most secrets is not the most"):
    val walked = played(exileInEmpire.secrets(p1, 2).secrets(p2, 2)
      .secrets(p3, 0).ready, bewitch)
    assertEquals(walked.asked, Vector.empty)
    assertEquals(said(bewitch, walked.events),
      Vector(line("unmet", "Did not have the most secrets.")))

  test("Bewitch: an empty Reliquary still makes the player a Citizen"):
    val start = exileInEmpire.secrets(p1, 3).secrets(p2, 0).secrets(p3, 0)
      .ready
    assertEquals(start.game.campaign.reliquary, Vector.empty)
    val walked = played(start, bewitch, chooseOne(bewitch.become))
    assertEquals(Imperials.role(walked.end, p1), Some(Role.Citizen))
    assertEquals(said(bewitch, walked.events), Vector(
      line("reliquary-empty", "The Reliquary was empty."),
      line("used", "Became a Citizen.")))

  test("Ancient Pact asks only the holder of the Darkest Secret"):
    val start = exileInEmpire.darkestSecret(Some(p1), 0).reliquaryFromDeck(1)
      .ready
    val stocked = start.game.campaign.reliquary
    val holding = played(start, pact, chooseOne(pact.become))
    assertEquals(Imperials.role(holding.end, p1), Some(Role.Citizen))
    assert(Look(holding.end).player(p1).relics.exists(relic =>
      stocked.contains(relic.id)))
    assertEquals(holding.end.game.campaign.reliquary, Vector.empty)
    val not = played(exileInEmpire.darkestSecret(Some(p2), 0).ready, pact)
    assertEquals(said(pact, not.events),
      Vector(line("unmet", "Did not hold the Darkest Secret.")))

  test("Royal Ambitions asks only a player who rules strictly the most " +
      "sites"):
    val most = exileInEmpire.warbandsAt(homeSite, p1, 1)
      .warbandsAt(nearSite, p1, 1).warbandsAt(awaySite, p2, 1)
    val start = most.reliquaryFromDeck(2).ready
    val accepted = played(start, ambitions, chooseOne(ambitions.become))
    assertEquals(Imperials.role(accepted.end, p1), Some(Role.Citizen))
    assertEquals(accepted.end.game.campaign.reliquary.size, 1)
    assertJournalRebuilds(start, accepted.events, accepted.end)
    val tied = exileInEmpire.warbandsAt(homeSite, p1, 1)
      .warbandsAt(awaySite, p2, 1)
    val walked = played(tied.ready, ambitions)
    assertEquals(said(ambitions, walked.events),
      Vector(line("unmet", "Did not rule the most sites.")))
