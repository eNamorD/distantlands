package distantlands.gameplay.powers.poweruse

import distantlands.gameplay.GrandMaskCard
import distantlands.gameplay.powerresolver.{ContributingPower, Contribution}
import distantlands.model._

/** Grand Mask (R05): "If you're an Exile, during your turn you rule cards
  * except battle plans at Imperial sites, and Imperial players do not."
  *
  * The rule is `GrandMaskRule`, which `CardRule` and `PowerAccess` read.
  * This power hooks no window. It is registered so the card counts as
  * implemented. It writes no line.
  */
case object GrandMask extends ContributingPower:
  val id: PowerId = GrandMaskCard.power.id
  def source: RuleSourceRef = RuleSourceRef.GameRule(id.value)
  def contributions: Map[PowerWindow, Vector[Contribution]] = Map.empty
