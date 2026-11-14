package distantlands.gameplay

import distantlands.gameplay.BannerTakeParkFixture.{ParkAfterBannerTake,
  decisionId, resumeAnswer}
import distantlands.gameplay.powers.WalkerPowerCatalog
import distantlands.gameplay.setup.FirstGameSetupFixture.catalog
import distantlands.gameplay.walker.WalkerPowers
import distantlands.model._
import distantlands.model.OathState.Ready
import distantlands.testkit.{Journal, Outcomes, Table}
import distantlands.testkit.Table.p1
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
