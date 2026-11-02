package oathdigital.gameplay.powers.whenplayed

import oathdigital.catalog.ExecutableCatalog
import oathdigital.gameplay.powerresolver.ContributingPower

/** The When Played powers of catalog batches 3 and 4, which
  * `WalkerPowerCatalog` wires in. Each names its denizen card, so all are
  * always present. */
object WhenPlayedPowers:
  def forCatalog(catalog: ExecutableCatalog): Vector[ContributingPower] =
    Vector[ContributingPower](ThreateningRoar.forCatalog(catalog),
      Riots.forCatalog(catalog), AnimalHost.forCatalog(catalog), Charlatan,
      KeyToTheCity, BanditPrince, SaladDays, FabledFeast.forCatalog(catalog),
      TownMeeting.forCatalog(catalog), GreatHerd.forCatalog(catalog),
      RoyalTax, Revelation, Blackmail.forCatalog(catalog),
      Dissent.forCatalog(catalog), WizardsConclave.forCatalog(catalog),
      BanditChief, ARoundOfAle.forCatalog(catalog),
      CallForHelp.forCatalog(catalog), TrueOath.forCatalog(catalog))
