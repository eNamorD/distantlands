package distantlands.gameplay.chronicle

import distantlands.catalog.holding.{ArbiterCard, ChroniclerCard,
  IronHandCard, MatchmakerCard, PeacemakerCard, RivalCard, SeerCard,
  SteadfastCard, WealthyCard}
import distantlands.gameplay.cards.{AncientCitySite, BrokenPeaksSite,
  BuriedGiantSite}
import distantlands.gameplay.OathRules
import distantlands.gameplay.actions.ProcedureNotes
import distantlands.gameplay.powers.{NoteText, WalkerPowerCatalog}
import distantlands.gameplay.setup.FirstGameSetupFixture.catalog
import distantlands.gameplay.walker.WalkerCompleted
import distantlands.model._
import distantlands.testkit.{Outcomes, Table}
import distantlands.testkit.Table.{p2, p3}
import distantlands.testkit.Unwrap.accepted

/** Resolve Legacies, the Sun's steps 2-4 (CR "Task 1: The Sun"; Legacies
  * design, slice 3). Every game here is p1's win, in the Chronicle Phase
  * with Start Chronicle answered. */
class ResolveLegaciesSuite extends munit.FunSuite:
  private val rules = new OathRules(catalog,
    walkerPowerCatalog = WalkerPowerCatalog.default(catalog))
  private val noteId = ProcedureNotes.id(TriggeredProcedureRef.ChronicleSun)
  private def won: Table = ChronicleWalk.chronicleWonByP1

  /** The Sun walked to its end on `table`, each park answered by `answers`
    * where it names one and plainly otherwise, and every decision it
    * parked on. */
  private final case class Sun(ready: ReadyGame, events: Vector[OathEvent],
      asked: Vector[Decide])

  private def sun(table: Table,
      answers: PartialFunction[Decide, DecisionAnswer] = PartialFunction.empty)(
      using munit.Location): Sun =
    val done = WalkerCompleted(TriggeredProcedureRef.ChronicleSun)
    var asked = Vector.empty[Decide]
    val started = accepted(rules.next(OathTransition(table.state, Vector.empty),
      TriggeredProcedureRef.ChronicleBegin))
    val walked = Iterator.iterate(started)(step =>
      val decide = ChronicleWalk.parked(step.state).get
      asked :+= decide
      val next = accepted(rules.resolveWalker(step.state, decide.owner,
        decide.decisionId, answers.applyOrElse(decide,
          ChronicleWalk.plainestAnswer)))
      OathTransition(next.state, step.events ++ next.events))
      .find(_.events.contains(done)).get
    val events = walked.events.take(walked.events.indexOf(done) + 1)
    Sun(Outcomes.ready(accepted(events.foldLeft[
      Either[OathViolation, OathState]](Right(table.state))((state, event) =>
        state.flatMap(rules.evolve(_, event))))), events, asked)

  private def legaciesOf(ready: ReadyGame, player: PlayerId)
      : Vector[LegacyState] =
    ready.game.campaign.lineages(ready.game.current.player(player).get.lineage)
      .legacies

  /** The lines Resolve Legacies wrote, in order. */
  private def lines(run: Sun): Vector[String] =
    val keys = ResolveLegacies.noteKeys
    NoteText.said(noteId, keys, run.events)
      .filter(said => keys.exists(_.name == said.key)).map(_.text)

  private def inDeck(ready: ReadyGame, legacy: LegacyId): Boolean =
    ready.game.cardIndex.toOption.get.get(legacy).isEmpty

  test("a dormant legacy whose goal is met turns active, and one whose " +
      "goal is missed returns to the deck"):
    val run = sun(won.darkestSecret(Some(p2), 1)
      .legacy(p2, SeerCard, active = false)
      .legacy(p3, IronHandCard, active = false))
    assertEquals(legaciesOf(run.ready, p2),
      Vector(LegacyState(SeerCard.id, active = true)))
    assertEquals(legaciesOf(run.ready, p3), Vector.empty)
    assert(inDeck(run.ready, IronHandCard.id))
    assertEquals(lines(run), Vector(s"p2 activated ${SeerCard.id.value}",
      s"p3 discarded ${IronHandCard.id.value}"))

  test("a public legacy goes active to the one player who meets its goal, " +
      "and one nobody meets returns to the deck"):
    val run = sun(won.peoplesFavor(Some(p3), 1)
      .publicLegacies(MatchmakerCard, IronHandCard))
    assertEquals(legaciesOf(run.ready, p3),
      Vector(LegacyState(MatchmakerCard.id, active = true)))
    assertEquals(run.ready.game.current.commonCards.publicLegacies,
      Vector.empty)
    assert(inDeck(run.ready, IronHandCard.id))
    assertEquals(lines(run), Vector(s"p3 took ${MatchmakerCard.id.value}",
      s"${IronHandCard.id.value} returned to the legacy deck"))

  test("the outgoing Chancellor breaks a public tie between Imperials " +
      "alone, choosing among the tied players"):
    // p2 is Chancellor and p3 a Citizen; the Empire rules the most sites.
    val table = won.imperial(p2, p3).warbandsAt(AncientCitySite, p2, 1)
      .publicLegacies(IronHandCard)
    val decisionId = ResolveLegacies.publicDecisionId(IronHandCard.id)
    val run = sun(table, {
      case decide if decide.decisionId == decisionId =>
        DecisionAnswer.ChooseOneAnswer(DecisionOptionRef.Player(p3)) })
    val asked = run.asked.find(_.decisionId == decisionId).get
    assertEquals(asked.owner, p2)
    assertEquals(asked.query.asInstanceOf[DecisionQuery.ChooseOne].options
      .map(_.ref), Vector(p2, p3).map(DecisionOptionRef.Player(_)))
    assertEquals(legaciesOf(run.ready, p3),
      Vector(LegacyState(IronHandCard.id, active = true)))

  test("a public legacy tied with an Exile goes to nobody"):
    val run = sun(won.warbandsAt(AncientCitySite, p2, 1)
      .warbandsAt(BrokenPeaksSite, p3, 1).publicLegacies(IronHandCard))
    assert(inDeck(run.ready, IronHandCard.id))
    assert(!run.asked.exists(_.decisionId ==
      ResolveLegacies.publicDecisionId(IronHandCard.id)))

  test("an active altering legacy alters its foundation once, seated or " +
      "in the box"):
    val run = sun(won.legacy(p2, WealthyCard))
    assertEquals(run.ready.game.campaign.foundations(FoundationNumber.II),
      FoundationState(FoundationFace.Altered, Set(WealthyCard.id)))
    def foundationLines(run: Sun) =
      lines(run).filter(_.contains(" set Foundation "))
    assertEquals(foundationLines(run), Vector(
      s"${WealthyCard.id.value} set Foundation II to Movers and Shakers"))
    // A second Chronicle with the face already listing it writes nothing.
    val again = sun(won.legacy(p2, WealthyCard)
      .update(_.updateCampaign(_.copy(foundations = run.ready.game.campaign
        .foundations))))
    assertEquals(foundationLines(again), Vector.empty)

  test("a boxed lineage's active legacy alters too, and the lineage is " +
      "unchanged (ruling 8)"):
    val boxed = LineageState(LineageId("lineage:boxed"), None, Role.Exile,
      Vector(LegacyState(WealthyCard.id, active = true),
        LegacyState(RivalCard.id, active = false)), Vector.empty)
    val table = won.update(_.updateCampaign(campaign => campaign.copy(
      baseline = campaign.baseline.copy(lineages = Vector(boxed)))))
    val run = sun(table)
    assertEquals(run.ready.game.campaign.foundations(FoundationNumber.II),
      FoundationState(FoundationFace.Altered, Set(WealthyCard.id)))
    assertEquals(ChronicleRecord.campaignFields(run.ready).lineages
      .find(_.id == boxed.id), Some(boxed))

  test("dormant legacies are resolved in turn order, from the last round's " +
      "starter (R21, R25)"):
    val run = sun(won.roundStarter(p3).warbandsAt(BuriedGiantSite, p3, 1)
      .legacy(p2, SeerCard, active = false)
      .legacy(p3, IronHandCard, active = false))
    assertEquals(lines(run), Vector(s"p3 activated ${IronHandCard.id.value}",
      s"p2 discarded ${SeerCard.id.value}"))

  test("an Exile's active legacy whose goal they miss goes dormant, one " +
      "they meet stays active, and an Imperial's stays active"):
    val run = sun(won.imperial(p3).darkestSecret(Some(p2), 1)
      .legacy(p2, SeerCard).legacy(p2, IronHandCard)
      .legacy(p3, RivalCard))
    assertEquals(legaciesOf(run.ready, p2), Vector(
      LegacyState(SeerCard.id, active = true),
      LegacyState(IronHandCard.id, active = false)))
    assertEquals(legaciesOf(run.ready, p3),
      Vector(LegacyState(RivalCard.id, active = true)))
    assert(lines(run).contains(s"p2's ${IronHandCard.id.value} went dormant"))

  test("an altering legacy gone dormant leaves its foundation altered"):
    val altered = FoundationState(FoundationFace.Altered, Set(WealthyCard.id))
    val run = sun(won.legacy(p2, WealthyCard).update(_.updateCampaign(
      campaign => campaign.copy(foundations = campaign.foundations.updated(
        FoundationNumber.II, altered)))))
    assertEquals(legaciesOf(run.ready, p2),
      Vector(LegacyState(WealthyCard.id, active = false)))
    assertEquals(run.ready.game.campaign.foundations(FoundationNumber.II),
      altered)

  test("after the Sun every dormant legacy on a seated lineage was active " +
      "when the Chronicle began (ruling 2)"):
    val table = won.darkestSecret(Some(p3), 1)
      .legacy(p2, SeerCard, active = false).legacy(p2, IronHandCard)
      .legacy(p3, RivalCard, active = false)
      .legacy(p3, MatchmakerCard)
    val before = table.ready.game.campaign.lineages.values
      .flatMap(_.legacies).filter(_.active).map(_.id).toSet
    val run = sun(table)
    val dormant = run.ready.game.current.players.flatMap(player =>
      legaciesOf(run.ready, player.player)).filterNot(_.active).map(_.id)
    assert(dormant.nonEmpty)
    assert(dormant.forall(before), dormant)

  /** `table` with `cards` held active by `player`. */
  private def holding(table: Table, player: PlayerId,
      cards: distantlands.catalog.Legacy*): Table =
    cards.foldLeft(table)(_.legacy(player, _))

  private def capOf(run: Sun, player: PlayerId): Option[Decide] =
    run.asked.find(_.decisionId == ResolveLegacies.capDecisionId(player))

  test("a player with five active legacies discards exactly two of them, " +
      "and one with three is asked nothing"):
    // Imperials' unmet legacies stay active through step 3.
    val table = holding(holding(won.imperial(p2, p3), p2, IronHandCard,
      SeerCard, RivalCard, MatchmakerCard, ArbiterCard), p3, SteadfastCard,
      ChroniclerCard, PeacemakerCard)
    val run = sun(table, {
      case decide if decide.decisionId ==
          ResolveLegacies.capDecisionId(p2) =>
        DecisionAnswer.ChooseManyAnswer(Vector(SeerCard.id, ArbiterCard.id)
          .map(DecisionOptionRef.Legacy(_))) })
    val asked = capOf(run, p2).get.query.asInstanceOf[DecisionQuery.ChooseMany]
    assertEquals((asked.min, asked.max), (2, 2))
    assertEquals(asked.options.map(_.ref), Vector(IronHandCard, SeerCard,
      RivalCard, MatchmakerCard, ArbiterCard).map(card =>
      DecisionOptionRef.Legacy(card.id)))
    assertEquals(legaciesOf(run.ready, p2).map(_.id),
      Vector(IronHandCard.id, RivalCard.id, MatchmakerCard.id))
    assert(inDeck(run.ready, SeerCard.id) && inDeck(run.ready, ArbiterCard.id))
    assertEquals(capOf(run, p3), None)
    assert(lines(run).contains(s"p2 discarded ${SeerCard.id.value}"))

  test("players over the cap are asked in turn order (R21)"):
    val table = holding(holding(won.imperial(p2, p3).roundStarter(p3), p2,
      IronHandCard, SeerCard, RivalCard, MatchmakerCard), p3, SteadfastCard,
      ChroniclerCard, PeacemakerCard, ArbiterCard)
    val run = sun(table)
    assertEquals(run.asked.map(_.decisionId).filter(_.startsWith(
      "chronicle.sun.legacy-cap.")), Vector(p3, p2).map(
      ResolveLegacies.capDecisionId))
