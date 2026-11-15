package distantlands.gameplay.setup.legacies

import distantlands.gameplay.actions.ProcedureNotes
import distantlands.gameplay.chronicle.ChronicleLegacyRules
import distantlands.gameplay.chronicle.legacies.{IronHandCard, RivalCard}
import distantlands.gameplay.operations.TheGrandScepterCard
import distantlands.model._

/** The lines that open Setup for what game start's legacy powers did
  * (Legacy Setup powers ruling 9; R7). Each Note is always in the tree, so
  * Setup's shape never depends on them, and writes nothing when its power
  * did not act. They read the table as game start left it: nothing in
  * Setup moves a warband or the Grand Scepter before them, and on resume
  * they are already in the journal. */
object StartLines:
  private val notes = NoteSet(ProcedureNotes.id(TriggeredProcedureRef.Setup))
  /** "p2 started with 8 warbands with Circle of Swords". */
  val startedWithWarbands: NoteKey = notes.key("legacy-started-warbands",
    Vector(NotePart.Arg(0), NotePart.Text(" started with "), NotePart.Arg(1),
      NotePart.Text(" "), NotePart.Plural(1, "warband", "warbands"),
      NotePart.Text(" with "), NotePart.Arg(2)))
  /** "p1 started with the Grand Scepter with Scepter Bearer". */
  val startedWithScepter: NoteKey = notes.key("legacy-started-scepter",
    Vector(NotePart.Arg(0),
      NotePart.Text(" started with the Grand Scepter with "), NotePart.Arg(1)))
  /** "p3 placed a warband at Deep Woods with Iron Hand", once per site. */
  val placedWarband: NoteKey = notes.key("legacy-placed-warband", Vector(
    NotePart.Arg(0), NotePart.Text(" placed a warband at "), NotePart.Arg(1),
    NotePart.Text(" with "), NotePart.Arg(2)))
  def noteKeys: Vector[NoteKey] = notes.declared

  private def note(line: ReadyGame => Option[LogNote]): Operation =
    Note(ProcedureNotes.id(TriggeredProcedureRef.Setup),
      states => line(states.now))

  /** Two Notes for the boards and the Grand Scepter, then one per map slot
    * for Iron Hand and one per slot for Rival: a constant shape, each Note
    * writing nothing when its power did not act there. */
  def operations: Vector[Operation] =
    Vector(note(circle), note(scepter)) ++
      (0 until MapState.slots).map(slot =>
        note(site(IronHandCard.id, world = true, slot))) ++
      (0 until MapState.slots).map(slot =>
        note(site(RivalCard.id, world = false, slot)))

  private def circle(state: ReadyGame): Option[LogNote] =
    ChronicleLegacyRules.holder(state, CircleOfSwordsCard.id)
      .flatMap(state.game.current.player).map(held =>
        startedWithWarbands.sourceless(NoteArg.Player(held.player),
          NoteArg.Number(held.board.warbands),
          NoteArg.Card(CircleOfSwordsCard.id)))

  /** An Imperial holder other than the Chancellor, who holds the Grand
    * Scepter anyway (ruling 6). */
  private def scepter(state: ReadyGame): Option[LogNote] =
    ChronicleLegacyRules.holder(state, ScepterBearerCard.id,
      ChronicleLegacyRules.imperialRoles)
      .filterNot(Imperials.chancellor(state).contains)
      .filter(player => state.game.current.player(player).exists(
        _.relics.exists(_.id == TheGrandScepterCard.id)))
      .map(player => startedWithScepter.sourceless(NoteArg.Player(player),
        NoteArg.Card(ScepterBearerCard.id)))

  /** The line for the site in map slot `slot`, Hinterward, when the
    * holder's warband holds it: the Empire's stored sites, kept or former,
    * for Iron Hand, the others for Rival. */
  private def site(legacy: LegacyId, world: Boolean, slot: Int)(
      state: ReadyGame): Option[LogNote] =
    val stored = state.game.campaign.baseline.world.map(_.site).toSet
    // A holder whose warbands game start did not place holds no such site,
    // so Iron Hand's previous-game clause (R39) needs no filter here.
    for
      player <- ChronicleLegacyRules.holder(state, legacy, Set(Role.Exile))
      held <- state.game.current.player(player)
      site <- state.game.current.map.inPlay.lift(slot)
      if stored(site) == world && state.game.current.map.sites(site).forces
        .count(ForceKind.Exile(held.lineage)) > 0
    yield placedWarband.sourceless(NoteArg.Player(player),
      NoteArg.Site(site), NoteArg.Card(legacy))
