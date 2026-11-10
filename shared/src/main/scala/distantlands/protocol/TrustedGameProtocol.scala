package distantlands.protocol

/** What the game starts from (Chronicle browsing design, "Create
  * request"): a new first-game Chronicle under a name, or a saved
  * Chronicle's JSON. */
enum ChronicleRequest:
  case New(name: String)
  case Saved(json: String)

/** The server generates the game ID and derives each lineage, so a request
  * names only the players, their colors and the Chronicle. */
final case class TrustedGameCreateRequest(
    participants: Vector[BootstrapParticipantRequest],
    chronicle: ChronicleRequest
)

final case class TrustedSeatLink(playerId: String, url: String)
/** `chronicle` is the starting Chronicle, for the browser to keep. */
final case class TrustedGameCreateResponse(gameId: String,
    seats: Vector[TrustedSeatLink], chronicle: ChronicleEntry)
