# Catalog Batch 10, Slice 11: Challenge Reach Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement School of Vines (E27i), Sigil of the Eye (R43), Sigil of
the Heart (R44), Magician's Code (32) and Favored Son (235): four Challenge
modifiers that widen which banners a Challenge can reach, and a WHEN PLAYED
power that takes the People's Favor.

**Architecture:** The Challenge stops gating its start on `legalBanners`.
Instead its banner question reads the walk's modifier picks
(`ModifierPicks.cards`), and when no banner is in reach under them it
refuses the walk with the same `NoPlayableOption("challenge")`. The four
cards move beside the Challenge in `gameplay/actions/challenge`, as
Underground Library sits beside the Recover, and a new `ChallengeReach`
object reads their picks. Their powers are plain selected modifiers in a
new `gameplay/powers/challenge` package that hook no window. Favored Son is
a `WhenPlayedPower` whose `Take` carries the People's Favor ribbon as its
`leaving`.

**Tech Stack:** Scala 3, munit, `./sbtw` (sbt wrapper), `-Werror`.

**Spec:** [Catalog batch 10 design](../specs/2026-11-03-catalog-batch-10-design.md):
read "Slice 11: Challenge reach", ruling 11, and the "School of Vines and
the Sigils" and "Favored Son" readings.

**Card text** (from `~/projects/cards`; no FAQ for any of the five):

- School of Vines (intact): "You may challenge a banner held by an enemy
  even if your pawn is not at their pawn's site."
- Sigil of the Eye: "You may challenge the Darkest Secret from any site."
- Sigil of the Heart: "You may challenge the People's Favor from any site."
- Magician's Code (2 favor): "If you're challenging the Darkest Secret,
  gain [secret] [secret] and place them on the banner along with any other
  [secret] you're placing _(even none)_."
- Favored Son: "**WHEN PLAYED,** take the People's Favor. If you do,
  adjust its [favor] using its left ribbon."

## Plan-time findings

These were found while writing the plan. Each departs from or adds to the
spec, so the review at the end checks them.

1. **An empty banner question does not refuse the start.** The spec says
   that once `afterModifiers` stops refusing, an empty banner question
   refuses the start through `ModifierChoice`'s Start preview and
   `QueuedStart.priced`. A probe at `4018ee69` showed otherwise: with the
   build gate removed, both "no legal banner" tests in
   `ChallengeProcedureSuite` got `Right`, because the walk parks on the
   empty question. So the banner `Branch` itself refuses: with no banner in
   reach under the picks, it builds a `BuildOps` that returns
   `Left(NoPlayableOption("challenge"))` instead of the question. The whole
   walk is then refused, so nothing is spent. `ModifierChoice.refusal`
   shows "Start Challenge" unavailable until a pick opens a banner, and
   `QueuedStart.priced` finds that pick for the Act menu. The refusal is
   the same violation as today, so the existing suites hold.
2. **Refusal order.** The gate now comes after the Supply spend. A start
   with neither Supply nor a banner in reach is refused for the Supply
   (`insufficient-supply`), where today it is refused for the banner. No
   existing test has both.
3. **Reach modifiers must widen.** The spec gates only Magician's Code on
   being useful. Picking School of Vines or a Sigil that opens nothing would
   leave "Start Challenge" unavailable with nothing else to pick, a dead
   end. So all three are unavailable, with "Brings no new banner into
   reach", unless they add a banner to the set the picks so far allow.
4. **"Enemy".** The rules glossary says Imperial players are not enemies of
   each other. School of Vines therefore does not reach a banner held by an
   Imperial ally: a Citizen still needs the Chancellor's site. A new
   `Imperials.enemies` states this once.
5. **Magician's Code's lower bound.** The spec says the amount's lower bound
   "drops to 0". The rule that the challenger places strictly more than the
   banner holds still applies, counting the 2 gained secrets. So the lower
   bound is `max(0, prior + 1 - 2)`: 0 when the banner holds 1 secret or
   none, which is the card's "(even none)".
6. **The gained secrets pass through the board.** The 2 secrets go from the
   shared bank to the user's board just before the payment, and the
   payment moves them with the rest. The Challenge's existing line reads
   what left the board, so it counts them: "Took Darkest Secret from the
   bank with 2 secrets." A direct bank-to-banner move would leave a
   0-secret payment, and the line would not be written at all.
7. **Magician's Code with the People's Favor.** Picked, then the People's
   Favor chosen, the Code does nothing and its 2 favor stays paid. The text
   says "If you're challenging the Darkest Secret", and the player chose.
8. **A third Favored Son line.** The spec gives two lines. When Lost Tongue
   (or a Banner Guardian) refuses the take, a third line says "The People's
   Favor did not move.", as Wizard's Conclave's does.
9. **The left ribbon.** The People's Favor's Grand Council face is never
   set in a generated game, so Favored Son uses the Mob face's ribbon, the
   `ChallengeRibbon` branch, as the spec says.
10. **The cards move to `gameplay/actions/challenge`.** The Challenge reads
    the picks by card id, and `gameplay.actions` may not import
    `gameplay.powers` (`check-architecture.py`), as slice 8 found for
    Drowned Library. About 20 test files import the moved cards; Tasks 1
    and 2 rewrite those imports.

## Global Constraints

- **Slice 10 first.** Slice 10 edits `WhenPlayedPowers.all`,
  `WalledGardenSuite`'s imports, the spec's status line and the ROADMAP's
  engine-blocked list, which this plan also edits. Before Task 1, check
  that slice 10 has merged into `0.2.0-alpha`, then rebase onto it:

  ```bash
  git log --oneline 0.2.0-alpha | grep "feat(powers): implement Autumn Wind"
  ```

  If that prints nothing, stop and report that slice 10 has not merged.
  Otherwise, with a clean worktree, run `git rebase 0.2.0-alpha`.
- Work only in this worktree (`.worktrees/batch-10-slice-11`). Its
  `.tooling` and `node_modules` are symlinks to the main checkout.
- Add files by name. Never `git add -A`. Push nothing.
- Commit trailer: `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`.
- Shell is zsh. Never write a bare `===` or `=word`, and quote globs.
- Every build compiles with `-Werror`, so an unused import fails it.
- Log lines name no actor (they omit "you").
- Test helper names say what they build.
- `python3 scripts/check-architecture.py` passes: 800 lines per file at
  most, and `gameplay.actions` imports nothing from `gameplay.powers`.
- Coverage stays at 92% or more:
  `./sbtw clean coverage test coverageReport`, run in the background.

---

### Task 1: The reach seam, School of Vines and the Sigils

