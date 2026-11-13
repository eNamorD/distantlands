package distantlands.frontend

/** A rectangle on the mat, in the mat image's pixels from its upper left. */
private[frontend] final case class MatRect(x: Double, y: Double, w: Double,
    h: Double):
  def right: Double = x + w
  def bottom: Double = y + h
  def ratio: Double = w / h

  def overlaps(that: MatRect): Boolean =
    x < that.right && that.x < right && y < that.bottom && that.y < bottom

  def within(that: MatRect): Boolean =
    x >= that.x && y >= that.y && right <= that.right && bottom <= that.bottom

  /** The CSS box of this rectangle inside `parent`, each length a
    * percentage of the parent's, so the board keeps its layout at any size.
    * Four decimals at most and no trailing zeros, the form the CSSOM reads
    * a percentage back in.
    */
  def percentOf(parent: MatRect): Vector[(String, String)] =
    def percent(value: Double, of: Double): String =
      f"${value / of * 100}%.4f".reverse.dropWhile(_ == '0')
        .dropWhile(_ == '.').reverse + "%"
    Vector("left" -> percent(x - parent.x, parent.w),
      "top" -> percent(y - parent.y, parent.h),
      "width" -> percent(w, parent.w), "height" -> percent(h, parent.h))

/** Where everything lies on the map mat (real proportions design).
  *
  * The numbers are measured from the mat image, 5787×2303 px, and recorded
  * in `docs/catalog/asset-proportions.md`. The mat prints a frame for each
  * site and none for its cards: a site's three card slots are derived, the
  * height of the frame and the width the denizen ratio gives it.
  */
private[frontend] object MatLayout:
  val Width = 5787.0
  val Height = 2303.0
  val Mat: MatRect = MatRect(0, 0, Width, Height)

  /** The board's width in the map's own pixels, before the map is scaled:
    * it puts a card slot at about 135px, the size a map card had before the
    * board took the mat's proportions. */
  val DesignWidthPx = 2400.0

  /** A card stands as tall as a site frame. */
  val CardH = 509.0
  val CardW: Double = CardH * 381 / 596
  val SiteW: Double = CardH * 770 / 596
  val SlotsPerSite = 3

  /** The site rows the mat prints in each column. */
  val Rows: Vector[Int] = Vector(2, 3, 3)

  private val FrameW = 669.0
  private val FrameLeft = Vector(71.0, 1994.0, 3945.0)
  private val RowTop = 428.0
  private val RowPitch = 534.5
  private val SlotGap = 48.0

  /** The mat's column for a region: the Cradle, the Provinces, the
    * Hinterland, left to right. A region with another id takes the column
    * of its place in the world, held to the last. */
  def column(regionId: String, at: Int): Int = regionId match
    case "cradle" => 0
    case "provinces" => 1
    case "hinterland" => 2
    case _ => math.max(0, math.min(at, FrameLeft.size - 1))

  def siteFrame(column: Int, row: Int): MatRect =
    MatRect(FrameLeft(column), RowTop + row * RowPitch, FrameW, CardH)

  /** The site card, centred in the frame the mat prints for it. */
  def siteCard(column: Int, row: Int): MatRect =
    val frame = siteFrame(column, row)
    MatRect(frame.x + (frame.w - SiteW) / 2, frame.y, SiteW, CardH)

  def cardSlot(column: Int, row: Int, slot: Int): MatRect =
    val frame = siteFrame(column, row)
    MatRect(frame.right + SlotGap + slot * (CardW + SlotGap), frame.y,
      CardW, CardH)

  /** A relic lies in a card slot, at its top. */
  def relicSlot(column: Int, row: Int, slot: Int): MatRect =
    cardSlot(column, row, slot).copy(h = CardW)

  /** A site's frame and its three slots together: one site's whole row. */
  def siteRow(column: Int, row: Int): MatRect =
    val frame = siteFrame(column, row)
    MatRect(frame.x, frame.y,
      cardSlot(column, row, SlotsPerSite - 1).right - frame.x, CardH)

  /** A region's discard pile: a card on its side, at the region's upper
    * right. */
  def discard(column: Int): MatRect =
    MatRect(FrameLeft(column) + 932, 80, CardH, CardW)

  def regionName(column: Int): MatRect =
    MatRect(FrameLeft(column), 60, 560, 240)

  val relicDeck: MatRect = MatRect(881, 1913, CardW, CardW)
  /** The world deck lies on its side, as the discards do. */
  val worldDeck: MatRect = MatRect(1278, 1913, CardH, CardW)
  val roundWheel: MatRect = MatRect(62, 1602, 642, 642)
  /** The tracker's line of text, in the clear band over the Cradle's foot. */
  val trackNote: MatRect = MatRect(71, 1500, 1790, 80)
  val visionCells: Vector[MatRect] =
    Vector(805.0, 1000.0, 1120.0, 1295.0, 1415.0, 1535.0)
      .map(x => MatRect(x, 1690, 103, 159))
  val visionsLabel: MatRect = MatRect(1680, 1650, 210, 150)

  val FavorBankSuits: Vector[String] =
    Vector("discord", "arcane", "order", "hearth", "beast", "nomad")
  private val BankCentre =
    Vector(2438.0, 2839.0, 3241.0, 3642.0, 4044.0, 4449.0)

  /** A suit's emblem and its bank circle, as one box. */
  def favorBank(suit: String): Option[MatRect] =
    Option(FavorBankSuits.indexOf(suit)).filter(_ >= 0)
      .map(at => MatRect(BankCentre(at) - 217, 2050, 310, 240))
