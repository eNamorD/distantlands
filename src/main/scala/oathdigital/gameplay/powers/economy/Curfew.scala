package oathdigital.gameplay.powers.economy

import oathdigital.catalog.{Denizen, ExecutableCatalog, ModifierMark,
  PrintedPower, SiteOnly}
import oathdigital.gameplay.EffectiveSuit
import oathdigital.gameplay.actions.economy.TradeProcedure
import oathdigital.gameplay.powerresolver.{ContributingPower, Contribution,
  PowerCtx, Transform}
import oathdigital.gameplay.powers.{CatalogResolution, PayOrAsk,
  PowerAnswers, RuledCards, RulerToll}
import oathdigital.model._

object CurfewCard extends Denizen(DenizenId("119"), "Curfew", Suit.Order) with SiteOnly:
  val power = PrintedPower(PowerId("denizen.curfew"),
    persistent = true, cost = Cost.free,
    modifier = Some(ModifierMark.Action(MajorActionType.Trade)),
    text = "Enemies **cannot** trade with cards ruled by Curfew's ruler " +
      "unless they give [favor] to its ruler. _(Give it to " +
      "Chancellor if Empire, burn it if bandits.)_")
  val powers: Vector[PrintedPower] = Vector(power)

/** Curfew (card 119, site-only), a persistent rule of a faceup site card: an
  * enemy of Curfew's ruler who trades with a card that ruler rules, an
  * adviser included, gives the ruler 1 favor, or burns it when bandits rule,
  * on top of the Trade's cost ([[RulerToll]]). The ruler is exempt.
  *
  * The toll runs after the rest of the Trade's cost and reads the Trade's
  * source when it runs. Under bandits it is a required batch placed for
  * every Trade by an enemy. When somebody may let the trader off (a player
  * ruler, or the Bandit Crown's holder under bandits), the trader is asked
  * first to pay or to trade without paying, which that player may consent
  * to (consent slice 4), only for a source the ruler rules. A trader who
  * cannot pay and is not let off is refused, and the restriction look-ahead
  * hides that source. Its line reads the payment's step.
  */
case object Curfew extends ContributingPower:
  val id: PowerId = CurfewCard.power.id
  val decisionId: String = "power.curfew.toll"
  private val notes = NoteSet(id)
  override def noteKeys: Vector[NoteKey] = notes.declared
  /** "Cannot trade with its ruler's cards without giving favor.": the
    * reason of the free option once its ruler refused. */
  val noFreeTrade: NoteKey = notes.key("no-free-trade", Vector(NotePart.Text(
    "Cannot trade with its ruler's cards without giving favor.")))
  /** The free option's reason while its ruler may be asked. */
  val noFreeTradeConsent: NoteKey = notes.key("no-free-trade-consent",
    Vector(NotePart.Text("Cannot trade with its ruler's cards without " +
      "giving favor or ruler's consent.")))
  notes.include(RulerToll.notes)
  val step: PayOrAsk = PayOrAsk(id, CurfewCard.id, decisionId, noFreeTrade,
    noFreeTradeConsent)

  /** The free option of a Trade with `source`. */
  def free(source: DecisionOptionRef): DecisionOptionRef.Button =
    PayOrAsk.free(source.wireId)

  val cardId: DenizenId = CurfewCard.id
  def source: RuleSourceRef = RuleSourceRef.GameRule(id.value)
  override lazy val resolution: PowerResolution =
    CatalogResolution.of(CurfewCard.power)

  override def narratedDecisions: Set[String] = Set(Curfew.decisionId)

  def contributions: Map[PowerWindow, Vector[Contribution]] = Map(
    PowerWindow.TradeCost -> Vector(Transform((ctx, operations) =>
      RulerToll.enemyRuler(ctx.state, cardId, ctx.actor)
        .fold(operations)(charged(ctx.catalog, ctx.state, ctx.actor, _,
          operations)))),
    PowerWindow.PayOrAsk -> Vector(Curfew.step.restriction))

  override def applicable(ctx: PowerCtx): Boolean =
    if ctx.window == PowerWindow.PayOrAsk then Curfew.step.asking(ctx)
    else RulerToll.enemyRuler(ctx.state, cardId, ctx.actor).nonEmpty

  /** `operations` with what a Trade with a card `ruler` rules owes, read
    * against the Trade's source when it runs: the step first and its outcome
    * last when somebody may let the trader off (the outcome runs nothing
    * when the step was not asked), else the payment and its line last. */
  private def charged(catalog: ExecutableCatalog, state: ReadyGame,
      trader: PlayerId, ruler: SiteRuler,
      operations: Vector[Operation]): Vector[Operation] =
    PayOrAsk.letOffBy(state, ruler, trader) match
      case Some(granter) =>
        val paid = Vector(RulerToll.payment(trader, ruler),
          Note(id, RulerToll.note(cardId, trader, ruler)))
        val waived = Vector(Note(id, _ => Some(RulerToll.waived(
          Curfew.step.source, NoteArg.Player(granter)))))
        Branch((ready, pending) => tradedWith(catalog, ready, ruler, pending)
          .toVector.map(source => Curfew.step.question(trader, source.wireId,
            s"Curfew: ${RulerToll.paying(ruler)} to trade with this card?",
            RulerToll.payOption(ready, trader, ruler,
              RulerToll.favorTaken(operations, trader)),
            "Trade without paying"))) +: operations :+
          Curfew.step.outcome(paid, waived)
      case None => operations ++ Vector(
        BuildOps((ready, pending) => Right(
          tradedWith(catalog, ready, ruler, pending)
            .map(_ => RulerToll.payment(trader, ruler)).toVector),
          required = true),
        Note(id, RulerToll.note(cardId, trader, ruler)))

  /** The Trade's source, when it is a card `ruler` rules. */
  private def tradedWith(catalog: ExecutableCatalog,
      ready: ReadyGame, ruler: SiteRuler,
      pending: PendingTree): Option[DecisionOptionRef] =
    PowerAnswers.one(pending, TradeProcedure.decisionId).filter(ref =>
      DecisionOptionRef.denizenOrEdificeOf(ref)
        .exists(ruled(catalog, ready, ruler, _)))

  /** Whether `ruler` rules `card`: one of its faceup advisers, or a card at
    * a site it rules. */
  private def ruled(catalog: ExecutableCatalog,
      ready: ReadyGame, ruler: SiteRuler, card: CardId)
      : Boolean = EffectiveSuit.of(catalog, ready, card).exists(suit =>
    RuledCards.of(catalog, ready, ruler, suit).contains(card))
