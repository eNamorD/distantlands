package oathdigital.gameplay.chronicle

import oathdigital.gameplay.{OathRules, WalkerDiceFixture}
import oathdigital.gameplay.actions.ProcedureNotes
import oathdigital.gameplay.powers.{NoteText, WalkerPowerCatalog}
import oathdigital.gameplay.setup.FirstGameSetupFixture.catalog
import oathdigital.model._
import oathdigital.testkit.{CatalogNames, Outcomes, RecordedOps, Table}
import oathdigital.testkit.Table.p1
import oathdigital.testkit.Unwrap.accepted

/** The Stars task (CR pp. 40-41, Chronicle rulings 7, 8, 17 and 18): rival
  * sites are archived with their cards, the Empire's sites are kept, and
  * the journaled Chronicle reads both from the table. p1 is the Chancellor
  * holding the Throne; the Empire holds Ancient City (an Enduring site)
  * only when a test gives it an Imperial warband. */
class StarsTaskSuite extends munit.FunSuite:
  private val rules = new OathRules(catalog,
    walkerPowerCatalog = WalkerPowerCatalog.default(catalog),
    walkerDice = WalkerDiceFixture.reversingShuffle)

  private val ancientCity = CatalogNames.site("Ancient City")
  private val brokenPeaks = CatalogNames.site("Broken Peaks")

  private def empireAt(sites: String*): Table =
    sites.foldLeft(ChronicleWalk.throneHeldByChancellorP1)((table, site) =>
      table.warbandsAt(site, p1, 1))

  /** `table`, from the Throne through Finish, every choice plain. */
  private def chronicleFinished(table: Table)(using munit.Location)
      : OathTransition =
    ChronicleWalk.answeringEverything(rules, accepted(rules.next(
      OathTransition(table.state, Vector.empty),
      TriggeredProcedureRef.ChronicleSun)))

  private def recordOf(done: OathTransition)(using munit.Location)
      : Chronicle =
    RecordedOps.all(done.events).collect {
      case ChronicleUpdated(chronicle) => chronicle
    }.lastOption.getOrElse(fail("no Chronicle was journaled"))

  /** The cards on `site`, in no particular order. ChronicleExtractionSuite
    * checks the order the record keeps them in. */
  private def cardsAt(ready: ReadyGame, site: SiteId): Set[CardId] =
    val state = ready.game.current.map.sites(site)
    (state.denizens.map(_.id) ++ state.relics.map(_.id)).toSet

  test("rival sites are archived Cradlemost first, above the stored atlas, " +
      "and the Empire keeps its sites"):
    val done = chronicleFinished(empireAt("Broken Peaks"))
    val ready = Outcomes.ready(done.state)
    val record = recordOf(done)
    val stored = ready.game.campaign.atlas.entries.collect {
      case AtlasEntry.StoredSite(id, _, _, _) => id }
    val inPlay = ready.game.current.map.hinterward
    assertEquals(record.world.map(_.site), Vector(brokenPeaks))
    assertEquals(record.atlasBox.map(_.site),
      inPlay.filterNot(_ == brokenPeaks) ++ stored)

  test("every site keeps every card left on it, including an Enduring " +
      "rival site's denizen"):
    val roar = CatalogNames.denizen("Threatening Roar")
    val done = chronicleFinished(empireAt("Broken Peaks")
      .denizen("Threatening Roar", at = "Ancient City"))
    val ready = Outcomes.ready(done.state)
    val record = recordOf(done)
    assert(record.atlasBox.find(_.site == ancientCity).get.items
      .contains(roar), record.atlasBox)
    (record.world ++ record.atlasBox.take(7)).foreach(stored =>
      assertEquals(stored.items.toSet, cardsAt(ready, stored.site),
        stored.site))

  test("the record takes the decks, the Reliquary, both Dispossessed piles " +
      "and the kept starting advisers from the table"):
    val done = chronicleFinished(empireAt("Broken Peaks"))
    val ready = Outcomes.ready(done.state)
    val record = recordOf(done)
    val current = ready.game.current
    val campaign = ready.game.campaign
    assertEquals(record.worldDeck, current.commonCards.worldDeck.collect {
      case id: DenizenId => id })
    assertEquals(record.relicDeck, current.commonCards.relicDeck)
    assertEquals(record.reliquary, campaign.reliquary)
    assertEquals(record.dispossessed, (campaign.dispossessed ++
      current.newlyDispossessed).collect { case id: DenizenId => id })
    assertEquals(current.newlyDispossessed.size, 6)
    current.players.foreach(player =>
      assertEquals(record.lineages.find(_.id == player.lineage)
        .map(_.startingAdvisers),
        Some(campaign.lineages(player.lineage).startingAdvisers)))

  test("each archived and kept site writes its line"):
    val done = chronicleFinished(empireAt("Broken Peaks"))
    val inPlay = Outcomes.ready(done.state).game.current.map.hinterward
    val said = NoteText.said(
      ProcedureNotes.id(TriggeredProcedureRef.ChronicleStars),
      ChronicleStarsProcedure.noteKeys, done.events).map(_.text)
    assertEquals(said, inPlay.filterNot(_ == brokenPeaks)
      .map(site => s"${site.value} was archived") :+
      s"${brokenPeaks.value} stays in the Empire")

  test("the Stars follow the Beacon and precede Finish"):
    assertEquals(Vector(TriggeredProcedureRef.ChronicleBeacon,
      TriggeredProcedureRef.ChronicleStars).map(ChronicleSequence.after),
      Vector(Some(TriggeredProcedureRef.ChronicleStars),
        Some(TriggeredProcedureRef.ChronicleFinish)))
