package distantlands.gameplay.roles

import distantlands.catalog.{AdviserOnly, Denizen, Locked, PrintedPower}
import distantlands.gameplay.PowerAccess
import distantlands.gameplay.powerresolver.{Contribution, ContributingPower}
import distantlands.model._

object CouncilSeatCard extends Denizen(DenizenId("123"), "Council Seat", Suit.Order) with Locked with AdviserOnly:
  val power = PrintedPower(PowerId("denizen.council-seat"),
    persistent = true, cost = Cost.free,
    text = "If you're a Citizen, you **cannot** be exiled, even by " +
      "yourself.")
  val powers: Vector[PrintedPower] = Vector(power)

/** Council Seat (card 123, adviser-only, locked): "If you're a Citizen, you
  * cannot be exiled, even by yourself." (catalog batch 10, slice 1).
  *
  * A rule with no contributions: `Citizenship.exile` builds nothing for a
  * Citizen it guards. Every exile path stays choosable and carries the
  * warning (ruling 8); its costs are still paid. It guards only exiles
  * during play: the Chronicle's Empire break makes a Citizen an Exile
  * without exiling them (ruling 10). It writes no line.
  */
case object CouncilSeat extends ContributingPower:
  val id: PowerId = CouncilSeatCard.power.id
  def source: RuleSourceRef = RuleSourceRef.GameRule(id.value)
  def contributions: Map[PowerWindow, Vector[Contribution]] = Map.empty
  val warning: String = "Council Seat: cannot be exiled."

  /** Whether `player` is a Citizen holding the Seat faceup. */
  def guards(ready: ReadyGame, player: PlayerId): Boolean =
    Imperials.role(ready, player).contains(Role.Citizen) &&
      PowerAccess.holdsFaceupAdviser(ready, player, CouncilSeatCard.id)

  /** `option`, an exile of `player`, warned while the Seat guards them. */
  def warned(ready: ReadyGame, player: PlayerId, option: DecisionOption)
      : DecisionOption =
    if guards(ready, player) then
      DecisionOption.Warned(option, OptionReason.Written(warning))
    else option
