package distantlands.gameplay.actions.economy

import distantlands.gameplay.actions.ProcedureNotes
import distantlands.gameplay.cards.AncientCitySite
import distantlands.gameplay.powers.NoteText
import distantlands.gameplay.powers.campaign.{LongbowsCard, ScoutsCard,
  WrestlersCard}
import distantlands.model._
import distantlands.testkit.{EconomyActions, PowerRules, Table}
import distantlands.testkit.Table.p1

/** A Trade for favor whose suit's bank holds less than the Trade asks for
  * says so in its line, so a player reading the log sees why the gain fell
  * short (Trade bank shortfall, owner wording 2026-11-26).
  */
class TradeBankShortfallSuite extends munit.FunSuite:
  private val rules = PowerRules.withWalkerPowers
  private val home = AncientCitySite.id
  /** A plain Order denizen to Trade with. */
  private val wrestlers = WrestlersCard.id

  /** p1 stands with Wrestlers and holds two Order advisers, so a Trade for
    * favor with Wrestlers asks the Order bank for 3. */
  private def twoOrderAdvisersWithOrderBank(held: Int) = Table.start
    .pawn(p1, at = home).denizen(wrestlers, at = home)
    .adviser(p1, LongbowsCard.id).adviser(p1, ScoutsCard.id)
    .secrets(p1, faceUp = 1).bankFavor(Suit.Order, held).ready

  private def tradeLine(held: Int): Vector[String] =
    val done = EconomyActions.trade(rules,
      twoOrderAdvisersWithOrderBank(held), p1, Vector.empty, "favor",
      DecisionOptionRef.Denizen(wrestlers))
    NoteText.said(ProcedureNotes.id(ActionRef.Trade),
      TradeProcedure.noteKeys, done.journal.events).map(_.text)

  test("a full bank reads as the plain Trade"):
    assertEquals(tradeLine(held = 5),
      Vector(s"Traded with ${wrestlers.value} for 3 favor"))

  test("a bank holding less than the Trade asks for names what it had"):
    assertEquals(tradeLine(held = 1), Vector(
      s"Traded with ${wrestlers.value} for 3 favor; but Order bank only had 1"))

  test("an empty bank says it had none"):
    assertEquals(tradeLine(held = 0), Vector(
      s"Traded with ${wrestlers.value} for 3 favor; but Order bank had none"))
