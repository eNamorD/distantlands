package oathdigital.gameplay.powers.setup

import oathdigital.gameplay.powers.{NoteText, WalkerPowerCatalog}
import oathdigital.gameplay.powers.action.{EldersCard, MessengerCard}
import oathdigital.gameplay.setup.{FirstGameSetupFixture, SetupProcedure,
  SetupWalkDriver}
import oathdigital.gameplay.walker.WalkerPowers
import oathdigital.model._
import oathdigital.testkit.{Look, Table}
import oathdigital.testkit.Unwrap.accepted

/** The SETUP (END) rules of catalog batch 9, slice 1: Hall of Mockery, Lost
  * Guardians, Ruined School and Broken Horn. Each runs once at Setup's end
  * when its ruined face stands on the map, and not otherwise. The driver
  * folds the recorded operations as replay does, so the finished game is
  * the replayed one. */
class RuinedSetupRulesSuite extends munit.FunSuite:
  private val catalog = FirstGameSetupFixture.catalog
  private val powers = WalkerPowers.selected(WalkerPowerCatalog.default(catalog),
    Vector.empty)
  /** The fixture's first site, in the Cradle. */
  private val site = FirstGameSetupFixture.sites.head
  private val banditSite = FirstGameSetupFixture.sites(1)

  // The Setup walk is the subject (the face's effect runs when the walk
  // reaches it), so these tests start from the fresh Setup, not a Table.
  private def stagedWith(edifice: EdificeId,
      side: EdificeSide = EdificeSide.Ruined): Table =
    Table.from(FirstGameSetupFixture.freshReady).edifice(edifice, side, at = site)

  /** Bandits rule exactly `bandited`, one each; every other site is empty.
    * The fresh deal fills every site with bandits to its capacity. */
  private def banditsOnlyAt(table: Table, bandited: SiteId*): Table =
    FirstGameSetupFixture.sites.foldLeft(table)((staged, s) =>
      staged.bandits(s, if bandited.contains(s) then 1 else 0))

  private def finish(ready: ReadyGame): SetupWalkDriver.SetupWalked =
    val tree = accepted(SetupProcedure.build(catalog, ready,
      ready.game.current.turn.activePlayer, Vector.empty))
    SetupWalkDriver.driveWithEvents(ready, tree, TriggeredProcedureRef.Setup,
      powers)

  private def said(rule: RuinedSetupRule, events: Vector[OathEvent])
      : Vector[NoteText.Said] = NoteText.said(rule.id, rule.noteKeys, events)

  // ---- Hall of Mockery ----

  test("Hall of Mockery places 4 favor on the People's Favor and burns 1 " +
      "favor from each bank, without moving the banner"):
    val staged = stagedWith(HallOfDebateCard.id).ready
    val banks = staged.banks.favor
    val walked = finish(staged)
    val look = Look(walked.ready)
    assertEquals(look.favorOnPeoplesFavor, staged.game.current.banners
      .peoplesFavor.favor + HallOfMockery.Favor)
    assertEquals(look.peoplesFavorHolder, None)
    Suit.all.foreach(suit =>
      assertEquals(look.bankFavor(suit), banks(suit) - 1))
    assertEquals(said(HallOfMockery, walked.events), Vector(NoteText.Said(
      "placed-and-burned", "Placed 4 favor on the People's Favor and " +
        s"burned ${Suit.all.size} favor from the banks.", covers = false)))

  test("Hall of Mockery with empty banks places the favor and writes only " +
      "the placement"):
    val staged = Suit.all.foldLeft(stagedWith(HallOfDebateCard.id))(
      (table, suit) => table.bankFavor(suit, 0)).ready
    val walked = finish(staged)
    assertEquals(Look(walked.ready).favorOnPeoplesFavor, staged.game.current
      .banners.peoplesFavor.favor + HallOfMockery.Favor)
    assertEquals(said(HallOfMockery, walked.events), Vector(NoteText.Said(
      "placed", "Placed 4 favor on the People's Favor.", covers = false)))

  test("the intact Hall of Debate places nothing at Setup's end"):
    val staged = stagedWith(HallOfDebateCard.id, EdificeSide.Intact).ready
    val walked = finish(staged)
    assertEquals(Look(walked.ready).favorOnPeoplesFavor,
      staged.game.current.banners.peoplesFavor.favor)
    assertEquals(said(HallOfMockery, walked.events), Vector.empty)

  // ---- Lost Guardians ----

  test("Lost Guardians places 3 secrets on the Darkest Secret, which stays " +
      "in the shared bank"):
    val staged = stagedWith(TomeGuardiansCard.id).ready
    val walked = finish(staged)
    val look = Look(walked.ready)
    assertEquals(look.secretsOnDarkestSecret, staged.game.current.banners
      .darkestSecret.secrets + LostGuardians.Secrets)
    assertEquals(look.darkestSecretHolder, None)
    assertEquals(said(LostGuardians, walked.events), Vector(NoteText.Said(
      "placed", "Placed 3 secrets on the Darkest Secret.", covers = false)))

  test("the intact Tome Guardians places nothing at Setup's end"):
    val staged = stagedWith(TomeGuardiansCard.id, EdificeSide.Intact).ready
    val walked = finish(staged)
    assertEquals(Look(walked.ready).secretsOnDarkestSecret,
      staged.game.current.banners.darkestSecret.secrets)
    assertEquals(said(LostGuardians, walked.events), Vector.empty)

  // ---- Ruined School ----

  test("Ruined School places 1 secret on each bandit site"):
    val staged = banditsOnlyAt(stagedWith(SchoolOfVinesCard.id), banditSite)
      .ready
    val before = Look(staged).siteTokens(banditSite).secrets
    val walked = finish(staged)
    assertEquals(Look(walked.ready).siteTokens(banditSite).secrets, before + 1)
    assertEquals(Look(walked.ready).siteTokens(site).secrets,
      Look(staged).siteTokens(site).secrets)
    assertEquals(said(RuinedSchool, walked.events), Vector(NoteText.Said(
      "placed", "Placed 1 secret on the bandit site.", covers = false)))

  test("Ruined School with no bandit site places nothing and writes no line"):
    val staged = banditsOnlyAt(stagedWith(SchoolOfVinesCard.id)).ready
    val walked = finish(staged)
    FirstGameSetupFixture.sites.foreach(s => assertEquals(
      Look(walked.ready).siteTokens(s), Look(staged).siteTokens(s)))
    assertEquals(said(RuinedSchool, walked.events), Vector.empty)

  test("the intact School of Vines places nothing at Setup's end"):
    val staged = banditsOnlyAt(stagedWith(SchoolOfVinesCard.id,
      EdificeSide.Intact), banditSite).ready
    val walked = finish(staged)
    assertEquals(Look(walked.ready).siteTokens(banditSite).secrets,
      Look(staged).siteTokens(banditSite).secrets)
    assertEquals(said(RuinedSchool, walked.events), Vector.empty)

  // ---- Broken Horn ----

  private val conspiracy = oathdigital.gameplay.actions.VisionRules.Conspiracy
  private val staged = Vector[WorldCardId](EldersCard.id, MessengerCard.id,
    conspiracy)

  /** The Horn on `side` at the Cradle site, with Elders, Messenger and the
    * Conspiracy in the Cradle's pile beside what Setup's reveals discard
    * there. */
  private def hornWithPile(side: EdificeSide): ReadyGame =
    stagedWith(ForestHornCard.id, side)
      .discarded(Region.Cradle, staged*).ready

  /** The Cradle's pile as Setup leaves it when nothing buries it: the
    * intact Horn buries nothing. */
  private def pileLeftBySetup: Vector[WorldCardId] =
    Look(finish(hornWithPile(EdificeSide.Intact)).ready)
      .discardPile(Region.Cradle)

  test("Broken Horn buries every card of its region's discard pile, " +
      "Visions included, to the bottom of the world deck"):
    val ready = hornWithPile(EdificeSide.Ruined)
    assertEquals(ready.game.current.map.regionOf(site), Some(Region.Cradle))
    val pile = pileLeftBySetup
    assert(staged.forall(pile.contains))
    val walked = finish(ready)
    assertEquals(Look(walked.ready).discardPile(Region.Cradle), Vector.empty)
    val deck = walked.ready.game.current.commonCards.worldDeck
    assertEquals(deck.takeRight(pile.size).toSet, pile.toSet)
    assertEquals(said(BrokenHorn, walked.events), Vector(NoteText.Said(
      "buried", s"Buried the Cradle discard pile (${pile.size} cards).",
      covers = false)))

  test("the intact Forest Horn buries nothing at Setup's end"):
    val walked = finish(hornWithPile(EdificeSide.Intact))
    assert(staged.forall(Look(walked.ready).discardPile(Region.Cradle)
      .contains))
    assertEquals(said(BrokenHorn, walked.events), Vector.empty)

  // ---- Broken Clock ----

  test("Broken Clock advances the round marker once at Setup's end"):
    val staged = stagedWith(GreatClockCard.id).ready
    assertEquals(staged.game.current.tracks.round, 1)
    val walked = finish(staged)
    assertEquals(walked.ready.game.current.tracks.round, 2)
    assertEquals(said(BrokenClock, walked.events), Vector(NoteText.Said(
      "advanced", "Advanced the round marker to round 2.", covers = false)))

  test("the intact Great Clock moves no marker at Setup's end"):
    val staged = stagedWith(GreatClockCard.id, EdificeSide.Intact).ready
    val walked = finish(staged)
    assertEquals(walked.ready.game.current.tracks.round,
      staged.game.current.tracks.round)
    assertEquals(said(BrokenClock, walked.events), Vector.empty)
