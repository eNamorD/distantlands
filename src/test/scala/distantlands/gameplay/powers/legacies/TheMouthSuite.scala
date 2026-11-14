package distantlands.gameplay.powers.legacies

import distantlands.gameplay.actions.negotiation.HighPriestCard
import distantlands.gameplay.BanditCrownCard
import distantlands.gameplay.cards.AncientCitySite
import distantlands.gameplay.chronicle.legacies.{IronHand, IronHandCard}
import distantlands.gameplay.operations.TheGrandScepterCard
import distantlands.gameplay.phases.rest.ArbiterCard
import distantlands.gameplay.powers.NoteText
import distantlands.gameplay.powers.campaign.StickyFireCard
import distantlands.gameplay.powers.economy.{AnimalPlaymatesCard,
  BelovedCard, BirdsongCard, DefameCard}
import distantlands.gameplay.setup.legacies.TheStandardBearerCard
import distantlands.gameplay.setup.FirstGameSetupFixture.catalog
import distantlands.model._
import distantlands.testkit.{CardRefs, Look, PhasePowerHarness, Table}
import distantlands.testkit.CardRefs.LegacyRef
import distantlands.testkit.Table.{p1, p2}
import distantlands.testkit.Unwrap.accepted

/** The Mouth (L34): "ACTION: Burn 2 favor to activate a dormant legacy if
  * you meet its goal. You can even activate legacies in the Public
  * Ambitions foundation." (Legacy in-play powers rulings 1 and 23-24;
  * implementation rulings R25, R27 and R30.) */