**Files:**
- Create: `src/main/scala/oathdigital/gameplay/actions/challenge/ChallengeReachCards.scala`
- Create: `src/main/scala/oathdigital/gameplay/actions/challenge/ChallengeReach.scala`
- Create: `src/main/scala/oathdigital/gameplay/powers/challenge/ChallengeModifiers.scala`
- Create: `src/test/scala/oathdigital/gameplay/powers/challenge/ChallengeReachSuite.scala`
- Modify: `src/main/scala/oathdigital/gameplay/actions/challenge/ChallengeProcedure.scala`
- Modify: `src/main/scala/oathdigital/model/Imperials.scala`
- Modify: `src/main/scala/oathdigital/gameplay/walker/WalkerProcedureRegistry.scala` (the Challenge entry's comment)
- Modify: `src/main/scala/oathdigital/gameplay/powers/ActionModifiers.scala`
- Modify: `src/main/scala/oathdigital/gameplay/powers/setup/RuinedSiteSetupRules.scala` (School of Vines moves out)
- Modify: `src/main/scala/oathdigital/catalog/holding/HoldingRelics.scala` (both Sigils move out)
- Modify: `src/main/scala/oathdigital/gameplay/cards/NewFoundations.scala` (import)
- Modify (imports): `AnimalPlaymatesSuite`, `AnimalHostSuite`, `ThreateningRoarSuite`, `DazzleSuite`, `ForestHornSuite`, `ShapeEmpireSuite`, `RuinedSetupRulesSuite`, `ChronicleTtsCodecSuite`
- Modify: `src/test/resources/powers/unimplemented.txt` (regenerated)

**Interfaces:**
- Produces:
  - `ChallengeProcedure.legalBanners(state: ReadyGame, actor: PlayerId, picks: Vector[CardId] = Vector.empty): Vector[Banner]`
  - `ChallengeReach.unbound(ready: ReadyGame, actor: PlayerId, picks: Vector[CardId], banner: Banner, holder: PlayerId): Boolean`
  - `ChallengeReach.widens(ready: ReadyGame, actor: PlayerId, answered: Vector[Answered], card: CardId): Boolean`
  - `ChallengeReach.noNewBanner: String`
  - `Imperials.enemies(ready: ReadyGame, player: PlayerId, other: PlayerId): Boolean`
  - `ChallengeModifiers.all: Vector[ContributingPower]`, holding `SchoolOfVines`, `SigilOfTheEye`, `SigilOfTheHeart`
  - The card objects `SchoolOfVinesCard`, `SigilOfTheEyeCard`, `SigilOfTheHeartCard` in `oathdigital.gameplay.actions.challenge`

- [ ] **Step 1: Check that slice 10 has merged, and rebase**

Follow the "Slice 10 first" constraint above.

- [ ] **Step 2: Write the failing suite**

Create `src/test/scala/oathdigital/gameplay/powers/challenge/ChallengeReachSuite.scala`:

```scala
package oathdigital.gameplay.powers.challenge

import oathdigital.catalog.ModifierMark
import oathdigital.gameplay.{ModifierPlay, OathRules}
import oathdigital.gameplay.actions.challenge.{ChallengeProcedure,
  ChallengeReach, SchoolOfVinesCard, SigilOfTheEyeCard, SigilOfTheHeartCard}
import oathdigital.gameplay.powerresolver.ModifierPicks
import oathdigital.gameplay.powers.WalkerPowerCatalog
import oathdigital.gameplay.setup.FirstGameSetupFixture.catalog
import oathdigital.gameplay.walker.ModifierChoice
import oathdigital.model._
import oathdigital.model.DecisionAnswer.{ChooseAmountAnswer, ChooseOneAnswer}
import oathdigital.model.OathState.Ready
import oathdigital.testkit.{ActPowerHarness, Look, MenuPlay, Outcomes,
  ParkedNode, Table}
import oathdigital.testkit.Outcomes.assertWireRoundTrips
import oathdigital.testkit.Table.{p1, p2}
import oathdigital.testkit.Unwrap.accepted

/** School of Vines (E27, intact), Sigil of the Eye (R43) and Sigil of the
  * Heart (R44): free Challenge modifiers that lift the pawn-site
  * requirement for some banners (catalog batch 10, slice 11). */
class ChallengeReachSuite extends munit.FunSuite:
  private val powers = WalkerPowerCatalog.default(catalog)
  private val rules = new OathRules(catalog, walkerPowerCatalog = powers)
  private val window = PowerWindow.ChallengeModifierSelection
  private val peoplesFavor = DecisionOptionRef.Banner(Banner.PeoplesFavor)
  private val darkestSecret = DecisionOptionRef.Banner(Banner.DarkestSecret)

  /** p1, in Act with 6 favor and 6 faceup secrets. p2, at their quiet site
    * away from p1, holds the People's Favor with 1 favor and the Darkest
    * Secret with 1 secret, so p1 could outbid both but reaches neither. */
  private def p2HoldsBothBannersAway: Table = Table.start
    .favor(p1, 6).secrets(p1, faceUp = 6)
    .peoplesFavor(Some(p2), favor = 1).darkestSecret(Some(p2), secrets = 1)

  /** `p2HoldsBothBannersAway` with School of Vines intact at p1's site,
    * where p1 may use it. */
  private def schoolAtP1sSite: Table =
    p2HoldsBothBannersAway.edifice(SchoolOfVinesCard, EdificeSide.Intact,
      at = Table.quietPawnSite(p1))

  private def refOf(card: CardId): DecisionOptionRef =
    DecisionOptionRef.ofCard(card).getOrElse(fail(s"$card has no option"))

  /** The question parked in `state`, read with the production powers. */
  private def parkedQuestion(state: OathState): Decide =
    ParkedNode.of(state, catalog, powers) match
      case Right(Some(ParkedNode.Decision(_, decide, _))) => decide
      case other => fail(s"expected a parked question, got $other")

  private def chooseOneOptions(decide: Decide): Vector[DecisionOption] =
    decide.query match
      case one: DecisionQuery.ChooseOne => one.options
      case other => fail(s"expected a choose-one, got $other")

  /** The modifier decision's options when p1 starts a Challenge. */
  private def challengeModifierOptions(table: Table): Vector[DecisionOption] =
    val begun = accepted(MenuPlay.started(rules, Ready(table.ready), p1,
      ActionRef.Challenge))
    val asked = parkedQuestion(begun.state)
    assertEquals(asked.decisionId, ModifierPicks.decisionId(window))
    chooseOneOptions(asked)

  /** p1's Challenge started with `picks` picked, parked on its banner
    * question. */
  private def challengeAtBannerWith(table: Table, picks: PowerId*)
      : OathTransition =
    val started = accepted(ModifierPlay.started(rules, Ready(table.ready),
      ActionRef.Challenge, p1, window, picks.toVector))
    assertEquals(parkedQuestion(started.state).decisionId,
      ChallengeProcedure.bannerDecisionId)
    started

  private def bannersOffered(at: OathTransition): Vector[DecisionOptionRef] =
    chooseOneOptions(parkedQuestion(at.state)).map(_.ref)

  test("each card's printed power carries the Challenge icon"):
    val icon = Some(ModifierMark.Action(MajorActionType.Challenge))
    assertEquals(SchoolOfVinesCard.intact.power.modifier, icon)
    assertEquals(SigilOfTheEyeCard.power.modifier, icon)
    assertEquals(SigilOfTheHeartCard.power.modifier, icon)

  test("without a reach modifier, banners held away are out of reach and " +
      "the Challenge is refused"):
    val ready = p2HoldsBothBannersAway.ready
    assert(!MenuPlay.availableInMenu(catalog, ready, ActionRef.Challenge))
    assertEquals(rules.startWalker(Ready(ready), ActionRef.Challenge, p1)
      .left.toOption, Some(OathViolation.NoPlayableOption("challenge")))

  test("Sigil of the Eye opens the Darkest Secret from any site, and only " +
      "it"):
    val table = p2HoldsBothBannersAway.relic(p1, SigilOfTheEyeCard)
    assert(MenuPlay.availableInMenu(catalog, table.ready, ActionRef.Challenge))
    val options = challengeModifierOptions(table)
    // Nothing is in reach before the pick, so Start waits for it.
    assertEquals(options.find(_.ref == ModifierPicks.start),
      Some(DecisionOption.Unavailable(DecisionOption.Button(
        ModifierPicks.start, "Start Challenge with no modifiers"),
        OptionReason.Written(ModifierChoice.cannotStart))))
    assert(options.exists(option =>
      option.ref == refOf(SigilOfTheEyeCard.id) && option.available))
    assertEquals(bannersOffered(challengeAtBannerWith(table,
      SigilOfTheEye.id)), Vector(darkestSecret))

  test("Sigil of the Heart opens the People's Favor from any site, and " +
      "only it"):
    val table = p2HoldsBothBannersAway.relic(p1, SigilOfTheHeartCard)
    assert(MenuPlay.availableInMenu(catalog, table.ready, ActionRef.Challenge))
    assertEquals(bannersOffered(challengeAtBannerWith(table,
      SigilOfTheHeart.id)), Vector(peoplesFavor))

  test("a Sigil that brings no new banner into reach cannot be picked"):
    // The People's Favor is unheld, so it needs no Sigil; p2 holds the
    // Darkest Secret away, so Sigil of the Eye opens it.
    val table = Table.start.favor(p1, 6).secrets(p1, faceUp = 6)
      .peoplesFavor(None, favor = 1).darkestSecret(Some(p2), secrets = 1)
      .relic(p1, SigilOfTheEyeCard).relic(p1, SigilOfTheHeartCard)
    val heart = refOf(SigilOfTheHeartCard.id)
    assertEquals(challengeModifierOptions(table).find(_.ref == heart),
      Some(DecisionOption.Unavailable(
        DecisionOption.forRef(heart).getOrElse(fail("no Sigil option")),
        OptionReason.Written(ChallengeReach.noNewBanner))))

  test("School of Vines opens every banner an enemy holds, from any site"):
    val table = schoolAtP1sSite
    assert(MenuPlay.availableInMenu(catalog, table.ready, ActionRef.Challenge))
    assertEquals(bannersOffered(challengeAtBannerWith(table,
      SchoolOfVines.id)), Vector(peoplesFavor, darkestSecret))

  test("School of Vines does not reach a banner an Imperial ally holds"):
    // p2 is the Chancellor and p1 a Citizen, so they are not enemies.
    val ready = schoolAtP1sSite.imperial(p2, p1).turn(p1, Phase.Act).ready
    assert(!Imperials.enemies(ready, p1, p2))
    assert(!ChallengeReach.widens(ready, p1, Vector.empty,
      SchoolOfVinesCard.id))
    assert(!MenuPlay.availableInMenu(catalog, ready, ActionRef.Challenge))

  test("a Challenge a Sigil opened takes the banner and replays"):
    val ready = p2HoldsBothBannersAway.relic(p1, SigilOfTheEyeCard).ready
    val started = challengeAtBannerWith(Table.from(ready), SigilOfTheEye.id)
    val chosen = accepted(rules.resolveWalker(started.state, p1,
      ChallengeProcedure.bannerDecisionId, ChooseOneAnswer(darkestSecret)))
    // The banner holds 1 secret, so the amount starts at 2.
    val done = accepted(rules.resolveWalker(chosen.state, p1,
      ChallengeProcedure.amountDecisionId, ChooseAmountAnswer(2)))
    val after = Outcomes.ready(done.state)
    assertEquals(Look(after).darkestSecretHolder, Some(p1))
    assertEquals(Look(after).secretsOnDarkestSecret, 2)
    val events = started.events ++ chosen.events ++ done.events
    assertEquals(ActPowerHarness.replayed(rules, ready, events), after)
    assertWireRoundTrips(events)
```

- [ ] **Step 3: Run the suite to see it fail**

Run: `./sbtw "testOnly oathdigital.gameplay.powers.challenge.ChallengeReachSuite"`
Expected: compile errors: `SchoolOfVinesCard`, `ChallengeReach`,
`SigilOfTheEye` and the other new names are not found in
`oathdigital.gameplay.actions.challenge` or this package.

- [ ] **Step 4: Move the cards beside the Challenge**

Create `src/main/scala/oathdigital/gameplay/actions/challenge/ChallengeReachCards.scala`.
School of Vines' two faces are copied from `RuinedSiteSetupRules.scala`
unchanged, except that the intact power gains its Challenge icon. The
Sigils are copied from `HoldingRelics.scala` and gain theirs:

```scala
package oathdigital.gameplay.actions.challenge

import oathdigital.catalog.{Edifice, EdificeFace, Locked, ModifierMark,
  PrintedPower, Relic}
import oathdigital.model._

/** The cards whose picks widen a Challenge (catalog batch 10, slice 11).
  * They live beside the Challenge, which reads their picks
  * ([[ChallengeReach]]), as Underground Library lives beside the Recover.
  * Their powers are in `gameplay.powers.challenge`; Ruined School's setup
  * rule is in `RuinedSiteSetupRules`. */
object SchoolOfVinesCard extends Edifice(EdificeId("E27"), Suit.Beast):
  object intact extends EdificeFace("School of Vines") with Locked:
    val power = PrintedPower(PowerId("edifice.e27.intact"),
      persistent = false, cost = Cost.free,
      modifier = Some(ModifierMark.Action(MajorActionType.Challenge)),
      text = "You may challenge a banner held by an enemy even if your " +
        "pawn is not at their pawn's site.")
    val powers: Vector[PrintedPower] = Vector(power)
  object ruined extends EdificeFace("Ruined School"):
    val power = PrintedPower(PowerId("edifice.e27.ruined"),
      persistent = false, cost = Cost.free,
      text = "**SETUP (END) / WHEN EXPLORED:** Place [secret] on each " +
        "site ruled by the bandits. _Players may take it in Wake._")
    val powers: Vector[PrintedPower] = Vector(power)

object SigilOfTheEyeCard extends Relic(RelicId("R43"), "Sigil of the Eye", value = 66, defense = 1):
  val power = PrintedPower(PowerId("relic.sigil-of-the-eye"),
    persistent = false, cost = Cost.free,
    modifier = Some(ModifierMark.Action(MajorActionType.Challenge)),
    text = "You may challenge the Darkest Secret from any site.")
  val powers: Vector[PrintedPower] = Vector(power)

object SigilOfTheHeartCard extends Relic(RelicId("R44"), "Sigil of the Heart", value = 53, defense = 1):
  val power = PrintedPower(PowerId("relic.sigil-of-the-heart"),
    persistent = false, cost = Cost.free,
    modifier = Some(ModifierMark.Action(MajorActionType.Challenge)),
    text = "You may challenge the People's Favor from any site.")
  val powers: Vector[PrintedPower] = Vector(power)
```

Then:

1. In `RuinedSiteSetupRules.scala`, delete the `object SchoolOfVinesCard`
   block (the first card in the file) and add
   `import oathdigital.gameplay.actions.challenge.SchoolOfVinesCard` above
   `import oathdigital.gameplay.powers.DispossessedDraws`. The file still
   uses `Edifice`, `EdificeFace`, `Locked` and `PrintedPower` for its
   other cards.
2. In `HoldingRelics.scala`, delete `object SigilOfTheEyeCard` and
   `object SigilOfTheHeartCard`. Its imports stay in use.
3. In `NewFoundations.scala`, add
   `import oathdigital.gameplay.actions.challenge.{SchoolOfVinesCard, SigilOfTheEyeCard, SigilOfTheHeartCard}`
   above `import oathdigital.gameplay.actions.recover.UndergroundLibraryCard`,
   wrapped at 80 columns as the file's other imports are.
4. Rewrite the test imports:

```bash
perl -pi -e 's/^import oathdigital\.gameplay\.powers\.setup\.SchoolOfVinesCard$/import oathdigital.gameplay.actions.challenge.SchoolOfVinesCard/' src/test/scala/oathdigital/gameplay/powers/economy/AnimalPlaymatesSuite.scala src/test/scala/oathdigital/gameplay/powers/whenplayed/AnimalHostSuite.scala src/test/scala/oathdigital/gameplay/powers/whenplayed/ThreateningRoarSuite.scala
```

```bash
perl -pi -e 's/^import oathdigital\.gameplay\.powers\.setup\.\{(\w+), SchoolOfVinesCard\}$/import oathdigital.gameplay.actions.challenge.SchoolOfVinesCard\nimport oathdigital.gameplay.powers.setup.$1/' src/test/scala/oathdigital/gameplay/powers/whenplayed/DazzleSuite.scala src/test/scala/oathdigital/gameplay/powers/action/ForestHornSuite.scala src/test/scala/oathdigital/gameplay/chronicle/ShapeEmpireSuite.scala
```

5. In `src/test/scala/oathdigital/gameplay/powers/setup/RuinedSetupRulesSuite.scala`,
   which shares the old package and so imported nothing, add
   `import oathdigital.gameplay.actions.challenge.SchoolOfVinesCard` above
   `import oathdigital.gameplay.setup.{FirstGameSetupFixture, SetupProcedure,`.
6. In `src/test/scala/oathdigital/application/chronicle/ChronicleTtsCodecSuite.scala`,
   replace

```scala
import oathdigital.catalog.holding.{ArbiterCard, LightFingersCard,
  SigilOfTheEyeCard}
```

   with

```scala
import oathdigital.catalog.holding.{ArbiterCard, LightFingersCard}
import oathdigital.gameplay.actions.challenge.SigilOfTheEyeCard
```

7. Check that nothing names the old homes:

```bash
git grep -nE 'setup\.SchoolOfVinesCard|setup\.\{[^}]*SchoolOfVinesCard|holding\.[^\n]*SigilOfThe' -- src
```

   Expected: no output.

- [ ] **Step 5: Add `Imperials.enemies`**

In `src/main/scala/oathdigital/model/Imperials.scala`, after `isImperial`:

```scala
  /** Whether `player` and `other` are enemies (rules glossary, "Enemy"):
    * two different players are, unless both are Imperial. */
  def enemies(ready: ReadyGame, player: PlayerId, other: PlayerId)
      : Boolean =
    player != other && !(isImperial(ready, player) &&
      isImperial(ready, other))
```

- [ ] **Step 6: Add `ChallengeReach`**

Create `src/main/scala/oathdigital/gameplay/actions/challenge/ChallengeReach.scala`:

```scala
package oathdigital.gameplay.actions.challenge

import oathdigital.gameplay.powerresolver.ModifierPicks
import oathdigital.model._

/** What the Challenge modifiers change about a Challenge (catalog batch
  * 10, slice 11), read from the walk's picks, which a park keeps and
  * replay rebuilds, as `RecoverProcedure` reads Drowned Library's:
  *
  *  - School of Vines: a banner an enemy holds needs no pawn at its
  *    holder's site.
  *  - Sigil of the Eye: the Darkest Secret needs none, whoever holds it;
  *    Sigil of the Heart: the People's Favor.
  */
object ChallengeReach:
  /** Why School of Vines or a Sigil cannot be picked: the picks so far
    * already reach every banner it would. */
  val noNewBanner: String = "Brings no new banner into reach"

  /** Whether `picks` let `actor` challenge `banner`, which `holder` holds,
    * without standing at the holder's pawn site. */
  def unbound(ready: ReadyGame, actor: PlayerId, picks: Vector[CardId],
      banner: Banner, holder: PlayerId): Boolean =
    (picks.contains(SchoolOfVinesCard.id) &&
      Imperials.enemies(ready, actor, holder)) ||
      (banner match
        case Banner.DarkestSecret => picks.contains(SigilOfTheEyeCard.id)
        case Banner.PeoplesFavor => picks.contains(SigilOfTheHeartCard.id))

  /** Whether picking `card` beside the picks among `answered` brings a
    * banner into reach. */
  def widens(ready: ReadyGame, actor: PlayerId, answered: Vector[Answered],
      card: CardId): Boolean =
    val picks = ModifierPicks.cards(answered)
    ChallengeProcedure.legalBanners(ready, actor, picks :+ card).size >
      ChallengeProcedure.legalBanners(ready, actor, picks).size
```

- [ ] **Step 7: Gate the Challenge on its banner question**

In `ChallengeProcedure.scala`:

1. Add `import oathdigital.gameplay.powerresolver.ModifierPicks`.
2. Replace `build` and delete `afterModifiers`:

```scala
  /** The start checks only the banner faces: whether a banner is in reach
    * depends on the modifiers the walk picks first, so the banner question
    * refuses the walk instead (`bannerQuestion`). */
  def build(catalog: ExecutableCatalog, state: ReadyGame, actor: PlayerId,
      args: Vector[DecisionOptionRef]): Either[OathViolation, Operation] = for
    _ <- noStartArgs(args)
    _ <- supportedFaces(state)
  yield tree(actor)
```

3. Replace the comment on `tree` with:

```scala
  /** The modifier decision, then the Challenge. "Start Challenge" previews
    * the Challenge, so it shows unavailable until the picks bring a banner
    * into reach. */
```

4. Replace `legalBanners` with:

```scala
  /** The banners the actor could challenge now under `picks`, the modifier
    * picks so far, in the banners' declared order: not already held by the
    * actor, the holder (if any) at the actor's pawn site unless a pick lifts
    * that ([[ChallengeReach.unbound]]), and strictly more relevant faceup
    * resources than the banner holds. Supply is a cost owned by
    * `SpendSupply`, not a fact about a banner.
    */
  def legalBanners(state: ReadyGame, actor: PlayerId,
      picks: Vector[CardId] = Vector.empty): Vector[Banner] =
    val current = state.game.current
    current.player(actor).toVector.flatMap { player =>
      Banner.all.filter { banner =>
        val holder = BannerRules.holder(current, banner)
        !holder.contains(actor) &&
          holder.forall(rival => (player.pawnSite.nonEmpty &&
            player.pawnSite == current.pawnSite(rival)) ||
            ChallengeReach.unbound(state, actor, picks, banner, rival)) &&
          BannerRules.playerResources(player, banner) >
            BannerRules.resources(current, banner)
      }
    }
```

5. In `challenge`, replace the banner `Branch` (the one building the
   `Decide(bannerDecisionId, ...)`) with
   `Branch((ready, pending) => Vector(bannerQuestion(ready, actor, picks(pending)))),`
   and add below `challenge`:

```scala
  /** The banner question, or, with no banner in reach under `picks`, a step
    * that refuses the walk. The refusal is the whole walk's, so the Supply
    * spent before it is never spent: a start with no modifier to pick is
    * refused as before, and "Start Challenge" previews unavailable until a
    * pick opens a banner. An empty question would park instead. */
  private def bannerQuestion(ready: ReadyGame, actor: PlayerId,
      picks: Vector[CardId]): Operation =
    legalBanners(ready, actor, picks) match
      case Vector() => BuildOps((_, _) =>
        Left(OathViolation.NoPlayableOption(ActionRef.Challenge.key)))
      case banners => Decide(bannerDecisionId, actor,
        DecisionQuery.ChooseOne(banners.map(banner =>
          DecisionOption.Banner(DecisionOptionRef.Banner(banner))),
          heading = Some("Choose a banner to Challenge"),
          confirmVerb = Some("Challenge")),
        window = Some(PowerWindow.ChallengeBannerSelection))

  /** The cards the walk's modifier decision picked. */
  private def picks(pending: PendingTree): Vector[CardId] =
    ModifierPicks.cards(pending.answered)
```

6. In the object's doc comment, replace "It opens with the modifier
   decision, which asks nothing while no power selects at its window." with
   "It opens with the modifier decision, whose picks the banner question
   reads ([[ChallengeReach]])."

In `WalkerProcedureRegistry.scala`, replace the Challenge entry's comment
with:

```scala
    /** Challenge. Its first step after the modifier decision spends the
      * Supply, so a start runs an operation before its first decision and
      * cannot use the playable-option preview gate. Its banner question
      * refuses the walk when no banner is in reach under the picks. */
```

- [ ] **Step 8: Add the reach modifiers**

Create `src/main/scala/oathdigital/gameplay/powers/challenge/ChallengeModifiers.scala`:

```scala
package oathdigital.gameplay.powers.challenge

import oathdigital.catalog.Relic
import oathdigital.gameplay.actions.challenge.{ChallengeReach,
  SchoolOfVinesCard, SigilOfTheEyeCard, SigilOfTheHeartCard}
import oathdigital.gameplay.powerresolver.{Contribution, ContributingPower,
  OfferReason, OnEdificeFace, PowerCtx, PowerUse}
import oathdigital.gameplay.powers.{CatalogResolution, SelectedModifier}
import oathdigital.model._

/** The Challenge modifiers (catalog batch 10, slice 11). A pick is the
  * whole effect: the Challenge reads it ([[ChallengeReach]]), so none hooks
  * a window, and none writes a line, since the start line names it. */
object ChallengeModifiers:
  val all: Vector[ContributingPower] =
    Vector(SchoolOfVines, SigilOfTheEye, SigilOfTheHeart)

  /** Unavailable while the picks so far already reach every banner
    * `card`'s pick would: a pick that opened nothing would leave "Start
    * Challenge" unavailable with nothing else to pick. */
  private[challenge] def reachReason(ctx: PowerCtx, card: CardId)
      : Option[OfferReason] =
    Option.unless(ChallengeReach.widens(ctx.state, ctx.actor, ctx.answered,
      card))(OfferReason.Unavailable(ChallengeReach.noNewBanner))

/** A Sigil: a free relic Challenge modifier that lifts the pawn-site
  * requirement for one banner. */
sealed trait Sigil extends SelectedModifier:
  def actions: Set[MajorActionType] = Set(MajorActionType.Challenge)
  def effects: Map[PowerWindow, Vector[Contribution]] = Map.empty
  override def offerReason(ctx: PowerCtx): Option[OfferReason] =
    ChallengeModifiers.reachReason(ctx, cardId)

/** Sigil of the Eye (relic R43): "You may challenge the Darkest Secret
  * from any site." */
case object SigilOfTheEye extends Sigil:
  val id: PowerId = SigilOfTheEyeCard.power.id
  val card: Relic = SigilOfTheEyeCard

/** Sigil of the Heart (relic R44): "You may challenge the People's Favor
  * from any site." */
case object SigilOfTheHeart extends Sigil:
  val id: PowerId = SigilOfTheHeartCard.power.id
  val card: Relic = SigilOfTheHeartCard

/** School of Vines (E27, intact), a free Challenge modifier: "You may
  * challenge a banner held by an enemy even if your pawn is not at their
  * pawn's site." Imperial players are not each other's enemies.
  *
  * It is written as Drowned Library is, since a selected modifier's card
  * is a denizen or a relic, never an edifice face. It is listed only while
  * the intact face is up (`OnEdificeFace`), and wherever its user may use
  * the edifice. */
case object SchoolOfVines extends OnEdificeFace:
  private val printed = SchoolOfVinesCard.intact.power
  val id: PowerId = printed.id
  val edifice: EdificeId = SchoolOfVinesCard.id
  val side: EdificeSide = EdificeSide.Intact

  def source: RuleSourceRef = RuleSourceRef.GameRule(id.value)
  override lazy val resolution: PowerResolution = CatalogResolution.of(printed)
  override def selectionCard: Option[CardId] = Some(edifice)
  override def selectionWindows: Set[PowerWindow] =
    Set(PowerWindow.ChallengeModifierSelection)

  /** Its use at its selection window: free, placing nothing. */
  override def use(ctx: PowerCtx): Option[PowerUse] =
    SelectedModifier.selectionAction(ctx.window).map(_ =>
      PowerUse.of(ctx.actor, printed, edifice, secret = false,
        places = false))

  /** At a selection window, for a Challenge only. */
  override def applicable(ctx: PowerCtx): Boolean =
    SelectedModifier.selectionAction(ctx.window).forall(
      _ == MajorActionType.Challenge)

  override def offerReason(ctx: PowerCtx): Option[OfferReason] =
    ChallengeModifiers.reachReason(ctx, edifice)

  def contributions: Map[PowerWindow, Vector[Contribution]] = Map.empty
```

In `ActionModifiers.scala`:

1. Add `import oathdigital.gameplay.powers.challenge.ChallengeModifiers`.
2. Change the doc comment's first line to "The Search, Trade, Muster,
   Recover and Challenge modifiers, registered together with".
3. Change the closing `VowOfWandering, VowOfPoverty, RelicWorship, SilverCharm)`
   to `VowOfWandering, VowOfPoverty, RelicWorship, SilverCharm) ++` and add
   the line `      ChallengeModifiers.all` after it.

- [ ] **Step 9: Regenerate the snapshots and run the suites**

```bash
NOTE_KEYS_SNAPSHOT=write UNIMPLEMENTED_SNAPSHOT=write ./sbtw "testOnly oathdigital.gameplay.powers.*"
```

```bash
git diff src/test/resources
```

Expected: `unimplemented.txt` loses exactly `edifice.e27.intact`,
`relic.sigil-of-the-eye` and `relic.sigil-of-the-heart`;
`declared-keys.txt` is unchanged.

Run: `./sbtw "testOnly oathdigital.gameplay.powers.challenge.* oathdigital.gameplay.ChallengeProcedureSuite oathdigital.application.gamelog.ProcedureNoteLinesSuite oathdigital.application.chronicle.* oathdigital.gameplay.powers.setup.* oathdigital.gameplay.chronicle.ShapeEmpireSuite"`
Expected: PASS, including `ChallengeProcedureSuite`'s two "no legal
banner" tests unchanged.

If a `ChallengeReachSuite` test fails on a role or a pawn site, print the
table (`Look(ready).pawn(p2)`, `Imperials.role(ready, p1)`) before
changing the test: the suite assumes the quiet table seats every player an
Exile and p2 away from p1.

- [ ] **Step 10: Commit**

```bash
git add src/main/scala/oathdigital/gameplay/actions/challenge/ChallengeReachCards.scala src/main/scala/oathdigital/gameplay/actions/challenge/ChallengeReach.scala src/main/scala/oathdigital/gameplay/actions/challenge/ChallengeProcedure.scala src/main/scala/oathdigital/gameplay/powers/challenge/ChallengeModifiers.scala src/main/scala/oathdigital/gameplay/powers/ActionModifiers.scala src/main/scala/oathdigital/model/Imperials.scala src/main/scala/oathdigital/gameplay/walker/WalkerProcedureRegistry.scala src/main/scala/oathdigital/gameplay/powers/setup/RuinedSiteSetupRules.scala src/main/scala/oathdigital/catalog/holding/HoldingRelics.scala src/main/scala/oathdigital/gameplay/cards/NewFoundations.scala src/test/scala/oathdigital/gameplay/powers/challenge/ChallengeReachSuite.scala src/test/scala/oathdigital/gameplay/powers/economy/AnimalPlaymatesSuite.scala src/test/scala/oathdigital/gameplay/powers/whenplayed/AnimalHostSuite.scala src/test/scala/oathdigital/gameplay/powers/whenplayed/ThreateningRoarSuite.scala src/test/scala/oathdigital/gameplay/powers/whenplayed/DazzleSuite.scala src/test/scala/oathdigital/gameplay/powers/action/ForestHornSuite.scala src/test/scala/oathdigital/gameplay/chronicle/ShapeEmpireSuite.scala src/test/scala/oathdigital/gameplay/powers/setup/RuinedSetupRulesSuite.scala src/test/scala/oathdigital/application/chronicle/ChronicleTtsCodecSuite.scala src/test/resources/powers/unimplemented.txt
git commit -m "$(cat <<'EOF'
feat(powers): School of Vines and the Sigils widen a Challenge's reach

The Challenge's banner question reads the modifier picks and refuses the
walk when no banner is in reach under them, so Start Challenge waits for
a pick that opens one. The cards move beside the Challenge.

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>
EOF
)"
```

**Review checkpoint 1:** review Task 1's diff (spec slice 11, findings 1
to 4 and 10) before Task 2.

