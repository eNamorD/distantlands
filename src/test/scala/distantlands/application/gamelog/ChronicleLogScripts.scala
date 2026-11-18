package distantlands.application.gamelog

import distantlands.gameplay.powers.banner.TheNeedleCard
import distantlands.gameplay.setup.legacies.SeerCard
import distantlands.gameplay.cards.{AncientCitySite, BrokenPeaksSite,
  BuriedGiantSite, DeepWoodsSite}
import distantlands.gameplay.chronicle.{BuildEdifice, HidingPlace,
  LineageScoring, OfferCitizenship, ShapeFoundation}
import distantlands.gameplay.chronicle.legacies.{Chronicler, ChroniclerCard,
  Gatekeeper, GatekeeperCard, GreatArchitectCard, Heirloom, HeirloomCard,
  IronHandCard, OathsOfLoyalty, OathsOfLoyaltyCard, Reformer, ReformerCard,
  Revolutionary, RevolutionaryCard, Rival, RivalCard, RoyalLine,
  RoyalLineCard, TheBrightBeacon, TheBrightBeaconCard, TheEarCard}
import distantlands.gameplay.{BanditCrownCard, GrandMaskCard}
import distantlands.gameplay.powers.action.{EldersCard, GiantsPathCard}
import distantlands.gameplay.powers.campaign.RivalKhanCard
import distantlands.gameplay.powers.economy.{BirdsongCard, CupOfPlentyCard,
  DowntroddenCard, GolemLegionsCard, HallowedSpringCard, MapLibraryCard,
  VillageIdiotCard, WildPasturesCard}
