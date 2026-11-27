package distantlands.frontend

import Dom.{all, one, press}
import distantlands.protocol.art.ArtCatalog
import org.scalajs.dom

/** The Settings dialog and its one switch, Game art (game art design,
  * "The Settings dialog"). Runs the real shell in `TestBrowser`. */
class SettingsDialogSuite extends munit.FunSuite:
  override def afterEach(context: AfterEach): Unit = GameArt.reset()

  /** Runs `check` against a shell, with the development tools shown or
    * not, and disposes of it. */
  private def withShell(developmentTools: Boolean = false)(
      check: TestBrowser => Unit): Unit =
    val browser = new TestBrowser()
    val shell = new GameTableShell(browser.mount, developmentTools,
      logStore = None)
    try check(browser)
    finally
      shell.dispose()
      browser.close()

  private def toggle(browser: TestBrowser): dom.html.Button =
    one(browser.mount, ".settings-toggle").asInstanceOf[dom.html.Button]
  private def panel(browser: TestBrowser): dom.Element =
    one(browser.mount, ".settings-panel")
  private def switch(browser: TestBrowser): dom.html.Button =
    one(browser.mount, ".setting-switch").asInstanceOf[dom.html.Button]
  private def reason(browser: TestBrowser): dom.Element =
    one(browser.mount, ".setting-reason")
  /** Whether the switch wears the Withheld face: `aria-disabled`, so it
    * still takes focus and its hover note gives the reason. */
  private def withheld(browser: TestBrowser): Boolean =
    switch(browser).getAttribute("aria-disabled") == "true"

  test("every player has a Settings button in the Players pane header, " +
      "beside Dev tools when those are shown"):
    withShell() { browser =>
      assertEquals(all(browser.mount,
        ".pane-players .pane-header .header-controls > button")
        .map(_.textContent), Vector("Settings"))
    }
    withShell(developmentTools = true) { browser =>
      assertEquals(all(browser.mount,
        ".pane-players .pane-header .header-controls > button")
        .map(_.textContent), Vector("Settings", "Dev tools"))
    }

  test("the button opens a dialog headed Settings and moves focus to " +
      "Close; Close returns focus to the button"):
    withShell() { browser =>
      assert(panel(browser).hasAttribute("hidden"))
      assertEquals(panel(browser).getAttribute("role"), "dialog")
      assertEquals(toggle(browser).getAttribute("aria-expanded"), "false")
      toggle(browser).click()
      assert(!panel(browser).hasAttribute("hidden"))
      assertEquals(toggle(browser).getAttribute("aria-expanded"), "true")
      assertEquals(one(panel(browser), ".pane-heading").textContent,
        "Settings")
      assertEquals(panel(browser).getAttribute("aria-labelledby"),
        one(panel(browser), ".pane-heading").id)
      val close = one(panel(browser), ".settings-close")
      assertEquals(dom.document.activeElement, close)
      close.asInstanceOf[dom.html.Button].click()
      assert(panel(browser).hasAttribute("hidden"))
      assertEquals(dom.document.activeElement, toggle(browser))
    }

  test("Escape closes the dialog and returns focus to the button"):
    withShell() { browser =>
      toggle(browser).click()
      press(switch(browser), "Escape")
      assert(panel(browser).hasAttribute("hidden"))
      assertEquals(dom.document.activeElement, toggle(browser))
    }

  test("opening Settings closes the development panel, and the reverse"):
    withShell(developmentTools = true) { browser =>
      val dev = one(browser.mount, ".development-panel")
      val devToggle = one(browser.mount, ".dev-toggle")
        .asInstanceOf[dom.html.Button]
      devToggle.click()
      toggle(browser).click()
      assert(dev.hasAttribute("hidden"))
      assertEquals(devToggle.getAttribute("aria-expanded"), "false")
      assert(!panel(browser).hasAttribute("hidden"))
      devToggle.click()
      assert(panel(browser).hasAttribute("hidden"))
      assertEquals(toggle(browser).getAttribute("aria-expanded"), "false")
      assert(!dev.hasAttribute("hidden"))
    }

  test("the switch is a switch named Game art, described by its hint"):
    TestArt.listingWholeCatalog()
    withShell() { browser =>
      val control = switch(browser)
      assertEquals(control.getAttribute("role"), "switch")
      assertEquals(one(control, ".setting-switch-label").textContent,
        "Game art")
      assertEquals(control.getAttribute("aria-checked"), "true")
      assert(!control.hasAttribute("aria-disabled"))
      assert(!control.hasAttribute(HoverTip.Attribute))
      val hint = one(browser.mount, ".setting-hint")
      assertEquals(hint.textContent,
        "Card faces, the mat and pieces drawn from the host's art.")
      assert(control.getAttribute("aria-describedby").split(" ")
        .contains(hint.id))
      assert(reason(browser).hasAttribute("hidden"))
    }

  test("a server with no art shows the switch disabled, with its reason"):
    TestArt.listing()
    withShell() { browser =>
      assert(withheld(browser))
      assert(!switch(browser).disabled)
      assertEquals(switch(browser).getAttribute("aria-checked"), "false")
      assertEquals(reason(browser).textContent,
        "This server has no game art.")
      assertEquals(switch(browser).getAttribute(HoverTip.Attribute),
        "This server has no game art.")
      assert(!reason(browser).hasAttribute("hidden"))
      assert(switch(browser).getAttribute("aria-describedby").split(" ")
        .contains(reason(browser).id))
    }

  test("a failed art list shows the switch disabled, with its reason"):
    GameArt.install(GameArt.Manifest.Failed, new MemoryChronicleStorage())
    withShell() { browser =>
      assert(withheld(browser))
      assertEquals(reason(browser).textContent,
        "Couldn't load the art list; reload to try again.")
    }

  test("until the art list arrives the switch is disabled and says so"):
    withShell() { browser =>
      assert(withheld(browser))
      assertEquals(reason(browser).textContent, "Loading the art list…")
    }

  test("a manifest listing part of the catalog adds the count to the hint"):
    TestArt.listing(ArtCatalog.allPaths.take(3)*)
    withShell() { browser =>
      assertEquals(one(browser.mount, ".setting-hint").textContent,
        "Card faces, the mat and pieces drawn from the host's art. " +
          s"3 of ${ArtCatalog.allPaths.size} images; the rest stay drawn.")
      assert(!withheld(browser))
    }

  test("flipping the switch saves the preference and redraws the table " +
      "at once, and the switch keeps its place"):
    val store = TestArt.listingWholeCatalog()
    withShell() { browser =>
      var redraws = 0
      GameArt.onChange(() => redraws += 1)
      toggle(browser).click()
      val control = switch(browser)
      control.click()
      assertEquals(store.values.get("distantlands.art"), Some("off"))
      assertEquals(redraws, 1)
      assertEquals(control.getAttribute("aria-checked"), "false")
      assertEquals(one(control, ".setting-switch-state").textContent, "Off")
      assert(switch(browser) eq control)
      control.click()
      assertEquals(store.values.get("distantlands.art"), Some("on"))
      assertEquals(control.getAttribute("aria-checked"), "true")
      assertEquals(one(control, ".setting-switch-state").textContent, "On")
    }

  test("a withheld switch changes nothing when clicked"):
    val store = TestArt.listing()
    withShell() { browser =>
      var redraws = 0
      GameArt.onChange(() => redraws += 1)
      switch(browser).click()
      assertEquals(store.values.get("distantlands.art"), None)
      assertEquals(redraws, 0)
    }

  test("each table update brings the dialog up to date"):
    val browser = new TestBrowser()
    val shell = new GameTableShell(browser.mount, developmentTools = false,
      logStore = None)
    try
      assert(withheld(browser))
      TestArt.listingWholeCatalog()
      def pane(): dom.Element = dom.document.createElement("div")
      shell.update("game", "decision", pane(), pane(), pane(), pane())
      assert(!withheld(browser))
      assertEquals(switch(browser).getAttribute("aria-checked"), "true")
    finally
      shell.dispose()
      browser.close()
