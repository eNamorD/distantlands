package oathdigital.application.gamelog

import oathdigital.application.{GameApplicationService,
  InMemoryEventStreamRepository}
import oathdigital.gameplay.setup.FirstGameSetupFixture.catalog
import oathdigital.gameplay.setup.LaterGameFixture
import oathdigital.model._
import oathdigital.testkit.{Dice, Look, Situation}
import oathdigital.testkit.Table.p1

/** [[LogScripts]]' scripts for a later game. */
object LaterGameLogScripts:
  /** A later game's Setup: l1 the seated Chancellor, l2 a Citizen, the
    * Reliquary holding two relics, and every choice the first offered. */
  def laterGameWoken(using munit.Location): Script =
    val record = LaterGameFixture.chronicle(LineageId("l1"), LineageId("l2"))
    val chronicle = record.copy(relicDeck = record.relicDeck.drop(2),
      reliquary = record.relicDeck.take(2))
    val repository = new InMemoryEventStreamRepository
    val service = new GameApplicationService(catalog, repository,
      campaignDicePort = Dice.steady)
    val situation = Situation.wake(Situation.journaled(service, catalog,
      repository, "later-game-setup"), chronicle,
      LaterGameFixture.orders(chronicle, p1))
    Script("later-game-setup", service, Look(situation.ready).active)

  val named: Vector[(String, () => Script)] = Vector(
    "later-game-setup" -> (() => laterGameWoken))
