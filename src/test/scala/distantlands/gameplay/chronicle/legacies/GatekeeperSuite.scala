package distantlands.gameplay.chronicle.legacies

import distantlands.catalog.holding.{IronHandCard, SeerCard, WealthyCard}
import distantlands.gameplay.OathRules
import distantlands.gameplay.actions.ProcedureNotes
import distantlands.gameplay.cards.AncientCitySite
import distantlands.gameplay.chronicle.{ChronicleWalk, ResolveLegacies}
import distantlands.gameplay.powers.{NoteText, PowerImplementationStatus,
  WalkerPowerCatalog}
import distantlands.gameplay.powers.economy.VillageIdiotCard
import distantlands.gameplay.setup.FirstGameSetupFixture.catalog
import distantlands.model._
import distantlands.testkit.Table
import distantlands.testkit.Table.{p1, p2, p3}

/** Gatekeeper (L32): "Before players resolve legacies, you may take one
  * active legacy from an enemy if you meet its goal." A step between Sun
  * steps 1 and 2 (ruling 6). Every game here is p1's win. */
class GatekeeperSuite extends munit.FunSuite:
  private val rules = new OathRules(catalog,
    walkerPowerCatalog = WalkerPowerCatalog.default(catalog))
  private def won: Table = ChronicleWalk.chronicleWonByP1

  private def sun(table: Table,
      answers: PartialFunction[Decide, DecisionAnswer] = PartialFunction.empty)(
      using munit.Location): ChronicleWalk.Run =
    ChronicleWalk.through(rules, table, TriggeredProcedureRef.ChronicleSun,
      answers)

  private def taking(legacy: LegacyId): PartialFunction[Decide, DecisionAnswer] =
    case decide if decide.decisionId == Gatekeeper.decisionId(p2) =>
      DecisionAnswer.ChooseOneAnswer(DecisionOptionRef.Legacy(legacy))

  private def asked(run: ChronicleWalk.Run): Option[Decide] =
    run.asked.find(_.decisionId == Gatekeeper.decisionId(p2))

  private def offered(run: ChronicleWalk.Run): Vector[DecisionOptionRef] =
    asked(run).get.query.asInstanceOf[DecisionQuery.ChooseOne]
      .options.map(_.ref)

  private def legaciesOf(ready: ReadyGame, player: PlayerId): Vector[LegacyId] =
    ready.game.campaign.lineages(ready.game.current.player(player).get.lineage)
      .legacies.map(_.id)

  private def lines(run: ChronicleWalk.Run): Vector[String] =
    val keys = LegacyChroniclePowers.sunNoteKeys
    NoteText.said(ProcedureNotes.id(TriggeredProcedureRef.ChronicleSun), keys,
      run.events).filter(said => keys.exists(_.name == said.key)).map(_.text)

  /** p2 holds Gatekeeper and the Darkest Secret, so meets Seer's goal. */
  private def secretKeeper: Table =
    won.darkestSecret(Some(p2), 1).legacy(p2, GatekeeperCard)

  /** p2 is the Chancellor, ruling Ancient City and its Hearth denizen, so
    * meets Wealthy's goal, which p3, an Exile, holds. Taking it leaves p2
    * four active legacies, which an Imperial keeps through step 3. */
  private def empireTakesWealthy: Table =
    won.imperial(p2).warbandsAt(AncientCitySite, p2, 1)
      .denizen(VillageIdiotCard, AncientCitySite)
      .legacy(p2, GatekeeperCard).legacy(p2, SeerCard)
      .legacy(p2, IronHandCard).legacy(p3, WealthyCard)

  test("Gatekeeper is implemented"):
    assert(PowerImplementationStatus.implemented(GatekeeperCard.power.id))

  test("the holder is offered Decline, then exactly the enemies' active " +
      "legacies whose goal they meet (R6)"):
    val run = sun(secretKeeper.legacy(p3, SeerCard)
      .legacy(p3, IronHandCard).legacy(p1, RivalCard, active = false))
    assertEquals(offered(run), Vector(Gatekeeper.decline,
      DecisionOptionRef.Legacy(SeerCard.id)))
    assertEquals(asked(run).get.owner, p2)

  test("the taken legacy joins the holder active, with a line"):
    val run = sun(secretKeeper.legacy(p3, SeerCard), taking(SeerCard.id))
    assert(legaciesOf(run.ready, p2).contains(SeerCard.id))
    assert(!legaciesOf(run.ready, p3).contains(SeerCard.id))
    assertEquals(lines(run), Vector(s"p2 took ${SeerCard.id.value} from p3 " +
      s"with ${GatekeeperCard.id.value}"))

  test("declining takes nothing and writes no line (ruling 3)"):
    val run = sun(secretKeeper.legacy(p3, SeerCard))
    assert(asked(run).nonEmpty)
    assert(legaciesOf(run.ready, p3).contains(SeerCard.id))
    assertEquals(lines(run), Vector.empty)

  test("an enemy's dormant legacy is not takeable, even with its goal met"):
    assertEquals(asked(sun(secretKeeper.legacy(p3, SeerCard,
      active = false))), None)

  test("an Imperial ally's legacy is not an enemy's"):
    val run = sun(secretKeeper.imperial(p2, p3).legacy(p3, SeerCard))
    assertEquals(asked(run), None)

  test("nothing takeable asks nothing, and a dormant Gatekeeper asks " +
      "nothing (ruling 1)"):
    assertEquals(asked(sun(secretKeeper.legacy(p3, IronHandCard))), None)
    assertEquals(asked(sun(won.darkestSecret(Some(p2), 1)
      .legacy(p2, GatekeeperCard, active = false).legacy(p3, SeerCard))), None)

  test("a taken foundation-altering legacy alters its foundation in step 2, " +
      "and the cap then asks (ruling 6)"):
    val run = sun(empireTakesWealthy, taking(WealthyCard.id))
    assertEquals(run.ready.game.campaign.foundations(FoundationNumber.II),
      FoundationState(FoundationFace.Altered, Set(WealthyCard.id)))
    val cap = run.asked.find(_.decisionId == ResolveLegacies.capDecisionId(p2))
      .get.query.asInstanceOf[DecisionQuery.ChooseMany]
    assertEquals((cap.min, cap.max), (1, 1))
    assert(cap.options.map(_.ref).contains(
      DecisionOptionRef.Legacy(WealthyCard.id)))

  test("a walk resumed at Gatekeeper's question applies the answer, and a " +
      "later question resumes past it"):
    // The walk parks at Gatekeeper, then, after the take changed the
    // lineages, at the cap; each answer is given to a walk rebuilt from the
    // journal.
    val run = sun(empireTakesWealthy, taking(WealthyCard.id).orElse {
      case decide if decide.decisionId == ResolveLegacies.capDecisionId(p2) =>
        DecisionAnswer.ChooseManyAnswer(Vector(
          DecisionOptionRef.Legacy(IronHandCard.id)))
    })
    assertEquals(legaciesOf(run.ready, p2).toSet,
      Set(GatekeeperCard.id, SeerCard.id, WealthyCard.id))
    assertEquals(run.asked.map(_.decisionId).filter(_.startsWith(
      "chronicle.sun.legacy")), Vector(Gatekeeper.decisionId(p2),
      ResolveLegacies.capDecisionId(p2)))
