package distantlands.protocol.projection

/** Whom an askable option would ask, and about which powers, for the
  * client to word its confirmation (consent design, "Projection"). Player
  * ids: the client words their display names. `granterPlayerIds` and
  * `powers` are the required asks to ignore a power; `payers` the required
  * asks to pay; `alternatives` the asks any one of which will do (catalog
  * batch 8, slice 15). */
final case class ConsentAskProjection(granterPlayerIds: Vector[String],
    powers: Vector[String],
    alternatives: Vector[ConsentAlternativeProjection] = Vector.empty,
    payers: Vector[ConsentAlternativeProjection] = Vector.empty)

/** One ask on its own: its granter, its powers, and whether the granter
  * is asked to pay rather than to waive. */
final case class ConsentAlternativeProjection(granterPlayerId: String,
    powers: Vector[String], paying: Boolean)

/** The requester's own view of their waiting request: who has not yet
  * answered. `either` when any one of them will do. `use` is the card
  * whose automatic power the rulers decide on (catalog batch 8, item
  * 10b). */
final case class ConsentWaitingProjection(granterPlayerIds: Vector[String],
    either: Boolean = false, use: Option[String] = None)

/** An asked granter's prompt: who asks, about which of the granter's own
  * powers, for which option. `paying` when the granter is asked to pay
  * and travel there rather than to waive (catalog batch 8, slice 15).
  * `use` is the card whose automatic power the ruler decides on (catalog
  * batch 8, item 10b). `adjust` says what the granter may adjust later
  * from the power's overlay, which the prompt's notice words (reconsider
  * design, "Prompt notice"; implementation ruling R21): `answer`, this
  * answer and their preferences; `preferences`, their preferences alone,
  * for an automatic power; `none`, when some power of the ask has no
  * granter, as for a payer or an Imperial law. */
final case class ConsentPromptProjection(requesterPlayerId: String,
    powers: Vector[String], target: String, paying: Boolean = false,
    use: Option[String] = None, adjust: String = "none")
object ConsentPromptProjection:
  val adjustments: Set[String] = Set("answer", "preferences", "none")

/** A waivable power the viewer rules, with their setting for each other
  * player: `never`, `case-by-case` or `always`. Only ever the viewer's own
  * (ruling 16). `cardId` names the card that prints it, when a card does, so
  * the card can open the grid (consent slice 3), and `bannerKey` the banner
  * that prints it, so the banner can. `requests` is the turn's requests
  * about this power that the viewer answered, newest first (reconsider
  * design, "Projection"). */
final case class ConsentPreferencesProjection(powerId: String, power: String,
    rows: Vector[ConsentPreferenceRowProjection],
    cardId: Option[String] = None, bannerKey: Option[String] = None,
    requests: Vector[ConsentRequestRowProjection] = Vector.empty)
final case class ConsentPreferenceRowProjection(playerId: String,
    setting: String)

/** One request of this turn that the viewer answered (reconsider design,
  * "Projection"). `requesterPlayerId`, `decisionId`, `optionKind` and
  * `optionId` are what `ReconsiderConsent` sends back. `label` reads the
  * option as the prompt read it. `answer` is `allowed` or `refused`.
  * `alsoPowers` names the viewer's other powers the same ask covered.
  * `use` is the card whose automatic power the request was about.
  * `change` says whether the answer may be changed now: `open`, `used`
  * (the requester acted on it), `automatic`, `waiting` (a request about
  * the same option waits) or `lapsed` (the viewer lost a power the answer
  * covers). */
final case class ConsentRequestRowProjection(requesterPlayerId: String,
    decisionId: String, optionKind: String, optionId: String, label: String,
    answer: String, alsoPowers: Vector[String], use: Option[String],
    change: String)
