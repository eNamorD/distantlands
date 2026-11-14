# Legacy in-play powers: implementation rulings

Rulings taken while planning the [Legacy in-play powers design](2026-11-14-legacy-in-play-powers-design.md),
numbered across its slices. Each says which plan raised it. They continue
the design's rulings 1-27 in the "Legacy in-play powers rulings" section
of `docs/rules/ambiguities.md`.

## Slice 1

[Plan](../../plans/2026-11/2026-11-14-legacy-in-play-powers-slice1.md).
Ruled by the owner on 2026-11-14.

- **R1. Does "If you are an Imperial" cover Golden Key's ACTION as well
  as its peek?** *Decision:* yes, it gates both; an Exile holder's menu
  leaves the ACTION out. *Reason:* the condition heads the whole text, as
  on the other two "IF YOU ARE AN IMPERIAL:" legacies, whose condition
  gates their TRAVEL and BATTLE PLAN. *Slice/files:* slice 1, Tasks 5 and
  6; `GoldenKey`, `GamePresentationProjector.identifiesCard`.
- **R2. Which relics has The Ear's holder "not identified"?** *Decision:*
  those missing from the holder's recorded card knowledge
  (`knowledge.siteRelics`), so their own pawn's site counts until they
  peek there. *Reason:* knowledge is what the minor peek changes; the
  projection's view is a presentation detail. *Slice/files:* slice 1,
  Task 8; `TheEarAction`.
- **R3. What do High Priest's and The Ear's lines say?** *Decision:* "p2
  moved 3 secrets from the Darkest Secret with High Priest" and "p2
  peeked at the relics at Deep Woods with The Ear". *Reason:* they follow
  ruling 13's pattern, and The Ear's line never names the relics.
  *Slice/files:* slice 1, Tasks 7 and 8; `HighPriest`, `TheEarAction`.
- **R4. How does the Act menu show an ACTION with nothing to do?**
  *Decision:* unavailable, with its reason, as card ACTIONs are. Only
  Golden Key for an Exile holder is left out. *Reason:* consistent with
  the existing menu; R1 makes the Exile case a role gate, not a lack of
  targets. *Slice/files:* slice 1, Tasks 3, 4, 5, 7 and 8; each power's
  `refusal`, `GoldenKey.listed`.
- **R5. What do the shared bank and Golden Key read while the Reliquary
  is set aside?** *Decision:* both read `reliquaryInPlay`, so a set-aside
  Reliquary shows nothing and gives nothing. *Reason:* the design's
  ruling 9 is about display, not an override of Foundations ruling 3, and
  with no Empire no holder is Imperial anyway. *Slice/files:* slice 1,
  Tasks 5 and 6; `GoldenKey`, `GamePresentationProjector.reliquary`.

## Slice 2

[Plan](../../plans/2026-11/2026-11-14-legacy-in-play-powers-slice2.md).
Ruled by the owner on 2026-11-14.

- **R6. Does Pathfinder also ignore Lost Lagoon?** *Decision:* yes.
  *Reason:* Lost Lagoon makes its site an Island and is the Island power
  there; `TravelTerrain.ignoredOnCoastRoute` already treats it as one.
  *Slice/files:* slice 2, Task 2; `Pathfinder`.
- **R7. How does Keeper of Order's user choose +2 or −2?** *Decision:*
  choosing the plan asks a follow-up, "Add 2 attack dice" or "Remove 2
  attack dice", as Mountain Giant does. *Reason:* one plan option per
  source keeps the plan decision's shape and its Imperial Tactics consent
  unchanged. *Slice/files:* slice 2, Task 5; `KeeperOfOrder`.
- **R8. Do the passive powers write lines?** *Decision:* Ancestral Lands,
  Pathfinder, Light Fingers and Beloved write none; Keeper of Order writes
  "Added 2 attack dice." or "Removed 2 attack dice.", and Outriders'
  "Skulls ignored." when it ignores a skull; High Priest's secrets show in
  the deal's own lines. *Reason:* it matches the terrain powers and the
  existing battle-plan lines. *Slice/files:* slice 2, Tasks 1-6.
- **R9. How does a Negotiation saved before slice 2 resume?** *Decision:*
  its start facts decode with an empty High Priest list, so the walk
  resumes as it was. *Reason:* old saves must keep loading, and an empty
  list is exactly what such a game had. *Slice/files:* slice 2, Task 6;
  `NegotiationStartFacts.read`.
- **R10. May `legacy.high-priest` leave the unimplemented list in slice 1,
  before its WHEN NEGOTIATING half?** *Decision:* yes. *Reason:* slice 2
  lands that half; The Ear set the precedent; no traceability note is
  needed once slice 2 lands. *Slice/files:* slices 1 and 2;
  `src/test/resources/powers/unimplemented.txt`.

### Slice 2 review

