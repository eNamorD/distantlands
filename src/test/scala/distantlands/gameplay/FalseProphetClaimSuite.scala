package distantlands.gameplay

import distantlands.gameplay.actions.VisionRules
import distantlands.gameplay.actions.cardplay.CardPlayProcedure
import distantlands.gameplay.operations.{OperationPipeline, OperationPolicy}
import distantlands.gameplay.cards.{AncientCitySite, BrokenPeaksSite}
import distantlands.gameplay.powers.{NoteText, WalkerPowerCatalog}
import distantlands.gameplay.powers.whenplayed.FalseProphetClaim
import distantlands.gameplay.roles.Citizenship
import distantlands.gameplay.setup.FirstGameSetupFixture._
import distantlands.gameplay.walker.{ParkedDecisionAssertions, ProcedureWalker}
import distantlands.model._
import distantlands.model.OathState.Ready
import distantlands.testkit.{Look, Outcomes, PlacementPicks, RecordedOps,
  Table}
import distantlands.testkit.Table.{p1, p2, p3}
import distantlands.testkit.Unwrap.{accepted, present}

/** A claimed Vision's discard (catalog batch 11, rulings 14, 32 and 33):
  * p1 owns Conquest, which p2 claims with a warband. p1 stands at Ancient
  * City, in the Cradle, so p1's discards go to the Provinces. */
