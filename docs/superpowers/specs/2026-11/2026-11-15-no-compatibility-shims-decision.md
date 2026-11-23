# No Compatibility Shims Before the First Public Release

> Status: decision recorded 2026-11-15 from the November architecture and
> hygiene reviews. It is stated for agents in
> [codebase structure](../../../architecture/codebase-structure.md),
> "Change, don't preserve". It ends at the first public release.

## Decision

Until the first public release, no change keeps an earlier commit's shape
working. A changed signature, field, file or name moves every caller with it,
in the same change. The repository gains no forwarding overload, optional
field with a fallback, adapter, facade or parallel code path whose only job is
to let the previous shape keep compiling or decoding.

What a change must keep is the rules as the game plays them, and the frontend
and server at the same commit understanding each other. Saved journals from
earlier commits need not replay; new wire fields are required, not optional.

## Why

The three cleanup rounds of October and November deleted shims that earlier
slices had left behind to be "safe": forwarding methods such as
`PowerAccess.pawnSite` and `MusterSource.cardIdOf`, a `CampaignSetup.playerOf`
beside `CurrentGameState.player`, and optional-field fallbacks in codecs. None
had a second caller. Each cost a later reader a detour and a later agent a
wrong guess about which entry point was current.

Agents have no sense of time. Asked to change something "safely" or "without
breaking anything", they read the previous commit as a deployed system and
preserve it. The rule gives "safe" a concrete meaning instead.

## Considered and rejected

- **Optional fields and fallbacks in codecs.** Rejected on 2026-10-13: the
  alpha has no journals worth preserving, and every fallback is a branch that
  tests must cover and replay must trust.
- **A deprecation period for renamed symbols.** Rejected: with one repository
  and one build, a deprecated symbol has no external caller to wait for. The
  compiler finds every internal caller at once.
- **Stating the rule in `CLAUDE.md`.** Rejected: that file is read on every
  call of every session. The rule matters only when code is designed or
  written, so it lives in the architecture guide those sessions read.

## Consequences

- A review that finds a forwarding overload, an optional-field fallback or a
  one-caller adapter removes it; it does not need a second reason.
- `scripts/new-symbols.sh` lists the public symbols a branch adds so that a
  slice ends with each one inlined, merged into an existing name, or kept on
  purpose.
- This decision is retired at the first public release. Retiring it means
  deleting the "Change, don't preserve" section's first paragraph and recording
  the release's compatibility promise in its place.
