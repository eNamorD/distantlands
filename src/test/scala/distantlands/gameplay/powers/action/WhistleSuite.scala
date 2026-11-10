package distantlands.gameplay.powers.action

import distantlands.gameplay.cards.{AncientCitySite, BrokenPeaksSite,
  BuriedGiantSite}
import distantlands.gameplay.powers.{NoteText, PowerFixture}
import distantlands.model._
import distantlands.testkit.{Look, PhasePowerHarness, Refusals, Table}
import distantlands.testkit.ActPowerHarness.usableIds
import distantlands.testkit.Outcomes.ready
import distantlands.testkit.Table.{p2, p3}
import distantlands.testkit.Unwrap.accepted

class WhistleSuite extends munit.FunSuite:
  import PowerFixture._
  import MovementFixture._
  import PhasePowerHarness.{awaits, backToActing, ops, replayed}

  private val whistle = WhistleCard.id
  /** p1's Act at Ancient City holding Whistle with `secrets` faceup
    * secrets; p2 stands at Broken Peaks and p3 at Buried Giant. `homelands`
    * adds the ruined edifices at Deep Woods and Golden Valley. */
  private def staged(secrets: Int = 2)(using munit.Location) =
    Table.from(homelands(quietTable)).turn(actor, Phase.Act)
      .pawn(actor, AncientCitySite).pawn(p2, BrokenPeaksSite)
      .pawn(p3, BuriedGiantSite).relic(actor, whistle).secrets(actor, secrets)
      .ready
  private val target = DecisionOptionRef.Player(p2)

  test("it pulls the chosen pawn to the actor's site and hands over the secret"):
    val start = staged()
    val parked = accepted(use(start, Whistle.id, whistle))
    assert(awaits(parked, Whistle.decisionId))
    assertEquals(Look(ready(parked.state)).tokensOn(whistle), Tokens(0, 1))

    val done = accepted(choose(parked.state, Whistle.decisionId, target))
    val after = ready(done.state)
    assert(backToActing(done))
    assertEquals(Look(after).pawn(p2), AncientCitySite.id)
    assertEquals(Look(after).pawn(p3), BuriedGiantSite.id)
    assertEquals(Look(after).pawn(actor), AncientCitySite.id)
    assertEquals(Look(after).tokensOn(whistle), Tokens.empty)
    assertEquals(Look(after).player(p2).board.faceUpSecrets,
      Look(start).player(p2).board.faceUpSecrets + 1)
    assertEquals(Look(after).player(actor).board.faceUpSecrets, 1)
    assertEquals(replayed(start, parked.events ++ done.events), Right(done.state))

  test("only players at other sites are offered, even when there is one"):
    val start = Table.from(staged()).pawn(p3, AncientCitySite).ready
    val parked = accepted(use(start, Whistle.id, whistle))
    assert(awaits(parked, Whistle.decisionId))
    assertEquals(choose(parked.state, Whistle.decisionId,
      DecisionOptionRef.Player(p3)).left.toOption,
      Some(Refusals.notOffered(Whistle.decisionId)))
    val done = accepted(choose(parked.state, Whistle.decisionId, target))
    assert(backToActing(done))

  test("with nobody to pull, the cost is paid and the secret stays"):
    val start = Table.from(staged()).pawn(p3, AncientCitySite).pawn(p2,
      AncientCitySite).ready
    val done = accepted(use(start, Whistle.id, whistle))
    val after = ready(done.state)
    assert(backToActing(done))
    assertEquals(Look(after).tokensOn(whistle), Tokens(0, 1))
    assertEquals(Look(after).player(actor).board.faceUpSecrets, 1)
    assertEquals(Look(after).pawn(p3), AncientCitySite.id)
    assert(!ops(done.events).exists {
      case Move(Piece.Pawn(_), _, _, _, _) => true
      case _ => false
    })
    assert(!usableIds(after).contains(Whistle.id),
      "the secret still rests on the Whistle")

  test("it is unusable without a secret"):
    assert(usableIds(staged()).contains(Whistle.id))
    assert(!usableIds(staged(secrets = 0)).contains(Whistle.id))
    assertEquals(use(staged(secrets = 0), Whistle.id, whistle).left.toOption,
      Some(OathViolation.InsufficientSecrets(1, 0)))

  test("it writes the pawn it pulled and the secret it gave"):
    val parked = accepted(use(staged(), Whistle.id, whistle))
    val done = accepted(choose(parked.state, Whistle.decisionId, target))
    assertEquals(NoteText.said(Whistle, done.events), Vector(NoteText.Said(
      NoteKey.Used, s"Placed ${p2.value} at " +
        s"${AncientCitySite.id.value} and gave " +
        s"${p2.value} the Whistle's secret.", covers = false)))

  test("with nobody to pull it writes that"):
    val start = Table.from(staged()).pawn(p3, AncientCitySite).pawn(p2,
      AncientCitySite).ready
    val done = accepted(use(start, Whistle.id, whistle))
    assertEquals(NoteText.said(Whistle, done.events), Vector(NoteText.Said(
      "used.none", "No pawn could be pulled.", covers = false)))
