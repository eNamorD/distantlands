package oathdigital.gameplay.powers.targeting

import oathdigital.gameplay.powerresolver.ContributingPower

/** The persistent rules that keep a player, or what they hold, from being
  * targeted by a Raid, a Challenge or a Conspiracy, campaigned against, or
  * taken, registered together. Each names its card, so all are always present.
  */
object TargetProtections:
  val all: Vector[ContributingPower] =
    Vector(CircletOfCommand, Diplomat, ForgottenVault, LostTongue,
      OakenFortress, RottingFortress, HallOfDebate, TomeGuardians)
