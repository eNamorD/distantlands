package distantlands.gameplay.actions.explore

import distantlands.catalog.ExecutableCatalog
import distantlands.gameplay.actions.ProcedureNotes
import distantlands.gameplay.actions.dice.RollStep
import distantlands.gameplay.powerresolver.ModifierPicks
import distantlands.gameplay.setup.AtlasPlacement
import distantlands.gameplay.setup.legacies.{SetupLegacyRules, WorldCrafter,
  WorldCrafterCard}
import distantlands.gameplay.walker.{ModifierChoice, WalkerRollFeedback}
import distantlands.model._
import distantlands.model.DecisionAnswer.ChooseOneAnswer

/** The node the `WhenExplored` window is folded on, right after an Explore
  * places a site: "it triggers when the card is placed on the map by the
  * Explore action" (rulebook, "Setup / When Explored"). It names the placed
  * site, so a power resolves only for a card that lies there, and it walks
  * nothing but what the powers add. */
final case class ExploredSite(site: SiteId) extends Operation:
  override val window: Option[PowerWindow] = Some(PowerWindow.WhenExplored)
  val children: Vector[Operation] = Vector.empty

/** Declared Explore procedure tree for the
  * [[distantlands.gameplay.walker.ProcedureWalker]]: the action Wide
  * Horizons, altered Foundation I, gives every player (Foundations design,
  * slice 3, "Explore").
  *
  * {{{
  * Sequence(
  *   Repeat(ModifierChoice),                  // window = ExploreModifierSelection;
  *                                            // no card prints an Explore
  *                                            // modifier, so nothing is asked
  *   Sequence(
  *     ModifyDicePool("explore", +2),
  *     Repeat(guard = not succeeded && lastChoice != Stop,
  *       Sequence(
  *         SpendSupply(actor, 1),             // window = ExploreCost
  *         Roll("explore", Defense, Automatic), // window = ExploreRoll;
  *                                              // then the reroll choice
  *         Branch(choice when not yet success))), // Decide("explore.choice")
  *     Branch(if success ->
  *       Vector(Branch(Decide("explore.end"),  // asked with 2 or more sites,
  *           Note(chose)),                   // of World Crafter's holder
  *         BuildOps(PlaceAtlasSite, pawn Move),
  *         Note(explored),
  *         Branch(ExploredSite(site))),        // window = WhenExplored
  *       else if stopped -> Vector(Note(failed)))))
  * }}}
  *
  * It works like a Recover against a difficulty of 4 (rulebook, "Altered
  * Foundation I: Wide Horizons"): each roll is 2 defense dice for 1 Supply,
  * the shields of every roll count together, and after a short roll the
  * player continues or stops. It borrows only Recover's dice: every window
  * here is Explore's own, so no power that names Recover hooks it
  * (Foundations ruling 2), and the generic reroll choice still follows
  * each roll.
  *
  * The gates are start gates. A resume rebuilds the tree without them,
  * since the Explore itself moves the pawn and may fill the last slot or
  * take the last site.
  */
