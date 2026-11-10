package distantlands.gameplay.powers.action

import distantlands.gameplay.powers.NoteText
import distantlands.model._
import distantlands.testkit.{Look, PhasePowerHarness, Table}
import distantlands.testkit.Outcomes.assertWireRoundTrips
import distantlands.testkit.Table.{p1, p2}
import distantlands.testkit.Unwrap.accepted

class StorytellerSuite extends munit.FunSuite:
  import PhasePowerHarness._

  private val teller = StorytellerCard.id
  private val source = DecisionOptionRef.Denizen(teller)

  private def staged(favor: Int = 1): Table =
    Table.start.turn(p1, Phase.Act).adviser(p1, teller).favor(p1, favor)

  private def onBanner(ready: ReadyGame): Int =
    ready.game.current.banners.darkestSecret.secrets

  test("it places a favor, then a secret from the shared bank on a held " +
      "Darkest Secret"):
    val ready = staged().darkestSecret(Some(p2), 1).ready
    val t = accepted(use(ready, Storyteller, source))
    parked.assertResumed(t.state, Phase.Act, p1)
    val end = after(t)
    assertEquals(Look(end).tokensOn(teller), Tokens(1, 0))
    assertEquals(onBanner(end), 2)
    assertEquals(end.game.current.banners.darkestSecret.holder, Some(p2))
    assertEquals(NoteText.said(Storyteller, t.events), Vector(NoteText.Said(
      NoteKey.Used, s"${p1.value} placed 1 secret on the Darkest Secret.",
      covers = false)))
    assertEquals(replayed(ready, t.events), Right(t.state))
    assertWireRoundTrips(t.events)

  test("a Darkest Secret nobody holds takes the secret too"):
    val unheld = staged().darkestSecret(None, 0).ready
    val t = accepted(use(unheld, Storyteller, source))
    assertEquals(onBanner(after(t)), 1)

  test("it is unusable without a favor"):
    val broke = staged(favor = 0).ready
    assert(!usableNow(broke).exists(_.power.id == Storyteller.id))
    assertEquals(use(broke, Storyteller, source).left.toOption,
      Some(OathViolation.InsufficientFavor(1, 0)))
