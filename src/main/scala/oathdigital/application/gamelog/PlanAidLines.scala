package oathdigital.application.gamelog

import oathdigital.model._
import oathdigital.model.OathEvent.PlanAidSaved
import oathdigital.protocol.projection.{LogKind, LogSpan}
import LogSpan.Text

/** The Imperial Tactics lines (Empire phase design, "1b-ii", "Log"). An
  * offer names its ally, who is never the actor, and says what the save
  * added and what it withdrew: "p2 offered Shield Wall and 2 favor to
  * pay.", "p2 withdrew Shield Wall.", or both in one line. Every player
  * sees it. The clearing writes nothing. */
private[gamelog] final class PlanAidLines(words: LogWords):
  def lines(journal: LogJournal, at: Int, viewer: Option[PlayerId])
      : Vector[Posted] =
    (for
      before <- journal.readyBefore(at)
      after <- journal.readyAfter(at)
    yield journal.event(at) match
      case PlanAidSaved(ally, window, offer) =>
        val earlier =
          if before.planAid.window.contains(window) then
            before.planAid.offerOf(ally)
          else AidOffer()
        def item(ref: DecisionOptionRef): Vector[LogSpan] =
          refWords(ref, ally, after, viewer)
        val added = (offer.plans.filterNot(earlier.plans.contains) ++
          offer.requirements.filterNot(earlier.requirements.contains))
          .map(item) ++ amountWords(offer.amounts - earlier.amounts)
        val withdrawn = (earlier.plans.filterNot(offer.plans.contains) ++
          earlier.requirements.filterNot(offer.requirements.contains))
          .map(item) ++ amountWords(earlier.amounts - offer.amounts)
        val parts = Vector(
          Option.when(added.nonEmpty)(Text(" offered ") +:
            LogWords.join(added)),
          Option.when(withdrawn.nonEmpty)(Text(
            if added.nonEmpty then ", and withdrew " else " withdrew ") +:
            LogWords.join(withdrawn))).flatten.flatten
        Option.when(parts.nonEmpty)(Posted(LogKind.Decision,
          (words.player(ally) +: parts) :+ Text("."))).toVector
      case _ => Vector.empty).getOrElse(Vector.empty)

  private def refWords(ref: DecisionOptionRef, ally: PlayerId,
      after: ReadyGame, viewer: Option[PlayerId]): Vector[LogSpan] =
    def card(id: CardId) = words.one(words.seen(id, Vector(after), viewer))
    ref match
      case DecisionOptionRef.Denizen(id) => card(id)
      case DecisionOptionRef.Relic(id) => card(id)
      case DecisionOptionRef.Edifice(id) => card(id)
      case DecisionOptionRef.Button(_) => Vector(Text("the Oathkeeper title"))
      case DecisionOptionRef.Banner(banner) =>
        Vector(Text("the "), words.banner(banner))
      case DecisionOptionRef.Player(player) if player == ally =>
        after.game.current.players.find(_.player == ally)
          .flatMap(_.pawnSite).fold(Vector[LogSpan](Text("their pawn")))(
            site => Vector(Text("their pawn at "), words.site(site)))
      case other => Vector(Text(other.wireId))

  /** "2 favor to pay", "1 secret to burn", one item per part that is
    * not zero. */
  private def amountWords(amounts: AidAmounts): Vector[Vector[LogSpan]] =
    Vector(
      (amounts.favor, "favor", "pay"),
      (amounts.secrets, secrets(amounts.secrets), "pay"),
      (amounts.favorBurnt, "favor", "burn"),
      (amounts.secretsBurnt, secrets(amounts.secretsBurnt), "burn"))
      .collect { case (count, what, verb) if count > 0 =>
        Vector(Text(s"$count $what to $verb")) }

  private def secrets(count: Int): String =
    LogWords.plural(count, "secret", "secrets")
