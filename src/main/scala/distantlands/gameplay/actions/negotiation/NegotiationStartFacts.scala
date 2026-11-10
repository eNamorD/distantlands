package distantlands.gameplay.actions.negotiation

import distantlands.catalog.ExecutableCatalog
import distantlands.gameplay.powerresolver.PowerUseRestriction
import distantlands.model._

/** What a Negotiation's start settles for its whole walk, stored as
  * `walkerStartFacts` (catalog batch 12, slice 2): the players the actor
  * may choose (`candidates`), and the players among the actor and the
  * candidates who may use The Tribunal (`tribunalUsers`).
  *
  * Both read the walk's power-use restrictions, which a rebuild does not
  * carry, and both could change once the deal settles: Citizenship changes
  * a role, which Bandit Forest reads. So the start computes them, and every
  * rebuild reads them back, as Travel reads its source.
  *
  * Stored as option refs: each candidate as a player, then a `Button`
  * holding [[NegotiationStartFacts.Marker]], then each Tribunal user as a
  * player.
  */
final case class NegotiationStartFacts(candidates: Vector[PlayerId],
    tribunalUsers: Vector[PlayerId]):
  def refs: Vector[DecisionOptionRef] =
    candidates.map(DecisionOptionRef.Player(_)) ++
      (DecisionOptionRef.Button(NegotiationStartFacts.Marker) +:
        tribunalUsers.map(DecisionOptionRef.Player(_)))

  /** Whether a deal among `participants` takes binding promises: one of
    * them may use The Tribunal (ruling 11). */
  def bindingPromises(participants: Vector[PlayerId]): Boolean =
    participants.exists(tribunalUsers.contains)

object NegotiationStartFacts:
  /** The key of the `Button` ref between the two lists. */
  val Marker: String = "negotiation.tribunal-users"

  def at(catalog: ExecutableCatalog, ready: ReadyGame, actor: PlayerId,
      restrictions: Vector[PowerUseRestriction]): NegotiationStartFacts =
    val candidates = NegotiationDeal.eligible(catalog, ready, actor,
      restrictions)
    NegotiationStartFacts(candidates, NegotiationReach.tribunalUsers(catalog,
      ready, actor +: candidates, restrictions))

  def read(refs: Vector[DecisionOptionRef])
      : Either[OathViolation, NegotiationStartFacts] =
    val (before, after) = refs.span(_ != DecisionOptionRef.Button(Marker))
    for
      _ <- Either.cond(after.nonEmpty, (), malformed(refs))
      candidates <- players(before, refs)
      users <- players(after.drop(1), refs)
    yield NegotiationStartFacts(candidates, users)

  private def players(part: Vector[DecisionOptionRef],
      refs: Vector[DecisionOptionRef]): Either[OathViolation, Vector[PlayerId]] =
    val ids = part.collect { case DecisionOptionRef.Player(id) => id }
    Either.cond(ids.size == part.size, ids, malformed(refs))

  private def malformed(refs: Vector[DecisionOptionRef]): OathViolation =
    OathViolation.InvalidEventOrder(
      s"walker procedure ${ActionRef.Negotiation.key} has malformed start " +
        s"facts: ${refs.map(_.kind).mkString(", ")}")
