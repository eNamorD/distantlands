package distantlands.gameplay.chronicle.legacies

import distantlands.catalog.{ExecutableCatalog, Legacy, LegacyGoal,
  PrintedPower}
import distantlands.gameplay.actions.ProcedureNotes
import distantlands.gameplay.chronicle.{ChronicleLegacyRules, GatherDenizens}
import distantlands.gameplay.powerresolver.{Contribution, ContributingPower}
import distantlands.gameplay.powers.PowerAnswers
import distantlands.model._

object OathsOfLoyaltyCard extends Legacy(LegacyId("L25"), "Oaths of Loyalty",
    LegacyGoal.HighestRelic, Suit.Hearth):
  val power = PrintedPower(PowerId("legacy.oaths-of-loyalty"),
    persistent = false, cost = Cost.free,
    text = "When you would gain a starting adviser, bury it, except " +
      "for—**CHRONICLE (WORLD):** In Gather Denizens, keep your " +
      "faceup advisers. They will be your facedown starting " +
      "advisers in the next game.")
  val powers: Vector[PrintedPower] = Vector(power)

/** Oaths of Loyalty (legacy L25): "When you would gain a starting adviser,
  * bury it, except for— CHRONICLE (WORLD): In Gather Denizens, keep your
  * faceup advisers. They will be your facedown starting advisers in the
  * next game." (Legacy Chronicle powers design, rulings 15 and 16.)
  *
  * Gather Denizens first asks the holder, read when it is reached, for zero
  * to three of their faceup advisers; each one's tokens return, and it
  * becomes a facedown starting adviser of the lineage (R23). Every other
  * starting adviser the holder would gain is buried at the bottom of the
  * world deck: the keep questions of Protect and Dispossess, Welcome a
  * Stranger and the next Setup's hand stay, and the kept card is buried
  * (R24); under Teeming World the holder draws none (`SetupDeal.of`). The
  * power hooks no window; it is registered so the card counts as
  * implemented. Lines: "p2 kept Birdsong and Golem Legions with Oaths of
  * Loyalty", "p2 buried Fae Merchant with Oaths of Loyalty". */
case object OathsOfLoyalty extends ContributingPower:
  val id: PowerId = OathsOfLoyaltyCard.power.id
  def source: RuleSourceRef = RuleSourceRef.GameRule(id.value)
  def contributions: Map[PowerWindow, Vector[Contribution]] = Map.empty

  /** The most faceup advisers the holder keeps (ruling 15). */
  val MaxKept: Int = 3

  def keepDecisionId(player: PlayerId): String =
    s"chronicle.world.legacy.oaths-of-loyalty.${player.value}"

  private val worldId = ProcedureNotes.id(TriggeredProcedureRef.ChronicleWorld)
  private val worldNotes = NoteSet(worldId)
  val keptAdvisers: NoteKey = worldNotes.key("oaths-kept", Vector(
    NotePart.Arg(0), NotePart.Text(" kept "), NotePart.Arg(1),
    NotePart.Text(" with "), NotePart.Arg(2)))

  /** The procedures whose keep a holder's burial replaces, each with its
    * own "buried" line. */
  private val burying: Vector[TriggeredProcedureRef] = Vector(
    TriggeredProcedureRef.ChronicleWorld, TriggeredProcedureRef.ChronicleBeacon,
    TriggeredProcedureRef.Setup)

  private val buriedKeys: Map[PowerId, (NoteSet, NoteKey)] =
    burying.map(procedure =>
      val notes =
        if procedure == TriggeredProcedureRef.ChronicleWorld then worldNotes
        else NoteSet(ProcedureNotes.id(procedure))
      ProcedureNotes.id(procedure) -> (notes, notes.key("oaths-buried",
        Vector(NotePart.Arg(0), NotePart.Text(" buried "), NotePart.Arg(1),
          NotePart.Text(" with "), NotePart.Arg(2))))).toMap

  /** "p2 buried Fae Merchant with Oaths of Loyalty", under `noteId`, the
    * procedure whose keep the burial replaces. */
  private def buried(noteId: PowerId): Option[NoteKey] =
    buriedKeys.get(noteId).map(_._2)

  /** The line itself, under `noteId`. */
  def buriedLine(noteId: PowerId, player: PlayerId, card: CardId)
      : Option[LogNote] = buried(noteId).map(_.sourceless(
    NoteArg.Player(player), NoteArg.Card(card),
    NoteArg.Card(OathsOfLoyaltyCard.id)))

  /** The lines this power declares under `procedure`'s note id. */
  def noteKeys(procedure: TriggeredProcedureRef): Vector[NoteKey] =
    buriedKeys.get(ProcedureNotes.id(procedure)).toVector.flatMap((notes, _) =>
      notes.declared)

  /** Gather Denizens' keep, before the gathering: the holder chooses up to
    * three of their faceup advisers. */
  def keep(catalog: ExecutableCatalog, state: ReadyGame): Vector[Operation] =
    ChronicleLegacyRules.keepsAdvisers(state).toVector.flatMap(holder =>
      val faceup = state.game.current.player(holder).toVector
        .flatMap(_.advisers).collect {
          case card @ DenizenState(_, Orientation.FaceUp, _) => card }
      if faceup.isEmpty then Vector.empty
      else Vector(
        Decide(keepDecisionId(holder), holder, DecisionQuery.ChooseMany(0,
          math.min(MaxKept, faceup.size), faceup.map(card =>
            DecisionOption.Denizen(DecisionOptionRef.Denizen(card.id))),
          heading = Some("Oaths of Loyalty: keep up to three faceup " +
            "advisers as starting advisers"),
          confirm = Some(CountedConfirm("Keep", "adviser", "advisers",
            Some("Keep none"))))),
        Branch((live, pending) =>
          val chosen = PowerAnswers.many(pending, keepDecisionId(holder))
            .getOrElse(Vector.empty).toSet
          val kept = faceup.filter(card =>
            chosen(DecisionOptionRef.Denizen(card.id)))
          live.game.current.player(holder).map(_.lineage).toVector.flatMap(
            lineage => if kept.isEmpty then Vector.empty
            else Vector(
              BuildOps((_, _) => Right(kept.flatMap(card =>
                GatherDenizens.returnTokens(catalog, holder, card) :+
                  Move(Piece.Card(card.id),
                    PositionedLocation(Location.PlayArea(holder)),
                    PositionedLocation(Location.StartingAdvisers(lineage)),
                    resultingOrientation = Some(Orientation.FaceDown)))),
                required = true),
              Note(worldId, _ => Some(keptAdvisers.sourceless(
                NoteArg.Player(holder), NoteArg.Cards(kept.map(_.id)),
                NoteArg.Card(OathsOfLoyaltyCard.id)))))))))
