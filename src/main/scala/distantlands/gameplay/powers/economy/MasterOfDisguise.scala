package distantlands.gameplay.powers.economy

import distantlands.catalog.{AdviserOnly, Denizen, ExecutableCatalog,
  ModifierMark, PrintedPower}
import distantlands.gameplay.NoteSupport
import distantlands.gameplay.powerresolver.{AsksOnSelection, Contribution,
  ModifierPicks, OfferReason, PowerCtx}
import distantlands.gameplay.powers.{PowerAnswers, SelectedModifier}
import distantlands.model._

object MasterOfDisguiseCard extends Denizen(DenizenId("78"), "Master of Disguise", Suit.Arcane) with AdviserOnly:
  val power = PrintedPower(PowerId("denizen.master-of-disguise"),
    persistent = false, cost = Cost(secret = 1),
    modifier = Some(ModifierMark.Action(MajorActionType.Trade)),
    text = "Act as if you had another player's advisers instead. _(You " +
      "can't use your other advisers.)_")
  val powers: Vector[PrintedPower] = Vector(power)

/** Master of Disguise (card 78, adviser-only), a selected Trade modifier
  * costing one secret, paid onto it when picked: "Act as if you had another
  * player's advisers instead. (You can't use your other advisers.)"
  *
  * Once picked and paid for, it asks which other player, and records the
  * answer as state with `ActAsIfAdvisers` (catalog batch 11, ruling 8).
  * For the rest of the Trade the chosen player's advisers take the
  * holder's place wherever a rule reads whom an adviser serves or binds
  * (`PowerAccess.advisersOf`): their suits count, their Trade modifiers are
  * offered and the holder pays their costs, and their vows bind the
  * holder. The holder's own advisers do nothing, this card included once
  * picked. Their relics and the site cards they may use still offer their
  * modifiers (ruling 19).
  *
  * Unavailable once another modifier is picked (FAQ). Its line names the
  * player: "Acted as if holding the advisers of {p2}."
  */
case object MasterOfDisguise extends SelectedModifier, AsksOnSelection:
  val id: PowerId = MasterOfDisguiseCard.power.id

  val card: Denizen = MasterOfDisguiseCard
  def actions: Set[MajorActionType] = Set(MajorActionType.Trade)
  override def cost: Cost = MasterOfDisguiseCard.power.cost

  /** The question its pick asks: whose advisers the holder acts with. */
  val decisionId: String = "master-of-disguise.player"
  /** Why it is unavailable once another modifier is picked. */
  val notFirst: String = "Only as your first modifier"

  private val notes = NoteSet(id)
  /** "Acted as if holding the advisers of {p2}." */
  val acted: NoteKey = notes.key("acted", Vector(
    NotePart.Text("Acted as if holding the advisers of "), NotePart.Arg(0),
    NotePart.Text(".")))
  override def noteKeys: Vector[NoteKey] = notes.declared
  override def narratedDecisions: Set[String] = Set(decisionId)

  def effects: Map[PowerWindow, Vector[Contribution]] = Map.empty

  override def offerReason(ctx: PowerCtx): Option[OfferReason] =
    Option.when(ModifierPicks.cards(ctx.answered).nonEmpty)(
      OfferReason.Unavailable(notFirst))

  /** The player question, then the answer recorded as state, then the
    * line. The pick and its payment come before the question, so they are
    * judged on the holder's own advisers. */
  def selectionQuestions(catalog: ExecutableCatalog, ready: ReadyGame,
      actor: PlayerId): Vector[Operation] = Vector(
    Decide(decisionId, actor, DecisionQuery.ChooseOne(
      ready.game.current.players.map(_.player).filterNot(_ == actor)
        .map(player => DecisionOption.Player(DecisionOptionRef.Player(player))),
      heading = Some("Master of Disguise: act with whose advisers?"))),
    BuildOps((_, tree) => Right(PowerAnswers.last(tree.answered, decisionId)
      .toVector.collect { case DecisionOptionRef.Player(player) =>
        ActAsIfAdvisers(player) })),
    Note(id, actedNote))

  private def actedNote(states: NoteStates): Option[LogNote] =
    NoteSupport.answer(states, decisionId).collect {
      case DecisionOptionRef.Player(player) =>
        acted(PowerSourceRef.Card(cardId), NoteArg.Player(player))
    }
