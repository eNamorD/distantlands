# Derive the wire codecs

> Status: probed, 2026-11-03. The owner approved the recommended
> defaults: derive, as a Mirror deriver with no new dependency, probe
> first, and only if the probe is green without a macro. It is not; see
> "Probe result". Question 3 landed on its own: an unexpected field on a
> projection now reports a stale bundle, as an unknown variant does.
> Question 5 stands: the operation codec stays hand-written.
>
> Written as a design note, 2026-11-02, for the roadmap's architecture candidate
> "Derive the wire codecs" (architecture scans of 2026-10-23). It re-measures
> the candidate at `0d1ae92`, evaluates how a codec can be derived in this
> build without a new dependency, counts the operation codec's mechanical
> arms as the roadmap asks, and recommends what to do. Nothing here is
> implemented. Companion notes: the
> [command vocabulary](2026-11-02-one-command-vocabulary-design.md) and the
> [HTTP gateway](2026-11-02-one-http-gateway-design.md).

Vocabulary: a **DTO** is a case class or case object in
`shared/src/main/scala/distantlands/protocol`, the wire form the server
writes and the frontend reads. A **codec** is the hand-written pair of
`encode` and `decode` functions for one DTO. An **`exact` set** is the
`Set("field", ...)` a decoder passes to `exact`, which refuses any key the
set does not name. The **operation codec** is `WalkerOperationCodec` in
`serialization`, the JVM-only journal spelling of `CoreOperation`, which the
roadmap item names separately.

## The numbers, at `0d1ae92`

| measure | roadmap (2026-10-23) | now | how counted |
|---|---|---|---|
| DTOs | 94 | 96 | `case class`/`case object` lines under `shared/src/main`, less the seven `ProtocolDecodeFailure` cases |
| hand-written `exact` sets | 86 | 91 | lines matching `exact(` under `shared/src/main`, less the five `def exact` lines |
| codec lines | 1,647 | 1,653 | `wc -l` of the eleven `*Codec*.scala` files plus `CommandJsonSupport.scala` and `CommandIntentDecoders.scala` |
| codec-suite lines | 1,026 | 1,028 | `wc -l shared/src/test/scala/distantlands/protocol/*.scala` |
| DTO lines | not given | 843 | the five `*Dto(s).scala`, `CommandIntents.scala`, `CommandProtocol.scala`, `TrustedGameProtocol.scala` |
| encode/decode functions | not given | 117 | `def (encode\|decode)[A-Za-z]*(` under `shared/src/main` |
| DTO fields with a default | not given | 79 | field lines with `= ` in the DTO files; 58 are `None`, 22 `Vector.empty`, the rest `0`, `1`, `3`, `false` and one `true` |
| discriminated wire types | not given | 6 traits, 3 enums | `sealed trait`/`enum` under `shared/src/main`, less `ProtocolDecodeFailure`; tag keys `"kind"` (25 sites), `"form"` (7), `"type"` (1) |

The codec is twice the size of what it encodes (1,653 lines for 843), and
the suite that pins it is larger than the DTOs again. Exactness is 91
hand-copied field lists, one per object shape, each a second spelling of
the case class's parameter list.

Two of the roadmap's claims could not be re-measured. The worktree is a
shallow clone of 127 commits; `CoreOperations.scala` and
`WalkerOperationCodec.scala` appear together in the only two of those
commits that touch either, which is consistent with "13 of 16" but does not
re-establish it. "A third of a typical wire commit is codec and codec test"
likewise: only two feature commits in the window touch `shared/src/main`.
In them the DTO change was 5 and 11 lines, the codec change 16 and 18, and
the codec-suite change 6 and 7 (`f131f16`, `8a6deb0`), so the codec and its
test were about three times the DTO delta, and about a quarter of each
commit. The low count also says something: in 127 commits the wire changed
twice. The pain is real per wire change, and wire changes are currently
rare.

## What exists

`ProjectionCodecSupport` (87 lines) and `CommandJsonSupport` (58 lines)
hold the primitives: `obj`, `exact`, `field`, `string`, `int`, `bool`,
`array`, `optional`, `optionalAbsent`, `default`, `traverse`, with
`Result[A] = Either[ProtocolDecodeFailure, A]` and a JSON path threaded
through every call so a failure names `$.world[0].sites[0].relics.relics`.
Every codec is a `for` over those primitives. `ConsentProjectionCodec` is
representative: `ConsentPromptProjection(requesterPlayerId, powers, target,
paying = false, use = None)` takes 20 lines to encode and decode, of which
the `exact` line, the five field reads and the `yield` restate the case
class, and the two "written only when set" lines
(`if value.paying then row("paying") = true`; `value.use.foreach(...)`)
are the only decisions.

