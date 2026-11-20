# Reconsidering a consent answer

**Status:** designed 2026-11-20. It extends the
[consent system](../2026-10/2026-10-08-consent-system-design.md) and amends
its rulings 12 and 13. All three slices are implemented
([slice 1 plan](../../plans/2026-11/2026-11-20-consent-reconsider-slice1.md),
[slice 2 plan](../../plans/2026-11/2026-11-20-consent-reconsider-slice2.md),
[slice 3 plan](../../plans/2026-11/2026-11-20-consent-reconsider-slice3.md)),
with the decisions made on the way in the
[implementation rulings](2026-11-20-consent-reconsider-design-implementation-rulings.md).

Vocabulary: the consent design defines **granter**, **consent request**,
**consent answer**, **consent preference** and **consent ledger**. The
[undo design](../2026-10/2026-10-06-undo-design.md) defines **command
batch**. This design adds **reconsidering**, **locked answer** and **use
mark**.

## Goal

A granter who answered a consent request can change that answer later in the
same turn, in either direction, until the requester has acted on it. The
granter does this from the power's overlay, which lists the turn's requests
about that power under the preference rows. The same work fixes the
preference rows' layout and renames their heading so it says what the
granter consents to.

## User story

Red rules the Narrow Pass. Blue asked to Travel to the Deep Woods and Red
answered **No**.

1. Red clicks the Narrow Pass power. Under "Who may ignore this power" the
   overlay lists "Requests this turn", with the entry "Blue asked to select
   Deep Woods", the state "Refused" and an **Allow** button.
2. Red clicks **Allow**. The entry reads "Allowed" and its button reads
   **Refuse**. Everyone's log shows "Red now gives consent to ignore Narrow
   Pass and select Deep Woods."
3. In Blue's Travel destination choice, Deep Woods is selectable again.
   Nothing moves until Blue clicks it; Blue does not ask again.
4. Before Blue clicks, Red may click **Refuse**. The log shows "Red no
   longer gives consent to ignore Narrow Pass and select Deep Woods." and
   Deep Woods is greyed out again.
5. Blue selects Deep Woods and travels. Red's entry now reads "Allowed,
   used". Its **Refuse** button is shown as unavailable, and its hover note
   reads "Blue has acted on this consent. It unlocks if Blue undoes that."
6. Blue undoes the Travel. Red's entry reads "Allowed" again and **Refuse**
   works.

## Rulings

These continue the consent design's numbering.

20. **Reconsidering.** The player who answered a request may change that
    answer, from a refusal to a grant or from a grant to a refusal. This
    covers an answer their preference gave for them (ruling 10). The change
    is accepted at any time in the turn the answer belongs to, including
    during another player's parked decision, while that player still rules
    every power they were asked about.
21. **Locked answers.** An answer is locked while the move it decided still
    stands: the requester has chosen the request's option, and has not
    undone that choice. A locked answer cannot be changed. When the
    requester undoes the choice, the answer unlocks. A refusal that stopped
    the requester is never locked, because the requester never chose the
    option. Every answer of one request locks together, so a ruler who
    refused to waive Toll Roads cannot change that after the payer's grant
    let the traveller through.
22. **Effect.** A change rewrites what the ledger remembers for the
    changer's own powers on that request. A late grant only reopens the
    option; it never answers the requester's parked decision. The requester
    selects the option without asking again.
23. **Several granters.** Red's refusal closed Green's prompt unanswered
    (ruling 9). After Red changes to a grant, the option is askable again,
    and a new request prompts only Green.
24. **Preferences are untouched.** Changing an answer that Never Allow or
    Always Allow gave does not change the preference. The preference rows
    sit directly above the list.
25. **Undo.** A change is never a command batch and is never undone, like
    every consent event (ruling 13). The requester may undo past it.
26. **No limit.** A granter may change an unlocked answer any number of
    times. Each change writes a public log line.

Ruling 12 is amended: a refused target stays unavailable, and a granted
target stays selectable, until its granter reconsiders. Ruling 13 is
amended: an answer never blocks undo, and an undo may unlock it.

