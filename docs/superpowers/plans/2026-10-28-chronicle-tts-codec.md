# Chronicle TTS Codec Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Read and write the Oath NF Tabletop Simulator (TTS) Chronicle export string on the JVM.

**Architecture:** Two layers in `application/chronicle/`.
- `TtsWire` is an exact port of harsch's `chronicle-codec.js`. It turns a string into `TtsChronicle`, a value holding TTS numbers, and back, with the same alphabet, scramble, checksum and packing.
- `ChronicleTtsCodec` maps `TtsChronicle` to and from our `Chronicle` through `TtsIds`, fills the fields TTS lacks with defaults, and lists what an export leaves out.

`LineageState.startingRelics` lands first, because the TTS player section has a field for it.

**Tech Stack:** Scala 3, munit, ujson (test resource only).

**Spec:** `docs/superpowers/specs/2026-10-28-chronicle-tts-codec-design.md`

## Global Constraints

- **Source:** the format is `chronicle-codec.js` at harsch1/oath-nf-tts-scripts commit `f6051bb0` (2026-10-04, encoding version 2).
- **Versions:** decode versions 2 and 3; the public encoder writes version 3 only.
- **Version 3 layout:** one digit after the Foundations bitmask for the current Oath.
  - 0 means unset;
  - 1 Supremacy, 2 Devotion, 3 The People, 4 Protection;
  - 5-20 are reserved and 21+ are fan content, and both refuse.
- **Import defaults:** Oathkeeper goal Supremacy, era target 20 with no scores, no previous players, no Foundation alteration sources.
- **Errors:** an unknown TTS id refuses the import and names the number.
- **Game start:** refuses a non-empty `startingRelics`. The card accounting counts those relics as stored relics.
- **Build rules:**
  - The 800-line file cap, `-Werror`, and the architecture check (`python3 scripts/check-architecture.py`).
  - Coverage of at least 92% statements (`./sbtw clean coverage test coverageReport`).
- **Docs:** written in plain, normal prose.
- **Commits:**
  - End every commit with `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`.
  - Never `git add -A`; add files by name.
- **Branch:** work in a worktree off `0.2.0-alpha`, and merge back into `0.2.0-alpha` only when the user says so.

**Deviation from the spec, agreed in the plan:** `Chronicle` has no `name` until browsing slice 1. So `decode` takes only the text here; browsing slice 1 adds the `name` parameter and the "the name" entry in the omitted list. Task 4 Step 6 edits the spec's Components line to match.

---

### Task 1: `LineageState.startingRelics`

**Files:**
- Modify: `src/main/scala/oathdigital/model/GameState.scala:29-35`
- Modify: `src/main/scala/oathdigital/serialization/GameEventJsonSupport.scala:127-149`
- Modify: `src/main/scala/oathdigital/gameplay/setup/ChronicleAccounting.scala`
- Modify: `src/main/scala/oathdigital/gameplay/setup/GameStartRules.scala:62-65`
- Modify: `src/main/scala/oathdigital/gameplay/chronicle/ChronicleRecord.scala:41-42`
- Test: `src/test/scala/oathdigital/gameplay/setup/ChronicleAccountingSuite.scala`, `src/test/scala/oathdigital/gameplay/setup/GameStartRulesSuite.scala`, `src/test/scala/oathdigital/serialization/GameEventFactsWireSuite.scala`

**Interfaces:**
- Produces: `LineageState(id, previousPlayer, role, legacies, startingAdvisers, startingRelics: Vector[RelicId] = Vector.empty)`.

- [ ] **Step 1: Write the failing tests**

In `ChronicleAccountingSuite`, add next to `lineageAdvising`:

```scala
  private def lineageKeeping(relic: RelicId): LineageState =
    LineageState(LineageId("lineage:any"), None, Role.Exile, Vector.empty,
      Vector.empty, startingRelics = Vector(relic))

  test("a starting relic also in the relic deck is stored twice"):
    val relic = chronicle.relicDeck.head
    val kept = chronicle.copy(lineages = Vector(lineageKeeping(relic)))
    assertEquals(ChronicleAccounting.knownAndUnique(catalog, kept),
      Left(s"relic ${relic.value} is stored twice"))

  test("a relic moved from the relic deck to a starting relic is conserved"):
    val relic = chronicle.relicDeck.head
    val moved = chronicle.copy(relicDeck = chronicle.relicDeck.tail,
      lineages = Vector(lineageKeeping(relic)))
    assertEquals(ChronicleAccounting.conserves(chronicle, moved), Right(()))
```

In `GameStartRulesSuite`, after "game start keeps the whole Chronicle":

```scala
  test("game start refuses a lineage's starting relics until Legacies"):
    val relic = chronicle.relicDeck.head
    val keeping = chronicle.copy(relicDeck = chronicle.relicDeck.tail,
      lineages = Vector(LineageState(LineageId("l9"), None, Role.Exile,
        Vector.empty, Vector.empty, startingRelics = Vector(relic))))
    assertEquals(GameStartRules.evolve(catalog, keeping, orders),
      Left(OathViolation.UnsupportedChronicle(
        "starting relics require Legacies")))
```

In `GameEventFactsWireSuite`, find the lineage-role test at line 60, and in the `LineageState(` it builds at line 64 set `startingRelics = Vector(RelicId("R01"))` on the first lineage, so the round trip covers the field. If that test builds every lineage the same way, add the field only where `index == 0`:

```scala
      roles.zipWithIndex.map((role, index) => LineageState(
        /* existing arguments unchanged */,
        startingRelics = if index == 0 then Vector(RelicId("R01")) else Vector.empty))
```

- [ ] **Step 2: Run them and see them fail**

Run: `./sbtw "testOnly oathdigital.gameplay.setup.ChronicleAccountingSuite oathdigital.gameplay.setup.GameStartRulesSuite oathdigital.serialization.GameEventFactsWireSuite"`
Expected: compile error, `startingRelics` is not a member of `LineageState`.

