package oathdigital.gameplay.powers.rest

import oathdigital.catalog.{AdviserOnly, Denizen, ExecutableCatalog,
  PrintedPower}
import oathdigital.gameplay.powerresolver.PhasePower
import oathdigital.model._

object NaysayersCard extends Denizen(DenizenId("21"), "Naysayers", Suit.Discord) with AdviserOnly:
  val power = PrintedPower(PowerId("denizen.naysayers"),
    persistent = false, cost = Cost.free,
    text = "**REST:** If any Exile is the Oathkeeper or Usurper, take " +
      "[favor] from the Chancellor.")
  val powers: Vector[PrintedPower] = Vector(power)

/** Naysayers (card 21, adviser-only): "REST: If any Exile is the Oathkeeper
  * or Usurper, take [favor] from the Chancellor." (catalog batch 10,
  * slice 1).
  *
  * An optional REST power, as Insomnia's is. It is usable while an Exile,
  * the user included (Naysayers FAQ), holds the title on either side, and
  * a Chancellor other than the user has favor. It takes the favor with a
  * `Take` from the Chancellor's play area. Its line, "Took 1 favor from
  * {Purple}.", covers the generic take line.
  */
case object Naysayers extends PhasePower:
  val id: PowerId = NaysayersCard.power.id
  val Favor: Int = 1
  private val notes = NoteSet(id)
  /** "Took 1 favor from {0}." */
  val took: NoteKey = notes.key(NoteKey.Used, Vector(
    NotePart.Text("Took 1 favor from "), NotePart.Arg(0),
    NotePart.Text(".")))
  override def noteKeys: Vector[NoteKey] = notes.declared

  def timing: PowerTiming = PowerTiming.Rest

  /** Whether an Exile holds the title, on either side. */
  def exileTitled(ready: ReadyGame): Boolean =
    ready.game.current.title.holder.exists(holder =>
      Imperials.role(ready, holder).contains(Role.Exile))

  /** The Chancellor `player` would take from: one other than `player`,
    * with favor. */
  private def payer(ready: ReadyGame, player: PlayerId): Option[PlayerId] =
    Imperials.chancellor(ready).filter(chancellor => chancellor != player &&
      ready.game.current.player(chancellor)
        .exists(_.board.favor > 0))

  def usable(catalog: ExecutableCatalog, ready: ReadyGame, player: PlayerId,
      source: DecisionOptionRef): Boolean =
    exileTitled(ready) && payer(ready, player).isDefined

  def build(catalog: ExecutableCatalog, ready: ReadyGame, player: PlayerId,
      source: DecisionOptionRef): Either[OathViolation, Operation] =
    payer(ready, player).toRight(OathViolation.StartUnavailable(
      "The Chancellor has no favor to take")).map(chancellor =>
      Sequence(Vector(
        BuildOps((_, _) => Right(Vector(Take(Piece.Favor(Favor), player,
          Location.PlayArea(chancellor), Location.PlayArea(player),
          required = true))), required = true),
        Note(id, _ => PowerSourceRef.of(source).map(card =>
          took(card, NoteArg.Player(chancellor))), covers = true))))
