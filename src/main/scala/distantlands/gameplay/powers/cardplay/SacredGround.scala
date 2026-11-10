package oathdigital.gameplay.powers.cardplay

import oathdigital.gameplay.SacredGroundCard
import oathdigital.gameplay.actions.VisionRules
import oathdigital.gameplay.powerresolver._
import oathdigital.model._

/** Sacred Ground (E08's intact face), a persistent rule: "Players cannot play
  * Visions, except the Conspiracy, faceup unless their pawn is at this site."
  *
  * It binds every player whose pawn is elsewhere, the site's own ruler
  * included. Its ruined face is `DesecratedGroundRule`.
  * When the look-ahead hides a faceup placement because of it, it writes
  * "{Red} cannot play a Vision faceup."
  */
case object SacredGround extends ContributingPower:
  val edifice: EdificeId = SacredGroundCard.id
  val id: PowerId = SacredGroundCard.intact.power.id

  def source: RuleSourceRef = RuleSourceRef.GameRule(id.value)

  private val notes = NoteSet(id)
  notes.include(VisionPlay.notes)
  override def noteKeys: Vector[NoteKey] = notes.declared

  def contributions: Map[PowerWindow, Vector[Contribution]] = Map(
    PowerWindow.ActionCardPlayedFaceup ->
      Vector(Restriction((ctx, _) => blocked(ctx), VisionPlay.note(edifice))))

  private def blocked(ctx: PowerCtx): Option[OathViolation] = for
    vision <- VisionPlay.pending(ctx)
    if vision != VisionRules.Conspiracy
    site <- ctx.state.game.current.edificeSite(edifice, EdificeSide.Intact)
    if ctx.state.game.current.map.inPlay.contains(site)
    if !ctx.state.game.current.pawnSite(VisionPlay.player(ctx))
      .contains(site)
  yield VisionPlay.forbidden("Sacred Ground")
