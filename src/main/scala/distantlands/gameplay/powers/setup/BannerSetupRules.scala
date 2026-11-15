package distantlands.gameplay.powers.setup

import distantlands.catalog.{Edifice, EdificeFace, Locked, ModifierMark,
  PrintedPower}
import distantlands.gameplay.powerresolver.{Contribution, ContributingPower,
  PowerCtx, Transform}
import distantlands.model._

object HallOfDebateCard extends Edifice(EdificeId("E01"), Suit.Discord):
  object intact extends EdificeFace("Hall of Debate") with Locked:
    val power = PrintedPower(PowerId("edifice.e01.intact"),
      persistent = true, cost = Cost.free,
      modifier = Some(ModifierMark.Action(MajorActionType.Campaign)),
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
      modifier = Some(ModifierMark.Action(MajorActionType.Campaign)),
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

/** A ruined edifice face that adds steps at SETUP (END), in the shape of
  * E02's `MarketRule` (catalog batch 9, slice 1). It names `WhenExplored`
  * beside `SetupEnd`, so the same contribution serves an Explore, which
  * folds that window for the site it places. It applies only while its
  * face stands on the map, and at an Explore only at the explored site
  * (`EdificeSetupSupport.resolvesAt`).
  *
  * Most faces are a [[RuinedSetupRule]]: one batch, then a line. Fallen
  * Spire returns its own steps, since its shuffles are tree nodes the
  * walker orders, never batch members (catalog batch 10, slice 6).
  */
abstract class RuinedSetupSteps extends ContributingPower:
  def edifice: EdificeId

  final def source: RuleSourceRef = RuleSourceRef.GameRule(id.value)

  private def at(ctx: PowerCtx): Option[SiteId] =
    ctx.state.game.current.edificeSite(edifice, EdificeSide.Ruined)
      .filter(EdificeSetupSupport.resolvesAt(ctx, _))

  override def applicable(ctx: PowerCtx): Boolean = at(ctx).isDefined

  /** The steps this face adds at its window, for the face at `site`. */
  protected def steps(site: SiteId): Vector[Operation]

  final def contributions: Map[PowerWindow, Vector[Contribution]] =
    val effect = Vector(Transform((ctx, ops) => at(ctx) match {
      case Some(site) => ops ++ steps(site)
      case None => ops
    }))
    Map(PowerWindow.SetupEnd -> effect, PowerWindow.WhenExplored -> effect)

/** A ruined face whose SETUP (END) effect is one batch, then its line. */
abstract class RuinedSetupRule extends RuinedSetupSteps:
  protected def build(ready: ReadyGame, at: SiteId): Vector[CoreOperation]

  /** The line this face writes after its effect, read from that step. */
  protected def note(at: SiteId): Note

  protected final def steps(site: SiteId): Vector[Operation] =
    Vector(BuildOps((ready, _) => Right(build(ready, site))), note(site))

/** Hall of Mockery (E01, ruined): "SETUP (END): Place 4 favor on the
  * People's Favor and move it to the shared bank; players cannot start with
  * it. Burn 1 favor from each favor bank."
  *
  * The favor comes from the shared supply, as Bandit Market's does, and each
  * favor bank burns one with no burner, a bank already empty burning none.
  * A held banner goes back to the shared bank: in a later game the
  * Chancellor may start with it, and when explored anyone may hold it. A
  * banner already in the shared bank stays there.
  *
  * Its line reads the step: "Placed 4 favor on the People's Favor, moved it
  * to the shared bank and burned 6 favor from the banks.", leaving out the
  * move when nobody held the banner and the burn when no bank held favor.
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
  /** "Placed {4 favor} on the People's Favor, moved it to the shared bank
    * and burned {6 favor} from the banks." */
  val placedMovedAndBurned: NoteKey = notes.key("placed-moved-and-burned",
    Vector(NotePart.Text("Placed "), NotePart.Arg(0),
      NotePart.Text(" on the People's Favor, moved it to the shared bank " +
        "and burned "), NotePart.Arg(1), NotePart.Text(" from the banks.")))
  /** "Placed {4 favor} on the People's Favor and moved it to the shared
    * bank." */
  val placedAndMoved: NoteKey = notes.key("placed-and-moved", Vector(
    NotePart.Text("Placed "), NotePart.Arg(0),
    NotePart.Text(" on the People's Favor and moved it to the shared bank.")))
  override def noteKeys: Vector[NoteKey] = notes.declared

  protected def build(ready: ReadyGame, at: SiteId): Vector[CoreOperation] =
    (Move(Piece.Favor(Favor), PositionedLocation(Location.SharedBank),
      PositionedLocation(Location.OnBanner(Banner.PeoplesFavor))) +:
      BannerToSharedBank.move(ready, Banner.PeoplesFavor).toVector) ++
      Suit.all.map(bank =>
        Burn.favor(1, PositionedLocation(Location.FavorBank(bank)), None))

  protected def note(at: SiteId): Note = Note(id, states =>
    states.previous.flatMap { (before, after) =>
      val gained = after.game.current.banners.peoplesFavor.favor -
        before.game.current.banners.peoplesFavor.favor
      val burned = Suit.all.map(suit => before.banks.favor.getOrElse(suit, 0) -
        after.banks.favor.getOrElse(suit, 0)).sum
      val moved = BannerToSharedBank.moved(before, after, Banner.PeoplesFavor)
      val card = PowerSourceRef.Card(edifice)
      val (favor, bank) = (MarketRule.favor(gained), MarketRule.favor(burned))
      // The favor comes from the shared bank, which never runs short, in
      // the same batch as the banner's move: a step that moved the banner
      // always placed 4 favor. No favor placed means the step did not run,
      // so nothing moved either and no line is right.
      if gained == 0 then None
      else if moved && burned > 0 then
        Some(placedMovedAndBurned(card, favor, bank))
      else if moved then Some(placedAndMoved(card, favor))
      else if burned > 0 then Some(placedAndBurned(card, favor, bank))
      else Some(placed(card, favor))
    })

/** "Move it to the shared bank", as Hall of Mockery and Lost Guardians
  * print: a held banner leaves its holder for the shared bank, which holds
  * a banner nobody holds. One the shared bank already holds stays. */
object BannerToSharedBank:
  /** The move, while a player holds `banner`. */
  def move(ready: ReadyGame, banner: Banner): Option[CoreOperation] =
    holder(ready, banner).map(player => Move(Piece.Banner(banner),
      PositionedLocation(Location.PlayArea(player)),
      PositionedLocation(Location.SharedBank)))

  /** Whether the step from `before` to `after` returned `banner`. */
  def moved(before: ReadyGame, after: ReadyGame, banner: Banner): Boolean =
    holder(before, banner).nonEmpty && holder(after, banner).isEmpty

  private def holder(ready: ReadyGame, banner: Banner): Option[PlayerId] =
    banner match
      case Banner.PeoplesFavor => ready.game.current.banners.peoplesFavor.holder
      case Banner.DarkestSecret =>
        ready.game.current.banners.darkestSecret.holder

/** Lost Guardians (E18, ruined): "SETUP (END): Place 3 secrets on the
  * Darkest Secret and move it to the shared bank. Players cannot start with
  * it."
  *
  * The secrets come from the shared supply, and a held banner goes back to
  * the shared bank, as Hall of Mockery's does. Its line reads the step:
  * "Placed 3 secrets on the Darkest Secret and moved it to the shared
  * bank.", or only the placement when nobody held the banner.
  */
case object LostGuardians extends RuinedSetupRule:
  val edifice: EdificeId = TomeGuardiansCard.id
  val id: PowerId = TomeGuardiansCard.ruined.power.id
  val Secrets: Int = 3
  private val notes = NoteSet(id)
  /** "Placed {3 secrets} on the Darkest Secret." */
  val placed: NoteKey = notes.key("placed", Vector(NotePart.Text("Placed "),
    NotePart.Arg(0), NotePart.Text(" on the Darkest Secret.")))
  /** "Placed {3 secrets} on the Darkest Secret and moved it to the shared
    * bank." */
  val placedAndMoved: NoteKey = notes.key("placed-and-moved", Vector(
    NotePart.Text("Placed "), NotePart.Arg(0),
    NotePart.Text(" on the Darkest Secret and moved it to the shared bank.")))
  override def noteKeys: Vector[NoteKey] = notes.declared

  protected def build(ready: ReadyGame, at: SiteId): Vector[CoreOperation] =
    Move(Piece.Secrets(Secrets),
      PositionedLocation(Location.SharedBank),
      PositionedLocation(Location.OnBanner(Banner.DarkestSecret))) +:
      BannerToSharedBank.move(ready, Banner.DarkestSecret).toVector

  protected def note(at: SiteId): Note = Note(id, states => for
    (before, after) <- states.previous
    gained = after.game.current.banners.darkestSecret.secrets -
      before.game.current.banners.darkestSecret.secrets
    if gained > 0
  yield
    val secrets = NoteArg.Amount(gained, NoteUnit.Secret)
    val card = PowerSourceRef.Card(edifice)
    if BannerToSharedBank.moved(before, after, Banner.DarkestSecret) then
      placedAndMoved(card, secrets)
    else placed(card, secrets))
