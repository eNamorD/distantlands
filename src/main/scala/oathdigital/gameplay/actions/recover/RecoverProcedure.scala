package oathdigital.gameplay.actions.recover

import oathdigital.catalog.ExecutableCatalog
import oathdigital.gameplay.actions.{ProcedureNotes, RecoverRules}
import oathdigital.gameplay.actions.cardplay.CardSlots
import oathdigital.gameplay.actions.dice.RollStep
import oathdigital.gameplay.powerresolver.ModifierPicks
import oathdigital.gameplay.walker.{ModifierChoice, WalkerRollFeedback}
import oathdigital.model._
import oathdigital.model.DecisionAnswer.ChooseOneAnswer

/** Declared Recover procedure tree for the [[oathdigital.gameplay.walker.ProcedureWalker]]}.
  *
  * {{{
  * Sequence(
  *   Repeat(ModifierChoice),                  // the modifiers, one per
  *                                            // pass, then "Start Recover"
  *   Sequence(                                // window = RecoverActionEligibility
  *     ModifyDicePool("recover", +2),         // window = RecoverBeforeFirstRoll
  *     Repeat(guard = has difficulty && not succeeded && lastChoice != Stop,
  *       Sequence(
  *         SpendSupply(actor, 1),            // window = RecoverCost
  *         Roll("recover", Defense, Automatic), // window = RecoverRoll;
  *                                              // rolls in the same command
  *         Branch(choice when not yet success) // -> Decide("recover.choice") or nothing
  *       )),
  *     Branch(if no difficulty -> Vector(BuildOps(refuse)),
  *       else if success ->
  *       Vector(Decide("recover.relic"),
  *         Sequence(BuildOps(move chosen relic facedown),
  *           Note(recovered))),              // window = RecoverAfterRelic
  *       else if stopped -> Vector(Note(failed)),
  *       else Vector.empty)))                 // succeeded with no relic
  * }}}
  *
  * Descriptions of PowerWindows:
  * `RecoverActionEligibility` - At the start of the Recover itself, after the modifier decision
  * `RecoverBeforeFirstRoll` - Before the very first roll
  * `RecoverAfterRelic` - After the relic is chosen
  * `RecoverRoll` - Each roll, before its reroll choice
  *
  * Semantics:
  *  - Each roll = 2 defense dice (pool count fixed to 2 by the head
  *    `ModifyDicePool`) and costs 1 supply, debited by `SpendSupply`.
  *  - The roll is automatic: the supply is spent before the dice are asked
  *    for, so a park in front of them would ask the player to confirm a
  *    roll they have already paid for and cannot decline. The walk rolls
  *    and carries on to the first thing that is a question.
  *  - Success = `DefenseDieFace.score` over the combined faces from every roll
  *    of the "recover" pool (the walker accumulates roll outcomes per pool)
  *    reaching [[difficulty]]: 5 once Drowned Library is picked
  *    ([[fromRelicDeck]]), else the site's. The site is the actor's pawn
  *    site. Thus a Doubler on a later roll multiplies earlier shields.
  *  - A FAILED roll parks the continue/stop choice: Continue rolls again,
  *    Stop abandons with no relic. The choice is only reachable while the
  *    recovery has not yet succeeded, because the `Branch` carrying it
  *    declares nothing once it has.
  *  - A successful roll parks a relic decision only when a facedown relic is
  *    available. Otherwise Recover finishes as a legal wasted action.
  *  - With Drowned Library picked, the relic decision offers the relic deck
  *    instead of the site's relics, and the move takes its top card, unseen
  *    until recovered.
  *
  * A Recover with no difficulty is refused after its modifier decision,
  * not when the tree is built, since Drowned Library makes any site
  * recoverable: with no difficulty the `Repeat` runs no round, and the
  * last branch refuses. "Start" and the Act menu judge the tree after the
  * picks, so they refuse it too. Generic supply feasibility belongs to
  * `OperationPipeline`; role, foundation state, and relic availability do
  * not gate Recover.
  */
