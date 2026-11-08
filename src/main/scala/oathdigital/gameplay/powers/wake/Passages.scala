package oathdigital.gameplay.powers.wake

import oathdigital.catalog.{Edifice, EdificeFace, ExecutableCatalog, Locked,
  PrintedPower}
import oathdigital.gameplay.SiteRulers
import oathdigital.gameplay.operations.PowerUseRules
import oathdigital.gameplay.powerresolver.{ContributingPower, Contribution,
  PhasePower, PowerUse, PowerUseRefusal, PowerUseRestriction, Waiver}
import oathdigital.gameplay.powers.action.PawnMoves
import oathdigital.model._

object HiddenPassagesCard extends Edifice(EdificeId("E25"), Suit.Hearth):
  object intact extends EdificeFace("Hidden Passages") with Locked:
    val power = PrintedPower(PowerId("edifice.e25.intact"),
      persistent = false, cost = Cost.free,
      text = "**WAKE:** If your pawn is at this site and its ruler " +
        "consents, place your pawn on any site.")
    val powers: Vector[PrintedPower] = Vector(power)
  object ruined extends EdificeFace("Sinking Passages"):
    val power = PrintedPower(PowerId("edifice.e25.ruined"),
      persistent = false, cost = Cost.free,
      text = "**WAKE:** If your pawn is at this site, you may burn " +
        "[favor-burnt] to place your pawn on any site.")
    val powers: Vector[PrintedPower] = Vector(power)

/** A WAKE power of the Passages edifice (E25): with the user's pawn at the
  * face's site, place it on any other site (catalog batch 9, slice 6). The
  * engine records the once-per-turn use. The placement is a plain `Move`,
  * not a Travel, so no Travel rule reads it; its line is the movement
  * relics': "{Blue} placed at {site}."
  */
sealed abstract class PassagesPower extends PhasePower:
  def edifice: EdificeId
  protected def side: EdificeSide
  protected def decisionId: String
  protected def heading: String
  protected def placed: NoteKey

  final def timing: PowerTiming = PowerTiming.Wake
  override def narratedDecisions: Set[String] = Set(decisionId)

  /** Where the face stands. */
  final def passagesSite(ready: ReadyGame): Option[SiteId] =
    ready.game.current.edificeSite(edifice, side)

  final def usable(catalog: ExecutableCatalog, ready: ReadyGame,
      player: PlayerId, source: DecisionOptionRef): Boolean =
    passagesSite(ready).exists(ready.game.current.pawnSite(player).contains)

  final def build(catalog: ExecutableCatalog, ready: ReadyGame,
      player: PlayerId, source: DecisionOptionRef)
      : Either[OathViolation, Operation] =
    Right(Sequence(Vector[Operation](
      Branch((live, _) => passagesSite(live).toVector.map(site =>
        PawnMoves.siteChoice(decisionId, player,
          PawnMoves.sitesOtherThan(live, site), heading))),
      BuildOps((live, pending) => PawnMoves.chosenSite(pending, decisionId)
        .flatMap(PawnMoves.relocate(live, player, _))),
      Note(id, PawnMoves.placedNote(placed, source, player)))))

/** Sinking Passages (E25, ruined), WAKE: "If your pawn is at this site, you
  * may burn 1 favor to place your pawn on any site." The favor is burnt as
  * the power's cost, which the engine pays before the tree. */
case object SinkingPassages extends PassagesPower:
  val edifice: EdificeId = HiddenPassagesCard.id
  val id: PowerId = HiddenPassagesCard.ruined.power.id
  protected def side: EdificeSide = EdificeSide.Ruined
  override def cost: Cost = Cost(favorBurnt = 1)
  val decisionId: String = "power.sinking-passages.site"
  protected val heading: String =
    "Sinking Passages: place your pawn at which site?"
  private val notes = NoteSet(id)
  val placed: NoteKey = PawnMoves.placedKey(notes, NoteKey.Used)
  override def noteKeys: Vector[NoteKey] = notes.declared

/** Hidden Passages (E25, intact), WAKE: "If your pawn is at this site and
  * its ruler consents, place your pawn on any site."
  *
  * The consent is a power-use restriction the power declares on its own
  * use, as City Wall declares its ban (catalog batch 8, slice 10): a user
  * who does not rule the site is refused, waivably by the player who rules
  * it, so the Wake menu shows the power askable with "Hidden Passages:
  * Needs its ruler's consent." and a grant remembered under the use key
  * opens it for the turn. Under bandits the Bandit Crown's holder acts as
  * its ruler; with no holder nobody may consent and the menu shows "Hidden
  * Passages: Bandits rule this site."; at an unruled site, or for the
  * ruler, nothing is asked.
  */
case object HiddenPassages extends PassagesPower with ContributingPower
    with PowerUseRestriction:
  val edifice: EdificeId = HiddenPassagesCard.id
  val id: PowerId = HiddenPassagesCard.intact.power.id
  protected def side: EdificeSide = EdificeSide.Intact
  val decisionId: String = "power.hidden-passages.site"
  protected val heading: String =
    "Hidden Passages: place your pawn at which site?"
  val source: RuleSourceRef = RuleSourceRef.GameRule(id.value)
  val card: PowerSourceRef = PowerSourceRef.Card(edifice)
  private val notes = NoteSet(id)
  val placed: NoteKey = PawnMoves.placedKey(notes, NoteKey.Used)
  /** "Needs its ruler's consent.": the reason while the ruler may be
    * asked, and once they refused. */
  val needsConsent: NoteKey = notes.key("needs-consent", Vector(
    NotePart.Text("Needs its ruler's consent.")))
  /** "Bandits rule this site.": the reason when nobody may consent. */
  val banditsRule: NoteKey = notes.key("bandits-rule", Vector(
    NotePart.Text("Bandits rule this site.")))
  override def noteKeys: Vector[NoteKey] = notes.declared

  def contributions: Map[PowerWindow, Vector[Contribution]] = Map.empty
  override def powerUseRestrictions: Vector[PowerUseRestriction] =
    Vector(this)
  override val waiver: Option[Waiver] = Some(Waiver(granter, card,
    (_, _) => needsConsent(card)))

  /** Who may consent: the player who rules the Passages' site, or the
    * Bandit Crown's holder under bandits. */
  def granter(ready: ReadyGame): Option[PlayerId] =
    passagesSite(ready).flatMap(SiteRulers.consentGranterAt(ready, _))

  def refusal(catalog: ExecutableCatalog, ready: ReadyGame, use: PowerUse)
      : Option[PowerUseRefusal] =
    if use.power != id then None
    else for
      site <- passagesSite(ready)
      ruler <- SiteRulers.rulerOf(ready, site)
      if ruler != SiteRuler.Player(use.user)
      allowed = SiteRulers.consentGranterAt(ready, site)
      // The Bandit Crown's holder uses it as its ruler would.
      if !allowed.contains(use.user)
      (granter, plain) <- ruler match
        case SiteRuler.Player(player) => Some(Some(player) -> needsConsent)
        case SiteRuler.Bandits =>
          Some(allowed -> allowed.fold(banditsRule)(_ => needsConsent))
        case _ => None
    yield PowerUseRefusal(id, plain(card), OperationReason("hidden-passages",
      s"${use.user.value} needs the consent of the ruler of ${site.value}",
      OperationReasonKind.Impossible, Some(OperationWaiver(id, granter,
        ConsentKey(id, use.user, PowerUseRules.useKey,
          PowerUseRules.target(use)), needsConsent(card), plain(card)))))
