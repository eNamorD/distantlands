package oathdigital.frontend

import oathdigital.protocol.{ChronicleConvertRequest,
  ChronicleConvertRequestCodec, ChronicleConvertResponse,
  ChronicleConvertResponseCodec, ChronicleEntry}
import scala.concurrent.Future
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.util.Try

/** The browser's one Chronicle route, `POST /chronicles/convert` (Chronicle
  * browsing design, "Server"). Every failure comes back as the words the
  * page shows. */
private[frontend] final class ChronicleClient(transport: JsonTransport):
  def convert(request: ChronicleConvertRequest)
      : Future[Either[String, ChronicleConvertResponse]] =
    transport.request("POST", "/chronicles/convert",
        Some(ChronicleConvertRequestCodec.encode(request)))
      .map(_.left.map(_.message).flatMap { response =>
        if response.status >= 200 && response.status < 300 then
          ChronicleConvertResponseCodec.decode(response.body).left.map(error =>
            s"The server's answer could not be read: ${error.message}")
        else Left(ChronicleClient.reason(response))
      })

  /** The Chronicle under `name`: the browser changes the JSON's name and the
    * server returns the new canonical JSON, digest and summary. */
  def rename(entry: ChronicleEntry, name: String)
      : Future[Either[String, ChronicleEntry]] =
    ChronicleClient.withName(entry.json, name.trim) match
      case Left(reason) => Future.successful(Left(reason))
      case Right(json) =>
        convert(ChronicleConvertRequest.FromJson(json)).map(_.map(_.entry))

private[frontend] object ChronicleClient:
  def withName(json: String, name: String): Either[String, String] =
    Try {
      val value = ujson.read(json)
      value("name") = name
      ujson.write(value)
    }.toOption.toRight("The saved Chronicle could not be read.")

  /** A refusal's reason as the server wrote it, without the status line. */
  def reason(response: TransportResponse): String =
    GameJson.responseFailure(response) match
      case GameClientFailure.HttpFailure(_, _, detail) => detail
      case other => other.message
