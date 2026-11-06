package oathdigital.gameplay.powers.recover

import oathdigital.catalog.ModifierMark
import oathdigital.gameplay.{ModifierPlay, OathRules}
import oathdigital.gameplay.WalkerDiceFixture.scripted
import oathdigital.gameplay.actions.recover.{RecoverProcedure,
  UndergroundLibraryCard}
import oathdigital.gameplay.cards.{BrokenPeaksSite, DeepWoodsSite,
  FairIsleSite}
import oathdigital.gameplay.powers.WalkerPowerCatalog
import oathdigital.gameplay.setup.FirstGameSetupFixture.catalog
import oathdigital.gameplay.walker.{DeltaPayload, WalkerStepRecorded}
import oathdigital.model._
import oathdigital.model.DecisionAnswer.ChooseOneAnswer
import oathdigital.model.OathState.Ready
import oathdigital.testkit.{ActPowerHarness, Look, MenuPlay, Outcomes,
  ParkedNode, Table}
import oathdigital.testkit.Outcomes.assertWireRoundTrips
import oathdigital.testkit.Table.p1
import oathdigital.testkit.Unwrap.accepted

/** Drowned Library (E13, ruined), a Recover modifier for 1 secret: with
  * the user's pawn at its site, the Recover takes the relic deck's top card
  * at difficulty 5 (catalog batch 10, slice 8). */
