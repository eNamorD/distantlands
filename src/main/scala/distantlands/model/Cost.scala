package distantlands.model

/** A typed payment, which [[PayCost]] settles: `favor`/`secret` are
  * placed at a destination card, `favorBurnt`/`secretBurnt` leave play to
  * the shared bank. All fields are non-negative; an all-zero cost is legal
  * and represents a free payment.
  */
final case class Cost(favor: Int = 0, secret: Int = 0, favorBurnt: Int = 0,
    secretBurnt: Int = 0):
  require(favor >= 0 && secret >= 0 && favorBurnt >= 0 && secretBurnt >= 0,
    "costs must be non-negative")

object Cost:
  val free: Cost = Cost(0, 0, 0, 0)
