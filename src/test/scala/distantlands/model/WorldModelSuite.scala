package distantlands.model

class WorldModelSuite extends munit.FunSuite:
  private def player(id: String, lineage: String): PlayerState = PlayerState(
    PlayerId(id), LineageId(lineage), None,
    PlayerBoardState(0, 0, 0, 0, SupplyTrack.full), Vector.empty,
    Vector.empty, Vector.empty)

  test("site rule derives players and shared rulers directly from forces"):
    val red = player("red-player", "red-lineage")
    val blue = player("blue-player", "blue-lineage")
    val players = Vector(red, blue)

    assertEquals(SiteRule.ruler(SiteForces.Empty, players),
      Right(SiteRuler.Unruled))
    assertEquals(SiteRule.ruler(
      SiteForces.Occupied(ForceKind.Bandit, 2), players),
      Right(SiteRuler.Bandits))
    assertEquals(SiteRule.ruler(
      SiteForces.Occupied(ForceKind.Imperial, 1), players),
      Right(SiteRuler.Empire))
    assertEquals(SiteRule.ruler(SiteForces.Occupied(
      ForceKind.Exile(red.lineage), 3), players),
      Right(SiteRuler.Player(red.player)))
    assertEquals(SiteRule.ruledBy(SiteForces.Occupied(
      ForceKind.Exile(red.lineage), 1), players, red.player), Right(true))

  test("same-ruler and enemy semantics support Campaign target derivation"):
    val red = player("red-player", "red-lineage")
    val players = Vector(red)
    val redForces = SiteForces.Occupied(ForceKind.Exile(red.lineage), 1)

    assertEquals(SiteRule.sameRuler(redForces,
      SiteForces.Occupied(ForceKind.Exile(red.lineage), 4), players), Right(true))
    assertEquals(SiteRule.sameRuler(SiteForces.Occupied(ForceKind.Imperial, 1),
      SiteForces.Occupied(ForceKind.Imperial, 2), players), Right(true))
    assertEquals(SiteRule.sameRuler(SiteForces.Empty, SiteForces.Empty, players),
      Right(false))
    assert(SiteRule.enemies(SiteRuler.Player(red.player), SiteRuler.Empire))
    assert(!SiteRule.enemies(SiteRuler.Bandits, SiteRuler.Bandits))
    assert(!SiteRule.enemies(SiteRuler.Unruled, SiteRuler.Empire))

  test("site rule rejects unknown and duplicate current lineage mappings"):
    val red = player("red-player", "red-lineage")
    val duplicate = player("other-red-player", "red-lineage")
    val forces = SiteForces.Occupied(ForceKind.Exile(red.lineage), 1)

    assertEquals(SiteRule.ruler(forces, Vector.empty),
      Left(SiteRuleError.UnknownLineage(red.lineage)))
    assertEquals(SiteRule.ruler(forces, Vector(red, duplicate)),
      Left(SiteRuleError.DuplicateCurrentLineage(
        red.lineage, Vector(red.player, duplicate.player))))

  private val banditHeldSite = SiteState(
    SiteForces.Occupied(ForceKind.Bandit, 1), Vector.empty, Vector.empty,
    Tokens.empty)

  /** A map of the sites S1 to S`count`, placed going Hinterward. */
  private def mapOfSites(count: Int): MapState = MapState.inSlots(
    (1 to count).toVector.map(index => SiteId(s"S$index") -> banditHeldSite))

  test("sites fill the Cradle's 2 slots, then the Provinces' 3, then the " +
      "Hinterland's 3"):
    assertEquals(Region.all.map(_.slots), Vector(2, 3, 3))
    val full = mapOfSites(8)
    assertEquals(full.cradle.map(_.value), Vector("S1", "S2"))
    assertEquals(full.provinces.map(_.value), Vector("S3", "S4", "S5"))
    assertEquals(full.hinterland.map(_.value), Vector("S6", "S7", "S8"))
    assertEquals(full.sites.keySet, full.inPlay.toSet)
    val three = mapOfSites(3)
    assertEquals(three.cradle.map(_.value), Vector("S1", "S2"))
    assertEquals(three.provinces.map(_.value), Vector("S3"))
    assertEquals(three.hinterland, Vector.empty)
    assertEquals(Region.all.map(three.sitesIn), Vector(three.cradle,
      three.provinces, three.hinterland))

  test("the next slot is in the first region not yet full, and a full map " +
      "has none"):
    assertEquals(mapOfSites(1).nextSlot, Some(Region.Cradle))
    assertEquals(mapOfSites(2).nextSlot, Some(Region.Provinces))
    assertEquals(mapOfSites(4).nextSlot, Some(Region.Provinces))
    assertEquals(mapOfSites(5).nextSlot, Some(Region.Hinterland))
    assertEquals(mapOfSites(7).nextSlot, Some(Region.Hinterland))
    assertEquals(mapOfSites(8).nextSlot, None)
    assertEquals(Region.all.map(mapOfSites(4).emptySlots), Vector(0, 1, 3))
    assertEquals(Region.all.map(mapOfSites(8).emptySlots), Vector(0, 0, 0))

  test("the Hintermost site is the last one Hinterward, and a map with no " +
      "site has none"):
    assertEquals(mapOfSites(8).hintermost, Some(SiteId("S8")))
    assertEquals(mapOfSites(4).hintermost, Some(SiteId("S4")))
    assertEquals(mapOfSites(2).hintermost, Some(SiteId("S2")))
    assertEquals(mapOfSites(0).hintermost, None)
    assertEquals(mapOfSites(0).nextSlot, Some(Region.Cradle))

  test("a site placed in a region takes its next slot, after its sites"):
    val four = mapOfSites(4)
    val placed = four.withSite(Region.Provinces, SiteId("S9"), banditHeldSite)
    assertEquals(placed.provinces.map(_.value), Vector("S3", "S4", "S9"))
    assertEquals(placed.hintermost, Some(SiteId("S9")))
    assertEquals(placed.sites(SiteId("S9")), banditHeldSite)
    assertEquals(placed.nextSlot, Some(Region.Hinterland))
    assertEquals(mapOfSites(1).withSite(Region.Cradle, SiteId("S9"),
      banditHeldSite).cradle.map(_.value), Vector("S1", "S9"))
    assertEquals(mapOfSites(5).withSite(Region.Hinterland, SiteId("S9"),
      banditHeldSite).hinterland.map(_.value), Vector("S9"))

  /** An Atlas of the sites A1 to A`count` around an Empire divider, A1 at
    * the Recent end. */
  private def atlasOfSites(count: Int): AtlasState = AtlasState(
    (1 to count).toVector.flatMap(index => Vector[AtlasEntry](
      AtlasEntry.StoredSite(SiteId(s"A$index"), Vector.empty, Vector.empty),
      AtlasEntry.EmpireDivider)))

  test("the Recent end of the Atlas is its first site and the Forgotten " +
      "end its last, and one site is both"):
    assertEquals(AtlasEnd.all.map(_.key), Vector("recent", "forgotten"))
    assertEquals(AtlasEnd.all.flatMap(end => AtlasEnd.fromKey(end.key)),
      AtlasEnd.all)
    assertEquals(AtlasEnd.fromKey("middle"), None)
    val three = atlasOfSites(3)
    assertEquals(three.sites.map(_.id.value), Vector("A1", "A2", "A3"))
    assertEquals(three.site(AtlasEnd.Recent).map(_.id.value), Some("A1"))
    assertEquals(three.site(AtlasEnd.Forgotten).map(_.id.value), Some("A3"))
    assertEquals(AtlasEnd.all.map(atlasOfSites(1).site(_).map(_.id.value)),
      Vector(Some("A1"), Some("A1")))
    assertEquals(AtlasEnd.all.map(atlasOfSites(0).site), Vector(None, None))
    val taken = three.without(SiteId("A2"))
    assertEquals(taken.sites.map(_.id.value), Vector("A1", "A3"))
    assertEquals(taken.entries.count(_ == AtlasEntry.EmpireDivider), 3)

  test("a bandit site holds its bandits, its printed tokens and its relics " +
      "facedown, and a stored site's edifice is placed ruined before its " +
      "denizens, faceup"):
    val edifice = EdificeId("E-test")
    val denizen = DenizenId("D-test")
    val stored = AtlasEntry.StoredSite(SiteId("A1"),
      Vector(DenizenState(denizen, Orientation.FaceDown, Tokens.empty),
        EdificeState(EdificeId("E-other"), EdificeSide.Intact, Tokens.empty)),
      Vector.empty, Some(edifice))
    assertEquals(stored.placedDenizens, Vector[SiteDenizenState](
      EdificeState(edifice, EdificeSide.Ruined, Tokens.empty),
      DenizenState(denizen, Orientation.FaceUp, Tokens.empty),
      EdificeState(EdificeId("E-other"), EdificeSide.Ruined, Tokens.empty)))
    val relics = Vector(RelicId("R-a"), RelicId("R-b"))
    assertEquals(SiteState.banditSite(2, Tokens(3, 0), stored.placedDenizens,
      relics), SiteState(SiteForces.Occupied(ForceKind.Bandit, 2),
      stored.placedDenizens,
      relics.map(RelicState(_, Orientation.FaceDown, Tokens.empty)),
      Tokens(3, 0)))
    assertEquals(SiteState.banditSite(0, Tokens.empty, Vector.empty,
      Vector.empty).forces, SiteForces.Empty)
    assertEquals(Vector(0, 1, 2, 3).map(SiteState.openRelicSlots(2, _)),
      Vector(2, 1, 0, 0))

