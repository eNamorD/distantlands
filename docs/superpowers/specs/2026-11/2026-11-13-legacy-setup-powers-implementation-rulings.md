# Legacy Setup powers: implementation rulings

Rulings taken while planning the [Legacy Setup powers design](2026-11-13-legacy-setup-powers-design.md),
numbered across its slices. Each says which plan raised it. They continue
the design's rulings 1-20 in the "Legacy Setup powers rulings" section of
`docs/rules/ambiguities.md`.

## Slice 1

[Plan](../../plans/2026-11/2026-11-13-legacy-setup-powers-slice1.md).

- **R1. Game start reads a holder from the Chronicle.** A seated lineage
  whose stored record holds the legacy active is its holder. Its role is
  the stored role: Chancellor and Citizen are Imperial, and anything else,
  including a lineage the Chronicle does not list, is an Exile. The walk
  reads the holder with `ChronicleLegacyRules.holder`, as the Chronicle
  powers do. *Reason:* game start asks no question and has no walk; the
  stored record is what it builds from.
- **R2. An Exile's warbands run out in a fixed order.** (Iron Hand's part amended by R39: it acts only for a holder in the previous game.) An Exile's 14
  warbands go to their board first (3, or Circle of Swords' 8), then to
  Iron Hand's Empire sites, Cradlemost first, then to Rival's sites. A site
  the holder's supply misses takes bandits equal to its capacity.
  *Reason:* the rulebook gives boards first (Players before Empire), and
  Empire ruling R30 already settles a short purple supply this way.
- **R3. An Imperial Circle of Swords takes what the others leave.** The
  holder takes `min(8, 24 - 3 × other Imperial boards)` purple warbands;
  the Empire's sites share the rest, Cradlemost first, and a site the
  supply misses takes bandits (R30). *Reason:* "if other Imperials would
  gain fewer than three, you gain less" protects only the other boards.
- **R4. Only an Exile's Iron Hand acts at setup.** (Iron Hand's part amended by R39: it acts only for a holder in the previous game.) *Reason:* an Imperial
  holder would have discarded it in the Chronicle (Chronicle powers
  ruling 18); its discard during a game is out of scope.
- **R5. Rival's sites take no relics from the deck, and their holder may
  start on them.** Rival's holder may place their pawn at any site their
  own warbands hold. *Reason:* the Empire rules say "Do not fill empty
  relic slots", and the card's "You may place your pawn on these sites".
- **R6. Imperial Maps counts from what game start fixed.** Its empty slots
  are `MapState.slots` less the Empire's stored sites less the sites Rival
  places, worked out again from the Chronicle and the seats. *Reason:* a
  step whose shape read the live map would change shape on resume (the
  Legacies slice 3 fault).
- **R7. One line per power, first in Setup.** "p2 started with 8 warbands
  with Circle of Swords", "p1 started with the Grand Scepter with Scepter
  Bearer", and one line per site, "p3 placed a warband at Deep Woods with
  Iron Hand" and "p3 placed a warband at Deep Woods with Rival". *Reason:*
  game start is not journaled, so Setup's walk is the first place the log
  can say what these powers did; a line per site reads as a pawn's line
  does.
- **R8. The game-start pass lives in the plan and the materializer.** Only
  Scepter Bearer is applied in `LaterGameSetup`. *Reason:* the boards and
  the map are built by `FirstGameSetupMaterializer`; the spec's
  "`LaterGameSetup` gains a legacy pass" names the wrong place for three
  of the four.
- **R9. Iron Hand places no warbands in an exile-only game.** *Superseded
  by R40:* a first game played exile-only wipes every stored legacy, so no
  Iron Hand is held there. In
  `SetupMode.ExileOnly` the holder's warbands go to no site, and every
  player may still place their pawn at any site. *Reason:* spec ruling 7
  places the holder's warbands only on sites restored up to the Empire
  divider, and an exile-only Chronicle stores no Empire site, so there are
  none. *Slice/files:* slice 1; `StartingLegacies.of`,
  `IronHandSetupSuite`.
- **R10. Setup's new steps may shift a walk saved mid-Setup.** The Notes
  that open Setup shift its node paths, so a game saved mid-Setup by an
  earlier build may not resume correctly; this applies to every step
  slices 2 to 4 insert into Setup too. *Reason:* the project makes no
  mid-phase save compatibility promise across builds (Legacy Chronicle
  powers R15). *Slice/files:* slice 1; `SetupProcedure.tree`,
  `StartLines`.
