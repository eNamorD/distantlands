package distantlands.gameplay

import distantlands.gameplay.WalkerDiceFixture.scripted
import distantlands.gameplay.actions.dice.RerollChoice
import distantlands.gameplay.actions.explore.ExploreProcedure
import distantlands.gameplay.cards.{BuriedGiantSite, DeepWoodsSite,
  DesolateShoreSite, DunesSite, FairIsleSite, GoldenValleySite}
import distantlands.gameplay.phases.ActOffers
import distantlands.gameplay.powers.WalkerPowerCatalog
import distantlands.gameplay.powers.action.StonePortalCard
import distantlands.gameplay.powers.campaign.BrassArmyCard
import distantlands.gameplay.powers.dice.JinxCard
import distantlands.gameplay.powers.recover.SilverCharmCard
import distantlands.gameplay.powers.travel.BrassArmyMoves
import distantlands.gameplay.setup.FirstGameSetupFixture.catalog
import distantlands.gameplay.walker.{DeltaPayload, LogNoted, WalkerCompleted,
  WalkerDice, WalkerProcedureRegistry, WalkerRollFeedback,
  WalkerStepRecorded}
import distantlands.model._
import distantlands.model.DecisionAnswer.ChooseOneAnswer
import distantlands.model.OathState.Ready
import distantlands.testkit.{EconomyActions, Look, MenuPlay, Outcomes, Table}
import distantlands.testkit.Outcomes.assertWireRoundTrips
import distantlands.testkit.Table.{p1, p2}
import distantlands.testkit.Unwrap.{accepted, rejected}

/** Explore, the action Wide Horizons gives (Foundations design, slice 3,
  * "Explore"): its gates, its rolls against a difficulty of 4, the end of
  * the Atlas it takes a site from, and where the site and the pawn go. */
