package distantlands.gameplay.chronicle

import distantlands.gameplay.powers.PowerAnswers
import distantlands.model._

/** Assign Tasks, the Sun's last step (CR p. 36, Chronicle rulings 1-3). The
  * winner takes the Throne. The People's Favor's holder takes the World and
  * the Darkest Secret's holder the Beacon; when a banner is unheld, the
  * winner gives its task to any seated player. A player left holding both
  * gives one of them, their choice, to another seated player. */
object ChronicleTaskAssignment:
  val worldDecisionId: String = "chronicle.assign.world"
  val beaconDecisionId: String = "chronicle.assign.beacon"
  val giveTaskDecisionId: String = "chronicle.assign.give-task"
  val giveRecipientDecisionId: String = "chronicle.assign.give-recipient"
  val giveWorld: DecisionOptionRef.Button =
    DecisionOptionRef.Button("chronicle.give-world")
  val giveBeacon: DecisionOptionRef.Button =
    DecisionOptionRef.Button("chronicle.give-beacon")

  /** The lines the step writes: a task taken by right, and a task given by
    * choice, with who gave it. */
  final case class Notes(took: ChronicleTask => Operation,
      gave: (ChronicleTask, PlayerId) => Operation)

  def steps(winner: PlayerId, notes: Notes): Vector[Operation] = Vector(
    BuildOps((_, _) => Right(Vector(
      ChronicleTaskAssigned(ChronicleTask.Throne, winner))), required = true),
    notes.took(ChronicleTask.Throne),
    bannerTask(ChronicleTask.World, worldDecisionId,
      _.banners.peoplesFavor.holder, winner, "World", notes),
    bannerTask(ChronicleTask.Beacon, beaconDecisionId,
      _.banners.darkestSecret.holder, winner, "Beacon", notes),
    Branch((state, _) => doubledHolder(state).toVector.flatMap(holder =>
      giveAway(state, holder, notes))))

  private def assign(task: ChronicleTask, player: PlayerId): BuildOps =
    BuildOps((_, _) => Right(Vector(ChronicleTaskAssigned(task, player))),
      required = true)

  private def bannerTask(task: ChronicleTask, decisionId: String,
      holder: CurrentGameState => Option[PlayerId], winner: PlayerId,
      name: String, notes: Notes): Operation =
    Branch((state, _) => holder(state.game.current) match
      case Some(player) => Vector(assign(task, player), notes.took(task))
      case None => Vector(
        Decide(decisionId, winner, DecisionQuery.ChooseOne(
          ChronicleTasks.playerOptions(ChronicleTasks.seated(state)),
          heading = Some(s"Give the $name task"),
          confirmVerb = Some("Give to"))),
        BuildOps((_, pending) => ChronicleTasks.chosenPlayer(pending,
          decisionId).map(player => Vector(ChronicleTaskAssigned(task, player))),
          required = true),
        notes.gave(task, winner)))

  private def doubledHolder(state: ReadyGame): Option[PlayerId] =
    val tasks = state.game.current.chronicleTasks
    tasks.get(ChronicleTask.World).filter(tasks.get(ChronicleTask.Beacon).contains)

  private def giveAway(state: ReadyGame, holder: PlayerId,
      notes: Notes): Vector[Operation] = Vector(
    Decide(giveTaskDecisionId, holder, DecisionQuery.ChooseOne(Vector(
      DecisionOption.Button(giveWorld, "Give the World task"),
      DecisionOption.Button(giveBeacon, "Give the Beacon task")),
      heading = Some("Give one task away"))),
    Decide(giveRecipientDecisionId, holder, DecisionQuery.ChooseOne(
      ChronicleTasks.playerOptions(
        ChronicleTasks.seated(state).filterNot(_ == holder)),
      heading = Some("Choose who takes it"), confirmVerb = Some("Give to"))),
    BuildOps((_, pending) => for
      task <- givenTask(pending)
      player <- ChronicleTasks.chosenPlayer(pending, giveRecipientDecisionId)
    yield Vector(ChronicleTaskAssigned(task, player)), required = true),
    Branch((_, pending) => givenTask(pending).toOption.toVector
      .map(notes.gave(_, holder))))

  private def givenTask(pending: PendingTree)
      : Either[OathViolation, ChronicleTask] =
    PowerAnswers.one(pending, giveTaskDecisionId).collect {
      case `giveWorld` => ChronicleTask.World
      case `giveBeacon` => ChronicleTask.Beacon
    }.toRight(PowerAnswers.missing(giveTaskDecisionId))
