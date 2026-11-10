package distantlands.gameplay.powers.campaign

import distantlands.catalog.{Denizen, ExecutableCatalog, ModifierMark,
  PrintedPower}
import distantlands.gameplay.actions.campaign.CampaignProcedure
import distantlands.gameplay.powerresolver.{Deferred, OperationRewrite,
  Transform}
import distantlands.gameplay.powers.{PowerAnswers, RuledCards}
import distantlands.model._

object WildMountsCard extends Denizen(DenizenId("153"), "Wild Mounts", Suit.Nomad):
  val power = PrintedPower(PowerId("denizen.wild-mounts"),
    persistent = false, cost = Cost.free,
    modifier = Some(ModifierMark.EitherSidePlan),
    text = "If you would discard any number of [suit-nomad] battle " +
      "plans, you may instead discard any one [suit-beast] card " +
      "you rule.")
  val powers: Vector[PrintedPower] = Vector(power)

/** Wild Mounts (card 153), a battle plan for either side: "If you would
  * discard any number of [suit-nomad] battle plans, you may instead discard
  * any one [suit-beast] card you rule."
  *
  * It is free, and choosing it does nothing at the time. Its rewrite at the
  * Campaign's root folds `Late`, after every plan's own steps there. Once
  * the plan was used, it holds each discard of a Nomad card chosen as a
  * battle plan on its side (`OperationRewrite.ops`, `Deferred`), however
  * the plan built that discard. It does not read the discard's acting
  * player, which is the attacker for a bandit defender's plan. It appends
  * one step that resolves the held discards:
  *
  *  - it asks a player user who rules a Beast card, their faceup advisers
  *    and the faceup denizens at sites they rule, to discard one of them or
  *    to "Skip Wild Mounts". A Beast card chosen is discarded instead of the
  *    plans; skipping runs the held discards;
  *  - for a bandit user, it follows the FAQ: with two or more held
  *    discards, the first Beast card the bandits rule in map order is
  *    discarded instead;
  *  - otherwise it runs the held discards.
  *
  * Whether the plan was used is read at walk time, so a bandit's use,
  * recorded during the walk, counts. A held discard that runs writes the
  * plan's own "Discarded after the Campaign.", under the plan's power.
  *
  * The swap writes "Discarded {card} instead of the battle plans.", or,
  * for a defender, "{Blue} discarded {card} instead of the battle plans.".
  */
