package distantlands.frontend

import distantlands.protocol.{ChronicleConvertRequest,
  ChronicleConvertRequestCodec, ChronicleConvertResponse,
  ChronicleConvertResponseCodec, ChronicleEntry, ChronicleFoundation,
  ChronicleScore, ChronicleSummary, ChronicleEntryCodec}
import scala.collection.mutable.ArrayBuffer
import scala.concurrent.Future

/** Chronicle entries, a fixed clock, a converting server and browser
  * devices for the browsing suites. An entry's JSON is a stand-in,
  * `{"format":1,"name":…}`: the browser never reads a Chronicle's JSON
  * except to rename it. */
private[frontend] object ChronicleFixtures:
  val savedAt = "2026-10-29T12:00:00.000Z"
  val clock: () => String = () => savedAt

  def entry(name: String, digest: String = "d-1", game: Option[Int] = Some(1),
      json: Option[String] = None): ChronicleEntry =
    ChronicleEntry(
      json.getOrElse(ujson.write(ujson.Obj("format" -> 1, "name" -> name))),
      digest,
      ChronicleSummary(name, game, 20, Vector(
        ChronicleScore("red-lineage", 9), ChronicleScore("blue-lineage", 6))))

  /** `entry` listing the six foundations, those numbered in `altered` on
    * their altered face. */
  def listingFoundations(entry: ChronicleEntry, altered: Int*)
      : ChronicleEntry =
    val faces = Vector("Imperial Maps" -> "Wide Horizons",
      "Powerful Tribes" -> "Movers and Shakers",
      "Quiet Ambitions" -> "Public Ambitions",
      "Ties of Fellowship" -> "Teeming World",
      "Mob's Favor" -> "Grand Council", "Wandering Flame" -> "Festival")
    entry.copy(summary = entry.summary.copy(foundations =
      faces.zipWithIndex.map { case ((normal, turned), index) =>
        if altered.contains(index + 1) then
          ChronicleFoundation(index + 1, "altered", turned)
        else ChronicleFoundation(index + 1, "normal", normal)
      }))

  def converted(entry: ChronicleEntry, tts: Option[String] = None)
      : TransportResponse =
    TransportResponse(200, ChronicleConvertResponseCodec.encode(
      ChronicleConvertResponse(entry, tts)))

  /** The reply `POST /chronicles/convert` gives these fixtures: JSON comes
    * back under the name it carries, with digest "d-<name>"; a TTS string
    * comes back under its name with no game. */
  def convertReply(body: Option[String], tts: Option[String])
      : TransportResponse =
    ChronicleConvertRequestCodec.decode(body.getOrElse("")) match
      case Right(ChronicleConvertRequest.FromJson(json)) =>
        val name = ujson.read(json)("name").str
        converted(entry(name, s"d-$name", json = Some(json)), tts)
      case Right(ChronicleConvertRequest.FromTts(_, name)) =>
        converted(entry(name, s"d-$name", game = None), tts)
      case Left(_) =>
        TransportResponse(400, """{"error":"malformed","message":"bad"}""")

  /** Answers `POST /chronicles/convert` by `convertReply`; any other
    * request is a 404. */
  def convertingTransport(
      requests: ArrayBuffer[RecordedRequest] = ArrayBuffer.empty,
      tts: Option[String] = Some("TTS-STRING")): ScriptedTransport =
    ScriptedTransport((_, url, body) => Future.successful(Right(
      if url == "/chronicles/convert" then convertReply(body, tts)
      else TransportResponse(404,
        """{"error":"not-found","message":"no route"}"""))),
      requests)

  def devices(storage: Option[ChronicleStorage] =
        Some(new MemoryChronicleStorage()),
      files: FakeChronicleFiles = new FakeChronicleFiles(),
      history: FakePageHistory = new FakePageHistory()): ChronicleDevices =
    ChronicleDevices(storage, files, history, clock)

  /** `POST /games`'s answer: game "host-game" with seats Red and Blue, and
    * the starting Chronicle named `name`, with digest "d-start". */
  def seatLinksResponse(name: String): String =
    ujson.write(ujson.Obj("gameId" -> "host-game",
      "seats" -> ujson.Arr(
        ujson.Obj("playerId" -> "Red", "url" -> "https://oath.test/s/red-code"),
        ujson.Obj("playerId" -> "Blue",
          "url" -> "https://oath.test/s/blue-code")),
      "chronicle" -> ChronicleEntryCodec.encodeValue(
        entry(name, "d-start"))))
