package oathdigital.gameplay.actions.challenge

import oathdigital.catalog.ExecutableCatalog
import oathdigital.gameplay.NoteSupport
import oathdigital.gameplay.actions.{BannerRules, ProcedureNotes}
import oathdigital.gameplay.powerresolver.ModifierPicks
import oathdigital.gameplay.walker.ModifierChoice
import oathdigital.model._

/** Challenge on the walker: spend one Supply, choose a banner, choose how many
  * resources replace it, return the banner's resources (the ribbon), then pay
  * the replacement and take the banner. Takes no start selection. It opens
  * with the modifier decision, whose picks the banner question reads
  * ([[ChallengeReach]]).
  *
  * The Supply is spent first so no decision reads it. The banner and the
  * amount are decided before any resource moves, so both read the board as
  * the player sees it; the ribbon and the payment are built when reached.
  */
object ChallengeProcedure:
  val bannerDecisionId: String = "challenge.banner"
  val amountDecisionId: String = "challenge.amount"

  /** "Start Challenge": the modifier decision's button. */
  val startLabel: String = "Start Challenge"

  private val notes = NoteSet(ProcedureNotes.id(ActionRef.Challenge))
  /** "Took People's Favor from p2 with 2 favor". Favor never pluralises;
    * secrets do, which is why the unit is in the template and not an
    * argument. */
  val tookFromPlayerWithFavor: NoteKey = notes.key("took-from-player-favor",
    Vector(NotePart.Text("Took "), NotePart.Arg(0), NotePart.Text(" from "),
      NotePart.Arg(1), NotePart.Text(" with "), NotePart.Arg(2),
      NotePart.Text(" favor")))
  val tookFromBankWithFavor: NoteKey = notes.key("took-from-bank-favor",
    Vector(NotePart.Text("Took "), NotePart.Arg(0),
      NotePart.Text(" from the bank with "), NotePart.Arg(1),
      NotePart.Text(" favor")))
  val tookFromPlayerWithSecrets: NoteKey = notes.key(
    "took-from-player-secrets",
    Vector(NotePart.Text("Took "), NotePart.Arg(0), NotePart.Text(" from "),
      NotePart.Arg(1), NotePart.Text(" with "), NotePart.Arg(2),
      NotePart.Text(" "), NotePart.Plural(2, "secret", "secrets")))
  val tookFromBankWithSecrets: NoteKey = notes.key("took-from-bank-secrets",
    Vector(NotePart.Text("Took "), NotePart.Arg(0),
      NotePart.Text(" from the bank with "), NotePart.Arg(1),
      NotePart.Text(" "), NotePart.Plural(1, "secret", "secrets")))
  def noteKeys: Vector[NoteKey] = notes.declared

  /** The start checks only the banner faces: whether a banner is in reach
    * depends on the modifiers the walk picks first, so the banner question
    * refuses the walk instead (`bannerQuestion`). */
  def build(catalog: ExecutableCatalog, state: ReadyGame, actor: PlayerId,
      args: Vector[DecisionOptionRef]): Either[OathViolation, Operation] = for
    _ <- noStartArgs(args)
    _ <- supportedFaces(state)
  yield tree(actor)

  def rebuild(catalog: ExecutableCatalog, state: ReadyGame, actor: PlayerId,
      args: Vector[DecisionOptionRef]): Either[OathViolation, Operation] =
    noStartArgs(args).map(_ => tree(actor))

  /** The modifier decision, then the Challenge. "Start Challenge" previews
    * the Challenge, so it shows unavailable until the picks bring a banner
    * into reach. */
  private def tree(actor: PlayerId): Operation =
    val rest = challenge(actor)
    Sequence(Vector(ModifierChoice.loop(actor,
      PowerWindow.ChallengeModifierSelection, rest, startLabel), rest))

  /** The banners the actor could challenge now under `picks`, the modifier
    * picks so far, in the banners' declared order: not already held by the
    * actor, the holder (if any) at the actor's pawn site unless a pick lifts
    * that ([[ChallengeReach.unbound]]), and strictly more relevant faceup
    * resources than the banner holds. Magician's Code's 2 secrets count
    * toward the Darkest Secret (`ChallengeReach.bonus`). Supply is a cost
    * owned by `SpendSupply`, not a fact about a banner.
    */
  def legalBanners(state: ReadyGame, actor: PlayerId,
      picks: Vector[CardId] = Vector.empty): Vector[Banner] =
    val current = state.game.current
    current.player(actor).toVector.flatMap { player =>
      Banner.all.filter { banner =>
        val holder = BannerRules.holder(current, banner)
        !holder.contains(actor) &&
          holder.forall(rival => (player.pawnSite.nonEmpty &&
            player.pawnSite == current.pawnSite(rival)) ||
            ChallengeReach.unbound(state, actor, picks, banner, rival)) &&
          BannerRules.playerResources(player, banner) +
            ChallengeReach.bonus(picks, banner) >
            BannerRules.resources(current, banner)
      }
    }

  private def noStartArgs(args: Vector[DecisionOptionRef])
      : Either[OathViolation, Unit] = Either.cond(args.isEmpty, (),
    OathViolation.InvalidEventOrder(
      s"walker procedure ${ActionRef.Challenge.key} takes no start selection, " +
        s"got ${args.map(_.kind).mkString(", ")}"))

  /** The ribbon rules below are the printed ones for People's Favor `Mob` and
    * Darkest Secret `WanderingFlame` only.
    */
  private def supportedFaces(state: ReadyGame): Either[OathViolation, Unit] =
    val banners = state.game.current.banners
    Either.cond(banners.peoplesFavor.active == PeoplesFavorFace.Mob &&
      banners.darkestSecret.active == DarkestSecretFace.WanderingFlame, (),
      OathViolation.UnsupportedBannerState("unsupported active banner face"))

  private def challenge(actor: PlayerId): Operation = Sequence(Vector[Operation](
    Sequence(Vector[Operation](SpendSupply(actor, 1)),
      Some(PowerWindow.ChallengeCost)),
    Branch((ready, pending) => Vector(bannerQuestion(ready, actor,
      picks(pending)))),
    Branch((ready, pending) => bannerOf(pending).toVector.map(banner =>
      amountDecision(ready, actor, banner,
        ChallengeReach.bonus(picks(pending), banner)))),
    Branch((ready, pending) => effects(ready, actor, pending))),
    Some(PowerWindow.ChallengeActionEligibility))

  /** The banner question, or, with no banner in reach under `picks`, a step
    * that refuses the walk. The refusal is the whole walk's, so the Supply
    * spent before it is never spent: a start with no modifier to pick is
    * refused, after the Supply check, and "Start Challenge" previews
    * unavailable until a pick opens a banner. An empty question would park
    * instead.
    *
    * The menu prices a start by walking it to its first park, so this gate
    * holds only while nothing parks between the modifier decision and here:
    * a first power to ask at `ChallengeCost` must make the menu's preview
    * walk on to this question. */
  private def bannerQuestion(ready: ReadyGame, actor: PlayerId,
      picks: Vector[CardId]): Operation =
    legalBanners(ready, actor, picks) match
      case Vector() => BuildOps((_, _) =>
        Left(OathViolation.NoPlayableOption(ActionRef.Challenge.key)))
      case banners => Decide(bannerDecisionId, actor,
        DecisionQuery.ChooseOne(banners.map(banner =>
          DecisionOption.Banner(DecisionOptionRef.Banner(banner))),
          heading = Some("Choose a banner to Challenge"),
          confirmVerb = Some("Challenge")),
        window = Some(PowerWindow.ChallengeBannerSelection))

  /** The cards the walk's modifier decision picked. */
  private def picks(pending: PendingTree): Vector[CardId] =
    ModifierPicks.cards(pending.answered)

  /** How many of the actor's own resources go on the banner: strictly more
    * than it holds, counting `bonus`, the secrets Magician's Code adds, so
    * the least amount is 2 lower with the Code, and never below 0. */
  private def amountDecision(ready: ReadyGame, actor: PlayerId,
      banner: Banner, bonus: Int): Operation =
    val current = ready.game.current
    val prior = BannerRules.resources(current, banner)
    val own = current.player(actor)
      .fold(0)(BannerRules.playerResources(_, banner))
    val least = math.max(0, prior + 1 - bonus)
    val unit = banner match
      case Banner.PeoplesFavor => "favor"
      case Banner.DarkestSecret => "secrets"
    val heading =
      if bonus == 0 then s"Place more than $prior $unit to take ${banner.key}"
      else s"Place more than $prior $unit, counting Magician's Code's " +
        s"$bonus, to take ${banner.key}"
    Decide(amountDecisionId, actor, DecisionQuery.ChooseAmount(least,
      math.max(least, own), Some(heading), "Take banner"),
      window = Some(PowerWindow.ChallengeAmountSelection))

  /** Reached only after both answers exist. Selecting reads the two answers
    * and the banner's holder, which no earlier step changes; the ribbon and
    * the payment amount are built when walked.
    */
  private def effects(ready: ReadyGame, actor: PlayerId,
      pending: PendingTree): Vector[Operation] =
    (for
      banner <- bannerOf(pending)
      amount <- amountOf(pending)
    yield
      val bonus = ChallengeReach.bonus(picks(pending), banner)
      Vector[Operation](
        Sequence(ChallengeRibbon.steps(actor, banner),
          Some(PowerWindow.ChallengeRibbon)),
        Sequence(gained(actor, bonus) ++ Vector[Operation](
          payment(actor, banner, amount + bonus),
          // Directly after the payment, so `previous` is the payment and the
          // note reads what actually left the play area rather than the
          // amount answered. The holder is read here, before the take moves
          // the banner, which is the holder the deleted rule read off the
          // recorded `Take`. No power contributes at `ChallengePlacement`; a
          // first one that inserted a leaf between the payment and the note
          // would leave the note reading its step instead, so it must append
          // or the note must read the state as Travel's does.
          Note(ProcedureNotes.id(ActionRef.Challenge), tookNote(actor,
            banner, BannerRules.holder(ready.game.current, banner))),
          custody(ready, actor, banner)),
          Some(PowerWindow.ChallengePlacement))))
      .getOrElse(Vector[Operation](BuildOps((_, _) => Left(
        OathViolation.InvalidEventOrder(
          "Challenge reached its effects without a banner and an amount")))))

  /** What the challenger put on the banner, in the step before the note,
    * and who held it. Nothing paid writes nothing. */
  private def tookNote(actor: PlayerId, banner: Banner,
      holder: Option[PlayerId])(states: NoteStates): Option[LogNote] =
    states.previous.map(step => banner match
      case Banner.PeoplesFavor => -NoteSupport.favor(step, actor)
      case Banner.DarkestSecret => -NoteSupport.secrets(step, actor))
      .filter(_ > 0).map(paid => holder match
        case Some(rival) => fromPlayer(banner).sourceless(
          NoteArg.Banner(banner), NoteArg.Player(rival), NoteArg.Number(paid))
        case None => fromBank(banner).sourceless(
          NoteArg.Banner(banner), NoteArg.Number(paid)))

  private def fromPlayer(banner: Banner): NoteKey = banner match
    case Banner.PeoplesFavor => tookFromPlayerWithFavor
    case Banner.DarkestSecret => tookFromPlayerWithSecrets

  private def fromBank(banner: Banner): NoteKey = banner match
    case Banner.PeoplesFavor => tookFromBankWithFavor
    case Banner.DarkestSecret => tookFromBankWithSecrets

  private def payment(actor: PlayerId, banner: Banner, amount: Int): Operation =
    Move(banner match {
      case Banner.PeoplesFavor => Piece.Favor(amount)
      case Banner.DarkestSecret => Piece.Secrets(amount)
    }, PositionedLocation(Location.PlayArea(actor)),
      PositionedLocation(Location.OnBanner(banner)), by = Some(actor))

  /** Magician's Code's secrets, from the shared bank to the actor's board
    * just before the payment, which places them with the rest: the
    * Challenge's line then counts them. None without the Code. */
  private def gained(actor: PlayerId, bonus: Int): Vector[Operation] =
    Option.when(bonus > 0)(Move(Piece.Secrets(bonus),
      PositionedLocation(Location.SharedBank),
      PositionedLocation(Location.PlayArea(actor))): Operation).toVector

  /** The banner moves to the challenger by a required `Take`: the payment
    * buys it, so a refused custody fails the path and the search hides that
    * banner. The walker checks the `Take` whole and records its `Move`, which
    * the Game Log reads.
    */
  private def custody(ready: ReadyGame, actor: PlayerId,
      banner: Banner): Operation =
    Take(Piece.Banner(banner), actor,
      BannerRules.holder(ready.game.current, banner)
        .fold[Location](Location.SharedBank)(Location.PlayArea(_)),
      Location.PlayArea(actor), required = true)

  private def bannerOf(pending: PendingTree): Option[Banner] =
    pending.answered.collectFirst:
      case Answered(`bannerDecisionId`, DecisionAnswer.ChooseOneAnswer(
          DecisionOptionRef.Banner(banner)), _) => banner

  private def amountOf(pending: PendingTree): Option[Int] =
    pending.answered.collectFirst:
      case Answered(`amountDecisionId`, DecisionAnswer.ChooseAmountAnswer(n), _) =>
        n