- **R17. Rival places no sites in an exile-only game.** *Superseded by
  R40:* a first game played exile-only wipes every stored legacy, so no
  Rival is held there. Game start counts
  the exile-only game's eight sites as placed before Rival
  (`StartingLegacies.placedBefore`), so Rival's "three Cradlemost empty
  slots" are none. *Reason:* an exile-only game fills all eight slots at
  game start, before the foundations. *Confidence:* low; the exile-only
  mode is the owner's own (2026-11-12) and has no rulebook timing.
  *Slice/files:* slice 1 (recorded during slice 2);
  `StartingLegacies.placedBefore`, `RivalSetupSuite`.

## Slice 2

[Plan](../../plans/2026-11/2026-11-13-legacy-setup-powers-slice2.md).

- **R11. World Crafter's Imperial Maps lines.** One line per slot, "p2
  placed Deep Woods from Recent Sites with World Crafter", replaces both
  the "filled the map from" line and the per-site "placed X on the map"
  lines. *Reason:* one line per placement, and no site is named twice.
  *Slice/files:* slice 2; `WorldCrafter.mapStep`.
- **R12. World Crafter's last slot.** World Crafter asks for a slot only
  while two or more Atlas sites remain, counted from what game start fixed
  less the earlier slots; a slot with one site left takes it with no
  question, and its line says "from Recent Sites". *Reason:* with one site
  the choice is empty; this matches Imperial Maps and Explore.
  *Slice/files:* slice 2; `WorldCrafter.mapStep`.
- **R13. World Crafter held by the Chancellor.** The holder fills slot by
  slot. *Reason:* the power changes how the map is filled, not only who
  fills it. *Slice/files:* slice 2; `ImperialMaps.step`.
- **R14. Populist's question.** One choose-one, Decline first, asked
  whenever a seated holder holds Populist active, even when dividing
  changes nothing. *Reason:* the step keeps one shape, which keeps resume
  safe, and Decline is always offered (spec ruling 3). *Slice/files:*
  slice 2; `Populist`.
- **R15. Populist with no remainder.** A second line key: "p2 divided the
  favor evenly among the banks with Populist". *Reason:* "and took 0
  favor" says nothing. *Slice/files:* slice 2; `Populist`.
- **R16. When Matchmaker's holder sees another player's draw.** The holder
  sees another player's two drawn legacies only while that player's keep
  decision, `setup.legacy-choice.<owner>`, is pending, which the
  projection reads from the parked decision, so no field is saved. Before
  and after that, the draw shows as backs to the holder and to the owner.
  Once one is kept, it is named to its owner alone, and the returned one to
  nobody. The question is headed "Matchmaker: keep a legacy for another
  player". *Reason:* spec ruling 13 says the holder sees them "while
  choosing", and an earlier view would inform the holder's World Crafter
  and Populist choices, which come first. *Slice/files:* slice 2;
  `LegacyKeep`, `GamePresentationProjector.identifiesCard`.
  *Superseded 2026-11-15* by [catalog batch 13](2026-11-14-catalog-batch-13-design.md), ruling 6: the holder draws into their own hand, so the hand rule names the draw to them alone.

## Slice 3

[Plan](../../plans/2026-11/2026-11-13-legacy-setup-powers-slice3.md).

- **R18. ChooseAmounts' answer.** `DecisionQuery.ChooseAmounts` reuses
  `DistributeSlot` for its slots and is answered by
  `DecisionAnswer.DistributeAnswer`, sent as the `DistributeWire`; no new
  answer form. *Reason:* no new saved or wire format, and the answer's
  shape, one amount per slot, is the same. *Slice/files:* slice 3;
  `Decisions.scala`, `DecisionQueries`.
- **R19. Secret Society's reshuffle.** A new `ShufflePile.VisionPackets`,
  journaled like any shuffle, with the operation-codec kind
  `{"kind": "vision-packets"}`; older streams still decode, which a test
  pins. *Reason:* a plain shuffle would reorder the denizens, against spec
  ruling 14. *Slice/files:* slice 3; `ShufflePile`, `WalkerShuffles`,
  `CardOperationCodec`.
- **R20. How each holder sees the Visions.** Secret Society records no
  `Peek` and shows no deck position: while its question is parked, the
  holder learns the Visions' names only through the question's options,
  and the world deck stays hidden. Seer records a `Peek` of each Vision for
  the holder when its answer applies, and the holder keeps that knowledge
  as any `Peek` is kept. *Reason:* Secret Society acts "before building the
  world deck", so the holder learns which Visions exist, not where they
  lie; after a take the deck is rebuilt at random, and after a Decline a
  lasting view would leak the order. Seer's card says "peek at the
  Visions", and the holder chose their positions. *Slice/files:* slice 3;
  `SecretSociety`, `Seer`, `GamePresentationProjector.identifiesCard`.
