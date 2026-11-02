package oathdigital.gameplay.powers.action

import oathdigital.gameplay.actions.VisionRules
import oathdigital.gameplay.cards.AncientCitySite
import oathdigital.gameplay.powers.{NoteText, PowerFixture}
import oathdigital.gameplay.powers.campaign.WrestlersCard
import oathdigital.gameplay.setup.FirstGameSetupFixture.catalog
import oathdigital.model._
import oathdigital.model.OathState.Ready
import oathdigital.testkit.{Look, MenuPlay, PhasePowerHarness, Table}
import oathdigital.testkit.Outcomes.{assertWireRoundTrips, ready}
import oathdigital.testkit.Unwrap.accepted

/** Squalid District (catalog batch 9, slice 9): discard facedown advisers,
  * draw as many cards from the world deck's bottom as facedown advisers,
  * a Vision among them advancing the Visions Drawn track. */
class SqualidDistrictSuite extends munit.FunSuite:
  import PowerFixture._
  import PhasePowerHarness.{awaits, backToActing, replayed, rules}

  private val district = FestivalDistrictCard.id
  private val source = DecisionOptionRef.Edifice(district)
  private val power = SqualidDistrict.forCatalog(catalog)
  private val faith = VisionRules.Faith

  /** p1's Act at Ancient City beside the ruined District with 2 favor,
    * holding Wrestlers and Faith facedown, Wolves faceup; Elders and
    * Messenger lie at the deck's bottom (Messenger last). */
  private def staged(deckBottom: Table => Table =
      _.worldDeckBottom(EldersCard, MessengerCard)): ReadyGame =
    deckBottom(Table.from(quietTable).turn(actor, Phase.Act)
      .pawn(actor, AncientCitySite)
      .edifice(district, EdificeSide.Ruined, at = AncientCitySite)
      .favor(actor, 2).adviser(actor, WrestlersCard, facedown = true)
      .adviser(actor, faith, facedown = true).adviser(actor, WolvesCard))
      .ready

  private def use(state: ReadyGame) = rules.startWalker(
    Ready(MenuPlay.withoutMenu(state)),
    ActionRef.UsePower(SqualidDistrict.id), actor, Vector(source))

  private def choose(state: OathState, slots: Int*) = rules.resolveWalker(
    state, actor, SqualidDistrict.decisionId,
    DecisionAnswer.ChooseManyAnswer(slots.toVector.map(
      DecisionOptionRef.AdviserSlot(actor, _))))

  test("the facedown advisers are offered by slot; the chosen ones are " +
      "discarded and as many bottom denizens arrive facedown, with the line"):
    val start = staged()
    val parked = accepted(use(start))
    assert(awaits(parked, SqualidDistrict.decisionId))
    // 1 placed, 1 burnt.
    assertEquals(Look(ready(parked.state)).tokensOn(district), Tokens(1, 0))
    assertEquals(Look(ready(parked.state)).favor(actor), 0)
    val done = accepted(choose(parked.state, 0, 1))
    assert(backToActing(done))
    val after = ready(done.state)
    assertEquals(Look(after).player(actor).advisers.toSet,
      Set[AdviserState](DenizenState(WolvesCard.id, Orientation.FaceUp,
        Tokens.empty),
        DenizenState(EldersCard.id, Orientation.FaceDown, Tokens.empty),
        DenizenState(MessengerCard.id, Orientation.FaceDown, Tokens.empty)))
    assert(Look(after).inRegionalDiscard(WrestlersCard.id))
    assert(Look(after).inRegionalDiscard(faith))
    assertEquals(NoteText.said(power, done.events), Vector(NoteText.Said(
      NoteKey.Used, "Discarded 2 facedown advisers and drew 2 cards " +
        "facedown.", covers = false)))
    assertEquals(replayed(start, parked.events ++ done.events),
      Right(done.state))
    assertWireRoundTrips(parked.events ++ done.events)

  test("a Vision at the deck's bottom is drawn as a facedown adviser and " +
      "advances the Visions Drawn track once per Vision drawn"):
    // Both advisers go, and two Visions come from the deck's bottom, so the
    // track advances twice.
    val (conquest, sanctuary) = (VisionRules.Conquest, VisionRules.Sanctuary)
    val start = staged(_.worldDeckBottom(EldersCard, conquest, sanctuary))
    val parked = accepted(use(start))
    val done = accepted(choose(parked.state, 0, 1))
    val after = ready(done.state)
    assertEquals(Look(after).player(actor).advisers.toSet,
      Set[AdviserState](DenizenState(WolvesCard.id, Orientation.FaceUp,
        Tokens.empty),
        VisionState(sanctuary, Orientation.FaceDown),
        VisionState(conquest, Orientation.FaceDown)))
    assert(Look(after).inRegionalDiscard(faith))
    assertEquals(after.game.current.commonCards.worldDeck.lastOption,
      Some(EldersCard.id))
    assertEquals(after.game.current.tracks.visionsDrawn,
      start.game.current.tracks.visionsDrawn + 2)
    assertEquals(NoteText.said(power, done.events), Vector(NoteText.Said(
      NoteKey.Used, "Discarded 2 facedown advisers and drew 2 cards " +
        "facedown.", covers = false)))
    assertEquals(replayed(start, parked.events ++ done.events),
      Right(done.state))
    assertWireRoundTrips(parked.events ++ done.events)

  test("a denizen drawn leaves the track alone, and a short deck draws " +
      "fewer"):
    val one = accepted(choose(accepted(use(staged())).state, 0))
    assertEquals(ready(one.state).game.current.tracks.visionsDrawn,
      staged().game.current.tracks.visionsDrawn)
    val short = staged().updateCurrent(c => c.copy(commonCards =
      c.commonCards.copy(worldDeck = Vector(EldersCard.id))))
    val fewer = accepted(choose(accepted(use(short)).state, 0, 1))
    assertEquals(Look(ready(fewer.state)).advisers(actor).toSet,
      Set[CardId](WolvesCard.id, EldersCard.id))
    assertEquals(NoteText.said(power, fewer.events), Vector(NoteText.Said(
      NoteKey.Used, "Discarded 2 facedown advisers and drew 1 card " +
        "facedown.", covers = false)))

  test("choosing nothing changes no adviser; with no facedown adviser " +
      "nothing is asked"):
    val none = accepted(choose(accepted(use(staged())).state))
    assertEquals(Look(ready(none.state)).advisers(actor).size, 3)
    assertEquals(NoteText.said(power, none.events), Vector(NoteText.Said(
      "used.none", "Discarded no adviser.", covers = false)))
    val bare = Table.from(quietTable).turn(actor, Phase.Act)
      .pawn(actor, AncientCitySite)
      .edifice(district, EdificeSide.Ruined, at = AncientCitySite)
      .favor(actor, 2).ready
    val done = accepted(use(bare))
    assert(backToActing(done))
    assertEquals(Look(ready(done.state)).tokensOn(district), Tokens(1, 0))
    assertEquals(NoteText.said(power, done.events), Vector(NoteText.Said(
      "used.none", "Discarded no adviser.", covers = false)))

  test("the intact Festival District offers no action"):
    val intact = Table.from(quietTable).turn(actor, Phase.Act)
      .pawn(actor, AncientCitySite)
      .edifice(district, EdificeSide.Intact, at = AncientCitySite)
      .favor(actor, 2).ready
    assert(!PhasePowerHarness.usableNow(intact).exists(
      _.power.id == SqualidDistrict.id))
