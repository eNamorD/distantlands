package distantlands.gameplay.chronicle.legacies

import distantlands.catalog.{ExecutableCatalog, Legacy, LegacyGoal,
  PrintedPower}
import distantlands.gameplay.actions.ProcedureNotes
import distantlands.gameplay.chronicle.{ChronicleLegacyRules, ChronicleTasks,
  LegacyGoals}
import distantlands.gameplay.powerresolver.{Contribution, ContributingPower,
  Transform}
import distantlands.gameplay.powers.PowerAnswers
import distantlands.model._

object GatekeeperCard extends Legacy(LegacyId("L32"), "Gatekeeper",
    LegacyGoal.MostOfSuit, Suit.Discord, Some(FoundationNumber.III)):
  val power = PrintedPower(PowerId("legacy.gatekeeper"),
    persistent = false, cost = Cost.free,
    text = "Set Foundation III to Public Ambitions. **CHRONICLE " +
      "(SUN):** Before players resolve legacies, you may take one " +
      "active legacy from an enemy if you meet its goal.")
  val powers: Vector[PrintedPower] = Vector(power)

/** Gatekeeper (legacy L32): "Before players resolve legacies, you may take
  * one active legacy from an enemy if you meet its goal." Its step is the
  * window before Flip Legacies (Legacy Chronicle powers design, ruling 6).
  * The holder, read when the step is reached, may take one active legacy
  * of a seated enemy whose goal they meet as `LegacyGoals` counts it then,
  * or decline (ruling 3): one choose-one, Decline first (R6). The legacy
  * joins them active; step 2 alters its foundation if it alters one, and
  * step 4 applies the cap. The step's shape reads only the state at its
  * own question, so a walk resumed there finds the same options. Line:
  * "p2 took Seer from p3 with Gatekeeper" (R4). */
case object Gatekeeper extends ContributingPower:
  val id: PowerId = GatekeeperCard.power.id
  def source: RuleSourceRef = RuleSourceRef.GameRule(id.value)
  def contributions: Map[PowerWindow, Vector[Contribution]] = Map(
    PowerWindow.ChronicleBeforeResolveLegacies -> Vector(Transform(
      (ctx, ops) => ops :+ step(ctx.catalog))))

  def decisionId(player: PlayerId): String =
    s"chronicle.sun.legacy.gatekeeper.${player.value}"
  val decline: DecisionOptionRef.Button = DecisionOptionRef.Button("decline")

  private val noteId = ProcedureNotes.id(TriggeredProcedureRef.ChronicleSun)
  private val notes = NoteSet(noteId)
  val took: NoteKey = notes.key("gatekeeper-took", Vector(NotePart.Arg(0),
    NotePart.Text(" took "), NotePart.Arg(1), NotePart.Text(" from "),
    NotePart.Arg(2), NotePart.Text(" with "), NotePart.Arg(3)))
  /** The Sun lines this power writes, on the Sun's note id. */
  def sunNoteKeys: Vector[NoteKey] = notes.declared

  def step(catalog: ExecutableCatalog): Operation = Branch((state, _) =>
    ChronicleLegacyRules.holder(state, GatekeeperCard.id).toVector.flatMap(
      holder => takeable(catalog, state, holder) match
        case Vector() => Vector.empty
        case options => Vector(
          Decide(decisionId(holder), holder, DecisionQuery.ChooseOne(
            DecisionOption.Button(decline, "Decline") +:
              options.map((_, legacy) =>
                DecisionOption.Legacy(DecisionOptionRef.Legacy(legacy))),
            heading = Some("Gatekeeper: you may take an active legacy " +
              "from an enemy whose goal you meet"),
            confirmVerb = Some("Take"))),
          Branch((_, pending) => taken(holder, options, pending)))))

  /** Each seated enemy's active legacies whose goal `holder` meets, with
    * their owner, in turn order (Legacies R21). */
  private def takeable(catalog: ExecutableCatalog, state: ReadyGame,
      holder: PlayerId): Vector[(PlayerId, LegacyId)] = for
    enemy <- LegacyGoals.turnOrder(state)
      if Imperials.enemies(state, holder, enemy)
    lineage <- ChronicleTasks.lineageOf(state, enemy).toVector
    legacy <- state.game.campaign.lineages.get(lineage).toVector
      .flatMap(_.legacies) if legacy.active
      if catalog.legacy(legacy.id).exists(card =>
        LegacyGoals.meeting(state, catalog, card)(holder))
  yield enemy -> legacy.id

  private def taken(holder: PlayerId, options: Vector[(PlayerId, LegacyId)],
      pending: PendingTree): Vector[Operation] =
    val chosen = PowerAnswers.one(pending, decisionId(holder)).toVector
      .collect { case DecisionOptionRef.Legacy(id) => id }
    options.filter((_, legacy) => chosen.contains(legacy)).flatMap(
      (enemy, legacy) => Vector(
        BuildOps((live, _) => Right(Vector(Move(Piece.Card(legacy),
          PositionedLocation(Location.Legacies(lineage(live, enemy))),
          PositionedLocation(Location.Legacies(lineage(live, holder))),
          resultingOrientation = Some(Orientation.FaceUp)))), required = true),
        Note(noteId, _ => Some(took.sourceless(NoteArg.Player(holder),
          NoteArg.Card(legacy), NoteArg.Player(enemy),
          NoteArg.Card(GatekeeperCard.id))))))

  private def lineage(ready: ReadyGame, player: PlayerId): LineageId =
    ready.game.current.player(player).get.lineage
