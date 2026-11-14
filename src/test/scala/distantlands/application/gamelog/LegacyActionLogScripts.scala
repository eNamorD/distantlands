package distantlands.application.gamelog

import distantlands.gameplay.cards.{AncientCitySite, BrokenPeaksSite}
import distantlands.gameplay.chronicle.legacies.TheEarCard
import distantlands.gameplay.powers.campaign.StickyFireCard
import distantlands.gameplay.powers.economy.DefameCard
import distantlands.gameplay.actions.negotiation.HighPriestCard
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
import distantlands.testkit.{MenuPlay, Table}
import distantlands.testkit.Table.{p1, p2}
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

  val named: Vector[(String, () => Script)] = Vector(
    "legacy-actions" -> (() => legacyActions),
    "legacy-modifiers" -> (() => legacyModifiers))
