package distantlands.server

import java.nio.file.{Files, Path}

/** The stylesheet as the art suites read it: the text, a rule's
 * declarations, the art layer's block, and the colours and contrast
 * the stylesheet's variables give. */
trait StylesheetText extends munit.FunSuite:
  protected val css = Files.readString(Path.of("frontend", "styles.css"))

  /** The declarations of the first rule that opens a line with
    * `selector`. */
  protected def rule(selector: String)(using munit.Location): String =
    val opening = ("(?m)^" + java.util.regex.Pattern.quote(selector) +
      " \\{").r.findFirstMatchIn(css)
    assert(opening.nonEmpty, s"no rule $selector")
    css.substring(opening.get.start, css.indexOf("}", opening.get.start))

  /** The stylesheet without its comments, so a rule's selector list is
    * the selectors alone. */
  protected val uncommented = css.replaceAll("(?s)/\\*.*?\\*/", "")

  /** The art layer's rules: from their opening comment to the document
    * level overlay's comment, which follows them. */
  protected def artRules(using munit.Location): String =
    val from = css.indexOf("/* The host's art, laid")
    val to = css.indexOf("/* Document level, above .game-pane", from)
    assert(from >= 0 && to > from, "the art rules were not found")
    css.substring(from, to)

  /** A colour variable's value, as `#rrggbb`. */
  protected def colour(name: String)(using munit.Location): (Int, Int, Int) =
    val hex = s"--$name: #([0-9a-f]{6});".r.findFirstMatchIn(css)
      .getOrElse(fail(s"no colour --$name")).group(1)
    (Integer.parseInt(hex.substring(0, 2), 16),
      Integer.parseInt(hex.substring(2, 4), 16),
      Integer.parseInt(hex.substring(4, 6), 16))

  /** The WCAG contrast ratio of two sRGB colours. */
  protected def contrast(one: (Int, Int, Int), other: (Int, Int, Int)): Double =
    def luminance(value: (Int, Int, Int)): Double =
      def linear(channel: Int): Double =
        val share = channel / 255.0
        if share <= 0.03928 then share / 12.92
        else math.pow((share + 0.055) / 1.055, 2.4)
      0.2126 * linear(value._1) + 0.7152 * linear(value._2) +
        0.0722 * linear(value._3)
    val (light, dark) = (luminance(one) max luminance(other),
      luminance(one) min luminance(other))
    (light + 0.05) / (dark + 0.05)
