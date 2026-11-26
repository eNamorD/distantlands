# Game Art, Slice 1: Plumbing and Cards Implementation Plan

> Executed inline by the implementer. Steps use checkbox (`- [ ]`) syntax
> for tracking.

**Goal:** A host starts the server with `--art-path <dir>`, and a player
with **Game art** switched on in the table's new Settings dialog sees the
licensed image on every card face and card back, in the same box the drawn
face fills. Where an image is missing, slow or failed, that box stays
drawn.

**Architecture:** The server scans the folder once at startup against
`ArtCatalog.allPaths`, a table in the `shared` module, and serves the kept
files under `/art/` with a manifest at `/art/manifest.json`. The client's
`GameArt` joins that manifest with a stored preference and answers one
question, `GameArt.url(key)`. Every surface swaps art through
`ArtLayer.attach(box, key)`, which adds an absolutely positioned image to a
box and marks the box `art-ready` when the image loads; the stylesheet then
hides the box's `printed` content with opacity. Layout never reads the
image.

**Tech Stack:** Scala 3, akka-http, Scala.js, munit, jsdom, CSS, `./sbtw`
(sbt wrapper), compiler flags including `-Werror`.

**Spec:** [Game art from the host's folder](../../specs/2026-11/2026-11-26-game-art-design.md),
"Delivery", item 1. It builds on the
[real proportions design](../../specs/2026-11/2026-11-13-real-proportions-design.md)
and its
[implementation rulings](../../specs/2026-11/2026-11-13-real-proportions-design-implementation-rulings.md)
(R9, the `printed` marking; R4, the token band). Rulings made while this
plan runs go in the
[implementation rulings](../../specs/2026-11/2026-11-26-game-art-design-implementation-rulings.md).
Read the spec in full before starting.

## Rulings folded in

The orchestrator ruled on the plan's open questions; each is recorded in
the implementation rulings.

- **R1.** A card's path is its "(NF)" file when the set has one, else its
  base file `cards/<Name>.png`. The catalog lists both forms by name
  (Task 2, Task 3).
- **R2.** A knowable card has its back's image on the box and its face's
  image on the hover summary, a box of its own (Task 7).
- **R3.** Printed content is hidden by its nearest box, through
  `--printed-opacity` (Task 6).
- **R4.** An art path that is not a folder serves no art and logs a line
  (Task 4).
- **R5.** `DLQ_ART_PATH` (Task 1). Slice 3 documents it.
- **R6.** The catalog holds the cards and backs this slice draws; later
  slices add theirs (Task 2).
- **R7.** The manifest is `no-cache`; "Loading the art list…" while
  pending; a disabled switch shows Off; Settings and Dev tools close each
  other; the Vision's turn is confirmed by eye and matches the overlay's
  long edge; a known dormant legacy gets its face (Tasks 4, 7, 8, 9).

## Global Constraints

- Work only in `/Users/roman/projects/distantlands/.worktrees/game-art`, on
  branch `game-art`. Never commit on `0.2-alpha` or `main`.
- Licensed art: never commit, copy into the worktree, or upload any file
  from `/Users/roman/projects/distantlands/reference/assets/`. No test
  reads a licensed file. A test that needs a folder builds a temporary one
  of 1×1 PNGs named on catalog paths. Tracked docs list file names only.
- `.tooling` and `node_modules` in the worktree are symlinks to the main
  checkout. Never `git add -A` or `git add .`; add files by path.
- The layout must not depend on whether art is shown. No rule reads the
  image's size; the image never sizes its box.
- With art off, or with no manifest, every renderer builds exactly the DOM
  it builds today. Every existing render suite passes without edits.
- The preference key is exactly `distantlands.art`. It is on by default.
- The manifest is `GET /art/manifest.json`, body `{"files": [...]}`. A
  served file carries `Cache-Control: public, max-age=86400` and an ETag.
- The image is `<img class="art" alt="" aria-hidden="true"
  decoding="async" loading="lazy">`. The drawn content stays in the DOM and
  in the accessibility tree; it is hidden with `opacity: 0`, never with
  `display: none`.
- UI copy, exactly: the button "Settings"; the heading "Settings"; the
  switch "Game art"; the hint "Card faces, the mat and pieces drawn from
  the host's art."; the reasons "This server has no game art." and
  "Couldn't load the art list; reload to try again."; the partial count
  "<n> of <total> images; the rest stay drawn."
- No `transition` and no `animation`. No new colour: the dialog and the
  switch use the stylesheet's existing custom properties.
- Old journals and wire formats need no backward compatibility. This slice
  changes no wire format.
- The build is `-Werror`. `scripts/check-architecture.py` enforces size
  limits and layering: the shared module references only
  `distantlands.protocol`, and the frontend only `frontend` and `protocol`.
- Never write a bare `===` or `=word` in a shell command (zsh aborts the
  line).
- Never touch the live database `var/distantlands*`. The browser check
  uses a scratch database on port 8083.
- Docs, comments, commit messages and plans are plain English prose. Test
  helper names state what they build.
- Commit messages are Conventional Commits and end with
  `Co-Authored-By: Claude Fable 5.2 <noreply@anthropic.com>`. Run
  `git log -3` after every commit.

### Protected

These must behave exactly as before. Do not edit them except where a step
names the edit:

- The `printed` marking (ruling R9): which elements carry it, and that no
  control is printed or sits inside printed content. `PrintedLayerSuite`
  and `CardFaceSuite` pass without edits.
- `CardFace.render`'s existing DOM: classes, attributes, child order and
  the click that opens the inspector. This slice only appends images.
- `CardInspectionOverlay`: every method keeps its signature and behaviour.
- `ChronicleStorage` and `MemoryChronicleStorage`: used as they are.
- `ProductionFrontendRoutes`: its paths and its cache policy.
- `ServerConfig`: every existing option, its environment variable, its
  default and its error text. `ServerConfigSuite`'s existing tests pass
  without edits.
- The development panel: its markup, its Escape and Close behaviour and
  where focus returns.
- Player boards and the reliquary board stay abstract; the Chronicle pages
  draw no art.

## File Structure

| File | Responsibility |
|---|---|
| `shared/src/main/scala/distantlands/protocol/art/ArtCatalog.scala` (new) | `ArtKey`; the table from game identifiers to folder paths; `allPaths`. |
| `src/main/scala/distantlands/server/ServerConfig.scala` | The `--art-path` option. |
| `src/main/scala/distantlands/server/ArtLibrary.scala` (new) | The startup scan: which catalog paths the folder holds, and the two log lines. |
| `src/main/scala/distantlands/server/ArtRoutes.scala` (new) | `/art/manifest.json` and `/art/<path>`. |
| `src/main/scala/distantlands/server/ServerRoutes.scala`, `DistantLandsServer.scala` | Wiring and the startup log. |
| `docs/operations/game-art.md` (new) | The folder's layout and every path, names only. |
| `frontend/src/main/scala/distantlands/frontend/GameArt.scala` (new) | Manifest, preference, `url`, and which URLs loaded or failed. |
| `frontend/src/main/scala/distantlands/frontend/ArtLayer.scala` (new) | `attach(box, key)`. |
| `frontend/src/main/scala/distantlands/frontend/SettingsDialog.scala` (new) | The Settings button, dialog and switch. |
| `frontend/src/main/scala/distantlands/frontend/CardFace.scala`, `ServerUiSupport.scala` | Card faces and backs attach art. |
| `frontend/src/main/scala/distantlands/frontend/GameTableShell.scala`, `ServerModeUi.scala`, `Main.scala` | Wiring. |
| `frontend/styles.css` | The art layer, the Vision's quarter turn, the dialog. |
| `scripts/check-architecture.py` | `distantlands.art` is a storage key, not a package. |

---

### Task 1: The `--art-path` option

**Files:**
- Modify: `src/main/scala/distantlands/server/ServerConfig.scala`
- Test: `src/test/scala/distantlands/server/ServerConfigSuite.scala` (append)

**Interfaces:**
- Produces: `ServerConfig.artPath: Option[java.nio.file.Path]`, the last
  field, default `None`; absolute and normalised when set. Read from
  `--art-path DIR`, then `DLQ_ART_PATH` (R5).

- [ ] **Step 1: Write the failing tests**

Append to `ServerConfigSuite`, inside the class. The suite's `parse` helper
returns the parsed configuration; use `rejected` for the error case as the
neighbouring tests do.

```scala
  test("no art path is configured unless the host names one"):
    assertEquals(parse(Array.empty, Map.empty).artPath, None)

  test("--art-path names the art folder, absolute and normalised"):
    val config = parse(Array("--art-path", "var/art/../assets"), Map.empty)
    assertEquals(config.artPath,
      Some(Paths.get("var/assets").toAbsolutePath.normalize))

  test("DLQ_ART_PATH names the art folder, and --art-path overrides it"):
    assertEquals(parse(Array.empty, Map("DLQ_ART_PATH" -> "var/env-art"))
      .artPath, Some(Paths.get("var/env-art").toAbsolutePath.normalize))
    assertEquals(parse(Array("--art-path", "var/cli-art"),
      Map("DLQ_ART_PATH" -> "var/env-art")).artPath,
      Some(Paths.get("var/cli-art").toAbsolutePath.normalize))

  test("a blank --art-path is refused"):
    val errors = rejected(ServerConfig.parse(Array("--art-path", " "),
      Map.empty, version))
    assertEquals(errors, Vector("--art-path: must not be blank"))

  test("the usage line names --art-path"):
    assert(ServerConfig.usage.contains("[--art-path DIR]"))
```

- [ ] **Step 2: Run the tests to verify they fail**

Run: `./sbtw "root/testOnly distantlands.server.ServerConfigSuite"`
Expected: compilation fails with `value artPath is not a member of
distantlands.server.ServerConfig`.

- [ ] **Step 3: Implement the option**

In `ServerConfig.scala`:

1. Add the field last, with a default so existing constructions compile:

```scala
final case class ServerConfig(
    host: String,
    port: Int,
    publicBaseUrl: Option[URI],
    databasePath: Path,
    mode: ServerMode,
    version: String,
    /** The host's folder of licensed art (game art design, "The art
      * folder"); without it the server serves none. */
    artPath: Option[Path] = None
)
```

2. Add `"--art-path"` at the end of `SupportedOptionOrder`.
3. Change `usage` to read
   `"[--mode development|trusted-alpha] [--art-path DIR]. "` in place of
   `"[--mode development|trusted-alpha]. "`.
4. In `parse`, after `mode`:

```scala
    val artPath = optionalValue("--art-path", "DLQ_ART_PATH", cli,
      environment) match
      case None => Right(None)
      case Some(raw) => parsePath("--art-path", raw).map(Some(_))
```

5. Add `"--art-path" -> artPath.left.toOption` to `validationErrors`, and
   pass `artPath.toOption.get` as the last argument of the `ServerConfig`
   built on success.

- [ ] **Step 4: Run the tests to verify they pass**

Run: `./sbtw "root/testOnly distantlands.server.ServerConfigSuite"`
Expected: PASS, every test.

- [ ] **Step 5: Commit**

```bash
git add src/main/scala/distantlands/server/ServerConfig.scala \
  src/test/scala/distantlands/server/ServerConfigSuite.scala
git commit -m "feat(server): add the --art-path option

Co-Authored-By: Claude Fable 5.2 <noreply@anthropic.com>"
git log -3
```

---

### Task 2: `ArtCatalog`

**Files:**
- Create: `shared/src/main/scala/distantlands/protocol/art/ArtCatalog.scala`
- Test: `src/test/scala/distantlands/catalog/ArtCatalogSuite.scala` (new;
  it reads `NewFoundations.catalog`, which only the root project holds)

**Interfaces:**
- Produces, in package `distantlands.protocol.art`:
  - `final case class ArtKey private[art] (path: String)`
  - `ArtCatalog.cardFace(name: String): Option[ArtKey]`
  - `ArtCatalog.cardBack(cardKind: String): Option[ArtKey]`, for the
    projection's kinds `"denizen"`, `"vision"`, `"relic"`, `"legacy"`
  - `ArtCatalog.allPaths: Vector[String]`
  - `ArtCatalog.reprintedDenizens`, `baseDenizens`, `relics`,
    `edificeFaces`, `legacies`, `visions`: `Vector[String]`, the printed
    names with a face image.
- Consumes: nothing. The shared module references only
  `distantlands.protocol`.

The card names below were read from the card objects and checked against
the file names in the `bgs-cards` clone. `reprintedDenizens` holds the 33
denizens with a "(NF)" file and `baseDenizens` the 222 with only a base
file (R1).

- [ ] **Step 1: Write the failing tests**

Create `src/test/scala/distantlands/catalog/ArtCatalogSuite.scala`:

```scala
package distantlands.catalog

import distantlands.gameplay.cards.NewFoundations
import distantlands.protocol.art.ArtCatalog
import distantlands.protocol.projection.VisionCardPresentation

/** `ArtCatalog` names files by the cards' printed names, which the card
  * objects own (game art design, "The catalog"). A renamed card, or a card
  * the table forgot, fails here. */
class ArtCatalogSuite extends munit.FunSuite:
  private val catalog = NewFoundations.catalog

  private def pathOf(name: String): Option[String] =
    ArtCatalog.cardFace(name).map(_.path)

  test("every relic, edifice face, legacy and Vision has one face path, " +
      "named for the card"):
    val names = catalog.relics.map(_.name) ++
      catalog.edifices.flatMap(e => Vector(e.intact.name, e.ruined.name)) ++
      catalog.legacies.map(_.name) ++
      VisionCardPresentation.byId.values.map(_.name)
    names.foreach(name =>
      assertEquals(pathOf(name), Some(s"cards/$name (NF).png"), name))

  test("every denizen has one face path: its New Foundations file when " +
      "the set has one, else its base file"):
    assertEquals(ArtCatalog.reprintedDenizens.size, 33)
    assertEquals(ArtCatalog.baseDenizens.size, 222)
    assertEquals((ArtCatalog.reprintedDenizens ++ ArtCatalog.baseDenizens)
      .sorted, catalog.denizens.map(_.name).sorted)
    ArtCatalog.reprintedDenizens.foreach(name =>
      assertEquals(pathOf(name), Some(s"cards/$name (NF).png"), name))
    ArtCatalog.baseDenizens.foreach(name =>
      assertEquals(pathOf(name), Some(s"cards/$name.png"), name))

  test("a name no card prints has no face path"):
    assertEquals(pathOf("Facedown denizen"), None)

  test("the lists hold no name the card objects do not print"):
    assertEquals(ArtCatalog.relics.toSet, catalog.relics.map(_.name).toSet)
    assertEquals(ArtCatalog.edificeFaces.toSet, catalog.edifices
      .flatMap(e => Vector(e.intact.name, e.ruined.name)).toSet)
    assertEquals(ArtCatalog.legacies.toSet,
      catalog.legacies.map(_.name).toSet)
    assertEquals(ArtCatalog.visions.toSet,
      VisionCardPresentation.byId.values.map(_.name).toSet)

  test("each kind of card a viewer may not identify has its back"):
    assertEquals(Vector("denizen", "vision", "relic", "legacy")
      .map(kind => ArtCatalog.cardBack(kind).map(_.path)), Vector(
      Some("tts/backs/denizen-back.jpg"), Some("tts/backs/vision-back.jpg"),
      Some("tts/backs/relic-back.jpg"), Some("tts/backs/legacy-back.jpg")))
    assertEquals(ArtCatalog.cardBack("edifice"), None)

  test("no two faces share a path, and every path is listed once"):
    assertEquals(ArtCatalog.allPaths.distinct, ArtCatalog.allPaths)
    val faces = ArtCatalog.reprintedDenizens ++ ArtCatalog.baseDenizens ++
      ArtCatalog.relics ++ ArtCatalog.edificeFaces ++ ArtCatalog.legacies ++
      ArtCatalog.visions
    assertEquals(faces.distinct, faces)
    assertEquals(ArtCatalog.allPaths.size, faces.size + 4)

  test("every path is a png, jpg or webp under cards/ or tts/, with no " +
      "step out of the folder"):
    ArtCatalog.allPaths.foreach { path =>
      assert(path.matches("""(cards|tts)/[^\\]+\.(png|jpg|webp)"""), path)
      assert(!path.split('/').contains(".."), path)
    }
```

- [ ] **Step 2: Run the tests to verify they fail**

Run: `./sbtw "root/testOnly distantlands.catalog.ArtCatalogSuite"`
Expected: compilation fails with `value art is not a member of
distantlands.protocol`.

- [ ] **Step 3: Write `ArtCatalog`**

Create `shared/src/main/scala/distantlands/protocol/art/ArtCatalog.scala`:

```scala
package distantlands.protocol.art

/** One image in the host's art folder, named by its path under the folder
  * (game art design, "The catalog"). Only [[ArtCatalog]] makes one, so every
  * key is a path the server may serve. */
final case class ArtKey private[art] (path: String)

/** Where each game identifier's image lies in the host's art folder. The
  * server serves exactly `allPaths`, and the client asks for nothing else,
  * so the two read one table. It holds file names only; the art is the
  * host's.
  *
  * A card is known by its printed name, which is also its file's name: an
  * edifice has one name for each face, so a name is a card and its side.
  * It grows with the surfaces that draw art (implementation ruling R6).
  */
object ArtCatalog:
  /** A list written as names parted by semicolons, wrapped freely. */
  private def names(list: String): Vector[String] =
    list.split(';').toVector.map(_.trim.replaceAll("\\s+", " "))
      .filter(_.nonEmpty)

  /** The denizens New Foundations reprints, whose files end "(NF)". */
  val reprintedDenizens: Vector[String] = names("""
    A Round of Ale; Alchemist; Ancient Pact; Ballot Box; Bandit
    Chief; Bewitch; Book Burning; Bracken; Deed Writer; Defame;
    Disgraced Captain; Family Wagon; Forest Council; Forgotten
    Vault; Giant Python; Gossip; Inquisitor; Keep; Long-Lost Heir;
    Magician's Code; Martial Culture; Memory of Home; Quick Exit;
    Relic Breaker; Relic Thief; Relic Worship; Riots; Royal
    Ambitions; Salt the Earth; Tinker's Fair; Vow of Division; Vow
    of Silence; Vow of Union
    """)

  /** The denizens New Foundations left as the base game printed them,
    * whose files carry the bare name. */
  val baseDenizens: Vector[String] = names("""
    A Fast Steed; A Small Favor; Acting Troupe; Ancient Binding;
    Ancient Bloodline; Animal Host; Animal Playmates; Arcane Armor;
    Arcane Brokers; Armed Mob; Assassin; Augury; Autumn Wind;
    Awaited Return; Bandit Paymaster; Bandit Prince; Banner
    Breakers; Baron; Battle Axes; Battle Honors; Bear Traps; Beast
    Tamer; Bed of Roots; Billowing Fog; Birdsong; Blackmail; Blood
    Pact; Bog; Book Binders; Call for Help; Captains; Careful Plans;
    Catacombs; Chaos Cult; Charlatan; Charming Friend; City Wall;
    Code of Honor; Convoys; Council Arbiter; Council Seat; Cracked
    Sage; Cracking Ground; Crop Rotation; Curfew; Dark Enforcer;
    Dazzle; Diplomat; Disciples; Dissent; Downtrodden; Dream Thief;
    Elders; Enchantress; Encirclement; Errand Boy; Extra Provisions;
    Fabled Feast; Fae Battalion; Fae Merchant; Faithful Friend;
    False Prophet; Family Heirloom; Favored Son; Fearsome General;
    Field Promotion; Fire Talkers; Firebrand; Forced Labor; Forest
    Paths; Forest Warden; Friendly Familiar; Gambling Hall;
    Garrison; Garrison Armory; Glamor; Gleaming Armor; Golem
    Legions; Grasping Vines; Great Crusade; Great Feast; Great Herd;
    Hearts and Minds; Herald; Homesteaders; Honor Guard; Horse
    Archers; Hospital; Hospitality; Hunger; Hunting Party;
    Initiation Rite; Insect Swarm; Insomnia; Jinx; Key to the City;
    Kindred Warriors; Knights Errant; Lancers; Land Warden; League
    Treaty; Levelers; Longbows; Lost Tongue; Map Library; Marriage;
    Marsh Spirit; Master at Arms; Master of Disguise; Memory of
    Nature; Mercenaries; Messenger; Military Parade; Mountain Giant;
    Mounted Library; Mounted Patrol; Moving Market; Mushrooms;
    Nature Worship; Naysayers; New Growth; News from Afar;
    Observatory; Old Songs; Oracle; Outriders; Palanquin; Peace
    Envoy; Pied Piper; Pilgrimage; Plague Engines; Pledge of
    Defense; Pledge to Discord; Pressgangs; Quartermaster; Rain
    Boots; Rangers; Relic Hunter; Reliquary Raid; Resettle;
    Revelation; Rival Khan; Roving Terror; Rowdy Pub; Royal Stables;
    Royal Tax; Rusting Ray; Saddle Makers; Salad Days; Scouts;
    Scryer; Sealing Ward; Search Party; Second Chance; Second Wind;
    Secret Police; Secret Signal; Shield Wall; Shifting Fog; Siege
    Engines; Signal Trees; Silver Tongue; Skilled Merchants; Sleight
    of Hand; Small Friends; Sneak Attack; Special Envoy; Specialist;
    Spell Breaker; Spinning Bee; Spirit Snare; Spoiled Supplies;
    Storm Caller; Storyteller; Taming Charm; Tavern Songs; Tents;
    Terror Spells; The Gathering; The Great Levy; The Old Oak; The
    Red Seer; Threatening Roar; Toll Roads; Town Meeting; Tracker;
    Traveling Doctor; Traveling Negotiator; Tribute Spoils; True
    Names; True Oath; Tutor; Twin Brother; Tyrant; Unstable Summon;
    Village Constable; Village Idiot; Vow of Beastkin; Vow of
    Kinship; Vow of Obedience; Vow of Peace; Vow of Poverty; Vow of
    Wandering; Vow of Wisdom; Walled Garden; War Tortoise; Ward of
    Silence; Warning Signals; Watchdog; Way Station; Wayside Inn;
    Welcoming Party; Whispering Leaves; Wild Allies; Wild Cry; Wild
    Mounts; Witch's Bargain; Wizard School; Wizard's Conclave;
    Wolves; Wrestlers; Zealots
    """)

  /** Every relic. */
  val relics: Vector[String] = names("""
    Amber Flame; Ancient Writ; Bag of Siegeworks; Bandit Crown;
    Bandit Standard; Barbed Net; Black Sword; Bone Dice; Book of
    Records; Brass Army; Brass Horse; Circlet of Command; Clay
    Rattle; Cracked Horn; Crystal Vial; Cup of Plenty; Cursed
    Cauldron; Demon Tail; Dowsing Sticks; Dragonskin Drum; Fearsome
    Shield; Grand Mask; Horned Mask; Imperial Seal; Ivory Eye; Lost
    Tapestry; Magic Carpet; Magic Waterskin; Obsidian Cage; Oracular
    Pig; Painted Trumpet; Ring of Devotion; Secret Testament;
    Shifting Map; Sigil of the Eye; Sigil of the Heart; Silver
    Charm; Singing Mask; Skeleton Key; Spiteful Mirror; Sticky Fire;
    The Grand Scepter; Truthful Harp; Weeping Banner; Whispering
    Stone; Whistle; Wine of Welcome; Yew Staff
    """)

  /** Both faces of every edifice. */
  val edificeFaces: Vector[String] = names("""
    Amber Doors; Bandit Forest; Bandit Market; Bandit Patrols;
    Bandit Tribunal; Blasted Pastures; Boiling Lake; Broken Clock;
    Broken Forge; Broken Horn; Cracked Rampart; Desecrated Ground;
    Drowned Library; Empty Grounds; Fallen Spire; Festival District;
    Forbidden Forest; Forest Horn; Giant's Path; Giant's Rampage;
    Grand Canal; Great Clock; Great Forge; Great Market; Great
    Spire; Hall of Bandits; Hall of Debate; Hall of Ministers; Hall
    of Mockery; Hallowed Spring; Hidden Passages; Hiding Place; Lost
    Doors; Lost Guardians; Lost Lagoon; Marble Fountains; Murky
    Fountain; Oaken Fortress; Proving Grounds; Relic Patrols;
    Rotting Fortress; Ruined Baths; Ruined School; Ruined Temple;
    Sacred Ground; School of Vines; Shattered Portal; Sinking
    Passages; Spiral Castle; Spiral Dungeons; Squalid District;
    Still Boiling Lake; Stone Baths; Stone Portal; Temple of
    Ancestors; The Tribunal; Tome Guardians; Towering Rampart;
    Underground Library; Wild Pastures
    """)

  /** Every legacy. */
  val legacies: Vector[String] = names("""
    Ancestral Lands; Arbiter; Beloved; Chronicler; Circle of Swords;
    Conspirator; Gatekeeper; Golden Key; Great Architect; Heirloom;
    High Priest; Iron Hand; Keeper of Order; Light Fingers;
    Matchmaker; Oaths of Loyalty; Pathfinder; Peacemaker; Populist;
    Quest to Distant Lands; Reformer; Revolutionary; Rival; Royal
    Line; Ruthless; Scepter Bearer; Secret Society; Seer; Steadfast;
    The Bright Beacon; The Ear; The Mouth; The Needle; The Standard
    Bearer; Wealthy; World Crafter
    """)

  /** The four Visions and the Conspiracy, which share the Vision back. */
  val visions: Vector[String] = names(
    "Vision of Conquest; Vision of Faith; Vision of Rebellion; " +
      "Vision of Sanctuary; Conspiracy")

  /** Every card with a "(NF)" file. */
  private val reprinted: Vector[String] =
    reprintedDenizens ++ relics ++ edificeFaces ++ legacies ++ visions

  /** Each card's file: the New Foundations file when the set has one,
    * else the base game's (implementation ruling R1). */
  private val facePaths: Vector[(String, String)] =
    reprinted.map(name => name -> s"cards/$name (NF).png") ++
      baseDenizens.map(name => name -> s"cards/$name.png")

  private val faces: Map[String, ArtKey] =
    facePaths.map((name, path) => name -> ArtKey(path)).toMap

  /** The back of each kind of card a viewer may be unable to identify, by
    * the projection's card kind. */
  private val backs: Map[String, ArtKey] = Map(
    "denizen" -> ArtKey("tts/backs/denizen-back.jpg"),
    "vision" -> ArtKey("tts/backs/vision-back.jpg"),
    "relic" -> ArtKey("tts/backs/relic-back.jpg"),
    "legacy" -> ArtKey("tts/backs/legacy-back.jpg"))

  /** The face of the card printed `name`, when the folder may hold one. */
  def cardFace(name: String): Option[ArtKey] = faces.get(name)

  /** The back of a card of `cardKind`, when the folder may hold one. */
  def cardBack(cardKind: String): Option[ArtKey] = backs.get(cardKind)

  /** Every path the client can ask for, each once: the server's allowlist
    * and the list in `docs/operations/game-art.md`. */
  val allPaths: Vector[String] =
    facePaths.map(_._2) ++ Vector("denizen", "vision", "relic", "legacy")
      .map(kind => backs(kind).path)
```

- [ ] **Step 4: Run the tests to verify they pass**

Run: `./sbtw "root/testOnly distantlands.catalog.ArtCatalogSuite"`
Expected: PASS, every test.

Run: `python3 scripts/check-architecture.py`
Expected: no error names `ArtCatalog.scala`.

- [ ] **Step 5: Commit**

```bash
git add shared/src/main/scala/distantlands/protocol/art/ArtCatalog.scala \
  src/test/scala/distantlands/catalog/ArtCatalogSuite.scala
git commit -m "feat(protocol): add the art catalog of card faces and backs

Co-Authored-By: Claude Fable 5.2 <noreply@anthropic.com>"
git log -3
```

---

### Task 3: `docs/operations/game-art.md`

**Files:**
- Create: `docs/operations/game-art.md`
- Test: `src/test/scala/distantlands/catalog/ArtCatalogSuite.scala` (append)

**Interfaces:**
- Consumes: `ArtCatalog.allPaths`.
- Produces: the page slice 3 links from the quick start and
  `configuration.md`. Its "Paths" section holds one bullet per path, the
  path in backticks, in `allPaths` order.

- [ ] **Step 1: Write the failing test**

Append to `ArtCatalogSuite`, with `import java.nio.file.{Files, Path}` at
the top of the file:

```scala
  /** The page a host assembles the folder from must list what the server
    * serves, no more and no less. */
  test("game-art.md lists exactly the catalog's paths, in order"):
    val page = Files.readString(Path.of("docs", "operations", "game-art.md"))
    val listed = page.linesIterator.dropWhile(_ != "## Paths")
      .collect { case s"- `$path`" => path }.toVector
    assertEquals(listed, ArtCatalog.allPaths)
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `./sbtw "root/testOnly distantlands.catalog.ArtCatalogSuite"`
Expected: the new test FAILS with `java.nio.file.NoSuchFileException:
docs/operations/game-art.md`.

- [ ] **Step 3: Write the page**

Create `docs/operations/game-art.md` with this text, then the bullets:

````markdown
# Game art

Distant Lands draws every card, the map and every piece itself. A host who
owns the licensed art can have the table show it instead: start the server
with `--art-path <dir>` (or `DLQ_ART_PATH`), and each player can switch
**Game art** on or off in the table's Settings.

The art is licensed. Distant Lands ships none of it, and this page holds
file names only. Wherever a file is missing, the table keeps the drawn
face for that one card.

## The folder

```
<art-path>/
  cards/    card faces
  tts/
    backs/  card backs
```

- `cards/` is a copy of the Buried Giant card database's Oath images
  (`bgs-cards`, `content/card-images/oath/en-US`), the whole directory.
  A card New Foundations reprints is read from its file ending
  ` (NF).png`; a denizen it left alone is read from the base file that
  carries the bare name. Files the list below does not name are ignored.
- `tts/backs/` holds the card backs of the Tabletop Simulator mod
  "Oath: New Foundations": the denizen, Vision, relic and legacy backs,
  saved under the names below.

The server reads the folder once, when it starts, and logs how many files
it found and which listed paths the folder lacks. Restart it after
changing the folder.

## Paths

Every path the table can ask for, relative to the folder.
````

Then print the bullets from the catalog and append them. This script reads
the names out of `ArtCatalog.scala`, so the page cannot drift from it by a
typing slip; the test is the real check.

```bash
python3 - <<'PY' >> docs/operations/game-art.md
import re
source = open("shared/src/main/scala/distantlands/protocol/art/ArtCatalog.scala").read()
def listed(name):
    block = re.search(r'val %s: Vector\[String\] = names\((.*?)\)\n' % name,
                      source, re.S).group(1)
    text = "".join(re.findall(r'"+(.*?)"+', block, re.S))
    return [" ".join(n.split()) for n in text.split(";") if n.strip()]
print()
for group in ("reprintedDenizens", "relics", "edificeFaces", "legacies", "visions"):
    for name in listed(group):
        print(f"- `cards/{name} (NF).png`")
for name in listed("baseDenizens"):
    print(f"- `cards/{name}.png`")
for kind in ("denizen", "vision", "relic", "legacy"):
    print(f"- `tts/backs/{kind}-back.jpg`")
PY
```

Read the page's tail to check the bullets are there and well formed.

- [ ] **Step 4: Run the test and the link check**

Run: `./sbtw "root/testOnly distantlands.catalog.ArtCatalogSuite"`
Expected: PASS, 8 tests.

Run: `python3 scripts/check-markdown-links.py`
Expected: no error.

- [ ] **Step 5: Commit**

```bash
git add docs/operations/game-art.md \
  src/test/scala/distantlands/catalog/ArtCatalogSuite.scala
git commit -m "docs(operations): list the art folder's paths

Co-Authored-By: Claude Fable 5.2 <noreply@anthropic.com>"
git log -3
```

---

### Task 4: The scan and the `/art` routes

**Files:**
- Create: `src/main/scala/distantlands/server/ArtLibrary.scala`
- Create: `src/main/scala/distantlands/server/ArtRoutes.scala`
- Modify: `src/main/scala/distantlands/server/ServerRoutes.scala`
- Modify: `src/main/scala/distantlands/server/DistantLandsServer.scala`
- Test: `src/test/scala/distantlands/server/ArtLibrarySuite.scala` (new)
- Test: `src/test/scala/distantlands/server/ArtRoutesSuite.scala` (new)
- Test: `src/test/scala/distantlands/server/ServerRoutesSuite.scala` (append)

**Interfaces:**
- Consumes: `ArtCatalog.allPaths`; `ServerConfig.artPath`.
- Produces:
  - `ArtLibrary.scan(folder: Option[Path], catalog: Vector[String] =
    ArtCatalog.allPaths): ArtLibrary`
  - `ArtLibrary#files: Map[String, Path]`, `#summary: Option[String]`,
    `#warning: Option[String]`; `ArtLibrary.empty`
  - `ArtRoutes.route(art: ArtLibrary): Route`
  - `ServerRoutes.route(runtime, blockingExecutionContext, config,
    readiness, art: ArtLibrary = ArtLibrary.empty): Route`

- [ ] **Step 1: Write the failing scan tests**

Create `src/test/scala/distantlands/server/ArtLibrarySuite.scala`:

```scala
package distantlands.server

import java.nio.file.{Files, Path}
import java.util.Base64

/** The startup scan of the host's art folder (game art design, "Serving
  * the art"). Every folder here is a temporary one of 1×1 PNGs; no test
  * reads a licensed file. */
class ArtLibrarySuite extends munit.FunSuite:
  private val catalog = Vector("cards/Alchemist (NF).png",
    "cards/Sticky Fire (NF).png", "tts/backs/relic-back.jpg")

  test("without a folder the library is empty and logs nothing"):
    val art = ArtLibrary.scan(None, catalog)
    assertEquals(art.files, Map.empty[String, Path])
    assertEquals(art.summary, None)
    assertEquals(art.warning, None)

  test("the library keeps exactly the catalog paths the folder holds"):
    val folder = ArtFolders.onePixelFolder("cards/Alchemist (NF).png",
      "tts/backs/relic-back.jpg", "cards/Not A Card (NF).png",
      "cards/notes.txt")
    val art = ArtLibrary.scan(Some(folder), catalog)
    assertEquals(art.files.keySet,
      Set("cards/Alchemist (NF).png", "tts/backs/relic-back.jpg"))
    assertEquals(art.files("cards/Alchemist (NF).png"),
      folder.resolve("cards/Alchemist (NF).png"))

  test("a catalog path that is a directory in the folder is not kept"):
    val folder = ArtFolders.onePixelFolder()
    Files.createDirectories(folder.resolve("cards/Alchemist (NF).png"))
    assertEquals(ArtLibrary.scan(Some(folder), catalog).files,
      Map.empty[String, Path])

  test("the summary counts the files and names the folder"):
    val folder = ArtFolders.onePixelFolder("cards/Alchemist (NF).png")
    assertEquals(ArtLibrary.scan(Some(folder), catalog).summary,
      Some(s"art: 1 files from $folder"))

  test("the warning names each catalog path the folder lacks"):
    val folder = ArtFolders.onePixelFolder("cards/Alchemist (NF).png")
    assertEquals(ArtLibrary.scan(Some(folder), catalog).warning, Some(
      s"art: $folder lacks 2 catalog files: cards/Sticky Fire (NF).png, " +
        "tts/backs/relic-back.jpg"))

  test("a complete folder gives no warning"):
    val folder = ArtFolders.onePixelFolder(catalog*)
    assertEquals(ArtLibrary.scan(Some(folder), catalog).warning, None)

  test("a path that is not a folder serves nothing and says so"):
    val missing = Files.createTempDirectory("art-missing-").resolve("none")
    val art = ArtLibrary.scan(Some(missing), catalog)
    assertEquals(art.files, Map.empty[String, Path])
    assertEquals(art.summary, Some(s"art: 0 files from $missing"))
    assertEquals(art.warning,
      Some(s"art: $missing is not a folder; no game art is served"))

/** Temporary art folders for the server suites. */
object ArtFolders:
  /** The bytes of a 1×1 transparent PNG. */
  val onePixel: Array[Byte] = Base64.getDecoder.decode(
    "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNkYPhfDwAC" +
      "hwGA60e6kgAAAABJRU5ErkJggg==")

  /** A new temporary folder holding a 1×1 PNG at each of `paths`,
    * whatever the path's extension says. */
  def onePixelFolder(paths: String*): Path =
    val folder = Files.createTempDirectory("art-folder-")
    paths.foreach { path =>
      val file = folder.resolve(path)
      Files.createDirectories(file.getParent)
      Files.write(file, onePixel)
    }
    folder
```

- [ ] **Step 2: Run the scan tests to verify they fail**

Run: `./sbtw "root/testOnly distantlands.server.ArtLibrarySuite"`
Expected: compilation fails with `Not found: ArtLibrary`.

- [ ] **Step 3: Write `ArtLibrary`**

Create `src/main/scala/distantlands/server/ArtLibrary.scala`:

```scala
package distantlands.server

import java.nio.file.{Files, Path}

import distantlands.protocol.art.ArtCatalog

/** The host's art folder as the server serves it: the catalog paths the
  * folder held when the server started (game art design, "Serving the
  * art"). The folder is read once; a rescan needs a restart. A request is
  * answered from `files` alone, so no path a client sends reaches the
  * disk unless the catalog lists it.
  */
final class ArtLibrary private (
    folder: Option[Path],
    /** Each kept catalog path and the file it names. */
    val files: Map[String, Path],
    missing: Vector[String],
    isFolder: Boolean
):
  /** The startup line, when the host named a folder. */
  def summary: Option[String] =
    folder.map(dir => s"art: ${files.size} files from $dir")

  /** What the host should know is wrong with the folder, if anything: it
    * is not a folder, or it lacks catalog files, each named. */
  def warning: Option[String] = folder.flatMap { dir =>
    if !isFolder then
      Some(s"art: $dir is not a folder; no game art is served")
    else Option.when(missing.nonEmpty)(
      s"art: $dir lacks ${missing.size} catalog files: " +
        missing.mkString(", "))
  }

object ArtLibrary:
  private val Extensions = Set("png", "jpg", "webp")

  /** A server with no art folder. */
  val empty: ArtLibrary = new ArtLibrary(None, Map.empty, Vector.empty, true)

  /** Reads `folder` against `catalog`: a path is kept when its extension
    * is an image's and the folder holds a regular file there. */
  def scan(folder: Option[Path],
      catalog: Vector[String] = ArtCatalog.allPaths): ArtLibrary =
    folder.fold(empty) { dir =>
      if !Files.isDirectory(dir) then
        new ArtLibrary(Some(dir), Map.empty, Vector.empty, false)
      else
        val (kept, missing) = catalog.partition(path =>
          Extensions(path.drop(path.lastIndexOf('.') + 1)) &&
            Files.isRegularFile(dir.resolve(path)))
        new ArtLibrary(Some(dir),
          kept.map(path => path -> dir.resolve(path)).toMap, missing, true)
    }
```

- [ ] **Step 4: Run the scan tests to verify they pass**

Run: `./sbtw "root/testOnly distantlands.server.ArtLibrarySuite"`
Expected: PASS, 7 tests.

- [ ] **Step 5: Write the failing route tests**

Create `src/test/scala/distantlands/server/ArtRoutesSuite.scala`:

```scala
package distantlands.server

import distantlands.testkit.HttpTestServer
import distantlands.testkit.HttpTestServer.cacheControl

/** `/art/manifest.json` and `/art/<path>` (game art design, "Serving the
  * art"), over temporary folders of 1×1 PNGs. */
class ArtRoutesSuite extends munit.FunSuite:
  private val catalog = Vector("cards/Alchemist (NF).png",
    "cards/Giant's Path (NF).png", "tts/backs/relic-back.jpg")

  private def serving(art: ArtLibrary)(check: HttpTestServer => Unit): Unit =
    val server = HttpTestServer.start("art-routes-test")(_ =>
      ArtRoutes.route(art))
    try check(server)
    finally server.close()

  private def files(server: HttpTestServer): Vector[String] =
    val manifest = server.get("/art/manifest.json")
    assertEquals(manifest.statusCode(), 200)
    ujson.read(manifest.body())("files").arr.map(_.str).toVector

  test("without a folder the manifest is served and lists nothing"):
    serving(ArtLibrary.empty) { server =>
      assertEquals(files(server), Vector.empty)
      assertEquals(server.get("/art/cards/Alchemist%20(NF).png").statusCode(),
        404)
    }

  test("the manifest lists exactly the files present and in the catalog, " +
      "and is revalidated"):
    val folder = ArtFolders.onePixelFolder("cards/Alchemist (NF).png",
      "cards/Giant's Path (NF).png", "cards/Not A Card (NF).png")
    serving(ArtLibrary.scan(Some(folder), catalog)) { server =>
      assertEquals(files(server), Vector("cards/Alchemist (NF).png",
        "cards/Giant's Path (NF).png"))
      assertEquals(cacheControl(server.get("/art/manifest.json")),
        Some("no-cache"))
    }

  test("a listed path is served with a day's cache, an ETag and its type"):
    val folder = ArtFolders.onePixelFolder("cards/Alchemist (NF).png",
      "cards/Giant's Path (NF).png", "tts/backs/relic-back.jpg")
    serving(ArtLibrary.scan(Some(folder), catalog)) { server =>
      val card = server.getBytes("/art/cards/Alchemist%20(NF).png")
      assertEquals(card.statusCode(), 200)
      assertEquals(card.body().toVector, ArtFolders.onePixel.toVector)
      assertEquals(card.headers().firstValue("Cache-Control").get(),
        "public, max-age=86400")
      assert(card.headers().firstValue("ETag").isPresent)
      assertEquals(card.headers().firstValue("Content-Type").get(),
        "image/png")
      assertEquals(server.get("/art/cards/Giant's%20Path%20(NF).png")
        .statusCode(), 200)
      assertEquals(server.getBytes("/art/tts/backs/relic-back.jpg")
        .headers().firstValue("Content-Type").get(), "image/jpeg")
    }

  test("an unlisted path, a step out of the folder and a wrong extension " +
      "are not found"):
    val folder = ArtFolders.onePixelFolder("cards/Alchemist (NF).png",
      "cards/Not A Card (NF).png", "cards/Alchemist (NF).gif")
    java.nio.file.Files.write(folder.resolveSibling("outside.png"),
      ArtFolders.onePixel)
    serving(ArtLibrary.scan(Some(folder), catalog)) { server =>
      Vector("/art/cards/Not%20A%20Card%20(NF).png",
        "/art/cards/Alchemist%20(NF).gif",
        "/art/../outside.png", "/art/cards/../../outside.png",
        "/art/cards/%2e%2e/%2e%2e/outside.png", "/art/", "/art",
        "/art/cards").foreach(path =>
        assertEquals(server.get(path).statusCode(), 404, path))
    }
```

- [ ] **Step 6: Run the route tests to verify they fail**

Run: `./sbtw "root/testOnly distantlands.server.ArtRoutesSuite"`
Expected: compilation fails with `Not found: ArtRoutes`.

- [ ] **Step 7: Write `ArtRoutes`**

Create `src/main/scala/distantlands/server/ArtRoutes.scala`:

```scala
package distantlands.server

import akka.http.scaladsl.model.{ContentTypes, HttpEntity, StatusCodes}
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route

/** The host's art (game art design, "Serving the art"): the manifest,
  * which every client fetches once, and the files it lists. */
object ArtRoutes:
  // A table loads hundreds of images; revalidating each on every page
  // load costs more than a day's staleness after a host replaces a file.
  private val ArtCacheControl =
    RawHeader("Cache-Control", "public, max-age=86400")
  // The manifest is small and changes when the host restarts with another
  // folder, so each page load asks again.
  private val ManifestCacheControl = RawHeader("Cache-Control", "no-cache")

  def route(art: ArtLibrary): Route =
    val manifest = ujson.write(ujson.Obj("files" ->
      ujson.Arr.from(art.files.keys.toVector.sorted.map(ujson.Str(_)))))
    pathPrefix("art"):
      get:
        path("manifest.json") {
          respondWithHeader(ManifestCacheControl):
            complete(HttpEntity(ContentTypes.`application/json`, manifest))
        } ~
          // The request's segments, decoded and joined, are looked up in
          // the scan. Anything the scan did not keep is not found, without
          // a look at the disk: a `..` segment names no catalog path.
          path(Segments) { segments =>
            art.files.get(segments.mkString("/")) match
              case Some(file) =>
                respondWithHeader(ArtCacheControl):
                  getFromFile(file.toFile)
              case None => complete(StatusCodes.NotFound)
          } ~
          complete(StatusCodes.NotFound)
```

`getFromFile` answers with an ETag and Last-Modified and picks the content
type from the extension. If the 404 cases for `/art` and `/art/` do not
pass because `pathPrefix("art")` leaves them unmatched, they fall to the
server's own 404, which the test also accepts; keep the final
`complete(StatusCodes.NotFound)` for paths under `/art/`.

- [ ] **Step 8: Run the route tests to verify they pass**

Run: `./sbtw "root/testOnly distantlands.server.ArtRoutesSuite"`
Expected: PASS, 4 tests.

- [ ] **Step 9: Write the failing wiring test**

Append to `ServerRoutesSuite`, inside the class:

```scala
  test("both modes serve the art manifest and the art the folder holds"):
    val folder = ArtFolders.onePixelFolder("tts/backs/relic-back.jpg")
    val art = ArtLibrary.scan(Some(folder))
    Vector(ServerMode.Development, ServerMode.TrustedAlpha).foreach { mode =>
      val runtime = openRuntime("art-routes-")
      try
        val server = HttpTestServer.start("server-art-routes-test")(blocking =>
          ServerRoutes.route(runtime, blocking, config(mode),
            ServerReadiness.starting("test-version"), art))
        try
          assertEquals(ujson.read(server.get("/art/manifest.json").body()),
            ujson.Obj("files" -> ujson.Arr("tts/backs/relic-back.jpg")),
            mode.toString)
          assertEquals(server.get("/art/tts/backs/relic-back.jpg")
            .statusCode(), 200, mode.toString)
        finally server.close()
      finally runtime.close()
    }

  test("a server with no art folder still serves an empty manifest"):
    val runtime = openRuntime("no-art-routes-")
    try
      val server = HttpTestServer.start("server-no-art-routes-test")(blocking =>
        ServerRoutes.route(runtime, blocking,
          config(ServerMode.TrustedAlpha),
          ServerReadiness.starting("test-version")))
      try assertEquals(ujson.read(server.get("/art/manifest.json").body()),
        ujson.Obj("files" -> ujson.Arr()))
      finally server.close()
    finally runtime.close()
```

Run: `./sbtw "root/testOnly distantlands.server.ServerRoutesSuite"`
Expected: compilation fails, `too many arguments for method route`.

- [ ] **Step 10: Wire the routes and the startup log**

In `ServerRoutes.scala`, add the parameter and serve the routes in every
mode:

```scala
  def route(
      runtime: ServerRuntime,
      blockingExecutionContext: ExecutionContext,
      config: ServerConfig,
      readiness: ServerReadiness,
      art: ArtLibrary = ArtLibrary.empty
  ): Route =
```

and change the last line to:

```scala
    HealthRoutes.route(readiness) ~ ArtRoutes.route(art) ~ application
```

In `DistantLandsServer.scala`, in the `case Right(runtime) =>` branch,
before `val route`:

```scala
        val art = ArtLibrary.scan(config.artPath)
        art.summary.foreach(line => system.log.info(line))
        art.warning.foreach(line => system.log.warn(line))
```

and pass `art` as the last argument of `ServerRoutes.route(...)`.

- [ ] **Step 11: Run the server suites**

Run: `./sbtw "root/testOnly distantlands.server.*"`
Expected: PASS, every suite.

- [ ] **Step 12: Commit**

```bash
git add src/main/scala/distantlands/server/ArtLibrary.scala \
  src/main/scala/distantlands/server/ArtRoutes.scala \
  src/main/scala/distantlands/server/ServerRoutes.scala \
  src/main/scala/distantlands/server/DistantLandsServer.scala \
  src/test/scala/distantlands/server/ArtLibrarySuite.scala \
  src/test/scala/distantlands/server/ArtRoutesSuite.scala \
  src/test/scala/distantlands/server/ServerRoutesSuite.scala
git commit -m "feat(server): scan the art folder and serve it under /art

Co-Authored-By: Claude Fable 5.2 <noreply@anthropic.com>"
git log -3
```

---

### Task 5: `GameArt`

**Files:**
- Create: `frontend/src/main/scala/distantlands/frontend/GameArt.scala`
- Modify: `frontend/src/main/scala/distantlands/frontend/Main.scala`
- Modify: `scripts/check-architecture.py:106-109` (the storage-key
  exception)
- Test: `frontend/src/test/scala/distantlands/frontend/GameArtSuite.scala`
  (new)
- Test helper: `frontend/src/test/scala/distantlands/frontend/TestArt.scala`
  (new)

**Interfaces:**
- Consumes: `ArtKey`, `ArtCatalog.allPaths`; `JsonTransport`,
  `TransportResponse`; `ChronicleStorage`, `MemoryChronicleStorage`.
- Produces, on `private[frontend] object GameArt`:
  - `sealed trait Manifest` with `Manifest.Pending`, `Manifest.Failed`,
    `Manifest.Listed(files: Set[String])`
  - `val PreferenceKey = "distantlands.art"`, `val ManifestUrl =
    "/art/manifest.json"`
  - `def manifest: Manifest`
  - `def start(transport: JsonTransport, browser: Option[ChronicleStorage]): Future[Unit]`
  - `def install(manifest: Manifest, store: ChronicleStorage): Unit`
  - `def onChange(listener: () => Unit): Unit`
  - `def enabled: Boolean`, `def setEnabled(on: Boolean): Unit`
  - `def url(key: ArtKey): Option[String]`
  - `def markLoaded(url: String): Unit`, `def markFailed(url: String): Unit`,
    `def hasLoaded(url: String): Boolean`, `def hasFailed(url: String): Boolean`
  - `def reset(): Unit`
- Produces, on `private[frontend] object TestArt` (tests only):
  `listing(paths: String*): MemoryChronicleStorage`,
  `listingWholeCatalog(): MemoryChronicleStorage`.

`GameArt` is one object, as `CardInspection` is: cards reach
`CardFace.render` through renderers that carry no session, so the
renderers ask a slot. Until `start` or `install` runs, the manifest is
`Pending` and `url` answers `None`, so every existing suite sees the drawn
table.

- [ ] **Step 1: Write the failing tests**

Create `frontend/src/test/scala/distantlands/frontend/TestArt.scala`:

```scala
package distantlands.frontend

import distantlands.protocol.art.ArtCatalog

/** Game art states for the render suites. Each returns the store the
  * preference is kept in. A suite that uses one calls `GameArt.reset()`
  * after each test. */
private[frontend] object TestArt:
  /** Game art on, with a manifest listing exactly `paths`. */
  def listing(paths: String*): MemoryChronicleStorage =
    val store = new MemoryChronicleStorage()
    GameArt.install(GameArt.Manifest.Listed(paths.toSet), store)
    store

  /** Game art on, with a manifest listing every catalog path. */
  def listingWholeCatalog(): MemoryChronicleStorage =
    listing(ArtCatalog.allPaths*)
```

Create `frontend/src/test/scala/distantlands/frontend/GameArtSuite.scala`:

```scala
package distantlands.frontend

import distantlands.protocol.art.ArtCatalog
import scala.concurrent.Future
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

/** `GameArt` joins the server's manifest with the player's preference
  * (game art design, "The client"). */
class GameArtSuite extends munit.FunSuite:
  override def afterEach(context: AfterEach): Unit = GameArt.reset()

  private val alchemist = ArtCatalog.cardFace("Alchemist").get
  private val relicBack = ArtCatalog.cardBack("relic").get

  /** A transport that answers the manifest request with `answer` and
    * records every request. */
  private def answering(answer: Either[GameClientFailure, TransportResponse])
      : ScriptedTransport =
    new ScriptedTransport((_, _, _) => Future.successful(answer))

  test("before the manifest arrives nothing has art"):
    assertEquals(GameArt.manifest, GameArt.Manifest.Pending)
    assertEquals(GameArt.url(alchemist), None)

  test("a listed key's URL is its path under /art, each segment encoded"):
    TestArt.listing(alchemist.path, relicBack.path)
    assertEquals(GameArt.url(alchemist),
      Some("/art/cards/Alchemist%20(NF).png"))
    assertEquals(GameArt.url(relicBack), Some("/art/tts/backs/relic-back.jpg"))

  test("a key the manifest lacks has no URL"):
    TestArt.listing(relicBack.path)
    assertEquals(GameArt.url(alchemist), None)

  test("with the preference off nothing has a URL"):
    TestArt.listing(alchemist.path)
    GameArt.setEnabled(false)
    assertEquals(GameArt.url(alchemist), None)
    GameArt.setEnabled(true)
    assert(GameArt.url(alchemist).nonEmpty)

  test("the preference is on by default and is saved under " +
      "distantlands.art"):
    val store = TestArt.listing(alchemist.path)
    assert(GameArt.enabled)
    GameArt.setEnabled(false)
    assertEquals(store.values.get("distantlands.art"), Some("off"))
    GameArt.setEnabled(true)
    assertEquals(store.values.get("distantlands.art"), Some("on"))

  test("a stored off is read when the page loads"):
    GameArt.install(GameArt.Manifest.Listed(Set(alchemist.path)),
      new MemoryChronicleStorage(Map("distantlands.art" -> "off")))
    assert(!GameArt.enabled)
    assertEquals(GameArt.url(alchemist), None)

  test("start fetches the manifest once and lists its files"):
    val transport = answering(Right(TransportResponse(200,
      """{"files":["cards/Alchemist (NF).png"]}""")))
    GameArt.start(transport, Some(new MemoryChronicleStorage())).map { _ =>
      assertEquals(transport.requests.toVector,
        Vector(RecordedRequest("GET", "/art/manifest.json", None)))
      assertEquals(GameArt.manifest,
        GameArt.Manifest.Listed(Set("cards/Alchemist (NF).png")))
      assert(GameArt.url(alchemist).nonEmpty)
    }

  test("a failed fetch, a refusal and an unreadable answer each count as " +
      "a failed manifest"):
    val answers = Vector[Either[GameClientFailure, TransportResponse]](
      Left(GameClientFailure.NetworkFailure("GET /art/manifest.json failed")),
      Right(TransportResponse(404, "")),
      Right(TransportResponse(200, "<html>")),
      Right(TransportResponse(200, """{"files":"all"}""")))
    answers.foldLeft(Future.successful(())) { (done, answer) =>
      done.flatMap(_ => GameArt.start(answering(answer), None)).map { _ =>
        assertEquals(GameArt.manifest, GameArt.Manifest.Failed)
        assertEquals(GameArt.url(alchemist), None)
      }
    }

  test("a browser that refuses storage gets the default, and the switch " +
      "still works for the life of the page"):
    GameArt.start(answering(Right(TransportResponse(200,
      """{"files":["cards/Alchemist (NF).png"]}"""))), None).map { _ =>
      assert(GameArt.enabled)
      GameArt.setEnabled(false)
      assert(!GameArt.enabled)
      assertEquals(GameArt.url(alchemist), None)
    }

  test("a store that refuses the write still switches for the page"):
    val refusing = new ChronicleStorage:
      def read(key: String): Option[String] = None
      def write(key: String, value: String): Boolean = false
      def remove(key: String): Unit = ()
    GameArt.install(GameArt.Manifest.Listed(Set(alchemist.path)), refusing)
    GameArt.setEnabled(false)
    assert(!GameArt.enabled)

  test("the listener hears the manifest arrive and each change of the " +
      "preference"):
    var heard = 0
    GameArt.onChange(() => heard += 1)
    GameArt.start(answering(Right(TransportResponse(200, """{"files":[]}"""))),
      None).map { _ =>
      assertEquals(heard, 1)
      GameArt.setEnabled(false)
      assertEquals(heard, 2)
    }

  test("loaded and failed URLs are remembered until reset"):
    GameArt.markLoaded("/art/a.png")
    GameArt.markFailed("/art/b.png")
    assert(GameArt.hasLoaded("/art/a.png") && !GameArt.hasFailed("/art/a.png"))
    assert(GameArt.hasFailed("/art/b.png") && !GameArt.hasLoaded("/art/b.png"))
    GameArt.reset()
    assert(!GameArt.hasLoaded("/art/a.png") && !GameArt.hasFailed("/art/b.png"))
```

- [ ] **Step 2: Run the tests to verify they fail**

Run: `./sbtw "frontend/testOnly distantlands.frontend.GameArtSuite"`
Expected: compilation fails with `Not found: GameArt`.

- [ ] **Step 3: Write `GameArt`**

Create `frontend/src/main/scala/distantlands/frontend/GameArt.scala`:

```scala
package distantlands.frontend

import distantlands.protocol.art.ArtKey
import scala.concurrent.Future
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.scalajs.js
import scala.util.Try

/** Whether a piece of the table has licensed art to show (game art design,
  * "The client"): the server's manifest joined with the player's
  * preference. Renderers ask one question, `url`, and draw what they draw
  * today when the answer is `None`.
  *
  * One object, as `CardInspection` is: cards reach `CardFace.render`
  * through renderers that carry no session. Nothing waits for the
  * manifest; the table is drawn at once and `onChange`'s listener redraws
  * it when the manifest arrives or the preference flips.
  */
private[frontend] object GameArt:
  /** What the server said it can serve. */
  sealed trait Manifest extends Product with Serializable
  object Manifest:
    /** Not answered yet. */
    case object Pending extends Manifest
    /** The fetch failed; it is not tried again for this page load. */
    case object Failed extends Manifest
    /** The paths the server serves; empty when it has no art folder. */
    final case class Listed(files: Set[String]) extends Manifest

  /** The preference's `localStorage` key; "off" turns art off, and
    * anything else, or nothing, leaves it on. */
  val PreferenceKey = "distantlands.art"
  val ManifestUrl = "/art/manifest.json"

  private var current: Manifest = Manifest.Pending
  private var storage: ChronicleStorage = new MemoryChronicleStorage()
  /** The switch's position once flipped on this page, which holds even
    * when the browser refuses to store it. */
  private var chosen = Option.empty[Boolean]
  private var listener: () => Unit = () => ()
  private var loaded = Set.empty[String]
  private var failed = Set.empty[String]

  def manifest: Manifest = current

  /** Reads the preference from `browser`, or keeps it for the page alone
    * when the browser stores nothing, and fetches the manifest once. */
  def start(transport: JsonTransport, browser: Option[ChronicleStorage])
      : Future[Unit] =
    storage = browser.getOrElse(new MemoryChronicleStorage())
    chosen = None
    transport.request("GET", ManifestUrl, None).map { answer =>
      current = answer.toOption
        .filter(response => response.status >= 200 && response.status < 300)
        .flatMap(response => Try(ujson.read(response.body)("files").arr
          .map(_.str).toSet).toOption)
        .fold[Manifest](Manifest.Failed)(Manifest.Listed(_))
      listener()
    }

  /** Puts a manifest and a store in place without a fetch. */
  def install(manifest: Manifest, store: ChronicleStorage): Unit =
    current = manifest
    storage = store
    chosen = None
    listener()

  /** `listener` runs when the manifest arrives and when the preference
    * changes: the table redraws. */
  def onChange(listener: () => Unit): Unit = this.listener = listener

  def enabled: Boolean =
    chosen.getOrElse(!storage.read(PreferenceKey).contains("off"))

  def setEnabled(on: Boolean): Unit =
    chosen = Some(on)
    storage.write(PreferenceKey, if on then "on" else "off")
    listener()

  /** Where `key`'s image is served, when the preference is on and the
    * manifest lists its path. */
  def url(key: ArtKey): Option[String] = current match
    case Manifest.Listed(files) if enabled && files(key.path) =>
      Some("/art/" + key.path.split('/')
        .map(segment => js.URIUtils.encodeURIComponent(segment))
        .mkString("/"))
    case _ => None

  /** The browser holds `url`'s image: a fresh box may show it at once. */
  def markLoaded(url: String): Unit = loaded += url
  def hasLoaded(url: String): Boolean = loaded(url)
  /** `url` failed: it is not asked for again until the page reloads. */
  def markFailed(url: String): Unit = failed += url
  def hasFailed(url: String): Boolean = failed(url)

  /** Back to a page that has asked for nothing, for the suites. */
  def reset(): Unit =
    current = Manifest.Pending
    storage = new MemoryChronicleStorage()
    chosen = None
    listener = () => ()
    loaded = Set.empty
    failed = Set.empty
```

- [ ] **Step 4: Tell the architecture check the key is not a package**

`"distantlands.art"` in a string would read as a reference to a package
named `art`. In `scripts/check-architecture.py`, change the comment and the
pattern above `brace_group_import`:

```python
# `distantlands.art`, `distantlands.chronicles`, `distantlands.log` and
# `distantlands.table` are browser storage keys, not packages.
qualified_reference = re.compile(
    r"\bdistantlands\.(?!(?:art|chronicles|log|table)\b)([A-Za-z0-9_]+)(?:\.([A-Za-z0-9_]+))?")
```

- [ ] **Step 5: Start `GameArt` when the page loads**

In `Main.scala`, `main` builds one transport and starts `GameArt` with it:

```scala
  def main(args: Array[String]): Unit =
    val mount = Option(dom.document.getElementById("app")).getOrElse:
      throw new IllegalStateException("missing #app mount point")
    HoverTip.install()
    val transport = new SameOriginJsonTransport
    // The manifest is fetched once per page load and nothing waits for
    // it: the table draws its own faces until it arrives.
    GameArt.start(transport, ChronicleStorage.browser)
    start(mount, dom.window.location.pathname,
      mount.getAttribute("data-runtime-mode") == "trusted-alpha",
      transport, search = dom.window.location.search)
```

`Main.start`, the tested seam, does not fetch: the suites' scripted
transports see no new request.

- [ ] **Step 6: Run the tests to verify they pass**

Run: `./sbtw "frontend/testOnly distantlands.frontend.GameArtSuite"`
Expected: PASS, 12 tests.

Run: `python3 scripts/check-architecture.py`
Expected: no error.

- [ ] **Step 7: Commit**

```bash
git add frontend/src/main/scala/distantlands/frontend/GameArt.scala \
  frontend/src/main/scala/distantlands/frontend/Main.scala \
  frontend/src/test/scala/distantlands/frontend/GameArtSuite.scala \
  frontend/src/test/scala/distantlands/frontend/TestArt.scala \
  scripts/check-architecture.py
git commit -m "feat(frontend): join the art manifest with the player's preference

Co-Authored-By: Claude Fable 5.2 <noreply@anthropic.com>"
git log -3
```

---

### Task 6: `ArtLayer` and its stylesheet rules

**Files:**
- Create: `frontend/src/main/scala/distantlands/frontend/ArtLayer.scala`
- Modify: `frontend/styles.css` (after the `.card-slot-empty` rule, near
  line 1376)
- Test: `frontend/src/test/scala/distantlands/frontend/ArtLayerSuite.scala`
  (new)
- Test: `src/test/scala/distantlands/server/StylesheetArtSuite.scala` (new)

**Interfaces:**
- Consumes: `GameArt.url`, `markLoaded`, `markFailed`, `hasLoaded`,
  `hasFailed`; `ArtKey`.
- Produces: `ArtLayer.attach(box: dom.Element, key: Option[ArtKey]): Unit`.
  It takes an `Option` because the catalog's lookups return one; `None`
  does nothing.
- Produces, in CSS: `.art`; the class `art-ready` on a box whose image has
  loaded; the custom property `--printed-opacity`, which `.printed` reads
  and each box sets (R3).

**How the layer is stacked.** A box that holds an image isolates its
stacking (`:has(> .art)`), and the image takes `z-index: -1`: above the
box's background, below everything else in the box. Live state on the box
(tokens, badges, a Dormant tag) is therefore over the image without a
rule of its own, whether it is positioned or in the flow. No class is
added to the box when the image is attached, so a box with art and a box
without differ only by the `img.art` element until the image loads.

**How printed content is hidden (R3).** `.printed` reads
`--printed-opacity`. Each box kind sets it to 1, and a ready box sets it
to 0. A custom property inherits, so printed content follows its nearest
box: a ready box never hides the drawn content of a box inside it whose
own image is missing.

- [ ] **Step 1: Write the failing `ArtLayer` tests**

Create `frontend/src/test/scala/distantlands/frontend/ArtLayerSuite.scala`:

```scala
package distantlands.frontend

import Dom.{all, one}
import distantlands.protocol.art.ArtCatalog
import org.scalajs.dom

/** `ArtLayer.attach` is the one way a surface swaps in art (game art
  * design, "`ArtLayer`"). jsdom loads no image, so the suite dispatches
  * the image's `load` and `error` itself. */
class ArtLayerSuite extends munit.FunSuite:
  override def afterEach(context: AfterEach): Unit = GameArt.reset()

  private val alchemist = ArtCatalog.cardFace("Alchemist")
  private val alchemistUrl = "/art/cards/Alchemist%20(NF).png"

  /** A box holding one printed child, as a drawn face does. */
  private def drawnBox(): dom.Element =
    val box = dom.document.createElement("span")
    val printed = dom.document.createElement("span")
    printed.setAttribute("class", "printed")
    printed.textContent = "Alchemist"
    box.appendChild(printed)
    box

  test("with no URL for the key nothing is attached"):
    val box = drawnBox()
    ArtLayer.attach(box, alchemist)
    ArtLayer.attach(box, None)
    assertEquals(all(box, "img"), Vector.empty)
    assert(!box.classList.contains("art-ready"))

  test("the image is decoration that fills the box and loads lazily"):
    TestArt.listing(alchemist.get.path)
    val box = drawnBox()
    ArtLayer.attach(box, alchemist)
    val image = one(box, "img.art")
    assertEquals(image.getAttribute("src"), alchemistUrl)
    assertEquals(image.getAttribute("alt"), "")
    assertEquals(image.getAttribute("aria-hidden"), "true")
    assertEquals(image.getAttribute("decoding"), "async")
    assertEquals(image.getAttribute("loading"), "lazy")
    assert(!box.classList.contains("art-ready"))
    assertEquals(one(box, ".printed").textContent, "Alchemist")

  test("a load marks the box ready and keeps the drawn content"):
    TestArt.listing(alchemist.get.path)
    val box = drawnBox()
    ArtLayer.attach(box, alchemist)
    one(box, "img.art").dispatchEvent(new dom.Event("load"))
    assert(box.classList.contains("art-ready"))
    assertEquals(one(box, ".printed").textContent, "Alchemist")
    assert(GameArt.hasLoaded(alchemistUrl))

  test("an error removes the image and the box stays drawn"):
    TestArt.listing(alchemist.get.path)
    val box = drawnBox()
    ArtLayer.attach(box, alchemist)
    one(box, "img.art").dispatchEvent(new dom.Event("error"))
    assertEquals(all(box, "img"), Vector.empty)
    assert(!box.classList.contains("art-ready"))
    assert(GameArt.hasFailed(alchemistUrl))

  test("a URL remembered as loaded marks a fresh box ready at once"):
    TestArt.listing(alchemist.get.path)
    GameArt.markLoaded(alchemistUrl)
    val box = drawnBox()
    ArtLayer.attach(box, alchemist)
    assert(box.classList.contains("art-ready"))
    assertEquals(one(box, "img.art").getAttribute("src"), alchemistUrl)

  test("a URL remembered as failed makes no image"):
    TestArt.listing(alchemist.get.path)
    GameArt.markFailed(alchemistUrl)
    val box = drawnBox()
    ArtLayer.attach(box, alchemist)
    assertEquals(all(box, "img"), Vector.empty)
    assert(!box.classList.contains("art-ready"))

  test("an error after a remembered load takes the ready mark back"):
    TestArt.listing(alchemist.get.path)
    GameArt.markLoaded(alchemistUrl)
    val box = drawnBox()
    ArtLayer.attach(box, alchemist)
    one(box, "img.art").dispatchEvent(new dom.Event("error"))
    assert(!box.classList.contains("art-ready"))
    assertEquals(all(box, "img"), Vector.empty)
```

- [ ] **Step 2: Run the tests to verify they fail**

Run: `./sbtw "frontend/testOnly distantlands.frontend.ArtLayerSuite"`
Expected: compilation fails with `Not found: ArtLayer`.

- [ ] **Step 3: Write `ArtLayer`**

Create `frontend/src/main/scala/distantlands/frontend/ArtLayer.scala`:

```scala
package distantlands.frontend

import distantlands.protocol.art.ArtKey
import org.scalajs.dom

/** The one way a surface swaps licensed art in for what it draws (game
  * art design, "`ArtLayer`").
  *
  * The image is laid over the box and never sizes it. Until it loads the
  * box shows its drawn content; once it has, the box is `art-ready` and
  * the stylesheet hides the box's `printed` content with opacity, which
  * keeps the content's space and keeps it in the accessibility tree. An
  * image that fails is removed, and the box stays drawn.
  */
private[frontend] object ArtLayer:
  /** Lays `key`'s image over `box`, when `GameArt` has a URL for it. */
  def attach(box: dom.Element, key: Option[ArtKey]): Unit =
    key.flatMap(GameArt.url).filterNot(GameArt.hasFailed).foreach { url =>
      val image = dom.document.createElement("img")
        .asInstanceOf[dom.html.Image]
      image.className = "art"
      image.alt = ""
      image.setAttribute("aria-hidden", "true")
      image.setAttribute("decoding", "async")
      image.setAttribute("loading", "lazy")
      // The table is rebuilt on every poll, which builds a fresh image.
      // The browser already holds one it has loaded, so the fresh box is
      // ready at once and a redraw never flashes the drawn face.
      if GameArt.hasLoaded(url) then box.classList.add("art-ready")
      image.addEventListener("load", (_: dom.Event) =>
        GameArt.markLoaded(url)
        box.classList.add("art-ready"))
      image.addEventListener("error", (_: dom.Event) =>
        GameArt.markFailed(url)
        box.classList.remove("art-ready")
        image.remove())
      image.src = url
      box.appendChild(image)
    }
```

- [ ] **Step 4: Run the tests to verify they pass**

Run: `./sbtw "frontend/testOnly distantlands.frontend.ArtLayerSuite"`
Expected: PASS, 7 tests.

- [ ] **Step 5: Write the failing stylesheet tests**

Create `src/test/scala/distantlands/server/StylesheetArtSuite.scala`:

```scala
package distantlands.server

import java.nio.file.{Files, Path}

/** The art layer's rules (game art design, "`ArtLayer`"). The suite pins
  * declarations; that an image covers its box and hides only that box's
  * printed content is covered by the browser check. */
class StylesheetArtSuite extends munit.FunSuite:
  private val css = Files.readString(Path.of("frontend", "styles.css"))

  /** The declarations of the first rule that opens a line with
    * `selector`. */
  private def rule(selector: String)(using munit.Location): String =
    val opening = ("(?m)^" + java.util.regex.Pattern.quote(selector) +
      " \\{").r.findFirstMatchIn(css)
    assert(opening.nonEmpty, s"no rule $selector")
    css.substring(opening.get.start, css.indexOf("}", opening.get.start))

  /** The stylesheet without its comments, so a rule's selector list is
    * the selectors alone. */
  private val uncommented = css.replaceAll("(?s)/\\*.*?\\*/", "")

  /** The art layer's rules: from their opening comment to the document
    * level overlay's comment, which follows them. */
  private def artRules(using munit.Location): String =
    val from = css.indexOf("/* Licensed art")
    val to = css.indexOf("/* Document level, above .game-pane", from)
    assert(from >= 0 && to > from, "the art rules were not found")
    css.substring(from, to)

  test("the image fills its box, keeps its proportions and sizes nothing"):
    val image = rule(".art")
    Vector("position: absolute", "inset: 0", "width: 100%", "height: 100%",
      "object-fit: contain", "z-index: -1", "pointer-events: none")
      .foreach(part => assert(image.contains(part), part))

  test("a box holding an image isolates its stacking, so the image lies " +
      "over the box's fill and under everything else in it"):
    assert(artRules.contains(":has(> .art) { isolation: isolate; }"))

  test("printed content is hidden with opacity, by its nearest box"):
    assert(rule(".printed").contains("opacity: var(--printed-opacity, 1)"))
    assert(artRules.contains(
      ":is(.card-face, .card-hover-summary) { --printed-opacity: 1; }"))
    assert(artRules.contains(
      ":is(.card-face, .card-hover-summary).art-ready { --printed-opacity: 0; }"))

  test("the art layer hides nothing with display or visibility, and " +
      "moves nothing"):
    Vector("display: none", "visibility: hidden", "transition", "animation")
      .foreach(part => assert(!artRules.contains(part), part))

  test("no other rule sets a printed element's opacity"):
    val rules = """([^{}]+)\{([^{}]*)\}""".r
    val setters = rules.findAllMatchIn(uncommented).filter(found =>
      found.group(1).contains(".printed") &&
        found.group(2).contains("opacity")).map(_.group(1).trim).toVector
    assertEquals(setters, Vector(".printed"))
```

Run: `./sbtw "root/testOnly distantlands.server.StylesheetArtSuite"`
Expected: FAIL, `no rule .art`.

- [ ] **Step 6: Add the rules**

First check that no existing rule sets opacity on a printed element:

Run: `grep -n "printed" frontend/styles.css | grep -n "opacity"`
Expected: no output. If a rule appears, stop and report it; do not edit
it.

In `frontend/styles.css`, after the `.card-slot-empty` rule and its
closing brace, before the comment that starts `/* Document level, above
.game-pane`, add:

```css
/* Licensed art, laid over the box that draws the same thing (game art
   design). The image fills the box and never sizes it: every box has its
   real proportions already, so the layout is the same with art or
   without. A box that holds an image isolates its stacking, and the image
   lies over the box's fill and under everything else in the box, so what
   lies on a card in play is over the art with no rule of its own. */
.art { position: absolute; inset: 0; z-index: -1; width: 100%; height: 100%;
  object-fit: contain; pointer-events: none; }
:has(> .art) { isolation: isolate; }
/* Once its image has loaded a box is ready, and what the box prints is
   hidden with opacity: the drawn text keeps its space and stays in the
   accessibility tree. Printed content follows its nearest box, so a ready
   box never hides what a box inside it draws when that box's own image is
   missing. */
.printed { opacity: var(--printed-opacity, 1); }
:is(.card-face, .card-hover-summary) { --printed-opacity: 1; }
:is(.card-face, .card-hover-summary).art-ready { --printed-opacity: 0; }

```

- [ ] **Step 7: Run the stylesheet suites**

Run: `./sbtw "root/testOnly distantlands.server.Stylesheet*"`
Expected: PASS, every suite.

- [ ] **Step 8: Commit**

```bash
git add frontend/src/main/scala/distantlands/frontend/ArtLayer.scala \
  frontend/src/test/scala/distantlands/frontend/ArtLayerSuite.scala \
  src/test/scala/distantlands/server/StylesheetArtSuite.scala \
  frontend/styles.css
git commit -m "feat(frontend): lay art over a box and hide what it prints

Co-Authored-By: Claude Fable 5.2 <noreply@anthropic.com>"
git log -3
```

---

### Task 7: Card faces, backs and the inspection overlay

**Files:**
- Modify: `frontend/src/main/scala/distantlands/frontend/CardFace.scala:92-134`
  (`render`)
- Modify: `frontend/src/main/scala/distantlands/frontend/ServerUiSupport.scala:188-193`
  (`facedownCard`)
- Modify: `frontend/styles.css` (the art rules from Task 6)
- Test: `frontend/src/test/scala/distantlands/frontend/CardArtSuite.scala`
  (new)
- Test: `src/test/scala/distantlands/server/StylesheetArtSuite.scala`
  (append)

**Interfaces:**
- Consumes: `ArtLayer.attach(box, key)`; `ArtCatalog.cardFace(name)`,
  `ArtCatalog.cardBack(cardKind)`; `TestArt`.
- Produces: no new name. Every caller of `CardFace.render` and
  `ServerUiSupport.facedownCard` gets art with no change of its own: the
  player boards, the site rows, the decision panels, the card lists and
  the inspection overlay.

**Which image a card gets.**

| Card as projected | Box | Image |
|---|---|---|
| Face up | the face | `cardFace(name)` |
| Hidden (the viewer cannot identify it) | the back | `cardBack(cardKind)` |
| Dormant legacy the viewer knows | the face, with its Dormant tag | `cardFace(name)` |
| Knowable (facedown, the viewer knows it) | the back; the hover summary is a box of its own | `cardBack(cardKind)` on the box, `cardFace(name)` on the summary (R2) |

The inspection overlay calls `CardFace.render` with the card turned face
up, so it shows the face's image at the overlay's size with no code of its
own. Its rules text is not printed and stays.

**A Vision.** Its image is landscape (596×381) and its back is portrait.
In a row a Vision keeps the denizen box, so its face image is turned a
quarter and sized to the box: the image's width is the box's height. In
the overlay the Vision's box is landscape, so the face is upright and the
back is the one turned.

- [ ] **Step 1: Write the failing surface tests**

Create `frontend/src/test/scala/distantlands/frontend/CardArtSuite.scala`:

```scala
package distantlands.frontend

import Dom.{all, one}
import distantlands.model.PlayerColor
import distantlands.protocol.art.ArtCatalog
import TestSites.*
import org.scalajs.dom

/** Card faces and backs with game art (game art design, "The surfaces"):
  * with art on a box holds an `img.art` with the catalog's `src`, with art
  * off it holds none, and the layout is the same either way. */
class CardArtSuite extends munit.FunSuite:
  override def afterEach(context: AfterEach): Unit = GameArt.reset()

  private val alchemist = CardDetails("9", "denizen", "Alchemist",
    suit = Some("arcane"), orientation = Some("face-up"), favor = 1)
  private val knownRelic = CardDetails("R01", "relic", "Sticky Fire",
    orientation = Some("face-down"), defense = Some(3))
  private val vision = CardDetails("vision:vision-of-faith", "vision",
    "Vision of Faith", orientation = Some("face-up"))
  private def hidden(kind: String): CardDetails = CardDetails("hidden", kind,
    s"Facedown $kind", orientation = Some("face-down"), hidden = true)

  /** The `src` of each image directly in `box`. */
  private def images(box: dom.Element): Vector[String] =
    all(box, ":scope > img.art").map(_.getAttribute("src"))

  test("a face-up card holds its face's image"):
    TestArt.listingWholeCatalog()
    assertEquals(images(CardFace.render(alchemist)),
      Vector("/art/cards/Alchemist%20(NF).png"))

  test("an edifice holds the image of the face it shows"):
    TestArt.listingWholeCatalog()
    assertEquals(images(CardFace.render(CardDetails("E01", "edifice",
      "Hall of Mockery", orientation = Some("face-up"),
      side = Some("ruined")))), Vector("/art/cards/Hall%20of%20Mockery%20(NF).png"))

  test("a card the viewer cannot identify holds its kind's back"):
    TestArt.listingWholeCatalog()
    Vector("denizen", "vision", "relic", "legacy").foreach(kind =>
      assertEquals(images(CardFace.render(hidden(kind))),
        Vector(s"/art/tts/backs/$kind-back.jpg"), kind))

  test("a facedown site relic holds the relic back"):
    TestArt.listingWholeCatalog()
    assertEquals(images(ServerUiSupport.facedownCard("relic")),
      Vector("/art/tts/backs/relic-back.jpg"))

  test("a knowable card holds its back, and its hover summary its face"):
    TestArt.listingWholeCatalog()
    val node = CardFace.render(knownRelic)
    assert(node.classList.contains("card-face-knowable"))
    assertEquals(images(node), Vector("/art/tts/backs/relic-back.jpg"))
    assertEquals(images(one(node, ".card-hover-summary")),
      Vector("/art/cards/Sticky%20Fire%20(NF).png"))

  test("a dormant legacy its viewer knows holds its face and keeps its " +
      "Dormant tag"):
    TestArt.listingWholeCatalog()
    val node = CardFace.render(CardDetails("L01", "legacy", "Iron Hand",
      suit = Some("order"), orientation = Some("face-down")))
    assertEquals(images(node), Vector("/art/cards/Iron%20Hand%20(NF).png"))
    assertEquals(one(node, ".legacy-dormant").textContent, "Dormant")
    assert(!one(node, ".legacy-dormant").classList.contains("printed"))

  test("a Vision holds its face in the denizen box it keeps"):
    TestArt.listingWholeCatalog()
    val node = CardFace.render(vision)
    assert(node.classList.contains("card-face-vision"))
    assertEquals(images(node),
      Vector("/art/cards/Vision%20of%20Faith%20(NF).png"))

  test("a denizen New Foundations did not reprint holds its base file"):
    TestArt.listingWholeCatalog()
    assertEquals(images(CardFace.render(CardDetails("1", "denizen",
      "Wrestlers", suit = Some("order"), orientation = Some("face-up")))),
      Vector("/art/cards/Wrestlers.png"))

  test("a card the catalog does not name stays drawn"):
    TestArt.listingWholeCatalog()
    val node = CardFace.render(CardDetails("x", "denizen", "Old Oak",
      suit = Some("order"), orientation = Some("face-up")))
    assertEquals(all(node, "img"), Vector.empty)

  test("a card whose file the folder lacks stays drawn, and its " +
      "neighbour does not"):
    TestArt.listing(ArtCatalog.cardFace("Alchemist").get.path)
    assertEquals(all(CardFace.render(knownRelic), "img"), Vector.empty)
    assertEquals(images(CardFace.render(alchemist)).size, 1)

  test("with art off, and before the manifest arrives, no card holds an " +
      "image"):
    val cards = Vector(alchemist, knownRelic, vision, hidden("denizen"))
    cards.foreach(card =>
      assertEquals(all(CardFace.render(card), "img"), Vector.empty))
    TestArt.listingWholeCatalog()
    GameArt.setEnabled(false)
    cards.foreach(card =>
      assertEquals(all(CardFace.render(card), "img"), Vector.empty))
    assertEquals(all(ServerUiSupport.facedownCard("relic"), "img"),
      Vector.empty)

  test("the image is the box's last child and sits under no printed mark"):
    TestArt.listingWholeCatalog()
    val node = CardFace.render(alchemist)
    assertEquals(node.lastElementChild.getAttribute("class"), "art")
    assertEquals(all(node, ".printed img, .card-tokens img"), Vector.empty)
    // What lies on the card in play is still there, unmarked.
    assertEquals(all(node, ".card-tokens:not(.printed)").size, 1)

  test("a loaded image marks only its own box ready"):
    TestArt.listingWholeCatalog()
    val node = CardFace.render(knownRelic)
    one(node, ":scope > img.art").dispatchEvent(new dom.Event("load"))
    assert(node.classList.contains("art-ready"))
    assert(!one(node, ".card-hover-summary").classList.contains("art-ready"))

  test("the inspection overlay shows a card's face image, and a hidden " +
      "card's back"):
    TestArt.listingWholeCatalog()
    val browser = new TestBrowser()
    val shell = new GameTableShell(browser.mount, developmentTools = false,
      logStore = None)
    try
      val origin = dom.document.createElement("button")
        .asInstanceOf[dom.html.Element]
      browser.mount.appendChild(origin)
      CardInspection.open(knownRelic, origin)
      assertEquals(images(one(browser.mount, ".card-overlay-body > .card-face")),
        Vector("/art/cards/Sticky%20Fire%20(NF).png"))
      // The rules beside the image are not printed and stay.
      assertEquals(all(browser.mount,
        ".card-overlay-details .printed").size, 0)
      CardInspection.open(hidden("relic"), origin)
      assertEquals(images(one(browser.mount, ".card-overlay-body > .card-face")),
        Vector("/art/tts/backs/relic-back.jpg"))
    finally
      shell.dispose()
      browser.close()

  /** A board with a face-up denizen, a known and an unknown site relic,
    * and a player to see it. */
  private def boardWithCards(): dom.Element =
    val red = GamePlayer("red", "Red", "Exile", PlayerColor.Red)
    val woods = quietSite("woods", "Woods").withDenizens(
      GameSiteCard("9", "Alchemist", Some(alchemist)))
      .withRelicCapacity(2).withRelics(2, knownRelic)
    WorldBoardRenderer.world(TestProjections.actingProjection(
      players = Vector(red),
      world = Vector(GameRegion("cradle", Vector(woods)))),
      BoardOptions.none)

  /** `node`'s markup with every art image taken out. */
  private def withoutArt(node: dom.Element): String =
    val copy = node.cloneNode(true).asInstanceOf[dom.Element]
    all(copy, "img.art").foreach(_.remove())
    copy.outerHTML

  test("a board drawn with art and without has the same boxes, classes " +
      "and inline geometry once the images are removed"):
    val drawn = boardWithCards()
    TestArt.listingWholeCatalog()
    val withArt = boardWithCards()
    assert(all(withArt, "img.art").size >= 2)
    assertEquals(withoutArt(withArt), drawn.outerHTML)
```

If `TestSites.withRelics` or `BoardOptions.none` has another shape than
`PrintedLayerSuite` uses, build the board as `PrintedLayerSuite` does and
keep the helper's name and the assertion.

- [ ] **Step 2: Run the tests to verify they fail**

Run: `./sbtw "frontend/testOnly distantlands.frontend.CardArtSuite"`
Expected: FAIL. The tests that expect an image fail with
`Vector() != Vector("/art/...")`; "with art off ..." passes.

- [ ] **Step 3: Attach art in `CardFace.render`**

In `CardFace.scala`, add the import
`import distantlands.protocol.art.ArtCatalog`, and replace the block from
`if card.hidden then` to the end of its `else` (lines 109-125) with:

```scala
    val face = ArtCatalog.cardFace(card.name)
    val back = ArtCatalog.cardBack(card.cardKind)
    if card.hidden then
      node.appendChild(text("span", "card-back-letter printed", backLetter(card.cardKind)))
      ArtLayer.attach(node, back)
    else if dormantLegacy(card) then
      summary(card).foreach(node.appendChild)
      node.appendChild(text("span", "legacy-dormant", "Dormant"))
      ArtLayer.attach(node, face)
    else if faceDown(card) then
      // Knowable: the back is what shows at rest; the summary is revealed by
      // CSS on hover only, inside the same box, so nothing moves.
      node.appendChild(text("span", "card-back-letter printed", backLetter(card.cardKind)))
      val pip = element("span", "card-knowable-pip")
      pip.setAttribute("aria-hidden", "true")
      node.appendChild(pip)
      val peek = element("span", "card-hover-summary")
      summary(card).foreach(peek.appendChild)
      // The summary is a box of its own: it shows the face while the card
      // shows its back, and each falls back to its own drawing.
      ArtLayer.attach(peek, face)
      node.appendChild(peek)
      ArtLayer.attach(node, back)
    else
      summary(card).foreach(node.appendChild)
      ArtLayer.attach(node, face)
```

In `ServerUiSupport.facedownCard`, before `node` is returned, add
`ArtLayer.attach(node, ArtCatalog.cardBack(cardKind))` and the import
`import distantlands.protocol.art.ArtCatalog` at the top of the file (the
file already imports `distantlands.protocol._`; if `art.ArtCatalog`
resolves through it, write `art.ArtCatalog.cardBack(cardKind)` and add no
import).

- [ ] **Step 4: Run the card suites**

Run: `./sbtw "frontend/testOnly distantlands.frontend.CardArtSuite distantlands.frontend.CardFaceSuite distantlands.frontend.PrintedLayerSuite distantlands.frontend.CardInspectionOverlaySuite"`
Expected: PASS, every test, with no edit to the three existing suites.

- [ ] **Step 5: Write the failing stylesheet test for the Vision**

Append to `StylesheetArtSuite`:

```scala
  /** A Vision's image is landscape and its back portrait. Whichever does
    * not match the box it is in is turned a quarter and sized to the
    * box: its width is the box's height. */
  test("a Vision's face is turned in its denizen box, and its back in " +
      "the overlay's landscape box"):
    val turned = rule(".card-face-vision:not(.card-face-down) > .art,\n" +
      ".card-face-vision > .card-hover-summary > .art")
    Vector("inset: auto", "top: 50%", "left: 50%",
      "width: calc(100% / (var(--ratio-denizen)))",
      "height: calc(100% * (var(--ratio-denizen)))",
      "transform: translate(-50%, -50%) rotate(-90deg)")
      .foreach(part => assert(turned.contains(part), part))
    val upright = rule(
      ".card-overlay-body > .card-face-vision:not(.card-face-down) > .art")
    Vector("inset: 0", "width: 100%", "height: 100%", "transform: none")
      .foreach(part => assert(upright.contains(part), part))
    val back = rule(
      ".card-overlay-body > .card-face-vision.card-face-down > .art")
    Vector("inset: auto", "top: 50%", "left: 50%",
      "width: calc(100% * (var(--ratio-denizen)))",
      "height: calc(100% / (var(--ratio-denizen)))",
      "transform: translate(-50%, -50%) rotate(-90deg)")
      .foreach(part => assert(back.contains(part), part))
```

Run: `./sbtw "root/testOnly distantlands.server.StylesheetArtSuite"`
Expected: the new test FAILS with `no rule .card-face-vision:not(...)`.

- [ ] **Step 6: Add the Vision's rules**

In `frontend/styles.css`, after the three `--printed-opacity` rules from
Task 6, add:

```css
/* A Vision is a denizen card printed on its side: its image is landscape
   and its back portrait. In a row it keeps the denizen box, so its face
   is turned a quarter and sized to the box, the image's width being the
   box's height. The overlay reads a Vision upright in a landscape box, so
   there the face fills the box and the back is the one turned. */
.card-face-vision:not(.card-face-down) > .art,
.card-face-vision > .card-hover-summary > .art { inset: auto; top: 50%;
  left: 50%; width: calc(100% / (var(--ratio-denizen)));
  height: calc(100% * (var(--ratio-denizen)));
  transform: translate(-50%, -50%) rotate(-90deg); }
.card-overlay-body > .card-face-vision:not(.card-face-down) > .art {
  inset: 0; width: 100%; height: 100%; transform: none; }
.card-overlay-body > .card-face-vision.card-face-down > .art { inset: auto;
  top: 50%; left: 50%; width: calc(100% * (var(--ratio-denizen)));
  height: calc(100% / (var(--ratio-denizen)));
  transform: translate(-50%, -50%) rotate(-90deg); }
```

`StylesheetProportionsSuite` allows each ratio's numbers once; these rules
read `var(--ratio-denizen)` and write no number.

- [ ] **Step 7: Run the stylesheet suites and the frontend suites**

Run: `./sbtw "root/testOnly distantlands.server.Stylesheet*"`
Expected: PASS.

Run: `./sbtw frontend/test`
Expected: PASS, every suite; no existing suite edited.

- [ ] **Step 8: Commit**

```bash
git add frontend/src/main/scala/distantlands/frontend/CardFace.scala \
  frontend/src/main/scala/distantlands/frontend/ServerUiSupport.scala \
  frontend/src/test/scala/distantlands/frontend/CardArtSuite.scala \
  src/test/scala/distantlands/server/StylesheetArtSuite.scala \
  frontend/styles.css
git commit -m "feat(frontend): show art on card faces and backs

Co-Authored-By: Claude Fable 5.2 <noreply@anthropic.com>"
git log -3
```

---

### Task 8: The Settings dialog

**Files:**
- Create: `frontend/src/main/scala/distantlands/frontend/SettingsDialog.scala`
- Modify: `frontend/src/main/scala/distantlands/frontend/GameTableShell.scala`
  (the header controls near line 170, `inertBehind`, `update`, `dispose`)
- Modify: `frontend/src/main/scala/distantlands/frontend/ServerModeUi.scala`
- Modify: `frontend/styles.css`
- Test: `frontend/src/test/scala/distantlands/frontend/SettingsDialogSuite.scala`
  (new)
- Test: `src/test/scala/distantlands/server/StylesheetArtSuite.scala`
  (append)

**Interfaces:**
- Consumes: `GameArt.manifest`, `enabled`, `setEnabled`, `onChange`;
  `ArtCatalog.allPaths`; `ServerUiSupport.button`, `element`, `text`.
- Produces:
  - `final class SettingsDialog(mount: dom.Element, opening: () => Unit)`
    with `val toggle: dom.html.Button`, `val node: dom.html.Element`,
    `def isOpen: Boolean`, `def close(returnFocus: Boolean = true): Unit`,
    `def refresh(): Unit`, `def dispose(): Unit`
  - The Settings button in the Players pane header for every player, in a
    `span.header-controls` that also holds Dev tools when the shell shows
    it.
  - `ServerModeUi.start` registers the table's redraw with
    `GameArt.onChange`.

Read `DESIGN.md` ("Buttons", "Elevation & Depth", "Do's and Don'ts") and
`/Users/roman/.claude/skills/impeccable/reference/craft-floor.md` before
the CSS step.

**Behaviour.**

- The button reads "Settings", with `aria-controls="settings-panel"` and
  `aria-expanded`. It opens and closes the dialog. Opening moves focus to
  Close; closing by the button, Close or Escape returns focus to the
  button. Opening Settings closes the development panel, and opening the
  development panel closes Settings: they share one place on the screen.
- The dialog is `aside#settings-panel.settings-panel`, `role="dialog"`,
  `aria-labelledby="settings-heading"`, with the heading "Settings" and a
  Close button, built as the development panel is.
- The switch is a `button.setting-switch` with `role="switch"`,
  `aria-checked` and `aria-describedby="game-art-hint game-art-reason"`.
  It holds the label "Game art" and the word "On" or "Off", which is
  hidden from assistive technology since `aria-checked` says it.
- The hint always reads "Card faces, the mat and pieces drawn from the
  host's art." When the manifest lists some but not all of the catalog it
  adds " <n> of <total> images; the rest stay drawn."
- The switch wears the Withheld face (`DESIGN.md`, "Buttons":
  `aria-disabled="true"`, so it still takes focus, with the reason as its
  hover note), shows Off, and shows its reason on the line below it: for an empty
  manifest "This server has no game art."; for a failed fetch "Couldn't
  load the art list; reload to try again."; before the manifest arrives
  "Loading the art list…".
- A click flips `GameArt.setEnabled`, which saves the preference and runs
  the table's redraw. The redraw reaches `GameTableShell.update`, which
  calls `refresh()`, so the dialog also follows a manifest that arrives
  while it is open. `refresh` changes attributes and text in place and
  never rebuilds the switch, so focus stays on it.

- [ ] **Step 1: Write the failing tests**

Create
`frontend/src/test/scala/distantlands/frontend/SettingsDialogSuite.scala`:

```scala
package distantlands.frontend

import Dom.{all, one, press}
import distantlands.protocol.art.ArtCatalog
import org.scalajs.dom

/** The Settings dialog and its one switch, Game art (game art design,
  * "The Settings dialog"). Runs the real shell in `TestBrowser`. */
class SettingsDialogSuite extends munit.FunSuite:
  override def afterEach(context: AfterEach): Unit = GameArt.reset()

  /** Runs `check` against a shell, with the development tools shown or
    * not, and disposes of it. */
  private def withShell(developmentTools: Boolean = false)(
      check: TestBrowser => Unit): Unit =
    val browser = new TestBrowser()
    val shell = new GameTableShell(browser.mount, developmentTools,
      logStore = None)
    try check(browser)
    finally
      shell.dispose()
      browser.close()

  private def toggle(browser: TestBrowser): dom.html.Button =
    one(browser.mount, ".settings-toggle").asInstanceOf[dom.html.Button]
  private def panel(browser: TestBrowser): dom.Element =
    one(browser.mount, ".settings-panel")
  private def switch(browser: TestBrowser): dom.html.Button =
    one(browser.mount, ".setting-switch").asInstanceOf[dom.html.Button]
  private def reason(browser: TestBrowser): dom.Element =
    one(browser.mount, ".setting-reason")
  /** Whether the switch wears the Withheld face: `aria-disabled`, so it
    * still takes focus and its hover note gives the reason. */
  private def withheld(browser: TestBrowser): Boolean =
    switch(browser).getAttribute("aria-disabled") == "true"

  test("every player has a Settings button in the Players pane header, " +
      "beside Dev tools when those are shown"):
    withShell() { browser =>
      assertEquals(all(browser.mount,
        ".pane-players .pane-header .header-controls > button")
        .map(_.textContent), Vector("Settings"))
    }
    withShell(developmentTools = true) { browser =>
      assertEquals(all(browser.mount,
        ".pane-players .pane-header .header-controls > button")
        .map(_.textContent), Vector("Settings", "Dev tools"))
    }

  test("the button opens a dialog headed Settings and moves focus to " +
      "Close; Close returns focus to the button"):
    withShell() { browser =>
      assert(panel(browser).hasAttribute("hidden"))
      assertEquals(panel(browser).getAttribute("role"), "dialog")
      assertEquals(toggle(browser).getAttribute("aria-expanded"), "false")
      toggle(browser).click()
      assert(!panel(browser).hasAttribute("hidden"))
      assertEquals(toggle(browser).getAttribute("aria-expanded"), "true")
      assertEquals(one(panel(browser), ".pane-heading").textContent,
        "Settings")
      assertEquals(panel(browser).getAttribute("aria-labelledby"),
        one(panel(browser), ".pane-heading").id)
      val close = one(panel(browser), ".settings-close")
      assertEquals(dom.document.activeElement, close)
      close.asInstanceOf[dom.html.Button].click()
      assert(panel(browser).hasAttribute("hidden"))
      assertEquals(dom.document.activeElement, toggle(browser))
    }

  test("Escape closes the dialog and returns focus to the button"):
    withShell() { browser =>
      toggle(browser).click()
      press(switch(browser), "Escape")
      assert(panel(browser).hasAttribute("hidden"))
      assertEquals(dom.document.activeElement, toggle(browser))
    }

  test("opening Settings closes the development panel, and the reverse"):
    withShell(developmentTools = true) { browser =>
      val dev = one(browser.mount, ".development-panel")
      val devToggle = one(browser.mount, ".dev-toggle")
        .asInstanceOf[dom.html.Button]
      devToggle.click()
      toggle(browser).click()
      assert(dev.hasAttribute("hidden"))
      assertEquals(devToggle.getAttribute("aria-expanded"), "false")
      assert(!panel(browser).hasAttribute("hidden"))
      devToggle.click()
      assert(panel(browser).hasAttribute("hidden"))
      assertEquals(toggle(browser).getAttribute("aria-expanded"), "false")
      assert(!dev.hasAttribute("hidden"))
    }

  test("the switch is a switch named Game art, described by its hint"):
    TestArt.listingWholeCatalog()
    withShell() { browser =>
      val control = switch(browser)
      assertEquals(control.getAttribute("role"), "switch")
      assertEquals(one(control, ".setting-switch-label").textContent,
        "Game art")
      assertEquals(control.getAttribute("aria-checked"), "true")
      assert(!control.hasAttribute("aria-disabled"))
      assert(!control.hasAttribute(HoverTip.Attribute))
      val hint = one(browser.mount, ".setting-hint")
      assertEquals(hint.textContent,
        "Card faces, the mat and pieces drawn from the host's art.")
      assert(control.getAttribute("aria-describedby").split(" ")
        .contains(hint.id))
      assert(reason(browser).hasAttribute("hidden"))
    }

  test("a server with no art shows the switch disabled, with its reason"):
    TestArt.listing()
    withShell() { browser =>
      assert(withheld(browser))
      assert(!switch(browser).disabled)
      assertEquals(switch(browser).getAttribute("aria-checked"), "false")
      assertEquals(reason(browser).textContent,
        "This server has no game art.")
      assertEquals(switch(browser).getAttribute(HoverTip.Attribute),
        "This server has no game art.")
      assert(!reason(browser).hasAttribute("hidden"))
      assert(switch(browser).getAttribute("aria-describedby").split(" ")
        .contains(reason(browser).id))
    }

  test("a failed art list shows the switch disabled, with its reason"):
    GameArt.install(GameArt.Manifest.Failed, new MemoryChronicleStorage())
    withShell() { browser =>
      assert(withheld(browser))
      assertEquals(reason(browser).textContent,
        "Couldn't load the art list; reload to try again.")
    }

  test("until the art list arrives the switch is disabled and says so"):
    withShell() { browser =>
      assert(withheld(browser))
      assertEquals(reason(browser).textContent, "Loading the art list…")
    }

  test("a manifest listing part of the catalog adds the count to the hint"):
    TestArt.listing(ArtCatalog.allPaths.take(3)*)
    withShell() { browser =>
      assertEquals(one(browser.mount, ".setting-hint").textContent,
        "Card faces, the mat and pieces drawn from the host's art. " +
          s"3 of ${ArtCatalog.allPaths.size} images; the rest stay drawn.")
      assert(!withheld(browser))
    }

  test("flipping the switch saves the preference and redraws the table " +
      "at once, and the switch keeps its place"):
    val store = TestArt.listingWholeCatalog()
    withShell() { browser =>
      var redraws = 0
      GameArt.onChange(() => redraws += 1)
      toggle(browser).click()
      val control = switch(browser)
      control.click()
      assertEquals(store.values.get("distantlands.art"), Some("off"))
      assertEquals(redraws, 1)
      assertEquals(control.getAttribute("aria-checked"), "false")
      assertEquals(one(control, ".setting-switch-state").textContent, "Off")
      assert(switch(browser) eq control)
      control.click()
      assertEquals(store.values.get("distantlands.art"), Some("on"))
      assertEquals(control.getAttribute("aria-checked"), "true")
      assertEquals(one(control, ".setting-switch-state").textContent, "On")
    }

  test("a withheld switch changes nothing when clicked"):
    val store = TestArt.listing()
    withShell() { browser =>
      var redraws = 0
      GameArt.onChange(() => redraws += 1)
      switch(browser).click()
      assertEquals(store.values.get("distantlands.art"), None)
      assertEquals(redraws, 0)
    }

  test("each table update brings the dialog up to date"):
    val browser = new TestBrowser()
    val shell = new GameTableShell(browser.mount, developmentTools = false,
      logStore = None)
    try
      assert(withheld(browser))
      TestArt.listingWholeCatalog()
      def pane(): dom.Element = dom.document.createElement("div")
      shell.update("game", "decision", pane(), pane(), pane(), pane())
      assert(!withheld(browser))
      assertEquals(switch(browser).getAttribute("aria-checked"), "true")
    finally
      shell.dispose()
      browser.close()
```

The last test builds its own shell because it calls `update`.

- [ ] **Step 2: Run the tests to verify they fail**

Run: `./sbtw "frontend/testOnly distantlands.frontend.SettingsDialogSuite"`
Expected: FAIL, `expected exactly one .settings-toggle, found 0` and its
like.

- [ ] **Step 3: Write `SettingsDialog`**

Create
`frontend/src/main/scala/distantlands/frontend/SettingsDialog.scala`:

```scala
package distantlands.frontend

import distantlands.protocol.art.ArtCatalog
import org.scalajs.dom
import ServerUiSupport._

/** The table's settings (game art design, "The Settings dialog"): a
  * button for the Players pane header and the dialog it opens, built as
  * the development panel is. It holds one switch, Game art.
  *
  * `opening` runs before the dialog opens, so the shell can close what
  * shares its place. `refresh` brings the switch up to date in place and
  * never rebuilds it, so a redraw of the table leaves focus on it.
  */
private[frontend] final class SettingsDialog(mount: dom.Element,
    opening: () => Unit):
  val toggle: dom.html.Button = button("Settings", "settings-toggle")
  toggle.setAttribute("aria-controls", "settings-panel")
  toggle.setAttribute("aria-expanded", "false")

  val node: dom.html.Element =
    element("aside", "settings-panel").asInstanceOf[dom.html.Element]
  node.id = "settings-panel"
  node.setAttribute("hidden", "")
  node.setAttribute("role", "dialog")
  node.setAttribute("aria-labelledby", "settings-heading")
  private val header = element("div", "pane-header")
  private val heading =
    text("h2", "pane-heading", "Settings").asInstanceOf[dom.html.Element]
  heading.id = "settings-heading"
  heading.tabIndex = -1
  private val closeButton = button("Close", "settings-close")
  header.appendChild(heading)
  header.appendChild(closeButton)
  private val content = element("div", "pane-content")

  private val art = button("", "setting-switch")
  art.setAttribute("role", "switch")
  art.setAttribute("aria-describedby", "game-art-hint game-art-reason")
  art.appendChild(text("span", "setting-switch-label", "Game art"))
  private val state = text("span", "setting-switch-state", "Off")
  // `aria-checked` says it; the word is for the eye.
  state.setAttribute("aria-hidden", "true")
  art.appendChild(state)
  private val hint = text("p", "setting-hint", "")
  hint.id = "game-art-hint"
  private val reason = text("p", "setting-reason", "")
  reason.id = "game-art-reason"
  private val setting = element("div", "setting")
  Vector(art, hint, reason).foreach(setting.appendChild)
  content.appendChild(setting)
  node.appendChild(header)
  node.appendChild(content)
  mount.appendChild(node)

  def isOpen: Boolean = !node.hasAttribute("hidden")

  private def open(): Unit =
    opening()
    refresh()
    node.removeAttribute("hidden")
    toggle.setAttribute("aria-expanded", "true")
    closeButton.focus()

  /** Hides the dialog. Focus returns to the button unless another dialog
    * is taking it. */
  def close(returnFocus: Boolean = true): Unit =
    val wasOpen = isOpen
    node.setAttribute("hidden", "")
    toggle.setAttribute("aria-expanded", "false")
    if wasOpen && returnFocus then toggle.focus()

  /** The switch, its hint and its reason, from `GameArt` as it stands. */
  def refresh(): Unit =
    val total = ArtCatalog.allPaths.size
    val (available, why, count) = GameArt.manifest match
      case GameArt.Manifest.Pending => (false, "Loading the art list…", "")
      case GameArt.Manifest.Failed =>
        (false, "Couldn't load the art list; reload to try again.", "")
      case GameArt.Manifest.Listed(files) if files.isEmpty =>
        (false, "This server has no game art.", "")
      case GameArt.Manifest.Listed(files) if files.size < total =>
        (true, "", s" ${files.size} of $total images; the rest stay drawn.")
      case GameArt.Manifest.Listed(_) => (true, "", "")
    val on = available && GameArt.enabled
    // The Withheld face (DESIGN.md, "Buttons"): the switch still takes
    // focus, and its hover note gives the reason the line below it shows.
    if available then
      art.removeAttribute("aria-disabled")
      art.removeAttribute(HoverTip.Attribute)
    else
      art.setAttribute("aria-disabled", "true")
      HoverTip.set(art, why)
    art.setAttribute("aria-checked", on.toString)
    state.textContent = if on then "On" else "Off"
    hint.textContent = SettingsDialog.Hint + count
    reason.textContent = why
    if why.isEmpty then reason.setAttribute("hidden", "")
    else reason.removeAttribute("hidden")

  toggle.onclick = _ => if isOpen then close() else open()
  closeButton.onclick = _ => close()
  art.onclick = _ =>
    // Saves the preference and redraws the table, which refreshes this
    // dialog through the shell; the direct refresh covers a page with no
    // table to redraw. A withheld switch answers no click.
    if !art.hasAttribute("aria-disabled") then
      GameArt.setEnabled(!GameArt.enabled)
      refresh()
  private val escape: dom.KeyboardEvent => Unit = event =>
    if event.key == "Escape" && isOpen then
      event.preventDefault()
      close()
  node.addEventListener("keydown", escape)
  refresh()

  def dispose(): Unit =
    node.removeEventListener("keydown", escape)
    toggle.remove()
    node.remove()

private[frontend] object SettingsDialog:
  val Hint = "Card faces, the mat and pieces drawn from the host's art."
```

- [ ] **Step 4: Wire the dialog into the shell and the table**

In `GameTableShell.scala`:

1. Replace `if developmentTools then players.header.appendChild(devToggle)`
   with the header controls. `settings` is declared lazily because
   `setDev` and the dialog name each other:

```scala
  // Settings for every player; Dev tools beside it where they are shown.
  private lazy val settings: SettingsDialog =
    new SettingsDialog(mount, () => hideDev())
  private val headerControls = element("span", "header-controls")
  players.header.appendChild(headerControls)
```

   and, after `mount.appendChild(dev)`:

```scala
  headerControls.appendChild(settings.toggle)
  if developmentTools then headerControls.appendChild(devToggle)
```

2. Replace `setDev` with a version that closes Settings when it opens,
   and add `hideDev`, which takes no focus:

```scala
  /** Hides the development panel without moving focus: Settings is
    * opening in its place. */
  private def hideDev(): Unit =
    dev.setAttribute("hidden", "")
    devToggle.setAttribute("aria-expanded", "false")
  private def setDev(open: Boolean): Unit =
    if open then
      settings.close(returnFocus = false)
      dev.removeAttribute("hidden")
    else dev.setAttribute("hidden", "")
    devToggle.setAttribute("aria-expanded", open.toString)
    if open then close.focus() else devToggle.focus()
```

3. In `inertBehind`, make the dialog inert with the table:
   `Vector(table, dev, settings.node).foreach(...)`.
4. At the end of `update`, add `settings.refresh()`.
5. In `dispose`, add `settings.dispose()` before `table.remove()`.

In `ServerModeUi.start`, before `session.open(...)`:

```scala
    // The manifest arriving, or the Game art switch, redraws the table.
    GameArt.onChange(() => screen.render())
```

- [ ] **Step 5: Run the tests to verify they pass**

Run: `./sbtw "frontend/testOnly distantlands.frontend.SettingsDialogSuite"`
Expected: PASS, 12 tests.

Run: `./sbtw frontend/test`
Expected: PASS. If a suite asserts the Players header's children or
reaches the Dev tools button as the header's child, read the assertion: a
selector that named `.pane-header > .dev-toggle` now needs
`.pane-header .dev-toggle`. Change only that selector and list the change
as a deviation.

- [ ] **Step 6: Write the failing stylesheet tests**

Append to `StylesheetArtSuite`:

```scala
  /** The declarations of every rule whose selector list names
    * `selector`. */
  private def rulesNaming(selector: String): Vector[String] =
    val rules = """([^{}]+)\{([^{}]*)\}""".r
    rules.findAllMatchIn(uncommented).filter(_.group(1).split(",")
      .exists(_.trim == selector)).map(_.group(2)).toVector

  test("the Settings button and Close wear the Control face, with the " +
      "touch size"):
    Vector(".settings-toggle", ".settings-close").foreach { selector =>
      val face = rulesNaming(selector).mkString(" ")
      Vector("background: var(--raised)", "border: 1px solid var(--brass-line)",
        "border-radius: 4px", "min-height: 30px", "min-height: 44px")
        .foreach(part => assert(face.contains(part), s"$selector $part"))
    }

  test("the settings dialog sits where the development panel does and " +
      "hides when hidden"):
    val panel = rule(".settings-panel")
    Vector("position: fixed", "z-index: 50", "right: 12px", "top: 48px")
      .foreach(part => assert(panel.contains(part), part))
    assert(rule(".settings-panel[hidden]").contains("display: none"))

  test("the switch is a full-width control of touch height; on, it wears " +
      "the selection colours; withheld, it dims"):
    val control = rule(".setting-switch")
    Vector("width: 100%", "min-height: 44px", "background: var(--raised)",
      "border: 1px solid var(--brass-line)")
      .foreach(part => assert(control.contains(part), part))
    val on = rule(""".setting-switch[aria-checked="true"]""")
    Vector("border-color: var(--selection)",
      "background: var(--selection-fill)")
      .foreach(part => assert(on.contains(part), part))
    assert(rule(""".setting-switch[aria-disabled="true"]""")
      .contains("opacity: 0.5"))

  test("a hidden reason takes no room"):
    assert(rule(".setting-reason[hidden]").contains("display: none"))
```

Run: `./sbtw "root/testOnly distantlands.server.StylesheetArtSuite"`
Expected: the four new tests FAIL.

- [ ] **Step 7: Add the dialog's rules**

In `frontend/styles.css`:

1. Add `.settings-toggle, .settings-close` to the three selector lists
   that give the Control face: the face itself (`.map-control, .dev-toggle,
   .dev-close, ...` near line 1841), its hover (near line 1848) and the
   coarse-pointer size (near line 2497). Write each as
   `.dev-close, .settings-toggle, .settings-close,` in the list.
2. Change `.game-table button, .development-panel button { cursor:
   pointer; }` to name `.settings-panel button` as well, and add
   `.settings-panel button:focus-visible` to the focus rule that lists
   `.development-panel button:focus-visible`.
3. After the `.development-panel .events` rule, add:

```css
/* The Players header's controls: Settings, and Dev tools where shown. */
.header-controls { display: flex; align-items: center; gap: 5px; }
/* The settings dialog, where the development panel opens and built as it
   is; the two never show together. */
.settings-panel { position: fixed; z-index: 50; right: 12px; top: 48px;
  width: min(360px, calc(100vw - 24px)); max-height: calc(100dvh - 64px);
  display: flex; flex-direction: column; background: var(--base);
  border: 1px solid var(--brass-line); box-shadow: 0 12px 45px #000b; }
.settings-panel[hidden] { display: none; }
/* A switch: its name at the left, On or Off at the right. On, it wears
   the selection colours, as the setting in force does in a consent row. */
.setting-switch { display: flex; align-items: center;
  justify-content: space-between; gap: 12px; width: 100%; min-height: 44px;
  padding: 4px 9px; color: #e8d9bb; background: var(--raised);
  border: 1px solid var(--brass-line); border-radius: 4px; font: inherit;
  font-size: .9rem; }
.setting-switch[aria-checked="true"] { border-color: var(--selection);
  background: var(--selection-fill); color: var(--selection); }
.setting-switch:hover:not([aria-disabled="true"]) {
  border-color: var(--cream-bright); }
.setting-switch[aria-disabled="true"] { opacity: 0.5; cursor: not-allowed; }
.setting-switch-state { font-weight: 700; }
.setting-hint, .setting-reason { margin: 6px 0 0; font-size: .78rem;
  line-height: 1.35; color: var(--ink-dim); }
.setting-reason { color: var(--brass-bright); }
.setting-reason[hidden] { display: none; }
```

Check the three custom properties exist before using them:

Run: `grep -n -e "--selection-fill:" -e "--selection:" -e "--brass-bright:" -e "--cream-bright:" -e "--ink-dim:" -e "--raised:" frontend/styles.css`
Expected: one definition of each. If one is missing, use the property the
neighbouring Control rules use and list the change as a deviation.

- [ ] **Step 8: Run the stylesheet suites and the frontend suites**

Run: `./sbtw "root/testOnly distantlands.server.Stylesheet*"`
Expected: PASS.

Run: `./sbtw frontend/test`
Expected: PASS.

- [ ] **Step 9: Commit**

```bash
git add frontend/src/main/scala/distantlands/frontend/SettingsDialog.scala \
  frontend/src/main/scala/distantlands/frontend/GameTableShell.scala \
  frontend/src/main/scala/distantlands/frontend/ServerModeUi.scala \
  frontend/src/test/scala/distantlands/frontend/SettingsDialogSuite.scala \
  src/test/scala/distantlands/server/StylesheetArtSuite.scala \
  frontend/styles.css
git commit -m "feat(frontend): add the Settings dialog and the Game art switch

Co-Authored-By: Claude Fable 5.2 <noreply@anthropic.com>"
git log -3
```

---

### Task 9: Gate and browser check

**Files:** none, unless a check finds a defect; a defect is fixed
test-first in its own commit.

- [ ] **Step 1: Run the full gate**

```bash
./sbtw test
./sbtw frontend/test
python3 scripts/check-architecture.py
python3 scripts/check-markdown-links.py
```

Expected: each passes. Record the test counts for the report.

- [ ] **Step 2: Start a scratch server with the art folder**

The scratch database lives in the session's scratchpad, never under
`var/`. Run in the background:

```bash
./sbtw frontend/fastLinkJS
./sbtw "runMain distantlands.server.DistantLandsServer --port 8083 --database-path /private/tmp/claude-501/-Users-roman-projects-distantlands/c17e13c6-2b51-40f4-a67d-0fd7cda78149/scratchpad/game-art-db/distantlands --art-path /Users/roman/projects/distantlands/reference/assets"
```

Expected in the server's output: one line `art: <n> files from
/Users/roman/projects/distantlands/reference/assets`, and either no
warning or a warning naming the lacking paths. Record both for the report.

- [ ] **Step 3: Check the table in the browser**

Open `http://127.0.0.1:8083/?mode=server`, create a game and open the
table. At 1440×900, 1024×768 and a phone width (375×812), with the browser
tools:

1. Settings opens from the Players header, shows Game art On, and closes
   with Close and with Escape, focus returning to the button.
2. With art on: a relic, an edifice, a legacy and a reprinted denizen show
   their images in their boxes; a facedown adviser and a facedown site
   relic show their backs; tokens on a card lie over the image; a denizen
   New Foundations did not reprint shows its base file. A Vision in a row reads turned a quarter, and
   upright in the overlay.
3. The inspection overlay shows the image large, with the rules text
   beside it.
4. Flip the switch off: the table redraws drawn faces at once, without a
   reload. Take a screenshot with art on and one with art off at each
   size, and compare the cards' boxes with
   `getBoundingClientRect` on the same `[data-card-id]` elements: the
   rectangles must be equal.
5. Reload: the switch is still off. Switch it on again.
6. A blocked file stays drawn: check with
   `document.querySelectorAll('.card-face:not(.art-ready) > img.art')`
   after the load settles, and by renaming nothing in the art folder. Use
   a card the catalog does not name and one request blocked in the
   network tools if the tools allow it; report which was done.
7. Restart the scratch server without `--art-path` and reload: the switch
   is disabled with "This server has no game art."

Record what each step showed, with screenshots or page text.

- [ ] **Step 4: Stop the scratch server**

Stop the background server. Check nothing listens on 8083:
`lsof -nP -iTCP:8083 -sTCP:LISTEN` prints nothing.

- [ ] **Step 5: Report**

Report to the orchestrator: commits (hash and subject), gate results with
the real numbers, what the browser check showed, deviations, open
questions and anything left undone.

---

## Self-Review

**Spec coverage, slice 1 ("Plumbing and cards").**

| Spec | Task |
|---|---|
| `--art-path <dir>`; without it no art and the setting disabled | 1, 4, 8 |
| The scan at startup; kept files are catalog paths with an image extension | 4 |
| `GET /art/manifest.json`, always served | 4 |
| `GET /art/<path>`; other paths, `..` and a wrong extension are 404 without the disk | 4 |
| `Cache-Control: public, max-age=86400` and an ETag | 4 |
| The startup line and the warning naming lacking paths | 4 |
| `ArtCatalog` in `shared`; cards and backs; `allPaths` | 2 |
| `docs/operations/game-art.md`, names only, equal to `allPaths` | 3 |
| `GameArt`: manifest once, preference `distantlands.art`, on by default, storage guard, `url` | 5 |
| `GameArt` remembers loaded and failed URLs | 5, 6 |
| `ArtLayer.attach`: the image's attributes, `art-ready` on load, removal on error, remembered URLs | 6 |
| Printed content hidden with opacity and kept in the accessibility tree | 6 |
| The Settings dialog: button, dialog, switch, hint, reasons, partial count, save and redraw | 8 |
| Card faces and backs; a Vision turned in a denizen box | 7 |
| The inspection overlay shows the image large; rules text stays | 7 |
| Layout invariance | 7 (test), 9 (browser) |
| Server route, catalog, `GameArt`, `ArtLayer`, surface and settings tests | 2-8 |
| Browser check at three sizes | 9 |

Left to later slices, as the spec's "Delivery" orders: the mat, site
cards, banners, the legacy and foundation strips and oath goals (slice
2); symbols, pieces, tokens and dice, `DESIGN.md`, the quick start and
`configuration.md` links, and L3's removal from the roadmap (slice 3). The
legacy strip's own back chip (`LegacyStrip`) and the decks' and discard
piles' backs on the mat are drawn by slice 2 with their surfaces.

**Placeholder scan.** Every code step carries its code. Two steps name a
fallback to take if a helper's shape differs from what was read
(`TestSites` in Task 7, a header selector in Task 8); each says what to
keep.

**Type consistency.** `ArtKey(path)`; `ArtCatalog.cardFace(name)` and
`cardBack(cardKind)` return `Option[ArtKey]`; `ArtLayer.attach(box,
Option[ArtKey])`; `GameArt.url(ArtKey): Option[String]`;
`GameArt.Manifest.{Pending, Failed, Listed}`; `TestArt.listing` and
`listingWholeCatalog` return the `MemoryChronicleStorage`;
`SettingsDialog(mount, opening)`; `ServerRoutes.route(..., art)`;
`ArtLibrary.scan(folder, catalog)`. The names agree across tasks.
