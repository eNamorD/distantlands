package distantlands.gameplay.actions.negotiation

import distantlands.catalog.ExecutableCatalog
import distantlands.gameplay.actions.{AdviserLimits, ProcedureNotes}
import distantlands.gameplay.roles.Citizenship
import distantlands.model._
import distantlands.model.DecisionAnswer.DeclineDeal

/** How one deal walks (catalog batch 12, slice 6a): the prefix of its
  * decision ids, the player who owns its decision, and, as the answers so
  * far give them, its participants, its rules and the line that opens a
  * deal nobody declined. The Negotiate action walks one under
  * `NegotiationDeal.NegotiationPrefix`; The Gathering walks one per deal
  * (slice 6b), with `bystanders`, its players at the site outside the
  * deal, who co-own the decision only to pass, and `passed`, who has
  * passed. */
final case class DealWalk(prefix: String, owner: PlayerId,
    participants: (ReadyGame, PendingTree) => Vector[PlayerId],
    rules: Vector[PlayerId] => DealRules,
    opened: PendingTree => LogNote,
    bystanders: (ReadyGame, PendingTree) => Vector[PlayerId] =
      (_, _) => Vector.empty,
    passed: PendingTree => Vector[PlayerId] = _ => Vector.empty)

/** One deal's walk, as the operations a procedure puts in its tree:
  *
  * {{{
  * Repeat(!closed) { Branch -> Decide(<prefix>.deal, co-owned) }
  * Branch(opening line; agreed -> BuildOps(settle), BuildOps(site returns),
  *   notes, per site: Branch -> Decide(<prefix>.warbands.<site>),
  *   BuildOps(place), note; Citizenship, promise notes)
  * }}}
  *
  * Its lines are the Negotiate action's keys ([[NegotiationProcedure]]).
  * In a private deal (`DealRules.privateDeal`) only the participants read
  * the promise lines (ruling 23).
  */
