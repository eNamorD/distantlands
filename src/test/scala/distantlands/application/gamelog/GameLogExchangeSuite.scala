package distantlands.application.gamelog

import distantlands.gameplay.setup.FirstGameSetupFixture.catalog
import distantlands.model._
import distantlands.protocol.projection.{LogKind}
import distantlands.testkit.Table
import distantlands.testkit.Table.{p1, p2, p3}
import LogScripts._

/** The negotiation scripts stand p2 with the actor, p1, at Ancient City:
  * p2 is the partner and p3 the bystander. */
class GameLogExchangeSuite extends munit.FunSuite:
  private def lines(script: Script): Vector[String] =
    texts(format(script, None).filter(_.depth == 1))

  private val partner = p2
  private val bystander = p3

  test("a declined negotiation names who ended it, and nothing else"):
    val script = negotiationDeclined
    val all = lines(script)
    assertEquals(all.count(_.startsWith("Negotiation ended by ")), 1, all)
    assert(!all.exists(_.startsWith("Negotiated with ")), all)
    assert(!format(script, None).exists(_.kind == LogKind.Section))

  test("an agreed negotiation names the other negotiators once"):
    val script = negotiationAgreed
    val all = lines(script)
    val negotiated = all.filter(_.startsWith("Negotiated with "))
    assertEquals(negotiated.size, 1, all)
    assert(negotiated.head.contains(playerLabel(partner)), negotiated.head)

  test("an agreed deal's favor transfer names giver and recipient"):
    val script = negotiationAgreed
    assert(lines(script).contains(s"Negotiated with ${playerLabel(partner)}"),
      lines(script))
    // The script's deal gives the partner one favor.
    assert(lines(script).contains(
      s"${playerLabel(script.actor)} gave 1 favor to ${playerLabel(partner)}"),
      lines(script))

  test("an agreed promise is written once, quoted, naming its author and the others"):
    val script = negotiationPromised
    val expected = s"${playerLabel(script.actor)} promised " +
      s"${playerLabel(partner)}: “I will not attack you this round.”"
    Vector(None, Some(script.actor), Some(partner), Some(bystander))
      .foreach { viewer =>
        val all = texts(format(script, viewer).filter(_.depth == 1))
        assertEquals(all.count(_ == expected), 1, s"$viewer: $all")
      }
    assertEquals(lines(script).last, expected, lines(script))

  test("an agreed binding promise is written once, naming its author and " +
      "the others"):
    val script = negotiationBindingPromised
    val expected = s"${playerLabel(script.actor)} made a binding promise " +
      s"to ${playerLabel(partner)}: “I will give you 2 favor next round.”"
    Vector(None, Some(script.actor), Some(partner), Some(bystander))
      .foreach { viewer =>
        val all = texts(format(script, viewer).filter(_.depth == 1))
        assertEquals(all.count(_ == expected), 1, s"$viewer: $all")
      }
    assertEquals(lines(script).last, expected, lines(script))

  /** A bystander reads the disclosed card as its back, not by its slot.
    * The rule this line replaced read it through `LogWords.slotted` and
    * wrote "facedown adviser (slot 1)"; a note's card word has no slot
    * fallback, which slice 2 accepted and the wording ledger records under
    * "A facedown card's slot, in a negotiation only". */
  test("a disclosed adviser is named to its owner and recipient, a back to others"):
    val script = negotiationDisclosed
    val shown = (viewer: Option[PlayerId]) =>
      val all = texts(format(script, viewer))
      all.find(_.contains(" showed ")).getOrElse(
        fail(s"no disclosure line for $viewer in $all"))
    assert(shown(None).startsWith(playerLabel(partner)), shown(None))
    // Birdsong is p2's only adviser, so the slot wording would be slot 1.
    assert(!shown(Some(script.actor)).endsWith("(slot 1)"),
      shown(Some(script.actor)))
    assert(!shown(Some(partner)).endsWith("(slot 1)"), shown(Some(partner)))
    val (giver, recipient) = (playerLabel(partner), playerLabel(script.actor))
    assertEquals(shown(Some(bystander)), s"$giver showed $recipient a Denizen")

  test("a site given under Deed Writer writes its own lines, not the " +
      "generic warband line"):
    val script = negotiationSiteGiven
    val all = texts(format(script, Some(script.actor)))
    val (giver, recipient) = (playerLabel(partner), playerLabel(script.actor))
    assert(all.contains(s"$giver gave Broken Peaks to $recipient"), all)
    assert(all.contains(s"$recipient placed 2 warbands at Broken Peaks"), all)
    assert(!all.exists(_.startsWith("Moved ")), all)

  test("a given facedown adviser is named to both who held it, a back to " +
      "others"):
    val script = negotiationAdviserGiven
    val gave = (viewer: Option[PlayerId]) =>
      val all = texts(format(script, viewer))
      all.find(_.contains(" gave ")).getOrElse(
        fail(s"no transfer line for $viewer in $all"))
    val (giver, recipient) = (playerLabel(partner), playerLabel(script.actor))
    Vector(Some(script.actor), Some(partner)).foreach(viewer =>
      assertEquals(gave(viewer), s"$giver gave Birdsong to $recipient",
        viewer.toString))
    assertEquals(gave(Some(bystander)), s"$giver gave a Denizen to $recipient")

  test("a used power is named by its source card, once"):
    val all = texts(formatWithoutNotes(silverTongue, None).filter(_.depth == 1))
    assertEquals(all.count(_.startsWith("Used ")), 1, all)
    // A faceup adviser, so every viewer reads its name.
    assert(all.contains("Used Silver Tongue"), all)

  test("a banner's power is named by its banner, not its id"):
    // Any ready state will do: a banner's name is the same on every board.
    val ready = Table.start.ready
    val words = new LogWords(catalog, presentation)
    assertEquals(words.power(ready, p1,
      PowerId("banner.darkest-secret.wandering-flame.move"), None)
      .map(_.text).mkString, "Darkest Secret")

  test("a private deal opens in private, and its transfer reads to all"):
    val script = negotiationWhispered
    val opened = s"Negotiated with ${playerLabel(p3)}, in private."
    val gave = s"${playerLabel(script.actor)} gave 1 favor to " +
      s"${playerLabel(p3)}"
    Vector(None, Some(p1), Some(p2), Some(p3)).foreach { viewer =>
      val all = texts(format(script, viewer).filter(_.depth == 1))
      assertEquals(all.count(_ == opened), 1, s"$viewer: $all")
      assertEquals(all.count(_ == gave), 1, s"$viewer: $all")
    }

  test("a private deal's promise reads only to its participants"):
    val script = negotiationWhispered
    val promise = s"${playerLabel(script.actor)} promised " +
      s"${playerLabel(p3)}: “I will not attack you this round.”"
    Vector(Some(p1) -> 1, Some(p3) -> 1, Some(p2) -> 0, None -> 0).foreach {
      (viewer, count) =>
        val all = texts(format(script, viewer))
        assertEquals(all.count(_ == promise), count, s"$viewer: $all")
    }
