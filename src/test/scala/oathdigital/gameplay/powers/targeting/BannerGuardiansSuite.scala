package oathdigital.gameplay.powers.targeting

import oathdigital.gameplay.CampaignFixture.{RaidBoard, on, raidBoard}
import oathdigital.gameplay.actions.campaign.CampaignIds
import oathdigital.gameplay.powers.NoteText
import oathdigital.gameplay.powers.setup.{HallOfDebateCard, TomeGuardiansCard}
import oathdigital.gameplay.powers.travel.BrokenPeaksSite
import oathdigital.model._
import oathdigital.model.DecisionAnswer.ChooseOneAnswer
import oathdigital.testkit.{Journal, Outcomes}
import oathdigital.testkit.Table.{p1, p2}
import oathdigital.testkit.Unwrap.accepted

/** Hall of Debate and Tome Guardians (catalog batch 9, slice 2): a Raid by
  * an enemy of the intact face's ruler may not target the banner that
  * ruler holds. A Challenge is not a target. */
class BannerGuardiansSuite extends munit.FunSuite:
  import TargetingFixture._

  private val raid = ChooseOneAnswer(DecisionOptionRef.Button("raid"))
  private val peoplesFavor = DecisionOptionRef.Banner(Banner.PeoplesFavor)
  private val darkestSecret = DecisionOptionRef.Banner(Banner.DarkestSecret)

  enum Ruler:
    case Defender, Attacker, Bandits, Nobody

  /** The Raid's target options after the kind answer, and the whole walk's
    * events from the board it ran on. */
  private final case class RaidTargets(ready: ReadyGame,
      options: Vector[DecisionOption], events: Vector[OathEvent])

  /** A Raid on the rival, who holds both banners, with `edifice` on `side`
    * at the first site after the origin, ruled by `ruler`. */
  private def raidTargets(edifice: EdificeId, ruler: Ruler,
      side: EdificeSide = EdificeSide.Intact): RaidTargets =
    val RaidBoard(b, _) = raidBoard()
    val afterOrigin = b.ready.game.current.map.inPlay.find(_ != b.origin).get
    val held = on(b)(table =>
      val staged = table.edifice(edifice, side, at = afterOrigin)
      ruler match
        case Ruler.Defender => staged.warbandsAt(afterOrigin, b.rival, 2)
        case Ruler.Attacker => staged.warbandsAt(afterOrigin, b.actor, 2)
        case Ruler.Bandits => staged.bandits(afterOrigin, 2)
        case Ruler.Nobody => staged)
    val started = accepted(start(held.ready, ActionRef.Campaign, b.actor))
    val kind = accepted(rules.resolveWalker(started.state, b.actor,
      CampaignIds.kind, raid))
    RaidTargets(held.ready,
      optionsIncludingUnavailableAt(kind, ActionRef.Campaign),
      started.events ++ kind.events)

  private def available(targets: RaidTargets): Set[DecisionOptionRef] =
    targets.options.filter(_.available).map(_.ref).toSet

  test("Hall of Debate hides the ruler's People's Favor from an enemy's " +
      "Raid, with its reason, and leaves the Darkest Secret"):
    val targets = raidTargets(HallOfDebateCard.id, Ruler.Defender)
    assert(!available(targets).contains(peoplesFavor))
    assert(available(targets).contains(darkestSecret))
    targets.options.find(_.ref == peoplesFavor) match
      case Some(DecisionOption.Unavailable(_, OptionReason.Noted(power,
          note))) =>
        assertEquals(power, HallOfDebate.id)
        assertEquals(NoteText.sentence(HallOfDebate.noteKeys, note),
          s"${p2.value}'s People's Favor cannot be targeted.")
      case other => fail(s"expected the Hall's reason, got $other")

  test("Tome Guardians hides the ruler's Darkest Secret and leaves the " +
      "People's Favor"):
    val targets = raidTargets(TomeGuardiansCard.id, Ruler.Defender)
    assert(!available(targets).contains(darkestSecret))
    assert(available(targets).contains(peoplesFavor))

  test("the attacker ruling the edifice hides nothing from their own Raid"):
    val targets = raidTargets(HallOfDebateCard.id, Ruler.Attacker)
    assert(available(targets).contains(peoplesFavor))

  test("under bandits, or unruled, nobody holds the banner through the " +
      "edifice, so it stays a target"):
    assert(available(raidTargets(HallOfDebateCard.id, Ruler.Bandits))
      .contains(peoplesFavor))
    assert(available(raidTargets(TomeGuardiansCard.id, Ruler.Nobody))
      .contains(darkestSecret))

  test("the ruined faces protect nothing"):
    assert(available(raidTargets(HallOfDebateCard.id, Ruler.Defender,
      EdificeSide.Ruined)).contains(peoplesFavor))
    assert(available(raidTargets(TomeGuardiansCard.id, Ruler.Defender,
      EdificeSide.Ruined)).contains(darkestSecret))

  test("a ruler who does not hold the banner is not protected"):
    val RaidBoard(b, _) = raidBoard()
    val afterOrigin = b.ready.game.current.map.inPlay.find(_ != b.origin).get
    // The attacker holds the People's Favor; the defender rules the Hall.
    val held = on(b)(_.edifice(HallOfDebateCard.id, EdificeSide.Intact,
      at = afterOrigin).warbandsAt(afterOrigin, b.rival, 2)
      .peoplesFavor(Some(b.actor), favor = 3))
    val started = accepted(start(held.ready, ActionRef.Campaign, b.actor))
    val kind = accepted(rules.resolveWalker(started.state, b.actor,
      CampaignIds.kind, raid))
    // The attacker's own banner is never a target; the Darkest Secret is.
    assertEquals(optionsAt(kind, ActionRef.Campaign).toSet,
      Set[DecisionOptionRef](darkestSecret,
        DecisionOptionRef.Relic(raidBoard().relic)))

  test("the hidden option writes no line, and the Raid's journal replays"):
    val targets = raidTargets(HallOfDebateCard.id, Ruler.Defender)
    assertEquals(NoteText.said(HallOfDebate.id, HallOfDebate.noteKeys,
      targets.events), Vector.empty)
    val replayed = Outcomes.ready(accepted(Journal.evolved(rules,
      targets.ready, targets.events)))
    assertEquals(replayed.game.current.walkerPending.nonEmpty, true)

  // ---- Challenge ----

  test("a Challenge may name the banner: the guardians protect only a " +
      "Campaign's targets"):
    // p2 holds the People's Favor at Ancient City and rules Broken Peaks,
    // where the Hall of Debate stands.
    val staged = challengeTable.edifice(HallOfDebateCard.id,
      EdificeSide.Intact, at = BrokenPeaksSite)
      .warbandsAt(BrokenPeaksSite, p2, 2).ready
    assert(optionsAt(accepted(start(staged, ActionRef.Challenge, p1)),
      ActionRef.Challenge).contains(peoplesFavor))