**Not reconsidered in this work.** The answer to an automatic power's
request (the City Wall deactivating a Tyrant) stays final. The list shows it
with its button unavailable. See "Deferred".

## Architecture

### State

`ConsentState` gains the turn's settled requests:

- `history: Vector[ConsentRequest]`, each with its asks as answered. A
  request joins it when it settles, granted or refused. A withdrawn request
  joins it only with the asks already refused, because those refusals are
  remembered (ruling 14) and must be their granter's to change
  (implementation ruling R14). It belongs to `turn`, like `answers`: a
  request settling in a new turn starts it empty.
- At most one entry per requester, decision, option and answering granter.
  A later request with the same four replaces the earlier one. This happens
  when a ruler loses the site and the new ruler is asked (ruling 12).

`reconsidered(granter, requester, decisionId, target, granted, now)` finds
the entry whose ask `granter` answered, sets that ask's answer, and rewrites
`answers` for the keys of that ask's powers as answered by `granter`. A
change to a grant also drops `granter`'s refusal from the request's other
keys, which a required refusal wrote for every key of the request. It fails
when no such entry exists at `now`.

The **use mark** records that the requester acted on a request. It lives in
the part of `ReadyGame` that undo rewinds, not in the ledger:

- `CurrentGameState` gains the set of use marks, each naming a turn, a
  requester, a decision and an option.
- The fold of the walker event that records an answer sets a mark when the
  answer chooses an option for which `history` holds a granted request by
  that requester at that decision. A choose-many answer marks each such
  option it includes.
- Undo returns to a state saved before the answer, so the mark is gone and
  the answer unlocks. No undo code changes.

An answer is locked when its request's mark is set for the current turn.

### Events

`ConsentReconsidered(granter, requester, decisionId, target, granted)` joins
`ConsentEvent`. `ConsentEvolution` folds it with `reconsidered`. Like the
other consent events it opens no command batch. Its codec requires every
field; old journals need no support.

### Commands

`ReconsiderConsent(requesterId, decisionId, target, allow)` is a new intent.
`ConsentDesk.reconsider` answers with one `ConsentReconsidered`, or with
`ConsentUnavailable` when:

- the game is over;
- the issuer answered no request with that requester, decision and option
  this turn;
- the request is about an automatic power (`use` is set);
- the issuer no longer rules a power they were asked about;
- the answer is locked;
- the answer is already what the command asks for.

It resumes nothing: a late grant does not answer the parked decision
(ruling 22).

### Projection

`ConsentPreferencesProjection` gains `requests`, newest first: the turn's
history entries in which the viewer answered an ask naming this power. Each
row carries:

- the requester's player id, the decision id and the option's wire id,
  which the command sends back;
- the option's label, read as the prompt reads it, with the log's fallback
  label for a button whose decision is no longer parked;
- the answer, `allowed` or `refused`;
- the names of the viewer's other powers the same ask covered;
- the card name, when the request was about an automatic power;
- `change`: `open`, `used` (locked) or `automatic` (never changeable).

Only the viewer's own answers leave the server, as with preferences
(ruling 16).

### Game Log

Both lines are public and name the granter, who acts off turn:

- "Red now gives consent to ignore Narrow Pass and select Deep Woods."
- "Red no longer gives consent to ignore Narrow Pass and select Deep Woods."

The powers are the changer's own on that request. The option is worded as
the request line words it.

### Frontend

The overlay's consent column, in the established design system
(`DESIGN.md`):

**Preference rows.** The heading reads "Who may ignore this power". Each
row shows the player's name on its own line and the three settings below it
in equal thirds, so no button wraps alone. This holds at 360px.

**Requests list.** A second block, headed "Requests this turn", under the
preference rows. It is absent when the power has no requests this turn.

- One entry per request, newest first.
- Line 1 says what was asked: "Blue asked to select Deep Woods". When the
  ask covered another of the viewer's powers, a small second line reads
  "Also answers Secret Police".
