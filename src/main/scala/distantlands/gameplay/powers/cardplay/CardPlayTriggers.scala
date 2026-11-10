package distantlands.gameplay.powers.cardplay

import distantlands.gameplay.powerresolver.ContributingPower

/** The powers that act when a card is played, registered together: the
  * triggers that reward a play and the rules that change or forbid one. Each
  * names its card, so all are always present.
  */
object CardPlayTriggers:
  val all: Vector[ContributingPower] =
    Vector[ContributingPower](WildCry, WelcomingParty,
      Gossip, BookBinders, SaddleMakers, VowOfObedience,
      SecretPolice, SacredGround, VowOfWisdom)
