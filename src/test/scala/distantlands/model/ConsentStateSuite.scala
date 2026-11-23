package distantlands.model

import distantlands.testkit.Unwrap.accepted

/** The consent ledger's transitions (consent design, "State" and rulings
  * 9, 12, 14, 15 and 17). */
class ConsentStateSuite extends munit.FunSuite:
  private val red = PlayerId("red")
  private val blue = PlayerId("blue")
  private val green = PlayerId("green")
  private val pass = PowerId("site.narrow-pass.pass")
  private val police = PowerId("denizen.secret-police")
  private val woods = DecisionOptionRef.Site(SiteId("site:deep-woods"))
  private val turn = ConsentTurn(1, blue)

  private def request(asks: ConsentAsk*): ConsentRequest =
    ConsentRequest(blue, "travel.destination", woods, asks.toVector)
  private def key(power: PowerId): ConsentKey =
    ConsentKey(power, blue, "travel.destination", woods)
  private def waitingOn(asks: ConsentAsk*): ConsentState =
    accepted(ConsentState().requested(request(asks*)))
  private val redOnPass = ConsentAsk(red, Vector(pass))
  private val greenOnPolice = ConsentAsk(green, Vector(police))

  test("a grant from the only ask is remembered for the turn and ends the request"):
    val granted = accepted(waitingOn(redOnPass).answered(red, granted = true,
      turn))
    assertEquals(granted.pending, None)
    assertEquals(granted.remembered(key(pass), turn),
      Some(ConsentAnswer.Granted))

  test("a refusal settles the request at once and refuses every key"):
    val refused = accepted(waitingOn(redOnPass, greenOnPolice)
      .answered(green, granted = false, turn))
    assertEquals(refused.pending, None)
    assertEquals(refused.remembered(key(pass), turn),
      Some(ConsentAnswer.Refused))
    assertEquals(refused.remembered(key(police), turn),
      Some(ConsentAnswer.Refused))

  test("a granted request remembers each key with its ask's granter, and " +
      "the answer counts only for that granter"):
    val granted = accepted(accepted(waitingOn(redOnPass, greenOnPolice)
      .answered(red, granted = true, turn)).answered(green, granted = true,
      turn))
    assertEquals(granted.answers.get(key(pass)),
      Some(ConsentAnswer.Granted.by(red)))
    assertEquals(granted.answers.get(key(police)),
      Some(ConsentAnswer.Granted.by(green)))
    assertEquals(granted.answer(key(pass), Some(red), turn),
      Some(ConsentAnswer.Granted))
    assertEquals(granted.answer(key(pass), Some(green), turn), None)
    assertEquals(granted.answer(key(pass), None, turn), None)

  test("a refusal is the refuser's, for every key: it lapses once they " +
      "may no longer answer, whoever was asked about the key"):
    val refused = accepted(waitingOn(redOnPass, greenOnPolice)
      .answered(green, granted = false, turn))
    assertEquals(refused.answers.get(key(pass)),
      Some(ConsentAnswer.Refused.by(green)))
    assertEquals(refused.answer(key(police), Some(green), turn),
      Some(ConsentAnswer.Refused))
    // Red never refused the Pass, so it is not refused of red.
    assertEquals(refused.answer(key(pass), Some(red), turn), None)

  test("a request covering a power none of its asks holds is refused"):
    val stray = request(redOnPass).copy(covers = Vector(key(police)))
    assert(ConsentState().requested(stray).isLeft)

  test("a supposed grant counts for any granter, nobody included"):
    val supposed = ConsentState().granting(Vector(key(pass)), turn)
    assertEquals(supposed.answer(key(pass), None, turn),
      Some(ConsentAnswer.Granted))
    assertEquals(supposed.answer(key(pass), Some(red), turn),
      Some(ConsentAnswer.Granted))

  test("a grant with another ask still open keeps the request waiting"):
    val partial = accepted(waitingOn(redOnPass, greenOnPolice)
      .answered(red, granted = true, turn))
    assertEquals(partial.pending.map(_.open.map(_.granter)),
      Some(Vector(green)))
    assertEquals(partial.remembered(key(pass), turn), None)

  test("answers expire with their turn, and the next turn's replace them"):
    val granted = accepted(waitingOn(redOnPass).answered(red, granted = true,
      turn))
    val later = ConsentTurn(1, red)
    assertEquals(granted.remembered(key(pass), later), None)
    val next = accepted(accepted(granted.requested(request(greenOnPolice)))
      .answered(green, granted = false, later))
    assertEquals(next.answers.keySet, Set(key(police)))

  test("a settled request joins the turn's history with its asks as " +
      "answered, granted or refused"):
    val granted = accepted(waitingOn(redOnPass).answered(red, granted = true,
      turn))
    assertEquals(granted.historyAt(turn),
      Vector(request(redOnPass.copy(answer = Some(true)))))
    val refused = accepted(waitingOn(redOnPass, greenOnPolice)
      .answered(red, granted = false, turn))
    assertEquals(refused.historyAt(turn), Vector(request(
      redOnPass.copy(answer = Some(false)), greenOnPolice)))

  test("a request joins the history only once it settles, and a " +
      "withdrawn one that nobody refused joins nothing"):
    val partial = accepted(waitingOn(redOnPass, greenOnPolice)
      .answered(red, granted = true, turn))
    assertEquals(partial.historyAt(turn), Vector.empty)
    assertEquals(accepted(partial.withdrawn(blue)).historyAt(turn),
      Vector.empty)

  test("a withdrawn request leaves its refused alternative in the " +
      "history, as that refuser's to change (R14)"):
    val redOrGreen = waitingOn(redOnPass.copy(alternative = true),
      greenOnPolice.copy(alternative = true))
    val refused = accepted(redOrGreen.answered(red, granted = false, turn))
    assertEquals(refused.historyAt(turn), Vector.empty)
    val withdrawn = accepted(refused.withdrawn(blue))
    assertEquals(withdrawn.historyAt(turn), Vector(request(
      redOnPass.copy(alternative = true, answer = Some(false)))))
    val changed = accepted(withdrawn.reconsidered(red, blue,
      "travel.destination", woods, granted = true, turn))
    assertEquals(changed.answers.get(key(pass)),
      Some(ConsentAnswer.Granted.by(red)))

  test("an alternative's refusal in a new turn starts the history " +
      "empty, though its request still waits"):
    val earlier = accepted(waitingOn(redOnPass).answered(red, granted = true,
      turn))
    val later = ConsentTurn(1, red)
    val waiting = accepted(earlier.requested(request(
      redOnPass.copy(alternative = true),
      greenOnPolice.copy(alternative = true))))
    val refused = accepted(waiting.answered(red, granted = false, later))
    assert(refused.pending.nonEmpty)
    assertEquals(refused.historyAt(later), Vector.empty)

  test("the history belongs to its turn: another turn reads none, and a " +
      "request settling in a new turn starts it empty"):
    val granted = accepted(waitingOn(redOnPass).answered(red, granted = true,
      turn))
    val later = ConsentTurn(1, red)
    assertEquals(granted.historyAt(later), Vector.empty)
    val next = accepted(accepted(granted.requested(request(greenOnPolice)))
      .answered(green, granted = false, later))
    assertEquals(next.historyAt(later),
      Vector(request(greenOnPolice.copy(answer = Some(false)))))

  test("a later request about the same option takes over the asks its " +
      "granters answered before, and leaves another granter's entry"):
    val both = accepted(accepted(waitingOn(redOnPass, greenOnPolice)
      .answered(red, granted = true, turn)).answered(green, granted = true,
      turn))
    val again = accepted(accepted(both.requested(request(greenOnPolice)))
      .answered(green, granted = false, turn))
    assertEquals(again.historyAt(turn), Vector(
      request(redOnPass.copy(answer = Some(true))),
      request(greenOnPolice.copy(answer = Some(false)))))
    val alone = accepted(accepted(again.requested(request(redOnPass)))
      .answered(red, granted = false, turn))
    assertEquals(alone.historyAt(turn), Vector(
      request(greenOnPolice.copy(answer = Some(false))),
      request(redOnPass.copy(answer = Some(false)))))

  test("an undo or a redo carries the history with the answers"):
    val granted = accepted(waitingOn(redOnPass).answered(red, granted = true,
      turn))
    assertEquals(granted.carried.historyAt(turn), granted.historyAt(turn))

  private val destination = "travel.destination"
  private def refusedByRedAlone: ConsentState =
    accepted(waitingOn(redOnPass).answered(red, granted = false, turn))
  private def grantedByRedAndGreen: ConsentState =
    accepted(accepted(waitingOn(redOnPass, greenOnPolice)
      .answered(red, granted = true, turn)).answered(green, granted = true,
      turn))

  test("a refusal changed to a grant is remembered as the changer's " +
      "grant, in the answers and in the history"):
    val changed = accepted(refusedByRedAlone.reconsidered(red, blue,
      destination, woods, granted = true, turn))
    assertEquals(changed.answer(key(pass), Some(red), turn),
      Some(ConsentAnswer.Granted))
    assertEquals(changed.historyAt(turn),
      Vector(request(redOnPass.copy(answer = Some(true)))))
    assert(changed.grantedTo(blue, destination, woods, turn))

  test("a grant changed to a refusal rewrites the changer's own keys " +
      "only"):
    val changed = accepted(grantedByRedAndGreen.reconsidered(red, blue,
      destination, woods, granted = false, turn))
    assertEquals(changed.answers.get(key(pass)),
      Some(ConsentAnswer.Refused.by(red)))
    assertEquals(changed.answers.get(key(police)),
      Some(ConsentAnswer.Granted.by(green)))
    assertEquals(changed.historyAt(turn).map(_.asks.map(_.answer)),
      Vector(Vector(Some(false), Some(true))))
    assert(!changed.grantedTo(blue, destination, woods, turn))

  test("with several granters, a change to a grant takes the changer's " +
      "refusal off the other granter's keys, so only that granter is " +
      "still to ask"):
    val refused = accepted(waitingOn(redOnPass, greenOnPolice)
      .answered(red, granted = false, turn))
    assertEquals(refused.answers.get(key(police)),
      Some(ConsentAnswer.Refused.by(red)))
    val changed = accepted(refused.reconsidered(red, blue, destination,
      woods, granted = true, turn))
    assertEquals(changed.answers.get(key(pass)),
      Some(ConsentAnswer.Granted.by(red)))
    assertEquals(changed.answers.get(key(police)), None)
    assertEquals(changed.historyAt(turn).map(_.asks.map(_.answer)),
      Vector(Vector(Some(true), None)))
    assert(!changed.grantedTo(blue, destination, woods, turn))

  test("a change that makes the request granted remembers a co-granter's " +
      "grant that the refusal had kept back"):
    val refused = accepted(accepted(waitingOn(redOnPass, greenOnPolice)
      .answered(green, granted = true, turn)).answered(red, granted = false,
      turn))
    assertEquals(refused.answers.get(key(police)),
      Some(ConsentAnswer.Refused.by(red)))
    val changed = accepted(refused.reconsidered(red, blue, destination,
      woods, granted = true, turn))
    assertEquals(changed.answers.get(key(pass)),
      Some(ConsentAnswer.Granted.by(red)))
    assertEquals(changed.answers.get(key(police)),
      Some(ConsentAnswer.Granted.by(green)))
    assert(changed.grantedTo(blue, destination, woods, turn))
    // The remembered grant is Green's own to change (R9).
    val greenRefuses = accepted(changed.reconsidered(green, blue,
      destination, woods, granted = false, turn))
    assertEquals(greenRefuses.answers.get(key(police)),
      Some(ConsentAnswer.Refused.by(green)))
    assertEquals(greenRefuses.answers.get(key(pass)),
      Some(ConsentAnswer.Granted.by(red)))

  test("a change leaves the preferences and the waiting request alone"):
    val set = refusedByRedAlone.preferenceSet(red, pass, blue,
      ConsentSetting.Never)
    val changed = accepted(set.reconsidered(red, blue, destination, woods,
      granted = true, turn))
    assertEquals(changed.preferences, set.preferences)
    assertEquals(changed.pending, set.pending)

  test("only the granter who answered changes an entry, for that " +
      "requester, decision and option, and nothing is found once the " +
      "turn has ended"):
    val refused = refusedByRedAlone
    val other = DecisionOptionRef.Site(SiteId("site:dunes"))
    assert(refused.reconsidered(green, blue, destination, woods,
      granted = true, turn).isLeft)
    assert(refused.reconsidered(red, green, destination, woods,
      granted = true, turn).isLeft)
    assert(refused.reconsidered(red, blue, "campaign.targets", woods,
      granted = true, turn).isLeft)
    assert(refused.reconsidered(red, blue, destination, other,
      granted = true, turn).isLeft)
    assert(refused.reconsidered(red, blue, destination, woods,
      granted = true, ConsentTurn(1, red)).isLeft)
    assertEquals(refused.asked(red, blue, destination, woods,
      ConsentTurn(1, red)), None)

  test("a granter whose ask closed unanswered has no entry to change"):
    val refused = accepted(waitingOn(redOnPass, greenOnPolice)
      .answered(red, granted = false, turn))
    assertEquals(refused.asked(green, blue, destination, woods, turn), None)

  test("after a later request took over a granter's ask, a change " +
      "reaches the later entry"):
    val first = accepted(waitingOn(redOnPass).answered(red, granted = true,
      turn))
    val second = accepted(accepted(first.requested(request(redOnPass)))
      .answered(red, granted = false, turn))
    val changed = accepted(second.reconsidered(red, blue, destination,
      woods, granted = true, turn))
    assertEquals(changed.historyAt(turn),
      Vector(request(redOnPass.copy(answer = Some(true)))))

  test("only an asked granter answers, and only the requester withdraws"):
    val open = waitingOn(redOnPass)
    assert(open.answered(green, granted = true, turn).isLeft)
    assert(open.withdrawn(red).isLeft)
    assertEquals(accepted(open.withdrawn(blue)), ConsentState())

  test("a second request while one waits is refused, as is one with no ask"):
    assert(waitingOn(redOnPass).requested(request(greenOnPolice)).isLeft)
    assert(ConsentState().requested(request()).isLeft)

  test("a preference is set, replaced, removed by case by case, and " +
      "cleared per granter and power"):
    val always = ConsentState().preferenceSet(red, pass, blue,
      ConsentSetting.Always)
    assertEquals(always.preference(red, pass, blue),
      Some(ConsentPreference.Always))
    val never = always.preferenceSet(red, pass, blue, ConsentSetting.Never)
    assertEquals(never.preference(red, pass, blue),
      Some(ConsentPreference.Never))
    assertEquals(never.preferenceSet(red, pass, blue,
      ConsentSetting.CaseByCase), ConsentState())
    val three = never.preferenceSet(red, pass, green, ConsentSetting.Always)
      .preferenceSet(red, police, blue, ConsentSetting.Always)
    assertEquals(three.cleared(red, pass).preferences.keySet,
      Set((red, police, blue)))

  test("what an undo carries keeps answers and preferences, not a request"):
    val open = waitingOn(redOnPass).preferenceSet(red, pass, green,
      ConsentSetting.Always)
    assertEquals(open.carried, open.copy(pending = None))

  test("a choice's grant and the preference it sets"):
    assertEquals(ConsentChoice.values.toVector.map(choice =>
      (choice.key, choice.granted, choice.setting)), Vector(
      ("never", false, Some(ConsentSetting.Never)),
      ("no", false, None),
      ("yes", true, None),
      ("always", true, Some(ConsentSetting.Always))))
    assertEquals(ConsentSetting.fromKey("case-by-case"),
      Some(ConsentSetting.CaseByCase))
    assertEquals(ConsentChoice.fromKey("maybe"), None)

  private val payer = PowerId("test.payer")
  private val ruler = PowerId("test.ruler")

  /** Blue asks red or green about the two ways past a toll on the woods:
    * red agreeing to pay, or green waiving it. */
  private def eitherRedOrGreen: ConsentRequest =
    request(ConsentAsk(red, Vector(payer), alternative = true),
      ConsentAsk(green, Vector(ruler), alternative = true))
      .copy(covers = Vector(key(payer), key(ruler)))

  test("one alternative's grant settles the request, granting only its " +
      "keys"):
    val asked = accepted(ConsentState().requested(eitherRedOrGreen))
    val done = accepted(asked.answered(green, granted = true, turn))
    assertEquals(done.pending, None)
    assertEquals(done.answer(key(ruler), Some(green), turn),
      Some(ConsentAnswer.Granted))
    assertEquals(done.answer(key(payer), Some(red), turn), None)

  test("one alternative's refusal leaves the others asked"):
    val asked = accepted(ConsentState().requested(eitherRedOrGreen))
    val refused = accepted(asked.answered(red, granted = false, turn))
    assertEquals(refused.pending.map(_.open.map(_.granter)),
      Some(Vector(green)))
    assertEquals(refused.answer(key(payer), Some(red), turn),
      Some(ConsentAnswer.Refused))

  test("every alternative refused settles it refused"):
    val asked = accepted(ConsentState().requested(eitherRedOrGreen))
    val done = for
      one <- asked.answered(red, granted = false, turn)
      both <- one.answered(green, granted = false, turn)
    yield both
    assertEquals(done.map(_.pending), Right(None))
    assertEquals(done.toOption.flatMap(_.answer(key(ruler), Some(green),
      turn)), Some(ConsentAnswer.Refused))

  test("a required ask still has to grant beside the alternatives"):
    val yellow = PlayerId("yellow")
    val withPass = eitherRedOrGreen.copy(
      asks = ConsentAsk(yellow, Vector(pass)) +: eitherRedOrGreen.asks,
      covers = eitherRedOrGreen.covers :+ key(pass))
    val asked = accepted(ConsentState().requested(withPass))
    val one = accepted(asked.answered(red, granted = true, turn))
    assertEquals(one.pending.map(_.open.map(_.granter)), Some(Vector(yellow)))

  test("an alternative may not share a required ask's granter"):
    val clash = eitherRedOrGreen.copy(asks =
      ConsentAsk(red, Vector(pass)) +: eitherRedOrGreen.asks)
    assert(ConsentState().requested(clash).isLeft)

  test("a required grant is not remembered while alternatives are open, " +
      "nor once the request is withdrawn"):
    val yellow = PlayerId("yellow")
    val withPass = eitherRedOrGreen.copy(
      asks = ConsentAsk(yellow, Vector(pass)) +: eitherRedOrGreen.asks,
      covers = eitherRedOrGreen.covers :+ key(pass))
    val asked = accepted(ConsentState().requested(withPass))
    val passed = accepted(asked.answered(yellow, granted = true, turn))
    val refused = accepted(passed.answered(red, granted = false, turn))
    assertEquals(refused.answer(key(pass), Some(yellow), turn), None)
    assertEquals(refused.answer(key(payer), Some(red), turn),
      Some(ConsentAnswer.Refused))
    val withdrawn = accepted(refused.withdrawn(blue))
    assertEquals(withdrawn.answer(key(pass), Some(yellow), turn), None)

  test("a turn renamed by a round advance keeps its answers; another " +
      "turn's are left alone"):
    val refused = accepted(waitingOn(redOnPass).answered(red,
      granted = false, turn))
    val later = ConsentTurn(2, blue)
    assertEquals(refused.turnRenamed(turn, later).remembered(key(pass), later),
      Some(ConsentAnswer.Refused))
    assertEquals(refused.turnRenamed(ConsentTurn(1, red), later)
      .remembered(key(pass), later), None)
