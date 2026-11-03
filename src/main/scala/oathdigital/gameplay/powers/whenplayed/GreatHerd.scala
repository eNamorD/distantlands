package oathdigital.gameplay.powers.whenplayed

import oathdigital.catalog.{Denizen, ExecutableCatalog, PrintedPower,
  SiteOnly}
import oathdigital.gameplay.{NoteSupport, SiteRulers}
import oathdigital.gameplay.powerresolver.PowerCtx
import oathdigital.gameplay.powers.PowerAnswers
import oathdigital.model._

object GreatHerdCard extends Denizen(DenizenId("30"), "Great Herd", Suit.Nomad) with SiteOnly:
  val power = PrintedPower(PowerId("denizen.great-herd"),
    persistent = false, cost = Cost.free,
    text = "**WHEN PLAYED:** you may swap Great Herd with a " +
      "[suit-nomad] card at any site.")
  val powers: Vector[PrintedPower] = Vector(power)

/** Great Herd (card 30, site-only), WHEN PLAYED: you may swap Great Herd
  * with a Nomad card at any site.
  *
  * The candidates are the faceup Nomad denizens and ruined Nomad edifices at
  * every other site in play, whoever rules it, read live and in map order.
  * With none, nothing is asked and the line says so. Otherwise the actor
  * picks one or keeps Great Herd. The swap is a required batch, so the
  * walker's search hides a card whose swap a restriction refuses. Each card
  * keeps its favor and secrets, and the card moved in does not run its own
  * WHEN PLAYED. The effect is one `Branch`, so the node count this power
  * adds to the card-played window never depends on live state.
  */
case object GreatHerd extends WhenPlayedPower:
  val id: PowerId = GreatHerdCard.power.id
  val decisionId: String = "cardplay.great-herd.swap"
  val keep: DecisionOptionRef.Button = DecisionOptionRef.Button("keep")
  private val notes = NoteSet(id)
  override def noteKeys: Vector[NoteKey] = notes.declared
  /** "{Red} swapped it with {card} at {site}." */
  val swapped: NoteKey = notes.key("swapped", Vector(NotePart.Arg(0),
    NotePart.Text(" swapped it with "), NotePart.Arg(1), NotePart.Text(" at "),
    NotePart.Arg(2), NotePart.Text(".")))
  /** "No Nomad card could be swapped." */
  val none: NoteKey = notes.key("none", Vector(
    NotePart.Text("No Nomad card could be swapped.")))

  private final case class Candidate(site: SiteId, card: CardId,
      option: DecisionOption)

  val cardId: DenizenId = GreatHerdCard.id

  override def narratedDecisions: Set[String] = Set(decisionId)

  def effect(ctx: PowerCtx): Vector[Operation] =
    Vector(Branch((live, _) =>
      val found = candidates(ctx.catalog, live)
      if found.isEmpty then Vector(Note(id, _ =>
        Some(none(PowerSourceRef.Card(cardId)))))
      else Vector(
        Decide(decisionId, ctx.actor, DecisionQuery.ChooseOne(
          found.map(_.option) :+ DecisionOption.Button(keep, "Keep Great Herd"),
          heading = Some("Great Herd: swap it with a Nomad card at another " +
            "site?"))),
        BuildOps((ready, pending) =>
          swap(ctx.catalog, ready, ctx.actor, pending),
          required = true),
        Note(id, swappedNote(ctx.catalog, _, ctx.actor)))))

  /** The faceup Nomad denizens and ruined Nomad edifices at every other
    * site in play, in map order. */
  private def candidates(catalog: ExecutableCatalog,
      ready: ReadyGame): Vector[Candidate] =
    val map = ready.game.current.map
    SiteRulers.siteOf(ready, cardId).toVector.flatMap(here => map.inPlay
      .filter(_ != here).flatMap(site => map.sites.get(site).toVector
        .flatMap(_.denizens.collect {
          case DenizenState(card, Orientation.FaceUp, _)
              if nomad(catalog, card) =>
            Candidate(site, card, DecisionOption.Denizen(
              DecisionOptionRef.Denizen(card)))
          case EdificeState(card, EdificeSide.Ruined, _)
              if nomad(catalog, card) =>
            Candidate(site, card, DecisionOption.Edifice(
              DecisionOptionRef.Edifice(card)))
        })))

  private def nomad(catalog: ExecutableCatalog, card: CardId): Boolean =
    catalog.suitOf(card).contains(Suit.Nomad)

  private def swap(catalog: ExecutableCatalog,
      ready: ReadyGame, actor: PlayerId, pending: PendingTree)
      : Either[OathViolation, Vector[CoreOperation]] =
    PowerAnswers.one(pending, decisionId) match
      case Some(`keep`) => Right(Vector.empty)
      case Some(ref) => swapWith(catalog, ready, actor, ref)
        .toRight(OathViolation
        .InvalidEventOrder(s"${ref.wireId} is not a Nomad card Great Herd " +
          "can swap with"))
      case None => Left(PowerAnswers.missing(decisionId))

  private def swapWith(catalog: ExecutableCatalog,
      ready: ReadyGame, actor: PlayerId,
      ref: DecisionOptionRef)
      : Option[Vector[CoreOperation]] = for
    here <- SiteRulers.siteOf(ready, cardId)
    chosen <- candidates(catalog, ready).find(_.option.ref == ref)
  yield Vector(Swap(cardId, PositionedLocation(Location.Site(here)),
    chosen.card, PositionedLocation(Location.Site(chosen.site)),
    by = Some(actor)))

  /** Written only when the chosen card now stands where Great Herd stood. */
  private def swappedNote(catalog: ExecutableCatalog,
      states: NoteStates, actor: PlayerId)
      : Option[LogNote] = for
    ref <- NoteSupport.answer(states, decisionId)
    (before, after) <- states.previous
    here <- SiteRulers.siteOf(before, cardId)
    chosen <- candidates(catalog, before).find(_.option.ref == ref)
    if after.game.current.map.sites.get(here)
      .exists(_.denizens.exists(_.id == chosen.card))
  yield swapped(PowerSourceRef.Card(cardId), NoteArg.Player(actor),
    NoteArg.Card(chosen.card), NoteArg.Site(chosen.site))
