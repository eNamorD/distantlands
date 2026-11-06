package oathdigital.gameplay

import oathdigital.catalog.{AdviserOnly, Denizen, ExecutableCatalog, Locked,
  ModifierMark, PrintedPower, Relic}
import oathdigital.gameplay.powerresolver.ModifierPicks
import oathdigital.model._

object MarriageCard extends Denizen(DenizenId("148"), "Marriage", Suit.Hearth) with Locked with AdviserOnly:
  val power = PrintedPower(PowerId("denizen.marriage"),
    persistent = true, cost = Cost.free,
    text = "Marriage counts as two [suit-hearth] advisers, but only " +
      "counts as one toward your adviser limit.")
  val powers: Vector[PrintedPower] = Vector(power)

object PledgeToDiscordCard extends Denizen(DenizenId("223"), "Pledge to Discord", Suit.Discord) with Locked with AdviserOnly:
  val power = PrintedPower(PowerId("denizen.pledge-to-discord"),
    persistent = true, cost = Cost.free,
    text = "Your advisers all have the [suit-discord] suit instead of " +
      "their printed suit. _Marriage counts as two [suit-discord]._")
  val powers: Vector[PrintedPower] = Vector(power)

object SpitefulMirrorCard extends Relic(RelicId("R41"), "Spiteful Mirror", value = 27, defense = 1):
  val power = PrintedPower(PowerId("relic.spiteful-mirror"),
    persistent = false, cost = Cost(secret = 1),
    text = "This turn, you may act as if any cards you use to trade or " +
      "muster are [suit-discord] cards.")
  val powers: Vector[PrintedPower] = Vector(power)

object ActingTroupeCard extends Denizen(DenizenId("36"), "Acting Troupe", Suit.Arcane) with AdviserOnly:
  val power = PrintedPower(PowerId("denizen.acting-troupe"),
    persistent = false, cost = Cost.free,
    modifier = Some(ModifierMark.Action(MajorActionType.Trade)),
    text = "Act as if Acting Troupe is a [suit-beast] or [suit-order] " +
      "card instead.")
  val powers: Vector[PrintedPower] = Vector(power)
  /** The question its pick asks: which suit it acts as. */
  val suitDecision: String = "acting-troupe.suit"
  /** The suits it may act as, in the order the question offers them. */
  val suits: Vector[Suit] = Vector(Suit.Beast, Suit.Order)
  /** The question's answer that names `suit`. */
  def button(suit: Suit): DecisionOptionRef.Button =
    DecisionOptionRef.Button(suit.key)

/** A card's suit as the rules see it (catalog batch 8, slice 8).
  *
  * Pledge to Discord makes every faceup adviser of its holder Discord, while
  * Pledge itself is faceup among those advisers. A faceup Marriage counts
  * as two advisers of its suit, any other faceup adviser as one, and a
  * facedown adviser has no suit (catalog batch 5 rulings, "Counts as applies
  * everywhere"). Every reader of an adviser's suit reads `of`, including
  * the bank the favor on an adviser goes back to. Every reader of the card
  * a Trade or Muster draws on reads `traded`, which also sees Spiteful
  * Mirror.
  *
  * It lives in `gameplay`, below `actions` and `powers`, because the
  * operation pipeline reads `pledged` too. It holds the cards its rules
  * read: Marriage and Pledge to Discord, whose registered powers add
  * nothing, and Spiteful Mirror and Acting Troupe, whose picks it reads.
  * A picked Troupe counts as the suit its question was answered with,
  * beside a faceup Pledge too (catalog batch 10, slice 7).
  */
object EffectiveSuit:
  /** How many advisers of its suit `card` counts as, faceup. */
  def weight(card: CardId): Int = if card == MarriageCard.id then 2 else 1

  /** Whether `card` is a faceup adviser of a player who has Pledge to
    * Discord faceup. It needs no catalog: such a card is Discord whatever
    * it prints. */
  def pledged(ready: ReadyGame, card: CardId): Boolean =
    ready.game.current.players.exists(player =>
      faceUp(player, card) && faceUp(player, PledgeToDiscordCard.id))

  /** `card`'s suit: Discord when [[pledged]], otherwise its printed suit. */
  def of(catalog: ExecutableCatalog, ready: ReadyGame, card: CardId)
      : Option[Suit] =
    if pledged(ready, card) then Some(Suit.Discord)
    else catalog.suitOf(card)

  /** The same for a power that holds its own card object, which answers
    * the printed suit without a catalog. */
  def of(ready: ReadyGame, card: Denizen): Suit =
    if pledged(ready, card.id) then Suit.Discord else card.suit

  /** `player`'s faceup advisers of `suit`, each by its weight. `answered`
    * is the walk's answers, so a picked Acting Troupe counts as its
    * answered suit ([[troupe]]); a reader outside a Trade passes none. */
  def count(catalog: ExecutableCatalog, ready: ReadyGame, player: PlayerId,
      suit: Suit, answered: Vector[Answered] = Vector.empty): Int =
    ready.game.current.player(player).toVector.flatMap(_.advisers).collect {
        case DenizenState(id, Orientation.FaceUp, _)
            if acted(catalog, ready, answered, id).contains(suit) =>
          weight(id)
      }.sum

  /** Whether the walk's answers pick Spiteful Mirror, for a Trade or a
    * Muster. */
  def mirrored(answered: Vector[Answered]): Boolean =
    Vector(PowerWindow.TradeModifierSelection,
      PowerWindow.MusterModifierSelection).exists(window =>
      ModifierPicks.isPicked(answered, window, SpitefulMirrorCard.id))

  /** The suit a picked Acting Troupe acts as: the walk's answer to its
    * question, while the Trade's modifier decision picks it. */
  def troupe(answered: Vector[Answered]): Option[Suit] =
    val picked = ModifierPicks.isPicked(answered,
      PowerWindow.TradeModifierSelection, ActingTroupeCard.id)
    if !picked then None
    else answered.reverseIterator.collectFirst {
      case Answered(ActingTroupeCard.suitDecision,
          DecisionAnswer.ChooseOneAnswer(DecisionOptionRef.Button(key)), _) =>
        key
    }.flatMap(key => ActingTroupeCard.suits.find(_.key == key))

  /** The suit of the card `ref` names, as the Trade or Muster that draws on
    * it reads it: Discord once Spiteful Mirror is picked, otherwise
    * [[of]]. `None` for a reference that names no card. */
  def traded(catalog: ExecutableCatalog, ready: ReadyGame,
      answered: Vector[Answered], ref: DecisionOptionRef): Option[Suit] =
    DecisionOptionRef.denizenOrEdificeOf(ref).flatMap(card =>
      if mirrored(answered) then Some(Suit.Discord)
      else of(catalog, ready, card))

  /** `card`'s suit for the walk with `answered`: a picked Acting Troupe's
    * answered suit, otherwise [[of]]. */
  private def acted(catalog: ExecutableCatalog, ready: ReadyGame,
      answered: Vector[Answered], card: CardId): Option[Suit] =
    if card == ActingTroupeCard.id then
      troupe(answered).orElse(of(catalog, ready, card))
    else of(catalog, ready, card)

  private def faceUp(player: PlayerState, card: CardId): Boolean =
    player.advisers.exists {
      case DenizenState(held, Orientation.FaceUp, _) => held == card
      case _ => false
    }
