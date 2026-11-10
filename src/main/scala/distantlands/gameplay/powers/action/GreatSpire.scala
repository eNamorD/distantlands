package distantlands.gameplay.powers.action

import distantlands.catalog.ExecutableCatalog
import distantlands.gameplay.powers.DispossessedDraws
import distantlands.gameplay.powers.setup.GreatSpireCard
import distantlands.model._

/** Great Spire (E11, intact), ACTION: place 1 secret on this card, add 3
  * random cards from the Dispossessed to this region's discard pile, then
  * add the bottom 3 denizens from that pile to the Dispossessed (catalog
  * batch 10, slice 6).
  *
  * The draw is [[DispossessedDraws]]'s: the Dispossessed is shuffled, and
  * its first cards go on top of the pile. Both directions move the same
  * count, the smaller of 3 and the Dispossessed's size (ruling 7). The
  * bottom denizens are read after the new cards arrive, skipping Visions,
  * so a pile with fewer than 3 denizens sends back some of the new cards.
  * Nobody peeks. An empty Dispossessed cannot occur (ruling 7); if it did,
  * nothing would move and no line would be written. Its line: "Swapped 3
  * cards between the Dispossessed and the Cradle discard pile."
  */
case object GreatSpire extends PaidAction(GreatSpireCard.intact.power):
  val Count: Int = 3

  private val notes = NoteSet(id)
  override def noteKeys: Vector[NoteKey] = notes.declared
  /** "Swapped {3} {cards} between the Dispossessed and the {Cradle discard
    * pile}." */
  val used: NoteKey = notes.key(NoteKey.Used, Vector(
    NotePart.Text("Swapped "), NotePart.Arg(0),
    NotePart.Plural(0, " card", " cards"),
    NotePart.Text(" between the Dispossessed and the "), NotePart.Arg(1),
    NotePart.Text(".")))

  def build(catalog: ExecutableCatalog, ready: ReadyGame, player: PlayerId,
      source: DecisionOptionRef): Either[OathViolation, Operation] =
    Right(Sequence(Vector[Operation](Branch((live, _) =>
      spireRegion(live).toVector.flatMap { region =>
        val count = math.min(Count, live.game.campaign.dispossessed.size)
        if count == 0 then Vector.empty
        else swap(player, region, count)
      }))))

  private def swap(player: PlayerId, region: Region, count: Int)
      : Vector[Operation] = Vector(
    DispossessedDraws.shuffle,
    BuildOps((now, _) => Right(DispossessedDraws.toPile(now, region, count,
      Some(player))), required = true),
    BuildOps((now, _) => Right(DispossessedDraws.bottomDenizens(now, region,
      count).map(DispossessedDraws.intoDispossessed(_, region, Some(player)))),
      required = true),
    Note(id, _ => Some(used(PowerSourceRef.Card(GreatSpireCard.id),
      NoteArg.Number(count),
      NoteArg.Pile(SearchSource.RegionalDiscard(region))))))

  /** The region of the site where the intact Spire stands. */
  private def spireRegion(ready: ReadyGame): Option[Region] =
    ready.game.current.edificeSite(GreatSpireCard.id, EdificeSide.Intact)
      .flatMap(ready.game.current.map.regionOf)
