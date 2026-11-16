package distantlands.application.gamelog

import distantlands.gameplay.setup.OrderedWindow
import distantlands.model._
import distantlands.protocol.projection.{LogEntry, LogSpan}
import distantlands.testkit.Outcomes
import distantlands.testkit.Unwrap.present
import LogScripts._
import LaterGameLogScripts.{laterGamePublicAmbitions, laterGameWoken}

class GameLogSetupSuite extends munit.FunSuite:
  private def setupLines(script: Script, viewer: Option[PlayerId])
      : Vector[LogEntry] = format(script, viewer)
    .takeWhile(entry => text(entry) != "Round 1").filter(_.depth == 1)

  /** An adviser keep: Quiet Ambitions' "kept a legacy" also says "kept". */
  private def keptAdviser(line: String): Boolean =
    line.contains(" kept ") && !line.endsWith(" kept a legacy")

  test("every player places a pawn and keeps an adviser, in turn order"):
    // `woken` replays Setup: its placements and keeps are the subject.
    val script = woken
    val lines = texts(setupLines(script, None))
    assertEquals(lines.count(_.contains(" placed their pawn at ")),
      script.players.size, lines)
    assertEquals(lines.count(keptAdviser), script.players.size, lines)
    // Turn order runs from the first player through the seats.
    val ready = Outcomes.ready(script.history.steps.last.after)
    val seats = ready.game.current.players.map(_.player)
    val first = seats.indexOf(ready.setup.firstPlayer)
    val order = (seats.drop(first) ++ seats.take(first)).map(_.value)
    assertEquals(lines.filter(_.contains(" placed their pawn at "))
      .map(_.takeWhile(_ != ' ')), order)
    assertEquals(lines.filter(keptAdviser).map(_.takeWhile(_ != ' ')),
      order)
    assertEquals(lines.filter(_.endsWith(" kept a legacy"))
      .map(_.takeWhile(_ != ' ')), order)

  test("a kept adviser is named to its owner and read by its back by others"):
    // `woken` replays Setup: its keep lines are the subject.
    val script = woken
    val mine = setupLines(script, Some(script.actor))
      .filter(entry => keptAdviser(text(entry)))
    val own = mine.find(_.spans.head == LogSpan.Player(script.actor.value,
      playerLabel(script.actor))).getOrElse(
        fail(s"no kept line for ${script.actor} in ${texts(mine)}"))
    assert(own.spans.exists(_.isInstanceOf[LogSpan.Card]), own.spans)
    mine.filterNot(_ == own).foreach { entry =>
      assert("^.+ kept a (Denizen|Vision)$".r.matches(text(entry)), text(entry))
    }

  test("a kept legacy names no card, to its owner or anyone else"):
    // `woken` replays Setup: its Quiet Ambitions keeps are the subject.
    val script = woken
    (None +: script.players.map(Some(_))).foreach { viewer =>
      val kept = setupLines(script, viewer)
        .filter(entry => text(entry).endsWith(" kept a legacy"))
      assertEquals(kept.size, script.players.size, viewer)
      assert(kept.forall(!_.spans.exists(_.isInstanceOf[LogSpan.Card])),
        kept)
    }

  test("Public Ambitions names its row, card by card, to every viewer"):
    // A later game's Setup on Public Ambitions: its row line is the subject.
    val script = laterGamePublicAmbitions
    val row = Outcomes.ready(script.history.steps.last.after).game.current
      .commonCards.publicLegacies
    assertEquals(row.size, 4)
    val names = row.map(id => present(
      distantlands.gameplay.setup.FirstGameSetupFixture.catalog.legacy(id),
      id.value).name)
    Vector(Some(script.actor), Some(script.otherSeat), None).foreach {
      viewer =>
        val dealt = format(script, viewer).filter(entry =>
          text(entry).startsWith("Public Ambitions dealt "))
        assertEquals(dealt.map(text), Vector("Public Ambitions dealt " +
          names.init.mkString(", ") + " and " + names.last), viewer)
        assertEquals(dealt.flatMap(_.spans).collect {
          case LogSpan.Card(id, name) => id -> name },
          row.map(_.value).zip(names), viewer)
    }

  test("a card revealed in Reveal Cards is named to every viewer"):
    // Replays Setup, answering Reveal Cards by revealing all: the reveal
    // lines are the subject.
    val script = wokenRevealingAll
    val ready = Outcomes.ready(script.history.steps.last.after)
    val revealed = ready.game.current.players.map(player =>
      player.advisers.count(_ match
        case d: DenizenState => d.orientation == Orientation.FaceUp
        case _ => false)).sum
    assert(revealed > 0)
    val public = texts(setupLines(script, None)).filter(_.contains(" revealed "))
    assertEquals(public.size, revealed, public)
    public.foreach(line =>
      assert(!"^.+ revealed a (Denizen|Vision)$".r.matches(line), line))
    script.players.foreach { viewer =>
      assertEquals(texts(setupLines(script, Some(viewer)))
        .filter(_.contains(" revealed ")), public, viewer)
    }

  test("a later game's Chancellor takes a Reliquary relic first, named to " +
      "nobody"):
    // `laterGameWoken` replays a later game's Setup: its first line is the
    // subject.
    val script = laterGameWoken
    Vector(None, Some(script.actor), Some(PlayerId("p2"))).foreach(viewer =>
      assertEquals(texts(setupLines(script, viewer)).headOption,
        Some("p1 took a relic from the Reliquary")))

  test("the power-order pick writes no line of its own (catalog batch 13, " +
      "ruling 4)"):
    assert(formatter.procedures.of(TriggeredProcedureRef.Setup).narrates(
      OrderedWindow.decisionId(PlayerId("p2"), 0)))
