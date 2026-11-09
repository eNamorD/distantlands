package oathdigital.gameplay.powers.economy

import oathdigital.gameplay.GrandMaskCard
import oathdigital.gameplay.cards.{AncientCitySite, BrokenPeaksSite}
import oathdigital.gameplay.powerresolver.BatchContext
import oathdigital.gameplay.powers.NoteText
import oathdigital.gameplay.powers.action.{RovingTerror, RovingTerrorCard,
  TamingCharm, TamingCharmCard}
import oathdigital.gameplay.powers.campaign.{HorseArchersCard, WildMounts,
  WildMountsCard}
import oathdigital.gameplay.powers.setup.GreatForgeCard
import oathdigital.gameplay.setup.FirstGameSetupFixture.catalog
import oathdigital.model._
import oathdigital.model.DecisionAnswer.ChooseOneAnswer
import oathdigital.testkit.{Look, MenuPlay, Outcomes, PowerRules, Table}
import oathdigital.testkit.Outcomes.assertWireRoundTrips
import oathdigital.testkit.Table.{p1, p2, p3}
import oathdigital.testkit.Unwrap.accepted

/** Wild Pastures (E07, intact): "Whenever this card's ruler would discard
  * a Nomad card from a power, they may instead pay 1 favor to the Nomad
  * bank." (catalog batch 11, slice 4; rulings 11 and 20 to 25) */
