package oathdigital.gameplay

import oathdigital.gameplay.actions.VisionRules
import oathdigital.gameplay.actions.cardplay.CardPlayProcedure
import oathdigital.gameplay.operations.{OperationPipeline, OperationPolicy}
import oathdigital.gameplay.cards.{AncientCitySite, BrokenPeaksSite}
import oathdigital.gameplay.powers.{NoteText, WalkerPowerCatalog}
import oathdigital.gameplay.powers.whenplayed.FalseProphetClaim
import oathdigital.gameplay.roles.Citizenship
import oathdigital.gameplay.setup.FirstGameSetupFixture._
import oathdigital.gameplay.walker.{ParkedDecisionAssertions, ProcedureWalker}
import oathdigital.model._
import oathdigital.model.OathState.Ready
import oathdigital.testkit.{Look, Outcomes, PlacementPicks, RecordedOps,
  Table}
import oathdigital.testkit.Table.{p1, p2, p3}
import oathdigital.testkit.Unwrap.{accepted, present}

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
    val parkedAt = accepted(OperationPipeline.run(ready,
      RecordedOps.all(outcome.events), OperationPolicy.Permissive,
      Vector.empty)(Right(_))).state
    val decide = present(ProcedureWalker.parkedDecide(parkedAt, become,
      outcome.tree, walkerPowers), "the question")
    assertEquals((decide.decisionId, decide.owner), (placeId(claimed), p2))

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
