package distantlands.gameplay.powers.rest

import distantlands.gameplay.powers.NoteText
import distantlands.gameplay.setup.FirstGameSetupFixture.catalog
import distantlands.model._
import distantlands.testkit.{Look, PhasePowerHarness, Table}
import distantlands.testkit.Table.{p1, p2, p3}
import distantlands.testkit.Unwrap.accepted

/** Naysayers (card 21): "REST: If any Exile is the Oathkeeper or Usurper,
  * take [favor] from the Chancellor." (catalog batch 10, slice 1). */
class NaysayersSuite extends munit.FunSuite:
  import PhasePowerHarness._
  private val source = DecisionOptionRef.Denizen(NaysayersCard.id)

  /** p1, an Exile, rests holding Naysayers faceup; p2 is the Chancellor
    * with 2 favor, and `holder` holds the Oathkeeper title on `side`. */
  private def restingExileWhileTitled(holder: Option[PlayerId],
      side: TitleSide = TitleSide.Oathkeeper): Table =
    Table.start.imperial(p2).turn(p1, Phase.Rest)
      .adviser(p1, NaysayersCard).favor(p1, 0).favor(p2, 2)
      .oathkeeper(holder, side)

  private def usableFor(ready: ReadyGame): Boolean =
    Naysayers.usable(catalog, ready, p1, source)

  test("with an Exile as Oathkeeper it takes 1 favor from the Chancellor"):
    val ready = restingExileWhileTitled(Some(p1)).ready
    val done = accepted(use(ready, Naysayers, source))
    val end = Look(after(done))
    assertEquals(end.player(p1).board.favor, 1)
    assertEquals(end.player(p2).board.favor, 1)
    assertEquals(NoteText.said(Naysayers, done.events), Vector(
      NoteText.Said(NoteKey.Used, s"Took 1 favor from ${p2.value}.",
        covers = true)))
    assertEquals(replayed(ready, done.events), Right(done.state))

  test("another Exile as Usurper counts too, and takes the Chancellor's " +
      "last favor"):
    val ready = restingExileWhileTitled(Some(p3), TitleSide.Usurper)
      .favor(p2, 1).ready
    val end = Look(after(accepted(use(ready, Naysayers, source))))
    assertEquals(end.player(p1).board.favor, 1)
    assertEquals(end.player(p2).board.favor, 0)

  test("it is not usable while an Imperial player or nobody holds the " +
      "title"):
    assert(!usableFor(restingExileWhileTitled(Some(p2)).ready))
    assert(!usableFor(restingExileWhileTitled(None).ready))

  test("it is not usable with no Chancellor, or a Chancellor with no " +
      "favor"):
    val noEmpire = Table.start.turn(p1, Phase.Rest)
      .adviser(p1, NaysayersCard).oathkeeper(Some(p1)).ready
    assert(!usableFor(noEmpire))
    assert(!usableFor(restingExileWhileTitled(Some(p1)).favor(p2, 0).ready))

  test("it is optional"):
    assert(!Naysayers.forced)

  test("the Chancellor cannot take from themself"):
    val chancellorRests = Table.start.imperial(p2).turn(p2, Phase.Rest)
      .adviser(p2, NaysayersCard).favor(p2, 2).oathkeeper(Some(p1)).ready
    assert(!Naysayers.usable(catalog, chancellorRests, p2,
      DecisionOptionRef.Denizen(NaysayersCard.id)))
