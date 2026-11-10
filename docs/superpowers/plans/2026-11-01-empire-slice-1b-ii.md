# Empire Slice 1b-ii Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Imperial Tactics. While an Imperial player chooses battle plans,
any other Imperial player can offer them battle plans, requirements
(the Darkest Secret, the People's Favor, a pawn at a site) and amounts of
favor and secrets. The user picks offered plans in the normal plan window,
and the allies pay first.

**Architecture:** An offer is a standing fact, the way a consent preference
is. A new out-of-turn command, `SavePlanAid`, is accepted while an Imperial
user's plan decision is parked. A desk validates it and appends a
`PlanAidSaved` event, and that event updates a new `ReadyGame.planAid`
state. The plan window is rebuilt from state at every fold, so it sees the
offers as they are saved:
- `PlanContext` lends the allies' plans and requirements to the user.
- `CampaignPlanApplication` splits each payment between the allies and the
  user.

What aid has been spent is recorded as Campaign dice-pool markers, as a
bandit's applied plan already is. That way undo rewinds the spending with
the user's batch, and the end of the walk clears it.

Allies see a new projection, `planAidPrompt`. Their board highlights what
they can offer, and a panel builds the draft offer.

**Tech Stack:** Scala 3, Scala.js frontend, munit, sbt via `./sbtw`.

**Spec:** `docs/superpowers/specs/2026-10-29-empire-phase-design.md`
(sections "Shared rules", "Slice 1b" and "1b-ii: Imperial Tactics", and
rulings R10, R11, R12 and R16).

## Global Constraints

- **Worktree:**
  - Work in a worktree off `0.2.0-alpha`, never in the main checkout.
  - Before running `./sbtw`, symlink the main checkout's `.tooling` and
    `node_modules` into the worktree.
  - Remove the symlinks before `git worktree remove --force`.
- **Git:**
  - Never run `git add -A`; add files by name.
  - Push nothing.
  - Commit trailer: `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`.
- **Build:** the compiler runs with `-Werror`, so unused imports, unused
  values and unused match arms fail the build.
- **Gates before the last commit:**
  - `./sbtw clean coverage test coverageReport`, with statement coverage of
    at least 92%
  - `./sbtw frontend/test`
  - `python3 scripts/check-architecture.py`, which caps files at 800 lines
    and enforces the layer import rules
  - `python3 scripts/check-markdown-links.py`
- **Imperial roles:** game start keeps refusing them. This slice is reached
  only through test tables (`Table.imperial`).
- **Who takes part:** only Imperial players give or receive aid. An Exile
  user's plan window and an Exile's view are unchanged. A bandit defender is
  unchanged.
- **What aid covers (R16):**
  - Aid pays costs and meets requirements, and nothing more.
  - Great Crusade, Pledge of Defense, Kindred Warriors and Nature Worship
    keep counting only the user's own cards.
  - An ally's pawn at a site meets Towering Rampart's "your pawn is at this
    site".
- **Requirement refs.** These are `DecisionOptionRef` values, spelled the
  way the wire spells them:

  | Requirement | Ref | Wire |
  |---|---|---|
  | Darkest Secret, by the banner | `DecisionOptionRef.Banner(Banner.DarkestSecret)` | `banner:darkest-secret` |
  | Darkest Secret, by the Yew Staff | `DecisionOptionRef.Relic(RelicId("R40"))` | `relic:R40` |
  | People's Favor | `DecisionOptionRef.Banner(Banner.PeoplesFavor)` | `banner:peoples-favor` |
  | The ally's pawn at its site | `DecisionOptionRef.Player(ally)` | `player:<id>` |

- **Plan refs:** a plan is named by its source's `ref`, the same ref the
  plan decision answers with: `denizen:`, `relic:`, `edifice:`, or
  `button:title`.
- **Undo:** offers are not undoable. They open no command batch, as consent
  facts open none. Spent aid is undone with the user's batch.
- **Unavailable options** stay on offer, disabled with a hover reason. Hide
  only an option the player's role can never use.
- **Log lines** name only non-actors. The ally is never the actor, so lines
  name them.
- **Names and prose:** test helper names state what they build. Docs,
  comments and commit messages are normal prose.

## File Structure

| File | Responsibility |
|---|---|
| `src/main/scala/distantlands/model/PlanAid.scala` (new) | `AidAmounts`, `AidOffer`, `AidWindow`, `PlanAidState`, `PlanNeed` |
| `src/main/scala/distantlands/model/GameEventProtocol.scala` | `PlanAidEvent`, `PlanAidSaved`, `PlanAidCleared` |
| `src/main/scala/distantlands/model/GameStateProtocol.scala` | `ReadyGame.planAid` |
| `src/main/scala/distantlands/model/CampaignTypes.scala` | `CampaignPlanOffer.requires`, `lender`, `awaitsAid` |
| `src/main/scala/distantlands/model/Decisions.scala` | `DecisionOption.Lent` |
| `src/main/scala/distantlands/model/GameViolation.scala` | `OathViolation.PlanAidUnavailable` |
| `src/main/scala/distantlands/gameplay/aid/PlanAidEvolution.scala` (new) | Evolves the aid events |
| `src/main/scala/distantlands/gameplay/actions/campaign/PlanAid.scala` (new) | Read model: allies, offers, used and remaining aid, payment split, markers |
| `src/main/scala/distantlands/gameplay/powers/campaign/PlanContext.scala` | Lent plans and requirements, full-aid view |
| `src/main/scala/distantlands/gameplay/powers/campaign/BattlePlan.scala` | Offers awaiting aid; the lender |
| `src/main/scala/distantlands/gameplay/powers/campaign/ImperialTactics.scala` (new) | Note keys for the aid hover reasons |
| `src/main/scala/distantlands/gameplay/actions/campaign/CampaignPlanChoice.scala` | The window's gate, the attacker-used reason |
| `src/main/scala/distantlands/gameplay/actions/campaign/CampaignPlanApplication.scala` | Split payments, aid markers |
| `src/main/scala/distantlands/gameplay/OathRulesWalker.scala` | `planAidWindow`, `planAidOfferable` |
| `src/main/scala/distantlands/application/PlanAidDesk.scala` (new) | The `SavePlanAid` command and stale clearing |
| `src/main/scala/distantlands/application/PlanAidProjector.scala` (new) | `planAidPrompt` for an ally |
| `src/main/scala/distantlands/application/gamelog/PlanAidLines.scala` (new) | "p2 offered …" and "p2 withdrew …" |
| `src/main/scala/distantlands/serialization/PlanAidEventCodec.scala` (new) | Wire for the aid events |
| `shared/src/main/scala/distantlands/protocol/...` | `GameIntent.SavePlanAid`, codecs, `PlanAidPromptProjection` |
| `frontend/src/main/scala/distantlands/frontend/PlanAidPanel.scala` (new) | The offer panel |
| `frontend/src/main/scala/distantlands/frontend/PlanAidDraft.scala` (new) | The ally's draft offer and its board views |

---

### Task 1: The aid state, its events and their wire

**Files:**
- Create: `src/main/scala/distantlands/model/PlanAid.scala`
- Create: `src/main/scala/distantlands/gameplay/aid/PlanAidEvolution.scala`
- Create: `src/main/scala/distantlands/serialization/PlanAidEventCodec.scala`
- Modify: `src/main/scala/distantlands/model/GameEventProtocol.scala` (beside `ConsentEvent`, line 19, and the consent cases at 60-73)
- Modify: `src/main/scala/distantlands/model/GameStateProtocol.scala:35-41` (`ReadyGame`)
- Modify: `src/main/scala/distantlands/gameplay/OathRules.scala:71` (the `ConsentEvent` arm)
- Modify: `src/main/scala/distantlands/application/JournalFold.scala:99-119`
- Modify: `src/main/scala/distantlands/serialization/GameEventWire.scala`. Register the family in four places, the way `ConsentEventCodec` is registered:
  - the mix-in at lines 18-21
  - the type constants at 47-51
  - the discriminator and encoder chains at 258-264
  - `decodePayload` at 297
- Test: `src/test/scala/distantlands/model/PlanAidStateSuite.scala` (new)
- Test: `src/test/scala/distantlands/serialization/PlanAidEventWireSuite.scala` (new)
- Test: `src/test/scala/distantlands/application/JournalFoldSuite.scala`, or the suite that covers `JournalFold.carried`. Find it with `grep -rln "carried" src/test`.

**Interfaces:**
- Produces:
  - `AidAmounts(favor, secrets, favorBurnt, secretsBurnt)`, with `+`, `-` (floored at 0), `atLeast`, `isEmpty`, `cost`, `AidAmounts.none` and `AidAmounts.of(cost)`
  - `AidOffer(plans, requirements, amounts)`
  - `AidWindow(actor, side, user)`
  - `PlanAidState(window, offers)`, with `saved`, `offerOf` and `cleared`
  - `enum PlanNeed { DarkestSecret; PeoplesFavor; PawnAt(site) }`
  - `ReadyGame.planAid: PlanAidState`
  - `OathEvent.PlanAidSaved(ally, window, offer)` and `OathEvent.PlanAidCleared`, both `PlanAidEvent`
  - Wire types `"plan-aid.saved"` and `"plan-aid.cleared"`

- [ ] **Step 1: Write the failing state test**

```scala
package oathdigital.model

class PlanAidStateSuite extends munit.FunSuite:
  private val p1 = PlayerId("p1")
  private val p2 = PlayerId("p2")
  private val p3 = PlayerId("p3")
  private val defending = AidWindow(p1, CampaignPlanSide.Defender, p3)
  private val attacking = AidWindow(p1, CampaignPlanSide.Attacker, p1)
  private val twoFavor = AidOffer(amounts = AidAmounts(favor = 2))

  test("a saved offer replaces the ally's earlier offer in the same window"):
    val first = PlanAidState().saved(p2, defending, twoFavor)
    val second = first.saved(p2, defending, AidOffer())
    assertEquals(second.offerOf(p2), AidOffer())
    assertEquals(second.window, Some(defending))

  test("an offer for another window starts the state afresh"):
    val state = PlanAidState().saved(p2, attacking, twoFavor)
      .saved(p2, defending, AidOffer())
    assertEquals(state.window, Some(defending))
    assertEquals(state.offers, Map(p2 -> AidOffer()))

  test("amounts subtract down to zero and compare part by part"):
    val offered = AidAmounts(favor = 2, secrets = 1)
    assertEquals(offered - AidAmounts(favor = 3), AidAmounts(secrets = 1))
    assert(offered.atLeast(AidAmounts(favor = 2)))
    assert(!offered.atLeast(AidAmounts(favorBurnt = 1)))
    assertEquals(AidAmounts.of(Cost(favor = 1, secretBurnt = 2)),
      AidAmounts(favor = 1, secretsBurnt = 2))
```

- [ ] **Step 2: Run it and see it fail to compile**

Run: `./sbtw "testOnly oathdigital.model.PlanAidStateSuite"`
Expected: compile errors, because `AidWindow`, `AidOffer` and `PlanAidState` are not found.

- [ ] **Step 3: Write the model**

`src/main/scala/distantlands/model/PlanAid.scala`:

```scala
package oathdigital.model

/** Favor and secrets an Imperial ally offers towards another Imperial
  * player's battle plans (Imperial Tactics, CR p. 35; Empire phase design,
  * "1b-ii"). Favor to pay is placed onto a plan's card and favor to burn is
  * burnt, from the ally's board; secrets likewise. */
final case class AidAmounts(favor: Int = 0, secrets: Int = 0,
    favorBurnt: Int = 0, secretsBurnt: Int = 0):
  require(favor >= 0 && secrets >= 0 && favorBurnt >= 0 && secretsBurnt >= 0,
    "aid amounts are never negative")

  def +(other: AidAmounts): AidAmounts = AidAmounts(favor + other.favor,
    secrets + other.secrets, favorBurnt + other.favorBurnt,
    secretsBurnt + other.secretsBurnt)

  /** What is left after `other`, never below zero. */
  def -(other: AidAmounts): AidAmounts = AidAmounts(
    math.max(0, favor - other.favor), math.max(0, secrets - other.secrets),
    math.max(0, favorBurnt - other.favorBurnt),
    math.max(0, secretsBurnt - other.secretsBurnt))

  /** Whether every part is at least `other`'s. */
  def atLeast(other: AidAmounts): Boolean = favor >= other.favor &&
    secrets >= other.secrets && favorBurnt >= other.favorBurnt &&
    secretsBurnt >= other.secretsBurnt

  def isEmpty: Boolean = this == AidAmounts.none

  def cost: Cost = Cost(favor = favor, secret = secrets,
    favorBurnt = favorBurnt, secretBurnt = secretsBurnt)

object AidAmounts:
  val none: AidAmounts = AidAmounts()
  def of(cost: Cost): AidAmounts = AidAmounts(cost.favor, cost.secret,
    cost.favorBurnt, cost.secretBurnt)

/** One ally's offer: the plans they lend, named by the ref the plan
  * decision answers with; the requirements they meet, named by the thing
  * that meets them (the Darkest Secret or the Yew Staff, the People's
  * Favor, or the ally as a player for their pawn); and the amounts. The
  * offer is the consent: nobody approves anything. */
final case class AidOffer(plans: Vector[DecisionOptionRef] = Vector.empty,
    requirements: Vector[DecisionOptionRef] = Vector.empty,
    amounts: AidAmounts = AidAmounts.none):
  def isEmpty: Boolean = plans.isEmpty && requirements.isEmpty &&
    amounts.isEmpty

/** The plan window offers reach: the Campaign's actor, the side and the
  * Imperial player choosing that side's plans. */
final case class AidWindow(actor: PlayerId, side: CampaignPlanSide,
    user: PlayerId)

/** The standing offers of one plan window. An offer for another window
  * starts the state afresh, and `PlanAidCleared` empties it once no
  * Imperial plan decision is parked. Spent aid is not here: the plan's
  * application records it in the Campaign's pools (`PlanAid` in
  * gameplay). */
final case class PlanAidState(window: Option[AidWindow] = None,
    offers: Map[PlayerId, AidOffer] = Map.empty):
  def saved(ally: PlayerId, at: AidWindow, offer: AidOffer): PlanAidState =
    if window.contains(at) then copy(offers = offers.updated(ally, offer))
    else PlanAidState(Some(at), Map(ally -> offer))

  def offerOf(ally: PlayerId): AidOffer = offers.getOrElse(ally, AidOffer())

  def cleared: PlanAidState = PlanAidState()

/** A requirement an Imperial ally may meet for a plan's user (ruling R12):
  * holding the Darkest Secret, holding the People's Favor (checked by
  * Hearts and Minds at the end), or having a pawn at a site. */
enum PlanNeed:
  case DarkestSecret
  case PeoplesFavor
  case PawnAt(site: SiteId)
```

In `GameStateProtocol.scala`, add the field to `ReadyGame` after `consent`:

```scala
    consent: ConsentState = ConsentState(),
    planAid: PlanAidState = PlanAidState()
```

In `GameEventProtocol.scala`, next to `sealed trait ConsentEvent extends OathEvent`:

```scala
/** The Imperial Tactics facts (Empire phase design, "1b-ii"). Like consent
  * facts, they open no command batch and join none. */
sealed trait PlanAidEvent extends OathEvent
```

Inside `object OathEvent`, after the consent cases:

```scala
  /** `ally`'s whole offer for `window`, replacing their earlier one. */
  final case class PlanAidSaved(ally: PlayerId, window: AidWindow,
      offer: AidOffer) extends PlanAidEvent
  /** No Imperial plan decision is parked any more, so the offers end. */
  case object PlanAidCleared extends PlanAidEvent
```

`src/main/scala/distantlands/gameplay/aid/PlanAidEvolution.scala`:

```scala
package oathdigital.gameplay.aid

import oathdigital.model._
import oathdigital.model.OathEvent.{PlanAidCleared, PlanAidSaved}

/** Evolves the Imperial Tactics facts into `ReadyGame.planAid`. The desk
  * validated each offer when it was saved, so replay only records it. */
object PlanAidEvolution:
  def evolve(ready: ReadyGame, event: PlanAidEvent)
      : Either[OathViolation, ReadyGame] = event match
    case PlanAidSaved(ally, window, offer) =>
      Right(ready.copy(planAid = ready.planAid.saved(ally, window, offer)))
    case PlanAidCleared => Right(ready.copy(planAid = ready.planAid.cleared))
```

In `OathRules.evolve`, after the `ConsentEvent` arm:

```scala
      case event: PlanAidEvent => state match
        case Ready(ready) => PlanAidEvolution.evolve(ready, event)
          .map(Ready.apply)
        case _ => Left(GameNotStarted)
```

Then import `oathdigital.gameplay.aid.PlanAidEvolution`.

- [ ] **Step 4: Run the state test**

Run: `./sbtw "testOnly oathdigital.model.PlanAidStateSuite"`
Expected: PASS.

- [ ] **Step 5: Write the failing wire and journal tests**

`PlanAidEventWireSuite`: copy the `roundTrip` helper from `ConsentEventWireSuite.scala:18`, which is `encodeEvent("g", 7L, e)` followed by `decode`. Then:

```scala
  private val window = AidWindow(PlayerId("p1"), CampaignPlanSide.Defender,
    PlayerId("p3"))
  private val offer = AidOffer(
    plans = Vector(DecisionOptionRef.Denizen(DenizenId("108")),
      DecisionOptionRef.Button("title")),
    requirements = Vector(DecisionOptionRef.Banner(Banner.DarkestSecret),
      DecisionOptionRef.Relic(RelicId("R40")),
      DecisionOptionRef.Player(PlayerId("p2"))),
    amounts = AidAmounts(favor = 2, secrets = 1, favorBurnt = 0,
      secretsBurnt = 3))

  test("every aid fact round-trips"):
    Vector(OathEvent.PlanAidSaved(PlayerId("p2"), window, offer),
      OathEvent.PlanAidSaved(PlayerId("p2"), window, AidOffer()),
      OathEvent.PlanAidCleared).foreach(event =>
      assertEquals(roundTrip(event), event))

  test("aid facts carry their own types"):
    assertEquals(GameEventWire.encodeEvent("g", 7L,
      OathEvent.PlanAidCleared).eventType, "plan-aid.cleared")
```

If the record's field is not named `eventType`, use the name `ConsentEventWireSuite.scala:35` uses.

In the `JournalFold.carried` suite, add a test. Build a target `Ready` state with `planAid = PlanAidState()` and a current one with an offer saved. Assert that `JournalFold.carried(target, current)` keeps the current `planAid`.

- [ ] **Step 6: Run them and see them fail**

Run: `./sbtw "testOnly oathdigital.serialization.PlanAidEventWireSuite"`
Expected: FAIL, because an unknown event cannot be encoded.

- [ ] **Step 7: Write the codec and the journal arms**

`src/main/scala/distantlands/serialization/PlanAidEventCodec.scala`. It follows `ConsentEventCodec.scala`: it is the same kind of trait over `GameEventJsonSupport`, and it uses `DecisionAnswerCodec.encodeRef` and the ref decoder that `consentDecode` uses for `target`.

```scala
package oathdigital.serialization

import oathdigital.model._
import oathdigital.model.OathEvent.{PlanAidCleared, PlanAidSaved}

/** The Imperial Tactics facts (Empire phase design, "1b-ii"). */
private[serialization] trait PlanAidEventCodec { this: GameEventJsonSupport =>
  import GameEventWire._

  protected final val planAidDiscriminator
      : PartialFunction[OathEvent, String] =
      case _: PlanAidSaved => PlanAidSavedType
      case PlanAidCleared => PlanAidClearedType

  protected final val planAidEncoder: PartialFunction[OathEvent, ujson.Value] =
      case PlanAidSaved(ally, window, offer) => ujson.Obj(
        "ally" -> ally.value,
        "actor" -> window.actor.value,
        "side" -> sideKey(window.side),
        "user" -> window.user.value,
        "plans" -> ujson.Arr.from(offer.plans.map(DecisionAnswerCodec.encodeRef)),
        "requirements" -> ujson.Arr.from(
          offer.requirements.map(DecisionAnswerCodec.encodeRef)),
        "favor" -> offer.amounts.favor,
        "secrets" -> offer.amounts.secrets,
        "favorBurnt" -> offer.amounts.favorBurnt,
        "secretsBurnt" -> offer.amounts.secretsBurnt)
      case PlanAidCleared => ujson.Obj()

  private def sideKey(side: CampaignPlanSide): String = side match
    case CampaignPlanSide.Attacker => "attacker"
    case CampaignPlanSide.Defender => "defender"
```

`planAidDecode(eventType, payload, path): Option[Either[WireError, OathEvent]]` is a `PartialFunction[String, …]` returned with `.lift`, as `consentDecode` is. It reads the fields above:
- `side` decodes `"attacker"` or `"defender"`; any other value is a `WireError` at `$path.side`.
- Each ref is decoded with the same function `consentDecode` uses for `"target"`.
- An undecodable ref is a `WireError` at `$path.plans[i]` or `$path.requirements[i]`.

Every field is required. Old journals need not decode.

In `GameEventWire.scala`:
- Add `with PlanAidEventCodec` to the mix-in.
- Add the type constants `PlanAidSavedType = "plan-aid.saved"` and `PlanAidClearedType = "plan-aid.cleared"`.
- Chain `.orElse(planAidDiscriminator)` and `.orElse(planAidEncoder)`.
- Chain `.orElse(planAidDecode(eventType, payload, path))`.

In `JournalFold.scala:99`, widen the consent arm and its comment so that an aid fact also closes the open batch and joins none:

```scala
        // A consent or aid fact opens no batch and joins none (consent
        // design, "Events"; Empire phase design, "1b-ii"): it closes the
        // open batch, as a batch opening does, so a whole-journal replay
        // batches exactly as live appends did.
        case event @ (_: ConsentEvent | _: PlanAidEvent) =>
```

In `JournalFold.carried`, keep the standing offers across a rewind:

```scala
        OathState.Ready(game.copy(consent = now.consent.carried,
          planAid = now.planAid))
```

Then update its doc comment: offers, like consent grants, outlive the rewind.

- [ ] **Step 8: Run the tests**

Run: `./sbtw "testOnly oathdigital.serialization.PlanAidEventWireSuite oathdigital.model.PlanAidStateSuite oathdigital.application.JournalFold*"`
Expected: PASS.

- [ ] **Step 9: Commit**

```bash
git add src/main/scala/distantlands/model/PlanAid.scala \
  src/main/scala/distantlands/model/GameEventProtocol.scala \
  src/main/scala/distantlands/model/GameStateProtocol.scala \
  src/main/scala/distantlands/gameplay/aid/PlanAidEvolution.scala \
  src/main/scala/distantlands/gameplay/OathRules.scala \
  src/main/scala/distantlands/application/JournalFold.scala \
  src/main/scala/distantlands/serialization/PlanAidEventCodec.scala \
  src/main/scala/distantlands/serialization/GameEventWire.scala \
  src/test/scala/distantlands/model/PlanAidStateSuite.scala \
  src/test/scala/distantlands/serialization/PlanAidEventWireSuite.scala
git add <the JournalFold suite you changed>
git commit -m "feat(empire): record Imperial Tactics offers as standing facts

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 2: The aid read model

**Files:**
- Create: `src/main/scala/distantlands/gameplay/actions/campaign/PlanAid.scala`
- Test: `src/test/scala/distantlands/gameplay/actions/campaign/PlanAidSuite.scala` (new)
- Modify: `src/test/scala/distantlands/testkit/Table.scala`. Add a step `aid(ally, window, offer)` that sets `planAid` directly, as every `Table` step edits state.

**Interfaces:**
- Consumes: Task 1's model.
- Produces (object `oathdigital.gameplay.actions.campaign.PlanAid`):
  - `darkestSecret`, `yewStaff` and `peoplesFavor: DecisionOptionRef`
  - `window(ready, actor, side, user): Option[AidWindow]`, which is `Some` only for an Imperial user
  - `allies(ready, user): Vector[PlayerId]`: the other Imperial players, in turn order starting after the user
  - `offers(ready, window): Vector[(PlayerId, AidOffer)]`, in ally order
  - `meets(ready, ally, requirement: DecisionOptionRef): Boolean`
  - `requirementsOf(ready, ally): Vector[DecisionOptionRef]`: what the ally could offer as requirements now
  - `used(ready, side, ally): AidAmounts` and `usedRefs(ready, side, ally): Vector[DecisionOptionRef]`
  - `remaining(ready, window, ally): AidAmounts`: what the ally offered less what has been used
  - `payments(ready, window: Option[AidWindow], user, cost: Cost): Vector[(PlayerId, Cost)]`: the allies' parts first, then the user's
  - `spent(window, ally, part: Cost): Vector[CoreOperation]` and `lentUse(window, ally, ref): CoreOperation`: the markers
  - `spare(ready, window): AidAmounts`: what the allies hold beyond what they have offered and not yet used
  - `couldCover(ready, window, violation: OathViolation): Boolean`
  - `meetsAlone(ready, user, need: PlanNeed): Boolean`: whether the user meets `need` without aid
  - `lentNeed(ready, window, need, full: Boolean): Option[(PlayerId, DecisionOptionRef)]`: the first ally, in ally order, who meets `need` and lends the ref that meets it. With `full`, lending everything is assumed.

- [ ] **Step 1: Write the failing tests**

`PlanAidSuite` builds its state with `Table.start.imperial(p2, p1, p3)`, so p2 is the Chancellor and p1 and p3 are Citizens. It gives p2 and p1 favor on their boards with the existing `Table` favor step; use the one `ImperialCampaignBattleSuite` uses, or `board(favor = …)` if that is how the testkit spells it. Then it saves offers with the new `aid` step. In the snippets below, `ready` is that state as a `ReadyGame`, and `withPool(ready, key, n)` sets `rollPools(key)` with `DicePoolState(n)`.

```scala
  private val defending = AidWindow(p4, CampaignPlanSide.Defender, p3)

  test("allies are the other Imperial players, in turn order after the user"):
    // Seats p1..p4; turn order starts with the Chancellor: p2, p3, p4, p1.
    // After p3 come p4 (an Exile, skipped), p1, then p2.
    assertEquals(PlanAid.allies(ready, p3), Vector(p1, p2))

  test("an Exile user gets no window"):
    assertEquals(PlanAid.window(ready, p4, CampaignPlanSide.Attacker, p4),
      None)

  test("allies pay first, in ally order, and the user pays the rest"):
    // p2 offers 1 favor, p1 offers 1 favor; a 3 favor cost.
    assertEquals(PlanAid.payments(ready, Some(defending), p3,
      Cost(favor = 3)), Vector(first -> Cost(favor = 1),
        second -> Cost(favor = 1), p3 -> Cost(favor = 1)))

  test("spent aid is not offered again"):
    val after = withPool(ready, PlanAid.amountMarker(defending, p2,
      "favor"), 1)
    assertEquals(PlanAid.remaining(after, defending, p2), AidAmounts.none)

  test("a cost the allies cover leaves the user nothing to pay"):
    assertEquals(PlanAid.payments(ready, Some(defending), p3,
      Cost(favor = 1)), Vector(first -> Cost(favor = 1)))

  test("allies could cover a shortfall within what they hold unoffered"):
    assert(PlanAid.couldCover(ready, defending,
      OathViolation.InsufficientFavor(2, 0)))
    assert(!PlanAid.couldCover(ready, defending,
      OathViolation.InsufficientFavor(99, 0)))
```

If the table seats players in another order, recompute the literal from `TurnOrder.order`. `first` and `second` are `PlanAid.allies(ready, p3)(0)` and `(1)`. Add one test for `meets` on each of the four requirement refs, using `Table` steps such as `darkestSecret`, `peoplesFavor`, `relic` and the pawn placement step. Add one test that `usedRefs` reads back a `lentUse` marker.

- [ ] **Step 2: Run them and see them fail to compile**

Run: `./sbtw "testOnly oathdigital.gameplay.actions.campaign.PlanAidSuite"`
Expected: compile errors, because `PlanAid` is not found.

- [ ] **Step 3: Write the read model**

```scala
package oathdigital.gameplay.actions.campaign

import oathdigital.gameplay.TurnOrder
import oathdigital.gameplay.actions.BannerRules
import oathdigital.model._

/** What Imperial Tactics lends a plan's user (CR p. 35; Empire phase
  * design, "1b-ii"; rulings R10 to R12 and R16).
  *
  * Offers stand in `ReadyGame.planAid`. What the window's plans have used
  * of them is recorded by their applications as Campaign pool markers,
  * as a bandit's applied plan is (`CampaignPlans.appliedMarker`): the pools
  * are walker scratch, so undoing the user's pick undoes the use, and the
  * walk's end clears it. The pools are never rolled or projected.
  */
object PlanAid:
  val darkestSecret: DecisionOptionRef =
    DecisionOptionRef.Banner(Banner.DarkestSecret)
  /** "When using card powers, you may act as if you hold the Darkest
    * Secret." Its holder meets the Darkest Secret for a plan. */
  val yewStaff: DecisionOptionRef = DecisionOptionRef.Relic(RelicId("R40"))
  val peoplesFavor: DecisionOptionRef =
    DecisionOptionRef.Banner(Banner.PeoplesFavor)

  /** The window `user`'s plans on `side` are chosen in, when aid can reach
    * it: only an Imperial user is aided. */
  def window(ready: ReadyGame, actor: PlayerId, side: CampaignPlanSide,
      user: PlayerId): Option[AidWindow] =
    Option.when(Imperials.isImperial(ready, user))(
      AidWindow(actor, side, user))

  /** The other Imperial players, in turn order from the one after `user`. */
  def allies(ready: ReadyGame, user: PlayerId): Vector[PlayerId] =
    val order = TurnOrder.order(ready)
    val at = order.indexOf(user)
    (order.drop(at + 1) ++ order.take(math.max(at, 0)))
      .filter(player => player != user && Imperials.isImperial(ready, player))

  /** The saved offers that reach `window`, in ally order. */
  def offers(ready: ReadyGame, window: AidWindow)
      : Vector[(PlayerId, AidOffer)] =
    if !ready.planAid.window.contains(window) then Vector.empty
    else allies(ready, window.user).flatMap(ally =>
      ready.planAid.offers.get(ally).map(ally -> _))

  private def player(ready: ReadyGame, id: PlayerId): Option[PlayerState] =
    ready.game.current.players.find(_.player == id)

  /** Whether `ally` meets `requirement` now. */
  def meets(ready: ReadyGame, ally: PlayerId,
      requirement: DecisionOptionRef): Boolean = requirement match
    case DecisionOptionRef.Banner(banner) =>
      BannerRules.holder(ready.game.current, banner).contains(ally)
    case ref @ DecisionOptionRef.Relic(id) if ref == yewStaff =>
      player(ready, ally).exists(_.relics.exists(held =>
        held.id == id && held.orientation == Orientation.FaceUp))
    case DecisionOptionRef.Player(id) if id == ally =>
      player(ready, ally).exists(_.pawnSite.nonEmpty)
    case _ => false

  /** What `ally` could offer as requirements now. */
  def requirementsOf(ready: ReadyGame, ally: PlayerId)
      : Vector[DecisionOptionRef] =
    Vector(darkestSecret, yewStaff, peoplesFavor,
      DecisionOptionRef.Player(ally)).filter(meets(ready, ally, _))

  private def sideKey(side: CampaignPlanSide): String = side match
    case CampaignPlanSide.Attacker => "attacker"
    case CampaignPlanSide.Defender => "defender"

  private def prefix(window: AidWindow, ally: PlayerId): String =
    s"campaign.aid.${sideKey(window.side)}.${ally.value}."

  /** The pool counting what `ally`'s `part` paid in `window`: `favor`,
    * `secrets`, `favor-burnt` or `secrets-burnt`. */
  def amountMarker(window: AidWindow, ally: PlayerId, part: String): PoolKey =
    PoolKey(prefix(window, ally) + part)

  private def usedMarker(window: AidWindow, ally: PlayerId,
      ref: DecisionOptionRef): PoolKey =
    PoolKey(prefix(window, ally) + s"used.${ref.kind}.${ref.wireId}")

  private def count(ready: ReadyGame, pool: PoolKey): Int =
    ready.game.current.rollPools.get(pool).fold(0)(_.count)

  def used(ready: ReadyGame, window: AidWindow, ally: PlayerId): AidAmounts =
    AidAmounts(count(ready, amountMarker(window, ally, "favor")),
      count(ready, amountMarker(window, ally, "secrets")),
      count(ready, amountMarker(window, ally, "favor-burnt")),
      count(ready, amountMarker(window, ally, "secrets-burnt")))

  /** The plans and requirements of `ally`'s that `window`'s plans used. */
  def usedRefs(ready: ReadyGame, window: AidWindow, ally: PlayerId)
      : Vector[DecisionOptionRef] =
    val start = prefix(window, ally) + "used."
    ready.game.current.rollPools.keys.toVector.map(_.value)
      .filter(_.startsWith(start)).sorted.flatMap(key =>
        key.stripPrefix(start).split("\\.", 2) match
          case Array(kind, wireId) => DecisionOptionRef.fromWire(kind, wireId)
          case _ => None)

  def remaining(ready: ReadyGame, window: AidWindow, ally: PlayerId)
      : AidAmounts =
    if !ready.planAid.window.contains(window) then AidAmounts.none
    else ready.planAid.offerOf(ally).amounts - used(ready, window, ally)

  /** Who pays `cost`, for `user` in `window`: each ally in order pays what
    * their offer has left of each part, and the user pays the rest. Parts
    * of nothing are left out. */
  def payments(ready: ReadyGame, window: Option[AidWindow], user: PlayerId,
      cost: Cost): Vector[(PlayerId, Cost)] =
    val owed = AidAmounts.of(cost)
    val (lent, rest) = window.fold((Vector.empty[(PlayerId, AidAmounts)],
      owed))(at => offers(ready, at).foldLeft(
      (Vector.empty[(PlayerId, AidAmounts)], owed)) {
        case ((paid, left), (ally, _)) =>
          val part = take(remaining(ready, at, ally), left)
          (if part.isEmpty then paid else paid :+ (ally -> part), left - part)
      })
    (lent ++ Option.unless(rest.isEmpty)(user -> rest))
      .map((payer, part) => payer -> part.cost)

  private def take(offered: AidAmounts, owed: AidAmounts): AidAmounts =
    AidAmounts(math.min(offered.favor, owed.favor),
      math.min(offered.secrets, owed.secrets),
      math.min(offered.favorBurnt, owed.favorBurnt),
      math.min(offered.secretsBurnt, owed.secretsBurnt))

  /** The markers recording what `ally` paid of `part` in `window`. */
  def spent(window: AidWindow, ally: PlayerId, part: Cost)
      : Vector[CoreOperation] =
    Vector("favor" -> part.favor, "secrets" -> part.secret,
      "favor-burnt" -> part.favorBurnt, "secrets-burnt" -> part.secretBurnt)
      .collect { case (name, amount) if amount > 0 =>
        ModifyDicePool(amountMarker(window, ally, name), amount) }

  /** The marker recording that `window`'s plans used `ally`'s `ref`. */
  def lentUse(window: AidWindow, ally: PlayerId, ref: DecisionOptionRef)
      : CoreOperation = ModifyDicePool(usedMarker(window, ally, ref), 1)

  /** What the allies hold beyond what they have offered and not yet used:
    * favor and secrets either way, so to pay or to burn. */
  def spare(ready: ReadyGame, window: AidWindow): AidAmounts =
    allies(ready, window.user).flatMap(ally => player(ready, ally).map {
      held =>
        val left = remaining(ready, window, ally)
        AidAmounts(favor = math.max(0,
          held.board.favor - left.favor - left.favorBurnt),
          secrets = math.max(0,
            held.board.faceUpSecrets - left.secrets - left.secretsBurnt))
    }).foldLeft(AidAmounts.none)(_ + _)

  /** Whether the allies could still cover what a dry run found short. */
  def couldCover(ready: ReadyGame, window: AidWindow,
      violation: OathViolation): Boolean = violation match
    case OathViolation.InsufficientFavor(required, available) =>
      required - available <= spare(ready, window).favor
    case OathViolation.InsufficientSecrets(required, available) =>
      required - available <= spare(ready, window).secrets
    case _ => false

  /** Whether `user` meets `need` with no aid: the Darkest Secret by the
    * banner or a faceup Yew Staff (as `CardPowerBanners` sees it; actions
    * cannot import the powers), the People's Favor by the banner, a pawn
    * by the Campaign's origin. */
  def meetsAlone(ready: ReadyGame, user: PlayerId, need: PlanNeed): Boolean =
    need match
      case PlanNeed.DarkestSecret => meets(ready, user, darkestSecret) ||
        meets(ready, user, yewStaff)
      case PlanNeed.PeoplesFavor => meets(ready, user, peoplesFavor)
      case PlanNeed.PawnAt(site) =>
        CampaignSetup.originOf(ready, user).contains(site)

  /** The first ally who meets `need` and lends what meets it, with that
    * ref; `full` supposes every ally lends everything. */
  def lentNeed(ready: ReadyGame, window: AidWindow, need: PlanNeed,
      full: Boolean): Option[(PlayerId, DecisionOptionRef)] =
    val lent = offers(ready, window).toMap
    allies(ready, window.user).iterator.flatMap { ally =>
      val refs = need match
        case PlanNeed.DarkestSecret => Vector(darkestSecret, yewStaff)
        case PlanNeed.PeoplesFavor => Vector(peoplesFavor)
        case PlanNeed.PawnAt(site) => Vector(DecisionOptionRef.Player(ally))
          .filter(_ => player(ready, ally).exists(_.pawnSite.contains(site)))
      refs.find(ref => PlanAid.meets(ready, ally, ref) && (full ||
        lent.get(ally).exists(_.requirements.contains(ref)))).map(ally -> _)
    }.nextOption()
```

If `CardPowerBanners` knows Darkest Secret overrides other than the Yew Staff
(`grep -rn "DarkestSecretOverride" src/main`), list each in `meetsAlone` and
`lentNeed` the same way, and add a test.

Use the real names where they differ, and keep the behaviour:
- `PlayerState.relics` elements may be `RelicState` (`card.id`, `orientation`), as in `PlanContext.relic`.
- The pawn's site may be `pawnSite`, as `CampaignSetup.playerOf(...).flatMap(_.pawnSite)` reads it.
- A pool's count may be `DicePoolState.count`, or another field.
- `ModifyDicePool`'s third argument has a default, as `CampaignPlanApplication.marker` shows.

Add the `aid` step to `Table`:

```scala
  /** `ally`'s saved Imperial Tactics offer for `window`. */
  def aid(ally: PlayerId, window: AidWindow, offer: AidOffer): Table =
    edit(ready => ready.copy(planAid = ready.planAid.saved(ally, window,
      offer)))
```

Use whatever name the other `Table` steps edit state through, in place of `edit`.

Add one test each for `meetsAlone` and `lentNeed`: an unlent Darkest Secret is not met, a lent one is, and `full` meets it unlent.

- [ ] **Step 4: Run the tests**

Run: `./sbtw "testOnly oathdigital.gameplay.actions.campaign.PlanAidSuite"`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add src/main/scala/distantlands/gameplay/actions/campaign/PlanAid.scala \
  src/test/scala/distantlands/gameplay/actions/campaign/PlanAidSuite.scala \
  src/test/scala/distantlands/testkit/Table.scala
git commit -m "feat(empire): read what Imperial allies offer and have spent

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 3: Lent plans and requirements in the plan window

**Files:**
- Modify: `src/main/scala/distantlands/model/CampaignTypes.scala:148-155` (`CampaignPlanOffer`)
- Modify: `src/main/scala/distantlands/model/Decisions.scala:295` (beside `Badged`)
- Modify: `src/main/scala/distantlands/gameplay/powers/campaign/PlanContext.scala`
- Modify: `src/main/scala/distantlands/gameplay/powers/campaign/BattlePlan.scala` (`contributions`)
- Modify: `src/main/scala/distantlands/gameplay/powers/campaign/TitleDefensePlan.scala`
- Modify: `src/main/scala/distantlands/gameplay/powers/campaign/ToweringRampart.scala`
- Modify: these plans, so that each declares `requires`:
  - `FireTalkers.scala`, `FaeBattalion.scala`, `RustingRay.scala` and `UnstableSummon.scala` (the Darkest Secret)
  - `ToweringRampart.scala` (a pawn)
- Create: `src/main/scala/distantlands/gameplay/powers/campaign/ImperialTactics.scala`
- Modify: `src/main/scala/distantlands/gameplay/powers/campaign/PlanRules.scala` (register `ImperialTactics`)
- Modify: `src/main/scala/distantlands/gameplay/actions/campaign/CampaignPlans.scala` (`optionOf`)
- Modify: `src/main/scala/distantlands/gameplay/actions/campaign/CampaignPlanChoice.scala` (`userPass`)
- Modify: every exhaustive match on `DecisionOption` that has a `Badged` arm. Today these are `application/WalkerDecisionProjector.scala:354`, `application/gamelog/ChoiceWords.scala` and `gameplay/walker/PricedOptions.scala`; check with `grep -rn "Badged" src/main`.
- Modify: `src/test/resources/notes/declared-keys.txt`
- Test: `src/test/scala/distantlands/gameplay/powers/campaign/ImperialTacticsSuite.scala` (new)

**Interfaces:**
- Consumes: `PlanAid` (Task 2) and the `Table.aid` step.
- Produces:
  - `CampaignPlanOffer.requires: Vector[PlanNeed] = Vector.empty`, `lender: Option[PlayerId] = None` and `awaitsAid: Boolean = false`
  - `DecisionOption.Lent(option: DecisionOption, owner: PlayerId)`
  - `PlanContext.aidWindow: Option[AidWindow]`, `PlanContext.fullAid: PlanContext` and `PlanContext.lentBy(source): Option[PlayerId]`
  - `DecisionOption.lender(option): Option[PlayerId]`, which unwraps the wrappers to find a `Lent`
  - `ImperialTactics.awaitsPlan(owner)` and `ImperialTactics.awaitsNeed`, as `OptionReason`

- [ ] **Step 1: Write the failing tests**

`ImperialTacticsSuite` drives the plan window with `PlanDriver`, using the setup style of `ImperialCampaignBattleSuite.scala:24-38`. It works from `on(CampaignFixture.board(...))(_.imperial(chancellor, citizens*))`, the new `aid` step, `commit(rules(winningDice), b, 4)`, and `Run.options/offered/withheld/pick`. The cast:
- p1, an Exile, raids p3, a Citizen, so the defender's window is p3's.
- p2 is the Chancellor.
- p2 holds Shield Wall (`ShieldWallCard.id`) faceup as an adviser.

Each test writes its setup in full; the helper names below say what they build.

```scala
  test("an ally's faceup plan is shown awaiting their offer"):
    val run = citizenDefendsWithChancellorHoldingShieldWall()
    assertEquals(run.withheld(p1, ShieldWallRef),
      Some(ImperialTactics.awaitsPlan(p2)))

  test("an offered plan is choosable, lent by its owner"):
    val run = citizenDefendsWithChancellorOffering(
      AidOffer(plans = Vector(ShieldWallRef),
        amounts = AidAmounts(favor = 1)))
    assert(run.offered(p1).contains(ShieldWallRef))
    assert(run.options(p1).exists(option =>
      DecisionOption.lender(option).contains(p2)))

  test("an Exile defender sees no ally's plan"):
    val run = exileDefendsWhileChancellorHoldsShieldWall()
    assert(!run.options(p1).exists(_.ref == ShieldWallRef))

  test("a Darkest Secret plan of the user's is usable with an ally's banner"):
    // p3 holds Fire Talkers faceup; p2 holds the Darkest Secret and offers it.
    val run = citizenWithFireTalkersAndChancellorOffering(
      AidOffer(requirements = Vector(PlanAid.darkestSecret)))
    assert(run.offered(p1).contains(FireTalkersRef))

  test("the Yew Staff meets the Darkest Secret too"):
    val run = citizenWithFireTalkersAndChancellorOffering(
      AidOffer(requirements = Vector(PlanAid.yewStaff)),
      chancellorHoldsYewStaff = true)
    assert(run.offered(p1).contains(FireTalkersRef))

  test("an ally's pawn meets Towering Rampart's pawn"):
    // Towering Rampart stands at an Imperial site, not targeted (a Raid);
    // p2's pawn is there and p2 offers the plan and the pawn.
    val run = citizenDefendsRaidWithRampartAndChancellorPawnOffering(
      AidOffer(plans = Vector(RampartRef),
        requirements = Vector(DecisionOptionRef.Player(p2))))
    assert(run.offered(p1).contains(RampartRef))

  test("Great Crusade counts only the user's own nomads (R16)"):
    // p3 holds Great Crusade; p2 rules two other nomad cards and offers
    // everything; the plan's label still counts only p3's cards.
    val run = citizenWithGreatCrusadeAndChancellorRulingNomads()
    assertEquals(run.options(p1).collectFirst {
      case o if o.ref == GreatCrusadeRef => planLabel(o) },
      Some(PlanDice.label("Great Crusade", CampaignPlanSide.Defender, 1)))

  test("the Oathkeeper holder may lend the title's defense"):
    val run = citizenDefendsWithChancellorOathkeeperOffering(
      AidOffer(plans = Vector(DecisionOptionRef.Button("title"))))
    assert(run.offered(p1).contains(DecisionOptionRef.Button("title")))

  test("a plan the attacker used is unavailable to the defender"):
    // p1 (Citizen) raids p3 (Citizen); p2 lends Encirclement to p1 first,
    // then offers it to p3.
    val run = citizensRaidWithChancellorLendingEncirclementToBoth()
    assertEquals(run.withheld(p1, EncirclementRef),
      Some(OptionReason.Written("The attacker used this plan")))
```

`planLabel` unwraps the option to its label. Write it with `DecisionOption.lender`'s unwrapping style, or read the label from the offer.

- [ ] **Step 2: Run them and see them fail to compile**

Run: `./sbtw "testOnly oathdigital.gameplay.powers.campaign.ImperialTacticsSuite"`
Expected: compile errors, because `ImperialTactics`, `DecisionOption.Lent` and `PlanNeed` are not found.

- [ ] **Step 3: Extend the model**

`CampaignPlanOffer` (in `CampaignTypes.scala`) gains three defaulted fields after `warning`:

```scala
    amount: Option[PlanAmount] = None, warning: Option[OptionReason] = None,
    requires: Vector[PlanNeed] = Vector.empty,
    lender: Option[PlayerId] = None, awaitsAid: Boolean = false)
