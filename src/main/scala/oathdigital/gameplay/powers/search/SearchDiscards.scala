package oathdigital.gameplay.powers.search

import oathdigital.gameplay.powerresolver.{Contribution, ModifierPicks,
  OfferReason, OperationRewrite, PowerCtx, Transform}
import oathdigital.gameplay.powers.SelectedModifier
import oathdigital.model._

/** What Bracken, Cracked Horn and Ring of Devotion share (catalog batch 10,
  * slice 9): a selected Search modifier that changes where the user's own
  * discards go for the rest of that Search.
  *
  * A pick covers every card the user discards during the Search (ruling
  * 2): the cards not kept, at `SearchActionEligibility`, and every discard
  * of the card play, at `SearchPlayAdviser`: an adviser bumped over the
  * limit, a Vision displaced, a site card the play replaces or the People's
  * Favor discards, and the kept card itself when it is discarded. Each
  * window's `Transform` rewrites the user's `Discard.Denizen` and every
  * `Discard.Vision` with `OperationRewrite.ops`. A ruined edifice's discard
  * is another operation, so it is still set aside.
  *
  * Another power's discard is untouched (Bracken FAQ: "not cards like
  * Dazzle"): a WHEN PLAYED power runs under the card-play hook's own
  * window, which the rewrite does not enter. Spinning Bee's no-match
  * discard is never reached either, since Bee and these modifiers exclude
  * each other.
  *
  * Once one of the three is picked, the other two are unavailable.
  */
trait SearchDiscards extends SelectedModifier:
  /** `discard`, made by `actor` during the Search, as this power sends
    * it, read at the moment it is matched: the state, and the walk's
    * answers so far. */
  def redirected(ready: ReadyGame, answered: Vector[Answered],
      actor: PlayerId)(discard: SearchDiscards.UserDiscard)
      : Vector[CoreOperation]

  final def actions: Set[MajorActionType] = Set(MajorActionType.Search)

  final def effects: Map[PowerWindow, Vector[Contribution]] =
    SearchDiscards.windows.map(_ -> Vector(Transform(rewritten))).toMap

  final override def offerReason(ctx: PowerCtx): Option[OfferReason] =
    Option.when(ModifierPicks.cards(ctx.answered).exists(card =>
      card != cardId && SearchDiscards.cards(card)))(
      OfferReason.Unavailable(SearchDiscards.anotherPicked))

  private def rewritten(ctx: PowerCtx, children: Vector[Operation])
      : Vector[Operation] =
    OperationRewrite.ops(ctx, children)((ready, pending) => {
      case discard: Discard.Denizen if discard.by.contains(ctx.actor) =>
        redirected(ready, pending.answered, ctx.actor)(discard)
      case discard: Discard.Vision =>
        redirected(ready, pending.answered, ctx.actor)(discard)
    })

object SearchDiscards:
  /** A discard the user makes during the Search. A Vision discard names no
    * player, and every one the two windows hold is the user's. */
  type UserDiscard = Discard.Denizen | Discard.Vision

  /** Why the other two are unavailable once one is picked (spec,
    * "Readings"). */
  val anotherPicked: String =
    "Another power already sets where this Search's discards go."

  /** The windows whose discards are the Search's own. */
  private val windows: Vector[PowerWindow] = Vector(
    PowerWindow.SearchActionEligibility, PowerWindow.SearchPlayAdviser)

  /** The three cards' ids. */
  def cards: Set[CardId] = Set(CrackedHornCard.id, RingOfDevotionCard.id)
