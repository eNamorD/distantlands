package distantlands.gameplay.chronicle.legacies

import distantlands.gameplay.{OathRules, WalkerDiceFixture}
import distantlands.gameplay.actions.ProcedureNotes
import distantlands.gameplay.cards.{AncientCitySite, BrokenPeaksSite,
  BuriedGiantSite, DeepWoodsSite}
import distantlands.gameplay.chronicle.{BuildEdifice, ChronicleWalk,
  OfferCitizenship, ShapeEmpire}
import distantlands.gameplay.powers.{NoteText, PowerImplementationStatus,
  WalkerPowerCatalog}
import distantlands.gameplay.powers.action.FaeMerchantCard
import distantlands.gameplay.powers.economy.VillageIdiotCard
import distantlands.gameplay.powers.whenplayed.ThreateningRoarCard
import distantlands.gameplay.setup.FirstGameSetupFixture.catalog
import distantlands.catalog.holding.{MatchmakerCard, SeerCard,
  TheNeedleCard}
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

  /** p1, the Chancellor, holds the Throne, and the Empire rules Ancient
    * City and Deep Woods, which holds Fae Merchant; p3, an Exile, holds
    * Iron Hand and rules Broken Peaks and Buried Giant, which holds
    * Threatening Roar. */
  private def ironThrone: Table = ChronicleWalk.throneHeldByChancellorP1
    .warbandsAt(AncientCitySite, p1, 1).warbandsAt(DeepWoodsSite, p1, 1)
    .warbandsAt(BrokenPeaksSite, p3, 1).warbandsAt(BuriedGiantSite, p3, 1)
    .denizen(FaeMerchantCard, at = DeepWoodsSite)
    .denizen(ThreateningRoarCard, at = BuriedGiantSite)
    .legacy(p3, IronHandCard)

  private def onMap(ready: ReadyGame, card: CardId): Boolean =
    ready.game.current.map.sites.values.exists(_.denizens.exists(_.id == card))

  test("the holder performs Shape Empire over the sites they rule, and the " +
      "Empire's is skipped (ruling 19; R31)"):
    val run = through(ironThrone, TriggeredProcedureRef.ChronicleThrone,
      after = TriggeredProcedureRef.ChronicleSun)
    val removal = run.asked.filter(_.decisionId ==
      ShapeEmpire.removeDecisionId)
    assertEquals(removal.map(_.owner), Vector(p3))
    // The holder rules Buried Giant, so Threatening Roar stays; Deep Woods
    // is the Empire's, so its Fae Merchant is discarded.
    assert(onMap(run.ready, ThreateningRoarCard.id))
    assert(!onMap(run.ready, FaeMerchantCard.id))

  test("the holder performs Build Edifice over the sites they rule " +
      "(ruling 19)"):
    val suit = catalog.denizen(ThreateningRoarCard.id).get.suit
    val run = through(ironThrone, TriggeredProcedureRef.ChronicleThrone,
      after = TriggeredProcedureRef.ChronicleSun)
    val build = run.asked.find(_.decisionId == BuildEdifice.decisionId(suit))
      .get
    assertEquals(build.owner, p3)
    assertEquals(build.query.asInstanceOf[DecisionQuery.ChooseMany].options
      .map(_.ref), Vector(DecisionOptionRef.Denizen(ThreateningRoarCard.id)))

  test("an Imperial Great Architect still builds over the Empire's sites, " +
      "then the holder over theirs, under ids of their own (R32)"):
    // p2 is a Citizen holding Great Architect; Village Idiot, at Enduring
    // Ancient City, survives the holder's Shape Empire.
    val table = ironThrone.imperial(p1, p2)
      .denizen(VillageIdiotCard, at = AncientCitySite)
      .legacy(p2, GreatArchitectCard)
    val suit = catalog.denizen(ThreateningRoarCard.id).get.suit
    val other = Suit.all.find(_ != suit).get
    val run = through(table, TriggeredProcedureRef.ChronicleThrone, {
      case decide if decide.decisionId == BuildEdifice.decisionId(other) =>
        DecisionAnswer.ChooseManyAnswer(Vector(
          DecisionOptionRef.Denizen(VillageIdiotCard.id)))
    }, after = TriggeredProcedureRef.ChronicleSun)
    val builds = run.asked.filter(_.decisionId.startsWith(
      "chronicle.build-edifice."))
    assert(builds.exists(decide => decide.owner == p2 &&
      decide.decisionId == BuildEdifice.decisionId(other)))
    assert(builds.exists(decide => decide.owner == p3 &&
      decide.decisionId == BuildEdifice.ironHandDecisionId(suit)))
    assertEquals(builds.map(_.decisionId).distinct.size, builds.size)
    // Great Architect built the other suit's edifice: the holder is not
    // offered it.
    assert(!builds.exists(_.decisionId ==
      BuildEdifice.ironHandDecisionId(other)))

  test("a walk resumed at the holder's removal question finds the same " +
      "sites"):
    val started = ChronicleWalk.started(rules, ironThrone,
      TriggeredProcedureRef.ChronicleSun)
    val parked = ChronicleWalk.walkedTo(rules, started,
      ShapeEmpire.removeDecisionId)
    val decide = ChronicleWalk.parked(parked.state).get
    assertEquals(decide.owner, p3)
    val replayed = distantlands.testkit.Unwrap.accepted(parked.events
      .foldLeft[Either[OathViolation, OathState]](Right(ironThrone.state))(
        (state, event) => state.flatMap(rules.evolve(_, event))))
    assertEquals(ChronicleWalk.parked(replayed), Some(decide))

  test("the holder scores the legacy bonus at the sites they rule after " +
      "the winner, with no base and no gifts (ruling 20; R33)"):
    // p3 holds The Needle (Beast) beside Iron Hand (Order); Threatening
    // Roar, a Beast denizen, lies at Buried Giant.
    val run = through(ironThrone.legacy(p3, TheNeedleCard),
      TriggeredProcedureRef.ChronicleThrone,
      after = TriggeredProcedureRef.ChronicleSun)
    val lineage = run.ready.game.current.player(p3).get.lineage
    assertEquals(run.ready.game.campaign.era.score(lineage), 1)
    assert(run.ready.game.current.scoredLegacies.contains(TheNeedleCard.id))
    assert(lines(run, TriggeredProcedureRef.ChronicleThrone).contains(
      s"p3 scored 1 lineage point with ${IronHandCard.id.value} " +
        "(1 from Beast)"))

  test("the holder's scoring skips legacies Chronicler scored (ruling 8)"):
    val table = ironThrone.legacy(p3, TheNeedleCard).update(
      _.updateCurrent(_.copy(scoredLegacies = Set(TheNeedleCard.id))))
    val run = through(table, TriggeredProcedureRef.ChronicleThrone,
      after = TriggeredProcedureRef.ChronicleSun)
    val lineage = run.ready.game.current.player(p3).get.lineage
    assertEquals(run.ready.game.campaign.era.score(lineage), 0)
    assert(!lines(run, TriggeredProcedureRef.ChronicleThrone)
      .exists(_.startsWith("p3 scored")))

  test("the holder's sites are stored in place of the Empire's, Cradlemost " +
      "first, and the Empire's are archived; the record passes game start " +
      "(ruling 21; R35)"):
    // p1 wins and rules Ancient City; p3, an Exile, rules Broken Peaks and
    // Buried Giant, the most sites, so keeps Iron Hand through the Sun.
    val table = ChronicleWalk.chronicleWonByP1
      .warbandsAt(AncientCitySite, p1, 1)
      .warbandsAt(BrokenPeaksSite, p3, 1).warbandsAt(BuriedGiantSite, p3, 1)
      .legacy(p3, IronHandCard)
    val run = through(table, TriggeredProcedureRef.ChronicleFinish)
    val record = run.ready.game.campaign.evolved.get
    val stored = run.ready.game.current.map.hinterward.filter(site =>
      site == BrokenPeaksSite.id || site == BuriedGiantSite.id)
    assertEquals(record.world.map(_.site), stored)
    assert(record.atlasBox.exists(_.site == AncientCitySite.id))
    assertEquals(distantlands.gameplay.setup.ChronicleAccounting
      .knownAndUnique(catalog, record), Right(()))
    assertEquals(distantlands.gameplay.setup.ChronicleAccounting.conserves(
      run.ready.game.campaign.baseline, record), Right(()))
    assert(distantlands.gameplay.setup.GameStartRules.evolve(catalog, record,
      distantlands.gameplay.setup.LaterGameFixture.orders(record, p1))
      .isRight)
    val said = NoteText.said(ProcedureNotes.id(
      TriggeredProcedureRef.ChronicleStars),
      IronHand.noteKeys(TriggeredProcedureRef.ChronicleStars) ++
        distantlands.gameplay.chronicle.ChronicleStarsProcedure.noteKeys,
      run.events).map(_.text)
    assert(said.contains(s"${AncientCitySite.id.value} was archived"), said)
    assert(said.contains(s"${BuriedGiantSite.id.value} stays on the map " +
      s"with ${IronHandCard.id.value}"), said)

  /** `ironThrone` with p2 a Citizen holding Great Architect and Village
    * Idiot at Enduring Ancient City, so both p2 and p3 can build Beast. */
  private def twoBuilders: Table = ironThrone.imperial(p1, p2)
    .denizen(VillageIdiotCard, at = AncientCitySite)
    .legacy(p2, GreatArchitectCard)

  private val beast = catalog.denizen(ThreateningRoarCard.id).get.suit

  test("a suit the Great Architect built is not offered to the holder " +
      "(R32, R43)"):
    val run = through(twoBuilders, TriggeredProcedureRef.ChronicleThrone, {
      case decide if decide.decisionId == BuildEdifice.decisionId(beast) =>
        DecisionAnswer.ChooseManyAnswer(Vector(
          DecisionOptionRef.Denizen(VillageIdiotCard.id)))
    }, after = TriggeredProcedureRef.ChronicleSun)
    assert(run.asked.exists(_.decisionId == BuildEdifice.decisionId(beast)))
    assert(!run.asked.exists(_.decisionId ==
      BuildEdifice.ironHandDecisionId(beast)))
    assert(onMap(run.ready, ThreateningRoarCard.id))

  test("a walk parked at the holder's question after the Great Architect " +
      "declined finds the same edifice (R43)"):
    val started = ChronicleWalk.started(rules, twoBuilders,
      TriggeredProcedureRef.ChronicleSun)
    val parked = ChronicleWalk.walkedTo(rules, started,
      BuildEdifice.ironHandDecisionId(beast))
    val decide = ChronicleWalk.parked(parked.state).get
    assertEquals((decide.decisionId, decide.owner),
      (BuildEdifice.ironHandDecisionId(beast), p3))
    val replayed = distantlands.testkit.Unwrap.accepted(parked.events
      .foldLeft[Either[OathViolation, OathState]](Right(twoBuilders.state))(
        (state, event) => state.flatMap(rules.evolve(_, event))))
    assertEquals(ChronicleWalk.parked(replayed), Some(decide))

  test("a new Citizen discards Iron Hand before the cap, so three other " +
      "active legacies ask no discard (R30)"):
    val table = ChronicleWalk.throneHeldByChancellorP1
      .warbandsAt(AncientCitySite, p1, 1).warbandsAt(BuriedGiantSite, p3, 1)
      .legacy(p3, IronHandCard).legacy(p3, SeerCard).legacy(p3, RivalCard)
      .legacy(p3, MatchmakerCard)
    val run = through(table, TriggeredProcedureRef.ChronicleThrone, {
      case decide if decide.decisionId == OfferCitizenship.offerDecisionId =>
        DecisionAnswer.ChooseManyAnswer(Vector(DecisionOptionRef.Player(p3)))
      case decide if decide.decisionId ==
          OfferCitizenship.answerDecisionId(p3) =>
        DecisionAnswer.ChooseOneAnswer(OfferCitizenship.accept)
    }, after = TriggeredProcedureRef.ChronicleSun)
    assert(!run.asked.exists(_.decisionId ==
      OfferCitizenship.capDecisionId(p3)))
    assert(!holds(run.ready, p3))

  test("an Exile winner discards Iron Hand before the Crown's cap, so three " +
      "reactivated legacies ask no discard (R30)"):
    // p1 rules Ancient City alone, keeping Iron Hand active; Seer, Rival and
    // Matchmaker miss their goals, go dormant in step 3 and return at the
    // Crown.
    val run = through(ChronicleWalk.chronicleWonByP1
      .warbandsAt(AncientCitySite, p1, 1).legacy(p1, IronHandCard)
      .legacy(p1, SeerCard).legacy(p1, RivalCard).legacy(p1, MatchmakerCard),
      TriggeredProcedureRef.ChronicleSun)
    assert(!run.asked.exists(_.decisionId ==
      distantlands.gameplay.chronicle.ResolveLegacies.crownCapDecisionId(p1)))
    assert(!holds(run.ready, p1))
    assertEquals(run.ready.game.campaign.lineages(run.ready.game.current
      .player(p1).get.lineage).legacies.count(_.active), 3)

