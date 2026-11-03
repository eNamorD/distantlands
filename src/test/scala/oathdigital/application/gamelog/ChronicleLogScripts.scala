package oathdigital.application.gamelog

import oathdigital.gameplay.cards.{AncientCitySite, BrokenPeaksSite,
  BuriedGiantSite}
import oathdigital.gameplay.chronicle.OfferCitizenship
import oathdigital.model._
import oathdigital.model.DecisionAnswer.{ChooseManyAnswer, ChooseOneAnswer}
import oathdigital.testkit.{MenuPlay, Table}
import oathdigital.testkit.Table.{p1, p2, p3}
import LogScripts.atTable

/** The game's end and the Chronicle Phase (Chronicle steps design). Kept
  * beside `LogScripts`, which is at its size limit. */
object ChronicleLogScripts:
  /** p3 rests; p1, the Usurper, wins as their Wake begins. The script's
    * default answer has p1 start the Chronicle, which journals the baseline
    * unchanged. */
  def chronicleStarted(using munit.Location): Script =
    val (service, acting) = atTable("chronicle-started", Table.start
      .pawn(p1, AncientCitySite).pawn(p2, BrokenPeaksSite)
      .pawn(p3, BuriedGiantSite).oathkeeper(Some(p1), TitleSide.Usurper)
      .turn(p3, Phase.Act).banditsRefilled)
    acting.after(MenuPlay.start(p3, PhaseTransitionRef.BeginRest))
    Script("chronicle-started", service, p1)

  /** The same game, with p2 ruling Broken Peaks: at the Throne p1, the new
    * Chancellor, offers p2 Citizenship and p2 accepts (Empire slice 3b). */
  def throneOffered(using munit.Location): Script =
    val (service, acting) = atTable("throne-offered", Table.start
      .pawn(p1, AncientCitySite).pawn(p2, BrokenPeaksSite)
      .pawn(p3, BuriedGiantSite).oathkeeper(Some(p1), TitleSide.Usurper)
      .warbandsAt(BrokenPeaksSite, p2, 2)
      .turn(p3, Phase.Act).banditsRefilled)
    acting.withAnswers {
      case park if park.decisionId == OfferCitizenship.offerDecisionId =>
        ChooseManyAnswer(Vector(DecisionOptionRef.Player(p2)))
      case park if park.decisionId == OfferCitizenship.answerDecisionId(p2) =>
        ChooseOneAnswer(OfferCitizenship.accept)
    }.after(MenuPlay.start(p3, PhaseTransitionRef.BeginRest))
    Script("throne-offered", service, p1)

  val named: Vector[(String, () => Script)] =
    Vector("chronicle-started" -> (() => chronicleStarted),
      "throne-offered" -> (() => throneOffered))
