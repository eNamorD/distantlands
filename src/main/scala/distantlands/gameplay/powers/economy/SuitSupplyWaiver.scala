package distantlands.gameplay.powers.economy

import distantlands.catalog.ExecutableCatalog
import distantlands.gameplay.EffectiveSuit
import distantlands.gameplay.powerresolver.{Contribution, OperationRewrite,
  Transform}
import distantlands.gameplay.powers.PowerAnswers
import distantlands.model._

/** The shape of the Muster and Trade modifiers that waive the Supply payment
  * by the suit of the card the action uses (the Cup of Plenty, Animal
  * Playmates, Birdsong).
  *
  * The card is the answer to the action's source decision, which the cost node
  * cannot see, so the actor's Supply payment is replaced by a node that reads
  * the answer when it runs and pays unless `free` holds for the card's suit.
  * The suit is read off the answered card, as the action reads it
  * (`EffectiveSuit.traded`), not resolved as a source: this node
  * runs after the payment that placed a token on the card, which a source must
  * not yet hold. A card whose suit cannot be read pays. `free` also
  * receives the walk's answers, for a rule that counts the actor's advisers
  * as the action reads them.
  *
  * The payment is replaced through `OperationRewrite.nodes`, because its
  * replacement is a node, not a core op. A later `ops` rewrite, such as
  * `SupplyStrip`, still finds the payment inside it.
  */
private[economy] object SuitSupplyWaiver:
  def effects(window: PowerWindow, decisionId: String)(
      free: (ExecutableCatalog, ReadyGame, PlayerId, Vector[Answered], Suit)
        => Boolean)
      : Map[PowerWindow, Vector[Contribution]] = Map(
    window -> Vector(Transform((ctx, operations) =>
      OperationRewrite.nodes(ctx, operations)((_, _) => {
        case pay @ SpendSupply(player, _, _) if player == ctx.actor =>
          Vector(unlessFree(pay, decisionId, ctx.catalog, free))
      }))))

  private def unlessFree(pay: SpendSupply, decisionId: String,
      catalog: ExecutableCatalog,
      free: (ExecutableCatalog, ReadyGame, PlayerId, Vector[Answered], Suit)
        => Boolean)
      : Operation = BuildOps((ready, pending) => Right(
    if suitOf(ready, pending, decisionId, catalog)
      .exists(free(catalog, ready, pay.player, pending.answered, _))
    then Vector.empty
    else Vector[CoreOperation](pay)))

  private def suitOf(ready: ReadyGame, pending: PendingTree,
      decisionId: String, catalog: ExecutableCatalog): Option[Suit] =
    PowerAnswers.one(pending, decisionId)
      .flatMap(EffectiveSuit.traded(catalog, ready, pending.answered, _))