- [ ] **Step 3: Add the field, its wire form, the accounting and the refusal**

`GameState.scala`:

```scala
final case class LineageState(
    id: LineageId,
    previousPlayer: Option[PlayerId],
    role: Role,
    legacies: Vector[LegacyState],
    startingAdvisers: Vector[AdviserState],
    /** Relics the lineage keeps between games, as the TTS player section
      * stores them. Nothing creates them until Legacies. */
    startingRelics: Vector[RelicId] = Vector.empty
)
```

`GameEventJsonSupport.encodeLineageState`: add after `"startingAdvisers"`:

```scala
      "startingRelics" -> stringArray(lineage.startingRelics.map(_.value)))
```

(move the closing parenthesis accordingly). `decodeLineageState`: read it as required, since old journals need not decode:

```scala
    startingRelics = value("startingRelics").arr.toVector.map(r => RelicId(r.str))
  yield LineageState(LineageId(value("id").str),
    value("previousPlayer") match {
      case ujson.Null => None
      case id => Some(PlayerId(id.str))
    }, role, legacies, startingAdvisers, startingRelics)
```

If `stringArray` is not visible in that trait, use `ujson.Arr.from(lineage.startingRelics.map(r => ujson.Str(r.value)))`.

`ChronicleAccounting.knownAndUnique`: count kept relics with the stored ones:

```scala
    val advisers = chronicle.lineages.flatMap(_.startingAdvisers).map(_.id)
    val keptRelics = chronicle.lineages.flatMap(_.startingRelics)
    val stored = items ++ advisers ++ keptRelics
```

`ChronicleAccounting.conserves`: in `relics(chronicle)`, add `++ chronicle.lineages.flatMap(_.startingRelics)`.

`GameStartRules.buildPlan`: right after the `world` check:

```scala
      _ <- Either.cond(chronicle.lineages.forall(_.startingRelics.isEmpty), (),
        UnsupportedChronicle("starting relics require Legacies"))
```

`ChronicleRecord.scala:41-42`: the new-lineage branch builds `LineageState(live.id, seatedBy.get(live.id), live.role, Vector.empty, live.startingAdvisers)`. Pass `live.startingRelics` as the sixth argument so the record keeps them.

Update the scaladoc of `ChronicleAccounting.knownAndUnique` if it lists the fields it reads.

- [ ] **Step 4: Run the tests**

Run: `./sbtw "testOnly oathdigital.gameplay.setup.* oathdigital.serialization.* oathdigital.gameplay.chronicle.*"`
Expected: all pass.

- [ ] **Step 5: Commit**

```bash
git add src/main/scala/oathdigital/model/GameState.scala src/main/scala/oathdigital/serialization/GameEventJsonSupport.scala src/main/scala/oathdigital/gameplay/setup/ChronicleAccounting.scala src/main/scala/oathdigital/gameplay/setup/GameStartRules.scala src/main/scala/oathdigital/gameplay/chronicle/ChronicleRecord.scala src/test/scala/oathdigital/gameplay/setup/ChronicleAccountingSuite.scala src/test/scala/oathdigital/gameplay/setup/GameStartRulesSuite.scala src/test/scala/oathdigital/serialization/GameEventFactsWireSuite.scala
git commit -m "feat(chronicle): a lineage keeps starting relics, which game start refuses until Legacies

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 2: `TtsIds`

**Files:**
- Create: `src/main/scala/oathdigital/application/chronicle/TtsIds.scala`
- Create: `src/test/resources/tts/chronicle-codec-mapping.json`
- Test: `src/test/scala/oathdigital/application/chronicle/TtsIdsSuite.scala`

**Interfaces:**
- Produces:
  - `final class TtsIds(catalog: ExecutableCatalog)` with:
    - `card(tts: Int): Either[String, CardId]`
    - `ttsCard(id: CardId): Either[String, Int]`
    - `site(index: Int): Either[String, SiteId]`
    - `ttsSite(id: SiteId): Either[String, Int]`
    - `legacy(tts: Int): Either[String, LegacyId]`
    - `ttsLegacy(id: LegacyId): Either[String, Int]`
  - `object TtsIds { val sites: Vector[SiteId] }`, the sites in TTS index order.

Facts checked when this plan was written:
- TTS deck cards 1-258 (with 94, 110 and 174 absent), relics 401-447 and edifices 501-530 all carry the same names as our `DenizenId(n)`, `R(n-400)` and `E(n-500)` (the intact face name).
- TTS legacies 1-36 match `L01`-`L36`.
- The 24 site indices are our sites in alphabetical name order. Ancient City is 0, Buried Giant 2 and Hidden Place 11, and each of those has aliases.

- [ ] **Step 1: Save the mapping resource**

Copy the JS file's `MAPPING` tables, pinned to the commit, into a JSON test resource:

```bash
mkdir -p src/test/resources/tts
curl -sfL https://raw.githubusercontent.com/harsch1/oath-nf-tts-scripts/f6051bb0/chronicle-codec.js -o /tmp/chronicle-codec.js
python3 -I - /tmp/chronicle-codec.js src/test/resources/tts/chronicle-codec-mapping.json <<'EOF'
import json, re, sys
js = open(sys.argv[1]).read()
body = js.split('const MAPPING = {')[1]
def table(name, end):
    block = body.split(name + ': {')[1].split(end)[0]
    return block
ids = {int(a): json.loads('"' + b + '"') for a, b in
       re.findall(r'^\s+(\d+):\s*"((?:[^"\\]|\\.)*)"', table('ids', 'legacies: {'), re.M)}
legacies = {int(a): b for a, b in
            re.findall(r'^\s+(\d+):\s*"([^"]*)"', table('legacies', '}'), re.M)}
sites = [[n, int(i)] for n, i in
         re.findall(r'^\s+"([^"]+)":\s*(\d+),', table('sites', '}'), re.M)]
