package distantlands.gameplay.setup.legacies

import distantlands.catalog.{Legacy, LegacyGoal, PrintedPower}
import distantlands.gameplay.actions.ProcedureNotes
import distantlands.gameplay.powerresolver.{Contribution, ContributingPower,
  OfferResolution}
import distantlands.gameplay.powers.PowerAnswers
import distantlands.gameplay.setup.SetupPawnPlacement
import distantlands.model._

object TheStandardBearerCard extends Legacy(LegacyId("L18"), "The Standard Bearer",
    LegacyGoal.PeoplesFavor, Suit.Arcane):
  val power = PrintedPower(PowerId("legacy.the-standard-bearer"),
    persistent = false, cost = Cost.free,
    text = "**SETUP:** After placing your pawn, you may take a " +
      "banner from the shared bank and place two more " +
      "[favor]/[secret] on it from the shared bank.")
  val powers: Vector[PrintedPower] = Vector(power)

/** The Standard Bearer (legacy L18), its SETUP power (Legacy Setup powers
  * design, ruling 16). In the holder's own `SetupPawnPlaced` window, in the
  * order the holder chooses among its powers (catalog batch 13, ruling 4),
  * the holder may take a banner from the shared bank and
  * put two more of its own resource on it from the shared bank, which has
  * no count (R28). The banner is moved, not taken by its ribbon: it keeps
  * what it holds. Line: "p2 took the People's Favor with The Standard
  * Bearer".
  *
  * The step keeps its shape after the move, for a walk that parks right
  * after it (The Needle) and is rebuilt on the state the move left (Legacy
  * in-play powers R20): the banner the holder chose stays offered though
  * the shared bank no longer holds it. */
case object TheStandardBearer extends ContributingPower:
  val id: PowerId = TheStandardBearerCard.power.id
  def source: RuleSourceRef = RuleSourceRef.GameRule(id.value)
  def contributions: Map[PowerWindow, Vector[Contribution]] = Map(
    PowerWindow.SetupPawnPlaced -> Vector(OfferResolution(ctx =>
      SetupPawnPlacement.latest(ctx.answered).map(_._1)
        .filter(player => SetupLegacyRules.standardBearer(ctx.state)
          .contains(player))
        .map(holder => ResolutionOffer(option, Vector(step(holder)))))))

  /** This legacy in the placer's power-order question. */
  val option: DecisionOption =
    DecisionOption.Legacy(DecisionOptionRef.Legacy(TheStandardBearerCard.id))

  def decisionId(player: PlayerId): String =
    s"setup.legacy.the-standard-bearer.${player.value}"
  val decline: DecisionOptionRef.Button = DecisionOptionRef.Button("decline")

  private val noteId = ProcedureNotes.id(TriggeredProcedureRef.Setup)
  private val notes = NoteSet(noteId)
  val took: NoteKey = notes.key("standard-bearer-took", Vector(
    NotePart.Arg(0), NotePart.Text(" took the "), NotePart.Arg(1),
    NotePart.Text(" with "), NotePart.Arg(2)))
  def setupNoteKeys: Vector[NoteKey] = notes.declared

  /** The banners the shared bank holds, the People's Favor first. */
  private def inBank(state: ReadyGame): Vector[Banner] =
    val banners = state.game.current.banners
    Vector(Banner.PeoplesFavor -> banners.peoplesFavor.holder,
      Banner.DarkestSecret -> banners.darkestSecret.holder)
      .collect { case (banner, None) => banner }

  /** The banners offered: those in the shared bank and the one the holder
    * chose, the People's Favor first. */
  private def offered(state: ReadyGame, pending: PendingTree,
      holder: PlayerId): Vector[Banner] =
    val bank = inBank(state)
    val chosen = PowerAnswers.one(pending, decisionId(holder)).collect {
      case DecisionOptionRef.Banner(banner) => banner }
    Vector(Banner.PeoplesFavor, Banner.DarkestSecret)
      .filter(banner => bank.contains(banner) || chosen.contains(banner))

  private def resource(banner: Banner): Piece = banner match
    case Banner.PeoplesFavor => Piece.Favor(2)
    case Banner.DarkestSecret => Piece.Secrets(2)

  // The holder is read again at the step, so a legacy lost earlier in the
  // window does not resolve (catalog batch 13, ruling 5).
  private def step(holder: PlayerId): Operation = Branch((state, pending) =>
    if !SetupLegacyRules.standardBearer(state).contains(holder) then
      Vector.empty
    else offered(state, pending, holder) match
      case Vector() => Vector.empty
      case banners => Vector(
        Decide(decisionId(holder), holder, DecisionQuery.ChooseOne(
          DecisionOption.Button(decline, "Decline") +: banners.map(banner =>
            DecisionOption.Banner(DecisionOptionRef.Banner(banner))),
          heading = Some(
            "The Standard Bearer: you may take a banner from the shared bank"),
          confirmVerb = Some("Take"))),
        Branch((_, pending) =>
          PowerAnswers.one(pending, decisionId(holder)).toVector.collect {
            case DecisionOptionRef.Banner(banner) => banner
          }.flatMap(banner => Vector(
            BuildOps((_, _) => Right(Vector(
              Move(Piece.Banner(banner),
                PositionedLocation(Location.SharedBank),
                PositionedLocation(Location.PlayArea(holder))),
              Move(resource(banner), PositionedLocation(Location.SharedBank),
                PositionedLocation(Location.OnBanner(banner))))),
              required = true),
            Note(noteId, _ => Some(took.sourceless(NoteArg.Player(holder),
              NoteArg.Banner(banner),
              NoteArg.Card(TheStandardBearerCard.id)))))))))
