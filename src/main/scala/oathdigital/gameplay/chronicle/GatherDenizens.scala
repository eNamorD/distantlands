package oathdigital.gameplay.chronicle

import oathdigital.catalog.ExecutableCatalog
import oathdigital.model._

/** Gather Denizens, the World's second step (CR p. 38). Every adviser and
  * every discard pile goes into the gathering pile, the tokens on an
  * adviser back to their banks first; then the pile is shuffled. All but nine go to the world deck bottom, unseen; with fewer
  * than nine, the world deck's top makes up the difference. */
object GatherDenizens:
  val dealt: Int = 9

  def steps(catalog: ExecutableCatalog): Vector[Operation] = Vector(
    BuildOps((state, _) => Right(gatherAdvisers(catalog, state)),
      required = true),
    BuildOps((state, _) => Right(gatherDiscards(state)), required = true),
    Shuffle(ShufflePile.CardGathering),
    BuildOps((state, _) => Right(deal(state)), required = true))

  /** Each adviser's tokens back to their banks (favor to its suit's bank,
    * secrets to its holder), then the card to the gathering pile. Locked
    * advisers go too: the rule moves every adviser, not a discard. */
  private def gatherAdvisers(catalog: ExecutableCatalog, state: ReadyGame)
      : Vector[CoreOperation] =
    state.game.current.players.flatMap(player => player.advisers.collect {
      case DenizenState(id, _, tokens) =>
        val onCard = PositionedLocation(Location.OnCard(id))
        val favor = catalog.denizen(id).filter(_ => tokens.favor > 0).map(
          denizen => Move(Piece.Favor(tokens.favor), onCard,
            PositionedLocation(Location.FavorBank(denizen.suit))))
        // Returned secrets land faceup and turn facedown, as a discard's do.
        val secrets =
          if tokens.secrets == 0 then Vector.empty
          else Vector(Move(Piece.Secrets(tokens.secrets), onCard,
            PositionedLocation(Location.PlayArea(player.player))),
            FlipSecrets(player.player, tokens.secrets, SecretSide.FaceUp,
              SecretSide.FaceDown))
        favor.toVector ++ secrets ++ Vector(Move(Piece.Card(id),
          PositionedLocation(Location.PlayArea(player.player)),
          PositionedLocation(Location.CardGathering)))
    }.flatten)

  private def gatherDiscards(state: ReadyGame): Vector[CoreOperation] =
    Region.all.flatMap(region =>
      state.game.current.commonCards.discard(region).map(card =>
        Move(Piece.Card(card),
          PositionedLocation(Location.RegionalDiscard(region)),
          PositionedLocation(Location.CardGathering))))

  private def deal(state: ReadyGame): Vector[CoreOperation] =
    val pile = state.game.current.cardGathering
    val surplus = pile.drop(dealt).map(card => Move(Piece.Card(card),
      PositionedLocation(Location.CardGathering),
      PositionedLocation(Location.Deck(CardDeck.World), StackPosition.Bottom)))
    val shortfall = state.game.current.commonCards.worldDeck
      .take(dealt - pile.size.min(dealt)).map(card => Move(Piece.Card(card),
        PositionedLocation(Location.Deck(CardDeck.World), StackPosition.Top),
        PositionedLocation(Location.CardGathering)))
    surplus ++ shortfall