object ExploreProcedure:
  val explorePool: PoolKey = PoolKey("explore")
  val choiceDecisionId: String = "explore.choice"
  val endDecisionId: String = "explore.end"

  /** Every Explore decision's id starts here. */
  val decisionPrefix: String = "explore."

  /** "Explore works like a Recover action with a difficulty of 4." */
  val difficulty: Int = 4

  private val supplyCost: Int = 1

  /** "Start Explore": the modifier decision's button, were one asked. */
  val startLabel: String = "Start Explore"

  /** The gates' reasons, as the Act menu shows them. */
  val needsWideHorizons: String = "Explore needs the Wide Horizons foundation"
  val noEmptySlot: String = "The map has no empty site slot"
  val noAtlasSite: String = "The Atlas holds no site"
  val notAtHintermost: String = "Your pawn is not at the Hintermost site"

  val continueOption: DecisionOptionRef.Button =
    DecisionOptionRef.Button("continue")
  val stopOption: DecisionOptionRef.Button = DecisionOptionRef.Button("stop")
  val continueLabel: String = "Roll 2 more dice (1 Supply)"
  val stopLabel: String = "Stop exploring"
  /** Why rolling again is unavailable to a player with no Supply. */
  val noSupply: String = "No Supply left"
  /** The heading of the `explore.end` decision. */
  val endHeading: String = "Explore: place a site from"

  /** The two answers of the `explore.end` decision. */
  def endOption(end: AtlasEnd): DecisionOptionRef.Button =
    AtlasPlacement.endOption(end)

  private val notes = NoteSet(ProcedureNotes.id(ActionRef.Explore))
  /** "Explored Recent Site: Found Deep Woods". */
  val exploredRecent: NoteKey = notes.key("explored-recent", Vector(
    NotePart.Text("Explored Recent Site: Found "), NotePart.Arg(0)))
  /** "Explored Forgotten Site: Found Deep Woods" (Foundations ruling 4). */
  val exploredForgotten: NoteKey = notes.key("explored-forgotten", Vector(
    NotePart.Text("Explored Forgotten Site: Found "), NotePart.Arg(0)))
  /** "Failed to explore": the player answered Stop. */
  val failed: NoteKey = notes.key("failed", Vector(
    NotePart.Text("Failed to explore")))
  def noteKeys: Vector[NoteKey] = notes.declared ++
    WorldCrafter.exploreNoteKeys

  /** The line of a successful Explore from `end`. */
  def explored(end: AtlasEnd): NoteKey = end match
    case AtlasEnd.Recent => exploredRecent
    case AtlasEnd.Forgotten => exploredForgotten

  /** Whether Foundation I lies on Wide Horizons, its altered face. */
  def wideHorizons(state: ReadyGame): Boolean =
    state.game.campaign.foundations.get(FoundationNumber.I)
      .exists(_.face == FoundationFace.Altered)

  /** Why `actor` cannot start an Explore now, the first gate that fails:
    * Foundation I is Wide Horizons, a slot is empty, the Atlas holds a
    * site, and the actor's pawn is at the Hintermost site. Any role may
    * Explore. */
  def refusal(state: ReadyGame, actor: PlayerId): Option[String] =
    val current = state.game.current
    if !wideHorizons(state) then Some(needsWideHorizons)
    else if current.map.nextSlot.isEmpty then Some(noEmptySlot)
    else if state.game.campaign.atlas.sites.isEmpty then Some(noAtlasSite)
    else if !current.map.hintermost.exists(
        current.pawnSite(actor).contains) then Some(notAtHintermost)
    else None

  /** Every park of an Explore but its modifier decision shows the pool
    * rolled so far against the difficulty. */
  def rollFeedback(catalog: ExecutableCatalog, ready: ReadyGame,
      actor: PlayerId, decisionId: String): Option[WalkerRollFeedback] =
    Option.when(decisionId.startsWith(decisionPrefix) &&
        !ModifierPicks.isDecision(decisionId))(
      WalkerRollFeedback(explorePool, target = Some(difficulty)))

  /** A fresh start: the gates, then the tree. */
  def build(catalog: ExecutableCatalog, state: ReadyGame, actor: PlayerId)
      : Either[OathViolation, Operation] =
    refusal(state, actor).map(OathViolation.StartUnavailable(_)).toLeft(())
      .flatMap(_ => rebuild(catalog, state, actor))

  /** The same tree for an Explore already started, with no gate. */
  def rebuild(catalog: ExecutableCatalog, state: ReadyGame, actor: PlayerId)
      : Either[OathViolation, Operation] =
    val rest = exploration(catalog, actor)
    Right(Sequence(Vector(ModifierChoice.loop(actor,
      PowerWindow.ExploreModifierSelection, rest, startLabel), rest)))

  /** The end the explorer chose, or Recent when the Atlas held one site and
    * nothing was asked. */
  def chosenEnd(answered: Vector[Answered]): AtlasEnd =
    answered.reverse.collectFirst {
      case Answered(`endDecisionId`, ChooseOneAnswer(ref), _) => ref
    }.flatMap(AtlasPlacement.endOf)
      .getOrElse(AtlasEnd.Recent)

  /** Tree closes only over the command-stable actor; everything else is
    * read live. */
  private def exploration(catalog: ExecutableCatalog, actor: PlayerId)
      : Operation =
    def succeeded(ready: ReadyGame): Boolean =
      ready.game.current.rollOutcomes.get(explorePool)
        .exists(_.score >= difficulty)

    def stopped(pending: PendingTree): Boolean =
      pending.answered.lastOption.exists:
        case Answered(_, ChooseOneAnswer(selected), _) =>
          selected == stopOption
        case _ => false

    /** Roll again or stop. Without the Supply to roll again that option is
      * shown unavailable, with its reason. */
    def choice(ready: ReadyGame): Decide =
      val again = DecisionOption.Button(continueOption, continueLabel)
      val affordable = ready.game.current.player(actor)
        .exists(_.board.supply.supply >= supplyCost)
      Decide(choiceDecisionId, actor, DecisionQuery.ChooseOne(Vector(
        if affordable then again
        else DecisionOption.Unavailable(again,
          OptionReason.Written(noSupply)),
        DecisionOption.Button(stopOption, stopLabel)),
        heading = Some("Explore")))

    // Payment precedes the roll, so the pipeline rejects a short Supply
    // before any die is asked for.
    val round = Sequence(
      Sequence(Vector(SpendSupply(actor, supplyCost)),
        Some(PowerWindow.ExploreCost)),
      RollStep(explorePool, DiceSpec(DiceKind.Defense), Some(actor),
        Some(PowerWindow.ExploreRoll)),
      Branch((ready, _) =>
        if succeeded(ready) then Vector.empty else Vector(choice(ready))))

    /** Recent or Forgotten, asked while the Atlas holds two sites or more,
      * of World Crafter's holder when one is seated (Legacy Setup powers
      * ruling 11), with their line after the answer. A branch of its own:
      * once the site is placed the Atlas is shorter, and a walk resumed
      * past this node must still find the nodes after it where they were. */
    val endChoice = Branch((ready, _) =>
      if ready.game.campaign.atlas.sites.sizeIs < 2 then Vector.empty
      else
        val chooser = SetupLegacyRules.exploreEndChooser(ready, actor)
        Vector(Decide(endDecisionId, chooser, DecisionQuery.ChooseOne(
          AtlasEnd.all.map(end => DecisionOption.Button(endOption(end),
            AtlasPlacement.endLabel(end))), heading = Some(endHeading)))) ++
          Option.when(chooser != actor)(Note(ProcedureNotes.id(
            ActionRef.Explore), states => Some(WorldCrafter.chose(
            chosenEnd(states.answered)).sourceless(NoteArg.Player(chooser),
            NoteArg.Card(WorldCrafterCard.id))))).toVector)

    /** The site leaves its end of the Atlas for the next slot, and the
      * explorer's pawn is placed there. This is not a Travel. */
    val place = BuildOps((ready, pending) =>
      for
        planned <- AtlasPlacement.at(catalog, ready,
          chosenEnd(pending.answered))
        from <- ready.game.current.pawnSite(actor)
          .toRight(OathViolation.PawnSiteMissing(actor))
      yield Vector[CoreOperation](planned.operation,
        Move(Piece.Pawn(actor), PositionedLocation(Location.Site(from)),
          PositionedLocation(Location.Site(planned.site)))),
      required = true)

    /** The site the Explore placed: the new Hintermost site, since it took
      * the first empty slot going Hinterward. It is read from the live
      * state, never from the step before: a power may ask its own question
      * as the pawn is placed (Brass Army), and the walk then comes back to
      * the nodes below in a later command. */
    def placedSite(ready: ReadyGame): Option[SiteId] =
      ready.game.current.map.hintermost

    /** "Explored Forgotten Site: Found Deep Woods". */
    val exploredNote = Note(ProcedureNotes.id(ActionRef.Explore), states =>
      placedSite(states.now).map(site => explored(chosenEnd(states.answered))
        .sourceless(NoteArg.Site(site))))

    val whenExplored = Branch((ready, _) =>
      placedSite(ready).toVector.map(ExploredSite(_)))

    val failedNote = Note(ProcedureNotes.id(ActionRef.Explore),
      _ => Some(failed.sourceless()))

    val afterLoop = Branch((ready, pending) =>
      if succeeded(ready) then
        Vector(endChoice, place, exploredNote, whenExplored)
      else if stopped(pending) then Vector(failedNote)
      else Vector.empty)

    Sequence(
      ModifyDicePool(explorePool, +2),
      Repeat((ready, pending) => !succeeded(ready) && !stopped(pending),
        round),
      afterLoop)
