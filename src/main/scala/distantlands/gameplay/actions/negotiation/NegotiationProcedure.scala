package distantlands.gameplay.actions.negotiation

import distantlands.catalog.ExecutableCatalog
import distantlands.gameplay.actions.{AdviserLimits, ProcedureNotes}
import distantlands.gameplay.powerresolver.PowerUseRestriction
import distantlands.model._

/** Negotiation on the walker: choose who to negotiate with, then loop one
  * deal decision that every participant may answer until the deal closes,
  * then settle it if it was agreed. A 0-Supply minor action taking no start
  * selection.
  *
  * {{{
  * Sequence(
  *   Decide(negotiators)                  -- omitted when there is one candidate
  *   Repeat(!closed) { Branch -> Decide(deal, co-owned) }
  *   Branch(agreed -> BuildOps(settle), BuildOps(site returns), notes,
  *     per site: Branch -> Decide(warbands), BuildOps(place), note;
  *     Citizenship, promise notes))
  * }}}
  *
  * The deal's `Repeat` and its settlement are [[NegotiationDealTree]]'s,
  * walked under the prefix `negotiation`.
  *
  * The start reads the reach once, under the walk's power-use restrictions,
  * and stores it as walker start facts ([[NegotiationStartFacts]]); a
  * rebuild reads them back. The adviser limit is the powers' knowledge, so
  * the registry passes `AdviserLimit` to `build` and `rebuild`.
  *
  * Whispering Stone queues a private one: its start argument is the
  * private marker, its candidates are every other player, its negotiators
  * question takes one, and its promise lines are read only by the
  * participants.
  *
  * Nothing about the deal is stored: the guard, the query and the settlement
  * all read `PendingTree.answered` (see [[NegotiationDeal]]).
  */
