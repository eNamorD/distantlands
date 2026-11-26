package distantlands.frontend

/** The mat's coordinate table (real proportions design): every slot lies
  * where the mat prints it and has the proportions of the card it holds,
  * so the mat image can be laid under the board without moving anything.
  */
class MatLayoutSuite extends munit.FunSuite:
  import MatLayout.*

  /** The site rows the mat prints in each column. */
  private val Rows = Vector(2, 3, 3)

  private val sites = for column <- 0 until 3; row <- 0 until Rows(column)
    yield column -> row
  private val slots = for (column, row) <- sites; slot <- 0 until SlotsPerSite
    yield cardSlot(column, row, slot)
  private val banks = FavorBankSuits.flatMap(favorBank)

  /** Every rectangle the board draws, each once. */
  private val everything: Vector[MatRect] =
    sites.map(siteFrame).toVector ++ slots ++
      (0 until 3).map(discard) ++ (0 until 3).map(regionName) ++
      Vector(relicDeck, worldDeck, roundWheel, trackNote, visionsLabel) ++
      visionCells ++ banks

  private def near(actual: Double, expected: Double): Boolean =
    math.abs(actual / expected - 1) < 0.01

  test("the mat has the image's proportions"):
    assertEquals(Width, 5787.0)
    assertEquals(Height, 2303.0)

  test("the mat prints two site rows in the Cradle and three elsewhere"):
    assertEquals(sites.size, 8)
    assert(siteFrame(0, 1).bottom < trackNote.y)

  test("every rectangle lies inside the mat"):
    everything.foreach(rect => assert(rect.within(Mat), rect.toString))

  test("no two rectangles overlap"):
    for a <- everything.indices; b <- everything.indices if a < b do
      assert(!everything(a).overlaps(everything(b)),
        s"${everything(a)} overlaps ${everything(b)}")

  test("each site has three card slots to its right, of the denizen ratio"):
    sites.foreach { (column, row) =>
      val frame = siteFrame(column, row)
      val row3 = (0 until SlotsPerSite).map(cardSlot(column, row, _))
      assertEquals(row3.size, 3)
      row3.foreach { slot =>
        assert(slot.x > frame.right, slot.toString)
        assertEquals(slot.y, frame.y)
        assert(near(slot.ratio, 381.0 / 596), slot.toString)
      }
    }

  /** The mat draws each frame by hand: its 5 to 6 px line strays up to 8
    * px outside the nominal rectangle. The site's card covers the frame
    * and its line (game art ruling R38); the image is fitted with
    * `cover`, which trims about one percent of its height. */
  test("a site card covers its frame and the frame's line, and reaches " +
      "no neighbour"):
    sites.foreach { (column, row) =>
      val card = siteCard(column, row)
      val frame = siteFrame(column, row)
      assertEquals(card, MatRect(frame.x - 8, frame.y - 8, frame.w + 16,
        frame.h + 16))
      assert(!card.overlaps(cardSlot(column, row, 0)), card.toString)
      if sites.contains((column, row + 1)) then
        assert(!card.overlaps(siteCard(column, row + 1)), card.toString)
      assert(card.within(Mat), card.toString)
      // Close to the site card's own ratio: the trim is under 2%.
      assert(math.abs(card.ratio / (770.0 / 596) - 1) < 0.02, card.toString)
    }

  private def assertRect(actual: MatRect, x: Double, y: Double, w: Double,
      h: Double)(using munit.Location): Unit =
    assertEqualsDouble(actual.x, x, 0.01, actual.toString)
    assertEqualsDouble(actual.y, y, 0.01, actual.toString)
    assertEqualsDouble(actual.w, w, 0.01, actual.toString)
    assertEqualsDouble(actual.h, h, 0.01, actual.toString)

  /** The pieces on a site stand in two rows on its card (game art
    * rulings R48, R49): pawns from 19% of the frame's height for 30%,
    * warbands from 50% for 20%, both 4% in from each side. The card
    * prints its resources and its die above them and its power shield
    * and name below. */
  test("a site's pawns and warbands have a row each on its card, between " +
      "the print's resources and its power shield"):
    assertRect(sitePawnRow(0, 0), 97.76, 524.71, 615.48, 152.7)
    assertRect(siteWarbandRow(0, 0), 97.76, 682.5, 615.48, 101.8)
    sites.foreach { (column, row) =>
      val frame = siteFrame(column, row)
      val pawns = sitePawnRow(column, row)
      val warbands = siteWarbandRow(column, row)
      assert(pawns.within(frame) && warbands.within(frame))
      assert(!pawns.overlaps(warbands))
      // The print's coins and die end 18% down the card.
      assert(pawns.y >= frame.y + frame.h * 0.18)
      assert(warbands.bottom <= sitePowerShield(column, row).y)
    }

  /** The shield a site card prints its powers in is the same on all 24
    * site images (770 by 596): x 21 to 160, from its peak at y 425 to
    * the card's foot. The image covers the card's box, scaled to its
    * width and trimmed evenly at the top and the foot. */
  test("the power shield lies where the site card prints it, through " +
      "the image's cover fit"):
    assertRect(sitePowerShield(0, 0), 81.68, 795.48, 123.66, 149.52)
    sites.foreach { (column, row) =>
      val shield = sitePowerShield(column, row)
      assert(shield.within(siteCard(column, row)), shield.toString)
      assertEqualsDouble(shield.bottom, siteCard(column, row).bottom, 0.001)
    }

  /** One or two powers a site (game art ruling R55). The projection's
    * order is not the print's, so the side comes from the power's kind. */
  test("one power takes the whole shield; two stand as the card prints " +
      "them, in the order they were given"):
    val shield = sitePowerShield(0, 0)
    assertEquals(sitePowerRegions(Vector("mountain"), 0, 0), Vector(shield))
    assertEquals(sitePowerRegions(Vector.empty, 0, 0), Vector.empty)
    // The red line stands at x 90 of the image: 61.38 of the shield's
    // 123.66 lie left of it.
    val left = MatRect(shield.x, shield.y, 61.38, shield.h)
    val right = MatRect(shield.x + 61.38, shield.y, 62.27, shield.h)
    def assertRegions(kinds: Vector[String], expected: Vector[MatRect])(
        using munit.Location): Unit =
      val regions = sitePowerRegions(kinds, 0, 0)
      assertEquals(regions.size, expected.size)
      regions.zip(expected).foreach((region, rect) =>
        assertRect(region, rect.x, rect.y, rect.w, rect.h))
    // Headwaters arrives as River, Mountain and prints Mountain left.
    assertRegions(Vector("river", "mountain"), Vector(right, left))
    // Ancient City: Enduring left, River right.
    assertRegions(Vector("enduring", "river"), Vector(left, right))
    // Tidal Marshes: River left, Coast right.
    assertRegions(Vector("coast", "river"), Vector(right, left))
    // The islands print the Coast's ship above the Island's "+2", which
    // begins at y 527 of the image: 90.74 of the shield's 149.52.
    val upper = MatRect(shield.x, shield.y, shield.w, 90.74)
    val lower = MatRect(shield.x, shield.y + 90.74, shield.w, 58.78)
    assertRegions(Vector("coast", "island"), Vector(upper, lower))
    assertRegions(Vector("island", "coast"), Vector(lower, upper))

  test("a site's row spans its frame and its three slots"):
    sites.foreach { (column, row) =>
      // The row and its site card together: what a box must hold to show
      // the whole row (the Atlas in Chronicle Details).
      val whole = siteRowWithCard(column, row)
      assert(siteRow(column, row).within(whole), whole.toString)
      assert(siteCard(column, row).within(whole), whole.toString)
      assertEquals((whole.x, whole.y, whole.w, whole.h),
        (siteRow(column, row).x - 8, siteRow(column, row).y - 8,
          siteRow(column, row).w + 16, 525.0))
      val span = siteRow(column, row)
      assertEquals(span.x, siteFrame(column, row).x)
      assertEquals(span.right, cardSlot(column, row, 2).right)
    }

  test("a relic slot is the square top of its card slot"):
    val slot = cardSlot(1, 0, 2)
    val relic = relicSlot(1, 0, 2)
    assert(near(relic.ratio, 1.0), relic.toString)
    assertEquals((relic.x, relic.y, relic.w), (slot.x, slot.y, slot.w))

  test("the festival marker lies across the middle of a site's three " +
      "slots, a little short of their ends"):
    sites.foreach { (column, row) =>
      val marker = festivalMarker(column, row)
      val first = cardSlot(column, row, 0)
      val last = cardSlot(column, row, SlotsPerSite - 1)
      assert(marker.x > first.x && marker.right < last.right, marker.toString)
      assert(marker.w > 0.9 * (last.right - first.x), marker.toString)
      assert(near(marker.y + marker.h / 2, first.y + first.h / 2),
        marker.toString)
      assert(marker.h < first.h / 4, marker.toString)
    }

  /** A bank's two printed circles (game art ruling R42): the large one
    * the favor lies in and the suit's emblem beside it. */
  test("a bank's circle and its emblem are squares inside the bank's " +
      "box, the emblem at the circle's left, their rings touching"):
    FavorBankSuits.foreach { suit =>
      val bank = favorBank(suit).get
      val circle = favorBankCircle(suit).get
      val emblem = favorBankEmblem(suit).get
      assert(near(circle.ratio, 1.0) && near(emblem.ratio, 1.0), suit)
      assertEquals((circle.w, emblem.w), (186.0, 122.0), suit)
      assert(circle.within(bank) && emblem.within(bank), suit)
      assert(emblem.x < circle.x && emblem.right <= circle.x + 5, suit)
    }
    assertEquals(favorBankCircle("no-such-suit"), None)
    assertEquals(favorBankEmblem("no-such-suit"), None)

  test("a discard and the world deck are denizen cards on their side"):
    ((0 until 3).map(discard) :+ worldDeck).foreach(rect =>
      assert(near(rect.ratio, 596.0 / 381), rect.toString))

  test("the relic deck is square and the round wheel is round"):
    assert(near(relicDeck.ratio, 1.0))
    assert(near(roundWheel.ratio, 1.0))

  test("the Visions Drawn track has six cells, left to right"):
    assertEquals(visionCells.size, 6)
    assertEquals(visionCells.map(_.x), visionCells.map(_.x).sorted)

  test("the favor banks stand in the mat's order"):
    assertEquals(FavorBankSuits,
      Vector("discord", "arcane", "order", "hearth", "beast", "nomad"))
    assertEquals(banks.size, 6)
    assertEquals(banks.map(_.x), banks.map(_.x).sorted)
    assertEquals(favorBank("no-such-suit"), None)

  test("a region takes its column from its id, and an unknown one from " +
      "its place"):
    assertEquals(column("cradle", 2), 0)
    assertEquals(column("provinces", 0), 1)
    assertEquals(column("hinterland", 0), 2)
    assertEquals(column("other", 1), 1)
    assertEquals(column("other", 7), 2)

  /** Written as the CSSOM reads a percentage back, with no trailing zeros,
    * so a placed element's inline style compares equal to its rectangle. */
  test("a rectangle reads as percentages of its parent"):
    assertEquals(MatRect(50, 25, 100, 50).percentOf(MatRect(0, 0, 200, 100)),
      Vector("left" -> "25%", "top" -> "25%",
        "width" -> "50%", "height" -> "50%"))
    assertEquals(MatRect(1, 0, 3, 1).percentOf(MatRect(0, 0, 8, 1)),
      Vector("left" -> "12.5%", "top" -> "0%",
        "width" -> "37.5%", "height" -> "100%"))
    assertEquals(MatRect(1, 0, 1, 1).percentOf(MatRect(0, 0, 3, 1)).head,
      "left" -> "33.3333%")

  /** The survey itself, written out here rather than derived from
    * `MatLayout`, so a number moved off the mat fails even when every
    * invariant above still holds. A number corrected against the mat image
    * changes here in the same commit. */
  test("every surveyed number is where the mat prints it"):
    assertEquals((0 until 3).map(siteFrame(_, 0).x), Vector(71.0, 1994.0, 3945.0))
    sites.foreach((column, row) =>
      assertEquals((siteFrame(column, row).w, siteFrame(column, row).h),
        (669.0, 509.0)))
    assertEquals((0 until 3).map(siteFrame(1, _).y), Vector(428.0, 962.5, 1497.0))
    assertEquals(cardSlot(0, 0, 0).x, 788.0)
    assertEquals((0 until 3).map(discard(_)).map(rect => (rect.x, rect.y)),
      Vector((1003.0, 80.0), (2926.0, 80.0), (4877.0, 80.0)))
    assertEquals((relicDeck.x, relicDeck.y), (881.0, 1913.0))
    assertEquals((worldDeck.x, worldDeck.y), (1278.0, 1913.0))
    assertEquals(roundWheel, MatRect(62, 1602, 642, 642))
    assertEquals((0 until 3).map(regionName),
      Vector(MatRect(46, 60, 900, 240), MatRect(1969, 60, 900, 240),
        MatRect(3920, 60, 900, 240)))
    assertEquals(visionCells.map(_.x),
      Vector(805.0, 1000.0, 1120.0, 1295.0, 1415.0, 1535.0))
    visionCells.foreach(cell =>
      assertEquals((cell.y, cell.w, cell.h), (1690.0, 103.0, 159.0)))
    assertEquals(FavorBankSuits,
      Vector("discord", "arcane", "order", "hearth", "beast", "nomad"))
    assertEquals(banks.map(bank => bank.x + 217),
      Vector(2438.0, 2839.0, 3241.0, 3642.0, 4044.0, 4449.0))