case object WildMounts extends DenizenPlan(WildMountsCard,
    WildMountsCard.power):
  /** Where the held discards wait for the step that resolves them. */
  val deferred: DeferralKey = DeferralKey("wild-mounts")
  /** Bandits swap only when they would discard this many Nomad plans
    * (FAQ). */
  val BanditDiscards: Int = 2
  /** Under the Campaign's prefix, so a parked question is a Campaign
    * decision. */
  val decisionId: String = CampaignProcedure.decisionPrefix + "wild-mounts"
  val skip: DecisionOptionRef.Button = DecisionOptionRef.Button("skip")
  /** Exposed: other powers write this power's lines and include this
    * set to declare them. */
  val notes: NoteSet = NoteSet(id)
  override def noteKeys: Vector[NoteKey] = notes.declared
  /** "Discarded {card} instead of the battle plans." */
  val swapped: NoteKey = notes.key("swapped", Vector(
    NotePart.Text("Discarded "), NotePart.Arg(0),
    NotePart.Text(" instead of the battle plans.")))
  /** "{Blue} discarded {card} instead of the battle plans.", for a
    * defender. */
  val swappedOffTurn: NoteKey = notes.key("swapped-off-turn", Vector(
    NotePart.Arg(0), NotePart.Text(" discarded "), NotePart.Arg(1),
    NotePart.Text(" instead of the battle plans.")))

  def sides: Set[CampaignPlanSide] =
    Set(CampaignPlanSide.Attacker, CampaignPlanSide.Defender)

  def plan(context: PlanContext): Option[CampaignPlanOffer] =
    context.denizen(cardId).map(source => CampaignPlanOffer(source,
      "Wild Mounts: discard a Beast card instead of Nomad battle plans",
      Vector.empty, Vector.empty))

  override def rewrites: Map[PowerWindow, Transform] = Map(
    PowerWindow.CampaignActionEligibility -> Transform((ctx, children) =>
      OperationRewrite.ops(ctx, children)(swappable(ctx.actor)) :+
        Deferred.resolve(WildMounts.deferred)(swap(ctx.catalog, ctx.actor))))

  /** The plan, when it was used in this Campaign: the attacker's use first,
    * when both sides used it. */
  private def used(catalog: ExecutableCatalog, ready: ReadyGame,
      pending: PendingTree, actor: PlayerId): Option[PlanUse] =
    PlanUse.chosen(catalog, ready, pending, actor, cardRef, sides,
      afterOutcome = true).headOption

  /** Holds the discard of a Nomad card chosen as a battle plan on a side
    * the plan was used on. */
  private def swappable(actor: PlayerId)
      : OperationRewrite.Matcher[CoreOperation, CoreOperation] =
    (ready, pending) => {
      case discard: Discard.Denizen if discard.suit == Suit.Nomad &&
          PlanUse.sidesUsed(ready, pending, cardRef, sides).exists(side =>
            PlanUse.plans(ready, pending, side).contains(
              DecisionOptionRef.Denizen(discard.card))) =>
        Vector(Defer(WildMounts.deferred, Vector(discard)))
    }

  /** What resolves the held discards: a question, a swap, or the
    * discards. A plan both sides used (catalog batch 11, ruling 3) is held
    * once for each use, but it is one card, so it is resolved once. */
  private def swap(catalog: ExecutableCatalog,
      actor: PlayerId)(ready: ReadyGame, pending: PendingTree,
      holds: Vector[CoreOperation]): Vector[Operation] =
    val held = holds.distinctBy {
      case discard: Discard.Denizen => discard.card
      case other => other
    }
    used(catalog, ready, pending, actor).fold(discarded(catalog, held)) { use =>
      use.user match
        case Some(user) => ask(catalog, use, user, discarded(catalog, held),
          beasts(catalog, ready, SiteRuler.Player(user)))
        case None if held.size >= WildMounts.BanditDiscards =>
          beasts(catalog, ready, SiteRuler.Bandits).headOption
            .fold(discarded(catalog, held))(card =>
              instead(catalog, use, use.actor, card))
        case None => discarded(catalog, held)
    }

  /** The held discards, each run as the plan built it, then the plan's own
    * line under the plan's power. */
  private def discarded(catalog: ExecutableCatalog,
      held: Vector[CoreOperation]): Vector[Operation] =
    held.flatMap(operation =>
      BuildOps((_, _) => Right(Vector(operation))) +: (operation match {
        case discard: Discard.Denizen =>
          planPower(catalog, discard.card).toVector.map(PlanDiscard.discardedNote(_, discard.card))
        case _ => Vector.empty
      }))

  /** The power printed on `card`, which is the plan's. */
  private def planPower(catalog: ExecutableCatalog,
      card: DenizenId): Option[PowerId] =
    catalog.denizen(card).flatMap(_.powers.headOption).map(_.id)

  /** The faceup Beast denizens `ruler` rules, in map order. */
  private def beasts(catalog: ExecutableCatalog,
      ready: ReadyGame, ruler: SiteRuler): Vector[DenizenId] =
    RuledCards.of(catalog, ready, ruler, Suit.Beast).collect {
      case card: DenizenId => card }

  private def ask(catalog: ExecutableCatalog,
      use: PlanUse, user: PlayerId, discards: Vector[Operation],
      ruled: Vector[DenizenId]): Vector[Operation] =
    if ruled.isEmpty then discards
    else Vector(
      Decide(WildMounts.decisionId, user, DecisionQuery.ChooseOne(
        ruled.flatMap(card =>
          DecisionOption.forRef(DecisionOptionRef.Denizen(card))) :+
          DecisionOption.Button(WildMounts.skip, "Skip Wild Mounts"),
        heading = Some("Wild Mounts: discard a Beast card you rule " +
          "instead of your Nomad battle plans"))),
      Branch((_, pending) =>
        PowerAnswers.one(pending, WildMounts.decisionId) match {
          case Some(DecisionOptionRef.Denizen(card)) if ruled.contains(card) =>
            instead(catalog, use, user, card)
          case _ => discards
        }))

  /** The discard of `card` in place of the plans', by `acting`, and its
    * line. A player defender is named; bandits are no player. */
  private def instead(catalog: ExecutableCatalog,
      use: PlanUse, acting: PlayerId, card: DenizenId)
      : Vector[Operation] =
    val source = PowerSourceRef.Card(cardId)
    val line = (use.side, use.user) match
      case (CampaignPlanSide.Defender, Some(user)) => WildMounts
        .swappedOffTurn(source, NoteArg.Player(user), NoteArg.Card(card))
      case _ => WildMounts.swapped(source, NoteArg.Card(card))
    Vector(PlanDiscard.denizen(catalog, acting, card,
        use.user.map(_ => DiscardCause(id))),
      Note(id, _ => Some(line)))
