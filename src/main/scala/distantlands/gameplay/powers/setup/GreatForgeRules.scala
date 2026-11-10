package distantlands.gameplay.powers.setup

import distantlands.catalog.{Edifice, EdificeFace, Locked, PrintedPower}
import distantlands.gameplay.powerresolver.{Contribution, ContributingPower, PowerCtx, Transform}
import distantlands.gameplay.powers.RelicDraws
import distantlands.model._

object GreatForgeCard extends Edifice(EdificeId("E06"), Suit.Nomad):
  object intact extends EdificeFace("Great Forge") with Locked:
    val power = PrintedPower(PowerId("edifice.e06.intact"),
      persistent = false, cost = Cost.free,
      text = "SETUP / **WHEN EXPLORED:** If you place your pawn here, " +
        "draw a relic from the relic deck and take it facedown.")
    val powers: Vector[PrintedPower] = Vector(power)
  object ruined extends EdificeFace("Broken Forge"):
    val power = PrintedPower(PowerId("edifice.e06.ruined"),
      persistent = false, cost = Cost.free,
      text = "SETUP / **WHEN EXPLORED:** If you place your pawn here, " +
        "discard all relics at sites in this region.")
    val powers: Vector[PrintedPower] = Vector(power)

/** E06, both faces (2026-09-21 Chronicle design, "Setup powers"). See
  * `MarketRule` for the `WhenExplored`/window-sharing rationale.
  */
sealed abstract class ForgeRule extends ContributingPower:
  def edifice: EdificeId
  protected def side: EdificeSide

  final def source: RuleSourceRef = RuleSourceRef.GameRule(id.value)

  private def at(ctx: PowerCtx): Option[(PlayerId, SiteId)] =
    for
      edificeSite <- ctx.state.game.current.edificeSite(edifice, side)
      placement <- EdificeSetupSupport.pawnPlacement(ctx)
      if placement._2 == edificeSite
    yield placement

  override def applicable(ctx: PowerCtx): Boolean = at(ctx).isDefined

  protected def build(ready: ReadyGame, actor: PlayerId, at: SiteId)
      : Either[OathViolation, Vector[CoreOperation]]

  /** The line this face writes after its effect. */
  protected def note(actor: PlayerId, at: SiteId): Note

  final def contributions: Map[PowerWindow, Vector[Contribution]] =
    val effect = Vector(Transform((ctx, ops) => at(ctx) match {
      case Some((actor, site)) => ops :+ BuildOps((ready, _) =>
        build(ready, actor, site)) :+ note(actor, site)
      case None => ops
    }))
    Map(PowerWindow.SetupPawnPlaced -> effect, PowerWindow.WhenExplored -> effect)

case object GreatForge extends ForgeRule:
  val id: PowerId = GreatForgeCard.intact.power.id

  val edifice: EdificeId = GreatForgeCard.id
  protected def side: EdificeSide = EdificeSide.Intact

  private val notes = NoteSet(id)
  notes.borrow(RelicDraws.drew)
  override def noteKeys: Vector[NoteKey] = notes.declared

  protected def note(actor: PlayerId, at: SiteId): Note =
    Note(id, RelicDraws.drewNote(PowerSourceRef.Card(edifice), actor))

  protected def build(ready: ReadyGame, actor: PlayerId, at: SiteId)
      : Either[OathViolation, Vector[CoreOperation]] =
    Right(RelicDraws.takeTop(ready, actor))

case object BrokenForge extends ForgeRule:
  val id: PowerId = GreatForgeCard.ruined.power.id

  /** Every relic at a site in play, in map order. */
  private def siteRelics(ready: ReadyGame): Vector[CardId] =
    val current = ready.game.current
    current.map.inPlay.flatMap(site => current.map.sites(site).relics.map(_.id))

  val edifice: EdificeId = GreatForgeCard.id
  protected def side: EdificeSide = EdificeSide.Ruined

  private val notes = NoteSet(id)
  notes.include(EdificeSetupSupport.notes)
  override def noteKeys: Vector[NoteKey] = notes.declared

  /** The relics at sites before the step that no site holds after it. */
  protected def note(actor: PlayerId, at: SiteId): Note = Note(id, states => for
    step <- states.previous
    gone = BrokenForge.siteRelics(step._1)
      .filterNot(BrokenForge.siteRelics(step._2).contains)
    if gone.nonEmpty
  yield EdificeSetupSupport.discarded(PowerSourceRef.Card(edifice),
    NoteArg.Cards(gone)))

  protected def build(ready: ReadyGame, actor: PlayerId, at: SiteId)
      : Either[OathViolation, Vector[CoreOperation]] =
    ready.game.current.map.regionOf(at).toRight(
      OathViolation.InvalidEventOrder(s"${at.value} is not in play")).map { region =>
      val current = ready.game.current
      current.map.inPlay.filter(s => current.map.regionOf(s).contains(region))
        .flatMap(s => current.map.sites(s).relics.map(relic => Discard.Relic(
          relic.id, PositionedLocation(Location.Site(s)), relic.tokens.secrets, actor)))
    }