- **R21. Secret Society's excess discard.** After the holder's own
  `SetupPawnPlaced` window and before their keep, only when they are over
  the adviser limit: one choose-many of exactly the excess, over the
  advisers by slot, `setup.legacy.secret-society.<player>.discard`, with no
  Decline. The cards go where Setup's rejected advisers go. Setup writes no
  line for discarded advisers, so a line of its own names the discarded
  cards, each by the log's knowledge rule: regional discard piles are
  hidden, so a seat that did not know a card reads "a Denizen" or "a
  Vision". *Reason:* the spec puts the discard right after the pawn, and
  the limit rule is mandatory; the line says what happened without naming
  a card to a seat the discard pile hides it from. *Slice/files:* slice 3;
  `SecretSociety.excessStep`.
- **R22. Secret Society's question.** `setup.legacy.secret-society.<player>`,
  a choose-one: Decline, then the deck's Visions except the Conspiracy in
  catalog order (by card id), headed "Secret Society: you may take a Vision
  as a facedown adviser"; asked whenever a seated holder holds it active
  and the deck holds such a Vision. *Reason:* deck order would leak the
  Visions' relative positions after a Decline. *Slice/files:* slice 3;
  `SecretSociety`.
- **R23. Seer's question.** `setup.legacy.seer.<player>`: one slot per
  Vision, each from 1 to n + v, distinct, suggesting the current
  positions; headed "Seer: place the Visions in the world deck", confirmed
  "Place the Visions"; no Decline. It is skipped only when the query cannot
  be formed: no Vision in the deck, or a deck of one card. *Reason:* spec
  ruling 15 has no "may". *Slice/files:* slice 3; `Seer`.
- **R24. One window for both.** Secret Society and Seer both hook
  `SetupWorldDeck`, Secret Society first by registration order, which a
  test pins. *Reason:* spec rulings 14 and 15 put Secret Society before
  Seer, and one window is enough. *Slice/files:* slice 3;
  `LegacySetupPowers.all`.
- **R25. The ChooseAmounts control.** Distribute-style rows with `−` and
  `+` steppers, Shift+click to the bound, and an editable number field on
  each row bound to the same `SessionDrafts` draft. Confirm stays disabled
  on a clash, with the line "Two cards share position N". *Reason:* in a
  40-card deck steppers alone mean dozens of clicks; the draft in
  `SessionDrafts` keeps a typed value across redraws. *Slice/files:*
  slice 3; `ChooseAmountsPanelRenderer`, `ChooseAmountsDecisionState`.
- **R26. Seer's peeks write no line.** Setup joins
  `DetailLines.knowledgeOnlyPeeks`, so Seer writes only "p2 placed the
  Visions with Seer"; its peeks stay knowledge only, and the holder keeps
  knowing the Visions. *Reason:* as Legacy Chronicle powers R47, the peeks
  are knowledge, not events worth a line each. *Slice/files:* slice 3
  (recorded after it); `DetailLines`, `SetupLegacyLogSuite`, the
  `later-game-setup-world-deck` golden.

## Slice 4

[Plan](../../plans/2026-11/2026-11-13-legacy-setup-powers-slice4.md).

- **R27. Who placed, inside `SetupPawnPlaced`.** The placer is the latest
  `setup.pawn-placement.*` answer, and the site is none if the player
  stayed off. Inside the window the edifice powers come first, then The
  Standard Bearer, then Wealthy, by registration order, which a test pins.
  *Reason:* reading the last answer breaks as soon as a legacy asks inside
  the window. *Slice/files:* slice 4; `SetupPawnPlacement`,
  `EdificeSetupSupport`.
- **R28. The Standard Bearer.** `setup.legacy.the-standard-bearer.<player>`:
  Decline first, then the banners in the shared bank, the People's Favor
  before the Darkest Secret, asked only when one is there. The banner goes
  to the holder, then two of its own resource go onto it from the shared
  bank, which has no count, so it always gives two. Line: "p2 took the
  People's Favor with The Standard Bearer". *Reason:* spec ruling 16; "as
  many as the bank holds" is two when the bank is unbounded.
  *Slice/files:* slice 4; `TheStandardBearer`.
