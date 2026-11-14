package distantlands.gameplay.chronicle

import distantlands.model._

/** The power window each Chronicle step runs in, in step order (Chronicle
  * steps design, "Windows, not hardcoded actors"). A step is its window's
  * node: the base rule's operations are its children, and a power's
  * `Transform` adds to them or replaces them. */
object ChronicleWindows:
  import PowerWindow._
  val sun: Vector[PowerWindow] = Vector(ChronicleRevealRelics,
    ChronicleBeforeResolveLegacies, ChronicleFlipLegacies,
    ChronicleExileDormancy, ChronicleLegacyCap, ChronicleShuffleLegacies,
    ChronicleAfterResolveLegacies, ChronicleCrown, ChronicleAssignTasks)
  val throne: Vector[PowerWindow] = Vector(ChronicleStartOfThrone,
    ChronicleOfferCitizenship,
    ChronicleShapeEmpire, ChronicleBuildEdifice, ChronicleAfterBuildEdifice,
    ChronicleStockReliquary,
    ChronicleScatterRelics, ChronicleScoreLineage, ChronicleSwearOath)
  val world: Vector[PowerWindow] = Vector(ChronicleSeparateVisions,
    ChronicleGatherDenizens, ChronicleProtectAndDispossess)
  val beacon: Vector[PowerWindow] = Vector(ChronicleIntroduceNewcomers,
    ChronicleWelcomeStranger, ChronicleShapeFoundation)
  val stars: Vector[PowerWindow] = Vector(ChronicleWriteInTheStars,
    ChronicleArchiveRivalSites, ChronicleCleanUp, ChronicleStore)

  def step(window: PowerWindow, ops: Vector[Operation]): Operation =
    Sequence(ops, Some(window))
