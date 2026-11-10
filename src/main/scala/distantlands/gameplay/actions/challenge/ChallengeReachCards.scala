package distantlands.gameplay.actions.challenge

import distantlands.catalog.{Denizen, Edifice, EdificeFace, Locked,
  ModifierMark, PrintedPower, Relic}
import distantlands.model._

/** The cards whose picks widen a Challenge (catalog batch 10, slice 11).
  * They live beside the Challenge, which reads their picks
  * ([[ChallengeReach]]), as Underground Library lives beside the Recover.
  * Their powers are in `gameplay.powers.challenge`; Ruined School's setup
  * rule is in `RuinedSiteSetupRules`. */
object SchoolOfVinesCard extends Edifice(EdificeId("E27"), Suit.Beast):
  object intact extends EdificeFace("School of Vines") with Locked:
    val power = PrintedPower(PowerId("edifice.e27.intact"),
      persistent = false, cost = Cost.free,
      modifier = Some(ModifierMark.Action(MajorActionType.Challenge)),
      text = "You may challenge a banner held by an enemy even if your " +
        "pawn is not at their pawn's site.")
    val powers: Vector[PrintedPower] = Vector(power)
  object ruined extends EdificeFace("Ruined School"):
    val power = PrintedPower(PowerId("edifice.e27.ruined"),
      persistent = false, cost = Cost.free,
      text = "**SETUP (END) / WHEN EXPLORED:** Place [secret] on each " +
        "site ruled by the bandits. _Players may take it in Wake._")
    val powers: Vector[PrintedPower] = Vector(power)

object SigilOfTheEyeCard extends Relic(RelicId("R43"), "Sigil of the Eye", value = 66, defense = 1):
  val power = PrintedPower(PowerId("relic.sigil-of-the-eye"),
    persistent = false, cost = Cost.free,
    modifier = Some(ModifierMark.Action(MajorActionType.Challenge)),
    text = "You may challenge the Darkest Secret from any site.")
  val powers: Vector[PrintedPower] = Vector(power)

object SigilOfTheHeartCard extends Relic(RelicId("R44"), "Sigil of the Heart", value = 53, defense = 1):
  val power = PrintedPower(PowerId("relic.sigil-of-the-heart"),
    persistent = false, cost = Cost.free,
    modifier = Some(ModifierMark.Action(MajorActionType.Challenge)),
    text = "You may challenge the People's Favor from any site.")
  val powers: Vector[PrintedPower] = Vector(power)

object MagiciansCodeCard extends Denizen(DenizenId("32"), "Magician's Code", Suit.Arcane):
  val power = PrintedPower(PowerId("denizen.magicians-code"),
    persistent = false, cost = Cost(favor = 2),
    modifier = Some(ModifierMark.Action(MajorActionType.Challenge)),
    text = "If you're challenging the Darkest Secret, gain [secret] " +
      "[secret] and place them on the banner along with any other " +
      "[secret] you're placing _(even none)_.")
  val powers: Vector[PrintedPower] = Vector(power)
