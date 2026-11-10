package oathdigital.frontend

import Dom.{one, press}
import org.scalajs.dom
import scala.scalajs.js

/** The desktop table's left column is as wide as the viewer drags it, and
  * the width is kept per browser. The stylesheet decides where the width
  * applies (never on a phone); this drives the class and the property it
  * reads, and what is stored.
  */
class SidebarDividerSuite extends munit.FunSuite:
  private final class Fixture(stored: Option[String] = None):
    val browser = new TestBrowser()
    val store = new FakeLogStore(
      stored.fold(Map.empty)(width => Map(SidebarDivider.StoreKey -> width)))
    val table = dom.document.createElement("div").asInstanceOf[dom.html.Div]
    val sidebar = dom.document.createElement("section").asInstanceOf[dom.html.Element]
    table.appendChild(sidebar)
    browser.mount.appendChild(table)
    val divider = new SidebarDivider(table, sidebar, Some(store))
    val handle = one(table, ".sidebar-divider").asInstanceOf[dom.html.Element]
    // jsdom has no pointer capture; the divider holds the pointer while dragging.
    private val dynamic = handle.asInstanceOf[js.Dynamic]
    dynamic.setPointerCapture = (_: Double) => ()
    dynamic.hasPointerCapture = (_: Double) => false
    dynamic.releasePointerCapture = (_: Double) => ()

    def width: String = table.style.getPropertyValue("--sidebar-w")
    def sized: Boolean = table.classList.contains("sidebar-sized")

    // jsdom has no PointerEvent either; a mouse event carries the pointer
    // fields the divider reads.
    def pointer(kind: String, x: Double): Unit =
      val event = new dom.MouseEvent(kind, new dom.MouseEventInit {
        bubbles = true; cancelable = true; button = 0; buttons = 1
        clientX = x; clientY = 10 })
      js.Object.defineProperty(event, "pointerId", js.Dynamic.literal(value = 1)
        .asInstanceOf[js.PropertyDescriptor])
      handle.dispatchEvent(event)

    def close(): Unit =
      divider.dispose()
      browser.close()

  private def withFixture(stored: Option[String] = None)(body: Fixture => Unit): Unit =
    val fixture = new Fixture(stored)
    try body(fixture) finally fixture.close()

  test("with no stored width the stylesheet's columns stand"):
    withFixture() { f =>
      assert(!f.sized)
      assertEquals(f.width, "")
      assertEquals(f.handle.getAttribute("role"), "separator")
      assertEquals(f.handle.getAttribute("aria-orientation"), "vertical")
    }

  test("a stored width sizes the sidebar when the table opens"):
    withFixture(Some("280")) { f =>
      assert(f.sized)
      assertEquals(f.width, "280px")
      assertEquals(f.handle.getAttribute("aria-valuenow"), "280")
    }

  test("a stored width that is not a number is ignored"):
    withFixture(Some("wide")) { f => assert(!f.sized) }

  test("the arrow keys widen and narrow the sidebar and store its width"):
    withFixture(Some("300")) { f =>
      press(f.handle, "ArrowRight")
      assertEquals(f.width, "316px")
      assertEquals(f.store.values.get(SidebarDivider.StoreKey), Some("316"))
      press(f.handle, "ArrowLeft")
      press(f.handle, "ArrowLeft")
      assertEquals(f.width, "284px")
      assertEquals(f.store.values.get(SidebarDivider.StoreKey), Some("284"))
    }

  test("the sidebar is never narrower than its minimum"):
    withFixture(Some("250")) { f =>
      press(f.handle, "ArrowLeft")
      assertEquals(f.width, s"${SidebarDivider.MinWidth.toInt}px")
      press(f.handle, "Home")
      assertEquals(f.width, s"${SidebarDivider.MinWidth.toInt}px")
    }

  test("a drag moves the edge with the pointer and stores the width on release"):
    withFixture(Some("300")) { f =>
      f.pointer("pointerdown", 100)
      f.pointer("pointermove", 160)
      assertEquals(f.width, "360px")
      assertEquals(f.store.values.get(SidebarDivider.StoreKey), Some("300"))
      f.pointer("pointerup", 160)
      assertEquals(f.store.values.get(SidebarDivider.StoreKey), Some("360"))
      f.pointer("pointermove", 400)
      assertEquals(f.width, "360px")
    }

  test("a double click returns the default columns and forgets the width"):
    withFixture(Some("300")) { f =>
      f.handle.dispatchEvent(new dom.MouseEvent("dblclick",
        new dom.MouseEventInit { bubbles = true }))
      assert(!f.sized)
      assertEquals(f.width, "")
      assertEquals(f.store.values.get(SidebarDivider.StoreKey), Some(""))
    }

  test("the table shell carries one divider, after its panes"):
    val browser = new TestBrowser()
    val shell = new GameTableShell(browser.mount, developmentTools = false,
      logStore = None)
    try
      val divider = one(browser.mount, ".game-table > .sidebar-divider")
      assertEquals(divider.previousElementSibling.getAttribute("aria-labelledby"),
        "log-heading")
      press(divider, "ArrowRight")
      assert(one(browser.mount, ".game-table").classList.contains("sidebar-sized"))
    finally
      shell.dispose()
      browser.close()
