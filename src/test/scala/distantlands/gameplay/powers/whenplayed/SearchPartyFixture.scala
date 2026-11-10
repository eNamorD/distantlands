package distantlands.gameplay.powers.whenplayed

import distantlands.gameplay.actions.cardplay.CardPlayProcedure.PlayedAs
import distantlands.gameplay.cards.AncientCitySite
import distantlands.gameplay.powers.{PowerFixture, SearchFixture}
import distantlands.model._
import distantlands.model.DecisionAnswer.ChooseOneAnswer
import distantlands.testkit.{PlacementPicks, Table}
import distantlands.testkit.Unwrap.accepted

/** Drives Search Party's WHEN PLAYED through the rules with the production
  * walker powers (catalog batch 10, slice 12). p1 stands at Ancient City,
  * in the Cradle, so a card p1 discards goes to the Provinces pile. Each
  * helper returns the events of every command so far, so a replay can
  * start from the table. */
object SearchPartyFixture:
  import PowerFixture.actor
  import SearchFixture.{place, rules}

  /** Four plain Hearth denizens, with no production power of their own. */
  val plainHearth: Vector[DenizenId] =
    SearchFixture.anyPlainDenizensOf(Suit.Hearth).take(4)

  /** p1 at Ancient City holding Search Party facedown, with `arrange`
    * applied. The quiet table's discard piles start empty. */
  def p1HoldsSearchPartyFacedown(arrange: Table => Table)(
      using munit.Location): ReadyGame =
    arrange(Table.start.pawn(actor, AncientCitySite)
      .adviser(actor, SearchPartyCard, facedown = true)).ready

  /** p1 plays the facedown Search Party faceup, which fires its WHEN
    * PLAYED. */
  def playedFaceupFromFacedown(ready: ReadyGame)(using munit.Location)
      : OathTransition =
    SearchFixture.playFacedown(ready, Vector.empty, SearchPartyCard.id,
      PlayedAs.Faceup)

  /** `from`, parked on Search Party's pile question, answered `region`. */
  def pilePicked(from: OathTransition, region: Region)(
      using munit.Location): OathTransition =
    chained(from, rules.resolveWalker(from.state, actor,
      SearchParty.pileDecisionId, ChooseOneAnswer(SearchParty.pileRef(region))))

  /** `from`, parked on Search Party's card question, answered `card`. */
  def cardPicked(from: OathTransition, card: WorldCardId)(
      using munit.Location): OathTransition =
    chained(from, rules.resolveWalker(from.state, actor,
      SearchParty.cardDecisionId,
      ChooseOneAnswer(PlacementPicks.optionNaming(card))))

  /** Both questions answered: parked on `card`'s placement. */
  def peekedAndPicked(from: OathTransition, region: Region,
      card: WorldCardId)(using munit.Location): OathTransition =
    cardPicked(pilePicked(from, region), card)

  /** `from`, parked on `card`'s placement, placed the way `way` names. */
  def placedAs(from: OathTransition, card: WorldCardId, way: PlayedAs)(
      using munit.Location): OathTransition =
    chained(from, place(from, card, way))

  /** `next`'s transition, with `from`'s events before its own. */
  private def chained(from: OathTransition,
      next: Either[OathViolation, OathTransition])(using munit.Location)
      : OathTransition =
    val done = accepted(next)
    done.copy(events = from.events ++ done.events)
