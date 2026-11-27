package distantlands.gameplay.powers.action

import distantlands.catalog.{Denizen, ExecutableCatalog, PrintedPower}
import distantlands.gameplay.NoteSupport
import distantlands.gameplay.powers.PlayerFacts
import distantlands.model._

object BanditPaymasterCard extends Denizen(DenizenId("219"), "Bandit Paymaster", Suit.Discord):
  val power = PrintedPower(PowerId("denizen.bandit-paymaster"),
    persistent = false, cost = Cost(favor = 1),
    text = "**ACTION:** If your site is ruled by bandits, gain 2 " +
      "warbands.")
  val powers: Vector[PrintedPower] = Vector(power)

/** Bandit Paymaster (card 219), ACTION: place 1 favor on this card, then,
  * if your site is ruled by bandits, gain 2 warbands.
  *
  * Bandits rule a site that holds a bandit warband. There the player gains
  * 2 warbands, or what their supply holds, and no bandit is removed.
  * Anywhere else nothing happens and one line says so. Nothing is asked.
  */
case object BanditPaymaster extends PaidAction(BanditPaymasterCard.power):
  val Gained: Int = 2
  private val notes = NoteSet(id)
  /** "{Red} gained {n warbands}." */
  val paid: NoteKey = notes.key(NoteKey.Used, Vector(NotePart.Arg(0),
    NotePart.Text(" gained "), NotePart.Arg(1), NotePart.Text(".")))
  /** "{site} is not ruled by bandits." */
  val unruled: NoteKey = notes.key("used.none", Vector(NotePart.Arg(0),
    NotePart.Text(" is not ruled by bandits.")))
  override def noteKeys: Vector[NoteKey] = notes.declared

  def build(catalog: ExecutableCatalog, ready: ReadyGame, player: PlayerId,
      source: DecisionOptionRef)
      : Either[OathViolation, Operation] = Right(Sequence(Vector[Operation](
    BuildOps((live, _) => pay(live, player)),
    Note(id, paidNote(_, player, source)))))

  private def banditsRule(ready: ReadyGame, site: SiteId): Boolean =
    ready.game.current.map.sites.get(site)
      .exists(_.forces.count(ForceKind.Bandit) > 0)

  private def pay(ready: ReadyGame, player: PlayerId)
      : Either[OathViolation, Vector[CoreOperation]] =
    if !ready.game.current.pawnSite(player).exists(banditsRule(ready, _))
    then Right(Vector.empty)
    else PlayerFacts.forceKind(ready, player).map(kind =>
      Vector[CoreOperation](Gain.Warbands(player, kind, Gained)))

  /** The warbands the player gained in the step before the note, or that
    * the site is not the bandits'. */
  private def paidNote(states: NoteStates, player: PlayerId,
      source: DecisionOptionRef): Option[LogNote] =
    PowerSourceRef.of(source).zip(states.now.game.current.pawnSite(player))
      .map((card, site) =>
        if banditsRule(states.now, site) then paid(card,
          NoteArg.Player(player), NoteArg.Amount(
            states.previous.fold(0)(NoteSupport.warbands(_, player)),
            NoteUnit.Warband))
        else unruled(card, NoteArg.Site(site)))
