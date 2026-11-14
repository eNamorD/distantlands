package distantlands.gameplay.chronicle.legacies

import distantlands.catalog.{ExecutableCatalog, Legacy, LegacyGoal,
  PrintedPower}
import distantlands.gameplay.actions.ProcedureNotes
import distantlands.gameplay.chronicle.{ChronicleLegacyRules, ShapeEmpire}
import distantlands.gameplay.powerresolver.{Contribution, ContributingPower,
  Transform}
import distantlands.gameplay.powers.PowerAnswers
import distantlands.model._

object RevolutionaryCard extends Legacy(LegacyId("L15"), "Revolutionary",
    LegacyGoal.PeoplesFavor, Suit.Discord):
  val power = PrintedPower(PowerId("legacy.revolutionary"),
    persistent = false, cost = Cost.free,
    text = "**CHRONICLE (THRONE):** After Build Edifice, you may " +
      "discard any number of denizens at Imperial sites.")
  val powers: Vector[PrintedPower] = Vector(power)

/** Revolutionary (legacy L15): "After Build Edifice, you may discard any
  * number of denizens at Imperial sites." Its step is the window after
  * Build Edifice (Legacy Chronicle powers design, ruling 11). The holder,
  * read when the step is reached, chooses any of the denizens and ruined
  * edifices at sites the Empire rules, in one choose-many (R9, R13); each
  * goes where Shape Empire sends it (`ShapeEmpire.discardOf`). Intact
  * edifices stay, as in Shape Empire. A discarded ruined edifice is set
  * aside, then returned to the bottom of the edifice deck, as Build
  * Edifice's closing step returns its own (R16). Line:
  * "p2 discarded 3 denizens with Revolutionary" (R12). */
case object Revolutionary extends ContributingPower:
  val id: PowerId = RevolutionaryCard.power.id
  def source: RuleSourceRef = RuleSourceRef.GameRule(id.value)
  def contributions: Map[PowerWindow, Vector[Contribution]] = Map(
    PowerWindow.ChronicleAfterBuildEdifice -> Vector(Transform(
      (ctx, ops) => ops :+ step(ctx.catalog))))

  def decisionId(player: PlayerId): String =
    s"chronicle.throne.legacy.revolutionary.${player.value}"

  private val noteId =
    ProcedureNotes.id(TriggeredProcedureRef.ChronicleThrone)
  private val notes = NoteSet(noteId)
  val discardedWith: NoteKey = notes.key("revolutionary-discarded", Vector(
    NotePart.Arg(0), NotePart.Text(" discarded "), NotePart.Arg(1),
    NotePart.Text(" "), NotePart.Plural(1, "denizen", "denizens"),
    NotePart.Text(" with "), NotePart.Arg(2)))
  /** The Throne lines this power writes, on the Throne's note id. */
  def throneNoteKeys: Vector[NoteKey] = notes.declared

  def step(catalog: ExecutableCatalog): Operation = Branch((state, _) =>
    ChronicleLegacyRules.holder(state, RevolutionaryCard.id).toVector.flatMap(
      holder => discardable(catalog, state, holder) match
        case Vector() => Vector.empty
        case cards => Vector(
          Decide(decisionId(holder), holder, DecisionQuery.ChooseMany(0,
            cards.size, cards.map(_._1),
            heading = Some("Revolutionary: you may discard any denizens " +
              "at Imperial sites"),
            confirm = Some(CountedConfirm("Discard", "card", "cards",
              Some("Discard none"))))),
          Branch((_, pending) =>
            val chosen = PowerAnswers.many(pending, decisionId(holder))
              .getOrElse(Vector.empty).toSet
            val discards = cards.collect {
              case (option, discard) if chosen(option.ref) => discard }
            val ruined = discards.collect {
              case edifice: Discard.RuinedEdifice => edifice.card }
            if discards.isEmpty then Vector.empty
            else BuildOps((_, _) => Right(discards), required = true) +:
              (Option.when(ruined.nonEmpty)(BuildOps((_, _) =>
                Right(ruined.map(returned)), required = true)).toVector :+
              Note(noteId, _ => Some(discardedWith.sourceless(
                NoteArg.Player(holder), NoteArg.Number(discards.size),
                NoteArg.Card(RevolutionaryCard.id)))))))))

  /** A ruined edifice discarded after Build Edifice, from the set-aside list
    * to the bottom of the edifice deck, as Build Edifice's closing step
    * returns its own: nothing is left set aside for the Stars to store
    * (R16). */
  private def returned(edifice: EdificeId): CoreOperation =
    Move(Piece.Card(edifice), PositionedLocation(Location.SetAsideEdifices),
      PositionedLocation(Location.Deck(CardDeck.Edifice), StackPosition.Bottom))

  /** Each card at a site the Empire rules that Shape Empire could discard,
    * as its option and its discard, in map then row order. The question's
    * options are what this Branch read, so a walk resumed there finds the
    * same ones. */
  private def discardable(catalog: ExecutableCatalog, state: ReadyGame,
      holder: PlayerId): Vector[(DecisionOption, CoreOperation)] =
    val current = state.game.current
    for
      site <- current.map.inPlay
        if current.map.sites.get(site).exists(at => SiteRule.ruler(at.forces,
          current.players).contains(SiteRuler.Empire))
      region <- current.map.regionOf(site).toVector
      card <- current.map.sites(site).denizens
      discard <- ShapeEmpire.discardOf(catalog, site, region, card, holder)
        .toVector
    yield (ShapeEmpire.option(card), discard)
