package distantlands.gameplay.powers.whenplayed

import distantlands.gameplay.actions.ProcedureNotes
import distantlands.gameplay.actions.negotiation.{NegotiationProcedure,
  TheTribunalCard, TravelingNegotiatorCard}
import distantlands.gameplay.operations.TheGrandScepterCard
import distantlands.gameplay.powers.NoteText
import distantlands.gameplay.powers.economy.BirdsongCard
import distantlands.gameplay.powers.poweruse.{CityWall, CityWallCard}
import distantlands.gameplay.walker.{ProcedureWalker, WalkerDice, WalkerPowers}
import distantlands.model._
import distantlands.testkit.{Look, Table}
import distantlands.testkit.Table.{p1, p2, p3}

/** The Gathering's deals (catalog batch 12, rulings 3 to 5 and 32): the
  * open question, passes, and each deal under its own prefix. The card
  * stands at `homeSite`. */
class TheGatheringDealsSuite extends munit.FunSuite:
  import WhenPlayedHarness._

  private val power = registered[TheGathering.type]
  private val gathering = power.cardId
  private val played = hookAt(gathering, homeSite)
  private val alone = powers(power)
  private val birdsong: WorldCardId = BirdsongCard.id

  /** All three pawns at `homeSite` with the card: nobody is asked to
    * come. */
  private def together: Table = p1AtHomeSite.denizen(gathering, at = homeSite)
    .pawn(p2, homeSite).pawn(p3, homeSite)

  /** p1 and p2 there, p3 at `awaySite`, who is asked to come first. */
  private def p3Away: Table = p1AtHomeSite.denizen(gathering, at = homeSite)
    .pawn(p2, homeSite).pawn(p3, awaySite)

  private def open(players: PlayerId*): DecisionAnswer =
    DecisionAnswer.OpenDeal(players.toVector)
  private val pass = DecisionAnswer.PassGathering
  private val unpass = DecisionAnswer.UnpassGathering
  private val accept = DecisionAnswer.AcceptDeal
  private val decline = DecisionAnswer.DeclineDeal
  private val stay = DecisionAnswer.ChooseOneAnswer(TheGathering.stay)

  private def giving(to: PlayerId, favor: Int = 0, secrets: Int = 0,
      advisers: Vector[WorldCardId] = Vector.empty): DecisionAnswer =
    DecisionAnswer.ProposeTerms(NegotiationTerms(Vector(NegotiationTransfer(
      to, favor, Vector.empty, advisers, secrets = secrets))))

  private def openQuestions(walked: Walked): Vector[DecisionQuery.Gathering] =
    walked.asked.map(_.query).collect { case q: DecisionQuery.Gathering => q }

  private def openQuestion(at: ParkedAt)(using munit.Location)
      : DecisionQuery.Gathering = at.question.query match
    case question: DecisionQuery.Gathering => question
    case other => fail(s"expected the open question, got $other")

  private def deal(at: ParkedAt)(using munit.Location)
      : DecisionQuery.Negotiate = at.question.query match
    case question: DecisionQuery.Negotiate => question
    case other => fail(s"expected a deal, got $other")

  /** Whether `by`'s `answer` to the question `at` parks on is refused. */
  private def refused(at: ParkedAt, by: PlayerId, answer: DecisionAnswer,
      walkPowers: WalkerPowers = alone): Boolean =
    ProcedureWalker.resolve(at.at, played, at.tree,
      Answered(at.question.decisionId, answer, by), walkPowers,
      WalkerDice.unavailable).isLeft

  /** The Negotiation's lines among `events`, as plain sentences. */
  private def negotiationLines(events: Vector[OathEvent]): Vector[String] =
    NoteText.said(ProcedureNotes.id(ActionRef.Negotiation),
      NegotiationProcedure.noteKeys, events).map(_.text)

  test("two deals in one Gathering keep their answers apart"):
    val ready = together.favor(p1, 2).ready
    val walked = walkAnsweringAs(ready, alone, played,
      p1 -> open(p2), p1 -> giving(p2, favor = 1), p2 -> accept,
      p1 -> accept, p2 -> open(p3), p3 -> decline,
      p1 -> pass, p2 -> pass, p3 -> pass)
    assertEquals(walked.asked.map(_.decisionId), Vector("gathering.open",
      "gathering.1.deal", "gathering.1.deal", "gathering.1.deal",
      "gathering.open", "gathering.2.deal",
      "gathering.open", "gathering.open", "gathering.open"))
    assertEquals(Look(walked.end).favor(p2), Look(ready).favor(p2) + 1)
    // The actor's deal reads as the Negotiate action's; p2's, declined,
    // names who ended it.
    assertEquals(negotiationLines(walked.events), Vector(
      "Negotiated with p2", "p1 gave 1 favor to p2",
      "Negotiation ended by p3"))
    assertJournalRebuilds(ready, walked.events, walked.end)

  test("a deal another player opens names its opener"):
    val walked = walkAnsweringAs(together.favor(p2, 1).ready, alone, played,
      p2 -> open(p1), p2 -> giving(p1, favor = 1), p1 -> accept,
      p2 -> accept, p1 -> pass, p2 -> pass, p3 -> pass)
    assertEquals(negotiationLines(walked.events).take(2), Vector(
      "p2 negotiated with p1", "p2 gave 1 favor to p1"))

  test("a pass can be taken back, and a passed player named in a deal " +
      "keeps their pass"):
    val walked = walkAnsweringAs(together.ready, alone, played,
      p2 -> pass, p2 -> unpass, p2 -> pass, p1 -> open(p2), p2 -> decline,
      p3 -> pass, p1 -> pass)
    assertEquals(openQuestions(walked).map(_.passed), Vector(Vector(),
      Vector(p2), Vector(), Vector(p2), Vector(p2), Vector(p2, p3)))

  test("opening a deal clears the opener's pass, and only theirs"):
    val walked = walkAnsweringAs(together.ready, alone, played,
      p1 -> pass, p2 -> pass, p1 -> open(p2), p2 -> decline,
      p1 -> pass, p3 -> pass)
    assertEquals(openQuestions(walked).map(_.passed), Vector(Vector(),
      Vector(p1), Vector(p1, p2), Vector(p2), Vector(p1, p2)))

  test("a bystander may pass during a deal, and a participant may not"):
    val at = parkAfterAs(together.ready, alone, played, p1 -> open(p2),
      p3 -> pass)
    assertEquals(at.question.decisionId, "gathering.1.deal")
    assertEquals(deal(at).participants, Vector(p1, p2))
    assertEquals(deal(at).bystanders, Vector(p3))
    assertEquals(deal(at).passed, Vector(p3))
    assertEquals(at.question.owners, Vector(p1, p2, p3))
    assert(refused(at, p2, pass))

  test("the Gathering ends once every player at the site has passed"):
    val walked = walkAnsweringAs(together.ready, alone, played,
      p1 -> pass, p2 -> pass, p3 -> pass)
    assertEquals(walked.asked.map(_.decisionId),
      Vector.fill(3)("gathering.open"))
    assertEquals(negotiationLines(walked.events), Vector.empty)

  test("a secret and an adviser change hands"):
    val ready = together.secrets(p2, faceUp = 1).adviser(p2, BirdsongCard)
      .ready
    val walked = walkAnsweringAs(ready, alone, played, p1 -> open(p2),
      p2 -> giving(p1, secrets = 1, advisers = Vector(birdsong)),
      p1 -> accept, p2 -> accept, p1 -> pass, p2 -> pass, p3 -> pass)
    val (before, end) = (Look(ready), Look(walked.end))
    assertEquals(end.faceUpSecrets(p1), before.faceUpSecrets(p1) + 1)
    assertEquals(end.faceUpSecrets(p2), 0)
    assert(walked.end.game.current.player(p1).get.advisers
      .exists(_.id == birdsong))
    assertEquals(negotiationLines(walked.events).slice(1, 3), Vector(
      "p2 gave 1 secret to p1", s"p2 gave ${birdsong.value} to p1"))

  test("a disclosure settles, and the Grand Scepter's Citizenship offer " +
      "is open"):
    val ready = together.imperial(p1).relic(p1, TheGrandScepterCard)
      .boardWarbands(p2, 2).allowingCitizenshipPowers
      .adviser(p2, BirdsongCard, facedown = true).ready
    val at = parkAfterAs(ready, alone, played, p1 -> open(p2))
    assertEquals(deal(at).bounds(p1).citizenship, Vector(p2))
    val walked = walkAnsweringAs(ready, alone, played, p1 -> open(p2),
      p2 -> DecisionAnswer.ProposeTerms(NegotiationTerms(Vector.empty,
        Vector(NegotiationDisclosure(p1,
          NegotiationDisclosureRef.Adviser(p2, birdsong))))),
      p1 -> accept, p2 -> accept, p1 -> pass, p2 -> pass, p3 -> pass)
    assert(negotiationLines(walked.events)
      .contains(s"p2 showed p1 ${birdsong.value}"),
      negotiationLines(walked.events))

  test("The Tribunal at the site opens binding promises, and they are " +
      "closed without it"):
    val tribunal = together.edifice(TheTribunalCard, EdificeSide.Intact,
      at = homeSite).ready
    val opened = parkAfterAs(tribunal, alone, played, p1 -> open(p2))
    assert(deal(opened).bounds.values.forall(_.bindingPromises),
      deal(opened).bounds)
    val closed = parkAfterAs(together.ready, alone, played, p1 -> open(p2))
    assert(deal(closed).bounds.values.forall(!_.bindingPromises),
      deal(closed).bounds)

  test("a player elsewhere is named through Traveling Negotiator, and " +
      "refused without it or under City Wall"):
    val reaching = p3Away.adviser(p3, TravelingNegotiatorCard.id).ready
    val asked = parkAfterAs(reaching, alone, played, p3 -> stay)
    assertEquals(openQuestion(asked).present, Vector(p1, p2))
    assertEquals(openQuestion(asked).reach(p1), Vector(p2, p3))
    val dealt = parkAfterAs(reaching, alone, played, p3 -> stay,
      p1 -> open(p3))
    assertEquals(deal(dealt).participants, Vector(p1, p3))
    assertEquals(deal(dealt).bystanders, Vector(p2))
    // Without the card, p3 is out of reach.
    val unreached = parkAfterAs(p3Away.ready, alone, played, p3 -> stay)
    assertEquals(openQuestion(unreached).reach(p1), Vector(p2))
    assert(refused(unreached, p1, open(p3)))
    // City Wall, ruled by p2 at p3's site, stops p3's use of the card
    // (rulings 24 and 25), as the walk's restrictions judge it.
    val walledPowers = powersBeside(power, CityWall)
    val walled = p3Away.adviser(p3, TravelingNegotiatorCard.id)
      .denizen(CityWallCard.id, at = awaySite).warbandsAt(awaySite, p2, 1)
      .ready
    val blocked = parkAfterAs(walled, walledPowers, played, p3 -> stay)
    assertEquals(openQuestion(blocked).reach(p1), Vector(p2))
