package distantlands.gameplay.powers.banner

import distantlands.gameplay.{CampaignFixture, OathRules, WalkerDiceFixture}
import distantlands.gameplay.powers.{NoteText, WalkerPowerCatalog}
import distantlands.gameplay.powers.campaign.PlanDriver
import distantlands.gameplay.setup.FirstGameSetupFixture.catalog
import distantlands.gameplay.walker.ParkedDecisionAssertions
import distantlands.model._
import distantlands.model.OathState.Ready
import distantlands.testkit.{Journal, Outcomes, ParkedNode, Table}
import distantlands.testkit.Table.{p1, p2}
import distantlands.testkit.Unwrap.{accepted, present}

/** The Needle (L05): after its holder takes a banner, they may flip it
  * (Legacy in-play powers ruling 20; implementation rulings R12-R14). */
class TheNeedleSuite extends munit.FunSuite:
  private val powers = WalkerPowerCatalog.default(catalog)
  private val rules = new OathRules(catalog, walkerPowerCatalog = powers)
  private val parkedOn = new ParkedDecisionAssertions(catalog, powers)
  private def answer(state: OathState, by: PlayerId, id: String,
      ref: DecisionOptionRef) = accepted(rules.resolveWalker(state, by, id,
    DecisionAnswer.ChooseOneAnswer(ref)))

  /** p1 in Act at Ancient City with 6 favor and 6 faceup secrets, holding
    * The Needle (`active` or dormant); nobody holds a banner: the People's
    * Favor shows the Mob with 2 favor, the Darkest Secret the Wandering
    * Flame with 1 secret. */
  private def challenger(active: Boolean = true): Table = Table.start
    .favor(p1, 6).secrets(p1, faceUp = 6, faceDown = 4)
    .legacy(p1, TheNeedleCard, active)
    .peoplesFavor(None, favor = 2).darkestSecret(None, secrets = 1)

  /** p1 challenges for `banner`, bidding 3, and the walk goes on to its
    * first park or end. */
  private def challenged(table: Table, banner: Banner): OathTransition =
    val started = accepted(rules.startWalker(Ready(table.ready),
      ActionRef.Challenge, p1))
    val chosen = answer(started.state, p1, "challenge.banner",
      DecisionOptionRef.Banner(banner))
    val bid = accepted(rules.resolveWalker(chosen.state, p1,
      "challenge.amount", DecisionAnswer.ChooseAmountAnswer(3)))
    bid.copy(events = started.events ++ chosen.events ++ bid.events)

  private def asked(state: OathState, banner: Banner): Boolean =
    parkedOn.parkedDecision(state).exists(facts =>
      facts.decision == TheNeedle.decisionId(banner) && facts.awaiting == p1)

  private def lines(events: Vector[OathEvent]) =
    NoteText.said(TheNeedle.id, TheNeedle.noteKeys, events)

  test("after a challenge takes the People's Favor its holder is asked, " +
      "keep first, and flipping turns it to the Grand Council (R13)"):
    val taken = challenged(challenger(), Banner.PeoplesFavor)
    assert(asked(taken.state, Banner.PeoplesFavor))
    present(accepted(ParkedNode.of(taken.state, catalog, powers)),
      "a park") match
      case ParkedNode.Decision(_, decide, awaiting) =>
        assertEquals((decide.decisionId, awaiting),
          ("power.the-needle.peoples-favor", p1))
        decide.query match
          case one: DecisionQuery.ChooseOne =>
            assertEquals(one.options.collect {
              case DecisionOption.Button(ref, label) => (ref, label) },
              Vector(TheNeedle.keep -> "Keep this side",
                TheNeedle.flip -> "Flip it"))
          case other => fail(s"expected a choose-one, got $other")
      case other => fail(s"expected a decision, parked on $other")
    val done = answer(taken.state, p1,
      TheNeedle.decisionId(Banner.PeoplesFavor), TheNeedle.flip)
    val banner = Outcomes.ready(done.state).game.current.banners.peoplesFavor
    assertEquals((banner.active, banner.holder, banner.favor),
      (PeoplesFavorFace.GrandCouncil, Some(p1), 3))
    assertEquals(lines(done.events), Vector(NoteText.Said("flipped",
      s"${p1.value} flipped the People's Favor with ${TheNeedleCard.id.value}",
      covers = false)))

  test("keeping the side changes nothing and writes no line (R12)"):
    val taken = challenged(challenger(), Banner.PeoplesFavor)
    val done = answer(taken.state, p1,
      TheNeedle.decisionId(Banner.PeoplesFavor), TheNeedle.keep)
    assertEquals(Outcomes.ready(done.state).game.current.banners.peoplesFavor
      .active, PeoplesFavorFace.Mob)
    assertEquals(lines(done.events), Vector.empty)

  test("flipping the Darkest Secret to the Festival places no marker"):
    val taken = challenged(challenger(), Banner.DarkestSecret)
    val done = answer(taken.state, p1,
      TheNeedle.decisionId(Banner.DarkestSecret), TheNeedle.flip)
    val banner = Outcomes.ready(done.state).game.current.banners.darkestSecret
    assertEquals((banner.active, banner.festival),
      (DarkestSecretFace.Festival, None))

  test("after a raid takes the Darkest Secret from the Festival, flipping " +
      "turns it to the Wandering Flame"):
    // p1 raids p2 at Ancient City; p2 holds the Darkest Secret on the
    // Festival with 4 secrets, its marker at Ancient City.
    import CampaignFixture.on
    val base = CampaignFixture.board(warbands = 5, supply = 7)
    val b = on(base)(_.pawn(p2, at = base.origin).boardWarbands(p2, 2)
      .legacy(p1, TheNeedleCard).darkestSecret(Some(p2), secrets = 4,
        face = DarkestSecretFace.Festival).festival(base.origin))
    val run = PlanDriver.commit(CampaignFixture.rules(WalkerDiceFixture.raid),
      b, 4, targets = Vector(DecisionOptionRef.Banner(Banner.DarkestSecret)),
      raid = true).finishPlanWindows
    assert(PlanDriver.awaits(run, p1, TheNeedle.decisionId(
      Banner.DarkestSecret)))
    val done = run.pick(p1, TheNeedle.decisionId(Banner.DarkestSecret),
      TheNeedle.flip)
    val banner = Outcomes.ready(done.state).game.current.banners.darkestSecret
    assertEquals((banner.active, banner.holder, banner.festival),
      (DarkestSecretFace.WanderingFlame, Some(p1), None))

  test("a dormant Needle's holder, or a banner going to another player, " +
      "asks nothing (ruling 1)"):
    val dormant = challenged(challenger(active = false), Banner.PeoplesFavor)
    assert(!asked(dormant.state, Banner.PeoplesFavor))
    val theirs = Table.start.favor(p1, 6).legacy(p2, TheNeedleCard)
      .peoplesFavor(None, favor = 2)
    val other = challenged(theirs, Banner.PeoplesFavor)
    assertEquals(parkedOn.parkedDecision(other.state).map(_.decision)
      .filter(_.startsWith("power.the-needle")), None)

  test("a walk resumed at The Needle's question flips once"):
    val table = challenger()
    val taken = challenged(table, Banner.PeoplesFavor)
    val resumed = accepted(Journal.evolved(rules, table.ready, taken.events))
    val done = answer(resumed, p1,
      TheNeedle.decisionId(Banner.PeoplesFavor), TheNeedle.flip)
    assertEquals(Outcomes.ready(done.state).game.current.banners.peoplesFavor
      .active, PeoplesFavorFace.GrandCouncil)

  test("one raid hands over both banners: two questions, the People's " +
      "Favor first, resuming between them (R14)"):
    // p1 raids p2 at the Campaign's origin; p2 holds the People's Favor
    // with 2 favor and the Darkest Secret with 2 secrets, both targets.
    import CampaignFixture.on
    val base = CampaignFixture.board(warbands = 5, supply = 7)
    val b = on(base)(_.pawn(p2, at = base.origin).boardWarbands(p2, 2)
      .legacy(p1, TheNeedleCard).peoplesFavor(Some(p2), favor = 2)
      .darkestSecret(Some(p2), secrets = 2))
    val run = PlanDriver.commit(CampaignFixture.rules(WalkerDiceFixture.raid),
      b, 4, targets = Vector(DecisionOptionRef.Banner(Banner.PeoplesFavor),
        DecisionOptionRef.Banner(Banner.DarkestSecret)),
      raid = true).finishPlanWindows
    assert(PlanDriver.awaits(run, p1, TheNeedle.decisionId(
      Banner.PeoplesFavor)))
    val first = run.pick(p1, TheNeedle.decisionId(Banner.PeoplesFavor),
      TheNeedle.flip)
    assert(PlanDriver.awaits(first, p1, TheNeedle.decisionId(
      Banner.DarkestSecret)))
    // The second question survives a reload: the journal replays to it.
    val reloaded = accepted(Journal.evolved(run.game, b.ready, first.events))
    assertEquals(reloaded, first.state)
    val done = first.pick(p1, TheNeedle.decisionId(Banner.DarkestSecret),
      TheNeedle.keep)
    val banners = Outcomes.ready(done.state).game.current.banners
    assertEquals((banners.peoplesFavor.holder, banners.peoplesFavor.active,
      banners.darkestSecret.holder, banners.darkestSecret.active),
      (Some(p1), PeoplesFavorFace.GrandCouncil, Some(p1),
        DarkestSecretFace.WanderingFlame))
    assertEquals(lines(done.events).map(_.key), Vector("flipped"))
