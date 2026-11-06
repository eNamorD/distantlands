package oathdigital.gameplay.powers.action

import oathdigital.catalog.{Edifice, EdificeFace, ExecutableCatalog, Locked,
  PrintedPower}
import oathdigital.gameplay.NoteSupport
import oathdigital.gameplay.powers.PowerAnswers
import oathdigital.model._

object UndergroundLibraryCard extends Edifice(EdificeId("E13"), Suit.Arcane):
  object intact extends EdificeFace("Underground Library") with Locked:
    val power = PrintedPower(PowerId("edifice.e13.intact"),
      persistent = false, cost = Cost(favor = 1),
      text = "**ACTION:** Peek at all relics in this region, then swap " +
        "one with a relic you have, placing it facedown.")
    val powers: Vector[PrintedPower] = Vector(power)
  object ruined extends EdificeFace("Drowned Library"):
    val power = PrintedPower(PowerId("edifice.e13.ruined"),
      persistent = false, cost = Cost(secret = 1),
      text = "If your pawn is at this site, you may recover the top card " +
        "of the relic deck instead. Its recover difficulty is 5.")
    val powers: Vector[PrintedPower] = Vector(power)

/** Underground Library (E13, intact), ACTION: place 1 favor on this card,
  * peek at every relic at the sites of the Library's region, then swap one
  * of them with a relic you have, placing it facedown (catalog batch 9,
  * slice 10).
  *
  * Every relic in the region is recorded as a `Peek`, so the user may
  * identify each, as Barbed Net peeks at a site's. The user then chooses
  * the site relic to take, by its slot, and the relic of theirs to give, by
  * its slot (owner question 10: faceup or facedown). A faceup given relic
  * is flipped facedown first, since a site holds facedown relics only, and
  * the two are swapped in one `Swap`, so the taken relic arrives facedown
  * as it lay. The swap is required, so a relic a restriction refuses
  * (Ancient Bloodline) is marked on the question. With no relic in the
  * region, or none held, nothing is asked and the cost stays paid.
  *
  * Lines: "{Blue} peeked at the relics in the Cradle: {relics}.", covering
  * the peeks, then "Swapped {given} for {taken} at {site}.", covering the
  * swap, or "No relic could be swapped." The ruined face, Drowned Library,
  * needs a Recover whose source a power sets and is not implemented.
  */
