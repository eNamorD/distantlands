package distantlands.application.gamelog

import distantlands.application.{ConsentFixture, GameAccepted,
  GameApplicationService, TollConsentFixture, TreeDecision}
import distantlands.gameplay.actions.travel.TravelProcedure
import distantlands.gameplay.cards.NarrowPassSite
import distantlands.gameplay.powers.PayOrAsk
import distantlands.gameplay.powers.travel.{TollRoads, TravelFixture}
import distantlands.model._
import distantlands.protocol.projection.{LogEntry, LogTakenBack}
import distantlands.testkit.Table.{p1, p2, p3}
import distantlands.testkit.Unwrap.{accepted, present}
import LogScripts.{formatter, texts}
import distantlands.testkit.Commands

/** What the Game Log says about consent, and to whom (consent design,
  * "Game Log"). */
class GameLogConsentSuite extends munit.FunSuite:
  import ConsentFixture._
  private val asked = "Asked p2 for consent to ignore Narrow Pass and " +
    "select Dunes."

  private def log(service: GameApplicationService, viewer: PlayerId)
      : Vector[LogEntry] =
    formatter.format(present(accepted(service.history(gameId)),
      "a stored game").steps, Some(viewer))

  private def asking(service: GameApplicationService)(using munit.Location)
      : GameAccepted =
    val started = startTravel(service)
    accepted(service.handle(gameId, started.nextSequence, requestDunes))

  private def answering(service: GameApplicationService, at: GameAccepted,
      choice: ConsentChoice)(using munit.Location): GameAccepted =
    accepted(service.handle(gameId, at.nextSequence,
      Commands.answerConsent(p2, choice)))

  test("the request and a grant read the same to every player"):
    val (service, _) = ConsentFixture.service()
    answering(service, asking(service), ConsentChoice.Yes)
    Vector(p1, p2, p3).foreach { viewer =>
      val lines = texts(log(service, viewer))
      assert(lines.contains(asked), lines)
      assert(lines.contains("p2 gave consent."), lines)
    }

  private def changing(service: GameApplicationService, at: GameAccepted,
      allow: Boolean)(using munit.Location): GameAccepted =
    accepted(service.handle(gameId, at.nextSequence,
      Commands.reconsiderConsent(p2, p1,
        TravelProcedure.destinationDecisionId, dunes, allow)))

  test("a changed answer is written for every player, in both " +
      "directions, and names the granter"):
    val (service, _) = ConsentFixture.service()
    val no = answering(service, asking(service), ConsentChoice.No)
    val granted = changing(service, no, allow = true)
    changing(service, granted, allow = false)
    Vector(p1, p2, p3).foreach { viewer =>
      val lines = texts(log(service, viewer))
      val gives = lines.indexOf(
        "p2 now gives consent to ignore Narrow Pass and select Dunes.")
      val withdraws = lines.indexOf("p2 no longer gives consent to " +
        "ignore Narrow Pass and select Dunes.")
      assert(gives >= 0, lines)
      assert(withdraws > gives, lines)
      assert(lines.indexOf("p2 gave no consent.") < gives, lines)
    }

  test("a changed answer words its option as its request's line did, " +
      "after the decision is gone"):
    val (service, _) = ConsentFixture.service(TollConsentFixture.tolled)
    val asked = accepted(service.handle(gameId,
      TollConsentFixture.atTheStep(service).nextSequence,
      TollConsentFixture.requestFree))
    val no = answering(service, asked, ConsentChoice.No)
    val paid = accepted(service.handle(gameId, no.nextSequence,
      Commands.resolveWalker(p1, TreeDecision(TollRoads.decisionId,
        DecisionAnswer.ChooseOneAnswer(PayOrAsk.pay)))))
    accepted(service.handle(gameId, paid.nextSequence,
      Commands.reconsiderConsent(p2, p1, TollRoads.decisionId,
        TollRoads.free(TravelFixture.coast), allow = true)))
    val lines = texts(log(service, p1))
    assert(lines.contains("Asked p2 for consent to ignore Toll Roads and " +
      "select Travel without paying."), lines)
    assert(lines.contains("p2 now gives consent to ignore Toll Roads and " +
      "select Travel without paying."), lines)

  test("with two rulers asked, a changed answer names the changer's own " +
      "power alone"):
    val (service, _) = ConsentFixture.service(dunesBehindTwoRulers)
    val no = answering(service, asking(service), ConsentChoice.No)
    changing(service, no, allow = true)
    val lines = texts(log(service, p3))
    assert(lines.contains("Asked p3 and p2 for consent to ignore " +
      "Forbidden Forest and Narrow Pass and select Dunes."), lines)
    assert(lines.contains(
      "p2 now gives consent to ignore Narrow Pass and select Dunes."), lines)

  test("a changed answer's line is never struck through by the " +
      "requester's undo"):
    val (service, _) = ConsentFixture.service()
    val yes = answering(service, asking(service), ConsentChoice.Yes)
    val undone = accepted(service.handle(gameId, yes.nextSequence,
      Commands.undo(p1)))
    val refused = changing(service, undone, allow = false)
    accepted(service.handle(gameId, refused.nextSequence,
      Commands.undo(p1)))
    val line = present(log(service, p1).find(entry =>
      entry.spans.map(_.text).mkString == "p2 no longer gives consent " +
        "to ignore Narrow Pass and select Dunes."), "the change's line")
    assertEquals(line.takenBack, None)

  test("a refusal names the ruler"):
    val (service, _) = ConsentFixture.service()
    answering(service, asking(service), ConsentChoice.Never)
    assert(texts(log(service, p1)).contains("p2 gave no consent."))

  test("a preference line is written for its granter alone"):
    val (service, _) = ConsentFixture.service()
    val always = accepted(service.handle(gameId, 0L,
      setPreference(ConsentSetting.Always)))
    val never = accepted(service.handle(gameId, always.nextSequence,
      setPreference(ConsentSetting.Never)))
    accepted(service.handle(gameId, never.nextSequence,
      setPreference(ConsentSetting.CaseByCase)))
    val ruler = texts(log(service, p2))
    Vector("p2 now always allows p1 to ignore Narrow Pass.",
      "p2 now never allows p1 to ignore Narrow Pass.",
      "p2 now decides case by case whether p1 may ignore Narrow Pass.")
      .foreach(line => assert(ruler.contains(line), ruler))
    Vector(p1, p3).foreach(viewer =>
      assert(!texts(log(service, viewer)).exists(_.contains("Narrow Pass")),
        texts(log(service, viewer))))

  test("a reset line is written for its granter alone"):
    val stale = TravelFixture.board().copy(consent = ConsentState()
      .preferenceSet(p2, NarrowPassSite.pass, p1, ConsentSetting.Always))
    val (service, _) = ConsentFixture.service(stale)
    startTravel(service)
    val reset = "p2's consent preferences for Narrow Pass were reset."
    assert(texts(log(service, p2)).contains(reset))
    assert(!texts(log(service, p1)).contains(reset))

  test("a withdrawn request's line is struck through, under no header"):
    val (service, _) = ConsentFixture.service()
    val waiting = asking(service)
    accepted(service.handle(gameId, waiting.nextSequence,
      Commands.undo(p1)))
    val entry = present(log(service, p1).find(LogScripts.text(_) == asked),
      "the request line")
    assertEquals(entry.takenBack,
      Some(LogTakenBack(entry.sequence, Vector.empty)))

  test("the requester's log cannot tell an automatic grant from a manual " +
      "one"):
    val (manual, _) = ConsentFixture.service()
    answering(manual, asking(manual), ConsentChoice.Yes)
    val (automatic, _) = ConsentFixture.service()
    val set = accepted(automatic.handle(gameId, 0L,
      setPreference(ConsentSetting.Always)))
    val started = startTravel(automatic, set.nextSequence)
    accepted(automatic.handle(gameId, started.nextSequence, requestDunes))
    assertEquals(texts(log(automatic, p1)), texts(log(manual, p1)))

  test("a request from a button names it by its label"):
    val (service, _) = ConsentFixture.service(TollConsentFixture.tolled)
    accepted(service.handle(gameId,
      TollConsentFixture.atTheStep(service).nextSequence,
      TollConsentFixture.requestFree))
    assert(texts(log(service, p1)).contains("Asked p2 for consent to ignore " +
      "Toll Roads and select Travel without paying."), texts(log(service, p1)))