---

### Task 2: Magician's Code

**Files:**
- Create: `src/test/scala/oathdigital/gameplay/powers/challenge/MagiciansCodeSuite.scala`
- Modify: `src/main/scala/oathdigital/gameplay/actions/challenge/ChallengeReachCards.scala`
- Modify: `src/main/scala/oathdigital/gameplay/actions/challenge/ChallengeReach.scala`
- Modify: `src/main/scala/oathdigital/gameplay/actions/challenge/ChallengeProcedure.scala`
- Modify: `src/main/scala/oathdigital/gameplay/powers/challenge/ChallengeModifiers.scala`
- Modify: `src/main/scala/oathdigital/catalog/holding/HoldingArcaneDenizens.scala` (the card moves out)
- Modify: `src/main/scala/oathdigital/gameplay/cards/NewFoundations.scala` (import)
- Modify (imports): every test file importing `oathdigital.catalog.holding.MagiciansCodeCard`, and `RivalKhanSuite`
- Modify: `src/test/resources/powers/unimplemented.txt` (regenerated)

**Interfaces:**
- Consumes: Task 1's `ChallengeReach`, `ChallengeModifiers`,
  `ChallengeProcedure.legalBanners(state, actor, picks)`.
- Produces:
  - `MagiciansCodeCard` in `oathdigital.gameplay.actions.challenge`
  - `ChallengeReach.CodeSecrets: Int` (2), `ChallengeReach.codeTooFew: String`
  - `ChallengeReach.bonus(picks: Vector[CardId], banner: Banner): Int`
  - `ChallengeReach.codeReaches(ready: ReadyGame, actor: PlayerId, answered: Vector[Answered]): Boolean`
  - `MagiciansCode` (a `SelectedModifier`) in `ChallengeModifiers.all`

