package distantlands.protocol.projection

import ProjectionCodecSupport._

/** The wire of the offer panel's projection (Empire phase design, "Offer
  * panel"). */
private[projection] object PlanAidProjectionCodec:
  def encodePrompt(value: PlanAidPromptProjection): ujson.Value = ujson.Obj(
    "userPlayerId" -> value.userPlayerId, "side" -> value.side,
    "plans" -> encoded(value.plans)(ActionProjectionCodec.encodeOptionRow),
    "requirements" -> encoded(value.requirements)(
      ActionProjectionCodec.encodeOptionRow),
    "saved" -> encodeOffer(value.saved), "used" -> encodeOffer(value.used),
    "favorHeld" -> value.favorHeld, "secretsHeld" -> value.secretsHeld)

  def decodePrompt(raw: ujson.Value, path: String)
      : Result[PlanAidPromptProjection] = for
    row <- obj(raw, path)
    _ <- exact(row, Set("userPlayerId", "side", "plans", "requirements",
      "saved", "used", "favorHeld", "secretsHeld"), path)
    user <- string(row, "userPlayerId", path)
    side <- string(row, "side", path)
    plans <- options(row, "plans", path)
    requirements <- options(row, "requirements", path)
    saved <- field(row, "saved", path).flatMap(decodeOffer(_, s"$path.saved"))
    used <- field(row, "used", path).flatMap(decodeOffer(_, s"$path.used"))
    favor <- int(row, "favorHeld", path)
    secrets <- int(row, "secretsHeld", path)
  yield PlanAidPromptProjection(user, side, plans, requirements, saved, used,
    favor, secrets)

  private def options(row: ujson.Obj, name: String, path: String)
      : Result[Vector[DecisionOptionProjection]] =
    array(row, name, path).flatMap(traverse(_, s"$path.$name")(
      ActionProjectionCodec.decodeOptionRow))

  private def encodeOffer(value: PlanAidOfferProjection): ujson.Value =
    ujson.Obj("plans" -> encoded(value.plans)(ujson.Str(_)),
      "requirements" -> encoded(value.requirements)(ujson.Str(_)),
      "favor" -> value.favor, "secrets" -> value.secrets,
      "favorBurnt" -> value.favorBurnt, "secretsBurnt" -> value.secretsBurnt)

  private def decodeOffer(raw: ujson.Value, path: String)
      : Result[PlanAidOfferProjection] = for
    row <- obj(raw, path)
    _ <- exact(row, Set("plans", "requirements", "favor", "secrets",
      "favorBurnt", "secretsBurnt"), path)
    plans <- strings(row, "plans", path)
    requirements <- strings(row, "requirements", path)
    favor <- int(row, "favor", path)
    secrets <- int(row, "secrets", path)
    favorBurnt <- int(row, "favorBurnt", path)
    secretsBurnt <- int(row, "secretsBurnt", path)
  yield PlanAidOfferProjection(plans, requirements, favor, secrets,
    favorBurnt, secretsBurnt)
