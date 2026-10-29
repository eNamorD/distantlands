package oathdigital.gameplay.chronicle

import oathdigital.gameplay.{OathRules, WalkerDiceFixture}
import oathdigital.gameplay.powers.WalkerPowerCatalog
import oathdigital.gameplay.setup.FirstGameSetupFixture.catalog
import oathdigital.model._
import oathdigital.model.DecisionAnswer.ChooseOneAnswer
import oathdigital.testkit.{RecordedOps, Table}
import oathdigital.testkit.Table.p1
import oathdigital.testkit.Unwrap.accepted

/** A game won for real: p1 is the Usurper as their Wake begins, wins, and
  * answers every Chronicle task to the journaled Chronicle. */
object EvolvedChronicleFixture:
  val rules: OathRules = new OathRules(catalog,
    walkerPowerCatalog = WalkerPowerCatalog.default(catalog),
    walkerDice = WalkerDiceFixture.reversingShuffle)

  val usurperWakes: OathState = Table.start.turn(p1, Phase.Wake)
    .oathkeeper(Some(p1), TitleSide.Usurper).state

  /** The Rest that ends the game, up to Start Chronicle. */
  lazy val won: OathTransition = accepted(rules.next(
    OathTransition(usurperWakes, Vector.empty), PhaseTransitionRef.FinishRest))

  /** Start Chronicle and every task, answered. */
  lazy val done: OathTransition =
    ChronicleWalk.answeringEverything(rules, accepted(rules.resolveWalker(
      won.state, p1, ChronicleBeginProcedure.decisionId,
      ChooseOneAnswer(ChronicleBeginProcedure.start))))

  /** The Chronicle the walk journals. */
  lazy val record: Chronicle = RecordedOps.all(done.events).collect {
    case ChronicleUpdated(chronicle) => chronicle
  }.last
