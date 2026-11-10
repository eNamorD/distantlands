package distantlands.model

/** The limits on a Negotiation author's promises (catalog batch 12,
  * ruling 10). */
class NegotiationPromiseSuite extends munit.FunSuite:
  private val longest = "x" * NegotiationPromise.MaxLength

  test("promises inside every limit pass"):
    assertEquals(NegotiationPromise.problem(Vector.empty), None)
    assertEquals(NegotiationPromise.problem(Vector("I will not attack you.",
      longest)), None)
    assertEquals(NegotiationPromise.problem(Vector.tabulate(
      NegotiationPromise.MaxPerKind)(i => s"Promise $i")), None)

  test("each broken limit is named"):
    assertEquals(NegotiationPromise.problem(Vector.tabulate(
      NegotiationPromise.MaxPerKind + 1)(i => s"Promise $i")),
      Some("more than 5 promises"))
    assertEquals(NegotiationPromise.problem(Vector("Same", "Same")),
      Some("the same promise twice"))
    assertEquals(NegotiationPromise.problem(Vector("  ")),
      Some("a blank promise"))
    assertEquals(NegotiationPromise.problem(Vector(" padded")),
      Some("a promise with surrounding spaces"))
    assertEquals(NegotiationPromise.problem(Vector("two\nlines")),
      Some("a promise with a control character"))
    assertEquals(NegotiationPromise.problem(Vector(longest + "x")),
      Some("a promise over 160 characters"))

  test("a line separator or an invisible format character is refused"):
    Vector("a\u2028b", "a\u2029b", "a\u200Bb", "a\u202Eb").foreach(text =>
      assertEquals(NegotiationPromise.problem(Vector(text)),
        Some("a promise with a control character"), text))

  test("a promise's own fault is named before the count or a repeat"):
    assertEquals(NegotiationPromise.problem(Vector("", "")),
      Some("a blank promise"))
    assertEquals(NegotiationPromise.problem(
      Vector.fill(NegotiationPromise.MaxPerKind + 1)("Same")),
      Some("more than 5 promises"))

  test("length counts characters, not UTF-16 units"):
    // Each emoji is one character and two UTF-16 units.
    val emoji = "\uD83D\uDE00" * NegotiationPromise.MaxLength
    assertEquals(NegotiationPromise.problem(Vector(emoji)), None)

  test("terms refuse promises that break a limit, of either kind"):
    intercept[IllegalArgumentException](NegotiationTerms(promises =
      Vector("")))
    intercept[IllegalArgumentException](NegotiationTerms(bindingPromises =
      Vector(longest + "x")))