- **R29. Wealthy.** `setup.legacy.wealthy.<player>`: "Take 1 favor from
  two players", offered only when two or more other players hold favor,
  then "Take 3 favor from the shared bank"; with one option nothing is
  asked. Then `setup.legacy.wealthy.<player>.players`, a choose-many of
  exactly two, in turn order. No Decline. Lines: "p2 took 1 favor from p1
  and p3 with Wealthy", "p2 took 3 favor from the shared bank with
  Wealthy". *Reason:* spec ruling 17. *Slice/files:* slice 4; `Wealthy`.
  Amended by R34 for exactly two givers.
- **R30. Quest's pawn step.** The holder's pawn question adds "Don't place
  your pawn" after the sites; staying off writes "p2 placed no pawn with
  Quest to Distant Lands". The `SetupPawnPlaced` window still runs, so The
  Standard Bearer and Wealthy act, but no edifice power fires. The steps
  that read the pawn's region (Secret Society's excess discard, Choose
  Adviser, Reveal Cards) are skipped, keyed on the missing pawn. *Reason:*
  spec ruling 18; keying on the missing pawn matches ruling 20.
  *Slice/files:* slice 4; `SetupProcedure`, `QuestToDistantLands`.
- **R31. Quest's swap.** In Setup, after the keep and before the point
  where Choose Adviser would run, only for a holder with no pawn:
  `setup.legacy.quest.<player>`, Decline then the Dispossessed in catalog
  order, seen through a `Peek` of each card, which the holder keeps as The
  Bright Beacon's holder does; then `setup.legacy.quest.<player>.give`, no
  Decline, over their facedown advisers that are not Visions, or their
  hand when they have none. The cards trade places. Line: "p2 swapped a
  card with the Dispossessed with Quest to Distant Lands". *Reason:* the
  holder looks through the Dispossessed at the table; a Secret Society
  Vision cannot be given, since spec ruling 19 offers starting advisers
  only and the Vision is not one (spec ruling 14). *Slice/files:* slice 4;
  `QuestToDistantLands`.
- **R32. The first Wake.** A new `TriggeredProcedureRef.QuestFirstWake`,
  journal key `"quest-first-wake"`, started at the start of a pawnless
  player's Wake before the Wake checks and Forced Wake: the placement
  (`wake.legacy.quest.<player>`, any site in play, no Decline), "p2 placed
  their pawn at Deep Woods", then Choose Adviser
  (`wake.legacy.quest.<player>.adviser`), Secret Society's excess discard
  (`.discard`) and Reveal Cards (`.reveal`). A test pins that older streams
  still decode.
  *Reason:* spec ruling 20 says "before anything else"; folded into Forced
  Wake it would run too late. *Slice/files:* slice 4;
  `QuestFirstWakeProcedure`, `OathDriver`, `ProcedureRef`.
- **R33. The pawn-site audit.** A pawnless player stands at no site for
  every other player's rule, and no read fails with `PawnSiteMissing`. A
  reader that must decide something else about a pawnless player is
  reported, not guessed. *Reason:* spec ruling 20. *Slice/files:* slice 4;
  the readers the audit finds.
- **R34. Wealthy with exactly two givers** (amends R29). When exactly two
  other players hold favor, both give and no choose-many is asked.
  *Reason:* a `ChooseMany(2,2)` over two options has one answer.
  *Slice/files:* slice 4; `Wealthy`.
- **R35. The first Wake's confirm label.** The placement's Confirm reads
  "Place your pawn at" and the site, not Setup's "Start at". *Reason:* the
  Wake is not Setup, and the question reads "Place your pawn at any site".
  *Slice/files:* slice 4; `QuestFirstWakeProcedure`.
- **R36. The world deck's legacy powers beside Oaths' Teeming World
  burials.** Spec ruling 14, R19 and R20 hold with two rulings of the
  Legacy Chronicle powers batch: under Teeming World, Secret Society's
  rebuild shuffles only the Visions and keeps the Oaths burials below them
  in their order (its R49), and after Seer's placement or Secret Society's
  rebuild the Oaths holder peeks their buried cards again (its R50).
  *Reason:* those rulings. *Slice/files:* slice 3; `WalkerShuffles`,
  `OathsOfLoyalty`, `Seer`, `SecretSociety`.
- **R37. Quest's swap needs a card to give.** The swap, and its Peek of
  each Dispossessed card, comes only when the Dispossessed holds a denizen
  and the holder has a card to give; otherwise nothing is asked or shown.
  *Reason:* a swap needs a card on both sides, and the Peeks would reveal
  the Dispossessed for nothing. *Slice/files:* slice 4;
  `QuestToDistantLands`.
