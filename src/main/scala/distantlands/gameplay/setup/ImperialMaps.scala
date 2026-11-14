package distantlands.gameplay.setup

import distantlands.catalog.ExecutableCatalog
import distantlands.gameplay.actions.ProcedureNotes
import distantlands.model._
import distantlands.model.DecisionAnswer.ChooseOneAnswer

/** Foundation I on its normal face, Imperial Maps, as a step of Setup's
  * walk (Foundations design, slice 4; setup card `OATH-SETUP04a-NF`): "The
  * Chancellor fills the remaining empty site slots going Hinterward. They
  * must fill these slots with Recent Sites or Forgotten Sites from the
  * Atlas, but not both."
  *
  * {{{
  * Sequence(
  *   Decide(setup.imperial-maps, chancellor),  // with 2 or more Atlas sites
  *   Note(filled the map from ... Sites),
  *   Repeat(guard = a slot is empty and the Atlas holds a site,
  *     Sequence(
  *       BuildOps(PlaceAtlasSite, 1 favor per denizen to its suit's bank),
  *       Note(placed the site))))
  * }}}
  *
  * The Recent option names the Atlas's first site, which the rulebook
  * makes public; nothing else about the Atlas is shown before the fill.
  *
  * The step's shape reads only what game start fixed, the Chronicle's
  * Empire and Atlas and the sites Rival placed from it, so a walk resumed
  * past it finds every node where it was once the map is full and the
  * Atlas shorter.
  */
object ImperialMaps:
  val decisionId: String = "setup.imperial-maps"
  val heading: String = "Imperial Maps: fill the map from"

  private val notes = NoteSet(ProcedureNotes.id(TriggeredProcedureRef.Setup))
  /** "p1 filled the map from Recent Sites". */
  val filledFromRecent: NoteKey = notes.key("filled-from-recent", Vector(
    NotePart.Arg(0), NotePart.Text(" filled the map from Recent Sites")))
  /** "p1 filled the map from Forgotten Sites". */
  val filledFromForgotten: NoteKey = notes.key("filled-from-forgotten",
    Vector(NotePart.Arg(0),
      NotePart.Text(" filled the map from Forgotten Sites")))
  /** "p1 placed Deep Woods on the map", once per site the fill places. */
  val placedSite: NoteKey = notes.key("placed-site", Vector(
    NotePart.Arg(0), NotePart.Text(" placed "), NotePart.Arg(1),
    NotePart.Text(" on the map")))
  def noteKeys: Vector[NoteKey] = notes.declared

  /** The line that names the end the map was filled from. */
  def filledFrom(end: AtlasEnd): NoteKey = end match
    case AtlasEnd.Recent => filledFromRecent
    case AtlasEnd.Forgotten => filledFromForgotten

  /** The slots past the Empire's and Rival's sites, which the Chancellor
    * fills. Read from what game start fixed, the Chronicle and the seats,
    * never from the live map (R6). */
  def emptySlotsAtStart(ready: ReadyGame): Int =
    math.max(0, MapState.slots - ready.game.campaign.baseline.world.size -
      rivalCount(ready))

  /** The Atlas's sites once Rival took its own, read as the slots are. */
  private def atlasAtStart(ready: ReadyGame): Int =
    ready.game.campaign.baseline.atlasBox.size - rivalCount(ready)

  private def rivalCount(ready: ReadyGame): Int =
    val baseline = ready.game.campaign.baseline
    StartingLegacies.rivalSites(baseline,
      ready.game.current.players.map(_.lineage), baseline.world.size)._2.size

  /** Whether Imperial Maps' step runs: a Chancellor is seated, Foundation I
    * lies on its normal face, and a slot is left past the Empire's sites
    * and Rival's.
    * Under Wide Horizons and with no Empire nothing is filled at Setup,
    * and a first game has placed its eight sites. */
  def applies(ready: ReadyGame): Boolean =
    Imperials.chancellor(ready).nonEmpty && emptySlotsAtStart(ready) > 0 &&
      ready.game.campaign.foundations.get(FoundationNumber.I)
        .forall(_.face == FoundationFace.Normal)

  /** The end the Chancellor chose, or Recent when the Atlas held one site
    * and nothing was asked. */
  def chosenEnd(answered: Vector[Answered]): AtlasEnd =
    answered.collectFirst {
      case Answered(`decisionId`, ChooseOneAnswer(ref), _) => ref
    }.flatMap(AtlasPlacement.endOf).getOrElse(AtlasEnd.Recent)

  /** The step for `chancellor`. Recent or Forgotten is asked while the
    * Chronicle's Atlas holds two sites or more past Rival's; either answer
    * fills every empty slot from that end, one site at a time going
    * Hinterward. */
  def step(catalog: ExecutableCatalog, ready: ReadyGame, chancellor: PlayerId)
      : Operation =
    val question = Option.when(atlasAtStart(ready) >= 2)(
      Decide(decisionId, chancellor, DecisionQuery.ChooseOne(
        AtlasEnd.all.map(end => DecisionOption.Button(
          AtlasPlacement.endOption(end), optionLabel(catalog, ready, end))),
        heading = Some(heading))))
    val filledNote = Note(ProcedureNotes.id(TriggeredProcedureRef.Setup),
      states => Some(filledFrom(chosenEnd(states.answered))
        .sourceless(NoteArg.Player(chancellor))))
    Sequence(question.toVector ++ Vector(filledNote,
      Repeat((state, _) => state.game.current.map.nextSlot.nonEmpty &&
          state.game.campaign.atlas.sites.nonEmpty,
        Sequence(place(catalog), placedNote(chancellor)))))

  /** The words of `end`'s option. "In the Atlas box, all information is
    * private except the first site on the Recent Sites side and denizens
    * and edifices stored with it" (rulebook, "Public and Private
    * Information"), so Recent names that site, "Recent Sites (Buried Giant
    * first)", and Forgotten names none. */
  private def optionLabel(catalog: ExecutableCatalog, ready: ReadyGame,
      end: AtlasEnd): String =
    val words = AtlasPlacement.endLabel(end)
    val front = ready.game.campaign.atlas.site(AtlasEnd.Recent)
      .filter(_ => end == AtlasEnd.Recent)
    front.fold(words)(stored => s"$words (${catalog.site(stored.id)
      .fold(stored.id.value)(_.name)} first)")

  /** The next site from the chosen end goes to the next slot, and each
    * suit's bank takes 1 favor from the shared bank per denizen it places:
    * Foundation II resolves after Foundation I and counts every denizen on
    * the map. */
  private def place(catalog: ExecutableCatalog): BuildOps =
    BuildOps((state, pending) =>
      AtlasPlacement.at(catalog, state, chosenEnd(pending.answered))
        .map(planned => planned.operation +:
          AtlasPlacement.suitFavor(catalog, planned)),
      required = true)

  /** The site just placed: the new Hintermost site, since it took the first
    * empty slot going Hinterward. */
  private def placedNote(chancellor: PlayerId): Note =
    Note(ProcedureNotes.id(TriggeredProcedureRef.Setup), states =>
      states.now.game.current.map.hintermost.map(site =>
        placedSite.sourceless(NoteArg.Player(chancellor), NoteArg.Site(site))))
