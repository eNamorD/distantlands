package distantlands.application.gamelog

import distantlands.model._
import distantlands.testkit.MenuPlay
import distantlands.testkit.Table.p1
import LogScriptSupport.{atTable, tableWithOnePawnPerSite}

/** [[LogScripts]]' scripts for the Festival, the Darkest Secret's altered
  * face (Foundations design, slice 6). */
object FestivalLogScripts:
  /** p1 holds the Festival at Ancient City and ends the Act: the Rest
    * places the marker there, and the next player wakes. */
  def festivalRest(using munit.Location): Script =
    val (service, acting) = atTable("festival-rest", tableWithOnePawnPerSite
      .darkestSecret(Some(p1), 1, DarkestSecretFace.Festival)
      .banditsRefilled)
    acting.after(MenuPlay.start(p1, PhaseTransitionRef.BeginRest))
      .finishingRest(p1)
    Script("festival-rest", service, p1)

  val named: Vector[(String, () => Script)] = Vector(
    "festival-rest" -> (() => festivalRest))
