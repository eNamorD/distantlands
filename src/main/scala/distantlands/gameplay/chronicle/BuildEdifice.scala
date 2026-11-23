package distantlands.gameplay.chronicle

import distantlands.catalog.ExecutableCatalog
import distantlands.gameplay.actions.ProcedureNotes
import distantlands.gameplay.powers.PowerAnswers
import distantlands.model._

/** Build Edifice, the Throne's third step (CR p. 37, Chronicle rulings 9, 12
  * and 25). Each suit has one available edifice: the first of its suit in
  * the edifice deck after a shuffle, so a random one, or the first of its
  * suit set aside when the deck has none. The Throne holder may bury one
  * card at a site they rule, edifices included, to place the available
  * edifice of the buried card's suit there intact; at most one edifice is
  * built. The step ends by returning every set-aside edifice to the deck:
  * the whole deck is shuffled, so no order survives. A buried edifice goes
  * straight to the deck, which ends in the same place.
  *
  * The builder is read when the offer is reached
  * (`ChronicleLegacyRules.builder`): an Imperial holding Great Architect,
  * who buries a card of any suit and then picks which available edifice
  * replaces it, even beside Iron Hand; else the Iron Hand holder over their
  * own sites; else the Throne holder (Legacy Chronicle powers design,
  * rulings 10 and 19; R32, re-ruled by the owner 2026-11-14). A burial
  * returns favor to the buried card's own suit bank (R14). */
object BuildEdifice:
  /** The card to bury, or [[skip]]. */
  val cardDecisionId = "chronicle.build-edifice.card"

  /** Great Architect's pick of the available edifice to place. */
  val edificeDecisionId = "chronicle.build-edifice.edifice"

  val skip: DecisionOptionRef.Button = DecisionOptionRef.Button("build-nothing")

  private val noteId = ProcedureNotes.id(TriggeredProcedureRef.ChronicleThrone)
  private val notes = NoteSet(noteId)
  val built: NoteKey = notes.key("built", Vector(NotePart.Arg(0),
    NotePart.Text(" built "), NotePart.Arg(1), NotePart.Text(" at "),
    NotePart.Arg(2)))
  def noteKeys: Vector[NoteKey] = notes.declared

  /** The edifice each suit offers (ruling 9). */
  def available(catalog: ExecutableCatalog, current: CurrentGameState)
      : Vector[(Suit, EdificeId)] =
    val pool = current.commonCards.edificeDeck ++ current.setAsideEdifices
    Suit.all.flatMap(suit => pool.find(id =>
      catalog.edifice(id).exists(_.suit == suit)).map(suit -> _))

  def steps(catalog: ExecutableCatalog): Vector[Operation] = Vector(
    // The shuffle draws the random edifice per suit: the deck is otherwise
    // in catalog order.
    Shuffle(ShufflePile.EdificeDeck),
    // One Branch: a resume re-reads it against live state, and every answer
    // is replayed against the offer the step began with.
    Branch((state, _) => ChronicleLegacyRules.builder(state).fold(
      violation => Vector(ChronicleTasks.failing(violation)),
      holder => offer(catalog, state, holder,
        available(catalog, state.game.current)))),
    BuildOps((state, _) => Right(state.game.current.setAsideEdifices.map(id =>
      Move(Piece.Card(id), PositionedLocation(Location.SetAsideEdifices),
        PositionedLocation(Location.Deck(CardDeck.Edifice),
          StackPosition.Bottom)))), required = true))

  /** The cards at sites the holder rules, by site: those whose suit has an
    * available edifice, or every card for Great Architect's holder (ruling
    * 10). */
  private def candidates(catalog: ExecutableCatalog, state: ReadyGame,
      holder: PlayerId, offered: Vector[(Suit, EdificeId)])
      : Vector[(SiteId, SiteDenizenState)] =
    val map = state.game.current.map
    val anySuit = ChronicleLegacyRules.anyEdifice(state, holder)
    ChronicleTasks.ruled(state, holder).flatMap(site =>
      map.sites(site).denizens.filter(card => anySuit ||
        catalog.suitOf(card.id).exists(suit =>
          offered.exists(_._1 == suit))).map(site -> _))

  private def offer(catalog: ExecutableCatalog, state: ReadyGame,
      holder: PlayerId, offered: Vector[(Suit, EdificeId)])
      : Vector[Operation] =
    val cards = candidates(catalog, state, holder, offered)
    val anySuit = ChronicleLegacyRules.anyEdifice(state, holder)
    if cards.isEmpty then Vector.empty
    else
      def placed(pending: PendingTree)
          : Option[(SiteId, SiteDenizenState, EdificeId)] =
        chosen(pending, cards).flatMap((site, card) =>
          edificeFor(catalog, pending, card, offered, anySuit)
            .map((site, card, _)))
      Vector(
        Decide(cardDecisionId, holder, DecisionQuery.ChooseOne(
          cards.map((_, card) => ShapeEmpire.option(card)) :+
            DecisionOption.Button(skip, "Build nothing"),
          heading = Some(
            if anySuit then "Bury a card to build an edifice"
            else "Bury a card to build the available matching edifice"),
          confirmVerb = Some("Bury"))),
        // Great Architect picks the edifice once a card is chosen; with one
        // available edifice there is nothing to pick.
        Branch((_, pending) => Option.when(anySuit &&
            chosen(pending, cards).isDefined)(
          Decide(edificeDecisionId, holder, DecisionQuery.ChooseOne(
            offered.map((_, id) =>
              DecisionOption.Edifice(DecisionOptionRef.Edifice(id))),
            heading = Some("Choose the edifice to build in its place"),
            confirmVerb = Some("Build")), answersAlone = true)).toVector),
        BuildOps((_, pending) => Right(placed(pending).toVector
          .flatMap((site, card, edifice) => bury(site, card,
            catalog.suitOf(card.id), holder) :+ EdificeBuilt(edifice, site))),
          required = true),
        Branch((_, pending) => placed(pending).toVector.map(
          (site, _, edifice) => Note(noteId, _ => Some(built.sourceless(
            NoteArg.Player(holder), NoteArg.Card(edifice),
            NoteArg.Site(site)))))))

  /** The edifice the burial places: the available one of the card's suit,
    * or Great Architect's pick. */
  private def edificeFor(catalog: ExecutableCatalog, pending: PendingTree,
      card: SiteDenizenState, offered: Vector[(Suit, EdificeId)],
      anySuit: Boolean): Option[EdificeId] =
    if anySuit then PowerAnswers.one(pending, edificeDecisionId).collect:
      case DecisionOptionRef.Edifice(id) if offered.exists(_._2 == id) => id
    else catalog.suitOf(card.id).flatMap(suit =>
      offered.find(_._1 == suit).map(_._2))

  private def chosen(pending: PendingTree,
      cards: Vector[(SiteId, SiteDenizenState)])
      : Option[(SiteId, SiteDenizenState)] =
    PowerAnswers.one(pending, cardDecisionId).flatMap(ref =>
      cards.find((_, card) => ShapeEmpire.option(card).ref == ref))

  private def bury(site: SiteId, card: SiteDenizenState, suit: Option[Suit],
      holder: PlayerId): Vector[CoreOperation] =
    val buried = card match
      case DenizenState(id, _, _) => BuryableCard.Denizen(id)
      case EdificeState(id, _, _) => BuryableCard.Edifice(id)
    Bury.standard(buried, PositionedLocation(Location.Site(site)), suit,
      card.tokens.favor, card.tokens.secrets, holder)
