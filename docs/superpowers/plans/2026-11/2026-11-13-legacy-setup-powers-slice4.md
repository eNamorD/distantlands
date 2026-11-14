# Legacy Setup Powers, Slice 4: Player Choices Implementation Plan

> Executed inline by the implementer.

**Goal:** The last three SETUP powers act in each player's own Setup
step. The Standard Bearer's holder may take a banner from the shared bank
and load it. Wealthy's holder takes favor from two players or from the
bank. Quest to Distant Lands' holder may stay off the map, may swap a card
with the Dispossessed, and places their pawn at the start of their first
Wake, where the steps Setup deferred for them run.

**Architecture:** The Standard Bearer and Wealthy hook
`PowerWindow.SetupPawnPlaced`; they and the edifice powers there read the
placing player from the latest pawn-placement answer, not the last answer,
so a decision asked inside the window keeps the window's shape on resume
(R27). The pawn step offers Quest's holder "Don't place your pawn"
(`SetupLegacyRules.mayStayOff`); a player with no pawn skips the steps that
read the pawn's region (Secret Society's excess discard, Choose Adviser,
Reveal Cards) and, if they hold Quest, is offered the swap. A new
triggered procedure, `TriggeredProcedureRef.QuestFirstWake`, started by
the driver at the start of a pawnless player's Wake, places the pawn and
runs the deferred steps, built from step builders shared with Setup
(`SetupAdviserSteps`). An audit makes every reader of another player's
pawn site treat a pawnless player as standing at no site.

**Tech Stack:** Scala 3.9.0 (JVM and Scala.js), munit, `./sbtw`; frontend
suites with `./sbtw "frontend/test"`. Compiler flags
`-deprecation -feature -unchecked -Wunused:imports,privates,locals,implicits,nowarn -Werror`.

