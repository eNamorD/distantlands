package distantlands.gameplay.powers.legacies

import distantlands.gameplay.actions.negotiation.HighPriestCard
import distantlands.gameplay.powers.NoteText
import distantlands.model._
import distantlands.testkit.{Look, PhasePowerHarness, Table}
import distantlands.testkit.Unwrap.accepted

/** High Priest (L21), ACTION: move secrets from the Darkest Secret to your
  * board (Legacy in-play powers rulings 11 and 13; implementation rulings
  * R3 and R4). */
class HighPriestSuite extends munit.FunSuite:
  import PhasePowerHarness._

  private val actor = Table.p1
  private val source = DecisionOptionRef.Legacy(HighPriestCard.id)
  private def table(secrets: Int = 3): Table =
    Table.start.turn(actor, Phase.Act).legacy(actor, HighPriestCard)
      .secrets(actor, faceUp = 0).darkestSecret(Some(actor), secrets)
  private def usable(ready: ReadyGame): Boolean =
    usableNow(ready).exists(_.power.id == HighPriest.id)
  private def amount(n: Int) = DecisionAnswer.ChooseAmountAnswer(n)

  test("the holder chooses from 1 to the secrets on the Darkest Secret, " +
      "and that many move to their board"):
    val ready = table().ready
    assert(usable(ready))
    val started = accepted(use(ready, HighPriest, source))
    assert(awaits(started, HighPriest.decisionId))
    val done = accepted(answer(started, actor, HighPriest.decisionId,
      amount(2)))
    val now = Look(after(done))
    assertEquals((now.secretsOnDarkestSecret, now.faceUpSecrets(actor)),
      (1, 2))
    assertEquals(now.darkestSecretHolder, Some(actor))
    assertEquals(replayed(ready, started.events ++ done.events),
      Right(done.state))
    assertEquals(NoteText.said(HighPriest, started.events ++ done.events),
      Vector(NoteText.Said(NoteKey.Used, s"${actor.value} moved 2 secrets " +
        s"from the Darkest Secret with ${HighPriestCard.id.value}",
        covers = false)))

  test("an amount past the secrets on the banner is refused"):
    val started = accepted(use(table().ready, HighPriest, source))
    assert(answer(started, actor, HighPriest.decisionId, amount(4)).isLeft)

  test("a walk resumed at High Priest's question moves the chosen amount"):
    val ready = table().ready
    val started = accepted(use(ready, HighPriest, source))
    val resumed = accepted(replayed(ready, started.events))
    val done = accepted(answer(OathTransition(resumed, Vector.empty), actor,
      HighPriest.decisionId, amount(3)))
    assertEquals(Look(after(done)).secretsOnDarkestSecret, 0)

  test("it is not offered without the Darkest Secret, or with none on it " +
      "(R4)"):
    val without = Table.start.turn(actor, Phase.Act)
      .legacy(actor, HighPriestCard).darkestSecret(Some(Table.p2), 2).ready
    assert(!usable(without))
    assertEquals(use(without, HighPriest, source).left.toOption,
      Some(OathViolation.StartUnavailable(
        "You do not hold the Darkest Secret")))
    val bare = table(secrets = 0).ready
    assert(!usable(bare))
    assertEquals(use(bare, HighPriest, source).left.toOption,
      Some(OathViolation.StartUnavailable(
        "The Darkest Secret holds no secrets")))
