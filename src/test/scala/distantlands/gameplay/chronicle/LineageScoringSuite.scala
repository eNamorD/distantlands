package distantlands.gameplay.chronicle

import distantlands.catalog.holding.{CircleOfSwordsCard, HeirloomCard,
  TheNeedleCard, WealthyCard}
import distantlands.gameplay.SacredGroundCard
import distantlands.gameplay.GrandMaskCard
import distantlands.gameplay.cards.{AncientCitySite, BrokenPeaksSite,
  BuriedGiantSite, DesolateShoreSite}
import distantlands.gameplay.powers.economy.{BirdsongCard, HallowedSpringCard,
  TheOldOakCard, VowOfPovertyCard}
import distantlands.gameplay.powers.poweruse.ForestCouncilCard
import distantlands.gameplay.powers.whenplayed.AutumnWindCard
import distantlands.gameplay.{OathRules, WalkerDiceFixture}
import distantlands.gameplay.actions.ProcedureNotes
import distantlands.gameplay.powers.{NoteText, WalkerPowerCatalog}
import distantlands.gameplay.setup.FirstGameSetupFixture.catalog
import distantlands.model._
import distantlands.testkit.{Outcomes, RecordedOps, Table}
import distantlands.testkit.Table.{p1, p2, p3}
import distantlands.testkit.Unwrap.accepted

/** Score Lineage Points (CR p. 37, Chronicle ruling 13): p1 won and scores
  * 3, and may give 1 point to another seated player for 2 of theirs. */
