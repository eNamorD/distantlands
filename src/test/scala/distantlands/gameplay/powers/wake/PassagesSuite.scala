package oathdigital.gameplay.powers.wake

import oathdigital.gameplay.BanditCrownCard
import oathdigital.gameplay.cards.{AncientCitySite, BrokenPeaksSite}
import oathdigital.gameplay.powers.{NoteText, PhasePowerCatalog}
import oathdigital.gameplay.setup.FirstGameSetupFixture.catalog
import oathdigital.gameplay.walker.WalkerPowers
import oathdigital.model._
import oathdigital.model.OathState.Ready
import oathdigital.testkit.{ActPowerHarness, Look, MenuPlay, Outcomes,
  PhasePowerHarness, Table}
import oathdigital.testkit.Table.{p1, p2}
import oathdigital.testkit.Unwrap.accepted

/** Hidden Passages and Sinking Passages (catalog batch 9, slice 6): WAKE,
  * with the pawn at the face's site, place it on any other site; the intact
  * face needs the site's ruler's consent, the ruined face burns a favor. */
class PassagesSuite extends munit.FunSuite:
  private val passages = HiddenPassagesCard.id
  private val source = DecisionOptionRef.Edifice(passages)
  private val home = AncientCitySite.id
  private val peaks = BrokenPeaksSite.id
  private val rules = ActPowerHarness.rules()
  private val covers = ConsentKey(HiddenPassages.id, p1, "power.use", source)

  /** p1's Wake with 2 favor, the Passages on `side` at Ancient City, p1's
    * pawn at `pawnAt`, and `ruled` settling who rules Ancient City. */
  private def staged(side: EdificeSide, pawnAt: SiteId = home,
      ruled: Table => Table = identity): ReadyGame =
    ruled(Table.start.turn(p1, Phase.Wake).pawn(p1, pawnAt).favor(p1, 2)
      .edifice(passages, side, at = home)).ready

  private def use(ready: ReadyGame, power: PowerId) = rules.startWalker(
    Ready(MenuPlay.withoutMenu(ready)), ActionRef.UsePower(power), p1,
    Vector(source))

  private def choose(state: OathState, decisionId: String, site: SiteId) =
    rules.resolveWalker(state, p1, decisionId,
      DecisionAnswer.ChooseOneAnswer(DecisionOptionRef.Site(site)))

  private def offered(ready: ReadyGame, power: PowerId): Boolean =
    PhasePowerHarness.menuPowers(catalog, ready, p1,
      PhasePowerCatalog.default, WalkerPowers.empty)
      .exists(_.power.id == power)

  private def menuOption(ready: ReadyGame, power: PowerId)(
      using munit.Location): DecisionOption =
    MenuPlay.menuOptionFor(catalog, ready, ActionRef.UsePower(power),
      Vector(source))

  // ---- Sinking Passages ----

  test("Sinking Passages burns 1 favor and places the pawn at the chosen " +
      "site, with its line"):
    val ready = staged(EdificeSide.Ruined)
    assert(offered(ready, SinkingPassages.id))
    val parked = accepted(use(ready, SinkingPassages.id))
    assertEquals(Look(Outcomes.ready(parked.state)).favor(p1), 1)
    val done = accepted(choose(parked.state, SinkingPassages.decisionId,
      peaks))
    assertEquals(Look(Outcomes.ready(done.state)).pawn(p1), peaks)
    assertEquals(NoteText.said(SinkingPassages, done.events), Vector(
      NoteText.Said(NoteKey.Used, s"${p1.value} placed at ${peaks.value}.",
        covers = false)))
    assertEquals(ActPowerHarness.replayed(rules, ready,
      parked.events ++ done.events), Outcomes.ready(done.state))

  test("Sinking Passages is unusable with the pawn elsewhere, with no " +
      "favor, and once per turn"):
    assert(!offered(staged(EdificeSide.Ruined, pawnAt = peaks),
      SinkingPassages.id))
    assert(!offered(Table.from(staged(EdificeSide.Ruined)).favor(p1, 0).ready,
      SinkingPassages.id))
    val parked = accepted(use(staged(EdificeSide.Ruined), SinkingPassages.id))
    val once = Outcomes.ready(accepted(choose(parked.state,
      SinkingPassages.decisionId, peaks)).state)
    assert(once.game.current.turn.usedPowers.contains(PowerUseRef(
      PowerTiming.Wake, PowerSourceRef.Card(passages), SinkingPassages.id)))

  // ---- Hidden Passages ----

  test("Hidden Passages at an unruled site asks nothing and places the pawn"):
    val ready = staged(EdificeSide.Intact)
    assert(menuOption(ready, HiddenPassages.id).available)
    val parked = accepted(use(ready, HiddenPassages.id))
    val done = accepted(choose(parked.state, HiddenPassages.decisionId, peaks))
    assertEquals(Look(Outcomes.ready(done.state)).pawn(p1), peaks)
    assertEquals(Look(Outcomes.ready(done.state)).favor(p1), 2)
    assertEquals(NoteText.said(HiddenPassages, done.events), Vector(
      NoteText.Said(NoteKey.Used, s"${p1.value} placed at ${peaks.value}.",
        covers = false)))

  test("under another player's rule it is askable of the ruler, and its " +
      "start is refused until a grant is remembered"):
    val ready = staged(EdificeSide.Intact, ruled = _.warbandsAt(home, p2, 1))
    menuOption(ready, HiddenPassages.id) match
      case DecisionOption.Unavailable(_, ask: OptionReason.Askable) =>
        assertEquals(ask.power, HiddenPassages.id)
        assertEquals(ask.note, HiddenPassages.needsConsent(HiddenPassages.card))
        assertEquals(ask.asks, Vector(ConsentAsk(p2,
          Vector(HiddenPassages.id))))
        assertEquals(ask.covers, Vector(covers))
      case other => fail(s"expected an askable option, got $other")
    assert(use(ready, HiddenPassages.id).isLeft)
    val granted = ready.copy(consent = ConsentState(
      answers = Map(covers -> ConsentAnswer.Granted.by(p2)),
      turn = Some(ConsentTurn.of(ready))))
    assert(menuOption(granted, HiddenPassages.id).available)
    val parked = accepted(use(granted, HiddenPassages.id))
    val done = accepted(choose(parked.state, HiddenPassages.decisionId, peaks))
    assertEquals(Look(Outcomes.ready(done.state)).pawn(p1), peaks)

  test("a remembered refusal leaves it unavailable with the plain reason"):
    val ready = staged(EdificeSide.Intact, ruled = _.warbandsAt(home, p2, 1))
    val refused = ready.copy(consent = ConsentState(
      answers = Map(covers -> ConsentAnswer.Refused.by(p2)),
      turn = Some(ConsentTurn.of(ready))))
    assertEquals(menuOption(refused, HiddenPassages.id),
      DecisionOption.Unavailable(DecisionOption.Edifice(source),
        OptionReason.Noted(HiddenPassages.id,
          HiddenPassages.needsConsent(HiddenPassages.card))))

  test("under bandits nobody may consent, and the ruler uses it freely"):
    assertEquals(menuOption(staged(EdificeSide.Intact,
      ruled = _.bandits(home, 1)), HiddenPassages.id),
      DecisionOption.Unavailable(DecisionOption.Edifice(source),
        OptionReason.Noted(HiddenPassages.id,
          HiddenPassages.banditsRule(HiddenPassages.card))))
    assert(menuOption(staged(EdificeSide.Intact,
      ruled = _.warbandsAt(home, p1, 1)), HiddenPassages.id).available)

  test("under bandits the Crown's holder uses it freely, and anyone else " +
      "asks the holder"):
    assert(menuOption(staged(EdificeSide.Intact,
      ruled = _.bandits(home, 1).relic(p1, BanditCrownCard)),
      HiddenPassages.id).available)
    menuOption(staged(EdificeSide.Intact,
        ruled = _.bandits(home, 1).relic(p2, BanditCrownCard)),
        HiddenPassages.id) match
      case DecisionOption.Unavailable(_, OptionReason.Askable(_, note, asks,
          _)) =>
        assertEquals(note, HiddenPassages.needsConsent(HiddenPassages.card))
        assertEquals(asks, Vector(ConsentAsk(p2, Vector(HiddenPassages.id))))
      case other => fail(s"not askable: $other")

  test("with the pawn elsewhere the intact face is out of reach"):
    assert(!offered(staged(EdificeSide.Intact, pawnAt = peaks),
      HiddenPassages.id))
