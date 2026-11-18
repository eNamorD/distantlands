package distantlands.frontend

import distantlands.frontend.Dom.{all, one}
import distantlands.frontend.TestSites.quietSite
import distantlands.model.PlayerColor
import distantlands.protocol.{DecisionAnswerWire, DecisionOptionWire,
  GameIntent => Intent}
import distantlands.testkit.Unwrap.as
import ParkedDecision.{Routed, Surface}
import org.scalajs.dom

/** A choose-many whose options open picked, as Shape Empire's sites do: the
  * draft starts with them, a click drops one, and the Action panel lists the
  * sites that are picked so far.
  */
class PreselectedChooseManySuite extends munit.FunSuite:
  private val context = BoardSelectionContext("game", "red", 1L)
  private val red = GamePlayer("red", "Red", "Exile", PlayerColor.Red)
  private def site(id: String) = quietSite(id, s"Site $id")
  private val decisionId = "chronicle.shape-empire.remove"
  private def option(id: String, preselected: Boolean = true) =
    DecisionOptionState("site", id, s"Site $id", preselected = preselected)
  private val sites = DecisionQueryState.ChooseMany(
    Vector(option("plains"), option("hills"), option("marsh")), 1, 3,
    Some("Choose sites in the next Empire."),
    confirm = Some(CountedConfirmState("Keep", "site", "sites")))
  private val parked = WalkerDecisionState("chronicle", decisionId,
    query = Some(sites), presentation = "board")
  private val table = TestProjections.actingProjection(players = Vector(red),
    world = Vector(GameRegion("cradle", Vector(site("plains"), site("hills"),
      site("marsh"))))).copy(walkerDecision = Some(parked))

  private def pane(draft: Option[BoardSelectionDraft]): dom.Element =
    val panel = dom.document.createElement("div")
    ParkedDecision.render(table, ServerUiSupport.viewerPresentation(table,
      "red"), Routed(Some(Surface.BoardDecision(parked, sites)), None),
      canControl = true, panel,
      SessionDrafts.empty.copy(context = Some(context), board = draft),
      new RecordingControls())
    panel

  test("a fresh position opens with every preselected option picked"):
    val draft = SessionDrafts.empty.reconcile(context, table).board
    assertEquals(draft.map(_.keys),
      Some(Vector("site:plains", "site:hills", "site:marsh")))
    assertEquals(draft.flatMap(_.command), Some(Intent.ResolveWalker(
      decisionId, DecisionAnswerWire.ChooseManyWire(
        Vector("plains", "hills", "marsh").map(DecisionOptionWire("site", _))))))

  test("a click drops a picked site, and the choice stays dropped"):
    val opened = SessionDrafts.empty.reconcile(context, table)
    val dropped = opened.staged(Draft.Board(opened.board.map(
      _.click(option("hills")))))
    assertEquals(dropped.reconcile(context, table).board.map(_.keys),
      Some(Vector("site:plains", "site:marsh")))

  test("the panel lists the picked sites under a counted summary"):
    val draft = Some(BoardSelectionDraft(context, decisionId, sites,
      Vector("site:plains", "site:marsh")))
    val panel = pane(draft)
    assertEquals(all(panel, ".board-decision-row").map(_.textContent),
      Vector("Site plains", "Site marsh"))
    assertEquals(one(panel, ".board-decision-confirm").textContent,
      "Keep 2 sites")

  test("with every site dropped Confirm is withheld, for the query needs one"):
    val draft = Some(BoardSelectionDraft(context, decisionId, sites,
      Vector.empty))
    assertEquals(draft.flatMap(_.command), None)
    assert(as[dom.html.Button](one(pane(draft), ".board-decision-confirm"))
      .disabled)
