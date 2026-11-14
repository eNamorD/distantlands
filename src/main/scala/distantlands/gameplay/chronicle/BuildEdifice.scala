package distantlands.gameplay.chronicle

import distantlands.catalog.ExecutableCatalog
import distantlands.gameplay.actions.ProcedureNotes
import distantlands.gameplay.powers.PowerAnswers
import distantlands.model._

/** Build Edifice, the Throne's third step (CR p. 37, Chronicle rulings 9 and
  * 12). Each suit offers one edifice: the first of its suit in the edifice
  * deck after a shuffle, so a random one, or the first of its suit set aside
  * when the deck has none. For each, the Throne holder may bury a card of
  * that suit at a site they rule, edifices included, to place the edifice
  * there intact. The step ends by returning every set-aside edifice to the
  * deck: the whole deck is shuffled, so no order survives. A buried edifice
  * goes straight to the deck, which ends in the same place.
  *
  * The builders are read when each suit's offer is reached
  * (`ChronicleLegacyRules.builders`): an Imperial holding Great Architect,
  * who may bury a card of any suit for each available edifice, then the
  * Iron Hand holder over their own sites; either alone; else the Throne
  * holder (Legacy Chronicle powers design, rulings 10 and 19; R32). A
  * second builder is offered a suit's edifice only if the first built
  * none, read from the first's journaled answer (R43), under an id of its
  * own. A burial returns favor to the buried card's own
  * suit bank (R14). */
object BuildEdifice:
  def decisionId(suit: Suit): String = s"chronicle.build-edifice.${suit.key}"
  /** The Iron Hand holder's question when a Great Architect builds first
    * (R32). */
  def ironHandDecisionId(suit: Suit): String = decisionId(suit) + ".iron-hand"

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
    Shuffle(ShufflePile.EdificeDeck)) ++
    // Two Branches per suit, one per builder slot: a resume re-reads a Branch
    // against live state, and building one suit's last edifice must not
    // change the node count the other answers are replayed against.
    // The second slot skips a suit the first built: it reads the first's
    // journaled answer, never a recomputed pool, so a resumed walk finds
    // the same question (Legacy Chronicle powers R43).
    Suit.all.flatMap(suit => Vector(0, 1).map(slot => Branch((state, pending) =>
      ChronicleLegacyRules.builders(state).fold(
        violation => Vector(ChronicleTasks.failing(violation)),
        builders => builders.lift(slot)
          .filterNot(_ => slot == 1 && builtBy(pending, decisionId(suit)))
          .toVector.flatMap(holder =>
            available(catalog, state.game.current).find(_._1 == suit).toVector
              .flatMap((_, edifice) => offer(catalog, state, holder, suit,
                edifice, if slot == 0 then decisionId(suit)
                  else ironHandDecisionId(suit)))))))) :+
    BuildOps((state, _) => Right(state.game.current.setAsideEdifices.map(id =>
      Move(Piece.Card(id), PositionedLocation(Location.SetAsideEdifices),
        PositionedLocation(Location.Deck(CardDeck.Edifice),
          StackPosition.Bottom)))), required = true)

  /** The cards of `suit` at sites the holder rules, by site; of any suit
    * for Great Architect's holder (ruling 10). */
  private def candidates(catalog: ExecutableCatalog, state: ReadyGame,
      holder: PlayerId, suit: Suit): Vector[(SiteId, SiteDenizenState)] =
    val map = state.game.current.map
    val anySuit = ChronicleLegacyRules.anyEdifice(state, holder)
    map.inPlay.filter(ChronicleTasks.rules(state, holder, _)).flatMap(site =>
      map.sites(site).denizens.filter(card =>
        anySuit || catalog.suitOf(card.id).contains(suit)).map(site -> _))

  private def offer(catalog: ExecutableCatalog, state: ReadyGame,
      holder: PlayerId, suit: Suit, edifice: EdificeId, id: String)
      : Vector[Operation] =
    val cards = candidates(catalog, state, holder, suit)
    if cards.isEmpty then Vector.empty
    else Vector(
      Decide(id, holder, DecisionQuery.ChooseMany(0, 1, cards.map {
        case (_, card: DenizenState) =>
          DecisionOption.Denizen(DecisionOptionRef.Denizen(card.id))
        case (_, card: EdificeState) =>
          DecisionOption.Edifice(DecisionOptionRef.Edifice(card.id))
      }, heading = Some(s"Bury a card to build ${name(catalog, edifice)}"),
        confirm = Some(CountedConfirm("Bury", "card", "cards",
          Some("Build nothing"))))),
      BuildOps((_, pending) => Right(chosen(pending, id, cards).toVector
        .flatMap((site, card) => bury(site, card, catalog.suitOf(card.id),
          holder) :+
          EdificeBuilt(edifice, site))), required = true),
      Branch((_, pending) => chosen(pending, id, cards).toVector.map(
        (site, _) => Note(noteId, _ => Some(built.sourceless(
          NoteArg.Player(holder), NoteArg.Card(edifice), NoteArg.Site(site)))))))

  /** Whether the question `id` was answered with a card buried. */
  private def builtBy(pending: PendingTree, id: String): Boolean =
    PowerAnswers.many(pending, id).exists(_.nonEmpty)

  private def name(catalog: ExecutableCatalog, edifice: EdificeId): String =
    catalog.edifice(edifice).fold(edifice.value)(_.intact.name)

  private def chosen(pending: PendingTree, id: String,
      cards: Vector[(SiteId, SiteDenizenState)])
      : Option[(SiteId, SiteDenizenState)] =
    PowerAnswers.many(pending, id).flatMap(_.headOption).flatMap(ref =>
      cards.find {
        case (_, card: DenizenState) => ref == DecisionOptionRef.Denizen(card.id)
        case (_, card: EdificeState) => ref == DecisionOptionRef.Edifice(card.id)
      })

  private def bury(site: SiteId, card: SiteDenizenState, suit: Option[Suit],
      holder: PlayerId): Vector[CoreOperation] =
    val buried = card match
      case DenizenState(id, _, _) => BuryableCard.Denizen(id)
      case EdificeState(id, _, _) => BuryableCard.Edifice(id)
    Bury.standard(buried, PositionedLocation(Location.Site(site)), suit,
      card.tokens.favor, card.tokens.secrets, holder)
