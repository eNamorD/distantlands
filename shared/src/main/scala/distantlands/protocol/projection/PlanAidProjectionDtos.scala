package oathdigital.protocol.projection

/** An Imperial ally's view of another Imperial player's open plan window
  * (Empire phase design, "Offer panel"): what they may offer, drawn as
  * board options; what their saved offer holds; what of it was used; and
  * what they hold. Refs are `kind:id`, as `BoardOptionViews.key` spells
  * them. */
final case class PlanAidPromptProjection(userPlayerId: String, side: String,
    plans: Vector[DecisionOptionProjection],
    requirements: Vector[DecisionOptionProjection],
    saved: PlanAidOfferProjection, used: PlanAidOfferProjection,
    favorHeld: Int, secretsHeld: Int)

/** An offer, or its used part: plan and requirement refs as `kind:id`, and
  * the four amounts. */
final case class PlanAidOfferProjection(plans: Vector[String] = Vector.empty,
    requirements: Vector[String] = Vector.empty, favor: Int = 0,
    secrets: Int = 0, favorBurnt: Int = 0, secretsBurnt: Int = 0)
