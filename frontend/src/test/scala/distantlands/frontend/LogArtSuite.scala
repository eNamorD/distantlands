package distantlands.frontend

import Dom.all
import distantlands.protocol.projection.{DieFace, LogKind, LogSpan}

/** The game log with game art. The log only grows: a line already drawn
  * is not drawn again, so a change of game art must redraw the list, or a
  * roll drawn before the manifest arrived would keep its glyphs for the
  * whole session and a roll drawn with art would keep its crops after the
  * switch is turned off (whole-feature review). */
class LogArtSuite extends munit.FunSuite:
  override def afterEach(context: AfterEach): Unit = GameArt.reset()

  private val roll = TestLog.entry(3, LogKind.Action, Vector(
    LogSpan.Text("Rolled "),
    LogSpan.Dice(DieFace.OneSword, Vector(DieFace.HollowSword))))
  private val later = TestLog.textEntry(4, LogKind.Action, "Attack: 1")

  test("a roll in the log takes its crops when the manifest arrives, " +
      "loses them when the switch is turned off, and keeps its place"):
    val content = TestLog.sizedBox(total = 0, height = 0, scrolled = 0)
    val pane = new GameLogPane(content)
    def crops: Int = all(content, ".die-face > .art-crop").size
    pane.show("g|red", Vector(roll), Map.empty)
    assertEquals(all(content, ".die-face").size, 2)
    assertEquals(crops, 0)
    TestArt.listingWholeCatalog()
    pane.show("g|red", Vector(roll), Map.empty)
    assertEquals(crops, 2)
    assertEquals(all(content, ".die-face").size, 2)
    // A new line still appends without a redraw: the chips are the same
    // elements.
    val chips = all(content, ".die-face")
    pane.show("g|red", Vector(roll, later), Map.empty)
    assert(all(content, ".die-face").zip(chips).forall(_ eq _))
    GameArt.setEnabled(false)
    pane.show("g|red", Vector(roll, later), Map.empty)
    assertEquals(crops, 0)
    assertEquals(all(content, ".die-face").size, 2)

  test("the log overlay redraws its rolls with the art too"):
    val browser = new TestBrowser()
    try
      val overlay = new GameLogOverlay(browser.mount, () => (), () => ())
      overlay.show("g|red", Vector(roll), Map.empty, None)
      assertEquals(all(browser.mount, ".log-overlay .art-crop").size, 0)
      TestArt.listingWholeCatalog()
      overlay.show("g|red", Vector(roll), Map.empty, None)
      assertEquals(all(browser.mount, ".log-overlay .art-crop").size, 2)
    finally browser.close()
