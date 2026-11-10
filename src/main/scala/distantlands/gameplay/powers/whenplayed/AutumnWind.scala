package oathdigital.gameplay.powers.whenplayed

import oathdigital.catalog.{Denizen, PrintedPower}
import oathdigital.gameplay.powerresolver.PowerCtx
import oathdigital.gameplay.powers.PowerAnswers
import oathdigital.model._

object AutumnWindCard extends Denizen(DenizenId("213"), "Autumn Wind", Suit.Beast):
  val power = PrintedPower(PowerId("denizen.autumn-wind"),
    persistent = false, cost = Cost.free,
    text = "**WHEN PLAYED,** shuffle together all discard piles, then " +
      "deal these cards to the discard piles as evenly as " +
      "possible. You decide ties.")
  val powers: Vector[PrintedPower] = Vector(power)

/** Autumn Wind (card 213), WHEN PLAYED: "shuffle together all discard
  * piles, then deal these cards to the discard piles as evenly as
  * possible. You decide ties." (catalog batch 10, slice 10).
  *
  * The effect is one `Branch` that reads the piles' total. With no cards
  * nothing happens and no line is written. When the total does not divide
  * by 3, the user names as many piles as the remainder to take one card
  * more. The question comes first: the shuffle shows what nobody may see
  * yet, so the legality search stops there (`HiddenOutcomes`). Then every
  * pile, bottom first and in region order, goes to the gathering pile, the
  * walker shuffles it and records the order, and the pile is dealt from
  * its head. Each round of the deal gives one card to every pile still
  * short of its share, in region order, and each card goes on top.
  *
  * Nothing parks between the gathering and the deal, so no viewer ever
  * sees the gathering pile. Every dealt card passed through it, so nobody
  * knows any of them (`CardKnowledgeMoves`). The line names each pile's
  * count, and so the answer.
  */
case object AutumnWind extends WhenPlayedPower:
  val cardId: DenizenId = AutumnWindCard.id
  val id: PowerId = AutumnWindCard.power.id
  val decisionId: String = "power.autumn-wind.extra"

  private val notes = NoteSet(id)
  /** "Shuffled the discard piles together and dealt {4} to the Cradle, {4}
    * to the Provinces and {3} to the Hinterland." */
  val dealt: NoteKey = notes.key("dealt", Vector(
    NotePart.Text("Shuffled the discard piles together and dealt "),
    NotePart.Arg(0), NotePart.Text(" to the Cradle, "), NotePart.Arg(1),
    NotePart.Text(" to the Provinces and "), NotePart.Arg(2),
    NotePart.Text(" to the Hinterland.")))
  override def noteKeys: Vector[NoteKey] = notes.declared
  override def narratedDecisions: Set[String] = Set(decisionId)

  /** The question's heading for `extra` extra cards. */
  def heading(extra: Int): String =
    if extra == 1 then "Autumn Wind: choose the pile that takes the extra card"
    else s"Autumn Wind: choose the $extra piles that take the extra cards"

  private val gathering = PositionedLocation(Location.CardGathering)

  def effect(ctx: PowerCtx): Vector[Operation] = Vector(Branch((live, _) =>
    val total = Region.all.map(region =>
      live.game.current.commonCards.discard(region).size).sum
    val extra = total % Region.all.size
    if total == 0 then Vector.empty
    else
      val ask = if extra == 0 then Vector.empty
        else Vector(question(ctx.actor, extra))
      ask ++ Vector[Operation](
        BuildOps((ready, _) => Right(gathered(ready, ctx.actor)),
          required = true),
        Shuffle(ShufflePile.CardGathering),
        BuildOps((ready, pending) => takers(pending, extra)
          .flatMap(deal(ready, _, ctx.actor)), required = true),
        Note(id, states => Some(dealt(PowerSourceRef.Card(cardId),
          Region.all.map(region => NoteArg.Number(
            states.now.game.current.commonCards.discard(region).size))*))))))

  private def question(actor: PlayerId, extra: Int): Decide =
    Decide(decisionId, actor, DecisionQuery.ChooseMany(extra, extra,
      Region.all.map(region =>
        DecisionOption.DiscardPile(DecisionOptionRef.DiscardPile(region))),
      heading = Some(heading(extra)),
      confirm = Some(CountedConfirm("Deal an extra card to", "pile",
        "piles"))))

  /** Every discard pile, bottom first, in region order, to the gathering
    * pile. */
  private def gathered(ready: ReadyGame, actor: PlayerId)
      : Vector[CoreOperation] =
    Region.all.flatMap(region =>
      ready.game.current.commonCards.discard(region).map(card =>
        Move(Piece.Card(card),
          PositionedLocation(Location.RegionalDiscard(region)), gathering,
          by = Some(actor))))

  /** The piles that take an extra card: none when the total divides by 3,
    * otherwise the `extra` different piles answered. */
  private def takers(pending: PendingTree, extra: Int)
      : Either[OathViolation, Set[Region]] =
    if extra == 0 then Right(Set.empty)
    else PowerAnswers.many(pending, decisionId)
      .toRight(PowerAnswers.missing(decisionId)).flatMap { refs =>
        val regions = refs.collect {
          case DecisionOptionRef.DiscardPile(region) => region }
        Either.cond(refs.size == extra && regions.distinct.size == extra,
          regions.toSet, OathViolation.InvalidEventOrder(
            s"Autumn Wind deals its extra cards to $extra different piles"))
      }

  /** The gathering pile dealt from its head. Each round gives one card to
    * every pile still short of its share, in region order, so `takers`
    * each take one card more than the others. Refuses when the pile does
    * not hold exactly the cards counted before the question, such as a
    * card already gathered, which the deal would leave stranded. */
  private def deal(ready: ReadyGame, takers: Set[Region], actor: PlayerId)
      : Either[OathViolation, Vector[CoreOperation]] =
    val pile = ready.game.current.cardGathering
    val share = pile.size / Region.all.size
    def quota(region: Region): Int =
      share + (if takers.contains(region) then 1 else 0)
    val seats = (0 to share).toVector.flatMap(round =>
      Region.all.filter(region => round < quota(region)))
    Either.cond(seats.size == pile.size,
      pile.zip(seats).map((card, region) => Move(Piece.Card(card), gathering,
        PositionedLocation(Location.RegionalDiscard(region),
          StackPosition.Top), by = Some(actor))),
      OathViolation.InvalidEventOrder(
        "Autumn Wind deals only the cards it gathered"))
