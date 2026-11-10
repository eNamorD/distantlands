package distantlands.serialization

import distantlands.model._
import distantlands.model.OathEvent.{PlanAidCleared, PlanAidSaved}

/** The Imperial Tactics facts (Empire phase design, "1b-ii"). */
private[serialization] trait PlanAidEventCodec { this: GameEventJsonSupport =>
  import GameEventWire._

  protected final val planAidDiscriminator
      : PartialFunction[OathEvent, String] =
      case _: PlanAidSaved => PlanAidSavedType
      case PlanAidCleared => PlanAidClearedType

  protected final val planAidEncoder: PartialFunction[OathEvent, ujson.Value] =
      case PlanAidSaved(ally, window, offer) => ujson.Obj(
        "ally" -> ally.value,
        "actor" -> window.actor.value,
        "side" -> sideKey(window.side),
        "user" -> window.user.value,
        "plans" -> ujson.Arr.from(
          offer.plans.map(DecisionAnswerCodec.encodeRef)),
        "requirements" -> ujson.Arr.from(
          offer.requirements.map(DecisionAnswerCodec.encodeRef)),
        "favor" -> offer.amounts.favor,
        "secrets" -> offer.amounts.secrets,
        "favorBurnt" -> offer.amounts.favorBurnt,
        "secretsBurnt" -> offer.amounts.secretsBurnt)
      case PlanAidCleared => ujson.Obj()

  private def sideKey(side: CampaignPlanSide): String = side match
    case CampaignPlanSide.Attacker => "attacker"
    case CampaignPlanSide.Defender => "defender"

  protected final def planAidDecode(eventType: String, payload: ujson.Value,
      path: String): Option[Either[WireError, OathEvent]] =
    def player(name: String) = PlayerId(payload(name).str)
    def side: Either[WireError, CampaignPlanSide] = payload("side").str match
      case "attacker" => Right(CampaignPlanSide.Attacker)
      case "defender" => Right(CampaignPlanSide.Defender)
      case other => Left(WireError.InvalidValue(s"$path.side",
        s"unknown plan side '$other'"))
    def refs(name: String): Either[WireError, Vector[DecisionOptionRef]] =
      payload(name).arr.toVector.zipWithIndex
        .foldLeft[Either[WireError, Vector[DecisionOptionRef]]](
          Right(Vector.empty)) { case (done, (raw, index)) => for
            decoded <- done
            ref <- DecisionAnswerCodec.decodeRef(raw, s"$path.$name[$index]")
          yield decoded :+ ref }
    val decoder: PartialFunction[String, Either[WireError, OathEvent]] =
        case PlanAidSavedType =>
          for
            at <- side
            plans <- refs("plans")
            requirements <- refs("requirements")
          yield PlanAidSaved(player("ally"),
            AidWindow(player("actor"), at, player("user")),
            AidOffer(plans, requirements, AidAmounts(
              payload("favor").num.toInt, payload("secrets").num.toInt,
              payload("favorBurnt").num.toInt,
              payload("secretsBurnt").num.toInt)))
        case PlanAidClearedType => Right(PlanAidCleared)
    decoder.lift(eventType)
}