```

Document them in the class's comment:
- `requires` names what an ally may meet for the plan.
- `lender` is the ally whose source it is.
- `awaitsAid` marks an offer that stands only once an ally offers more.

In `Decisions.scala`, after `Badged`:

```scala
  /** A battle plan an Imperial ally lends the decision's owner (Imperial
    * Tactics): its source is `owner`'s. */
  final case class Lent(option: DecisionOption, owner: PlayerId)
      extends DecisionOption
```

Give it `ref` and `available` the way `Badged` has them, delegating to `option`. In `object DecisionOption`, add:

```scala
  /** The ally who lends `option`, through any wrappers. */
  def lender(option: DecisionOption): Option[PlayerId] = option match
    case Lent(_, owner) => Some(owner)
    case Badged(inner, _) => lender(inner)
    case Priced(inner, _) => lender(inner)
    case Unavailable(inner, _) => lender(inner)
    case Warned(inner, _) => lender(inner)
    case _ => None
```

Compile with `./sbtw compile`. In every match that now fails as non-exhaustive, add a `Lent` arm next to the `Badged` arm that treats it the same way, unwrapping to the inner option. In `WalkerDecisionProjector.optionProjection`, add:

```scala
      case DecisionOption.Lent(inner, owner) => optionProjection(ready,
        viewer, index, inner,
        details :+ s"${presentation.playerLabel(owner)}'s plan")