**Spec:** [Legacy Setup powers design](../../specs/2026-11/2026-11-13-legacy-setup-powers-design.md),
rulings 1-4 and 16-20 and section "Slice 4: player choices".
[Implementation rulings](../../specs/2026-11/2026-11-13-legacy-setup-powers-implementation-rulings.md)
(R1-R26 stand; this plan proposes R27-R33). Card text in
`src/main/scala/distantlands/catalog/holding/HoldingLegacies.scala` (The
Standard Bearer L18, Quest to Distant Lands L30, Wealthy L31). CR p. 13
(Setup's player choices) and the Wake rules
(`docs/rules/rules-reference.md#wake`).

## Prerequisite

Slice 3 has landed. Check before Task 1:

```bash
grep -q "case object SetupWorldDeck" src/main/scala/distantlands/model/PowerWindow.scala &&
grep -q "object Seer" src/main/scala/distantlands/gameplay/setup/legacies/Seer.scala &&
grep -q "TriggeredProcedureRef.Setup)" src/main/scala/distantlands/application/gamelog/DetailLines.scala &&
echo ready
```

Expected: `ready`.

## Open questions (proposed rulings R27-R33)

The plan is written to the recommendation of each. Production code waits
for the orchestrator's rulings.

- **R27 (Task 1). Who placed, inside `SetupPawnPlaced`.**
  `EdificeSetupSupport.pawnPlacement` reads the last answer, which a
  decision asked inside the window (The Standard Bearer, Wealthy) would
  change on resume, so the edifice powers' operations would vanish from
  the refolded window and the walk would lose its place. *Recommended:*
  the placer is the latest answer to a pawn-placement decision
  (`setup.pawn-placement.*`), read by a shared `SetupPawnPlacement`; the
  site is that answer's site, or none when the player stayed off (R30).
  Within the window the edifice powers come first and the legacies after,
  The Standard Bearer before Wealthy, by registration order, which a test
  pins (the ROADMAP's "Simultaneous setup effects" stays open).
- **R28 (Task 2). The Standard Bearer's question.**
  `setup.legacy.the-standard-bearer.<player>`: a choose-one, Decline first,
  then each banner in the shared bank (the People's Favor, the Darkest
  Secret) in that order, headed "The Standard Bearer: you may take a
  banner from the shared bank"; asked only when a banner is there. The
  banner moves to the holder, then two of its own resource move from the
  shared bank onto it (the shared bank has no count, so it always gives
  two). Line: "p2 took the People's Favor with The Standard Bearer".
- **R29 (Task 3). Wealthy's questions.** `setup.legacy.wealthy.<player>`:
  a choose-one, "Take 1 favor from two players" first when at least two
  other players hold favor, then "Take 3 favor from the shared bank";
  with one option no question. Then `setup.legacy.wealthy.<player>.players`,
  a `ChooseMany(2, 2)` over the other players who hold favor, in turn
  order. No Decline: the card has no "may". Lines: "p2 took 1 favor from
  p1 and p3 with Wealthy", "p2 took 3 favor from the shared bank with
  Wealthy".
- **R30 (Task 4). Quest in Setup's pawn step.** The holder's pawn decision
  adds a button, "Don't place your pawn", after the sites. Staying off
  writes "p2 placed no pawn with Quest to Distant Lands". The
  `SetupPawnPlaced` window still runs, so The Standard Bearer and Wealthy
  act, but no edifice power fires (no site). Every step that reads the
  pawn's region is skipped for a player with no pawn, keyed on the missing
  pawn, not on the legacy: Secret Society's excess discard, Choose Adviser
  and Reveal Cards.
- **R31 (Task 4). Quest's swap.** After the holder's keep, only when they
  placed no pawn. `setup.legacy.quest.<player>`: a choose-one, Decline
  first, then every Dispossessed card in catalog order, headed "Quest to
  Distant Lands: you may swap a card with the Dispossessed". The holder
  sees the Dispossessed by a `Peek` of each card, recorded as the question
  is reached, as The Bright Beacon does (Legacy Chronicle powers R22); Setup
  peeks write no line (R26). Then `setup.legacy.quest.<player>.give`: a
  choose-one, no Decline, over their facedown advisers that are not Visions
  (a Secret Society Vision is neither a starting adviser nor a Foundation
  IV draw), or, with none, their hand. The cards trade places: a facedown
  adviser's replacement joins the advisers facedown, a hand card's
  replacement joins the hand, and the given card goes to the Dispossessed.
  Line: "p2 swapped a card with the Dispossessed with Quest to Distant
  Lands".
- **R32 (Task 5). The first Wake.** A new triggered procedure,
  `TriggeredProcedureRef.QuestFirstWake` (journal key
  `"quest-first-wake"`), which the driver starts at the start of a Wake
  whose player has no pawn, before the Wake checks and Forced Wake. Its
  tree: `wake.legacy.quest.<player>`, a choose-one of every site in play,
  no Decline; the pawn's placement; "p2 placed their pawn at Deep Woods";
  then Choose Adviser (`wake.legacy.quest.<player>.adviser`, with Oaths'
  burial), Secret Society's excess discard
  (`wake.legacy.quest.<player>.discard`) and Reveal Cards
  (`wake.legacy.quest.<player>.reveal`), built by the same step builders
  as Setup's. No `SetupPawnPlaced` power fires. *Alternative:* fold the
  placement into Forced Wake, which runs after the Wake checks and reuses
  its key.
- **R33 (Task 6). The pawn-site audit.** A player with no pawn stands at
  no site: every rule that reads another player's pawn site treats them as
  absent from every site, and no such read fails with `PawnSiteMissing`.
  The holder's own actions cannot run before their placement, which opens
  their Wake, so the audit covers other players' turns, round and turn
  boundaries, and Setup's remaining steps. A reader the audit finds that
  must decide something about the pawnless player is reported, not
  guessed.

## Global Constraints

- Scala 3 with `-Werror`: no unused imports, private members or locals, no
  non-exhaustive matches; every Scala file at or under 800 lines
  (`scripts/check-architecture.py`); `gameplay/actions` imports nothing
  from `gameplay.powers`.
- A new test names cards by their card objects.
- Only seated players' active legacies act (spec ruling 1); every "you
  may" has its own id and a Decline (ruling 3); several players or cards
  are one choose-many (ruling 4).
- No `Branch`'s children or ids depend on state the walk changes; the
  window's folds read the placer from the pawn answer (R27). Wealthy's
  options are read from the favor before its question, which nothing in
  the window changes before it.
- Every new `Decide` has a resume test; no two decisions share an id.
- Hidden information: the Dispossessed and the swapped cards are the
  holder's only, each with a redaction test per viewer.
- Old saved streams still decode; the only new saved form is R32's
  procedure key. A game saved mid-Setup or mid-Wake by an earlier build may
  not resume (R10).
- TDD, one commit per task; coverage at or above 92% statements and 84.4%
  branches; note-key and unimplemented snapshots regenerated and reviewed
  in each task that changes them; a golden generated only for its script;
  `git status --short src/test/resources` otherwise clean.
- Conventional Commits ending `Co-Authored-By: Claude Opus 5.5
  <noreply@anthropic.com>`; explicit pathspecs; `git log -3` after each
  commit. Another session commits in this tree: never stage its files.

## The gate

```bash
./sbtw clean coverage test coverageReport
./sbtw "frontend/test"
python3 scripts/check-architecture.py
python3 scripts/check-markdown-links.py
git status --short src/test/resources
```

Then the running-app check of Task 7, Step 4, with screenshots or page
text in the report.

---

### Task 1: The placer inside `SetupPawnPlaced`

**Files:**
- Create: `src/main/scala/distantlands/gameplay/setup/SetupPawnPlacement.scala`
- Modify: `src/main/scala/distantlands/gameplay/powers/setup/EdificeSetupSupport.scala` (`pawnPlacement` reads it)
- Create: `src/test/scala/distantlands/gameplay/setup/SetupPawnPlacementSuite.scala`

**Interfaces:**
- Produces: `SetupPawnPlacement.latest(answered: Vector[Answered]): Option[(PlayerId, Option[SiteId])]` — the latest pawn-placement answer's player, and its site when it names one; `SetupPawnPlacement.isPawnDecision(id: String): Boolean`.

- [ ] **Step 1: Write the failing test**

```scala
package distantlands.gameplay.setup

import distantlands.model._
import distantlands.model.DecisionAnswer.ChooseOneAnswer

/** Who placed, read inside `SetupPawnPlaced` (R27): the latest pawn
  * answer, whatever was answered after it in the window. */
class SetupPawnPlacementSuite extends munit.FunSuite:
  private val (p1, p2) = (PlayerId("p1"), PlayerId("p2"))
  private val site = SiteId("site:deep-woods")
  private def pawn(player: PlayerId, ref: DecisionOptionRef) =
    Answered(SetupProcedure.pawnDecisionId(player), ChooseOneAnswer(ref),
      player)

  test("the latest pawn answer names the placer and the site, though a " +
      "window's question was answered after it"):
    val answered = Vector(pawn(p1, DecisionOptionRef.Site(SiteId("site:a"))),
      pawn(p2, DecisionOptionRef.Site(site)),
      Answered("setup.legacy.wealthy.p2", ChooseOneAnswer(
        DecisionOptionRef.Button("bank")), p2))
    assertEquals(SetupPawnPlacement.latest(answered), Some(p2 -> Some(site)))

  test("a player who stayed off placed at no site"):
    assertEquals(SetupPawnPlacement.latest(Vector(pawn(p2,
      DecisionOptionRef.Button("stay-off")))), Some(p2 -> None))

  test("no pawn answer, no placer"):
    assertEquals(SetupPawnPlacement.latest(Vector.empty), None)
```

Also extend `GreatForgeRulesSuite` (it already folds the window): a fake
power hooked at `SetupPawnPlaced`, registered after Great Forge, that asks
one decision; the walk parks there, the answer is given to a walk rebuilt
from the journal, and Great Forge's relic is drawn exactly once. Read the
suite's own fake-power helpers first.

- [ ] **Step 2: Run it and see it fail**

Run: `./sbtw "testOnly distantlands.gameplay.setup.SetupPawnPlacementSuite distantlands.gameplay.powers.setup.GreatForgeRulesSuite"`
Expected: compile error; with the object stubbed, the Great Forge resume
test fails (its relic drawn twice or the walk refusing the path).

- [ ] **Step 3: Implement**

```scala
package distantlands.gameplay.setup

import distantlands.model._

/** The placement a `SetupPawnPlaced` window belongs to (R27): the latest
  * answer to a pawn-placement decision, whatever the window has asked
  * since. A power hooked there reads it, so a question it asks does not
  * change which player the window is for when the walk is rebuilt. */
object SetupPawnPlacement:
  private val prefix = "setup.pawn-placement."

  def isPawnDecision(id: String): Boolean = id.startsWith(prefix)

  def latest(answered: Vector[Answered])
      : Option[(PlayerId, Option[SiteId])] =
    answered.reverse.collectFirst {
      case Answered(id, DecisionAnswer.ChooseOneAnswer(ref), by)
          if isPawnDecision(id) => by -> (ref match
            case DecisionOptionRef.Site(site) => Some(site)
            case _ => None)
    }
```

`SetupProcedure.pawnDecisionId` uses the same prefix (make the prefix one
constant). `EdificeSetupSupport.pawnPlacement`, Setup branch:
`SetupPawnPlacement.latest(ctx.answered).flatMap((player, site) =>
site.map(player -> _))`, and its scaladoc says why.

- [ ] **Step 4: Run it and see it pass**

Run: `./sbtw "testOnly distantlands.gameplay.setup.* distantlands.gameplay.powers.setup.* distantlands.application.gamelog.GameLogGoldenSuite"`
Expected: every test passes; goldens unchanged.

- [ ] **Step 5: Commit**

`fix(setup): read who placed from the latest pawn answer inside SetupPawnPlaced`

### Task 2: The Standard Bearer

**Files:**
- Create: `src/main/scala/distantlands/gameplay/setup/legacies/TheStandardBearer.scala`
- Modify: `HoldingLegacies.scala` (remove `TheStandardBearerCard`), `LegacySetupPowers.scala`, `SetupLegacyRules.scala` (`standardBearer`)
- Modify: tests importing `TheStandardBearerCard` from `catalog.holding` (`git grep -ln TheStandardBearerCard -- src/test`)
- Modify: the two snapshots
- Create: `src/test/scala/distantlands/gameplay/setup/legacies/TheStandardBearerSuite.scala`

**Interfaces:**
- Consumes: `SetupPawnPlacement.latest` (Task 1).
- Produces: `TheStandardBearerCard`, `case object TheStandardBearer extends ContributingPower` (hooks `SetupPawnPlaced`); `TheStandardBearer.decisionId(player)`, `.decline`, `.took: NoteKey`; `SetupLegacyRules.standardBearer(state): Option[PlayerId]`.

- [ ] **Step 1: Write the failing test**

`TheStandardBearerSuite` (l2 a Citizen holds it; production rules;
Setup parked as the slice 3 suites park it):

```scala
  test("with both banners in the bank the holder is offered Decline, the " +
      "People's Favor and the Darkest Secret, after their pawn's line")
  test("a walk resumed at the question takes the banner and puts two of " +
      "its resource on it")
  test("with one banner in the bank only it is offered")
  test("with no banner in the bank nothing is asked")
  test("declining changes nothing and writes no line")
  test("the line: p2 took the People's Favor with The Standard Bearer")
  test("another player's pawn does not ask the holder")
  test("the card has left holding and its power is registered")
```

The later-game fixture's Oathkeeper goal is Supremacy, so both banners
start in the shared bank (`LaterGameSetup` gives the Chancellor a banner
only for The People or Devotion). For one banner, set the Chronicle's
`oathkeeperGoal` to `OathkeeperGoal.ThePeople`; for none, give the other
banner to p1 through the same goal and take it with a `Table` step, or
skip the case if no Chronicle starts with both banners held, and say so.
Check the banner after the take: `ready.game.current.banners.peoplesFavor`
holder is p2 and its favor is 1 + 2 = 3. The resume test parks at the
question with `parkedAt`, answers it, and parks at p2's keep.

- [ ] **Step 2: Run it and see it fail**

Run: `./sbtw "testOnly distantlands.gameplay.setup.legacies.TheStandardBearerSuite"`
Expected: compile error, `TheStandardBearerCard` is not found in
`gameplay.setup.legacies`.

- [ ] **Step 3: Implement**

```scala
/** The Standard Bearer (legacy L18), its SETUP power (Legacy Setup powers
  * design, ruling 16). In the holder's own `SetupPawnPlaced` window, after
  * the pawn's line, the holder may take a banner from the shared bank and
  * put two more of its own resource on it from the shared bank (R28). */
case object TheStandardBearer extends ContributingPower:
  val id: PowerId = TheStandardBearerCard.power.id
  def source: RuleSourceRef = RuleSourceRef.GameRule(id.value)
  def contributions: Map[PowerWindow, Vector[Contribution]] = Map(
    PowerWindow.SetupPawnPlaced -> Vector(Transform((ctx, ops) =>
      SetupPawnPlacement.latest(ctx.answered).map(_._1)
        .filter(player => SetupLegacyRules.standardBearer(ctx.state)
          .contains(player))
        .fold(ops)(holder => ops :+ step(holder)))))

  def decisionId(player: PlayerId): String =
    s"setup.legacy.the-standard-bearer.${player.value}"
  val decline: DecisionOptionRef.Button = DecisionOptionRef.Button("decline")

  /** The banners the shared bank holds, the People's Favor first. */
  private def inBank(state: ReadyGame): Vector[Banner] =
    Vector(Banner.PeoplesFavor -> state.game.current.banners.peoplesFavor
        .holder, Banner.DarkestSecret -> state.game.current.banners
        .darkestSecret.holder).collect { case (banner, None) => banner }

  private def step(holder: PlayerId): Operation = Branch((state, _) =>
    inBank(state) match
      case Vector() => Vector.empty
      case banners => Vector(
        Decide(decisionId(holder), holder, DecisionQuery.ChooseOne(
          DecisionOption.Button(decline, "Decline") +: banners.map(banner =>
            DecisionOption.Banner(DecisionOptionRef.Banner(banner))),
          heading = Some(
            "The Standard Bearer: you may take a banner from the shared bank"),
          confirmVerb = Some("Take"))),
        Branch((_, pending) =>
          PowerAnswers.one(pending, decisionId(holder)).toVector.collect {
            case DecisionOptionRef.Banner(banner) => banner
          }.flatMap(banner => Vector(
            BuildOps((_, _) => Right(Vector(
              Move(Piece.Banner(banner), PositionedLocation(Location.SharedBank),
                PositionedLocation(Location.PlayArea(holder))),
              Move(resource(banner), PositionedLocation(Location.SharedBank),
                PositionedLocation(Location.OnBanner(banner))))),
              required = true),
            Note(noteId, _ => Some(took.sourceless(NoteArg.Player(holder),
              NoteArg.Banner(banner), NoteArg.Card(TheStandardBearerCard.id))))))))
```

with `resource(PeoplesFavor) = Piece.Favor(2)`,
`resource(DarkestSecret) = Piece.Secrets(2)`, `took` = "{0} took {1} with
{2}" on Setup's note id. Check the real names of `Banner`'s cases,
`DecisionOptionRef.Banner`, how a banner is moved to a player in a walk
(grep `Piece.Banner(` in `gameplay`: the Challenge or Raid moves it; use
that operation if it is not a plain `Move`), and how secrets are placed on
the Darkest Secret (their side). `LegacySetupPowers.all` appends
`TheStandardBearer`.

- [ ] **Step 4: Run it and see it pass**

Run: `./sbtw "testOnly distantlands.gameplay.setup.* distantlands.gameplay.chronicle.* distantlands.catalog.*"`

- [ ] **Step 5: Regenerate the snapshots and commit**

`feat(legacies): The Standard Bearer takes a banner from the shared bank after their pawn`

### Task 3: Wealthy

**Files:**
- Create: `src/main/scala/distantlands/gameplay/setup/legacies/Wealthy.scala`
- Modify: `HoldingLegacies.scala` (remove `WealthyCard`; its Foundation II alteration stays on the card), `LegacySetupPowers.scala` (after `TheStandardBearer`), `SetupLegacyRules.scala` (`wealthy`)
- Modify: tests importing `WealthyCard` from `catalog.holding`
- Modify: the two snapshots
- Create: `src/test/scala/distantlands/gameplay/setup/legacies/WealthySuite.scala`

**Interfaces:**
- Produces: `WealthyCard`, `case object Wealthy extends ContributingPower` (hooks `SetupPawnPlaced`); `Wealthy.decisionId(player)`, `Wealthy.playersDecisionId(player)`, `Wealthy.fromPlayers`, `Wealthy.fromBank: DecisionOptionRef.Button`, `Wealthy.tookFromPlayers`, `Wealthy.tookFromBank: NoteKey`.

- [ ] **Step 1: Write the failing tests**

`WealthySuite` (l3 an Exile holds it; three seats, each with 1 favor at
start; production rules):

```scala
  test("with two other players holding favor the holder chooses between " +
      "them and the bank, no Decline")
  test("from two players: one choose-many of exactly two, then 1 favor " +
      "from each, resumed at each question")
  test("from the bank: 3 favor, no further question")
  test("with fewer than two other players holding favor nothing is asked " +
      "and the bank gives 3")
  test("a two-player game asks nothing")
  test("the lines name the players or the bank")
  test("The Standard Bearer then Wealthy for one holder (R27)")
  test("the card has left holding, its power is registered, and it still " +
      "alters Foundation II")
```

Movers and Shakers gives every board 6 favor; the fixture's Foundation II
is normal, so each player holds 1. For "fewer than two", zero p1's favor
with a Chronicle that cannot do it: instead seat two players
(`LaterGameFixture.seats(2)`), which is also "a two-player game asks
nothing" (one other player). For the order test, give l3 both legacies
and check the asked decisions in order with `SetupWalkDriver` under the
production power catalog (pass `WalkerPowerCatalog.default(catalog)` as
its powers).

- [ ] **Step 2: Run it and see it fail**

Run: `./sbtw "testOnly distantlands.gameplay.setup.legacies.WealthySuite"`

- [ ] **Step 3: Implement**

The contribution reads the placer as The Standard Bearer's does. Its
step:

```scala
  private def step(holder: PlayerId): Operation = Branch((state, _) =>
    val holding = givers(state, holder)
    val options = Option.when(holding.sizeIs >= 2)(DecisionOption.Button(
      fromPlayers, "Take 1 favor from two players")).toVector :+
      DecisionOption.Button(fromBank, "Take 3 favor from the shared bank")
    val choice = Option.when(options.sizeIs > 1)(Decide(decisionId(holder),
      holder, DecisionQuery.ChooseOne(options,
        heading = Some("Wealthy: take favor")))).toVector
    choice :+ Branch((_, pending) =>
      if PowerAnswers.one(pending, decisionId(holder)).contains(fromPlayers)
      then fromTwo(holder, holding)
      else fromBankOps(holder)))
```

`givers` lists the other seated players holding favor in turn order
(`LegacyGoals.turnOrder` or Setup's own order; pick the one Setup's
`turnOrder` uses). `fromTwo` asks `playersDecisionId` as
`ChooseMany(2, 2, players as DecisionOption.Player(...))`, then
`BuildOps` moving `Piece.Favor(1)` from each chosen player's play area to
the holder's, then the line with both players. `fromBankOps` moves
`Piece.Favor(3)` from `Location.SharedBank` to the holder and writes the
bank line. Check the option kind for a player (`DecisionOption.Player`)
in `Decisions.scala`.

- [ ] **Step 4: Run it and see it pass**, **Step 5: snapshots and commit**

`feat(legacies): Wealthy takes favor from two players or the shared bank after their pawn`

### Task 4: Quest in Setup: no pawn, and the swap

**Files:**
- Modify: `src/main/scala/distantlands/gameplay/setup/SetupProcedure.scala` (pawn options; skip region-reading steps without a pawn; the swap step)
- Create: `src/main/scala/distantlands/gameplay/setup/SetupAdviserSteps.scala` (Choose Adviser and Reveal Cards, moved out of `SetupProcedure`, parameterized by decision ids, for Task 5)
- Create: `src/main/scala/distantlands/gameplay/setup/legacies/QuestToDistantLands.scala`
- Modify: `HoldingLegacies.scala` (remove `QuestToDistantLandsCard`), `LegacySetupPowers.scala`, `SetupLegacyRules.scala` (`mayStayOff`)
- Modify: `src/main/scala/distantlands/gameplay/setup/legacies/SecretSociety.scala` (`excessStep` skips a pawnless player)
- Modify: the two snapshots
- Create: `src/test/scala/distantlands/gameplay/setup/legacies/QuestSetupSuite.scala`
- Create: `src/test/scala/distantlands/application/QuestProjectionSuite.scala`

**Interfaces:**
- Produces: `SetupLegacyRules.mayStayOff(state, player): Boolean`; `QuestToDistantLands.stayOff: DecisionOptionRef.Button` ("stay-off"); `QuestToDistantLands.swapDecisionId(player)`, `.giveDecisionId(player)`, `.placedNoPawn`, `.swapped: NoteKey`; `SetupAdviserSteps.chooseAdviser(player, decisionId): Operation`, `SetupAdviserSteps.reveal(catalog, player, decisionId): Operation`.

- [ ] **Step 1: Write the failing tests**

`QuestSetupSuite` (l2 a Citizen holds Quest; production rules):

```scala
  test("the holder's pawn question offers the sites and Don't place your " +
      "pawn, last; other players' offer only sites")
  test("staying off: no pawn, the line, no Choose Adviser and no Reveal " +
      "Cards for the holder; the hand stays in temporaryHands")
  test("staying off, the edifice powers at SetupPawnPlaced do not fire for " +
      "the holder, The Standard Bearer and Wealthy still do (ruling 18)")
  test("the swap: Decline first, then the Dispossessed; resumed at each " +
      "question; a facedown adviser's replacement is a facedown adviser " +
      "and Oaths does not bury it (ruling 19)")
  test("the swap with a Ties of Fellowship hand: the replacement joins the " +
      "hand and the given card goes to the Dispossessed")
  test("placing the pawn: no swap is offered")
  test("the line: p2 swapped a card with the Dispossessed with Quest to " +
      "Distant Lands, naming no card")
  test("the card has left holding and its power is registered")
```

The Dispossessed of the later-game fixture is empty: store some denizens
in the test Chronicle's `dispossessed` (take them from its world deck so
the deck accounting holds). For the facedown-adviser case give l2 a
stored starting adviser and Oaths of Loyalty, so Oaths' burial would act
on a gain.

`QuestProjectionSuite`: while the swap waits, the holder identifies every
Dispossessed card and is offered them; p1 and the public identify none and
see no query; after the swap, p1 sees one more or one fewer facedown
adviser or hand card as the case is, and identifies none of the swapped
cards.

- [ ] **Step 2: Run them and see them fail**

- [ ] **Step 3: Implement**

`SetupLegacyRules.mayStayOff(state, player)`: Quest's holder is `player`.

`SetupProcedure.playerStep`, the pawn question's options:
`SetupPawnSites.legal(...).map(site option) ++ Option.when(
SetupLegacyRules.mayStayOff(state, player))(DecisionOption.Button(
QuestToDistantLands.stayOff, "Don't place your pawn"))`. `placePawn`
writes no move for the stay-off answer; the pawn's line is replaced by
`QuestToDistantLands.placedNoPawn` ("{0} placed no pawn with {1}").

Move Choose Adviser (`chooseAdviser`, `adviserSelection`,
`keptAdviserNote`, its `Branch`) and Reveal Cards (`revealCards`,
`revealable`, `revealed`, `reveal`) into `SetupAdviserSteps`, keyed by
decision id, unchanged in behaviour (run Setup's suites and goldens green
before and after, as slice 2's Task 4 did). In `playerStep`, wrap the
discard, Choose Adviser and Reveal Cards in a `Branch` that yields nothing
while the player has no pawn (read live; their pawn is settled by the
pawn step before). Then add, after the keep and before Choose Adviser,
`QuestToDistantLands.swapStep(participant.playerId)`: a `Branch` yielding
the swap only for Quest's holder with no pawn:

```scala
  def swapStep(player: PlayerId): Operation = Branch((state, _) =>
    if !SetupLegacyRules.mayStayOff(state, player) ||
        state.game.current.pawnSite(player).nonEmpty then Vector.empty
    else
      val cards = state.game.campaign.dispossessed.sortBy(_.value)
      if cards.isEmpty then Vector.empty
      else cards.map(card => Peek(player, card, Location.Dispossessed)) ++
        Vector(Decide(swapDecisionId(player), player, DecisionQuery.ChooseOne(
          DecisionOption.Button(decline, "Decline") +: cards.map(option),
          heading = Some(
            "Quest to Distant Lands: you may swap a card with the Dispossessed"))),
        Branch((live, pending) => swapped(live, player, pending))))
```

`swapped` asks `giveDecisionId` over the player's facedown advisers that
are not Visions, by slot, or, with none, their hand
(`temporaryHands(player)`), then a `BuildOps` of two `Move`s: the taken
card from `Location.Dispossessed` to `Location.PlayArea(player)` facedown
or to `Location.Hand(player)`, and the given card the other way, then the
line. Check how the Dispossessed is a `Location` and a `CardContainer`
(The Bright Beacon reads it), and that a `Peek` there records
`knowledge.advisers` as R22 relies on.

`SecretSociety.excessStep` yields nothing while the player has no pawn.

- [ ] **Step 4: Run them and see them pass**, **Step 5: snapshots and commit**

`feat(legacies): Quest to Distant Lands may stay off the map and swap with the Dispossessed`

### Task 5: The first Wake

**Files:**
- Modify: `src/main/scala/distantlands/model/ProcedureRef.scala` (`QuestFirstWake`, key `"quest-first-wake"`, in the enum's `values` list)
- Create: `src/main/scala/distantlands/gameplay/phases/wake/QuestFirstWakeProcedure.scala`
- Modify: `src/main/scala/distantlands/gameplay/walker/WalkerProcedureRegistry.scala` (build and rebuild)
- Modify: `src/main/scala/distantlands/gameplay/OathDriver.scala` (`enterWake` starts it first when the active player has no pawn; `turnStart` hands back to `enterWake` after it)
- Modify: `src/main/scala/distantlands/gameplay/actions/ProcedureNotes.scala` (its note keys), `src/main/scala/distantlands/application/gamelog/ProcedureLog.scala` (its lines), `src/main/scala/distantlands/application/WalkerDecisionProjector.scala` if its rebuild lists procedures
- Create: `src/test/scala/distantlands/gameplay/phases/wake/QuestFirstWakeSuite.scala`
- Modify: the procedure-key codec test (`git grep -ln "forced-wake" -- src/test`)

**Interfaces:**
- Consumes: `SetupAdviserSteps` (Task 4), `SecretSociety.excessStep`.
- Produces: `TriggeredProcedureRef.QuestFirstWake`; `QuestFirstWakeProcedure.placeDecisionId(player)` (`wake.legacy.quest.<player>`), `.adviserDecisionId`, `.discardDecisionId`, `.revealDecisionId`; `QuestFirstWakeProcedure.placedPawn: NoteKey`.

- [ ] **Step 1: Write the failing tests**

```scala
  test("a pawnless player's Wake opens with the placement, at any site in " +
      "play, no Decline, before the Wake checks")
  test("then Choose Adviser, discarding by the new pawn's region, then " +
      "Reveal Cards; a Ties hand card swapped in is buried under Oaths")
  test("then Secret Society's excess discard, when over the limit")
  test("no SetupPawnPlaced edifice power fires")
  test("a walk resumed at each question places once and asks the next")
  test("the line: p2 placed their pawn at Deep Woods")
  test("a player with a pawn is not asked; the procedure never starts")
  test("the procedure key round-trips on the wire, and older keys still " +
      "decode")
```

The scenarios start from Setup with Quest's holder staying off (Task 4's
answers), then end other players' turns with `Situation.endingWake` and
`finishingRest` until p2's Wake. The Oaths case: l2 holds Quest and Oaths
of Loyalty with no starting advisers, draws a Ties hand, swaps a hand card
in, and at the Wake's Choose Adviser keeps it: it is buried at the world
deck's bottom (Legacy Chronicle powers ruling 16), as Setup buries.

- [ ] **Step 2: Run them and see them fail**

- [ ] **Step 3: Implement**

`QuestFirstWakeProcedure.build(catalog, ready, actor, args)`:

```scala
    Sequence(Vector(
      Decide(placeDecisionId(actor), actor, DecisionQuery.ChooseOne(
        ready.game.current.map.inPlay.map(site =>
          DecisionOption.Site(DecisionOptionRef.Site(site))),
        heading = Some("Place your pawn at any site"),
        confirmVerb = Some("Start at"))),
      BuildOps(placePawn(actor), required = true),
      Note(noteId, placedLine(actor)),
      SetupAdviserSteps.chooseAdviser(actor, adviserDecisionId(actor)),
      SecretSociety.excessStep(catalog, actor, discardDecisionId(actor)),
      SetupAdviserSteps.reveal(catalog, actor, revealDecisionId(actor))))
```

`excessStep` takes its decision id as a parameter now (Setup passes its
own). `placePawn` moves the pawn from the play area to the site, as
Setup's does; no window is folded on it. In `OathDriver.enterWake`, before
`StateBasedEvaluation.atWake`, start `QuestFirstWake` when the active
player is seated with no pawn site; `turnStart` treats
`TriggeredProcedureRef.QuestFirstWake` as the hand-back point that calls
`enterWake` again, which now finds the pawn and runs the Wake checks and
Forced Wake. Read how `Setup` is registered in `WalkerProcedureRegistry`
and how its lines are configured in `ProcedureLog` (`narratedPrefixes`),
and mirror both with `"wake.legacy.quest."`.

- [ ] **Step 4: Run them and see them pass**, **Step 5: snapshots and commit**

`feat(legacies): a pawnless player places their pawn at the start of their first Wake`

### Task 6: The pawn-site audit

**Files:**
- Modify: the readers the audit finds
- Create: `src/test/scala/distantlands/gameplay/PawnlessPlayerSuite.scala`

- [ ] **Step 1: List the readers**

```bash
git grep -n "PawnSiteMissing\|pawnSite(" -- src/main/scala
```

Sort each hit into: (a) the actor's own pawn (an action, a Wake power, a
card play): safe, since the pawnless player's first act is the placement;
(b) another player's pawn (targeting, "players at the site", rulers,
Campaign defenders, Oathkeeper or Vision checks, round-end steps, Setup's
later steps, projection and log); (c) every player's. Write the list, with
the class of each, into the task's commit message.

