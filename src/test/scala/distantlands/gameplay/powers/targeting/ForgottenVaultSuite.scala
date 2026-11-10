package distantlands.gameplay.powers.targeting

import distantlands.gameplay.powers.search.CrackedHornCard
import distantlands.gameplay.CampaignFixture.{RaidBoard, on, raidBoard}
import distantlands.gameplay.actions.VisionRules
import distantlands.gameplay.actions.campaign.CampaignIds
import distantlands.gameplay.cards.{AncientCitySite, BrokenPeaksSite}
import distantlands.gameplay.powers.{NoteText, WalkerPowerCatalog}
import distantlands.gameplay.setup.FirstGameSetupFixture.catalog
import distantlands.gameplay.walker.{ProcedureWalker, WalkerOutcome,
  WalkerPowers}
import distantlands.model._
import distantlands.model.DecisionAnswer.ChooseOneAnswer
import distantlands.testkit.Table
import distantlands.testkit.Table.{p1, p2, p3}
import distantlands.testkit.Unwrap.{accepted, as, present}

class ForgottenVaultSuite extends munit.FunSuite:
  import TargetingFixture._

  private val vault = ForgottenVaultCard.id
  /** The Conspiracy tests' Vault site. */
  private val vaultSite = BrokenPeaksSite.id
  private val raid = ChooseOneAnswer(DecisionOptionRef.Button("raid"))
  private val power = ForgottenVault

  private def notes(events: Vector[OathEvent]): Vector[NoteText.Said] =
    NoteText.said(power.id, power.noteKeys, events)

  /** `table` with the Vault at `site`, which `ruler` rules with 2 warbands
    * (`None` leaves the site to nobody). */
  private def vaultAt(table: Table, ruler: Option[PlayerId],
      site: SiteId = vaultSite): Table =
    val placed = table.denizen(vault, at = site)
    ruler.fold(placed)(placed.warbandsAt(site, _, 2))

  // ---- Raid ----

  enum Ruler:
    case Defender, Attacker, Bandits

  /** The Raid's defender, their faceup relic, its target options and the
    * journal of the kind answer. */
  private final case class RaidTargets(defender: PlayerId, relic: RelicId,
      options: Vector[DecisionOptionRef], events: Vector[OathEvent])

  /** Starts a Raid on the defender with the Vault's site, the first site
    * after the origin, ruled by `ruler`, and answers the kind. */
  private def raidTargets(ruler: Ruler): RaidTargets =
    val RaidBoard(b, relic) = raidBoard()
    val afterOrigin = b.ready.game.current.map.inPlay.find(_ != b.origin).get
    val held = on(b)(vaultAt(_, ruler match
      case Ruler.Defender => Some(b.rival)
      case Ruler.Attacker => Some(b.actor)
      case Ruler.Bandits => None, afterOrigin))
    val started = accepted(start(held.ready, ActionRef.Campaign, b.actor))
    val kind = accepted(rules.resolveWalker(started.state, b.actor,
      CampaignIds.kind, raid))
    RaidTargets(b.rival, relic, optionsAt(kind, ActionRef.Campaign),
      kind.events)

  private val conspiracy = VisionRules.Conspiracy

  private val crackedHorn = CrackedHornCard.id

  /** The target options a Conspiracy walk parked on (empty when nothing was
    * asked), and its journal. */
  private final case class ConspiracyWalk(options: Vector[DecisionOptionRef],
      events: Vector[OathEvent])

  /** p1 plays Conspiracy at Ancient City, where p2 stands holding Cracked
    * Horn on `side`, and the People's Favor when `banner`. The Vault stands at
    * Broken Peaks, which p2 rules. */
  private def conspiracyTargets(banner: Boolean,
      side: Orientation = Orientation.FaceUp): ConspiracyWalk =
    val staged = Table.start.pawn(p1, at = AncientCitySite)
      .pawn(p2, at = AncientCitySite).hand(p1, conspiracy)
      .relic(p2, crackedHorn, facedown = side == Orientation.FaceDown)
    val bannered =
      if banner then staged.peoplesFavor(Some(p2), favor = 1) else staged
    val ready = vaultAt(bannered, Some(p2)).ready
    val hook = CardPlayedFaceup(conspiracy, p1,
      RuleSourceRef.Adviser(p1, conspiracy))
    val powers = WalkerPowers.selected(WalkerPowerCatalog.default(catalog),
      Vector.empty)
    accepted(ProcedureWalker.advance(ready, hook, None, powers)) match
      case WalkerOutcome.Parked(pending, events) =>
        val decide = present(ProcedureWalker.parkedDecide(ready, hook, pending,
          powers), "the target decision")
        ConspiracyWalk(
          as[DecisionQuery.ChooseOne](decide.query).options.map(_.ref), events)
      case WalkerOutcome.Finished(_, events) =>
        ConspiracyWalk(Vector.empty, events)

  test("a Raid may not target the ruler's relics, but may target their " +
      "banners"):
    val targets = raidTargets(Ruler.Defender)
    assert(!targets.options.contains(DecisionOptionRef.Relic(targets.relic)),
      targets.options.toString)
    assertEquals(targets.options.toSet, Set[DecisionOptionRef](
      DecisionOptionRef.Banner(Banner.PeoplesFavor),
      DecisionOptionRef.Banner(Banner.DarkestSecret)))
    assertEquals(notes(targets.events), Vector.empty)

  test("the Vault's ruler may target an enemy's relics"):
    val targets = raidTargets(Ruler.Attacker)
    assert(targets.options.contains(DecisionOptionRef.Relic(targets.relic)))
    assertEquals(notes(targets.events), Vector.empty)

  test("a Vault ruled by bandits protects nothing"):
    val targets = raidTargets(Ruler.Bandits)
    assert(targets.options.contains(DecisionOptionRef.Relic(targets.relic)))

  // ---- Conspiracy ----

  test("Conspiracy may take the ruler's relic as well as their banner"):
    val walk = conspiracyTargets(banner = true)
    assertEquals(walk.options.toSet, Set[DecisionOptionRef](
      DecisionOptionRef.RelicSlot(p2, 0),
      DecisionOptionRef.Banner(Banner.PeoplesFavor)))
    assertEquals(notes(walk.events), Vector.empty)

  test("Conspiracy may take the ruler's facedown relic too"):
    val walk = conspiracyTargets(banner = true, Orientation.FaceDown)
    assert(walk.options.contains(DecisionOptionRef.RelicSlot(p2, 0)),
      walk.options.toString)
    assertEquals(notes(walk.events), Vector.empty)

  test("a Conspiracy against the ruler's relic alone asks for it and writes " +
      "nothing"):
    val walk = conspiracyTargets(banner = false)
    assertEquals(walk.options, Vector[DecisionOptionRef](
      DecisionOptionRef.RelicSlot(p2, 0)))
    assertEquals(notes(walk.events), Vector.empty)

  test("an Empire Vault shields a Citizen's relics from an Exile's Raid"):
    // p1 raids as an Exile; p3 is the Chancellor and p2, the defender, a
    // Citizen. The Empire holds the Vault's site.
    val RaidBoard(b, relic) = raidBoard()
    val afterOrigin = b.ready.game.current.map.inPlay.find(_ != b.origin).get
    val held = on(b)(table => vaultAt(table.imperial(p3, p2), Some(p3),
      afterOrigin))
    val started = accepted(start(held.ready, ActionRef.Campaign, b.actor))
    val kind = accepted(rules.resolveWalker(started.state, b.actor,
      CampaignIds.kind, raid))
    val options = optionsAt(kind, ActionRef.Campaign)
    assert(!options.contains(DecisionOptionRef.Relic(relic)),
      options.toString)
    assert(options.contains(DecisionOptionRef.Banner(Banner.PeoplesFavor)))
    val hidden = optionsIncludingUnavailableAt(kind, ActionRef.Campaign)
      .collectFirst:
        case DecisionOption.Unavailable(option, reason)
            if option.ref == DecisionOptionRef.Relic(relic) => reason
    assertEquals(hidden, Some(OptionReason.Noted(power.id,
      ForgottenVault.shielded(PowerSourceRef.Card(vault),
        NoteArg.Player(p2)))))