```

- [ ] **Step 4: Make `PlanContext` lend**

Add a parameter and these members to `PlanContext`. Every existing source and requirement check keeps its own branch first, and the lent branch comes after it.

```scala
final case class PlanContext(ready: ReadyGame, setup: CampaignSetup,
    side: CampaignPlanSide, full: Boolean = false):

  /** The window aid reaches, when the user is Imperial. */
  def aidWindow: Option[AidWindow] =
    user.flatMap(PlanAid.window(ready, setup.actor, side, _))

  /** The same context as if every ally offered every plan and requirement
    * they could: what the window shows as awaiting an offer. */
  def fullAid: PlanContext = copy(full = true)

  /** The allies and what each lends: their saved offers, or, in the full
    * view, everything. `lends(ally, ref)` asks one. */
  private def lends(ally: PlayerId, ref: DecisionOptionRef): Boolean =
    aidWindow.exists(window =>
      if full then PlanAid.allies(ready, window.user).contains(ally)
      else PlanAid.offers(ready, window).exists((lender, offer) =>
        lender == ally && (offer.plans.contains(ref) ||
          offer.requirements.contains(ref))))

  private def lenders: Vector[PlayerId] = aidWindow.fold(
    Vector.empty[PlayerId])(window => PlanAid.allies(ready, window.user))

  /** The ally whose source `source` is, when it is not the user's. */
  def lentBy(source: CampaignPlanSource): Option[PlayerId] =
    val owner = source match
      case CampaignPlanSource.Adviser(player, _) => Some(player)
      case CampaignPlanSource.Relic(player, _) => Some(player)
      case CampaignPlanSource.Title(player) => Some(player)
      case CampaignPlanSource.SiteCard(site, _) => rulerOf(site)
      case CampaignPlanSource.SiteEdifice(site, _) => rulerOf(site)
    owner.filter(owner => !user.contains(owner))

  private def rulerOf(site: SiteId): Option[PlayerId] =
    CampaignSetup.defenderAt(ready, site).collect {
      case CampaignDefender.Player(player) => player }

  /** An ally's requirement that meets `need`, as the ref they offered. */
  private def lentNeed(need: PlanNeed): Option[(PlayerId, DecisionOptionRef)] =
    aidWindow.flatMap(PlanAid.lentNeed(ready, _, need, full))
