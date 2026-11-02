package oathdigital.gameplay.powers.setup

import oathdigital.catalog.{Edifice, EdificeFace, Locked, PrintedPower}
import oathdigital.gameplay.powerresolver.{Contribution, ContributingPower,
  PowerCtx, Transform}
import oathdigital.model._

object HallOfDebateCard extends Edifice(EdificeId("E01"), Suit.Discord):
  object intact extends EdificeFace("Hall of Debate") with Locked:
    val power = PrintedPower(PowerId("edifice.e01.intact"),
      persistent = true, cost = Cost.free,
      text = "If this card's ruler holds the People's Favor, their " +
        "enemies **cannot** target the People's Favor. If ruled by " +
        "Empire, all Imperials have this power.")
    val powers: Vector[PrintedPower] = Vector(power)
  object ruined extends EdificeFace("Hall of Mockery"):
    val power = PrintedPower(PowerId("edifice.e01.ruined"),
      persistent = false, cost = Cost.free,
      text = "**SETUP (END) / WHEN EXPLORED:** Place [favor] [favor] " +
        "[favor] [favor] on the People's Favor and move it to the " +
        "shared bank—-players **cannot** start with it. Burn " +
        "[favor-burnt] from each favor bank.")
    val powers: Vector[PrintedPower] = Vector(power)

object TomeGuardiansCard extends Edifice(EdificeId("E18"), Suit.Order):
  object intact extends EdificeFace("Tome Guardians") with Locked:
    val power = PrintedPower(PowerId("edifice.e18.intact"),
      persistent = true, cost = Cost.free,
      text = "If this card's ruler holds the Darkest Secret, their " +
        "enemies **cannot** target the Darkest Secret. If ruled by " +
        "Empire, all Imperials have this power.")
    val powers: Vector[PrintedPower] = Vector(power)
  object ruined extends EdificeFace("Lost Guardians"):
    val power = PrintedPower(PowerId("edifice.e18.ruined"),
      persistent = false, cost = Cost.free,
      text = "**SETUP (END) / WHEN EXPLORED:** Place [secret] [secret] " +
        "[secret] on the Darkest Secret and move it to the shared " +
        "bank. Players **cannot** start with it.")
    val powers: Vector[PrintedPower] = Vector(power)

/** A SETUP (END) rule of a ruined edifice face, in the shape of E02's
  * `MarketRule` (catalog batch 9, slice 1). It names `WhenExplored` beside
  * `SetupEnd` so the same contribution serves an explore procedure once one
  * folds that window. It applies only while its face stands on the map.
  */
abstract class RuinedSetupRule extends ContributingPower:
  def edifice: EdificeId

  final def source: RuleSourceRef = RuleSourceRef.GameRule(id.value)

  protected def at(ready: ReadyGame): Option[SiteId] =
    EdificeSetupSupport.siteOf(ready, edifice, EdificeSide.Ruined)

  override def applicable(ctx: PowerCtx): Boolean = at(ctx.state).isDefined

  protected def build(ready: ReadyGame, at: SiteId): Vector[CoreOperation]

  /** The line this face writes after its effect, read from that step. */
  protected def note(at: SiteId): Note

  final def contributions: Map[PowerWindow, Vector[Contribution]] =
    val effect = Vector(Transform((ctx, ops) => at(ctx.state) match {
      case Some(site) =>
        ops :+ BuildOps((ready, _) => Right(build(ready, site))) :+ note(site)
      case None => ops
    }))
    Map(PowerWindow.SetupEnd -> effect, PowerWindow.WhenExplored -> effect)

/** Hall of Mockery (E01, ruined): "SETUP (END): Place 4 favor on the
  * People's Favor and move it to the shared bank; players cannot start with
  * it. Burn 1 favor from each favor bank."
  *
  * The favor comes from the shared supply, as Bandit Market's does, and each
  * favor bank burns one with no burner, a bank already empty burning none.
  * The banner is not moved: in the all-Exile first game both banners start
  * in the shared bank, and "players cannot start with it" undoes a
  * later-game setup step the engine does not have (catalog batch 9, "Catalog
  * and roadmap corrections").
  *
  * Its line reads the step: "Placed 4 favor on the People's Favor and burned
  * 6 favor from the banks.", or only the placement when no bank held favor.
  */
