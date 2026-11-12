package distantlands.gameplay.phases.rest

import distantlands.model._

/** The one way into the Rest phase (Foundations design, slice 6): the
  * phase change, the lines that report it, then the `RestStart` window,
  * where the Festival's holder places its marker
  * (`DarkestSecretFestival`). Begin Rest and every power that ends the Act
  * phase take it, so the window folds once for each Rest, whichever way
  * the Act ended.
  */
object RestEntry:
  /** The `RestStart` window, folded once the turn is in the Rest phase. */
  val window: Operation = Sequence(Vector.empty, Some(PowerWindow.RestStart))

  /** The Act ends: `EnterPhase(Rest)`, then `lines`, then the window. The
    * lines come first so that a power's own "Act phase ended" line stays
    * beside the phase change. */
  def steps(lines: Operation*): Vector[Operation] =
    (EnterPhase(Phase.Rest) +: lines.toVector) :+ window