class TheMouthSuite extends munit.FunSuite:
  import PhasePowerHarness._

  private val source = DecisionOptionRef.Legacy(TheMouthCard.id)
  private def legacy(card: LegacyRef): DecisionOptionRef =
    DecisionOptionRef.Legacy(CardRefs.legacyId(card))

  /** p1, the Chancellor in their Act at Ancient City with 2 favor, holds
    * The Mouth active, the Grand Scepter and Sticky Fire, the highest
    * relic, which meets Ruthless's goal; p1 also holds the Darkest Secret,
    * which meets Arbiter's. Ruthless and Arbiter are p1's dormant legacies. Defame
    * stands at Ancient City, for Ruthless. The Public Ambitions row holds
    * Golden Key, whose goal the Scepter meets, and The Standard Bearer,
    * whose People's Favor nobody holds. */
  private def mouthHolder: Table = Table.start.imperial(p1)
    .turn(p1, Phase.Act).pawn(p1, at = AncientCitySite).favor(p1, 2)
    .relic(p1, TheGrandScepterCard).relic(p1, StickyFireCard)
    .legacy(p1, TheMouthCard).legacy(p1, RuthlessCard, active = false)
    .legacy(p1, ArbiterCard, active = false)
    .darkestSecret(Some(p1), secrets = 1)
    .denizen(DefameCard, AncientCitySite)
    .publicLegacies(GoldenKeyCard, TheStandardBearerCard)

  private def legacies(ready: ReadyGame): Vector[LegacyState] =
    ready.game.campaign.lineages(Look(ready).player(p1).lineage).legacies

  private def offered(ready: ReadyGame): Vector[DecisionOptionRef] =
    TheMouth.targets(catalog, ready, p1).map(DecisionOptionRef.Legacy(_))

  private def lines(events: Vector[OathEvent]): Vector[String] =
    NoteText.said(TheMouth, events).map(_.text)

  test("it offers the holder's dormant legacies whose goal they meet, then " +
      "the Public Ambitions legacies, and burns 2 favor"):
    val ready = mouthHolder.ready
    assertEquals(offered(ready), Vector(legacy(RuthlessCard),
      legacy(ArbiterCard), legacy(GoldenKeyCard)))
    val done = useAndAnswer(ready, TheMouth, source, TheMouth.decisionId,
      legacy(ArbiterCard))
    assertEquals(Look(after(done.answered)).favor(p1), 0)

  test("an own dormant legacy flips faceup and is in effect at once: the " +
      "activated Ruthless's ACTION is usable that turn"):
    // With 4 favor, 2 are left for Ruthless.
    val ready = mouthHolder.favor(p1, 4).ready
    val done = useAndAnswer(ready, TheMouth, source, TheMouth.decisionId,
      legacy(RuthlessCard))
    val now = after(done.answered)
    assert(legacies(now).contains(LegacyState(RuthlessCard.id, active = true)))
    assert(usableNow(now).exists(_.power.id == Ruthless.id))
    assertEquals(lines(done.answered.events), Vector(
      s"p1 activated ${RuthlessCard.id.value} with ${TheMouthCard.id.value}"))
    assertEquals(replayed(ready, done.events), Right(done.answered.state))

  test("a Public Ambitions legacy moves faceup into the lineage, and the row " +
      "no longer holds it"):
    val ready = mouthHolder.ready
    val done = useAndAnswer(ready, TheMouth, source, TheMouth.decisionId,
      legacy(GoldenKeyCard))
    val now = after(done.answered)
    assert(legacies(now).contains(LegacyState(GoldenKeyCard.id, active = true)))
    assertEquals(now.game.current.commonCards.publicLegacies,
      Vector(TheStandardBearerCard.id))
    assertEquals(lines(done.answered.events), Vector(
      s"p1 took ${GoldenKeyCard.id.value} from the Public Ambitions with " +
        TheMouthCard.id.value))

  test("a legacy that alters a foundation alters it now, and the line says " +
      "so"):
    // p1 advises Animal Playmates, the only Beast card, so meets dormant
    // Beloved's goal; Beloved names Foundation IV.
    val ready = mouthHolder.legacy(p1, BelovedCard, active = false)
      .adviser(p1, AnimalPlaymatesCard).ready
    val done = useAndAnswer(ready, TheMouth, source, TheMouth.decisionId,
      legacy(BelovedCard))
    val now = after(done.answered)
    assert(now.game.campaign.foundations(FoundationNumber.IV)
      .alterationSources.contains(BelovedCard.id))
    assertEquals(lines(done.answered.events).last.takeWhile(_ != ' '),
      BelovedCard.id.value)
    assertEquals(lines(done.answered.events).size, 2)

  test("with four active legacies the holder discards to three, choosing, " +
      "and no foundation flips back"):
    val ready = mouthHolder.legacy(p1, GoldenKeyCard)
      .legacy(p1, HighPriestCard).publicLegacies(TheStandardBearerCard).ready
    val done = useAndAnswer(ready, TheMouth, source, TheMouth.decisionId,
      legacy(ArbiterCard))
    assert(awaits(done.answered, TheMouth.capDecisionId(p1)))
    val capped = accepted(answer(done.answered, p1,
      TheMouth.capDecisionId(p1), DecisionAnswer.ChooseManyAnswer(
        Vector(legacy(HighPriestCard)))))
    val now = after(capped)
    assertEquals(legacies(now).filter(_.active).map(_.id).toSet,
      Set(TheMouthCard.id, GoldenKeyCard.id, ArbiterCard.id))
    assertEquals(lines(capped.events), Vector(
      s"p1 discarded ${HighPriestCard.id.value} with The Mouth"))

  test("a walk resumed at the cap keeps the chosen legacy offered and the " +
      "activation's one line, and the journal replays"):
    val ready = mouthHolder.legacy(p1, GoldenKeyCard)
      .legacy(p1, HighPriestCard).publicLegacies(TheStandardBearerCard).ready
    val done = useAndAnswer(ready, TheMouth, source, TheMouth.decisionId,
      legacy(ArbiterCard))
    val resumed = accepted(replayed(ready, done.events))
    val capped = accepted(rules.resolveWalker(resumed, p1,
      TheMouth.capDecisionId(p1), DecisionAnswer.ChooseManyAnswer(
        Vector(legacy(GoldenKeyCard)))))
    val events = done.events ++ capped.events
    assertEquals(ops(events).count(_.isInstanceOf[Flip]), 1)
    assertEquals(lines(events).count(_.contains("activated")), 1)
    assertEquals(replayed(ready, events), Right(capped.state))

  test("an Imperial holder may activate Iron Hand, which the same batch " +
      "discards, with Iron Hand's line (R25)"):
    // p1's Imperial warband at Ancient City is the only site anyone rules,
    // which meets dormant Iron Hand's goal.
    val ready = mouthHolder.legacy(p1, IronHandCard, active = false)
      .warbandsAt(AncientCitySite, p1, 1).ready
    val done = useAndAnswer(ready, TheMouth, source, TheMouth.decisionId,
      legacy(IronHandCard))
    val now = after(done.answered)
    assert(!legacies(now).exists(_.id == IronHandCard.id))
    assertEquals(NoteText.said(IronHand.id, IronHand.noteKeys,
      done.answered.events).map(_.text),
      Vector(s"p1 discarded ${IronHandCard.id.value}"))

  test("with no legacy whose goal they meet it is shown unavailable with " +
      "its reason (R30)"):
    val bare = Table.start.imperial(p1).turn(p1, Phase.Act)
      .pawn(p1, at = AncientCitySite).favor(p1, 2)
      .legacy(p1, TheMouthCard).legacy(p1, ArbiterCard, active = false).ready
    assert(!usableNow(bare).exists(_.power.id == TheMouth.id))
    assertEquals(use(bare, TheMouth, source).left.toOption, Some(
      OathViolation.StartUnavailable("No legacy whose goal you meet")))

  test("a dormant Mouth offers nothing (ruling 1)"):
    val dormant = Table.start.imperial(p1).turn(p1, Phase.Act).favor(p1, 2)
      .relic(p1, TheGrandScepterCard).legacy(p1, TheMouthCard, active = false)
      .legacy(p1, RuthlessCard, active = false).ready
    assert(!usableNow(dormant).exists(_.power.id == TheMouth.id))

  test("another player's facedown adviser changes no target: a facedown " +
      "card has no suit (R31)"):
    // p1 advises Animal Playmates faceup, the only faceup Beast card, and
    // holds Beloved dormant; p2's facedown Birdsong, a Beast card, counts
    // for nobody.
    val base = mouthHolder.legacy(p1, BelovedCard, active = false)
      .adviser(p1, AnimalPlaymatesCard)
    val hidden = base.adviser(p2, BirdsongCard, facedown = true).ready
    assert(offered(base.ready).contains(legacy(BelovedCard)))
    assertEquals(offered(hidden), offered(base.ready))

  test("another player's facedown relic changes no target or reason (R31)"):
    // p1's only legacy to activate is dormant Conspirator, a highest-relic
    // goal p1 meets with Sticky Fire; p2's facedown Bandit Crown, worth
    // more, counts for nobody.
    val base = Table.start.imperial(p1).turn(p1, Phase.Act)
      .pawn(p1, at = AncientCitySite).favor(p1, 2)
      .relic(p1, StickyFireCard).legacy(p1, TheMouthCard)
      .legacy(p1, ConspiratorCard, active = false)
    val hidden = base.relic(p2, BanditCrownCard, facedown = true).ready
    assertEquals(offered(base.ready), Vector(legacy(ConspiratorCard)))
    assertEquals(offered(hidden), offered(base.ready))
    assert(use(hidden, TheMouth, source).isRight)
