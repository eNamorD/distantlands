package oathdigital.gameplay.powers.challenge

import oathdigital.catalog.Relic
import oathdigital.gameplay.actions.challenge.{ChallengeReach,
  SchoolOfVinesCard, SigilOfTheEyeCard, SigilOfTheHeartCard}
import oathdigital.gameplay.powerresolver.{Contribution, ContributingPower,
  OfferReason, OnEdificeFace, PowerCtx, PowerUse}
import oathdigital.gameplay.powers.{CatalogResolution, SelectedModifier}
import oathdigital.model._

/** The Challenge modifiers (catalog batch 10, slice 11). A pick is the
  * whole effect: the Challenge reads it ([[ChallengeReach]]), so none hooks
  * a window, and none writes a line, since the start line names it. */
object ChallengeModifiers:
  val all: Vector[ContributingPower] =
    Vector(SchoolOfVines, SigilOfTheEye, SigilOfTheHeart)

  /** Unavailable while the picks so far already reach every banner
    * `card`'s pick would: a pick that opened nothing would leave "Start
    * Challenge" unavailable with nothing else to pick. */
  private[challenge] def reachReason(ctx: PowerCtx, card: CardId)
      : Option[OfferReason] =
    Option.unless(ChallengeReach.widens(ctx.state, ctx.actor, ctx.answered,
      card))(OfferReason.Unavailable(ChallengeReach.noNewBanner))

/** A Sigil: a free relic Challenge modifier that lifts the pawn-site
  * requirement for one banner. */
sealed trait Sigil extends SelectedModifier:
  def actions: Set[MajorActionType] = Set(MajorActionType.Challenge)
  def effects: Map[PowerWindow, Vector[Contribution]] = Map.empty
  override def offerReason(ctx: PowerCtx): Option[OfferReason] =
    ChallengeModifiers.reachReason(ctx, cardId)

/** Sigil of the Eye (relic R43): "You may challenge the Darkest Secret
  * from any site." */
case object SigilOfTheEye extends Sigil:
  val id: PowerId = SigilOfTheEyeCard.power.id
  val card: Relic = SigilOfTheEyeCard

/** Sigil of the Heart (relic R44): "You may challenge the People's Favor
  * from any site." */
case object SigilOfTheHeart extends Sigil:
  val id: PowerId = SigilOfTheHeartCard.power.id
  val card: Relic = SigilOfTheHeartCard

/** School of Vines (E27, intact), a free Challenge modifier: "You may
  * challenge a banner held by an enemy even if your pawn is not at their
  * pawn's site." Imperial players are not each other's enemies.
  *
  * It is written as Drowned Library is, since a selected modifier's card
  * is a denizen or a relic, never an edifice face. It is listed only while
  * the intact face is up (`OnEdificeFace`), and wherever its user may use
  * the edifice. */
case object SchoolOfVines extends OnEdificeFace:
  private val printed = SchoolOfVinesCard.intact.power
  val id: PowerId = printed.id
  val edifice: EdificeId = SchoolOfVinesCard.id
  val side: EdificeSide = EdificeSide.Intact

  def source: RuleSourceRef = RuleSourceRef.GameRule(id.value)
  override lazy val resolution: PowerResolution = CatalogResolution.of(printed)
  override def selectionCard: Option[CardId] = Some(edifice)
  override def selectionWindows: Set[PowerWindow] =
    Set(PowerWindow.ChallengeModifierSelection)

  /** Its use at its selection window: free, placing nothing. */
  override def use(ctx: PowerCtx): Option[PowerUse] =
    SelectedModifier.selectionAction(ctx.window).map(_ =>
      PowerUse.of(ctx.actor, printed, edifice, secret = false,
        places = false))

  /** At a selection window, for a Challenge only. */
  override def applicable(ctx: PowerCtx): Boolean =
    SelectedModifier.selectionAction(ctx.window).forall(
      _ == MajorActionType.Challenge)

  override def offerReason(ctx: PowerCtx): Option[OfferReason] =
    ChallengeModifiers.reachReason(ctx, edifice)

  def contributions: Map[PowerWindow, Vector[Contribution]] = Map.empty
