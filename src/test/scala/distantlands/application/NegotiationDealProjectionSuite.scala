package distantlands.application

import distantlands.gameplay.{GrandMaskCard, OathRules}
import distantlands.gameplay.actions.negotiation.{CouncilArbiterCard,
  DeedWriterCard,
  NegotiationDeal, TheTribunalCard}
import distantlands.gameplay.cards.{BrokenPeaksSite, DunesSite}
import distantlands.gameplay.operations.TheGrandScepterCard
import distantlands.gameplay.powers.action.BrassHorseCard
import distantlands.gameplay.powers.campaign.StickyFireCard
import distantlands.gameplay.powers.economy.BirdsongCard
import distantlands.gameplay.powers.rest.SilverTongueCard
import distantlands.gameplay.powers.search.TruthfulHarpCard
import distantlands.gameplay.setup.FirstGameSetupFixture.catalog
import distantlands.model._
import distantlands.model.DecisionAnswer.{AcceptDeal, ChooseManyAnswer, ProposeTerms}
import distantlands.protocol.projection.{DecisionQueryProjection,
  NegotiationCitizenshipProjection,
  GameProjection, NegotiationDealProjection, NegotiationPromiseProjection,
  NegotiationSiteProjection}
import distantlands.testkit.{MenuPlay, Table}
import distantlands.testkit.Table.{p1, p2, p3}

