package distantlands.gameplay.setup.legacies

import distantlands.catalog.{ExecutableCatalog, Legacy, LegacyGoal,
  PrintedPower}
import distantlands.gameplay.actions.{CardPlay, ProcedureNotes, VisionRules}
import distantlands.gameplay.chronicle.legacies.OathsOfLoyalty
import distantlands.gameplay.powerresolver.{Contribution, ContributingPower,
  Transform}
import distantlands.gameplay.powers.{AdviserLimit, PowerAnswers}
import distantlands.model._

object SecretSocietyCard extends Legacy(LegacyId("L28"), "Secret Society",
    LegacyGoal.HighestRelic, Suit.Arcane):
  val power = PrintedPower(PowerId("legacy.secret-society"),
    persistent = false, cost = Cost.free,
    text = "**SETUP:** Before building the world deck, you may secretly " +
      "choose a Vision except the Conspiracy to take as a facedown " +
      "adviser _(discard excess advisers)_. If you do, advance the " +
      "Visions Drawn marker once.")
  val powers: Vector[PrintedPower] = Vector(power)

/** Secret Society (legacy L28), its SETUP power (Legacy Setup powers
  * design, ruling 14). In the `SetupWorldDeck` window, before Seer, its
  * holder may take one Vision but the Conspiracy from the world deck, as a
  * facedown adviser, offered in catalog order and seen only through the
  * question's options (R20, R22). A take advances Visions Drawn once and rebuilds
  * the deck in the card's layout (`ShufflePile.VisionPackets`, R19). The
  * Vision is not a starting adviser, so Oaths of Loyalty does not bury it;
  * excess advisers are discarded after the holder's pawn (R21). Line: "p2
  * took a Vision as a facedown adviser with Secret Society". */