```

Then extend the members:
- **`denizen(id)`:** after the user's adviser and the user's site card, try:
  - each lender's faceup adviser `id` that `lends(ally, DecisionOptionRef.Denizen(id))`, giving `CampaignPlanSource.Adviser(ally, id)`;
  - then a faceup `id` at a site whose `rulerOf` is a lender who lends it, giving `CampaignPlanSource.SiteCard(site, id)`.

  A facedown card is never lent.
- **`relic(id)`:** after the user's faceup relic, a lender's faceup relic that they lend, giving `CampaignPlanSource.Relic(ally, id)`.
- **`edifice(id, face)`:** after the user's sites, an edifice at a site whose `rulerOf` is a lender who lends `DecisionOptionRef.Edifice(id)`.
- **`holdsDarkestSecret`:** the user's own check (keep `CardPowerBanners`) `|| lentNeed(PlanNeed.DarkestSecret).nonEmpty`.
- **`pawnAt(site)`:** the user's own check `|| lentNeed(PlanNeed.PawnAt(site)).nonEmpty`.
- **New `def lentRequirement(need: PlanNeed): Option[(PlayerId, DecisionOptionRef)] = lentNeed(need)`:** public, for the application in Task 4.

`playerState(id)` is the lookup `held` already does for the user. Generalise it to take a player.

`PlanContext.of(ctx)` keeps `full = false`.

- [ ] **Step 5: Offer plans that await aid**

`ImperialTactics.scala`:

```scala
package oathdigital.gameplay.powers.campaign

import oathdigital.gameplay.powerresolver.{Contribution, ContributingPower}
import oathdigital.model._

/** Imperial Tactics (CR p. 35): an Imperial player may use battle plans an
  * ally lends and meet requirements with an ally's. The rule hooks no
  * window; it holds the hover reasons of a plan that waits on an offer.
  */
case object ImperialTactics extends ContributingPower:
  val id: PowerId = PowerId("rule.imperial-tactics")
  def source: RuleSourceRef = RuleSourceRef.GameRule(id.value)
  def contributions: Map[PowerWindow, Vector[Contribution]] = Map.empty

  private val notes = NoteSet(id)
  /** "{p2} may offer this plan." */
  val awaitsPlanKey: NoteKey = notes.key("awaits-plan", Vector(
    NotePart.Arg(0), NotePart.Text(" may offer this plan.")))
  /** "An Imperial ally may meet what this plan needs." */
  val awaitsNeedKey: NoteKey = notes.key("awaits-need", Vector(
    NotePart.Text("An Imperial ally may meet what this plan needs.")))
  override def noteKeys: Vector[NoteKey] = notes.declared

  def awaitsPlan(owner: PlayerId): OptionReason =
    OptionReason.Noted(id, awaitsPlanKey(NoteArg.Player(owner)))
  val awaitsNeed: OptionReason = OptionReason.Noted(id, awaitsNeedKey())
```

`NoteKey.apply` builds a `LogNote`. Check how `ShieldWall.killed(...)` is applied and copy that form.

Register `ImperialTactics` in `PlanRules.forCatalog`, as `Vector(ImperialTactics)`, and add its keys to `src/test/resources/notes/declared-keys.txt` in the file's order. `DeclaredNoteKeysSuite` names the lines it misses.

In `BattlePlan.contributions`, the offer becomes:

```scala
      side => BattlePlan.windowOf(side) -> Vector[Contribution](Offer(ctx =>
        PlanContext.of(ctx).filter(_.side == side).flatMap(BattlePlan.lent(
          _, plan)).map(_.copy(sides = sides))))
```

In `object BattlePlan`:

```scala
  /** The plan `plan` gives `context`, with its lender; or, when it stands
    * only with more aid, the plan as the full view gives it, unavailable
    * until an ally offers what it needs (Imperial Tactics). */
  def lent(context: PlanContext,
      plan: PlanContext => Option[CampaignPlanOffer])
      : Option[CampaignPlanOffer] =
    val actual = plan(context).map(offer =>
      offer.copy(lender = context.lentBy(offer.source)))
    if context.aidWindow.isEmpty || actual.exists(_.withheld.isEmpty) then
      actual
    else plan(context.fullAid).filter(_.withheld.isEmpty).map { offer =>
      val owner = context.fullAid.lentBy(offer.source)
      offer.copy(lender = owner, awaitsAid = true, withheld = Some(
        owner.fold(ImperialTactics.awaitsNeed)(ImperialTactics.awaitsPlan)))
    }.orElse(actual)
```

`BattlePlan.use` builds a `PowerUse` for the user. It keeps reading `plan(context)` directly, because an awaiting plan is not used.

In `CampaignPlans.optionOf`, wrap a lent plan's inner option before the badge:

```scala
    val owned = offered.offer.lender.fold(inner)(DecisionOption.Lent(inner, _))
    badgeOf(offered.offer.sides).fold(owned)(DecisionOption.Badged(owned, _))
```

- [ ] **Step 6: Lend the title, mark the requirements**

`TitleDefensePlan.plan`: the holder is the user, or an ally who lends `DecisionOptionRef.Button("title")`. Only the defender holds the plan:

```scala
  def plan(context: PlanContext): Option[CampaignPlanOffer] =
    val title = context.ready.game.current.title
    context.user.filter(user =>
      context.setup.defender == CampaignDefender.Player(user)).flatMap { user =>
      title.holder.filter(holder => holder == user ||
        context.lendsTitle(holder)).map { holder =>
        val dice = title.side match
          case TitleSide.Oathkeeper => 1
          case TitleSide.Usurper => 2
        CampaignPlanOffer(CampaignPlanSource.Title(holder),
          s"${title.side} title: add $dice defense ${if dice == 1 then "die" else "dice"}",
          Vector.empty, Vector(CampaignPlanEffect.AddDefenseDice(dice)))
      }
    }
```

Add `def lendsTitle(holder: PlayerId): Boolean = lenders.contains(holder) && lends(holder, DecisionOptionRef.Button("title"))` to `PlanContext`, and update the class comment: the title's holder may lend it (CR p. 35, "the Oathkeeper title is a defensive battle plan").

`CampaignPlanApplication.pay` places a burnt title cost from `PlayArea(player)`. It must place it from the holder, so it reads the payer from the split in Task 4.

Requirements on the plans:
- **Fire Talkers, Fae Battalion, Rusting Ray:** where the offer is built after the `holdsDarkestSecret` filter, add `requires = Vector(PlanNeed.DarkestSecret)`.
- **Unstable Summon:** add it on the branch that has an amount.
- **Towering Rampart:** when the plan stands only by the pawn (`!context.targets(source.siteId)`), add `requires = Vector(PlanNeed.PawnAt(source.siteId))`.

- [ ] **Step 7: The attacker's plan is not the defender's**

In `CampaignPlanChoice.userPass`, for the defender's side, every listed offer whose ref the attacker picked is unavailable:

```scala
    val attackerUsed: Set[DecisionOptionRef] =
      if side != CampaignPlanSide.Defender then Set.empty
      else CampaignAnswers.picks(pending,
        CampaignIds.planDecision(CampaignPlanSide.Attacker)).toSet
```

In the `listed` map, check `attackerUsed(CampaignPlans.refOf(offered.offer.source))` before `withheld`. When it holds, the option is `unavailable(offered, OptionReason.Written(CampaignPlanChoice.attackerUsed))`. Add this to `object CampaignPlanChoice`:

```scala
  /** Why the defender cannot use a plan the attacker used (CR p. 35). */
  val attackerUsed: String = "The attacker used this plan"
