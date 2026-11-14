package distantlands.application.gamelog

import distantlands.gameplay.cards.{AncientCitySite, BrokenPeaksSite}
import distantlands.gameplay.chronicle.legacies.{IronHandCard, TheEarCard}
import distantlands.gameplay.operations.TheGrandScepterCard
import distantlands.gameplay.phases.rest.{ArbiterCard, ArbiterStep}
import distantlands.gameplay.powers.action.GrandScepterAction
import distantlands.gameplay.powers.campaign.StickyFireCard
import distantlands.gameplay.powers.economy.DefameCard
import distantlands.gameplay.actions.negotiation.HighPriestCard
import distantlands.gameplay.powers.legacies.{ConspiratorCard,
  ConspiratorPrediction, TheMouth, TheMouthCard}
import distantlands.gameplay.setup.legacies.TheStandardBearerCard
import distantlands.gameplay.powers.legacies.{GoldenKey, GoldenKeyCard,
  HighPriest, Peacemaker, PeacemakerCard, Ruthless, RuthlessCard,
  TheEarAction}
import distantlands.gameplay.TravelPlay
import distantlands.gameplay.actions.travel.TravelProcedure
import distantlands.gameplay.powers.campaign.WrestlersCard
import distantlands.gameplay.powers.economy.BelovedCard
import distantlands.gameplay.powers.travel.AncestralLandsCard
import distantlands.gameplay.powers.wake.LightFingersCard
import distantlands.model._
import distantlands.testkit.{MenuPlay, RoundEndChoices, Step, Table}
import distantlands.testkit.Table.{p1, p2, p3}
import LogScriptSupport.{atTable, tableWithOnePawnPerSite}

/** The legacies' ACTIONs (Legacy in-play powers design, slice 1), one Act
  * of p1, the Chancellor: Ruthless buries Defame at Ancient City, Golden
  * Key takes a relic from the Reliquary, High Priest moves a secret from
  * the Darkest Secret, The Ear peeks at Broken Peaks' relic, and
  * Peacemaker returns the banner it is asked about, each question answered
  * by default. Reached through `GoldenScripts`. */