case object SecretSociety extends ContributingPower:
  val id: PowerId = SecretSocietyCard.power.id
  def source: RuleSourceRef = RuleSourceRef.GameRule(id.value)
  def contributions: Map[PowerWindow, Vector[Contribution]] = Map(
    PowerWindow.SetupWorldDeck -> Vector(Transform((_, ops) => ops :+ step)))

  def decisionId(player: PlayerId): String =
    s"setup.legacy.secret-society.${player.value}"
  val decline: DecisionOptionRef.Button = DecisionOptionRef.Button("decline")

  private val noteId = ProcedureNotes.id(TriggeredProcedureRef.Setup)
  private val notes = NoteSet(noteId)
  val tookVision: NoteKey = notes.key("secret-society-took", Vector(
    NotePart.Arg(0), NotePart.Text(" took a Vision as a facedown adviser with "),
    NotePart.Arg(1)))
  /** "p2 discarded Tinker with Secret Society": the line names each card by
    * the knowledge rule, so a viewer who does not know it reads "a Denizen"
    * (R21). */
  val discarded: NoteKey = notes.key("secret-society-discarded", Vector(
    NotePart.Arg(0), NotePart.Text(" discarded "), NotePart.Arg(1),
    NotePart.Text(" with "), NotePart.Arg(2)))
  def setupNoteKeys: Vector[NoteKey] = notes.declared

  /** The discard's line when the first Wake runs it (ruling 20). */
  private val wakeNotes = NoteSet(ProcedureNotes.id(
    TriggeredProcedureRef.QuestFirstWake))
  val wakeDiscarded: NoteKey = wakeNotes.key("secret-society-discarded",
    Vector(NotePart.Arg(0), NotePart.Text(" discarded "), NotePart.Arg(1),
      NotePart.Text(" with "), NotePart.Arg(2)))
  def wakeNoteKeys: Vector[NoteKey] = wakeNotes.declared

  def discardDecisionId(player: PlayerId): String =
    s"${decisionId(player)}.discard"

  /** Right after `player`'s pawn, while Secret Society's holder holds more
    * advisers than their limit: discard the excess, one choose-many of
    * exactly that many over their advisers by slot, each card to the top of
    * the discard pile of the region after the pawn's, where Setup's
    * rejected advisers go (R21). Read when reached: only this player's own
    * steps came between the take and here. `id` names the question, and
    * `procedure` the note id its line is written under: Setup's, or the
    * first Wake's for a player who placed no pawn. */
  def excessStep(catalog: ExecutableCatalog, player: PlayerId, id: String,
      procedure: TriggeredProcedureRef): Operation =
    Branch((state, _) =>
      val excess = AdviserLimit.towardLimit(state, player) -
        AdviserLimit.of(catalog, state, player)
      // A player with no pawn discards at their first Wake (ruling 18).
      if !SetupLegacyRules.secretSociety(state).contains(player) ||
          excess <= 0 || state.game.current.pawnSite(player).isEmpty then
        Vector.empty
      else
        val held = state.game.current.player(player).toVector
          .flatMap(_.advisers).map(_.id: CardId)
        val lineId = ProcedureNotes.id(procedure)
        val line = if procedure == TriggeredProcedureRef.Setup then discarded
          else wakeDiscarded
        Vector(
          Decide(id, player, DecisionQuery.ChooseMany(excess, excess,
            held.indices.toVector.map(slot => DecisionOption.AdviserSlot(
              DecisionOptionRef.AdviserSlot(player, slot))),
            heading = Some("Secret Society: discard your excess advisers"),
            confirm = Some(CountedConfirm("Discard", "adviser", "advisers")))),
          BuildOps((live, pending) => discards(live, player, held,
            chosen(pending.answered, id, held)), required = true),
          Note(lineId, states => Option(chosen(states.answered, id, held))
            .filter(_.nonEmpty).map(cards => line.sourceless(
              NoteArg.Player(player), NoteArg.Cards(cards),
              NoteArg.Card(SecretSocietyCard.id))))))

  /** The cards the discard answer names, by slot of `held`. */
  private def chosen(answered: Vector[Answered], id: String,
      held: Vector[CardId]): Vector[CardId] =
    answered.collectFirst {
      case Answered(`id`, DecisionAnswer.ChooseManyAnswer(refs), _) => refs
    }.toVector.flatten.collect {
      case DecisionOptionRef.AdviserSlot(_, slot) if held.isDefinedAt(slot) =>
        held(slot)
    }

  private def discards(ready: ReadyGame, player: PlayerId,
      held: Vector[CardId], cards: Vector[CardId])
      : Either[OathViolation, Vector[CoreOperation]] =
    for
      site <- ready.game.current.pawnSite(player)
        .toRight(OathViolation.PawnSiteMissing(player))
      region <- ready.game.current.map.regionOf(site).toRight(
        OathViolation.InvalidEventOrder(s"${site.value} is not in play"))
      _ <- Either.cond(cards.nonEmpty && cards.forall(card =>
          held.exists(_ == card)), (), OathViolation.InvalidEventOrder(
        "no discard answer is recorded for Secret Society"))
    yield cards.map(card => Move(Piece.Card(card),
      PositionedLocation(Location.PlayArea(player)),
      PositionedLocation(Location.RegionalDiscard(CardPlay.nextRegion(region)),
        StackPosition.Top)))

  /** The Visions the holder may take, in catalog order: deck order would
    * tell their places after a Decline (R22). */
  private def takeable(state: ReadyGame): Vector[VisionId] =
    state.game.current.commonCards.worldDeck.collect {
      case id: VisionId if id != VisionRules.Conspiracy => id }
      .sortBy(_.value)

  private def step: Operation = Branch((state, _) =>
    SetupLegacyRules.secretSociety(state).toVector.flatMap { holder =>
      takeable(state) match
        case Vector() => Vector.empty
        case visions => Vector(
          Decide(decisionId(holder), holder, DecisionQuery.ChooseOne(
            DecisionOption.Button(decline, "Decline") +: visions.map(id =>
              DecisionOption.Vision(DecisionOptionRef.Vision(id))),
            heading = Some(
              "Secret Society: you may take a Vision as a facedown adviser"),
            confirmVerb = Some("Take"))),
          Branch((_, pending) =>
            PowerAnswers.one(pending, decisionId(holder)).toVector.collect {
              case DecisionOptionRef.Vision(vision) => vision
            }.flatMap(vision => Vector(
              BuildOps((_, _) => Right(Vector(
                Move(Piece.Card(vision),
                  PositionedLocation(Location.Deck(CardDeck.World)),
                  PositionedLocation(Location.PlayArea(holder)),
                  resultingOrientation = Some(Orientation.FaceDown)),
                AdvanceVisionsDrawn)), required = true),
              Shuffle(ShufflePile.VisionPackets),
              OathsOfLoyalty.rememberBurials,
              Note(noteId, _ => Some(tookVision.sourceless(
                NoteArg.Player(holder), NoteArg.Card(SecretSocietyCard.id))))))))
    })
