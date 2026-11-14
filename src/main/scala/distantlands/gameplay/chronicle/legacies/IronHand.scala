package distantlands.gameplay.chronicle.legacies

import distantlands.catalog.{Legacy, LegacyGoal, PrintedPower}
import distantlands.gameplay.actions.ProcedureNotes
import distantlands.gameplay.chronicle.{ChronicleLegacyRules, ChronicleTasks}
import distantlands.gameplay.powerresolver.{Contribution, ContributingPower}
import distantlands.model._

object IronHandCard extends Legacy(LegacyId("L01"), "Iron Hand",
    LegacyGoal.MostSites, Suit.Order):
  val power = PrintedPower(PowerId("legacy.iron-hand"),
    persistent = false, cost = Cost.free,
    text = "**If you are Imperial, discard Iron Hand.**\n\n**SETUP:** If " +
      "you were in the previous game, you place one warband on all " +
      "sites coming from the Atlas until the Empire divider, and " +
      "players may place their pawn at any site.\n\n**CHRONICLE:** " +
      "Score your legacies in addition to the winner, and perform " +
      "Shape Empire and Build Edifice (Throne). Your sites are " +
      "stored in place of the Empire (Stars).")
  val powers: Vector[PrintedPower] = Vector(power)

/** Iron Hand (legacy L01), its Chronicle half (Legacy Chronicle powers
  * design, rulings 18-21). An Imperial holding it active discards it at
  * four checkpoints: the start of the Sun, before the Sun's legacy cap,
  * after Crown Chancellor and after Offer Citizenship, each but the first
  * before that step's cap (R29, R30, R46). While a seated Exile holds it active
  * (`ChronicleLegacyRules.ironHand`), they perform Shape Empire and Build
  * Edifice in the Empire's place, score the legacy bonus after the winner,
  * and their sites are stored in place of the Empire's. Its SETUP half is
  * read at game start (`StartingLegacies`, Legacy Setup powers ruling 7)
  * and by `SetupPawnSites`; its discard during a game waits for its batch
  * (R7). This power hooks
  * no window; the steps read it. Line: "p2 discarded Iron Hand" (R34). */
case object IronHand extends ContributingPower:
  val id: PowerId = IronHandCard.power.id
  def source: RuleSourceRef = RuleSourceRef.GameRule(id.value)
  def contributions: Map[PowerWindow, Vector[Contribution]] = Map.empty

  private val sunNotes = NoteSet(ProcedureNotes.id(
    TriggeredProcedureRef.ChronicleSun))
  private val throneNotes = NoteSet(ProcedureNotes.id(
    TriggeredProcedureRef.ChronicleThrone))
  private val starsNotes = NoteSet(ProcedureNotes.id(
    TriggeredProcedureRef.ChronicleStars))
  /** "Buried Giant stays on the map with Iron Hand", for each of the
    * holder's stored sites (ruling 21; R34). */
  val stored: NoteKey = starsNotes.key("iron-hand-stored", Vector(
    NotePart.Arg(0), NotePart.Text(" stays on the map with "),
    NotePart.Arg(1)))
  /** "p3 scored 2 lineage points with Iron Hand (2 from Beast)" (R34). */
  val scored: NoteKey = throneNotes.key("iron-hand-scored", Vector(
    NotePart.Arg(0), NotePart.Text(" scored "), NotePart.Arg(1),
    NotePart.Text(" with "), NotePart.Arg(2), NotePart.Text(" ("),
    NotePart.Arg(3), NotePart.Text(")")))

  /** Score Lineage Points' second scorer (ruling 20): `holder`'s `legacies`
    * not yet scored are recorded (R33), and a positive `shares` total is
    * scored with its line. */
  def scoring(state: ReadyGame, holder: PlayerId, legacies: Vector[LegacyId],
      shares: Vector[(Suit, Int)]): Vector[Operation] =
    val total = shares.map(_._2).sum
    val ops = Option.when(legacies.nonEmpty)(LegaciesScored(legacies))
      .toVector ++ Option.when(total > 0)(ChronicleTasks.lineageOf(state,
        holder).map(LineagePointsScored(_, total))).flatten.toVector
    Option.when(ops.nonEmpty)(BuildOps((_, _) => Right(ops),
      required = true)).toVector ++
      Option.when(total > 0)(Note(ProcedureNotes.id(
        TriggeredProcedureRef.ChronicleThrone), _ => Some(scored.sourceless(
        NoteArg.Player(holder), NoteArg.Amount(total, NoteUnit.LineagePoint),
        NoteArg.Card(IronHandCard.id), NoteArg.SuitShares(
          shares.map((suit, points) => points -> suit)))))).toVector

  private def discardKey(notes: NoteSet): NoteKey =
    notes.key("iron-hand-discarded", Vector(NotePart.Arg(0),
      NotePart.Text(" discarded "), NotePart.Arg(1)))
  private val sunDiscarded = discardKey(sunNotes)
  private val throneDiscarded = discardKey(throneNotes)

  /** "p2 discarded Iron Hand", under the Sun's or the Throne's note id. */
  def discarded(procedure: TriggeredProcedureRef): NoteKey =
    if procedure == TriggeredProcedureRef.ChronicleSun then sunDiscarded
    else throneDiscarded

  /** The lines this power declares under `procedure`'s note id. */
  def noteKeys(procedure: TriggeredProcedureRef): Vector[NoteKey] =
    procedure match
      case TriggeredProcedureRef.ChronicleSun => sunNotes.declared
      case TriggeredProcedureRef.ChronicleThrone => throneNotes.declared
      case TriggeredProcedureRef.ChronicleStars => starsNotes.declared
      case _ => Vector.empty

  /** A checkpoint (ruling 18): an active Iron Hand a seated Imperial holds
    * returns to the legacy deck, read when reached (R29). */
  def discard(procedure: TriggeredProcedureRef): Operation =
    Branch((state, _) =>
      ChronicleLegacyRules.holder(state, IronHandCard.id,
        ChronicleLegacyRules.imperialRoles).toVector.flatMap(holder =>
        ChronicleTasks.lineageOf(state, holder).toVector.flatMap(lineage =>
          Vector(BuildOps((_, _) => Right(Vector(Move(
              Piece.Card(IronHandCard.id),
              PositionedLocation(Location.Legacies(lineage)),
              PositionedLocation(Location.Deck(CardDeck.Legacy))))),
              required = true),
            Note(ProcedureNotes.id(procedure), _ => Some(discarded(procedure)
              .sourceless(NoteArg.Player(holder),
                NoteArg.Card(IronHandCard.id))))))))