Five things in the codecs are not restatements of a DTO, and a derivation
has to keep each:

1. **Exactness.** 91 `exact` sets. `CommandJsonSupport.exact` additionally
   classifies `playerId`, `actor`, `actorId` and `actorPlayerId` as
   `ActorInjection`, and `rejectActorFields` repeats that for the intent
   root.
2. **Discriminators and their names.** `GameIntent` is tagged by `"type"`
   with camelCase names (`resolveWalker`); `DecisionAnswerWire` and
   `NegotiationInformation` by `"kind"` with kebab-case names that
   deliberately match the journal's `DecisionAnswerCodec` tags
   (`choose-one`); `DecisionQueryProjection` by `"form"`; `LogSpan` and
   `SiteForcesProjection` by `"kind"`. An unknown tag is `UnknownVariant`
   for projections and `InvalidValue` for commands.
3. **Presence rules.** 79 defaulted fields, written only when set, so an
   older payload still decodes (the suite has eleven "a payload without X
   decodes to none" tests). Most are `None` or `Vector.empty`; a few are
   not the type's empty value: `implemented: Boolean = true`,
   `supplyMaximum`-style integers with a fallback through `intOr`, and
   three numeric defaults.
4. **Value rules that are not shape.** Non-blank strings and non-negative
   integers in commands (`CommandJsonSupport.string`, `integer`);
   `noDuplicates` over placements, options, amounts, transfers and
   disclosures; "expected one consent target"; `long` bounded to the
   largest JSON-safe integer; a player colour or die face looked up by key.
5. **Renamed keys.** A handful of wire keys differ from the field name:
   `BannerProjection.key` is written as `"banner"` (twice), and sections and
   option rows spell `kind`/`id` pairs from differently named fields.

The frontend's handling of a decode failure matters to the roadmap's
requirement. `GameClient.decodeProjection` and `logResponse` turn
`UnknownVariant` into `UnsupportedProjection`, and `TableSession` stops
polling and offers Reload for that case alone. An `UnexpectedField` becomes
`DecodeFailure`: shown as a notice, polling continues, and the notice
clears on the next page that parses. So today an unknown field does *not*
report a stale bundle; an unknown variant does. Strict unknown-field
rejection is what makes a server-side field rename visible at all, which is
worth keeping, but the Reload posture rides on the discriminators. A
derivation keeps both, and the one-line change that would make an
unexpected field on a projection trigger the Reload posture too is a
separate decision (question 3).

## How a codec can be derived here

`build.sbt` declares one JSON library for both projects: `ujson` 4.4.3
(`%%` for the JVM, `%%%` for Scala.js). That is the tree model only; its
POM depends on `upickle-core`, not on `upickle`, so no `ReadWriter`
derivation is on the classpath. Scala is 3.9.0, so `scala.deriving.Mirror`,
`inline`, `scala.compiletime` and `scala.quoted` macros are available in
both projects with no dependency. No file in the repository uses `derives`
or `Mirror` yet.

### Option A: upickle's derivation

Adding `com.lihaoyi %%% upickle 4.4.3` would bring `derives ReadWriter`.
It is a new dependency on both platforms. Its default spellings differ from
every rule above: a sum is tagged `$type` with the class name, defaults are
omitted by its own rule, and unknown keys are accepted unless a custom
`upickle.Api` turns `allowUnknownKeys` off. Its failures are exceptions
carrying a path through `upickle.core.TraceVisitor`, from which the typed
`ProtocolDecodeFailure` family (`MissingField`, `UnexpectedField`,
`UnknownVariant`, with the exact paths the suite asserts) would have to be
reconstructed from messages. Every one of the five rules becomes
configuration against a library's conventions. Not recommended.

### Option B: a Mirror-based deriver in the shared module

A `WireCodec[A]` typeclass in `oathdigital.protocol`:

```scala
trait WireCodec[A]:
  def encode(value: A): ujson.Value
  def decode(raw: ujson.Value, path: String): Result[A]
```

with givens for `String`, `Int`, `Long`, `Boolean`, `Option[A]`,
`Vector[A]` and the enums, and an `inline given derived` that takes a
`Mirror.Of[A]` and yields a `WireCodec[A]`. For a product, `constValueTuple`
over `MirroredElemLabels` gives the field names, `summonAll` over
`Tuple.Map[MirroredElemTypes, WireCodec]` the field codecs, and the decoder's
`exact` set is the label tuple itself: the 91 hand-copied sets become one
line in the deriver. For a sum, `MirroredElemLabels` gives the case names,
and a small `Tagged[A]` given per sealed trait carries the tag key and the
name mapping (`"form"`, `camelCase`/`kebab-case`, the `Wire`/`Projection`
suffix stripped), which keeps rule 2 and keeps the journal-matching tags.

Rule 3 is the one Mirror cannot see: a case class default is not in the
mirror. Two honest routes. The cheap one is an `Absent[A]` given
(`Option` → `None`, `Vector` → empty, `Boolean` → `false`) applied to any
missing field whose type has one, which covers the `None`, `Vector.empty`
and `false` defaults, all but about ten of the 79, and makes "written only
when the value is not its absent value" the encoder's rule too; the
remaining handful (`implemented = true`, numeric fallbacks) keep a hand-written
codec, which the deriver respects because an explicit given wins. The
thorough one is a 30-line `scala.quoted` macro reading the synthetic
`$lessinit$greater$default$N` methods, so every default is honoured
exactly. Start with the cheap route; add the macro only if the hand-written
residue is more than a few codecs.

Rule 4 stays hand-written as refined codecs (`NonBlank`, `NonNegative`) and
a post-decode `validate: A => Result[A]` hook the deriver calls, so
`noDuplicates` and "expected one consent target" live beside the DTO rather
than inside a decoder. Rule 5 is a `@key("banner")` annotation read through
`m.MirroredElemLabels` plus a small macro, or, since wire formats may change
in place before public release (standing rules), the field is renamed to
its key and the annotation is never written. The second is simpler.

Estimated size: deriver and givens 150 to 200 lines; residue of
hand-written codecs 300 to 400 lines (the sums' tag tables, the refined
command codecs, the validation hooks, the three or four non-empty
defaults); codecs deleted about 1,300 lines. The suite keeps every
round-trip test (they pin the wire shape the frontend reads, and that is
still a shape anyone can break by renaming a field) and loses the
per-object "unexpected field" and "payload without X" tests to two deriver
tests, roughly 1,028 → 650 lines.

Risks specific to this route: inline derivation of `GameProjection` (31
fields) and `WalkerDecisionProjection` may hit `-Xmax-inlines` and will
lengthen the Scala.js compile; scoverage skips a method body over 3,000
tree nodes, so a derived codec may leave the measured coverage (the ratchet
is 92.0% statements), as the operation codec's split comment already
records; `-Werror` with `-Wunused:implicits` will flag unused givens in
scopes that import the deriver. None is blocking; each is a line in the
first slice's notes.

## The operation codec

`WalkerOperationCodec` (557 lines, JVM only) spells 45 wire kinds for 31
`CoreOperation` leaves (`Discard` has four, `Gain` three, `Burn` is a trait
encoded by accessors). Each kind has an encode arm and a decode arm, 90
arms, plus four dispatch arms over the type unions the coverage limit
forced. Counted by hand, an arm pair is **mechanical** when both sides are
a field-by-field spelling with no dropped field, no optional-when-present
key and no decode-time rule:

| | kinds |
|---|---|
| mechanical | 22: `peek`, `flip`, `bury`, `discard-relic`, `reveal`, `flip-secrets`, `gain-favor`, `gain-secrets`, `gain-warbands`, `advance-visions-drawn`, `begin-turn`, `enter-phase`, `record-campaign-result`, `queue-procedure`, `set-die-faces`, `defer`, `take-deferred`, `act-as-if`, `close-pick`, `exchange`, `sacrifice`, `kill` |
| drops a runtime-only field (`required`, `window`, `mode`) | 10: `discard-denizen`, `discard-vision`, `discard-ruined-edifice`, `play`, `take`, `replace`, `pay-cost`, `spend-supply`, `modify-dice-pool`, `roll` |
| optional-when-present keys | 8: `swap` and `move` (`by`), `shuffle` (`order`), `take` (`leaving`, `sourceSide`), `pay-cost` (`intoOccupied`, `matchingBank`), `reroll` (`faces`), `open-pick` and `modify-roll-outcome` (nullable) |
| decode-time rules | 7: `draw` (non-empty, position not Unspecified), `play` (destination a site or play area), `swap` (two cards, two places), `replace` (equal counts, new colour), `spend-supply` and `gain-supply` (positive), `burn` (resource must be favor or secrets) |
| flattens a variant into sibling keys | 1: `record-power-use` (`siteId` / `cardKind`+`cardId` / `bannerKey`) |
| delegates to a bespoke spelling | 3: `give` (`encodeGive` with a tag parameter), `set-oathkeeper`, `set-oathkeeper-goal` |

Twenty-three kinds fall in at least one of the last five rows (several in
two), so about half the arms are mechanical and half carry a decision a
derivation cannot make without an exclusion annotation, a presence rule or
a validation hook per kind. The co-change with `CoreOperations` is also
largely the intended kind: the match is exhaustive, so a new `CoreOperation`
case fails to compile until it has a real arm, which the file's own doc
comment names as the lesson of Catacombs' unencodable `Move`. A derived
codec would make the new case compile silently with whatever fields it
has, including `required` and `window`, and the journal would carry them.

Recommendation for the operation codec: leave it hand-written. Its
mechanical half would save perhaps 150 lines against a journal whose
exhaustiveness check is currently doing work.

## Slices

Each slice is one session, independently mergeable, and gated by the full
gate (`./sbtw -batch test frontend/test frontend/fullOptJS`, the
architecture and link checks, `git diff --check`).

1. **Deriver and shadow test.** Add `WireCodec`, the primitive givens, the
   product and sum derivation, `Absent`, `Tagged` and the validation hook,
   under `shared/.../protocol/WireCodec.scala`. Derive codecs for the six
   consent and log DTOs only, and add a shadow test in
   `ProjectionProtocolSuite` that encodes the suite's populated projection
   fixtures through both the hand-written and the derived codec and asserts
   byte-equal JSON, plus two deriver tests: an unknown key at a nested path
   and an unknown tag. Nothing is deleted. This slice is the probe: if the
   shadow test cannot be made green for those six without a macro, stop
   and record why in this note. Pins: the shadow test, the two deriver
   tests, every existing round-trip.
2. **Projections.** Switch `WorldProjectionCodec`, `GameProjectionCodec`,
   `ConsentProjectionCodec` and `LogPageCodec` to derived codecs; rename
   the two `key` fields that the wire spells `banner` (standing rule:
   formats change in place; update the frontend readers and the
   `HttpGameClientSuite` fixtures together). Delete the hand-written
   codecs and the "unexpected field" tests they carried. Pins: the
   round-trips, the shadow test (now against recorded fixtures), the
   frontend renderer suites that decode real payloads.
3. **Decision projections.** `ActionProjectionCodec` (438 lines, 23
   `exact` sets, the `"form"` discriminator with its seven forms). The
   `UnknownVariant` on an unknown form moves into `Tagged`. Pins: the
   twelve decision round-trip tests, "the decoder refuses an unknown form
   and a field the form does not declare".
4. **Commands.** `CommandIntentCodec`, `CommandIntentDecoders`,
   `CommandNestedCodecs`, `BootstrapProtocolCodec`,
   `TrustedGameProtocolCodec`, with `NonBlank`/`NonNegative` refined codecs
   and the `noDuplicates` hooks. `ActorInjection` stays: the deriver's
   unknown-key failure is mapped to it for the four actor names at the
   intent root. Pins: `CommandProtocolSuite` (every intent round-trips,
   actor injection, duplicates at exact paths, retired intents rejected),
   `TrustedGameProtocolSuite`, `GameHttpWireSuite`. If the command
   vocabulary note lands first, this slice derives the new wire intents
   instead.
5. **Residue review.** List what is still hand-written, decide whether the
   defaults macro is worth it, and update the "Projection" paragraph of
   `docs/architecture/codebase-structure.md` ("Add shared DTO/codec fields
   only when the wire contract truly changes" becomes "add the field; the
   codec is derived").

## Probe result

Slice 1 asks for the consent and log DTOs to derive byte-equal to their
hand-written codecs without a macro. Read against those codecs, they
cannot, for three reasons, each enough alone:

- **Presence follows the default, not the type.** The codecs write a field
  with no default always and a defaulted field only when it is not its
  default: `ConsentAlternativeProjection.paying` (no default) is always
  written, `ConsentWaitingProjection.either` (`= false`) only when true. A
  `Boolean` is a `Boolean` to a Mirror, so the cheap `Absent` route either
  drops `paying: false` or writes `either: false`.
- **The same type is spelt two ways.** `ConsentPreferencesProjection.cardId`
  and `LogEntry.section` default to `None` and are written as `null`, while
  `ConsentWaitingProjection.use` (also `= None`) is omitted. Presence is a
  per-field choice the codecs made, not a rule a deriver could state.
- **Renamed and computed fields.** `LogSpan.Text(value)` is written as
  `"text"`, and `LogSpan.Dice(first, rest)` writes a computed `faces` key
  that its decoder validates (non-empty, one die). Those need per-field
  overrides on top of any derivation.

So the probe stops here, as the slice says. Two ways remain, both larger
than the probe assumed: the `scala.quoted` defaults macro with per-field
overrides for the renamed and computed fields and a one-time move of the
`null` fields to omission (the standing rules allow a wire change in
place), or leaving the codecs hand-written. Neither is worth starting
while the wire changes as rarely as it does; the roadmap item waits for a
wire-heavy phase.

## Risks

- **Wire shape drifts without anyone noticing.** Today a wire change is a
  codec edit a reviewer reads. With derivation a renamed DTO field renames
  a wire key silently, and only the frontend's strict decode catches it at
  run time. The shadow test against recorded fixtures (slice 2) is the
  guard: a fixture file per top-level DTO, diffed in review.
- **Compatibility.** The standing rules allow formats to change in place
  before public release, so slice 2's two renames and any `Absent`-driven
  omission are allowed; the current writer, reader, fixtures and
  development data move together. After public release this candidate
  would need a compatibility story it does not have today, which is a
  reason to do it before, or not at all.
- **Inline compile cost and readability.** A derivation failure reads as a
  compiler message about a missing given for a tuple type, not as a line
  in a codec. The deriver file needs a comment block that says how to read
  one.
- **Coverage ratchet.** Derived code outside the measurement may move the
  statement figure either way; re-baseline in the slice that moves it,
  with the number in the commit body, as `build.sbt` already documents.

## Recommendation

Do it partially, and not first. Derive the shared protocol DTOs (slices 1
to 4); leave the operation codec hand-written. The saving is real, about
1,300 codec lines and 400 suite lines, and every future wire field becomes
a one-line change plus a fixture line. But wire churn is low right now (two
changes in 127 commits), the probe slice is the only cheap part, and the
two companion candidates reduce code the frontend and the players actually
touch. Order: the command vocabulary first, this candidate's slice 1 as a
probe when a wire-heavy phase is next (the table UI rework or L7 would be),
and the rest only if the probe is green without a macro.

## Dependencies

- The [command vocabulary note](2026-11-02-one-command-vocabulary-design.md)
  changes the mapper, not the wire intents, so the two are independent;
  slice 4 here is smaller if the wire intents are already the only
  per-command codec arms.
- The [HTTP gateway note](2026-11-02-one-http-gateway-design.md) does not
  touch the shared module.

## Questions for the owner

1. **Derive at all, given two wire changes in 127 commits?** Recommended
   default: yes, but only the probe (slice 1) until the next wire-heavy
   phase, and then only if the probe is green.
2. **Mirror deriver or upickle?** Recommended default: the Mirror deriver;
   no new dependency, typed failures and exact paths kept.
3. **Should an unexpected field on a projection trigger the frontend's
   Reload posture, as an unknown variant does?** Recommended default: yes,
   as a one-line change in `GameClient.decodeProjection` with a test in
   `HttpGameClientSuite`, independent of this candidate; it is what the
   roadmap item already believes happens.
4. **Rename the two `key` fields to `banner` on the wire, or carry a
   `@key` annotation?** Recommended default: rename; the standing rules
   allow it and it removes a mechanism.
5. **Leave `WalkerOperationCodec` hand-written?** Recommended default: yes;
   half its arms carry rules, and its exhaustiveness check is the guard the
   Catacombs incident taught.
