package distantlands.gameplay.chronicle.legacies

import distantlands.gameplay.{BanditCrownCard, GrandMaskCard, OathRules,
  WalkerDiceFixture}
import distantlands.gameplay.actions.ProcedureNotes
import distantlands.gameplay.cards.AncientCitySite
import distantlands.gameplay.chronicle.{ChronicleWalk, ReliquaryAndRelics}
import distantlands.gameplay.powers.{NoteText, PowerImplementationStatus,
  WalkerPowerCatalog}
import distantlands.gameplay.powers.economy.CupOfPlentyCard
import distantlands.gameplay.setup.FirstGameSetupFixture.catalog
import distantlands.model._
import distantlands.testkit.{Outcomes, Table}
import distantlands.testkit.Table.{p1, p2}
import distantlands.testkit.Unwrap.accepted

/** The Ear (L19), its Chronicle half: "In Scatter Relics, you examine the
  * relics and choose which sites they go to." (ruling 14; R21, R22). p1,
  * the Chancellor, holds the Throne and the Empire rules Ancient City,
  * which has no relic slot; p2, an Exile, holds The Ear and three relics,
  * which Scatter Relics sets aside. */
class TheEarSuite extends munit.FunSuite:
  private val rules = new OathRules(catalog,
    walkerPowerCatalog = WalkerPowerCatalog.default(catalog),
    walkerDice = WalkerDiceFixture.reversingShuffle)

  private def earTable: Table = ChronicleWalk.throneHeldByChancellorP1
    .warbandsAt(AncientCitySite, p1, 1)
    .relic(p2, CupOfPlentyCard).relic(p2, GrandMaskCard)
    .relic(p2, BanditCrownCard).legacy(p2, TheEarCard)

  private def through(table: Table,
      answers: PartialFunction[Decide, DecisionAnswer])(
      using munit.Location): ChronicleWalk.Run =
    ChronicleWalk.through(rules, table, TriggeredProcedureRef.ChronicleThrone,
      answers, after = TriggeredProcedureRef.ChronicleSun)

  /** The Ear's questions answered with `relics`, in turn. */
  private def placing(relics: RelicId*)
      : PartialFunction[Decide, DecisionAnswer] =
    var left = relics.toVector
    {
      case decide if decide.decisionId == TheEar.decisionId(p2) =>
        val next = left.head
        left = left.drop(1)
        DecisionAnswer.ChooseOneAnswer(DecisionOptionRef.Relic(next))
    }

  private def asked(run: ChronicleWalk.Run): Vector[Decide] =
    run.asked.filter(_.decisionId == TheEar.decisionId(p2))

  private def offered(decide: Decide): Vector[DecisionOptionRef] =
    decide.query.asInstanceOf[DecisionQuery.ChooseOne].options.map(_.ref)

  private def relicsAt(ready: ReadyGame, site: SiteId): Vector[RelicId] =
    ready.game.current.map.sites(site).relics.map(_.id)

  private def slots: Vector[SiteId] =
    ReliquaryAndRelics.emptySlots(catalog, earTable.ready)

  test("The Ear is implemented"):
    assert(PowerImplementationStatus.implemented(TheEarCard.power.id))

  test("the holder chooses the relic for each empty slot in Hinterward " +
      "order while two or more remain, the last is placed without a " +
      "question, and nothing is left set aside (ruling 14; R21)"):
    val run = through(earTable, placing(GrandMaskCard.id, CupOfPlentyCard.id))
    assertEquals(asked(run).size, 2)
    assert(asked(run).forall(_.owner == p2))
    assertEquals(relicsAt(run.ready, slots(0)).take(1), Vector(GrandMaskCard.id))
    val placed = slots.take(3).distinct.flatMap(relicsAt(run.ready, _))
    assertEquals(placed.toSet, Set(GrandMaskCard.id, CupOfPlentyCard.id,
      BanditCrownCard.id))
    assert(run.ready.game.current.setAsideRelics.isEmpty)
    val lines = NoteText.said(ProcedureNotes.id(
      TriggeredProcedureRef.ChronicleThrone),
      LegacyChroniclePowers.throneNoteKeys, run.events).map(_.text)
    assert(lines.contains(s"p2 chose where the relics went with " +
      TheEarCard.id.value), lines)

  test("each question offers only the relics still set aside"):
    val run = through(earTable, placing(GrandMaskCard.id, CupOfPlentyCard.id))
    assertEquals(offered(asked(run)(0)).toSet, Set[DecisionOptionRef](
      DecisionOptionRef.Relic(CupOfPlentyCard.id),
      DecisionOptionRef.Relic(GrandMaskCard.id),
      DecisionOptionRef.Relic(BanditCrownCard.id)))
    assertEquals(offered(asked(run)(1)).toSet, Set[DecisionOptionRef](
      DecisionOptionRef.Relic(CupOfPlentyCard.id),
      DecisionOptionRef.Relic(BanditCrownCard.id)))

  test("with fewer empty slots than relics the rest return to the relic " +
      "deck as before"):
    // Fill every empty slot but one with relics from the relic deck.
    val deck = earTable.ready.game.current.commonCards.relicDeck
    val filled = slots.drop(1).zip(deck).foldLeft(earTable)((table, pair) =>
      table.relicAt(pair._2, pair._1))
    val run = through(filled, PartialFunction.empty)
    // One slot, three relics: one question, then the deck takes the rest.
    assertEquals(asked(run).size, 1)
    assert(run.ready.game.current.setAsideRelics.isEmpty)

  test("without The Ear in effect the deal is the shuffled one, as before"):
    val run = through(ChronicleWalk.throneHeldByChancellorP1
      .warbandsAt(AncientCitySite, p1, 1)
      .relic(p2, CupOfPlentyCard).relic(p2, GrandMaskCard)
      .legacy(p2, TheEarCard, active = false), PartialFunction.empty)
    assertEquals(asked(run), Vector.empty)
    assert(run.ready.game.current.setAsideRelics.isEmpty)

  test("a walk parked mid-scatter, after a first pick, resumes to the same " +
      "question (R21)"):
    val started = accepted(rules.next(OathTransition(earTable.state,
      Vector.empty), TriggeredProcedureRef.ChronicleSun))
    val first = ChronicleWalk.walkedTo(rules, started, TheEar.decisionId(p2))
    val next = accepted(rules.resolveWalker(first.state, p2,
      TheEar.decisionId(p2), DecisionAnswer.ChooseOneAnswer(
        DecisionOptionRef.Relic(GrandMaskCard.id))))
    val parked = OathTransition(next.state, first.events ++ next.events)
    val decide = ChronicleWalk.parked(parked.state).get
    assertEquals(decide.decisionId, TheEar.decisionId(p2))
    // The second pass offers a different set: Grand Mask is placed.
    assert(!offered(decide).contains(DecisionOptionRef.Relic(GrandMaskCard.id)))
    val replayed = accepted(parked.events.foldLeft[
      Either[OathViolation, OathState]](Right(earTable.state))((state, event) =>
        state.flatMap(rules.evolve(_, event))))
    assertEquals(ChronicleWalk.parked(replayed), Some(decide))
    // The second pick is a different relic, and lands in the next slot.
    val done = ChronicleWalk.answeringEverything(rules, OathTransition(
      accepted(rules.resolveWalker(parked.state, p2, TheEar.decisionId(p2),
        DecisionAnswer.ChooseOneAnswer(
          DecisionOptionRef.Relic(BanditCrownCard.id)))).state, Vector.empty))
    val ready = Outcomes.ready(done.state)
    assertEquals(slots.take(2).flatMap(relicsAt(ready, _)).take(2),
      Vector(GrandMaskCard.id, BanditCrownCard.id))
