package distantlands.frontend

import distantlands.frontend.Dom.{all, one}
import distantlands.model.PlayerColor
import distantlands.protocol.{GameIntent => Intent}
import org.scalajs.dom
import scala.scalajs.js

/** The requests list stays live while the overlay is open (reconsider
  * design, "Live while open"): redrawn from each projection, while the
  * rest of the overlay ignores polls. Red rules the Narrow Pass. jsdom
  * lays nothing out, so the scroll test checks that the redraw writes the
  * scroll back; that the reader's place holds on screen is the browser
  * check's. */
class ConsentRequestsLiveSuite extends munit.FunSuite:
  private val red = GamePlayer("red", "Red", "Exile", PlayerColor.Red)
  private val blue = GamePlayer("blue", "Blue", "Exile", PlayerColor.Blue)
  private val power = "site.narrow-pass.pass"
  private val pass = SitePower("pass", "Pass", Some("Printed text."), power)

  private def row(answer: String, change: String,
      optionId: String = "site:deep-woods", label: String = "Deep Woods")
      : ConsentRequestRowState =
    ConsentRequestRowState("blue", "travel.destination", "site", optionId,
      label, answer, Vector.empty, None, change)

  private def tableWhereRedAnswered(rows: ConsentRequestRowState*)
      : GameProjection =
    TestProjections.actingProjection(players = Vector(red, blue)).copy(
      consentPreferences = Vector(ConsentPreferencesState(power,
        "Narrow Pass", Vector(ConsentPreferenceRowState("blue",
          "case-by-case")), requests = rows.toVector)))

  /** An overlay open on the Pass for `first`, with what was sent. */
  private final class OpenOverlay(first: GameProjection):
    var sent = Vector.empty[Intent]
    val root: dom.Element = dom.document.createElement("div")
    dom.document.body.appendChild(root)
    val overlay = new CardInspectionOverlay(root)
    private val opener = dom.document.createElement("button")
      .asInstanceOf[dom.html.Element]
    dom.document.body.appendChild(opener)
    def links(value: GameProjection): SitePowerLinks =
      SitePowerLinks.of(value, Some(command => sent :+= command))
    overlay.showPower("Pass", Vector("Printed text."),
      links(first).grid(pass), opener)
    def snapshot(value: GameProjection, position: Long = 1L,
        failure: Option[AnyRef] = None): Unit =
      overlay.showConsentRequests(links(value), position, failure)
    def change: dom.html.Button =
      one(root, ".consent-request-change").asInstanceOf[dom.html.Button]
    def states: Vector[String] =
      all(root, ".consent-request-state").map(_.textContent)
    def close(): Unit =
      overlay.dispose()
      root.remove()
      opener.remove()

  private def withOverlay(first: GameProjection)(run: OpenOverlay => Unit)
      : Unit =
    val open = new OpenOverlay(first)
    try run(open) finally open.close()

  test("a later projection redraws the list: an entry appears, changes " +
      "and locks in place"):
    withOverlay(tableWhereRedAnswered()) { open =>
      assertEquals(open.states, Vector.empty)
      open.snapshot(tableWhereRedAnswered(row("refused", "open")))
      assertEquals(open.states, Vector("Refused"))
      open.snapshot(tableWhereRedAnswered(row("allowed", "open")))
      assertEquals((open.states, open.change.textContent),
        (Vector("Allowed"), "Refuse"))
      open.snapshot(tableWhereRedAnswered(row("allowed", "used")))
      assertEquals(open.states, Vector("Allowed, used"))
      assertEquals(open.change.getAttribute("aria-disabled"), "true")
    }

  test("the preference rows ignore the projection: a setting just chosen " +
      "stays pressed"):
    withOverlay(tableWhereRedAnswered(row("refused", "open"))) { open =>
      val always = all(open.root, ".consent-grid-choice")(2)
        .asInstanceOf[dom.html.Button]
      always.click()
      // The poll still carries the old setting.
      open.snapshot(tableWhereRedAnswered(row("allowed", "open")))
      assertEquals(all(open.root, ".consent-grid-choice")
        .map(_.getAttribute("aria-pressed")), Vector("false", "false",
        "true"))
      assert(all(open.root, ".consent-grid-choice")(2) eq always)
      assertEquals(open.states, Vector("Allowed"))
    }

  test("keyboard focus stays on the entry's button when it turns from " +
      "Allow to Refuse"):
    withOverlay(tableWhereRedAnswered(
        row("allowed", "open", optionId = "site:dunes", label = "Dunes"),
        row("refused", "open"))) { open =>
      val allow = all(open.root, ".consent-request-change")(1)
        .asInstanceOf[dom.html.Button]
      allow.focus()
      allow.click()
      open.snapshot(tableWhereRedAnswered(
        row("allowed", "open", optionId = "site:dunes", label = "Dunes"),
        row("allowed", "open")))
      val focused = dom.document.activeElement
      assertEquals(focused.textContent, "Refuse")
      assertEquals(focused.closest("[data-request-key]")
        .getAttribute("data-request-key"),
        "blue|travel.destination|site|site:deep-woods")
    }

  test("when the focused entry is gone, focus goes to Close"):
    withOverlay(tableWhereRedAnswered(row("refused", "open"))) { open =>
      open.change.focus()
      open.snapshot(tableWhereRedAnswered())
      assertEquals(dom.document.activeElement.textContent, "Close")
      assert(one(open.root, ".consent-requests").hasAttribute("hidden"))
    }

  test("a redraw puts the reader's scroll back after it rebuilds the " +
      "list"):
    withOverlay(tableWhereRedAnswered(row("refused", "open"))) { open =>
      val body = one(open.root, ".card-overlay-body")
      // jsdom lays nothing out and never moves a scroll, so the test
      // watches the writes: the browser drops the scroll when the list is
      // emptied, and the redraw must write it back.
      var top = 40.0
      var writes = Vector.empty[Double]
      js.Dynamic.global.Object.defineProperty(body, "scrollTop",
        js.Dynamic.literal(configurable = true,
          get = (() => top): js.Function0[Double],
          set = ((value: Double) => { writes :+= value; top = value })
            : js.Function1[Double, Unit]))
      open.snapshot(tableWhereRedAnswered(row("allowed", "open")))
      assertEquals(writes, Vector(40.0))
    }

  test("each overlay that draws the column keeps its list live: a ruled " +
      "card, the Festival banner and the festival marker"):
    val later = tableWhereRedAnswered(row("refused", "open"))
    def shownAfterASnapshot(show: (CardInspectionOverlay, ConsentGrid,
        dom.html.Element) => Unit): Vector[String] =
      val root = dom.document.createElement("div")
      dom.document.body.appendChild(root)
      val overlay = new CardInspectionOverlay(root)
      val opener = dom.document.createElement("button")
        .asInstanceOf[dom.html.Element]
      dom.document.body.appendChild(opener)
      val grid = SitePowerLinks.of(tableWhereRedAnswered(), None).grid(pass)
        .getOrElse(fail("no grid"))
      show(overlay, grid, opener)
      overlay.showConsentRequests(SitePowerLinks.of(later, None), 1L, None)
      val asked = all(root, ".consent-request-asked").map(_.textContent)
      overlay.dispose()
      root.remove()
      opener.remove()
      asked
    val expected = Vector("Blue asked to select Deep Woods")
    assertEquals(shownAfterASnapshot((overlay, grid, opener) =>
      overlay.showRuled(CardDetails("113", "denizen", "Secret Police"), grid,
        opener)), expected)
    assertEquals(shownAfterASnapshot((overlay, grid, opener) =>
      overlay.showFestival("Festival", Vector("Printed text."), None,
        Some(grid), opener)), expected)
    assertEquals(shownAfterASnapshot((overlay, grid, opener) =>
      overlay.showFestivalMarker(Some(grid), opener)), expected)

  test("control lost while the overlay is open disables the open " +
      "entry's button, though the rows are the same"):
    withOverlay(tableWhereRedAnswered(row("refused", "open"))) { open =>
      assert(!open.change.disabled)
      open.overlay.showConsentRequests(SitePowerLinks.of(
        tableWhereRedAnswered(row("refused", "open")), None), 1L, None)
      assert(open.change.disabled)
    }

  test("the same rows are not redrawn, so an open hover note survives a " +
      "poll"):
    withOverlay(tableWhereRedAnswered(row("refused", "open"))) { open =>
      val before = open.change
      open.snapshot(tableWhereRedAnswered(row("refused", "open")))
      assert(open.change eq before)
    }

  test("a clicked button waits through a repaint of the same position, " +
      "so a second click sends nothing (R20)"):
    withOverlay(tableWhereRedAnswered(row("refused", "open"))) { open =>
      val same = tableWhereRedAnswered(row("refused", "open"))
      open.snapshot(same, position = 7L)
      open.change.click()
      // A log page lands and repaints the same position.
      open.snapshot(same, position = 7L)
      assertEquals(open.change.getAttribute("aria-disabled"), "true")
      open.change.click()
      assertEquals(open.sent.size, 1)
    }

  test("a clicked button comes back when the server answers: with a new " +
      "position, or with a failure"):
    withOverlay(tableWhereRedAnswered(row("refused", "open"))) { open =>
      val same = tableWhereRedAnswered(row("refused", "open"))
      open.snapshot(same, position = 7L)
      open.change.click()
      open.snapshot(same, position = 8L)
      assertEquals(open.change.getAttribute("aria-disabled"), null)
      open.change.click()
      val refusal = new Object
      open.snapshot(same, position = 8L, failure = Some(refusal))
      assertEquals(open.change.getAttribute("aria-disabled"), null)
      // The same failure, still shown, answers no later click.
      open.change.click()
      open.snapshot(same, position = 8L, failure = Some(refusal))
      assertEquals(open.change.getAttribute("aria-disabled"), "true")
      open.snapshot(same, position = 8L, failure = Some(new Object))
      assertEquals(open.change.getAttribute("aria-disabled"), null)
    }

  test("a closed overlay, and one showing no column, ignore snapshots"):
    val root = dom.document.createElement("div")
    dom.document.body.appendChild(root)
    val overlay = new CardInspectionOverlay(root)
    val opener = dom.document.createElement("button")
      .asInstanceOf[dom.html.Element]
    dom.document.body.appendChild(opener)
    val links = SitePowerLinks.of(tableWhereRedAnswered(
      row("refused", "open")), None)
    overlay.showConsentRequests(links, 1L, None)
    overlay.showText("Oath", Vector("Rules the most sites"), opener)
    overlay.showConsentRequests(links, 1L, None)
    assertEquals(all(root, ".consent-requests"), Vector.empty)
    overlay.dispose()
    root.remove()
    opener.remove()

  test("the shell hands the overlay each snapshot's requests"):
    val browser = new TestBrowser()
    val shell = new GameTableShell(browser.mount, developmentTools = false,
      logStore = None)
    try
      val origin = dom.document.createElement("button")
        .asInstanceOf[dom.html.Element]
      browser.mount.appendChild(origin)
      CardInspection.openPower("Pass", Vector("Printed text."),
        SitePowerLinks.of(tableWhereRedAnswered(), None).grid(pass), origin)
      shell.showConsentRequests(SitePowerLinks.of(tableWhereRedAnswered(
        row("refused", "open")), None), 1L, None)
      assertEquals(all(browser.mount, ".consent-request-asked")
        .map(_.textContent), Vector("Blue asked to select Deep Woods"))
    finally
      shell.dispose()
      browser.close()
