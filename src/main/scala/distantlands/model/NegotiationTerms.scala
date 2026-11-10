package distantlands.model

final case class NegotiationTransfer(
    recipient: PlayerId,
    favor: Int,
    relics: Vector[RelicId]
):
  require(favor >= 0, "Negotiation favor must be non-negative")
  require(relics.distinct.size == relics.size,
    "Negotiation relic transfers must be distinct")

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

  /** Why `promises` breaks a limit, or `None`. Length counts code points,
    * so an emoji is one character. */
  def problem(promises: Vector[String]): Option[String] =
    if promises.size > MaxPerKind then Some(s"more than $MaxPerKind promises")
    else if promises.distinct.size != promises.size then
      Some("the same promise twice")
    else promises.collectFirst {
      case text if text.isBlank => "a blank promise"
      case text if text.strip != text => "a promise with surrounding spaces"
      case text if text.exists(_.isControl) =>
        "a promise with a control character"
      case text if text.codePointCount(0, text.length) > MaxLength =>
        s"a promise over $MaxLength characters"
    }

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
    bindingPromises: Boolean = false
):
  require(maxFavor >= 0, "negotiation favor bound must be non-negative")
