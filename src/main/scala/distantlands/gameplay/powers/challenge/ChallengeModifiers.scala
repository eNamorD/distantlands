package distantlands.gameplay.powers.challenge

import distantlands.catalog.{Denizen, Relic}
import distantlands.gameplay.PowerAccess
import distantlands.gameplay.actions.BannerRules
import distantlands.gameplay.actions.challenge.{ChallengeReach,
  MagiciansCodeCard, SchoolOfVinesCard, SigilOfTheEyeCard,
  SigilOfTheHeartCard}
import distantlands.gameplay.powerresolver.{Contribution, ContributingPower,
  ModifierPicks, OfferReason, OnEdificeFace, PowerCtx, PowerUse}
import distantlands.gameplay.powers.{CatalogResolution, SelectedModifier}
import distantlands.model._

/** The Challenge modifiers (catalog batch 10, slice 11). A pick is the
  * whole effect: the Challenge reads it ([[ChallengeReach]]), so none hooks
  * a window, and none writes a line, since the start line names it. */
object ChallengeModifiers:
  val all: Vector[ContributingPower] =
    Vector(SchoolOfVines, SigilOfTheEye, SigilOfTheHeart, MagiciansCode)

  /** Unavailable while the picks so far, with Magician's Code if it could
    * still be picked, already reach every banner `card`'s pick would: a
    * pick that opened nothing would leave "Start Challenge" unavailable with
    * nothing else to pick. */
  private[challenge] def reachReason(ctx: PowerCtx, card: CardId)
      : Option[OfferReason] =
    Option.unless(ChallengeReach.widens(ctx.state, ctx.actor, ctx.answered,
      card, codeAssumed(ctx)))(
      OfferReason.Unavailable(ChallengeReach.noNewBanner))

  /** Magician's Code, while the actor could still pick it and pay its
    * favor. A power-use restriction that refused it after a free pick
    * would still leave that pick a dead end; none refuses it today. */
  private def codeAssumed(ctx: PowerCtx): Vector[CardId] =
    val affordable = ctx.state.game.current.player(ctx.actor).exists(
      BannerRules.playerResources(ctx.state, _, Banner.PeoplesFavor) >=
        MagiciansCodeCard.power.cost.favor)
    Vector[CardId](MagiciansCodeCard.id)
      .filter(card => affordable && pickable(ctx, card))

  /** The reach cards the actor could still pick: School of Vines on its
    * intact face, and the Sigils. They are free. */
  private[challenge] def reachAssumed(ctx: PowerCtx): Vector[CardId] =
    Vector[CardId](SigilOfTheEyeCard.id, SigilOfTheHeartCard.id)
      .filter(pickable(ctx, _)) ++
      Vector[CardId](SchoolOfVinesCard.id).filter(card =>
        SchoolOfVines.faceUp(ctx.state) && pickable(ctx, card))

  private def pickable(ctx: PowerCtx, card: CardId): Boolean =
    !ModifierPicks.cards(ctx.answered).contains(card) &&
      PowerAccess.locate(ctx.state, ctx.actor, card).isDefined

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

/** Magician's Code (card 32), a Challenge modifier for 2 favor, placed on
  * the card: "If you're challenging the Darkest Secret, gain [secret]
  * [secret] and place them on the banner along with any other [secret]
  * you're placing (even none)." The Challenge counts, bounds and pays the
  * 2 secrets ([[ChallengeReach.bonus]]). It is unavailable while the
  * Darkest Secret would stay out of reach with them and with every reach
  * card the actor could still pick. Picked, then the People's Favor
  * chosen, it does nothing, and its favor stays paid. */
case object MagiciansCode extends SelectedModifier:
  val id: PowerId = MagiciansCodeCard.power.id
  val card: Denizen = MagiciansCodeCard
  def actions: Set[MajorActionType] = Set(MajorActionType.Challenge)
  override def cost: Cost = MagiciansCodeCard.power.cost
  def effects: Map[PowerWindow, Vector[Contribution]] = Map.empty

  override def offerReason(ctx: PowerCtx): Option[OfferReason] =
    ChallengeReach.codeReason(ctx.state, ctx.actor, ctx.answered,
      ChallengeModifiers.reachAssumed(ctx)).map(OfferReason.Unavailable(_))
