package distantlands.gameplay.powers.campaign

import distantlands.catalog.{Denizen, ExecutableCatalog, ModifierMark,
  PrintedPower}
import distantlands.gameplay.NoteSupport
import distantlands.gameplay.actions.campaign.CampaignProcedure
import distantlands.gameplay.actions.cardplay.CardSlots
import distantlands.gameplay.powers.PowerAnswers
import distantlands.model._

object RelicHunterCard extends Denizen(DenizenId("126"), "Relic Hunter", Suit.Order):
  val power = PrintedPower(PowerId("denizen.relic-hunter"),
    persistent = false, cost = Cost.free,
    modifier = Some(ModifierMark.AttackPlan),
    text = "You may target facedown relics at targeted sites, adding 1 " +
      "[defense-die] per relic. You may put any relics you take on " +
      "the bottom of the relic deck.")
  val powers: Vector[PrintedPower] = Vector(power)

/** Relic Hunter (card 126), an attacker's battle plan: "You may target
  * facedown relics at targeted sites, adding 1 defense die per relic. You
  * may put any relics you take on the bottom of the relic deck."
  *
  * It is free and offered in a Conquest only, since only a Conquest has
  * targeted sites (owner's ruling, 2026-11-03); a Raid never shows it.
  * Used in the attacker's plan window, it asks its user which facedown
  * relics at the Conquest's targeted sites to target, as Glamor asks its
  * extra targets after the plan is chosen. The relics are named by their
  * slot at their site (`SiteRelicSlot`), so no option discloses a facedown
  * relic. Each targeted relic adds one defense die. A relic a restriction
  * would not let the attacker take (Ancient Bloodline) is shown
  * unavailable with its reason. With no facedown relic at the targeted
  * sites that can be taken, the plan is shown unavailable.
  *
  * When the attacker wins, in the Conquest's placement window, after the
  * survivors are placed: the attacker peeks at the targeted relics, chooses
  * which of them go to the bottom of the relic deck, and takes them all,
  * still facedown; the chosen ones are then put on the bottom of the relic
  * deck. A defeated attacker takes nothing, and the relics stay where they
  * lie. The peek is how a relic the attacker takes becomes one they know
  * before the bottom question; the generic "Peeked at" lines stand.
  *
  * Lines: "Targeted {2} facedown relics for {2} defense dice." when the
  * relics are chosen; "Took {relics}." and "Put {relics} on the bottom of
  * the relic deck." when the takes run.
  */
