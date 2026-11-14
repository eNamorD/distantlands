package distantlands.gameplay.chronicle.legacies

import distantlands.gameplay.{OathRules, WalkerDiceFixture}
import distantlands.gameplay.actions.ProcedureNotes
import distantlands.gameplay.cards.{AncientCitySite, BuriedGiantSite}
import distantlands.gameplay.chronicle.{ChronicleWalk, OfferCitizenship}
import distantlands.gameplay.powers.{NoteText, PowerImplementationStatus,
  WalkerPowerCatalog}
import distantlands.gameplay.setup.FirstGameSetupFixture.catalog
import distantlands.model._
import distantlands.testkit.Table
import distantlands.testkit.Table.{p1, p2, p3}

/** Iron Hand (L01), its Chronicle half: "If you are Imperial, discard Iron
  * Hand. ... Score your legacies in addition to the winner, and perform
  * Shape Empire and Build Edifice (Throne). Your sites are stored in place
  * of the Empire (Stars)." (rulings 18-21). */
class IronHandSuite extends munit.FunSuite:
  private val rules = new OathRules(catalog,
    walkerPowerCatalog = WalkerPowerCatalog.default(catalog),
    walkerDice = WalkerDiceFixture.reversingShuffle)

  private def through(table: Table, procedure: TriggeredProcedureRef,
      answers: PartialFunction[Decide, DecisionAnswer] = PartialFunction.empty,
      after: TriggeredProcedureRef = TriggeredProcedureRef.ChronicleBegin)(
      using munit.Location): ChronicleWalk.Run =
    ChronicleWalk.through(rules, table, procedure, answers, after)

  private def holds(ready: ReadyGame, player: PlayerId): Boolean =
    ready.game.campaign.lineages(ready.game.current.player(player).get.lineage)
      .legacies.exists(_.id == IronHandCard.id)

  private def lines(run: ChronicleWalk.Run, procedure: TriggeredProcedureRef)
      : Vector[String] =
    val keys = IronHand.noteKeys(procedure)
    NoteText.said(ProcedureNotes.id(procedure), keys, run.events)
      .filter(said => keys.exists(_.name == said.key)).map(_.text)

  private val discardLine = s"p3 discarded ${IronHandCard.id.value}"

  test("Iron Hand is implemented"):
    assert(PowerImplementationStatus.implemented(IronHandCard.power.id))

  test("an Imperial holding Iron Hand active discards it at the start of " +
      "the Sun (ruling 18)"):
    val run = through(ChronicleWalk.chronicleWonByP1.imperial(p2, p3)
      .legacy(p3, IronHandCard), TriggeredProcedureRef.ChronicleSun)
    assert(!holds(run.ready, p3))
    assertEquals(lines(run, TriggeredProcedureRef.ChronicleSun).take(1),
      Vector(discardLine))

  test("a dormant Iron Hand an Imperial holds stays until Flip Legacies " +
      "activates it, then the Crown checkpoint discards it (R29)"):
    // p1, the Chancellor, wins; the Empire rules Ancient City, so the
    // Imperials meet Iron Hand's goal, and p3 stays a Citizen at the Crown.
    val run = through(ChronicleWalk.chronicleWonByP1.imperial(p1, p3)
      .warbandsAt(AncientCitySite, p1, 1)
      .legacy(p3, IronHandCard, active = false),
      TriggeredProcedureRef.ChronicleSun)
    assert(!holds(run.ready, p3))
    val said = NoteText.said(ProcedureNotes.id(
      TriggeredProcedureRef.ChronicleSun),
      distantlands.gameplay.chronicle.ChronicleSunProcedure.noteKeys,
      run.events).map(_.text)
    assert(said.indexOf(s"p3 activated ${IronHandCard.id.value}") <
      said.indexOf(discardLine), said)

  test("an Exile winner holding Iron Hand discards it after Crown " +
      "Chancellor (ruling 18)"):
    // p1 rules Ancient City alone, so keeps Iron Hand active in step 3.
    val run = through(ChronicleWalk.chronicleWonByP1
      .warbandsAt(AncientCitySite, p1, 1).legacy(p1, IronHandCard),
      TriggeredProcedureRef.ChronicleSun)
    assert(!holds(run.ready, p1))
    assert(lines(run, TriggeredProcedureRef.ChronicleSun).contains(
      s"p1 discarded ${IronHandCard.id.value}"))

  test("an Exile who becomes a Citizen at Offer Citizenship discards Iron " +
      "Hand, and the Empire shapes itself again (ruling 18)"):
    val table = ChronicleWalk.throneHeldByChancellorP1
      .warbandsAt(AncientCitySite, p1, 1).warbandsAt(BuriedGiantSite, p3, 1)
      .legacy(p3, IronHandCard)
    val run = through(table, TriggeredProcedureRef.ChronicleThrone, {
      case decide if decide.decisionId == OfferCitizenship.offerDecisionId =>
        DecisionAnswer.ChooseManyAnswer(Vector(DecisionOptionRef.Player(p3)))
      case decide if decide.decisionId ==
          OfferCitizenship.answerDecisionId(p3) =>
        DecisionAnswer.ChooseOneAnswer(OfferCitizenship.accept)
    }, after = TriggeredProcedureRef.ChronicleSun)
    assert(!holds(run.ready, p3))
    assert(lines(run, TriggeredProcedureRef.ChronicleThrone)
      .contains(discardLine))

  test("a boxed lineage's Iron Hand does nothing (ruling 1)"):
    val boxed = LineageState(LineageId("lineage:boxed"), None, Role.Citizen,
      Vector(LegacyState(IronHandCard.id, active = true)), Vector.empty)
    val table = ChronicleWalk.chronicleWonByP1.update(_.updateCampaign(
      campaign => campaign.copy(baseline = campaign.baseline.copy(
        lineages = Vector(boxed)))))
    val run = through(table, TriggeredProcedureRef.ChronicleSun)
    assertEquals(lines(run, TriggeredProcedureRef.ChronicleSun), Vector.empty)
