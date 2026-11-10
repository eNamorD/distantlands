package distantlands.model

import distantlands.model._

final case class OathTransition(
    state: OathState,
    events: Vector[OathEvent]
)