- [ ] **Step 2: Write the failing test**

```scala
/** A player with no pawn stands at no site (spec ruling 20; R33): another
  * player's turn before their first Wake runs with no failure. */
class PawnlessPlayerSuite extends munit.FunSuite:
  test("p1's whole turn runs while p2 has no pawn")
  test("rules that look for players at a site do not find p2")
  test("the projection and the Game Log draw p2 with no pawn")
```

The first test plays p1's Wake, a Travel, a Search, a Campaign at a site
and p1's Rest through the production rules with p2 pawnless (Task 4's
answers), then reaches p2's first Wake. Add a case for each (b) reader the
list finds that a turn can reach: a test that fails today, then the fix.

- [ ] **Step 3: Fix each reader** so a pawnless player counts as at no
site, with a comment naming R33. A reader that has to decide something
else for a pawnless player is a stop: report it as an open question.

- [ ] **Step 4: Run the suites**, **Step 5: commit**

`fix(legacies): a pawnless player stands at no site for every other rule`

### Task 7: The golden log, the docs and the running app

- [ ] **Step 1: The golden script** in `LaterGameLogScripts`:
  `later-game-quest-first-wake`. l2 a Citizen holds The Standard Bearer,
  Wealthy and Quest to Distant Lands active, with two Dispossessed
  denizens. p2 stays off, takes the People's Favor, takes 3 favor from
  the bank, and swaps; p1's turn runs; p2's first Wake places the pawn,
  chooses an adviser and reveals nothing. Generate with the test filter
  and review both seats line by line: no line names a swapped or
  Dispossessed card to p1.