out = {"source": "harsch1/oath-nf-tts-scripts chronicle-codec.js @ f6051bb0",
       "ids": {str(k): v for k, v in sorted(ids.items())},
       "legacies": {str(k): v for k, v in sorted(legacies.items())},
       "sites": sites}
json.dump(out, open(sys.argv[2], 'w'), indent=1, ensure_ascii=False)
print(len(ids), len(legacies), len(sites))
EOF
```

Expected output: `332 36 29` (29 site names over 24 indices). Look at the file: `ids` keys 1-530, `sites` in file order with the canonical name first for each index.

- [ ] **Step 2: Write the failing test**

```scala
package oathdigital.application.chronicle

import oathdigital.gameplay.setup.FirstGameSetupFixture
import oathdigital.model._

/** The TTS id tables against the TTS codec's own name tables (resource
  * copied from chronicle-codec.js at f6051bb0) and the catalog's names. A
  * renumbering on either side fails here. */
class TtsIdsSuite extends munit.FunSuite:
  private val catalog = FirstGameSetupFixture.catalog
  private val ids = TtsIds(catalog)
  private val mapping = ujson.read(scala.io.Source.fromResource(
    "tts/chronicle-codec-mapping.json").mkString)

  private def catalogName(id: CardId): Option[String] = id match
    case d: DenizenId => catalog.denizen(d).map(_.name)
    case r: RelicId => catalog.relic(r).map(_.name)
    case e: EdificeId => catalog.edifice(e).map(_.intact.name)
    case _ => None

  test("every TTS card id names the catalog card with the same name"):
    val wrong = mapping("ids").obj.toVector.flatMap((number, name) =>
      val found = ids.card(number.toInt).toOption.flatMap(catalogName)
      Option.when(!found.contains(name.str))(s"$number ${name.str}: $found"))
    assertEquals(wrong, Vector.empty)

  test("every TTS legacy id names the catalog legacy with the same name"):
    val wrong = mapping("legacies").obj.toVector.flatMap((number, name) =>
      val found = ids.legacy(number.toInt).toOption
        .flatMap(catalog.legacy).map(_.name)
      Option.when(!found.contains(name.str))(s"$number ${name.str}: $found"))
    assertEquals(wrong, Vector.empty)

  test("each TTS site index is the catalog site with its canonical name"):
    val canonical = mapping("sites").arr.toVector
      .map(pair => pair(1).num.toInt -> pair(0).str)
      .distinctBy(_._1)
    assertEquals(canonical.map(_._1), Vector.range(0, 24))
    canonical.foreach((index, name) =>
      assertEquals(ids.site(index).toOption.flatMap(catalog.site).map(_.name),
        Some(name), index))

  test("ids map back to the same TTS numbers"):
    val cards = mapping("ids").obj.keys.map(_.toInt).toVector
    cards.foreach(n =>
      assertEquals(ids.card(n).flatMap(ids.ttsCard), Right(n), n))
    Vector.range(0, 24).foreach(i =>
      assertEquals(ids.site(i).flatMap(ids.ttsSite), Right(i), i))
    Vector.range(1, 37).foreach(n =>
      assertEquals(ids.legacy(n).flatMap(ids.ttsLegacy), Right(n), n))

  test("a number the catalog lacks is refused by number"):
    assertEquals(ids.card(94), Left("TTS card 94 is not in the catalog"))
    assertEquals(ids.card(600), Left("TTS card 600 is not in the catalog"))
    assertEquals(ids.site(24), Left("TTS site 24 is not in the catalog"))
    assertEquals(ids.legacy(37), Left("TTS legacy 37 is not in the catalog"))
```

If `catalog.denizen(DenizenId("94"))` exists in our catalog, replace 94 with another number the catalog lacks, such as 399.

- [ ] **Step 3: Run it and see it fail**

Run: `./sbtw "testOnly oathdigital.application.chronicle.TtsIdsSuite"`
Expected: compile error, `TtsIds` not found.

- [ ] **Step 4: Write `TtsIds`**

```scala
package oathdigital.application.chronicle

import oathdigital.catalog.ExecutableCatalog
import oathdigital.model._

/** TTS Chronicle numbers for catalog ids (harsch1/oath-nf-tts-scripts
  * chronicle-codec.js at f6051bb0). Deck cards are their printed number,
  * relics 400 plus their number, edifices 500 plus theirs, and legacies their
  * number. Sites are an index; both faces of a site share it. A number is
  * accepted only for a card the catalog holds. */
final class TtsIds(catalog: ExecutableCatalog):
  def card(tts: Int): Either[String, CardId] =
    val id: Option[CardId] =
      if tts >= 1 && tts < 400 then Some(DenizenId(tts.toString))
        .filter(d => catalog.denizen(d).nonEmpty)
      else if tts > 400 && tts < 500 then Some(RelicId(f"R${tts - 400}%02d"))
        .filter(r => catalog.relic(r).nonEmpty)
      else if tts > 500 && tts < 600 then Some(EdificeId(f"E${tts - 500}%02d"))
        .filter(e => catalog.edifice(e).nonEmpty)
      else None
    id.toRight(s"TTS card $tts is not in the catalog")

  def ttsCard(id: CardId): Either[String, Int] = id match
    case DenizenId(n) => n.toIntOption.filter(v => v >= 1 && v < 400)
      .toRight(s"denizen $n has no TTS number")
    case RelicId(r) => numbered(r, "R").map(400 + _)
      .toRight(s"relic $r has no TTS number")
    case EdificeId(e) => numbered(e, "E").map(500 + _)
      .toRight(s"edifice $e has no TTS number")
    case other => Left(s"${other.value} has no TTS number")

  def site(index: Int): Either[String, SiteId] =
    TtsIds.sites.lift(index).toRight(s"TTS site $index is not in the catalog")

  def ttsSite(id: SiteId): Either[String, Int] =
    Some(TtsIds.sites.indexOf(id)).filter(_ >= 0)
      .toRight(s"site ${id.value} has no TTS index")

  def legacy(tts: Int): Either[String, LegacyId] =
    Some(LegacyId(f"L$tts%02d")).filter(l => tts >= 1 &&
      catalog.legacy(l).nonEmpty).toRight(s"TTS legacy $tts is not in the catalog")

  def ttsLegacy(id: LegacyId): Either[String, Int] =
    numbered(id.value, "L").toRight(s"legacy ${id.value} has no TTS number")

  private def numbered(value: String, prefix: String): Option[Int] =
    Option.when(value.startsWith(prefix))(value.drop(prefix.length))
      .flatMap(_.toIntOption).filter(_ > 0)

