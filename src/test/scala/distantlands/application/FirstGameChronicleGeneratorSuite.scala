package distantlands.application

import distantlands.gameplay.operations.TheGrandScepterCard
import distantlands.gameplay.setup.FirstGameSetupFixture.catalog
import distantlands.model.{CardId, Chronicle, EdificeId, Suit}
import distantlands.testkit.Unwrap.{accepted, present}

class FirstGameChronicleGeneratorSuite extends munit.FunSuite:
  private val random = ChronicleRandomPort.random

  private def generated(using munit.Location): Chronicle =
    accepted(FirstGameChronicleGenerator.generate(catalog, "Test Chronicle",
      random))

  /** The printed suit of `card`; fails naming a card the catalog lacks. */
  private def suitOf(card: CardId)(using munit.Location): Suit =
    present(catalog.suitOf(card), s"${card.value} in the catalog")

  test("the atlas box holds all 24 sites, each Homeland carrying its suit's edifice"):
    val chronicle = generated
    assertEquals(chronicle.atlasBox.map(_.site).toSet, catalog.sites.map(_.id).toSet)
    assertEquals(chronicle.atlasBox.map(_.site).distinct.size, 24)
    catalog.sites.foreach { site =>
      val stored = present(chronicle.atlasBox.find(_.site == site.id),
        s"${site.name} in the atlas box")
      site.homeland match
        case Some(suit) =>
          assertEquals(stored.items.size, 1)
          stored.items.head match
            case edifice: EdificeId => assertEquals(suitOf(edifice), suit)
            case other => fail(s"${site.name} stores $other, not an edifice")
        case None => assertEquals(stored.items, Vector.empty)
    }

  test("the world deck has 60 denizens, 10 per suit"):
    val chronicle = generated
    assertEquals(chronicle.worldDeck.size, 60)
    assertEquals(chronicle.worldDeck.distinct.size, 60)
    Suit.all.foreach { suit =>
      val suited = chronicle.worldDeck.filter(id => suitOf(id) == suit)
      assertEquals(suited.size, 10)
    }

  test("the dispossessed pile has 12 denizens, 2 per suit, disjoint from " +
      "the world deck"):
    val chronicle = generated
    assertEquals(chronicle.dispossessed.size, 12)
    assertEquals(chronicle.dispossessed.distinct.size, 12)
    assertEquals(chronicle.worldDeck.toSet intersect chronicle.dispossessed.toSet,
      Set.empty[distantlands.model.DenizenId])
    Suit.all.foreach { suit =>
      val suited = chronicle.dispossessed.filter(id => suitOf(id) == suit)
      assertEquals(suited.size, 2)
    }

  test("the relic deck has every ordinary relic exactly once"):
    val chronicle = generated
    val ordinary = catalog.relics.filter(_.id != TheGrandScepterCard.id)
      .map(r => distantlands.model.RelicId(r.id.value))
    assertEquals(chronicle.relicDeck.toSet, ordinary.toSet)
    assertEquals(chronicle.relicDeck.size, ordinary.size)

  test("every Homeland carries the lowest-id edifice of its suit"):
    val chronicle = generated
    chronicle.atlasBox.flatMap(_.items).collect { case id: EdificeId => id }
      .foreach { id =>
        val suit = suitOf(id)
        assertEquals(id.value,
          catalog.edifices.filter(_.suit == suit).map(_.id.value).min)
      }

  test("a suit with fewer than 12 denizens is refused, with its count"):
    val hearth = catalog.denizens.filter(_.suit == Suit.Hearth)
    val short = catalog.copy(denizens =
      catalog.denizens.filterNot(_.suit == Suit.Hearth) ++ hearth.take(11))
    val refused = FirstGameChronicleGenerator.generate(short, "Short", random)
    assertEquals(refused.left.toOption,
      Some(ChronicleGeneratorFailure.TooFewSuitDenizens(Suit.Hearth, 11)))

  test("five generated atlas orders are not all the same: the port is " +
      "consulted"):
    val orders = Vector.fill(5)(generated.atlasBox.map(_.site))
    assert(orders.distinct.size > 1, "24 shuffled sites should not repeat every time")
