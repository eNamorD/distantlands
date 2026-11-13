package distantlands.gameplay.chronicle

import distantlands.catalog.{ExecutableCatalog, FoundationCards}
import distantlands.gameplay.actions.ProcedureNotes
import distantlands.gameplay.powers.PowerAnswers
import distantlands.model._

/** Resolve Legacies, the Sun's steps 2-4 (CR "Task 1: The Sun"; Legacies
  * design, slice 3). Each part is a `Branch` read when it is reached, so a
  * power hooked at the window before sees the state it changes. Players
  * are taken in the game's turn order (`LegacyGoals.turnOrder`, R21).
  *
  * Step 2, Flip Legacies (R25): every seated lineage's dormant legacies,
  * activated when their player meets the goal and returned to the deck
  * otherwise, in one batch; then each public legacy, in row order, to the
  * one player who meets its goal, to the tied Imperial the outgoing
  * Chancellor chooses (ruling 9), or back to the deck; then every active
  * legacy of a seated or boxed lineage that alters a foundation whose face
  * does not yet list it (ruling 8), Foundation I to VI.
  *
  * Step 3, Exiles' legacies: see `dormancy`. Step 4, the cap: see `cap`.
  *
  * Lines (R24): "p2 activated Seer", "p3 discarded Iron Hand", "p3 took
  * Matchmaker", "Iron Hand returned to the legacy deck" and "Wealthy set
  * Foundation II to Movers and Shakers", "p2's Iron Hand went dormant".
  */
