package distantlands.frontend

import distantlands.protocol.{GameIntent => GameCommand, WalkerStartArgWire}

/** An Imperial ally's offer as they build it (Empire phase design, "Offer
  * panel"): keys are `kind:id`, as the prompt and the board spell them. It
  * starts from the saved offer and is kept while the prompt is for the same
  * window. */
private[frontend] final case class PlanAidDraft(context: BoardSelectionContext,
    userPlayerId: String, side: String, plans: Vector[String],
    requirements: Vector[String], favor: Int, secrets: Int, favorBurnt: Int,
    secretsBurnt: Int):

  /** `key` added, or taken out unless it was used. */
  def toggled(prompt: PlanAidPromptState, key: String): PlanAidDraft =
    def flip(keys: Vector[String], used: Vector[String]) =
      if !keys.contains(key) then keys :+ key
      else if used.contains(key) then keys
      else keys.filterNot(_ == key)
    if prompt.plans.exists(option => BoardOptionViews.key(option) == key) ||
        prompt.saved.plans.contains(key) then
      copy(plans = flip(plans, prompt.used.plans))
    else copy(requirements = flip(requirements, prompt.used.requirements))

  def matchesSaved(prompt: PlanAidPromptState): Boolean =
    plans.toSet == prompt.saved.plans.toSet &&
      requirements.toSet == prompt.saved.requirements.toSet &&
      favor == prompt.saved.favor && secrets == prompt.saved.secrets &&
      favorBurnt == prompt.saved.favorBurnt &&
      secretsBurnt == prompt.saved.secretsBurnt

  def command: GameCommand.SavePlanAid = GameCommand.SavePlanAid(
    plans.map(PlanAidDraft.wire), requirements.map(PlanAidDraft.wire),
    favor, secrets, favorBurnt, secretsBurnt)

private[frontend] object PlanAidDraft:
  def wire(key: String): WalkerStartArgWire = key.split(":", 2) match
    case Array(kind, id) => WalkerStartArgWire(kind, id)
    case _ => WalkerStartArgWire(key, "")

  def fresh(context: BoardSelectionContext, prompt: PlanAidPromptState)
      : PlanAidDraft = PlanAidDraft(context, prompt.userPlayerId, prompt.side,
    prompt.saved.plans, prompt.saved.requirements, prompt.saved.favor,
    prompt.saved.secrets, prompt.saved.favorBurnt, prompt.saved.secretsBurnt)

  /** The draft kept for the same window and seat, else a fresh one; none
    * without a prompt. */
  def reconcile(previous: Option[PlanAidDraft], context: BoardSelectionContext,
      prompt: Option[PlanAidPromptState]): Option[PlanAidDraft] =
    prompt.map(open => previous.filter(draft =>
      draft.context.gameId == context.gameId &&
        draft.context.playerId == context.playerId &&
        draft.userPlayerId == open.userPlayerId && draft.side == open.side)
      .getOrElse(fresh(context, open)))

  /** The board's views: each offerable item, `Locked` when used,
    * `Selected` when in the draft, else `Available`. */
  def views(prompt: PlanAidPromptState, draft: PlanAidDraft)
      : Map[String, OptionView] =
    (prompt.plans ++ prompt.requirements).map { option =>
      val key = BoardOptionViews.key(option)
      val used = prompt.used.plans.contains(key) ||
        prompt.used.requirements.contains(key)
      val chosen = draft.plans.contains(key) || draft.requirements.contains(key)
      key -> OptionView(option, if used then OptionState.Locked
        else if chosen then OptionState.Selected else OptionState.Available,
        None)
    }.toMap
