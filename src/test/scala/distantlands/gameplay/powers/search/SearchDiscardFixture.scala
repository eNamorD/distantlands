package distantlands.gameplay.powers.search

import distantlands.gameplay.actions.cardplay.CardPlayProcedure.PlayedAs
import distantlands.gameplay.cards.AncientCitySite
import distantlands.gameplay.powers.{PowerFixture, SearchFixture}
import distantlands.gameplay.powers.action.HomesteadersCard
import distantlands.model._
import distantlands.testkit.Table

/** The Search discards that Bracken, Cracked Horn and Ring of Devotion
  * redirect (catalog batch 10, slice 9, ruling 2): the cards not kept, an
  * adviser bumped over the limit, and a site card the People's Favor
  * discards. p1 searches the world deck at Ancient City, in the Cradle, so
  * a discard goes to the Provinces pile unless a power redirects it. */
object SearchDiscardFixture:
  import PowerFixture.actor
  import SearchFixture.{keep, place, placeReplacing}

  /** The three Discord denizens the Search draws, none with a power that
    * acts here. The first is kept. */
  val drawn: Vector[DenizenId] = SearchFixture.discordCards.take(3)
  val kept: DenizenId = drawn.head
  val unkept: Vector[DenizenId] = drawn.tail
  /** Three more, which fill p1's adviser area facedown. */
  val heldAdvisers: Vector[DenizenId] = SearchFixture.discordCards.slice(3, 6)
  /** The adviser a faceup play bumps. */
  val bumped: DenizenId = heldAdvisers.head
  /** The card at Ancient City the People's Favor lets p1 discard. */
  val siteCard: DenizenId = HomesteadersCard.id

  /** p1 at Ancient City with 5 Supply, the world deck topped by `top`, and
    * whatever `arrange` adds. */
  def searchAtAncientCity(arrange: Table => Table = identity,
      top: Vector[WorldCardId] = drawn): ReadyGame =
    arrange(Table.from(SearchFixture.staged(top, supply = 5))).ready

  /** `table` with p1's adviser area full of three facedown cards. */
  def withThreeFacedownAdvisers(table: Table): Table =
    heldAdvisers.foldLeft(table)((done, card) =>
      done.adviser(actor, card, facedown = true))

  /** `table` with Homesteaders at Ancient City and p1 holding the People's
    * Favor, Mob face up. */
  def withPeoplesFavorAndSiteCard(table: Table): Table =
    table.denizen(siteCard, at = AncientCitySite)
      .peoplesFavor(Some(actor), 1)

  /** `searched`, parked on the keep question, finished: `card` kept and
    * placed by `play`. The events are the whole Search's. */
  private def finished(searched: OathTransition, card: WorldCardId)(
      play: OathTransition => Either[OathViolation, OathTransition])
      : OathTransition = (for
    chosen <- keep(searched, card)
    placed <- play(chosen)
  yield placed.copy(events = searched.events ++ chosen.events ++
    placed.events)).fold(error => throw new AssertionError(error.toString),
    identity)

  /** The kept card played facedown, so only the unkept draws are
    * discarded. */
  def keptFacedown(searched: OathTransition)(using munit.Location)
      : OathTransition =
    finished(searched, kept)(place(_, kept, PlayedAs.Facedown))

  /** The kept card played faceup in `bumped`'s place. */
  def keptFaceupBumpingAnAdviser(searched: OathTransition)(
      using munit.Location): OathTransition =
    finished(searched, kept)(placeReplacing(_, kept, PlayedAs.Faceup,
      bumped))

  /** The kept card played to Ancient City in `siteCard`'s place. */
  def keptAtSiteReplacingTheSiteCard(searched: OathTransition)(
      using munit.Location): OathTransition =
    finished(searched, kept)(placeReplacing(_, kept, PlayedAs.AtSite,
      siteCard))

  /** `card` kept and played to Ancient City. */
  def keptAtSite(searched: OathTransition, card: WorldCardId)(
      using munit.Location): OathTransition =
    finished(searched, card)(place(_, card, PlayedAs.AtSite))
