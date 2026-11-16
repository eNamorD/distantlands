package distantlands.gameplay.setup.legacies

import distantlands.gameplay.actions.ProcedureNotes
import distantlands.gameplay.chronicle.legacies.{IronHandCard, RivalCard}
import distantlands.gameplay.powers.{NoteText, PlayerFacts}
import distantlands.application.GameCommand
import distantlands.gameplay.setup.{GameStartRules, ImperialMaps,
  LaterGameFixture, SetupLegacyFixture, SetupPawnSites,
  SetupProcedure}
import distantlands.gameplay.setup.FirstGameSetupFixture.catalog
import distantlands.model._
import distantlands.testkit.{ParkedNode, Situation}
import distantlands.testkit.Unwrap.{accepted, present}

/** Rival at setup: "If you are an Exile: Setup: Before resolving
  * foundations, fill the three Cradlemost empty site slots using the Empire
  * setup rules, but place one warband of yours on each. You may place your
  * pawn on these sites." (spec ruling 8; R2, R5, R6). */
class RivalSetupSuite extends munit.FunSuite:
  private val l1 = LineageId("l1")
  private val l2 = LineageId("l2")
  private val l3 = LineageId("l3")
  private val p1 = PlayerId("p1")

  private val record = SetupLegacyFixture.holding(
    LaterGameFixture.chronicle(l1, l2), l3, Role.Exile, RivalCard.id -> true)
  private val rivalSites = record.atlasBox.take(3)

  private def started(chronicle: Chronicle): ReadyGame =
    GameStartRules.evolve(catalog, chronicle,
      LaterGameFixture.orders(chronicle, p1)).fold(
      problem => fail(problem.toString), identity)

  test("the next three Recent Sites follow the Empire's, each with one of " +
      "the holder's warbands and no relic dealt (R5)"):
    val ready = started(record)
    val map = ready.game.current.map
    assertEquals(map.inPlay, (record.world ++ rivalSites).map(_.site))
    rivalSites.foreach { stored =>
      val site = map.sites(stored.site)
      assertEquals(site.forces, SiteForces.Occupied(ForceKind.Exile(l3), 1))
      assertEquals(site.relics.map(_.id),
        stored.items.collect { case id: RelicId => id })
      assert(site.denizens.forall {
        case edifice: EdificeState => edifice.side == EdificeSide.Intact
        case _ => true
      }, site.denizens)
    }
    assert(!ready.game.campaign.atlas.sites.exists(entry =>
      rivalSites.exists(_.site == entry.id)))

  test("Imperial Maps fills only the slots still empty after them (R6)"):
    val ready = started(record)
    assertEquals(ImperialMaps.emptySlotsAtStart(ready), 3)
    val woken = Situation.wake(Situation.rules(catalog), record,
      LaterGameFixture.orders(record, p1)).ready
    assertEquals(woken.game.current.map.inPlay.size, MapState.slots)
    assertEquals(woken.game.current.map.inPlay.take(5),
      (record.world ++ rivalSites).map(_.site))

  test("Imperial Maps asks nothing when Rival leaves one Atlas site (R6): " +
      "the first question is the keep"):
    val short = SetupLegacyFixture.holding(
      LaterGameFixture.chronicle(l1, Vector(l2), 4), l3, Role.Exile,
      RivalCard.id -> true)
    assertEquals(short.atlasBox.size, 4)
    val begun = Situation.start(Situation.rules(catalog)).parkedAfter(
      GameCommand.Begin(short, LaterGameFixture.orders(short, p1)))
    present(accepted(ParkedNode.of(begun.state, catalog)), "a park") match
      case ParkedNode.Decision(_, decide, _) => assertEquals(
        decide.decisionId, SetupProcedure.legacyDecisionId(p1))
      case other => fail(s"expected a decision, parked on $other")
    assertEquals(begun.ready.game.current.map.inPlay,
      (short.world ++ short.atlasBox).map(_.site))

  test("their denizens add favor to the banks (ruling 8)"):
    val without = started(SetupLegacyFixture.holding(
      LaterGameFixture.chronicle(l1, l2), l3, Role.Exile,
      RivalCard.id -> false))
    val ready = started(record)
    val cards = rivalSites.flatMap(stored =>
      ready.game.current.map.sites(stored.site).denizens)
      .flatMap(card => catalog.suitOf(card.id))
    assert(cards.nonEmpty, "the fixture's Rival sites store no card")
    assertEquals(ready.banks.favor.values.sum,
      without.banks.favor.values.sum + cards.size)

  test("the holder may place their pawn at their own sites (R5)"):
    val ready = started(record)
    val legal = SetupPawnSites.legal(catalog, ready, PlayerId("p3")).toSet
    assert(rivalSites.map(_.site).toSet.subsetOf(legal), legal)
    // Another Exile may not.
    val other = SetupPawnSites.legal(catalog,
      ready, PlayerId("p2")).toSet
    assert(rivalSites.map(_.site).toSet.intersect(other).isEmpty, other)

  test("one Exile's warbands reach Iron Hand's sites before Rival's (R2)"):
    // Circle of Swords' 8 on the board leave 6 of the 14: the 5 Empire
    // sites, then the first of Rival's 3; the other 2 take bandits.
    val all = SetupLegacyFixture.holding(
      LaterGameFixture.chronicle(l1, Vector(l2), 5), l3, Role.Exile,
      CircleOfSwordsCard.id -> true, IronHandCard.id -> true,
      RivalCard.id -> true)
    val ready = started(all)
    val held = SiteForces.Occupied(ForceKind.Exile(l3), 1)
    val rivals = all.atlasBox.take(3)
    def forces(stored: StoredSite) =
      ready.game.current.map.sites(stored.site).forces
    assertEquals(all.world.map(forces).distinct, Vector(held))
    assertEquals(forces(rivals.head), held)
    assertEquals(rivals.tail.map(forces), rivals.tail.map(stored =>
      val capacity = catalog.site(stored.site).get.capacity
      if capacity == 0 then SiteForces.Empty
      else SiteForces.Occupied(ForceKind.Bandit, capacity)))
    assertEquals(PlayerFacts.banked(ready, ForceKind.Exile(l3)), 0)
    val said = NoteText.said(ProcedureNotes.id(TriggeredProcedureRef.Setup),
      SetupProcedure.noteKeys, Situation.wake(Situation.rules(catalog), all,
        LaterGameFixture.orders(all, p1)).events).map(_.text)
    assertEquals(said.count(_.endsWith(s"with ${IronHandCard.id.value}")), 5)
    assertEquals(said.filter(_.endsWith(s"with ${RivalCard.id.value}")),
      Vector(s"p3 placed a warband at ${rivals.head.site.value} with " +
        RivalCard.id.value))

  test("a first game played exile-only wipes a held Rival, which does " +
      "nothing (R40)"):
    val exileOnly = SetupLegacyFixture.holding(
      distantlands.gameplay.setup.FirstGameSetupFixture.chronicle, l3,
      Role.Exile, RivalCard.id -> true)
    val ready = started(exileOnly)
    assertEquals(ready.game.current.map.inPlay,
      exileOnly.atlasBox.take(MapState.slots).map(_.site))
    assertEquals(ready.game.current.map.sites.values.count(
      _.forces.count(ForceKind.Exile(l3)) > 0), 0)
    assert(!ready.game.campaign.lineages(l3).legacies.exists(
      _.id == RivalCard.id))

  test("an Imperial holder places nothing (ruling 8)"):
    val ready = started(SetupLegacyFixture.holding(
      LaterGameFixture.chronicle(l1, l2), l2, Role.Citizen,
      RivalCard.id -> true))
    assertEquals(ready.game.current.map.inPlay, record.world.map(_.site))
