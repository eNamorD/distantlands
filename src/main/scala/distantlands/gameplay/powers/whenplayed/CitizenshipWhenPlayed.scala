package distantlands.gameplay.powers.whenplayed

import distantlands.gameplay.powerresolver.PowerCtx
import distantlands.gameplay.powers.{PowerAnswers, ReliquaryDraws}
import distantlands.gameplay.roles.{Citizenship, RoleNotes}
import distantlands.model._

/** A WHEN PLAYED "if you're an Exile and ..., you may [take a random relic
  * from the Reliquary and] become a Citizen": Long-Lost Heir, Bewitch,
  * Ancient Pact and Royal Ambitions.
  *
  * Read when the card resolves: a player who is not an Exile, or who does
  * not meet `condition`, is asked nothing and gets a line saying why.
  * Otherwise the player chooses to become a Citizen, shown unavailable
  * under the first-game option, or to stay an Exile. Becoming one first
  * takes the relic when `takesRelic` (ruling R22: an empty Reliquary takes
  * none), then runs `Citizenship.become`, which makes the player the
  * Chancellor in a game with no Empire.
  *
  * Lines: "Became a Citizen.", "Became the Chancellor.", "Declined
  * Citizenship.", "Was not an Exile." and the card's own unmet line. It
  * narrates its question.
  */
abstract class CitizenshipWhenPlayed(val cardId: DenizenId, val id: PowerId,
    name: String, condition: Option[CitizenshipWhenPlayed.Condition],
    takesRelic: Boolean) extends WhenPlayedPower:
  val decisionId: String = s"cardplay.${id.value.stripPrefix("denizen.")}" +
    ".citizenship"
  val become: DecisionOptionRef.Button = DecisionOptionRef.Button("become")
  val decline: DecisionOptionRef.Button = DecisionOptionRef.Button("decline")
  private val notes = NoteSet(id)
  val roleNotes: RoleNotes = RoleNotes.declare(notes)
  /** "Declined Citizenship." */
  val declined: NoteKey = notes.key("declined",
    Vector(NotePart.Text("Declined Citizenship.")))
  /** "Was not an Exile." */
  val notExile: NoteKey = notes.key("not-exile",
    Vector(NotePart.Text("Was not an Exile.")))
  /** The card's unmet condition, as "Did not hold the Darkest Secret." */
  val unmet: Option[NoteKey] = condition.map(needed =>
    notes.key("unmet", Vector(NotePart.Text(needed.unmet))))
  if takesRelic then notes.borrow(ReliquaryDraws.empty)
  override def noteKeys: Vector[NoteKey] = notes.declared
  override def narratedDecisions: Set[String] = Set(decisionId)

  private def card: PowerSourceRef = PowerSourceRef.Card(cardId)

  def effect(ctx: PowerCtx): Vector[Operation] =
    val actor = ctx.actor
    Vector(Branch((live, _) =>
      if !Imperials.role(live, actor).contains(Role.Exile) then
        Vector(Note(id, _ => Some(notExile(card))))
      else if condition.exists(needed => !needed.meets(live, actor)) then
        unmet.toVector.map(key => Note(id, _ => Some(key(card))))
      else ask(live, actor)))

  private def ask(ready: ReadyGame, actor: PlayerId): Vector[Operation] =
    Vector(
      Decide(decisionId, actor, DecisionQuery.ChooseOne(Vector(
        offer(ready), DecisionOption.Button(decline, "Stay an Exile")),
        heading = Some(s"$name: you may become a Citizen"))),
      Branch((_, pending) =>
        if PowerAnswers.one(pending, decisionId).contains(become) then
          (if takesRelic then ReliquaryDraws.steps(id, actor, card)
            else Vector.empty) ++ Vector(Citizenship.become(actor),
            Note(id, roleNotes.became(_, actor, card)))
        else Vector(Note(id, _ => Some(declined(card))))))

  private def offer(ready: ReadyGame): DecisionOption =
    val button = DecisionOption.Button(become, "Become a Citizen")
    if Citizenship.ignored(ready) then DecisionOption.Unavailable(button,
      OptionReason.Written(Citizenship.ignoredReason))
    else button

object CitizenshipWhenPlayed:
  /** A card's condition beyond being an Exile, and the line when it is not
    * met, as "Did not hold the Darkest Secret." */
  final case class Condition(meets: (ReadyGame, PlayerId) => Boolean,
      unmet: String)

  /** Whether `player`'s count is strictly more than every other seated
    * player's (ruling R21): a tie is not the most. */
  def most(ready: ReadyGame, player: PlayerId)(count: PlayerId => Int)
      : Boolean =
    val mine = count(player)
    ready.game.current.players.map(_.player).filter(_ != player)
      .forall(count(_) < mine)