```

- [ ] **Step 8: Run the tests**

Run: `./sbtw "testOnly oathdigital.gameplay.powers.campaign.* oathdigital.gameplay.actions.campaign.* oathdigital.notes.DeclaredNoteKeysSuite"`

Use `DeclaredNoteKeysSuite`'s real package.

Expected: PASS, and no existing plan suite changes. With no Imperial user, `aidWindow` is `None`, so `lent` returns the plan as before.

- [ ] **Step 9: Commit**

```bash
git add <every file listed under Files for this task>
git commit -m "feat(empire): lend Imperial allies' plans and requirements

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 4: Allies pay first

**Files:**
- Modify: `src/main/scala/distantlands/gameplay/actions/campaign/CampaignPlanApplication.scala`. It changes in five places:
  - `pay`
  - `payAmount`
  - the first and second `Branch`es
  - a new requirement-and-plan marker slot
  - its class comment
- Modify: `src/main/scala/distantlands/gameplay/powers/campaign/PlanSurcharge.scala` (`surcharge`, the card case)
- Modify: `src/main/scala/distantlands/gameplay/powers/campaign/PeaceEnvoy.scala:187` (the favor `Give`)
- Modify: `src/main/scala/distantlands/gameplay/powers/campaign/ArcaneArmor.scala` (`plan` and `bounds`)
- Modify: `src/main/scala/distantlands/gameplay/powers/campaign/HeartsAndMinds.scala` (`endDiscard`)
- Test: `src/test/scala/distantlands/gameplay/powers/campaign/ImperialTacticsPaymentSuite.scala` (new)

**Interfaces:**
- Consumes:
  - `PlanAid.payments`, `PlanAid.spent`, `PlanAid.lentUse` and `PlanAid.window` (Task 2)
  - `CampaignPlanOffer.requires` and `lender`, and `PlanContext.lentRequirement` (Task 3)
- Produces: a chosen plan's costs paid by the allies first, with markers that `PlanAid.remaining` and `usedRefs` read.

- [ ] **Step 1: Write the failing tests**

All tests use the same table as Task 3: p1 raids p3 and p2 is the Chancellor. Each one picks a plan with `Run.pick(p3, CampaignIds.planDecision(CampaignPlanSide.Defender), ref)`. `favorOf(run, p)` reads the board's favor in `run.transition.state`.

```scala
  test("an ally's offered favor pays a lent plan's cost"):
    val run = citizenDefendsWithChancellorOffering(AidOffer(
      plans = Vector(ShieldWallRef), amounts = AidAmounts(favor = 1)))
      .pick(p3, defenderPlans, ShieldWallRef)
    assertEquals(favorOf(run, p2), chancellorFavor - 1)
    assertEquals(favorOf(run, p3), citizenFavor)

  test("the user pays what the offer does not cover"):
    // Shield Wall lent, no favor offered: p3 pays its favor.
    val run = citizenDefendsWithChancellorOffering(AidOffer(
      plans = Vector(ShieldWallRef))).pick(p3, defenderPlans, ShieldWallRef)
    assertEquals(favorOf(run, p3), citizenFavor - 1)

  test("spent aid is not spent twice"):
    // p2 offers 1 favor; p3 picks Shield Wall (lent) then Extra Provisions
    // (p3's own): the second is paid by p3.
    val run = citizenDefendsWithChancellorOfferingAndOwnExtraProvisions(
      AidOffer(plans = Vector(ShieldWallRef), amounts = AidAmounts(favor = 1)))
      .pick(p3, defenderPlans, ShieldWallRef)
      .pick(p3, defenderPlans, ExtraProvisionsRef)
    assertEquals(favorOf(run, p2), chancellorFavor - 1)
    assertEquals(favorOf(run, p3), citizenFavor - 1)

  test("a used lent plan is recorded against its lender"):
    val run = citizenDefendsWithChancellorOffering(AidOffer(
      plans = Vector(ShieldWallRef), amounts = AidAmounts(favor = 1)))
      .pick(p3, defenderPlans, ShieldWallRef)
    assertEquals(PlanAid.usedRefs(ready(run), defending, p2),
      Vector(ShieldWallRef))

  test("an offered requirement a plan relied on is recorded as used"):
    val run = citizenWithFireTalkersAndChancellorOffering(
      AidOffer(requirements = Vector(PlanAid.darkestSecret)))
      .pick(p3, defenderPlans, FireTalkersRef)
    assertEquals(PlanAid.usedRefs(ready(run), defending, p2),
      Vector(PlanAid.darkestSecret))

  test("Arcane Armor's amount reaches the allies' offered secrets"):
    // p3 holds Arcane Armor and 0 secrets; p2 offers 2 secrets to pay.
    val run = citizenWithArcaneArmorAndChancellorOffering(
      AidOffer(amounts = AidAmounts(secrets = 2)))
    assert(run.offered(p1).contains(ArcaneArmorRef))

  test("Hearts and Minds stays when the lender of the People's Favor holds it"):
    // Hearts and Minds is a site-only card at an Imperial site, so it is
    // the Chancellor's (R10). p2 offers it, 3 favor and the People's Favor.
    // p3 wins as defender; at the end the card is not discarded.
    val run = citizenDefendsWithHeartsAndMindsAndPeoplesFavorLent()
      .pick(p3, defenderPlans, HeartsAndMindsRef).finishPlanWindows
    assert(heartsAndMindsStillAtItsSite(run))
```

Add one more test: a surcharge (Gleaming Armor held by the attacker) on a lent plan is paid from the offered secret before the user's.

- [ ] **Step 2: Run them and see them fail**

Run: `./sbtw "testOnly oathdigital.gameplay.powers.campaign.ImperialTacticsPaymentSuite"`
Expected: FAIL. p3 pays everything, and no markers are written.

- [ ] **Step 3: Split the application's payments**

In `CampaignPlanApplication`:

```scala
  /** The window aid reaches, when the user is Imperial. */
  private def aidWindow(ready: ReadyGame): Option[AidWindow] =
    user.flatMap(PlanAid.window(ready, setup.actor, side, _))

  /** `cost`, paid onto `card` (or, for the title, from the payer's play
    * area) by the allies first and the user last, with the markers of what
    * each ally paid. */
  private def split(ready: ReadyGame, player: PlayerId, cost: Cost,
      card: Option[CardId]): Vector[Operation] =
    val window = aidWindow(ready)
    PlanAid.payments(ready, window, player, cost).flatMap { (payer, part) =>
      val paid = card match
        case Some(id) => payment(Costs.onCard(payer, id, part, catalog,
          intoOccupied = true))
        case None if part.favor + part.secret == 0 =>
          payment(PayCost(payer, Location.PlayArea(payer), part))
        case None => refuse("the title has no card to place a plan's cost on")
      val markers = window.filter(_ => payer != player).toVector
        .flatMap(PlanAid.spent(_, payer, part))
      paid +: markers
    }
```

`pay` becomes `pay(ready)`. It computes `cost` as today and returns `Vector.empty` for a free cost or a bandit. Otherwise it returns `split(ready, player, cost, CampaignPlans.cardOf(source))`. The children change to `Branch((ready, _) => pay(ready))`.

`payAmount` becomes `Branch((ready, pending) => payAmount(ready, pending))`. In its body, the owed cost is paid with `split(ready, player, cost, CampaignPlans.cardOf(source))`. Keep the refusal for a title with an amount.

Add a slot after `chosenEffects` and before `marker`, so that a dry run that stops at a question writes nothing:

```scala
      Branch((ready, _) => lent(ready)),
```

`gameplay/actions` must not import `gameplay.powers`, so the slot reads
requirements through `PlanAid.meetsAlone` and `PlanAid.lentNeed` (Task 2)
and the plan's own `requires`. Hearts and Minds declares
`requires = Vector(PlanNeed.PeoplesFavor)` in its `plan`; note in its doc
that this need is checked at the end, not to choose it.

```scala
  /** The markers of what this plan used of its lenders: the plan itself,
    * when an ally lent it, and each requirement an ally met for it that
    * the user does not meet alone (ruling R12), Hearts and Minds' People's
    * Favor among them, so its end reads who lent it. */
  private def lent(ready: ReadyGame): Vector[Operation] =
    (for
      window <- aidWindow(ready)
      player <- user
    yield offered.offer.lender.map(PlanAid.lentUse(window, _,
        CampaignPlans.refOf(source))).toVector ++
      offered.offer.requires.filterNot(PlanAid.meetsAlone(ready, player, _))
        .flatMap(PlanAid.lentNeed(ready, window, _, full = false))
        .map((ally, ref) => PlanAid.lentUse(window, ally, ref)))
      .getOrElse(Vector.empty)
```

Update the class comment:
- Costs are paid by the user's Imperial allies first, from what they offered, in turn order after the user, and then by the user (rulings R11 and R12).
- What each ally paid, lent and met is recorded as pool markers (`PlanAid`).

- [ ] **Step 4: Surcharges, Peace Envoy's gift, Arcane Armor, Hearts and Minds**

**`PlanSurcharge.surcharge`, card case.** Replace the single `Costs.onCard(user, card, cost, ...)` with one per `PlanAid.payments(ready, PlanAid.window(ready, application.setup.actor, application.side, user), user, cost)`. Follow each ally's payment with `PlanAid.spent(window, ally, part)`. The title case keeps `onTitle(ready, user)`.

**`PeaceEnvoy.scala:187`.** The gift `Give(Piece.Favor(dice), user, ...)` becomes one `Give` per payment's `favor` part, with that payer as the giver. Follow each ally's part with `PlanAid.spent`. Read the surrounding code and keep its other arguments as they are.

**`ArcaneArmor`:**
- `plan`: `secrets` adds what aid offers:

  ```scala
      val secrets = context.user.fold(0)(faceUpSecrets(context.ready, _)) +
        context.aidSecrets
  ```

  Add to `PlanContext`:

  ```scala
  /** The secrets to pay the allies offer and have not used, or, in the
    * full view, hold. */
  def aidSecrets: Int = aidWindow.fold(0)(window =>
    if full then PlanAid.spare(ready, window).secrets +
      lenders.map(PlanAid.remaining(ready, window, _).secrets).sum
    else lenders.map(PlanAid.remaining(ready, window, _).secrets).sum)
  ```

- `bounds(ready, user)`: add the remaining offered secrets of the window whose user is `user`:

  ```scala
    val aided = ready.planAid.window.filter(_.user == user).fold(0)(window =>
      PlanAid.allies(ready, user).map(PlanAid.remaining(ready, window, _)
        .secrets).sum)
    val secrets = faceUpSecrets(ready, user) + aided
  ```

**`HeartsAndMinds.endDiscard`.** The card stays if its user holds the People's Favor, or if the ally whose People's Favor it used holds it now:

```scala
  override def endDiscard: PlanUse => Vector[Operation] = use =>
    if use.user.exists(favored(use.ready, _)) || lentFavor(use) then
      Vector.empty
    else PlanDiscard.afterCampaign(catalog, id, use, cardId)

  private def lentFavor(use: PlanUse): Boolean = use.user.exists(user =>
    PlanAid.allies(use.ready, user).exists(ally => favored(use.ready, ally) &&
      PlanAid.usedRefs(use.ready, AidWindow(use.actor,
        CampaignPlanSide.Defender, user), ally).contains(PlanAid.peoplesFavor)))
```

Update the doc comment: an ally who lent the People's Favor keeps it met until the end (ruling R12).

- [ ] **Step 5: Run the tests**

Run: `./sbtw "testOnly oathdigital.gameplay.powers.campaign.* oathdigital.gameplay.actions.campaign.*"`
Expected: PASS, and every existing plan suite still passes.

- [ ] **Step 6: Commit**

```bash
git add <every file listed under Files for this task, plus PlanAid.scala and PlanContext.scala if changed>
git commit -m "feat(empire): Imperial allies pay plan costs before the user

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 5: The window opens when aid could help

**Files:**
- Modify: `src/main/scala/distantlands/gameplay/actions/campaign/CampaignPlanChoice.scala` (`userPass` and the class comment)
- Test: `src/test/scala/distantlands/gameplay/powers/campaign/ImperialTacticsWindowSuite.scala` (new)

**Interfaces:**
- Consumes: `CampaignPlanOffer.awaitsAid` (Task 3) and `PlanAid.couldCover` (Task 2).

- [ ] **Step 1: Write the failing tests**

```scala
  test("a Citizen's window opens for an ally's plan though they have none"):
    val run = citizenWithNoPlansDefendsWhileChancellorHoldsShieldWall()
    assert(PlanDriver.awaits(run, p3, defenderPlans))

  test("a Citizen's window opens for a plan only allies can pay for"):
    // p3 holds Shield Wall, has 0 favor; p2 holds favor.
    val run = brokeCitizenWithShieldWallAndFavoredChancellor()
    assert(PlanDriver.awaits(run, p3, defenderPlans))

  test("a Citizen's window stays shut when no ally can help"):
    // p3 holds Shield Wall with 0 favor; no ally holds any favor or plan.
    val run = brokeCitizenWithShieldWallAndBrokeChancellor()
    assert(!PlanDriver.awaits(run, p3, defenderPlans))

  test("an Exile's window is skipped as before"):
    val run = brokeExileWithShieldWall()
    assert(!PlanDriver.awaits(run, p4, defenderPlans))

  test("the window stays open when an ally withdraws while it waits"):
    // p2 offers Shield Wall, then saves an empty offer (via the Table step
    // on the parked state); the decision is still parked with Finish.
    val run = citizenWithNoPlansDefendsWhileChancellorHoldsShieldWall()
    val withdrawn = run.copy(transition = run.transition.copy(state =
      savedOnParkedState(run, p2, AidOffer())))
    assert(PlanDriver.awaits(withdrawn, p3, defenderPlans))
