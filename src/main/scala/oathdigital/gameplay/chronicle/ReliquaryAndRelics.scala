package oathdigital.gameplay.chronicle

import oathdigital.catalog.ExecutableCatalog
import oathdigital.gameplay.operations.TheGrandScepterCard
import oathdigital.gameplay.powers.PowerAnswers
import oathdigital.model._

/** Stock Reliquary and Scatter Relics, the Throne's fourth and fifth steps
  * (CR p. 37). */
object ReliquaryAndRelics:
  val discardDecisionId: String = "chronicle.stock-reliquary.discard"
  val reliquaryLimit: Int = 5

  private val scepter = TheGrandScepterCard.id

  /** The Throne holder adds every relic they hold but the Grand Scepter to
    * the Reliquary, facedown, then discards down to five of their choice. */
  val stock: Vector[Operation] = Vector(Branch((state, _) =>
    ChronicleTasks.forHolder(state, ChronicleTask.Throne)(holder => Vector(
      BuildOps((live, _) => Right(held(live, holder).map(relic =>
        Move(Piece.Card(relic.id),
          PositionedLocation(Location.PlayArea(holder)),
          PositionedLocation(Location.Reliquary)))), required = true),
      Branch((live, _) => discardDown(live.game.campaign.reliquary, holder))))))

  private def held(state: ReadyGame, player: PlayerId): Vector[RelicState] =
    state.game.current.players.find(_.player == player).toVector
      .flatMap(_.relics.filterNot(_.id == scepter))

  private def discardDown(reliquary: Vector[RelicId], holder: PlayerId)
      : Vector[Operation] =
    val excess = reliquary.size - reliquaryLimit
    if excess <= 0 then Vector.empty
    else Vector(
      Decide(discardDecisionId, holder, DecisionQuery.ChooseMany(excess,
        excess, reliquary.map(id =>
          DecisionOption.Relic(DecisionOptionRef.Relic(id))),
        heading = Some("Discard relics from the Reliquary"),
        confirm = Some(CountedConfirm("Discard", "relic", "relics")))),
      BuildOps((_, pending) => PowerAnswers.many(pending, discardDecisionId)
        .toRight(PowerAnswers.missing(discardDecisionId)).map(_.collect {
          case DecisionOptionRef.Relic(id) => Move(Piece.Card(id),
            PositionedLocation(Location.Reliquary),
            PositionedLocation(Location.SetAsideRelics))
        }), required = true))

  /** Set aside the Grand Scepter and the Reliquary: neither is touched, the
    * Scepter staying with its holder. Every relic players hold joins the
    * discarded relics; they are shuffled and dealt facedown to empty relic
    * slots going Hinterward, and the rest return to the relic deck. Dealing
    * takes the head of the shuffled pile; which end is "top" does not
    * matter after a shuffle. */
  def scatter(catalog: ExecutableCatalog): Vector[Operation] = Vector(
    BuildOps((state, _) => Right(state.game.current.players.flatMap(player =>
      held(state, player.player).map(relic => Discard.Relic(relic.id,
        PositionedLocation(Location.PlayArea(player.player)),
        relic.tokens.secrets, player.player)))), required = true),
    Shuffle(ShufflePile.SetAsideRelics),
    BuildOps((state, _) => Right(deal(catalog, state)), required = true))

  private def deal(catalog: ExecutableCatalog, state: ReadyGame)
      : Vector[CoreOperation] =
    val map = state.game.current.map
    val slots = map.inPlay.flatMap(site => Vector.fill(math.max(0,
      catalog.site(site).fold(0)(_.relicSlots) -
        map.sites.get(site).fold(0)(_.relics.size)))(site))
    val relics = state.game.current.setAsideRelics
    val from = PositionedLocation(Location.SetAsideRelics)
    relics.zip(slots).map((relic, site) => Move(Piece.Card(relic), from,
      PositionedLocation(Location.Site(site)),
      resultingOrientation = Some(Orientation.FaceDown))) ++
      relics.drop(slots.size).map(relic => Move(Piece.Card(relic), from,
        PositionedLocation(Location.Deck(CardDeck.Relic),
          StackPosition.Bottom)))