- [ ] **Step 1: Write the failing suite**

Create `src/test/scala/oathdigital/gameplay/powers/challenge/MagiciansCodeSuite.scala`:

```scala
package oathdigital.gameplay.powers.challenge

import oathdigital.catalog.ModifierMark
import oathdigital.gameplay.{ModifierPlay, OathRules}
import oathdigital.gameplay.actions.ProcedureNotes
import oathdigital.gameplay.actions.challenge.{ChallengeProcedure,
  ChallengeReach, MagiciansCodeCard, SigilOfTheHeartCard}
import oathdigital.gameplay.powerresolver.ModifierPicks
import oathdigital.gameplay.powers.{NoteText, WalkerPowerCatalog}
import oathdigital.gameplay.setup.FirstGameSetupFixture.catalog
import oathdigital.model._
import oathdigital.model.DecisionAnswer.{ChooseAmountAnswer, ChooseOneAnswer}
import oathdigital.model.OathState.Ready
import oathdigital.testkit.{ActPowerHarness, Look, MenuPlay, Outcomes,
  ParkedNode, Table}
import oathdigital.testkit.Outcomes.assertWireRoundTrips
import oathdigital.testkit.Table.{p1, p2}
import oathdigital.testkit.Unwrap.accepted

/** Magician's Code (card 32), a Challenge modifier for 2 favor: a
  * Challenge of the Darkest Secret gains 2 secrets and places them with
  * the rest (catalog batch 10, slice 11). */
class MagiciansCodeSuite extends munit.FunSuite:
  private val powers = WalkerPowerCatalog.default(catalog)
  private val rules = new OathRules(catalog, walkerPowerCatalog = powers)
  private val window = PowerWindow.ChallengeModifierSelection
  private val peoplesFavor = DecisionOptionRef.Banner(Banner.PeoplesFavor)
  private val darkestSecret = DecisionOptionRef.Banner(Banner.DarkestSecret)

  /** p1, in Act with 4 favor and `faceUp` faceup secrets, holds Magician's
    * Code faceup. The People's Favor is unheld with 6 favor, out of p1's
    * reach; the Darkest Secret is unheld with `onBanner` secrets. */
  private def p1HoldsCodeBeforeDarkestSecret(faceUp: Int, onBanner: Int)
      : Table = Table.start.favor(p1, 4).secrets(p1, faceUp = faceUp)
    .adviser(p1, MagiciansCodeCard)
    .peoplesFavor(None, favor = 6).darkestSecret(None, secrets = onBanner)

  private def parkedQuestion(state: OathState): Decide =
    ParkedNode.of(state, catalog, powers) match
      case Right(Some(ParkedNode.Decision(_, decide, _))) => decide
      case other => fail(s"expected a parked question, got $other")

  private def chooseOneOptions(decide: Decide): Vector[DecisionOption] =
    decide.query match
      case one: DecisionQuery.ChooseOne => one.options
      case other => fail(s"expected a choose-one, got $other")

  /** p1's Challenge started with `picks` picked, parked on its banner
    * question. */
  private def challengeAtBannerWith(table: Table, picks: PowerId*)
      : OathTransition =
    val started = accepted(ModifierPlay.started(rules, Ready(table.ready),
      ActionRef.Challenge, p1, window, picks.toVector))
    assertEquals(parkedQuestion(started.state).decisionId,
      ChallengeProcedure.bannerDecisionId)
    started

  private def bannerChosen(from: OathTransition, banner: DecisionOptionRef)
      : OathTransition = accepted(rules.resolveWalker(from.state, p1,
    ChallengeProcedure.bannerDecisionId, ChooseOneAnswer(banner)))

  /** The amount question's range at `at`. */
  private def amountRange(at: OathTransition): (Int, Int) =
    parkedQuestion(at.state).query match
      case query: DecisionQuery.ChooseAmount => (query.min, query.max)
      case other => fail(s"expected an amount, got $other")

  test("its printed power carries the Challenge icon"):
    assertEquals(MagiciansCodeCard.power.modifier,
      Some(ModifierMark.Action(MajorActionType.Challenge)))

  test("its 2 secrets reach a Darkest Secret p1's own cannot, even with " +
      "none of p1's own"):
    val table = p1HoldsCodeBeforeDarkestSecret(faceUp = 0, onBanner = 1)
    val ready = table.ready
    val started = challengeAtBannerWith(table, MagiciansCode.id)
    assertEquals(chooseOneOptions(parkedQuestion(started.state)).map(_.ref),
      Vector(darkestSecret))
    val chosen = bannerChosen(started, darkestSecret)
    // 0 of p1's own and the 2 gained beat the banner's 1.
    assertEquals(amountRange(chosen), (0, 0))
    val done = accepted(rules.resolveWalker(chosen.state, p1,
      ChallengeProcedure.amountDecisionId, ChooseAmountAnswer(0)))
    val after = Outcomes.ready(done.state)
    assertEquals(Look(after).darkestSecretHolder, Some(p1))
    assertEquals(Look(after).secretsOnDarkestSecret, 2)
    assertEquals(Look(after).faceUpSecrets(p1), 0)
    // The pick paid 2 favor onto the card.
    assertEquals(Look(after).favor(p1), 4 - 2)
    assertEquals(Look(after).adviserTokens(p1, MagiciansCodeCard),
      Tokens(2, 0))
    val events = started.events ++ chosen.events ++ done.events
    assertEquals(NoteText.said(ProcedureNotes.id(ActionRef.Challenge),
      ChallengeProcedure.noteKeys, events), Vector(NoteText.Said(
        "took-from-bank-secrets",
        "Took Darkest Secret from the bank with 2 secrets", covers = false)))
    assertEquals(ActPowerHarness.replayed(rules, ready, events), after)
    assertWireRoundTrips(events)

  test("with p1's own secrets, the amount starts 2 lower and the payment " +
      "places the 2 gained with them"):
    val table = p1HoldsCodeBeforeDarkestSecret(faceUp = 4, onBanner = 1)
    // Unpicked, p1 must place more than the banner's 1. (The banner holds
    // 1 so its ribbon burns it and places none: a larger one would ask
    // which of the tied empty sites take the placed half.)
    assertEquals(amountRange(bannerChosen(challengeAtBannerWith(table),
      darkestSecret)), (2, 4))
    val chosen = bannerChosen(challengeAtBannerWith(table, MagiciansCode.id),
      darkestSecret)
    assertEquals(amountRange(chosen), (0, 4))
    val done = accepted(rules.resolveWalker(chosen.state, p1,
      ChallengeProcedure.amountDecisionId, ChooseAmountAnswer(2)))
    val after = Outcomes.ready(done.state)
    assertEquals(Look(after).secretsOnDarkestSecret, 2 + 2)
    assertEquals(Look(after).faceUpSecrets(p1), 4 - 2)

  test("it cannot be picked when 2 more secrets do not reach the Darkest " +
      "Secret"):
    // p2 holds the People's Favor away, and Sigil of the Heart opens it,
    // so the modifier decision is asked; 2 + 2 does not beat 5.
    val table = p1HoldsCodeBeforeDarkestSecret(faceUp = 2, onBanner = 5)
      .peoplesFavor(Some(p2), favor = 1).relic(p1, SigilOfTheHeartCard)
    val begun = accepted(MenuPlay.started(rules, Ready(table.ready), p1,
      ActionRef.Challenge))
    val asked = parkedQuestion(begun.state)
    assertEquals(asked.decisionId, ModifierPicks.decisionId(window))
    val code = DecisionOptionRef.Denizen(MagiciansCodeCard.id)
    assertEquals(chooseOneOptions(asked).find(_.ref == code),
      Some(DecisionOption.Unavailable(
        DecisionOption.forRef(code).getOrElse(fail("no Code option")),
        OptionReason.Written(ChallengeReach.codeTooFew))))

  test("picked, it adds nothing to a Challenge of the People's Favor"):
    // p1's 6 favor can outbid the People's Favor's 2 after the pick's 2.
    val table = p1HoldsCodeBeforeDarkestSecret(faceUp = 0, onBanner = 1)
      .favor(p1, 6).peoplesFavor(None, favor = 2)
    val started = challengeAtBannerWith(table, MagiciansCode.id)
    assertEquals(chooseOneOptions(parkedQuestion(started.state)).map(_.ref),
      Vector(peoplesFavor, darkestSecret))
    // The pick left p1 4 favor: the People's Favor's range is as usual.
    assertEquals(amountRange(bannerChosen(started, peoplesFavor)), (3, 4))
```

