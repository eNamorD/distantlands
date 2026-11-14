package distantlands.gameplay.powers.legacies

import distantlands.model._

/** Tables for the legacies' in-play powers (Legacy in-play powers design). */
object LegacyActionFixture:
  /** The lineage nobody is seated with in [[boxed]]. */
  val boxedLineage: LineageId = LineageId("boxed")

  /** `ready` with `player`'s legacies moved to a lineage nobody is seated
    * with, so they are a boxed lineage's and never act (ruling 1). */
  def boxed(ready: ReadyGame, player: PlayerId): ReadyGame =
    val lineage = ready.game.current.player(player)
      .getOrElse(throw AssertionError(s"$player is not seated")).lineage
    ready.updateCampaign(campaign =>
      val held = campaign.lineages(lineage)
      campaign.copy(lineages = campaign.lineages
        .updated(lineage, held.copy(legacies = Vector.empty))
        .updated(boxedLineage, held.copy(id = boxedLineage,
          previousPlayer = None))))