object NegotiationProcedure:
  private val notes = NoteSet(ProcedureNotes.id(ActionRef.Negotiation))
  /** "Negotiated with p2", or "Negotiated with p2 and p3". */
  val negotiated: NoteKey = notes.key("negotiated", Vector(
    NotePart.Text("Negotiated with "), NotePart.Arg(0)))
  /** "Negotiated with p3, in private." (Whispering Stone, ruling 23). */
  val negotiatedPrivately: NoteKey = notes.key("negotiated-private", Vector(
    NotePart.Text("Negotiated with "), NotePart.Arg(0),
    NotePart.Text(", in private.")))
  val ended: NoteKey = notes.key("ended", Vector(
    NotePart.Text("Negotiation ended by "), NotePart.Arg(0)))
  /** "p1 gave 1 favor to p2". */
  val gaveFavor: NoteKey = notes.key("gave-favor", Vector(
    NotePart.Arg(0), NotePart.Text(" gave "), NotePart.Arg(1),
    NotePart.Text(" favor to "), NotePart.Arg(2)))
  val gaveCard: NoteKey = notes.key("gave-card", Vector(
    NotePart.Arg(0), NotePart.Text(" gave "), NotePart.Arg(1),
    NotePart.Text(" to "), NotePart.Arg(2)))
  /** "p2 gave Broken Peaks to p1" (Deed Writer, catalog batch 12, ruling
    * 18). */
  val gaveSite: NoteKey = notes.key("gave-site", Vector(
    NotePart.Arg(0), NotePart.Text(" gave "), NotePart.Arg(1),
    NotePart.Text(" to "), NotePart.Arg(2)))
  /** "p1 placed 2 warbands at Broken Peaks" (ruling 20). */
  val placedWarbands: NoteKey = notes.key("placed-warbands", Vector(
    NotePart.Arg(0), NotePart.Text(" placed "), NotePart.Arg(1),
    NotePart.Text(" "), NotePart.Plural(1, "warband", "warbands"),
    NotePart.Text(" at "), NotePart.Arg(2)))
  /** "p2 showed p1 Birdsong". */
  val showed: NoteKey = notes.key("showed", Vector(
    NotePart.Arg(0), NotePart.Text(" showed "), NotePart.Arg(1),
    NotePart.Text(" "), NotePart.Arg(2)))
  /** "p1 was shown Sticky Fire at Broken Peaks". */
  val wasShown: NoteKey = notes.key("was-shown", Vector(
    NotePart.Arg(0), NotePart.Text(" was shown "), NotePart.Arg(1),
    NotePart.Text(" at "), NotePart.Arg(2)))
  /** "p1 offered Citizenship to p2 and p3." */
  val offeredCitizenship: NoteKey = notes.key("offered-citizenship", Vector(
    NotePart.Arg(0), NotePart.Text(" offered Citizenship to "),
    NotePart.Arg(1), NotePart.Text(".")))
  /** "p2 became a Citizen.", after an agreed offer. */
  val becameCitizen: NoteKey = notes.key("became-citizen", Vector(
    NotePart.Arg(0), NotePart.Text(" became a Citizen.")))
  /** "p2 became the Chancellor.", in a game that had no Empire. */
  val becameChancellor: NoteKey = notes.key("became-chancellor", Vector(
    NotePart.Arg(0), NotePart.Text(" became the Chancellor.")))
  /** "p1 promised p2 and p3: “…”" (catalog batch 12, ruling 12). */
  val promised: NoteKey = notes.key("promised", Vector(
    NotePart.Arg(0), NotePart.Text(" promised "), NotePart.Arg(1),
    NotePart.Text(": "), NotePart.Arg(2)))
  /** "p1 made a binding promise to p2: “…”" (ruling 11). */
  val promisedBinding: NoteKey = notes.key("promised-binding", Vector(
    NotePart.Arg(0), NotePart.Text(" made a binding promise to "),
    NotePart.Arg(1), NotePart.Text(": "), NotePart.Arg(2)))
  def noteKeys: Vector[NoteKey] = notes.declared

  /** The one start argument of a private Negotiation, which only a
    * power's queued `Run` passes (Whispering Stone, ruling 21). */
  val privateArgs: Vector[DecisionOptionRef] =
    Vector(DecisionOptionRef.Button("negotiation.private"))

  /** Whether `actor`'s Negotiation walk is a power's private one: its
    * entry is a sourced `Run` of the Negotiation carrying the private
    * marker. A menu Negotiation has no entry. */
  def queuedPrivately(state: ReadyGame, actor: PlayerId): Boolean =
    state.game.current.walkerEntry.exists(entry =>
      entry.procedure == ActionRef.Negotiation && entry.actor == actor &&
        entry.source.nonEmpty && entry.kind == QueueKind.Run &&
        entry.startArgs == privateArgs)

  /** A fresh start: the reach is read once, under the walk's power-use
    * restrictions, and refused when it holds nobody. A private start
    * offers every other player. */
  def build(limits: AdviserLimits,
      restrictions: Vector[PowerUseRestriction])(
      catalog: ExecutableCatalog, state: ReadyGame, actor: PlayerId,
      args: Vector[DecisionOptionRef]): Either[OathViolation, Operation] =
    for
      privately <- marked(args)
      _ <- Either.cond(!privately || queuedPrivately(state, actor), (),
        refusedArgs(args))
      facts = NegotiationStartFacts.at(catalog, state, actor, restrictions,
        privately)
      _ <- Either.cond(facts.candidates.nonEmpty, (),
        OathViolation.NegotiationUnavailable(
          "no other player is at your site or within reach"))
    yield tree(catalog, limits, actor, facts, privately)

  /** A resume rebuilds what the start built, from the facts it stored and
    * the start arguments. */
  def rebuild(limits: AdviserLimits)(catalog: ExecutableCatalog,
      state: ReadyGame, actor: PlayerId,
      args: Vector[DecisionOptionRef]): Either[OathViolation, Operation] =
    for
      privately <- marked(args)
      facts <- NegotiationStartFacts.read(state.game.current.walkerStartFacts)
    yield tree(catalog, limits, actor, facts, privately)

  /** What the start stores for every rebuild ([[NegotiationStartFacts]]).
    * The registry passes no start arguments here, so a private start is
    * read from the walk's entry. */
  def startFacts(restrictions: Vector[PowerUseRestriction])(
      catalog: ExecutableCatalog, state: ReadyGame,
      actor: PlayerId): Vector[DecisionOptionRef] =
    NegotiationStartFacts.at(catalog, state, actor, restrictions,
      queuedPrivately(state, actor)).refs

  /** What the start arguments say: none for a menu Negotiation, the
    * private marker for a power's private one. */
  private def marked(args: Vector[DecisionOptionRef])
      : Either[OathViolation, Boolean] =
    if args.isEmpty then Right(false)
    else if args == privateArgs then Right(true)
    else Left(refusedArgs(args))

  private def refusedArgs(args: Vector[DecisionOptionRef]): OathViolation =
    OathViolation.InvalidEventOrder(
      s"walker procedure ${ActionRef.Negotiation.key} takes no start " +
        "selection but a power's private marker, got " +
        args.map(_.kind).mkString(", "))

  private def tree(catalog: ExecutableCatalog, limits: AdviserLimits,
      actor: PlayerId, facts: NegotiationStartFacts,
      privately: Boolean): Operation =
    val candidates = facts.candidates
    val choose: Vector[Operation] =
      if candidates.size < 2 then Vector.empty
      else Vector(Decide(NegotiationDeal.negotiatorsDecisionId, actor,
        DecisionQuery.ChooseMany(1, if privately then 1 else candidates.size,
          candidates.map(id =>
            DecisionOption.Player(DecisionOptionRef.Player(id))),
          heading = Some(if privately then
            "Choose who to negotiate with in private"
            else "Choose who to negotiate with"),
          confirm = Some(CountedConfirm("Negotiate with", "player",
            "players"))),
        window = Some(PowerWindow.NegotiationEligibility)))
    Sequence(choose ++ NegotiationDealTree(catalog, limits, DealWalk(
      NegotiationDeal.NegotiationPrefix, actor,
      (ready, pending) => NegotiationDeal.participants(ready, actor,
        candidates, pending),
      participants => facts.rules(participants)
        .copy(privateDeal = privately),
      pending => (if privately then negotiatedPrivately else negotiated)
        .sourceless(NoteArg.Players(NegotiationDeal.chosen(candidates,
          pending))))))
