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
- **R22. In what order does the log show a flip and the taker's line?**
  *Decision:* the flip line comes before the taker's line, as R20's timing
  writes them: "p1 flipped the Darkest Secret with The Needle", then "p1
  took the Darkest Secret with The Standard Bearer". *Reason:* it follows
  R20's timing exactly, and each line says what happened; moving the park
  point would reopen the resume problem R20 closed. *Confidence:* low;
  readers may find "flipped" before "took" odd, and the owner may later
  want The Needle's question to wait for the taker's line.
  *Slice/files:* slice 3; `TheNeedle` and the banner takers.
- **R23. Where does Iron Hand's in-game line sit?** *Decision:* as a
  trigger line before the action's "Became a Citizen." line. *Reason:* it
  matches how the log already shows power lines. *Slice/files:* slice 3,
  Task 5; `Citizenship.become`.

## Slice 4

The [slice 4 plan](../../plans/2026-11/2026-11-14-legacy-in-play-powers-slice4.md)'s
open questions, ruled on 2026-11-14.

- **R24. Is Conspirator's prediction compared before or after Royal
  Line?** *Decision:* right after Royal Line's choice, against the winner
  that results from it (after the Successor substitution and any Royal
  Line choice); both run in the step R36 adds after the game's result is
  set and before Start Chronicle, so a Conspirator win overrides the
  result before the Chronicle opens and every Chronicle step reads it.
  *Reason:* the owner's ruling ("Conspirator should check after Royal Line
  power is activated"), placed by R36. This supersedes the first decision,
  to compare at the game's end before Royal Line, and the second, to
  compare in the Sun right after its first window. *Slice/files:* slice 4,
  re-ruled twice; `ConspiratorReveal`, `ChronicleWinnerProcedure`.
- **R25. May The Mouth activate Iron Hand for an Imperial holder?**
  *Decision:* yes; the same batch then discards it, with Iron Hand's
  discard line. *Reason:* "If you are Imperial, discard Iron Hand" is a
  standing condition, not a restriction on activation; leaving Iron Hand
  out of the targets invents a rule, and leaving it active breaks the
  card. *Confidence:* low; leaving it out of the targets gives the same
  board for less cost, and the owner may prefer that. *Slice/files:*
  slice 4, Task 1; `TheMouth`, `IronHand`.
- **R26. How does Conspirator's holder see their prediction?**
  *Decision:* through the holder-only log line alone. *Reason:* no
  frontend change is needed; the log already carries holder-only lines
  (Heirloom). *Slice/files:* slice 4, Task 3.
- **R27. How are the slice's lines worded?** *Decision:* "p2 wrote down a
  prediction with Conspirator"; "p2 predicted p3 wins in round 6", read by
  the holder only; "p2 had written down p3 to win in round 6 with
  Conspirator" at the reveal; "p2 won with Conspirator"; the winner banner
  "Conspirator's prediction". The Mouth: "p1 activated Arbiter with The
  Mouth", "p1 took Golden Key from the Public Ambitions with The Mouth",
  "Beloved set Foundation IV to Teeming World", "p1 discarded Seer with The
  Mouth". *Reason:* it matches the batch's "<player> <verb> … with
  <legacy>" pattern, and the public line names nothing. *Slice/files:*
  slice 4, Tasks 1-4.
- **R28. How is the round written?** *Decision:* as an amount from 1 to
  8, which no log line names; a test shows that neither `DetailLines` nor
  any other viewer's projection or log exposes the amount or the winner
  until the Sun reveals it, after Royal Line's choice (R24).
  *Slice/files:* slice 4, Task 3.
- **R29. How is a void prediction revealed?** *Decision:* with the same
  line, and no extra word. *Reason:* the game-end state shows Conspirator
  is gone, and one line shape is simpler. *Slice/files:* slice 4, Task 4.
- **R30. What does The Mouth show with nothing to activate?**
  *Decision:* unavailable, with "No legacy whose goal you meet".
  *Reason:* R4 already reads "not offered" this way for legacy ACTIONs,
  and the design's ruling 23 reads under R4. *Slice/files:* slice 4,
  Task 1.

Ruled by the owner on 2026-11-14, from the review of slice 4.

- **R31. Which cards does The Mouth's goal reading count?** *Decision:*
  during the game, only faceup advisers and faceup relics, for every
  player, the holder included; the Chronicle's Resolve Legacies keeps its
  scoring. Narrows the design's ruling 23 ("as Resolve Legacies reads
  them"). *Reason:* CR p. 17 gives a facedown adviser no suit, and reading
  hidden cards would tell the holder what they are; in the Chronicle
  everything is revealed. *Slice/files:* slice 4 fix; `LegacyGoals`,
  `TheMouth`.
- **R32. How are an Imperial's Iron Hand's activation and discard lines
  written (R25)?** *Decision:* they name Iron Hand in words. *Reason:*
  activation reveals the legacy (design, ruling 24), but the same batch
  discards it, so a card reference never sees it faceup; words are the
  smallest change and match the Conspirator reveal. *Slice/files:* slice
  4 fix; `TheMouth`, `IronHand`.
- **R33. Does `wonBy` keep its Conspirator case?** *Decision:* yes, for
  exhaustiveness, with a comment and a test. *Reason:* the reveal step
  writes the victory line itself, and removing the case would make the
  match non-exhaustive. *Slice/files:* slice 4 fix; `GameLogFormatter`.

Ruled by the owner on 2026-11-14, from the R24 change.

- **R34. Who starts the Chronicle before the Sun compares the
  prediction?** *Superseded by R36.* *Decision:* keep as is: Start Chronicle belongs to the
  winner as it stands at the game's end, and the winner banner updates
  once the Sun's comparison runs. *Reason:* the prediction stays secret
  until the Sun reveals it, so a provisional winner before then is what
  the table knows; no change is needed. *Slice/files:* slice 4, R24
  change; `ChronicleBeginProcedure`, `ConspiratorReveal`.
- **R35. May the R24 change withdraw the game-end key and rename the
  reveal's lines?** *Decision:* yes: the `game-end-conspirator` procedure
  key is withdrawn, and the reveal's note keys are the Sun's
  `conspirator-revealed` and `conspirator-won`. *Reason:* no released
  save uses them; only dev-database games saved after a game end during
  this batch are affected. *Slice/files:* slice 4, R24 change;
  `ProcedureRef`, `ConspiratorReveal`.

Ruled by the owner on 2026-11-14.

- **R36. When do Royal Line and Conspirator's comparison run?**
  *Decision:* Royal Line's choice and then Conspirator's comparison run in
  a new step after the game's result is set and before the Start
  Chronicle decision. Start Chronicle then belongs to the final winner
  (after the Successor, Royal Line and any Conspirator win), and the
  winner banner shows that winner throughout. Supersedes R34 and amends
  R24; it also moves the Legacy Chronicle powers batch's Royal Line out of
  the Sun's first window (its design ruling 22 and R40). *Reason:* the
  owner's ruling; nothing in the rules happens between the game's end and
  the Sun's first window, so only the digital Start Chronicle pause moves
  relative to them. *Slice/files:* slice 4, R36 change;
  `ChronicleWinnerProcedure`, `ChronicleSequence`, `RoyalLine`,
  `ConspiratorReveal`, `ChronicleSunProcedure`.
