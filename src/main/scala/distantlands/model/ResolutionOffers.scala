package distantlands.model

/** One power's steps, offered to the node that orders a window's powers
  * (catalog batch 13, ruling 4): the option that names the power in the
  * placer's question, by its card, and the steps that resolve it.
  *
  * The offer is rebuilt at every fold. It must stand while its own steps
  * run, since a walk resumed inside them finds them among the offers
  * again. */
final case class ResolutionOffer(option: DecisionOption,
    steps: Vector[Operation])

/** A resolution offer with the power that made it. */
final case class OfferedResolution(power: PowerId, offer: ResolutionOffer)
