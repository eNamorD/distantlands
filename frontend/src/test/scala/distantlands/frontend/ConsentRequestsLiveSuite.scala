package distantlands.frontend

import distantlands.frontend.Dom.{all, one}
import distantlands.model.PlayerColor
import distantlands.protocol.{GameIntent => Intent}
import org.scalajs.dom

/** The requests list stays live while the overlay is open (reconsider
  * design, "Live while open"): redrawn from each projection, while the
  * rest of the overlay ignores polls. Red rules the Narrow Pass. */
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
    def snapshot(value: GameProjection): Unit =
      overlay.showConsentRequests(links(value))
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

  test("the reader's scroll is kept across a redraw"):
    withOverlay(tableWhereRedAnswered(row("refused", "open"))) { open =>
      val body = one(open.root, ".card-overlay-body")
      body.scrollTop = 40
      open.snapshot(tableWhereRedAnswered(row("allowed", "open")))
      assertEquals(body.scrollTop, 40.0)
    }

  test("the same rows are not redrawn, so an open hover note survives a " +
      "poll; a click is redrawn at the next snapshot (R20)"):
    withOverlay(tableWhereRedAnswered(row("refused", "open"))) { open =>
      val before = open.change
      open.snapshot(tableWhereRedAnswered(row("refused", "open")))
      assert(open.change eq before)
      before.click()
      assert(open.change.disabled)
      // The server refused: the same rows come back, and the button with
      // them.
      open.snapshot(tableWhereRedAnswered(row("refused", "open")))
      assert(!open.change.disabled)
      assertEquals(open.sent.size, 1)
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
    overlay.showConsentRequests(links)
    overlay.showText("Oath", Vector("Rules the most sites"), opener)
    overlay.showConsentRequests(links)
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
        row("refused", "open")), None))
      assertEquals(all(browser.mount, ".consent-request-asked")
        .map(_.textContent), Vector("Blue asked to select Deep Woods"))
    finally
      shell.dispose()
      browser.close()
