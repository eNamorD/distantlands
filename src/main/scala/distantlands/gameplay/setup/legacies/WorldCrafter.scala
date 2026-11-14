package distantlands.gameplay.setup.legacies

import distantlands.catalog.{ExecutableCatalog, Legacy, LegacyGoal,
  PrintedPower}
import distantlands.gameplay.actions.ProcedureNotes
import distantlands.gameplay.powerresolver.{Contribution, ContributingPower}
import distantlands.gameplay.setup.{AtlasPlacement, ImperialMaps}
import distantlands.model._

object WorldCrafterCard extends Legacy(LegacyId("L13"), "World Crafter",
    LegacyGoal.PeoplesFavor, Suit.Nomad):
  val power = PrintedPower(PowerId("legacy.world-crafter"),
    persistent = false, cost = Cost.free,
    text = "**SETUP:** For the Imperial Maps foundation, you fill site " +
      "slots, using any mix of Recent and Forgotten Sites _(after " +
      "resolving the Rival legacy)_. For the Wide Horizons " +
      "foundation, when anyone explores, you fill the slot with a " +
      "Recent or Forgotten Site.")
  val powers: Vector[PrintedPower] = Vector(power)

/** World Crafter (legacy L13), its SETUP half (Legacy Setup powers design,
  * rulings 10 and 11). At Imperial Maps its holder fills the slots in the
  * Chancellor's place, choosing Recent or Forgotten Sites for each slot in
  * Hinterward order (`mapStep`); under Wide Horizons its holder chooses the
  * end for every successful Explore (`ExploreProcedure`). This power hooks
  * no window; the steps ask `SetupLegacyRules`. Lines: "p2 placed Deep
  * Woods from Recent Sites with World Crafter" (R11) and "p3 chose
  * Forgotten Sites with World Crafter". */
case object WorldCrafter extends ContributingPower:
  val id: PowerId = WorldCrafterCard.power.id
  def source: RuleSourceRef = RuleSourceRef.GameRule(id.value)
  def contributions: Map[PowerWindow, Vector[Contribution]] = Map.empty

  /** The question for slot `slot`, counted from 1 in Hinterward order. */
  def slotDecisionId(player: PlayerId, slot: Int): String =
    s"setup.legacy.world-crafter.${player.value}.$slot"

  private val setupId = ProcedureNotes.id(TriggeredProcedureRef.Setup)
  private val setupNotes = NoteSet(setupId)
  val placedRecent: NoteKey = setupNotes.key("world-crafter-placed-recent",
    Vector(NotePart.Arg(0), NotePart.Text(" placed "), NotePart.Arg(1),
      NotePart.Text(" from Recent Sites with "), NotePart.Arg(2)))
  val placedForgotten: NoteKey = setupNotes.key(
    "world-crafter-placed-forgotten", Vector(NotePart.Arg(0),
      NotePart.Text(" placed "), NotePart.Arg(1),
      NotePart.Text(" from Forgotten Sites with "), NotePart.Arg(2)))
  /** The Setup lines this power writes, on Setup's note id. */
  def setupNoteKeys: Vector[NoteKey] = setupNotes.declared

  private def placed(end: AtlasEnd): NoteKey = end match
    case AtlasEnd.Recent => placedRecent
    case AtlasEnd.Forgotten => placedForgotten

  /** Imperial Maps filled by `holder`: one step per slot game start left
    * empty, each asked while two or more Atlas sites remain (R12). The
    * count and the questions are read from what game start fixed, so a
    * resumed walk finds every slot where it was. */
  def mapStep(catalog: ExecutableCatalog, ready: ReadyGame,
      holder: PlayerId): Operation =
    val atlas = ImperialMaps.atlasAtStart(ready)
    Sequence((1 to ImperialMaps.emptySlotsAtStart(ready)).toVector.map(
      slot => slotStep(catalog, holder, slot, asked = atlas - slot >= 1)))

  /** Slot `slot`: the question, the site from the chosen end with its
    * Foundation II favor, and its line. The question is a `Branch` so that
    * its Recent option names the Atlas's front site when it is asked. */
  private def slotStep(catalog: ExecutableCatalog, holder: PlayerId,
      slot: Int, asked: Boolean): Operation =
    val decisionId = slotDecisionId(holder, slot)
    def end(answered: Vector[Answered]): AtlasEnd = answered.collectFirst {
      case Answered(`decisionId`, DecisionAnswer.ChooseOneAnswer(ref), _) =>
        ref
    }.flatMap(AtlasPlacement.endOf).getOrElse(AtlasEnd.Recent)
    val question = Branch((state, _) =>
      if !asked then Vector.empty
      else Vector(Decide(decisionId, holder, DecisionQuery.ChooseOne(
        AtlasEnd.all.map(option => DecisionOption.Button(
          AtlasPlacement.endOption(option),
          ImperialMaps.optionLabel(catalog, state, option))),
        heading = Some(s"World Crafter: fill slot $slot from")))))
    val place = BuildOps((state, pending) =>
      AtlasPlacement.at(catalog, state, end(pending.answered))
        .map(planned => planned.operation +:
          AtlasPlacement.suitFavor(catalog, planned)),
      required = true)
    val line = Note(setupId, states =>
      states.now.game.current.map.hintermost.map(site =>
        placed(end(states.answered)).sourceless(NoteArg.Player(holder),
          NoteArg.Site(site), NoteArg.Card(WorldCrafterCard.id))))
    Sequence(question, place, line)
