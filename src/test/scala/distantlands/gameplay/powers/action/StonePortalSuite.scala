package distantlands.gameplay.powers.action

import distantlands.gameplay.cards.{AncientCitySite, BrokenPeaksSite}
import distantlands.gameplay.powers.{NoteText, PowerFixture}
import distantlands.model._
import distantlands.model.OathState.Ready
import distantlands.testkit.{Look, MenuPlay, PhasePowerHarness, Refusals, Table}
import distantlands.testkit.ActPowerHarness.usableIds
import distantlands.testkit.Outcomes.ready
import distantlands.testkit.Unwrap.accepted

/** Stone Portal (catalog batch 9, slice 6): from its site, place your pawn
  * elsewhere; from a site you rule, place it at the Portal. */
class StonePortalSuite extends munit.FunSuite:
  import PowerFixture._
  import PhasePowerHarness.{awaits, backToActing, ops, replayed, rules}

  private val portal = StonePortalCard.id
  private val source = DecisionOptionRef.Edifice(portal)
  private val home = AncientCitySite.id
  private val peaks = BrokenPeaksSite.id

  /** p1's Act with 2 faceup secrets, the intact Portal at Ancient City and
    * p1's pawn at `pawnAt`; `ruled` settles who rules Ancient City. */
  private def staged(pawnAt: SiteId = home, ruled: Table => Table = identity,
      side: EdificeSide = EdificeSide.Intact): ReadyGame =
    ruled(Table.from(quietTable).turn(actor, Phase.Act).pawn(actor, pawnAt)
      .edifice(portal, side, at = home).secrets(actor, 2)).ready

  private def use(state: ReadyGame) = rules.startWalker(
    Ready(MenuPlay.withoutMenu(state)), ActionRef.UsePower(StonePortal.id),
    actor, Vector(source))

  private def choose(state: OathState, site: SiteId) = rules.resolveWalker(
    state, actor, StonePortal.decisionId,
    DecisionAnswer.ChooseOneAnswer(DecisionOptionRef.Site(site)))

  test("at its site, the user pays a secret and places their pawn at a " +
      "chosen other site, with the line, and the journal replays"):
    val start = staged()
    assert(usableIds(start).contains(StonePortal.id))
    val parked = accepted(use(start))
    assert(awaits(parked, StonePortal.decisionId))
    assertEquals(Look(ready(parked.state)).tokensOn(portal), Tokens(0, 1))
    val done = accepted(choose(parked.state, peaks))
    assert(backToActing(done))
    assertEquals(Look(ready(done.state)).pawn(actor), peaks)
    assertEquals(NoteText.said(StonePortal, done.events), Vector(
      NoteText.Said(NoteKey.Used, s"${actor.value} placed at ${peaks.value}.",
        covers = false)))
    assertEquals(replayed(start, parked.events ++ done.events),
      Right(done.state))

  test("the Portal's own site is not offered"):
    val parked = accepted(use(staged()))
    assertEquals(choose(parked.state, home).left.toOption,
      Some(Refusals.notOffered(StonePortal.decisionId)))

  test("from a site the user rules, the pawn is placed at the Portal " +
      "without a question"):
    val start = staged(pawnAt = peaks, ruled = _.warbandsAt(home, actor, 1))
    assert(usableIds(start).contains(StonePortal.id))
    val done = accepted(use(start))
    assert(backToActing(done))
    assertEquals(Look(ready(done.state)).pawn(actor), home)
    assertEquals(ops(done.events).count {
      case Move(Piece.Pawn(_), _, _, _, _) => true
      case _ => false
    }, 1)

  test("from elsewhere without ruling the Portal's site it is out of " +
      "reach, and the ruined face offers nothing"):
    assert(!usableIds(staged(pawnAt = peaks)).contains(StonePortal.id))
    assert(!usableIds(staged(side = EdificeSide.Ruined))
      .contains(StonePortal.id))
