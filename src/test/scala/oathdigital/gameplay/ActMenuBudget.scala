package oathdigital.gameplay

import oathdigital.gameplay.CampaignFixture.board
import oathdigital.gameplay.cards.{AncientCitySite, BrokenPeaksSite,
  BuriedGiantSite, DeepWoodsSite, DesolateShoreSite}
import oathdigital.gameplay.powers.{PhasePowerCatalog, WalkerPowerCatalog}
import oathdigital.gameplay.powers.action.{SecondChanceCard, WildAlliesCard,
  WolvesCard}
import oathdigital.gameplay.powers.cardplay.WildCryCard
import oathdigital.gameplay.powers.search.ErrandBoyCard
import oathdigital.gameplay.powers.whenplayed.AnimalHostCard
import oathdigital.gameplay.setup.FirstGameSetupFixture.catalog
import oathdigital.gameplay.walker.{ProcedureWalker, WalkerOutcome,
  WalkerPowers, WalkerProcedureRegistry}
import oathdigital.model._
import oathdigital.testkit.Table

/** The Act menu's budget (walker-driven game loop design, ruling 8): on a
  * full board, building and walking the menu to its park, and reading the
  * park back, each take under 50 ms. A benchmark run by hand, never by
  * `sbt test`:
  *
  * {{{./sbtw "Test/runMain oathdigital.gameplay.ActMenuBudget"}}}
  *
  * Each measure is the best of five runs after three warm-up runs. It exits
  * with status 1 when a measure is over budget.
  */
object ActMenuBudget:
  private val budgetMillis = 50L

  private def best(body: => Any): Long =
    (1 to 3).foreach(_ => body)
    (1 to 5).map { _ =>
      val start = System.nanoTime()
      body
      (System.nanoTime() - start) / 1000000L
    }.min

  /** A faceup Beast stands at five sites, p1's own among them. p2's and
    * p3's pawns stand at two of them, each with some of p1's warbands;
    * Bandits and p1's warbands hold a third; the fourth holds nothing to
    * attack. With `allies`, p1 holds Wild Allies faceup, whose menu option
    * and site question price a Campaign from each of those sites. */
  private def beastSites(allies: Boolean): (ReadyGame, PlayerId) =
    val beasts = Vector(AncientCitySite -> SecondChanceCard,
      BrokenPeaksSite -> ErrandBoyCard, BuriedGiantSite -> WolvesCard,
      DeepWoodsSite -> WildCryCard, DesolateShoreSite -> AnimalHostCard)
    val held =
      if allies then Table.start.adviser(Table.p1, WildAlliesCard)
      else Table.start
    val table = beasts.foldLeft(held)((table, beast) =>
        table.denizen(beast._2, at = beast._1))
      .warbandsAt(BrokenPeaksSite, Table.p1, 2)
      .warbandsAt(BuriedGiantSite, Table.p1, 1)
      .bandits(DeepWoodsSite, 2).warbandsAt(DeepWoodsSite, Table.p1, 3)
    (table.ready, Table.p1)

  def main(args: Array[String]): Unit =
    val walkerPowers = WalkerPowers.selected(
      WalkerPowerCatalog.default(catalog), Vector.empty)
    val phasePowers = PhasePowerCatalog.default(catalog)
    // A rich board answers "Start" at once; a poor one cannot afford any
    // start, so every option searches all its modifier picks to the end.
    val boards = Vector(7, 0).map { supply =>
      val b = board(extras = 3, warbands = 5, supply = supply)
      (s"with $supply Supply", b.ready, b.actor)
    } ++ Vector(false, true).map { allies =>
      val (ready, actor) = beastSites(allies)
      (s"at five Beast sites${if allies then " with Wild Allies" else ""}",
        ready, actor)
    }
    val measures = boards.map { (label, ready, actor) =>
      val menu = TriggeredProcedureRef.ActMenu
      def tree: Operation = WalkerProcedureRegistry.build(menu, catalog,
          ready, actor, Vector.empty, phasePowers,
          walkerPowers = walkerPowers)
        .fold(violation => sys.error(s"the Act menu must build: $violation"),
          identity)
      def parked: PendingTree = ProcedureWalker.advance(ready, tree, None,
          walkerPowers) match
        case Right(WalkerOutcome.Parked(pending, _)) => pending
        case other => sys.error(s"the Act menu must park: $other")
      val park = best(parked)
      val pending = parked
      val asked = ready.updateCurrent(_.copy(
        walkerPending = Some(pending), walkerProcedure = Some(menu)))
      val read = best(ProcedureWalker.openDecisions(asked, tree, pending,
        walkerPowers))
      println(s"Act menu $label: park $park ms; reading it " +
        s"$read ms (budget $budgetMillis ms each)")
      (park, read)
    }
    if measures.exists((park, read) =>
        park >= budgetMillis || read >= budgetMillis) then sys.exit(1)
