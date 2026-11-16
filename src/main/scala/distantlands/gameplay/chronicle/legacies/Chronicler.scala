package distantlands.gameplay.chronicle.legacies

import distantlands.catalog.{ExecutableCatalog, Legacy, LegacyGoal,
  PrintedPower}
import distantlands.gameplay.actions.ProcedureNotes
import distantlands.gameplay.chronicle.{ChronicleLegacyRules, ChronicleTasks,
  LegacyGoals, LineageScoring, RuinedTempleWin}
import distantlands.gameplay.powerresolver.{Contribution, ContributingPower,
  Transform}
import distantlands.gameplay.powers.PowerAnswers
import distantlands.model._

object ChroniclerCard extends Legacy(LegacyId("L02"), "Chronicler",
    LegacyGoal.MostSites, Suit.Arcane):
  val power = PrintedPower(PowerId("legacy.chronicler"),
    persistent = false, cost = Cost.free,
    text = "**CHRONICLE (SUN):** After Resolve Legacies, you may " +
      "declare that all players score their legacies now. _Each " +
      "legacy may only be scored once per game._")
  val powers: Vector[PrintedPower] = Vector(power)

/** Chronicler (legacy L02): "After Resolve Legacies, you may declare that
  * all players score their legacies now. Each legacy may only be scored
  * once per game." Its step is the window after Sun step 5 (Legacy
  * Chronicle powers design, ruling 7). The holder, read when the step is
  * reached, declares or declines (ruling 3; R6). On a declaration every
  * seated player, in turn order (Legacies R21), scores
  * `LineageScoring.bonus`, with no base and no gifts, and one
  * `LegaciesScored` records every unscored active legacy they hold, those
  * scoring 0 included; a dormant legacy neither scores nor is recorded
  * (ruling 8; R1, R2, re-ruled by the owner 2026-11-14). Lines (R4): "p2
  * declared Chronicler", then "p1 scored 2 lineage points (2 from Beast)"
  * for each player who scores a point. A winner who pledged at Ruined
  * Temple scores double (`LineageScoring.scoredBonus`; catalog batch 13,
  * ruling 19). */
case object Chronicler extends ContributingPower:
  val id: PowerId = ChroniclerCard.power.id
  def source: RuleSourceRef = RuleSourceRef.GameRule(id.value)
  def contributions: Map[PowerWindow, Vector[Contribution]] = Map(
    PowerWindow.ChronicleAfterResolveLegacies -> Vector(Transform(
      (ctx, ops) => ops :+ step(ctx.catalog))))

  def decisionId(player: PlayerId): String =
    s"chronicle.sun.legacy.chronicler.${player.value}"
  val declare: DecisionOptionRef.Button = DecisionOptionRef.Button("declare")
  val decline: DecisionOptionRef.Button = DecisionOptionRef.Button("decline")

  private val noteId = ProcedureNotes.id(TriggeredProcedureRef.ChronicleSun)
  private val notes = NoteSet(noteId)
  val declared: NoteKey = notes.key("chronicler-declared", Vector(
    NotePart.Arg(0), NotePart.Text(" declared "), NotePart.Arg(1)))
  val scored: NoteKey = notes.key("chronicler-scored", Vector(
    NotePart.Arg(0), NotePart.Text(" scored "), NotePart.Arg(1),
    NotePart.Text(" ("), NotePart.Arg(2), NotePart.Text(")")))
  /** "p1 scored 4 lineage points (4 from Beast, doubled by Ruined
    * Temple)", a pledged winner's share (catalog batch 13, ruling 19). */
  val scoredDoubled: NoteKey = notes.key("chronicler-scored-doubled", Vector(
    NotePart.Arg(0), NotePart.Text(" scored "), NotePart.Arg(1),
    NotePart.Text(" ("), NotePart.Arg(2),
    NotePart.Text(s", doubled by ${RuinedTempleWin.name})")))
  /** The Sun lines this power writes, on the Sun's note id. */
  def sunNoteKeys: Vector[NoteKey] = notes.declared

  def step(catalog: ExecutableCatalog): Operation = Branch((state, _) =>
    ChronicleLegacyRules.holder(state, ChroniclerCard.id).toVector.flatMap(
      holder => Vector(
        Decide(decisionId(holder), holder, DecisionQuery.ChooseOne(Vector(
          DecisionOption.Button(decline, "Decline"),
          DecisionOption.Button(declare, "Declare")),
          heading = Some("Chronicler: you may declare that all players " +
            "score their legacies now"))),
        Branch((_, pending) =>
          if PowerAnswers.one(pending, decisionId(holder)).contains(declare)
          then Vector(Note(noteId, _ => Some(declared.sourceless(
              NoteArg.Player(holder), NoteArg.Card(ChroniclerCard.id)))),
            scoring(catalog))
          else Vector.empty))))

  /** Every seated player's bonus, read when reached, after the
    * declaration. */
  private def scoring(catalog: ExecutableCatalog): Operation =
    Branch((state, _) =>
      val players = LegacyGoals.turnOrder(state)
      val legacies = players.flatMap(LineageScoring.unscored(state, _))
      val shares = players.map(player =>
        player -> LineageScoring.scoredBonus(state, catalog, player))
        .filter(_._2.nonEmpty)
      val ops = Option.when(legacies.nonEmpty)(LegaciesScored(legacies))
        .toVector ++ shares.flatMap((player, share) =>
          ChronicleTasks.lineageOf(state, player).map(
            LineagePointsScored(_, share.map(_._2).sum)))
      Option.when(ops.nonEmpty)(BuildOps((_, _) => Right(ops),
        required = true)).toVector ++
        shares.map((player, share) => Note(noteId, _ => Some(
          (if RuinedTempleWin.doubles(state, player) then scoredDoubled
            else scored).sourceless(NoteArg.Player(player),
            NoteArg.Amount(share.map(_._2).sum, NoteUnit.LineagePoint),
            NoteArg.SuitShares(share.map((suit, points) =>
              points -> suit)))))))
