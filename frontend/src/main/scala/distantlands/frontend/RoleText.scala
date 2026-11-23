package distantlands.frontend

import distantlands.frontend.ServerUiSupport.text

import org.scalajs.dom

/** A role's name as a label. The Chancellor and the Citizens are the
  * Empire, so their role reads in the Empire's purple, the colour its
  * warbands take; an Exile's role does not.
  */
object RoleText:
  /** Whether `role` ("chancellor", "Citizen", ...) is one of the Empire's. */
  def imperial(role: String): Boolean =
    Set("chancellor", "citizen").contains(role.toLowerCase)

  /** A span reading `role`, with `className`, marked `role-imperial` when
    * the role is the Empire's. */
  def span(role: String, className: String = ""): dom.Element =
    val classes = (className +: Option.when(imperial(role))("role-imperial").toSeq)
      .filter(_.nonEmpty).mkString(" ")
    text("span", classes, role)