Ruled by the owner on 2026-11-14, from the review of slice 2.

- **R15. Which of Lost Lagoon's powers does Pathfinder ignore?**
  *Decision:* only its Island add on arriving at its site, never its Coast
  power on leaving it. Narrows R6. *Reason:* R6 means "the Island power";
  the card's ignore never makes travel worse (design, ruling 15).
  *Slice/files:* slice 2 fix; `Pathfinder.ignored`.
- **R16. May Take Wealth's pawn gate refuse after the resource check?**
  *Decision:* yes: with an enemy pawn there and the resource missing, the
  refusal is `ResourceUnavailable`. *Reason:* the menu leaves the option
  out either way, and keeping the gate a restriction is what lets Light
  Fingers ignore it; only an invalid submission's reason changes.
  *Slice/files:* slice 2 fix; `TakeWealthProcedureSuite`.
- **R17. Does Old Songs stack with Beloved?** *Decision:* yes: a Beloved
  source with exactly one secret and no favor qualifies. *Reason:* Old
  Songs already admits sources Map Library and Skilled Merchants add;
  Beloved's "empty" is the Trade's ordinary requirement Old Songs relaxes.
  *Slice/files:* slice 2 fix; `BelovedSuite`.
- **R18. Should Imperial Infighting hide the enemy Imperial's lendable
  plans?** *Decision:* no change to `PlanAid.allies`. *Reason:* CR p. 35
  makes the enemy a lawful lender, and the attacker-used guard enforces
  Imperial Infighting's limit; the extra offer window is noise, not a
  rules defect, and out of this batch's scope. *Slice/files:* none.
- **R19. May `StylesheetTableEdgeSuite` assert literal CSS?** *Decision:*
  yes. *Reason:* the assertions pin the two rules the app check
  confirmed; their brittleness is cheap to update. *Slice/files:* slice
  2, Task 7.

## Slice 3

[Plan](../../plans/2026-11/2026-11-14-legacy-in-play-powers-slice3.md).
Ruled by the owner on 2026-11-14.

- **R11. Where does Arbiter's choice wait for the end die?** *Decision:* a
  journaled `SetEndDieModifier` writes `CurrentGameState.endDieModifier`,
  which the roll reads and clears; the field is also cleared whenever the
  round ends without a roll consuming it, and a test shows no stale
  modifier reaches a later round. *Reason:* scratch state the walk's end
  may clear is the fragile option; the journal keeps replay exact.
  *Slice/files:* slice 3, Tasks 3 and 4; `StateBasedEvaluation`,
  `ArbiterStep`.
- **R12. How are the slice's unworded lines written?** *Decision:* "p2
  flipped the People's Favor with The Needle", keeping writing nothing;
  "The end die rolled 4, modified by +2 to 6. Stable Regime needed 6.",
  with today's line for an unmodified roll; Arbiter's Decline writes
  nothing; Iron Hand "… on becoming a Citizen", or "… on becoming the
  Chancellor" with no Empire. *Reason:* it matches the existing line
  patterns, and all of it is public. *Slice/files:* slice 3, Tasks 2-5.
- **R13. In what order are the options asked?** *Decision:* the plainest
  answer first: The Needle "Keep this side", then "Flip it"; Arbiter
  Decline, then −1, +1, +2, +3. *Reason:* existing default-answer tests
  and goldens keep their results, as the Chronicle powers do.
  *Slice/files:* slice 3, Tasks 2 and 4.
- **R14. What if one batch hands The Needle's holder both banners?**
  *Decision:* one question per banner, People's Favor first, ids
  `power.the-needle.<banner key>`. *Reason:* the card says "a banner",
  each take is its own choice, and distinct ids keep two decisions from
  sharing one id. *Slice/files:* slice 3, Task 2; `TheNeedle`.
- **R20. How does The Needle's question park after a take?** *Decision:*
  inside the taking walk, right after the take: every banner taker's tree
  shape is made independent of the banner's current holder. Challenge's
  custody and line read the holder inside a `BuildOps` at walk time;
  Favored Son and Wizard's Conclave decide "already held" inside their own
  `BuildOps`; every other banner taker is audited for the same shape. Each
  fix is its own commit with a resume test that parks right after the
  take, and no golden or log line changes. *Reason:* it keeps the design's
  ruling 20 timing, since a flip mid-action can change what follows, and
  removes a latent resume bug. *Slice/files:* slice 3;
  `ChallengeProcedure`, `FavoredSon`, `WizardsConclave` and the audited
  takers.
- **R21. When does Beloved's token leave another player's adviser?**
  *Decision:* at the next Rest, as every card's tokens do; no engine
  change, and the design's ruling 18 wording is corrected. *Reason:* the
  engine already follows the rulebook's clean-up; only the spec's wording
  was narrower. *Slice/files:* the design; `Beloved`'s doc.
