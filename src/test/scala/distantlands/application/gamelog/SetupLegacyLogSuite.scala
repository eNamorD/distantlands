package distantlands.application.gamelog

import distantlands.model._

/** The Game Log for the legacies' Setup powers (Legacy Setup powers
  * design). */
class SetupLegacyLogSuite extends munit.FunSuite:
  test("Seer's peeks write no line, and its holder still knows the Visions " +
      "(R26)"):
    val script = LaterGameLogScripts.laterGameSetupWorldDeck
    Vector("p1", "p2", "p3").map(PlayerId(_)).foreach { viewer =>
      val log = GoldenLog.render(LogScripts.format(script, Some(viewer)))
      assert(!log.contains("peeked at"), log)
      assert(log.contains("placed the Visions with"), log)
    }
    script.history.steps.last.after match
      case OathState.Ready(ready) =>
        val visions = ready.game.current.commonCards.worldDeck.collect {
          case id: VisionId => id }
        assert(visions.nonEmpty)
        val known = ready.knowledge.advisers.getOrElse(PlayerId("p2"),
          Vector.empty)
        assert(visions.forall(known.contains), known)
      case other => fail(s"expected a ready game, got $other")
