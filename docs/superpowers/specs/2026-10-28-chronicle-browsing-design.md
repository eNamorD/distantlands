# Chronicle browsing design

Status: approved in brainstorming, 2026-10-28. Next step: the Chronicle codec
(TTS) design, then an implementation plan for browsing slice 1.

Roadmap item: **Phase - Chronicle steps**, "Saved-campaign browsing": import,
export, load and delete a campaign. A campaign is a named, saved Chronicle.
Starting game 2 from an evolved Chronicle stays with the Empire phase
("Saved-campaign continuation").

## Goal

A player keeps Chronicles in their browser, starts a game from one, and takes
the evolved Chronicle home automatically when the game's Chronicle Phase ends.
Chronicles move between browsers as a JSON file or, once the codec lands, a
Tabletop Simulator (TTS) string.

## Decisions

- **Loading starts a game.** Loading a Chronicle makes it the browser's loaded
  Chronicle; Continue Chronicle starts a game from it. Until the Empire phase,
  game start refuses an evolved Chronicle ("a non-empty world requires the
  Empire"), so only first-game Chronicles start today.
- **Stored in the browser.** Each browser keeps its own Chronicles in local
  storage. The server stores no Chronicle outside a game's journal and has no
  list of them.
- **No host check.** Any browser that reaches the server may convert a
  Chronicle or start a game from one.
- **The name is the identity.** `Chronicle` gains `name`; a browser keeps its
  names unique. No separate id exists.
- **JSON is stored; TTS is exchanged.** The browser stores the canonical JSON,
  which holds every field. The TTS string cannot hold the Era scorepad or, before
  its version 3, the Oathkeeper goal, so it is an import and export format only.
  A full Chronicle is about 3-5 KB of JSON against roughly 0.5-1 KB of TTS;
  local storage holds about 5 MB per origin, so space does not decide this.
- **The server does every conversion.** The model and its codecs compile only on
  the JVM. The browser treats a Chronicle as text plus a server-made digest and
  summary.
- **Every seat saves automatically.** When `ChronicleUpdated` is journaled,
  every seat's browser saves the evolved Chronicle under the game's Chronicle
  name, by the rules in "Automatic save". Every seat also gets Export Chronicle.
- **Relic order is not hidden.** The evolved Chronicle shows the relic deck's
  order to every seat. That is accepted.

## Model

- **`Chronicle.name: String`.** Trimmed, 1 to 80 characters. `GameStarted`
  carries it inside the Chronicle, and `ChronicleUpdated` copies it forward
  unchanged. Old journals need not decode.
- **The era scorepad.** `EraState(targetScore: Int, games: Vector[EraGame])`,
  with `EraGame(entries: Vector[EraEntry])` and
  `EraEntry(lineage: LineageId, totalScore: Int, endingRole: Role)`, one entry
  per participating player. `lineageScores` is no longer stored: a lineage's
  score is its latest entry's `totalScore`, or 0 if it has none. The Throne's
  Score Lineage Points step appends the game's `EraGame`, and `endingRole` is
  the player's role when the score is recorded, after the Sun has crowned
  ([ruling 19](../../rules/ambiguities.md#chronicle-rulings)). The game number is
  `games.size + 1`. Resetting `games` at an era's end belongs to the Eras item.
- **Digest.** SHA-256 of the canonical JSON, hex encoded.

## Server

- **`ChronicleJsonCodec`.** Versioned (`"format": 1`) JSON holding every
  Chronicle field. Encoding is canonical: one key order, no whitespace, so the
  digest is stable.
- **Summary.** `{name, game, eraTarget, scores: [{lineage, score}]}` derived from
  the Chronicle. `game` is the number of the next game, `games.size + 1`; it is
  absent for a Chronicle imported from TTS, whose scorepad has no games.
- **`POST /chronicles/convert`.** Stateless. Takes `{json}` or `{tts, name}` and
  returns `{json, digest, summary, tts?}`. `tts` is absent when the TTS encoding
  cannot hold the Chronicle or the codec is not yet available. A decode failure
  returns its reason.
- **Create request.** `TrustedGameCreateRequest` gains
  `chronicle: New(name) | Saved(json)`. `New` generates a random first-game
  Chronicle with that name, as game creation does now. `Saved` decodes the JSON
  and runs `GameStartRules.evolve`; a refusal returns its reason. The response
  gains the starting Chronicle's `{json, digest, summary}`.
- **Projection.** Every seat sees the Chronicle name and the starting
  Chronicle's digest from the start. Once `ChronicleUpdated` is journaled, every
  seat also sees the evolved Chronicle's `{json, digest, summary}`. Export as TTS
  goes through the convert route.

## Browser

### Storage

- `oath.chronicles`: a list of `{name, json, digest, summary, savedAt}`.
- `oath.chronicles.loaded`: the loaded entry's name.
- If storage refuses, the page says Chronicles cannot be kept in this browser;
  export still works for the Chronicle on screen. An entry that does not parse
  is listed as "Unreadable" with only Delete.

### Chronicle page (`/`)

Top to bottom:

1. **Loaded Chronicle summary**, when one is loaded: the name as text with an
   edit button that turns it into a text box with Save and Cancel (Save refuses
   an empty or taken name), then "Game N · Era: T to win · Red 9, Blue 6".
2. **Continue Chronicle**, only when a Chronicle is loaded. Goes to player
   selection.
3. **New Chronicle.** Goes to player selection with a name field.
4. **Load Chronicle**, only when the browser holds at least one entry. Lists the
   entries (name, game, era, saved date), each with Load and Delete. Delete asks
   to confirm; deleting the loaded entry clears the loaded Chronicle.
5. **Edit Chronicle**, disabled, hover reason "Coming in a later release".
6. **Import Chronicle.** A JSON file or a pasted TTS string, which needs a name.
   The convert route checks it. A name clash asks to rename the import or replace
   the entry. A successful import becomes the loaded Chronicle.
7. **Export Chronicle.** The loaded Chronicle as a `<name>.chronicle.json`
   download or a copied TTS string. Disabled without a loaded Chronicle; TTS is
   disabled with a reason until the codec lands.

### Player selection page (`/new-game`)

Today's creation page. Above the players it shows either a **Chronicle name**
field (New Chronicle; default "Chronicle 2026-10-28" with the current date;
must not match a saved name) or the loaded Chronicle's name as text (Continue
Chronicle), and a Back link to `/`. Create game sends `New(name)` or
`Saved(json)`. On success with `New`, the browser saves the returned entry and
loads it. A refusal shows on this page. Opening the page without having chosen
New or Continue redirects to `/`.

### Automatic save

When the projection carries an evolved Chronicle, the browser looks up the entry
with the game's Chronicle name:

| Entry with that name | Action |
|---|---|
| none | save it |
| digest equals the evolved Chronicle's | nothing |
| digest equals the starting Chronicle's | replace it silently |
| any other digest | prompt at the table: save under a new name, or overwrite |

The saved entry becomes the loaded Chronicle. The table's menu gains Export
Chronicle for every seat.

## Errors

The convert and create routes return the codec's or game start's reason as
text, and the page shows it beside the control that sent it.

## Testing

- **Server:** JSON round trips, including the evolved Chronicle from
  `ChronicleEndToEndSuite`; the digest is stable; the Throne appends an
  `EraGame` with roles as scored; create with `New` and with `Saved`, including
  the evolved Chronicle's refusal; convert errors; the projection's digest and
  evolved Chronicle for every seat.
- **Frontend:** refused storage and unreadable entries; the four automatic-save
  cases; name clashes on New, import and rename; which buttons show with no
  entries, entries but none loaded, and a loaded entry; the `/new-game` redirect.

## Order of work

0. The Chronicle codec (TTS): its own design, plan and build.
1. Slice 1: `Chronicle.name`, the era scorepad, `ChronicleJsonCodec`, digest and
   summary, the convert route, the create request and response, the projection.
2. Slice 2: the Chronicle page, the player selection page, storage, import and
   export, and the automatic save at the table. TTS export and import turn on
   here if the codec has landed.

## Out of scope

Edit Chronicle (a later slice), starting game 2 (Empire phase), era end and
reset (Eras), sharing Chronicles between browsers other than by file or string,
and any server-side Chronicle list.

## Known limits

- Renaming a Chronicle in one browser does not rename it in others; a game
  started from the renamed copy carries the new name.
- A player who never reopens the game after its Chronicle Phase gets no
  automatic save; any other seat can export it.
