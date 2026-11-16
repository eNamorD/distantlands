package distantlands.gameplay.chronicle

import distantlands.catalog.{ExecutableCatalog, FoundationCards}
import distantlands.gameplay.actions.ProcedureNotes
import distantlands.model._

/** Shape Foundation, the Beacon's third step (CR p. 39): "You may flip one
  * foundation setup card from its altered side to its normal side unless
  * it's being altered by an active legacy." While a foundation lies
  * altered, the Beacon task holder chooses one altered face to restore, or
  * keeps them all, which is the first option. A face whose alteration
  * sources name a legacy that a lineage of this Chronicle holds active is
  * shown unavailable, naming that legacy, even when every altered face is
  * held and only Keep may be chosen. A face Setup With No Empire eroded,
  * or one whose sources are no longer active, may be restored, III
  * included (Foundations ruling 1). Sun step 2 adds the sources
  * (`ResolveLegacies`).
  *
  * Lines, as every Beacon decision line names the holder: "p3 restored
  * Foundation I to Imperial Maps", or "p3 did not flip any foundations"
  * for Keep. Nothing is written when no face is altered.
  */
object ShapeFoundation:
  val decisionId: String = "chronicle.beacon.shape-foundation"
  val heading: String = "Shape Foundation: restore one altered foundation"
  val keep: DecisionOptionRef.Button = DecisionOptionRef.Button("keep")
  val keepLabel: String = "Keep every foundation as it lies"

  def restore(number: FoundationNumber): DecisionOptionRef.Button =
    DecisionOptionRef.Button(s"restore-${slug(number)}")

  private def slug(number: FoundationNumber): String =
    number.toString.toLowerCase

  private def normalName(number: FoundationNumber): String =
    FoundationCards.face(number, FoundationFace.Normal).name

  private val noteId = ProcedureNotes.id(TriggeredProcedureRef.ChronicleBeacon)
  private val notes = NoteSet(noteId)
  /** "p3 restored Foundation I to Imperial Maps", one key per foundation,
    * since a restore always turns the card to its one normal face. */
  val restored: Map[FoundationNumber, NoteKey] =
    FoundationNumber.all.map(number => number -> notes.key(
      s"restored-foundation-${slug(number)}", Vector(NotePart.Arg(0),
        NotePart.Text(s" restored Foundation $number to " +
          normalName(number))))).toMap
  /** "p3 did not flip any foundations": the holder kept every face. */
  val keptAll: NoteKey = notes.key("kept-foundations", Vector(NotePart.Arg(0),
    NotePart.Text(" did not flip any foundations")))
  def noteKeys: Vector[NoteKey] = notes.declared

  def steps(catalog: ExecutableCatalog): Vector[Operation] = Vector(
    Branch((state, _) =>
      ChronicleTasks.forHolder(state, ChronicleTask.Beacon)(holder =>
        altered(state) match
          case Vector() => Vector.empty
          case faces => Vector(
            Decide(decisionId, holder, DecisionQuery.ChooseOne(
              DecisionOption.Button(keep, keepLabel) +:
                faces.map(option(catalog, state, _)),
              heading = Some(heading))),
            BuildOps((live, pending) =>
              restoring(live, chosen(pending.answered)), required = true),
            Note(noteId, states => Some(chosen(states.answered)
              .fold(keptAll)(restored).sourceless(NoteArg.Player(holder))))))))

  /** The altered foundations, I to VI. */
  private def altered(state: ReadyGame): Vector[FoundationNumber] =
    FoundationNumber.all.filter(number => state.game.campaign.foundations
      .get(number).exists(_.face == FoundationFace.Altered))

  /** The active legacies among `number`'s alteration sources: those a
    * lineage this Chronicle records, seated or in the box, holds active. */
  def activeSourcesOf(state: ReadyGame, number: FoundationNumber)
      : Vector[LegacyId] =
    val active = ChronicleRecord.campaignFields(state).lineages
      .flatMap(_.legacies).filter(_.active).map(_.id).toSet
    state.game.campaign.foundations.get(number).toVector
      .flatMap(_.alterationSources.toVector.sortBy(_.value))
      .filter(active)

  private def option(catalog: ExecutableCatalog, state: ReadyGame,
      number: FoundationNumber): DecisionOption =
    val shown = DecisionOption.Button(restore(number),
      s"Restore Foundation $number to ${normalName(number)}")
    activeSourcesOf(state, number) match
      case Vector() => shown
      case legacies => DecisionOption.Unavailable(shown, OptionReason.Written(
        "An active legacy alters it: " + legacies.map(legacy =>
          catalog.legacy(legacy).fold(legacy.value)(_.name)).mkString(", ")))

  /** The foundation the answer restores; none for Keep. */
  private def chosen(answered: Vector[Answered]): Option[FoundationNumber] =
    answered.collectFirst {
      case Answered(`decisionId`, DecisionAnswer.ChooseOneAnswer(ref), _) =>
        ref
    }.flatMap(ref => FoundationNumber.all.find(restore(_) == ref))

  /** The restore, while the face is still altered and no active legacy
    * holds it. */
  private def restoring(state: ReadyGame, number: Option[FoundationNumber])
      : Either[OathViolation, Vector[CoreOperation]] = number match
    case None => Right(Vector.empty)
    case Some(foundation) => Either.cond(
      altered(state).contains(foundation) &&
        activeSourcesOf(state, foundation).isEmpty,
      Vector(FoundationRestored(foundation)),
      OathViolation.InvalidEventOrder(
        s"Foundation $foundation cannot be restored"))
