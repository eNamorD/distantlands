package oathdigital.application

import oathdigital.gameplay.CampaignFixture
import oathdigital.gameplay.actions.campaign.CampaignIds
import oathdigital.gameplay.powers.campaign.{OutridersCard, StickyFireCard}
import oathdigital.gameplay.setup.FirstGameSetupFixture._
import oathdigital.model.DecisionAnswer.{ChooseAmountAnswer, ChooseOneAnswer}
import oathdigital.model.OathState.Ready
import oathdigital.model._
import oathdigital.protocol.projection.{DieFace => RolledFace,
  WalkerDecisionProjection, WalkerStatusProjection}
import oathdigital.testkit.Table.{p1, p2}
import oathdigital.testkit.Unwrap.present

/** What a parked Campaign projects: the status lines of each question (the
  * forces, the dice, the totals and the winner), the attack roll's faces,
  * score and losses at the sacrifice, and the options already answered at a
  * repeated decision. Every park is built by driving the real Campaign
  * commands over a `CampaignFixture` board, so each test proves what a real
  * park looks like, not a hand-assembled `PendingTree`. Split from
  * `WalkerDecisionProjectorSuite`, which proves the projector's option and
  * disclosure rules on substituted trees.
  */
class WalkerCampaignParkProjectionSuite extends munit.FunSuite:
  /** The actor of every `CampaignFixture` board. Both Campaign parks below
    * use its board with 5 warbands and a Supply of 7: p1 campaigns against
    * the 2 Bandits at its origin. */
  private val attacker: PlayerId = p1

  /** Task 8: the battle-plan window is a `Repeat` -- it re-asks
    * `campaign.attacker-plan` after every pick until the actor finishes, so
    * a second pass parks on the SAME decision id with one answer already
    * recorded at it. Built by driving the real commands `CampaignPlanWindowSuite`
    * drives (start, force, pick Sticky Fire), rather than hand-assembling a
    * `PendingTree`, so this proves what a real second pass looks like.
    */
  private lazy val campaignWithOnePlanPlayed: ReadyGame =
    val relic = StickyFireCard.id
    // A second, free attacker plan (Outriders) so at least one option besides
    // Finish is still offered after Sticky Fire is picked -- with only one
    // plan available the window would settle past the decision entirely
    // rather than repeat it (see `CampaignPlanWindowSuite`'s "rebuilt after
    // each pick" test), and there would be nothing to prove a repeat against.
    val b = CampaignFixture.withAdviser(CampaignFixture.withRelic(
      CampaignFixture.board(warbands = 5, supply = 7), relic),
      OutridersCard.id, Orientation.FaceUp)
    val g = CampaignFixture.rules()
    val started = g.startWalker(Ready(b.ready), ActionRef.Campaign, b.actor)
      .getOrElse(fail("Campaign must start"))
    val forced = g.resolveWalker(started.state, b.actor, CampaignIds.force,
      ChooseAmountAnswer(2)).getOrElse(fail("the force must be accepted"))
    val picked = g.resolveWalker(forced.state, b.actor, CampaignIds.attackerPlan,
      ChooseOneAnswer(DecisionOptionRef.Relic(relic)))
      .fold(e => fail(s"the Sticky Fire plan must be accepted: $e"), identity)
    picked.state match
      case Ready(ready) => ready
      case other => fail(s"expected a ready game, got $other")

  private def project(ready: ReadyGame, viewer: Option[PlayerId])
      : Option[WalkerDecisionProjection] =
    new WalkerDecisionProjector(catalog, new GamePresentationProjector(catalog))
      .project(ScopedProjectionContext(ready, viewer))

  test("a repeated decision lists what has already been answered at it"):
    val projected = project(campaignWithOnePlanPlayed, viewer = Some(attacker))
    assertEquals(projected.map(_.decisionId), Some("campaign.attacker-plan"))
    assertEquals(projected.toVector.flatMap(_.answeredOptions).map(_.label),
      Vector("Sticky Fire"))

  test("a played battle plan shows what it paid, and the window shows the " +
      "forces, the dice and the total paid"):
    val projected = project(campaignWithOnePlanPlayed, viewer = Some(attacker))
    assertEquals(projected.toVector.flatMap(_.answeredOptions).map(option =>
      option.label -> option.details), Vector("Sticky Fire" -> Vector("Free")))
    assertEquals(projected.toVector.flatMap(_.status).map(line =>
      line.label -> line.value), Vector("Attacking force" -> "2 warbands",
      "Defending force" -> "2 warbands", "Attack dice" -> "2",
      "Defense dice" -> "2", "Paid so far" -> "Nothing"))

  test("answers at other decisions do not count as answered options"):
    val projected = project(campaignAtSacrifice, viewer = Some(attacker))
    assertEquals(projected.map(_.decisionId), Some(CampaignIds.sacrifice))
    assertEquals(projected.toVector.flatMap(_.answeredOptions), Vector.empty)

  /** Task 9: a Campaign parked at the sacrifice question, after an attack of
    * two swords and a skull plus one sword with a force of two -- the skull
    * costs one warband, so one is still left to sacrifice.
    */
  private lazy val campaignAtSacrifice: ReadyGame =
    val b = CampaignFixture.board(warbands = 5, supply = 7)
    val defense = Vector.fill(present(catalog.sites.find(_.id == b.origin),
      "the origin").defense)(DefenseDieFace.Blank)
    val g = CampaignFixture.rules(CampaignFixture.dice(
      Vector(AttackDieFace.TwoSwordsSkull, AttackDieFace.OneSword), defense))
    val started = g.startWalker(Ready(b.ready), ActionRef.Campaign, b.actor)
      .getOrElse(fail("Campaign must start"))
    val committed = g.resolveWalker(started.state, b.actor, CampaignIds.force,
      ChooseAmountAnswer(2)).getOrElse(fail("the force must be accepted"))
    committed.state match
      case Ready(ready) => ready
      case other => fail(s"expected a ready game, got $other")

  test("a parked sacrifice projects the attack pool's faces, score and losses"):
    val projected = project(campaignAtSacrifice, viewer = Some(attacker))
    assertEquals(projected.map(_.decisionId), Some(CampaignIds.sacrifice))
    val outcome = projected.flatMap(_.rollOutcome)
    assertEquals(outcome.map(_.pool), Some("campaign.attack"))
    assertEquals(outcome.map(_.faces),
      Some(Vector(RolledFace.TwoSwordsSkull, RolledFace.OneSword)))
    assertEquals(outcome.map(_.score), Some(3))
    assertEquals(outcome.flatMap(_.target), None)
    assertEquals(outcome.map(_.detail), Some(Vector("1 skull loss")))

  private def facts(projected: Vector[WalkerStatusProjection])
      : Vector[(String, String)] = projected.map(line => line.label -> line.value)

  private def answered(ready: ReadyGame, id: String, answer: DecisionAnswer)
      : ReadyGame =
    val b = CampaignFixture.board(warbands = 5, supply = 7)
    val defense = Vector.fill(present(catalog.sites.find(_.id == b.origin),
      "the origin").defense)(DefenseDieFace.Blank)
    val g = CampaignFixture.rules(CampaignFixture.dice(
      Vector(AttackDieFace.TwoSwordsSkull, AttackDieFace.OneSword), defense))
    g.resolveWalker(Ready(ready), ready.game.current.walkActor, id, answer)
      .fold(e => fail(s"$id must be accepted: $e"), _.state) match
      case Ready(next) => next
      case other => fail(s"expected a ready game, got $other")

  private lazy val campaignAtForce: ReadyGame =
    val b = CampaignFixture.board(warbands = 5, supply = 7)
    CampaignFixture.rules().startWalker(Ready(b.ready), ActionRef.Campaign,
      b.actor).fold(e => fail(s"Campaign must start: $e"), _.state) match
      case Ready(ready) => ready
      case other => fail(s"expected a ready game, got $other")

  test("the force question shows the defending force and the printed " +
      "defense"):
    val projected = project(campaignAtForce, viewer = Some(attacker))
    assertEquals(projected.map(_.decisionId), Some(CampaignIds.force))
    assertEquals(facts(projected.toVector.flatMap(_.status)), Vector(
      "Defending force" -> "2 warbands", "Defense dice" -> "2"))

  test("the sacrifice shows the force left and the defense to come, and " +
      "leaves the attack total to the roll it shows"):
    val projected = project(campaignAtSacrifice, viewer = Some(attacker))
    assertEquals(facts(projected.toVector.flatMap(_.status)), Vector(
      "Attacking force" -> "1 of 2 warbands",
      "Defending force" -> "2 warbands", "Defense dice" -> "2"))

  test("a viewer waiting on the sacrifice sees the attack total too"):
    val waiting = new WalkerDecisionProjector(catalog,
      new GamePresentationProjector(catalog)).waiting(
      ScopedProjectionContext(campaignAtSacrifice, Some(p2)))
    assertEquals(facts(waiting.toVector.flatMap(_.status)), Vector(
      "Attacking force" -> "1 of 2 warbands",
      "Defending force" -> "2 warbands", "Attack total" -> "3",
      "Defense dice" -> "2"))

  test("after the defense roll the totals and the winner show"):
    val after = answered(campaignAtSacrifice, CampaignIds.sacrifice,
      ChooseAmountAnswer(0))
    val projected = project(after, viewer = Some(attacker))
    // The placement draws the defense roll, so its total is left to it.
    assertEquals(projected.map(_.decisionId), Some(CampaignIds.placement))
    assertEquals(facts(projected.toVector.flatMap(_.status)), Vector(
      "Attack total" -> "3", "Winner" -> "Attacker"))