class FalseProphetClaimSuite extends munit.FunSuite:
  private val walkerPowers = WalkerPowerCatalog.default(catalog)
  private val rules = new OathRules(catalog, walkerPowerCatalog = walkerPowers)
  private val parked = new ParkedDecisionAssertions(catalog, walkerPowers)
  private val claimed = VisionRules.Conquest
  private val drawn = VisionRules.Faith

  private def placeId(card: VisionId): String =
    CardPlayProcedure.placementDecisionId(card)

  /** p1 at Ancient City owns Conquest, which p2 at Broken Peaks claims;
    * Faith tops the world deck. */
  private def conquestClaimedByP2: Table = Table.start
    .pawn(p1, at = AncientCitySite).pawn(p2, at = BrokenPeaksSite)
    .revealedVision(p1, claimed).warbandsOnCard(claimed, p2, 1)
    .worldDeckTop(drawn)

  /** `table`'s p1 Searches and reveals Faith over Conquest: the walk parks
    * on what the discard asks next. */
  private def revealedOverClaim(table: Table): OathState =
    val searched = accepted(SearchPlay.searched(rules, Ready(table.ready), p1))
    answer(searched.state, p1, placeId(drawn),
      DecisionOptionRef.RevealedVision(p1))

  private def answer(state: OathState, by: PlayerId, decisionId: String,
      ref: DecisionOptionRef): OathState =
    accepted(rules.resolveWalker(state, by, decisionId,
      DecisionAnswer.ChooseOneAnswer(ref))).state

  private def warbandsOnClaimed(state: OathState, owner: PlayerId): Int =
    val ready = Outcomes.ready(state)
    ready.game.current.warbandsOn(claimed,
      ForceKind.Exile(Look(ready).player(owner).lineage))

  /** The state the recorded operations of a bare walk's `events` reach
    * from `ready`: where the walk parked or ended. */
  private def replayedFrom(ready: ReadyGame, events: Vector[OathEvent])
      : ReadyGame =
    accepted(OperationPipeline.run(ready, RecordedOps.all(events),
      OperationPolicy.Permissive, Vector.empty)(Right(_))).state

  private def revealedBy(state: OathState, player: PlayerId): Vector[VisionId] =
    Look(Outcomes.ready(state)).player(player).revealedVisions.map(_.id)

  test("the owner's reveal over a claimed Vision asks the claimant, who " +
      "reveals it; their warband is killed"):
    val asked = revealedOverClaim(conquestClaimedByP2)
    parked.assertParked(asked, ActionRef.Search, placeId(claimed), p2)
    val done = answer(asked, p2, placeId(claimed),
      DecisionOptionRef.RevealedVision(p2))
    assertEquals(revealedBy(done, p2), Vector[VisionId](claimed))
    assertEquals(revealedBy(done, p1), Vector[VisionId](drawn))
    assertEquals(warbandsOnClaimed(done, p2), 0)

  test("the claimant may discard it, and may not play it facedown"):
    val asked = revealedOverClaim(conquestClaimedByP2)
    val offered = PlacementPicks.parkedOptions(asked, walkerPowers)
    val facedown = offered.collectFirst {
      case DecisionOption.Unavailable(inner, reason)
          if inner.ref.isInstanceOf[DecisionOptionRef.AdviserPlay] => reason }
    assertEquals(facedown, Some(OptionReason.Written(
      FalseProphetClaim.toFaceupOrDiscard)))
    val discard = present(offered.filter(_.available).map(_.ref).collectFirst {
      case pile: DecisionOptionRef.DiscardPile => pile }, "a discard option")
    val done = answer(asked, p2, placeId(claimed), discard)
    assertEquals(revealedBy(done, p2), Vector.empty)
    assertEquals(warbandsOnClaimed(done, p2), 0)

  test("a Citizen claimant can only discard it"):
    val asked = revealedOverClaim(conquestClaimedByP2.imperial(p3, p2))
    val available = PlacementPicks.parkedOptions(asked, walkerPowers)
      .filter(_.available).map(_.ref)
    assert(available.forall(_.isInstanceOf[DecisionOptionRef.DiscardPile]),
      available.toString)
    assertEquals(available.size, 1)

  test("the owner turning Citizen discards the claimed Vision and asks " +
      "the claimant on the owner's turn"):
    val ready = conquestClaimedByP2.imperial(p3).turn(p1, Phase.Act).ready
    val become = Citizenship.become(p1)
    val outcome = Outcomes.parked(ProcedureWalker.advance(ready, become,
      None, walkerPowers))
    // The question is read where the walk parked: after the role change's
    // batch, whose operations are held for the follow-up.
    val parkedAt = replayedFrom(ready, outcome.events)
    val decide = present(ProcedureWalker.parkedDecide(parkedAt, become,
      outcome.tree, walkerPowers), "the question")
    assertEquals((decide.decisionId, decide.owner), (placeId(claimed), p2))

  test("two claimed Visions discarded together are each asked before " +
      "either warband is killed"):
    // p1 kept Conquest and Sanctuary (Desecrated Ground); p2 claims both,
    // and p1 turning Citizen discards both in one batch.
    val sanctuary = VisionRules.Sanctuary
    val ready = conquestClaimedByP2.revealedVision(p1, sanctuary)
      .warbandsOnCard(sanctuary, p2, 1).imperial(p3).turn(p1, Phase.Act)
      .ready
    val become = Citizenship.become(p1)
    val first = Outcomes.parked(ProcedureWalker.advance(ready, become, None,
      walkerPowers))
    val atFirst = replayedFrom(ready, first.events)
    val second = Outcomes.parked(ProcedureWalker.resolve(atFirst, become,
      first.tree, Answered(placeId(claimed), DecisionAnswer.ChooseOneAnswer(
        DecisionOptionRef.RevealedVision(p2)), p2), walkerPowers))
    val events = first.events ++ second.events
    val atSecond = replayedFrom(ready, events)
    // Neither warband is killed yet, so Sanctuary is still claimed.
    assertEquals(atSecond.game.current.warbandsOn(sanctuary,
      ForceKind.Exile(Look(atSecond).player(p2).lineage)), 1)
    val asked = present(ProcedureWalker.parkedDecide(atSecond, become,
      second.tree, walkerPowers), "the second question")
    assertEquals((asked.decisionId, asked.owner), (placeId(sanctuary), p2))
    val pile = present(asked.query match
      case one: DecisionQuery.ChooseOne => one.options.map(_.ref).collectFirst {
        case pile: DecisionOptionRef.DiscardPile => pile }
      case _ => None, "a discard option")
    val done = Outcomes.finished(ProcedureWalker.resolve(atSecond, become,
      second.tree, Answered(placeId(sanctuary),
        DecisionAnswer.ChooseOneAnswer(pile), p2), walkerPowers))
    val end = replayedFrom(ready, events ++ done.events)
    assertEquals(Look(end).player(p2).revealedVisions.map(_.id),
      Vector[VisionId](claimed))
    val kind = ForceKind.Exile(Look(end).player(p2).lineage)
    assertEquals((end.game.current.warbandsOn(claimed, kind),
      end.game.current.warbandsOn(sanctuary, kind)), (0, 0))

  test("a claimant who swaps one claimed Vision out for another starts no " +
      "claim on it"):
    // p1 turning Citizen discards Conquest and Sanctuary, both p2's claims;
    // p2 reveals Conquest, then Sanctuary, which discards Conquest again.
    val sanctuary = VisionRules.Sanctuary
    val ready = conquestClaimedByP2.revealedVision(p1, sanctuary)
      .warbandsOnCard(sanctuary, p2, 1).imperial(p3).turn(p1, Phase.Act)
      .ready
    val become = Citizenship.become(p1)
    val first = Outcomes.parked(ProcedureWalker.advance(ready, become, None,
      walkerPowers))
    val second = Outcomes.parked(ProcedureWalker.resolve(
      replayedFrom(ready, first.events), become, first.tree,
      Answered(placeId(claimed), DecisionAnswer.ChooseOneAnswer(
        DecisionOptionRef.RevealedVision(p2)), p2), walkerPowers))
    val events = first.events ++ second.events
    val done = Outcomes.finished(ProcedureWalker.resolve(
      replayedFrom(ready, events), become, second.tree,
      Answered(placeId(sanctuary), DecisionAnswer.ChooseOneAnswer(
        DecisionOptionRef.RevealedVision(p2)), p2), walkerPowers))
    val end = replayedFrom(ready, events ++ done.events)
    assertEquals(Look(end).player(p2).revealedVisions.map(_.id),
      Vector[VisionId](sanctuary))
    val kind = ForceKind.Exile(Look(end).player(p2).lineage)
    assertEquals((end.game.current.warbandsOn(claimed, kind),
      end.game.current.warbandsOn(sanctuary, kind)), (0, 0))

  /** `conquestClaimedByP2`, where p2 owns Sanctuary, which p3 claims. */
  private def sanctuaryClaimedByP3: Table = conquestClaimedByP2
    .revealedVision(p2, VisionRules.Sanctuary)
    .warbandsOnCard(VisionRules.Sanctuary, p3, 1)

  test("a claimant's reveal that discards a Vision another player claims " +
      "asks that player in turn, then kills both warbands"):
    val sanctuary = VisionRules.Sanctuary
    val asked = revealedOverClaim(sanctuaryClaimedByP3)
    parked.assertParked(asked, ActionRef.Search, placeId(claimed), p2)
    val nested = answer(asked, p2, placeId(claimed),
      DecisionOptionRef.RevealedVision(p2))
    parked.assertParked(nested, ActionRef.Search, placeId(sanctuary), p3)
    val done = answer(nested, p3, placeId(sanctuary),
      DecisionOptionRef.RevealedVision(p3))
    assertEquals(revealedBy(done, p1), Vector[VisionId](drawn))
    assertEquals(revealedBy(done, p2), Vector[VisionId](claimed))
    assertEquals(revealedBy(done, p3), Vector[VisionId](sanctuary))
    val ready = Outcomes.ready(done)
    def on(vision: VisionId, owner: PlayerId): Int =
      ready.game.current.warbandsOn(vision,
        ForceKind.Exile(Look(ready).player(owner).lineage))
    assertEquals((on(claimed, p2), on(sanctuary, p3)), (0, 0))

  test("the nested claimant's reveal is judged for them"):
    // Sacred Ground stands at Ancient City, where p1 and p2 stand, so both
    // reveal; p3 at Broken Peaks may only discard Sanctuary.
    val table = sanctuaryClaimedByP3.pawn(p2, at = AncientCitySite)
      .pawn(p3, at = BrokenPeaksSite)
      .edifice(SacredGroundCard.id, EdificeSide.Intact,
        at = AncientCitySite.id)
    val nested = answer(revealedOverClaim(table), p2, placeId(claimed),
      DecisionOptionRef.RevealedVision(p2))
    val offered = PlacementPicks.parkedOptions(nested, walkerPowers)
      .filter(_.available).map(_.ref)
    assert(!offered.contains(DecisionOptionRef.RevealedVision(p3)),
      offered.toString)
    assert(offered.exists(_.isInstanceOf[DecisionOptionRef.DiscardPile]),
      offered.toString)

  test("with two claimants the current player chooses who plays it, and " +
      "both warbands are killed"):
    val asked = revealedOverClaim(conquestClaimedByP2
      .warbandsOnCard(claimed, p3, 1))
    val chooser = FalseProphetClaim.claimantDecisionId(claimed)
    parked.assertParked(asked, ActionRef.Search, chooser, p1)
    val chosen = answer(asked, p1, chooser, DecisionOptionRef.Player(p3))
    parked.assertParked(chosen, ActionRef.Search, placeId(claimed), p3)
    val done = answer(chosen, p3, placeId(claimed),
      DecisionOptionRef.RevealedVision(p3))
    assertEquals(revealedBy(done, p3), Vector[VisionId](claimed))
    assertEquals(warbandsOnClaimed(done, p2), 0)
    assertEquals(warbandsOnClaimed(done, p3), 0)

  test("the claimant's reveal is judged for the claimant"):
    // Sacred Ground stands at p1's Ancient City, so p1 reveals Faith there.
    // p2 may reveal Conquest only with their pawn there too.
    def claimantMayRevealFrom(claimantSite: SiteId): Boolean =
      val asked = revealedOverClaim(conquestClaimedByP2
        .pawn(p2, at = claimantSite)
        .edifice(SacredGroundCard.id, EdificeSide.Intact,
          at = AncientCitySite.id))
      PlacementPicks.parkedOptions(asked, walkerPowers).filter(_.available)
        .map(_.ref).contains(DecisionOptionRef.RevealedVision(p2))
    assert(!claimantMayRevealFrom(BrokenPeaksSite.id))
    assert(claimantMayRevealFrom(AncientCitySite.id))

  test("the claimant's reveal at Desecrated Ground keeps their Visions, " +
      "and the line names them"):
    val table = conquestClaimedByP2.revealedVision(p2, VisionRules.Sanctuary)
      .edifice(SacredGroundCard.id, EdificeSide.Ruined,
        at = BrokenPeaksSite.id)
    val asked = revealedOverClaim(table)
    val done = accepted(rules.resolveWalker(asked, p2, placeId(claimed),
      DecisionAnswer.ChooseOneAnswer(DecisionOptionRef.RevealedVision(p2))))
    assertEquals(revealedBy(done.state, p2),
      Vector[VisionId](VisionRules.Sanctuary, claimed))
    assertEquals(NoteText.said(DesecratedGroundRule.power,
      DesecratedGroundRule.noteKeys, done.events), Vector(NoteText.Said(
      "kept-off-turn", s"${p2.value} kept ${VisionRules.Sanctuary.value}.",
      covers = false)))