- [ ] **Step 2: Docs.** `ambiguities.md`: the design's rulings 16 to 20 and
  R27 to R33 as ruled, numbered on from the last item. Traceability: a
  "Legacy SETUP powers in the player choices" row and the Wake row's note.
  rules-reference: "Digital:" notes on step 7 (the pawn and Choose
  Adviser) and on Wake. ROADMAP: tick "Legacy powers: setup"; the in-play
  item still names every other in-play half; "Simultaneous setup effects"
  stays open. The spec's Status: all four slices landed.
- [ ] **Step 3: The gate.**
- [ ] **Step 4: The running app.** Import a Chronicle (format of slice 3's
  check, lineages `red-lineage` and `blue-lineage`) where Blue, a Citizen,
  holds The Standard Bearer, Wealthy and Quest. Walk Setup: Blue's pawn
  panel shows the sites and "Don't place your pawn"; staying off, The
  Standard Bearer's and Wealthy's panels follow; the swap panel names the
  Dispossessed to Blue and Red waits without names. Play Red's turn to
  Blue's Wake: the placement panel offers every site, then Choose Adviser.
  Record screenshots or page text; stop the server.
- [ ] **Step 5: Commit**

`test(legacies): a golden from Setup through a Quest holder's first Wake, and the docs`

## Self-review notes