object TtsIds:
  /** The sites in TTS index order: alphabetical by name. */
  val sites: Vector[SiteId] = Vector(
    "ancient-city", "broken-peaks", "buried-giant", "deep-woods",
    "desolate-shore", "dunes", "fair-isle", "golden-valley", "great-slum",
    "green-shore", "headwaters", "hidden-place", "mines", "narrow-pass",
    "painted-towers", "riverbank", "rocky-coast", "salt-flats",
    "shrouded-woods", "solitary-pillar", "standing-stones", "steppe",
    "sunken-isles", "tidal-marshes").map(name => SiteId(s"site:$name"))
```

Check the extractor shapes before relying on them. `DenizenId(n)`, `RelicId(r)` and `EdificeId(e)` must be pattern-matchable, as `CatalogNames.nameOf` uses them. `CardId` must have `.value`. If `LegacyId`'s value accessor is not `.value`, use what `ChronicleAccounting` uses. The architecture check allows `application` to import `catalog`; `FirstGameChronicleGenerator` already does.

- [ ] **Step 5: Run the tests**

Run: `./sbtw "testOnly oathdigital.application.chronicle.TtsIdsSuite"`
Expected: PASS. If a name test fails, the message lists each mismatch. Fix the table only if the catalog agrees with the TTS name; otherwise stop and report the mismatch to the user.

- [ ] **Step 6: Commit**

```bash
git add src/main/scala/oathdigital/application/chronicle/TtsIds.scala src/test/resources/tts/chronicle-codec-mapping.json src/test/scala/oathdigital/application/chronicle/TtsIdsSuite.scala
git commit -m "feat(chronicle): map TTS Chronicle numbers to catalog ids, checked against the TTS codec's names

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 3: `TtsWire`, the exact port

**Files:**
- Create: `src/main/scala/oathdigital/application/chronicle/TtsWire.scala`
- Test: `src/test/scala/oathdigital/application/chronicle/TtsWireSuite.scala`

**Interfaces:**
- Produces:

```scala
final case class TtsSite(index: Int, items: Vector[Int])
final case class TtsLegacy(id: Int, flipped: Boolean)
final case class TtsPlayer(status: Int, cards: Vector[Int],
    legacies: Vector[TtsLegacy], relics: Vector[Int])
/** Every section that the string held, in order; absent trailing
  * sections are None. Card ids are full TTS ids (relics 401-499). */
final case class TtsChronicle(
    atlasBox: Option[Vector[TtsSite]] = None,
    world: Option[Vector[TtsSite]] = None,
    worldDeck: Option[Vector[Int]] = None,
    relicDeck: Option[Vector[Int]] = None,
    dispossessed: Option[Vector[Int]] = None,
    reliquary: Option[Vector[Int]] = None,
    foundations: Option[TtsFoundations] = None,
    players: Vector[Option[TtsPlayer]] = Vector.fill(7)(None))
final case class TtsFoundations(mask: Int, goal: Option[Int])
object TtsWire:
  val colors: Vector[String]  // red, blue, yellow, black, white, brown, pink
  def decodePlain(plain: String, version: Int): Either[String, TtsChronicle]
  def encodePlain(chronicle: TtsChronicle, version: Int): Either[String, String]
  def decodeExport(text: String): Either[String, (Int, TtsChronicle)]
  private[chronicle] def encodeExport(chronicle: TtsChronicle, version: Int): Either[String, String]
```

Port notes, each a rule from the JS file:
- **Numbers.** JS numbers are exact up to 2^53 - 1; use `Long` and refuse a value above that, as `toBase82` and `fromBase82` do.
- **Site tokens.** `index + item1*100 + item2*100000 + item3*100000000`, each token ending in `#`. An empty section is no sites.
- **Decks.** Pack in chunks of 5 ids, each chunk padded to 7 characters, with the first chunk rightmost. Card decks use radix 400 and ids 1-399. The relic deck uses radix 100 over `id - 400`. Unpacking reads the chunks right to left and stops a chunk at zero.
- **Small lists.** One number (`packNumber`), written as `""` when empty. The Reliquary, player cards and player relics use this. Relic lists are stored as `id - 400`.
- **Legacies.** The value is `id + 100` when flipped, radix 200. Fewer than 5 are one number; 5 or more are chunked like a deck. Decode tells the two apart by length: under 7 characters is one number.
- **Foundations.** In version 2, `toBase82(mask)`; an empty section reads as 0. In version 3, `toBase82(mask)` followed by one character for the goal digit (`goal.getOrElse(0)`). Decoding version 3 reads 0 characters as mask 0 with no goal, 1 as the mask alone, and 2 as the mask and goal; any other length refuses.
- **Players.** `[status digit][cards]#[legacies]#[relics]`. A status outside 0-2 refuses.
- **Sections.** Split on `>`; more than 14 sections refuses. Encoding writes sections up to the last present one, and an absent one before that is empty.
- **Wrapper.** `padEncode(version, 2) + scramble(plain) + padEncode(checksum(plain), 2)`; whitespace is ignored and under 5 characters refuses. The scramble starts from seed 468529063 over the alphabet plus `#>`. Each character steps `seed = seed * 16807 % 2147483647`, shifts by the seed modulo 84, then adds the true index to the seed modulo 2147483647. The checksum is the sum of `(i + 1) * index`, modulo 82*82.
- **Error messages.** Keep the JS wording: `"Checksum failure, the export string is likely corrupted"`, `"Export string is too short"`, and `"This export string uses encoding version N, which is not supported"` for a version other than 2 or 3.

