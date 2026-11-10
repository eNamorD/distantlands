package distantlands.gameplay.powers.campaign

import distantlands.gameplay.powerresolver.ContributingPower

/** The Campaign battle plans the first batch's engine ported, registered
  * together: the title's defense, Outriders, Brass Army and Watchdog. All are
  * always present: each card plan names its card, and no card prints the
  * title's defense.
  */
object BattlePlans:
  val all: Vector[ContributingPower] =
    Vector(TitleDefensePlan.plan, Outriders, BrassArmy, Watchdog)
