package distantlands.gameplay.actions.recover

import distantlands.catalog.{Edifice, EdificeFace, Locked, ModifierMark,
  PrintedPower}
import distantlands.model._

/** Underground Library (E13). The card lives beside the Recover, which
  * reads Drowned Library's pick (`RecoverProcedure.fromRelicDeck`); its
  * powers are `UndergroundLibrary` (intact) and `DrownedLibrary` (ruined).
  */
object UndergroundLibraryCard extends Edifice(EdificeId("E13"), Suit.Arcane):
  object intact extends EdificeFace("Underground Library") with Locked:
    val power = PrintedPower(PowerId("edifice.e13.intact"),
      persistent = false, cost = Cost(favor = 1),
      text = "**ACTION:** Peek at all relics in this region, then swap " +
        "one with a relic you have, placing it facedown.")
    val powers: Vector[PrintedPower] = Vector(power)
  object ruined extends EdificeFace("Drowned Library"):
    val power = PrintedPower(PowerId("edifice.e13.ruined"),
      persistent = false, cost = Cost(secret = 1),
      modifier = Some(ModifierMark.Action(MajorActionType.Recover)),
      text = "If your pawn is at this site, you may recover the top card " +
        "of the relic deck instead. Its recover difficulty is 5.")
    val powers: Vector[PrintedPower] = Vector(power)
