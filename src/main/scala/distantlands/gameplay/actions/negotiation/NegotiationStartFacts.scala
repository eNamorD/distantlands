package distantlands.gameplay.actions.negotiation

import distantlands.catalog.ExecutableCatalog
import distantlands.gameplay.powerresolver.PowerUseRestriction
import distantlands.model._

/** What a Negotiation's start settles for its whole walk, stored as
  * `walkerStartFacts` (catalog batch 12, slices 2 and 3): the players the
  * actor may choose (`candidates`), and the players among the actor and
  * the candidates who may use The Tribunal (`tribunalUsers`), Council
  * Arbiter (`arbiterUsers`), Festival District (`festivalUsers`) and Deed
  * Writer (`deedWriterUsers`).
  *
  * Each reads the walk's power-use restrictions, which a rebuild does not
  * carry, and each could change once the deal settles: Citizenship changes
  * a role, which Bandit Forest reads, and an adviser term can move Council
  * Arbiter. A site term moves no card, but Deed Writer is non-persistent,
  * so its users are read under the restrictions too. So the start computes
  * them, and every rebuild reads them back, as Travel reads its source.
  *
  * Stored as option refs: each candidate as a player, then for each user
  * list a `Button` holding its marker followed by its players.
  */
final case class NegotiationStartFacts(candidates: Vector[PlayerId],
    tribunalUsers: Vector[PlayerId],
    arbiterUsers: Vector[PlayerId] = Vector.empty,
    festivalUsers: Vector[PlayerId] = Vector.empty,
    deedWriterUsers: Vector[PlayerId] = Vector.empty):
  def refs: Vector[DecisionOptionRef] =
    candidates.map(DecisionOptionRef.Player(_)) ++
      NegotiationStartFacts.Markers.zip(Vector(tribunalUsers, arbiterUsers,
        festivalUsers, deedWriterUsers)).flatMap((marker, users) =>
        DecisionOptionRef.Button(marker) +: users.map(DecisionOptionRef.Player(_)))

  /** Whether a deal among `participants` takes binding promises: one of
    * them may use The Tribunal (ruling 11). */
  def bindingPromises(participants: Vector[PlayerId]): Boolean =
    participants.exists(tribunalUsers.contains)

  /** What a deal among `participants` allows (rulings 11, 14, 15 and
    * 18). */
  def rules(participants: Vector[PlayerId]): DealRules =
    DealRules(arbiterUsers.filter(participants.contains),
      festivalUsers.exists(participants.contains),
      bindingPromises(participants),
      siteTraders = deedWriterUsers.filter(participants.contains))

object NegotiationStartFacts:
  /** The key of the `Button` ref before the Tribunal's users. */
  val Marker: String = "negotiation.tribunal-users"
  val ArbiterMarker: String = "negotiation.arbiter-users"
  val FestivalMarker: String = "negotiation.festival-users"
  val DeedWriterMarker: String = "negotiation.deed-writer-users"
  private val Markers: Vector[String] =
    Vector(Marker, ArbiterMarker, FestivalMarker, DeedWriterMarker)

  /** The facts at a fresh start. A private start (Whispering Stone,
    * ruling 21) offers every other player, wherever their pawn stands. */
  def at(catalog: ExecutableCatalog, ready: ReadyGame, actor: PlayerId,
      restrictions: Vector[PowerUseRestriction],
      privately: Boolean = false): NegotiationStartFacts =
    val candidates =
      if privately then
        ready.game.current.players.map(_.player).filter(_ != actor)
      else NegotiationDeal.eligible(catalog, ready, actor, restrictions)
    val players = actor +: candidates
    NegotiationStartFacts(candidates,
      NegotiationReach.tribunalUsers(catalog, ready, players, restrictions),
      NegotiationAdvisers.arbiterUsers(catalog, ready, players, restrictions),
      NegotiationAdvisers.festivalUsers(catalog, ready, players,
        restrictions),
      NegotiationSites.deedWriterUsers(catalog, ready, players, restrictions))

  /** The facts `refs` store: every marker once, in order, with only
    * players around them. */
  def read(refs: Vector[DecisionOptionRef])
      : Either[OathViolation, NegotiationStartFacts] =
    sections(refs).toRight(malformed(refs)).flatMap(parts =>
      parts.foldLeft[Either[OathViolation, Vector[Vector[PlayerId]]]](
        Right(Vector.empty))((done, part) =>
        done.flatMap(lists => players(part, refs).map(lists :+ _))))
      .map(lists => NegotiationStartFacts(lists(0), lists(1), lists(2),
        lists(3), lists(4)))

  /** `refs` cut at each marker in order, or `None` when one is missing. */
  private def sections(refs: Vector[DecisionOptionRef])
      : Option[Vector[Vector[DecisionOptionRef]]] =
    Markers.foldLeft(Option((Vector.empty[Vector[DecisionOptionRef]], refs))) {
      case (Some((done, rest)), marker) =>
        val (before, after) = rest.span(_ != DecisionOptionRef.Button(marker))
        Option.when(after.nonEmpty)((done :+ before, after.drop(1)))
      case (None, _) => None
    }.map((done, rest) => done :+ rest)

  private def players(part: Vector[DecisionOptionRef],
      refs: Vector[DecisionOptionRef]): Either[OathViolation, Vector[PlayerId]] =
    val ids = part.collect { case DecisionOptionRef.Player(id) => id }
    Either.cond(ids.size == part.size, ids, malformed(refs))

  private def malformed(refs: Vector[DecisionOptionRef]): OathViolation =
    OathViolation.InvalidEventOrder(
      s"walker procedure ${ActionRef.Negotiation.key} has malformed start " +
        s"facts: ${refs.map(_.kind).mkString(", ")}")
