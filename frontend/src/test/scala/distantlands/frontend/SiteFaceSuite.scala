package distantlands.frontend

import distantlands.frontend.Dom.{all, firstMatch}
import distantlands.frontend.TestSites.*
import distantlands.testkit.Unwrap.as
import org.scalajs.dom

/** The site box, which carries its own numbers in its corners the way a
  * printed Oath site does. Runs under jsdom (`Test / jsEnv` in `build.sbt`).
  */
class SiteFaceSuite extends munit.FunSuite:
  private def glyphs(node: dom.Element, selector: String): Vector[String] =
    all(node, s"$selector .token-glyph").map(_.getAttribute("aria-label"))

  /** The box classes of a row's cards. */
  private def boxClasses(row: dom.Element): Vector[String] =
    all(row, ".card-face").map(_.getAttribute("class"))

  private def denizen(id: String, name: String, suit: String): GameSiteCard =
    GameSiteCard(id, name, Some(CardDetails(id, "denizen", name,
      suit = Some(suit), orientation = Some("face-up"))))

  /** Deep Woods holds two denizens in three slots; a test that depends on
    * its loose tokens, defense or powers sets them.
    */
  private val woods = quietSite("site:woods", "Deep Woods")
    .withLooseTokens(favor = 2, secrets = 1).withDenizenCapacity(3)
    .withDenizens(denizen("denizen:fox", "Fox", "beast"),
      denizen("denizen:owl", "Owl", "arcane"))
    .withDefense(2)
    .copy(powers = Vector(SitePower("coast", "Coast",
      Some("Travel along the Coast route."))))

  /** Salt Flats holds one denizen and two facedown relics, no loose tokens;
    * a test that depends on its defense or Recover difficulty sets them.
    */
  private val saltFlats = quietSite("site:salt-flats", "Salt Flats")
    .withDenizenCapacity(1).withRelicCapacity(2)
    .withDenizens(denizen("denizen:dune", "Dune", "nomad"))
    .withRelics(facedown = 2)
    .copy(recoverDifficulty = Some(4))

  test("a site's denizens and relics share one row, with no headings"):
    // One denizen and two facedown relics make three boxes in the row.
    val node = ServerUiSupport.siteDetails(saltFlats.copy(denizenCapacity = 1,
      relicCapacity = 2))
    val row = firstMatch(node, ".site-cards")
      .getOrElse(fail("no site card row"))
    assertEquals(all(row, ".card-face").size, 3)
    assertEquals(all(node, ".site-denizens"), Vector.empty)
    assertEquals(all(node, ".site-relics"), Vector.empty)
    assert(!node.textContent.contains("Denizens"), node.textContent)
    assert(!node.textContent.contains("Relics"), node.textContent)
    assert(!node.textContent.contains("None"), node.textContent)

  test("empty denizen slots fill the row up to capacity, ahead of the relics"):
    val row = firstMatch(ServerUiSupport.siteDetails(woods.copy(
      denizenCapacity = 2, relicCapacity = 1,
      denizens = Vector(denizen("denizen:fox", "Fox", "beast")),
      relics = GameSiteRelics(Vector(TestSites.hiddenRelic)))), ".site-cards")
      .getOrElse(fail("no site card row"))
    assertEquals(boxClasses(row), Vector(
      "card-face card-face-denizen",
      "card-face card-face-denizen card-slot-empty",
      "card-face card-face-relic card-face-down"))

  /** A slot a site owns is drawn whether or not a card sits in it, so the row
    * reads as the site's capacity rather than as its current contents.
    */
  /** A relic's back is what the card prints on that side. */
  test("an unknown relic's back letter is marked printed"):
    val back = ServerUiSupport.facedownCard("relic")
    assertEquals(all(back, ".card-back-letter").map(_.getAttribute("class")),
      Vector("card-back-letter printed"))

  test("an empty relic slot is drawn like an empty denizen slot"):
    val row = firstMatch(ServerUiSupport.siteDetails(woods.copy(
      denizenCapacity = 1, relicCapacity = 2, denizens = Vector.empty,
      relics = GameSiteRelics())), ".site-cards")
      .getOrElse(fail("no site card row"))
    assertEquals(all(row, ".card-face").map(_.getAttribute("class")), Vector(
      "card-face card-face-denizen card-slot-empty",
      "card-face card-face-relic card-slot-empty",
      "card-face card-face-relic card-slot-empty"))
    assertEquals(all(row, ".card-face").map(_.getAttribute("aria-label")),
      Vector("Empty denizen slot", "Empty relic slot", "Empty relic slot"))

  test("a relic in a slot leaves the rest of the relic slots drawn"):
    // One denizen, then two relic slots that no relic card has been put in.
    val row = firstMatch(ServerUiSupport.siteDetails(saltFlats.copy(
      denizenCapacity = 1, relicCapacity = 2)), ".site-cards")
      .getOrElse(fail("no site card row"))
    assertEquals(boxClasses(row), Vector(
      "card-face card-face-denizen",
      "card-face card-face-relic card-face-down",
      "card-face card-face-relic card-face-down"))

  /** The letter badge repeated the first character of the name beside it. */
  test("a site heading carries no letter badge"):
    val heading = ServerUiSupport.siteHeading(woods.copy(label = "Deep Woods"))
    assertEquals(all(heading, ".site-visual"), Vector.empty)
    assertEquals(firstMatch(heading, ".site-name").map(_.textContent),
      Some("Deep Woods"))

  /** The three cells are always present so the name stays in the middle one,
    * whether or not the site holds anything.
    */
  test("a heading keeps its token cell even when the site holds nothing"):
    val heading = ServerUiSupport.siteHeading(woods.copy(looseFavor = 0,
      looseSecrets = 0))
    assertEquals(all(heading, ".site-tokens").size, 1)
    assertEquals(firstMatch(heading, ".site-tokens").map(_.textContent),
      Some(""))

  test("loose favor and secrets sit in the site's upper left as glyphs"):
    val heading = ServerUiSupport.siteHeading(woods.copy(label = "Deep Woods",
      looseFavor = 2, looseSecrets = 1))
    assertEquals(glyphs(heading, ".site-tokens"), Vector("favor", "secret"))
    assertEquals(all(heading, ".site-tokens .site-token-count")
      .map(_.textContent), Vector("2", "1"))
    assertEquals(firstMatch(heading, ".site-name").map(_.textContent),
      Some("Deep Woods"))

  test("a token the site does not hold is not drawn at all"):
    val heading = ServerUiSupport.siteHeading(
      woods.copy(looseFavor = 0, looseSecrets = 3))
    assertEquals(glyphs(heading, ".site-tokens"), Vector("secret"))
    assertEquals(all(heading, ".site-tokens .site-token-count")
      .map(_.textContent), Vector("3"))

  test("defense is a die in the upper right, drawn once per point"):
    val heading = ServerUiSupport.siteHeading(woods.copy(defense = 2))
    assertEquals(glyphs(heading, ".site-defense"),
      Vector("defense die", "defense die"))
    assertEquals(firstMatch(heading, ".site-defense")
      .map(_.getAttribute("aria-label")), Some("Defense 2"))

  /** A site anyone can walk into still says so, rather than leaving the
    * reader to decide whether a missing die means zero or means unknown.
    */
  test("an undefended site says zero instead of drawing no die"):
    val heading = ServerUiSupport.siteHeading(saltFlats.copy(defense = 0))
    assertEquals(glyphs(heading, ".site-defense"), Vector.empty)
    assertEquals(firstMatch(heading, ".site-defense").map(_.textContent),
      Some("0"))
    assertEquals(firstMatch(heading, ".site-defense")
      .map(_.getAttribute("aria-label")), Some("Defense 0"))

  test("site powers sit in the lower left, the requirement in the lower right"):
    val node = ServerUiSupport.siteDetails(woods.copy(
      powers = Vector(SitePower("coast", "Coast",
        Some("Travel along the Coast route."))),
      recoverDifficulty = Some(5)))
    val footer = firstMatch(node, ".site-footer")
      .getOrElse(fail("no site footer"))
    assertEquals(as[dom.Element](footer.firstChild).getAttribute("class"),
      "site-powers")
    assertEquals(all(footer, ".site-power").map(_.textContent), Vector("Coast"))
    assertEquals(as[dom.Element](footer.lastChild).getAttribute("class"),
      "site-requirement printed")

  /** The box has room for one line of powers, so a second power joins the
    * first on it rather than wrapping onto a line the box then clips. Each
    * power keeps its rules text on hover.
    */
  test("a site's powers share one line, each with its text on hover"):
    val node = ServerUiSupport.siteDetails(woods.copy(powers = Vector(
      SitePower("river", "River", Some("Part of the River route.")),
      SitePower("coast", "Coast", Some("Travel along the Coast route.")),
      SitePower("enduring", "Enduring", None))))
    val line = firstMatch(node, ".site-powers")
      .getOrElse(fail("no site powers"))
    assertEquals(line.textContent, "River · Coast · Enduring")
    assertEquals(all(line, ".site-power").map(_.getAttribute("data-tip")), Vector(
      "River: Part of the River route.",
      "Coast: Travel along the Coast route.",
      "Enduring"))

  test("forge and recover are one corner, never both"):
    val recovering = saltFlats.copy(recoverDifficulty = Some(4))
    val recover = firstMatch(ServerUiSupport.siteDetails(recovering),
      ".site-requirement").getOrElse(fail("no requirement"))
    assertEquals(recover.textContent, "Recover 4")
    assertEquals(glyphs(recover, ""), Vector.empty)

    val forged = firstMatch(ServerUiSupport.siteDetails(recovering.copy(
      forgeCost = Some(ForgeCost(2, 1)))), ".site-requirement")
      .getOrElse(fail("no requirement"))
    assert(forged.textContent.startsWith("Forge"), forged.textContent)
    assert(!forged.textContent.contains("Recover"), forged.textContent)
    assertEquals(glyphs(forged, ""), Vector("favor", "favor", "secret"))

  test("a site with neither requirement leaves the corner out"):
    assertEquals(all(ServerUiSupport.siteDetails(woods.copy(
      recoverDifficulty = None, forgeCost = None)), ".site-requirement"),
      Vector.empty)

  /** Zoomed far out a site keeps its defense and its forces in a short form
    * the stylesheet swaps in, so a zoom never re-renders (ruling R5). The
    * short forms are for the eye: assistive technology reads the long ones,
    * and the hover note carries the long text. */
  test("defense has a short form: one die and its count, hidden from " +
      "assistive technology"):
    val heading = ServerUiSupport.siteHeading(woods.copy(defense = 2))
    val short = firstMatch(heading, ".site-defense-short")
      .getOrElse(fail("no short defense"))
    assertEquals(short.getAttribute("aria-hidden"), "true")
    assertEquals(all(short, ".token-glyph").map(_.getAttribute("class")),
      Vector("token-glyph token-defense-die"))
    assertEquals(short.textContent, "2")
    assertEquals(short.getAttribute(HoverTip.Attribute), "Defense 2")
    assert(short.classList.contains("printed"))
    assertEquals(firstMatch(heading, ".site-defense")
      .map(_.getAttribute("aria-label")), Some("Defense 2"))

  test("forces have a short form: the warband in the force's colour and " +
      "its count"):
    val forces = SiteForces.Bandit(3, "Bandit Warbands")
    val details = ServerUiSupport.siteDetails(woods.copy(forces = Some(forces)))
    val short = firstMatch(details, ".site-forces-short")
      .getOrElse(fail("no short forces"))
    assert(short.classList.contains("force-bandit"), short.getAttribute("class"))
    assertEquals(short.getAttribute("aria-hidden"), "true")
    assertEquals(all(short, ".token-glyph").map(_.getAttribute("class")),
      Vector("token-glyph token-warband"))
    assertEquals(short.textContent, "3")
    assertEquals(short.getAttribute(HoverTip.Attribute), "Bandit Warbands x3")
    assert(!short.classList.contains("printed"))
    assertEquals(firstMatch(details, "p.site-forces").map(_.textContent),
      Some("Bandit Warbands x3"))

  /** An Exile's and the Empire's short forms carry their force's colour
    * class, which the glyph takes too, and the long text as the hover note;
    * the Empire's carries its outline ring in the stylesheet (ruling R7). */
  test("the short forces of an Exile and of the Empire carry their class " +
      "and their words"):
    Vector(
      SiteForces.Exile(12, "purple-exile", distantlands.model.PlayerColor.Purple,
        "Purple Warbands") -> ("force-purple", "Purple Warbands x12"),
      SiteForces.Imperial(10, "Imperial Warbands") ->
        ("force-empire", "Imperial Warbands x10")).foreach {
      case (forces, (cssClass, words)) =>
        val short = firstMatch(ServerUiSupport.siteDetails(
          woods.copy(forces = Some(forces))), ".site-forces-short")
          .getOrElse(fail("no short forces"))
        assert(short.classList.contains(cssClass), short.getAttribute("class"))
        assertEquals(short.getAttribute(HoverTip.Attribute), words)
        assertEquals(short.textContent, forces.count.toString)
    }
