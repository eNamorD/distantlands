package oathdigital.gameplay.powers.search

import oathdigital.catalog.{Denizen, ExecutableCatalog, ModifierMark,
  PrintedPower}
import oathdigital.gameplay.NoteSupport
import oathdigital.gameplay.powerresolver.AsksOnSelection
import oathdigital.gameplay.powers.PowerAnswers
import oathdigital.model._

object BrackenCard extends Denizen(DenizenId("197"), "Bracken", Suit.Beast):
  val power = PrintedPower(PowerId("denizen.bracken"),
    persistent = false, cost = Cost(secret = 1),
    modifier = Some(ModifierMark.Action(MajorActionType.Search)),
    text = "You may put all of the cards you discard on the top or " +
      "bottom of any one discard pile _(even your region's)_.")
  val powers: Vector[PrintedPower] = Vector(power)

/** Bracken (card 197), a selected Search modifier for 1 secret, placed on
  * the card: "You may put all of the cards you discard on the top or bottom
  * of any one discard pile (even your region's)." (catalog batch 10,
  * slice 9).
  *
  * Picked and paid for, it asks a discard pile, any of the three, then Top
  * or Bottom, in the same pass of the modifier decision, as `ActingTroupe`
  * asks its suit. Every discard of the Search ([[SearchDiscards]]) then
  * goes to that end of that pile, read from the answers when the discard
  * is built. Several discards to the bottom go one under the other, so the
  * last discarded lies lowest.
  *
  * Its one line follows the answers and names both: "This Search's
  * discards go to the bottom of the Hinterland discard pile." The generic
  * discard lines then name the pile of each card.
  */
case object Bracken extends SearchDiscards, AsksOnSelection:
  val id: PowerId = BrackenCard.power.id
  val card: Denizen = BrackenCard
  override def cost: Cost = BrackenCard.power.cost

  val pileDecision: String = "bracken.pile"
  val positionDecision: String = "bracken.position"
  /** The pile question's button for `region`'s discard pile. */
  def pileRef(region: Region): DecisionOptionRef.Button =
    DecisionOptionRef.Button(region.key)
  val top: DecisionOptionRef.Button = DecisionOptionRef.Button("top")
  val bottom: DecisionOptionRef.Button = DecisionOptionRef.Button("bottom")

  private val notes = NoteSet(id)
  override def noteKeys: Vector[NoteKey] = notes.declared
  /** "This Search's discards go to the top of the {Cradle discard
    * pile}." */
  val discardsTop: NoteKey = notes.key("discards-top", Vector(
    NotePart.Text("This Search's discards go to the top of the "),
    NotePart.Arg(0), NotePart.Text(".")))
  /** "This Search's discards go to the bottom of the {Cradle discard
    * pile}." */
  val discardsBottom: NoteKey = notes.key("discards-bottom", Vector(
    NotePart.Text("This Search's discards go to the bottom of the "),
    NotePart.Arg(0), NotePart.Text(".")))
  override def narratedDecisions: Set[String] =
    Set(pileDecision, positionDecision)

  /** The pile question, the end question, then the line. */
  def selectionQuestions(catalog: ExecutableCatalog, ready: ReadyGame,
      actor: PlayerId): Vector[Operation] = Vector(
    Decide(pileDecision, actor, DecisionQuery.ChooseOne(
      Region.all.map(region => DecisionOption.Button(pileRef(region),
        SearchSource.name(SearchSource.RegionalDiscard(region)))),
      heading = Some("Bracken: choose a discard pile"))),
    Decide(positionDecision, actor, DecisionQuery.ChooseOne(Vector(
      DecisionOption.Button(top, "Top"),
      DecisionOption.Button(bottom, "Bottom")),
      heading = Some("Bracken: the top or the bottom of the pile?"))),
    Note(id, sentNote))

  /** The pile and the end the answers chose. */
  private def destination(answered: Vector[Answered])
      : Option[(Region, StackPosition)] = for
    pile <- PowerAnswers.last(answered, pileDecision)
      .flatMap(ref => Region.all.find(pileRef(_) == ref))
    end <- PowerAnswers.last(answered, positionDecision).collect {
      case `top` => StackPosition.Top
      case `bottom` => StackPosition.Bottom
    }
  yield (pile, end)

  def redirected(ready: ReadyGame, answered: Vector[Answered],
      actor: PlayerId)(discard: SearchDiscards.UserDiscard)
      : Vector[CoreOperation] =
    destination(answered).fold(Vector[CoreOperation](discard)) {
      (pile, end) => discard match
        case denizen: Discard.Denizen =>
          Vector(denizen.copy(to = pile, position = end))
        case vision: Discard.Vision =>
          Vector(vision.copy(to = pile, position = end))
    }

  private def sentNote(states: NoteStates): Option[LogNote] =
    for
      chosen <- NoteSupport.answer(states, pileDecision)
      pile <- Region.all.find(pileRef(_) == chosen)
      end <- NoteSupport.answer(states, positionDecision)
      key <- Option.when(end == top)(discardsTop)
        .orElse(Option.when(end == bottom)(discardsBottom))
    yield key(PowerSourceRef.Card(cardId),
      NoteArg.Pile(SearchSource.RegionalDiscard(pile)))
