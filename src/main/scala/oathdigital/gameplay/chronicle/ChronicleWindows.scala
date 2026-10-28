package oathdigital.gameplay.chronicle

import oathdigital.model._

/** The power window each Chronicle step runs in, in step order (Chronicle
  * steps design, "Windows, not hardcoded actors"). A step is its window's
  * node: the base rule's operations are its children, and a power's
  * `Transform` adds to them or replaces them. */
object ChronicleWindows:
  import PowerWindow._
  val sun: Vector[PowerWindow] = Vector(ChronicleRevealRelics,
    ChronicleFlipLegacies, ChronicleExileDormancy, ChronicleLegacyCap,
    ChronicleShuffleLegacies, ChronicleCrown, ChronicleAssignTasks)
  val throne: Vector[PowerWindow] = Vector(ChronicleOfferCitizenship,
    ChronicleShapeEmpire, ChronicleBuildEdifice, ChronicleStockReliquary,
    ChronicleScatterRelics, ChronicleScoreLineage, ChronicleSwearOath)

  def step(window: PowerWindow, ops: Vector[Operation]): Operation =
    Sequence(ops, Some(window))