- Line 2 shows the state as a word, then one button. The states are
  "Refused", "Allowed" and "Allowed, used". The button reads **Allow** on a
  refusal and **Refuse** on a grant. It wears the compact Control face, as
  the settings do.
- A click sends `ReconsiderConsent` with no confirmation step. The change
  is reversible until the requester acts.
- A locked entry keeps its button, in the Withheld face
  (`aria-disabled="true"`), with the hover note "Blue has acted on this
  consent. It unlocks if Blue undoes that."
- An automatic power's entry reads "Blue's Tyrant: deactivated" or "Blue's
  Tyrant: let work". Its button is withheld with the hover note "This
  answer cannot be changed."
- Without control, the buttons are disabled, as the settings are.
- The list uses no accent color. Consent amber stays reserved for "ask
  first", and selection blue for the pressed setting.

**Live while open.** The overlay ignores polls while open, so that a
setting just chosen stays shown. The requests list is the exception: it is
redrawn from each projection, so an entry locks in place when the requester
acts. When the server refuses a change because the entry locked first, the
redraw shows the locked entry.

**Prompt notice.** The granter's prompt notice becomes "You can change this
answer, and your consent preferences, by clicking on the Narrow Pass
power." An automatic power's prompt keeps the old notice.

## Testing

- **Ledger.** `reconsidered` in both directions; several granters, where a
  change to a grant leaves the option askable about the other granter only;
  an entry replaced by a later request; nothing found after the turn ends.
- **Use mark and undo.** Yes at the prompt makes the move and locks the
  answer; the requester's undo unlocks it; a refusal then greys the option.
  A late grant is unlocked until the requester selects the option. A
  choose-many answer marks an included option.
- **Desk.** Each refusal reason listed under "Commands".
- **Projection.** The viewer's own answers only, newest first, with each
  `change` value; a power with no requests carries an empty list.
- **Codecs.** The event, the intent and the projection round-trip.
- **Game Log.** Both lines, for every viewer.
- **Frontend.** The preference row's structure and heading; an entry in
  each state; a click sends the intent; a withheld button sends nothing; the
  list redraws while the overlay is open; the new prompt notice.
- **Browser.** The overlay at desktop width and at 360px, once, after the
  suites pass.

## Slices

1. **Preference rows.** The heading and the two-line row. Frontend only.
2. **Ledger and command.** `history`, the use mark, `ConsentReconsidered`,
   `ReconsiderConsent`, `ConsentDesk.reconsider` and the log lines.
3. **Requests list.** The projection rows, the list in the overlay, its
   live redraw and the prompt notice.

Slices 1 and 2 are independent. Slice 3 needs slice 2. The implementation
amends rulings 12 and 13 in the consent design when slice 2 lands.

## Deferred

- **Automatic powers.** Their answers would follow ruling 21: locked once
  the step resolved, unlocked when the issuer undoes to before the play that
  triggered it. A remembered answer is applied inside the walk, at a second
  place, so the mark needs a second path.
- **Asks with no power overlay.** They keep today's behaviour, where an
  answer is final:
  - Warband Transfers should stop being a consent decision at all, as a
    Negotiation offer is not one.
  - A Palanquin target and a Toll Roads payer could get their controls on
    the Palanquin and Toll Roads cards.
  - The Chancellor's Eye could get its controls at each site it concerns.
- **A mark from an earlier use of the same option.** A use mark names a
  requester, a decision and an option, so an earlier use locks a later
  request about the same three. This is reachable only where one target
  carries different covered keys twice in a turn, such as the same Hall
  of Ministers slot. Unlocking it would need entry ids in the ledger
  (implementation ruling R15).
- **The list's wording of an option.** The list words an option from the
  current state; the log's line words it from the request's own state. A
  positional option (an adviser or relic slot) names what sits there now.
  The projector has no journal, and the ledger holds no wording
  (implementation ruling R23).
- **A turn-long grant first used through another choice.** A grant that
  lasts the turn (the Festival's "use this card") is marked only when the
  requester chooses the option it was asked from. If a late grant is first
  used through a different choice, the list shows it as unused and
  **Refuse** still works. The move stands; only later uses are blocked.