class LineageScoringSuite extends munit.FunSuite:
  private val rules = new OathRules(catalog,
    walkerPowerCatalog = WalkerPowerCatalog.default(catalog),
    walkerDice = WalkerDiceFixture.reversingShuffle)

  private def atGifts(using munit.Location): OathTransition =
    atGiftsOn(ChronicleWalk.throneHeldByChancellorP1
      .warbandsAt(AncientCitySite, p1, 1))

  /** The Throne on `table`, every step before Score Lineage Points
    * answered plainly, parked on the gifts. */
  private def atGiftsOn(table: Table)(using munit.Location): OathTransition =
    ChronicleWalk.walkedTo(rules, accepted(rules.next(OathTransition(
      table.state, Vector.empty), TriggeredProcedureRef.ChronicleSun)),
      LineageScoring.giftsDecisionId)

  private def gifting(toP2: Int, toP3: Int)(using munit.Location)
      : OathTransition = giftingOn(atGifts, toP2, toP3)

  private def giftingOn(at: OathTransition, toP2: Int, toP3: Int)(
      using munit.Location): OathTransition =
    val next = accepted(rules.resolveWalker(at.state, p1,
      LineageScoring.giftsDecisionId, DecisionAnswer.DistributeAnswer(Vector(
        DistributeAmount(DecisionOptionRef.Player(p2), toP2),
        DistributeAmount(DecisionOptionRef.Player(p3), toP3)))))
    OathTransition(next.state, at.events ++ next.events)

  /** The lines Score Lineage Points wrote, in order. */
  private def scoreLines(events: Vector[OathEvent]): Vector[String] =
    val keys = LineageScoring.noteKeys
    NoteText.said(ProcedureNotes.id(TriggeredProcedureRef.ChronicleThrone),
      keys, events).filter(said => keys.exists(_.name == said.key))
      .map(_.text)

  private def scoreOf(ready: ReadyGame, player: PlayerId): Int =
    ready.game.campaign.era.score(
      ready.game.current.player(player).get.lineage)

  test("the winner may give the other seated players one point for two"):
    val parked = ChronicleWalk.parked(atGifts.state).get
    assertEquals(parked.owner, p1)
    parked.query match
      case spread: DecisionQuery.Distribute =>
        assertEquals((spread.slots.map(slot => slot.ref -> slot.maximum),
          spread.minTotal, spread.maxTotal), (Vector(
            DecisionOptionRef.Player(p2) -> 1,
            DecisionOptionRef.Player(p3) -> 1), 0, 1))
      case other => fail(other.toString)
    val before = Outcomes.ready(atGifts.state)
    val after = Outcomes.ready(gifting(1, 0).state)
    assertEquals(Vector(p1, p2, p3).map(player =>
      scoreOf(after, player) - scoreOf(before, player)), Vector(1, 1, 0))

  test("keeping every point scores 3"):
    val before = Outcomes.ready(atGifts.state)
    val after = Outcomes.ready(gifting(0, 0).state)
    assertEquals(scoreOf(after, p1) - scoreOf(before, p1), 3)

  test("the lines name the points earned, each gift and the points kept " +
      "(R28)"):
    assertEquals(scoreLines(gifting(1, 0).events), Vector(
      "p1 scored 3 lineage points",
      "p1 gave up 2 points to give p2 1 point.",
      "p1 keeps 1 point"))

  test("the earned total is written before the gift question"):
    assertEquals(scoreLines(atGifts.events),
      Vector("p1 scored 3 lineage points"))

  /** p1, the Chancellor, with the Empire ruling Ancient City (two Beast
    * denizens and a Nomad edifice) and Broken Peaks (a ruined Beast
    * edifice); Buried Giant, which p2 rules, holds a Beast denizen, and p1
    * advises with another. */
  private def empireWithCards: Table = ChronicleWalk.throneHeldByChancellorP1
    .warbandsAt(AncientCitySite, p1, 1).warbandsAt(BrokenPeaksSite, p1, 1)
    .warbandsAt(BuriedGiantSite, p2, 1)
    .denizen(ForestCouncilCard, AncientCitySite)
    .denizen(TheOldOakCard, AncientCitySite)
    .edifice(SacredGroundCard, EdificeSide.Intact, AncientCitySite)
    .edifice(HallowedSpringCard, EdificeSide.Ruined, BrokenPeaksSite)
    .denizen(VowOfPovertyCard, BuriedGiantSite)
    .adviser(p1, BirdsongCard)

  private def bonusOf(table: Table)(using munit.Location)
      : Vector[(Suit, Int)] =
    LineageScoring.bonus(table.ready, catalog, p1)

  test("each legacy scores 1 per card of its printed suit at the sites the " +
      "winner rules, ruined edifices included, and never advisers or other " +
      "sites (ruling 10, R32)"):
    assertEquals(bonusOf(empireWithCards.legacy(p1, TheNeedleCard)
      .legacy(p1, CircleOfSwordsCard)),
      Vector(Suit.Beast -> 3, Suit.Nomad -> 1))

  test("legacies of one suit add into one share, and a suit with no card is " +
      "left out (R31)"):
    assertEquals(bonusOf(empireWithCards.legacy(p1, TheNeedleCard)
      .legacy(p1, HeirloomCard).legacy(p1, WealthyCard)),
      Vector(Suit.Beast -> 6))

  test("a legacy already scored this game adds no bonus (ruling 8)"):
    val scored = empireWithCards.legacy(p1, TheNeedleCard)
      .legacy(p1, CircleOfSwordsCard).update(_.updateCurrent(_.copy(
        scoredLegacies = Set(TheNeedleCard.id))))
    assertEquals(bonusOf(scored), Vector(Suit.Nomad -> 1))

  test("a winner with no legacy has no bonus"):
    assertEquals(bonusOf(empireWithCards), Vector.empty)


  /** p1 holding `legacies` over the Empire's cards, parked on the gifts. */
  private def holdingAtGifts(legacies: distantlands.catalog.Legacy*)(
      using munit.Location): OathTransition =
    atGiftsOn(legacies.foldLeft(empireWithCards)(_.legacy(p1, _)))

  /** The lineage points `events` recorded for p1's lineage. */
  private def scoredByP1(at: OathTransition, events: Vector[OathEvent])
      : Vector[Int] =
    val lineage = Outcomes.ready(at.state).game.current.player(p1).get.lineage
    RecordedOps.all(events).collect {
      case LineagePointsScored(`lineage`, points) => points }

  /** The lineage points `events` recorded, by player, in the order
    * recorded. */
  private def recorded(at: OathTransition, events: Vector[OathEvent])
      : Vector[(PlayerId, Int)] =
    val players = Outcomes.ready(at.state).game.current.players
      .map(player => player.lineage -> player.player).toMap
    RecordedOps.all(events).collect {
      case LineagePointsScored(lineage, points) if players.contains(lineage) =>
        players(lineage) -> points }

  private def giftCap(at: OathTransition)(using munit.Location): Int =
    ChronicleWalk.parked(at.state).get.query match
      case spread: DecisionQuery.Distribute => spread.maxTotal
      case other => fail(other.toString)

  test("the winner earns 3 plus the bonus, and may give away half of it"):
    // The Needle over the Empire's 3 Beast cards: 3 + 3.
    val at = holdingAtGifts(TheNeedleCard)
    assertEquals(giftCap(at), 3)
    val kept = giftingOn(at, 0, 0)
    assertEquals(scoreOf(Outcomes.ready(kept.state), p1) -
      scoreOf(Outcomes.ready(at.state), p1), 6)
    assertEquals(scoreLines(kept.events),
      Vector("p1 scored 6 lineage points (3 base, 3 from Beast)"))

  test("two legacies of two suits score two shares, in suit order"):
    val at = holdingAtGifts(TheNeedleCard, CircleOfSwordsCard)
    assertEquals(giftCap(at), 3)
    assertEquals(scoreLines(giftingOn(at, 0, 0).events), Vector(
      "p1 scored 7 lineage points (3 base, 3 from Beast, 1 from Nomad)"))

  test("after a gift the lines name it and the points kept (R28)"):
    val at = holdingAtGifts(TheNeedleCard)
    assertEquals(scoreLines(giftingOn(at, 1, 0).events), Vector(
      "p1 scored 6 lineage points (3 base, 3 from Beast)",
      "p1 gave up 2 points to give p2 1 point.",
      "p1 keeps 4 points"))

  test("a winner who gives every point away records nothing and keeps 0 " +
      "points (R29)"):
    val at = holdingAtGifts(TheNeedleCard)
    val done = giftingOn(at, 2, 1)
    assertEquals(scoredByP1(at, done.events), Vector.empty)
    assertEquals(scoreOf(Outcomes.ready(done.state), p1),
      scoreOf(Outcomes.ready(at.state), p1))
    assertEquals(scoreLines(done.events), Vector(
      "p1 scored 6 lineage points (3 base, 3 from Beast)",
      "p1 gave up 4 points to give p2 2 points.",
      "p1 gave up 2 points to give p3 1 point.",
      "p1 keeps 0 points"))

  test("bandit sites and relics at the winner's sites add nothing"):
    val table = empireWithCards.legacy(p1, TheNeedleCard)
      .bandits(DesolateShoreSite, 1).denizen(AutumnWindCard, DesolateShoreSite)
      .relicAt(GrandMaskCard, AncientCitySite)
    assertEquals(bonusOf(table), Vector(Suit.Beast -> 3))

  test("a site that turns Imperial at Offer Citizenship counts"):
    // p2 rules Buried Giant, with a Beast denizen there, and accepts.
    val started = accepted(rules.next(OathTransition(empireWithCards
      .legacy(p1, TheNeedleCard).state, Vector.empty),
      TriggeredProcedureRef.ChronicleSun))
    def answering(step: OathTransition, by: PlayerId, decisionId: String,
        value: DecisionAnswer): OathTransition =
      val next = accepted(rules.resolveWalker(step.state, by, decisionId, value))
      OathTransition(next.state, step.events ++ next.events)
    val offered = answering(started, p1, OfferCitizenship.offerDecisionId,
      DecisionAnswer.ChooseManyAnswer(Vector(DecisionOptionRef.Player(p2))))
    val joined = answering(offered, p2, OfferCitizenship.answerDecisionId(p2),
      DecisionAnswer.ChooseOneAnswer(OfferCitizenship.accept))
    val at = ChronicleWalk.walkedTo(rules, joined,
      LineageScoring.giftsDecisionId)
    assertEquals(bonusOf(Table.from(Outcomes.ready(at.state))),
      Vector(Suit.Beast -> 4))
    assertEquals(scoreLines(giftingOn(at, 0, 0).events).head,
      "p1 scored 7 lineage points (3 base, 4 from Beast)")

  test("every point given away records each gift and nothing for p1"):
    val at = holdingAtGifts(TheNeedleCard)
    assertEquals(recorded(at, giftingOn(at, 2, 1).events),
      Vector(p2 -> 2, p3 -> 1))

  test("two gifts and kept points record p1's kept points first, then each " +
      "gift"):
    val at = holdingAtGifts(TheNeedleCard)
    val done = giftingOn(at, 1, 1)
    // Two gifts of 1 cost 4 of the 6 earned.
    assertEquals(recorded(at, done.events), Vector(p1 -> 2, p2 -> 1, p3 -> 1))
    assertEquals(scoreLines(done.events), Vector(
      "p1 scored 6 lineage points (3 base, 3 from Beast)",
      "p1 gave up 2 points to give p2 1 point.",
      "p1 gave up 2 points to give p3 1 point.",
      "p1 keeps 2 points"))

  test("a winner alone at the table is asked no gift question, and the " +
      "line names the total with no gift or keeps line"):
    // The engine starts a one-seat game (only the start pages ask for
    // two players): p2 and p3 are not seated.
    val alone = ChronicleWalk.throneHeldByChancellorP1.update(state =>
      val p1Lineage = state.game.current.player(p1).get.lineage
      state.updateCurrent(current => current.copy(
          players = current.players.filter(_.player == p1),
          chronicleTasks = ChronicleTask.all.map(_ -> p1).toMap))
        .updateCampaign(campaign => campaign.copy(lineages =
          campaign.lineages.filter(_._1 == p1Lineage)))
        .copy(playerColors = state.playerColors.filter(_._1 == p1)))
    val done = distantlands.gameplay.walker.WalkerCompleted(
      TriggeredProcedureRef.ChronicleThrone)
    val throne = Iterator.iterate(accepted(rules.next(OathTransition(
        alone.state, Vector.empty), TriggeredProcedureRef.ChronicleSun)))(
      step =>
        val decide = ChronicleWalk.parked(step.state).get
        assertNotEquals(decide.decisionId, LineageScoring.giftsDecisionId)
        val next = accepted(rules.resolveWalker(step.state, decide.owner,
          decide.decisionId, ChronicleWalk.plainestAnswer(decide)))
        OathTransition(next.state, step.events ++ next.events))
      .find(_.events.contains(done)).get
    assertEquals(scoreLines(throne.events),
      Vector("p1 scored 3 lineage points"))