- [ ] **Step 1: Write the failing tests**

```scala
package oathdigital.application.chronicle

/** The TTS string layer against the test strings of chronicle-codec.js at
  * f6051bb0, made by the Lua exporter in TTS. */
class TtsWireSuite extends munit.FunSuite:
  private val plain =
    "M#5#F#7f,#6#66)#7#8#K#623#:n@-#7sK#1#0#)oy~#6Bu#6f6#L#6cv#,&CK#3#6M/#>" +
      "D&1?8w#8U{9#>" +
      "7+-x;V08oZ8huHKQPKOQ4J)es4@aDMR,5JBHv,5)~EF/OybDN2D5rps-D;1311dFNUBqTQ6L~;A5a>" +
      "00yuz%g00g:LA100{N:i(01IG=nz00il1!!012G;R2>" +
      "00005QqFZ:dW^&Hg{t!5-4ONLOau9x/yb37AIM2pZqK@pO3p00/c17BaBG-@qHk"
  private val luaPlain =
    "M#5#F#7f,#6#66)#7#8#K#623#:n@-#7sK#1#0#)oy~#6Bu#6f6#L#6cv#,&CK#3#6M/#>" +
      "D&1?8w#8U{9#>" +
      "7+-x;V08oZ8huHKQPKOQ4J)es4@aDMR,5JBHv,5)~EF/OybDN2D5rps-D;1311dFNUBqTQ6L~;A5a>" +
      "00wjXpz00,,9Hd00cGcDm00aVgF=00F?%0}00i%s(Z>" +
      "00005QqFZ:dW^&Hg{t!5-4ONLOau9x/yb37AIM2pZqK@pO3p00/c17BaBG-@qHk>"
  private val luaExport = "02H3uk5&m#lvuYL;KIO:Mg9FjOs)5.yU!bk{6k9tUwlHwZ}z%uTY3.}IvkwdiP9.r)s-g,XAxa!)JRjGCc5ze$;93~2=J;chc)06a}66Pn:tI#vWs~QDbnK5PpZ#&Xq36lcTLE-g359{rq/DBYLh@RBb~R;?$:iMT},~=/N@)J+fh9Ip8T1#dMl?Gf:M?tM%r/8%cL,lF{RGZOorax@0pONlP@9O.&SmJ>G6bq2t^7FJFmI>W$a,3r4:&ohBXUX)$&d%DuAJl%i~Y,:P"
  private val fullExport = "02H3uk5&m#lvuYL;KIO:Mg9FjOs)5.yU!bk{6k9tUwlHwZ}z%uTY3.}IvkwdiP9.r)s-g,XAxa!)JRjGCc5ze$;93~2=J;chc)06a}66Pn:tI#vWs~QDbnK5PpZ#&Xq36lcTLE-g359{rq/DBYLh@RBb~R;?$:iMT},~=/N@)J+fh9Ip8T1#dMl?Gf:M?tM%r/8%cL,lF{RGZOorax@0pONlP@9O.&SmJ>G6bq2t^7FJFmI>W$a,3r4:&ohBXUX)$&d%DuAJl%i~Y,c)@b!/eoUQ9Qs!w223^(:%Lf.{eA5@(1u^DqWE$Kv#=iyHXC/5T"

  private def decoded(text: String, version: Int = 2)(using munit.Location)
      : TtsChronicle =
    TtsWire.decodePlain(text, version).fold(fail(_), identity)

  test("the exporter's plain string decodes and re-encodes exactly"):
    val chronicle = decoded(plain)
    assertEquals(chronicle.worldDeck.map(_.size), Some(55))
    assertEquals(TtsWire.encodePlain(chronicle, 2), Right(plain))

  test("the Lua pair: the plain string scrambles to the exporter's string"):
    val chronicle = decoded(luaPlain)
    assertEquals(chronicle.reliquary, Some(Vector.empty))
    assertEquals(TtsWire.encodePlain(chronicle, 2), Right(luaPlain))
    assertEquals(TtsWire.encodeExport(chronicle, 2), Right(luaExport))
    assertEquals(TtsWire.decodeExport(luaExport), Right((2, chronicle)))

  test("the full export re-encodes to the same string"):
    val (version, chronicle) = TtsWire.decodeExport(fullExport)
      .fold(fail(_), identity)
    assertEquals(version, 2)
    assertEquals(chronicle.foundations, Some(TtsFoundations(8, None)))
    assertEquals(chronicle.players(2).map(_.status), Some(2))
    assertEquals(TtsWire.encodeExport(chronicle, 2), Right(fullExport))

  test("whitespace around and inside the string is ignored"):
    assertEquals(TtsWire.decodeExport(s"\n $luaExport \n").map(_._2),
      Right(decoded(luaPlain)))

  test("a changed character fails the checksum"):
    val bad = luaExport.take(10) +
      (if luaExport(10) == 'a' then 'b' else 'a') + luaExport.drop(11)
    assertEquals(TtsWire.decodeExport(bad),
      Left("Checksum failure, the export string is likely corrupted"))

  test("an unknown version and a short string are refused"):
    assertEquals(TtsWire.decodeExport("01" + luaExport.drop(2)),
      Left("This export string uses encoding version 1, which is not supported"))
    assertEquals(TtsWire.decodeExport("02ab"), Left("Export string is too short"))

  test("a skipped and an empty Reliquary both read as no relics"):
    assertEquals(decoded(plain + ">").reliquary, Some(Vector.empty))
    assertEquals(decoded(plain + ">0").reliquary, Some(Vector.empty))

  test("legacies: under five are one number, five or more are chunked"):
    val legacies = Vector(1, 2, 3, 4, 5, 36).zipWithIndex
      .map((id, i) => TtsLegacy(id, flipped = i % 2 == 1))
    for count <- Vector(0, 1, 4, 5, 6) do
      val player = TtsPlayer(1, Vector(120), legacies.take(count), Vector(447))
      val chronicle = TtsChronicle(players = Vector.fill(7)(Some(player)))
      val text = TtsWire.encodePlain(chronicle, 2).fold(fail(_), identity)
      val field = text.split(">", -1).last.split("#", -1)(1)
      assert(if count < 5 then field.length <= 5 else field.length % 7 == 0,
        s"$count: $field")
      assertEquals(decoded(text).players.last, Some(player), count)

  test("seven starting cards cannot be encoded exactly"):
    val player = TtsPlayer(0, Vector.fill(7)(120), Vector.empty, Vector.empty)
    assert(TtsWire.encodePlain(TtsChronicle(players = Vector(Some(player)) ++
      Vector.fill(6)(None)), 2).isLeft)

  test("every Foundations mask round trips, with a version 3 goal digit"):
    for mask <- 0 until 64; goal <- Vector(None, Some(0), Some(4), Some(21)) do
      val chronicle = TtsChronicle(foundations = Some(TtsFoundations(mask, goal)))
      val text = TtsWire.encodePlain(chronicle, 3).fold(fail(_), identity)
      assertEquals(decoded(text, 3).foundations,
        Some(TtsFoundations(mask, Some(goal.getOrElse(0)))), (mask, goal))

  test("version 3 reads a Foundations section of 0, 1 or 2 characters"):
    assertEquals(decoded(">>>>>>", 3).foundations,
      Some(TtsFoundations(0, None)))
    assertEquals(decoded(">>>>>>8", 3).foundations,
      Some(TtsFoundations(8, None)))
    assert(TtsWire.decodePlain(">>>>>>812", 3).isLeft)
```

