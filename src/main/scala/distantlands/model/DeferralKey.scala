package oathdigital.model

/** The name [[Defer]] holds ops under, until a later step of the same
  * action takes them (operation rewrite design, "Deferral"). Named after
  * the power that owns it. */
final case class DeferralKey(value: String)
