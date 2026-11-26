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
    text = "**If you are an Imperial, discard Iron " +
      "Hand.**\n\n**SETUP:** If you were in the previous game, " +
      "you place one warband on all sites coming from the " +
      "Atlas until the Empire divider, and players may place " +
      "their pawn at any site.\n\n**CHRONICLE:** Score your " +
      "legacies in addition to the winner, and perform Shape " +
      "Empire and Build Edifice (Throne). Your sites are " +
      "stored in place of the Empire (Stars).")
  val powers: Vector[PrintedPower] = Vector(power)

/** Iron Hand (legacy L01), its Chronicle half (Legacy Chronicle powers
  * design, rulings 18-21). An Imperial holding it active discards it at
  * four checkpoints: the start of the Sun, before the Sun's legacy cap,
  * after Crown Chancellor and after Offer Citizenship, each but the first
  * before that step's cap (R29, R30, R46). While a seated Exile holds it active
  * (`ChronicleLegacyRules.ironHand`), they perform Shape Empire and Build
  * Edifice in the Empire's place, score the legacy bonus after the winner,
  * and their sites are stored in place of the Empire's. Its SETUP half,
  * "If you were in the previous game", acts only when the holder's
  * lineage records a previous player (Legacy Setup powers ruling 7, as
  * R39 amends it): read at game start (`StartingLegacies`) and in Setup
  * (`SetupLegacyRules.ironHand`, `SetupPawnSites`). During a game, a
  * holder who becomes Imperial discards it in the change's own batch,
  * right after `RoleChanged` (`RoleChanges.becomeImperial`; Legacy in-play
  * powers ruling 22), with "p2 discarded Iron Hand on becoming a Citizen",
  * or "... the Chancellor" (R12). This power hooks no window; the steps
  * read it. Line: "p2 discarded Iron Hand" (R34). */
case object IronHand extends ContributingPower:
  val id: PowerId = IronHandCard.power.id
  def source: RuleSourceRef = RuleSourceRef.GameRule(id.value)
  def contributions: Map[PowerWindow, Vector[Contribution]] = Map.empty

  private val notes = NoteSet(id)
  override def noteKeys: Vector[NoteKey] = notes.declared
  /** "p2 discarded Iron Hand on becoming a Citizen" (R12). */
  private val onCitizen: NoteKey = notes.key("discarded-on-citizen", Vector(
    NotePart.Arg(0), NotePart.Text(" discarded "), NotePart.Arg(1),
    NotePart.Text(" on becoming a Citizen")))
  /** "p2 discarded Iron Hand on becoming the Chancellor", with no Empire
    * (R12). */
  private val onChancellor: NoteKey = notes.key("discarded-on-chancellor",
    Vector(NotePart.Arg(0), NotePart.Text(" discarded "), NotePart.Arg(1),
      NotePart.Text(" on becoming the Chancellor")))

  /** "p1 discarded Iron Hand": an Imperial's Iron Hand that The Mouth
    * activated goes straight back to the legacy deck (Legacy in-play
    * powers R25). It names the card in words: it never shows faceup where
    * a viewer's card reference could see it (R32). */
  val discardedNow: NoteKey = notes.key("discarded", Vector(NotePart.Arg(0),
    NotePart.Text(s" discarded ${IronHandCard.name}")))

  /** The in-game discard's line for the `role` the holder reached. */
  def discardedOnBecoming(role: Role): NoteKey =
    if role == Role.Chancellor then onChancellor else onCitizen

  /** The discard when `lineage`'s player becomes Imperial during a game
    * (ruling 22): Iron Hand returns to the legacy deck when the lineage
    * holds it active, else nothing (ruling 1). */
  def inGameDiscard(ready: ReadyGame, lineage: LineageId)
      : Vector[CoreOperation] =
    Option.when(ready.game.campaign.lineages.get(lineage).exists(
      _.legacies.contains(LegacyState(IronHandCard.id, active = true))))(
      Move(Piece.Card(IronHandCard.id),
        PositionedLocation(Location.Legacies(lineage)),
        PositionedLocation.legacyDeckBottom)).toVector

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
              PositionedLocation.legacyDeckBottom))),
              required = true),
            Note(ProcedureNotes.id(procedure), _ => Some(discarded(procedure)
              .sourceless(NoteArg.Player(holder),
                NoteArg.Card(IronHandCard.id))))))))
