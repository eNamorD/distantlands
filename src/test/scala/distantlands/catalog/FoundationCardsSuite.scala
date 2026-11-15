package distantlands.catalog

import distantlands.model.{FoundationFace, FoundationNumber, PowerId}

/** The six foundation setup cards (Foundations design, slice 1). */
class FoundationCardsSuite extends munit.FunSuite:
  test("the six cards are listed I to VI, each face by its printed name"):
    assertEquals(FoundationCards.all.map(card =>
      (card.number, card.normal.name, card.altered.name)), Vector(
      (FoundationNumber.I, "Imperial Maps", "Wide Horizons"),
      (FoundationNumber.II, "Powerful Tribes", "Movers and Shakers"),
      (FoundationNumber.III, "Quiet Ambitions", "Public Ambitions"),
      (FoundationNumber.IV, "Ties of Fellowship", "Teeming World"),
      (FoundationNumber.V, "Mob's Favor", "Grand Council"),
      (FoundationNumber.VI, "Wandering Flame", "Festival")))

  test("each face has its own id, spelled from its numeral and its name"):
    assertEquals(FoundationCards.all.flatMap(card =>
      Vector(card.normal.id, card.altered.id)).map(_.value), Vector(
      "foundation.i.imperial-maps", "foundation.i.wide-horizons",
      "foundation.ii.powerful-tribes", "foundation.ii.movers-and-shakers",
      "foundation.iii.quiet-ambitions", "foundation.iii.public-ambitions",
      "foundation.iv.ties-of-fellowship", "foundation.iv.teeming-world",
      "foundation.v.mobs-favor", "foundation.v.grand-council",
      "foundation.vi.wandering-flame", "foundation.vi.festival"))

  test("a face is found by its card's number and which face lies up, and " +
      "by its id"):
    val festival = FoundationCards.face(FoundationNumber.VI,
      FoundationFace.Altered)
    assertEquals(festival.name, "Festival")
    assertEquals(FoundationCards.face(FoundationNumber.VI,
      FoundationFace.Normal).name, "Wandering Flame")
    assertEquals(FoundationCards.faceWithId(festival.id), Some(festival))
    // The banner's Festival side is another source with another id.
    assertEquals(FoundationCards.faceWithId(
      PowerId("banner.darkest-secret.festival")), None)

  test("a face's text is the printed card's, a blank line between " +
      "paragraphs"):
    assertEquals(FoundationCards.face(FoundationNumber.V,
      FoundationFace.Altered).text,
      "Flip the People's Favor to its **Grand Council** side. Place " +
        "[favor] on it from the shared bank.\n\n" +
        "If the Oathkeeper goal is the People's Favor, give the banner to " +
        "the Chancellor, if any. Otherwise, place it in the shared bank.")
    assertEquals(FoundationCards.face(FoundationNumber.I,
      FoundationFace.Altered).text.split("\n\n").last,
      "_Keep this card out as a reminder._")
    assertEquals(FoundationCards.all.map(card =>
      (card.normal.text.split("\n\n").length,
        card.altered.text.split("\n\n").length)),
      Vector((5, 5), (3, 2), (1, 2), (3, 2), (2, 2), (2, 2)))
