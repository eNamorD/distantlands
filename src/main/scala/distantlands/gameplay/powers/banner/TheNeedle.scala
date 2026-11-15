package distantlands.gameplay.powers.banner

import distantlands.catalog.{Legacy, LegacyGoal, PrintedPower}
import distantlands.gameplay.actions.BannerRules
import distantlands.gameplay.chronicle.ChronicleLegacyRules
import distantlands.gameplay.powerresolver.{Contribution, ExecutedBatch,
  ExecutedTrigger}
import distantlands.gameplay.powers.PowerAnswers
import distantlands.model._

object TheNeedleCard extends Legacy(LegacyId("L05"), "The Needle",
    LegacyGoal.MostSites, Suit.Beast):
  val power = PrintedPower(PowerId("legacy.the-needle"),
    persistent = false, cost = Cost.free,
    text = "After you take a banner in any way, you may flip it to its " +
      "other side.")
  val powers: Vector[PrintedPower] = Vector(power)

/** The Needle (legacy L05): "After you take a banner in any way, you may
  * flip it to its other side" (Legacy in-play powers design, ruling 20).
  *
  * An `ExecutedTrigger`: after a walker batch moves a banner into its
  * holder's play area from anywhere else (a challenge, a raid, Favored
  * Son, Wizard's Conclave, the Conspiracy, The Standard Bearer at Setup),
  * its holder is asked, on or off turn, "Keep this side" or "Flip it",
  * once per banner, the People's Favor first (implementation rulings R13
  * and R14). Flipping is a `FlipBanner`: its tokens stay, flipping to the
  * Festival places no marker and flipping away from it takes the marker
  * off. A banner placed at game start is not taken: game start is no
  * walk.
  *
  * Line (R12): "p2 flipped the People's Favor with The Needle"; keeping
  * the side writes nothing.
  */
case object TheNeedle extends ExecutedTrigger:
  val id: PowerId = TheNeedleCard.power.id
  def source: RuleSourceRef = RuleSourceRef.GameRule(id.value)
  def contributions: Map[PowerWindow, Vector[Contribution]] = Map.empty

  def decisionId(banner: Banner): String = s"power.the-needle.${banner.key}"
  val keep: DecisionOptionRef.Button = DecisionOptionRef.Button("keep")
  val flip: DecisionOptionRef.Button = DecisionOptionRef.Button("flip")

  private val notes = NoteSet(id)
  /** "{p2} flipped the {People's Favor} with {The Needle}". */
  val flipped: NoteKey = notes.key("flipped", Vector(NotePart.Arg(0),
    NotePart.Text(" flipped the "), NotePart.Arg(1),
    NotePart.Text(" with "), NotePart.Arg(2)))
  override def noteKeys: Vector[NoteKey] = notes.declared
  override def narratedDecisions: Set[String] =
    Banner.all.map(decisionId).toSet

  /** The banners `batch` moved into `holder`'s play area from elsewhere,
    * by a `Move` or a `Take`, in banner order. The batch is read as
    * executed: a composite's children are not rebuilt, since building some
    * of them anew is not safe outside their own run. */
  private def taken(batch: ExecutedBatch, holder: PlayerId): Vector[Banner] =
    val into = Location.PlayArea(holder)
    val moved = batch.executed.collect {
      case Move(Piece.Banner(banner), PositionedLocation(from, _, _),
          PositionedLocation(to, _, _), _, _)
          if to == into && from != into => banner
      case Take(Piece.Banner(banner), _, from, to, _, _, _, _)
          if to == into && from != into => banner
    }.toSet
    Banner.all.filter(moved)

  private def flipped(answered: Vector[Answered], banner: Banner): Boolean =
    PowerAnswers.last(answered, decisionId(banner)).contains(flip)

  def onExecuted(batch: ExecutedBatch): Option[Operation] =
    for
      holder <- ChronicleLegacyRules.holder(batch.now, TheNeedleCard.id)
      banners = taken(batch, holder)
      if banners.nonEmpty
    yield Sequence(banners.flatMap(banner => Vector[Operation](
      Decide(decisionId(banner), holder, DecisionQuery.ChooseOne(Vector(
        DecisionOption.Button(keep, "Keep this side"),
        DecisionOption.Button(flip, "Flip it")),
        heading = Some(s"The Needle: flip the " +
          s"${BannerRules.displayName(banner)}?"))),
      BuildOps((_, pending) => Right(Option.when(
        flipped(pending.answered, banner))(FlipBanner(banner): CoreOperation)
        .toVector), required = true),
      Note(id, states => Option.when(flipped(states.answered, banner))(
        TheNeedle.flipped.sourceless(NoteArg.Player(holder),
          NoteArg.Banner(banner), NoteArg.Card(TheNeedleCard.id)))))))
