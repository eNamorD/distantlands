package oathdigital.application.chronicle

import oathdigital.application.FirstGameBootstrapMapper.lineageOf
import oathdigital.gameplay.setup.FirstGameSetupFixture
import oathdigital.model._
import oathdigital.testkit.CatalogNames

/** TTS strings to and from Chronicles (Chronicle TTS codec design). */
class ChronicleTtsCodecSuite extends munit.FunSuite:
  private val codec = ChronicleTtsCodec(FirstGameSetupFixture.catalog)
  private val chronicle = FirstGameSetupFixture.chronicle
  private val fullExport = "02H3uk5&m#lvuYL;KIO:Mg9FjOs)5.yU!bk{6k9tUwlHwZ}z%uTY3.}IvkwdiP9.r)s-g,XAxa!)JRjGCc5ze$;93~2=J;chc)06a}66Pn:tI#vWs~QDbnK5PpZ#&Xq36lcTLE-g359{rq/DBYLh@RBb~R;?$:iMT},~=/N@)J+fh9Ip8T1#dMl?Gf:M?tM%r/8%cL,lF{RGZOorax@0pONlP@9O.&SmJ>G6bq2t^7FJFmI>W$a,3r4:&ohBXUX)$&d%DuAJl%i~Y,c)@b!/eoUQ9Qs!w223^(:%Lf.{eA5@(1u^DqWE$Kv#=iyHXC/5T"

  private def roundTripped(c: Chronicle)(using munit.Location): Chronicle =
    codec.encode(c).flatMap(e => codec.decode(e.text, c.name)).fold(fail(_), identity)

  test("the full Lua export decodes to the values its own test asserts"):
    val full = codec.decode(fullExport, "Imported").fold(fail(_), identity)
    assertEquals(full.reliquary, Vector("Spiteful Mirror", "Sigil of the Eye",
      "Whistle", "Lost Tapestry", "Cracked Horn").map(CatalogNames.relic))
    assertEquals(full.foundations, Map(FoundationNumber.IV ->
      FoundationState(FoundationFace.Altered, Set.empty)))
    val yellow = full.lineages.find(_.id == lineageOf(PlayerColor.Yellow)).get
    assertEquals(yellow.role, Role.Chancellor)
    assertEquals(yellow.startingAdvisers.map(_.id),
      Vector(CatalogNames.denizen("Scouts")))
    assertEquals(yellow.legacies, Vector(
      LegacyState(CatalogNames.legacy("Light Fingers"), active = false),
      LegacyState(CatalogNames.legacy("Arbiter"), active = true)))
    assertEquals(full.lineages.find(_.id == lineageOf(PlayerColor.Brown))
      .map(_.legacies.map(_.active)), Some(Vector(true, false, true)))
    assert(!full.lineages.exists(_.id == lineageOf(PlayerColor.Black)))
    assertEquals(full.oathkeeperGoal, OathkeeperGoal.Supremacy)
    assertEquals(full.era, EraState(20, Vector.empty))

  test("the first-game fixture round trips, losing only its name"):
    val exported = codec.encode(chronicle).fold(fail(_), identity)
    assert(exported.text.startsWith("03"), exported.text)
    assertEquals(exported.omitted, Vector("the name"))
    assertEquals(roundTripped(chronicle), chronicle)

  test("every Oathkeeper goal survives version 3"):
    OathkeeperGoal.all.foreach(goal =>
      assertEquals(roundTripped(chronicle.copy(oathkeeperGoal = goal))
        .oathkeeperGoal, goal))

  test("a lineage's role, legacies, advisers and relics round trip"):
    val relic = chronicle.relicDeck.head
    val red = LineageState(lineageOf(PlayerColor.Red), None, Role.Citizen,
      Vector(LegacyState(CatalogNames.legacy("Arbiter"), active = true)),
      Vector(DenizenState(chronicle.worldDeck.head, Orientation.FaceDown,
        Tokens.empty)), startingRelics = Vector(relic))
    val kept = chronicle.copy(relicDeck = chronicle.relicDeck.tail,
      worldDeck = chronicle.worldDeck.tail, lineages = Vector(red))
    assertEquals(roundTripped(kept), kept)

  test("an export lists what the TTS string leaves out"):
    val evolved = chronicle.copy(
      era = EraState(15, Vector(EraGame(Vector(
        EraEntry(lineageOf(PlayerColor.Red), 6, Role.Exile))))),
      lineages = Vector(LineageState(lineageOf(PlayerColor.Red),
        Some(PlayerId("Red")), Role.Exile, Vector.empty, Vector.empty)),
      foundations = Map(FoundationNumber.II -> FoundationState(
        FoundationFace.Altered, Set(CatalogNames.legacy("Arbiter")))))
    assertEquals(codec.encode(evolved).map(_.omitted), Right(Vector(
      "the name", "the era target", "the era scorepad", "previous players",
      "Foundation alteration sources", "Exile lineages with nothing kept")))

  test("an unknown card, a wrong kind and a reserved goal are refused"):
    val unknownSite = TtsChronicle(atlasBox = Some(Vector(TtsSite(30, Vector.empty))))
    assertEquals(codec.fromTts(unknownSite, "Imported"), Left("TTS site 30 is not in the catalog"))
    val relicInDeck = TtsChronicle(worldDeck = Some(Vector(401)))
    assertEquals(codec.fromTts(relicInDeck, "Imported"), Left("TTS card 401 is not a denizen"))
    val reserved = TtsChronicle(foundations = Some(TtsFoundations(0, Some(7))))
    assertEquals(codec.fromTts(reserved, "Imported"), Left("Oathkeeper goal 7 is reserved"))
    val fan = TtsChronicle(foundations = Some(TtsFoundations(0, Some(21))))
    assertEquals(codec.fromTts(fan, "Imported"), Left("Oathkeeper goal 21 is fan content"))

  test("a lineage without a TTS color is refused on export"):
    val odd = chronicle.copy(lineages = Vector(LineageState(LineageId("l9"),
      None, Role.Exile, Vector.empty, Vector.empty)))
    assertEquals(codec.encode(odd), Left("lineage l9 has no TTS color"))

  test("the full Lua export survives our own encoding"):
    val full = codec.decode(fullExport, "Imported").fold(fail(_), identity)
    assertEquals(roundTripped(full), full)

  test("altered Foundations round trip"):
    val altered = chronicle.copy(foundations = Vector(FoundationNumber.II,
      FoundationNumber.VI).map(_ -> FoundationState(FoundationFace.Altered,
        Set.empty)).toMap)
    assertEquals(roundTripped(altered), altered)

  test("a version 3 goal digit of 0 reads as Supremacy"):
    val unset = TtsChronicle(foundations = Some(TtsFoundations(0, Some(0))))
    assertEquals(codec.fromTts(unset, "Imported").map(_.oathkeeperGoal),
      Right(OathkeeperGoal.Supremacy))

  test("a denizen in the relic deck is refused"):
    val number = TtsIds(FirstGameSetupFixture.catalog)
      .ttsCard(chronicle.worldDeck.head)
      .fold(fail(_), identity)
    assertEquals(codec.fromTts(TtsChronicle(relicDeck = Some(Vector(number))), "Imported"),
      Left(s"TTS card $number is not a relic"))

  test("a kept Vision is refused on export"):
    val vision = LineageState(lineageOf(PlayerColor.Red), None, Role.Exile,
      Vector.empty, Vector(VisionState(VisionId("vision"), Orientation.FaceDown)))
    assertEquals(codec.encode(chronicle.copy(lineages = Vector(vision))),
      Left(s"lineage ${vision.id.value} keeps the Vision vision, " +
        "which TTS cannot store"))

  test("an import takes its name from the caller and refuses a blank one"):
    assertEquals(codec.decode(fullExport, "Harsch's game").map(_.name),
      Right("Harsch's game"))
    assertEquals(codec.decode(fullExport, " "),
      Left("a Chronicle name has no leading or trailing spaces"))
