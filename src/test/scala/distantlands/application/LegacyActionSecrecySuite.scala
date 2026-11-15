package distantlands.application

import distantlands.gameplay.cards.{AncientCitySite, BrokenPeaksSite,
  DunesSite}
import distantlands.gameplay.chronicle.legacies.TheEarCard
import distantlands.gameplay.powers.action.BrassHorseCard
import distantlands.gameplay.powers.campaign.StickyFireCard
import distantlands.gameplay.powers.legacies.{GoldenKey, GoldenKeyCard,
  TheEarAction}
import distantlands.gameplay.setup.FirstGameSetupFixture.catalog
import distantlands.model._
import distantlands.protocol.projection.GameProjectionCodec
import distantlands.testkit.{PhasePowerHarness, Table}
import distantlands.testkit.Table.{p1, p2}

/** What Golden Key takes and what The Ear peeks stay hidden from every
  * other player (Legacy in-play powers rulings 10, 12 and 13). */
class LegacyActionSecrecySuite extends munit.FunSuite:
  import PhasePowerHarness.{after, useAndAnswer}

  private val projector = new GameProjector(catalog)
  private def wireFor(ready: ReadyGame, viewer: PlayerId): String =
    GameProjectionCodec.encode(projector.project("g",
      LoadedGame(OathState.Ready(ready), 1L), viewer))
  private def known(ready: ReadyGame, viewer: PlayerId) =
    ready.knowledge.siteRelics.getOrElse(viewer, Map.empty)

  test("the relic Golden Key takes is named to no other player"):
    val ready = Table.start.imperial(p1).turn(p1, Phase.Act)
      .legacy(p1, GoldenKeyCard).favor(p1, 1).secrets(p1, faceUp = 1)
      .reliquaryFromDeck(2).ready
    val chosen = ready.game.campaign.reliquaryInPlay.head
    val done = after(useAndAnswer(ready, GoldenKey,
      DecisionOptionRef.Legacy(GoldenKeyCard.id), GoldenKey.decisionId,
      DecisionOptionRef.Relic(chosen)).answered)
    assertEquals(known(done, p2), known(ready, p2))
    assert(!wireFor(done, p2).contains(s"\"${chosen.value}\""))

  test("the relics The Ear peeks are named to no other player"):
    // p2 stands away from Broken Peaks, so nothing names its relics to p2.
    val ready = Table.start.turn(p1, Phase.Act).pawn(p1, AncientCitySite)
      .pawn(p2, DunesSite).legacy(p1, TheEarCard).relicAt(StickyFireCard, BrokenPeaksSite)
      .relicAt(BrassHorseCard, BrokenPeaksSite).ready
    val done = after(useAndAnswer(ready, TheEarAction,
      DecisionOptionRef.Legacy(TheEarCard.id), TheEarAction.decisionId,
      DecisionOptionRef.Site(BrokenPeaksSite.id)).answered)
    assertEquals(known(done, p2), known(ready, p2))
    val wire = wireFor(done, p2)
    Vector(StickyFireCard.id, BrassHorseCard.id).foreach(id =>
      assert(!wire.contains(s"\"${id.value}\""), id.value))
