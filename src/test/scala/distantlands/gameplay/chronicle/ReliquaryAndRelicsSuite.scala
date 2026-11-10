package distantlands.gameplay.chronicle

import distantlands.gameplay.cards.AncientCitySite
import distantlands.gameplay.{OathRules, WalkerDiceFixture}
import distantlands.gameplay.operations.TheGrandScepterCard
import distantlands.gameplay.powers.WalkerPowerCatalog
import distantlands.gameplay.powers.PlayerFacts
import distantlands.gameplay.powers.campaign.ObsidianCageCard
import distantlands.gameplay.setup.FirstGameSetupFixture.catalog
import distantlands.model._
import distantlands.testkit.{Outcomes, Table}
import distantlands.testkit.Table.{p1, p2}
import distantlands.testkit.Unwrap.accepted

/** Stock Reliquary and Scatter Relics (CR p. 37). p1 is the Chancellor
  * holding the Throne and the Empire rules Ancient City. */
class ReliquaryAndRelicsSuite extends munit.FunSuite:
  private val rules = new OathRules(catalog,
    walkerPowerCatalog = WalkerPowerCatalog.default(catalog),
    walkerDice = WalkerDiceFixture.reversingShuffle)

  private val empire: Table =
    ChronicleWalk.throneHeldByChancellorP1.warbandsAt(AncientCitySite, p1, 1)

  /** The relics in the quiet table's relic deck, in deck order. */
  private val deckRelics: Vector[RelicId] =
    empire.ready.game.current.commonCards.relicDeck

  private def throneStarted(table: Table)(using munit.Location)
      : OathTransition =
    accepted(rules.next(OathTransition(table.state, Vector.empty),
      TriggeredProcedureRef.ChronicleSun))

  private def done(transition: OathTransition)(using munit.Location)
      : ReadyGame =
    Outcomes.ready(ChronicleWalk.answeringEverything(rules, transition).state)

  private def holding(table: Table, player: PlayerId,
      relics: Vector[RelicId]): Table =
    relics.foldLeft(table)((next, relic) => next.relic(player, relic))

  private def relicsOf(ready: ReadyGame, player: PlayerId): Vector[RelicId] =
    ready.game.current.player(player).get.relics.map(_.id)

  /** `table` with `relics` taken from the relic deck into the Reliquary. */
  private def inReliquary(table: Table, relics: Vector[RelicId]): Table =
    table.update(_.updateCurrent(current => current.copy(commonCards =
      current.commonCards.copy(relicDeck =
        current.commonCards.relicDeck.filterNot(relics.contains)))))
      .update(_.updateCampaign(_.copy(reliquary = relics)))

  test("the Throne holder's relics join the Reliquary; the Grand Scepter " +
      "stays with them"):
    val two = deckRelics.take(2)
    val after = done(throneStarted(holding(
      empire.relic(p1, TheGrandScepterCard.id), p1, two)))
    assertEquals(after.game.campaign.reliquary, two)
    assertEquals(relicsOf(after, p1), Vector(TheGrandScepterCard.id))

  test("over five, the holder discards relics of their choice down to five"):
    val stored = deckRelics.take(4)
    val held = deckRelics.slice(4, 7)
    val started = throneStarted(holding(inReliquary(empire, stored), p1, held))
    val walked = ChronicleWalk.walkedTo(rules, started,
      ReliquaryAndRelics.discardDecisionId)
    val parked = ChronicleWalk.parked(walked.state).get
    assertEquals(parked.owner, p1)
    parked.query match
      case many: DecisionQuery.ChooseMany =>
        assertEquals((many.min, many.max, many.options.size), (2, 2, 7))
      case other => fail(other.toString)
    val discarded = Vector(stored.head, held.last)
    val answered = accepted(rules.resolveWalker(walked.state, p1,
      ReliquaryAndRelics.discardDecisionId, DecisionAnswer.ChooseManyAnswer(
        discarded.map(DecisionOptionRef.Relic(_)))))
    val after = Outcomes.ready(answered.state)
    assertEquals(after.game.campaign.reliquary,
      (stored ++ held).filterNot(discarded.contains))

  test("five or fewer in the Reliquary asks nothing"):
    val started = throneStarted(holding(inReliquary(empire,
      deckRelics.take(3)), p1, deckRelics.slice(3, 5)))
    val after = done(started)
    assertEquals(after.game.campaign.reliquary.size, 5)

  test("Scatter deals held and discarded relics facedown to empty slots " +
      "Hinterward and returns the rest to the relic deck"):
    val after = done(throneStarted(holding(empire, p2, deckRelics)))
    val map = after.game.current.map
    val slots = map.inPlay.map(site => catalog.site(site).get.relicSlots).sum
    val onSites = map.inPlay.flatMap(map.sites(_).relics)
    assertEquals(onSites.size, slots)
    assert(onSites.forall(_.orientation == Orientation.FaceDown), onSites)
    assertEquals(relicsOf(after, p2), Vector.empty)
    assertEquals(after.game.current.setAsideRelics, Vector.empty)
    assertEquals(after.game.current.commonCards.relicDeck.size,
      deckRelics.size - slots)

  test("Scatter fills the first empty slot Hinterward first"):
    val relic = deckRelics.head
    val after = done(throneStarted(empire.relic(p2, relic)))
    val map = after.game.current.map
    val first = map.inPlay.find(site => catalog.site(site).get.relicSlots > 0)
      .get
    assertEquals(map.sites(first).relics.map(_.id), Vector(relic))

  test("Scatter leaves the Grand Scepter with its holder"):
    val after = done(throneStarted(empire.relic(p1, TheGrandScepterCard.id)))
    assertEquals(relicsOf(after, p1), Vector(TheGrandScepterCard.id))

  test("warbands on Obsidian Cage go back to their supply, not to a board, " +
      "before the relics leave their holders"):
    // The game has ended, so the Cage's warbands simply return (owner's
    // ruling, 2026-11-03).
    val cage = ObsidianCageCard.id
    val table = empire.relic(p2, cage).boardWarbands(p2, 1)
      .warbandsOnCard(cage, p2, 2).warbandsOnCard(cage, p1, 1)
    val board = (ready: ReadyGame, player: PlayerId) =>
      ready.game.current.player(player).get.board.warbands
    val before = table.ready
    val kinds = before.game.current.warbandsOnCards(cage).keys
    val supply = (ready: ReadyGame) =>
      kinds.toVector.map(PlayerFacts.banked(ready, _)).sum
    val after = done(throneStarted(table))
    assertEquals(after.game.current.warbandsOnCards, Map.empty)
    assertEquals(board(after, p2), board(before, p2))
    assertEquals(board(after, p1), board(before, p1))
    assertEquals(supply(after), supply(before) + 3)
