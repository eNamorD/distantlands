package distantlands.gameplay.chronicle.legacies

import distantlands.gameplay.{OathRules, SacredGroundCard, WalkerDiceFixture}
import distantlands.gameplay.actions.ProcedureNotes
import distantlands.gameplay.cards.{AncientCitySite, BrokenPeaksSite}
import distantlands.gameplay.chronicle.ChronicleWalk
import distantlands.gameplay.powers.action.FaeMerchantCard
import distantlands.gameplay.powers.setup.GreatMarketCard
import distantlands.gameplay.powers.{NoteText, PowerImplementationStatus,
  WalkerPowerCatalog}
import distantlands.gameplay.powers.whenplayed.ThreateningRoarCard
import distantlands.gameplay.setup.FirstGameSetupFixture.catalog
import distantlands.model._
import distantlands.testkit.Table
import distantlands.testkit.Table.{p1, p2}
import distantlands.testkit.Unwrap.accepted

/** Reformer (L35): "If you won, you may move and swap denizens at sites you
  * rule, ignoring the locked restriction." A step at the start of the
  * Throne (ruling 12). p1 won and is the Chancellor; the Empire rules
  * Ancient City (room for three) and Broken Peaks (room for one). */
class ReformerSuite extends munit.FunSuite:
  private val rules = new OathRules(catalog,
    walkerPowerCatalog = WalkerPowerCatalog.default(catalog),
    walkerDice = WalkerDiceFixture.reversingShuffle)

  private val ancientCity = AncientCitySite.id
  private val brokenPeaks = BrokenPeaksSite.id

  private def empire: Table = ChronicleWalk.throneHeldByChancellorP1
    .warbandsAt(AncientCitySite, p1, 1).warbandsAt(BrokenPeaksSite, p1, 1)
    .denizen(ThreateningRoarCard, at = AncientCitySite)

  /** The Throne's start, every pass answered in turn from `passes`, a
    * pick and, unless it is Done, a destination; Done once they run out. */
  private def reformed(table: Table,
      passes: Vector[(DecisionOptionRef, DecisionOptionRef)])(
      using munit.Location): ChronicleWalk.Run =
    var picks = passes.map(_._1)
    var destinations = passes.map(_._2)
    ChronicleWalk.through(rules, table, TriggeredProcedureRef.ChronicleThrone, {
      case decide if decide.decisionId == Reformer.pickId(p1) =>
        val next = picks.headOption.getOrElse(Reformer.done)
        picks = picks.drop(1)
        DecisionAnswer.ChooseOneAnswer(next)
      case decide if decide.decisionId == Reformer.destinationId(p1) =>
        val next = destinations.head
        destinations = destinations.drop(1)
        DecisionAnswer.ChooseOneAnswer(next)
    }, after = TriggeredProcedureRef.ChronicleSun)

  private def at(ready: ReadyGame, site: SiteId): Vector[CardId] =
    ready.game.current.map.sites(site).denizens.map(_.id)

  private def lines(run: ChronicleWalk.Run): Vector[String] =
    val keys = LegacyChroniclePowers.throneNoteKeys
    NoteText.said(ProcedureNotes.id(TriggeredProcedureRef.ChronicleThrone),
      keys, run.events).filter(said => keys.exists(_.name == said.key))
      .map(_.text)

  private def denizen(card: distantlands.catalog.Denizen) =
    DecisionOptionRef.Denizen(card.id)

  test("Reformer is implemented"):
    assert(PowerImplementationStatus.implemented(ReformerCard.power.id))

  test("the winner moves a locked card to another site they rule with " +
      "room (ruling 12; Chronicle ruling 16)"):
    // An intact Sacred Ground shows the lock; the only locked cards a site
    // holds are intact edifices.
    val table = empire.edifice(SacredGroundCard, EdificeSide.Intact,
      at = AncientCitySite).legacy(p1, ReformerCard)
    val run = reformed(table, Vector(
      DecisionOptionRef.Edifice(SacredGroundCard.id) ->
        DecisionOptionRef.Site(brokenPeaks)))
    assertEquals(at(run.ready, brokenPeaks), Vector(SacredGroundCard.id))
    assertEquals(lines(run), Vector(s"p1 moved ${SacredGroundCard.id.value} " +
      s"to ${brokenPeaks.value} with ${ReformerCard.id.value}"))

  test("the winner swaps two denizens between sites they rule"):
    val table = empire.denizen(FaeMerchantCard, at = BrokenPeaksSite)
      .legacy(p1, ReformerCard)
    val run = reformed(table, Vector(denizen(ThreateningRoarCard) ->
      denizen(FaeMerchantCard)))
    assertEquals(at(run.ready, brokenPeaks), Vector(ThreateningRoarCard.id))
    assert(at(run.ready, ancientCity).contains(FaeMerchantCard.id))
    assertEquals(lines(run), Vector(s"p1 swapped " +
      s"${ThreateningRoarCard.id.value} and ${FaeMerchantCard.id.value} " +
      s"with ${ReformerCard.id.value}"))

  test("a full site is no destination for a move, only for a swap"):
    val table = empire.denizen(FaeMerchantCard, at = BrokenPeaksSite)
      .legacy(p1, ReformerCard)
    val run = reformed(table, Vector(denizen(ThreateningRoarCard) ->
      denizen(FaeMerchantCard)))
    val offered = run.asked.find(_.decisionId == Reformer.destinationId(p1))
      .get.query.asInstanceOf[DecisionQuery.ChooseOne].options.map(_.ref)
    assertEquals(offered, Vector(denizen(FaeMerchantCard)))

  test("the holder repeats until Done, each pass read when reached"):
    val table = empire.legacy(p1, ReformerCard)
    val run = reformed(table, Vector(
      denizen(ThreateningRoarCard) -> DecisionOptionRef.Site(brokenPeaks),
      denizen(ThreateningRoarCard) -> DecisionOptionRef.Site(ancientCity)))
    assertEquals(at(run.ready, ancientCity), Vector(ThreateningRoarCard.id))
    assertEquals(run.asked.count(_.decisionId == Reformer.pickId(p1)), 3)
    assertEquals(lines(run).size, 2)

  test("a later pass picks a different card than an earlier one"):
    // Pass 1 moves Threatening Roar to Broken Peaks; pass 2 swaps Fae
    // Merchant, at Ancient City, with it.
    val table = empire.denizen(FaeMerchantCard, at = AncientCitySite)
      .legacy(p1, ReformerCard)
    val run = reformed(table, Vector(
      denizen(ThreateningRoarCard) -> DecisionOptionRef.Site(brokenPeaks),
      denizen(FaeMerchantCard) -> denizen(ThreateningRoarCard)))
    assertEquals(at(run.ready, brokenPeaks), Vector(FaeMerchantCard.id))
    assertEquals(at(run.ready, ancientCity), Vector(ThreateningRoarCard.id))

  test("Done first changes nothing and writes no line"):
    val run = reformed(empire.legacy(p1, ReformerCard), Vector.empty)
    assertEquals(at(run.ready, ancientCity), Vector(ThreateningRoarCard.id))
    assertEquals(lines(run), Vector.empty)
    assertEquals(run.asked.head.decisionId, Reformer.pickId(p1))

  test("an intact edifice moves too (R10)"):
    val table = empire.edifice(GreatMarketCard, EdificeSide.Intact,
      at = AncientCitySite).legacy(p1, ReformerCard)
    val run = reformed(table, Vector(
      DecisionOptionRef.Edifice(GreatMarketCard.id) ->
        DecisionOptionRef.Site(brokenPeaks)))
    assertEquals(at(run.ready, brokenPeaks), Vector(GreatMarketCard.id))
    assertEquals(run.ready.game.current.map.sites(brokenPeaks).denizens,
      Vector(EdificeState(GreatMarketCard.id, EdificeSide.Intact,
        Tokens.empty)))

  test("a walk parked mid-pass after a completed move resumes to the same " +
      "question (R13)"):
    // Pass 1 moves Threatening Roar to Broken Peaks; pass 2 picks it again
    // and parks on its destination. Rebuilt from the journal, the walk
    // parks on the same question with the same options.
    val table = empire.legacy(p1, ReformerCard)
    val started = accepted(rules.next(OathTransition(table.state,
      Vector.empty), TriggeredProcedureRef.ChronicleSun))
    def answer(step: OathTransition, id: String, ref: DecisionOptionRef) =
      val next = accepted(rules.resolveWalker(step.state, p1, id,
        DecisionAnswer.ChooseOneAnswer(ref)))
      OathTransition(next.state, step.events ++ next.events)
    val roar = denizen(ThreateningRoarCard)
    val parked = Vector(
      (Reformer.pickId(p1), roar),
      (Reformer.destinationId(p1), DecisionOptionRef.Site(brokenPeaks)),
      (Reformer.pickId(p1), roar))
      .foldLeft(started)((step, pair) => answer(step, pair._1, pair._2))
    val decide = ChronicleWalk.parked(parked.state).get
    assertEquals(decide.decisionId, Reformer.destinationId(p1))
    val replayed = accepted(parked.events.foldLeft[
      Either[OathViolation, OathState]](Right(table.state))((state, event) =>
        state.flatMap(rules.evolve(_, event))))
    assertEquals(ChronicleWalk.parked(replayed), Some(decide))
    assertEquals(decide.query.asInstanceOf[DecisionQuery.ChooseOne].options
      .map(_.ref), Vector(DecisionOptionRef.Site(ancientCity)))

  test("a holder who lost is not asked"):
    val run = reformed(empire.legacy(p2, ReformerCard), Vector.empty)
    assert(!run.asked.exists(_.decisionId.startsWith(
      "chronicle.throne.legacy.reformer.")))

  test("with nothing to move or swap the winner is not asked"):
    val table = ChronicleWalk.throneHeldByChancellorP1
      .warbandsAt(AncientCitySite, p1, 1)
      .denizen(ThreateningRoarCard, at = AncientCitySite)
      .legacy(p1, ReformerCard)
    assert(!reformed(table, Vector.empty).asked.exists(
      _.decisionId == Reformer.pickId(p1)))
