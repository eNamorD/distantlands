package oathdigital.gameplay.setup

import oathdigital.gameplay.actions.VisionRules
import oathdigital.gameplay.operations.TheGrandScepterCard
import oathdigital.model._
import oathdigital.testkit.CatalogNames

/** The card accounting a Chronicle passes before game start reads it, and
  * before the Chronicle Phase journals it (Chronicle steps design, "Exit
  * and extraction"). */
class ChronicleAccountingSuite extends munit.FunSuite:
  private val catalog = FirstGameSetupFixture.catalog
  private val chronicle = FirstGameSetupFixture.chronicle

  private def lineageAdvising(card: DenizenId): LineageState =
    LineageState(LineageId("lineage:any"), None, Role.Exile, Vector.empty,
      Vector(DenizenState(card, Orientation.FaceDown, Tokens.empty)))

  test("the first-game fixture's Chronicle passes"):
    assertEquals(ChronicleAccounting.knownAndUnique(catalog, chronicle),
      Right(()))

  test("an unknown site in the world is refused"):
    val unknown = chronicle.copy(world = Vector(StoredSite(SiteId("no-site"))))
    assertEquals(ChronicleAccounting.knownAndUnique(catalog, unknown),
      Left("unknown atlas site no-site"))

  test("a site both stored and in the world is refused"):
    val twice = chronicle.copy(world = chronicle.atlasBox.take(1))
    assertEquals(ChronicleAccounting.knownAndUnique(catalog, twice),
      Left(s"duplicate atlas site ${chronicle.atlasBox.head.site.value}"))

  test("a denizen both Dispossessed and a starting adviser is refused"):
    val card = chronicle.worldDeck.head
    val twice = chronicle.copy(worldDeck = chronicle.worldDeck.tail,
      dispossessed = Vector(card), lineages = Vector(lineageAdvising(card)))
    assertEquals(ChronicleAccounting.knownAndUnique(catalog, twice),
      Left(s"denizen ${card.value} is stored twice"))

  test("a relic both in the Reliquary and the relic deck is refused"):
    val relic = chronicle.relicDeck.head
    val twice = chronicle.copy(reliquary = Vector(relic))
    assertEquals(ChronicleAccounting.knownAndUnique(catalog, twice),
      Left(s"relic ${relic.value} is stored twice"))

  test("a Vision stored at a site is refused"):
    val vision = VisionRules.Conquest
    val stored = chronicle.copy(world = Vector(StoredSite(
      CatalogNames.site("Ancient City"), Vector(vision))))
      .copy(atlasBox = chronicle.atlasBox.filterNot(
        _.site == CatalogNames.site("Ancient City")))
    assertEquals(ChronicleAccounting.knownAndUnique(catalog, stored),
      Left(s"${vision.value} is a vision, which the Chronicle does not store"))

  test("the Grand Scepter is never stored"):
    val stored = chronicle.copy(reliquary = Vector(TheGrandScepterCard.id))
    assertEquals(ChronicleAccounting.knownAndUnique(catalog, stored),
      Left("the Grand Scepter is not stored"))

  test("a record that keeps every baseline site and relic conserves it"):
    val moved = chronicle.copy(world = chronicle.atlasBox.take(2),
      atlasBox = chronicle.atlasBox.drop(2),
      relicDeck = chronicle.relicDeck.tail,
      reliquary = chronicle.relicDeck.take(1))
    assertEquals(ChronicleAccounting.conserves(chronicle, moved), Right(()))

  test("a record that loses a site or a relic does not conserve it"):
    assertEquals(ChronicleAccounting.conserves(chronicle,
      chronicle.copy(atlasBox = chronicle.atlasBox.tail)),
      Left(s"site ${chronicle.atlasBox.head.site.value} is missing"))
    assertEquals(ChronicleAccounting.conserves(chronicle,
      chronicle.copy(relicDeck = chronicle.relicDeck.tail)),
      Left(s"relic ${chronicle.relicDeck.head.value} is missing"))

  test("a record with a site or relic the baseline lacks does not conserve it"):
    val extra = chronicle.atlasBox.head
    val baseline = chronicle.copy(atlasBox = chronicle.atlasBox.tail)
    assertEquals(ChronicleAccounting.conserves(baseline, chronicle),
      Left(s"site ${extra.site.value} was not in the baseline"))
