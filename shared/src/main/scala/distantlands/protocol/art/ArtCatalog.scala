package distantlands.protocol.art

/** One image in the host's art folder, named by its path under the folder
  * (game art design, "The catalog"). Only [[ArtCatalog]] makes one, so every
  * key is a path the server may serve. */
final case class ArtKey private[art] (path: String)

/** One face of a die: a square of its die's sheet, in the sheet's pixels
  * from its upper left, with the sheet's size, so a renderer can show the
  * face alone (game art design, "Dice"). */
final case class DieCrop(sheet: ArtKey, x: Int, y: Int, w: Int, h: Int,
    sheetW: Int, sheetH: Int)

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

  /** Every site. */
  val sites: Vector[String] = names("""
    Ancient City; Broken Peaks; Buried Giant; Deep Woods; Desolate Shore;
    Dunes; Fair Isle; Golden Valley; Great Slum; Green Shore; Headwaters;
    Hidden Place; Mines; Narrow Pass; Painted Towers; Riverbank; Rocky
    Coast; Salt Flats; Shrouded Woods; Solitary Pillar; Standing Stones;
    Steppe; Sunken Isles; Tidal Marshes
    """)

  /** A banner's file for each face, by the projection's face key. */
  private val banners: Vector[(String, ArtKey)] = Vector(
    "mob" -> "Banner of the People's Favor - Mob",
    "grand-council" -> "Banner of the People's Favor - Grand Council",
    "wandering-flame" -> "Banner of the Darkest Secret - Wandering Flame",
    "festival" -> "Banner of the Darkest Secret - Festival")
    .map((face, name) => face -> ArtKey(s"cards/$name (NF).png"))

  private val FoundationNumerals = Vector("I", "II", "III", "IV", "V", "VI")

  /** Each foundation card's two faces, by its number, 1 to 6, and the
    * face's printed name. */
  private val foundations: Vector[((Int, String), ArtKey)] = Vector(
    1 -> "Imperial Maps", 1 -> "Wide Horizons",
    2 -> "Powerful Tribes", 2 -> "Movers and Shakers",
    3 -> "Quiet Ambitions", 3 -> "Public Ambitions",
    4 -> "Ties of Fellowship", 4 -> "Teeming World",
    5 -> "Mob's Favor", 5 -> "Grand Council",
    6 -> "Wandering Flame", 6 -> "Festival")
    .map((number, name) => (number, name) -> ArtKey(
      s"cards/Foundation ${FoundationNumerals(number - 1)} - $name (NF).png"))

  /** Each Oath goal card, by the projection's goal key. The files are
    * not named for the card's title; the card data names them. */
  private val oathGoals: Vector[(String, ArtKey)] = Vector(
    "supremacy" -> "Supremacy Oath", "protection" -> "Protection Oath",
    "devotion" -> "Devotion Oath", "the-people" -> "People Oath")
    .map((goal, name) => goal -> ArtKey(s"cards/$name (NF).png"))

  /** The mat the World pane is laid out on. */
  val mat: ArtKey = ArtKey("tts/mat/mat.jpg")

  def banner(face: String): Option[ArtKey] = banners.toMap.get(face)
  def foundation(number: Int, name: String): Option[ArtKey] =
    foundations.toMap.get((number, name))
  def oathGoal(goal: String): Option[ArtKey] = oathGoals.toMap.get(goal)

  /** Every card with a "(NF)" file. */
  private val reprinted: Vector[String] =
    reprintedDenizens ++ relics ++ edificeFaces ++ legacies ++ visions

  /** Each card's file: the New Foundations file when the set has one,
    * else the base game's (implementation ruling R1). */
  private val facePaths: Vector[(String, String)] =
    reprinted.map(name => name -> s"cards/$name (NF).png") ++
      baseDenizens.map(name => name -> s"cards/$name.png") ++
      sites.map(name => name -> s"cards/$name (NF).png")

  private val faces: Map[String, ArtKey] =
    facePaths.map((name, path) => name -> ArtKey(path)).toMap

  /** The back of each kind of card a viewer may be unable to identify, by
    * the projection's card kind. */
  private val backs: Map[String, ArtKey] = Map(
    "denizen" -> ArtKey("tts/backs/denizen-back.jpg"),
    "vision" -> ArtKey("tts/backs/vision-back.jpg"),
    "relic" -> ArtKey("tts/backs/relic-back.jpg"),
    "legacy" -> ArtKey("tts/backs/legacy-back.jpg"))

  /** The printed symbol of each sprite token (implementation rulings R23,
    * R24): what a card prints in its text. The warband has none. */
  private val symbols: Vector[(String, ArtKey)] = Vector(
    "favor" -> "favor", "favor-burnt" -> "favorb", "secret" -> "secret",
    "secret-burnt" -> "secretb", "suit-arcane" -> "suit-arcane",
    "suit-beast" -> "suit-beast", "suit-discord" -> "suit-discord",
    "suit-hearth" -> "suit-hearth", "suit-nomad" -> "suit-nomad",
    "suit-order" -> "suit-order", "attack-die" -> "dicer",
    "defense-die" -> "diceb", "round-die" -> "dicep", "skull" -> "skull",
    "shield" -> "shield", "sword" -> "sword", "hollow-sword" -> "swordh")
    .map((token, file) => token -> ArtKey(s"symbols/symbol-$file.png"))

  /** The banner a card's restriction hangs over its face, by the mark's
    * kind; an edifice's towers have no symbol (R32). */
  private val restrictions: Vector[(String, ArtKey)] = Vector(
    "adviser" -> ArtKey("symbols/symbol-restrictionperson.png"),
    "site" -> ArtKey("symbols/symbol-restrictiontree.png"))

  /** A token as it lies in play (R23). */
  private val tokens: Vector[(String, ArtKey)] = Vector(
    "favor" -> ArtKey("tts/tokens/favor.png"),
    "secret" -> ArtKey("tts/tokens/secret.png"))

  /** The player colours, by `PlayerColor`'s keys, which this module may
    * not name; `ArtCatalogSuite` holds the two lists equal. */
  private val colours = Vector("purple", "red", "blue", "yellow", "white",
    "black", "pink", "brown")
  private val pawns: Vector[(String, ArtKey)] =
    colours.map(colour => colour -> ArtKey(s"tts/pieces/pawn-$colour.png"))
  private val warbands: Vector[(String, ArtKey)] =
    colours.map(colour => colour -> ArtKey(s"tts/pieces/warband-$colour.png"))
  private val banditWarband = ArtKey("tts/tokens/bandit.png")

  /** The marker on the Visions Drawn track. */
  val visionsDrawnMarker: ArtKey =
    ArtKey("tts/markers/visions-drawn-marker.png")
  /** The festival marker, as the overlay shows it whole (R29). */
  val festivalMarker: ArtKey = ArtKey("tts/tokens/festival-underway.jpg")
  /** The Usurper's mark on the round wheel (R33). */
  val usurperMarker: ArtKey = ArtKey("tts/tokens/usurper.png")

  private val titles: Vector[(String, ArtKey)] = Vector("oathkeeper", "usurper")
    .map(side => side -> ArtKey(s"tts/titles/$side.jpg"))

  /** Each die's sheet of faces, by the die's key. */
  private val dieSheets: Vector[(String, ArtKey)] = Vector(
    "attack" -> ArtKey("tts/attack-die/attack_die.png"),
    "defense" -> ArtKey("tts/defense-die/defense_die.png"))

  /** Each die face's square on its die's sheet, by the face's wire key.
    * Both sheets are 2048×2048 and lay six sides out three by two in
    * their lower two thirds; a face printed on several sides is read
    * from one. The numbers are recorded, with how they were measured, in
    * `docs/catalog/asset-proportions.md`. */
  private val dieCrops: Vector[(String, DieCrop)] =
    def face(die: String, x: Int, y: Int, side: Int): DieCrop =
      DieCrop(dieSheets.toMap.apply(die), x, y, side, side, 2048, 2048)
    Vector(
      "two-swords-skull" -> face("attack", 14, 702, 638),
      "hollow-sword" -> face("attack", 698, 702, 638),
      "one-sword" -> face("attack", 1392, 702, 638),
      "blank" -> face("defense", 10, 707, 642),
      "one-shield" -> face("defense", 700, 707, 642),
      "two-shields" -> face("defense", 10, 1387, 642),
      "doubler" -> face("defense", 1391, 1387, 642))

  def dieFace(key: String): Option[DieCrop] = dieCrops.toMap.get(key)

  def symbol(token: String): Option[ArtKey] = symbols.toMap.get(token)
  def restriction(kind: String): Option[ArtKey] = restrictions.toMap.get(kind)
  def token(token: String): Option[ArtKey] = tokens.toMap.get(token)
  def pawn(colour: String): Option[ArtKey] = pawns.toMap.get(colour)
  /** A force's warband: a player's colour, "empire" or "bandit" (R26). */
  def warband(force: String): Option[ArtKey] = force match
    case "empire" => warbands.toMap.get("purple")
    case "bandit" => Some(banditWarband)
    case colour => warbands.toMap.get(colour)
  def title(side: String): Option[ArtKey] = titles.toMap.get(side)
  def dieSheet(die: String): Option[ArtKey] = dieSheets.toMap.get(die)

  /** The face of the card printed `name`, when the folder may hold one. */
  def cardFace(name: String): Option[ArtKey] = faces.get(name)

  /** The back of a card of `cardKind`, when the folder may hold one. */
  def cardBack(cardKind: String): Option[ArtKey] = backs.get(cardKind)

  /** Every path the client can ask for, each once: the server's allowlist
    * and the list in `docs/operations/game-art.md`. */
  val allPaths: Vector[String] =
    (reprinted.map(name => s"cards/$name (NF).png") ++
      baseDenizens.map(name => s"cards/$name.png") ++
      Vector("denizen", "vision", "relic", "legacy")
        .map(kind => backs(kind).path)) ++
      sites.map(name => s"cards/$name (NF).png") ++
      banners.map(_._2.path) ++ foundations.map(_._2.path) ++
      oathGoals.map(_._2.path) ++ Vector(mat.path) ++
      symbols.map(_._2.path) ++ restrictions.map(_._2.path) ++
      tokens.map(_._2.path) ++ pawns.map(_._2.path) ++
      warbands.map(_._2.path) ++ Vector(banditWarband.path,
        visionsDrawnMarker.path, festivalMarker.path, usurperMarker.path) ++
      titles.map(_._2.path) ++ dieSheets.map(_._2.path)