case object UndergroundLibrary
    extends PaidAction(UndergroundLibraryCard.intact.power):
  val edifice: EdificeId = UndergroundLibraryCard.id
  val takeDecisionId: String = "power.underground-library.take"
  val giveDecisionId: String = "power.underground-library.give"
  private val notes = NoteSet(id)
  /** "{Blue} peeked at the relics in the {Cradle}: {relics}." */
  val peeked: NoteKey = notes.key("used.peeked", Vector(NotePart.Arg(0),
    NotePart.Text(" peeked at the relics in the "), NotePart.Arg(1),
    NotePart.Text(": "), NotePart.Arg(2), NotePart.Text(".")))
  /** "Swapped {given} for {taken} at {site}." */
  val swapped: NoteKey = notes.key(NoteKey.Used, Vector(
    NotePart.Text("Swapped "), NotePart.Arg(0), NotePart.Text(" for "),
    NotePart.Arg(1), NotePart.Text(" at "), NotePart.Arg(2),
    NotePart.Text(".")))
  /** "No relic could be swapped." */
  val nothing: NoteKey = notes.key("used.none", Vector(
    NotePart.Text("No relic could be swapped.")))
  override def noteKeys: Vector[NoteKey] = notes.declared
  override def narratedDecisions: Set[String] =
    Set(takeDecisionId, giveDecisionId)

  /** A relic at a site of the Library's region, by its slot there. */
  final case class SiteRelic(site: SiteId, index: Int, relic: RelicId):
    def ref: DecisionOptionRef = DecisionOptionRef.SiteRelicSlot(site, index)

  /** A relic the user holds, by its slot. */
  final case class HeldRelic(slot: Int, relic: RelicState):
    def ref(owner: PlayerId): DecisionOptionRef =
      DecisionOptionRef.RelicSlot(owner, slot)

  def region(ready: ReadyGame): Option[Region] =
    ready.game.current.edificeSite(edifice, EdificeSide.Intact)
      .flatMap(ready.game.current.map.regionOf)

  /** Every relic at a site of the region, in map then slot order. */
  def relicsInRegion(ready: ReadyGame): Vector[SiteRelic] =
    val map = ready.game.current.map
    region(ready).toVector.flatMap(home => map.inPlay
      .filter(map.regionOf(_).contains(home)).flatMap(site =>
        map.sites.get(site).toVector.flatMap(_.relics.zipWithIndex).map(
          (relic, index) => SiteRelic(site, index, relic.id))))

  def heldRelics(ready: ReadyGame, player: PlayerId): Vector[HeldRelic] =
    ready.game.current.player(player).toVector
      .flatMap(_.relics.zipWithIndex).map((relic, slot) =>
        HeldRelic(slot, relic))

  /** Whether anything may be swapped: a relic in the region and one held. */
  private def swappable(ready: ReadyGame, player: PlayerId): Boolean =
    relicsInRegion(ready).nonEmpty && heldRelics(ready, player).nonEmpty

  def build(catalog: ExecutableCatalog, ready: ReadyGame, player: PlayerId,
      source: DecisionOptionRef)
      : Either[OathViolation, Operation] = Right(Sequence(Vector[Operation](
    BuildOps((live, _) => Right(relicsInRegion(live).map(found =>
      Peek(player, found.relic, Location.Site(found.site))))),
    Note(id, peekedNote(_, player), covers = true),
    Branch((live, _) => Option.when(swappable(live, player))(
      Decide(takeDecisionId, player, DecisionQuery.ChooseOne(
        relicsInRegion(live).map(found => DecisionOption.SiteRelicSlot(
          DecisionOptionRef.SiteRelicSlot(found.site, found.index))),
        heading = Some("Underground Library: take which relic?")))).toVector),
    Branch((live, pending) => Option.when(swappable(live, player) &&
        PowerAnswers.one(pending, takeDecisionId).nonEmpty)(
      Decide(giveDecisionId, player, DecisionQuery.ChooseOne(
        heldRelics(live, player).map(held => DecisionOption.RelicSlot(
          DecisionOptionRef.RelicSlot(player, held.slot))),
        heading = Some("Underground Library: give which of your relics?"))))
      .toVector),
    BuildOps((live, pending) => swap(live, player, pending),
      required = true),
    Note(id, swappedNote(_, player), covers = true))))

  /** The flip of a faceup given relic, then the swap. Nothing without the
    * answers: nothing could be swapped. */
  private def swap(ready: ReadyGame, player: PlayerId, pending: PendingTree)
      : Either[OathViolation, Vector[CoreOperation]] =
    (PowerAnswers.one(pending, takeDecisionId),
        PowerAnswers.one(pending, giveDecisionId)) match
      case (Some(takeRef), Some(giveRef)) =>
        exchange(ready, player, takeRef, giveRef)
      case _ => Right(Vector.empty)

  private def exchange(ready: ReadyGame, player: PlayerId,
      takeRef: DecisionOptionRef, giveRef: DecisionOptionRef)
      : Either[OathViolation, Vector[CoreOperation]] =
    for
      taken <- relicsInRegion(ready).find(_.ref == takeRef).toRight(
        OathViolation.InvalidEventOrder(
          s"${takeRef.wireId} is not a relic in the Library's region"))
      offered <- heldRelics(ready, player).find(_.ref(player) == giveRef)
        .toRight(OathViolation.InvalidEventOrder(
          s"${giveRef.wireId} is not a relic the user holds"))
    yield
      val flipped = Option.when(
        offered.relic.orientation == Orientation.FaceUp)(Flip(
        offered.relic.id, Location.PlayArea(player), Orientation.FaceDown))
      flipped.toVector :+ Swap(taken.relic,
        PositionedLocation(Location.Site(taken.site)), offered.relic.id,
        PositionedLocation(Location.PlayArea(player)), by = Some(player))

  private def peekedNote(states: NoteStates, player: PlayerId)
      : Option[LogNote] = for
    home <- region(states.now)
    relics = relicsInRegion(states.now).map(_.relic)
    if relics.nonEmpty
  yield peeked(PowerSourceRef.Card(edifice), NoteArg.Player(player),
    NoteArg.RegionName(home), NoteArg.Cards(relics))

  /** The swap's line, read from the answers; without them, the empty one. */
  private def swappedNote(states: NoteStates, player: PlayerId)
      : Option[LogNote] =
    val card = PowerSourceRef.Card(edifice)
    (NoteSupport.answer(states, takeDecisionId),
        NoteSupport.answer(states, giveDecisionId), states.previous) match
      case (Some(DecisionOptionRef.SiteRelicSlot(site, index)),
          Some(DecisionOptionRef.RelicSlot(_, slot)), Some((before, _))) =>
        for
          taken <- relicsInRegion(before).find(found =>
            found.site == site && found.index == index)
          offered <- heldRelics(before, player).find(_.slot == slot)
        yield swapped(card, NoteArg.Card(offered.relic.id),
          NoteArg.Card(taken.relic), NoteArg.Site(site))
      case _ => Some(nothing(card))
