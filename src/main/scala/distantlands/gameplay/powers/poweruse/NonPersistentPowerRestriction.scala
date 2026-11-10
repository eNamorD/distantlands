package oathdigital.gameplay.powers.poweruse

import oathdigital.catalog.ExecutableCatalog
import oathdigital.gameplay.{BanditCrownRule, SiteRulers}
import oathdigital.gameplay.operations.PowerUseRules
import oathdigital.gameplay.powerresolver.{Contribution, ContributingPower,
  PowerUse, PowerUseRefusal, PowerUseRestriction, Waiver}
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
  * ruler's consent lifts it (consent design, ruling 1); a `consent` names
  * who else may (Bandit Forest's Crown holder). It writes no line.
  *
  * `describe` names the guarded cards in the operation reason; the two
  * plain texts are the hover reasons.
  */
final class NonPersistentPowerRestriction(val id: PowerId, card: CardId,
    code: String, describe: String, usePlainText: String,
    placingPlainText: String, guarded: (ReadyGame, CardId) => Boolean,
    binds: (ReadyGame, PlayerId) => Boolean,
    consent: Option[NonPersistentPowerRestriction.Consent] = None)
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
  private val cardSource = PowerSourceRef.Card(card)
  /** The reasons while `consent`'s granter may be asked: (use, placing). */
  private val asked: Option[(NoteKey, NoteKey)] = consent.map(rule =>
    notes.key("no-powers-here-consent", Vector(NotePart.Text(
      rule.useAskedText))) ->
      notes.key("no-placing-here-consent", Vector(NotePart.Text(
        rule.placingAskedText))))
  /** Why a non-persistent use is refused, while its granter may be asked. */
  def useAsked: NoteKey = asked.map(_._1).getOrElse(usePlain)

  override val waiver: Option[Waiver] = consent.map(rule =>
    Waiver(rule.granter, cardSource, (_, _) => useAsked(cardSource)))

  override def powerUseRestrictions: Vector[PowerUseRestriction] =
    Vector(this)
  override def operationRestrictions(catalog: ExecutableCatalog)
      : Vector[OperationRestriction] =
    Vector(placing)

  private def refused(reasonCode: String, player: PlayerId, ready: ReadyGame,
      target: DecisionOptionRef, plain: NoteKey): OperationReason =
    val askedKey = if plain == usePlain then asked.map(_._1)
      else asked.map(_._2)
    OperationReason(reasonCode, s"$describe cannot be used by " +
      s"${player.value}", OperationReasonKind.Impossible,
      // With nobody to ask, the refusal carries no waiver, as before the
      // Bandit Crown.
      consent.zip(askedKey).flatMap((rule, key) =>
        rule.granter(ready).map(granter => OperationWaiver(id, Some(granter),
          ConsentKey(id, player, PowerUseRules.useKey, target),
          key(cardSource), plain(cardSource)))))

  def refusal(catalog: ExecutableCatalog, ready: ReadyGame, use: PowerUse)
      : Option[PowerUseRefusal] =
    for
      used <- use.card
      if binds(ready, use.user) && guarded(ready, used)
      key <- if !use.persistent then Some(usePlain)
        else Placements.byUse(ready, use).map(_ => placingPlain)
    yield PowerUseRefusal(id, key(cardSource), refused(code, use.user, ready,
      PowerUseRules.target(use), key))

  private object placing extends OperationRestriction:
    def reason(ready: ReadyGame, operation: CoreOperation)
        : Option[OperationReason] =
      Placements.onto(ready, operation).filter((player, placed) =>
        binds(ready, player) && guarded(ready, placed)).map(placed =>
        refused(s"$code-placing", placed._1, ready,
          PowerUseRules.cardRef(placed._2), placingPlain))

object NonPersistentPowerRestriction:
  /** Who may waive the restriction for a player it binds, and the two
    * reasons shown while they may be asked. */
  final case class Consent(granter: ReadyGame => Option[PlayerId],
      useAskedText: String, placingAskedText: String)

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

  /** Whether Bandit Forest, the Forbidden Forest's ruined face, is on the
    * map. */
  private def forestRuined(ready: ReadyGame): Boolean =
    ready.game.current.edificeSite(ForbiddenForestCard.id, EdificeSide.Ruined)
      .isDefined

  /** Bandit Forest (E29, ruined): the cards at sites bandits rule, while
    * the ruined face is on the map. It binds Imperial players only. The
    * Bandit Crown's holder may waive it for an Imperial other than
    * themselves (catalog batch 11, ruling 6). */
  val banditForest: NonPersistentPowerRestriction =
    NonPersistentPowerRestriction(ForbiddenForestCard.ruined.power.id,
      ForbiddenForestCard.id, "bandit-forest", "cards ruled by bandits",
      "Imperials cannot use the non-persistent powers of cards ruled by " +
        "bandits.",
      "Imperials cannot place favor or secrets on cards ruled by bandits.",
      guarded = (ready, card) => forestRuined(ready) &&
        ready.game.current.map.inPlay.exists(site =>
          SiteRulers.rulerOf(ready, site).contains(SiteRuler.Bandits) &&
            standsAt(ready, site, card)),
      binds = Imperials.isImperial,
      consent = Some(Consent(ready =>
        BanditCrownRule.holder(ready).filter(_ => forestRuined(ready)),
        "Imperials cannot use the non-persistent powers of cards ruled by " +
          "bandits without the Bandit Crown holder's consent.",
        "Imperials cannot place favor or secrets on cards ruled by bandits " +
          "without the Bandit Crown holder's consent.")))
