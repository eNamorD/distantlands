package distantlands.protocol

private[protocol] object CommandIntentDecoders:
  import CommandJsonSupport._
  import GameIntent._
  import ProtocolDecodeFailure._

  def decode(kind: String, value: ujson.Obj, path: String)
      : Either[ProtocolDecodeFailure, GameIntent] = kind match
    case "undo" => empty(value, path, Undo)
    case "redo" => empty(value, path, Redo)
    case "resolveWalker" =>
      for
        _ <- exact(value, Set("type", "decisionId", "payload"), path)
        id <- string(value, "decisionId", path)
        payload <- field(value, "payload", path).flatMap(
          CommandNestedCodecs.decodeDecisionAnswerWire(_, s"$path.payload"))
      yield ResolveWalker(id, payload)
    case "requestConsent" =>
      for
        _ <- exact(value, Set("type", "decisionId", "target", "granters"),
          path)
        id <- string(value, "decisionId", path)
        target <- field(value, "target", path).flatMap(raw =>
          CommandNestedCodecs.decodeStartArgsWire(ujson.Arr(raw),
            s"$path.target"))
          .flatMap:
            case Vector(one) => Right(one)
            case _ => Left(InvalidValue(s"$path.target",
              "expected one consent target"))
        granters <- value.value.get("granters").fold(
          Right(Vector.empty))(strings(_, s"$path.granters"))
      yield RequestConsent(id, target, granters)
    case "answerConsent" => one(value, path, "choice")(AnswerConsent.apply)
    case "setConsentPreference" =>
      for
        _ <- exact(value, Set("type", "powerId", "requesterId", "setting"),
          path)
        power <- string(value, "powerId", path)
        requester <- string(value, "requesterId", path)
        setting <- string(value, "setting", path)
      yield SetConsentPreference(power, requester, setting)
    case "savePlanAid" =>
      def amount(name: String) = field(value, name, path).flatMap(
        integer(_, s"$path.$name"))
      for
        _ <- exact(value, Set("type", "plans", "requirements", "favor",
          "secrets", "favorBurnt", "secretsBurnt"), path)
        plans <- field(value, "plans", path).flatMap(
          CommandNestedCodecs.decodeStartArgsWire(_, s"$path.plans"))
        requirements <- field(value, "requirements", path).flatMap(
          CommandNestedCodecs.decodeStartArgsWire(_, s"$path.requirements"))
        favor <- amount("favor")
        secrets <- amount("secrets")
        favorBurnt <- amount("favorBurnt")
        secretsBurnt <- amount("secretsBurnt")
      yield SavePlanAid(plans, requirements, favor, secrets, favorBurnt,
        secretsBurnt)
    case other => Left(InvalidValue(s"$path.type", s"unknown intent type '$other'"))

  private def empty(value: ujson.Obj, path: String, result: GameIntent) = exact(value, Set("type"), path).map(_ => result)
  private def one(value: ujson.Obj, path: String, name: String)(f: String => GameIntent) =
    exact(value, Set("type", name), path).flatMap(_ => string(value, name, path)).map(f)
