package distantlands.application.gamelog

import distantlands.catalog.holding.{SeerCard, TheNeedleCard}
import distantlands.gameplay.cards.{AncientCitySite, BrokenPeaksSite,
  BuriedGiantSite, DeepWoodsSite}
import distantlands.gameplay.chronicle.{LineageScoring, OfferCitizenship,
  ShapeFoundation}
import distantlands.gameplay.GrandMaskCard
import distantlands.gameplay.chronicle.legacies.{Chronicler, ChroniclerCard,
  Gatekeeper, GatekeeperCard, Heirloom, HeirloomCard, IronHandCard,
  OathsOfLoyalty, OathsOfLoyaltyCard}
import distantlands.gameplay.powers.economy.{BirdsongCard, CupOfPlentyCard,
  DowntroddenCard, GolemLegionsCard, MapLibraryCard}
import distantlands.gameplay.powers.whenplayed.ThreateningRoarCard
import distantlands.model._
import distantlands.model.DecisionAnswer.{ChooseManyAnswer, ChooseOneAnswer}
import distantlands.testkit.{MenuPlay, Table}
import distantlands.testkit.Table.{p1, p2, p3}
import LogScripts.atTable

/** The game's end and the Chronicle Phase (Chronicle steps design). Kept
  * beside `LogScripts`, which is at its size limit. */
