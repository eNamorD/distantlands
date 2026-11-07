package oathdigital.gameplay.powers.whenplayed

import oathdigital.catalog.{Denizen, PrintedPower}
import oathdigital.gameplay.actions.cardplay.CardPlayProcedure
import oathdigital.gameplay.powerresolver.{ModifierPicks, PowerCtx}
import oathdigital.gameplay.powers.PowerAnswers
import oathdigital.model._

object SearchPartyCard extends Denizen(DenizenId("240"), "Search Party",
    Suit.Nomad):
  val power = PrintedPower(PowerId("denizen.search-party"),
    persistent = false, cost = Cost.free,
    text = "**WHEN PLAYED,** peek at the bottom 3 cards of a discard " +
      "pile and play one as if you searched. _Return the rest._")
  val powers: Vector[PrintedPower] = Vector(power)

/** Search Party (card 240), WHEN PLAYED: "peek at the bottom 3 cards of a
  * discard pile and play one as if you searched. Return the rest."
  * (catalog batch 10, slice 12).
  *
  * The user picks a discard pile; an empty one shows unavailable. Its
  * bottom 3 cards, or fewer, are recorded as `Peek`s, so only the user may
  * identify them, and the user must play one. The play is the Search's
  * card play from the pile itself (`Origin.DiscardPile`): faceup to the
  * site, faceup or facedown as an adviser, or discarded to the next
  * region's pile (ruling 5). A Vision follows Search's rules and leaves
  * the Visions Drawn track alone. The card never enters the temporary
  * hand, which an enclosing Search reads. The rest never moved, so they
  * stay at the bottom in order. The play's hook fires the card's own WHEN
  * PLAYED and the card-play triggers.
  *
  * The play may empty every pile, so the effect reads the live piles only
  * until the pile question is answered. After that it always builds the
  * full shape, so a resume inside the play finds its position.
  *
  * Its line covers the peeks: "Peeked at the bottom of the Hinterland
  * discard pile." The card play writes its own line, which names the card.
  * With every pile empty nothing is asked: "Every discard pile was empty."
  */
case object SearchParty extends WhenPlayedPower:
  val id: PowerId = SearchPartyCard.power.id
  val cardId: DenizenId = SearchPartyCard.id
  val pileDecisionId: String = "power.search-party.pile"
  val cardDecisionId: String = "power.search-party.card"
  /** How many cards it peeks at. */
  val Peeked: Int = 3
  /** Why an empty pile cannot be picked. */
  val emptyPile: String = "The pile is empty"

  private val notes = NoteSet(id)
  /** "Peeked at the bottom of the {Hinterland discard pile}." */
  val used: NoteKey = notes.key(NoteKey.Used, Vector(
    NotePart.Text("Peeked at the bottom of the "), NotePart.Arg(0),
    NotePart.Text(".")))
  /** "Every discard pile was empty." */
  val none: NoteKey = notes.key("used.none", Vector(
    NotePart.Text("Every discard pile was empty.")))
  override def noteKeys: Vector[NoteKey] = notes.declared
  override def narratedDecisions: Set[String] =
    Set(pileDecisionId, cardDecisionId)

  /** The pile question's button for `region`'s discard pile. */
  def pileRef(region: Region): DecisionOptionRef.Button =
    DecisionOptionRef.Button(region.key)

  private val searchParty: PowerSourceRef = PowerSourceRef.Card(cardId)

  def effect(ctx: PowerCtx): Vector[Operation] =
    Vector(Branch((live, pending) =>
      if chosen(pending.answered).isEmpty &&
          Region.all.forall(bottom(live, _).isEmpty) then
        Vector[Operation](Note(id, _ => Some(none(searchParty))))
      else Vector[Operation](pileQuestion(live, ctx.actor), peeks(ctx.actor),
        Note(id, peekedNote, covers = true), cardQuestion(ctx.actor),
        play(ctx))))

  /** The three piles, an empty one unavailable. */
  private def pileQuestion(ready: ReadyGame, actor: PlayerId): Decide =
    Decide(pileDecisionId, actor, DecisionQuery.ChooseOne(
      Region.all.map { region =>
        val button = DecisionOption.Button(pileRef(region),
          SearchSource.name(SearchSource.RegionalDiscard(region)).capitalize)
        if bottom(ready, region).isEmpty then
          DecisionOption.Unavailable(button, OptionReason.Written(emptyPile))
        else button
      },
      heading = Some("Search Party: peek at the bottom of a discard pile")))

  /** A peek at each card at the bottom of the chosen pile. */
  private def peeks(actor: PlayerId): Operation =
    BuildOps((now, pending) => Right(chosen(pending.answered).toVector
      .flatMap(region => bottom(now, region).map(card =>
        Peek(actor, card, Location.RegionalDiscard(region))))))

  private def peekedNote(states: NoteStates): Option[LogNote] =
    chosen(states.answered).map(region => used(searchParty,
      NoteArg.Pile(SearchSource.RegionalDiscard(region))))

  /** The card to play, among the peeked ones, bottom first. */
  private def cardQuestion(actor: PlayerId): Operation =
    Branch((now, pending) => chosen(pending.answered).toVector.map(region =>
      Decide(cardDecisionId, actor, DecisionQuery.ChooseOne(
        bottom(now, region).map(option),
        heading = Some("Search Party: choose a card to play")))))

  /** The picked card's play from its pile. Once it has been played, the
    * tree settles from the answers (`CardPlayProcedure.unchecked`). */
  private def play(ctx: PowerCtx): Operation = Branch((now, pending) =>
    (for
      region <- chosen(pending.answered)
      card <- PowerAnswers.last(pending.answered, cardDecisionId)
        .flatMap(worldCard)
    yield CardPlayProcedure.unchecked(ctx.catalog, now, ctx.actor, card,
      CardPlayProcedure.Origin.DiscardPile(region),
      ModifierPicks.cards(pending.answered))).toVector)

  private def chosen(answered: Vector[Answered]): Option[Region] =
    PowerAnswers.last(answered, pileDecisionId)
      .flatMap(ref => Region.all.find(pileRef(_) == ref))

  /** The pile's bottom cards, bottom first. A pile is stored top last. */
  private def bottom(ready: ReadyGame, region: Region): Vector[WorldCardId] =
    ready.game.current.commonCards.discard(region).take(Peeked)

  private def option(card: WorldCardId): DecisionOption = card match
    case denizen: DenizenId =>
      DecisionOption.Denizen(DecisionOptionRef.Denizen(denizen))
    case vision: VisionId =>
      DecisionOption.Vision(DecisionOptionRef.Vision(vision))

  private def worldCard(ref: DecisionOptionRef): Option[WorldCardId] =
    ref match
      case DecisionOptionRef.Denizen(denizen) => Some(denizen)
      case DecisionOptionRef.Vision(vision) => Some(vision)
      case _ => None