- Spec ruling 16: Task 2. 17: Task 3. 18: Task 4 (no pawn, edifices
  silent, The Standard Bearer and Wealthy still run, deferred steps).
  19: Task 4 (the swap, privacy, Oaths). 20: Task 5 (placement keyed on
  the missing pawn, deferred steps, no `SetupPawnPlaced`, the line) and
  Task 6 (no site until placed). Rulings 1-4 throughout.
- The slice 4 test list: The Standard Bearer with one banner, both and
  none (Task 2); Wealthy from the bank and from two players, and a
  two-player game asking nothing (Task 3); Quest swapping with a facedown
  adviser under Oaths and with a Ties hand card (Tasks 4 and 5); the first
  Wake placing the pawn, then Choose Adviser by its region, then Reveal
  Cards (Task 5); another player's turn before the holder's first Wake
  (Task 6); a golden (Task 7); resume tests for each new decision.
- Names across tasks: `SetupPawnPlacement.latest`, `.isPawnDecision`;
  `SetupLegacyRules.standardBearer`, `.wealthy`, `.mayStayOff`;
  `TheStandardBearer`, `Wealthy`, `QuestToDistantLands` and their ids;
  `SetupAdviserSteps.chooseAdviser`, `.reveal`;
  `SecretSociety.excessStep(catalog, player, decisionId)`;
  `TriggeredProcedureRef.QuestFirstWake`, `QuestFirstWakeProcedure`.
- Not fully coded in this plan: the test bodies of Tasks 2-6 and the
  audit's fixes, which depend on fixture facts (banner holders, favor,
  the Dispossessed) and on the audit's list; each step names what to
  check first.
