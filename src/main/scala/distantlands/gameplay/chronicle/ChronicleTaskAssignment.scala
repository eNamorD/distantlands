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

  def steps(winner: PlayerId, note: ChronicleTask => Operation)
      : Vector[Operation] = Vector(
    BuildOps((_, _) => Right(Vector(
      ChronicleTaskAssigned(ChronicleTask.Throne, winner))), required = true),
    note(ChronicleTask.Throne),
    bannerTask(ChronicleTask.World, worldDecisionId,
      _.banners.peoplesFavor.holder, winner, "World", note),
    bannerTask(ChronicleTask.Beacon, beaconDecisionId,
      _.banners.darkestSecret.holder, winner, "Beacon", note),
    Branch((state, _) => doubledHolder(state).toVector.flatMap(holder =>
      giveAway(state, holder, note))))

  private def assign(task: ChronicleTask, player: PlayerId): BuildOps =
    BuildOps((_, _) => Right(Vector(ChronicleTaskAssigned(task, player))),
      required = true)

  private def bannerTask(task: ChronicleTask, decisionId: String,
      holder: CurrentGameState => Option[PlayerId], winner: PlayerId,
      name: String, note: ChronicleTask => Operation): Operation =
    Branch((state, _) => holder(state.game.current) match
      case Some(player) => Vector(assign(task, player), note(task))
      case None => Vector(
        Decide(decisionId, winner, DecisionQuery.ChooseOne(
          ChronicleTasks.playerOptions(ChronicleTasks.seated(state)),
          heading = Some(s"Give the $name task"),
          confirmVerb = Some("Give to"))),
        BuildOps((_, pending) => ChronicleTasks.chosenPlayer(pending,
          decisionId).map(player => Vector(ChronicleTaskAssigned(task, player))),
          required = true),
        note(task)))

  private def doubledHolder(state: ReadyGame): Option[PlayerId] =
    val tasks = state.game.current.chronicleTasks
    tasks.get(ChronicleTask.World).filter(tasks.get(ChronicleTask.Beacon).contains)

  private def giveAway(state: ReadyGame, holder: PlayerId,
      note: ChronicleTask => Operation): Vector[Operation] = Vector(
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
    Branch((_, pending) => givenTask(pending).toOption.toVector.map(note)))

  private def givenTask(pending: PendingTree)
      : Either[OathViolation, ChronicleTask] =
    PowerAnswers.one(pending, giveTaskDecisionId).collect {
      case `giveWorld` => ChronicleTask.World
      case `giveBeacon` => ChronicleTask.Beacon
    }.toRight(PowerAnswers.missing(giveTaskDecisionId))
