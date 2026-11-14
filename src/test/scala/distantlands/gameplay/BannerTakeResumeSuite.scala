package distantlands.gameplay

import distantlands.gameplay.BannerTakeParkFixture.{ParkAfterBannerTake,
  decisionId, resumeAnswer}
import distantlands.gameplay.powerresolver.ContributingPower
import distantlands.gameplay.powers.WalkerPowerCatalog
import distantlands.gameplay.powers.whenplayed.{FavoredSon, FavoredSonCard,
  WhenPlayedHarness}
import distantlands.gameplay.setup.FirstGameSetupFixture.catalog
import distantlands.gameplay.walker.{ProcedureWalker, WalkerOutcome,
  WalkerPowers}
import distantlands.model._
import distantlands.model.OathState.Ready
import distantlands.testkit.{Journal, Outcomes, Table}
import distantlands.testkit.Table.{p1, p2}
import distantlands.testkit.Unwrap.accepted

/** Every banner taker keeps its tree's shape when a walk parks right after
  * its take and resumes on the state the take left (Legacy in-play powers
  * R20): The Needle parks there, as `ParkAfterBannerTake` does. */
class BannerTakeResumeSuite extends munit.FunSuite:
  private val powers = WalkerPowers(
    WalkerPowerCatalog.default(catalog).powers :+ ParkAfterBannerTake)
  private val rules = new OathRules(catalog, walkerPowerCatalog = powers)

  private def answer(state: OathState, by: PlayerId, id: String,
      reply: DecisionAnswer): OathTransition =
    accepted(rules.resolveWalker(state, by, id, reply))

  /** `events` replayed from `start`, then the park after the take answered
    * by `taker`: the state the resumed walk reaches. */
  private def resumed(start: ReadyGame, events: Vector[OathEvent],
      taker: PlayerId): ReadyGame =
    Outcomes.ready(answer(accepted(Journal.evolved(rules, start, events)),
      taker, decisionId, resumeAnswer).state)

  test("Challenge: a walk parked after the take resumes and keeps the " +
      "banner with the challenger"):
    // p1, in Act at Ancient City with 6 favor, challenges for the
    // unclaimed People's Favor, with 2 favor on it, bidding 3.
    val board = Table.start.favor(p1, 6).secrets(p1, faceUp = 6,
      faceDown = 4).peoplesFavor(None, favor = 2).ready
    val started = accepted(rules.startWalker(Ready(board), ActionRef.Challenge,
      p1))
    val chosen = answer(started.state, p1, "challenge.banner",
      DecisionAnswer.ChooseOneAnswer(
        DecisionOptionRef.Banner(Banner.PeoplesFavor)))
    val bid = answer(chosen.state, p1, "challenge.amount",
      DecisionAnswer.ChooseAmountAnswer(3))
    val end = resumed(board, started.events ++ chosen.events ++ bid.events,
      p1)
    assertEquals(end.game.current.banners.peoplesFavor.holder, Some(p1))
    assertEquals(end.game.current.banners.peoplesFavor.favor, 3)

  /** `power` played from `ready` beside the park trigger, parked after its
    * take, then resumed from the replayed state with the park answered by
    * `taker`: the state the walk ends in. */
  private def whenPlayedResumed(ready: ReadyGame, power: ContributingPower,
      card: DenizenId, taker: PlayerId): WalkerOutcome.Finished =
    val beside = WhenPlayedHarness.powersBeside(power, ParkAfterBannerTake)
    val hook = WhenPlayedHarness.hook(card)
    val parked = Outcomes.parked(ProcedureWalker.advance(ready, hook, None,
      beside))
    val at = WhenPlayedHarness.replayed(ready, parked.events)
    Outcomes.finished(ProcedureWalker.resolve(at, hook, parked.tree,
      Answered(decisionId, resumeAnswer, taker), beside))

  test("Favored Son: a walk parked after the take resumes and keeps the " +
      "People's Favor with the player"):
    // p1 plays Favored Son while p2 holds the People's Favor with 3 favor.
    val ready = Table.start.adviser(p1, FavoredSonCard)
      .peoplesFavor(Some(p2), favor = 3).ready
    val done = whenPlayedResumed(ready, FavoredSon, FavoredSonCard.id, p1)
    assertEquals(done.treeless.game.current.banners.peoplesFavor.holder,
      Some(p1))
    // The take's line, after the park, is still written: the tree kept the
    // shape it parked in.
    assertEquals(WhenPlayedHarness.said(FavoredSon, done.events).map(_.key),
      Vector("took"))