Before relying on the masks in the full-export test, check two values against the JS file's own test. Teeming World is Foundation IV, so its mask is 8. The yellow player is the third section (index 2) and is the Chancellor (status 2).

- [ ] **Step 2: Run it and see it fail**

Run: `./sbtw "testOnly oathdigital.application.chronicle.TtsWireSuite"`
Expected: compile error, `TtsWire` not found.

- [ ] **Step 3: Write `TtsWire`**

Write the file from the port notes above. The JS functions map to Scala methods as follows:

| JS | Scala (`private` in `object TtsWire`) | Returns |
|---|---|---|
| `toBase82` | `toBase82(value: Long)` | `Either[String, String]` |
| `fromBase82` | `fromBase82(text: String)` | `Either[String, Long]` |
| `padEncode` | `padEncode(value: Long, width: Int)` | `Either[String, String]` |
| `scramble` | `scramble(input: String, decoding: Boolean)` | `Either[String, String]` |
| `checksum` | `checksum(text: String)` | `Int` |
| `packIds` / `unpackIds` | `packIds(ids, radix)` / `unpackIds(section, radix)` | `Either[String, String]` / `Either[String, Vector[Int]]` |
| `packNumber` / `unpackNumber` | `packNumber(ids, radix)` / `unpackNumber(text, radix)` | `Either[String, String]` / `Either[String, Vector[Int]]` |
| site, deck, player and foundations codecs | one pair each | |

Build each section's encoder from these. Collect a vector of `Either` with a small `sequence` helper, of the kind `ChronicleExtraction.all` uses. Encode the deck sections and the relic lists with `id - 400` for relics, and add 400 back when decoding. Start the file with this scaladoc:

```scala
/** The Oath NF TTS Chronicle export string (harsch1/oath-nf-tts-scripts
  * chronicle-codec.js at f6051bb0), ported exactly: base-82 numbers, packed
  * sites, decks and lists, a scrambled body between a version and a
  * checksum. This layer knows TTS numbers only; `ChronicleTtsCodec` maps them
  * to catalog ids. Version 3 adds one goal digit after the Foundations
  * bitmask, as its maintainer announced. */
```

Keep the file under 800 lines; at roughly 250 lines it fits.

- [ ] **Step 4: Run the tests**

Run: `./sbtw "testOnly oathdigital.application.chronicle.TtsWireSuite"`
Expected: PASS. If the Lua export test fails while the plain round trip passes, the scramble or checksum differs from the JS. Compare `scramble` step by step with the JS `scramble`, especially the order of the seed updates.

- [ ] **Step 5: Commit**