```

`savedOnParkedState` evolves `OathEvent.PlanAidSaved(p2, window, offer)` onto the parked state with `run.game.evolve`.

- [ ] **Step 2: Run them and see them fail**

Run: `./sbtw "testOnly oathdigital.gameplay.powers.campaign.ImperialTacticsWindowSuite"`
Expected: FAIL. The first two tests find the window skipped.

- [ ] **Step 3: Open the window for aid**

In `userPass`, compute after `listed`:

```scala
    val window = PlanAid.window(pass.state, actor, side, user)
    // A plan that awaits an offer, or one the allies could still pay for,
    // keeps the window open for them (Empire phase design, "The window").
    val aidable = window.exists(at => offers.exists(offered =>
      offered.offer.awaitsAid && !attackerUsed(
        CampaignPlans.refOf(offered.offer.source))) ||
      offers.exists(offered => offered.offer.withheld.isEmpty &&
        pass.applies(applicationOf(setup, offered)).left.exists(
          PlanAid.couldCover(pass.state, at, _))))
    val playable = listed.exists(_.available)
    if !playable && !aidable && !pass.resuming then Vector.empty
    else Vector[Operation](
      Branch((_, _) => if !playable && !aidable then Vector.empty
        else Vector(decision(decisionId, user, listed, chosen.size))),
```

`listed` already filters out chosen plans. Keep that filter for `aidable` too, so that it skips offers whose ref is in `chosen`.

Why the parked decision is stable: while it waits, only offers change, and `aidable` depends on the allies' holdings and their full-aid view, not on what they offered. Write that in the comment.

Add a bullet to the class comment: an Imperial user's window is asked when some plan awaits an ally's offer, or could be paid with the allies' help, even when the user can use nothing alone.

- [ ] **Step 4: Run the tests**

Run: `./sbtw "testOnly oathdigital.gameplay.powers.campaign.* oathdigital.gameplay.actions.campaign.*"`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add src/main/scala/distantlands/gameplay/actions/campaign/CampaignPlanChoice.scala \
  src/test/scala/distantlands/gameplay/powers/campaign/ImperialTacticsWindowSuite.scala
git commit -m "feat(empire): open a Citizen's plan window when allies could help

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 6: The offer command

**Files:**
- Modify: `src/main/scala/distantlands/gameplay/OathRulesWalker.scala` (beside `grantAnswers`, line 209)
- Modify: `src/main/scala/distantlands/model/GameViolation.scala:108` (beside `ConsentUnavailable`)
- Modify: `src/main/scala/distantlands/server/GameRoutes.scala:38` (the violation's message)
- Create: `src/main/scala/distantlands/application/PlanAidDesk.scala`
- Modify: `src/main/scala/distantlands/application/GameCommands.scala`:
  - add `SavePlanAid`
  - give it an `issuer` of `None`
- Modify: `src/main/scala/distantlands/application/GameApplicationService.scala`, in three places:
  - `routed`, at 196-224
  - `appendBatch`, at 388-421
  - the defensive arm of `applyUnblockedCommand`, at 508
- Modify: `src/main/scala/distantlands/application/Authorization.scala:51-66` (the `savePlanAid` builder)
- Modify: `src/main/scala/distantlands/application/GameIntentMapper.scala:20-33`
- Modify: `shared/src/main/scala/distantlands/protocol/CommandIntents.scala`, `CommandIntentCodec.scala:7-20` and `CommandIntentDecoders.scala:30-42`
- Test: `src/test/scala/distantlands/application/PlanAidServiceSuite.scala` (new)
- Test: `src/test/scala/distantlands/application/PlanAidIntentMapperSuite.scala` (new; model it on `ConsentIntentMapperSuite`)
- Test: `shared/src/test/scala/distantlands/protocol/CommandProtocolSuite.scala` (a round trip for the new intent)

**Interfaces:**
- Consumes:
  - the Task 1 events
  - `PlanAid` (Task 2)
  - `DecisionOption.lender` (Task 3)
- Produces:
  - `OathRules.planAidWindow(state: OathState): Option[AidWindow]`
  - `OathRules.planAidOfferable(state, ally): Option[(Vector[DecisionOption], Vector[DecisionOptionRef])]`: the user's parked options that `ally` lends, and the requirement refs `ally` can offer
  - `GameCommand.SavePlanAid(ally: PlayerId, offer: AidOffer)`
  - `GameIntent.SavePlanAid(plans: Vector[WalkerStartArgWire], requirements: Vector[WalkerStartArgWire], favor: Int, secrets: Int, favorBurnt: Int, secretsBurnt: Int)`, with JSON type `"savePlanAid"`
  - `OathViolation.PlanAidUnavailable(reason: String)`

- [ ] **Step 1: Write the failing service tests**

`PlanAidServiceSuite` runs through `table.service(...)`, starting from the table that `EmpireCampaignScenarioSuite.scala:25-55` builds. It changes the table so that p1, an Exile, raids p3, a Citizen; p2, the Chancellor, holds Shield Wall faceup and 2 favor. The suite drives the Campaign to the defender's plan decision with that suite's `resolve` helper.

```scala
  test("an ally's offer is saved, opens no batch, and makes the plan choosable"):
    val parked = toCitizenDefenderPlans(service)
    val saved = accepted(service.handle(gameId, parked.nextSequence,
      GameCommand.SavePlanAid(p2, AidOffer(plans = Vector(ShieldWallRef),
        amounts = AidAmounts(favor = 1)))))
    assert(saved.events.exists(_.isInstanceOf[OathEvent.PlanAidSaved]))
    assert(!saved.events.exists(_.isInstanceOf[OathEvent.CommandAccepted]))
    assert(defenderOptions(service).contains(ShieldWallRef))

  test("the user is not their own ally"):
    rejected(service.handle(gameId, parked.nextSequence,
      GameCommand.SavePlanAid(p3, AidOffer())), "PlanAidUnavailable")

  test("an Exile cannot offer"):
    rejected(..., GameCommand.SavePlanAid(p1, AidOffer()), ...)

  test("an ally cannot offer a plan they do not lend, or more than they hold"):
    rejected(..., GameCommand.SavePlanAid(p2, AidOffer(plans =
      Vector(DecisionOptionRef.Denizen(DenizenId("999"))))), ...)
    rejected(..., GameCommand.SavePlanAid(p2, AidOffer(amounts =
      AidAmounts(favor = 3))), ...)

  test("an offer cannot drop what has been used"):
    // p2 offers Shield Wall and 1 favor; p3 picks it; p2 saves an empty offer.
    rejected(..., GameCommand.SavePlanAid(p2, AidOffer()), ...)

  test("the offers clear when the window finishes"):
    // after p3 finishes, ready.planAid is empty and a PlanAidCleared was appended
  test("an offer survives the user's undo and a reload"):
    // p3 picks Shield Wall, then undoes; the offer still stands and the
    // marker is gone (remaining favor is 1 again); reload with
    // ParkedServiceFixture.replayingSecond gives the same state.
  test("no offer is accepted when no Imperial plan decision is parked"):
    rejected(service.handle(gameId, 0L, GameCommand.SavePlanAid(p2,
      AidOffer())), "PlanAidUnavailable")
```

Write `rejected` like the consent service suites' rejection helper. Spell each test's setup out in full; the comments above say what each one sets up.

- [ ] **Step 2: Run them and see them fail to compile**

Run: `./sbtw "testOnly oathdigital.application.PlanAidServiceSuite"`
Expected: compile errors, because `SavePlanAid` is not found.

- [ ] **Step 3: The rules' reads**

In `OathRulesWalker.scala`, next to `grantAnswers`:

```scala
  /** The plan window aid reaches now: an Imperial user's parked battle-plan
    * decision (Empire phase design, "1b-ii"). */
  def planAidWindow(state: OathState): Option[AidWindow] =
    parkedPlanDecision(state).flatMap((ready, decide, side) =>
      PlanAid.window(ready, ready.game.current.walkActor, side, decide.owner))

  /** What `ally` may offer at the parked window: the user's options that
    * `ally` lends, whether available or awaiting the offer, and the
    * requirements `ally` meets. */
  def planAidOfferable(state: OathState, ally: PlayerId)
      : Option[(Vector[DecisionOption], Vector[DecisionOptionRef])] =
    parkedPlanDecision(state).collect {
      case (ready, decide, _) if decide.owner != ally &&
          Imperials.isImperial(ready, ally) =>
        (offered(decide).filter(DecisionOption.lender(_).contains(ally)),
          PlanAid.requirementsOf(ready, ally))
    }

  private def parkedPlanDecision(state: OathState)
      : Option[(ReadyGame, Decide, CampaignPlanSide)] =
    walkerResumeContext(state).toOption.flatMap {
      case (ready, _, tree, pending, powers, _) =>
        walkerCall(Right(ProcedureWalker.parkedDecide(ready, tree, pending,
          powers))).toOption.flatten.flatMap(decide =>
          CampaignPlanSide.values.find(side =>
            CampaignIds.planDecision(side) == decide.decisionId)
            .map((ready, decide, _)))
    }
```

If `CampaignPlanSide` is not an `enum` with `values`, list `Vector(CampaignPlanSide.Attacker, CampaignPlanSide.Defender)` instead. A position that the walk cannot rebuild gives `None`, as `grantAnswers` does.

`walkerResumeContext` rebuilds the tree, which is not free. `planAidWindow` therefore runs only when a walk is parked and its last answered decision, or its parked position, belongs to a Campaign. If an inexpensive check like `powerUseStep`'s `atSlot` is available, run it first: `ready.game.current.walkerProcedure.contains(ActionRef.Campaign)`, or whichever `ProcedureRef` a Campaign walk has. A `CampaignFromSite` power walk is not covered by that check, so also accept a `UsePower` procedure whose position is under a Campaign node. If that cannot be told inexpensively, rebuild the tree; this runs once per command.

- [ ] **Step 4: The desk, the command and the routes**

`src/main/scala/distantlands/application/PlanAidDesk.scala`:

```scala
package oathdigital.application

import oathdigital.gameplay.OathRules
import oathdigital.gameplay.actions.campaign.PlanAid
import oathdigital.model._
import oathdigital.model.OathEvent.{PlanAidCleared, PlanAidSaved}

/** The Imperial Tactics command (Empire phase design, "1b-ii"): an ally's
  * whole offer, accepted at any time while an Imperial user's plan
  * decision is parked. It opens no command batch, as a consent command
  * opens none, so nothing here is undoable; an ally takes an offer back by
  * saving a smaller one. */
private[application] final class PlanAidDesk(rules: OathRules):
  private type Result = Either[OathViolation, Vector[OathEvent]]

  def save(state: OathState, ally: PlayerId, offer: AidOffer): Result = for
    ready <- playing(state)
    window <- rules.planAidWindow(state).toRight(unavailable(
      "no Imperial battle-plan decision is open"))
    _ <- Either.cond(ally != window.user, (),
      unavailable("a player cannot aid their own plans"))
    (lendable, requirements) <- rules.planAidOfferable(state, ally)
      .toRight(unavailable(s"${ally.value} is not an Imperial ally"))
    used = PlanAid.usedRefs(ready, window, ally)
    spent = PlanAid.used(ready, window, ally)
    _ <- offer.plans.find(ref => !used.contains(ref) &&
      !lendable.exists(_.ref == ref)).toLeft(()).left.map(ref =>
      unavailable(s"${ref.wireId} is not a plan ${ally.value} can lend"))
    _ <- offer.requirements.find(ref => !used.contains(ref) &&
      !requirements.contains(ref)).toLeft(()).left.map(ref =>
      unavailable(s"${ally.value} does not meet ${ref.wireId}"))
    _ <- used.find(ref => !offer.plans.contains(ref) &&
      !offer.requirements.contains(ref)).toLeft(()).left.map(ref =>
      unavailable(s"${ref.wireId} was used and cannot be withdrawn"))
    _ <- Either.cond(offer.amounts.atLeast(spent), (),
      unavailable("an offer cannot go below what was used"))
    _ <- fits(ready, ally, offer.amounts - spent)
  yield Vector(PlanAidSaved(ally, window, offer))

  /** The clearing appended after every command once no Imperial plan
    * decision is parked any more, or another one is. */
  def stale(state: OathState): Vector[OathEvent] = state match
    case OathState.Ready(ready) if ready.planAid.window.nonEmpty &&
        rules.planAidWindow(state) != ready.planAid.window =>
      Vector(PlanAidCleared)
    case _ => Vector.empty

  /** What is left to spend fits what the ally holds now. */
  private def fits(ready: ReadyGame, ally: PlayerId, left: AidAmounts)
      : Either[OathViolation, Unit] =
    ready.game.current.players.find(_.player == ally).toRight(
      unavailable(s"${ally.value} is not seated")).flatMap(held =>
      Either.cond(left.favor + left.favorBurnt <= held.board.favor &&
        left.secrets + left.secretsBurnt <= held.board.faceUpSecrets, (),
        unavailable(s"${ally.value} cannot offer more than they hold")))

  private def unavailable(reason: String) =
    OathViolation.PlanAidUnavailable(reason)

  private def playing(state: OathState): Either[OathViolation, ReadyGame] =
    state match
      case OathState.Ready(ready) if ready.game.current.result.isEmpty =>
        Right(ready)
      case OathState.Ready(_) => Left(unavailable("the game is over"))
      case _ => Left(OathViolation.GameNotStarted)
```

`PlanAid` is in `gameplay.actions.campaign`. The application already imports gameplay, so the import is allowed.

**Model:**
- Add to `GameViolation.scala`: `final case class PlanAidUnavailable(reason: String) extends OathViolation`.
- In `GameRoutes.scala:38`, map it to its `reason`, as `ConsentUnavailable` is mapped. Mirror any other exhaustive match on violations that `./sbtw compile` names.

**`GameCommands.scala`:**

```scala
  /** `ally`'s whole Imperial Tactics offer for the parked plan window
    * (Empire phase design, "1b-ii"). Routed to the aid desk; legal over
    * another player's parked decision. */
  final case class SavePlanAid(ally: PlayerId, offer: AidOffer)
      extends GameCommand
```

Then `issuer` gets `case SavePlanAid(_, _) => None`.

**`GameApplicationService`:**
- Construct `private val aid = new PlanAidDesk(rules)` beside `consent`.
- In `routed`, before `case _`:

  ```scala
      case GameCommand.SavePlanAid(ally, offer) =>
        consentBatch(gameId, journal, aid.save(journal.state, ally, offer),
          expected, nextSequence)
  ```

- In `appendBatch`, add the aid clearing after the consent clearing:

  ```scala
      clearing = consent.stale(opened.state) ++ aid.stale(opened.state)
  ```

  If `stale` must see the state after the consent clearing, evolve it first. Clearing an offer does not depend on consent, so computing both on `opened.state` is correct.
- Add the defensive arm in `applyUnblockedCommand`: `case _: GameCommand.SavePlanAid => Left(InvalidEventOrder("an aid offer goes to the aid desk"))`.

Rename `consentBatch` only if the compiler or a reviewer asks for it. Its body fits an aid command unchanged, so add a line to its comment that it also appends aid offers.

**`Authorization.AuthorizedPlayer`:** `def savePlanAid(offer: AidOffer): GameCommand = GameCommand.SavePlanAid(playerId, offer)`, beside `answerConsent`.

**Shared intent:**

```scala
  /** The sender's whole Imperial Tactics offer: the plans they lend and the
    * requirements they meet, as refs, and the four amounts. */
  final case class SavePlanAid(plans: Vector[WalkerStartArgWire],
      requirements: Vector[WalkerStartArgWire], favor: Int, secrets: Int,
      favorBurnt: Int, secretsBurnt: Int) extends GameIntent
```

**The intent's JSON:**
- **Encode.** `CommandIntentCodec.encode` gets the arm `case SavePlanAid(...) => tagged("savePlanAid", ...)`. It writes the two ref arrays in the shape `requestConsent` writes its `target` (`optionKind`, `optionId`) and the four integers by name.
- **Decode.** `CommandIntentDecoders.decode` gets a `"savePlanAid"` arm:
  - It requires exactly the fields `type`, `plans`, `requirements`, `favor`, `secrets`, `favorBurnt` and `secretsBurnt`.
  - It decodes the arrays with `CommandNestedCodecs.decodeStartArgsWire`.
  - It decodes the integers with the integer reader the module already has. If it has none, add `int(value, name, path)` beside `string`, rejecting a negative.

**`GameIntentMapper.bind`:**

```scala
      case Intent.SavePlanAid(plans, requirements, favor, secrets, burnt,
          secretsBurnt) =>
        for
          lent <- refs(plans, "$.intent.plans")
          met <- refs(requirements, "$.intent.requirements")
          amounts <- Either.cond(Vector(favor, secrets, burnt,
            secretsBurnt).forall(_ >= 0), AidAmounts(favor, secrets, burnt,
            secretsBurnt), GameIntentMappingFailure("$.intent",
            "aid amounts are never negative"))
        yield actor.savePlanAid(AidOffer(lent, met, amounts))
```

`refs` maps each `WalkerStartArgWire` with the `optionRef(kind, id, path)` the `RequestConsent` arm uses, indexing the path.

- [ ] **Step 5: Run the tests**

Run: `./sbtw "testOnly oathdigital.application.PlanAid* oathdigital.application.GameApplicationService* oathdigital.server.GameHttpWireSuite" "shared/testOnly oathdigital.protocol.CommandProtocolSuite"`

If the shared project has another sbt name, use that.

Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add <every file listed under Files for this task>
git commit -m "feat(empire): accept Imperial Tactics offers over a parked plan window

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 7: The log lines

**Files:**
- Create: `src/main/scala/distantlands/application/gamelog/PlanAidLines.scala`
- Modify: `src/main/scala/distantlands/application/gamelog/GameLogFormatter.scala:49,182`
- Test: `src/test/scala/distantlands/application/gamelog/GameLogPlanAidSuite.scala` (new; model it on `GameLogConsentSuite`)

**Interfaces:**
- Consumes: `PlanAidSaved` (Task 1).
- Produces: one public `Posted(LogKind.Decision, spans)` per saved offer that changed something. `PlanAidCleared` posts nothing.

- [ ] **Step 1: Write the failing tests**

```scala
  test("an offer names what it added"):
    // p2 saves Shield Wall, the Darkest Secret and 2 favor to pay
    assertEquals(text(lines), Vector(
      "p2 offered Shield Wall, the Darkest Secret and 2 favor to pay."))

  test("a smaller offer names what it took back"):
    // p2 then saves the Darkest Secret and 2 favor to pay
    assertEquals(text(lines).last, "p2 withdrew Shield Wall.")

  test("a changed offer names both"):
    // p2 then saves Towering Rampart, 1 favor to pay and their pawn
    assertEquals(text(lines).last,
      "p2 offered Towering Rampart and their pawn at Hall of Debate, and " +
        "withdrew the Darkest Secret and 1 favor to pay.")

  test("the clearing writes nothing, and every viewer sees the line")
```

Player and card words come from `LogWords`, the way `GameLogConsentSuite` renders them. Use the words that suite's helpers produce in place of "p2" and the card names.

- [ ] **Step 2: Run them and see them fail**

Run: `./sbtw "testOnly oathdigital.application.gamelog.GameLogPlanAidSuite"`
Expected: FAIL, because no line is posted.

- [ ] **Step 3: Write the lines**

`PlanAidLines` has the shape of `ConsentLines`: `private[gamelog] final class PlanAidLines(words: LogWords)` with `def lines(journal: LogJournal, at: Int, viewer: Option[PlayerId]): Vector[Posted]`. For a `PlanAidSaved(ally, window, offer)`:
- `before` is the ally's offer in the state just before the event. Use the state `ConsentLines` reads its "before" from. If the window differs from the state's window, `before` is `AidOffer()`.
- **Added:** the plans, requirements and amount parts in the new offer but not in `before`. **Withdrawn:** those in `before` but not in the new offer. An amount part counts by its difference, so 2 favor to pay becoming 1 is "withdrew 1 favor to pay".
- **Words:**

  | Item | Words |
  |---|---|
  | A card plan | `words.card(...)`, as other lines name cards |
  | The title | "the Oathkeeper title" |
  | `darkestSecret` | "the Darkest Secret" |
  | `yewStaff` | the relic's card word |
  | `peoplesFavor` | "the People's Favor" |
  | `Player(ally)` | "their pawn at {site}", naming the ally's pawn site in the state after the event |
  | Amounts | "{n} favor to pay", "{n} {secret/secrets} to pay", "… to burn" |

- **Joining:** items are joined as "a, b and c". The line is "{ally} offered …", "{ally} withdrew …", or "{ally} offered …, and withdrew …", ending with a period. Nothing changed means no line.

Dispatch it in `GameLogFormatter`: construct it at line 49 beside `consent`, and add `case _: PlanAidEvent => (planAid.lines(journal, at, viewer), run)` beside the `ConsentEvent` arm at 182.

- [ ] **Step 4: Run the tests**

Run: `./sbtw "testOnly oathdigital.application.gamelog.*"`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add src/main/scala/distantlands/application/gamelog/PlanAidLines.scala \
  src/main/scala/distantlands/application/gamelog/GameLogFormatter.scala \
  src/test/scala/distantlands/application/gamelog/GameLogPlanAidSuite.scala
git commit -m "feat(empire): log what Imperial allies offer and withdraw

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 8: The ally's projection

**Files:**
- Create: `shared/src/main/scala/distantlands/protocol/projection/PlanAidProjectionDtos.scala`
- Modify: `shared/src/main/scala/distantlands/protocol/projection/GameProjectionDto.scala:32-34` (the new field)
- Modify: `shared/src/main/scala/distantlands/protocol/projection/GameProjectionCodec.scala:22,72-77,131`, plus a codec beside `ConsentProjectionCodec`
- Create: `src/main/scala/distantlands/application/PlanAidProjector.scala`
- Modify: `src/main/scala/distantlands/application/GameProjection.scala:26,51-66`
- Modify: `src/main/scala/distantlands/application/WalkerDecisionProjector.scala`. Make `optionProjection` reachable as `private[application] def option(ready, viewer, option): Option[DecisionOptionProjection]`.
- Test: `src/test/scala/distantlands/application/PlanAidProjectionSuite.scala` (new; model it on `ConsentProjectionSuite`)
- Test: the projection codec round-trip suite (find it with `grep -rln "consentPrompt" shared/src/test src/test`)

**Interfaces:**
- Consumes: `OathRules.planAidWindow` and `planAidOfferable` (Task 6), and `PlanAid` (Task 2).
- Produces:

```scala
package oathdigital.protocol.projection

/** An Imperial ally's view of another Imperial player's open plan window
  * (Empire phase design, "Offer panel"): what they may offer, drawn as
  * board options; what their saved offer holds; what of it was used; and
  * what they hold. Refs are `kind:id`, as `BoardOptionViews.key` spells
  * them. */
final case class PlanAidPromptProjection(userPlayerId: String, side: String,
    plans: Vector[DecisionOptionProjection],
    requirements: Vector[DecisionOptionProjection],
    saved: PlanAidOfferProjection, used: PlanAidOfferProjection,
    favorHeld: Int, secretsHeld: Int)

final case class PlanAidOfferProjection(plans: Vector[String] = Vector.empty,
    requirements: Vector[String] = Vector.empty, favor: Int = 0,
    secrets: Int = 0, favorBurnt: Int = 0, secretsBurnt: Int = 0)
```

The new field is `GameProjection.planAidPrompt: Option[PlanAidPromptProjection] = None`.

- [ ] **Step 1: Write the failing tests**

```scala
  test("an ally sees what they may offer and what they hold"):
    // parked at p3's defender plans; p2 holds Shield Wall, the Darkest
    // Secret and 2 favor
    val prompt = project(state, p2).planAidPrompt.get
    assertEquals(prompt.userPlayerId, "p3")
    assertEquals(prompt.plans.map(o => s"${o.kind}:${o.id}"),
      Vector("denizen:108"))
    assertEquals(prompt.requirements.map(o => s"${o.kind}:${o.id}"),
      Vector("banner:darkest-secret", "player:p2"))
    assertEquals(prompt.favorHeld, 2)

  test("a saved offer and its used part are shown")
  test("the user, an Exile and a bystander outside any window see no prompt")
  test("a facedown adviser is never among the plans")
```

- [ ] **Step 2: Run them and see them fail to compile**

Run: `./sbtw "testOnly oathdigital.application.PlanAidProjectionSuite"`
Expected: compile errors, because `planAidPrompt` is not found.

- [ ] **Step 3: Write the projector**

```scala
package oathdigital.application

import oathdigital.gameplay.OathRules
import oathdigital.gameplay.actions.campaign.PlanAid
import oathdigital.model._
import oathdigital.protocol.projection.{GameProjection => _, _}

/** The offer panel's projection for an Imperial ally while another
  * Imperial player's plan window is parked (Empire phase design, "Offer
  * panel"). Offerable plans are the user's own options the ally lends,
  * which the window already lists, available or awaiting the offer;
  * requirements are what the ally meets now. */
private[application] final class PlanAidProjector(rules: OathRules,
    decisions: WalkerDecisionProjector):
  def prompt(state: OathState, viewer: Option[PlayerId])
      : Option[PlanAidPromptProjection] = for
    ally <- viewer
    window <- rules.planAidWindow(state)
    (plans, requirements) <- rules.planAidOfferable(state, ally)
    ready <- state match
      case OathState.Ready(ready) => Some(ready)
      case _ => None
    held <- ready.game.current.players.find(_.player == ally)
  yield PlanAidPromptProjection(window.user.value, side(window.side),
    plans.flatMap(decisions.option(ready, viewer, _)),
    requirements.flatMap(ref => DecisionOption.forRef(ref)
      .flatMap(decisions.option(ready, viewer, _))),
    offer(ready.planAid.window.filter(_ == window)
      .fold(AidOffer())(_ => ready.planAid.offerOf(ally))),
    offer(AidOffer(PlanAid.usedRefs(ready, window, ally).filter(ref =>
      ready.planAid.offerOf(ally).plans.contains(ref)),
      PlanAid.usedRefs(ready, window, ally).filter(ref =>
        ready.planAid.offerOf(ally).requirements.contains(ref)),
      PlanAid.used(ready, window, ally))),
    held.board.favor, held.board.faceUpSecrets)

  private def side(side: CampaignPlanSide): String = side match
    case CampaignPlanSide.Attacker => "attacker"
    case CampaignPlanSide.Defender => "defender"

  private def offer(value: AidOffer): PlanAidOfferProjection =
    PlanAidOfferProjection(value.plans.map(key), value.requirements.map(key),
      value.amounts.favor, value.amounts.secrets, value.amounts.favorBurnt,
      value.amounts.secretsBurnt)

  private def key(ref: DecisionOptionRef): String = s"${ref.kind}:${ref.wireId}"
```

An option the ally lends that is listed as awaiting is `Unavailable` in the user's decision. Project it as available for the ally, because they can offer it: unwrap the `Unavailable` when its reason is an `ImperialTactics` note before calling `decisions.option`. Add a small helper that strips `Unavailable` and `Lent` wrappers and keeps `Badged` and `Priced`.

If `DecisionOption.forRef` gives nothing for a `Player` or `Banner` ref, build `DecisionOption.Player(ref)` or `DecisionOption.Banner(ref)` directly.

Wire it in `GameProjection.scala` the way `ConsentProjector` is wired: construct it at line 26 and set `planAidPrompt = aid.prompt(state, context.viewer)` in `applied`. The projector has no `context`, so take the viewer from `ScopedProjectionContext` as `ConsentProjector` does.

DTO codec: add `"planAidPrompt"`. It is written only when set, and on decode a missing field reads as `None`, as `consentPrompt` does.

- [ ] **Step 4: Run the tests**

Run: `./sbtw "testOnly oathdigital.application.*Projection*"`, plus the shared codec suite.
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add <every file listed under Files for this task>
git commit -m "feat(empire): project what an Imperial ally may offer

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 9: The offer panel and board toggles

**Files:**
- Create: `frontend/src/main/scala/distantlands/frontend/PlanAidDraft.scala`
- Create: `frontend/src/main/scala/distantlands/frontend/PlanAidPanel.scala`
- Modify: `frontend/src/main/scala/distantlands/frontend/frontend.scala:41-46` (alias `PlanAidPromptState`)
- Modify: `frontend/src/main/scala/distantlands/frontend/SessionDrafts.scala` (the `aid` slot and `Draft.Aid`)
- Modify: `frontend/src/main/scala/distantlands/frontend/TableScreen.scala:103-106` (aid board options when there is no board decision)
- Modify: `frontend/src/main/scala/distantlands/frontend/ParkedDecision.scala:137` (mount the panel beside `ConsentPanels.render`)
- Modify: the frontend stylesheet the consent panel's classes live in (find `consent-prompt` with grep)
- Test: `frontend/src/test/scala/distantlands/frontend/PlanAidPanelSuite.scala` (new)
- Test: `frontend/src/test/scala/distantlands/frontend/PlanAidDraftSuite.scala` (new)

**Interfaces:**
- Consumes:
  - `GameProjection.planAidPrompt` (Task 8)
  - `GameIntent.SavePlanAid` (Task 6)
  - `BoardOptions`, `OptionView` and `OptionState`
  - `ConsentAsking.name(value, id)`, for the user's display name
- Produces: `PlanAidDraft(context, userPlayerId, side, plans, requirements, favor, secrets, favorBurnt, secretsBurnt)` and `PlanAidPanel.render(value, canControl, panel, drafts, controls)`.

The panel, as approved:
- **Heading:** "You may offer your battle plans, to pay costs and to meet requirements for {user}".
- **Board:** offerable plans and requirements are highlighted on the board the way plan options are. A click toggles the item in the draft. Facedown cards are never highlighted.
- **Plans and Requirements lists:**

  | State | Row |
  |---|---|
  | Pending addition | the name, with no mark |
  | Saved, still in the draft | the name with a check mark |
  | Pending removal | the name struck through |
  | Used | the name with a lock and "used"; its board item is `Locked` and ignores clicks |

- **Amounts:**
  - Four steppers: Favor to pay, Secrets to pay, Favor to burn, Secrets to burn.
  - The lower bound is what was used.
  - The upper bound keeps favor to pay plus favor to burn within `favorHeld`, and likewise for secrets.
  - A saved amount shows "{n} offered".
  - A used amount shows "{n} used".
- **Button:**
  - "Offer" while nothing is saved and the draft differs.
  - "Save changes" while something is saved and the draft differs.
  - "Offered", disabled, when the draft equals what is saved.
  - Disabled whenever `canControl` is false.
- **Saving** sends `GameIntent.SavePlanAid` with the whole draft.

- [ ] **Step 1: Write the failing draft tests**

```scala
  test("a fresh draft starts from the saved offer")
  test("clicking an offerable card adds it, clicking again removes it")
  test("a used item cannot be toggled off")
  test("the draft resets when the prompt is for another window")
  test("the command carries the whole draft"):
    assertEquals(draft.command, GameIntent.SavePlanAid(
      Vector(WalkerStartArgWire("denizen", "108")),
      Vector(WalkerStartArgWire("banner", "darkest-secret")), 2, 0, 0, 0))
```

- [ ] **Step 2: Run them and see them fail to compile**

Run: `./sbtw "frontend/testOnly oathdigital.frontend.PlanAidDraftSuite"`
Expected: compile errors.

- [ ] **Step 3: Write the draft**

```scala
package oathdigital.frontend

import oathdigital.protocol.{GameIntent => GameCommand, WalkerStartArgWire}

/** An Imperial ally's offer as they build it (Empire phase design, "Offer
  * panel"): keys are `kind:id`, as the prompt and the board spell them. It
  * starts from the saved offer and is kept while the prompt is for the same
  * window. */
private[frontend] final case class PlanAidDraft(context: BoardSelectionContext,
    userPlayerId: String, side: String, plans: Vector[String],
    requirements: Vector[String], favor: Int, secrets: Int, favorBurnt: Int,
    secretsBurnt: Int):

  /** `key` added, or taken out unless it was used. */
  def toggled(prompt: PlanAidPromptState, key: String): PlanAidDraft =
    def flip(keys: Vector[String], used: Vector[String]) =
      if !keys.contains(key) then keys :+ key
      else if used.contains(key) then keys
      else keys.filterNot(_ == key)
    if prompt.plans.exists(option => s"${option.kind}:${option.id}" == key) ||
        prompt.saved.plans.contains(key) then
      copy(plans = flip(plans, prompt.used.plans))
    else copy(requirements = flip(requirements, prompt.used.requirements))

  def matchesSaved(prompt: PlanAidPromptState): Boolean =
    plans.toSet == prompt.saved.plans.toSet &&
      requirements.toSet == prompt.saved.requirements.toSet &&
      favor == prompt.saved.favor && secrets == prompt.saved.secrets &&
      favorBurnt == prompt.saved.favorBurnt &&
      secretsBurnt == prompt.saved.secretsBurnt

  def command: GameCommand.SavePlanAid = GameCommand.SavePlanAid(
    plans.map(PlanAidDraft.wire), requirements.map(PlanAidDraft.wire),
    favor, secrets, favorBurnt, secretsBurnt)

private[frontend] object PlanAidDraft:
  def wire(key: String): WalkerStartArgWire = key.split(":", 2) match
    case Array(kind, id) => WalkerStartArgWire(kind, id)
    case _ => WalkerStartArgWire(key, "")

  def fresh(context: BoardSelectionContext, prompt: PlanAidPromptState)
      : PlanAidDraft = PlanAidDraft(context, prompt.userPlayerId, prompt.side,
    prompt.saved.plans, prompt.saved.requirements, prompt.saved.favor,
    prompt.saved.secrets, prompt.saved.favorBurnt, prompt.saved.secretsBurnt)

  /** The draft kept for the same window and seat, else a fresh one; none
    * without a prompt. */
  def reconcile(previous: Option[PlanAidDraft], context: BoardSelectionContext,
      prompt: Option[PlanAidPromptState]): Option[PlanAidDraft] =
    prompt.map(open => previous.filter(draft =>
      draft.context.playerId == context.playerId &&
        draft.userPlayerId == open.userPlayerId && draft.side == open.side)
      .getOrElse(fresh(context, open)))

  /** The board's views: each offerable or saved item, `Locked` when used,
    * `Selected` when in the draft, else `Available`. */
  def views(prompt: PlanAidPromptState, draft: PlanAidDraft)
      : Map[String, OptionView] =
    (prompt.plans ++ prompt.requirements).map { option =>
      val key = s"${option.kind}:${option.id}"
      val used = prompt.used.plans.contains(key) ||
        prompt.used.requirements.contains(key)
      val chosen = draft.plans.contains(key) || draft.requirements.contains(key)
      key -> OptionView(option, if used then OptionState.Locked
        else if chosen then OptionState.Selected else OptionState.Available,
        None)
    }.toMap
```

If `BoardSelectionContext.playerId` is named differently, use its real name. In `SessionDrafts`:
- add `aid: Option[PlanAidDraft]` (default `None` in `empty`)
- reconcile it with `PlanAidDraft.reconcile(aid, context, projection.planAidPrompt)`
- add `case Draft.Aid(value) => copy(aid = value)`

In `TableScreen.render`, when the surface is not a board decision, use:

```scala
        }.orElse(for
          prompt <- value.planAidPrompt
          draft <- drafts.aid
          if view.controlsAvailable
        yield new BoardOptions(PlanAidDraft.views(prompt, draft), chosen =>
          controls.stage(Draft.Aid(Some(draft.toggled(prompt,
            s"${chosen.kind}:${chosen.id}"))))))
          .getOrElse(BoardOptions.none)
