package distantlands.frontend

import Dom.{all, click, one, press}
import distantlands.model.PlayerColor
import distantlands.protocol.ChronicleScorepadEntry
import distantlands.protocol.projection.{ChronicleDetailsProjection,
  ChronicleProjection, LineageDetailsProjection}
import org.scalajs.dom

/** Chronicle Details over the table (Chronicle Details design, slice 4): a
  * modal dialog, current on every snapshot, under the card inspector. Runs
  * the real shell in `TestBrowser`. */
class ChronicleDetailsOverlaySuite extends munit.FunSuite:
  private val seer = CardDetails("L22", "legacy", "Seer",
    suit = Some("arcane"), orientation = Some("face-up"))

  /** Red's game in the Ashen Crown Chronicle: one game on the scorepad, Red
    * the Chancellor holding Seer, and an empty Atlas. */
  private val ashenCrownWithRedHoldingSeer: GameProjection =
    TestProjections.actingProjection(
      players = Vector(GamePlayer("red", "Red", "Chancellor",
        PlayerColor.Red)),
      world = Vector(GameRegion("cradle", Vector.empty))).copy(
      chronicle = Some(ChronicleProjection("Ashen Crown", "d-1", None)),
      chronicleDetails = Some(ChronicleDetailsProjection(20,
        Vector(Vector(ChronicleScorepadEntry("red-lineage", 3,
          Some("chancellor")))),
        Vector(LineageDetailsProjection("red-lineage", 3,
          Some("chancellor"), Vector(seer))), None)))

  private def withShell(body: (TestBrowser, GameTableShell) => Unit): Unit =
    val browser = new TestBrowser()
    val shell = new GameTableShell(browser.mount, developmentTools = false,
      logStore = None)
    try body(browser, shell)
    finally
      shell.dispose()
      browser.close()

  /** A snapshot of `value` reaching the shell, as `TableScreen` hands it:
    * the World pane rebuilt, then the overlay told. */
  private def snapshot(shell: GameTableShell, value: GameProjection): Unit =
    shell.update("g", "k", dom.document.createElement("div"),
      WorldBoardRenderer.world(value, BoardOptions.none),
      dom.document.createElement("div"), dom.document.createElement("div"))
    shell.showChronicleDetails(Some(value))

  private def overlay(browser: TestBrowser): dom.Element =
    one(browser.mount, "aside.chronicle-details")
  private def content(browser: TestBrowser): dom.html.Element =
    one(browser.mount, ".chronicle-details-content")
      .asInstanceOf[dom.html.Element]
  private def detailsButton(browser: TestBrowser): dom.Element =
    one(browser.mount, "button.chronicle-details-button")
  private def table(browser: TestBrowser): dom.Element =
    one(browser.mount, ".game-table")

  test("the button opens a modal dialog named by the Chronicle, the table " +
      "inert behind it and focus in its content"):
    withShell { (browser, shell) =>
      snapshot(shell, ashenCrownWithRedHoldingSeer)
      assert(overlay(browser).hasAttribute("hidden"))
      click(detailsButton(browser))
      val node = overlay(browser)
      assert(!node.hasAttribute("hidden"))
      assertEquals(Vector(node.getAttribute("role"),
        node.getAttribute("aria-modal"), node.id),
        Vector("dialog", "true", "chronicle-details"))
      assertEquals(one(browser.mount,
        s"#${node.getAttribute("aria-labelledby")}").textContent,
        "Ashen Crown")
      assert(table(browser).hasAttribute("inert"))
      assertEquals(dom.document.activeElement, content(browser))
      assertEquals(all(content(browser), ".chronicle-details-body").size, 1)
    }

  test("Escape, Close and the scrim close it, the table usable again and " +
      "focus back on the button"):
    Vector[TestBrowser => Unit](
      browser => press(dom.document.body, "Escape"),
      browser => click(one(browser.mount, ".chronicle-details-close")),
      browser => click(overlay(browser))).foreach { closing =>
      withShell { (browser, shell) =>
        snapshot(shell, ashenCrownWithRedHoldingSeer)
        click(detailsButton(browser))
        closing(browser)
        assert(overlay(browser).hasAttribute("hidden"))
        assert(!table(browser).hasAttribute("inert"))
        assertEquals(dom.document.activeElement, detailsButton(browser))
      }
    }

  test("a click inside the panel does not close it"):
    withShell { (browser, shell) =>
      snapshot(shell, ashenCrownWithRedHoldingSeer)
      click(detailsButton(browser))
      click(content(browser))
      assert(!overlay(browser).hasAttribute("hidden"))
    }

  test("Escape with the inspector open closes only the inspector; a second " +
      "closes the overlay, and focus returns to the rebuilt button"):
    withShell { (browser, shell) =>
      snapshot(shell, ashenCrownWithRedHoldingSeer)
      val first = detailsButton(browser)
      click(first)
      click(one(content(browser), "button.legacy-chip"))
      val inspector = one(browser.mount, ".card-overlay")
      assert(!inspector.hasAttribute("hidden"))
      press(dom.document.activeElement, "Escape")
      assert(inspector.hasAttribute("hidden"))
      assert(!overlay(browser).hasAttribute("hidden"))
      // A snapshot rebuilds the World pane, and the button with it.
      snapshot(shell, ashenCrownWithRedHoldingSeer)
      press(content(browser), "Escape")
      assert(overlay(browser).hasAttribute("hidden"))
      assertNotEquals(detailsButton(browser), first)
      assertEquals(dom.document.activeElement, detailsButton(browser))
    }

  test("a snapshot redraws the open overlay in place, keeping its scroll, " +
      "and the inspector returns focus to the rebuilt chip (R11)"):
    withShell { (browser, shell) =>
      snapshot(shell, ashenCrownWithRedHoldingSeer)
      click(detailsButton(browser))
      content(browser).scrollTop = 40
      val chip = one(content(browser), "button.legacy-chip")
      click(chip)
      snapshot(shell, ashenCrownWithRedHoldingSeer.copy(chronicle =
        Some(ChronicleProjection("Ashen Crown II", "d-1", None))))
      assertEquals(content(browser).scrollTop, 40.0)
      assertEquals(one(browser.mount, "#chronicle-details-heading")
        .textContent, "Ashen Crown II")
      press(dom.document.activeElement, "Escape")
      val rebuilt = one(content(browser), "button.legacy-chip")
      assertNotEquals(rebuilt, chip)
      assertEquals(dom.document.activeElement, rebuilt)
    }

  test("an open overlay closes when a snapshot carries no Chronicle " +
      "Details, and a new opening starts at the top"):
    withShell { (browser, shell) =>
      snapshot(shell, ashenCrownWithRedHoldingSeer)
      click(detailsButton(browser))
      shell.showChronicleDetails(None)
      assert(overlay(browser).hasAttribute("hidden"))
      assert(!table(browser).hasAttribute("inert"))
      snapshot(shell, ashenCrownWithRedHoldingSeer)
      click(detailsButton(browser))
      content(browser).scrollTop = 40
      press(content(browser), "Escape")
      click(detailsButton(browser))
      assertEquals(content(browser).scrollTop, 0.0)
    }

  test("closing the overlay closes an inspector open over it, and leaves " +
      "the table usable and focus on the button (R19)"):
    Vector[(TestBrowser, GameTableShell) => Unit](
      (_, shell) => shell.showChronicleDetails(None),
      (browser, _) => click(one(browser.mount, ".chronicle-details-close")))
      .foreach { closing =>
        withShell { (browser, shell) =>
          snapshot(shell, ashenCrownWithRedHoldingSeer)
          click(detailsButton(browser))
          click(one(content(browser), "button.legacy-chip"))
          val inspector = one(browser.mount, ".card-overlay")
          assert(!inspector.hasAttribute("hidden"))
          closing(browser, shell)
          assert(inspector.hasAttribute("hidden"))
          assert(overlay(browser).hasAttribute("hidden"))
          assert(!table(browser).hasAttribute("inert"))
          assertEquals(dom.document.activeElement, detailsButton(browser))
        }
      }

  test("a redraw keeps the Atlas frame and the scorepad frame scrolled " +
      "sideways where the reader left them"):
    val withDeepWoodsRecent = ashenCrownWithRedHoldingSeer.copy(
      chronicleDetails = ashenCrownWithRedHoldingSeer.chronicleDetails.map(
        _.copy(atlasRecent = Some(TestSites.quietSite("site:deep-woods",
          "Deep Woods")))))
    withShell { (browser, shell) =>
      snapshot(shell, withDeepWoodsRecent)
      click(detailsButton(browser))
      def frames = Vector(".chronicle-details-atlas-frame",
        ".scoresheet-frame").map(selector => one(content(browser), selector)
          .asInstanceOf[dom.html.Element])
      frames.zip(Vector(30.0, 12.0)).foreach((frame, left) =>
        frame.scrollLeft = left)
      val before = frames
      snapshot(shell, withDeepWoodsRecent)
      assert(frames.zip(before).forall(_ != _), "the frames were redrawn")
      assertEquals(frames.map(_.scrollLeft), Vector(30.0, 12.0))
    }

  test("the game-end save prompt closes the overlay and an inspector over " +
      "it, and opens over a usable page with its name field focused " +
      "(R21)"):
    withShell { (browser, shell) =>
      snapshot(shell, ashenCrownWithRedHoldingSeer)
      click(detailsButton(browser))
      click(one(content(browser), "button.legacy-chip"))
      val inspector = one(browser.mount, ".card-overlay")
      assert(!inspector.hasAttribute("hidden"))
      // This browser keeps another Chronicle under the evolved one's name,
      // so saving it asks.
      val storage = new MemoryChronicleStorage()
      new ChronicleShelf(storage, () => "earlier")
        .put(ChronicleFixtures.entry("Ashen Crown", "d-9"))
      val chronicle = new TableChronicle(browser.mount,
        Some(new ChronicleShelf(storage, ChronicleFixtures.clock)),
        ChronicleClient(ChronicleFixtures.convertingTransport()),
        FakeChronicleFiles(), () => (), () => (), _ => (),
        beforeAsking = () => shell.closeChronicleDetails())
      chronicle.section(Some(ChronicleProjection("Ashen Crown", "d-1",
        Some(ChronicleFixtures.entry("Ashen Crown", "d-2")))))
      assert(inspector.hasAttribute("hidden"))
      assert(overlay(browser).hasAttribute("hidden"))
      assert(!table(browser).hasAttribute("inert"))
      val prompt = one(browser.mount, ".chronicle-save-prompt")
      assert(prompt.contains(dom.document.activeElement),
        dom.document.activeElement.outerHTML)
    }
