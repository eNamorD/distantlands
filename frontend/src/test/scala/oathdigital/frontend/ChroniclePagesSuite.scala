package oathdigital.frontend

import oathdigital.frontend.ChronicleFixtures.{entry, seatLinksResponse}
import oathdigital.frontend.ServerModeFixtures.withEmptyLogPage
import oathdigital.protocol.{ChronicleRequest, TrustedGameCreateRequestCodec}
import oathdigital.testkit.Unwrap.{accepted, as, present}
import org.scalajs.dom
import scala.collection.mutable.ArrayBuffer
import scala.concurrent.Future
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

/** The start pages together: the Chronicle page at `/`, player selection at
  * `/new-game`, moving between them, and creating a game from a new or a
  * loaded Chronicle. */
class ChroniclePagesSuite extends munit.FunSuite:
  /** A server that creates every game it is asked for, answering with the
    * Chronicle named in the request (a new one's name, or the saved JSON's),
    * unless `refusal` gives a 422 reason. */
  private def creatingTransport(requests: ArrayBuffer[RecordedRequest],
      refusal: Option[String]): JsonTransport =
    withEmptyLogPage(ScriptedTransport((_, url, body) =>
      Future.successful(Right(
        if url != "/games" then ChronicleFixtures.convertReply(body,
          Some("TTS-STRING"))
        else refusal match
          case Some(reason) => TransportResponse(422, ujson.write(ujson.Obj(
            "error" -> "chronicle-refused", "message" -> reason)))
          case None =>
            val name = accepted(TrustedGameCreateRequestCodec.decode(
              present(body, "a request body"))).chronicle match
              case ChronicleRequest.New(name) => name
              case ChronicleRequest.Saved(json) => ujson.read(json)("name").str
            TransportResponse(201, seatLinksResponse(name)))),
      requests))

  /** The trusted start pages at `path` over `devices`. */
  private def startPages(browser: TestBrowser, path: String,
      devices: ChronicleDevices,
      requests: ArrayBuffer[RecordedRequest] = ArrayBuffer.empty,
      refusal: Option[String] = None): Unit =
    Main.start(browser.mount, path, trustedAlpha = true,
      creatingTransport(requests, refusal), devices = devices)

  /** A storage holding "Ashen Crown", digest d-1, loaded. */
  private def storageWithAshenCrown(): MemoryChronicleStorage =
    val storage = new MemoryChronicleStorage()
    accepted(new ChronicleShelf(storage, ChronicleFixtures.clock)
      .put(entry("Ashen Crown")))
    storage

  private def shelfOver(storage: ChronicleStorage): ChronicleShelf =
    new ChronicleShelf(storage, ChronicleFixtures.clock)

  private def shows(browser: TestBrowser, className: String): Boolean =
    browser.byClass(className).nonEmpty

  private def createButton(browser: TestBrowser): dom.html.Button =
    as[dom.html.Button](browser.byClass("create-trusted-game").head)

  private def sentChronicles(requests: collection.Seq[RecordedRequest])
      : Vector[ChronicleRequest] =
    requests.toVector.filter(_.url == "/games").map(request =>
      accepted(TrustedGameCreateRequestCodec.decode(
        present(request.body, "a request body"))).chronicle)

  test("/new-game without a choice returns to the Chronicle page"):
    TestBrowser.useSynchronously(): browser =>
      val history = FakePageHistory()
      startPages(browser, "/new-game", ChronicleFixtures.devices(
        history = history))
      assertEquals(history.replaced.toVector, Vector("/"))
      assert(browser.mount.classList.contains("start-page"))
      assert(shows(browser, "new-chronicle"))
      assert(!shows(browser, "create-trusted-game"))

  test("New Chronicle opens player selection with a dated, free name"):
    TestBrowser.useSynchronously(): browser =>
      val history = FakePageHistory()
      startPages(browser, "/", ChronicleFixtures.devices(history = history))
      browser.click("new-chronicle")
      assertEquals(history.pushed.toVector, Vector("/new-game"))
      assertEquals(browser.input("Chronicle name").value,
        TrustedHostUi.defaultChronicleName())
      assert(TrustedHostUi.defaultChronicleName()
        .matches("Chronicle \\d{4}-\\d{2}-\\d{2}"))
      assert(shows(browser, "chronicle-back"))

  test("the default name skips a saved one"):
    TestBrowser.useSynchronously(): browser =>
      val storage = new MemoryChronicleStorage()
      accepted(shelfOver(storage).put(
        entry(TrustedHostUi.defaultChronicleName())))
      startPages(browser, "/", ChronicleFixtures.devices(Some(storage)))
      browser.click("new-chronicle")
      assertEquals(browser.input("Chronicle name").value,
        TrustedHostUi.defaultChronicleName() + " (2)")

  test("Back returns to the Chronicle page"):
    TestBrowser.useSynchronously(): browser =>
      val history = FakePageHistory()
      startPages(browser, "/", ChronicleFixtures.devices(history = history))
      browser.click("new-chronicle")
      browser.click("chronicle-back")
      assertEquals(history.pushed.toVector, Vector("/new-game", "/"))
      assert(shows(browser, "new-chronicle"))
      assert(!shows(browser, "create-trusted-game"))

  test("the browser's Back and Forward move between the pages"):
    TestBrowser.useSynchronously(): browser =>
      val history = FakePageHistory()
      startPages(browser, "/", ChronicleFixtures.devices(history = history))
      browser.click("new-chronicle")
      history.back("/")
      assert(shows(browser, "new-chronicle"))
      history.back("/new-game")
      assert(shows(browser, "create-trusted-game"))

  test("a taken or empty name is refused beside Create game, with no " +
      "request"):
    TestBrowser.useSynchronously(): browser =>
      val requests = ArrayBuffer.empty[RecordedRequest]
      startPages(browser, "/", ChronicleFixtures.devices(
        Some(storageWithAshenCrown())), requests)
      browser.click("new-chronicle")
      browser.input("Chronicle name").value = " Ashen Crown "
      browser.click("create-trusted-game")
      assert(browser.text.contains(
        "A Chronicle named \"Ashen Crown\" is already saved."), browser.text)
      browser.input("Chronicle name").value = ""
      browser.click("create-trusted-game")
      assert(browser.text.contains("A Chronicle needs a name."))
      assertEquals(requests.size, 0)

  test("Create with New sends the name and saves and loads the returned " +
      "Chronicle"):
    TestBrowser.use(): browser =>
      val storage = new MemoryChronicleStorage()
      val requests = ArrayBuffer.empty[RecordedRequest]
      startPages(browser, "/", ChronicleFixtures.devices(Some(storage)),
        requests)
      browser.click("new-chronicle")
      browser.input("Chronicle name").value = "  Iron Vow "
      browser.click("create-trusted-game")
      browser.settle.map { _ =>
        assertEquals(sentChronicles(requests),
          Vector(ChronicleRequest.New("Iron Vow")))
        val loaded = present(shelfOver(storage).loaded, "a loaded Chronicle")
        assertEquals(loaded.entry, entry("Iron Vow", "d-start"))
        assertEquals(browser.byClass("seat-link").size, 2)
      }

  test("Continue shows the loaded name and sends its JSON"):
    TestBrowser.use(): browser =>
      val storage = storageWithAshenCrown()
      val before = storage.values
      val requests = ArrayBuffer.empty[RecordedRequest]
      startPages(browser, "/", ChronicleFixtures.devices(Some(storage)),
        requests)
      browser.click("continue-chronicle")
      assertEquals(browser.byClass("chronicle-continued").head.textContent,
        "Chronicle: Ashen Crown")
      assert(!browser.nodes.exists(
        _.getAttribute("aria-label") == "Chronicle name"))
      browser.click("create-trusted-game")
      browser.settle.map { _ =>
        assertEquals(sentChronicles(requests),
          Vector(ChronicleRequest.Saved(entry("Ashen Crown").json)))
        assertEquals(storage.values, before)
      }

  test("a refused saved Chronicle shows the server's reason and keeps the " +
      "form editable"):
    TestBrowser.use(): browser =>
      startPages(browser, "/", ChronicleFixtures.devices(
        Some(storageWithAshenCrown())),
        refusal = Some("a non-empty world requires the Empire"))
      browser.click("continue-chronicle")
      browser.click("create-trusted-game")
      browser.settle.map { _ =>
        assertEquals(browser.byClass("status").last.textContent,
          "A non-empty world requires the Empire")
        assert(!createButton(browser).disabled)
        assertEquals(browser.byClass("seat-link"), Vector.empty)
      }

  test("refused storage still creates from a new Chronicle"):
    TestBrowser.use(): browser =>
      startPages(browser, "/", ChronicleFixtures.devices(storage = None))
      assert(shows(browser, "chronicle-storage-refused"))
      browser.click("new-chronicle")
      browser.click("create-trusted-game")
      browser.settle.map { _ =>
        assertEquals(browser.byClass("seat-link").size, 2)
      }
