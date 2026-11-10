package distantlands.gameplay.actions.negotiation

import distantlands.catalog.ExecutableCatalog
import distantlands.gameplay.actions.ProcedureNotes
import distantlands.gameplay.powerresolver.PowerUseRestriction
import distantlands.gameplay.roles.Citizenship
import distantlands.model._
import distantlands.model.DecisionAnswer.DeclineDeal

/** Negotiation on the walker: choose who to negotiate with, then loop one
  * deal decision that every participant may answer until the deal closes,
  * then settle it if it was agreed. A 0-Supply minor action taking no start
  * selection.
  *
  * {{{
  * Sequence(
  *   Decide(negotiators)                  -- omitted when there is one candidate
  *   Repeat(!closed) { Branch -> Decide(deal, co-owned) }
  *   Branch(agreed -> BuildOps(settle), notes, Citizenship, promise notes))
  * }}}
  *
  * The start reads the reach once, under the walk's power-use restrictions,
  * and stores it as walker start facts ([[NegotiationStartFacts]]); a
  * rebuild reads them back.
  *
  * Nothing about the deal is stored: the guard, the query and the settlement
  * all read `PendingTree.answered` (see [[NegotiationDeal]]).
  */
object NegotiationProcedure:
  private val notes = NoteSet(ProcedureNotes.id(ActionRef.Negotiation))
  /** "Negotiated with p2", or "Negotiated with p2 and p3". */
  val negotiated: NoteKey = notes.key("negotiated", Vector(
    NotePart.Text("Negotiated with "), NotePart.Arg(0)))
  val ended: NoteKey = notes.key("ended", Vector(
    NotePart.Text("Negotiation ended by "), NotePart.Arg(0)))
  /** "p1 gave 1 favor to p2". */
  val gaveFavor: NoteKey = notes.key("gave-favor", Vector(
    NotePart.Arg(0), NotePart.Text(" gave "), NotePart.Arg(1),
    NotePart.Text(" favor to "), NotePart.Arg(2)))
  val gaveCard: NoteKey = notes.key("gave-card", Vector(
    NotePart.Arg(0), NotePart.Text(" gave "), NotePart.Arg(1),
    NotePart.Text(" to "), NotePart.Arg(2)))
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

  /** A fresh start: the reach is read once, under the walk's power-use
    * restrictions, and refused when it holds nobody. */
  def build(restrictions: Vector[PowerUseRestriction])(
      catalog: ExecutableCatalog, state: ReadyGame, actor: PlayerId,
      args: Vector[DecisionOptionRef]): Either[OathViolation, Operation] =
    for
      _ <- noStartArgs(args)
      facts = NegotiationStartFacts.at(catalog, state, actor, restrictions)
      _ <- Either.cond(facts.candidates.nonEmpty, (),
        OathViolation.NegotiationUnavailable(
          "no other player is at your site or within reach"))
    yield tree(actor, facts)

  /** A resume rebuilds what the start built, from the facts it stored. */
  def rebuild(catalog: ExecutableCatalog, state: ReadyGame, actor: PlayerId,
      args: Vector[DecisionOptionRef]): Either[OathViolation, Operation] =
    for
      _ <- noStartArgs(args)
      facts <- NegotiationStartFacts.read(state.game.current.walkerStartFacts)
    yield tree(actor, facts)

  /** What the start stores for every rebuild ([[NegotiationStartFacts]]). */
  def startFacts(restrictions: Vector[PowerUseRestriction])(
      catalog: ExecutableCatalog, state: ReadyGame,
      actor: PlayerId): Vector[DecisionOptionRef] =
    NegotiationStartFacts.at(catalog, state, actor, restrictions).refs

  private def noStartArgs(args: Vector[DecisionOptionRef])
      : Either[OathViolation, Unit] = Either.cond(args.isEmpty, (),
    OathViolation.InvalidEventOrder(
      s"walker procedure ${ActionRef.Negotiation.key} takes no start " +
        s"selection, got ${args.map(_.kind).mkString(", ")}"))

  private def tree(actor: PlayerId, facts: NegotiationStartFacts): Operation =
    val candidates = facts.candidates
    val choose: Vector[Operation] =
      if candidates.size < 2 then Vector.empty
      else Vector(Decide(NegotiationDeal.negotiatorsDecisionId, actor,
        DecisionQuery.ChooseMany(1, candidates.size, candidates.map(id =>
          DecisionOption.Player(DecisionOptionRef.Player(id))),
          heading = Some("Choose who to negotiate with"),
          confirm = Some(CountedConfirm("Negotiate with", "player",
            "players"))),
        window = Some(PowerWindow.NegotiationEligibility)))
    Sequence(choose ++ Vector[Operation](
      Repeat((ready, pending) =>
        !NegotiationDeal.deal(ready, actor, candidates, pending).closed,
        Branch((ready, pending) => {
          val deal = NegotiationDeal.deal(ready, actor, candidates, pending)
          Vector(Decide(NegotiationDeal.dealDecisionId, actor,
            NegotiationDeal.snapshot(ready, deal,
              facts.bindingPromises(deal.participants)),
            coOwners = deal.participants.filter(_ != actor)))
        })),
      Branch((ready, pending) => {
        val deal = NegotiationDeal.deal(ready, actor, candidates, pending)
        // The opening line, as the rule posted it: the decline names who
        // ended the deal, and anything else names who was negotiated with,
        // whether or not the deal had substance to settle.
        val opening: Vector[Operation] = Vector(
          Note(ProcedureNotes.id(ActionRef.Negotiation),
            if deal.declined then endedNote
            else _ => Some(negotiated.sourceless(NoteArg.Players(
              NegotiationDeal.chosen(candidates, pending))))))
        // The settlement's lines, in the order `settle` applies them:
        // disclosures first, then transfers. A refused settlement fails the
        // command, so nothing is journaled and no note is reached.
        //
        // They state the agreement rather than the journal: the deleted
        // rule read the executed operations, these are derived from the
        // same `DealState` the settling node is. No power contributes at
        // `NegotiationSettlement`; a first one that removed an operation
        // would leave its line standing, so it must not.
        val settlement: Vector[Operation] =
          if !deal.agreed then Vector.empty
          else BuildOps((state, _) => NegotiationDeal.settle(state, deal),
            window = Some(PowerWindow.NegotiationSettlement)) +:
            (disclosed(deal) ++ transferred(deal) ++ offered(deal)).map(note =>
              Note(ProcedureNotes.id(ActionRef.Negotiation), _ => Some(note)))
        // Each Exile offered Citizenship becomes Imperial after the
        // transfers, in participant order; each may be asked their own
        // warband split (CR p. 34).
        val citizenship: Vector[Operation] =
          if !deal.agreed then Vector.empty
          else NegotiationDeal.citizens(deal).flatMap(citizen => Vector(
            Citizenship.become(citizen),
            Note(ProcedureNotes.id(ActionRef.Negotiation),
              becameNote(_, citizen))))
        // Each promise's line, last: the deal's pieces have all moved and
        // its roles changed by now. The engine enforces none of them.
        val promises: Vector[Operation] =
          if !deal.agreed then Vector.empty
          else promisedLines(deal).map(note =>
            Note(ProcedureNotes.id(ActionRef.Negotiation), _ => Some(note)))
        opening ++ settlement ++ citizenship ++ promises
      })))

  private def endedNote(states: NoteStates): Option[LogNote] =
    states.answered.reverse.collectFirst {
      case Answered(NegotiationDeal.dealDecisionId, DeclineDeal, by) =>
        ended.sourceless(NoteArg.Player(by))
    }

  /** One line per disclosure, in `NegotiationDeal.knowledge`'s order. */
  private def disclosed(deal: DealState): Vector[LogNote] =
    deal.participants.flatMap(deal.terms(_).disclosures).map:
      case NegotiationDisclosure(recipient,
          NegotiationDisclosureRef.Adviser(owner, card)) =>
        showed.sourceless(NoteArg.Player(owner), NoteArg.Player(recipient),
          NoteArg.Card(card))
      case NegotiationDisclosure(recipient,
          NegotiationDisclosureRef.HeldRelic(owner, relic)) =>
        showed.sourceless(NoteArg.Player(owner), NoteArg.Player(recipient),
          NoteArg.Card(relic))
      case NegotiationDisclosure(recipient,
          NegotiationDisclosureRef.SiteRelic(site, relic)) =>
        wasShown.sourceless(NoteArg.Player(recipient), NoteArg.Card(relic),
          NoteArg.Site(site))

  /** One line per transfer, in `NegotiationDeal.transfers`' order: each
    * author's favor before their relics. */
  private def transferred(deal: DealState): Vector[LogNote] =
    deal.participants.flatMap(author =>
      deal.terms(author).transfers.flatMap(transfer =>
        Option.when(transfer.favor > 0)(gaveFavor.sourceless(
          NoteArg.Player(author), NoteArg.Number(transfer.favor),
          NoteArg.Player(transfer.recipient))).toVector ++
          transfer.relics.map(relic => gaveCard.sourceless(
            NoteArg.Player(author), NoteArg.Card(relic),
            NoteArg.Player(transfer.recipient)))))

  /** The line for `citizen`'s new role, once the change has run. */
  private def becameNote(states: NoteStates, citizen: PlayerId)
      : Option[LogNote] = Imperials.role(states.now, citizen).collect {
    case Role.Chancellor => becameChancellor.sourceless(NoteArg.Player(citizen))
    case Role.Citizen => becameCitizen.sourceless(NoteArg.Player(citizen))
  }

  /** One line per author who offered Citizenship. */
  private def offered(deal: DealState): Vector[LogNote] =
    deal.participants.flatMap(author =>
      Option.when(deal.terms(author).citizenship.nonEmpty)(
        offeredCitizenship.sourceless(NoteArg.Player(author),
          NoteArg.Players(deal.terms(author).citizenship))).toVector)

  /** One line per promise, by author in participant order, the plain
    * promises before the binding ones. Each names the other participants. */
  private def promisedLines(deal: DealState): Vector[LogNote] =
    deal.participants.flatMap { author =>
      val others = NoteArg.Players(deal.participants.filter(_ != author))
      val terms = deal.terms(author)
      terms.promises.map(text => promised.sourceless(NoteArg.Player(author),
        others, NoteArg.Promise(text))) ++
        terms.bindingPromises.map(text => promisedBinding.sourceless(
          NoteArg.Player(author), others, NoteArg.Promise(text)))
    }
