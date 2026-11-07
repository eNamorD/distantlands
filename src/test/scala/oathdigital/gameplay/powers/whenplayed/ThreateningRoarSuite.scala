package oathdigital.gameplay.powers.whenplayed

import oathdigital.gameplay.powers.NoteText
import oathdigital.gameplay.powers.action.{FaeMerchantCard, HomesteadersCard}
import oathdigital.gameplay.powers.campaign.WildMountsCard
import oathdigital.gameplay.powers.search.ErrandBoyCard
import oathdigital.gameplay.actions.challenge.SchoolOfVinesCard
import oathdigital.gameplay.powers.targeting.OakenFortressCard
import oathdigital.model._
import oathdigital.testkit.{Look, Outcomes}
import oathdigital.testkit.Table.p1
import oathdigital.testkit.AppendedAutomaticUses

class ThreateningRoarSuite extends munit.FunSuite:
  import WhenPlayedHarness._

  private val power = registered[ThreateningRoar.type]
  private val roar = power.cardId
  private val errandBoy = ErrandBoyCard.id
  private val faeMerchant = FaeMerchantCard.id
  private val wildMounts = WildMountsCard.id
  private val homesteaders = HomesteadersCard.id

  private def discarded(cards: Vector[CardId]): NoteText.Said =
    NoteText.Said("discarded",
      s"Discarded ${cards.map(_.value).mkString(", ")}.", covers = true)

  test("as an adviser it discards the Beast and Nomad cards at sites in " +
      "the pawn's region"):
    // Beast and Nomad cards in p1's region go, the ruined Beast edifice with
    // them. Homesteaders (Hearth), the intact Oaken Fortress and Fae Merchant
    // in another region stay.
    val ruined = SchoolOfVinesCard.id
    val intact = OakenFortressCard.id
    val ready = p1AtHomeSite.adviser(p1, roar)
      .denizen(errandBoy, at = homeSite).denizen(homesteaders, at = homeSite)
      .denizen(wildMounts, at = nearSite)
      .edifice(SchoolOfVinesCard, EdificeSide.Ruined, at = nearSite)
      .edifice(OakenFortressCard, EdificeSide.Intact, at = nearSite)
      .denizen(faeMerchant, at = awaySite).ready
    val done = Outcomes.finished(play(ready, power, roar))
    val after = Look(done.treeless)
    assertEquals(after.cardsAt(homeSite), Vector[CardId](homesteaders))
    assertEquals(after.cardsAt(nearSite), Vector[CardId](intact))
    assertEquals(after.cardsAt(awaySite), Vector[CardId](faeMerchant))
    assertJournalRebuilds(ready, done.events, done.treeless)
    assertEquals(said(power, done.events), Vector(discarded(inMapOrder(
      homeSite -> Vector(errandBoy), nearSite -> Vector(wildMounts, ruined)))))

  test("played to a site it discards itself with the rest"):
    val ready = p1AtHomeSite.denizen(roar, at = homeSite)
      .denizen(wildMounts, at = homeSite).ready
    val done = Outcomes.finished(fire(ready, power, hookAt(roar, homeSite)))
    assertEquals(Look(done.treeless).cardsAt(homeSite), Vector.empty)
    assertEquals(said(power, done.events),
      Vector(discarded(Vector(roar, wildMounts))))

  test("with nothing to discard it says so"):
    val ready = p1AtHomeSite.adviser(p1, roar)
      .denizen(homesteaders, at = homeSite).ready
    val done = Outcomes.finished(play(ready, power, roar))
    assertEquals(ops(done.events), Vector.empty)
    assertEquals(said(power, done.events), Vector(NoteText.Said("none",
      "Nothing was discarded.", covers = true)))

  test("its effect is an automatic use of the card's player"):
    val played = CardPlayedFaceup(ThreateningRoarCard.id,
      RuleSourceRef.GameRule(ThreateningRoar.id.value))
    assertEquals(AppendedAutomaticUses.at(ThreateningRoar,
      PowerWindow.ActionCardPlayedFaceup, played), Vector(
      AppendedAutomaticUses.Named(p1, ThreateningRoar.id,
        ThreateningRoarCard.id)))