/** What each viewer of a parked deal is shown. */
class NegotiationDealProjectionSuite extends munit.FunSuite:
  private val rules = new OathRules(catalog)
  private val projector = new GameProjector(catalog)
  private val dealId = NegotiationDeal.dealDecisionId
  private val site = BrokenPeaksSite.id
  private val p1Relic = BrassHorseCard.id
  private val p2Relic = TruthfulHarpCard.id
  private val siteRelic = StickyFireCard.id

  /** Every player stands at Broken Peaks with 5 favor and one facedown
    * relic: p1 holds Brass Horse with a secret on it, p2 Truthful Harp and
    * p3 Grand Mask. Broken Peaks also holds Sticky Fire, which p1 knows. */
  private def gathered: Table = Table.start
    .pawn(p1, at = site).pawn(p2, at = site).pawn(p3, at = site)
    .favor(p1, 5).favor(p2, 5).favor(p3, 5)
    .relic(p1, p1Relic, facedown = true).tokens(p1Relic, secrets = 1)
    .relic(p2, p2Relic, facedown = true)
    .relic(p3, GrandMaskCard, facedown = true)
    .relicAt(siteRelic, at = site)
    .knowsRelicAt(p1, siteRelic, at = site)

  /** p1's Negotiation with `who`, parked on the deal, with `terms` proposed
    * when given. */
  private def parkedDeal(table: Table, who: Vector[PlayerId],
      terms: Option[NegotiationTerms] = None): OathState =
    val started = rules.startWalker(table.state, ActionRef.Negotiation, p1)
      .getOrElse(fail("Negotiation must start"))
    val chosen = rules.resolveWalker(started.state, p1,
      NegotiationDeal.negotiatorsDecisionId, ChooseManyAnswer(
        who.map(DecisionOptionRef.Player(_)))).getOrElse(fail("negotiators"))
    terms.fold(chosen.state)(value => rules.resolveWalker(chosen.state, p1,
      dealId, ProposeTerms(value)).getOrElse(fail("terms")).state)

  private def view(state: OathState, viewer: PlayerId): GameProjection =
    projector.project("negotiation", LoadedGame(state, 30), viewer)

  private def deal(projection: GameProjection): NegotiationDealProjection =
    projection.walkerDecision.flatMap(_.query).collect {
      case negotiate: DecisionQueryProjection.Negotiate => negotiate.deal }
      .orElse(projection.walkerWaiting.flatMap(_.deal))
      .getOrElse(fail("the deal must be projected"))

  test("the actor and every co-owner get the full decision with editing inputs"):
    val state = parkedDeal(gathered, who = Vector(p2, p3))
    Vector(p1, p2, p3).foreach { viewer =>
      val projection = view(state, viewer)
      assertEquals(projection.walkerWaiting, None, viewer.value)
      val decision = projection.walkerDecision.getOrElse(fail("owner decision"))
      assertEquals(decision.decisionId, dealId)
      assert(decision.query.exists(
        _.isInstanceOf[DecisionQueryProjection.Negotiate]))
      val editing = deal(projection).editing.getOrElse(fail("editing"))
      assertEquals(editing.editableFavor, 5)
      assert(!editing.canAccept)
      assert(projection.walkerDecision.nonEmpty)
    }

  test("a player outside the deal and the public view see it read-only"):
    val state = parkedDeal(gathered, who = Vector(p2))
    val outsider = view(state, p3)
    assertEquals(outsider.walkerDecision, None)
    val waiting = outsider.walkerWaiting.getOrElse(fail("waiting"))
    assertEquals(waiting.playerIds, Vector(p1.value, p2.value))
    assertEquals(deal(outsider).participantPlayerIds,
      Vector(p1.value, p2.value))
    assertEquals(deal(outsider).editing, None)
    val public = projector.projectPublic("negotiation", LoadedGame(state, 30))
    assertEquals(deal(public).editing, None)
    assertEquals(public.walkerDecision, None)

  test("terms show amounts to everyone but hide identities from everyone but their author"):
    val terms = NegotiationTerms(
      Vector(NegotiationTransfer(p2, 3, Vector(p1Relic))),
      Vector(NegotiationDisclosure(p2,
        NegotiationDisclosureRef.HeldRelic(p1, p1Relic))))
    val state = parkedDeal(gathered, Vector(p2), Some(terms))
    val author = deal(view(state, p1))
    assertEquals(author.transfers.head.favor, 3)
    assertEquals(author.transfers.head.relicCount, 1)
    assertEquals(author.transfers.head.relics.map(_.cardId), Vector(p1Relic.value))
    assertEquals(author.disclosures.head.card.map(_.cardId), Some(p1Relic.value))
    val others = Vector(view(state, p2), view(state, p3),
      projector.projectPublic("negotiation", LoadedGame(state, 30)))
    others.foreach { projection =>
      val seen = deal(projection)
      assertEquals(seen.transfers.head.favor, 3)
      assertEquals(seen.transfers.head.relicCount, 1)
      assertEquals(seen.transfers.head.relics, Vector.empty)
      assertEquals(seen.disclosures.head.kind, "held-relic")
      assertEquals(seen.disclosures.head.recipientPlayerId, p2.value)
      assertEquals(seen.disclosures.head.card, None)
    }

  test("acceptances and the right to accept are projected"):
    val proposed = parkedDeal(gathered, Vector(p2), Some(NegotiationTerms(
      Vector(NegotiationTransfer(p2, 1, Vector.empty)))))
    assert(deal(view(proposed, p2)).editing.exists(_.canAccept))
    val accepted = rules.resolveWalker(proposed, p2, dealId, AcceptDeal)
      .getOrElse(fail("accept")).state
    val seen = deal(view(accepted, p3))
    assertEquals(seen.acceptedPlayerIds, Vector(p2.value))
    assert(!deal(view(accepted, p2)).editing.exists(_.canAccept))
    assert(deal(view(accepted, p1)).editing.exists(_.canAccept))

  test("the Negotiation option is available while a candidate exists"):
    assert(MenuPlay.availableInMenu(catalog, gathered.ready,
      ActionRef.Negotiation))
    val alone = gathered.pawn(p2, at = DunesSite).pawn(p3, at = DunesSite)
    assert(!MenuPlay.availableInMenu(catalog, alone.ready,
      ActionRef.Negotiation))

  test("a faceup relic in a transfer is shown to every viewer, public included"):
    val faceUp = gathered.relic(p1, p1Relic).tokens(p1Relic, secrets = 1)
    val terms = NegotiationTerms(Vector(NegotiationTransfer(p2, 1,
      Vector(p1Relic))))
    val state = parkedDeal(faceUp, Vector(p2), Some(terms))
    val views = Vector(view(state, p1), view(state, p2),
      view(state, p3),
      projector.projectPublic("negotiation", LoadedGame(state, 30)))
    views.foreach(projection => assertEquals(
      deal(projection).transfers.head.relics.map(_.cardId),
      Vector(p1Relic.value)))

  test("the Scepter's holder sees whom they may offer Citizenship, and " +
      "everyone sees the offer"):
    val table = gathered.imperial(p1).relic(p1, TheGrandScepterCard)
      .allowingCitizenshipPowers
    val state = parkedDeal(table, who = Vector(p2, p3),
      terms = Some(NegotiationTerms(citizenship = Vector(p2))))
    val holder = deal(view(state, p1))
    assertEquals(holder.editing.map(_.editableCitizenship),
      Some(Vector("p2", "p3")))
    assertEquals(holder.editing.flatMap(_.citizenshipRefused), None)
    assertEquals(deal(view(state, p2)).editing.map(_.editableCitizenship),
      Some(Vector.empty))
    Vector(p1, p2, p3).foreach(viewer => assertEquals(
      deal(view(state, viewer)).citizenship,
      Vector(NegotiationCitizenshipProjection("p1", "p2")), viewer.value))

  test("every viewer reads every promise; the binding box stays closed"):
    val terms = NegotiationTerms(promises = Vector("I will not attack you."))
    val state = parkedDeal(gathered, Vector(p2), Some(terms))
    Vector(p1, p2, p3).foreach { viewer =>
      assertEquals(deal(view(state, viewer)).promises, Vector(
        NegotiationPromiseProjection(p1.value, "I will not attack you.",
          binding = false)), viewer.value)
    }
    val public = projector.projectPublic("negotiation", LoadedGame(state, 30))
    assertEquals(deal(public).promises.map(_.text),
      Vector("I will not attack you."))
    val editing = deal(view(state, p2)).editing.getOrElse(fail("editing"))
    assert(!editing.bindingPromisesOpen)

  test("with The Tribunal in reach, every author's editing opens binding " +
      "promises"):
    val state = parkedDeal(gathered.edifice(TheTribunalCard,
      EdificeSide.Intact, at = site), who = Vector(p2))
    Vector(p1, p2).foreach { viewer =>
      val editing = deal(view(state, viewer)).editing
        .getOrElse(fail(s"${viewer.value} edits"))
      assert(editing.bindingPromisesOpen, viewer.value)
    }

  test("a facedown adviser on offer is named to its author and counted " +
      "for everyone else"):
    val birdsong: WorldCardId = BirdsongCard.id
    val table = gathered.adviser(p1, CouncilArbiterCard.id)
      .adviser(p1, BirdsongCard.id, facedown = true)
    val state = parkedDeal(table, who = Vector(p2, p3), terms = Some(
      NegotiationTerms(Vector(NegotiationTransfer(p2, 0, Vector.empty,
        Vector(birdsong))))))
    val own = deal(view(state, p1)).transfers.head
    assertEquals((own.adviserCount, own.advisers.map(_.cardId)),
      (1, Vector(birdsong.value)))
    Vector(p2, p3).foreach { viewer =>
      val seen = deal(view(state, viewer)).transfers.head
      assertEquals((seen.adviserCount, seen.advisers), (1, Vector.empty),
        viewer.value)
    }

  test("the editor offers each author's movable advisers to the recipients " +
      "the rules allow"):
    val table = gathered.adviser(p1, CouncilArbiterCard.id)
    val state = parkedDeal(table, who = Vector(p2, p3))
    val p1Editing = deal(view(state, p1)).editing.getOrElse(fail("editing"))
    assertEquals(p1Editing.giftableAdvisers.map(_.cardId),
      Vector(CouncilArbiterCard.id.value))
    assertEquals(p1Editing.adviserRecipients, Vector(p2.value, p3.value))
    val p2Editing = deal(view(state, p2)).editing.getOrElse(fail("editing"))
    assertEquals(p2Editing.adviserRecipients, Vector(p1.value))

  test("terms over an adviser limit are projected with who they leave over"):
    // p2 holds Silver Tongue faceup (a limit of 2) and Birdsong; p1 offers
    // them Council Arbiter.
    val table = gathered.adviser(p1, CouncilArbiterCard.id)
      .adviser(p2, SilverTongueCard.id)
      .adviser(p2, BirdsongCard.id, facedown = true)
    val state = parkedDeal(table, who = Vector(p2), terms = Some(
      NegotiationTerms(Vector(NegotiationTransfer(p2, 0, Vector.empty,
        Vector(CouncilArbiterCard.id))))))
    Vector(p1, p2).foreach { viewer =>
      val editing = deal(view(state, viewer)).editing.getOrElse(fail("editing"))
      assertEquals(editing.overAdviserLimit, Vector(p2.value), viewer.value)
      assert(!editing.canAccept, viewer.value)
    }

  private val dunesOffer = NegotiationSiteProjection(DunesSite.id.value,
    "Dunes")

  test("every viewer sees a site on offer by name"):
    val table = gathered.adviser(p1, DeedWriterCard.id)
      .warbandsAt(DunesSite, p1, 2)
    val state = parkedDeal(table, who = Vector(p2, p3), terms = Some(
      NegotiationTerms(Vector(NegotiationTransfer(p2, 0, Vector.empty,
        Vector.empty, Vector(DunesSite.id))))))
    Vector(p1, p2, p3).foreach(viewer => assertEquals(
      deal(view(state, viewer)).transfers.head.sites, Vector(dunesOffer),
      viewer.value))

  test("the editor offers each author's sites to the recipients the " +
      "rules allow"):
    val table = gathered.adviser(p1, DeedWriterCard.id)
      .warbandsAt(DunesSite, p1, 2)
    val state = parkedDeal(table, who = Vector(p2, p3))
    val p1Editing = deal(view(state, p1)).editing.getOrElse(fail("editing"))
    assertEquals(p1Editing.giftableSites, Vector(dunesOffer))
    assertEquals(p1Editing.siteRecipients, Vector(p2.value, p3.value))
    val p2Editing = deal(view(state, p2)).editing.getOrElse(fail("editing"))
    assertEquals(p2Editing.siteRecipients, Vector(p1.value))

  test("terms that leave a recipient short of warbands are projected " +
      "with who"):
    val table = gathered.adviser(p1, DeedWriterCard.id)
      .warbandsAt(DunesSite, p1, 2).boardWarbands(p2, 0)
    val state = parkedDeal(table, who = Vector(p2), terms = Some(
      NegotiationTerms(Vector(NegotiationTransfer(p2, 0, Vector.empty,
        Vector.empty, Vector(DunesSite.id))))))
    Vector(p1, p2).foreach { viewer =>
      val editing = deal(view(state, viewer)).editing.getOrElse(fail("editing"))
      assertEquals(editing.shortOfWarbands, Vector(p2.value), viewer.value)
      assert(!editing.canAccept, viewer.value)
    }
