package distantlands.model

// The reference half of the walker decision contract: the persisted
// identity of one selectable thing. It lives beside `Decisions.scala`,
// which holds the options, queries and answers built on it; the two
// were one file until it reached the 800-line limit.

/** Stable identity of one selectable thing in a decision.
  *
  * This is the half that persists: an answer records references, never
  * options, so a relabelled prompt leaves every recorded answer valid. A
  * reference carries identity and nothing else — a card reference names the
  * card globally and deliberately omits the site or player it is currently
  * at, because location is a legality fact used while BUILDING a query, not
  * part of a card's identity.
  */
sealed trait DecisionOptionRef extends Product with Serializable:
  /** Which of the eighteen variants this is, as a stable wire string. */
  def kind: String

  /** The variant's identity as a stable wire string, paired with [[kind]].
    *
    * A reference travels off-process three ways — in a submitted answer, in
    * a journalled answer, and (from Task 4) in a projected option — and all
    * three spell it as this pair. The spelling lives here, next to the cases
    * it names, so those three code paths cannot drift into three
    * independently written tables of the same eight strings; that drift is
    * the failure mode this whole vocabulary exists to remove.
    */
  def wireId: String
object DecisionOptionRef:
  /** A choice with no game object behind it, keyed by a stable string
    * (e.g. `"continue"`, `"stop"`).
    */
  final case class Button(key: String) extends DecisionOptionRef:
    val kind: String = "button"
    def wireId: String = key
  final case class Player(id: PlayerId) extends DecisionOptionRef:
    val kind: String = "player"
    def wireId: String = id.value
  final case class Site(id: SiteId) extends DecisionOptionRef:
    val kind: String = "site"
    def wireId: String = id.value
  final case class Denizen(id: DenizenId) extends DecisionOptionRef:
    val kind: String = "denizen"
    def wireId: String = id.value
  final case class Relic(id: RelicId) extends DecisionOptionRef:
    val kind: String = "relic"
    def wireId: String = id.value
  final case class Vision(id: VisionId) extends DecisionOptionRef:
    val kind: String = "vision"
    def wireId: String = id.value
  final case class Edifice(id: EdificeId) extends DecisionOptionRef:
    val kind: String = "edifice"
    def wireId: String = id.value
  final case class Legacy(id: LegacyId) extends DecisionOptionRef:
    val kind: String = "legacy"
    def wireId: String = id.value
  /** One relic of another player, named by its position in their relic
    * vector rather than by the card, so a facedown relic's identity is never
    * disclosed by an option. The owner may contain a colon; the slot is
    * always the text after the last one.
    */
  final case class RelicSlot(owner: PlayerId, slot: Int)
      extends DecisionOptionRef:
    require(slot >= 0, "a relic slot must be non-negative")
    val kind: String = "relic-slot"
    def wireId: String = s"${owner.value}:$slot"
  /** One adviser slot of a player, named by its position in their advisers
    * rather than by the card, so a facedown adviser's identity is never
    * disclosed by an option. A slot below the printed capacity of three may
    * be empty; the powers that name a held adviser name an occupied one. The
    * owner may contain a colon; the slot is always the text after the last
    * one.
    */
  final case class AdviserSlot(owner: PlayerId, slot: Int)
      extends DecisionOptionRef:
    require(slot >= 0, "an adviser slot must be non-negative")
    val kind: String = "adviser-slot"
    def wireId: String = s"${owner.value}:$slot"
  /** A banner. It names no holder: who holds it is read from live state, so
    * an answer cannot name a holder that has since changed.
    */
  final case class Banner(banner: distantlands.model.Banner)
      extends DecisionOptionRef:
    val kind: String = "banner"
    def wireId: String = banner.key
  final case class Deck(id: CardDeck) extends DecisionOptionRef:
    val kind: String = "deck"
    def wireId: String = id.key
  /** A suit's favor bank: the one shared place favor of that suit returns
    * to. A suit is a closed six-case enum, so this can never be absent.
    */
  final case class FavorBank(suit: Suit) extends DecisionOptionRef:
    val kind: String = "favor-bank"
    def wireId: String = suit.key

  /** Slot `index` of `site`'s card row, below the site's capacity: the empty
    * slot a card is played to, or the card a play there replaces. The site
    * may contain a colon; the index is always the text after the last one.
    */
  final case class SiteSlot(site: SiteId, index: Int) extends DecisionOptionRef:
    require(index >= 0, "a site slot must be non-negative")
    val kind: String = "site-slot"
    def wireId: String = s"${site.value}:$index"
  /** Relic `index` of `site`'s relics, in their order there: a relic named
    * by its place rather than by the card, so a facedown relic's identity
    * is never disclosed by an option. The site may contain a colon; the
    * index is always the text after the last one.
    */
  final case class SiteRelicSlot(site: SiteId, index: Int)
      extends DecisionOptionRef:
    require(index >= 0, "a site relic slot must be non-negative")
    val kind: String = "site-relic-slot"
    def wireId: String = s"${site.value}:$index"
  /** Playing the card to `owner`'s adviser slot `index`, faceup or facedown.
    * A choose-one answer is one ref, so the ref carries the facing, and an
    * adviser slot is offered as two options. The owner may contain a colon;
    * the facing follows the last one and the index the one before it.
    */
  final case class AdviserPlay(owner: PlayerId, index: Int, faceup: Boolean)
      extends DecisionOptionRef:
    require(index >= 0, "an adviser slot must be non-negative")
    val kind: String = "adviser-play"
    def wireId: String =
      s"${owner.value}:$index:${if faceup then "faceup" else "facedown"}"
  /** Playing a Vision faceup as `owner`'s revealed Vision. A faceup
    * Conspiracy takes no slot, so it never names this. */
  final case class RevealedVision(owner: PlayerId) extends DecisionOptionRef:
    val kind: String = "revealed-vision"
    def wireId: String = owner.value
  /** Discarding the played card to `region`'s discard pile. */
  final case class DiscardPile(region: Region) extends DecisionOptionRef:
    val kind: String = "discard-pile"
    def wireId: String = region.key

  /** The option that names `card` by itself: a denizen, relic, edifice or
    * legacy. A vision has none, so a consent keyed by a card cannot name
    * one. */
  def ofCard(card: CardId): Option[DecisionOptionRef] = card match
    case id: DenizenId => Some(Denizen(id))
    case id: RelicId => Some(Relic(id))
    case id: EdificeId => Some(Edifice(id))
    case id: LegacyId => Some(Legacy(id))
    case _ => None

  /** The card `ref` names by itself: the inverse of `ofCard`. */
  def cardOf(ref: DecisionOptionRef): Option[CardId] = ref match
    case Denizen(id) => Some(id)
    case Relic(id) => Some(id)
    case Edifice(id) => Some(id)
    case Legacy(id) => Some(id)
    case _ => None

  /** The denizen or edifice `ref` names: the card an economy action can
    * draw on, which a relic never is. */
  def denizenOrEdificeOf(ref: DecisionOptionRef): Option[CardId] = ref match
    case Denizen(id) => Some(id)
    case Edifice(id) => Some(id)
    case _ => None

  /** Safe parse of the [[DecisionOptionRef.kind]]/[[DecisionOptionRef.wireId]]
    * pair from untrusted input: `None` for an unknown kind or an id that
    * variant cannot carry, never a thrown `require`.
    *
    * Total over the eighteen variants, and the exact inverse of the two
    * accessors above — a new variant that forgets this method fails to
    * compile, because the match below is exhaustive over nothing and the
    * accessors are abstract.
    */
  def fromWire(kind: String, wireId: String): Option[DecisionOptionRef] =
    if wireId.trim.isEmpty then None
    else kind match
      case "button" => Some(Button(wireId))
      case "player" => Some(Player(PlayerId(wireId)))
      case "site" => Some(Site(SiteId(wireId)))
      case "denizen" => Some(Denizen(DenizenId(wireId)))
      case "relic" => Some(Relic(RelicId(wireId)))
      case "vision" => Some(Vision(VisionId(wireId)))
      case "edifice" => Some(Edifice(EdificeId(wireId)))
      case "legacy" => Some(Legacy(LegacyId(wireId)))
      case "relic-slot" => slotOf(wireId).map((owner, slot) =>
        RelicSlot(owner, slot))
      case "adviser-slot" => slotOf(wireId).map((owner, slot) =>
        AdviserSlot(owner, slot))
      case "banner" => distantlands.model.Banner.fromKey(wireId).map(Banner(_))
      case "deck" => CardDeck.fromKey(wireId).map(Deck(_))
      case "favor-bank" => Suit.fromKey(wireId).map(FavorBank(_))
      case "site-slot" => indexOf(wireId).map((site, index) =>
        SiteSlot(SiteId(site), index))
      case "site-relic-slot" => indexOf(wireId).map((site, index) =>
        SiteRelicSlot(SiteId(site), index))
      case "adviser-play" => facingOf(wireId).flatMap((slot, faceup) =>
        slotOf(slot).map((owner, index) => AdviserPlay(owner, index, faceup)))
      case "revealed-vision" => Some(RevealedVision(PlayerId(wireId)))
      case "discard-pile" => Region.all.find(_.key == wireId).map(DiscardPile(_))
      case _ => None

  /** The owner and non-negative slot of a slot reference's wire id. */
  private def slotOf(wireId: String): Option[(PlayerId, Int)] =
    indexOf(wireId).map((owner, slot) => PlayerId(owner) -> slot)

  /** The text before the last colon, which may hold colons itself, and the
    * non-negative index after it. */
  private def indexOf(wireId: String): Option[(String, Int)] =
    val at = wireId.lastIndexOf(':')
    Option.when(at > 0)(wireId.take(at)).filter(_.trim.nonEmpty)
      .flatMap(owner => wireId.drop(at + 1).toIntOption
        .filter(_ >= 0).map(owner -> _))

  /** An adviser play's slot part and facing: the facing follows the last
    * colon. */
  private def facingOf(wireId: String): Option[(String, Boolean)] =
    val at = wireId.lastIndexOf(':')
    Option.when(at > 0)(wireId.take(at)).flatMap(slot =>
      wireId.drop(at + 1) match
        case "faceup" => Some(slot -> true)
        case "facedown" => Some(slot -> false)
        case _ => None)