- [ ] **Step 2: Run the suite to see it fail**

Run: `./sbtw "testOnly oathdigital.gameplay.powers.challenge.MagiciansCodeSuite"`
Expected: compile errors: `MagiciansCodeCard` is not in
`oathdigital.gameplay.actions.challenge`, and `MagiciansCode`,
`ChallengeReach.codeTooFew` are not found.

- [ ] **Step 3: Move the card beside the Challenge**

1. Append to `ChallengeReachCards.scala`, and add `Denizen` to its
   `oathdigital.catalog` import:

```scala
object MagiciansCodeCard extends Denizen(DenizenId("32"), "Magician's Code", Suit.Arcane):
  val power = PrintedPower(PowerId("denizen.magicians-code"),
    persistent = false, cost = Cost(favor = 2),
    modifier = Some(ModifierMark.Action(MajorActionType.Challenge)),
    text = "If you're challenging the Darkest Secret, gain [secret] " +
      "[secret] and place them on the banner along with any other " +
      "[secret] you're placing _(even none)_.")
  val powers: Vector[PrintedPower] = Vector(power)
```

2. In `HoldingArcaneDenizens.scala`, delete `object MagiciansCodeCard`. Its
   imports stay in use.
3. In `NewFoundations.scala`, add `MagiciansCodeCard` to the
   `oathdigital.gameplay.actions.challenge` import.
