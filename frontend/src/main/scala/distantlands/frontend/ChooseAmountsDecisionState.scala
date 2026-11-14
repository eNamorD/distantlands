package distantlands.frontend

import distantlands.protocol.{DecisionAnswerWire, DistributeAmountWire,
  GameIntent => GameCommand}

/** One amount per slot, each clamped to its own bounds; with `distinct`,
  * Confirm waits until no two are equal (Legacy Setup powers ruling 15). */
private[frontend] final case class ChooseAmountsDecisionState(
    slots: Vector[DistributeSlotBounds], distinct: Boolean,
    amounts: Map[String, Int]):
  def amount(item: String): Int = amounts.getOrElse(item, 0)
  def increment(item: String): ChooseAmountsDecisionState = step(item, 1)
  def decrement(item: String): ChooseAmountsDecisionState = step(item, -1)
  /** To the slot's maximum. */
  def fill(item: String): ChooseAmountsDecisionState = step(item, Int.MaxValue)
  /** To the slot's minimum. */
  def drain(item: String): ChooseAmountsDecisionState = step(item, Int.MinValue)
  /** A typed amount, clamped to the slot's bounds. */
  def set(item: String, value: Int): ChooseAmountsDecisionState =
    step(item, value - amount(item))

  /** The amounts two or more slots share, ascending. */
  def clashes: Vector[Int] =
    if !distinct then Vector.empty
    else slots.map(slot => amount(slot.item)).groupBy(identity)
      .collect { case (value, same) if same.size > 1 => value }
      .toVector.sorted

  def canConfirm: Boolean = clashes.isEmpty

  private def step(item: String, by: Int): ChooseAmountsDecisionState =
    slots.find(_.item == item).fold(this) { bounds =>
      val target = (amount(item).toLong + by).max(bounds.minimum.toLong)
        .min(bounds.maximum.toLong).toInt
      copy(amounts = amounts.updated(item, target))
    }

private[frontend] object ChooseAmountsDecisionState:
  /** Opens at the suggestions, else at each slot's minimum. */
  def opened(slots: Vector[DistributeSlotBounds], distinct: Boolean,
      suggested: Option[Vector[Int]]): ChooseAmountsDecisionState =
    ChooseAmountsDecisionState(slots, distinct, slots.map(_.item).zip(
      suggested.filter(_.size == slots.size)
        .getOrElse(slots.map(_.minimum))).toMap)

/** A draft answer to a parked `ChooseAmounts`, built from the projected
  * query alone, as [[WalkerDistributeDraft]] is. */
private[frontend] final case class WalkerChooseAmountsDraft(
    context: BoardSelectionContext, decisionId: String,
    query: DecisionQueryState.ChooseAmounts,
    state: ChooseAmountsDecisionState):
  def increment(item: String): WalkerChooseAmountsDraft =
    copy(state = state.increment(item))
  def decrement(item: String): WalkerChooseAmountsDraft =
    copy(state = state.decrement(item))
  def fill(item: String): WalkerChooseAmountsDraft =
    copy(state = state.fill(item))
  def drain(item: String): WalkerChooseAmountsDraft =
    copy(state = state.drain(item))
  def set(item: String, value: Int): WalkerChooseAmountsDraft =
    copy(state = state.set(item, value))
  def canConfirm: Boolean = state.canConfirm

  def command: Option[GameCommand.ResolveWalker] =
    Option.when(canConfirm)(GameCommand.ResolveWalker(decisionId,
      DecisionAnswerWire.DistributeWire(query.slots.map(slot =>
        DistributeAmountWire(slot.option.kind, slot.option.id,
          state.amount(WalkerPartitionDraft.itemId(slot.option)))))))

private[frontend] object WalkerChooseAmountsDraft:
  /** Adopts whichever parked decision projects a choose-amounts query, and
    * drops the draft when the decision, the query or the context changes. */
  def reconcile(previous: Option[WalkerChooseAmountsDraft],
      context: BoardSelectionContext, decision: Option[WalkerDecisionState])
      : Option[WalkerChooseAmountsDraft] =
    decision.flatMap(parked => parked.query.collect {
        case amounts: DecisionQueryState.ChooseAmounts =>
          parked.decisionId -> amounts })
      .map { case (decisionId, query) =>
        previous.filter(draft => draft.context == context &&
            draft.decisionId == decisionId && draft.query == query)
          .getOrElse(WalkerChooseAmountsDraft(context, decisionId, query,
            ChooseAmountsDecisionState.opened(query.slots.map(slot =>
              DistributeSlotBounds(WalkerPartitionDraft.itemId(slot.option),
                slot.minimum, slot.maximum)), query.distinct,
              Option.when(query.slots.nonEmpty &&
                query.slots.forall(_.suggested.nonEmpty))(
                query.slots.flatMap(_.suggested)))))
      }
