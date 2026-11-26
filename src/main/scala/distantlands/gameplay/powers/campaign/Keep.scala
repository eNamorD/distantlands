package distantlands.gameplay.powers.campaign

import distantlands.catalog.{Denizen, ModifierMark, PrintedPower, SiteOnly}
import distantlands.gameplay.actions.campaign.CampaignSetup
import distantlands.model._

object KeepCard extends Denizen(DenizenId("5"), "Keep", Suit.Order) with SiteOnly:
  val power = PrintedPower(PowerId("denizen.keep"),
    persistent = false, cost = Cost.free,
    modifier = Some(ModifierMark.DefensePlan),
    text = "If defeated in a raid, after the attacker takes what they " +
      "targeted, place your pawn at this site and end the action. " +
      "_Do not burn favor, move pawn, etc._")
  val powers: Vector[PrintedPower] = Vector(power)

/** Keep (card 5, site-only), a defender's battle plan: "If defeated in a
  * raid, after the attacker takes what they targeted, place your pawn at
  * this site and end the action. Do not burn favor, move pawn, etc."
  *
  * The card prints the defender's blue braid and crest, so it is a plan the
  * defender chooses, not a rule that applies by itself (card audit of
  * 2026-11-26). It is offered in a Raid to the defender of Keep's site: its
  * ruler, or for a site the Empire holds the Chancellor, on every turn
  * (`CampaignSetup.defenderAt`, ruling R14). It costs nothing.
  *
  * Chosen, it applies once the defender is defeated and the battle's losses
  * are made. The takes stand. The Raid's consequences (the facedown
  * discards, the set-aside relics, the half-favor burn) are dropped, and the
  * relocation question is replaced: the defender's pawn moves to Keep's
  * site, or stays when it is already there. Nothing else in the Raid
  * follows; the effects at the Campaign's end still run.
  *
  * Its line, "Ended the Raid with {Blue}'s pawn at {site}.", restates a
  * pawn move and covers it, so the Campaign's closing line does not say the
  * pawn was sent there.
  */
case object Keep extends DenizenPlan(KeepCard, KeepCard.power):
  private val notes = NoteSet(id)
  /** "Ended the Raid with {Blue}'s pawn at {site}." */
  val ended: NoteKey = notes.key("ended", Vector(
    NotePart.Text("Ended the Raid with "), NotePart.Arg(0),
    NotePart.Text("'s pawn at "), NotePart.Arg(1), NotePart.Text(".")))

  override def noteKeys: Vector[NoteKey] = notes.declared

  def sides: Set[CampaignPlanSide] = Set(CampaignPlanSide.Defender)

  def plan(context: PlanContext): Option[CampaignPlanOffer] =
    context.denizen(cardId)
      .filter(_ => context.setup.kind == CampaignKind.Raid)
      .map(source => CampaignPlanOffer(source,
        "Keep: if defeated, lose only what was targeted and move here",
        Vector.empty, Vector.empty))

  override def wrapping: Map[PowerWindow, BattlePlan.Wrap] = Map(
    PowerWindow.CampaignRaidConsequences -> ((_, use, children) =>
      kept(use).fold(children)(_ => Vector.empty)),
    PowerWindow.CampaignRaidRelocation -> ((_, use, children) =>
      kept(use).fold(children)((defender, site) =>
        Vector(placed(defender, site)))))

  /** The Raid's defeated defender and Keep's site, when that defender
    * chose Keep and still defends its site. */
  private def kept(use: PlanUse): Option[(PlayerId, SiteId)] = for
    result <- use.result if result.attackerWins
    defender <- result.defender.player
    site <- use.ready.game.current.faceUpDenizenSite(cardId)
    if CampaignSetup.defenderAt(use.ready, site)
      .contains(CampaignDefender.Player(defender))
  yield (defender, site)

  /** The defender's pawn placed at `site`, unless it stands there, then the
    * line. The line covers the move it restates. */
  private def placed(defender: PlayerId, site: SiteId): Operation =
    Branch((ready, _) => {
      val from = ready.game.current.pawnSite(defender).filter(_ != site)
      from.fold(Vector[Operation](line(defender, site, covers = false)))(
        origin => Vector[Operation](
          BuildOps((_, _) => Right(Vector[CoreOperation](Move(
            Piece.Pawn(defender), PositionedLocation(Location.Site(origin)),
            PositionedLocation(Location.Site(site)))))),
          line(defender, site, covers = true)))
    })

  private def line(defender: PlayerId, site: SiteId, covers: Boolean)
      : Note = Note(id, _ => Some(ended(PowerSourceRef.Card(cardId),
    NoteArg.Player(defender), NoteArg.Site(site))), covers)
