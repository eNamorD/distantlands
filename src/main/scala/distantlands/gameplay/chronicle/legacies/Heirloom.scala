package distantlands.gameplay.chronicle.legacies

import distantlands.catalog.{Legacy, LegacyGoal, PrintedPower}
import distantlands.gameplay.actions.ProcedureNotes
import distantlands.gameplay.chronicle.ChronicleLegacyRules
import distantlands.gameplay.operations.TheGrandScepterCard
import distantlands.gameplay.powerresolver.{Contribution, ContributingPower,
  Transform}
import distantlands.gameplay.powers.PowerAnswers
import distantlands.model._

object HeirloomCard extends Legacy(LegacyId("L20"), "Heirloom",
    LegacyGoal.DarkestSecret, Suit.Beast):
  val power = PrintedPower(PowerId("legacy.heirloom"),
    persistent = false, cost = Cost.free,
    text = "**CHRONICLE (THRONE):** Before Scatter Relics, secretly " +
      "choose one of your relics to keep facedown. You will start " +
      "the next game with it.")
  val powers: Vector[PrintedPower] = Vector(power)

/** Heirloom (legacy L20): "Before Scatter Relics, secretly choose one of
  * your relics to keep facedown. You will start the next game with it."
  * Its step is the window before Scatter Relics (Legacy Chronicle powers
  * design, ruling 13), after Stock Reliquary, so a Throne holder's relics
  * are already in the Reliquary (R17). The holder, read when the step is
  * reached, chooses one of their relics but the Grand Scepter, with no
  * Decline; a single one is kept without a question (R18). Its secrets
  * return to the holder facedown, then `StartingRelicKept` keeps it (R20).
  * Lines (R19): "p2 kept a relic with Heirloom" for every viewer, and "p2
  * kept Cup of Plenty with Heirloom" for the holder alone. */
case object Heirloom extends ContributingPower:
  val id: PowerId = HeirloomCard.power.id
  def source: RuleSourceRef = RuleSourceRef.GameRule(id.value)
  def contributions: Map[PowerWindow, Vector[Contribution]] = Map(
    PowerWindow.ChronicleBeforeScatterRelics -> Vector(Transform(
      (_, ops) => ops :+ step)))

  def decisionId(player: PlayerId): String =
    s"chronicle.throne.legacy.heirloom.${player.value}"

  private val noteId =
    ProcedureNotes.id(TriggeredProcedureRef.ChronicleThrone)
  private val notes = NoteSet(noteId)
  val keptPublic: NoteKey = notes.key("heirloom-kept", Vector(
    NotePart.Arg(0), NotePart.Text(" kept a relic with "), NotePart.Arg(1)))
  val keptPrivate: NoteKey = notes.key("heirloom-kept-named", Vector(
    NotePart.Arg(0), NotePart.Text(" kept "), NotePart.Arg(1),
    NotePart.Text(" with "), NotePart.Arg(2)))
  /** The Throne lines this power writes, on the Throne's note id. */
  def throneNoteKeys: Vector[NoteKey] = notes.declared

  val step: Operation = Branch((state, _) =>
    ChronicleLegacyRules.holder(state, HeirloomCard.id).toVector.flatMap(
      holder => keepable(state, holder) match
        case Vector() => Vector.empty
        case Vector(relic) => kept(holder, relic.id)
        case relics => Vector(
          Decide(decisionId(holder), holder, DecisionQuery.ChooseOne(
            relics.map(relic =>
              DecisionOption.Relic(DecisionOptionRef.Relic(relic.id))),
            heading = Some("Heirloom: keep a relic for the next game"),
            confirmVerb = Some("Keep"))),
          Branch((_, pending) => PowerAnswers.one(pending, decisionId(holder))
            .collect { case DecisionOptionRef.Relic(relic) => relic }
            .toVector.flatMap(kept(holder, _))))))

  private def keepable(state: ReadyGame, holder: PlayerId)
      : Vector[RelicState] =
    state.game.current.player(holder).toVector.flatMap(_.relics)
      .filterNot(_.id == TheGrandScepterCard.id)

  /** The relic's secrets back to `holder`, facedown, then the keep and its
    * two lines. */
  private def kept(holder: PlayerId, relic: RelicId): Vector[Operation] =
    Vector(BuildOps((live, _) => Right(returned(live, holder, relic) :+
        StartingRelicKept(holder, relic)), required = true),
      Note(noteId, _ => Some(keptPublic.sourceless(NoteArg.Player(holder),
        NoteArg.Card(HeirloomCard.id)))),
      Note(noteId, _ => Some(keptPrivate.sourceless(NoteArg.Player(holder),
        NoteArg.Card(relic), NoteArg.Card(HeirloomCard.id))
        .readBy(Vector(holder)))))

  private def returned(state: ReadyGame, holder: PlayerId, relic: RelicId)
      : Vector[CoreOperation] =
    val secrets = state.game.current.player(holder).toVector
      .flatMap(_.relics.find(_.id == relic)).map(_.tokens.secrets).sum
    if secrets == 0 then Vector.empty
    else Vector(Move(Piece.Secrets(secrets),
        PositionedLocation(Location.OnCard(relic)),
        PositionedLocation(Location.PlayArea(holder))),
      FlipSecrets(holder, secrets, SecretSide.FaceUp, SecretSide.FaceDown))