class WildPasturesSuite extends munit.FunSuite:
  private val rules = PowerRules.withWalkerAndPhasePowers
  private val home = AncientCitySite.id
  private val peaks = BrokenPeaksSite.id
  /** A Nomad battle plan with no other use here. */
  private val nomad = WildMountsCard.id
  private val pastures = WildPasturesCard

  /** p1's Act at Ancient City with `favor` on their board. Wild Pastures
    * stands at Broken Peaks, which `ruler` holds with 2 warbands, or nobody
    * rules. The Nomad bank holds 5. The quiet table leaves Broken Peaks
    * unruled. */
  private def pasturesAtPeaksRuledBy(ruler: Option[PlayerId],
      favor: Int = 2): Table =
    ruler.foldLeft(Table.start.turn(p1, Phase.Act).pawn(p1, home)
      .favor(p1, favor).edifice(pastures, EdificeSide.Intact, at = peaks)
      .bankFavor(Suit.Nomad, 5))((table, player) =>
      table.warbandsAt(peaks, player, 2))

  private def p1RulesPastures: Table = pasturesAtPeaksRuledBy(Some(p1))

  /** p1 starts the phase power `power` printed on `card`. */
  private def use(ready: ReadyGame, power: PowerId, card: DenizenId)
      : OathTransition = accepted(rules.startWalker(
    OathState.Ready(MenuPlay.withoutMenu(ready)), ActionRef.UsePower(power),
    p1, Vector(DecisionOptionRef.Denizen(card))))

  private def answer(from: OathTransition, by: PlayerId, decision: String,
      option: DecisionOptionRef): OathTransition = accepted(
    rules.resolveWalker(from.state, by, decision, ChooseOneAnswer(option)))

  private def after(t: OathTransition): ReadyGame = Outcomes.ready(t.state)

  private def awaits(t: OathTransition, decision: String): Boolean =
    PowerRules.parkedUnderWalkerAndPhasePowers.parkedDecision(t.state)
      .exists(_.decision == decision)

  /** p1 uses Taming Charm on the Nomad card at their site, and the walk
    * parks on Wild Pastures' question or ends. */
  private def tamedTheNomad(ready: ReadyGame): OathTransition =
    answer(use(ready, TamingCharm.id, TamingCharmCard.id), p1,
      TamingCharm.decisionId, DecisionOptionRef.Denizen(nomad))

  /** `discard` as a power p1 uses builds it. */
  private def byP1sPower(discard: Discard.Denizen): Discard.Denizen =
    discard.copy(cause = Some(DiscardCause(TamingCharm.id)))

  private def nomadDiscardFrom(from: Location): Discard.Denizen =
    byP1sPower(Discard.Denizen(nomad, PositionedLocation(from),
      Region.Provinces, Suit.Nomad, 0, 0, p1))

  private def asks(ready: ReadyGame, discard: CoreOperation): Boolean =
    WildPastures.askBefore(BatchContext(catalog, p1, ready), discard)
      .nonEmpty

  test("a phase power's Nomad discard asks its ruler, and paying keeps " +
      "the card and still gains"):
    val ready = p1RulesPastures.adviser(p1, TamingCharmCard.id)
      .denizen(nomad, at = home).ready
    val asked = tamedTheNomad(ready)
    assert(awaits(asked, WildPastures.decisionId(nomad)))
    val paid = answer(asked, p1, WildPastures.decisionId(nomad),
      WildPastures.pay)
    val end = after(paid)
    assertEquals(Look(end).cardsAt(home), Vector[CardId](nomad))
    // 2 favor, less 1 paid, plus Taming Charm's 2 (ruling 24).
    assertEquals(Look(end).favor(p1), 3)
    // The bank's 5, plus 1 paid, less 2 gained.
    assertEquals(Look(end).bankFavor(Suit.Nomad), 4)
    assertEquals(NoteText.said(WildPastures.id, WildPastures.noteKeys,
      paid.events), Vector(
      NoteText.Said("paid", "Paid 1 favor to the Nomad bank instead of " +
        s"discarding ${nomad.value}.", covers = false)))
    assertWireRoundTrips(asked.events ++ paid.events)

  test("declining discards the card as built"):
    val ready = p1RulesPastures.adviser(p1, TamingCharmCard.id)
      .denizen(nomad, at = home).ready
    val declined = answer(tamedTheNomad(ready), p1,
      WildPastures.decisionId(nomad), WildPastures.discard)
    assertEquals(Look(after(declined)).cardsAt(home), Vector.empty[CardId])
    assertEquals(NoteText.said(WildPastures.id, WildPastures.noteKeys,
      declined.events), Vector.empty)

  test("a ruler with no favor, another player's Wild Pastures, or an " +
      "unruled one asks nothing"):
    for table <- Vector(pasturesAtPeaksRuledBy(Some(p1), favor = 0),
        pasturesAtPeaksRuledBy(Some(p2)), pasturesAtPeaksRuledBy(None)) do
      val done = tamedTheNomad(table.adviser(p1, TamingCharmCard.id)
        .denizen(nomad, at = home).ready)
      assert(!awaits(done, WildPastures.decisionId(nomad)))
      assertEquals(Look(after(done)).cardsAt(home), Vector.empty[CardId])

  test("only a power's discard of a faceup Nomad card in play, or in a " +
      "discard pile, by the ruler, making no room, is asked about"):
    val ready = p1RulesPastures.denizen(nomad, at = home).ready
    val atHome = nomadDiscardFrom(Location.Site(home))
    assert(asks(ready, atHome))
    assert(asks(ready, nomadDiscardFrom(Location.RegionalDiscard(
      Region.Cradle))))
    // A rule's discard, one making room, another player's, a Discord one.
    assert(!asks(ready, atHome.copy(cause = None)))
    assert(!asks(ready, atHome.copy(cause =
      Some(DiscardCause(TamingCharm.id, makesRoom = true)))))
    assert(!asks(ready, atHome.copy(actingPlayer = p2)))
    assert(!asks(ready, atHome.copy(suit = Suit.Discord)))
    // A card in a hand (ruling 25).
    assert(!asks(ready, nomadDiscardFrom(Location.Hand(p1))))

  test("a ruler who is not the walk's actor is named in the line, and " +
      "declining replaces nothing"):
    val ready = p1RulesPastures.denizen(nomad, at = home).ready
    val ask = WildPastures.askBefore(BatchContext(catalog, p2, ready),
      nomadDiscardFrom(Location.Site(home)))
    assertEquals(ask.flatMap(_.instead(ChooseOneAnswer(WildPastures.pay)))
      .map(_.line), Some(WildPastures.paidOffTurn(
        PowerSourceRef.Card(WildPasturesCard.id), NoteArg.Player(p1),
        NoteArg.Card(nomad))))
    assertEquals(ask.flatMap(_.instead(
      ChooseOneAnswer(WildPastures.discard))), None)

  test("a facedown adviser has no suit, and a faceup one is asked about"):
    val held = p1RulesPastures.adviser(p1, nomad).ready
    val hidden = p1RulesPastures.adviser(p1, nomad, facedown = true).ready
    val fromBoard = nomadDiscardFrom(Location.PlayArea(p1))
    assert(asks(held, fromBoard))
    assert(!asks(hidden, fromBoard))

  test("a ruined Nomad edifice is asked about as a denizen is"):
    // Great Forge (E06) is a Nomad edifice.
    val ruined = p1RulesPastures
      .edifice(GreatForgeCard, EdificeSide.Ruined, at = home).ready
    assert(asks(ruined, Discard.RuinedEdifice(GreatForgeCard.id,
      PositionedLocation(Location.Site(home)), Suit.Nomad, 0, 0, p1,
      cause = Some(DiscardCause(TamingCharm.id)))))

  test("Roving Terror moving to a site with room pays, keeps the card, and " +
      "still moves"):
    def heldAt(ready: ReadyGame, site: SiteId): Int =
      ready.game.current.map.sites(site).denizens.size
    def capacityOf(site: SiteId): Int =
      catalog.site(site).map(_.capacity).getOrElse(0)
    val base = p1RulesPastures.denizen(RovingTerrorCard.id, at = home).ready
    // A site that keeps room with the Nomad card there, so the replace
    // makes none (ruling 20).
    val roomy = base.game.current.map.inPlay.find(site => site != home &&
      heldAt(base, site) < capacityOf(site) - 1)
    assert(roomy.nonEmpty)
    val target = roomy.getOrElse(peaks)
    val ready = Table.from(base).denizen(nomad, at = target).ready
    val started = use(ready, RovingTerror.id, RovingTerrorCard.id)
    val asked = answer(started, p1, RovingTerror.decisionId,
      DecisionOptionRef.SiteSlot(target, Look(ready).cardsAt(target)
        .indexOf(nomad)))
    val paid = answer(asked, p1, WildPastures.decisionId(nomad),
      WildPastures.pay)
    assertEquals(Look(after(paid)).cardsAt(target).filter(card =>
      card == nomad || card == RovingTerrorCard.id),
      Vector[CardId](nomad, RovingTerrorCard.id))

  test("the ruler is the card-level ruler: a Grand Mask wearer, during " +
      "their turn"):
    val masked = Table.start.imperial(p2, p1).turn(p3, Phase.Act)
      .warbandsAt(home, p2, 2)
      .edifice(pastures, EdificeSide.Intact, at = home)
      .relic(p3, GrandMaskCard).ready
    assertEquals(WildPastures.ruler(masked), Some(p3))
    assertEquals(WildPastures.ruler(pasturesAtPeaksRuledBy(None).ready), None)

  test("a When Played Nomad discard asks its ruler"):
    import oathdigital.gameplay.powers.whenplayed.WhenPlayedHarness
    import oathdigital.gameplay.powers.whenplayed.ThreateningRoarCard
    import oathdigital.gameplay.walker.ProcedureWalker
    import oathdigital.gameplay.powers.WalkerPowerCatalog
    val ready = WhenPlayedHarness.p1AtHomeSite.favor(p1, 1)
      .adviser(p1, ThreateningRoarCard.id)
      .denizen(nomad, at = WhenPlayedHarness.homeSite)
      .warbandsAt(WhenPlayedHarness.awaySite, p1, 2)
      .edifice(pastures, EdificeSide.Intact, at = WhenPlayedHarness.awaySite)
      .ready
    val park = Outcomes.parked(ProcedureWalker.advance(ready,
      WhenPlayedHarness.hook(ThreateningRoarCard.id), None,
      WalkerPowerCatalog.default(catalog)))
    assertEquals(park.tree.at.lastOption, Some("0"))
    assert(park.tree.at.contains("asked"))

  test("Wild Mounts asks first; skipped, the Nomad plan's discard asks " +
      "Wild Pastures"):
    import oathdigital.gameplay.CampaignFixture
    import oathdigital.gameplay.actions.campaign.CampaignIds
    import oathdigital.gameplay.powers.campaign.PlanDriver
    import oathdigital.gameplay.powers.search.BrackenCard
    val bracken = PlanDriver.planlessCard(BrackenCard)
    val horseArchers = HorseArchersCard.id
    val start = Vector(nomad, bracken, horseArchers).foldLeft(
      CampaignFixture.board(warbands = 5, supply = 7))((b, id) =>
      CampaignFixture.withAdviser(b, id, Orientation.FaceUp))
    val site = start.ready.game.current.map.inPlay
      .filterNot(_ == start.origin).last
    val b = CampaignFixture.on(CampaignFixture.withEdifice(
      CampaignFixture.actorRules(start, site), site, pastures,
      EdificeSide.Intact))(_.favor(start.actor, 2))
    val asked = Vector(DecisionOptionRef.Denizen(nomad),
      DecisionOptionRef.Denizen(horseArchers)).foldLeft(
      PlanDriver.commit(CampaignFixture.rules(PlanDriver.winningDice), b, 4))(
      (run, plan) => run.pick(b.actor, CampaignIds.attackerPlan, plan))
      .finishPlanWindows
    assert(PlanDriver.awaits(asked, b.actor, WildMounts.decisionId))
    val skipped = asked.pick(b.actor, WildMounts.decisionId, WildMounts.skip)
    assert(PlanDriver.awaits(skipped, b.actor,
      WildPastures.decisionId(horseArchers)))
    val paid = skipped.pick(b.actor, WildPastures.decisionId(horseArchers),
      WildPastures.pay)
    assert(Look(paid.state).advisers(b.actor).contains(horseArchers))

  test("Search Party's discarded card asks, and paying leaves it in its " +
      "pile"):
    import oathdigital.gameplay.actions.cardplay.CardPlayProcedure.PlayedAs
    import oathdigital.gameplay.powers.whenplayed.SearchPartyFixture._
    val ready = p1HoldsSearchPartyFacedown(_.discarded(Region.Provinces,
        nomad).favor(p1, 1).warbandsAt(peaks, p1, 2)
      .edifice(pastures, EdificeSide.Intact, at = peaks))
    val asked = placedAs(peekedAndPicked(playedFaceupFromFacedown(ready),
      Region.Provinces, nomad), nomad, PlayedAs.Discarded)
    val pending = PowerRules.parkedUnderWalkerPowers.parkedDecision(
      asked.state)
    assertEquals(pending.map(_.decision),
      Some(WildPastures.decisionId(nomad)))
