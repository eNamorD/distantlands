package distantlands.frontend

import Dom.all
import org.scalajs.dom

/** The shell is the one handler `CardInspection` requests reach, so what a
  * request carries has to arrive in the overlay it mounts. Runs the real
  * shell in `TestBrowser`.
  */
class ShellInspectionSuite extends munit.FunSuite:
  test("a text request's plate reaches the overlay the shell mounts"):
    val browser = new TestBrowser()
    val shell = new GameTableShell(browser.mount, developmentTools = false,
      logStore = None)
    try
      val origin = dom.document.createElement("button")
        .asInstanceOf[dom.html.Element]
      browser.mount.appendChild(origin)
      CardInspection.openText("Oathkeeper of Supremacy",
        Vector("Rules the most sites"), origin, Some("oath-goal"))
      assertEquals(all(browser.mount, ".card-overlay .card-plate-oath-goal")
        .map(_.textContent), Vector("Oathkeeper of Supremacy"))
    finally
      shell.dispose()
      browser.close()