```bash
git add src/main/scala/oathdigital/application/chronicle/TtsWire.scala src/test/scala/oathdigital/application/chronicle/TtsWireSuite.scala
git commit -m "feat(chronicle): port the TTS Chronicle string format, matching the Lua exporter's strings

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 4: `ChronicleTtsCodec`

**Files:**
- Create: `src/main/scala/oathdigital/application/chronicle/ChronicleTtsCodec.scala`
- Test: `src/test/scala/oathdigital/application/chronicle/ChronicleTtsCodecSuite.scala`
- Modify: `docs/superpowers/specs/2026-10-28-chronicle-tts-codec-design.md` (Components line), `docs/rules/implementation-traceability.md` (row "Full campaign serialization and migration"), `docs/ROADMAP.md` ("Chronicle import and export")

**Interfaces:**
- Consumes: `TtsIds` (Task 2), `TtsWire`, `TtsChronicle`, `TtsPlayer`, `TtsLegacy`, `TtsSite` and `TtsFoundations` (Task 3), `LineageState.startingRelics` (Task 1), and `FirstGameBootstrapMapper.lineageOf(color: PlayerColor): LineageId`.
- Produces:
  - `final class ChronicleTtsCodec(catalog: ExecutableCatalog)` with `decode(text: String): Either[String, Chronicle]` and `encode(chronicle: Chronicle): Either[String, TtsExport]`.
  - `final case class TtsExport(text: String, omitted: Vector[String])`.

Mapping rules:
- **Decode:**
  - Sites become `StoredSite(ids.site(index), items.map(ids.card))`.
  - The decks map through `ids.card`, then check the kind: `worldDeck` and `dispossessed` must be `DenizenId` and the relic deck and Reliquary must be `RelicId`. A wrong kind refuses as `"TTS card 401 is not a denizen"`.
  - Foundations: each set bit `b` gives `FoundationNumber.all(b) -> FoundationState(FoundationFace.Altered, Set.empty)`.
  - Goal digit: `None` or `Some(0)` gives Supremacy; 1 Supremacy, 2 Devotion, 3 ThePeople, 4 Protection. 5-20 refuse as `"Oathkeeper goal 7 is reserved"`, and 21 or more as `"Oathkeeper goal 21 is fan content"`.
  - Players: an empty Exile section (status 0, no cards, legacies or relics) gives no lineage. Any other gives:

    ```scala
    LineageState(lineageOf(color), None, role, legacies.map(l =>
      LegacyState(ids.legacy(l.id), active = l.flipped)),
      cards.map(c => DenizenState(denizen, Orientation.FaceDown, Tokens.empty)),
      startingRelics = relics.map(relic))
    ```

    `FaceDown` is how the Beacon keeps a starting adviser. Colors follow the TTS order red, blue, yellow, black, white, brown, pink, through `PlayerColor`.
  - Defaults: `era = EraState(20, Map.empty)`; absent sections are empty.
- **Encode:**
  - Every section is present, with the seven player sections in TTS color order. A color without a lineage writes `TtsPlayer(0, Vector.empty, Vector.empty, Vector.empty)`.
  - A lineage whose id is not `lineageOf` of a color refuses as `"lineage x has no TTS color"`.
  - A `VisionState` starting adviser refuses.
  - The goal digit is the inverse of the decode map.
  - The output is version 3.
- **Omitted list,** in this order, each entry present only when it applies:
  - `"the era target"` when it is not 20;
  - `"the era scores"` when any score is above 0;
  - `"previous players"` when any lineage has one;
  - `"Foundation alteration sources"` when any Foundation has one.

- [ ] **Step 1: Write the failing tests**

```scala
package oathdigital.application.chronicle

import oathdigital.application.FirstGameBootstrapMapper.lineageOf
import oathdigital.gameplay.setup.FirstGameSetupFixture
import oathdigital.model._
import oathdigital.testkit.CatalogNames

/** TTS strings to and from Chronicles (Chronicle TTS codec design). */
class ChronicleTtsCodecSuite extends munit.FunSuite:
  private val codec = ChronicleTtsCodec(FirstGameSetupFixture.catalog)
  private val chronicle = FirstGameSetupFixture.chronicle
  private val fullExport = "02H3uk5&m#lvuYL;KIO:Mg9FjOs)5.yU!bk{6k9tUwlHwZ}z%uTY3.}IvkwdiP9.r)s-g,XAxa!)JRjGCc5ze$;93~2=J;chc)06a}66Pn:tI#vWs~QDbnK5PpZ#&Xq36lcTLE-g359{rq/DBYLh@RBb~R;?$:iMT},~=/N@)J+fh9Ip8T1#dMl?Gf:M?tM%r/8%cL,lF{RGZOorax@0pONlP@9O.&SmJ>G6bq2t^7FJFmI>W$a,3r4:&ohBXUX)$&d%DuAJl%i~Y,c)@b!/eoUQ9Qs!w223^(:%Lf.{eA5@(1u^DqWE$Kv#=iyHXC/5T"

  private def roundTripped(c: Chronicle)(using munit.Location): Chronicle =
    codec.encode(c).flatMap(e => codec.decode(e.text)).fold(fail(_), identity)

  test("the full Lua export decodes to the values its own test asserts"):
    val full = codec.decode(fullExport).fold(fail(_), identity)
    assertEquals(full.reliquary, Vector("Spiteful Mirror", "Sigil of the Eye",
      "Whistle", "Lost Tapestry", "Cracked Horn").map(CatalogNames.relic))
    assertEquals(full.foundations, Map(FoundationNumber.IV ->
      FoundationState(FoundationFace.Altered, Set.empty)))
    val yellow = full.lineages.find(_.id == lineageOf(PlayerColor.Yellow)).get
    assertEquals(yellow.role, Role.Chancellor)
    assertEquals(yellow.startingAdvisers.map(_.id),
      Vector(CatalogNames.denizen("Scouts")))
    assertEquals(yellow.legacies, Vector(
      LegacyState(CatalogNames.legacy("Light Fingers"), active = false),
      LegacyState(CatalogNames.legacy("Arbiter"), active = true)))
    assertEquals(full.lineages.find(_.id == lineageOf(PlayerColor.Brown))
      .map(_.legacies.map(_.active)), Some(Vector(true, false, true)))
    assert(!full.lineages.exists(_.id == lineageOf(PlayerColor.Black)))
    assertEquals(full.oathkeeperGoal, OathkeeperGoal.Supremacy)
    assertEquals(full.era, EraState(20, Map.empty))

  test("the first-game fixture round trips and loses nothing"):
    val exported = codec.encode(chronicle).fold(fail(_), identity)
    assert(exported.text.startsWith("03"), exported.text)
    assertEquals(exported.omitted, Vector.empty)
    assertEquals(roundTripped(chronicle), chronicle)

  test("every Oathkeeper goal survives version 3"):
    OathkeeperGoal.all.foreach(goal =>
      assertEquals(roundTripped(chronicle.copy(oathkeeperGoal = goal))
        .oathkeeperGoal, goal))

  test("a lineage's role, legacies, advisers and relics round trip"):
    val relic = chronicle.relicDeck.head
    val red = LineageState(lineageOf(PlayerColor.Red), None, Role.Citizen,
      Vector(LegacyState(CatalogNames.legacy("Arbiter"), active = true)),
      Vector(DenizenState(chronicle.worldDeck.head, Orientation.FaceDown,
        Tokens.empty)), startingRelics = Vector(relic))
    val kept = chronicle.copy(relicDeck = chronicle.relicDeck.tail,
      worldDeck = chronicle.worldDeck.tail, lineages = Vector(red))
    assertEquals(roundTripped(kept), kept)

  test("an export lists what the TTS string leaves out"):
    val evolved = chronicle.copy(
      era = EraState(15, Map(lineageOf(PlayerColor.Red) -> 6)),
      lineages = Vector(LineageState(lineageOf(PlayerColor.Red),
        Some(PlayerId("Red")), Role.Exile, Vector.empty, Vector.empty)),
      foundations = Map(FoundationNumber.II -> FoundationState(
        FoundationFace.Altered, Set(CatalogNames.legacy("Arbiter")))))
    assertEquals(codec.encode(evolved).map(_.omitted), Right(Vector(
      "the era target", "the era scores", "previous players",
      "Foundation alteration sources")))

  test("an unknown card, a wrong kind and a reserved goal are refused"):
    val unknownSite = TtsChronicle(atlasBox = Some(Vector(TtsSite(30, Vector.empty))))
    assertEquals(codec.fromTts(unknownSite, 2), Left("TTS site 30 is not in the catalog"))
    val relicInDeck = TtsChronicle(worldDeck = Some(Vector(401)))
    assertEquals(codec.fromTts(relicInDeck, 2), Left("TTS card 401 is not a denizen"))
    val reserved = TtsChronicle(foundations = Some(TtsFoundations(0, Some(7))))
    assertEquals(codec.fromTts(reserved, 3), Left("Oathkeeper goal 7 is reserved"))
    val fan = TtsChronicle(foundations = Some(TtsFoundations(0, Some(21))))
    assertEquals(codec.fromTts(fan, 3), Left("Oathkeeper goal 21 is fan content"))

  test("a lineage without a TTS color is refused on export"):
    val odd = chronicle.copy(lineages = Vector(LineageState(LineageId("l9"),
      None, Role.Exile, Vector.empty, Vector.empty)))
    assertEquals(codec.encode(odd), Left("lineage l9 has no TTS color"))