class ExploreProcedureSuite extends munit.FunSuite:
  import DefenseDieFace.{Blank, OneShield, TwoShields}

  private val pool = ExploreProcedure.explorePool
  private val blanks: Vector[DieFace] = Vector(Blank, Blank)
  private val oneShield: Vector[DieFace] = Vector(OneShield, Blank)
  private val fourShields: Vector[DieFace] = Vector(TwoShields, TwoShields)

  private def rulesRolling(dice: WalkerDice): OathRules = new OathRules(
    catalog, walkerPowerCatalog = WalkerPowerCatalog.default(catalog),
    walkerDice = dice)

  /** p1's Act under Wide Horizons, with full Supply, on a map cut to
    * Ancient City, Broken Peaks and Buried Giant. p1's pawn stands at
    * Buried Giant, the Hintermost site. The Atlas holds Deep Woods at its
    * Recent end, then Desolate Shore, Dunes, Fair Isle, and Golden Valley
    * at its Forgotten end. Every site on the map holds its bandits, so the
    * check after an action places none. */
  private def p1AtHintermostOfThree: Table = Table.start
    .foundation(FoundationNumber.I, FoundationFace.Altered)
    .mapCutIntoAtlas(3).pawn(p1, BuriedGiantSite).banditsRefilled

  /** [[p1AtHintermostOfThree]] with one site, Deep Woods, left in the
    * Atlas. */
  private def p1AtHintermostAtlasOfDeepWoods: ReadyGame =
    // No Table step shortens the Atlas.
    p1AtHintermostOfThree.update(_.updateCampaign(campaign =>
      campaign.copy(atlas = AtlasState(campaign.atlas.entries.take(1)))))
      .ready

  private def started(rules: OathRules, ready: ReadyGame): OathTransition =
    accepted(rules.startWalker(Ready(ready), ActionRef.Explore, p1))

  private def resolved(rules: OathRules, from: OathTransition,
      decisionId: String, option: DecisionOptionRef): OathTransition =
    val next = accepted(rules.resolveWalker(from.state, p1, decisionId,
      ChooseOneAnswer(option)))
    next.copy(events = from.events ++ next.events)

  private def parkedDecisionId(at: OathTransition): String =
    EconomyActions.parkedDecision(at.state).decisionId

  private def completed(at: OathTransition): Boolean =
    at.events.contains(WalkerCompleted(ActionRef.Explore))

  /** The lines the Explore itself journaled, by key. */
  private def exploreLines(events: Vector[OathEvent]): Vector[String] =
    events.collect { case LogNoted(_, note, _)
      if ExploreProcedure.noteKeys.exists(_.name == note.key) => note.key }

  private def menuOption(ready: ReadyGame): DecisionOption =
    MenuPlay.menuOptionFor(catalog, ready, ActionRef.Explore)

  private val exploreButton =
    DecisionOption.Button(DecisionOptionRef.Button("explore"), "Explore")

  private def unavailable(reason: String): DecisionOption =
    DecisionOption.Unavailable(exploreButton, OptionReason.Written(reason))

  test("the Act menu offers Explore for 1 Supply at the Hintermost site " +
      "under Wide Horizons"):
    val ready = p1AtHintermostOfThree.ready
    assertEquals(ExploreProcedure.refusal(ready, p1), None)
    assertEquals(menuOption(ready),
      DecisionOption.Badged(exploreButton, "1 Supply"))

  test("under Imperial Maps the Act menu lists no Explore, and a start " +
      "is refused with its reason"):
    // Foundation I cannot change during a game, so no player can ever
    // Explore in it: the menu leaves the option out.
    val ready = p1AtHintermostOfThree
      .foundation(FoundationNumber.I, FoundationFace.Normal).ready
    val listed = MenuPlay.options(Ready(Table.from(ready).phaseMenuParked
      .ready), catalog).map(_.ref)
    assert(!listed.contains(exploreButton.ref), listed)
    assert(listed.contains(DecisionOptionRef.Button("recover")), listed)
    assertEquals(rejected(new OathRules(catalog).startWalker(
      Ready(MenuPlay.withoutMenu(ready)), ActionRef.Explore, p1)),
      OathViolation.StartUnavailable(ExploreProcedure.needsWideHorizons))

  test("a pawn away from the Hintermost site cannot Explore, and the " +
      "player whose pawn is there can"):
    // p2 stands at Broken Peaks, p1 at Buried Giant, on p2's turn.
    val ready = p1AtHintermostOfThree.turn(p2, Phase.Act).ready
    assertEquals(menuOption(ready),
      unavailable("Your pawn is not at the Hintermost site"))
    assertEquals(ExploreProcedure.refusal(ready, p1), None)

  test("a full map refuses Explore"):
    val ready = Table.start
      .foundation(FoundationNumber.I, FoundationFace.Altered)
      .pawn(p1, GoldenValleySite).ready
    assertEquals(ready.game.current.map.nextSlot, None)
    assertEquals(menuOption(ready),
      unavailable("The map has no empty site slot"))

  test("an Atlas with no site refuses Explore"):
    val ready = p1AtHintermostOfThree.update(_.updateCampaign(
      _.copy(atlas = AtlasState(Vector.empty)))).ready
    assertEquals(menuOption(ready), unavailable("The Atlas holds no site"))

  test("without Supply the Act menu says what Explore needs"):
    assertEquals(menuOption(p1AtHintermostOfThree.supply(p1, 0).ready),
      unavailable("Needs 1 Supply; you have 0"))

  test("a refusal that carries no reason of its own reads as Explore's"):
    assertEquals(ActOffers.reason(ActionRef.Explore,
      OathViolation.PawnSiteMissing(p1)), "Cannot explore now")

  test("any role may Explore: a Chancellor and a Citizen at the " +
      "Hintermost site are offered it"):
    val imperial = p1AtHintermostOfThree.imperial(p1, p2)
      .pawn(p2, BuriedGiantSite)
    assert(menuOption(imperial.ready).available)
    assert(menuOption(imperial.turn(p2, Phase.Act).ready).available)

  test("a roll that reaches 4 asks Recent or Forgotten, and Forgotten " +
      "places the Atlas's last site in the next slot with the explorer's " +
      "pawn on it"):
    val ready = p1AtHintermostOfThree.ready
    val rules = rulesRolling(scripted(fourShields))
    val rolled = started(rules, ready)
    assertEquals(parkedDecisionId(rolled), ExploreProcedure.endDecisionId)
    assertEquals(EconomyActions.parkedDecision(rolled.state).query,
      DecisionQuery.ChooseOne(Vector(
        DecisionOption.Button(DecisionOptionRef.Button("recent"),
          "Recent Sites"),
        DecisionOption.Button(DecisionOptionRef.Button("forgotten"),
          "Forgotten Sites")),
        heading = Some("Explore: place a site from")): DecisionQuery)
    val placed = resolved(rules, rolled, ExploreProcedure.endDecisionId,
      ExploreProcedure.endOption(AtlasEnd.Forgotten))
    assert(completed(placed), placed.events)
    val after = Outcomes.ready(placed.state)
    val look = Look(after)
    assertEquals(after.game.current.map.provinces,
      Vector(BuriedGiantSite.id, GoldenValleySite.id))
    assertEquals(look.pawn(p1), GoldenValleySite.id)
    assertEquals(look.supply(p1), 6)
    // Golden Valley: capacity 3, no relic slot, no printed resources.
    assertEquals(look.forces(GoldenValleySite),
      SiteForces.Occupied(ForceKind.Bandit, 3))
    assertEquals(after.game.campaign.atlas.sites.map(_.id),
      ready.game.campaign.atlas.sites.map(_.id).init)
    assertEquals(exploreLines(placed.events), Vector("explored-forgotten"))
    assertWireRoundTrips(placed.events)

  test("the site and the pawn's placement are one batch: the placement, " +
      "then the pawn's move, which is no Travel"):
    val rules = rulesRolling(scripted(fourShields))
    val placed = resolved(rules,
      started(rules, p1AtHintermostOfThree.ready),
      ExploreProcedure.endDecisionId,
      ExploreProcedure.endOption(AtlasEnd.Recent))
    val batches = placed.events.collect {
      case WalkerStepRecorded(_, DeltaPayload(ops), _) => ops }
    assertEquals(batches.last, Vector[CoreOperation](
      PlaceAtlasSite(AtlasEnd.Recent, Region.Provinces,
        DeepWoodsSite.capacity, DeepWoodsSite.relicSlots,
        DeepWoodsSite.startingResources),
      Move(Piece.Pawn(p1), PositionedLocation(Location.Site(
        BuriedGiantSite.id)), PositionedLocation(Location.Site(
        DeepWoodsSite.id)))))
    assertEquals(exploreLines(placed.events), Vector("explored-recent"))
    assertEquals(Look(placed.state).pawn(p1), DeepWoodsSite.id)

  test("with one site in the Atlas nothing is asked: the roll places it"):
    val rules = rulesRolling(scripted(fourShields))
    val placed = started(rules, p1AtHintermostAtlasOfDeepWoods)
    assert(completed(placed), placed.events)
    val after = Outcomes.ready(placed.state)
    assertEquals(after.game.current.map.hintermost, Some(DeepWoodsSite.id))
    assertEquals(Look(after).pawn(p1), DeepWoodsSite.id)
    assertEquals(after.game.campaign.atlas.sites, Vector.empty)
    assertEquals(exploreLines(placed.events), Vector("explored-recent"))

  test("a short roll asks to roll again or stop, showing the dice and " +
      "the difficulty of 4; another Supply adds 2 dice to the same total"):
    val rules = rulesRolling(scripted(oneShield, oneShield, fourShields))
    val short = started(rules, p1AtHintermostOfThree.ready)
    assertEquals(parkedDecisionId(short), ExploreProcedure.choiceDecisionId)
    assertEquals(EconomyActions.parkedDecision(short.state).query,
      DecisionQuery.ChooseOne(Vector(
        DecisionOption.Button(ExploreProcedure.continueOption,
          "Roll 2 more dice (1 Supply)"),
        DecisionOption.Button(ExploreProcedure.stopOption,
          "Stop exploring")), heading = Some("Explore")): DecisionQuery)
    assertEquals(WalkerProcedureRegistry.rollFeedback(ActionRef.Explore,
      catalog, Outcomes.ready(short.state), p1,
      ExploreProcedure.choiceDecisionId),
      Some(WalkerRollFeedback(pool, target = Some(4))))
    val again = resolved(rules, short, ExploreProcedure.choiceDecisionId,
      ExploreProcedure.continueOption)
    // 1 shield, then 1 more: 2 of the 4 needed, so it asks again.
    assertEquals(parkedDecisionId(again), ExploreProcedure.choiceDecisionId)
    assertEquals(Outcomes.ready(again.state).game.current.rollOutcomes(pool)
      .score, 2)
    assertEquals(Look(again.state).supply(p1), 5)
    val reached = resolved(rules, again, ExploreProcedure.choiceDecisionId,
      ExploreProcedure.continueOption)
    assertEquals(parkedDecisionId(reached), ExploreProcedure.endDecisionId)
    assertEquals(Look(reached.state).supply(p1), 4)

  test("with no Supply left, rolling again is shown unavailable with " +
      "its reason, and answering it is refused"):
    val rules = rulesRolling(scripted(blanks))
    val broke = started(rules, p1AtHintermostOfThree.supply(p1, 1).ready)
    assertEquals(Look(broke.state).supply(p1), 0)
    assertEquals(EconomyActions.parkedDecision(broke.state).query,
      DecisionQuery.ChooseOne(Vector(
        DecisionOption.Unavailable(
          DecisionOption.Button(ExploreProcedure.continueOption,
            "Roll 2 more dice (1 Supply)"),
          OptionReason.Written("No Supply left")),
        DecisionOption.Button(ExploreProcedure.stopOption,
          "Stop exploring")), heading = Some("Explore")): DecisionQuery)
    assert(rules.resolveWalker(broke.state, p1,
      ExploreProcedure.choiceDecisionId,
      ChooseOneAnswer(ExploreProcedure.continueOption)).isLeft)

  test("stopping fails the Explore: nothing is placed and the Supply " +
      "stays spent"):
    val ready = p1AtHintermostOfThree.ready
    val rules = rulesRolling(scripted(blanks))
    val stopped = resolved(rules, started(rules, ready),
      ExploreProcedure.choiceDecisionId, ExploreProcedure.stopOption)
    assert(completed(stopped), stopped.events)
    val after = Outcomes.ready(stopped.state)
    assertEquals(after.game.current.map, ready.game.current.map)
    assertEquals(after.game.campaign.atlas, ready.game.campaign.atlas)
    assertEquals(Look(after).pawn(p1), BuriedGiantSite.id)
    assertEquals(Look(after).supply(p1), 6)
    assertEquals(exploreLines(stopped.events), Vector("failed"))

  test("Jinx rerolls an Explore roll through the generic reroll choice"):
    val jinxed = p1AtHintermostOfThree.adviser(p1, JinxCard)
      .secrets(p1, faceUp = 1).ready
    val rules = rulesRolling(scripted(blanks, fourShields))
    val rolled = started(rules, jinxed)
    val reroll = RerollChoice.decisionId(pool, 1)
    assertEquals(reroll, "explore.reroll.1")
    assertEquals(parkedDecisionId(rolled), reroll)
    val rerolled = resolved(rules, rolled, reroll,
      RerollChoice.optionRef(JinxCard.id))
    assertEquals(parkedDecisionId(rerolled), ExploreProcedure.endDecisionId)
    assertEquals(Look(rerolled.state).faceUpSecrets(p1), 0)

  test("Silver Charm is not offered: Explore asks for no modifier and no " +
      "earlier die, though its holder pays Supply for defense dice"):
    val charmed = p1AtHintermostOfThree.relic(p1, SilverCharmCard).ready
    val rules = rulesRolling(scripted(blanks, blanks, fourShields))
    // No modifier decision: the start goes straight to the first roll.
    val short = started(rules, charmed)
    assertEquals(parkedDecisionId(short), ExploreProcedure.choiceDecisionId)
    // The second payment rolls at once, with no Silver Charm question.
    val again = resolved(rules, short, ExploreProcedure.choiceDecisionId,
      ExploreProcedure.continueOption)
    assertEquals(parkedDecisionId(again), ExploreProcedure.choiceDecisionId)
    val reached = resolved(rules, again, ExploreProcedure.choiceDecisionId,
      ExploreProcedure.continueOption)
    assertEquals(parkedDecisionId(reached), ExploreProcedure.endDecisionId)

  test("a question asked as the pawn is placed comes before the " +
      "Explore's line, which still names the site"):
    // Brass Army asks its holder whenever their pawn moves or is placed.
    val armed = Table.from(p1AtHintermostAtlasOfDeepWoods)
      .relic(p1, BrassArmyCard).secrets(p1, faceUp = 1).ready
    val rules = rulesRolling(scripted(fourShields))
    val asked = started(rules, armed)
    assertEquals(parkedDecisionId(asked), BrassArmyMoves.decisionId)
    assertEquals(Look(asked.state).pawn(p1), DeepWoodsSite.id)
    assertEquals(exploreLines(asked.events), Vector.empty)
    val placed = resolved(rules, asked, BrassArmyMoves.decisionId,
      BrassArmyMoves.flip)
    assert(completed(placed), placed.events)
    assertEquals(placed.events.collect {
      case LogNoted(_, LogNote(_, "explored-recent", args, _), _) => args
    }, Vector(Vector[NoteArg](NoteArg.Site(DeepWoodsSite.id))))

  test("after the end is chosen, a question asked as the pawn is placed " +
      "still leaves the chosen site, its stored edifice ruined, and the " +
      "line naming it"):
    // Two sites in the Atlas, so the end is asked; Deep Woods stores the
    // Stone Portal.
    val armed = p1AtHintermostOfThree.update(_.updateCampaign(campaign =>
        campaign.copy(atlas = AtlasState(campaign.atlas.entries.take(2)))))
      .storedInAtlas(DeepWoodsSite, StonePortalCard)
      .relic(p1, BrassArmyCard).secrets(p1, faceUp = 1).ready
    val rules = rulesRolling(scripted(fourShields))
    val rolled = started(rules, armed)
    assertEquals(parkedDecisionId(rolled), ExploreProcedure.endDecisionId)
    val asked = resolved(rules, rolled, ExploreProcedure.endDecisionId,
      ExploreProcedure.endOption(AtlasEnd.Recent))
    assertEquals(parkedDecisionId(asked), BrassArmyMoves.decisionId)
    val placed = resolved(rules, asked, BrassArmyMoves.decisionId,
      BrassArmyMoves.flip)
    assert(completed(placed), placed.events)
    val after = Outcomes.ready(placed.state)
    assertEquals(after.game.current.map.hintermost, Some(DeepWoodsSite.id))
    assertEquals(after.game.current.map.sites(DeepWoodsSite.id).denizens,
      Vector[SiteDenizenState](EdificeState(StonePortalCard.id,
        EdificeSide.Ruined, Tokens.empty)))
    assertEquals(after.game.campaign.atlas.sites.map(_.id),
      armed.game.campaign.atlas.sites.map(_.id).tail)
    assertEquals(placed.events.collect {
      case LogNoted(_, LogNote(_, "explored-recent", args, _), _) => args
    }, Vector(Vector[NoteArg](NoteArg.Site(DeepWoodsSite.id))))

  test("a replayed Explore reaches the same map without rolling or " +
      "choosing again"):
    val ready = p1AtHintermostOfThree.ready
    val rules = rulesRolling(scripted(blanks, fourShields))
    val short = started(rules, ready)
    val placed = resolved(rules, resolved(rules, short,
      ExploreProcedure.choiceDecisionId, ExploreProcedure.continueOption),
      ExploreProcedure.endDecisionId,
      ExploreProcedure.endOption(AtlasEnd.Forgotten))
    // These rules have no dice source, so a replay that rolled would fail.
    val silent = new OathRules(catalog,
      walkerPowerCatalog = WalkerPowerCatalog.default(catalog))
    val replayed = placed.events.foldLeft[Either[OathViolation, OathState]](
      Right(Ready(ready)))((state, event) =>
      state.flatMap(silent.evolve(_, event)))
    assertEquals(accepted(replayed), placed.state)

  test("a second Explore in the same turn starts from a fresh score and " +
      "spends its own Supply for each roll"):
    val rules = rulesRolling(scripted(fourShields, oneShield, fourShields))
    val first = resolved(rules, started(rules, p1AtHintermostOfThree.ready),
      ExploreProcedure.endDecisionId,
      ExploreProcedure.endOption(AtlasEnd.Recent))
    assert(completed(first), first.events)
    val between = MenuPlay.withoutMenu(Outcomes.ready(first.state))
    assertEquals(Look(between).pawn(p1), DeepWoodsSite.id)
    assertEquals(Look(between).supply(p1), 6)
    // The pawn stands on the site just placed, the new Hintermost site.
    val short = started(rules, between)
    assertEquals(parkedDecisionId(short), ExploreProcedure.choiceDecisionId)
    assertEquals(Outcomes.ready(short.state).game.current.rollOutcomes(pool)
      .score, 1)
    assertEquals(Look(short.state).supply(p1), 5)
    val reached = resolved(rules, short, ExploreProcedure.choiceDecisionId,
      ExploreProcedure.continueOption)
    assertEquals(Look(reached.state).supply(p1), 4)
    val second = resolved(rules, reached, ExploreProcedure.endDecisionId,
      ExploreProcedure.endOption(AtlasEnd.Recent))
    assert(completed(second), second.events)
    val after = Outcomes.ready(second.state)
    assertEquals(after.game.current.map.provinces, Vector(BuriedGiantSite.id,
      DeepWoodsSite.id, DesolateShoreSite.id))
    assertEquals(Look(after).pawn(p1), DesolateShoreSite.id)
    assertEquals(exploreLines(second.events), Vector("explored-recent"))

  test("with the Hintermost site's region full, the site goes to the top " +
      "slot of the next region and the pawn follows it"):
    // The Provinces hold Buried Giant, Deep Woods and Desolate Shore; Dunes
    // waits at the Recent end.
    val full = Table.start
      .foundation(FoundationNumber.I, FoundationFace.Altered)
      .mapCutIntoAtlas(5).pawn(p1, DesolateShoreSite).banditsRefilled.ready
    assertEquals(full.game.current.map.nextSlot, Some(Region.Hinterland))
    val rules = rulesRolling(scripted(fourShields))
    val placed = resolved(rules, started(rules, full),
      ExploreProcedure.endDecisionId,
      ExploreProcedure.endOption(AtlasEnd.Recent))
    assert(completed(placed), placed.events)
    val after = Outcomes.ready(placed.state)
    assertEquals(after.game.current.map.provinces,
      full.game.current.map.provinces)
    assertEquals(after.game.current.map.hinterland, Vector(DunesSite.id))
    assertEquals(after.game.current.map.hintermost, Some(DunesSite.id))
    assertEquals(Look(after).pawn(p1), DunesSite.id)
    assert(placed.events.exists {
      case WalkerStepRecorded(_, DeltaPayload(ops), _) =>
        ops.exists {
          case place: PlaceAtlasSite => place.region == Region.Hinterland
          case _ => false
        }
      case _ => false
    }, placed.events)

  test("an Explore that fills the last slot leaves a full map and an " +
      "empty Atlas, and the menu then says the map is full"):
    // Only Golden Valley waits in the Atlas, for the Hinterland's last
    // slot.
    val last = Table.start
      .foundation(FoundationNumber.I, FoundationFace.Altered)
      .mapCutIntoAtlas(7).pawn(p1, FairIsleSite).banditsRefilled.ready
    val rules = rulesRolling(scripted(fourShields))
    val placed = started(rules, last)
    assert(completed(placed), placed.events)
    val after = MenuPlay.withoutMenu(Outcomes.ready(placed.state))
    assertEquals(after.game.current.map.nextSlot, None)
    assertEquals(after.game.current.map.hintermost, Some(GoldenValleySite.id))
    assertEquals(after.game.campaign.atlas.sites, Vector.empty)
    assertEquals(Look(after).pawn(p1), GoldenValleySite.id)
    assertEquals(menuOption(after),
      unavailable("The map has no empty site slot"))

  test("an Exile explores while another player is the Chancellor"):
    val ready = p1AtHintermostOfThree.imperial(p2).ready
    assertEquals(Imperials.role(ready, p1), Some(Role.Exile))
    assertEquals(Imperials.role(ready, p2), Some(Role.Chancellor))
    val rules = rulesRolling(scripted(fourShields))
    val placed = resolved(rules, started(rules, ready),
      ExploreProcedure.endDecisionId,
      ExploreProcedure.endOption(AtlasEnd.Forgotten))
    assert(completed(placed), placed.events)
    assertEquals(Look(placed.state).pawn(p1), GoldenValleySite.id)
    assertEquals(exploreLines(placed.events), Vector("explored-forgotten"))
