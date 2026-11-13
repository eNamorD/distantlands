package distantlands.frontend

import Dom.{all, one}
import org.scalajs.dom
import scala.scalajs.js

/** What the shell itself decides for the table: the overlay a
  * `CardInspection` request reaches, and the classes the map's scale sets on
  * the map. Runs the real shell in `TestBrowser`.
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

  /** jsdom lays nothing out, so the map's viewport and content are given
    * sizes: a narrow viewport over the 2416px board fits it near 0.12. */
  private def sized(node: dom.Element, width: Double, height: Double): Unit =
    val define = js.Dynamic.global.Object.defineProperty
    Vector("clientWidth" -> width, "offsetWidth" -> width,
      "clientHeight" -> height, "offsetHeight" -> height).foreach((key, value) =>
      define(node, key, js.Dynamic.literal(configurable = true,
        get = (() => value): js.Function0[Double])))

  test("the map's scale sets the compact and tiny classes on either side " +
      "of their thresholds"):
    val browser = new TestBrowser()
    val shell = new GameTableShell(browser.mount, developmentTools = false,
      logStore = None)
    try
      val content = one(browser.mount, ".map-content")
      sized(content, 2416, 1000)
      sized(content.parentNode.parentNode.asInstanceOf[dom.Element], 300, 300)
      def button(label: String): dom.html.Element =
        one(browser.mount, s"""[aria-label="$label"]""")
          .asInstanceOf[dom.html.Element]
      button("Fit world map").click()
      val seen = (0 until 14).map { _ =>
        val scale = content.asInstanceOf[dom.html.Element].style
          .getPropertyValue("--map-scale").toDouble
        val step = (scale, content.classList.contains("map-tiny"),
          content.classList.contains("map-compact"))
        button("Zoom in on world map").click()
        step
      }
      seen.foreach((scale, tiny, compact) =>
        assertEquals((tiny, compact), (scale < 0.4, scale < 0.72), scale))
      assert(seen.exists(_._1 < 0.4) && seen.exists(_._1 >= 0.72),
        seen.map(_._1).toString)
      assert(seen.exists(step => step._1 >= 0.4 && step._1 < 0.72))
    finally
      shell.dispose()
      browser.close()
