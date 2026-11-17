package distantlands.gameplay.chronicle

import distantlands.gameplay.powerresolver.{Contribution, ContributingPower,
  Transform}
import distantlands.gameplay.powers.PowerAnswers
import distantlands.gameplay.powers.economy.HallowedSpringCard
import distantlands.model._

/** Hiding Place (E26 ruined): "CHRONICLE (THRONE): This site's ruler may
  * secretly keep a relic they have facedown. They start the next game
  * with it." Its step is at the start of the Throne, after the relics are
  * hidden and after Reformer, before Shape Empire can discard it (Last
  * Chronicle powers design, rulings 3, 4 and 6). The ruler is read when
  * the step is reached, by `SiteRule.rulingPlayer`: a player's warbands,
  * or the Chancellor for an Imperial site, never the Grand Mask's wearer.
  * Bandits, nobody, or an Empire with no Chancellor asks nothing. The
  * ruler chooses one of their relics but the Grand Scepter, or Decline,
  * even with a single relic. The relic is facedown already;
  * `RelicKeep` keeps it and writes its lines. */
case object HidingPlace extends ContributingPower:
  val id: PowerId = HallowedSpringCard.ruined.power.id
  def source: RuleSourceRef = RuleSourceRef.GameRule(id.value)
  /** After Reformer (ruling 6). Transforms of one phase fold by priority,
    * then by source key, and this power's key sorts before Reformer's,
    * whose priority is the default 0. */
  override def priority: Int = 1
  def contributions: Map[PowerWindow, Vector[Contribution]] = Map(
    PowerWindow.ChronicleStartOfThrone -> Vector(Transform(
      (_, ops) => ops :+ step)))

  val decline: DecisionOptionRef.Button = DecisionOptionRef.Button("decline")

  def decisionId(player: PlayerId): String =
    s"chronicle.throne.hiding-place.${player.value}"

  val step: Operation = Branch((state, _) =>
    ruler(state).toVector.flatMap(ruler =>
      RelicKeep.keepable(state, ruler) match
        case Vector() => Vector.empty
        case relics => Vector(
          Decide(decisionId(ruler), ruler, DecisionQuery.ChooseOne(
            DecisionOption.Button(decline, "Decline") +: relics.map(relic =>
              DecisionOption.Relic(DecisionOptionRef.Relic(relic.id))),
            heading = Some("Hiding Place: you may keep a relic for the " +
              "next game"))),
          Branch((_, pending) => PowerAnswers.one(pending, decisionId(ruler))
            .collect { case DecisionOptionRef.Relic(relic) => relic }
            .toVector.flatMap(RelicKeep.kept(ruler, _,
              HallowedSpringCard.id))))))

  /** The player who rules Hiding Place's site, while it is on the map. */
  private def ruler(state: ReadyGame): Option[PlayerId] =
    state.game.current.edificeSite(HallowedSpringCard.id, EdificeSide.Ruined)
      .flatMap(SiteRule.rulingPlayer(state, _)).collect {
        case SiteRuler.Player(player) => player }
