package distantlands.frontend

import distantlands.model.PlayerColor
import distantlands.protocol._
import org.scalajs.dom
import scala.scalajs.js
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import ServerUiSupport._

/** Player selection at `/new-game` for both runtime modes (Chronicle
  * browsing design, "Player selection page"): it starts a game from a new
  * Chronicle, named here, or from the loaded one, and saves a new Chronicle
  * once its game is created.
  *
  * Both modes post the same trusted creation request, so games get the same
  * generated setup. Trusted mode shows the seat links, which live only in this
  * result; the host distributes them manually. Development mode passes
  * `openCreated` to go straight to the new game instead.
  */
private[frontend] object TrustedHostUi:
  /** Lineage colors in menu order. Purple is the Chancellor's and is not offered yet. */
  val LineageColors: Vector[PlayerColor] = PlayerColor.all.filterNot(_ == PlayerColor.Purple)
  val MinPlayers = 2
  val MaxPlayers = 6

  def defaultPlayerId(color: PlayerColor): String = name(color)

  /** "Chronicle 2026-10-29", today's date: the name field's default. */
  def defaultChronicleName(): String =
    "Chronicle " + ChronicleText.localDate(new js.Date().toISOString())
  private def name(color: PlayerColor): String = color.key.capitalize

  /** Each lineage's Imperial role in the loaded Chronicle, by color: the
    * backend names a color's lineage `<color>-lineage`. */
  private def roles(chronicle: HostChronicle): Map[PlayerColor, String] =
    chronicle match
      case HostChronicle.New => Map.empty
      case HostChronicle.Continue(saved) =>
        val byLineage = saved.entry.summary.scores
          .flatMap(score => score.role.map(score.lineage -> _)).toMap
        LineageColors.flatMap(color =>
          byLineage.get(s"${color.key}-lineage").map(color -> _)).toMap

  /** "First player" or the Empire's: who goes first in this game. */
  private def firstPlayerSentence(roles: Map[PlayerColor, String]): String =
    if roles.values.exists(_ == "chancellor") then
      "Seating order is chosen at random. The Chancellor goes first, or a " +
        "random Citizen when the Chancellor does not play. "
    else "Seating order and the first player are chosen at random. "

  /** Mirrors the backend identifier rule in `TrustedGameCodecFields.identifier`. */
  def validPlayerId(value: String): Boolean =
    value.length <= 128 && value.matches("[A-Za-z0-9][A-Za-z0-9._:-]*")

  private final case class PlayerRow(color: PlayerColor, node: dom.Element, input: dom.html.Input)

  /** Draws the page and answers whether a game is being created, while
    * which the page must not be left: the seat links live only in its
    * result. */
  def start(mount: dom.Element, transport: JsonTransport,
      chronicle: HostChronicle, shelf: ChronicleShelf, back: () => Unit,
      openCreated: Option[TrustedGameCreateResponse => Unit] = None)
      : () => Boolean =
    mount.textContent = ""
    val backLink = button("Back", "chronicle-back")
    backLink.setAttribute("type", "button")
    backLink.onclick = event =>
      event.preventDefault()
      if !backLink.disabled then back()
    mount.appendChild(backLink)
    mount.appendChild(text("h1", "", "Create an Oath Digital game"))
    val imperial = roles(chronicle)
    def label(color: PlayerColor): String =
      name(color) + imperial.get(color).fold("")(role => s", ${role.capitalize}")
    mount.appendChild(text("p", "", openCreated match {
      case None => "Add one row per player, then send each player " +
        "their assigned link. Anyone with a seat link can control that seat. " +
        firstPlayerSentence(imperial) +
        "Save these links before leaving this page."
      case Some(_) => "Add one row per player. " +
        firstPlayerSentence(imperial) +
        "The new game opens as the first listed player; switch players from the toolbar."
    }))
    val form = element("form", "trusted-host-form")
    val nameField = chronicle match
      case HostChronicle.New =>
        val default = defaultChronicleName()
        val field = textField("chronicle-name", "Chronicle name",
          if shelf.find(default).isEmpty then default
          else shelf.uniqueName(default))
        val label = element("label", "host-field")
        label.appendChild(text("span", "", "Chronicle name"))
        label.appendChild(field)
        form.appendChild(label)
        Some(field)
      case HostChronicle.Continue(saved) =>
        form.appendChild(text("p", "chronicle-continued",
          s"Chronicle: ${saved.name}"))
        None
    val list = element("ol", "host-players")
    list.setAttribute("aria-label", "Players")
    form.appendChild(list)
    val status = text("p", "status", "")
    status.setAttribute("role", "status")
    var rows = Vector.empty[PlayerRow]

    val adder = element("div", "add-player")
    val toggle = button("Add a Player", "add-player-toggle")
    toggle.setAttribute("type", "button")
    toggle.setAttribute("aria-haspopup", "listbox")
    toggle.setAttribute("aria-expanded", "false")
    val menu = element("ul", "add-player-menu")
    menu.setAttribute("role", "listbox")
    menu.setAttribute("aria-label", "Player color")
    menu.setAttribute("hidden", "")
    val limit = text("span", "add-player-limit", "")
    adder.appendChild(toggle)
    adder.appendChild(menu)
    adder.appendChild(limit)
    form.appendChild(adder)

    def taken: Set[PlayerColor] = rows.map(_.color).toSet
    def options: Vector[dom.html.Button] =
      val found = menu.querySelectorAll("button")
      (0 until found.length).toVector.map(found(_).asInstanceOf[dom.html.Button])
    def closeMenu(): Unit =
      menu.setAttribute("hidden", "")
      toggle.setAttribute("aria-expanded", "false")
    def openMenu(): Unit =
      menu.textContent = ""
      LineageColors.filterNot(taken).foreach { color =>
        val item = element("li", "")
        item.setAttribute("role", "none")
        val option = button("", s"add-player-option add-player-${color.key}")
        option.setAttribute("type", "button")
        option.setAttribute("role", "option")
        option.appendChild(swatch(color))
        option.appendChild(text("span", "", label(color)))
        option.onclick = event => {
          event.preventDefault(); closeMenu(); addRow(color); toggle.focus()
        }
        item.appendChild(option)
        menu.appendChild(item)
      }
      menu.removeAttribute("hidden")
      toggle.setAttribute("aria-expanded", "true")
      options.headOption.foreach(_.focus())
    def refresh(): Unit =
      val full = rows.size >= MaxPlayers
      toggle.disabled = full || taken.size == LineageColors.size
      limit.textContent = if full then s"Maximum $MaxPlayers players" else ""
      if toggle.disabled then closeMenu()
    def addRow(color: PlayerColor): Unit =
      val node = element("li", s"host-player host-player-${color.key}")
      node.appendChild(swatch(color))
      node.appendChild(text("span", "host-player-color", label(color)))
      val input = dom.document.createElement("input").asInstanceOf[dom.html.Input]
      input.className = "host-player-id"
      input.setAttribute("aria-label", s"${label(color)} player ID")
      input.value = defaultPlayerId(color)
      input.required = true
      // An ID is kept as typed, so a phone must not capitalize or correct it.
      input.setAttribute("autocapitalize", "none")
      input.setAttribute("autocorrect", "off")
      input.setAttribute("spellcheck", "false")
      node.appendChild(input)
      val remove = button("Remove", s"remove-player remove-player-${color.key}")
      remove.setAttribute("type", "button")
      remove.setAttribute("aria-label", s"Remove ${name(color)} player")
      remove.onclick = event => {
        event.preventDefault()
        rows = rows.filterNot(_.color == color)
        list.removeChild(node)
        refresh()
      }
      node.appendChild(remove)
      list.appendChild(node)
      rows = rows :+ PlayerRow(color, node, input)
      refresh()

    toggle.onclick = event => {
      event.preventDefault()
      if menu.hasAttribute("hidden") then openMenu() else closeMenu()
    }
    menu.asInstanceOf[dom.html.Element].onkeydown = event => {
      val current = options.indexWhere(_ == dom.document.activeElement)
      event.key match
        case "Escape" => event.preventDefault(); closeMenu(); toggle.focus()
        case "ArrowDown" if options.nonEmpty =>
          event.preventDefault(); options((current + 1) % options.size).focus()
        case "ArrowUp" if options.nonEmpty =>
          event.preventDefault(); options((current - 1 + options.size) % options.size).focus()
        case _ => ()
    }

    LineageColors.take(MinPlayers).foreach(addRow)

    var creating = false
    val create = button("Create game", "create-trusted-game")
    create.setAttribute("type", "submit")
    def submit(): Unit =
      if create.disabled then return
      val ids = rows.map(_.input.value.trim)
      val problem =
        nameField.flatMap(field => shelf.nameProblem(field.value)).orElse(
        if rows.size < MinPlayers then Some(s"Need at least $MinPlayers players.")
        else rows.zip(ids).collectFirst {
          case (row, id) if !validPlayerId(id) =>
            s"${name(row.color)} player ID must start with a letter or digit and use only " +
              "letters, digits, '.', '_', ':' or '-' (up to 128 characters)."
        }.orElse(ids.diff(ids.distinct).headOption.map(id =>
          s"Player ID \"$id\" is used twice. Give each player a different ID.")))
      problem match
        case Some(message) => status.textContent = message; return
        case None => ()
      val participants = rows.zip(ids).map { case (row, id) =>
        BootstrapParticipantRequest(id, row.color)
      }
      val request = TrustedGameCreateRequest(participants, chronicle match
        case HostChronicle.New =>
          ChronicleRequest.New(nameField.fold("")(_.value.trim))
        case HostChronicle.Continue(saved) =>
          ChronicleRequest.Saved(saved.entry.json))
      create.disabled = true
      // Leaving now would lose the seat links, which only this page shows.
      backLink.disabled = true
      creating = true
      status.textContent = "Creating game…"
      transport.request("POST", "/games", Some(TrustedGameCreateRequestCodec.encode(request)))
        .foreach { result =>
          creating = false
          val decoded = result.left.map(_.message).flatMap { response =>
            if response.status >= 200 && response.status < 300 then
              TrustedGameCreateResponseCodec.decode(response.body).left.map(error =>
                GameClientFailure.DecodeFailure(error.path, error.message).message)
            // The generic reading of a 409 is a stale table position; here it
            // means every game ID the server drew was taken.
            else if response.status == 409 then
              Left(GameClientFailure.HttpFailure(409, "duplicate-game",
                "The server could not assign a new game ID. Select Create game again.")
                .message)
            // Game start's refusal of the Chronicle, in its own words.
            else if response.status == 422 then Left(ChronicleClient.reason(response))
            else Left(GameJson.responseFailure(response).message)
          }
          val unsaved = decoded.toOption.flatMap(created => chronicle match
            case HostChronicle.New => shelf.put(created.chronicle).left.toOption
            case HostChronicle.Continue(_) => None)
          decoded match
            case Left(message) =>
              create.disabled = false
              backLink.disabled = false
              status.textContent = message
            case Right(created) if openCreated.nonEmpty =>
              status.textContent = "Game created. Opening it…"
              openCreated.foreach(_(created))
            case Right(created) =>
              mount.removeChild(form)
              mount.removeChild(backLink)
              status.textContent =
                "Game created. Copy and save each assigned seat link." +
                  unsaved.fold("")(reason => s" $reason")
              mount.appendChild(text("p", "created-game-id", s"Game ID: ${created.gameId}"))
              val links = element("ol", "seat-links")
              created.seats.foreach { seat =>
                val row = element("li", "seat-link-row")
                row.appendChild(text("span", "", seat.playerId))
                val link = dom.document.createElement("input").asInstanceOf[dom.html.Input]
                link.className = "seat-link"
                link.value = seat.url
                link.readOnly = true
                link.setAttribute("aria-label", s"Seat link for ${seat.playerId}")
                row.appendChild(link)
                val copy = button("Copy link", "copy-seat-link")
                copy.onclick = _ => {
                  link.focus(); link.select()
                  status.textContent = "Link selected. Copy it to share with its player."
                  val clipboard = dom.window.navigator.asInstanceOf[js.Dynamic].selectDynamic("clipboard")
                  if !js.isUndefined(clipboard) && clipboard != null then
                    clipboard.writeText(seat.url).asInstanceOf[js.Promise[Unit]].toFuture.foreach { _ =>
                      status.textContent = s"Copied seat link for ${seat.playerId}."
                    }
                }
                row.appendChild(copy)
                links.appendChild(row)
              }
              mount.appendChild(links)
        }
    create.onclick = event => { event.preventDefault(); submit() }
    form.asInstanceOf[dom.html.Form].onsubmit = event => { event.preventDefault(); submit() }
    form.appendChild(create)
    mount.appendChild(form)
    mount.appendChild(status)
    () => creating

  private def swatch(color: PlayerColor): dom.Element =
    val node = element("span", s"color-swatch ${PlayerColorCss.of(color)}")
    node.setAttribute("aria-hidden", "true")
    node
