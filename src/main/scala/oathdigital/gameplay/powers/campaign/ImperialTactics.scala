package oathdigital.gameplay.powers.campaign

import oathdigital.gameplay.powerresolver.{Contribution, ContributingPower}
import oathdigital.model._

/** Imperial Tactics (CR p. 35): an Imperial player may use battle plans an
  * ally lends and meet requirements with an ally's. The rule hooks no
  * window; it holds the hover reasons of a plan that waits on an offer.
  */
case object ImperialTactics extends ContributingPower:
  val id: PowerId = PowerId("rule.imperial-tactics")
  def source: RuleSourceRef = RuleSourceRef.GameRule(id.value)
  def contributions: Map[PowerWindow, Vector[Contribution]] = Map.empty

  private val notes = NoteSet(id)
  /** "{p2} may offer this plan." */
  val awaitsPlanKey: NoteKey = notes.key("awaits-plan", Vector(
    NotePart.Arg(0), NotePart.Text(" may offer this plan.")))
  /** "An Imperial ally may meet what this plan needs." */
  val awaitsNeedKey: NoteKey = notes.key("awaits-need", Vector(
    NotePart.Text("An Imperial ally may meet what this plan needs.")))
  override def noteKeys: Vector[NoteKey] = notes.declared

  def awaitsPlan(owner: PlayerId): OptionReason =
    OptionReason.Noted(id, awaitsPlanKey.sourceless(NoteArg.Player(owner)))
  val awaitsNeed: OptionReason = OptionReason.Noted(id,
    awaitsNeedKey.sourceless())