4. Rewrite the test imports:

```bash
git grep -l '^import oathdigital.catalog.holding.MagiciansCodeCard$' -- src | xargs perl -pi -e 's/^import oathdigital\.catalog\.holding\.MagiciansCodeCard$/import oathdigital.gameplay.actions.challenge.MagiciansCodeCard/'
```

```bash
perl -pi -e 's/^import oathdigital\.catalog\.holding\.\{MagiciansCodeCard, SearchPartyCard\}$/import oathdigital.catalog.holding.SearchPartyCard\nimport oathdigital.gameplay.actions.challenge.MagiciansCodeCard/' src/test/scala/oathdigital/gameplay/powers/campaign/RivalKhanSuite.scala
```

```bash
git grep -n 'MagiciansCodeCard' -- src | grep holding
```

   Expected: no output. Record the list of files the first command
   rewrote (`git status --short`) for the commit.

- [ ] **Step 4: Teach `ChallengeReach` the Code**

In `ChallengeReach.scala`:

1. Add to the doc comment's list:
   `  *  - Magician's Code: 2 secrets, gained and placed with the rest, for`
   `  *    the Darkest Secret only.`
2. Add:

```scala
  /** The secrets Magician's Code gains and places. */
  val CodeSecrets: Int = 2

  /** Why Magician's Code cannot be picked. */
  val codeTooFew: String =
    "The Darkest Secret is out of reach, even with 2 more secrets"

  /** The secrets Magician's Code adds to a Challenge of `banner`: 2 for
    * the Darkest Secret while it is picked, else none. */
  def bonus(picks: Vector[CardId], banner: Banner): Int =
    if banner == Banner.DarkestSecret && picks.contains(MagiciansCodeCard.id)
    then CodeSecrets else 0

  /** Whether the Darkest Secret is in reach with Magician's Code picked
    * beside the picks among `answered`. */
  def codeReaches(ready: ReadyGame, actor: PlayerId,
      answered: Vector[Answered]): Boolean =
    ChallengeProcedure.legalBanners(ready, actor,
      ModifierPicks.cards(answered) :+ MagiciansCodeCard.id)
      .contains(Banner.DarkestSecret)
```

- [ ] **Step 5: Count, bound and pay the Code's secrets**

In `ChallengeProcedure.scala`:

1. In `legalBanners`, change the last comparison to
   `BannerRules.playerResources(player, banner) + ChallengeReach.bonus(picks, banner) >`
   (wrapped at 80 columns), and add to its doc comment: "Magician's Code's
   2 secrets count toward the Darkest Secret (`ChallengeReach.bonus`)."
2. Change the amount `Branch` to
   `Branch((ready, pending) => bannerOf(pending).toVector.map(banner => amountDecision(ready, actor, banner, ChallengeReach.bonus(picks(pending), banner)))),`
   wrapped at 80 columns.
3. Replace `amountDecision` with:

```scala
  /** How many of the actor's own resources go on the banner: strictly more
    * than it holds, counting `bonus`, the secrets Magician's Code adds, so
    * the least amount is 2 lower with the Code, and never below 0. */
  private def amountDecision(ready: ReadyGame, actor: PlayerId,
      banner: Banner, bonus: Int): Operation =
    val current = ready.game.current
    val prior = BannerRules.resources(current, banner)
    val own = current.player(actor)
      .fold(0)(BannerRules.playerResources(_, banner))
    val least = math.max(0, prior + 1 - bonus)
    val unit = banner match
      case Banner.PeoplesFavor => "favor"
      case Banner.DarkestSecret => "secrets"
    val heading =
      if bonus == 0 then s"Place more than $prior $unit to take ${banner.key}"
      else s"Place more than $prior $unit, counting Magician's Code's " +
        s"$bonus, to take ${banner.key}"
    Decide(amountDecisionId, actor, DecisionQuery.ChooseAmount(least,
      math.max(least, own), Some(heading), "Take banner"),
      window = Some(PowerWindow.ChallengeAmountSelection))
```

4. In `effects`, read the bonus beside the banner and the amount, and put
   the gain before the payment:

```scala
    (for
      banner <- bannerOf(pending)
      amount <- amountOf(pending)
    yield
      val bonus = ChallengeReach.bonus(picks(pending), banner)
      Vector[Operation](
        Sequence(ChallengeRibbon.steps(actor, banner),
          Some(PowerWindow.ChallengeRibbon)),
        Sequence(gained(actor, bonus) ++ Vector[Operation](
          payment(actor, banner, amount + bonus),
          // (keep the existing comment and Note here unchanged)
          Note(ProcedureNotes.id(ActionRef.Challenge), tookNote(actor, banner,
            BannerRules.holder(ready.game.current, banner))),
          custody(ready, actor, banner)), Some(PowerWindow.ChallengePlacement))))
```

   Keep the existing comment above the `Note` where it is, and the
   `.getOrElse` fallback after the `for`.
5. Add below `payment`:

```scala
  /** Magician's Code's secrets, from the shared bank to the actor's board
    * just before the payment, which places them with the rest: the
    * Challenge's line then counts them. None without the Code. */
  private def gained(actor: PlayerId, bonus: Int): Vector[Operation] =
    Option.when(bonus > 0)(Move(Piece.Secrets(bonus),
      PositionedLocation(Location.SharedBank),
      PositionedLocation(Location.PlayArea(actor))): Operation).toVector
```

- [ ] **Step 6: Add the Code's power**

In `ChallengeModifiers.scala`:

1. Add `Denizen` to the `oathdigital.catalog` import and
   `MagiciansCodeCard` to the `gameplay.actions.challenge` import.
2. Add `MagiciansCode` to `all`:
   `Vector(SchoolOfVines, SigilOfTheEye, SigilOfTheHeart, MagiciansCode)`.
3. Change `ChallengeModifiers`' doc comment's first sentence to "The
   Challenge modifiers (catalog batch 10, slice 11)." and keep the rest.
4. Append:

```scala
/** Magician's Code (card 32), a Challenge modifier for 2 favor, placed on
  * the card: "If you're challenging the Darkest Secret, gain [secret]
  * [secret] and place them on the banner along with any other [secret]
  * you're placing (even none)." The Challenge counts, bounds and pays the
  * 2 secrets ([[ChallengeReach.bonus]]). It is unavailable while the
  * Darkest Secret would stay out of reach with them. Picked, then the
  * People's Favor chosen, it does nothing, and its favor stays paid. */
case object MagiciansCode extends SelectedModifier:
  val id: PowerId = MagiciansCodeCard.power.id
  val card: Denizen = MagiciansCodeCard
  def actions: Set[MajorActionType] = Set(MajorActionType.Challenge)
  override def cost: Cost = MagiciansCodeCard.power.cost
  def effects: Map[PowerWindow, Vector[Contribution]] = Map.empty

  override def offerReason(ctx: PowerCtx): Option[OfferReason] =
    Option.unless(ChallengeReach.codeReaches(ctx.state, ctx.actor,
      ctx.answered))(OfferReason.Unavailable(ChallengeReach.codeTooFew))
```