```

A used item's view is `Locked`, so `BoardOptions` does not call `pick` for it. Check `mark`. If a locked view still answers clicks, have `toggled`'s used check carry it, which it already does.

- [ ] **Step 4: Write the failing panel tests, then the panel**

`PlanAidPanelSuite`, built in the style of `ConsentPromptPanelSuite` on `TestProjections.actingProjection(...)` with a `planAidPrompt`:

```scala
  test("the heading names the user")
  test("a pending addition shows no mark, a saved item a check, a removal is struck")
  test("a used item shows a lock and 'used'")
  test("a saved amount shows how much is offered and used, and cannot go below used")
  test("the button reads Offer, Save changes or Offered")
  test("saving submits the whole draft")
```

`PlanAidPanel.render(value, canControl, panel, drafts, controls)` renders nothing without `value.planAidPrompt` and `drafts.aid`. It builds:

- a `section.plan-aid-prompt`;
- the heading `p.plan-aid-heading`;
- for each list, a `div.plan-aid-list` with an `h3` ("Plans", "Requirements") and one row per item in `draft ∪ saved`, in prompt order:
  - a pending addition is `li.plan-aid-item`;
  - a saved item still drafted is `li.plan-aid-item.plan-aid-saved`, with a check mark;
  - a pending removal is `li.plan-aid-item.plan-aid-removed`, struck through by CSS;
  - a used item is `li.plan-aid-item.plan-aid-used`, with a lock and the text "used";
  - the label comes from the prompt option's `label`;
- an amounts block with four rows, each with:
  - the name
  - an "{n} offered" tag when `saved > 0`
  - an "{n} used" tag when `used > 0`
  - "−" and "+" buttons, staging `Draft.Aid` with the changed amount and disabled at the bounds
  - the value
- the button `button.plan-aid-save`, whose label and disabled state follow the rules above, and whose `onclick` is `controls.submitCommand(draft.command)`.

Mount it in `ParkedDecision.scala:137`: `PlanAidPanel.render(value, canControl, panel, drafts, controls)`, right after `ConsentPanels.render`. Pass `drafts` the way that function receives them. Add CSS next to the consent panel's: `.plan-aid-removed { text-decoration: line-through; }`, plus muted styles for `.plan-aid-used` and the tags, using the stylesheet's existing tokens.

- [ ] **Step 5: Run the frontend tests**

Run: `./sbtw frontend/test`
Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add <every file listed under Files for this task>
git commit -m "feat(empire): an offer panel for Imperial allies, built from the board

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 10: Scenario, docs and gates

**Files:**
- Create: `src/test/scala/distantlands/application/ImperialTacticsScenarioSuite.scala`
- Modify: `docs/rules/ambiguities.md` (R16, and R12 as now worded)
- Modify: `docs/rules/rules-reference.md`, "Imperial laws" (an "Imperial Tactics" line)
- Modify: `docs/rules/implementation-traceability.md:104`
- Modify: `docs/superpowers/specs/2026-10-29-empire-phase-design.md` (Status line)
- Modify: `docs/ROADMAP.md` (delete the "Slice 1b-ii, Imperial Tactics" item)
- Modify: `docs/architecture/bounded-campaign.md` (a short "Imperial Tactics" section)

- [ ] **Step 1: Write the scenario**

This is the spec's 1b-ii scenario: a Citizen defender uses the Chancellor's plan, paid in part by the Chancellor. It runs through the service, in the style of `EmpireCampaignScenarioSuite.scala:25-55`:
- `Table.start.imperial(p2, p3)`. p1 is an Exile.
- p2 holds Shield Wall faceup as an adviser and 1 favor.
- p3 holds Extra Provisions faceup as an adviser and 1 favor, and has warbands on their board.
- p1 has a pawn at p3's site and raids p3.

```scala
  test("a Citizen defender uses the Chancellor's plan, paid in part by the Chancellor"):
    // p1 starts the Campaign and finishes their plans; p3's window opens.
    val parked = toCitizenDefenderPlans(service)
    // p2 offers Shield Wall and 1 favor to pay.
    val offered = accepted(service.handle(gameId, parked.nextSequence,
      GameCommand.SavePlanAid(p2, AidOffer(plans = Vector(ShieldWallRef),
        amounts = AidAmounts(favor = 1)))))
    // p3 picks Shield Wall (paid by p2), then Extra Provisions (paid by p3),
    // then finishes.
    val shield = resolve(service, offered, p3, defenderPlans,
      DecisionAnswer.ChooseOneAnswer(ShieldWallRef))
    val provisions = resolve(service, shield, p3, defenderPlans,
      DecisionAnswer.ChooseOneAnswer(ExtraProvisionsRef))
    val finished = resolve(service, provisions, p3, defenderPlans,
      DecisionAnswer.ChooseOneAnswer(CampaignIds.finish))
    val ready = readyOf(service)
    assertEquals(favorOf(ready, p2), 0)
    assertEquals(favorOf(ready, p3), 0)
    assertEquals(ready.planAid, PlanAidState())
    assert(logText(service).contains(
      "p2 offered Shield Wall and 1 favor to pay."))
