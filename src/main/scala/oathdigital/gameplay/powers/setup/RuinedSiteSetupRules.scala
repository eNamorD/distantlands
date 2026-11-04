package oathdigital.gameplay.powers.setup

import oathdigital.catalog.{Edifice, EdificeFace, Locked, PrintedPower}
import oathdigital.model._

object SchoolOfVinesCard extends Edifice(EdificeId("E27"), Suit.Beast):
  object intact extends EdificeFace("School of Vines") with Locked:
    val power = PrintedPower(PowerId("edifice.e27.intact"),
      persistent = false, cost = Cost.free,
      text = "You may challenge a banner held by an enemy even if your " +
        "pawn is not at their pawn's site.")
    val powers: Vector[PrintedPower] = Vector(power)
  object ruined extends EdificeFace("Ruined School"):
    val power = PrintedPower(PowerId("edifice.e27.ruined"),
      persistent = false, cost = Cost.free,
      text = "**SETUP (END) / WHEN EXPLORED:** Place [secret] on each " +
        "site ruled by the bandits. _Players may take it in Wake._")
    val powers: Vector[PrintedPower] = Vector(power)

object ForestHornCard extends Edifice(EdificeId("E30"), Suit.Beast):
  object intact extends EdificeFace("Forest Horn") with Locked:
    val power = PrintedPower(PowerId("edifice.e30.intact"),
      persistent = false, cost = Cost(secret = 2),
      text = "**ACTION:** Choose a region. Discard all non-[suit-beast] " +
        "denizens at sites in that region.")
    val powers: Vector[PrintedPower] = Vector(power)
  object ruined extends EdificeFace("Broken Horn"):
    val power = PrintedPower(PowerId("edifice.e30.ruined"),
      persistent = false, cost = Cost.free,
      text = "SETUP (END) / **WHEN EXPLORED:** Bury this region's discard " +
        "pile.")
    val powers: Vector[PrintedPower] = Vector(power)

object GreatClockCard extends Edifice(EdificeId("E12"), Suit.Arcane):
  object intact extends EdificeFace("Great Clock") with Locked:
    val power = PrintedPower(PowerId("edifice.e12.intact"),
      persistent = false, cost = Cost(favor = 1, secret = 2, secretBurnt = 1),
      text = "**ACTION:** Advance the round marker once, if possible. _Do " +
        "not roll [round-die]. This cannot end the game._")
    val powers: Vector[PrintedPower] = Vector(power)
  object ruined extends EdificeFace("Broken Clock"):
    val power = PrintedPower(PowerId("edifice.e12.ruined"),
      persistent = false, cost = Cost.free,
      text = "**SETUP (END) / WHEN EXPLORED:** Advance the round marker " +
        "once, if possible. _Do not roll [round-die]. This cannot " +
        "end the game._")
    val powers: Vector[PrintedPower] = Vector(power)

/** The round marker's advance that both Great Clock faces make "if
  * possible" (catalog batch 10, slice 2): none on round 8, the last space.
  */
object RoundMarker:
  val LastRound: Int = 8

  /** The advance, or none on the last space. */
  def advance(ready: ReadyGame): Vector[CoreOperation] =
    Option.when(ready.game.current.tracks.round < LastRound)(AdvanceRound)
      .toVector

  /** The round the step before a line reached, when it moved the marker. */
  def reached(states: NoteStates): Option[Int] = for
    (before, after) <- states.previous
    round = after.game.current.tracks.round
    if round > before.game.current.tracks.round
  yield round

/** Ruined School (E27, ruined): "SETUP (END): Place 1 secret on each site
  * ruled by the bandits. Players may take it in Wake."
  *
  * Each bandit site takes one secret from the shared supply, as Bandit
  * Market's favor is placed; Take Wealth already takes a site's secrets.
  * Its line reads the step: "Placed 2 secrets on the bandit sites." With no
  * bandit site nothing is placed and no line is written. The School's
  * intact face is not implemented (catalog batch 9, "Out of scope").
  */