- [ ] **Step 7: Regenerate the snapshots and run the suites**

```bash
NOTE_KEYS_SNAPSHOT=write UNIMPLEMENTED_SNAPSHOT=write ./sbtw "testOnly oathdigital.gameplay.powers.*"
```

Expected: `unimplemented.txt` loses exactly `denizen.magicians-code`;
`declared-keys.txt` is unchanged.

Run: `./sbtw "testOnly oathdigital.gameplay.powers.challenge.* oathdigital.gameplay.ChallengeProcedureSuite oathdigital.gameplay.PhasePowerSuite oathdigital.gameplay.EconomyWalkerSuite oathdigital.gameplay.MusterProcedureSuite oathdigital.gameplay.TradeProcedureSuite oathdigital.gameplay.MinorActionsSuite oathdigital.testkit.TableSuite oathdigital.application.*"`
Expected: PASS.

`PhasePowerFixture` uses Magician's Code's printed power as the stand-in
for a synthetic phase power, and the production walker catalog now
registers the same power id as a Challenge modifier. If `PhasePowerSuite`
or a projection suite fails on that, stop and report: choosing another
stand-in card changes what those suites assert.

- [ ] **Step 8: Commit**

```bash
git add src/main/scala/oathdigital/gameplay/actions/challenge/ChallengeReachCards.scala src/main/scala/oathdigital/gameplay/actions/challenge/ChallengeReach.scala src/main/scala/oathdigital/gameplay/actions/challenge/ChallengeProcedure.scala src/main/scala/oathdigital/gameplay/powers/challenge/ChallengeModifiers.scala src/main/scala/oathdigital/catalog/holding/HoldingArcaneDenizens.scala src/main/scala/oathdigital/gameplay/cards/NewFoundations.scala src/test/scala/oathdigital/gameplay/powers/challenge/MagiciansCodeSuite.scala src/test/scala/oathdigital/gameplay/powers/campaign/RivalKhanSuite.scala src/test/resources/powers/unimplemented.txt
```

Then add, by name, each test file the import rewrite in Step 3 changed
(from `git status --short`), and commit:

```bash
git commit -m "$(cat <<'EOF'
feat(powers): implement Magician's Code

A Challenge of the Darkest Secret counts the Code's 2 secrets toward
reach and the amount, gains them to the board and pays them with the
rest, so the Challenge's line counts them.

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>
EOF
)"
```

---

### Task 3: Favored Son, docs and gates

**Files:**
- Create: `src/main/scala/oathdigital/gameplay/powers/whenplayed/FavoredSon.scala`
- Create: `src/test/scala/oathdigital/gameplay/powers/whenplayed/FavoredSonSuite.scala`
- Modify: `src/main/scala/oathdigital/gameplay/actions/challenge/ChallengeRibbon.scala`
- Modify: `src/main/scala/oathdigital/gameplay/powers/whenplayed/WhenPlayedPowers.scala`
- Modify: `src/main/scala/oathdigital/catalog/holding/HoldingHearthDenizens.scala` (the card moves out)
- Modify: `src/test/resources/powers/unimplemented.txt`, `src/test/resources/notes/declared-keys.txt` (regenerated)
- Modify: `docs/superpowers/specs/2026-11-03-catalog-batch-10-design.md`
- Modify: `docs/ROADMAP.md`

**Interfaces:**
- Consumes: the existing `ChallengeRibbon`, widened here.
- Produces:
  - `ChallengeRibbon` widened to `private[gameplay]`, with
    `peoplesFavorReturns(ready: ReadyGame): Vector[CoreOperation]`
  - `FavoredSonCard` and `case object FavoredSon extends WhenPlayedPower`
    in `oathdigital.gameplay.powers.whenplayed`, with note keys `took`,
    `held` and `none`

- [ ] **Step 1: Write the failing suite**

Create `src/test/scala/oathdigital/gameplay/powers/whenplayed/FavoredSonSuite.scala`:

```scala
package oathdigital.gameplay.powers.whenplayed

import oathdigital.gameplay.actions.BannerRules
import oathdigital.gameplay.powers.NoteText
import oathdigital.gameplay.powers.banner.SecretTestamentCard
import oathdigital.gameplay.powers.targeting.{LostTongue, LostTongueCard}
import oathdigital.model._
import oathdigital.testkit.{Look, Outcomes, Table}
import oathdigital.testkit.Outcomes.assertWireRoundTrips
import oathdigital.testkit.Table.{p1, p2}

/** Favored Son (card 235), WHEN PLAYED: take the People's Favor, and
  * return its favor by its left ribbon (catalog batch 10, slice 11). */
class FavoredSonSuite extends munit.FunSuite:
  import WhenPlayedHarness._

  private val power = registered[FavoredSon.type]
  private val son = power.cardId

  private val took = NoteText.Said("took", "Took the People's Favor.",
    covers = false)

  /** The favor banks after the People's Favor's `favor` returned from
    * `ready`'s banks, leftmost least first, as a Challenge returns it. */
  private def banksAfterRibbon(ready: ReadyGame, favor: Int)
      : Map[Suit, Int] = BannerRules.addFavor(ready.banks.favor,
    BannerRules.raidFavorReturn(ready.banks.favor, favor))

  test("takes the People's Favor from its holder and returns its favor to " +
      "the banks"):
    val ready = Table.start.adviser(p1, son)
      .peoplesFavor(Some(p2), favor = 3).ready
    val done = Outcomes.finished(play(ready, power, son))
    assertEquals(Look(done.treeless).peoplesFavorHolder, Some(p1))
    assertEquals(Look(done.treeless).favorOnPeoplesFavor, 0)
    assertEquals(done.treeless.banks.favor, banksAfterRibbon(ready, 3))
    assertEquals(said(power, done.events), Vector(took))
    assertJournalRebuilds(ready, done.events, done.treeless)
    assertWireRoundTrips(done.events)

  test("takes an unheld People's Favor from the shared bank"):
    val ready = Table.start.adviser(p1, son).peoplesFavor(None, favor = 2)
      .ready
    val done = Outcomes.finished(play(ready, power, son))
    assertEquals(Look(done.treeless).peoplesFavorHolder, Some(p1))
    assertEquals(Look(done.treeless).favorOnPeoplesFavor, 0)
    assertEquals(done.treeless.banks.favor, banksAfterRibbon(ready, 2))
    assertEquals(said(power, done.events), Vector(took))

  test("a user who already holds the People's Favor changes nothing"):
    val ready = Table.start.adviser(p1, son)
      .peoplesFavor(Some(p1), favor = 2).ready
    val done = Outcomes.finished(play(ready, power, son))
    assertEquals(ops(done.events), Vector.empty)
    assertEquals(Look(done.treeless).favorOnPeoplesFavor, 2)
    assertEquals(said(power, done.events), Vector(NoteText.Said("held",
      "Already held the People's Favor.", covers = false)))

  test("when Lost Tongue refuses the take, the People's Favor and its " +
      "favor stay"):
    // p2 holds the People's Favor and Lost Tongue faceup; p1 rules no
    // Nomad card.
    val ready = Table.start.adviser(p1, son).adviser(p2, LostTongueCard)
      .peoplesFavor(Some(p2), favor = 3).ready
    val done = Outcomes.finished(playBeside(ready, power, son, LostTongue))
    assertEquals(Look(done.treeless).peoplesFavorHolder, Some(p2))
    assertEquals(Look(done.treeless).favorOnPeoplesFavor, 3)
    assertEquals(said(power, done.events), Vector(NoteText.Said("none",
      "The People's Favor did not move.", covers = false)))

  test("Secret Testament leaves the take alone: the favor goes to the " +
      "banks, not to its holder"):
    val ready = Table.start.adviser(p1, son).relic(p2, SecretTestamentCard)
      .peoplesFavor(Some(p2), favor = 3).ready
    val done = Outcomes.finished(fireWithEveryPower(ready, hook(son)))
    assertEquals(Look(done.treeless).peoplesFavorHolder, Some(p1))
    assertEquals(Look(done.treeless).favor(p2), Look(ready).favor(p2))
    assertEquals(done.treeless.banks.favor, banksAfterRibbon(ready, 3))
```

- [ ] **Step 2: Run the suite to see it fail**

Run: `./sbtw "testOnly oathdigital.gameplay.powers.whenplayed.FavoredSonSuite"`
Expected: compile error: `FavoredSon` is not found.

- [ ] **Step 3: Give the ribbon one source**

In `ChallengeRibbon.scala`:

1. Change `private[challenge] object ChallengeRibbon:` to
   `private[gameplay] object ChallengeRibbon:`.
2. Rename `private def favorReturns(ready: ReadyGame)` to
   `def peoplesFavorReturns(ready: ReadyGame)`, update its one caller in
   `steps`, and give it the doc comment:

```scala
  /** The People's Favor's ribbon: every favor on it returns to the
    * least-favor banks, leftmost first, one at a time. Favored Son's take
    * carries it as its `leaving` (catalog batch 10, slice 11). */
```

- [ ] **Step 4: Implement Favored Son**

Create `src/main/scala/oathdigital/gameplay/powers/whenplayed/FavoredSon.scala`.
The card is copied from `HoldingHearthDenizens.scala` unchanged:

