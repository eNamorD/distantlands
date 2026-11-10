package distantlands.gameplay.chronicle

import distantlands.gameplay.operations.TheGrandScepterCard
import distantlands.model._

/** The table as the Stars task stores it (CR pp. 40-41, Chronicle rulings
  * 17 and 18). The Stars steps change nothing on the table; this reads it
  * once into the Chronicle's table fields. The winner's sites become the
  * Empire's `world` and the rest are archived at the atlas's Recent end,
  * each with every card left on it: its edifice, then its denizens in slot
  * order, then its relics, the order the atlas already stores. A card in a
  * pile the Chronicle cannot store refuses the record. */
object ChronicleExtraction:
  /** The sites in play, split by ruler, each in `MapState.hinterward`
    * order: Cradlemost first. */
  final case class SiteLayout(empire: Vector[SiteId], rivals: Vector[SiteId])

  def layout(ready: ReadyGame): Either[OathViolation, SiteLayout] =
    ready.game.current.result.map(_.winner).toRight(OathViolation
      .InvalidEventOrder("the Chronicle stores a game with no winner")).map(
      winner =>
        val (empire, rivals) = ready.game.current.map.hinterward
          .partition(ChronicleTasks.rules(ready, winner, _))
        SiteLayout(empire, rivals))

  def storedSite(site: SiteId, items: Vector[CardId])
      : Either[OathViolation, StoredSite] =
    Either.cond(items.size <= StoredSite.capacity, StoredSite(site, items),
      unstorable(s"${site.value} holds ${items.size} cards; a stored site " +
        s"holds at most ${StoredSite.capacity}"))

  /** `record` with its sites, decks, Dispossessed and Reliquary read from
    * the table. */
  def extract(ready: ReadyGame, record: Chronicle)
      : Either[OathViolation, Chronicle] =
    val current = ready.game.current
    val campaign = ready.game.campaign
    def atSite(site: SiteId) = storedSite(site, current.map.sites.get(site)
      .toVector.flatMap(state =>
        val (edifices, denizens) = state.denizens.map(_.id)
          .partition(_.isInstanceOf[EdificeId])
        edifices ++ denizens ++ state.relics.map(_.id)))
    for
      _ <- leftovers(ready)
      sites <- layout(ready)
      empire <- all(sites.empire.map(atSite))
      archived <- all(sites.rivals.map(atSite))
      carried <- all(campaign.atlas.entries.collect {
        case AtlasEntry.StoredSite(id, denizens, relics, edifice) =>
          storedSite(id,
            edifice.toVector ++ denizens.map(_.id) ++ relics.map(_.id))
      })
    yield record.copy(atlasBox = archived ++ carried, world = empire,
      worldDeck = denizens(current.commonCards.worldDeck),
      relicDeck = current.commonCards.relicDeck,
      dispossessed = denizens(campaign.dispossessed ++ current.newlyDispossessed),
      reliquary = campaign.reliquary)

  /** `leftovers` has already refused any Vision, so every world card left
    * is a denizen. */
  private def denizens(cards: Vector[WorldCardId]): Vector[DenizenId] =
    cards.collect { case id: DenizenId => id }

  private def leftovers(ready: ReadyGame): Either[OathViolation, Unit] =
    ready.game.cardIndex.left.map(problems =>
      unstorable(problems.mkString(", "))).flatMap(index =>
      val stray = index.ids.toVector.flatMap(index.get)
        .filterNot(storable).sortBy(_.id.value)
      Either.cond(stray.isEmpty, (), unstorable(
        "the Chronicle cannot store " + stray.map(card =>
          s"${card.id.value} (${card.location.container})").mkString(", "))))

  private def storable(card: LocatedCard): Boolean =
    card.id match
      case _: VisionId => false
      case _ => card.location.container match
        case CardContainer.Deck(_) | CardContainer.Site(_, _) |
            CardContainer.AtlasSite(_, _, _) | CardContainer.Lineage(_, _) |
            CardContainer.Reliquary | CardContainer.Dispossessed |
            CardContainer.NewlyDispossessed | CardContainer.SuitedReserve(_) =>
          true
        case CardContainer.Player(_, PlayerCardArea.Relics) =>
          card.id == TheGrandScepterCard.id
        case _ => false

  private def all[A](results: Vector[Either[OathViolation, A]])
      : Either[OathViolation, Vector[A]] =
    results.foldLeft[Either[OathViolation, Vector[A]]](Right(Vector.empty))(
      (done, next) => done.flatMap(values => next.map(values :+ _)))

  private def unstorable(reason: String): OathViolation =
    OathViolation.UnsupportedChronicle(reason)
