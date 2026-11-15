package distantlands.serialization

import scala.util.control.NonFatal
import distantlands.model._
import distantlands.serialization.WireError.InvalidValue

/** Journal wire form of negotiation terms, the one spelling the walker's
  * answer codec writes and reads.
  */
private[serialization] object NegotiationTermsCodec:
  import GameEventJsonSupport.{decodeWorldCardId, encodeCardId, traverse}
  def encode(terms: NegotiationTerms): ujson.Value = ujson.Obj(
    "transfers" -> ujson.Arr.from(terms.transfers.map(transfer => ujson.Obj(
      "recipientPlayerId" -> transfer.recipient.value,
      "favor" -> transfer.favor,
      "relicIds" -> ujson.Arr.from(transfer.relics.map(r => ujson.Str(r.value))),
      "advisers" -> ujson.Arr.from(transfer.advisers.map(encodeCardId)),
      "siteIds" -> ujson.Arr.from(transfer.sites.map(site =>
        ujson.Str(site.value))),
      "secrets" -> transfer.secrets))),
    "disclosures" -> ujson.Arr.from(terms.disclosures.map { disclosure =>
      val information = disclosure.information match {
        case NegotiationDisclosureRef.Adviser(owner, card) => ujson.Obj(
          "kind" -> "adviser", "ownerPlayerId" -> owner.value,
          "card" -> encodeCardId(card))
        case NegotiationDisclosureRef.HeldRelic(owner, relic) => ujson.Obj(
          "kind" -> "held-relic", "ownerPlayerId" -> owner.value,
          "relicId" -> relic.value)
        case NegotiationDisclosureRef.SiteRelic(site, relic) => ujson.Obj(
          "kind" -> "site-relic", "siteId" -> site.value,
          "relicId" -> relic.value)
      }
      ujson.Obj("recipientPlayerId" -> disclosure.recipient.value,
        "information" -> information)
    }),
    "citizenship" -> ujson.Arr.from(terms.citizenship.map(p =>
      ujson.Str(p.value))),
    "promises" -> ujson.Arr.from(terms.promises.map(ujson.Str(_))),
    "bindingPromises" -> ujson.Arr.from(terms.bindingPromises.map(ujson.Str(_))))

  def decode(value: ujson.Value, path: String)
      : Either[WireError, NegotiationTerms] = try
    for
      transfers <- traverse(value("transfers").arr.toVector) { row =>
        val raw = row("favor").num
        val rawSecrets = row("secrets").num
        for
          favor <- Either.cond(raw.isValidInt, raw.toInt, InvalidValue(
            s"$path.transfers.favor", s"favor '$raw' is not an integer"))
          secrets <- Either.cond(rawSecrets.isValidInt, rawSecrets.toInt,
            InvalidValue(s"$path.transfers.secrets",
              s"secrets '$rawSecrets' is not an integer"))
          advisers <- traverse(row("advisers").arr.toVector)(card =>
            decodeWorldCardId(card, s"$path.transfers.advisers"))
        yield NegotiationTransfer(PlayerId(row("recipientPlayerId").str),
          favor, row("relicIds").arr.toVector.map(v => RelicId(v.str)),
          advisers, row("siteIds").arr.toVector.map(v => SiteId(v.str)),
          secrets)
      }
      disclosures <- traverse(value("disclosures").arr.toVector) { row =>
        val info = row("information")
        val decoded: Either[WireError, NegotiationDisclosureRef] =
          info("kind").str match
            case "adviser" => decodeWorldCardId(info("card"),
              s"$path.disclosures.card").map(card =>
              NegotiationDisclosureRef.Adviser(
                PlayerId(info("ownerPlayerId").str), card))
            case "held-relic" => Right(NegotiationDisclosureRef.HeldRelic(
              PlayerId(info("ownerPlayerId").str), RelicId(info("relicId").str)))
            case "site-relic" => Right(NegotiationDisclosureRef.SiteRelic(
              SiteId(info("siteId").str), RelicId(info("relicId").str)))
            case other => Left(InvalidValue(s"$path.disclosures.kind",
              s"unknown disclosure kind '$other'"))
        decoded.map(NegotiationDisclosure(
          PlayerId(row("recipientPlayerId").str), _))
      }
    yield NegotiationTerms(transfers, disclosures,
      value("citizenship").arr.toVector.map(v => PlayerId(v.str)),
      value("promises").arr.toVector.map(_.str),
      value("bindingPromises").arr.toVector.map(_.str))
  catch { case NonFatal(error) => Left(InvalidValue(path,
    Option(error.getMessage).getOrElse("invalid Negotiation terms"))) }
