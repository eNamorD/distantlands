package oathdigital.gameplay.powers.action

import oathdigital.catalog.{Denizen, ExecutableCatalog, PrintedPower}
import oathdigital.gameplay.PowerAccess
import oathdigital.model._

object DarkEnforcerCard extends Denizen(DenizenId("227"), "Dark Enforcer", Suit.Discord):
  val power = PrintedPower(PowerId("denizen.dark-enforcer"),
    persistent = false, cost = Cost(favorBurnt = 1),
    text = "**ACTION:** Discard all [suit-order] and [suit-hearth] " +
      "cards from your site.")
  val powers: Vector[PrintedPower] = Vector(power)

/** Dark Enforcer (card 227), ACTION: burn 1 favor, then discard all Order
  * and Hearth cards from your site.
  *
  * Every faceup Order and Hearth denizen and ruined edifice at the player's
  * site gets the standard discard ([[SiteCards]]), as far as the discard
  * rules permit: each discard is optional, so one a restriction refuses is
  * skipped and the rest go. Nothing is asked.
  *
  * Its line names the cards that left the site, as Dazzle's does, and
  * covers the generic discard lines. With none, the line names the site.
  */
case object DarkEnforcer extends PaidAction(DarkEnforcerCard.power):
  private val notes = NoteSet(id)
  override def noteKeys: Vector[NoteKey] = notes.declared
  /** "Discarded {cards}." */
  val discarded: NoteKey = notes.key(NoteKey.Used, Vector(
    NotePart.Text("Discarded "), NotePart.Arg(0), NotePart.Text(".")))
  /** "{site} held no Order or Hearth card to discard." */
  val kept: NoteKey = notes.key("used.none", Vector(NotePart.Arg(0),
    NotePart.Text(" held no Order or Hearth card to discard.")))

  private val cards = new SiteCards(Set(Suit.Order, Suit.Hearth))

  def build(catalog: ExecutableCatalog, ready: ReadyGame, player: PlayerId,
      source: DecisionOptionRef)
      : Either[OathViolation, Operation] = Right(Sequence(Vector[Operation](
    BuildOps((live, _) => discards(catalog, live, player)),
    Note(this.id, discardedNote(catalog, _, player, source), covers = true))))

  private def discards(catalog: ExecutableCatalog,
      ready: ReadyGame, player: PlayerId)
      : Either[OathViolation, Vector[CoreOperation]] =
    PowerAccess.pawnSite(ready, player).toVector.flatMap(here =>
      cards.at(catalog, ready, here).map((card, suit) =>
        cards.discard(ready, here, card, suit, player, required = false)))
      .foldLeft[Either[OathViolation, Vector[CoreOperation]]](
        Right(Vector.empty))((done, next) =>
        done.flatMap(ops => next.map(ops :+ _)))

  /** The cards at the site before the step that are gone after it. None
    * gone names the site. */
  private def discardedNote(catalog: ExecutableCatalog,
      states: NoteStates, player: PlayerId,
      source: DecisionOptionRef): Option[LogNote] = for
    card <- PowerSourceRef.of(source)
    site <- PowerAccess.pawnSite(states.now, player)
  yield
    val gone: Vector[CardId] = states.previous.toVector.flatMap(
      (before, after) => cards.at(catalog, before, site).map(_._1.id)
        .filterNot(id =>
        after.game.current.map.sites.get(site)
          .exists(_.denizens.exists(_.id == id))))
    if gone.nonEmpty then discarded(card, NoteArg.Cards(gone))
    else kept(card, NoteArg.Site(site))