import distantlands.gameplay.powers.setup.GreatForgeCard
import distantlands.gameplay.powers.whenplayed.{GreatHerdCard,
  ThreateningRoarCard}
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

  /** The game of [[chronicleStarted]] with Hiding Place: p2 rules Broken
    * Peaks, where Hiding Place stands, and holds Cup of Plenty and Grand
    * Mask; at the start of the Throne p2 keeps Cup of Plenty (Last
    * Chronicle powers, slice 2). */
  def throneHidingPlace(using munit.Location): Script =
    val (service, acting) = atTable("throne-hiding-place", Table.start
      .pawn(p1, AncientCitySite).pawn(p2, BrokenPeaksSite)
      .pawn(p3, BuriedGiantSite).oathkeeper(Some(p1), TitleSide.Usurper)
      .edifice(HallowedSpringCard, EdificeSide.Ruined, at = BrokenPeaksSite)
      .warbandsAt(BrokenPeaksSite, p2, 1)
      .relic(p2, CupOfPlentyCard).relic(p2, GrandMaskCard)
      .turn(p3, Phase.Act).banditsRefilled)
    acting.withAnswers {
      case park if park.decisionId == HidingPlace.decisionId(p2) =>
        ChooseOneAnswer(DecisionOptionRef.Relic(CupOfPlentyCard.id))
    }.after(MenuPlay.start(p3, PhaseTransitionRef.BeginRest))
    Script("throne-hiding-place", service, p1)

  /** The game of [[chronicleStarted]] with Blasted Pastures at Broken
    * Peaks, which bandits rule, Elders at Deep Woods and Giant's Rampage at
    * Ancient City: at the start of the Throne all three are buried, named
    * inline (Last Chronicle powers, slice 3). */
  def throneBlastedPastures(using munit.Location): Script =
    blastedPastures("throne-blasted-pastures", table => table
      .denizen(EldersCard, at = DeepWoodsSite)
      .edifice(GiantsPathCard, EdificeSide.Ruined, at = AncientCitySite))

  /** The same, with six Nomad cards, so the line lists them as a card
    * list. */
  def throneBlastedPasturesListed(using munit.Location): Script =
    blastedPastures("throne-blasted-pastures-listed", table => table
      .denizen(EldersCard, at = DeepWoodsSite)
      .denizen(GreatHerdCard, at = DeepWoodsSite)
      .edifice(GiantsPathCard, EdificeSide.Ruined, at = AncientCitySite)
      .denizen(RivalKhanCard, at = AncientCitySite)
      .edifice(GreatForgeCard, EdificeSide.Intact, at = BuriedGiantSite))

  /** [[chronicleStarted]]'s table with Blasted Pastures at Broken Peaks and
    * the Nomad cards `nomads` places. */
  private def blastedPastures(name: String, nomads: Table => Table)(
      using munit.Location): Script =
    val (service, acting) = atTable(name, nomads(Table.start
      .pawn(p1, AncientCitySite).pawn(p2, BrokenPeaksSite)
      .pawn(p3, BuriedGiantSite).oathkeeper(Some(p1), TitleSide.Usurper)
      .edifice(WildPasturesCard, EdificeSide.Ruined, at = BrokenPeaksSite))
      .turn(p3, Phase.Act).banditsRefilled)
    acting.after(MenuPlay.start(p3, PhaseTransitionRef.BeginRest))
    Script(name, service, p1)

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

  /** An Empire game that ends at round eight: p1 is the Chancellor and the
    * Oathkeeper, p2 and p3 Citizens, and p3 holds Royal Line. p3 rests last
    * in the round, the Empire wins, and in the Sun p3 names p2 the winner
    * (Legacy Chronicle powers, slice 5). */
  def chronicleRoyalLine(using munit.Location): Script =
    val (service, acting) = atTable("chronicle-royal-line", Table.start
      .imperial(p1, p2, p3)
      .pawn(p1, AncientCitySite).pawn(p2, BrokenPeaksSite)
      .pawn(p3, BuriedGiantSite).oathkeeper(Some(p1), TitleSide.Oathkeeper)
      .roundMarkerAt(8).legacy(p3, RoyalLineCard)
      .turn(p3, Phase.Act).banditsRefilled)
    acting.withAnswers {
      case park if park.decisionId == RoyalLine.decisionId(p3) =>
        ChooseOneAnswer(DecisionOptionRef.Player(p2))
    }.after(MenuPlay.start(p3, PhaseTransitionRef.BeginRest))
    Script("chronicle-royal-line", service, p1)

  /** The game of [[chronicleStarted]] with The Ear and The Bright Beacon:
    * p2 holds both, the Darkest Secret, which keeps them active and brings
    * the Beacon task, and three relics. In Scatter Relics p2 chooses where
    * the relics go, and at the Beacon takes the six newcomers from the
    * Dispossessed (Legacy Chronicle powers, slice 3; R47). */
  def chronicleEarBeacon(using munit.Location): Script =
    val (service, acting) = atTable("chronicle-ear-beacon", Table.start
      .pawn(p1, AncientCitySite).pawn(p2, BrokenPeaksSite)
      .pawn(p3, BuriedGiantSite).oathkeeper(Some(p1), TitleSide.Usurper)
      .darkestSecret(Some(p2), 1).relic(p2, CupOfPlentyCard)
      .relic(p2, GrandMaskCard).relic(p2, BanditCrownCard)
      .legacy(p2, TheEarCard).legacy(p2, TheBrightBeaconCard)
      .turn(p3, Phase.Act).banditsRefilled)
    acting.withAnswers {
      case park if park.decisionId == TheBrightBeacon.sourceId(p2) =>
        ChooseOneAnswer(TheBrightBeacon.dispossessed)
    }.after(MenuPlay.start(p3, PhaseTransitionRef.BeginRest))
    Script("chronicle-ear-beacon", service, p1)

  /** The first offered card, and for a choose-one the first that is not a
    * button. */
  private def firstCard(decide: Decide): DecisionAnswer = decide.query match
    case many: DecisionQuery.ChooseMany =>
      ChooseManyAnswer(many.options.filter(_.available).take(1).map(_.ref))
    case one: DecisionQuery.ChooseOne => ChooseOneAnswer(one.options
      .filter(_.available).map(_.ref)
      .find(!_.isInstanceOf[DecisionOptionRef.Button]).get)
    case other => throw IllegalStateException(s"no card answer for $other")

  /** An Empire game that ends at round eight, as [[chronicleRoyalLine]]'s,
    * with the Throne's legacies acting: p1, the winner, moves a card with
    * Reformer; p2 builds with Great Architect; p3 keeps a card from Shape
    * Empire's discard with Rival and discards one at an Imperial site with
    * Revolutionary (Legacy Chronicle powers, slice 2). */
  def throneLegacies(using munit.Location): Script =
    val (service, acting) = atTable("throne-legacies", Table.start
      .imperial(p1, p2, p3)
      .pawn(p1, AncientCitySite).pawn(p2, BrokenPeaksSite)
      .pawn(p3, BuriedGiantSite).oathkeeper(Some(p1), TitleSide.Oathkeeper)
      .roundMarkerAt(8)
      .warbandsAt(AncientCitySite, p1, 1).warbandsAt(BrokenPeaksSite, p1, 1)
      .denizen(ThreateningRoarCard, AncientCitySite)
      .denizen(VillageIdiotCard, BrokenPeaksSite)
      .denizen(MapLibraryCard, DeepWoodsSite)
      .legacy(p1, ReformerCard).legacy(p2, GreatArchitectCard)
      .legacy(p3, RivalCard).legacy(p3, RevolutionaryCard)
      .turn(p3, Phase.Act).banditsRefilled)
    var reformed = false
    acting.withAnswers {
      case park if park.decisionId == Reformer.pickId(p1) =>
        val answer = if reformed then ChooseOneAnswer(Reformer.done)
          else firstCard(park.decide)
        reformed = true
        answer
      case park if park.decisionId == Reformer.destinationId(p1) =>
        firstCard(park.decide)
      case park if park.decisionId == Rival.decisionId(p3) ||
          park.decisionId == Revolutionary.decisionId(p3) =>
        firstCard(park.decide)
      case park if park.decisionId == BuildEdifice.cardDecisionId =>
        firstCard(park.decide)
    }.after(MenuPlay.start(p3, PhaseTransitionRef.BeginRest))
    Script("throne-legacies", service, p1)

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
      "throne-hiding-place" -> (() => throneHidingPlace),
      "throne-blasted-pastures" -> (() => throneBlastedPastures),
      "chronicle-oaths-heirloom" -> (() => chronicleOathsHeirloom),
      "chronicle-iron-hand" -> (() => chronicleIronHand),
      "chronicle-royal-line" -> (() => chronicleRoyalLine),
      "chronicle-ear-beacon" -> (() => chronicleEarBeacon),
      "throne-legacies" -> (() => throneLegacies),
      "beacon-restored-foundation" -> (() => beaconRestoredFoundation),
      "beacon-kept-foundations" -> (() => beaconKeptFoundations))
