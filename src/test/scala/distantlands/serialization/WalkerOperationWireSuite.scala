package distantlands.serialization

import distantlands.gameplay.powers.action.FaeMerchantCard
import distantlands.gameplay.powers.economy.KnightsErrantCard
import distantlands.gameplay.powers.setup.GreatMarketCard
import distantlands.model._
import distantlands.gameplay.setup.FirstGameSetupFixture
import distantlands.gameplay.walker.{DeltaPayload, RollPayload,
  WalkerStepRecorded}
import distantlands.serialization.WalkerWireEvents.{encodedStream,
  encodedStreamThatRoundTrips, recordedOperation}
import distantlands.testkit.Outcomes.assertWireRoundTrips
import distantlands.testkit.Unwrap.accepted

/** Walker step events on the wire across operation families: locations,
  * pieces, payloads, the step envelope, every variant of the vocabulary,
  * and the wire kinds' partition into families. Each family's own
  * spellings are pinned in its `<Family>OperationWireSuite`. */
class WalkerOperationWireSuite extends munit.FunSuite:
  test("walker delta events round-trip the node path each one ran at"):
    val player = PlayerId("red")
    val pool = PoolKey("recover")
    val relic = RelicId("R1")
    val site = SiteId("site")
    val move = Move(Piece.Card(relic),
      PositionedLocation(Location.Site(site)),
      PositionedLocation(Location.PlayArea(player)),
      resultingOrientation = Some(Orientation.FaceDown), by = Some(player))
    val events = Vector[OathEvent](
      WalkerStepRecorded("0", DeltaPayload(Vector(ModifyDicePool(pool, 2))), Vector.empty),
      WalkerStepRecorded("1.0.1", DeltaPayload(Vector(SpendSupply(player, 1))), Vector.empty),
      WalkerStepRecorded("2.1", DeltaPayload(Vector(move)), Vector.empty))

    val encoded = encodedStreamThatRoundTrips("walker", events)
    assertEquals(ujson.read(encoded).arr.map(
      _("payload")("nodeId").str).toVector,
      Vector("0", "1.0.1", "2.1"))

  test("Catacombs' recorded batch round-trips: a relic move off the top of " +
      "the relic deck and a cost placed on a card"):
    // The exact op vector `CatacombsContribution` builds. Encoding it used to
    // throw on `Location.Deck` and again on `PayCost`, which the append path
    // turned into a codec failure -- so `StartWalker` + Catacombs, the only
    // end-to-end route to this power once the legacy object goes, could never
    // be persisted.
    val player = PlayerId("red")
    val relic = RelicId("R1")
    val site = SiteId("site")
    val denizen = DenizenId("catacombs")
    val event: OathEvent = WalkerStepRecorded("0",
      DeltaPayload(Vector(
        Move(Piece.Card(relic),
          PositionedLocation(Location.Deck(CardDeck.Relic), StackPosition.Top),
          PositionedLocation(Location.Site(site)),
          resultingOrientation = Some(Orientation.FaceDown)),
        PayCost(player, Location.OnCard(denizen), Cost(secret = 1)))),
      Vector(PowerId("denizen.catacombs")))

    val encoded = encodedStreamThatRoundTrips("walker", Vector(event))
    val ops = ujson.read(encoded).arr.head("payload")("step")("ops").arr
    assertEquals(ops.map(_("kind").str).toVector, Vector("move", "pay-cost"))
    assertEquals(ops(0)("from")("location")("deck").str, "relic")
    assertEquals(ops(0)("from")("position").str, "top")
    assertEquals(ops(1)("placedAt")("card")("id").str, "catacombs")
    assertEquals(ops(1)("cost")("secret").num, 1d)

  test("every Location variant round-trips through the walker codec"):
    // `encodeLocation` is total over the sealed hierarchy, so this list is
    // the whole of it. Keeping the list exhaustive is what makes the
    // compiler's totality check meaningful on the decode side too: a new
    // variant fails to compile in the encoder and fails this test in the
    // decoder.
    val player = PlayerId("red")
    val locations = Vector[Location](
      Location.Hand(player),
      // Not `player`'s own play area: `PayCost`'s children move the favor out
      // of the payer's play area, and a move to the same location is illegal.
      Location.PlayArea(PlayerId("blue")),
      Location.Site(SiteId("site")),
      Location.OnCard(DenizenId("catacombs")),
      Location.OnBanner(Banner.DarkestSecret),
      Location.FavorBank(Suit.Arcane),
      Location.WarbandBank(ForceKind.Exile(LineageId("lineage"))),
      Location.WarbandBank(ForceKind.Imperial),
      Location.Deck(CardDeck.World),
      Location.Deck(CardDeck.Relic),
      Location.Deck(CardDeck.Edifice),
      Location.Deck(CardDeck.Legacy),
      Location.RegionalDiscard(Region.Cradle),
      Location.SharedBank,
      Location.SetAsideRelics,
      Location.Reliquary,
      Location.Atlas,
      Location.Dispossessed,
      Location.CardGathering,
      Location.NewlyDispossessed,
      Location.SuitedReserve(Suit.Beast),
      Location.StartingAdvisers(LineageId("lineage")),
      Location.Legacies(LineageId("lineage")),
      Location.PublicLegacies)
    // `PayCost` carries a bare `Location`, so it exercises each variant
    // without the stack-position wrapper or `Move`'s from/to constraints.
    val events = locations.map(location =>
      recordedOperation(PayCost(player, location, Cost(favor = 1))))

    val encoded = encodedStreamThatRoundTrips("walker", events)
    assertEquals(ujson.read(encoded).arr.map(
      _("payload")("step")("ops")(0)("placedAt")("kind").str).toVector.distinct.size,
      locations.map(_.getClass.getSimpleName).distinct.size)

  test("the gathering pile is card-gathering on the wire, as a location " +
      "and as a shuffled pile"):
    val events = Vector(
      recordedOperation(PayCost(PlayerId("red"), Location.CardGathering,
        Cost(favor = 1))),
      recordedOperation(Shuffle(ShufflePile.CardGathering,
        Some(Vector(DenizenId("denizen"))))))
    val encoded =
      ujson.read(encodedStreamThatRoundTrips("walker", events)).arr
    def op(at: Int): ujson.Value = encoded(at)("payload")("step")("ops")(0)
    assertEquals(op(0)("placedAt")("kind").str, "card-gathering")
    assertEquals(op(1)("pile")("kind").str, "card-gathering")

  test("Secret Society's packets are vision-packets on the wire, and the " +
      "world deck's shuffle keeps its older spelling (R19)"):
    val cards = Vector[CardId](DenizenId("denizen"), VisionId("vision:faith"))
    val events = Vector(
      recordedOperation(Shuffle(ShufflePile.VisionPackets, Some(cards))),
      recordedOperation(Shuffle(ShufflePile.Search(SearchSource.WorldDeck),
        Some(cards))))
    val encoded =
      ujson.read(encodedStreamThatRoundTrips("walker", events)).arr
    def pile(at: Int): ujson.Value =
      encoded(at)("payload")("step")("ops")(0)("pile")
    assertEquals(pile(0), ujson.Obj("kind" -> "vision-packets"))
    // A stream written before this pile kind existed reads the same.
    assertEquals(pile(1), ujson.Obj("kind" -> "world"))

  test("an unknown walker location kind, deck, operation kind or negative " +
      "cost decodes to a typed WireError"):
    val player = PlayerId("red")
    val event = recordedOperation(
      PayCost(player, Location.Deck(CardDeck.Relic), Cost(secret = 1)))
    val encoded = encodedStream("walker", Vector(event))
    accepted(GameEventWire.decodeStream(encoded))

    def refusing(edit: ujson.Value => Unit, refusal: WireError) =
      (edit, refusal)
    Vector(
      refusing(_("payload")("step")("ops")(0)("placedAt")("kind") = "not-a-location",
        WireError.InvalidValue("$[0].payload.step.ops[0].placedAt.kind",
          "unknown recorded walker location 'not-a-location'")),
      refusing(_("payload")("step")("ops")(0)("placedAt")("deck") = "not-a-deck",
        WireError.InvalidValue("$[0].payload.step.ops[0].placedAt.deck",
          "unknown card deck 'not-a-deck'")),
      refusing(_("payload")("step")("ops")(0)("cost")("secret") = -1,
        WireError.InvalidValue("$[0].payload.step.ops[0].cost.secret",
          "must be between 0 and 9007199254740991 inclusive")),
      refusing(_("payload")("step")("ops")(0)("kind") = "not-an-operation",
        WireError.InvalidValue("$[0].payload.step.ops[0].kind",
          "unknown recorded walker operation 'not-an-operation'"))
    ).foreach { (corrupt, refusal) =>
      val injected = ujson.read(encoded).arr
      corrupt(injected(0))
      assertEquals(GameEventWire.decodeStream(ujson.write(injected)),
        Left(refusal), ujson.write(injected))
    }

  test("a WalkerStepRecorded carrying two contribution ids round-trips"):
    val pool = PoolKey("recover")
    val event: OathEvent = WalkerStepRecorded("0",
      DeltaPayload(Vector(ModifyDicePool(pool, 2))),
      Vector(PowerId("power.one"), PowerId("power.two")))

    val encoded = encodedStreamThatRoundTrips("walker", Vector(event))
    assertEquals(ujson.read(encoded).arr.head("payload")("contributions").arr
      .map(_.str).toVector, Vector("power.one", "power.two"))

  test("a delta's operations are written under its payload, and a step with " +
      "a top-level ops key no longer decodes"):
    val event: OathEvent = WalkerStepRecorded("0",
      DeltaPayload(Vector(ModifyDicePool(PoolKey("recover"), 2))),
      Vector.empty)
    val encoded = encodedStreamThatRoundTrips("walker", Vector(event))
    val written = ujson.read(encoded).arr.head("payload")
    assertEquals(written("step")("ops")(0)("kind").str, "modify-dice-pool")
    assert(!written.obj.contains("ops"), written)

    // The shape written before this change: operations beside the payload
    // rather than inside it. It fails on the key the payload now requires,
    // which is a journal that does not load rather than one that silently
    // replays without its batch.
    val old = ujson.read(encoded)
    val moved = old.arr.head("payload")("step")("ops")
    old.arr.head("payload")("step").obj.remove("ops")
    old.arr.head("payload")("ops") = moved
    // The message is ujson's own wording for a missing key, so assert the
    // path and the error type rather than the text.
    GameEventWire.decodeStream(ujson.write(old)) match
      case Left(WireError.InvalidValue(path, _)) =>
        assertEquals(path, "$[0].payload")
      case other => fail(s"expected an InvalidValue at the step, got $other")

  test("an unencodable die face returns a typed WireError, " +
      "not a thrown exception"):
    // The append path must report a value it cannot write as a WireError,
    // exactly like an unrecognized value on the decode side
    // (`encodePayloadSafe`, GameEventWire.scala). This is the only test that
    // reaches that catch: every other encode-direction failure is an
    // envelope rejection, which is refused before it.
    //
    // The stub is a DieFace rather than a WalkerStepPayload because the
    // payload family is sealed and a fourth member is a compile error, not a
    // runtime one. DieFace stays open (DieFace.scala) and RollPayload holds
    // a plain Vector[DieFace], so an unknown face still reaches
    // `encodeDieFace`'s throw inside the protected encode. The event itself
    // must be one the encoder registers, since an unregistered event is
    // refused by `discriminator` first (GameEventWireEnvelopeSuite pins
    // that).
    //
    // So `encodeDieFace`'s fallback is the last throw this boundary can be
    // provoked through. Sealing DieFace would remove it and leave the
    // boundary untestable from any public entry point -- loudly, since this
    // stub stops compiling in the same build that -Werror deletes the
    // fallback, but weigh it there rather than discovering it here.

    // Stub of DieFace: a face neither die family declares.
    case object UnknownDieFace extends DieFace
    val event = WalkerStepRecorded("0",
      RollPayload(PoolKey("recover"), Vector(UnknownDieFace)), Vector.empty)
    GameEventWire.encodeEvent("walker", 0, event) match
      case Left(WireError.InvalidValue("$.payload", _)) => ()
      case other => fail(s"expected an invalid-payload WireError, got $other")

  private val player = PlayerId("red")

  private val other = PlayerId("blue")

  private val site = SiteId("site")

  private val denizen = DenizenId("denizen")

  private val vision = VisionId("vision")

  private val relic = RelicId("relic")

  private val edifice = EdificeId("edifice")

  private val legacy = LegacyId("legacy")

  private val lineage = LineageId("lineage")

  private val everyOperation: Vector[CoreOperation] = Vector(
    GainSupply(player, 2),
    SpendSupply(player, 1),
    ModifyDicePool(PoolKey("recover"), 2),
    RecordPowerUse(PowerUseRef(PowerTiming.Wake,
      PowerSourceRef.Site(site), PowerId("site.take-wealth"))),
    RecordPowerUse(PowerUseRef(PowerTiming.Rest,
      PowerSourceRef.Card(denizen), PowerId("denizen.silver-tongue"))),
    RecordPowerUse(PowerUseRef(PowerTiming.Act,
      PowerSourceRef.Card(relic), PowerId("relic.test-power"))),
    RecordPowerUse(PowerUseRef(PowerTiming.Act,
      PowerSourceRef.Card(legacy), PowerId("legacy.test-power"))),
    EnterPhase(Phase.Act),
    BeginTurn(player, Phase.Wake),
    BeginTurn(other, Phase.RoundEnd),
    Move(Piece.Card(relic),
      PositionedLocation(Location.Deck(CardDeck.Relic), StackPosition.Top),
      PositionedLocation(Location.Site(site)), Some(Orientation.FaceDown)),
    PayCost(player, Location.OnCard(denizen), Cost(favor = 1)),
    Peek(player, denizen, Location.Site(site)),
    Flip(denizen, Location.Site(site), Orientation.FaceUp),
    FlipSecrets(player, 2, SecretSide.FaceUp, SecretSide.FaceDown),
    Burn.favor(3, PositionedLocation(Location.PlayArea(player)), None),
    Bury(BuryableCard.Relic(relic), PositionedLocation(Location.PlayArea(player))),
    Discard.Denizen(denizen, PositionedLocation(Location.Site(site)),
      Region.Cradle, Suit.Arcane, favor = 1, secrets = 1,
      actingPlayer = player),
    Discard.Vision(vision, PositionedLocation(Location.Hand(player)),
      Region.Provinces),
    Discard.Buried(BuryableCard.Denizen(denizen),
      PositionedLocation(Location.Site(site)), Some(Suit.Arcane),
      favor = 1, secrets = 1, actingPlayer = player),
    Discard.Buried(BuryableCard.Vision(vision),
      PositionedLocation(Location.Hand(player)), None, favor = 0,
      secrets = 0, actingPlayer = player),
    Discard.RuinedEdifice(edifice, PositionedLocation(Location.Site(site)),
      Suit.Order, favor = 0, secrets = 0, actingPlayer = player),
    Discard.Relic(relic, PositionedLocation(Location.PlayArea(player)),
      secrets = 2, actingPlayer = player),
    RemoveFromMap(denizen, PositionedLocation(Location.Site(site)),
      PositionedLocation(Location.Dispossessed), Suit.Hearth, favor = 1,
      secrets = 2, actingPlayer = player),
    RemoveFromMap(edifice, PositionedLocation(Location.Site(site)),
      PositionedLocation(Location.Deck(CardDeck.Edifice),
        StackPosition.Bottom), Suit.Order, favor = 0, secrets = 0,
      actingPlayer = player),
    Draw(player, Vector(relic), Location.Deck(CardDeck.Relic),
      Location.PlayArea(player)),
    Draw(player, Vector(denizen), Location.RegionalDiscard(Region.Cradle),
      Location.Hand(player), StackPosition.Bottom),
    Exchange(
      Give(Piece.Favor(1), player, Location.PlayArea(player),
        Location.PlayArea(other)),
      Give(Piece.Secrets(1), other, Location.PlayArea(other),
        Location.PlayArea(player))),
    Gain.Favor(player, Suit.Beast, 2),
    Gain.Secrets(player, 3),
    Gain.Warbands(player, ForceKind.Imperial, 4),
    Give(Piece.Secrets(2), player, Location.PlayArea(player),
      Location.PlayArea(other)),
    Kill(Piece.Warbands(ForceKind.Imperial, 3),
      PositionedLocation(Location.Site(site))),
    Play(denizen, PositionedLocation(Location.Hand(player)),
      Location.Site(site), Orientation.FaceUp),
    Replace(Piece.Warbands(ForceKind.Imperial, 2),
      Piece.Warbands(ForceKind.Exile(lineage), 2),
      PositionedLocation(Location.Site(site))),
    Replace(Piece.Warbands(ForceKind.Imperial, 2),
      Piece.Warbands(ForceKind.Exile(lineage), 2),
      PositionedLocation(Location.Site(site)), by = Some(player)),
    Reveal(denizen, Location.Site(site)),
    Sacrifice(player, Piece.Warbands(ForceKind.Exile(lineage), 1),
      PositionedLocation(Location.Site(site))),
    Swap(denizen, PositionedLocation(Location.Hand(player)),
      vision, PositionedLocation(Location.PlayArea(player))),
    Take(Piece.Secrets(1), player, Location.SharedBank,
      Location.PlayArea(player)),
    Roll(PoolKey("recover"), DiceSpec(DiceKind.Defense)),
    ModifyRollOutcome(PoolKey("recover"), Some(1), Some(2)),
    Reroll(PoolKey("recover"), Vector(0, 2),
      Some(Vector(DefenseDieFace.TwoShields, DefenseDieFace.Blank))),
    Reroll(PoolKey("campaign.attack"), Vector(1)),
    SetDieFaces(PoolKey("campaign.defense"),
      Vector(DefenseDieFace.TwoShields, DefenseDieFace.TwoShields)),
    RecordCampaignResult(CampaignResult(player, CampaignKind.Conquest,
      CampaignDefender.Bandits, Vector(site), Vector.empty, force = 2,
      attackFaces = Vector(AttackDieFace.OneSword), attackScore = 1,
      skullLosses = 0, sacrificed = 0,
      defenseFaces = Vector(DefenseDieFace.Blank), defenseScore = 2,
      attackerWins = false)),
    Shuffle(ShufflePile.Search(SearchSource.WorldDeck),
      Some(Vector(denizen, vision))),
    Shuffle(ShufflePile.Search(SearchSource.RegionalDiscard(Region.Cradle)),
      Some(Vector(vision))),
    Shuffle(ShufflePile.Search(
      SearchSource.RegionalDiscard(Region.Hinterland))),
    Shuffle(ShufflePile.EdificeDeck,
      Some(Vector(GreatMarketCard.id))),
    Shuffle(ShufflePile.SetAsideRelics,
      Some(Vector(RelicId("R1")))),
    Shuffle(ShufflePile.CardGathering, Some(Vector(denizen))),
    Shuffle(ShufflePile.Reliquary),
    Shuffle(ShufflePile.Dispossessed, Some(Vector(denizen))),
    Shuffle(ShufflePile.SuitedReserve(Suit.Beast),
      Some(Vector(FaeMerchantCard.id))),
    SetOathkeeper(Some(PlayerId("red"))),
    SetOathkeeper(None),
    SetOathkeeperGoal(OathkeeperGoal.ThePeople),
    ChronicleUpdated(FirstGameSetupFixture.chronicle.copy(
      oathkeeperGoal = OathkeeperGoal.ThePeople)),
    RoleChanged(LineageId("lineage:red"), Role.Chancellor),
    RoleChanged(LineageId("lineage:blue"), Role.Citizen),
    ChronicleTaskAssigned(ChronicleTask.Beacon, PlayerId("red")),
    GrandScepterFromBox(PlayerId("red")),
    ImperialWarbandsTaken(8),
    WarbandsAdded(ForceKind.Imperial, 2),
    WarbandsAdded(ForceKind.Exile(LineageId("l1")), 3),
    UsurperLimiterRemoved,
    LineagePointsScored(LineageId("lineage:red"), 3),
    EdificeBuilt(GreatMarketCard.id, site),
    FoundationRestored(FoundationNumber.III),
    FoundationAltered(FoundationNumber.III, LegacyId("L32")),
    LegaciesScored(Vector(LegacyId("L02"))),
    StartingRelicKept(PlayerId("red"), RelicId("R1")),
    WinnerNamed(PlayerId("red"), VictoryKind.StableRegime),
    RelicsHidden(Vector(RelicId("R1"), RelicId("R2"))),
    PlaceAtlasSite(AtlasEnd.Forgotten, Region.Provinces, capacity = 2,
      relicSlots = 1, Tokens(3, 0)),
    SuitedReserveFromBox(Suit.Beast,
      Vector(FaeMerchantCard.id)),
    TrackPawn(PlayerId("red"), PlayerId("blue")),
    ClearTrackedPawn(PlayerId("red")),
    SetFestivalMarker(Some(site)),
    SetFestivalMarker(None),
    FlipBanner(Banner.PeoplesFavor),
    FlipBanner(Banner.DarkestSecret),
    SetEndDieModifier(2),
    SetEndDieModifier(-1),
    QueueProcedure(QueuedProcedure(PlayerId("p1"), ActionRef.Campaign,
        Vector.empty,
        Some(PowerSourceRef.Card(KnightsErrantCard.id)),
        QueueKind.Offer)),
    QueueProcedure(QueuedProcedure(PlayerId("p1"), ActionRef.Trade,
      Vector(DecisionOptionRef.Button("favor")), None, QueueKind.Run),
      atFront = true),
    DrawNomadFavor(player, 2),
    AdvanceVisionsDrawn,
    AdvanceRound,
    Defer(DeferralKey("test.held"), Vector(GainSupply(player, 1))),
    TakeDeferred(DeferralKey("test.held")),
    ActAsIf(site),
    ActAsIfAdvisers(player),
    OpenPick("test.decision", DecisionOptionRef.Button("title"), Some("label")),
    ClosePick())

  test("every CoreOperation variant round-trips through the walker codec"):
    // `encodeOperation`/`encodePiece` are total over the sealed hierarchy
    // (I8), exactly like `encodeLocation` above -- this list is every
    // `CoreOperation` case. The walker's tree-control nodes are not in this
    // list because they are not `CoreOperation`s: they live under the
    // unsealed `Operation` and cannot be passed to the encoder at all.
    val events = everyOperation.map(operation =>
      recordedOperation(operation))

    val encoded = encodedStreamThatRoundTrips("walker-ops", events)
    assertEquals(ujson.read(encoded).arr.map(
      _("payload")("step")("ops")(0)("kind").str).toVector.distinct.size,
      everyOperation.map(_.getClass.getSimpleName).distinct.size)

  test("a legacy's power source and a denizen's with the same id stay " +
      "apart through the codec and replay (in-play powers ruling 6)"):
    val id = "L29"
    val sources = Vector[CardId](LegacyId(id), DenizenId(id))
      .map(PowerSourceRef.Card(_))
    assertNotEquals(sources(0), sources(1))
    val uses = sources.map(source => recordedOperation(RecordPowerUse(
      PowerUseRef(PowerTiming.Wake, source, PowerId("legacy.ruthless")))))
    assertWireRoundTrips(uses)
    val encoded = encodedStreamThatRoundTrips("legacy-source", uses)
    assertEquals(ujson.read(encoded).arr.map(
      _("payload")("step")("ops")(0)("cardKind").str).toVector,
      Vector("legacy", "denizen"))

  /** The family an operation belongs to. Exhaustive, so a new family fails
    * to compile here until it is named. */
  private def familyOf(operation: CoreOperation): String = operation match
    case _: PieceOperation => "piece"
    case _: ResourceOperation => "resource"
    case _: CardOperation => "card"
    case _: DiceOperation => "dice"
    case _: TurnOperation => "turn"
    case _: ChronicleOperation => "chronicle"

  test("each wire kind belongs to one operation family"):
    val kinds: Map[String, Set[String]] = everyOperation.groupMap(familyOf)(
      operation => ujson.read(accepted(GameEventWire.encodeEvent("walker", 0,
        recordedOperation(operation))))("payload")("step")("ops")(0)("kind")
        .str).view.mapValues(_.toSet).toMap
    assertEquals(kinds.keySet,
      Set("piece", "resource", "card", "dice", "turn", "chronicle"))
    for
      (family, own) <- kinds
      (other, theirs) <- kinds
      if family < other
    do assertEquals(own.intersect(theirs), Set.empty[String],
      s"$family and $other share wire kinds")

  test("a walker roll payload round-trips attack and defense faces"):
    val events = Vector[OathEvent](
      WalkerStepRecorded("1", RollPayload(PoolKey("campaign.attack"),
        Vector(AttackDieFace.HollowSword, AttackDieFace.OneSword,
          AttackDieFace.TwoSwordsSkull)), Vector.empty),
      WalkerStepRecorded("2", RollPayload(PoolKey("campaign.defense"),
        Vector(DefenseDieFace.Doubler, DefenseDieFace.Blank),
        automatic = true), Vector.empty))
    val encoded = encodedStreamThatRoundTrips("walker-rolls", events)
    // `automatic` is written only when set, so a parked roll's wire form is
    // unchanged.
    assertEquals(ujson.read(encoded).arr.map(
      _("payload")("step").obj.contains("automatic")).toVector,
      Vector(false, true))

  test("a walker roll payload round-trips round faces, and a round Roll " +
      "keeps its die"):
    val events = Vector[OathEvent](
      WalkerStepRecorded("1", RollPayload(PoolKey("shattered-portal"),
        Vector(RoundDieFace.One, RoundDieFace.Six), automatic = true),
        Vector.empty),
      recordedOperation(Roll(PoolKey("shattered-portal"),
        DiceSpec(DiceKind.Round))))
    val encoded = encodedStreamThatRoundTrips("walker-round", events)
    val written = ujson.read(encoded).arr
    assertEquals(written(0)("payload")("step")("faces"),
      ujson.Arr("round-1", "round-6"))
    assertEquals(written(1)("payload")("step")("ops")(0)("die").str, "round")
    assert(GameEventWire.decodeStream(encoded.replace("round-6", "round-7"))
      .isLeft)

  test("every Piece variant round-trips through the walker codec"):
    // `encodePiece` is fully total (I8): unlike `encodeOperation`, it has no
    // throwing arm at all, so this list is the whole of `Piece`. `Take`
    // carries a bare `Piece`, so it exercises each variant the same way
    // `PayCost` exercises `Location` above.
    val player = PlayerId("red")
    val relic = RelicId("relic")
    val lineage = LineageId("lineage")
    val pieces: Vector[Piece] = Vector(
      Piece.Card(relic),
      Piece.Banner(Banner.PeoplesFavor),
      Piece.Pawn(player),
      Piece.Favor(1),
      Piece.Secrets(2),
      Piece.Warbands(ForceKind.Exile(lineage), 3))
    val events = pieces.map(piece => recordedOperation(Take(piece, player,
      Location.SharedBank, Location.PlayArea(player))))

    val encoded = encodedStreamThatRoundTrips("walker-pieces", events)
    assertEquals(ujson.read(encoded).arr.map(
      _("payload")("step")("ops")(0)("piece")("kind").str).toVector.distinct.size,
      pieces.map(_.getClass.getSimpleName).distinct.size)