```scala
package oathdigital.gameplay.powers.whenplayed

import oathdigital.catalog.{AdviserOnly, Denizen, Locked, PrintedPower}
import oathdigital.gameplay.actions.BannerRules
import oathdigital.gameplay.actions.challenge.ChallengeRibbon
import oathdigital.gameplay.powerresolver.PowerCtx
import oathdigital.model._

object FavoredSonCard extends Denizen(DenizenId("235"), "Favored Son", Suit.Hearth) with Locked with AdviserOnly:
  val power = PrintedPower(PowerId("denizen.favored-son"),
    persistent = false, cost = Cost.free,
    text = "**WHEN PLAYED,** take the People's Favor. If you do, adjust " +
      "its [favor] using its left ribbon.")
  val powers: Vector[PrintedPower] = Vector(power)

/** Favored Son (card 235), WHEN PLAYED: take the People's Favor; if you do,
  * adjust its favor using its left ribbon (catalog batch 10, slice 11).
  *
  * The user takes the banner from its holder or, unheld, from the shared
  * bank, by a `Take` that is not required, as Wizard's Conclave takes the
  * Darkest Secret. Its `leaving` is the People's Favor ribbon a Challenge
  * walks (`ChallengeRibbon.peoplesFavorReturns`), so the favor returns to
  * the banks only if the take happens. It is not walked under the
  * `ChallengeRibbon` window, so Secret Testament, a Challenge modifier,
  * stays out (ruling 11). When the user already holds the banner, nothing
  * moves. When Lost Tongue refuses the take, the banner and its favor stay.
  */
case object FavoredSon extends WhenPlayedPower:
  val id: PowerId = FavoredSonCard.power.id
  val cardId: DenizenId = FavoredSonCard.id
  private val notes = NoteSet(id)
  override def noteKeys: Vector[NoteKey] = notes.declared
  /** "Took the {People's Favor}." */
  val took: NoteKey = notes.key("took", Vector(NotePart.Text("Took the "),
    NotePart.Arg(0), NotePart.Text(".")))
  /** "Already held the {People's Favor}." */
  val held: NoteKey = notes.key("held", Vector(
    NotePart.Text("Already held the "), NotePart.Arg(0), NotePart.Text(".")))
  /** "The {People's Favor} did not move." */
  val none: NoteKey = notes.key("none", Vector(NotePart.Text("The "),
    NotePart.Arg(0), NotePart.Text(" did not move.")))

  private def cardSource: PowerSourceRef = PowerSourceRef.Card(cardId)
  private val banner = NoteArg.Banner(Banner.PeoplesFavor)

  def effect(ctx: PowerCtx): Vector[Operation] =
    Vector(Branch((live, _) => award(live, ctx.actor)))

  /** The take and its line, or the line that the user already held it. */
  private def award(ready: ReadyGame, actor: PlayerId): Vector[Operation] =
    val holder = BannerRules.holder(ready.game.current, Banner.PeoplesFavor)
    if holder.contains(actor) then Vector(Note(id, _ =>
      Some(held(cardSource, banner))))
    else Vector(
      BuildOps((live, _) => Right(Vector(Take(
        Piece.Banner(Banner.PeoplesFavor), actor,
        holder.fold[Location](Location.SharedBank)(Location.PlayArea(_)),
        Location.PlayArea(actor),
        leaving = ChallengeRibbon.peoplesFavorReturns(live))))),
      Note(id, takenNote(_, actor)))

  /** Whether the user holds the banner now, or Lost Tongue kept it. */
  private def takenNote(states: NoteStates, actor: PlayerId)
      : Option[LogNote] = Some(
    if BannerRules.holder(states.now.game.current, Banner.PeoplesFavor)
        .contains(actor)
    then took(cardSource, banner)
    else none(cardSource, banner))
```

Then:

1. In `HoldingHearthDenizens.scala`, delete `object FavoredSonCard` and
   narrow the catalog import to
   `import oathdigital.catalog.{Denizen, PrintedPower}`: Deed Writer, the
   card left, is neither locked nor adviser-only.
2. In `WhenPlayedPowers.scala`, change `Pilgrimage, AutumnWind)` to
   `Pilgrimage, AutumnWind, FavoredSon)`.

- [ ] **Step 5: Regenerate the snapshots and run the suites**

```bash
NOTE_KEYS_SNAPSHOT=write UNIMPLEMENTED_SNAPSHOT=write ./sbtw "testOnly oathdigital.gameplay.powers.*"
```

```bash
git diff src/test/resources
```

Expected: `unimplemented.txt` loses exactly `denizen.favored-son`, and
`declared-keys.txt` gains exactly:

```
denizen.favored-son held = Already held the {0}.
denizen.favored-son none = The {0} did not move.
denizen.favored-son took = Took the {0}.
```

Run: `./sbtw "testOnly oathdigital.gameplay.powers.whenplayed.* oathdigital.gameplay.powers.banner.* oathdigital.gameplay.ChallengeProcedureSuite oathdigital.gameplay.powers.challenge.*"`
Expected: PASS.

- [ ] **Step 6: Update the spec**

In `docs/superpowers/specs/2026-11-03-catalog-batch-10-design.md`:

1. Change the status line to
   `**Status:** designed 2026-11-03. Slices 1 to 11 landed.`
2. Replace the body of "## Slice 11: Challenge reach", from its first
   paragraph up to (not including) "## Slice 12: Nested card play", with:

```markdown
The Challenge's start no longer gates on `legalBanners`; `build` keeps
only its `supportedFaces` check. The banner question reads the picks with
`ModifierPicks.cards(answered)`, as `SearchProcedure` does, and with no
banner in reach under them it refuses the walk with the same
`NoPlayableOption("challenge")`. An empty question alone would not refuse
it: the walk would park on it. "Start Challenge" therefore previews
unavailable until a pick opens a banner, and `QueuedStart.priced` finds
that pick for the Act menu. The four cards live beside the Challenge in
`actions/challenge`, as Underground Library lives beside the Recover, and
`ChallengeReach` reads their picks:

- **School of Vines:** a banner an enemy holds no longer needs the user's
  pawn at its holder's pawn's site. Imperial players are not each other's
  enemies (rules glossary, "Enemy"), so a Citizen still needs the
  Chancellor's site.
- **Sigil of the Eye:** the same for the Darkest Secret, whoever holds it;
  **Sigil of the Heart:** for the People's Favor.
- **Magician's Code** (2 favor, placed on the card), for the Darkest Secret
  only: the strictly-more check counts the user's secrets plus 2, and the
  amount's lower bound drops by 2, to no less than 0. The 2 secrets are
  gained to the user's board just before the payment, which places them
  with the rest, so the Challenge's line counts them: "Took Darkest Secret
  from the bank with 2 secrets." It is selectable only when the Darkest
  Secret is then in reach.

School of Vines and the Sigils are selectable only when they bring a
banner into reach that the picks so far leave out: "Brings no new banner
into reach." A pick that opened nothing would leave "Start Challenge"
unavailable with nothing else to pick.

**Favored Son.** WHEN PLAYED: a non-required `Take` of the People's Favor
from its holder or the shared bank, as Wizard's Conclave takes a banner,
with the People's Favor branch of `ChallengeRibbon` as its `leaving`.
`ChallengeRibbon` widens to `private[gameplay]` so the ribbon has one
source; it is not walked under the `ChallengeRibbon` window, so Secret
Testament stays out (ruling 11). If the user holds the banner, nothing
happens. Lines: "Took the People's Favor.", "Already held the People's
Favor.", or, when Lost Tongue refuses the take, "The People's Favor did
not move."

Tests: each modifier opening a banner that was out of reach, and the
Challenge refused without it; a reach modifier that opens nothing shown
unavailable; School of Vines and an Imperial ally's banner; the Code's
amounts, payment and line; the Code unselectable when 2 secrets do not
suffice; Favored Son from a player, from the shared bank, when already
held, under Lost Tongue and beside Secret Testament; existing Challenge
suites unchanged.
```

3. In "## Seams opened", change the row
   `| Challenge judged after modifiers | 11 | four Challenge modifiers |`
   to `| Banner question under the picks, \`ChallengeReach\` | 11 | four Challenge modifiers |`.

- [ ] **Step 7: Update the ROADMAP**

In `docs/ROADMAP.md`, delete these three lines from the engine-blocked
list:

```markdown
- **Challenge** (`legalBanners` is hard-coded, no ribbon adjustment): School
  of Vines, Sigil of the Eye, Sigil of the Heart, Favored Son, Magician's
  Code.
```

- [ ] **Step 8: Run the gates**

```bash
python3 scripts/check-architecture.py
```

```bash
python3 scripts/check-markdown-links.py
```

Expected: both pass. Then run the full suite with coverage in the
background:

```bash
./sbtw clean coverage test coverageReport
```

Expected: every test passes, and statement coverage is at least 92%.

- [ ] **Step 9: Commit**

```bash
git add src/main/scala/oathdigital/gameplay/powers/whenplayed/FavoredSon.scala src/test/scala/oathdigital/gameplay/powers/whenplayed/FavoredSonSuite.scala src/main/scala/oathdigital/gameplay/actions/challenge/ChallengeRibbon.scala src/main/scala/oathdigital/gameplay/powers/whenplayed/WhenPlayedPowers.scala src/main/scala/oathdigital/catalog/holding/HoldingHearthDenizens.scala src/test/resources/powers/unimplemented.txt src/test/resources/notes/declared-keys.txt docs/superpowers/specs/2026-11-03-catalog-batch-10-design.md docs/ROADMAP.md
git commit -m "$(cat <<'EOF'
feat(powers): implement Favored Son

The People's Favor is taken by a Take whose leaving is the Challenge's
People's Favor ribbon, so the favor returns only when the take happens.

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>
EOF
)"
```

**Review checkpoint 2:** review Tasks 2 and 3 together (spec slice 11,
findings 5 to 9).
