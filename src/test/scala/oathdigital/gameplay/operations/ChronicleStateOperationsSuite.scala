package oathdigital.gameplay.operations

import oathdigital.gameplay.setup.FirstGameSetupFixture
import oathdigital.model._
import oathdigital.testkit.{CatalogNames, Table}
import oathdigital.testkit.Table.{p1, p2}
import oathdigital.testkit.Unwrap.{accepted, rejected}

/** The Chronicle's own primitives (Chronicle Sun and Throne plan, Task 3). */
class ChronicleStateOperationsSuite extends munit.FunSuite:
  private val executor = new OperationExecutor()
  private def run(ready: ReadyGame, ops: CoreOperation*) =
    executor.executeAll(ready, ops.toVector)

  private def lineageOf(ready: ReadyGame, player: PlayerId): LineageId =
    ready.game.current.players.find(_.player == player).get.lineage

  private val boxed = LineageId("lineage:boxed")

  /** The quiet table whose baseline Chronicle also lists `boxed`, a lineage
    * nobody seated, as a Citizen. */
  private def tableWithBoxedCitizen: ReadyGame =
    Table.start.update(_.updateCampaign(campaign => campaign.copy(
      baseline = campaign.baseline.copy(lineages = Vector(LineageState(boxed,
        None, Role.Citizen, Vector.empty, Vector.empty)))))).ready

  test("a role change updates a seated lineage"):
    val ready = Table.start.ready
    val lineage = lineageOf(ready, p1)
    val after = accepted(run(ready, RoleChanged(lineage, Role.Chancellor)))
    assertEquals(after.game.campaign.lineages(lineage).role, Role.Chancellor)
    assertEquals(after.game.campaign.boxRoles, Map.empty)

  test("a role change to a lineage in the box is kept apart"):
    val after = accepted(run(tableWithBoxedCitizen,
      RoleChanged(boxed, Role.Exile)))
    assertEquals(after.game.campaign.boxRoles, Map(boxed -> Role.Exile))
    assert(!after.game.campaign.lineages.contains(boxed))

  test("a role change to an unknown lineage is refused"):
    rejected(run(Table.start.ready,
      RoleChanged(LineageId("lineage:nobody"), Role.Exile)))

  test("a task goes to a seated player only"):
    val ready = Table.start.ready
    val after = accepted(run(ready,
      ChronicleTaskAssigned(ChronicleTask.World, p2)))
    assertEquals(after.game.current.chronicleTasks,
      Map(ChronicleTask.World -> p2))
    rejected(run(ready,
      ChronicleTaskAssigned(ChronicleTask.World, PlayerId("nobody"))))

  test("the Grand Scepter comes from the box faceup, once"):
    val after = accepted(run(Table.start.ready, GrandScepterFromBox(p1)))
    assertEquals(after.game.current.players.find(_.player == p1).get.relics
      .last, RelicState(TheGrandScepterCard.id, Orientation.FaceUp,
        Tokens.empty))
    rejected(run(after, GrandScepterFromBox(p2)))

  test("the pipeline's card inventory expects the Scepter it brought from " +
      "the box"):
    val run = accepted(OperationPipeline.run(Table.start.ready,
      Vector(GrandScepterFromBox(p1)), OperationPolicy.Permissive,
      Vector.empty)(Right(_)))
    assert(run.state.game.cardIndex.toOption.exists(
      _.locationOf(TheGrandScepterCard.id).nonEmpty))

  test("only a seated lineage scores lineage points"):
    rejected(run(Table.start.ready,
      LineagePointsScored(LineageId("lineage:nobody"), 1)))

  test("the Imperial supply is taken once and kept after"):
    val after = accepted(run(Table.start.ready, ImperialWarbandsTaken(8)))
    assertEquals(after.banks.warbandSupply.get(ForceKind.Imperial), Some(8))
    val again = accepted(run(after, ImperialWarbandsTaken(3)))
    assertEquals(again.banks.warbandSupply.get(ForceKind.Imperial), Some(8))

  test("purple warbands added raise an existing Imperial supply, and none " +
      "exists to raise before it is taken (Chronicle ruling 22)"):
    val taken = accepted(run(Table.start.ready, ImperialWarbandsTaken(24)))
    val raised = accepted(run(taken, ImperialWarbandsAdded(3)))
    assertEquals(raised.banks.warbandSupply.get(ForceKind.Imperial), Some(27))
    assert(run(Table.start.ready, ImperialWarbandsAdded(3)).isLeft)

  test("the game's first lineage points open its scorepad row for every " +
      "seated lineage, with its role, and later points add to it"):
    val ready = Table.start.ready
    val lineage = lineageOf(ready, p1)
    val after = accepted(run(ready, LineagePointsScored(lineage, 3),
      LineagePointsScored(lineage, 1)))
    val era = after.game.campaign.era
    assertEquals(era.games.size, ready.game.campaign.era.games.size + 1)
    assertEquals(era.games.last.entries.map(_.lineage),
      ready.game.current.players.map(_.lineage))
    assertEquals(era.score(lineage), 4)
    assertEquals(era.games.last.entries.find(_.lineage == lineage)
      .map(_.endingRole), Some(ready.game.campaign.lineages(lineage).role))

  test("scoring no points and an empty Imperial supply cannot be built"):
    intercept[IllegalArgumentException](
      LineagePointsScored(LineageId("lineage:any"), 0))
    intercept[IllegalArgumentException](ImperialWarbandsTaken(0))

  test("a built edifice comes from the deck intact"):
    val ready = Table.start.ready
    val edifice = ready.game.current.commonCards.edificeDeck.head
    val site = CatalogNames.site("Ancient City")
    val after = accepted(run(ready, EdificeBuilt(edifice, site)))
    assert(!after.game.current.commonCards.edificeDeck.contains(edifice))
    assertEquals(after.game.current.map.sites(site).denizens.last,
      EdificeState(edifice, EdificeSide.Intact, Tokens.empty))

  test("a built edifice may come from the set-aside list"):
    val start = Table.start.ready
    val edifice = start.game.current.commonCards.edificeDeck.head
    val ready = start.updateCurrent(current => current.copy(
      commonCards = current.commonCards.copy(
        edificeDeck = current.commonCards.edificeDeck.tail),
      setAsideEdifices = Vector(edifice)))
    val after = accepted(run(ready,
      EdificeBuilt(edifice, CatalogNames.site("Ancient City"))))
    assertEquals(after.game.current.setAsideEdifices, Vector.empty)

  test("an edifice already at a site cannot be built again"):
    val ready = Table.start
      .edifice("Bandit Market", EdificeSide.Ruined, at = "Ancient City").ready
    rejected(run(ready, EdificeBuilt(CatalogNames.edifice("Bandit Market"),
      CatalogNames.site("Broken Peaks"))))

  /** Denizens of `suit` the quiet table does not have in play. */
  private def boxedOf(ready: ReadyGame, suit: Suit): Vector[DenizenId] =
    val inPlay = ready.game.cardIndex.toOption.get.ids
    FirstGameSetupFixture.catalog.denizens.filter(_.suit == suit)
      .map(denizen => DenizenId(denizen.id.value))
      .filterNot(inPlay.contains)

  test("boxed denizens of a suit join its live reserve"):
    val ready = Table.start.ready
    val boxed = boxedOf(ready, Suit.Beast).take(2)
    assertEquals(boxed.size, 2)
    val after = accepted(run(ready, SuitedReserveFromBox(Suit.Beast, boxed)))
    assertEquals(after.game.campaign.suitedReserves.get(Suit.Beast),
      Some(boxed))

  test("a card in play cannot come from the box"):
    val ready = Table.start.ready
    val inPlay = ready.game.current.commonCards.worldDeck
      .collectFirst { case id: DenizenId => id }.get
    rejected(run(ready, SuitedReserveFromBox(CatalogNames.suitOf(inPlay),
      Vector(inPlay))))

  test("the pipeline's card inventory expects the denizens a reserve " +
      "brought from the box"):
    val ready = Table.start.ready
    val boxed = boxedOf(ready, Suit.Beast).take(2)
    val run = accepted(OperationPipeline.run(ready,
      Vector(SuitedReserveFromBox(Suit.Beast, boxed)),
      OperationPolicy.Permissive, Vector.empty)(Right(_)))
    assert(boxed.forall(card => run.state.game.cardIndex.toOption.exists(
      _.locationOf(card).nonEmpty)))

  test("a box card named twice is refused"):
    val ready = Table.start.ready
    val card = boxedOf(ready, Suit.Beast).head
    rejected(run(ready, SuitedReserveFromBox(Suit.Beast, Vector(card, card))))
