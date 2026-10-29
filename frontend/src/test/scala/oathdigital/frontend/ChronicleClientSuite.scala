package oathdigital.frontend

import oathdigital.frontend.ChronicleFixtures.{convertingTransport, entry}
import oathdigital.protocol.{ChronicleConvertRequest,
  ChronicleConvertRequestCodec}
import oathdigital.testkit.Unwrap.{accepted, present}
import scala.collection.mutable.ArrayBuffer
import scala.concurrent.Future
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

/** The convert route as the browser calls it, and renaming through it. */
class ChronicleClientSuite extends munit.FunSuite:
  /** A client whose every request is answered with `status` and `body`. */
  private def clientAnswering(status: Int, body: String): ChronicleClient =
    ChronicleClient(ScriptedTransport((_, _, _) =>
      Future.successful(Right(TransportResponse(status, body)))))

  test("convert posts the request and decodes the answer"):
    val requests = ArrayBuffer.empty[RecordedRequest]
    val client = ChronicleClient(convertingTransport(requests))
    client.convert(ChronicleConvertRequest.FromTts("abc", "Iron Vow")).map {
      result =>
        assertEquals(accepted(result).entry.summary.name, "Iron Vow")
        assertEquals(accepted(result).tts, Some("TTS-STRING"))
        assertEquals(requests.map(r => r.method -> r.url).toVector,
          Vector("POST" -> "/chronicles/convert"))
    }

  test("a refusal answers the server's reason"):
    clientAnswering(422,
      """{"error":"invalid-chronicle","message":"$.era: bad"}""")
      .convert(ChronicleConvertRequest.FromJson("{}"))
      .map(result => assertEquals(result, Left("$.era: bad")))

  test("a transport failure answers its message"):
    ChronicleClient(ScriptedTransport((_, _, _) => Future.successful(
      Left(GameClientFailure.NetworkFailure("offline")))))
      .convert(ChronicleConvertRequest.FromJson("{}"))
      .map(result => assertEquals(result, Left("offline")))

  test("an answer that does not decode says so"):
    clientAnswering(200, "{}").convert(ChronicleConvertRequest.FromJson("{}"))
      .map(result => assert(result.left.exists(
        _.startsWith("The server's answer could not be read")), result))

  test("rename rewrites the JSON's name and returns the server's entry"):
    val requests = ArrayBuffer.empty[RecordedRequest]
    val client = ChronicleClient(convertingTransport(requests))
    client.rename(entry("Ashen Crown"), "  Iron Vow ").map { result =>
      assertEquals(accepted(result).summary.name, "Iron Vow")
      accepted(ChronicleConvertRequestCodec.decode(
        present(requests.head.body, "a request body"))) match
        case ChronicleConvertRequest.FromJson(json) =>
          assertEquals(ujson.read(json)("name").str, "Iron Vow")
        case other => fail(other.toString)
    }

  test("rename of JSON that does not parse answers without a request"):
    val requests = ArrayBuffer.empty[RecordedRequest]
    ChronicleClient(convertingTransport(requests))
      .rename(entry("A", json = Some("not json")), "B").map { result =>
        assertEquals(result, Left("The saved Chronicle could not be read."))
        assertEquals(requests.size, 0)
      }
