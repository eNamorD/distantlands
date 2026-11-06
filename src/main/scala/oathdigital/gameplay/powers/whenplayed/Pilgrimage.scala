package oathdigital.gameplay.powers.whenplayed

import oathdigital.catalog.{AdviserOnly, Denizen, ExecutableCatalog,
  PrintedPower}
import oathdigital.gameplay.powerresolver.{Deferred, PowerCtx}
import oathdigital.gameplay.powers.DispossessedDraws
import oathdigital.model._

object PilgrimageCard extends Denizen(DenizenId("161"), "Pilgrimage", Suit.Nomad) with AdviserOnly:
  val power = PrintedPower(PowerId("denizen.pilgrimage"),
    persistent = false, cost = Cost.free,
    text = "**WHEN PLAYED:** move all denizens at your site to the " +
      "Dispossessed. Shuffle and draw denizens from the " +
      "Dispossessed equal to the number you moved. Peek at them " +
      "and put them on your region's discard pile.")
  val powers: Vector[PrintedPower] = Vector(power)

/** Pilgrimage (card 161), WHEN PLAYED: "move all denizens at your site to
  * the Dispossessed. Shuffle and draw denizens from the Dispossessed equal
  * to the number you moved. Peek at them and put them on your region's
  * discard pile." (catalog batch 10, slice 6).
  *
  * Each card at the user's site leaves in one `RemoveFromMap`, its favor
  * to its bank and its secrets to the user. A denizen goes to the
  * Dispossessed. A ruined edifice goes to the bottom of the edifice deck
  * and is not counted (ruling 9). A card a lock keeps stays with its
  * tokens: an intact edifice, a lock-icon denizen, or one Ancient Bloodline
  * locks for the user. The removal step skips what a restriction refuses.
  *
  * The count is the denizens that reached the Dispossessed. Then the draw
  * is [[DispossessedDraws]]'s: the shuffle includes the denizens just
  * moved, so they can come back. The cards go on top of the region's
  * discard pile, and the user peeks at each, so only the user's knowledge
  * records it. An `Inspect` shows them top first, as Scryer's does. Each
  * peek is also held under [[drawnKey]], since the walker selects a
  * `Branch` again when a parked command resumes, and by then nothing in
  * the state says how many cards were drawn. The `Inspect` reads the held
  * peeks (`Deferred.resolve`), so it asks the same question after a reload.
  *
  * The line covers the peeks, so no generic line names a card: "Sent 2
  * denizens on Pilgrimage and drew 2 from the Dispossessed." With none
  * sent, nothing is shuffled or drawn: "Sent no denizens on Pilgrimage."
  */
case object Pilgrimage extends WhenPlayedPower:
  val id: PowerId = PilgrimageCard.power.id
  val cardId: DenizenId = PilgrimageCard.id
  val inspectDecisionId: String = "power.pilgrimage.inspect"
  /** Holds the peeks of the drawn cards, top first, for the `Inspect`. */
  val drawnKey: DeferralKey = DeferralKey("pilgrimage.drawn")

  private val notes = NoteSet(id)
  /** "Sent {2} {denizens} on Pilgrimage and drew {2} from the
    * Dispossessed." */
  val used: NoteKey = notes.key(NoteKey.Used, Vector(NotePart.Text("Sent "),
    NotePart.Arg(0), NotePart.Plural(0, " denizen", " denizens"),
    NotePart.Text(" on Pilgrimage and drew "), NotePart.Arg(1),
    NotePart.Text(" from the Dispossessed.")))
  /** "Sent no denizens on Pilgrimage." */
  val none: NoteKey = notes.key("used.none", Vector(
    NotePart.Text("Sent no denizens on Pilgrimage.")))
  override def noteKeys: Vector[NoteKey] = notes.declared

  def effect(ctx: PowerCtx): Vector[Operation] = Vector(Branch((live, _) =>
    (for
      site <- live.game.current.pawnSite(ctx.actor)
      region <- live.game.current.map.regionOf(site)
    yield
      val pilgrims = denizensAt(live, site)
      Vector[Operation](
        BuildOps((ready, _) => removals(ctx.catalog, ready, site, ctx.actor)),
        Branch((moved, _) => draw(moved, pilgrims, region, ctx.actor)),
        inspect(ctx.actor))
    ).toVector.flatten))

  private def denizensAt(ready: ReadyGame, site: SiteId): Vector[DenizenId] =
    ready.game.current.map.sites.get(site).toVector.flatMap(_.denizens
      .collect { case denizen: DenizenState => denizen.id })

  /** One removal per card that may leave: every denizen, and a ruined
    * edifice. An intact edifice is locked, so none is built for it. */
  private def removals(catalog: ExecutableCatalog, ready: ReadyGame,
      site: SiteId, actor: PlayerId)
      : Either[OathViolation, Vector[CoreOperation]] =
    val from = PositionedLocation(Location.Site(site))
    ready.game.current.map.sites.get(site).toVector.flatMap(_.denizens)
      .foldLeft[Either[OathViolation, Vector[CoreOperation]]](
        Right(Vector.empty)) { (done, card) =>
        done.flatMap(ops => card match
          case denizen: DenizenState => catalog.suitOf(denizen.id)
            .toRight(OathViolation.UnknownWorldCard(denizen.id))
            .map(suit => ops :+ RemoveFromMap(denizen.id, from,
              PositionedLocation(Location.Dispossessed), suit,
              denizen.tokens.favor, denizen.tokens.secrets, actor))
          case edifice: EdificeState if edifice.side == EdificeSide.Ruined =>
            catalog.suitOf(edifice.id)
              .toRight(OathViolation.UnknownEdifice(edifice.id))
              .map(suit => ops :+ RemoveFromMap(edifice.id, from,
                PositionedLocation(Location.Deck(CardDeck.Edifice),
                  StackPosition.Bottom), suit, edifice.tokens.favor,
                edifice.tokens.secrets, actor))
          case _ => Right(ops))
      }

  private def draw(ready: ReadyGame, pilgrims: Vector[DenizenId],
      region: Region, actor: PlayerId): Vector[Operation] =
    val sent = pilgrims.count(ready.game.campaign.dispossessed.contains)
    val card = PowerSourceRef.Card(cardId)
    if sent == 0 then Vector(Note(id, _ => Some(none(card))))
    else Vector(
      DispossessedDraws.shuffle,
      BuildOps((now, _) => Right(DispossessedDraws.toPile(now, region, sent,
        Some(actor))), required = true),
      // Each peek is recorded under the key first, then run, so the peek is
      // the last leaf this step runs and the line restates it.
      BuildOps((now, _) => Right(drawnTopFirst(now, region, sent).flatMap {
        drawn =>
          val peek = Peek(actor, drawn, Location.RegionalDiscard(region))
          Vector(Defer(drawnKey, Vector(peek)), peek)
      })),
      Note(id, _ => Some(used(card, NoteArg.Number(sent),
        NoteArg.Number(sent))), covers = true))

  /** The drawn cards, top first, read from the held peeks. With nothing
    * held, nothing is asked. */
  private def inspect(actor: PlayerId): Operation =
    Deferred.resolve(drawnKey)((_, _, held) => Vector(Decide(
      inspectDecisionId, actor, DecisionQuery.Inspect(
        held.collect { case Peek(_, card, _) => card },
        heading = Some("Pilgrimage: the cards you drew")))))

  /** The `count` cards on top of `region`'s pile, top first. A pile is
    * stored top last. */
  private def drawnTopFirst(ready: ReadyGame, region: Region, count: Int)
      : Vector[CardId] =
    ready.game.current.commonCards.discard(region).takeRight(count).reverse
