package distantlands.frontend

import distantlands.protocol.{GameIntent => GameCommand, WalkerStartArgWire}
import org.scalajs.dom
import ServerUiSupport.{button, element, playerDisplayName, text}

/** The viewer's consent column for one power they rule (ruling 16): a
  * row per other player and what choosing a setting does, and the turn's
  * requests about the power. Without control (`choose` is `None`) the
  * grid only shows the settings. */
private[frontend] final case class ConsentGrid(rows: Vector[ConsentGridRow],
    choose: Option[(String, String) => Unit],
    requests: ConsentRequests = ConsentRequests.none)
private[frontend] final case class ConsentGridRow(playerId: String,
    name: String, setting: String)

/** One request of the turn that the viewer answered, with the
  * requester's display name. */
private[frontend] final case class ConsentRequestEntry(requester: String,
    row: ConsentRequestRowState)

/** The turn's requests about one power (reconsider design, "Requests
  * list"), newest first. `powerId` finds the power again in a later
  * projection. `change` sends an entry's new answer, and is `None` without
  * control. */
private[frontend] final case class ConsentRequests(powerId: String,
    entries: Vector[ConsentRequestEntry],
    change: Option[(ConsentRequestEntry, Boolean) => Unit])
private[frontend] object ConsentRequests:
  val none: ConsentRequests = ConsentRequests("", Vector.empty, None)

/** What a site power's label, or a site card, opens (consent design,
  * "Frontend"; slice 3): the power's printed text, and the viewer's grid
  * when they rule the power. `submit` sends a preference, and is `None`
  * without control. */
private[frontend] final class SitePowerLinks(value: Option[GameProjection],
    submit: Option[GameCommand => Unit]):
  def grid(power: SitePower): Option[ConsentGrid] =
    value.flatMap(_.consentPreferences.find(_.powerId == power.id))
      .flatMap(gridOf)

  /** The viewer's grid for the power `card` prints, when they rule it
    * (consent slice 3). */
  def cardGrid(card: CardDetails): Option[ConsentGrid] =
    value.flatMap(_.consentPreferences.find(_.cardId.contains(card.cardId)))
      .flatMap(gridOf)

  /** The viewer's grid for the power the banner keyed `key` prints, when
    * they rule it: the Darkest Secret's Festival, while its holder. */
  def bannerGrid(key: String): Option[ConsentGrid] =
    value.flatMap(_.consentPreferences.find(_.bannerKey.contains(key)))
      .flatMap(gridOf)

  /** The festival marker's power is the Darkest Secret's Festival face, so
    * the marker opens that banner's grid. */
  def festivalGrid: Option[ConsentGrid] = bannerGrid("darkest-secret")

  /** The turn's requests about the power `powerId`, when the viewer still
    * rules it: what an open overlay redraws its list from. */
  def requests(powerId: String): Option[ConsentRequests] = for
    projection <- value
    preferences <- projection.consentPreferences.find(_.powerId == powerId)
  yield requestsOf(projection, preferences)

  private def requestsOf(projection: GameProjection,
      preferences: ConsentPreferencesState): ConsentRequests =
    ConsentRequests(preferences.powerId, preferences.requests.map(row =>
        ConsentRequestEntry(playerDisplayName(projection,
          row.requesterPlayerId), row)),
      submit.map(send => (entry, allow) => send(GameCommand.ReconsiderConsent(
        entry.row.requesterPlayerId, entry.row.decisionId,
        WalkerStartArgWire(entry.row.optionKind, entry.row.optionId),
        allow))))

  private def gridOf(preferences: ConsentPreferencesState)
      : Option[ConsentGrid] =
    value.map(projection => ConsentGrid(preferences.rows.map(row =>
        ConsentGridRow(row.playerId, playerDisplayName(projection,
          row.playerId), row.setting)),
      submit.map(send => (requester, setting) =>
        send(GameCommand.SetConsentPreference(preferences.powerId, requester,
          setting))),
      requestsOf(projection, preferences)))

private[frontend] object SitePowerLinks:
  val none: SitePowerLinks = new SitePowerLinks(None, None)
  def of(value: GameProjection, submit: Option[GameCommand => Unit])
      : SitePowerLinks =
    new SitePowerLinks(Some(value), submit)

/** The grid as the overlay draws it (reconsider design, slice 1): the
  * heading says what the viewer consents to, and each row is a player's
  * name on its own line over that player's three settings, which the
  * stylesheet lays in equal thirds. A choice is shown at once and sent;
  * the overlay ignores polls while open, so it keeps what was chosen, and
  * a choice already shown sends nothing. */
private[frontend] object ConsentGridView:
  private val settings = Vector("never" -> "Never Allow",
    "case-by-case" -> "Case-by-case", "always" -> "Always Allow")

  def render(grid: ConsentGrid): dom.Element =
    val section = element("section", "consent-grid")
    section.appendChild(text("h4", "consent-grid-heading",
      "Who may ignore this power"))
    grid.rows.foreach { row =>
      val line = element("div", "consent-grid-row")
      line.setAttribute("data-player-id", row.playerId)
      line.appendChild(text("span", "consent-grid-player", row.name))
      // The three settings stand together under the name, and are named
      // for the player they answer (implementation ruling R2).
      val group = element("div", "consent-grid-choices")
      group.setAttribute("role", "group")
      group.setAttribute("aria-label", row.name)
      val choices = settings.map { (key, label) =>
        val choice = button(label, "consent-grid-choice")
        choice.setAttribute("data-setting", key)
        choice.setAttribute("aria-pressed", (key == row.setting).toString)
        choice.disabled = grid.choose.isEmpty
        key -> choice
      }
      choices.foreach { (key, choice) =>
        choice.onclick = _ =>
          if choice.getAttribute("aria-pressed") != "true" then
            choices.foreach((_, other) => other.setAttribute("aria-pressed",
              (other eq choice).toString))
            grid.choose.foreach(_(row.playerId, key))
        group.appendChild(choice)
      }
      line.appendChild(group)
      section.appendChild(line)
    }
    section.appendChild(ConsentRequestsView.mount(grid.requests))
    section
