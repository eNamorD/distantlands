package distantlands.gameplay.powers.rest

import distantlands.catalog.{AdviserOnly, Denizen, ExecutableCatalog, Locked,
  PrintedPower}
import distantlands.gameplay.NoteSupport
import distantlands.gameplay.powerresolver._
import distantlands.model._

object SilverTongueCard extends Denizen(DenizenId("92"), "Silver Tongue", Suit.Discord) with Locked with AdviserOnly:
  val power = PrintedPower(PowerId("denizen.silver-tongue"),
    persistent = false, cost = Cost.free,
    text = "You can only have two advisers. **REST:** Take [favor] from " +
      "a favor bank matching a card at your site.")
  val powers: Vector[PrintedPower] = Vector(power)

/** Silver Tongue (card 92): "You can only have two advisers. REST: Take a
  * favor from a favor bank matching a card at your site."
  *
  * The REST power is a [[distantlands.gameplay.powerresolver.PhasePower]]. The
  * adviser limit is a [[HolderAdviserLimit]], a registered transform at
  * `SearchPlayAdviser`, which Insomnia shares.
  */
case object SilverTongue extends PhasePower with ContributingPower:
  val id: PowerId = SilverTongueCard.power.id
  /** How many advisers the holder may have, in either orientation. */
  val HolderLimit: Int = 2

  def choiceDecisionId(ready: ReadyGame, player: PlayerId): String =
    s"silver-tongue-${ready.game.current.tracks.round}-${player.value}"

  val cardId: DenizenId = SilverTongueCard.id

  private val limit = HolderAdviserLimit(cardId, HolderLimit)

  def timing: PowerTiming = PowerTiming.Rest
  def source: RuleSourceRef = RuleSourceRef.GameRule(id.value)
  private val notes = NoteSet(id)
  notes.include(NoteSupport.notes)
  override def noteKeys: Vector[NoteKey] = notes.declared

  def usable(catalog: ExecutableCatalog, ready: ReadyGame,
      player: PlayerId, source: DecisionOptionRef): Boolean =
    stocked(catalog, ready, player).nonEmpty

  def build(catalog: ExecutableCatalog, ready: ReadyGame, player: PlayerId,
      source: DecisionOptionRef)
      : Either[OathViolation, Operation] =
    val choice = choiceDecisionId(ready, player)
    Right(Sequence(Vector(Branch((state, _) =>
      stocked(catalog, state, player) match {
      case Vector(only) => Vector(take(player, _ => Right(only)))
      case several => Vector(
        Decide(choice, player, DecisionQuery.ChooseOne(several.map(suit =>
          DecisionOption.FavorBank(DecisionOptionRef.FavorBank(suit))),
          heading = Some("Silver Tongue: take a favor from a bank"))),
        take(player, pending => pending.answered.collectFirst {
          case Answered(`choice`, DecisionAnswer.ChooseOneAnswer(
            DecisionOptionRef.FavorBank(suit)), _) => suit
        }.toRight(OathViolation.InvalidEventOrder(
          s"no Silver Tongue bank is recorded for $choice"))))
    }),
      Note(id, NoteSupport.tookNote(source, player), covers = true))))

  def contributions: Map[PowerWindow, Vector[Contribution]] =
    Map(PowerWindow.SearchPlayAdviser -> Vector(limit.contribution))

  /** The adviser limit Silver Tongue sets on `player`: its holder, and only
    * while it is faceup.
    */
  def limitFor(ready: ReadyGame, player: PlayerId): Option[Int] =
    limit.limitFor(ready, player)

  /** Suits of faceup denizens and edifices at the player's pawn site whose
    * bank holds favor, in suit order.
    */
  private def stocked(catalog: ExecutableCatalog,
      ready: ReadyGame, player: PlayerId): Vector[Suit] =
    val current = ready.game.current
    val cards = current.pawnSite(player)
      .flatMap(current.map.sites.get).toVector.flatMap(_.denizens.collect {
        case DenizenState(card, Orientation.FaceUp, _) => card: CardId
        case EdificeState(card, _, _) => card: CardId
      })
    val suits = cards.flatMap(catalog.suitOf(_)).toSet
    Suit.all.filter(suit => suits(suit) && ready.banks.favor.getOrElse(suit, 0) > 0)

  private def take(player: PlayerId,
      suit: PendingTree => Either[OathViolation, Suit]): Operation =
    BuildOps((_, pending) => suit(pending).map(bank => Vector(Move(
      Piece.Favor(1), PositionedLocation(Location.FavorBank(bank)),
      PositionedLocation(Location.PlayArea(player))))))