- **R38. Secret Society's Vision at Reveal Cards.** An Exile may reveal the
  facedown Vision Secret Society gave them at Reveal Cards, in Setup or at
  the first Wake. Revealing it places it on their Vision space exactly as
  a faceup Vision play does, discarding any Vision already there, and
  triggers no When Played power. An Imperial holder cannot reveal it.
  *Reason:* CR p. 13 lets a player reveal any adviser but a site-only one,
  and the rules on playing Visions say Exiles can reveal Visions and the
  Chancellor and Citizens cannot. No player held a Vision at Setup before
  slice 3, so `revealable` never had to offer one. *Slice/files:* slice 4;
  `SetupAdviserSteps`, `CardPlay.hasVisionSpace`.
- **R39. Iron Hand's previous-game clause.** (Owner re-ruling of spec
  ruling 7's first sentence, 2026-11-14; the printed text wins.) Iron
  Hand's SETUP half applies only when the holder's lineage was in the
  previous game: both clauses, the holder's warbands on the sites up to
  the Empire divider and every player placing their pawn at any site, in
  Setup With an Empire and With No Empire. A holder whose lineage sat it
  out does nothing at setup: Imperial warbands or bandits as usual, and
  the usual pawn sites. "Was in the previous game" is the lineage's
  stored `previousPlayer`; the Chronicler now records a lineage that sits
  a game out with none, so the field names only the immediately previous
  game and survives an era reset, which the scorepad would not. Saved
  Chronicles are not migrated; they correct themselves after one game.
  *Second look:* a TTS-imported Chronicle carries no previous player, so
  Iron Hand does nothing after a TTS import. *Reason:* the printed text,
  which the owner ruled to favor. *Slice/files:* re-ruling;
  `ChronicleRecord`, `StartingLegacies`, `SetupLegacyRules.ironHand`,
  `SetupPawnSites`, `StartLines`.
- **R40. A first game played exile-only starts with no legacy.** (Owner
  ruling, 2026-11-14; supersedes R9 and R17.) `SetupMode.ExileOnly` is
  renamed `FirstGameExileOnly`. That mode wipes the seated and boxed
  lineages' stored legacies at game start, because a first game has none:
  they return to the derived legacy deck before the legacy deal, so they
  can be drawn, and the deal's card accounting stays exact. The wipe needs
  no new random outcome and no new journaled field: the plan factory
  shuffles the starting Chronicle's deck into the `legacyOrder` the Begin
  command already journals. What a `legacyOrder` must hold has changed,
  though, so a `GameStarted` journaled by an earlier build may not replay
  (R42). Starting advisers and starting relics are unchanged. No legacy
  acts in this mode. The setup mode is read from the Chronicle as saved,
  before the wipe; the game's baseline is the wiped Chronicle, so when
  stored legacies were a Chronicle's only history its pawns are placed as
  in a first game (R41). The projection's starting digest is the saved
  Chronicle's (`CampaignState.saved`), so a browser's saved entry still
  matches it. *Reason:* the owner's ruling. *Slice/files:* re-ruling;
  `EmpireSetupRules.startingChronicle`, `GameStartRules`,
  `GeneratedFirstGamePlanFactory`, `GameProjection`.
- **R41. A legacies-only history, once wiped.** When stored legacies were
  a Chronicle's only history, the wiped baseline of its first game played
  exile-only carries none: the game places its sites exile-only and its
  pawns as in a first game, at any site. *Second look:* this is the one
  place the mode and the baseline disagree. *Reason:* the owner named this
  mode a first game, and once its legacies are wiped such a Chronicle
  differs from a first game in its site placement and a few table facts:
  the Citizenship powers stay on (only a first game ignores them), the
  Reliquary is set aside, and the first player and Usurper Limiter start
  as in Setup With No Empire. Making the pawn rule follow the mode would
  add a saved field, or spread the wipe across four readers, for a rare
  import case. *Slice/files:* re-ruling; `SetupPawnSites`,
  `ExileOnlySetupSuite`.
- **R42. Games journaled before R39 and R40 may not replay.** A
  `GameStarted` journaled by an earlier build no longer replays in two
  cases: an exile-only Chronicle with stored legacies, whose journaled
  `legacyOrder` game start now refuses ("the deal does not hold the legacy
  deck"); and a Chronicle whose Iron Hand holder's `previousPlayer` reads
  differently under R39. No release note or doc claims compatibility for
  such games. *Reason:* the alpha data policy treats releases as
  incompatible by default, the same trade-off as R10. *Slice/files:*
  re-ruling; `GameStartRules`, `ChronicleRecord`.
