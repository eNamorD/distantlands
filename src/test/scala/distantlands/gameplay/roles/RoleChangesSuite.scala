package oathdigital.gameplay.roles

import oathdigital.gameplay.actions.VisionRules
import oathdigital.gameplay.cards.{AncientCitySite, BrokenPeaksSite}
import oathdigital.gameplay.operations.TheGrandScepterCard
import oathdigital.gameplay.powers.PlayerFacts
import oathdigital.model._
import oathdigital.testkit.{Look, Table}
import oathdigital.testkit.Table.{p1, p2, p3}
import oathdigital.testkit.Unwrap.{accepted, present}
import oathdigital.testkit.RawOperationBatch

/** Becoming a Citizen or the Chancellor, and exile (CR p. 34; Empire phase
  * design, "Slice 3"). */
class RoleChangesSuite extends munit.FunSuite:
  private val peaks = BrokenPeaksSite.id

  private def applied(ready: ReadyGame,
      ops: Either[OathViolation, Vector[CoreOperation]])(
      using munit.Location): ReadyGame =
    accepted(RawOperationBatch.executeAll(ready, present(ops.toOption, "the ops")))

  private def everyPlaceKept(ready: ReadyGame, player: PlayerId)(
      using munit.Location): RoleChanges.Kept =
    present(RoleChanges.keepAll(ready, player).toOption, "the kept counts")

  /** p1 is the Chancellor and p3 an Exile in their Act, with 2 warbands
    * on their board and 3 at Broken Peaks, a revealed Vision and Supply 2. */
  private def exileInAct: Table = Table.start.imperial(p1)
    .turn(p3, Phase.Act).pawn(p3, at = BrokenPeaksSite)
    .boardWarbands(p3, 2).warbandsAt(BrokenPeaksSite, p3, 3)
    .revealedVision(p3, VisionRules.Conquest).supply(p3, 2)

  test("becoming a Citizen turns every warband purple and ends the Act"):
    val ready = exileInAct.ready
    val after = applied(ready, RoleChanges.becomeImperial(ready, p3,
      everyPlaceKept(ready, p3)))
    val look = Look(after)
    assertEquals(Imperials.role(after, p3), Some(Role.Citizen))
    assertEquals(look.player(p3).board.warbands, 2)
    assertEquals(look.forces(peaks),
      SiteForces.Occupied(ForceKind.Imperial, 3))
    assertEquals(look.player(p3).revealedVisions, Vector.empty)
    assertEquals(look.player(p3).board.supply.supply, 7)
    assertEquals(after.game.current.turn.phase, Phase.Rest)

  test("becoming a Citizen discards every revealed Vision"):
    val ready = exileInAct.revealedVision(p3, VisionRules.Sanctuary).ready
    val after = applied(ready, RoleChanges.becomeImperial(ready, p3,
      everyPlaceKept(ready, p3)))
    assertEquals(Look(after).player(p3).revealedVisions, Vector.empty)
    val discards = after.game.current.commonCards.regionalDiscards.values.flatten.toSet
    assert(Set[WorldCardId](VisionRules.Conquest, VisionRules.Sanctuary)
      .subsetOf(discards), discards)

  test("off their turn the Act phase is left alone"):
    val ready = exileInAct.turn(p2, Phase.Act).ready
    val after = applied(ready, RoleChanges.becomeImperial(ready, p3,
      everyPlaceKept(ready, p3)))
    assertEquals(after.game.current.turn, ready.game.current.turn)

  test("a short purple bank keeps only the counts chosen"):
    // The bank holds 2: p1's board takes the other 22.
    val ready = exileInAct.boardWarbands(p1, 22).ready
    assertEquals(RoleChanges.purpleAvailable(ready), 2)
    val kept = RoleChanges.Kept(board = 0, sites = Map(peaks -> 2))
    val own = ForceKind.Exile(Look(ready).player(p3).lineage)
    val after = applied(ready, RoleChanges.becomeImperial(ready, p3, kept))
    val look = Look(after)
    assertEquals(look.player(p3).board.warbands, 0)
    assertEquals(look.forces(peaks),
      SiteForces.Occupied(ForceKind.Imperial, 2))
    // All 5 of p3's own warbands go back to their bank: the 2 the site
    // keeps are replaced by purple ones.
    assertEquals(PlayerFacts.banked(after, own),
      PlayerFacts.banked(ready, own) + 5)

  test("kept counts beyond the bank are refused"):
    val ready = exileInAct.boardWarbands(p1, 22).ready
    assert(RoleChanges.becomeImperial(ready, p3,
      everyPlaceKept(ready, p3)).isLeft)

  test("kept counts must name places the player holds, within each"):
    val ready = exileInAct.ready
    def refused(kept: RoleChanges.Kept) =
      RoleChanges.becomeImperial(ready, p3, kept).isLeft
    assert(refused(RoleChanges.Kept(2, Map(peaks -> 3,
      AncientCitySite.id -> 0))))
    assert(refused(RoleChanges.Kept(-1, Map(peaks -> 3))))
    assert(refused(RoleChanges.Kept(2, Map(peaks -> 4))))
    // The bank has room for all 5, so all 5 must turn.
    assert(refused(RoleChanges.Kept(1, Map(peaks -> 3))))

  test("a Usurper title turns to its Oathkeeper side"):
    val ready = exileInAct.oathkeeperGoal(OathkeeperGoal.ThePeople)
      .oathkeeper(Some(p3), TitleSide.Usurper).ready
    val after = applied(ready, RoleChanges.becomeImperial(ready, p3,
      everyPlaceKept(ready, p3)))
    assertEquals(after.game.current.title,
      OathkeeperState(Some(p3), TitleSide.Oathkeeper))

  test("under Supremacy the title passes to the Chancellor (R19)"):
    val ready = exileInAct.oathkeeperGoal(OathkeeperGoal.Supremacy)
      .oathkeeper(Some(p3)).ready
    val after = applied(ready, RoleChanges.becomeImperial(ready, p3,
      everyPlaceKept(ready, p3)))
    assertEquals(after.game.current.title.holder, Some(p1))

  test("under Supremacy a Usurper title passes too, on its Oathkeeper side"):
    val ready = exileInAct.oathkeeperGoal(OathkeeperGoal.Supremacy)
      .oathkeeper(Some(p3), TitleSide.Usurper).ready
    val after = applied(ready, RoleChanges.becomeImperial(ready, p3,
      everyPlaceKept(ready, p3)))
    assertEquals(after.game.current.title,
      OathkeeperState(Some(p1), TitleSide.Oathkeeper))

  /** No Empire: p3, an Exile in their Act, with 2 warbands on their board.
    * p2 holds the Grand Scepter. */
  private def noEmpire: Table = Table.start.turn(p3, Phase.Act)
    .pawn(p3, at = BrokenPeaksSite).boardWarbands(p3, 2)
    .relic(p2, TheGrandScepterCard)

  test("with no Empire the player becomes the Chancellor"):
    val ready = noEmpire.ready
    assertEquals(RoleChanges.newRole(ready), Role.Chancellor)
    val after = applied(ready, RoleChanges.becomeImperial(ready, p3,
      everyPlaceKept(ready, p3)))
    assertEquals(Imperials.chancellor(after), Some(p3))
    assertEquals(after.banks.warbandSupply.get(ForceKind.Imperial),
      Some(MaterialBankState.ImperialWarbands))
    assertEquals(Look(after).player(p3).board.warbands, 2)
    assert(Look(after).player(p3).relics.exists(
      _.id == TheGrandScepterCard.id))
    assert(!after.game.current.tracks.usurperLimited)

  test("a new Chancellor keeps a Usurper title, turned to Oathkeeper"):
    Vector(OathkeeperGoal.ThePeople, OathkeeperGoal.Supremacy).foreach { goal =>
      val ready = noEmpire.oathkeeperGoal(goal)
        .oathkeeper(Some(p3), TitleSide.Usurper).ready
      val after = applied(ready, RoleChanges.becomeImperial(ready, p3,
        everyPlaceKept(ready, p3)))
      assertEquals(after.game.current.title,
        OathkeeperState(Some(p3), TitleSide.Oathkeeper), clue(goal))
    }

  test("a new Chancellor takes the Scepter from the site it lies at"):
    val ready = Table.start.turn(p3, Phase.Act)
      .pawn(p3, at = BrokenPeaksSite).relicAt(TheGrandScepterCard, peaks, facedown = false).ready
    val after = applied(ready, RoleChanges.becomeImperial(ready, p3,
      everyPlaceKept(ready, p3)))
    assert(Look(after).player(p3).relics.exists(
      _.id == TheGrandScepterCard.id))

  test("a Usurper Limiter already off is left alone"):
    val ready = noEmpire.update(_.updateCurrent(current => current.copy(
      tracks = current.tracks.copy(usurperLimited = false)))).ready
    val ops = present(RoleChanges.becomeImperial(ready, p3,
      everyPlaceKept(ready, p3)).toOption, "the ops")
    assert(!ops.contains(UsurperLimiterRemoved), ops)
    applied(ready, Right(ops))

  test("with no one holding the Scepter it comes from the box"):
    val ready = Table.start.turn(p3, Phase.Act)
      .pawn(p3, at = BrokenPeaksSite).ready
    val after = applied(ready, RoleChanges.becomeImperial(ready, p3,
      everyPlaceKept(ready, p3)))
    assert(Look(after).player(p3).relics.exists(
      _.id == TheGrandScepterCard.id))

  test("only an Exile becomes Imperial"):
    val ready = Table.start.imperial(p1, p3).ready
    assert(RoleChanges.becomeImperial(ready, p3,
      RoleChanges.Kept(0, Map.empty)).isLeft)

  /** p1 is the Chancellor and p3 a Citizen in their Act, with 4 purple
    * warbands on their board and 2 at Broken Peaks, and Supply 1. */
  private def citizenInAct: Table = Table.start.imperial(p1, p3)
    .turn(p3, Phase.Act).pawn(p3, at = BrokenPeaksSite)
    .boardWarbands(p3, 4).warbandsAt(BrokenPeaksSite, p3, 2).supply(p3, 1)

  test("exile swaps the board's purple warbands and leaves the map's"):
    val ready = citizenInAct.ready
    val after = applied(ready, RoleChanges.exile(ready, p3))
    val look = Look(after)
    assertEquals(Imperials.role(after, p3), Some(Role.Exile))
    assertEquals(look.player(p3).board.warbands, 4)
    assertEquals(look.forces(peaks),
      SiteForces.Occupied(ForceKind.Imperial, 2))
    assertEquals(look.player(p3).board.supply.supply, 7)
    assertEquals(after.game.current.turn.phase, Phase.Rest)

  test("exile off their turn leaves the Act phase alone"):
    val ready = citizenInAct.turn(p2, Phase.Act).ready
    val after = applied(ready, RoleChanges.exile(ready, p3))
    assertEquals(after.game.current.turn, ready.game.current.turn)

  test("exile takes no more of their own warbands than their bank holds"):
    // 12 of p3's 14 own warbands stand at Broken Peaks, Exile-coloured.
    val own = ForceKind.Exile(Look(citizenInAct.ready).player(p3).lineage)
    val ready = citizenInAct.update(_.updateCurrent(current => current.copy(
      map = current.map.copy(sites = current.map.sites.updatedWith(peaks)(
        _.map(_.copy(forces = SiteForces.Occupied(own, 12)))))))).ready
    val after = applied(ready, RoleChanges.exile(ready, p3))
    assertEquals(Look(after).player(p3).board.warbands, 2)

  test("only a Citizen is exiled"):
    assert(RoleChanges.exile(citizenInAct.ready, p1).isLeft)
    assert(RoleChanges.exile(exileInAct.ready, p3).isLeft)
