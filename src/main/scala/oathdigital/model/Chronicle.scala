package oathdigital.model

/**
 * Up to three ordered items in storage at a site: denizens, relics or an
 * edifice. Order is kept but not meaningful between games (2026-09-21
 * Chronicle design, "The Chronicle model").
 */
final case class StoredSite(site: SiteId, items: Vector[CardId] = Vector.empty):
  require(items.size <= StoredSite.capacity,
    s"a stored site holds at most ${StoredSite.capacity} items")

object StoredSite:
  /** The most cards a site keeps in storage. */
  val capacity: Int = 3

/**
 * The between-game record. Setup becomes a pure function of a Chronicle plus
 * recorded shuffle orders (2026-09-21 Chronicle design). Fields mirror the
 * Oath NF TTS Chronicle export format
 * (https://github.com/harsch1/oath-nf-tts-scripts/blob/main/chronicle-codec.js)
 * so `application/chronicle/ChronicleTtsCodec` can import and export the same
 * strings.
 *
 * `atlasBox` and `world` together account for every site: `atlasBox` holds
 * sites in storage (index 0 is the Recent end) and `world` holds the
 * Empire's sites, which stay on the map into the next game. Deck order
 * (`worldDeck`, `relicDeck`) is kept but not meaningful between games --
 * setup shuffles both. `reliquary`, `foundations` and `lineages` are empty
 * or default for a first game.
 *
 * `name` is the Chronicle's identity in a browser, which keeps its names
 * unique; `ChronicleUpdated` copies it forward unchanged.
 *
 * `era` and `oathkeeperGoal` are required; a first game starts at
 * `EraState(20, Vector.empty)` and `OathkeeperGoal.Supremacy`. The TTS string has
 * no era field, and gains an Oath digit in encoding version 3.
 */
final case class Chronicle(
    name: String,
    atlasBox: Vector[StoredSite],
    world: Vector[StoredSite] = Vector.empty,
    worldDeck: Vector[DenizenId],
    relicDeck: Vector[RelicId],
    era: EraState,
    oathkeeperGoal: OathkeeperGoal,
    dispossessed: Vector[DenizenId] = Vector.empty,
    reliquary: Vector[RelicId] = Vector.empty,
    foundations: Map[FoundationNumber, FoundationState] = Map.empty,
    lineages: Vector[LineageState] = Vector.empty
):
  Chronicle.nameProblem(name).foreach(problem =>
    throw new IllegalArgumentException(problem))

object Chronicle:
  val maxNameLength: Int = 80

  /** Why `name` cannot name a Chronicle, if it cannot: it is trimmed and
    * has 1 to 80 characters (Chronicle browsing design, "Model"). */
  def nameProblem(name: String): Option[String] =
    if name != name.trim then
      Some("a Chronicle name has no leading or trailing spaces")
    else if name.isEmpty then Some("a Chronicle needs a name")
    else if name.length > maxNameLength then
      Some(s"a Chronicle name has at most $maxNameLength characters")
    else None