object ChronicleLogScripts:
  /** p3 rests; p1, the Usurper, wins as their Wake begins. The script's
    * default answer has p1 start the Chronicle, which journals the baseline
    * unchanged. */
  def chronicleStarted(using munit.Location): Script =
    val (service, acting) = atTable("chronicle-started", Table.start
      .pawn(p1, AncientCitySite).pawn(p2, BrokenPeaksSite)
      .pawn(p3, BuriedGiantSite).oathkeeper(Some(p1), TitleSide.Usurper)
      .turn(p3, Phase.Act).banditsRefilled)
    acting.after(MenuPlay.start(p3, PhaseTransitionRef.BeginRest))
    Script("chronicle-started", service, p1)

  /** The same game, with legacies: p2 holds Seer dormant and misses its
    * goal, and p3 holds Iron Hand active and rules no site, so the Sun
    * discards Seer and turns Iron Hand dormant. */
  def chronicleWithLegacies(using munit.Location): Script =
    val (service, acting) = atTable("chronicle-with-legacies", Table.start
      .pawn(p1, AncientCitySite).pawn(p2, BrokenPeaksSite)
      .pawn(p3, BuriedGiantSite).oathkeeper(Some(p1), TitleSide.Usurper)
      .legacy(p2, SeerCard, active = false).legacy(p3, IronHandCard)
      .turn(p3, Phase.Act).banditsRefilled)
    acting.after(MenuPlay.start(p3, PhaseTransitionRef.BeginRest))
    Script("chronicle-with-legacies", service, p1)

  /** The same game, with p2 ruling Broken Peaks: at the Throne p1, the new
    * Chancellor, offers p2 Citizenship and p2 accepts (Empire slice 3b). */
  def throneOffered(using munit.Location): Script =
    val (service, acting) = atTable("throne-offered", Table.start
      .pawn(p1, AncientCitySite).pawn(p2, BrokenPeaksSite)
      .pawn(p3, BuriedGiantSite).oathkeeper(Some(p1), TitleSide.Usurper)
      .warbandsAt(BrokenPeaksSite, p2, 2)
      .turn(p3, Phase.Act).banditsRefilled)
    acting.withAnswers {
      case park if park.decisionId == OfferCitizenship.offerDecisionId =>
        ChooseManyAnswer(Vector(DecisionOptionRef.Player(p2)))
      case park if park.decisionId == OfferCitizenship.answerDecisionId(p2) =>
        ChooseOneAnswer(OfferCitizenship.accept)
    }.after(MenuPlay.start(p3, PhaseTransitionRef.BeginRest))
    Script("throne-offered", service, p1)

  /** The same game as [[chronicleStarted]], where p1 gives p2 one lineage
    * point at Score Lineage Points, for two of their three (R28). */
  def throneGifted(using munit.Location): Script =
    val (service, acting) = atTable("throne-gifted", Table.start
      .pawn(p1, AncientCitySite).pawn(p2, BrokenPeaksSite)
      .pawn(p3, BuriedGiantSite).oathkeeper(Some(p1), TitleSide.Usurper)
      .turn(p3, Phase.Act).banditsRefilled)
    acting.withAnswers {
      case park if park.decisionId == LineageScoring.giftsDecisionId =>
        DecisionAnswer.DistributeAnswer(Vector(
          DistributeAmount(DecisionOptionRef.Player(p2), 1),
          DistributeAmount(DecisionOptionRef.Player(p3), 0)))
    }.after(MenuPlay.start(p3, PhaseTransitionRef.BeginRest))
    Script("throne-gifted", service, p1)

  /** The game of [[throneOffered]] with legacies: p2, ruling Broken Peaks
    * (a Discord and an Arcane denizen) and holding the Darkest Secret,
    * holds Gatekeeper and Chronicler; p3 holds Seer. In the Sun p2 takes
    * Seer with Gatekeeper, then declares Chronicler (Legacy Chronicle
    * powers, slice 1). */
  def sunGatekeeperChronicler(using munit.Location): Script =
    val (service, acting) = atTable("sun-gatekeeper-chronicler", Table.start
      .pawn(p1, AncientCitySite).pawn(p2, BrokenPeaksSite)
      .pawn(p3, BuriedGiantSite).oathkeeper(Some(p1), TitleSide.Usurper)
      .warbandsAt(BrokenPeaksSite, p2, 2)
      .denizen(DowntroddenCard, BrokenPeaksSite)
      .denizen(MapLibraryCard, BrokenPeaksSite)
      .darkestSecret(Some(p2), 1)
      .legacy(p2, GatekeeperCard).legacy(p2, ChroniclerCard)
      .legacy(p3, SeerCard)
      .turn(p3, Phase.Act).banditsRefilled)
    acting.withAnswers {
      case park if park.decisionId == Gatekeeper.decisionId(p2) =>
        ChooseOneAnswer(DecisionOptionRef.Legacy(SeerCard.id))
      case park if park.decisionId == Chronicler.decisionId(p2) =>
        ChooseOneAnswer(Chronicler.declare)
    }.after(MenuPlay.start(p3, PhaseTransitionRef.BeginRest))
    Script("sun-gatekeeper-chronicler", service, p1)

  /** The game of [[chronicleStarted]] with Heirloom: p2, holding the
    * Darkest Secret, holds Heirloom, Cup of Plenty and Grand Mask, and at
    * the Throne keeps Cup of Plenty (Legacy Chronicle powers, slice 3). */
  def throneHeirloom(using munit.Location): Script =
    val (service, acting) = atTable("throne-heirloom", Table.start
      .pawn(p1, AncientCitySite).pawn(p2, BrokenPeaksSite)
      .pawn(p3, BuriedGiantSite).oathkeeper(Some(p1), TitleSide.Usurper)
      .darkestSecret(Some(p2), 1).relic(p2, CupOfPlentyCard)
      .relic(p2, GrandMaskCard).legacy(p2, HeirloomCard)
      .turn(p3, Phase.Act).banditsRefilled)
    acting.withAnswers {
      case park if park.decisionId == Heirloom.decisionId(p2) =>
        ChooseOneAnswer(DecisionOptionRef.Relic(CupOfPlentyCard.id))
    }.after(MenuPlay.start(p3, PhaseTransitionRef.BeginRest))
    Script("throne-heirloom", service, p1)

  /** The game of [[chronicleStarted]] with Oaths of Loyalty and Heirloom:
    * p2 holds the Darkest Secret, Cup of Plenty (the highest relic) and
    * Grand Mask, two faceup advisers, Oaths of Loyalty and Heirloom. At the
    * Throne p2 keeps Cup of Plenty with Heirloom; at the World p2 keeps both
    * advisers with Oaths. The journaled Chronicle comes back too, for the
    * next game's script (R27). */
  def oathsHeirloomPlayed(using munit.Location): (Script, Chronicle) =
    val (service, acting) = atTable("chronicle-oaths-heirloom", Table.start
      .pawn(p1, AncientCitySite).pawn(p2, BrokenPeaksSite)
      .pawn(p3, BuriedGiantSite).oathkeeper(Some(p1), TitleSide.Usurper)
      .darkestSecret(Some(p2), 1).relic(p2, CupOfPlentyCard)
      .relic(p2, GrandMaskCard).adviser(p2, BirdsongCard)
      .adviser(p2, GolemLegionsCard).legacy(p2, OathsOfLoyaltyCard)
      .legacy(p2, HeirloomCard).turn(p3, Phase.Act).banditsRefilled)
    val played = acting.withAnswers {
      case park if park.decisionId == Heirloom.decisionId(p2) =>
        ChooseOneAnswer(DecisionOptionRef.Relic(CupOfPlentyCard.id))
      case park if park.decisionId == OathsOfLoyalty.keepDecisionId(p2) =>
        ChooseManyAnswer(Vector(BirdsongCard.id, GolemLegionsCard.id)
          .map(DecisionOptionRef.Denizen(_)))
    }.after(MenuPlay.start(p3, PhaseTransitionRef.BeginRest))
    (Script("chronicle-oaths-heirloom", service, p1),
      played.ready.game.campaign.evolved.get)

  def chronicleOathsHeirloom(using munit.Location): Script =
    oathsHeirloomPlayed._1

  /** The game of [[chronicleStarted]] with Iron Hand: p3 rules Buried
    * Giant and Deep Woods, holds Iron Hand and The Needle, and Threatening
    * Roar lies at Buried Giant. p3 shapes and builds in the Empire's place,
    * scores after p1, and their sites are stored (Legacy Chronicle powers,
    * slice 4). */
  def chronicleIronHand(using munit.Location): Script =
    val (service, acting) = atTable("chronicle-iron-hand", Table.start
      .pawn(p1, AncientCitySite).pawn(p2, BrokenPeaksSite)
      .pawn(p3, BuriedGiantSite).oathkeeper(Some(p1), TitleSide.Usurper)
      .warbandsAt(BuriedGiantSite, p3, 1).warbandsAt(DeepWoodsSite, p3, 1)
      .denizen(ThreateningRoarCard, at = BuriedGiantSite)
      .legacy(p3, IronHandCard).legacy(p3, TheNeedleCard)
      .turn(p3, Phase.Act).banditsRefilled)
    acting.after(MenuPlay.start(p3, PhaseTransitionRef.BeginRest))
    Script("chronicle-iron-hand", service, p1)

  /** The first game, with Foundation I on Wide Horizons: at the Beacon
    * p1, who holds it, restores it to Imperial Maps (Foundations design,
    * slice 7). */
  def beaconRestoredFoundation(using munit.Location): Script =
    val (service, acting) = atTable("beacon-restored-foundation", Table.start
      .pawn(p1, AncientCitySite).pawn(p2, BrokenPeaksSite)
      .pawn(p3, BuriedGiantSite).oathkeeper(Some(p1), TitleSide.Usurper)
      .foundation(FoundationNumber.I, FoundationFace.Altered)
      .turn(p3, Phase.Act).banditsRefilled)
    acting.withAnswers {
      case park if park.decisionId == ShapeFoundation.decisionId =>
        ChooseOneAnswer(ShapeFoundation.restore(FoundationNumber.I))
    }.after(MenuPlay.start(p3, PhaseTransitionRef.BeginRest))
    Script("beacon-restored-foundation", service, p1)

  /** The same game, where p1 keeps every foundation as it lies, and the
    * line says so. */
  def beaconKeptFoundations(using munit.Location): Script =
    val (service, acting) = atTable("beacon-kept-foundations", Table.start
      .pawn(p1, AncientCitySite).pawn(p2, BrokenPeaksSite)
      .pawn(p3, BuriedGiantSite).oathkeeper(Some(p1), TitleSide.Usurper)
      .foundation(FoundationNumber.I, FoundationFace.Altered)
      .turn(p3, Phase.Act).banditsRefilled)
    acting.withAnswers {
      case park if park.decisionId == ShapeFoundation.decisionId =>
        ChooseOneAnswer(ShapeFoundation.keep)
    }.after(MenuPlay.start(p3, PhaseTransitionRef.BeginRest))
    Script("beacon-kept-foundations", service, p1)

  val named: Vector[(String, () => Script)] =
    Vector("chronicle-started" -> (() => chronicleStarted),
      "throne-offered" -> (() => throneOffered),
      "throne-gifted" -> (() => throneGifted),
      "sun-gatekeeper-chronicler" -> (() => sunGatekeeperChronicler),
      "throne-heirloom" -> (() => throneHeirloom),
      "chronicle-oaths-heirloom" -> (() => chronicleOathsHeirloom),
      "chronicle-iron-hand" -> (() => chronicleIronHand),
      "beacon-restored-foundation" -> (() => beaconRestoredFoundation),
      "beacon-kept-foundations" -> (() => beaconKeptFoundations))
