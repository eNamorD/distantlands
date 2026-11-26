package distantlands.frontend

import Dom.{all, one}
import distantlands.protocol.projection.{FoundationProjection,
  OathkeeperProjection}
import org.scalajs.dom

/** The printed cards the table shows as words have their image on the
  * inspection overlay's plate (game art design, "Card faces"): a
  * foundation and the Oath goal. */
class PlateArtSuite extends munit.FunSuite:
  override def afterEach(context: AfterEach): Unit = GameArt.reset()

  /** Runs `check` against a shell's mount, and disposes of the shell. */
  private def withShell(check: dom.Element => Unit): Unit =
    val browser = new TestBrowser()
    val shell = new GameTableShell(browser.mount, developmentTools = false,
      logStore = None)
    try check(browser.mount)
    finally
      shell.dispose()
      browser.close()

  private def plateImages(mount: dom.Element): Vector[String] =
    all(mount, ".card-overlay .card-plate > img.art")
      .map(_.getAttribute("src"))

  test("a foundation's plate holds the image of the face in use"):
    TestArt.listingWholeCatalog()
    withShell { mount =>
      val strip = FoundationStrip.render(FoundationStrip.ofGame(Vector(
        FoundationProjection(1, "altered", "Wide Horizons", "Rule"))))
      mount.appendChild(strip)
      one(strip, ".foundation-card").asInstanceOf[dom.html.Button].click()
      assertEquals(plateImages(mount), Vector(
        "/art/cards/Foundation%20I%20-%20Wide%20Horizons%20(NF).png"))
      // The title on the plate is printed; the rules beside it are not.
      assertEquals(all(mount, ".card-plate > .printed").size, 1)
      assertEquals(all(mount, ".card-overlay-details .printed").size, 0)
    }

  test("the strip's rows hold no image"):
    TestArt.listingWholeCatalog()
    assertEquals(all(FoundationStrip.render(FoundationStrip.ofGame(Vector(
      FoundationProjection(1, "normal", "Imperial Maps", "Rule")))), "img"),
      Vector.empty)

  test("the Oath goal's plate holds the goal card's image"):
    TestArt.listingWholeCatalog()
    withShell { mount =>
      val world = WorldBoardRenderer.world(TestProjections.actingProjection()
        .copy(oathkeeper = Some(OathkeeperProjection("supremacy", None,
          "oathkeeper", usurperLimited = false, winnerPlayerId = None))),
        BoardOptions.none)
      mount.appendChild(world)
      one(world, ".oathkeeper-goal").asInstanceOf[dom.html.Button].click()
      assertEquals(plateImages(mount),
        Vector("/art/cards/Supremacy%20Oath%20(NF).png"))
    }

  test("a plate with no key, and any plate with art off, stays drawn"):
    withShell { mount =>
      val origin = dom.document.createElement("button")
        .asInstanceOf[dom.html.Element]
      mount.appendChild(origin)
      TestArt.listingWholeCatalog()
      CardInspection.openText("Oathkeeper", Vector("Line"), origin,
        Some("oathkeeper-title"))
      assertEquals(plateImages(mount), Vector.empty)
      GameArt.setEnabled(false)
      CardInspection.openText("Foundation I · Wide Horizons", Vector("Rule"),
        origin, Some("foundation"),
        distantlands.protocol.art.ArtCatalog.foundation(1, "Wide Horizons"))
      assertEquals(plateImages(mount), Vector.empty)
    }

  /** The owner's review (game art ruling R43): a banner's overlay shows
    * the banner's own face, large, as a card's overlay shows the card. */
  test("a banner's overlay shows the banner's face, with its image and " +
      "what it holds, before its rules"):
    TestArt.listingWholeCatalog()
    withShell { mount =>
      val mob = BannerFace.render(BannerState("peoples-favor", "mob", None,
        3), None)
      mount.appendChild(mob)
      mob.click()
      val body = one(mount, ".card-overlay-body")
      val face = one(body, ":scope > .banner-face")
      assertEquals(all(body, ":scope > *").map(_.getAttribute("class")
        .split(" ").head), Vector("card-face", "card-overlay-details"))
      assertEquals(all(face, ":scope > img.art").map(_.getAttribute("src")),
        Vector("/art/cards/Banner%20of%20the%20People's%20Favor%20-%20" +
          "Mob%20(NF).png"))
      assertEquals(one(face, ".card-token-count").textContent, "3")
      val festival = BannerFace.render(BannerState("darkest-secret",
        "festival", None, 1), None)
      mount.appendChild(festival)
      festival.click()
      assertEquals(all(one(mount, ".card-overlay-body"),
        ":scope > .banner-face.banner-darkest-secret > img.art").size, 1)
    }

  test("with art off a banner's overlay shows its drawn face, and a " +
      "text overlay that is no banner shows none"):
    withShell { mount =>
      val mob = BannerFace.render(BannerState("peoples-favor", "mob", None,
        3), None)
      mount.appendChild(mob)
      mob.click()
      val face = one(mount, ".card-overlay-body > .banner-face")
      assertEquals(all(face, "img"), Vector.empty)
      assertEquals(all(face, ".banner-side").map(_.textContent),
        Vector("Mob"))
      CardInspection.openText("Supply", Vector("Line"), mob)
      assertEquals(all(mount, ".card-overlay-body > .banner-face"),
        Vector.empty)
    }

  /** The face in the overlay is a picture of the banner, not the
    * table's control: pressing it changes nothing, and a keyboard does
    * not stop on it. */
  private def assertOverlayFaceIsInert(mount: dom.Element): Unit =
    val face = one(mount, ".card-overlay-body > .banner-face")
    assertNotEquals(face.tagName.toLowerCase, "button")
    assert(!face.hasAttribute("tabindex"))
    val before = one(mount, ".card-overlay-body").innerHTML
    face.asInstanceOf[dom.html.Element].click()
    assertEquals(one(mount, ".card-overlay-body").innerHTML, before)

  test("the Festival's face in its overlay is inert: a press keeps where " +
      "the marker lies"):
    withShell { mount =>
      val festival = BannerFace.render(BannerState("darkest-secret",
        "festival", None, 1), Some("Mines"))
      mount.appendChild(festival)
      festival.click()
      assertEquals(one(mount, ".festival-copy-where").textContent,
        "Festival marker: at Mines")
      assertOverlayFaceIsInert(mount)
      assertEquals(one(mount, ".festival-copy-where").textContent,
        "Festival marker: at Mines")
    }

  test("a plain banner's face in its overlay is inert"):
    withShell { mount =>
      val mob = BannerFace.render(BannerState("peoples-favor", "mob", None,
        3), None)
      mount.appendChild(mob)
      mob.click()
      assertOverlayFaceIsInert(mount)
    }

  test("a ruled banner's face in its overlay is inert: a press keeps the " +
      "consent grid"):
    withShell { mount =>
      val origin = dom.document.createElement("button")
        .asInstanceOf[dom.html.Element]
      mount.appendChild(origin)
      CardInspection.openPower("Banner of the Darkest Secret · Wandering " +
        "Flame", Vector("Line"), Some(ConsentGrid(Vector(
          ConsentGridRow("Blue", "Blue", "ask")), None)), origin,
        Some(BannerState("darkest-secret", "wandering-flame", None, 1)))
      assertEquals(all(mount, ".card-overlay .consent-grid").size, 1)
      assertOverlayFaceIsInert(mount)
      assertEquals(all(mount, ".card-overlay .consent-grid").size, 1)
    }

  /** The Oath goal in the shared bank with `holder` and `side`. */
  private def sharedBankWithOath(holder: Option[String], side: String)
      : dom.Element =
    one(WorldBoardRenderer.world(TestProjections.actingProjection().copy(
      oathkeeper = Some(OathkeeperProjection("supremacy", holder, side,
        usurperLimited = false, winnerPlayerId = None))), BoardOptions.none),
      ".shared-bank")

  /** The owner's review (game art ruling R37): the shared bank shows the
    * Oath goal's card, not its title in words. */
  test("the shared bank shows the Oath goal as a card at its ratio, with " +
      "its image, and the drawn goal with art off"):
    val drawn = one(sharedBankWithOath(None, "oathkeeper"), ".oathkeeper-goal")
    assertEquals(drawn.tagName, "BUTTON")
    assertEquals(drawn.getAttribute("class"),
      "oathkeeper-goal card-plate card-plate-oath-goal")
    assertEquals(drawn.getAttribute("aria-label"), "Oathkeeper of Supremacy")
    assertEquals(all(drawn, ":scope > .card-name.printed")
      .map(_.textContent), Vector("Oathkeeper of Supremacy"))
    assertEquals(all(drawn, "img"), Vector.empty)
    TestArt.listingWholeCatalog()
    val withArt = one(sharedBankWithOath(None, "oathkeeper"),
      ".oathkeeper-goal")
    assertEquals(all(withArt, ":scope > img.art").map(_.getAttribute("src")),
      Vector("/art/cards/Supremacy%20Oath%20(NF).png"))
    val copy = withArt.cloneNode(true).asInstanceOf[dom.Element]
    all(copy, ".art").foreach(_.remove())
    assertEquals(copy.outerHTML, drawn.outerHTML)

  test("the Oath's holder stands in words under the goal's card"):
    Vector((None, "oathkeeper", "Oathkeeper: unheld"),
      (Some("Red"), "oathkeeper", "Oathkeeper: Red"),
      (Some("Red"), "usurper", "Usurper: Red")).foreach {
      (holder, side, words) =>
        val status = one(sharedBankWithOath(holder, side),
          ".oathkeeper-status")
        assertEquals(all(status, ":scope > *").map(_.getAttribute("class")
          .split(" ").head), Vector("oathkeeper-goal", "oathkeeper-holder"))
        assertEquals(one(status, ".oathkeeper-holder").textContent, words)
    }