class DrownedLibrarySuite extends munit.FunSuite:
  import DefenseDieFace.{Blank, OneShield, TwoShields}

  private val library = UndergroundLibraryCard.id
  private val libraryRef = DecisionOptionRef.Edifice(library)
  private val modifierDecisionId = "recover.modifiers"

  /** Rules whose Recover rolls two shields and two shields, then one shield
    * and a blank: 4 on the first roll, 5 after the second. */
  private def rulesRollingFourThenFive: OathRules = new OathRules(catalog,
    walkerPowerCatalog = WalkerPowerCatalog.default(catalog),
    walkerDice = scripted(Vector(TwoShields, TwoShields),
      Vector(OneShield, Blank)))

  /** p1 at `site`, where Drowned Library stands, with one faceup secret. */
  private def atDrownedLibrary(site: SiteId = FairIsleSite.id): Table =
    Table.start.pawn(p1, at = site)
      .edifice(UndergroundLibraryCard, EdificeSide.Ruined, at = site)
      .secrets(p1, faceUp = 1)

  /** `table` with Relic Worship as p1's adviser and a second secret to pick
    * it with, so the modifier decision is asked whatever Drowned Library's
    * state. */
  private def withRelicWorship(table: Table): Table =
    table.adviser(p1, RelicWorshipCard).secrets(p1, faceUp = 2)

  /** p1's Recover from `table`, started with `picks` picked. */
  private def recoverStarted(rules: OathRules, table: Table,
      picks: Vector[PowerId]): OathTransition =
    accepted(ModifierPlay.started(rules, Ready(table.ready),
      ActionRef.Recover, p1, PowerWindow.RecoverModifierSelection, picks))

  private def answered(rules: OathRules, from: OathTransition,
      decisionId: String, ref: DecisionOptionRef): OathTransition =
    accepted(rules.resolveWalker(from.state, p1, decisionId,
      ChooseOneAnswer(ref)))

  /** The modifier decision's options at p1's first Recover park. */
  private def modifierOptions(table: Table): Vector[DecisionOption] =
    val begun = accepted(MenuPlay.started(rulesRollingFourThenFive,
      Ready(table.ready), p1, ActionRef.Recover, Vector.empty))
    ParkedNode.of(begun.state, catalog,
        WalkerPowerCatalog.default(catalog)) match
      case Right(Some(ParkedNode.Decision(_, decide, _)))
          if decide.decisionId == modifierDecisionId =>
        decide.query match
          case one: DecisionQuery.ChooseOne => one.options
          case other => fail(s"expected a choose-one, got $other")
      case other => fail(s"expected the modifier decision, got $other")

  private def libraryOption(table: Table): DecisionOption =
    modifierOptions(table).find(_.ref == libraryRef)
      .getOrElse(fail("Drowned Library is not listed"))

  test("its printed power carries the Recover icon"):
    assertEquals(UndergroundLibraryCard.ruined.power.modifier,
      Some(ModifierMark.Action(MajorActionType.Recover)))

  test("picked, the secret goes onto the card, the Recover needs 5, and it " +
      "takes the relic deck's top card facedown"):
    val rules = rulesRollingFourThenFive
    val table = atDrownedLibrary()
    val ready = table.ready
    val top = ready.game.current.commonCards.relicDeck.head
    val started = recoverStarted(rules, table, Vector(DrownedLibrary.id))
    // Four shields reach Fair Isle's 3 but not 5, so the choice is asked.
    assertEquals(Outcomes.ready(started.state).game.current.walkerPending
      .map(_.at), Some(Vector("1", "1", "0", "2", "0")))
    val continued = answered(rules, started,
      RecoverProcedure.choiceDecisionId, DecisionOptionRef.Button("continue"))
    val taken = answered(rules, continued, RecoverProcedure.relicDecisionId,
      RecoverProcedure.relicDeckOption)
    val after = Outcomes.ready(taken.state)
    assertEquals(Look(after).player(p1).relics
      .map(held => held.id -> held.orientation),
      Vector(top -> Orientation.FaceDown))
    assertEquals(after.game.current.commonCards.relicDeck,
      ready.game.current.commonCards.relicDeck.tail)
    assertEquals(Look(after).tokensOn(library), Tokens(0, 1))
    val events = started.events ++ continued.events ++ taken.events
    assertEquals(ActPowerHarness.replayed(rules, ready, events), after)
    // The dice-pool change every Recover starts with does not round-trip
    // the wire (its window is not kept), so it is left out of the check.
    assertWireRoundTrips(events.filterNot {
      case WalkerStepRecorded(_, DeltaPayload(ops), _) =>
        ops.exists(_.isInstanceOf[ModifyDicePool])
      case _ => false
    })

  test("picked, a Recover that stops short of 5 takes nothing"):
    val rules = rulesRollingFourThenFive
    val table = atDrownedLibrary()
    val started = recoverStarted(rules, table, Vector(DrownedLibrary.id))
    val stopped = answered(rules, started, RecoverProcedure.choiceDecisionId,
      DecisionOptionRef.Button("stop"))
    val after = Outcomes.ready(stopped.state)
    assertEquals(Look(after).player(p1).relics, Vector.empty)
    assertEquals(after.game.current.commonCards.relicDeck,
      table.ready.game.current.commonCards.relicDeck)

  test("it makes a site with no Recover difficulty recoverable"):
    val table = atDrownedLibrary(DeepWoodsSite.id)
    assert(MenuPlay.availableInMenu(catalog, table.ready, ActionRef.Recover))
    assert(!MenuPlay.availableInMenu(catalog,
      table.secrets(p1, faceUp = 0).ready, ActionRef.Recover))

  test("it is offered for a Recover, at its secret"):
    assertEquals(libraryOption(withRelicWorship(atDrownedLibrary())),
      DecisionOption.Priced(DecisionOption.Edifice(libraryRef),
        OptionPrice(secrets = 1)))

  test("it is unavailable at a site its user rules but their pawn is not at"):
    val table = withRelicWorship(Table.start.pawn(p1, at = FairIsleSite)
      .edifice(UndergroundLibraryCard, EdificeSide.Ruined,
        at = BrokenPeaksSite)
      .warbandsAt(BrokenPeaksSite, p1, 1))
    assertEquals(libraryOption(table), DecisionOption.Unavailable(
      DecisionOption.Edifice(libraryRef),
      OptionReason.Written(DrownedLibrary.awayFromLibrary)))

  test("it is unavailable with an empty relic deck"):
    assertEquals(libraryOption(withRelicWorship(atDrownedLibrary())
        .relicDeckEmptied),
      DecisionOption.Unavailable(DecisionOption.Edifice(libraryRef),
        OptionReason.Written(DrownedLibrary.emptyRelicDeck)))

  test("it is not listed while the edifice shows its intact face"):
    val table = withRelicWorship(Table.start.pawn(p1, at = FairIsleSite)
      .edifice(UndergroundLibraryCard, EdificeSide.Intact,
        at = FairIsleSite))
    assert(!modifierOptions(table).exists(_.ref == libraryRef),
      modifierOptions(table))
