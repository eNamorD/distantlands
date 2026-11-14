package distantlands.application

import distantlands.gameplay.{BanditCrownCard, GrandMaskCard, OathRules,
  WalkerDiceFixture}
import distantlands.gameplay.cards.AncientCitySite
import distantlands.gameplay.chronicle.{ChronicleWalk, ReliquaryAndRelics}
import distantlands.gameplay.chronicle.legacies.{TheEar, TheEarCard}
import distantlands.gameplay.powers.WalkerPowerCatalog
import distantlands.gameplay.powers.economy.CupOfPlentyCard
import distantlands.gameplay.setup.FirstGameSetupFixture.catalog
import distantlands.model._
import distantlands.testkit.{Outcomes, Table}
import distantlands.testkit.Table.{p1, p2, p3}

/** Cards a legacy shows its holder alone in the Chronicle (Legacy Chronicle
  * powers design, "Hidden choices"; R22): the holder peeks them, and every
  * other seat and the public view still see them hidden. */
class HiddenChronicleCardsSuite extends munit.FunSuite:
  private val rules = new OathRules(catalog,
    walkerPowerCatalog = WalkerPowerCatalog.default(catalog),
    walkerDice = WalkerDiceFixture.reversingShuffle)
  private val presentation = new GamePresentationProjector(catalog)
  private val projector = new GameProjector(catalog)

  /** As `TheEarSuite`'s table: p2 holds The Ear and three relics. */
  private def earTable: Table = ChronicleWalk.throneHeldByChancellorP1
    .warbandsAt(AncientCitySite, p1, 1)
    .relic(p2, CupOfPlentyCard).relic(p2, GrandMaskCard)
    .relic(p2, BanditCrownCard).legacy(p2, TheEarCard)

  test("the examined relics are named to the holder alone, and the holder " +
      "still knows each where it lies; another seat and the public do not " +
      "(R22)"):
    val started = ChronicleWalk.started(rules, earTable,
      TriggeredProcedureRef.ChronicleSun)
    val atEar = ChronicleWalk.walkedTo(rules, started, TheEar.decisionId(p2))
    val ready = Outcomes.ready(atEar.state)
    val relics = Vector(CupOfPlentyCard.id, GrandMaskCard.id,
      BanditCrownCard.id)
    assert(relics.forall(presentation.identifiesAt(ready, Some(p2), _)))
    Vector(Some(p1), Some(p3), None).foreach(viewer =>
      assert(relics.forall(!presentation.identifiesAt(ready, viewer, _)),
        viewer))
    val loaded = LoadedGame(atEar.state, 1L)
    assert(projector.project("ear", loaded, p2).walkerDecision
      .exists(_.query.exists(_.offeredOptions.nonEmpty)))
    Vector(projector.project("ear", loaded, p1),
        projector.projectPublic("ear", loaded)).foreach(view =>
      assert(!view.walkerDecision.exists(_.query.nonEmpty)))
    val after = ChronicleWalk.endOf(rules, earTable.state, atEar,
      TriggeredProcedureRef.ChronicleThrone)
    val site = ReliquaryAndRelics.emptySlots(catalog, earTable.ready).head
    val first = after.game.current.map.sites(site).relics.head.id
    assert(presentation.identifiesAt(after, Some(p2), first), first)
    assert(!presentation.identifiesAt(after, Some(p1), first), first)