object NegotiationDealTree:
  def apply(catalog: ExecutableCatalog, limits: AdviserLimits,
      walk: DealWalk): Vector[Operation] = Vector(
    Repeat((ready, pending) => !deal(walk, ready, pending).closed,
      Branch((ready, pending) => {
        val now = deal(walk, ready, pending)
        val bystanders = walk.bystanders(ready, pending)
        Vector(Decide(NegotiationDeal.dealId(walk.prefix), walk.owner,
          NegotiationDeal.snapshot(catalog, ready, now,
            walk.rules(now.participants), limits).copy(
            bystanders = bystanders,
            passed = walk.passed(pending).filter(bystanders.contains)),
          coOwners = (now.participants ++ bystanders)
            .filter(_ != walk.owner)))
      })),
    Branch((ready, pending) => settlement(catalog, walk, ready, pending)))

  private def deal(walk: DealWalk, ready: ReadyGame,
      pending: PendingTree): DealState =
    NegotiationDeal.fold(walk.prefix, walk.participants(ready, pending),
      pending.answered)

  private def settlement(catalog: ExecutableCatalog, walk: DealWalk,
      ready: ReadyGame, pending: PendingTree): Vector[Operation] =
    val deal = this.deal(walk, ready, pending)
    // The opening line: the decline names who ended the deal, and anything
    // else is the walk's own line, whether or not the deal had substance.
    val opening: Vector[Operation] = Vector(
      Note(ProcedureNotes.id(ActionRef.Negotiation),
        if deal.declined then endedNote(walk.prefix)
        else _ => Some(walk.opened(pending))))
    // The settlement's lines, in the order `settle` applies them:
    // disclosures first, then transfers, then each given site's
    // warbands home. A refused settlement fails the
    // command, so nothing is journaled and no note is reached.
    //
    // They state the agreement rather than the journal: the deleted
    // rule read the executed operations, these are derived from the
    // same `DealState` the settling node is. No power contributes at
    // `NegotiationSettlement`; a first one that removed an operation
    // would leave its line standing, so it must not.
    val returns: Vector[Operation] =
      Option.when(NegotiationSites.moves(deal).nonEmpty)(
        // Required: a refused return fails the settlement rather than
        // leaving two kinds of warband at a site (ruling 20).
        BuildOps((state, _) => Right(NegotiationSites.returns(state,
          deal)), required = true)).toVector
    val settlement: Vector[Operation] =
      if !deal.agreed then Vector.empty
      else (BuildOps((state, _) => NegotiationDeal.settle(state, deal),
        window = Some(PowerWindow.NegotiationSettlement)) +: returns) ++
        (disclosed(deal) ++ transferred(deal) ++ offered(deal)).map(note =>
          Note(ProcedureNotes.id(ActionRef.Negotiation), _ => Some(note)))
    // After every transfer, each new ruler places warbands at each site
    // they received, in the order the sites moved (rulings 20, 29).
    val placements: Vector[Operation] =
      if !deal.agreed then Vector.empty
      else NegotiationSites.moves(deal).flatMap(placing(catalog, deal, _))
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
    // its roles changed by now. The engine enforces none of them. In
    // private, only the participants read them (ruling 23); every
    // other line of the settlement is public (rulings 23 and 30).
    val privately = walk.rules(deal.participants).privateDeal
    val promises: Vector[Operation] =
      if !deal.agreed then Vector.empty
      else promisedLines(deal).map(note =>
        val read = if privately then note.readBy(deal.participants)
          else note
        Note(ProcedureNotes.id(ActionRef.Negotiation), _ => Some(read)))
    opening ++ settlement ++ placements ++ citizenship ++ promises

  /** Who declined the deal under `prefix`. */
  private[negotiation] def endedNote(prefix: String)(
      states: NoteStates): Option[LogNote] =
    val id = NegotiationDeal.dealId(prefix)
    states.answered.reverse.collectFirst {
      case Answered(`id`, DeclineDeal, by) =>
        NegotiationProcedure.ended.sourceless(NoteArg.Player(by))
    }

  /** One line per disclosure, in `NegotiationDeal.knowledge`'s order. */
  private def disclosed(deal: DealState): Vector[LogNote] =
    deal.participants.flatMap(deal.terms(_).disclosures).map:
      case NegotiationDisclosure(recipient,
          NegotiationDisclosureRef.Adviser(owner, card)) =>
        NegotiationProcedure.showed.sourceless(NoteArg.Player(owner),
          NoteArg.Player(recipient), NoteArg.Card(card))
      case NegotiationDisclosure(recipient,
          NegotiationDisclosureRef.HeldRelic(owner, relic)) =>
        NegotiationProcedure.showed.sourceless(NoteArg.Player(owner),
          NoteArg.Player(recipient), NoteArg.Card(relic))
      case NegotiationDisclosure(recipient,
          NegotiationDisclosureRef.SiteRelic(site, relic)) =>
        NegotiationProcedure.wasShown.sourceless(NoteArg.Player(recipient),
          NoteArg.Card(relic), NoteArg.Site(site))

  /** One line per transfer, in `NegotiationDeal.transfers`' order: each
    * author's favor, then secrets, then relics, then advisers, then sites.
    * A facedown adviser reads as its back to every player but the two who
    * held it (`LogWords.seen`). */
  private[negotiation] def transferred(deal: DealState): Vector[LogNote] =
    deal.participants.flatMap(author =>
      deal.terms(author).transfers.flatMap(transfer =>
        Option.when(transfer.favor > 0)(
          NegotiationProcedure.gaveFavor.sourceless(NoteArg.Player(author),
            NoteArg.Number(transfer.favor),
            NoteArg.Player(transfer.recipient))).toVector ++
          Option.when(transfer.secrets > 0)(
            NegotiationProcedure.gaveSecrets.sourceless(
              NoteArg.Player(author), NoteArg.Number(transfer.secrets),
              NoteArg.Player(transfer.recipient))).toVector ++
          transfer.relics.map(relic =>
            NegotiationProcedure.gaveCard.sourceless(NoteArg.Player(author),
              NoteArg.Card(relic), NoteArg.Player(transfer.recipient))) ++
          transfer.advisers.map(card =>
            NegotiationProcedure.gaveCard.sourceless(NoteArg.Player(author),
              NoteArg.Card(card), NoteArg.Player(transfer.recipient))) ++
          transfer.sites.map(site =>
            NegotiationProcedure.gaveSite.sourceless(NoteArg.Player(author),
              NoteArg.Site(site), NoteArg.Player(transfer.recipient)))))

  /** `move`'s new ruler answers how many warbands to place, from 1 to
    * `NegotiationSites.most`; the answer places them, a required move, and
    * writes its line (ruling 20), which covers the generic warband line.
    * The question sits in a `Branch` so its bound reads the board when it
    * is asked. */
  private def placing(catalog: ExecutableCatalog, deal: DealState,
      move: SiteMove): Vector[Operation] =
    val id = NegotiationSites.warbandsDecisionId(deal.prefix, move.site)
    val name = catalog.site(move.site).fold(move.site.value)(_.name)
    Vector(
      Branch((ready, pending) => Vector(Decide(id, move.recipient,
        DecisionQuery.ChooseAmount(1, NegotiationSites.most(ready, deal,
          move, pending.answered),
          Some(s"Place how many warbands at $name?"), "Place")))),
      BuildOps((ready, pending) => Right(NegotiationSites.placed(
        pending.answered, deal.prefix, move.site).flatMap(count =>
        NegotiationSites.placement(ready, move, count)).toVector),
        required = true),
      Note(ProcedureNotes.id(ActionRef.Negotiation), states =>
        NegotiationSites.placed(states.answered, deal.prefix, move.site)
          .map(count => NegotiationProcedure.placedWarbands.sourceless(
            NoteArg.Player(move.recipient), NoteArg.Number(count),
            NoteArg.Site(move.site))), covers = true))

  /** The line for `citizen`'s new role, once the change has run. */
  private def becameNote(states: NoteStates, citizen: PlayerId)
      : Option[LogNote] = Imperials.role(states.now, citizen).collect {
    case Role.Chancellor =>
      NegotiationProcedure.becameChancellor.sourceless(NoteArg.Player(citizen))
    case Role.Citizen =>
      NegotiationProcedure.becameCitizen.sourceless(NoteArg.Player(citizen))
  }

  /** One line per author who offered Citizenship. */
  private def offered(deal: DealState): Vector[LogNote] =
    deal.participants.flatMap(author =>
      Option.when(deal.terms(author).citizenship.nonEmpty)(
        NegotiationProcedure.offeredCitizenship.sourceless(
          NoteArg.Player(author),
          NoteArg.Players(deal.terms(author).citizenship))).toVector)

  /** One line per promise, by author in participant order, the plain
    * promises before the binding ones. Each names the other participants. */
  private def promisedLines(deal: DealState): Vector[LogNote] =
    deal.participants.flatMap { author =>
      val others = NoteArg.Players(deal.participants.filter(_ != author))
      val terms = deal.terms(author)
      terms.promises.map(text => NegotiationProcedure.promised.sourceless(
        NoteArg.Player(author), others, NoteArg.Promise(text))) ++
        terms.bindingPromises.map(text =>
          NegotiationProcedure.promisedBinding.sourceless(
            NoteArg.Player(author), others, NoteArg.Promise(text)))
    }
