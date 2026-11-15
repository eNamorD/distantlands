package distantlands.model

/** Operations on dice pools and rolls. Dice-pool counts live in
  * `CurrentGameState.rollPools`; a `Roll` records that a roll
  * happens, and the walker or the next command supplies its faces.
  *
  * One operation family: a sealed area of Oath's operation
  * vocabulary, joined to [[CoreOperation]] by the union alias
  * in `CoreOperations.scala`. A leaf also extends
  * [[LeafOperation]]; a composite exposes its `children`.
  */
sealed trait DiceOperation extends OperationBase

/** Adds `delta` dice to a named pool's count in state. `window` makes the
  * node hookable exactly like `Decide`.
  */
final case class ModifyDicePool(pool: PoolKey, delta: Int,
    override val window: Option[PowerWindow] = None)
    extends DiceOperation, LeafOperation

/** Rolls `dice` drawn from `pool`; the pool count comes from state. `Parked`
  * (the default) parks the walker until the faces ride the next command;
  * `Automatic` takes them from the walker's dice source and keeps walking.
  * A `window` lets a power hook the roll.
  */
final case class Roll(pool: PoolKey, dice: DiceSpec,
    mode: RollMode = RollMode.Parked,
    override val window: Option[PowerWindow] = None)
    extends DiceOperation, LeafOperation

/** Edits a recorded roll outcome for `pool` (power-authored skulls/score
  * changes). Left `None` fields unchanged.
  */
final case class ModifyRollOutcome(pool: PoolKey, skulls: Option[Int],
    score: Option[Int]) extends DiceOperation, LeafOperation

/** Rerolls the dice at `indices` of `pool`'s recorded outcome: the faces
  * at those indices are replaced by `faces`, in order, and the skulls and
  * the score are worked out again (catalog batch 7, slice 1).
  *
  * A tree declares it with no faces. The walker fills them from its dice
  * source before running it, so the journal records them and replay applies
  * them as they ran, as it applies a `Shuffle`'s order. A `BuildOps` batch
  * is run as it is built, so a reroll inside one is never filled. Applying
  * a reroll with no faces is an error.
  */
final case class Reroll(pool: PoolKey, indices: Vector[Int],
    faces: Option[Vector[DieFace]] = None) extends DiceOperation, LeafOperation:
  override val required: Boolean = true

/** Sets dice of `pool` to `faces` instead of rolling them (catalog batch 7,
  * slice 3: Arcane Armor). The faces join the pool's recorded outcome,
  * which starts with no throw counted when the pool has not rolled, and the
  * pool holds as many dice fewer, so the roll that follows rolls only the
  * rest. The set dice are no part of that roll's throw, so no reroll
  * changes them. Setting no dice, more dice than the pool holds, or faces
  * of another kind than the outcome's is an error.
  */
final case class SetDieFaces(pool: PoolKey, faces: Vector[DieFace])
    extends DiceOperation, LeafOperation:
  override val required: Boolean = true
