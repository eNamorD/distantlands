package distantlands.gameplay.chronicle

import distantlands.catalog.ExecutableCatalog
import distantlands.gameplay.actions.ProcedureNotes
import distantlands.gameplay.powerresolver.{Contribution, ContributingPower,
  Transform}
import distantlands.gameplay.powers.economy.WildPasturesCard
import distantlands.model._

/** Blasted Pastures (E07 ruined): "CHRONICLE (START OF THRONE): Bury all
  * Nomad cards on the map unless the winner rules this card." (Last
  * Chronicle powers design, rulings 5 and 6). Its step is at the start of
  * the Throne, after Reformer's and Hiding Place's. It reads the board
  * when reached. While the ruined face is on the map and the Throne holder
  * does not rule its site, by Shape Empire's reading
  * (`ChronicleTasks.rules`), every Nomad card at a site in play is buried:
  * Blasted Pastures itself, locked cards and intact edifices included.
  * Favor on a card returns to the Nomad bank and its secrets go to the
  * Throne holder, facedown (`Bury.standard`). Advisers and relics are not
  * on the map, and a bury is not a discard, so Rival, Enduring and Hall of
  * Bandits keep nothing from it.
  *
  * Its one line, "Blasted Pastures buried {cards}", comes before the
  * burials: the log names a card by the state just after its note, and a
  * buried edifice has no side in the edifice deck. */
case object BlastedPastures extends ContributingPower:
  val id: PowerId = WildPasturesCard.ruined.power.id
  def source: RuleSourceRef = RuleSourceRef.GameRule(id.value)
  /** After Hiding Place, whose priority is 1 (ruling 6). Transforms of one
    * phase fold by priority, then by source key, and this power's key
    * sorts before Hiding Place's and Reformer's. */
  override def priority: Int = 2
  def contributions: Map[PowerWindow, Vector[Contribution]] = Map(
    PowerWindow.ChronicleStartOfThrone -> Vector(Transform(
      (ctx, ops) => ops :+ step(ctx.catalog))))

  private val noteId = ProcedureNotes.id(TriggeredProcedureRef.ChronicleThrone)
  private val notes = NoteSet(noteId)
  /** "Blasted Pastures buried Elders and Giant's Rampage". */
  val buried: NoteKey = notes.key("blasted-pastures-buried", Vector(
    NotePart.Arg(0), NotePart.Text(" buried "), NotePart.Arg(1)))
  /** The Throne lines this power writes, on the Throne's note id, which
    * `ChronicleThroneProcedure.noteKeys` declares. */
  def throneNoteKeys: Vector[NoteKey] = notes.declared

  def step(catalog: ExecutableCatalog): Operation = Branch((state, _) =>
    val current = state.game.current
    current.edificeSite(WildPasturesCard.id, EdificeSide.Ruined)
      .filter(current.map.inPlay.contains).toVector.flatMap(site =>
        ChronicleTasks.forHolder(state, ChronicleTask.Throne)(holder =>
          if ChronicleTasks.rules(state, holder, site) then Vector.empty
          else burying(burials(catalog, state, holder)))))

  /** The line, then one batch burying every card of `found`. */
  private def burying(found: Vector[(CardId, Vector[CoreOperation])])
      : Vector[Operation] =
    if found.isEmpty then Vector.empty
    else Vector(
      Note(noteId, _ => Some(buried.sourceless(
        NoteArg.Card(WildPasturesCard.id), NoteArg.Cards(found.map(_._1))))),
      BuildOps((_, _) => Right(found.flatMap(_._2)), required = true))

  /** Each Nomad card at a site in play, in map order, with its burial. */
  private def burials(catalog: ExecutableCatalog, state: ReadyGame,
      holder: PlayerId): Vector[(CardId, Vector[CoreOperation])] =
    val current = state.game.current
    current.map.inPlay.flatMap(site =>
      current.map.sites.get(site).toVector.flatMap(_.denizens).flatMap(card =>
        catalog.suitOf(card.id).filter(_ == Suit.Nomad).map(suit =>
          (card.id: CardId) -> Bury.standard(buryable(card),
            PositionedLocation(Location.Site(site)), Some(suit),
            card.tokens.favor, card.tokens.secrets, holder))))

  private def buryable(card: SiteDenizenState): BuryableCard = card match
    case held: DenizenState => BuryableCard.Denizen(held.id)
    case held: EdificeState => BuryableCard.Edifice(held.id)
