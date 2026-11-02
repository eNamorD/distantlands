package oathdigital.gameplay.powers.action

import oathdigital.gameplay.operations.HallOfMinistersCard
import oathdigital.gameplay.powers.NoteText
import oathdigital.gameplay.powers.campaign.CodeOfHonorCard
import oathdigital.gameplay.powers.search.ErrandBoyCard
import oathdigital.gameplay.powers.wake.AncientCitySite
import oathdigital.gameplay.setup.FirstGameSetupFixture.catalog
import oathdigital.model._
import oathdigital.testkit.{Look, PhasePowerHarness, Table}
import oathdigital.testkit.Outcomes.assertWireRoundTrips
import oathdigital.testkit.Table.p1
import oathdigital.testkit.Unwrap.accepted

class DarkEnforcerSuite extends munit.FunSuite:
  import PhasePowerHarness._

  private val enforcer = DarkEnforcerCard.id
  private val source = DecisionOptionRef.Denizen(enforcer)
  private val power = DarkEnforcer.forCatalog(catalog)
  private val home = AncientCitySite.id
  // One denizen of each suit the tests need; their powers go unused.
  private val codeOfHonor = CodeOfHonorCard.id
  private val homesteaders = HomesteadersCard.id
  private val errandBoy = ErrandBoyCard.id
  /** An Order edifice, on its ruined face, and a Hearth one, intact. */
  private val hallOfBandits = HallOfMinistersCard.id
  private val festivalDistrict = FestivalDistrictCard.id

  /** p1's Act at Ancient City holding Dark Enforcer, with `favor` favor. */
  private def staged(favor: Int = 1): Table =
    Table.start.turn(p1, Phase.Act).pawn(p1, home).adviser(p1, enforcer)
      .favor(p1, favor)

  /** The discard pile of the region after Ancient City's (the Cradle):
    * where a card discarded there goes. */
  private def pile(ready: ReadyGame): Vector[WorldCardId] =
    Look(ready).discardAfter(home)

  test("it burns a favor and discards every Order and Hearth card at the " +
      "site, with their returns"):
    val ready = staged().denizen(codeOfHonor, at = home)
      .denizen(homesteaders, at = home).denizen(errandBoy, at = home)
      .edifice(hallOfBandits, EdificeSide.Ruined, at = home)
      .tokens(codeOfHonor, favor = 1).ready
    val orderBank = ready.banks.favor.getOrElse(Suit.Order, 0)
    val t = accepted(use(ready, power, source))
    parked.assertResumed(t.state, Phase.Act, p1)
    val end = after(t)
    assertEquals(Look(end).favor(p1), 0)
    assertEquals(Look(end).cardsAt(home), Vector[CardId](errandBoy))
    assertEquals(pile(end).takeRight(2),
      Vector[WorldCardId](codeOfHonor, homesteaders))
    assertEquals(end.game.current.commonCards.edificeDeck.lastOption,
      Some(hallOfBandits))
    assertEquals(end.banks.favor.getOrElse(Suit.Order, 0), orderBank + 1)
    assertEquals(NoteText.said(power, t.events), Vector(NoteText.Said(
      NoteKey.Used, s"Discarded ${codeOfHonor.value}, ${homesteaders.value}, " +
        s"${hallOfBandits.value}.",
      covers = true)))
    assertEquals(replayed(ready, t.events), Right(t.state))
    assertWireRoundTrips(t.events)

  test("an intact edifice and a facedown card stay, and the line names " +
      "the site"):
    val ready = staged()
      .edifice(festivalDistrict, EdificeSide.Intact, at = home)
      .denizen(codeOfHonor, at = home, facedown = true).ready
    val t = accepted(use(ready, power, source))
    assertEquals(Look(after(t)).cardsAt(home), Look(ready).cardsAt(home))
    assertEquals(NoteText.said(power, t.events), Vector(NoteText.Said(
      "used.none", s"${home.value} held no Order or Hearth card to discard.",
      covers = true)))

  test("it is unusable without a favor to burn"):
    val broke = staged(favor = 0).ready
    assert(!usableNow(broke).exists(_.power.id == DarkEnforcer.id))
    assertEquals(use(broke, power, source).left.toOption,
      Some(OathViolation.InsufficientFavor(1, 0)))