object RecoverProcedure:
  val recoverPool: PoolKey = PoolKey("recover")
  val choiceDecisionId: String = "recover.choice"
  val relicDecisionId: String = "recover.relic"

  /** Synthetic decision id of a park on a Roll node itself (a `Roll` leaf
    * carries no `decisionId` of its own -- that concept only exists on
    * `Decide` nodes). Recover's own roll is automatic and never parks, and
    * no command answers a parked roll since the parked-roll retirement
    * (2026-10-23); the id stays declared so `rollFeedback` and the engine's
    * answerability check keep one name for the roll.
    */
  val rollDecisionId: String = "walker.recover.roll"

  /** Every Recover decision's id starts here, and so does the id of a
    * question a Recover modifier asks (Silver Charm's). */
  val decisionPrefix: String = "recover."

  private val supplyCost: Int = 1

  private val notes = NoteSet(ProcedureNotes.id(ActionRef.Recover))
  /** "Recovered Sticky Fire at Broken Peaks". */
  val recovered: NoteKey = notes.key("recovered", Vector(
    NotePart.Text("Recovered "), NotePart.Arg(0), NotePart.Text(" at "),
    NotePart.Arg(1)))
  /** "Failed to recover at Broken Peaks": the player answered Stop. A
    * Recover that succeeded with no relic to take writes nothing, as the
    * rule this replaced wrote nothing. */
  val failed: NoteKey = notes.key("failed", Vector(
    NotePart.Text("Failed to recover at "), NotePart.Arg(0)))
  def noteKeys: Vector[NoteKey] = notes.declared

  /** Every park of a Recover but its modifier decision shows the same
    * thing: the pool rolled so far and the Recover's difficulty, read with
    * the parked walk's answers, which is worth showing before the first
    * roll too. A question a Recover modifier asks
    * shows it as well, since its id starts with [[decisionPrefix]]. */
  def rollFeedback(catalog: ExecutableCatalog, ready: ReadyGame,
      actor: PlayerId, decisionId: String): Option[WalkerRollFeedback] =
    Option.when(decisionId == rollDecisionId ||
        (decisionId.startsWith(decisionPrefix) &&
          !ModifierPicks.isDecision(decisionId)))(
      WalkerRollFeedback(recoverPool,
        target = difficulty(catalog, ready, actor,
          ready.game.current.walkerPending.fold(Vector.empty[Answered])(
            _.answered)))).filter(_.target.nonEmpty)

  /** The actor's current pawn site -- the single definition `build`,
    * `rebuild`, and [[actorFacedownRelics]] all read, so nothing in this
    * module (or a caller outside it) can derive "the Recover site" a
    * different way and silently disagree with the others.
    */
  def actorSite(state: ReadyGame, actor: PlayerId): Option[SiteId] =
    state.game.current.pawnSite(actor)

  /** The facedown relics at the actor's current site -- exactly the options
    * the relic decision declares, read live off `state.game.current.map.sites`
    * rather than off the tree's closed-over `siteId`. The application-layer
    * projector projects that declared query, so the candidates a client is
    * offered and the set the resolver accepts cannot drift apart: there is
    * exactly one definition of "the actor's recoverable relics", not two
    * expressions that merely happen to agree today.
    */
  def actorFacedownRelics(state: ReadyGame, actor: PlayerId)
      : Vector[RelicState] =
    actorSite(state, actor).flatMap(state.game.current.map.sites.get).fold(
      Vector.empty[RelicState])(_.relics.filter(
      _.orientation == Orientation.FaceDown))

  /** "Start Recover": the modifier decision's button. */
  val startLabel: String = "Start Recover"

  /** Why a Recover with no difficulty is refused. */
  val noDifficulty: String = "site has no Recover Difficulty"

  /** Drowned Library's difficulty: "Its recover difficulty is 5." */
  val relicDeckDifficulty: Int = 5

  /** Whether the walk's modifier decision picked Drowned Library (E13,
    * ruined), whose Recover takes the relic deck's top card. Read from the
    * answers, which a park keeps and replay rebuilds, as `EffectiveSuit`
    * reads Acting Troupe's pick. */
  def fromRelicDeck(answered: Vector[Answered]): Boolean =
    ModifierPicks.isPicked(answered, PowerWindow.RecoverModifierSelection,
      UndergroundLibraryCard.id)

  /** The relic decision's one option with Drowned Library picked: the deck,
    * so the relic stays unseen until recovered. */
  val relicDeckOption: DecisionOptionRef.Deck =
    DecisionOptionRef.Deck(CardDeck.Relic)

  /** The difficulty of the actor's Recover: Drowned Library's once it is
    * picked, else the actor's pawn site's, if it has one. */
  def difficulty(catalog: ExecutableCatalog, ready: ReadyGame,
      actor: PlayerId, answered: Vector[Answered]): Option[Int] =
    if fromRelicDeck(answered) then Some(relicDeckDifficulty)
    else actorSite(ready, actor).flatMap(RecoverRules.difficulty(catalog, _))

  def build(catalog: ExecutableCatalog, state: ReadyGame,
      actor: PlayerId)
      : Either[OathViolation, Operation] = for
    built <- rebuild(catalog, state, actor)
  yield built

  /** The Recover itself, after its modifier decision, behind the start's
    * gates. */
  def afterModifiers(catalog: ExecutableCatalog, state: ReadyGame,
      actor: PlayerId): Either[OathViolation, Operation] = for
    rest <- recoveryAt(catalog, state, actor)
  yield rest

  /** Rebuilds the same command-local tree for an already-started Recover:
    * the modifier decision, then the Recover. */
  def rebuild(catalog: ExecutableCatalog, state: ReadyGame,
      actor: PlayerId): Either[OathViolation, Operation] =
    recoveryAt(catalog, state, actor).map(rest =>
      Sequence(Vector(ModifierChoice.loop(actor,
        PowerWindow.RecoverModifierSelection, rest, startLabel), rest)))

  /** The Recover at the actor's site. */
  private def recoveryAt(catalog: ExecutableCatalog, state: ReadyGame,
      actor: PlayerId): Either[OathViolation, Operation] = for
    siteId <- actorSite(state, actor).toRight(
      OathViolation.PawnSiteMissing(actor))
  yield recovery(catalog, actor, siteId)

  /** Tree closes only over command-stable actor and site; the difficulty
    * and the relic source are read live, with the walk's answers. */
  private def recovery(catalog: ExecutableCatalog, actor: PlayerId,
      siteId: SiteId): Operation =
    def scoreOf(ready: ReadyGame): Int =
      ready.game.current.rollOutcomes.get(recoverPool).fold(0)(_.score)

    def target(ready: ReadyGame, answered: Vector[Answered]): Option[Int] =
      difficulty(catalog, ready, actor, answered)

    def succeeded(ready: ReadyGame, answered: Vector[Answered]): Boolean =
      target(ready, answered).exists(scoreOf(ready) >= _)

    // The two buttons the continue/stop decision offers, declared once: the
    // query the player is shown, the set the walker accepts an answer from,
    // and the `Repeat` guard below all read these same two references.
    val continueOption = DecisionOptionRef.Button("continue")
    val stopOption = DecisionOptionRef.Button("stop")

    def stopped(pending: PendingTree): Boolean =
      pending.answered.lastOption.exists:
        case Answered(_, ChooseOneAnswer(selected), _) => selected == stopOption
        case _ => false

    // No `validate` closure guards this node, and that is not a lost check.
    // Its whole body rejected a continue-or-stop answer once the recovery had
    // already succeeded -- and the `Branch` carrying this node already omits
    // it in exactly that case, so on a rebuilt tree a stale choice answer
    // finds no matching `Decide` to resume at and is rejected before any
    // query is consulted.
    val choiceDecide = Decide(
      decisionId = choiceDecisionId,
      owner = actor,
      query = DecisionQuery.ChooseOne(Vector(
        DecisionOption.Button(continueOption, "Continue"),
        DecisionOption.Button(stopOption, "Stop")),
        // The panel's title, declared here with the button copy it frames
        // (plan ruling R4). The Roll park above it keeps a frontend literal
        // instead: a `Roll` node asks nothing, so it has no query to carry
        // a heading on.
        heading = Some("Recover")))

    /** The recovered relic's move to the actor, facedown. */
    def relicMove(relicId: RelicId): Move = Move(Piece.Card(relicId),
      PositionedLocation(Location.Site(siteId)),
      PositionedLocation(Location.PlayArea(actor)),
      resultingOrientation = Some(Orientation.FaceDown), by = Some(actor))

    /** The relic deck's top card's move to the actor, facedown. */
    def deckMove(relicId: RelicId): Move = Move(Piece.Card(relicId),
      PositionedLocation(Location.Deck(CardDeck.Relic), StackPosition.Top),
      PositionedLocation(Location.PlayArea(actor)),
      resultingOrientation = Some(Orientation.FaceDown), by = Some(actor))

    /** What a successful Recover may take, each with the move that takes
      * it: the relic deck's top card, shown as the deck, with Drowned
      * Library picked; else each facedown relic at the actor's live site. */
    def takeable(ready: ReadyGame, answered: Vector[Answered])
        : Vector[(DecisionOption, Move)] =
      if fromRelicDeck(answered) then
        ready.game.current.commonCards.relicDeck.headOption.toVector.map(
          top => DecisionOption.Deck(relicDeckOption) -> deckMove(top))
      else actorFacedownRelics(ready, actor).map(relic =>
        DecisionOption.Relic(DecisionOptionRef.Relic(relic.id)) ->
          relicMove(relic.id))

    val moveRelic = BuildOps((ready, pending) =>
      pending.answered.lastOption match {
        case Some(Answered(_, ChooseOneAnswer(ref), _)) =>
          takeable(ready, pending.answered).collectFirst {
            case (option, move) if option.ref == ref =>
              Vector[CoreOperation](move)
          }.toRight(OathViolation.InvalidEventOrder(
            s"${ref.wireId} is not a relic this Recover may take"))
        case _ => Left(OathViolation.InvalidEventOrder(
          "no recovered relic answer is recorded"))
      })

    /** The relic taken and the site it was recovered at. A site relic is
      * read from the relic decision's answer; the relic deck's top card from
      * the state before the move, the note's previous step, since a power
      * hooked at `RecoverAfterRelic` appends its leaves after the note. The
      * site is the one this tree closed over. */
    val recoveredNote = Note(ProcedureNotes.id(ActionRef.Recover), states =>
      states.answered.reverse.collectFirst {
        case Answered(`relicDecisionId`, ChooseOneAnswer(ref), _) => ref
      }.flatMap {
        case DecisionOptionRef.Relic(relic) => Some(relic)
        case `relicDeckOption` => states.previous.flatMap(
          _._1.game.current.commonCards.relicDeck.headOption)
        case _ => None
      }.map(relic => recovered.sourceless(NoteArg.Card(relic),
        NoteArg.Site(siteId))))

    /** "Failed to recover at Broken Peaks", read from the same `stopped`
      * the `Repeat` guard reads, so the line and the loop's own exit agree
      * on what ended the Recover. */
    val failedNote = Note(ProcedureNotes.id(ActionRef.Recover),
      _ => Some(failed.sourceless(NoteArg.Site(siteId))))

    // Payment precedes Roll so OperationPipeline rejects insufficient supply
    // before randomness is requested.
    val body = Sequence(
      Sequence(Vector(SpendSupply(actor, supplyCost)),
        Some(PowerWindow.RecoverCost)),
      RollStep(recoverPool, DiceSpec(DiceKind.Defense), Some(actor),
        Some(PowerWindow.RecoverRoll)),
      Branch((ready, pending) =>
        if succeeded(ready, pending.answered) then Vector.empty
        else Vector(choiceDecide)))

    val repeatGuard: (ReadyGame, PendingTree) => Boolean =
      (ready, pending) => target(ready, pending.answered).nonEmpty &&
        !succeeded(ready, pending.answered) && !stopped(pending)

    // Empty-site success is legal and finishes without a decision.
    //
    // The relic query is built from `actorFacedownRelics(ready, actor)` --
    // the actor's LIVE pawn site, re-derived from `ready` on every command --
    // rather than from this closure's own `siteId`, which froze at
    // build/rebuild time. That one method is also what the projector reads to
    // offer a client its candidates, so the offered set and the accepted set
    // are the SAME expression rather than two that agree only by convention.
    // A relic that left the site between the park and the answer is simply
    // absent from the rebuilt query, so the answer naming it is rejected.
    // Each relic is judged as card play judges a slot, so a relic Ancient
    // Bloodline locks is askable of its holder.
    // A Recover the player stopped declares its own line in the third
    // branch, which is the only place the abandoned Recover is named.
    //
    // With no difficulty the `Repeat` ran no round, and this branch refuses,
    // so the refusal is judged after the modifier decision.
    val refused = BuildOps((_, _) =>
      Left(OathViolation.RecoverUnavailable(noDifficulty)))

    val afterLoop = Branch((ready, pending) => {
      val options = takeable(ready, pending.answered)
      if target(ready, pending.answered).isEmpty then Vector(refused)
      else if succeeded(ready, pending.answered) && options.nonEmpty then
        Vector(Decide(
          decisionId = relicDecisionId,
          owner = actor,
          query = DecisionQuery.ChooseOne(options.flatMap((option, move) =>
            CardSlots.judge(catalog, ready, actor, option,
              Right(Vector(move)))),
            heading = Some("Take a relic"), confirmVerb = Some("Take"))),
          // The move and its line in one windowed node: a power hooked at
          // `RecoverAfterRelic` appends to the node the window names, so a
          // note outside it would read after that power's leaves and a line
          // one of them posted would precede the line saying what was
          // recovered (`RelicWorshipLogSuite`).
          Sequence(Vector(moveRelic, recoveredNote),
            Some(PowerWindow.RecoverAfterRelic)))
      else if stopped(pending) then Vector(failedNote)
      else Vector.empty
    })

    Sequence(
      ModifyDicePool(recoverPool, +2,
        window = Some(PowerWindow.RecoverBeforeFirstRoll)),
      Repeat(repeatGuard, body),
      afterLoop
    ).copy(window = Some(PowerWindow.RecoverActionEligibility))
