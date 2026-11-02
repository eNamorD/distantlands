package oathdigital.application

import oathdigital.model._

/** What a player asks the game to do, every identifier a model type. Mirrors
  * `protocol.GameIntent` case for case; `GameIntentMapper` is the only
  * conversion. The player asking is the actor of the `GameCommand.Issued`
  * that carries the intent, so no case names them. */
sealed trait PlayerIntent extends Product with Serializable:
  /** Whether a batch opens for this intent, so a `CommandAccepted(actor)` is
    * written and Undo can take the batch back (undo design, "Terms"). Undo,
    * Redo and the consent intents append their own events alone. */
  def opensBatch: Boolean = false
object PlayerIntent:
  /** Takes back the latest live command batch, which the actor must have
    * issued (undo design, "Rulings"). */
  case object Undo extends PlayerIntent
  /** Restores the batch most recently taken back since the last command
    * batch, which the actor must have issued (undo design, "Redo"). */
  case object Redo extends PlayerIntent
  /** Answers the parked walker decision, as the actor. */
  final case class ResolveWalker(treeDecision: TreeDecision)
      extends PlayerIntent:
    override def opensBatch = true
  /** An intent the service routes to its consent desk, never to the rules'
    * walker gate: each is legal over a parked decision (consent design,
    * "Commands"). */
  sealed trait Consent extends PlayerIntent
  /** The actor sets their standing answer for `requester` on `power`. */
  final case class SetConsentPreference(power: PowerId, requester: PlayerId,
      setting: ConsentSetting) extends Consent
  /** The actor asks the rulers of the powers blocking `target` at the parked
    * `decisionId` for consent to choose it. `granters` names the
    * alternatives to ask; empty asks them all (catalog batch 8, slice 15). */
  final case class RequestConsent(decisionId: String,
      target: DecisionOptionRef, granters: Vector[PlayerId] = Vector.empty)
      extends Consent
  /** The actor answers the request waiting on them. */
  final case class AnswerConsent(choice: ConsentChoice) extends Consent

sealed trait GameCommand extends Product with Serializable
object GameCommand:
  /** The host starts a game. No actor: a journal still begins with
    * `GameStarted`. */
  final case class Begin(chronicle: Chronicle, orders: SetupOrders)
      extends GameCommand
  /** `actor` asks for `intent`. The transport resolved the actor; the mapper
    * typed the intent; nothing else is needed to build one. */
  final case class Issued(actor: PlayerId, intent: PlayerIntent)
      extends GameCommand

  /** The player a command's batch is recorded for, in the `CommandAccepted`
    * that opens it (undo design, "Terms"): the actor of an intent that
    * opens a batch. Begin has none, so a journal still begins with
    * `GameStarted`. */
  def issuer(command: GameCommand): Option[PlayerId] = command match
    case Issued(actor, intent) if intent.opensBatch => Some(actor)
    case _ => None

  // The old one-case-per-intent spellings, kept while the test suites still
  // write them; the next slice retires them. Each builds and matches the
  // `Issued` it stands for.
  object Undo:
    def apply(actor: PlayerId): GameCommand = Issued(actor, PlayerIntent.Undo)
    def unapply(command: GameCommand): Option[PlayerId] = command match
      case Issued(actor, PlayerIntent.Undo) => Some(actor)
      case _ => None
  object Redo:
    def apply(actor: PlayerId): GameCommand = Issued(actor, PlayerIntent.Redo)
    def unapply(command: GameCommand): Option[PlayerId] = command match
      case Issued(actor, PlayerIntent.Redo) => Some(actor)
      case _ => None
  object ResolveWalker:
    def apply(actor: PlayerId, treeDecision: TreeDecision): GameCommand =
      Issued(actor, PlayerIntent.ResolveWalker(treeDecision))
    def unapply(command: GameCommand): Option[(PlayerId, TreeDecision)] =
      command match
        case Issued(actor, PlayerIntent.ResolveWalker(treeDecision)) =>
          Some((actor, treeDecision))
        case _ => None
  object SetConsentPreference:
    def apply(granter: PlayerId, power: PowerId, requester: PlayerId,
        setting: ConsentSetting): GameCommand =
      Issued(granter, PlayerIntent.SetConsentPreference(power, requester,
        setting))
  object RequestConsent:
    def apply(requester: PlayerId, decisionId: String,
        target: DecisionOptionRef,
        granters: Vector[PlayerId] = Vector.empty): GameCommand =
      Issued(requester, PlayerIntent.RequestConsent(decisionId, target,
        granters))
  object AnswerConsent:
    def apply(granter: PlayerId, choice: ConsentChoice): GameCommand =
      Issued(granter, PlayerIntent.AnswerConsent(choice))

/** One answer to the currently parked generic walker decision. */
final case class TreeDecision(decisionId: String, answer: DecisionAnswer)
