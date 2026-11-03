package oathdigital.server

import oathdigital.model.{PlayerColor, TriggeredProcedureRef}

import java.nio.file.{Files, Path}
import java.sql.{Connection, DriverManager}
import oathdigital.application._
import oathdigital.application.chronicle.{ChronicleConversion, ChronicleEntries}
import oathdigital.gameplay.chronicle.EvolvedChronicleFixture
import oathdigital.gameplay.powers.{PhasePowerCatalog, WalkerPowerCatalog}
import oathdigital.gameplay.setup.FirstGameSetupFixture.{catalog, chronicle,
  orders, participants}
import oathdigital.gameplay.setup.SetupProcedure
import oathdigital.gameplay.walker.ParkedDecisionAssertions
import oathdigital.persistence.HsqldbDatabaseOwner
import oathdigital.protocol._
import oathdigital.testkit.Unwrap.{accepted, present}

class TrustedGameProvisioningSuite extends munit.FunSuite:
  /** The parked decision, as this suite rebuilds it: the same catalog and
    * power catalogs `GameApplicationService` builds its rules with
    * (`GameApplicationService.scala:87-90`).
    */
  private val parkedAssertions = new ParkedDecisionAssertions(catalog,
    WalkerPowerCatalog.default(catalog), PhasePowerCatalog.default(catalog))

  private val gameId = "trusted-game"
  private val request = TrustedGameCreateRequest(Vector(
    BootstrapParticipantRequest("p1", PlayerColor.Red),
    BootstrapParticipantRequest("p2", PlayerColor.Blue)),
    ChronicleRequest.New("Ashen Crown"))
  private val codes = Vector("AAAAAAAAAAAAAAAAAAAAAA", "AQEBAQEBAQEBAQEBAQEBAQ",
    "AgICAgICAgICAgICAgICAg", "AwMDAwMDAwMDAwMDAwMDAw")
    .map(raw => accepted(SeatCode.parse(raw)))

  private def withDatabase(body: (HsqldbDatabaseOwner, Connection) => Unit): Unit =
    val path: Path = Files.createTempDirectory("trusted-provisioning-").resolve("games")
    val owner = accepted(HsqldbDatabaseOwner.open(path))
    val connection = DriverManager.getConnection(s"jdbc:hsqldb:file:$path", "SA", "")
    try body(owner, connection)
    finally { connection.close(); owner.close() }

  private def provision(owner: HsqldbDatabaseOwner, generate: () => SeatCode,
      gameIds: () => String = () => gameId) =
    new TrustedGameProvisioning(new GameApplicationService(catalog, owner.eventStreams),
      new GeneratedFirstGamePlanFactory(catalog), owner.trustedGames,
      new ChronicleConversion(catalog), generate, () => 1234L,
      gameIds)

  /** How many rows `gameId` has in each table a trusted game writes. */
  private final case class StoredRows(resources: Int, seats: Int, streams: Int,
      entries: Int)

  private val nothingStored = StoredRows(0, 0, 0, 0)

  /** A two-seat game begun with two events: one resource, two seats, one
    * stream, and its GameStarted and WalkerParked entries. */
  private val twoSeatGameStored = StoredRows(1, 2, 1, 2)

  private def rows(connection: Connection, gameId: String): StoredRows =
    def count(table: String): Int =
      val statement = connection.prepareStatement(
        s"SELECT COUNT(*) FROM $table WHERE game_id = ?")
      try
        statement.setString(1, gameId)
        val result = statement.executeQuery()
        result.next()
        result.getInt(1)
      finally statement.close()
    StoredRows(count("game_resources"), count("trusted_seats"),
      count("event_streams"), count("event_entries"))

  test("creates ordered links, digest-only seats and replayable journal in one commit"):
    withDatabase { (owner, connection) =>
      val generated = codes.iterator
      val response = provision(owner, () => generated.next()).create(request, "https://games.example.test")
      assertEquals(response.map(created => (created.gameId, created.seats)),
        Right(("trusted-game", Vector(
        TrustedSeatLink("p1", "https://games.example.test/s/AAAAAAAAAAAAAAAAAAAAAA"),
        TrustedSeatLink("p2", "https://games.example.test/s/AQEBAQEBAQEBAQEBAQEBAQ")))))
      // GameStarted plus the WalkerParked fact from Setup's immediate first
      // park (2026-09-21 Chronicle design, slice 2): two event entries for
      // one bootstrap, not the legacy one.
      assertEquals(rows(connection, gameId), twoSeatGameStored)
      codes.take(2).zipWithIndex.foreach { case (code, index) =>
        assertEquals(owner.identities.resolveTrustedSeat(code.digest),
          Right(TrustedSeat(gameId, s"p${index + 1}")))
      }
      val statement = connection.createStatement()
      try
        val result = statement.executeQuery("SELECT token_digest, created_at_millis FROM trusted_seats ORDER BY player_id")
        var index = 0
        while result.next() do
          assertEquals(result.getBytes(1).toVector, codes(index).digest.bytes)
          assertEquals(result.getLong(2), 1234L)
          index += 1
      finally statement.close()
      val loaded = new GameApplicationService(catalog, owner.eventStreams).load(gameId)
      assertEquals(loaded.toOption.flatten.map(_.nextSequence), Some(2L))
      val journal = present(accepted(owner.eventStreams.load(gameId)),
        s"stream $gameId").records.mkString
      codes.foreach(code => assert(!journal.contains(code.raw)))
    }

  test("a taken game ID is drawn again, keeping the seat codes"):
    withDatabase { (owner, connection) =>
      val first = codes.iterator
      accepted(provision(owner, () => first.next()).create(request, "https://games.test"))
      val ids = Iterator(gameId, "fresh")
      val second = codes.drop(2).iterator
      val response = provision(owner, () => second.next(), () => ids.next())
        .create(request, "https://games.test")
      assertEquals(response.map(_.gameId), Right("fresh"))
      assertEquals(response.map(_.seats.map(_.url)), Right(codes.drop(2).map(code =>
        s"https://games.test/s/${code.raw}")))
      assertEquals(rows(connection, "fresh"), twoSeatGameStored)
    }

  test("generated game IDs are distinct and follow the identifier rule"):
    val ids = Vector.fill(50)(TrustedGameProvisioning.generateGameId())
    assert(ids.forall(_.matches("game-[a-z2-7]{12}")), ids)
    assertEquals(ids.distinct.size, ids.size)

  test("eight taken game IDs and persistent digest collision return generic failures without partial rows"):
    withDatabase { (owner, connection) =>
      val generated = codes.iterator
      accepted(provision(owner, () => generated.next()).create(request, "http://localhost:8080"))
      val before = rows(connection, gameId)
      val duplicates = codes.drop(2).iterator
      var draws = 0
      assertEquals(provision(owner, () => duplicates.next(), () => { draws += 1; gameId })
        .create(request, "http://localhost:8080"), Left(TrustedGameFailure.DuplicateGame))
      assertEquals(draws, TrustedGameProvisioning.MaxGameIdAttempts)
      assertEquals(rows(connection, gameId), before)
      // The second game draws the first game's p1 code again for its p2.
      val firstGameP1Code = codes.head
      val collisionCodes = Vector(codes(2), firstGameP1Code).iterator
      val collision = provision(owner, () => collisionCodes.next(), () => "collision").create(
        request, "http://localhost:8080")
      assertEquals(collision, Left(TrustedGameFailure.CodeCollision))
      assertEquals(rows(connection, "collision"), nothingStored)
      codes.foreach(code => assert(!collision.toString.contains(code.raw)))
    }

  test("duplicate generated codes retry per seat with an eight-attempt terminal limit"):
    withDatabase { (owner, connection) =>
      val generated = Vector(codes.head, codes.head, codes(1)).iterator
      accepted(provision(owner, () => generated.next()).create(request, "https://games.test"))
      var count = 0
      val failed = provision(owner, () => { count += 1; codes(2) }, () => "exhausted").create(
        request, "https://games.test")
      assertEquals(failed, Left(TrustedGameFailure.CodeCollision))
      // p1 takes the code once; p2 then collides on every draw until
      // TrustedGameProvisioning's per-seat limit of 8 attempts.
      val perSeatAttempts = 8
      assertEquals(count, 1 + perSeatAttempts)
      assertEquals(rows(connection, "exhausted"), nothingStored)
    }

  test("event insertion failure rolls back game resource, seats and stream"):
    withDatabase { (owner, connection) =>
      val statement = connection.createStatement()
      try statement.execute("ALTER TABLE event_entries ADD CONSTRAINT fail_bootstrap CHECK (sequence > 0)")
      finally statement.close()
      val generated = codes.iterator
      val failed = provision(owner, () => generated.next()).create(request, "https://games.test")
      assertEquals(failed, Left(TrustedGameFailure.StorageFailure))
      assertEquals(rows(connection, gameId), nothingStored)
      codes.foreach(code => assert(!failed.toString.contains(code.raw)))
    }

  test("invalid bootstrap semantics and origins never persist anything or generate codes"):
    withDatabase { (owner, connection) =>
      val service = provision(owner, () => fail("must validate before code generation"))
      Vector(request.copy(participants = Vector.empty),
        request.copy(participants = request.participants.updated(1,
          request.participants(1).copy(color = PlayerColor.Red)))).foreach { invalid =>
        assertEquals(service.create(invalid, "https://games.test"), Left(TrustedGameFailure.InvalidRequest))
        assertEquals(rows(connection, gameId), nothingStored)
      }
      Vector("https://user:secret@games.test", "https://games.test/path", "https://games.test?q=secret",
        "ftp://games.test", "https://games.test/#secret").foreach { origin =>
        assertEquals(service.create(request, origin), Left(TrustedGameFailure.InvalidRequest))
      }
      assertEquals(rows(connection, gameId), nothingStored)
    }

  test("a new Chronicle starts the game under its name, and the response " +
      "carries its entry"):
    withDatabase { (owner, _) =>
      val generated = codes.iterator
      val created = accepted(provision(owner, () => generated.next())
        .create(request, "http://127.0.0.1:8080"))
      assertEquals(created.chronicle.summary.name, "Ashen Crown")
      assertEquals(created.chronicle.summary.game, Some(1))
      assertEquals(created.chronicle.digest,
        ChronicleEntries.digest(created.chronicle.json))
    }

  test("a new name is trimmed, and a saved first-game Chronicle starts " +
      "the game as it is"):
    withDatabase { (owner, _) =>
      val generated = codes.iterator
      val saved = ChronicleEntries().entry(chronicle)
      val padded = accepted(provision(owner, () => generated.next(),
        () => "padded").create(request.copy(chronicle =
        ChronicleRequest.New("  Ashen Crown ")), "http://127.0.0.1:8080"))
      assertEquals(padded.chronicle.summary.name, "Ashen Crown")
      val created = accepted(provision(owner, () => generated.next()).create(
        request.copy(chronicle = ChronicleRequest.Saved(saved.json)),
        "http://127.0.0.1:8080"))
      assertEquals(created.chronicle, saved)
    }

  test("an evolved Chronicle is refused with game start's reason, and " +
      "unreadable JSON and an overlong name with theirs, storing nothing"):
    withDatabase { (owner, connection) =>
      val service = provision(owner, () => codes.head)
      val evolved = ChronicleEntries().entry(EvolvedChronicleFixture.record)
      assertEquals(service.create(
        request.copy(chronicle = ChronicleRequest.Saved(evolved.json)),
        "http://127.0.0.1:8080"),
        Left(TrustedGameFailure.Refused(
          "an Empire-less later game requires Wide Horizons")))
      service.create(request.copy(chronicle = ChronicleRequest.Saved("{")),
        "http://127.0.0.1:8080") match
        case Left(TrustedGameFailure.Refused(reason)) =>
          assert(reason.startsWith("not a Chronicle file"), reason)
        case other => fail(other.toString)
      assertEquals(service.create(
        request.copy(chronicle = ChronicleRequest.New("a" * 81)),
        "http://127.0.0.1:8080"),
        Left(TrustedGameFailure.Refused(
          "a Chronicle name has at most 80 characters")))
      // The protocol already refuses a blank name.
      assertEquals(service.create(
        request.copy(chronicle = ChronicleRequest.New("   ")),
        "http://127.0.0.1:8080"), Left(TrustedGameFailure.InvalidRequest))
      assertEquals(rows(connection, gameId), nothingStored)
    }

  test("code generator exceptions are generic and cannot leak credentials"):
    withDatabase { (owner, connection) =>
      val failed = provision(owner, () => throw new IllegalStateException(codes.head.raw))
        .create(request, "https://games.test")
      assertEquals(failed, Left(TrustedGameFailure.StorageFailure))
      assert(!failed.toString.contains(codes.head.raw))
      assertEquals(rows(connection, gameId), nothingStored)
    }

  test("bootstrap preparation performs no repository IO, matches ordinary " +
      "handle and parks on the first player's pawn placement"):
    // Stub of EventStreamRepository: any load or append fails the test.
    val untouched = new EventStreamRepository:
      def load(id: String): Either[RepositoryFailure, Option[StoredEventStream]] = fail("preparation must not read storage")
      def append(id: String, expected: ExpectedStream, records: Vector[String]): Either[RepositoryFailure, RepositoryAppendResult] =
        fail("preparation must not write storage")
    val config = FirstGameBootstrapConfig(participants, orders.firstPlayer)
    val dealt = ChronicleFirstGamePlan.dealOrder(config,
      chronicle.worldDeck, chronicle.relicDeck)
    val prepared = accepted(new GameApplicationService(catalog, untouched)
      .prepareBootstrap("prepared", chronicle, dealt))
    val journal = new InMemoryEventStreamRepository
    val handled = accepted(new GameApplicationService(catalog, journal)
      .handle("prepared", 0L, GameCommand.Begin(chronicle, dealt)))
    assertEquals(prepared.state, handled.state)
    assertEquals(prepared.events, handled.events)
    parkedAssertions.assertParked(handled.state, TriggeredProcedureRef.Setup,
      SetupProcedure.pawnDecisionId(orders.firstPlayer), orders.firstPlayer)
    assertEquals(prepared.records,
      present(accepted(journal.load("prepared")), "stream prepared").records)

  test("store rejects invalid seat and record input without throwing or persisting rows"):
    withDatabase { (owner, connection) =>
      assertEquals(owner.trustedGames.create("invalid", Vector(codes.head.digest -> null),
        Vector("record"), 0L), Left(TrustedGameStoreFailure.InvalidInput))
      assertEquals(owner.trustedGames.create("invalid", Vector(codes.head.digest -> "p1"),
        Vector.empty, 0L), Left(TrustedGameStoreFailure.InvalidInput))
      assertEquals(rows(connection, "invalid"), nothingStored)
    }

  test("store preserves record order and rolls back even after an earlier event row was inserted"):
    withDatabase { (owner, connection) =>
      assertEquals(owner.trustedGames.create("ordered", Vector(codes.head.digest -> "p1"),
        Vector("first", "second", "third"), 0L), Right(()))
      assertEquals(owner.eventStreams.load("ordered").toOption.flatten,
        Some(StoredEventStream("ordered", Vector("first", "second", "third"))))
      val statement = connection.createStatement()
      try statement.execute("ALTER TABLE event_entries ADD CONSTRAINT fail_second CHECK (game_id <> 'partial' OR sequence = 0)")
      finally statement.close()
      assertEquals(owner.trustedGames.create("partial", Vector(codes(1).digest -> "p1"),
        Vector("first", "second"), 0L), Left(TrustedGameStoreFailure.StorageFailure))
      assertEquals(rows(connection, "partial"), nothingStored)
    }
