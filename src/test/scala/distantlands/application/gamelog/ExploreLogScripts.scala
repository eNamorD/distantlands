package distantlands.application.gamelog

import distantlands.application._
import distantlands.gameplay.actions.explore.ExploreProcedure
import distantlands.gameplay.cards.{BuriedGiantSite, DeepWoodsSite}
import distantlands.gameplay.powers.setup.GreatMarketCard
import distantlands.gameplay.setup.legacies.WorldCrafterCard
import distantlands.model._
import distantlands.model.DecisionAnswer.ChooseOneAnswer
import distantlands.testkit.{Dice, MenuPlay, Situation, Table}
import distantlands.testkit.Table.p1
import LogScriptSupport.atTable

/** [[LogScripts]]' scripts for Explore, the action Wide Horizons gives
  * (Foundations design, slice 3). */
object ExploreLogScripts:
  import DefenseDieFace.{Blank, TwoShields}

  /** p1's Act under Wide Horizons on a map cut to Ancient City, Broken
    * Peaks and Buried Giant, where p1's pawn stands. The Atlas holds Deep
    * Woods at its Recent end, then Desolate Shore, Dunes, Fair Isle, and
    * Golden Valley at its Forgotten end. */
  private[gamelog] def p1AtHintermostOfThree: Table = Table.start
    .foundation(FoundationNumber.I, FoundationFace.Altered)
    .mapCutIntoAtlas(3).pawn(p1, BuriedGiantSite)

  /** An Explore from `table` on stream `name`: a roll of blanks, a second
    * Supply for a roll of four shields, then a site from `end`. */
  private[gamelog] def exploring(name: String, table: Table, end: AtlasEnd)(
      using munit.Location): (GameApplicationService, Situation) =
    val (service, act) = atTable(name, table.banditsRefilled,
      new Dice.ScriptedDefense(Vector(Vector(Blank, Blank),
        Vector(TwoShields, TwoShields))))
    (service, act.withAnswers {
      case park if park.decisionId == ExploreProcedure.choiceDecisionId =>
        ChooseOneAnswer(ExploreProcedure.continueOption)
      case park if park.decisionId == ExploreProcedure.endDecisionId =>
        ChooseOneAnswer(ExploreProcedure.endOption(end))
    }.after(MenuPlay.start(p1, ActionRef.Explore)))

  /** p1 explores the Forgotten end and finds Golden Valley. */
  def explore(using munit.Location): Script =
    Script("explore", exploring("explore", p1AtHintermostOfThree,
      AtlasEnd.Forgotten)._1, p1)

  /** p1's dice show blanks, and p1 stops after the first roll. */
  def exploreStopped(using munit.Location): Script =
    val (service, act) = atTable("explore-stopped",
      p1AtHintermostOfThree.banditsRefilled, Dice.blanks)
    act.withAnswers {
      case park if park.decisionId == ExploreProcedure.choiceDecisionId =>
        ChooseOneAnswer(ExploreProcedure.stopOption)
    }.after(MenuPlay.start(p1, ActionRef.Explore))
    Script("explore-stopped", service, p1)

  /** p1 explores the Recent end and finds Deep Woods, which stores the
    * Great Market: placed ruined, Bandit Market resolves WHEN EXPLORED. */
  def exploreBanditMarket(using munit.Location): Script =
    Script("explore-bandit-market", exploring("explore-bandit-market",
      p1AtHintermostOfThree.storedInAtlas(DeepWoodsSite, GreatMarketCard),
      AtlasEnd.Recent)._1, p1)

  /** p1 explores while p2 holds World Crafter: p2 chooses the Forgotten
    * end on p1's turn and p1 finds Golden Valley (Legacy Setup powers
    * ruling 11). */
  def exploreWorldCrafter(using munit.Location): Script =
    Script("explore-world-crafter", exploring("explore-world-crafter",
      p1AtHintermostOfThree.legacy(Table.p2, WorldCrafterCard),
      AtlasEnd.Forgotten)._1, p1)

  val named: Vector[(String, () => Script)] = Vector(
    "explore" -> (() => explore),
    "explore-world-crafter" -> (() => exploreWorldCrafter),
    "explore-stopped" -> (() => exploreStopped),
    "explore-bandit-market" -> (() => exploreBanditMarket))
