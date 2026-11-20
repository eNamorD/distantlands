package distantlands.frontend

import distantlands.frontend.Dom.{all, one}
import distantlands.model.PlayerColor
import distantlands.protocol.{GameIntent => Intent, WalkerStartArgWire}
import org.scalajs.dom

/** The requests block of the consent column (reconsider design,
  * "Frontend", "Requests list"): the turn's requests about a power, as
  * the viewer answered them. Red rules the Narrow Pass; Blue asked. */
class ConsentRequestsViewSuite extends munit.FunSuite:
  private val red = GamePlayer("red", "Red", "Exile", PlayerColor.Red)
  private val blue = GamePlayer("blue", "Blue", "Exile", PlayerColor.Blue)
  private val power = "site.narrow-pass.pass"

  private def row(answer: String, change: String,
      also: Vector[String] = Vector.empty, use: Option[String] = None,
      optionId: String = "site:deep-woods", label: String = "Deep Woods")
      : ConsentRequestRowState =
    ConsentRequestRowState("blue", "travel.destination", "site", optionId,
      label, answer, also, use, change)

  private def tableWhereRedAnswered(rows: ConsentRequestRowState*)
      : GameProjection =
    TestProjections.actingProjection(players = Vector(red, blue)).copy(
      consentPreferences = Vector(ConsentPreferencesState(power,
        "Narrow Pass", Vector(ConsentPreferenceRowState("blue",
          "case-by-case")), requests = rows.toVector)))

  /** The block as the column draws it, with what its buttons send. */
  private def blockOf(rows: Vector[ConsentRequestRowState],
      control: Boolean = true): (dom.Element, () => Vector[Intent]) =
    var sent = Vector.empty[Intent]
    val links = SitePowerLinks.of(tableWhereRedAnswered(rows*),
      Option.when(control)(command => sent :+= command))
    val requests = links.requests(power).getOrElse(fail("no requests"))
    (ConsentRequestsView.mount(requests), () => sent)

  private def texts(block: dom.Element, selector: String): Vector[String] =
    all(block, selector).map(_.textContent)

  private def button(block: dom.Element): dom.html.Button =
    one(block, ".consent-request-change").asInstanceOf[dom.html.Button]

  test("a power with no requests draws a hidden, empty block"):
    val (block, _) = blockOf(Vector.empty)
    assert(block.hasAttribute("hidden"))
    assertEquals(block.children.length, 0)
    assertEquals(block.getAttribute("data-power-id"), power)

  test("a refused request reads who asked for what, its state, and an " +
      "Allow button that sends the change"):
    val (block, sent) = blockOf(Vector(row("refused", "open")))
    assert(!block.hasAttribute("hidden"))
    assertEquals(texts(block, ".consent-grid-heading"),
      Vector("Requests this turn"))
    assertEquals(texts(block, ".consent-request-asked"),
      Vector("Blue asked to select Deep Woods"))
    assertEquals(texts(block, ".consent-request-also"), Vector.empty)
    assertEquals(texts(block, ".consent-request-state"), Vector("Refused"))
    assertEquals(button(block).textContent, "Allow")
    button(block).click()
    assertEquals(sent(), Vector(Intent.ReconsiderConsent("blue",
      "travel.destination", WalkerStartArgWire("site", "site:deep-woods"),
      allow = true)))

  test("each entry is a group named by its first line, so a screen " +
      "reader can tell one Allow from another (R22)"):
    val (block, _) = blockOf(Vector(
      row("allowed", "open", optionId = "site:dunes", label = "Dunes"),
      row("refused", "open")))
    assertEquals(all(block, ".consent-request").map(entry =>
      (entry.getAttribute("role"), entry.getAttribute("aria-label"))),
      Vector(("group", "Blue asked to select Dunes"),
        ("group", "Blue asked to select Deep Woods")))

  test("a granted request reads Allowed and a Refuse button that sends " +
      "the refusal"):
    val (block, sent) = blockOf(Vector(row("allowed", "open")))
    assertEquals(texts(block, ".consent-request-state"), Vector("Allowed"))
    assertEquals(button(block).textContent, "Refuse")
    button(block).click()
    assertEquals(sent().map {
      case Intent.ReconsiderConsent(_, _, _, allow) => allow
      case other => fail(s"unexpected $other")
    }, Vector(false))

  test("a click sends once: the button waits, without losing keyboard " +
      "focus, until the list is redrawn (R20)"):
    val (block, sent) = blockOf(Vector(row("refused", "open")))
    dom.document.body.appendChild(block)
    button(block).focus()
    button(block).click()
    button(block).click()
    assertEquals(sent().size, 1)
    // A disabled button would drop focus to the page in a browser, and
    // the redraw could not find the reader's place again.
    assert(!button(block).disabled)
    assertEquals(button(block).getAttribute("aria-disabled"), "true")
    assertEquals(button(block).getAttribute("data-tip"), null)
    assert(dom.document.activeElement eq button(block))
    block.remove()

  test("an ask that covered other powers names them on a second line"):
    val (block, _) = blockOf(Vector(row("refused", "open",
      also = Vector("Secret Police", "Toll Roads"))))
    assertEquals(texts(block, ".consent-request-also"),
      Vector("Also answers Secret Police and Toll Roads"))

  test("a used entry reads Allowed, used; its Refuse is withheld with " +
      "the reason in the hover note, and sends nothing"):
    val (block, sent) = blockOf(Vector(row("allowed", "used")))
    assertEquals(texts(block, ".consent-request-state"),
      Vector("Allowed, used"))
    val refuse = button(block)
    assertEquals(refuse.textContent, "Refuse")
    assertEquals(refuse.getAttribute("aria-disabled"), "true")
    assert(!refuse.disabled)
    assertEquals(refuse.getAttribute("data-tip"), "Blue has acted on this " +
      "consent. It unlocks if Blue undoes that.")
    refuse.click()
    assertEquals(sent(), Vector.empty)
    assert(!block.textContent.contains("unlocks"))

  test("a refused entry whose option was used keeps Refused, with Allow " +
      "withheld"):
    val (block, _) = blockOf(Vector(row("refused", "used")))
    assertEquals(texts(block, ".consent-request-state"), Vector("Refused"))
    assertEquals(button(block).getAttribute("aria-disabled"), "true")

  test("an entry with a waiting request, and one that covers a lost " +
      "power, are withheld with their own notes (R8, R17)"):
    val (waiting, _) = blockOf(Vector(row("allowed", "waiting")))
    assertEquals(button(waiting).getAttribute("data-tip"),
      "A request about this option is waiting for an answer.")
    val (lapsed, _) = blockOf(Vector(row("refused", "lapsed")))
    assertEquals(button(lapsed).getAttribute("data-tip"),
      "You no longer rule every power this answer covers.")
    assertEquals(button(lapsed).getAttribute("aria-disabled"), "true")

  test("an automatic power's entry reads what happened, with the other " +
      "answer's button withheld (R18)"):
    val (stopped, sent) = blockOf(Vector(row("refused", "automatic",
      use = Some("Tyrant"), optionId = "use", label = "Use")))
    assertEquals(texts(stopped, ".consent-request-asked"),
      Vector("Blue's Tyrant: deactivated"))
    assertEquals(texts(stopped, ".consent-request-state"), Vector.empty)
    assertEquals(button(stopped).textContent, "Let it work")
    assertEquals(button(stopped).getAttribute("aria-disabled"), "true")
    assertEquals(button(stopped).getAttribute("data-tip"),
      "This answer cannot be changed.")
    button(stopped).click()
    assertEquals(sent(), Vector.empty)
    val (worked, _) = blockOf(Vector(row("allowed", "automatic",
      use = Some("Tyrant"), optionId = "use", label = "Use")))
    assertEquals(texts(worked, ".consent-request-asked"),
      Vector("Blue's Tyrant: let work"))
    assertEquals(button(worked).textContent, "Deactivate")

  test("a change value this client does not know is not changeable"):
    val (block, sent) = blockOf(Vector(row("allowed", "from-the-future")))
    assertEquals(button(block).getAttribute("aria-disabled"), "true")
    button(block).click()
    assertEquals(sent(), Vector.empty)

  test("without control an open entry's button is disabled, and a " +
      "withheld one keeps its note"):
    val (open, _) = blockOf(Vector(row("refused", "open")), control = false)
    assert(button(open).disabled)
    val (used, _) = blockOf(Vector(row("allowed", "used")), control = false)
    assert(!button(used).disabled)
    assertEquals(button(used).getAttribute("aria-disabled"), "true")

  test("entries keep the server's order, and each is keyed by its " +
      "requester, decision and option"):
    val (block, _) = blockOf(Vector(
      row("allowed", "open", optionId = "site:dunes", label = "Dunes"),
      row("refused", "open")))
    assertEquals(texts(block, ".consent-request-asked"), Vector(
      "Blue asked to select Dunes", "Blue asked to select Deep Woods"))
    assertEquals(all(block, ".consent-request").map(
      _.getAttribute("data-request-key")), Vector(
      "blue|travel.destination|site|site:dunes",
      "blue|travel.destination|site|site:deep-woods"))

  test("the column draws the block under the preference rows"):
    val links = SitePowerLinks.of(tableWhereRedAnswered(
      row("refused", "open")), None)
    val grid = links.grid(SitePower("pass", "Pass", None, power))
      .getOrElse(fail("no grid"))
    val column = ConsentGridView.render(grid)
    assertEquals(column.children.toVector.map(_.getAttribute("class")),
      Vector("consent-grid-heading", "consent-grid-row", "consent-requests"))
