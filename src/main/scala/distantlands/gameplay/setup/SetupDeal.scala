package distantlands.gameplay.setup

import distantlands.gameplay.chronicle.ChronicleLegacyRules
import distantlands.model._

/** Foundation IV's deal of the starting denizen stack: the one arithmetic
  * the plan factory, game start and the materializer share (CR p. 11;
  * Foundations design, slice 2). `draws` lists every seated player in seat
  * order with the number of denizens they draw; `stack` below is always the
  * deal's `denizenOrder`.
  *
  * Ties of Fellowship, the normal face: the first 6 cards seed the discard
  * piles, 2 each, then each player with no starting adviser draws a hand of
  * 3 to choose one adviser from, and the rest of the stack goes under the
  * Visions as the world deck.
  *
  * Teeming World, the altered face: each player draws until they hold three
  * starting advisers and keeps every card drawn, or, holding Oaths of
  * Loyalty, buries every card drawn (`buried`), then the whole rest of the
  * stack is dealt to the discard piles one card at a time, each card
  * beneath the ones its pile already holds, and the world deck gets no
  * denizen.
  */
final case class SetupDeal(face: FoundationFace,
    draws: Vector[(PlayerId, Int)],
    /** The players whose draws Oaths of Loyalty buries under Teeming World
      * (Legacy Chronicle powers R24, re-ruled 2026-11-14). */
    burying: Set[PlayerId] = Set.empty):
  private val teeming = face == FoundationFace.Altered
  /** Ties of Fellowship seeds each discard pile before the hands. */
  private val seeded = if teeming then 0 else 2 * Region.all.size
  private val drawn = draws.map(_._2).sum

  /** The fewest denizens the stack must hold for this deal. */
  def needed: Int = seeded + drawn

  /** The cards `player` draws, in the order drawn: players draw in seat
    * order, each all their cards at once. */
  private def drawnBy(stack: Vector[DenizenId], player: PlayerId)
      : Vector[DenizenId] =
    val seat = draws.indexWhere(_._1 == player)
    if seat < 0 then Vector.empty
    else
      val from = seeded + draws.take(seat).map(_._2).sum
      stack.slice(from, from + draws(seat)._2)

  /** The hand `player` chooses one adviser from in Setup: their draws under
    * Ties of Fellowship, nothing under Teeming World. */
  def hand(stack: Vector[DenizenId], player: PlayerId): Vector[DenizenId] =
    if teeming then Vector.empty else drawnBy(stack, player)

  /** The denizens `player` keeps as facedown advisers without a choice:
    * their draws under Teeming World, nothing under Ties of Fellowship. */
  def kept(stack: Vector[DenizenId], player: PlayerId): Vector[DenizenId] =
    if teeming && !burying(player) then drawnBy(stack, player)
    else Vector.empty

  /** The denizens an Oaths of Loyalty holder buries at the bottom of the
    * world deck in place of keeping them: their draws under Teeming World,
    * nothing otherwise (Legacy Chronicle powers R24, re-ruled). */
  def buried(stack: Vector[DenizenId], player: PlayerId): Vector[DenizenId] =
    if teeming && burying(player) then drawnBy(stack, player)
    else Vector.empty

  /** The discard piles the deal leaves, each in stored order, top last.
    * Teeming World deals the rest of the stack to the Cradle, the Provinces
    * and the Hinterland in turn. The first card dealt to a pile is its top
    * and each later card goes beneath it, so the end of the stack, where
    * the alpha shuffle puts the unimplemented denizens, lies at the bottoms
    * of the piles and the first Search of a pile does not draw it. */
  def discards(stack: Vector[DenizenId]): Map[Region, Vector[WorldCardId]] =
    val piles = Region.all.zipWithIndex
    if teeming then
      val rest = stack.drop(drawn).zipWithIndex
      piles.map((region, pile) => region -> rest.collect {
        case (card, dealt) if dealt % piles.size == pile => card
      }.reverse).toMap
    else piles.map((region, pile) =>
      region -> stack.slice(2 * pile, 2 * pile + 2)).toMap

  /** The denizens left for the world deck, in stack order. */
  def deck(stack: Vector[DenizenId]): Vector[DenizenId] =
    if teeming then Vector.empty else stack.drop(needed)

object SetupDeal:
  /** The deal of the game `chronicle` starts for `participants`, under the
    * Chronicle's Foundation IV face. A lineage's stored starting advisers
    * count toward Teeming World's three, and stop a Ties of Fellowship
    * hand. */
  def of(chronicle: Chronicle, participants: Vector[FirstGameParticipant])
      : SetupDeal =
    val stored = chronicle.lineages.map(lineage =>
      lineage.id -> lineage.startingAdvisers.size).toMap
    // Under Teeming World an Oaths of Loyalty holder draws 3 - x, x the
    // starting advisers it kept, and buries every card drawn (Legacy
    // Chronicle powers design, ruling 16; R24, re-ruled 2026-11-14). Under
    // Ties of Fellowship they draw a hand as usual, and Setup buries the
    // one they pick.
    val oaths = chronicle.lineages.filter(ChronicleLegacyRules.oathsHolds)
      .map(_.id).toSet
    val face = StartingFoundations.of(chronicle)(FoundationNumber.IV).face
    SetupDeal(face, participants.map { participant =>
      val held = stored.getOrElse(participant.lineageId, 0)
      participant.playerId -> (face match
        case FoundationFace.Normal => if held == 0 then 3 else 0
        case FoundationFace.Altered => math.max(0, 3 - held))
    }, burying =
      if face == FoundationFace.Altered then participants.collect {
        case participant if oaths(participant.lineageId) => participant.playerId
      }.toSet
      else Set.empty)

  /** A first game's deal: Ties of Fellowship, and every player draws. */
  def firstGame(participants: Vector[FirstGameParticipant]): SetupDeal =
    SetupDeal(FoundationFace.Normal, participants.map(_.playerId -> 3))
