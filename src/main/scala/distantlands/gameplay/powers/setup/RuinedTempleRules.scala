package distantlands.gameplay.powers.setup

import distantlands.catalog.ExecutableCatalog
import distantlands.gameplay.powers.PowerAnswers
import distantlands.gameplay.powers.action.TempleOfAncestorsCard
import distantlands.model._

/** Ruined Temple (E10 ruined), SETUP / WHEN EXPLORED: "If you place your
  * pawn here, you may burn a secret. If you do and then win the game,
  * score double the legacy points and take rule of all bandit sites."
  * (catalog batch 13, rulings 11 and 17). A placer with a secret is
  * asked, Keep first; on Burn, one secret burns, facedown first, since a
  * spent secret is never worth more, and `PledgeTemple` records the
  * pledge for the game. A player with no secret is offered nothing, and a
  * player who pledged is still offered it, so the offer stands while the
  * burn resolves (ruling 4). The win and the double read the pledge
  * (`RuinedTempleWin`). Line: "p2 burned a secret."; a kept secret writes
  * nothing. */
case object RuinedTemple extends PlacedPawnEdificeRule:
  val id: PowerId = TempleOfAncestorsCard.ruined.power.id
  def edifice: EdificeId = TempleOfAncestorsCard.id
  protected def side: EdificeSide = EdificeSide.Ruined
  protected def slug: String = "ruined-temple"

  def pledgeDecisionId(placer: Option[PlayerId]): String =
    decision("pledge", placer)
  val keep: DecisionOptionRef.Button = DecisionOptionRef.Button("keep")
  val burn: DecisionOptionRef.Button = DecisionOptionRef.Button("burn")

  private val notes = NoteSet(id)
  /** "p2 burned a secret." */
  val burned: NoteKey = notes.key(NoteKey.Used, Vector(NotePart.Arg(0),
    NotePart.Text(" burned a secret.")))
  override def noteKeys: Vector[NoteKey] = notes.declared
  /** The burn's line tells the answer. */
  override def narratedDecisions: Set[String] = Set(pledgeDecisionId(None))

  /** The secrets on `actor`'s board, faceup and facedown. */
  def secretsOf(ready: ReadyGame, actor: PlayerId): Int =
    ready.game.current.player(actor).fold(0)(player =>
      player.board.faceUpSecrets + player.board.faceDownSecrets)

  override protected def offers(ready: ReadyGame, actor: PlayerId): Boolean =
    secretsOf(ready, actor) > 0 || ready.game.current.templePledges(actor)

  protected def steps(catalog: ExecutableCatalog, actor: PlayerId,
      placer: Option[PlayerId]): Vector[Operation] =
    val ask = pledgeDecisionId(placer)
    // A pledged player is offered the Temple only so the offer stands
    // while their burn resolves; they are never asked again.
    Vector(Branch((live, _) =>
      if secretsOf(live, actor) == 0 ||
          live.game.current.templePledges(actor) then Vector.empty
      else Vector(
        Decide(ask, actor, DecisionQuery.ChooseOne(Vector(
          DecisionOption.Button(keep, "Keep your secrets"),
          DecisionOption.Button(burn, "Burn a secret")),
          heading = Some("Ruined Temple: you may burn a secret; if you " +
            "then win, score double the legacy points and take rule of " +
            "all bandit sites"))),
        Branch((_, pending) =>
          if !PowerAnswers.one(pending, ask).contains(burn) then Vector.empty
          else Vector(
            BuildOps((ready, _) => Right(burnOne(ready, actor) :+
              PledgeTemple(actor)), required = true),
            Note(id, _ => Some(burned(card, NoteArg.Player(actor)))))))))

  /** One of `actor`'s secrets, facedown when they have one. */
  private def burnOne(ready: ReadyGame, actor: PlayerId)
      : Vector[CoreOperation] =
    val facedown = ready.game.current.player(actor)
      .exists(_.board.faceDownSecrets > 0)
    val secretSide =
      if facedown then SecretSide.FaceDown else SecretSide.FaceUp
    Vector(Burn.secrets(1, PositionedLocation(Location.PlayArea(actor),
      side = Some(secretSide)), Some(actor)))
