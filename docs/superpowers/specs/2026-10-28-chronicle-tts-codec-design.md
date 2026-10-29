# Chronicle TTS codec design

Status: approved in brainstorming, 2026-10-28. Next step: implementation plan.

Roadmap item: **Phase - Chronicle steps**, "Chronicle import and export". It
comes before the [Chronicle browsing design](2026-10-28-chronicle-browsing-design.md),
whose convert route and Export Chronicle use this codec.

## Goal

Read and write the Tabletop Simulator (TTS) Chronicle export string, so a
Chronicle moves between this project and the Oath NF TTS mod.

## Source

The format is defined by `chronicle-codec.js` in
[harsch1/oath-nf-tts-scripts](https://github.com/harsch1/oath-nf-tts-scripts/blob/main/chronicle-codec.js),
read at commit `f6051bb0` (2026-10-04, encoding version 2). Its maintainer,
harsch, has announced version 3: one digit after the Foundations bitmask for the
current Oath (0 unset, 1 Supremacy, 2 Devotion, 3 The People, 4 Protection,
5-20 reserved, 21+ fan content), and said it is compatible with version 2
otherwise.

The format in brief:

- **Wrapper.** Two base-82 characters of version, the scrambled body (a fixed
  seed, 468529063, over the alphabet plus the structural characters `#` and
  `>`), then a two-character checksum of the plain body. Whitespace is ignored.
- **Sections**, separated by `>`, in order: `atlasBox`, `world`, `worldDeck`,
  `relicDeck`, `dispossessed`, `reliquary`, `foundations`, then one player
  section per color: red, blue, yellow, black, white, brown, pink. Trailing
  absent sections are omitted; an absent section before a present one is empty.
- **Sites.** One base-82 token per site, ended by `#`: the site index in the
  lowest two decimal digits, then up to three item ids of three digits each.
- **Decks.** Ids in chunks of five, each chunk seven base-82 characters, the
  first chunk rightmost. Card ids use radix 400, relic ids radix 100 (relic id
  minus 400).
- **Reliquary and player lists.** One unpadded number per list.
- **Foundations.** A bitmask of the flipped Foundations, Foundation I as bit 0.
- **Players.** `[status][cards]#[legacies]#[relics]`. Status 0 Exile, 1 Citizen,
  2 Chancellor. A legacy is its id plus 100 when flipped, radix 200; fewer than
  five legacies are one number, five or more are chunked like a deck.

## Decisions

- **A JVM port.** `ChronicleTtsCodec` ports the JS file exactly: alphabet,
  scramble, checksum, chunk packing and section order. The browser never runs
  it; the server converts.
- **Ids by table.** `TtsIds` maps TTS numbers to catalog ids:

  | TTS | This project |
  |---|---|
  | deck card `n` (1-399) | `DenizenId(n)` |
  | relic `400 + n` | `RelicId` `Rnn` |
  | edifice `500 + n` | `EdificeId` `Enn` |
  | site index | `SiteId`, by a table copied from the JS file's site names |
  | legacy `n` | `LegacyId` `Lnn` |
  | Foundation bit `b` | `FoundationNumber` `b + 1` |
  | player section `color` | the lineage of that color |

  Both faces of a site share one TTS index; a `SiteId` already names the card,
  not the face.
- **Versions.** Decode versions 2 and 3; the public encoder writes version 3
  only. The wrapper takes the version as a private parameter so the tests can
  re-encode the JS file's version 2 strings. Any other
  version is refused by number. If harsch publishes a version 3 layout that
  differs from the announcement, the codec changes to match it.
- **Import defaults.** TTS carries no name, era scorepad, Oathkeeper goal (in
  version 2, or a version 3 digit of 0), previous player or Foundation
  alteration source. Decoding takes the name from the caller and fills the rest
  with: Oathkeeper goal Supremacy, as the first-game generator does; era target
  20 with an empty scorepad; no previous players; no alteration sources.
- **Export lists its losses.** Encoding returns the string and the fields it
  left out: the name, a non-empty era scorepad, any previous player, and any
  Foundation alteration source. The caller shows them as a note; nothing is
  refused.
- **Players.** A player section maps to a `LineageState`: status to `role`,
  cards to `startingAdvisers`, legacies to `legacies` (flipped to
  `active = true`), relics to the new `startingRelics`. A section with Exile
  status and all fields empty is no lineage, and encoding writes such a section
  (`0##`) for every color without a lineage.
- **`LineageState.startingRelics: Vector[RelicId]`.** New, empty by default. It
  matches the TTS player section's separate relic field and keeps
  `startingAdvisers` denizens only. Nothing creates kept relics until Legacies,
  so game start refuses a non-empty `startingRelics` with a reason, as it does a
  non-empty `world`. The card accounting counts them as stored relics.
- **Unknown ids refuse.** A TTS id the table does not map, including cards the
  catalog lacks, refuses the import and names the number. A reserved or fan
  Oathkeeper digit refuses too.

## Components

- `application/chronicle/TtsIds.scala`: the id tables, both directions.
- `application/chronicle/ChronicleTtsCodec.scala`:
  `decode(text, name): Either[String, Chronicle]` and
  `encode(chronicle): Either[String, TtsExport]`, where
  `TtsExport(text: String, omitted: Vector[String])`. Encoding fails only for a
  value the format cannot represent, such as more than six starting advisers.
- `model/GameState.scala`: `LineageState.startingRelics`.
- `gameplay/setup/GameStartRules.scala` and `ChronicleAccounting.scala`: the
  refusal and the accounting.

## Testing

- The JS file's test strings: its plain string and the Lua plain and scrambled
  pair decode and re-encode exactly under version 2's wrapper; its full
  export decodes to the values its test asserts (Reliquary, Foundations,
  players with flipped legacies).
- Version 3: round trips for every Oathkeeper goal, digit 0 reads as the
  default, reserved and fan digits refuse.
- A changed character fails the checksum; an unknown version refuses.
- The id table joined with the JS file's name table and the catalog's names: a
  mismatch fails the test.
- The omitted list for a first-game Chronicle (only the name) and for an
  evolved one.
- A non-empty `startingRelics` refused at game start, and counted by the
  accounting.

## Order of work

One slice: `startingRelics` with its refusal and accounting, `TtsIds`, then the
codec and its tests. The convert route and the UI come with browsing slices 1
and 2.

## Out of scope

The convert route and any UI (browsing), TTS fields this project does not model
beyond those listed, and fan content.
