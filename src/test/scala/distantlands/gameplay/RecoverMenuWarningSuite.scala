package distantlands.gameplay

import distantlands.gameplay.actions.recover.{RecoverProcedure,
  UndergroundLibraryCard}
import distantlands.gameplay.cards.{DeepWoodsSite, FairIsleSite}
import distantlands.gameplay.powers.action.DowsingSticksCard
import distantlands.gameplay.setup.FirstGameSetupFixture.catalog
import distantlands.model._
import distantlands.testkit.{MenuPlay, Table}
import distantlands.testkit.Table.p1

/** The Act menu's Recover warns when a success would take nothing: no
  * facedown relic lies at the actor's site, and Drowned Library cannot
  * offer the relic deck instead. A wasted Recover is legal, so the button
  * stays choosable. */
class RecoverMenuWarningSuite extends munit.FunSuite:
  // Fair Isle has a Recover difficulty; Deep Woods has none.
  private val recoverSite = FairIsleSite.id

  /** The warnings an option carries, through the menu's wrappers. */
  private def warningsOf(option: DecisionOption): Vector[OptionReason] =
    option match
      case DecisionOption.Warned(inner, reason) => reason +: warningsOf(inner)
      case DecisionOption.Grouped(inner, _) => warningsOf(inner)
      case DecisionOption.Badged(inner, _) => warningsOf(inner)
      case DecisionOption.Unavailable(inner, _) => warningsOf(inner)
      case _ => Vector.empty

  private def recoverOption(table: Table): DecisionOption =
    MenuPlay.menuOptionFor(catalog, table.ready, ActionRef.Recover)

  private val nothingWarning =
    OptionReason.Written(RecoverProcedure.nothingToRecover)

  /** p1 in their Act at Fair Isle, which holds no relic. */
  private val atEmptyFairIsle: Table = Table.start.pawn(p1, at = recoverSite)

  test("a Recover at a site with a facedown relic warns of nothing"):
    val option = recoverOption(atEmptyFairIsle
      .relicAt(DowsingSticksCard.id, at = recoverSite))
    assert(option.available, option)
    assertEquals(warningsOf(option), Vector.empty)

  test("a Recover at a site with no relic is choosable, with a warning"):
    val option = recoverOption(atEmptyFairIsle)
    assert(option.available, option)
    assertEquals(warningsOf(option), Vector(nothingWarning))

  test("a faceup relic at the site is not one a Recover can take"):
    val option = recoverOption(atEmptyFairIsle
      .relicAt(DowsingSticksCard.id, at = recoverSite, facedown = false))
    assertEquals(warningsOf(option), Vector(nothingWarning))

  test("Drowned Library at the site, with a relic deck, warns of nothing"):
    val option = recoverOption(atEmptyFairIsle
      .edifice(UndergroundLibraryCard, EdificeSide.Ruined, at = recoverSite))
    assert(option.available, option)
    assertEquals(warningsOf(option), Vector.empty)

  test("Drowned Library with an empty relic deck still warns"):
    val option = recoverOption(atEmptyFairIsle
      .edifice(UndergroundLibraryCard, EdificeSide.Ruined, at = recoverSite)
      .relicDeckEmptied)
    assertEquals(warningsOf(option), Vector(nothingWarning))

  test("at a site with no Recover difficulty the Recover stays unavailable"):
    val option = recoverOption(Table.start.pawn(p1, at = DeepWoodsSite.id))
    assert(!option.available, option)
