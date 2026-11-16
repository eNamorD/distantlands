package distantlands.gameplay.chronicle

import distantlands.gameplay.actions.ProcedureNotes
import distantlands.gameplay.powers.PlayerFacts
import distantlands.gameplay.powers.action.TempleOfAncestorsCard
import distantlands.model._

/** Ruined Temple's pledge at the win (catalog batch 13, rulings 18 and
  * 19). The final winner is the one Royal Line and Conspirator leave, so
  * `step` is the last step of `ChronicleWinnerProcedure`, before the Sun:
  * Sun step 2's legacy goals then see the sites the winner rules. A
  * pledged winner's bandit sites each lose their bandits to the bandit
  * supply and take one of the winner's warbands, Imperial for an Imperial
  * winner, the bank topped up when short. Crown Chancellor and Shape
  * Empire then treat them as the winner's. Line: "p2 took rule of 3
  * bandit sites with Ruined Temple"; nothing with no bandit site. */
object RuinedTempleWin:
  /** The face's printed name, which the lines name. */
  val name: String = TempleOfAncestorsCard.ruined.name

  private val noteId = ProcedureNotes.id(TriggeredProcedureRef.ChronicleWinner)
  private val notes = NoteSet(noteId)
  /** "p2 took rule of 3 bandit sites with Ruined Temple". */
  val tookRule: NoteKey = notes.key("ruined-temple-took-rule", Vector(
    NotePart.Arg(0), NotePart.Text(" took rule of "), NotePart.Arg(1),
    NotePart.Text(" "), NotePart.Plural(1, "bandit site", "bandit sites"),
    NotePart.Text(s" with $name")))
  /** The lines `step` writes, on the final winner step's note id. */
  def noteKeys: Vector[NoteKey] = notes.declared

  /** Whether `player` won the game and pledged at Ruined Temple. */
  def doubles(ready: ReadyGame, player: PlayerId): Boolean =
    val current = ready.game.current
    current.templePledges(player) && current.result.exists(_.winner == player)

  /** The pledged winner's bandit sites, read when reached. */
  val step: Operation = Branch((state, _) =>
    state.game.current.result.map(_.winner).filter(doubles(state, _))
      .toVector.flatMap(winner =>
        val sites = banditSites(state)
        if sites.isEmpty then Vector.empty
        else PlayerFacts.forceKind(state, winner).fold(
          violation => Vector(ChronicleTasks.failing(violation)),
          kind => Vector(
            BuildOps((live, _) => Right(ruled(live, kind, sites)),
              required = true),
            Note(noteId, _ => Some(tookRule.sourceless(
              NoteArg.Player(winner), NoteArg.Number(sites.size))))))))

  /** Every site in play that holds bandits, with how many. */
  private def banditSites(ready: ReadyGame): Vector[(SiteId, Int)] =
    val current = ready.game.current
    current.map.inPlay.flatMap(site => current.map.sites.get(site)
      .map(_.forces.count(ForceKind.Bandit)).filter(_ > 0).map(site -> _))

  private def ruled(ready: ReadyGame, kind: ForceKind,
      sites: Vector[(SiteId, Int)]): Vector[CoreOperation] =
    def at(site: SiteId) = PositionedLocation(Location.Site(site))
    val short = sites.size - PlayerFacts.banked(ready, kind)
    Option.when[CoreOperation](short > 0)(WarbandsAdded(kind, short))
      .toVector ++ sites.flatMap((site, bandits) => Vector(
        Kill(Piece.Warbands(ForceKind.Bandit, bandits), at(site)),
        Move(Piece.Warbands(kind, 1),
          PositionedLocation(Location.WarbandBank(kind)), at(site))))