```

Use the existing suite's `resolve` helper and its log and state readers, and the player words the log really uses. Then let the Campaign run to its end with seeded dice (`Dice.raid` or whatever that suite uses), and assert the defense pool held 3 extra dice: 2 from Shield Wall and 1 from Extra Provisions.

- [ ] **Step 2: Run it**

Run: `./sbtw "testOnly oathdigital.application.ImperialTacticsScenarioSuite"`
Expected: PASS. If it fails, the failure is in an earlier task's code: fix it there and add the missing unit test.

- [ ] **Step 3: Docs**

- **`ambiguities.md`:**
  - Add R16 with the spec's wording ("what aid covers").
  - Update R12's text to the spec's.
  - Cite CR p. 35 for both.
- **`rules-reference.md`, under "Imperial laws":**

  ```markdown
  - **Imperial Tactics.** In a Campaign, an Imperial player may use battle plans in another Imperial player's play area with their consent, and plans at Imperial sites with the Chancellor's consent; any Imperial player may pay a plan's costs or meet its requirements, even split between players. The Oathkeeper title is a defensive battle plan. The attacker and defender cannot use the same plan (CR p. 35).
  ```

- **`implementation-traceability.md:104`:**
  - In the evidence column, add: "Imperial Tactics (slice 1b-ii): allies save standing offers over a parked plan window (`PlanAidDesk`), lent plans and requirements reach the window through `PlanContext`, allies pay first (`PlanAid.payments`), and an offer panel builds offers from the board (`ImperialTacticsSuite`, `ImperialTacticsPaymentSuite`, `ImperialTacticsWindowSuite`, `PlanAidServiceSuite`, `ImperialTacticsScenarioSuite`)."
  - Drop "Imperial Tactics (Empire slice 1b-ii) and" from the deferred column.
- **The spec's Status:** "Slices 1b-i and 1b-ii implemented, 2026-11-01." Use the real date of the last commit.
- **`ROADMAP.md`:** delete the 1b-ii item outright.
- **`bounded-campaign.md`:** a short section covering:
  - where offers live (`ReadyGame.planAid`, events, desk)
  - how the window reads them (`PlanContext`, the full-aid view, `BattlePlan.lent`)
  - how payments split
  - why spending is pool markers: undo and cleanup come with the walk

- [ ] **Step 4: Gates**

Run each one:
- `./sbtw clean coverage test coverageReport` (statement coverage at least 92%)
- `./sbtw frontend/test`
- `python3 scripts/check-architecture.py`, which watches the 800-line cap on `CampaignPlanApplication.scala`, `PlanContext.scala` and `GameApplicationService.scala`
- `python3 scripts/check-markdown-links.py`

Expected: all pass. If a file passes 800 lines, split out a cohesive part: for example, move `PlanContext`'s lending members into a `PlanLending` helper in the same package.

- [ ] **Step 5: Commit**

```bash
git add src/test/scala/distantlands/application/ImperialTacticsScenarioSuite.scala \
  docs/rules/ambiguities.md docs/rules/rules-reference.md \
  docs/rules/implementation-traceability.md \
  docs/superpowers/specs/2026-10-29-empire-phase-design.md \
  docs/ROADMAP.md docs/architecture/bounded-campaign.md
git commit -m "test(empire): a Citizen defends with the Chancellor's plan; record slice 1b-ii

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```
