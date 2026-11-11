package distantlands.model

final case class NegotiationTransfer(
    recipient: PlayerId,
    favor: Int,
    relics: Vector[RelicId],
    advisers: Vector[WorldCardId] = Vector.empty,
    sites: Vector[SiteId] = Vector.empty,
    /** Faceup secrets from the author's board (catalog batch 12, rulings 6
      * and 31). Only a deal whose rules open them takes any. */
    secrets: Int = 0
):
  require(favor >= 0, "Negotiation favor must be non-negative")
  require(secrets >= 0, "Negotiation secrets must be non-negative")
  require(relics.distinct.size == relics.size,
    "Negotiation relic transfers must be distinct")
  require(advisers.distinct.size == advisers.size,
    "a transfer row names one adviser twice")
  require(sites.distinct.size == sites.size,
    "a transfer row names one site twice")

sealed trait NegotiationDisclosureRef extends Product with Serializable
object NegotiationDisclosureRef:
  final case class Adviser(owner: PlayerId, card: WorldCardId)
      extends NegotiationDisclosureRef
  final case class HeldRelic(owner: PlayerId, relic: RelicId)
      extends NegotiationDisclosureRef
  final case class SiteRelic(site: SiteId, relic: RelicId)
      extends NegotiationDisclosureRef
final case class NegotiationDisclosure(
    recipient: PlayerId,
    information: NegotiationDisclosureRef
)
/** The limits on the text an author promises (catalog batch 12, ruling
  * 10): at most `MaxPerKind` of each kind, each at most `MaxLength`
  * characters, none blank, padded, multi-line or repeated. The engine
  * enforces no promise (ruling 12). */
object NegotiationPromise:
  val MaxLength: Int = 160
  val MaxPerKind: Int = 5

  /** Why `promises` breaks a limit, or `None`: a promise's own fault
    * first, then the count, then a repeat. Length counts code points, so an
    * emoji is one character. A line or paragraph separator, or an invisible
    * format character other than the zero-width joiner that joined emoji
    * use, counts as a control character. */
  def problem(promises: Vector[String]): Option[String] =
    promises.collectFirst {
      case text if text.isBlank => "a blank promise"
      case text if text.strip != text => "a promise with surrounding spaces"
      case text if text.codePoints.anyMatch(controlLike) =>
        "a promise with a control character"
      case text if text.codePointCount(0, text.length) > MaxLength =>
        s"a promise over $MaxLength characters"
    }.orElse {
      if promises.size > MaxPerKind then Some(s"more than $MaxPerKind promises")
      else Option.when(promises.distinct.size != promises.size)(
        "the same promise twice")
    }

  private def controlLike(codePoint: Int): Boolean =
    codePoint != ZeroWidthJoiner && (Character.getType(codePoint) match
      case Character.CONTROL | Character.LINE_SEPARATOR |
          Character.PARAGRAPH_SEPARATOR | Character.FORMAT => true
      case _ => false)
  private val ZeroWidthJoiner = 0x200D

/** One author's terms. `citizenship` names the Exiles the author, the Grand
  * Scepter's holder, offers Citizenship (CR p. 34). `promises` and
  * `bindingPromises` are what the author writes they will do; the Game Log
  * records them and the engine enforces none (catalog batch 12, rulings 10
  * to 12). */
final case class NegotiationTerms(
    transfers: Vector[NegotiationTransfer] = Vector.empty,
    disclosures: Vector[NegotiationDisclosure] = Vector.empty,
    citizenship: Vector[PlayerId] = Vector.empty,
    promises: Vector[String] = Vector.empty,
    bindingPromises: Vector[String] = Vector.empty
):
  require(transfers.map(_.recipient).distinct.size == transfers.size,
    "Negotiation transfers must have one row per recipient")
  require(disclosures.distinct.size == disclosures.size,
    "Negotiation disclosures must be distinct")
  require(citizenship.distinct.size == citizenship.size,
    "Negotiation Citizenship offers must be distinct")
  require(NegotiationPromise.problem(promises).isEmpty,
    s"Negotiation promises hold ${NegotiationPromise.problem(promises)
      .getOrElse("")}")
  require(NegotiationPromise.problem(bindingPromises).isEmpty,
    s"Negotiation binding promises hold ${NegotiationPromise
      .problem(bindingPromises).getOrElse("")}")

/** What one author may still put into a deal, computed from live state each
  * time the deal is asked: the other participants they may offer to, the
  * favor they hold, the relics they hold, and the information they may
  * currently promise to disclose (their facedown advisers and held relics,
  * and site relics they know that are still at their site), and the Exiles
  * they would offer Citizenship. `citizenshipRefused` is why a Scepter
  * holder may offer it to none of them (the first-game option).
  * `bindingPromises` is whether the author may add binding promises: false
  * until The Tribunal opens them, catalog batch 12 slice 2.
  */
final case class NegotiationBounds(
    recipients: Vector[PlayerId],
    maxFavor: Int,
    relics: Vector[RelicId],
    disclosures: Vector[NegotiationDisclosureRef],
    citizenship: Vector[PlayerId] = Vector.empty,
    citizenshipRefused: Option[String] = None,
    bindingPromises: Boolean = false,
    /** The advisers the author may give (no printed restriction refuses
      * them), offered only to `adviserRecipients` (catalog batch 12,
      * rulings 14 to 16). */
    advisers: Vector[WorldCardId] = Vector.empty,
    adviserRecipients: Vector[PlayerId] = Vector.empty,
    /** The sites the author may give (their own-colour warbands hold
      * them), offered only to `siteRecipients` (catalog batch 12, rulings
      * 18, 19 and 28). */
    sites: Vector[SiteId] = Vector.empty,
    siteRecipients: Vector[PlayerId] = Vector.empty,
    /** The faceup secrets the author may give, offered only to
      * `secretRecipients` (catalog batch 12, rulings 6 and 31). */
    maxSecrets: Int = 0,
    secretRecipients: Vector[PlayerId] = Vector.empty
):
  require(maxFavor >= 0, "negotiation favor bound must be non-negative")
  require(maxSecrets >= 0, "negotiation secret bound must be non-negative")
