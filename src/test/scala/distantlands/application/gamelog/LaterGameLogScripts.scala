package distantlands.application.gamelog

import distantlands.application.{GameApplicationService,
  InMemoryEventStreamRepository}
import distantlands.gameplay.powers.setup.GreatClockCard
import distantlands.gameplay.setup.FirstGameSetupFixture.catalog
import distantlands.gameplay.setup.LaterGameFixture
import distantlands.model._
import distantlands.testkit.{Dice, Look, Situation}
import distantlands.testkit.Table.p1

/** [[LogScripts]]' scripts for a later game. */
object LaterGameLogScripts:
  /** A later game's Setup: l1 the seated Chancellor, l2 a Citizen, the
    * Reliquary holding two relics, and every choice the first offered. */
  def laterGameWoken(using munit.Location): Script =
    laterGameWokenFrom("later-game-setup", identity)

  /** As [[laterGameWoken]], with the Great Clock stored at the first Atlas
    * site that keeps an edifice, a site that comes into play, so its ruined face, Broken Clock,
    * advances the round marker to round 2 at Setup's end. */
  def laterGameWithBrokenClock(using munit.Location): Script =
    laterGameWokenFrom("later-game-broken-clock", chronicle =>
      val first = chronicle.atlasBox.indexWhere(
        _.items.exists(_.isInstanceOf[EdificeId]))
      chronicle.copy(atlasBox = chronicle.atlasBox.updated(first,
        chronicle.atlasBox(first).copy(items = Vector(GreatClockCard.id)))))

  /** A later game's Setup on stream `name`, from the fixture's Chronicle as
    * `changed` leaves it. */
  private def laterGameWokenFrom(name: String,
      changed: Chronicle => Chronicle)(using munit.Location): Script =
    val record = LaterGameFixture.chronicle(LineageId("l1"), LineageId("l2"))
    val chronicle = changed(record.copy(relicDeck = record.relicDeck.drop(2),
      reliquary = record.relicDeck.take(2)))
    val repository = new InMemoryEventStreamRepository
    val service = new GameApplicationService(catalog, repository,
      campaignDicePort = Dice.steady)
    val situation = Situation.wake(Situation.journaled(service, catalog,
      repository, name), chronicle, LaterGameFixture.orders(chronicle, p1))
    Script(name, service, Look(situation.ready).active)

  val named: Vector[(String, () => Script)] = Vector(
    "later-game-setup" -> (() => laterGameWoken))
