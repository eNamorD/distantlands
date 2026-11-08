package oathdigital.model

// The card-play hooks are power windows, not Oath operations: they extend
// the unsealed [[Operation]], so they live outside `CoreOperations.scala`
// (catalog batch 11, slice 0).

/** Semantic window for a card played faceup, to a site or as a faceup
  * adviser. `byPower` is the power whose effect made the play (Search
  * Party, Temple of Ancestors, Oracle), `None` for an action's own play.
  * Powers supply its children; no hook is a delta.
  */
final case class CardPlayedFaceup(card: WorldCardId,
    resultingSource: RuleSourceRef, byPower: Option[PowerId] = None)
    extends Operation:
  override val window: Option[PowerWindow] = Some(PowerWindow.ActionCardPlayedFaceup)
  override val children: Vector[Operation] = Vector.empty

/** Semantic window for a card (a denizen or a Vision) placed facedown as an
  * adviser by `player`. `byPower` is as for [[CardPlayedFaceup]]. A discard
  * emits neither hook. Powers supply its children; no hook is a delta.
  */
final case class CardPlayedFacedown(card: WorldCardId, player: PlayerId,
    byPower: Option[PowerId] = None)
    extends Operation:
  override val window: Option[PowerWindow] = Some(PowerWindow.ActionCardPlayedFacedown)
  override val children: Vector[Operation] = Vector.empty