```

These tests use `codec.fromTts(t: TtsChronicle, version: Int): Either[String, Chronicle]`. Make it `private[chronicle]`; `decode` is `TtsWire.decodeExport(text).flatMap((version, tts) => fromTts(tts, version))`.

Check before relying on it: the fixture round trip asserts equality. If `FirstGameSetupFixture.chronicle` lists Foundations as `Normal` entries, or carries a non-default era or goal, the decoded value differs. In that case compare with the fixture normalised the same way, by dropping `Normal` Foundation entries. Do not loosen the codec.

- [ ] **Step 2: Run it and see it fail**

Run: `./sbtw "testOnly oathdigital.application.chronicle.ChronicleTtsCodecSuite"`
Expected: compile error, `ChronicleTtsCodec` not found.

- [ ] **Step 3: Write `ChronicleTtsCodec`**

Write the file from the mapping rules above, with this scaladoc:

```scala
/** A Chronicle to and from the TTS export string (Chronicle TTS codec
  * design). Decoding fills what TTS does not carry: Oathkeeper goal
  * Supremacy unless a version 3 string sets it, era target 20 with no
  * scores, no previous players, no Foundation alteration sources. Encoding
  * writes version 3 and lists what it left out. */
```

Map the colors with `TtsWire.colors.map(key => PlayerColor.all.find(_.key == key).get)`, or the `PlayerColor` lookup that exists. Use a `sequence` helper for vectors of `Either`. If `ChronicleExtraction.all` is private, add a private one here.

- [ ] **Step 4: Run the tests**

Run: `./sbtw "testOnly oathdigital.application.chronicle.*"`
Expected: PASS.

- [ ] **Step 5: Run the full gates**

Run: `./sbtw test`, then `python3 scripts/check-architecture.py`, then `python3 scripts/check-markdown-links.py`.
Expected: all pass.

Run: `./sbtw clean coverage test coverageReport`
Expected: statements at 92% or more. Uncovered refusal branches in `TtsWire` are acceptable only if the total stays at or above that.

- [ ] **Step 6: Update the docs**

- **Spec, Components:** change `decode(text, name): Either[String, Chronicle]` to `decode(text): Either[String, Chronicle]`, and add one sentence: "Browsing slice 1 adds the `name` parameter and the name entry in the omitted list, when `Chronicle` gains `name`."
- **`implementation-traceability.md`, row "Full campaign serialization and migration":**
  - Evidence: replace "No cross-version campaign migration or Chronicle string codec exists." with "`application/chronicle/ChronicleTtsCodec.scala` reads TTS strings of versions 2 and 3 and writes version 3, matching the Lua exporter's test strings (`TtsWireSuite`, `ChronicleTtsCodecSuite`, `TtsIdsSuite`). No cross-version campaign migration exists."
  - Gap: replace "the codec has no era field" with "the TTS string has no era field".
- **`ROADMAP.md`:** under "Chronicle import and export", add a sub-bullet: "The codec landed: TTS strings of versions 2 and 3 decode, and version 3 encodes. Import and export in the UI come with Saved-campaign browsing." Leave the box unticked.

- [ ] **Step 7: Commit**

```bash
git add src/main/scala/oathdigital/application/chronicle/ChronicleTtsCodec.scala src/test/scala/oathdigital/application/chronicle/ChronicleTtsCodecSuite.scala docs/superpowers/specs/2026-10-28-chronicle-tts-codec-design.md docs/rules/implementation-traceability.md docs/ROADMAP.md
git commit -m "feat(chronicle): read and write Chronicles as TTS strings, listing what an export leaves out

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```
