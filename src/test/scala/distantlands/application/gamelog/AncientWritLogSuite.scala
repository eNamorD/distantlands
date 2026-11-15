package distantlands.application.gamelog

import distantlands.gameplay.powers.action.{AncientWrit, AncientWritCard}
import distantlands.gameplay.setup.legacies.{PopulistCard, SeerCard,
  WorldCrafterCard}
import distantlands.model._
import distantlands.model.DecisionAnswer.ChooseOneAnswer
import distantlands.testkit.{MenuPlay, Table}
import distantlands.testkit.Table.{p1, p2}
import LogScripts.{atTable, format, texts}

/** How Ancient Writ reads in the Game Log: one draw line, which counts the
  * legacies and names none, and one keep line (catalog batch 13, "Lines").
  * The draw's `Draw` posts no generic line of its own. */
class AncientWritLogSuite extends munit.FunSuite:
  private val drawn = Vector(SeerCard, PopulistCard, WorldCrafterCard)

  /** p1 uses Ancient Writ on a deck of three and keeps Populist. The log as
    * `viewer` sees it. */
  private def used(viewer: PlayerId): Vector[String] =
    val table = Table.start.turn(p1, Phase.Act).relic(p1, AncientWritCard)
      .legacyDeck(drawn*)
    val (service, act) = atTable("ancient-writ", table)
    act.withAnswers {
      case park if park.decisionId == AncientWrit.decisionId =>
        ChooseOneAnswer(DecisionOptionRef.Legacy(PopulistCard.id))
    }.after(MenuPlay.start(p1, ActionRef.UsePower(AncientWrit.id),
      Vector(DecisionOptionRef.Relic(AncientWritCard.id))))
    texts(format(Script("ancient-writ", service, p1), Some(viewer)))

  test("each viewer reads one draw line and no legacy's name"):
    Vector(p1, p2).foreach { viewer =>
      val log = used(viewer)
      assertEquals(log.count(_.contains("drew")), 1, log.mkString("\n"))
      assert(log.exists(_.contains("drew 3 legacies")), log.mkString("\n"))
      assert(log.exists(_.contains("kept a legacy")), log.mkString("\n"))
      drawn.foreach(legacy => assert(!log.exists(_.contains(legacy.name)),
        s"$viewer ${legacy.name}\n${log.mkString("\n")}"))
    }
