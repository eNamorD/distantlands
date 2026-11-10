package distantlands.gameplay.operations

import distantlands.catalog.ExecutableCatalog
import distantlands.gameplay.powerresolver.{PowerUse, PowerUseRefusal,
  PowerUseRestriction}
import distantlands.model._

/** The power-use filter (catalog batch 8, slice 10): the refusals a use
  * meets, with consent applied, and how an option for a refused use is
  * shown. Every path that uses a power reads it: the phase menu and the
  * start check, the modifier picker, the battle-plan and reroll offers, and
  * the automatic powers (part 2). */
object PowerUseRules:
  /** The decision slot of a power-use grant's key. A grant covers the
    * card's use by its requester for the turn, on whichever path uses it,
    * so the key names the use instead of a decision. */
  val useKey: String = "power.use"

  /** The target of a grant's key: the card, or the power's own button
    * when it has no card or its card has no option ref. */
  def target(use: PowerUse): DecisionOptionRef =
    use.card.flatMap(DecisionOptionRef.ofCard)
      .getOrElse(DecisionOptionRef.Button(use.power.value))

  /** The option reference of `card`, the target of a grant's key; the
    * card's own button when it has no option ref. */
  def cardRef(card: CardId): DecisionOptionRef =
    DecisionOptionRef.ofCard(card)
      .getOrElse(DecisionOptionRef.Button(card.value))

  /** Every refusal `restrictions` give `use`, whatever consent was given:
    * what an automatic use is shaped by, so a grant or a refusal never
    * changes its shape (automatic power consent design, "The
    * `AutomaticUse` node"). */
  def raised(catalog: ExecutableCatalog, ready: ReadyGame, use: PowerUse,
      restrictions: Vector[PowerUseRestriction]): Vector[PowerUseRefusal] =
    restrictions.flatMap(_.refusal(catalog, ready, use))

  /** Every refusal `restrictions` give `use`, less those a grant
    * remembered this turn lifts. */
  def refusals(catalog: ExecutableCatalog, ready: ReadyGame, use: PowerUse,
      restrictions: Vector[PowerUseRestriction]): Vector[PowerUseRefusal] =
    raised(catalog, ready, use, restrictions).filterNot(refusal =>
      refusal.reason.waiver.exists(waiver => ready.consent.answer(
        waiver.covers, waiver.granter, ConsentTurn.of(ready))
        .contains(ConsentAnswer.Granted)))

  /** Whether consent may lift every one of `refusals` of `use`: the use
    * may be asked about, and each refusal names a player other than its
    * user who may waive it (consent design, rulings 1, 2, 4 and 5). */
  def waivable(use: PowerUse, refusals: Vector[PowerUseRefusal]): Boolean =
    use.consent && refusals.forall(_.reason.waiver.exists(
      _.granter.exists(_ != use.user)))

  /** How an option for a use `refusals` stop is shown: askable of its
    * granters when `consent` allows it and consent may lift every refusal
    * (ruling 8), else unavailable with the first refusal's reason. */
  def shown(ready: ReadyGame, actor: PlayerId,
      refusals: Vector[PowerUseRefusal], consent: Boolean): OptionReason =
    require(refusals.nonEmpty, "an option with no refusal is not refused")
    val first = refusals.head
    Option.when(consent)(OperationConsent.ask(ready, actor,
      refusals.map(_.reason))).flatten
      .getOrElse(OptionReason.Noted(first.power, first.note))