case object RelicHunter extends DenizenPlan(RelicHunterCard,
    RelicHunterCard.power):
  /** Under the Campaign's prefix, so a parked question is a Campaign
    * decision. */
  val decisionId: String = CampaignProcedure.decisionPrefix + "relic-hunter"
  /** Which taken relics go to the bottom of the relic deck. */
  val bottomDecisionId: String =
    CampaignProcedure.decisionPrefix + "relic-hunter.bottom"
  val heading: String = "Relic Hunter: target which facedown relics, for " +
    "a defense die each?"
  val bottomHeading: String = "Relic Hunter: put which taken relics on the " +
    "bottom of the relic deck?"
  val noRelic: String = "No facedown relic at the targeted sites"
  val noneTakeable: String =
    "No facedown relic at the targeted sites can be taken"
  private val notes = NoteSet(id)
  /** "Targeted {n} facedown relics for {n} defense dice." */
  val targeted: NoteKey = notes.key("targeted", Vector(
    NotePart.Text("Targeted "), NotePart.Arg(0),
    NotePart.Plural(0, " facedown relic for ", " facedown relics for "),
    NotePart.Arg(1), NotePart.Plural(1, " defense die.", " defense dice.")))
  /** "Took {relics}." */
  val took: NoteKey = notes.key("took", Vector(NotePart.Text("Took "),
    NotePart.Arg(0), NotePart.Text(".")))
  /** "Put {relics} on the bottom of the relic deck." */
  val bottomed: NoteKey = notes.key("bottomed", Vector(NotePart.Text("Put "),
    NotePart.Arg(0), NotePart.Text(" on the bottom of the relic deck.")))
  override def noteKeys: Vector[NoteKey] = notes.declared
  override def narratedDecisions: Set[String] =
    Set(decisionId, bottomDecisionId)

  def sides: Set[CampaignPlanSide] = Set(CampaignPlanSide.Attacker)

  /** A facedown relic at a targeted site, by its slot there. */
  final case class SiteRelic(site: SiteId, index: Int, relic: RelicId):
    def ref: DecisionOptionRef.SiteRelicSlot =
      DecisionOptionRef.SiteRelicSlot(site, index)

  /** Every facedown relic at `sites`, in target then slot order. */
  def relicsAt(ready: ReadyGame, sites: Vector[SiteId]): Vector[SiteRelic] =
    sites.flatMap(site => ready.game.current.map.sites.get(site).toVector
      .flatMap(_.relics.zipWithIndex.collect {
        case (relic, index) if relic.orientation == Orientation.FaceDown =>
          SiteRelic(site, index, relic.id)
      }))

  def plan(context: PlanContext): Option[CampaignPlanOffer] =
    context.denizen(cardId)
      .filter(_ => context.setup.kind == CampaignKind.Conquest)
      .map { source =>
        val offer = CampaignPlanOffer(source,
          "Relic Hunter: target facedown relics at the targeted sites",
          Vector.empty, Vector.empty)
        val actor = context.setup.actor
        val options = targetOptions(context.catalog, context.ready, actor,
          context.setup.targetSites)
        if options.isEmpty then
          offer.copy(withheld = Some(OptionReason.Written(noRelic)))
        else if !options.exists(_.available) then
          offer.copy(withheld = Some(OptionReason.Written(noneTakeable)))
        else offer.copy(amount = Some(PlanAmount(decisionId,
          (ready, user) => Right(targetQuery(targetOptions(context.catalog,
            ready, user, context.setup.targetSites))), {
            case DecisionAnswer.ChooseManyAnswer(selected) =>
              Some(selected.size)
            case _ => None
          }, dice)))
      }

  /** Each facedown relic at the targets, judged by whether `actor` may
    * take it: a refused one is shown with its reason. */
  private def targetOptions(catalog: ExecutableCatalog, ready: ReadyGame,
      actor: PlayerId, sites: Vector[SiteId]): Vector[DecisionOption] =
    relicsAt(ready, sites).flatMap(found => CardSlots.judge(catalog, ready,
      actor, DecisionOption.SiteRelicSlot(found.ref),
      Right(Vector(take(actor, found)))))

  private def targetQuery(options: Vector[DecisionOption]): DecisionQuery =
    DecisionQuery.ChooseMany(0, options.count(_.available), options,
      heading = Some(heading))

  /** One defense die per targeted relic, and the line. */
  private def dice(count: Int): Vector[CampaignPlanEffect] =
    if count == 0 then Vector.empty
    else Vector(CampaignPlanEffect.AddDefenseDice(count),
      CampaignPlanEffect.Run(Vector(Note(id, _ => Some(targeted(
        PowerSourceRef.Card(cardId), NoteArg.Number(count),
        NoteArg.Number(count)))))))

  private def take(actor: PlayerId, found: SiteRelic): Take =
    Take(Piece.Card(found.relic), actor, Location.Site(found.site),
      Location.PlayArea(actor))

  /** The targeted slots, from the plan's answer. */
  private def targetedSlots(pending: PendingTree)
      : Vector[DecisionOptionRef.SiteRelicSlot] =
    PowerAnswers.many(pending, decisionId).toVector.flatten.collect {
      case slot: DecisionOptionRef.SiteRelicSlot => slot }

  /** The targeted relics as they lie, before any is taken. */
  private def targetedRelics(ready: ReadyGame, pending: PendingTree)
      : Vector[SiteRelic] =
    val slots = targetedSlots(pending).toSet
    relicsAt(ready, ready.game.current.map.inPlay)
      .filter(found => slots(found.ref))

  /** After a Conquest victory, in the placement window: the peeks, the
    * bottom question, the takes and their lines. Each slot selects the same
    * children on every resume: the question reads only the plan's answer,
    * and its options are the targeted slots themselves. */
  override def later: Map[PowerWindow, PlanUse => Vector[Operation]] = Map(
    PowerWindow.CampaignPlacement -> (use =>
      if !use.won.contains(true) then Vector.empty
      else Vector[Operation](
        BuildOps((ready, pending) => Right(targetedRelics(ready, pending)
          .map(found => Peek(use.actor, found.relic,
            Location.Site(found.site))))),
        Branch((_, pending) => targetedSlots(pending) match
          case Vector() => Vector.empty
          case slots => Vector(Decide(bottomDecisionId, use.actor,
            DecisionQuery.ChooseMany(0, slots.size,
              slots.map(DecisionOption.SiteRelicSlot(_)),
              heading = Some(bottomHeading))))),
        BuildOps((ready, pending) => Right(takes(ready, use.actor, pending))),
        Note(id, tookNote(use.actor), covers = true),
        Note(id, bottomedNote, covers = true))))

  /** Every targeted relic taken, facedown as it lay, and those chosen put
    * on the bottom of the relic deck. */
  private def takes(ready: ReadyGame, actor: PlayerId, pending: PendingTree)
      : Vector[CoreOperation] =
    val bottom = PowerAnswers.many(pending, bottomDecisionId).toVector
      .flatten.toSet
    targetedRelics(ready, pending).flatMap(found =>
      Vector[CoreOperation](take(actor, found)) ++
        Option.when(bottom(found.ref))(Move(Piece.Card(found.relic),
          PositionedLocation(Location.PlayArea(actor)),
          PositionedLocation(Location.Deck(CardDeck.Relic),
            StackPosition.Bottom), by = Some(actor))))

  private def tookNote(actor: PlayerId)(states: NoteStates)
      : Option[LogNote] = for
    step <- states.previous
    kept = NoteSupport.relicsGained(step, actor)
    if kept.nonEmpty
  yield took(PowerSourceRef.Card(cardId), NoteArg.Cards(kept))

  private def bottomedNote(states: NoteStates): Option[LogNote] = for
    (before, after) <- states.previous
    deck = (ready: ReadyGame) => ready.game.current.commonCards.relicDeck
    put = deck(after).filterNot(deck(before).contains)
    if put.nonEmpty
  yield bottomed(PowerSourceRef.Card(cardId), NoteArg.Cards(put))
