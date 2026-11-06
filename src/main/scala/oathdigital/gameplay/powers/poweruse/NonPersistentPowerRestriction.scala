package oathdigital.gameplay.powers.poweruse

import oathdigital.catalog.ExecutableCatalog
import oathdigital.gameplay.SiteRulers
import oathdigital.gameplay.powerresolver.{Contribution, ContributingPower,
  PowerUse, PowerUseRefusal, PowerUseRestriction}
import oathdigital.gameplay.powers.action.GiantPythonCard
import oathdigital.gameplay.powers.travel.ForbiddenForestCard
import oathdigital.model._

/** A persistent rule that guards some cards against non-persistent powers
  * and placements (catalog batch 10, slice 1): Giant Python's "Other cards
  * at this site cannot be used for non-persistent powers or placing
  * [favor] / [secret]" and Bandit Forest's "Imperials cannot place
  * [favor]/[secret] on cards ruled by bandits or use their non-persistent
  * powers".
  *
  * For a player it `binds`, it refuses a non-persistent use of a guarded
  * card, a use whose cost places on one ([[Placements.byUse]]), and a
  * placement onto one ([[Placements.onto]]). It names no enemies, so no
  * consent lifts it (consent design, ruling 1). It writes no line.
  *
  * `describe` names the guarded cards in the operation reason; the two
  * plain texts are the hover reasons.
  */
final class NonPersistentPowerRestriction(val id: PowerId, card: CardId,
    code: String, describe: String, usePlainText: String,
    placingPlainText: String, guarded: (ReadyGame, CardId) => Boolean,
    binds: (ReadyGame, PlayerId) => Boolean)
    extends ContributingPower with PowerUseRestriction:
  def source: RuleSourceRef = RuleSourceRef.GameRule(id.value)
  def contributions: Map[PowerWindow, Vector[Contribution]] = Map.empty
  private val notes = NoteSet(id)
  override def noteKeys: Vector[NoteKey] = notes.declared
  /** Why a non-persistent use of a guarded card is refused. */
  val usePlain: NoteKey = notes.key("no-powers-here", Vector(NotePart.Text(
    usePlainText)))
  /** Why a placement on a guarded card is refused. */
  val placingPlain: NoteKey = notes.key("no-placing-here", Vector(
    NotePart.Text(placingPlainText)))

  override def powerUseRestrictions: Vector[PowerUseRestriction] =
    Vector(this)
  override def operationRestrictions(catalog: ExecutableCatalog)
      : Vector[OperationRestriction] =
    Vector(placing)

  private val cardSource = PowerSourceRef.Card(card)

  private def refused(reasonCode: String, player: PlayerId): OperationReason =
    OperationReason(reasonCode, s"$describe cannot be used by " +
      s"${player.value}", OperationReasonKind.Impossible)

  def refusal(catalog: ExecutableCatalog, ready: ReadyGame, use: PowerUse)
      : Option[PowerUseRefusal] =
    for
      used <- use.card
      if binds(ready, use.user) && guarded(ready, used)
      key <- if !use.persistent then Some(usePlain)
        else Placements.byUse(ready, use).map(_ => placingPlain)
    yield PowerUseRefusal(id, key(cardSource), refused(code, use.user))

  private object placing extends OperationRestriction:
    def reason(ready: ReadyGame, operation: CoreOperation)
        : Option[OperationReason] =
      Placements.onto(ready, operation).filter((player, placed) =>
        binds(ready, player) && guarded(ready, placed)).map(placed =>
        refused(s"$code-placing", placed._1))

object NonPersistentPowerRestriction:
  /** Whether `card` stands at `site`. */
  private def standsAt(ready: ReadyGame, site: SiteId, card: CardId)
      : Boolean =
    ready.game.current.map.sites.get(site).exists(_.denizens.exists(
      _.id == card))

  /** Giant Python's restriction (card 186, site-only): the other cards at
    * the site where Giant Python stands now, so it moves with it. It binds
    * every player, its ruler included. */
  val giantPython: NonPersistentPowerRestriction =
    NonPersistentPowerRestriction(GiantPythonCard.restriction.id,
      GiantPythonCard.id, "giant-python", "cards at Giant Python's site",
      "Cards at its site cannot be used for non-persistent powers.",
      "Cannot place favor or secrets on cards at its site.",
      guarded = (ready, card) => card != GiantPythonCard.id &&
        ready.game.current.faceUpDenizenSite(GiantPythonCard.id).exists(
          standsAt(ready, _, card)),
      binds = (_, _) => true)

  /** Bandit Forest (E29, ruined): the cards at sites bandits rule, while
    * the ruined face is on the map. It binds Imperial players only. */
  val banditForest: NonPersistentPowerRestriction =
    NonPersistentPowerRestriction(ForbiddenForestCard.ruined.power.id,
      ForbiddenForestCard.id, "bandit-forest", "cards ruled by bandits",
      "Imperials cannot use the non-persistent powers of cards ruled by " +
        "bandits.",
      "Imperials cannot place favor or secrets on cards ruled by bandits.",
      guarded = (ready, card) => ready.game.current.edificeSite(
        ForbiddenForestCard.id, EdificeSide.Ruined).isDefined &&
        ready.game.current.map.inPlay.exists(site =>
          SiteRulers.rulerOf(ready, site).contains(SiteRuler.Bandits) &&
            standsAt(ready, site, card)),
      binds = Imperials.isImperial)
