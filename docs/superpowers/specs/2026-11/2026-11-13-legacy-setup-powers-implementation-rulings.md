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
- **R2. An Exile's warbands run out in a fixed order.** An Exile's 14
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
- **R4. Only an Exile's Iron Hand acts at setup.** *Reason:* an Imperial
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
- **R9. Iron Hand places no warbands in an exile-only game.** In
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