case object RuinedSchool extends RuinedSetupRule:
  val edifice: EdificeId = SchoolOfVinesCard.id
  val id: PowerId = SchoolOfVinesCard.ruined.power.id
  private val notes = NoteSet(id)
  /** "Placed {2 secrets} on the bandit sites." */
  val placed: NoteKey = notes.key("placed", Vector(NotePart.Text("Placed "),
    NotePart.Arg(0),
    NotePart.Plural(0, " on the bandit site.", " on the bandit sites.")))
  override def noteKeys: Vector[NoteKey] = notes.declared

  protected def build(ready: ReadyGame, at: SiteId): Vector[CoreOperation] =
    val current = ready.game.current
    current.map.inPlay.filter(site => SiteRule.ruler(
        current.map.sites(site).forces, current.players)
      .contains(SiteRuler.Bandits))
      .map(site => Move(Piece.Secrets(1),
        PositionedLocation(Location.SharedBank),
        PositionedLocation(Location.Site(site))))

  private def siteSecrets(ready: ReadyGame): Int =
    ready.game.current.map.sites.values.map(_.tokens.secrets).sum

  protected def note(at: SiteId): Note = Note(id, states => for
    (before, after) <- states.previous
    placedCount = siteSecrets(after) - siteSecrets(before)
    if placedCount > 0
  yield placed(PowerSourceRef.Card(edifice),
    NoteArg.Amount(placedCount, NoteUnit.Secret)))

/** Broken Horn (E30, ruined): "SETUP (END): Bury this region's discard
  * pile."
  *
  * Every card of the pile of the Horn's region is buried, bottom to top: a
  * denizen or a Vision to the bottom of the world deck, carrying no tokens
  * since a discarded card holds none. Its line reads the step: "Buried the
  * Provinces discard pile (3 cards)." or "The Provinces discard pile was
  * empty." The Horn's intact face is a later slice.
  */
case object BrokenHorn extends RuinedSetupRule:
  val edifice: EdificeId = ForestHornCard.id
  val id: PowerId = ForestHornCard.ruined.power.id
  private val notes = NoteSet(id)
  /** "Buried the {Provinces} discard pile ({3} cards)." */
  val buried: NoteKey = notes.key("buried", Vector(
    NotePart.Text("Buried the "), NotePart.Arg(0),
    NotePart.Text(" discard pile ("), NotePart.Arg(1),
    NotePart.Plural(1, " card).", " cards).")))
  /** "The {Provinces} discard pile was empty." */
  val empty: NoteKey = notes.key("empty", Vector(NotePart.Text("The "),
    NotePart.Arg(0), NotePart.Text(" discard pile was empty.")))
  override def noteKeys: Vector[NoteKey] = notes.declared

  private def region(ready: ReadyGame, at: SiteId): Option[Region] =
    ready.game.current.map.regionOf(at)

  private def pile(ready: ReadyGame, at: SiteId): Vector[WorldCardId] =
    region(ready, at).toVector.flatMap(ready.game.current.commonCards.discard)

  protected def build(ready: ReadyGame, at: SiteId): Vector[CoreOperation] =
    region(ready, at).toVector.flatMap(home =>
      val from = PositionedLocation(Location.RegionalDiscard(home))
      ready.game.current.commonCards.discard(home).map {
        case denizen: DenizenId => Bury(BuryableCard.Denizen(denizen), from)
        case vision: VisionId => Bury(BuryableCard.Vision(vision), from)
      })

  protected def note(at: SiteId): Note = Note(id, states => for
    (before, after) <- states.previous
    home <- region(after, at)
    count = pile(before, at).size - pile(after, at).size
  yield
    val card = PowerSourceRef.Card(edifice)
    if count > 0 then buried(card, NoteArg.RegionName(home),
      NoteArg.Number(count))
    else empty(card, NoteArg.RegionName(home)))

/** Broken Clock (E12, ruined): "SETUP (END): Advance the round marker
  * once, if possible. Do not roll the round die. This cannot end the game."
  *
  * The advance is [[RoundMarker]]'s. At Setup the marker is on round 1, so
  * it always moves. Its line reads the step: "Advanced the round marker to
  * round 2." The WHEN EXPLORED half waits for an Explore procedure. Great
  * Clock, the intact face, is an action (catalog batch 10, slice 2).
  */
case object BrokenClock extends RuinedSetupRule:
  val edifice: EdificeId = GreatClockCard.id
  val id: PowerId = GreatClockCard.ruined.power.id
  private val notes = NoteSet(id)
  /** "Advanced the round marker to round {2}." */
  val advanced: NoteKey = notes.key("advanced", Vector(
    NotePart.Text("Advanced the round marker to round "), NotePart.Arg(0),
    NotePart.Text(".")))
  override def noteKeys: Vector[NoteKey] = notes.declared

  protected def build(ready: ReadyGame, at: SiteId): Vector[CoreOperation] =
    RoundMarker.advance(ready)

  protected def note(at: SiteId): Note = Note(id, states =>
    RoundMarker.reached(states).map(round =>
      advanced(PowerSourceRef.Card(edifice), NoteArg.Number(round))))