object LegacyActionLogScripts:
  def legacyActions(using munit.Location): Script =
    val table = tableWithOnePawnPerSite.imperial(p1).turn(p1, Phase.Act)
      .legacy(p1, RuthlessCard).legacy(p1, GoldenKeyCard)
      .legacy(p1, HighPriestCard).legacy(p1, TheEarCard)
      .legacy(p1, PeacemakerCard)
      .favor(p1, 3).secrets(p1, faceUp = 1).reliquaryFromDeck(2)
      .denizen(DefameCard, AncientCitySite)
      .relicAt(StickyFireCard, BrokenPeaksSite)
      .darkestSecret(Some(p1), 2).peoplesFavor(Some(p1), 1)
      .banditsRefilled
    val (service, act) = atTable("legacy-actions", table)
    def use(power: PowerId, card: LegacyId) = MenuPlay.start(p1,
      ActionRef.UsePower(power), Vector(DecisionOptionRef.Legacy(card)))
    act.after(use(Ruthless.id, RuthlessCard.id),
      use(GoldenKey.id, GoldenKeyCard.id),
      use(HighPriest.id, HighPriestCard.id),
      use(TheEarAction.id, TheEarCard.id),
      use(Peacemaker.id, PeacemakerCard.id))
    Script("legacy-actions", service, p1)

  /** The legacies' modifiers (slice 2), one turn of p1, the Chancellor,
    * holding Ancestral Lands, Light Fingers and Beloved active, at the
    * first Provinces site with p2's pawn beside them, the site holding 1
    * favor and p2 holding Wrestlers faceup: p1 takes the favor in Wake
    * (Light Fingers), trades for favor on Wrestlers (Beloved), then
    * travels to the first Cradle site for 1 Supply (Ancestral Lands). No
    * power writes a line of its own (R8). */
  def legacyModifiers(using munit.Location): Script =
    val base = Table.start
    val map = base.ready.game.current.map
    val province = map.provinces.head
    val cradle = map.cradle.head
    val table = base.imperial(p1).turn(p1, Phase.Wake)
      .pawn(p1, province).pawn(p2, province)
      .siteTokens(province, favor = 1)
      .adviser(p2, WrestlersCard).bankFavor(Suit.Order, 3)
      .legacy(p1, AncestralLandsCard).legacy(p1, LightFingersCard)
      .legacy(p1, BelovedCard).banditsRefilled
    val (service, waking) = atTable("legacy-modifiers", table)
    waking.withAnswers {
      case park if park.decisionId == TravelProcedure.destinationDecisionId =>
        TravelPlay.destination(cradle)
    }.after(
      MenuPlay.start(p1, ActionRef.TakeWealth,
        Vector(DecisionOptionRef.Button("favor"))),
      MenuPlay.start(p1, ActionRef.Trade,
        Vector(DecisionOptionRef.Button("favor"))),
      MenuPlay.start(p1, ActionRef.Travel))
    Script("legacy-modifiers", service, p1)

  /** The legacies' triggers and role powers (slice 3), the last turn of
    * round 5 in an Empire: p1, the Chancellor, holds the Oathkeeper title of
    * the People and the People's Favor, which keeps it there; p2, a Citizen, holds Arbiter; p3, an Exile at Broken Peaks
    * in their Act, the round's last seat, holds the Grand Scepter, Iron Hand
    * and 2 board warbands. p3 becomes a Citizen with the Scepter, which
    * ends their Act, and Rest runs on to the round's end, where p2 raises
    * the end die by 2: the 4 it rolls is a 6, and the Chancellor wins. The
    * Needle's line stays in `TheNeedleSuite`: taking a banner needs a
    * challenge or a raid with dice, which this turn does not hold. */
  def legacyTriggers(using munit.Location): Script =
    val table = Table.start.imperial(p1, p2)
      .oathkeeperGoal(OathkeeperGoal.ThePeople).oathkeeper(Some(p1))
      .peoplesFavor(Some(p1), favor = 1)
      .turn(p3, Phase.Act).pawn(p3, at = BrokenPeaksSite)
      .relic(p3, TheGrandScepterCard).boardWarbands(p3, 2)
      .legacy(p2, ArbiterCard).legacy(p3, IronHandCard)
      .allowingCitizenshipPowers
      // No Table step sets the round.
      .update(_.updateCurrent(c => c.copy(tracks =
        c.tracks.copy(round = 5, usurperLimited = false))))
      .banditsRefilled
    val (service, acting) = atTable("legacy-triggers", table,
      roundEnd = RoundEndChoices.endDie(4))
    acting.withAnswers {
      case park if park.decisionId == ArbiterStep.decisionId(p2) =>
        DecisionAnswer.ChooseOneAnswer(ArbiterStep.by(2))
    }.after(MenuPlay.start(p3, ActionRef.UsePower(GrandScepterAction.id),
      Vector(DecisionOptionRef.Relic(TheGrandScepterCard.id))))
    Script("legacy-triggers", service, p1)

  /** The legacies' activation and prediction (slice 4), from round 1 to
    * the game's end. p1, the Chancellor, holds the Oathkeeper title of
    * Devotion and the Darkest Secret; p2, a Citizen, holds the Grand
    * Scepter, Devotion's Successor goal; p3, an Exile at Broken Peaks and
    * the round's last seat, holds Conspirator and The Mouth active, The
    * Standard Bearer dormant, the People's Favor, which meets its goal, and
    * 2 favor. p2 rests; at p3's first Wake Conspirator writes down p2 and
    * round 5; in p3's Act The Mouth activates The Standard Bearer, a SETUP
    * power that does nothing now. An arrangement moves the round marker to
    * 5; p3 rests, the end die rolls 6, p2 wins as Successor, and the
    * prediction, revealed, makes p3 the winner. */
  def legacyPrediction(using munit.Location): Script =
    val table = Table.start.imperial(p1, p2)
      .oathkeeperGoal(OathkeeperGoal.Devotion).oathkeeper(Some(p1))
      .darkestSecret(Some(p1), secrets = 1).relic(p2, TheGrandScepterCard)
      .pawn(p3, at = BrokenPeaksSite).favor(p3, 2)
      .peoplesFavor(Some(p3), favor = 1)
      .legacy(p3, ConspiratorCard).legacy(p3, TheMouthCard)
      .legacy(p3, TheStandardBearerCard, active = false)
      .turn(p2, Phase.Act).banditsRefilled
    val (service, acting) = atTable("legacy-prediction", table,
      roundEnd = RoundEndChoices.endDie(6))
    acting.withAnswers {
      case park if park.decisionId == ConspiratorPrediction.roundDecisionId =>
        DecisionAnswer.ChooseAmountAnswer(5)
      case park if park.decisionId == ConspiratorPrediction.winnerDecisionId =>
        DecisionAnswer.ChooseOneAnswer(DecisionOptionRef.Player(p2))
      case park if park.decisionId == TheMouth.decisionId =>
        DecisionAnswer.ChooseOneAnswer(
          DecisionOptionRef.Legacy(TheStandardBearerCard.id))
    }.after(MenuPlay.starting(p2, PhaseTransitionRef.BeginRest))
      .endingWake(p3)
      .after(MenuPlay.start(p3, ActionRef.UsePower(TheMouth.id),
          Vector(DecisionOptionRef.Legacy(TheMouthCard.id))),
        Step.Arrange(Vector.fill(4)(AdvanceRound)),
        MenuPlay.starting(p3, PhaseTransitionRef.BeginRest))
    Script("legacy-prediction", service, p1)

  val named: Vector[(String, () => Script)] = Vector(
    "legacy-actions" -> (() => legacyActions),
    "legacy-modifiers" -> (() => legacyModifiers),
    "legacy-triggers" -> (() => legacyTriggers),
    "legacy-prediction" -> (() => legacyPrediction))