object ResolveLegacies:
  private val noteId = ProcedureNotes.id(TriggeredProcedureRef.ChronicleSun)
  private val notes = NoteSet(noteId)
  val activated: NoteKey = notes.key("activated-legacy", Vector(
    NotePart.Arg(0), NotePart.Text(" activated "), NotePart.Arg(1)))
  val discarded: NoteKey = notes.key("discarded-legacy", Vector(
    NotePart.Arg(0), NotePart.Text(" discarded "), NotePart.Arg(1)))
  val tookPublic: NoteKey = notes.key("took-public-legacy", Vector(
    NotePart.Arg(0), NotePart.Text(" took "), NotePart.Arg(1)))
  val returnedPublic: NoteKey = notes.key("returned-public-legacy", Vector(
    NotePart.Arg(0), NotePart.Text(" returned to the legacy deck")))
  /** "Wealthy set Foundation II to Movers and Shakers", one key per
    * foundation, since a legacy always sets the altered face. */
  val altered: Map[FoundationNumber, NoteKey] =
    FoundationNumber.all.map(number => number -> notes.key(
      s"altered-foundation-${number.toString.toLowerCase}", Vector(
        NotePart.Arg(0), NotePart.Text(s" set Foundation $number to " +
          FoundationCards.face(number, FoundationFace.Altered).name)))).toMap
  /** "p2's Iron Hand went dormant". */
  val wentDormant: NoteKey = notes.key("legacy-went-dormant", Vector(
    NotePart.Arg(0), NotePart.Text("'s "), NotePart.Arg(1),
    NotePart.Text(" went dormant")))
  /** "p2 discarded Seer", the player's own choice over the cap: the same
    * words as step 2's `discarded`, a key of its own to post as a
    * decision. */
  val capped: NoteKey = notes.key("capped-legacy", Vector(
    NotePart.Arg(0), NotePart.Text(" discarded "), NotePart.Arg(1)))
  def noteKeys: Vector[NoteKey] = notes.declared

  /** The most active legacies a player may hold ("Legacies in Detail"). */
  val MaxActive: Int = 3

  def capDecisionId(player: PlayerId): String =
    s"chronicle.sun.legacy-cap.${player.value}"

  def publicDecisionId(legacy: LegacyId): String =
    s"chronicle.sun.public-legacy.${legacy.value}"

  /** Sun step 2's children. */
  def flip(catalog: ExecutableCatalog): Vector[Operation] =
    Vector(dormant(catalog), publicRow(catalog), alterations(catalog))

  /** Sun step 3's children: each seated Exile's active legacies whose goal
    * they miss go dormant, in turn order. Imperials and the foundations
    * are untouched. These are the newly dormant legacies (ruling 2). */
  def dormancy(catalog: ExecutableCatalog): Vector[Operation] = Vector(
    Branch((state, _) =>
      val lapsed = for
        player <- LegacyGoals.turnOrder(state)
          if Imperials.role(state, player).contains(Role.Exile)
        legacy <- legaciesOf(state, player)
          if legacy.active && !meets(catalog, state, legacy.id, player)
      yield player -> legacy.id
      if lapsed.isEmpty then Vector.empty
      else BuildOps((live, _) => Right(lapsed.map((player, id) =>
          Flip(id, Location.Legacies(lineageOf(live, player)),
            Orientation.FaceDown))), required = true) +:
        lapsed.map((player, id) =>
          line(wentDormant, NoteArg.Player(player), NoteArg.Card(id)))))

  /** Sun step 4's children: `cap` for each seated player, in turn order. */
  def capAll: Vector[Operation] = Vector(Branch((state, _) =>
    LegacyGoals.turnOrder(state).map(cap)))

  /** A player above three active legacies discards exactly the excess, of
    * their choice, to the deck; at three or fewer nothing is asked. Read
    * when reached, so a reactivation can run it again (R19). */
  def cap(player: PlayerId): Operation =
    val decisionId = capDecisionId(player)
    Branch((state, _) =>
      val active = legaciesOf(state, player).filter(_.active).map(_.id)
      val excess = active.size - MaxActive
      if excess <= 0 then Vector.empty
      else Vector(
        Decide(decisionId, player, DecisionQuery.ChooseMany(excess, excess,
          active.map(id => DecisionOption.Legacy(DecisionOptionRef.Legacy(id))),
          heading = Some(s"Discard $excess active " +
            (if excess == 1 then "legacy" else "legacies") +
            ": you may keep three"),
          confirm = Some(CountedConfirm("Discard", "legacy", "legacies")))),
        Branch((_, pending) =>
          val chosen = PowerAnswers.many(pending, decisionId)
            .getOrElse(Vector.empty).collect {
              case DecisionOptionRef.Legacy(id) => id }
          Vector(BuildOps((now, _) => Right(chosen.map(id => Move(
              Piece.Card(id),
              PositionedLocation(Location.Legacies(lineageOf(now, player))),
              PositionedLocation(Location.Deck(CardDeck.Legacy))))),
              required = true)) ++
            chosen.map(id => line(capped, NoteArg.Player(player),
              NoteArg.Card(id))))))

  private def meets(catalog: ExecutableCatalog, ready: ReadyGame,
      legacy: LegacyId, player: PlayerId): Boolean =
    catalog.legacy(legacy).exists(card =>
      LegacyGoals.meeting(ready, catalog, card)(player))

  private def lineageOf(ready: ReadyGame, player: PlayerId): LineageId =
    ready.game.current.player(player).get.lineage

  private def legaciesOf(ready: ReadyGame, player: PlayerId)
      : Vector[LegacyState] =
    ready.game.campaign.lineages.get(lineageOf(ready, player)).toVector
      .flatMap(_.legacies)

  private def line(key: NoteKey, args: NoteArg*): Operation =
    Note(noteId, _ => Some(key.sourceless(args*)))

  private def dormant(catalog: ExecutableCatalog): Operation =
    Branch((state, _) =>
      val resolved = for
        player <- LegacyGoals.turnOrder(state)
        legacy <- legaciesOf(state, player) if !legacy.active
      yield (player, legacy.id, meets(catalog, state, legacy.id, player))
      if resolved.isEmpty then Vector.empty
      else BuildOps((live, _) => Right(resolved.map { (player, id, met) =>
          val at = PositionedLocation(Location.Legacies(lineageOf(live, player)))
          if met then Flip(id, at.location, Orientation.FaceUp)
          else Move(Piece.Card(id), at,
            PositionedLocation(Location.Deck(CardDeck.Legacy)))
        }), required = true) +:
        resolved.map((player, id, met) => line(if met then activated
          else discarded, NoteArg.Player(player), NoteArg.Card(id))))

  private def publicRow(catalog: ExecutableCatalog): Operation =
    Branch((state, _) => state.game.current.commonCards.publicLegacies
      .map(publicLegacy(catalog, _)))

  private def publicLegacy(catalog: ExecutableCatalog, legacy: LegacyId)
      : Operation =
    val decisionId = publicDecisionId(legacy)
    Branch((state, _) =>
      LegacyGoals.turnOrder(state).filter(meets(catalog, state, legacy, _))
      match
        case Vector() => Vector(BuildOps((_, _) => Right(Vector(Move(
            Piece.Card(legacy), PositionedLocation(Location.PublicLegacies),
            PositionedLocation(Location.Deck(CardDeck.Legacy))))),
            required = true),
          line(returnedPublic, NoteArg.Card(legacy)))
        case Vector(player) => taken(legacy, player)
        case tied => Imperials.chancellor(state).fold(Vector(
          ChronicleTasks.failing(OathViolation.InvalidEventOrder(
            s"no Chancellor breaks the tie for ${legacy.value}"))))(
          chancellor => Vector(
            Decide(decisionId, chancellor, DecisionQuery.ChooseOne(
              ChronicleTasks.playerOptions(tied),
              heading = Some("Choose who takes the public legacy"),
              confirmVerb = Some("Give"))),
            Branch((_, pending) =>
              ChronicleTasks.chosenPlayer(pending, decisionId).fold(
                violation => Vector(ChronicleTasks.failing(violation)),
                taken(legacy, _))))))

  /** `legacy` from the public row to `player`, active. */
  private def taken(legacy: LegacyId, player: PlayerId): Vector[Operation] =
    Vector(BuildOps((live, _) => Right(Vector(Move(Piece.Card(legacy),
        PositionedLocation(Location.PublicLegacies),
        PositionedLocation(Location.Legacies(lineageOf(live, player))),
        resultingOrientation = Some(Orientation.FaceUp)))), required = true),
      line(tookPublic, NoteArg.Player(player), NoteArg.Card(legacy)))

  private def alterations(catalog: ExecutableCatalog): Operation =
    Branch((state, _) =>
      val active = ChronicleRecord.campaignFields(state).lineages
        .flatMap(_.legacies).filter(_.active).map(_.id).sortBy(_.value)
      val foundations = state.game.campaign.foundations
      val altering = FoundationNumber.all.flatMap(number => active.filter(id =>
        catalog.legacy(id).flatMap(_.alters).contains(number) &&
          !foundations.get(number).exists(_.alterationSources(id)))
        .map(number -> _))
      if altering.isEmpty then Vector.empty
      else BuildOps((_, _) => Right(altering.map((number, id) =>
          FoundationAltered(number, id))), required = true) +:
        altering.map((number, id) => line(altered(number), NoteArg.Card(id))))
