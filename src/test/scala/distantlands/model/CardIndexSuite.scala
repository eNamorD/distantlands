package distantlands.model

import distantlands.catalog.holding.{ArbiterCard, SeerCard}
import distantlands.gameplay.chronicle.legacies.RivalCard
import distantlands.testkit.Table
import distantlands.testkit.Table.p1
import distantlands.testkit.Unwrap.{accepted, rejected}

class CardIndexSuite extends munit.FunSuite:
  import TestGameFixtures._

  test("container membership derives a card's location"):
    val index = accepted(CardIndex.from(game))

    // The fixture's world deck holds only worldDenizen, so it is at index 0.
    assertEquals(
      index.locationOf(worldDenizen),
      Some(CardLocation(CardContainer.Deck(CardDeck.World), 0))
    )
    assertEquals(index.stateOf(worldDenizen), None)
    // The fixture puts siteDenizen alone on its first site, sites.head.
    assertEquals(
      index.locationOf(siteDenizen.id),
      Some(
        CardLocation(
          CardContainer.Site(sites.head, SiteCardArea.Denizens),
          0
        )
      )
    )
    // The fixture stores storedEdifice in the Atlas stored site S9, the
    // first Atlas entry.
    assertEquals(
      index.locationOf(storedEdifice.id),
      Some(
        CardLocation(
          CardContainer.AtlasSite(
            atlasPosition = 0,
            site = SiteId("S9"),
            area = SiteCardArea.Denizens
          ),
          0
        )
      )
    )

  test("a stored site's edifice is indexed in the Atlas, after its " +
      "denizens, with no state"):
    val edifice = EdificeId("E-stored")
    // Patched with copy: no Table is built on this fixture's game.
    val storing = game.copy(campaign = game.campaign.copy(atlas = AtlasState(
      game.campaign.atlas.entries.map {
        case stored: AtlasEntry.StoredSite =>
          stored.copy(edifice = Some(edifice))
        case other => other
      })))
    val index = accepted(CardIndex.from(storing))
    assertEquals(index.locationOf(edifice), Some(CardLocation(
      CardContainer.AtlasSite(0, SiteId("S9"), SiteCardArea.Denizens), 1)))
    assertEquals(index.stateOf(edifice), None)

  test("the derived index rejects a card present in two containers"):
    // Patched with copy, not a Table step: the test makes an illegal state
    // on purpose.
    val duplicatedSite = game.current.map.sites(sites(2)).copy(
      denizens = Vector(
        DenizenState(worldDenizen, Orientation.FaceUp, Tokens.empty)
      )
    )
    val duplicatedGame = game.copy(
      current = game.current.copy(
        map = game.current.map.copy(
          sites = game.current.map.sites.updated(sites(2), duplicatedSite)
        )
      )
    )

    val result = CardIndex.from(duplicatedGame)

    assert(
      rejected(result).exists {
        case CardIndexProblem.DuplicateCard(id, locations) =>
          id == worldDenizen && locations.size == 2
        case _ => false
      }
    )

  test("expected catalog cards can be checked without storing an instance ID"):
    val absent = RelicId("R-missing")
    val result = CardIndex.from(game, expectedCards = Set(absent))

    assertEquals(
      result,
      Left(Vector(CardIndexProblem.MissingCard(absent)))
    )

  test("temporary hands and set-aside relics are indexed as owning zones"):
    // Patched with copy, not a Table step: the test makes an illegal state
    // on purpose. The reliquary and the world deck are emptied so
    // reliquaryRelic and worldDenizen are not also in them.
    val prepared = game.copy(
      campaign = game.campaign.copy(reliquary = Vector.empty),
      current = game.current.copy(
        commonCards = game.current.commonCards.copy(worldDeck = Vector.empty),
        temporaryHands = Map(playerId -> Vector(worldDenizen)),
        setAsideRelics = Vector(reliquaryRelic)
      )
    )
    val index = accepted(CardIndex.from(prepared))

    assertEquals(index.locationOf(worldDenizen), Some(CardLocation(
      CardContainer.Player(playerId, PlayerCardArea.Hand), 0)))
    assertEquals(index.locationOf(reliquaryRelic), Some(CardLocation(
      CardContainer.SetAsideRelics, 0)))

  test("temporary hands remain indexed even when their owner is invalid"):
    // Patched with copy, not a Table step: the test makes an illegal state
    // on purpose. The world deck is emptied so worldDenizen is not also in it.
    val unknown = PlayerId("unknown")
    val prepared = game.copy(current = game.current.copy(
      commonCards = game.current.commonCards.copy(worldDeck = Vector.empty),
      temporaryHands = Map(unknown -> Vector(worldDenizen))))

    val index = accepted(CardIndex.from(prepared))
    assertEquals(index.locationOf(worldDenizen), Some(CardLocation(
      CardContainer.Player(unknown, PlayerCardArea.Hand), 0)))
    assert(DomainValidation.validate(prepared).contains(
      DomainProblem.UnknownTemporaryHandOwner(unknown)))

  test("a lineage's legacies and the public row are indexed, and no " +
      "legacy lies in the legacy deck"):
    val ready = Table.start.legacy(p1, SeerCard)
      .legacy(p1, ArbiterCard, active = false)
      .publicLegacies(RivalCard).ready
    val index = ready.game.cardIndex.toOption.get
    val lineage = ready.game.current.player(p1).get.lineage
    assertEquals(index.get(SeerCard.id).map(_.location.container),
      Some(CardContainer.Lineage(lineage, LineageCardArea.Legacies)))
    assertEquals(index.get(ArbiterCard.id).flatMap(_.state),
      Some(LegacyState(ArbiterCard.id, active = false)))
    assertEquals(index.get(RivalCard.id).map(_.location.container),
      Some(CardContainer.PublicLegacies))
    assert(index.ids.flatMap(index.get).forall(
      _.location.container != CardContainer.Deck(CardDeck.Legacy)))
