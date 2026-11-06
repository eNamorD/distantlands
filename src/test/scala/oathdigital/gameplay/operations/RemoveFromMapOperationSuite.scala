package oathdigital.gameplay.operations

import oathdigital.gameplay.powers.action.HomesteadersCard
import oathdigital.gameplay.powers.setup.GreatClockCard
import oathdigital.model._
import oathdigital.testkit.{Look, Table}
import oathdigital.testkit.Table.p1
import oathdigital.testkit.Unwrap.accepted

/** `RemoveFromMap` (catalog batch 10, slice 6): a card leaves the map for a
  * pile that holds no tokens. Its favor goes to its suit's bank and its
  * secrets to the acting player, facedown, in the same operation, so a
  * restriction that refuses the move refuses the returns too. */
class RemoveFromMapOperationSuite extends munit.FunSuite:
  private val executor = new OperationExecutor()
  private val site = Table.start.ready.game.current.map.inPlay.head
  private val from = PositionedLocation(Location.Site(site))
  private val toDispossessed = PositionedLocation(Location.Dispossessed)
  private val homesteaders = HomesteadersCard.id

  /** Homesteaders at `site` with 1 favor and 2 secrets, and 3 favor in the
    * Hearth bank. */
  private val homesteadersWithTokens: ReadyGame = Table.start
    .denizen(HomesteadersCard, at = site)
    .tokens(homesteaders, favor = 1, secrets = 2)
    .bankFavor(Suit.Hearth, 3).ready

  private def homesteadersRemoved(favor: Int): RemoveFromMap =
    RemoveFromMap(homesteaders, from, toDispossessed, Suit.Hearth,
      favor = favor, secrets = 2, actingPlayer = p1)

  test("a denizen goes to the end of the Dispossessed, its favor to its " +
      "bank and its secrets to the actor facedown"):
    val done = accepted(executor.execute(homesteadersWithTokens,
      homesteadersRemoved(favor = 1)))
    assertEquals(done.game.campaign.dispossessed.lastOption,
      Some(homesteaders))
    assert(!Look(done).cardsAt(site).contains(homesteaders))
    assertEquals(Look(done).bankFavor(Suit.Hearth), 3 + 1)
    assertEquals(Look(done).faceDownSecrets(p1),
      Look(homesteadersWithTokens).faceDownSecrets(p1) + 2)

  test("a ruined edifice goes to the bottom of the edifice deck"):
    val ruined = Table.start
      .edifice(GreatClockCard, EdificeSide.Ruined, at = site).ready
    val done = accepted(executor.execute(ruined, RemoveFromMap(
      GreatClockCard.id, from, PositionedLocation(Location.Deck(
        CardDeck.Edifice), StackPosition.Bottom), Suit.Arcane, favor = 0,
      secrets = 0, actingPlayer = p1)))
    assertEquals(done.game.current.commonCards.edificeDeck.lastOption,
      Some(GreatClockCard.id))
    assert(!Look(done).cardsAt(site).contains(GreatClockCard.id))

  test("tokens that do not match the card are refused"):
    assertEquals(executor.execute(homesteadersWithTokens,
      homesteadersRemoved(favor = 0)).left.toOption,
      Some(OperationError.InvalidDescription(
        s"discard resources on ${homesteaders.value} do not match the card")))

  test("a lock refuses the whole removal, returns included"):
    // The returns are the removal's children, so the lock judges them with
    // the card's move and the card keeps its tokens.
    assert(LockedCards(Set(homesteaders))
      .reason(homesteadersWithTokens, homesteadersRemoved(favor = 1))
      .isDefined)
