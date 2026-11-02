package oathdigital.gameplay.powers.cardplay

import oathdigital.catalog.ExecutableCatalog
import oathdigital.gameplay.powerresolver.ContributingPower

/** The powers that act when a card is played, registered together: the
  * triggers that reward a play and the rules that change or forbid one. Each
  * names its card, so all are always present.
  */
object CardPlayTriggers:
  def forCatalog(catalog: ExecutableCatalog): Vector[ContributingPower] =
    Vector[ContributingPower](WildCry.forCatalog(catalog), WelcomingParty,
      Gossip, BookBinders, SaddleMakers.forCatalog(catalog), VowOfObedience,
      SecretPolice, SacredGround, VowOfWisdom)
