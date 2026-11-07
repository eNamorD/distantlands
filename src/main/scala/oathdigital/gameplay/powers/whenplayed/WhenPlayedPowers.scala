package oathdigital.gameplay.powers.whenplayed

import oathdigital.gameplay.powerresolver.ContributingPower

/** The When Played powers of catalog batches 3 and 4, which
  * `WalkerPowerCatalog` wires in. Each names its denizen card, so all are
  * always present. */
object WhenPlayedPowers:
  val all: Vector[ContributingPower] =
    Vector[ContributingPower](ThreateningRoar,
      Riots, AnimalHost, Charlatan,
      KeyToTheCity, BanditPrince, SaladDays, FabledFeast,
      TownMeeting, GreatHerd,
      RoyalTax, Revelation, Blackmail,
      Dissent, WizardsConclave,
      BanditChief, ARoundOfAle,
      CallForHelp, TrueOath,
      LongLostHeir, Bewitch, AncientPact, RoyalAmbitions,
      Pilgrimage, AutumnWind)
