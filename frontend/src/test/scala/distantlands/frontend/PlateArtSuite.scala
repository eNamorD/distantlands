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