case object HallOfMockery extends RuinedSetupRule:
  val edifice: EdificeId = HallOfDebateCard.id
  val id: PowerId = HallOfDebateCard.ruined.power.id
  val Favor: Int = 4
  private val notes = NoteSet(id)
  /** "Placed {4 favor} on the People's Favor and burned {6 favor} from the
    * banks." */
  val placedAndBurned: NoteKey = notes.key("placed-and-burned", Vector(
    NotePart.Text("Placed "), NotePart.Arg(0),
    NotePart.Text(" on the People's Favor and burned "), NotePart.Arg(1),
    NotePart.Text(" from the banks.")))
  /** "Placed {4 favor} on the People's Favor." */
  val placed: NoteKey = notes.key("placed", Vector(NotePart.Text("Placed "),
    NotePart.Arg(0), NotePart.Text(" on the People's Favor.")))
  override def noteKeys: Vector[NoteKey] = notes.declared

  protected def build(ready: ReadyGame, at: SiteId): Vector[CoreOperation] =
    Move(Piece.Favor(Favor), PositionedLocation(Location.SharedBank),
      PositionedLocation(Location.OnBanner(Banner.PeoplesFavor))) +:
      Suit.all.map(bank =>
        Burn.favor(1, PositionedLocation(Location.FavorBank(bank)), None))

  protected def note(at: SiteId): Note = Note(id, states =>
    states.previous.flatMap { (before, after) =>
      val gained = after.game.current.banners.peoplesFavor.favor -
        before.game.current.banners.peoplesFavor.favor
      val burned = Suit.all.map(suit => before.banks.favor.getOrElse(suit, 0) -
        after.banks.favor.getOrElse(suit, 0)).sum
      val card = PowerSourceRef.Card(edifice)
      if gained > 0 && burned > 0 then Some(placedAndBurned(card,
        MarketRule.favor(gained), MarketRule.favor(burned)))
      else if gained > 0 then Some(placed(card, MarketRule.favor(gained)))
      else None
    })

/** Lost Guardians (E18, ruined): "SETUP (END): Place 3 secrets on the
  * Darkest Secret and move it to the shared bank. Players cannot start with
  * it."
  *
  * The secrets come from the shared supply. The banner is not moved, for
  * the reason Hall of Mockery's doc gives. Its line reads the step: "Placed
  * 3 secrets on the Darkest Secret."
  */
case object LostGuardians extends RuinedSetupRule:
  val edifice: EdificeId = TomeGuardiansCard.id
  val id: PowerId = TomeGuardiansCard.ruined.power.id
  val Secrets: Int = 3
  private val notes = NoteSet(id)
  /** "Placed {3 secrets} on the Darkest Secret." */
  val placed: NoteKey = notes.key("placed", Vector(NotePart.Text("Placed "),
    NotePart.Arg(0), NotePart.Text(" on the Darkest Secret.")))
  override def noteKeys: Vector[NoteKey] = notes.declared

  protected def build(ready: ReadyGame, at: SiteId): Vector[CoreOperation] =
    Vector(Move(Piece.Secrets(Secrets),
      PositionedLocation(Location.SharedBank),
      PositionedLocation(Location.OnBanner(Banner.DarkestSecret))))

  protected def note(at: SiteId): Note = Note(id, states => for
    (before, after) <- states.previous
    gained = after.game.current.banners.darkestSecret.secrets -
      before.game.current.banners.darkestSecret.secrets
    if gained > 0
  yield placed(PowerSourceRef.Card(edifice),
    NoteArg.Amount(gained, NoteUnit.Secret)))
