package oathdigital.application.gamelog

import oathdigital.model._
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
      .pawn(p1, "Ancient City").pawn(p2, "Broken Peaks")
      .pawn(p3, "Buried Giant").oathkeeper(Some(p1), TitleSide.Usurper)
      .turn(p3, Phase.Act).banditsRefilled)
    acting.after(MenuPlay.start(p3, PhaseTransitionRef.BeginRest))
    Script("chronicle-started", service, p1)

  val named: Vector[(String, () => Script)] =
    Vector("chronicle-started" -> (() => chronicleStarted))
