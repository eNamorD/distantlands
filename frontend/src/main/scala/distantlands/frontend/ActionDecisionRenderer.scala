package oathdigital.frontend
import org.scalajs.dom
import ServerUiSupport._
private[frontend] object ActionDecisionRenderer:
 def status(value: GameProjection, presentation: ViewerPresentation): dom.Element =
   val node = element("div", "status")
   val waitingFor = presentation.waitingForPlayerIds
   waitingFor match
     case _ if waitingFor.nonEmpty =>
       node.appendChild(dom.document.createTextNode("Waiting for "))
       waitingFor.zipWithIndex.foreach((playerId, index) =>
         if index > 0 then node.appendChild(dom.document.createTextNode(", "))
         node.appendChild(playerReference(value, playerId)))
       // The question a parked decision asks rides on the same line, so the
       // pane names the players waited on once.
       value.walkerWaiting.filter(_.playerIds == waitingFor).flatMap(_.heading)
         .foreach(heading =>
           node.appendChild(dom.document.createTextNode(s": $heading")))
     case _ if value.walkerDecision.nonEmpty =>
       // A parked walker Decide belonging to this viewer projects
       // no waitingForPlayerIds (see the ServerUiSupport.viewerPresentation
       // comment), so this is genuinely this viewer's own turn. Falling
       // through to the generic phase text or the activeParticipantId
       // fallback below would show the wrong or no turn indicator at all.
       node.textContent = value.walkerDecision.flatMap(_.query).flatMap(_.heading)
         .getOrElse("Your decision.")
     case _ if value.phase == "game-over" =>
       value.oathkeeper.flatMap(_.winnerPlayerId) match
         case Some(winner) =>
           val victory = VictoryWords.of(
             value.oathkeeper.flatMap(_.winnerVictoryKind))
           node.appendChild(dom.document.createTextNode("Game over — "))
           node.appendChild(playerReference(value, winner))
           node.appendChild(dom.document.createTextNode(s" wins ($victory)."))
         case None => node.textContent = "Game over."
     case _ =>
       // Nothing is parked and no one is waited on: the active player's
       // own turn in a named phase, as "Red's turn · Act".
       value.activeParticipantId match
         case Some(playerId) =>
           node.appendChild(playerReference(value, playerId))
           node.appendChild(dom.document.createTextNode(
             s"'s turn · ${phaseName(value.phase)}"))
         case None => node.textContent = phaseName(value.phase)
   node

 /** The phase key as a word: "act" reads "Act", "game-over" "Game over". */
 private def phaseName(phase: String): String =
   phase.replace('-', ' ').capitalize
 def actionsPanel(value: GameProjection, presentation: ViewerPresentation,
     routed: ParkedDecision.Routed, canControl: Boolean,
     drafts: SessionDrafts, controls: TableControls): dom.Element =
   import controls._
   val panel = element("section", "panel wake-actions")
   panel.appendChild(text("h2", "", "Available actions"))
   panel.appendChild(status(value, presentation))
   // What the awaited player sees beside the question, so the table can
   // follow along: a Campaign's forces, dice and totals.
   value.walkerWaiting.filter(
       _.playerIds == presentation.waitingForPlayerIds)
     .foreach(waiting => WalkerPanelSupport.statusFacts(waiting.status, panel))
   // Before the winner banner and every phase's controls: the steps to take
   // back or restore belong to whatever the pane shows below them.
   StepControls.render(value, canControl, submitCommand).foreach(panel.appendChild)
   value.oathkeeper.flatMap(_.winnerPlayerId).foreach { winner =>
     val victory = VictoryWords.of(
       value.oathkeeper.flatMap(_.winnerVictoryKind))
     panel.appendChild(winnerBanner(value, winner, victory))
   }
   // Drawn cards wait in the temporary hand: starting advisers, a Search's
   // draw, a Vision in flight. A card a placement is about rides along, even
   // when it is played from the board. The projection drops every card a
   // decision offers as an option, so nothing here has to know which action
   // put them there or which decision is asking.
   if value.temporaryHandPreview.nonEmpty then
     val preview = element("section", "temporary-hand-preview")
     preview.appendChild(text("h3", "", "Cards in hand"))
     val cards = element("div", "decision-cards")
     value.temporaryHandPreview.foreach(card =>
       cards.appendChild(CardFace.render(card)))
     preview.appendChild(cards)
     panel.appendChild(preview)
   ParkedDecision.render(value, presentation, routed, canControl, panel, drafts,
     controls)
   panel
