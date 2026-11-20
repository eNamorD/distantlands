# Reconsidering a Consent Answer, Slice 1: Preference Rows Implementation Plan

> Executed inline by the implementer. Steps use checkbox (`- [ ]`) syntax
> for tracking.

**Goal:** The consent column's heading reads "Who may ignore this power",
and each preference row shows the player's name on its own line with the
three settings below it in equal thirds, so no setting wraps alone, at
360px too.

**Architecture:** One renderer draws the column everywhere it appears:
`ConsentGridView.render` in `SitePowerLinks.scala`, which
`CardInspectionOverlay` calls for a site power, a ruled card, the Festival
banner and the festival marker. The slice changes that renderer's heading
and wraps a row's three settings in one element, and changes the
`.consent-grid*` rules in `frontend/styles.css` so that element is a
three-column grid. No projection, intent, wire or server code changes.
The stylesheet's rules are pinned by a suite in the root project, on the
pattern of `StylesheetChronicleDetailsSuite`.

**Tech Stack:** Scala.js frontend, munit, jsdom, CSS, `./sbtw` (sbt
wrapper), compiler flags including `-Werror`.

**Spec:** [Reconsidering a consent answer](../../specs/2026-11/2026-11-20-consent-reconsider-design.md),
section "Frontend", paragraph "Preference rows", and "Slices", item 1.
It extends the
[consent design](../../specs/2026-10/2026-10-08-consent-system-design.md)
(ruling 16, "Frontend"). Rulings made while this plan runs go in the
[implementation rulings](../../specs/2026-11/2026-11-20-consent-reconsider-design-implementation-rulings.md).
Read `DESIGN.md` ("Buttons", "Elevation & Depth", "Hover Note", "Do's and
Don'ts") and
`/Users/roman/.claude/skills/impeccable/reference/craft-floor.md` before
the CSS task.

## Global Constraints

- The heading reads exactly "Who may ignore this power".
- Each row shows the player's name on its own line and the three settings
  below it in equal thirds, so no button wraps alone. This holds at 360px.
- The settings keep their labels, order and behaviour: "Never Allow",
  "Case-by-case", "Always Allow"; the setting in force carries
  `aria-pressed="true"`; a click on another setting moves the pressed
  state at once and sends one `SetConsentPreference`; a click on the
  pressed setting sends nothing; without control every setting is
  `disabled`.
- The settings wear the compact Control face (`DESIGN.md`, "Buttons"):
  Raised fill, Brass Line border, 4px radius, 0.78rem, 30px tall. The
  pressed setting keeps selection blue on Selection Fill. No new color.
- No `transition` and no `animation`. No accent color. Touch targets are
  44px through the existing `@media (pointer: coarse)` rule, which already
  lists `.consent-grid-choice`.
- Protected (do not change): `ConsentGrid`, `ConsentGridRow` and
  `SitePowerLinks`; every `CardInspectionOverlay` method; the class names
  `consent-grid`, `consent-grid-heading`, `consent-grid-row`,
  `consent-grid-player` and `consent-grid-choice`, and the row's
  `data-player-id` and a setting's `data-setting`; the prompt notice in
  `ConsentPanels.scala`, which slice 3 changes; the `.overlay-confirm`
  rules, which share a face with the settings.
- Work in
  `/Users/roman/projects/distantlands/.worktrees/consent-reconsider` on
  branch `consent-reconsider`. `.tooling` and `node_modules` are symlinks;
  never commit them. Never commit on `0.2.0-alpha` or `main`. Push
  nothing, merge nothing.
- Stage files by name. The hook blocks `git add -A`, `git add -u`,
  `git add .`, `git add --all`, `git commit -a` and `git stash`.
- Commits use Conventional Commits with a scope and end with
  `Co-Authored-By: Claude Fable 5.2 <noreply@anthropic.com>`. Run
  `git log -3` after each commit and report any commit that is not ours.
- Code, comments, docs and commit messages are plain English prose. Wrap
  Scala and Markdown at 80 columns. Scaladoc cites "reconsider design,
  slice 1".
- Test helper names state what they build.
- Never write a bare `===` or `=word` in a shell command. Quote globs.
- Claim evidence in reports: tag each claim *gate*, *read* or *UNKNOWN*.

## Rulings

Recorded in the
[implementation rulings](../../specs/2026-11/2026-11-20-consent-reconsider-design-implementation-rulings.md);
the tasks below apply them.

- **R1.** `.consent-grid { font-size: 0.82em; }`, the details column's
  size. If Task 4 finds a name or a setting under the Eleven-Pixel Floor
  at 360px, stop and report; do not shrink further. Task 2, Task 4.
- **R2.** The settings' wrapper carries `role="group"` and `aria-label`
  set to the player's name. Task 1.
- **R3.** `4px 6px` padding on `.consent-grid-choices
  .consent-grid-choice` only; the shared Control-face rule is untouched.
  Task 4 measures it. If "Case-by-case" still clips or wraps at 360px,
  stop and report the measured widths; do not choose another fix. Task 2,
  Task 4.

## Decisions already made in this plan

These are mechanical or follow from the design system; each is listed so
the reviewer can reject it.

- A row's three settings sit in a new `div.consent-grid-choices`. The
  existing class names stay, so the other consent suites' selectors still
  match.
- R3's reasoning, to be confirmed by measurement in Task 4. A setting in
  thirds takes `4px 6px` padding where the Control face says `4px 9px`.
  The third sets the button's width, so side padding only matters at the
  narrowest width: at 360px the overlay's body is about 305px wide
  inside (360 less `4vmin` twice, the 1px border twice and `1.5ex`
  twice). Three settings and two 6px gaps leave about 97px a setting. The
  widest label, "Case-by-case" at 0.78rem, is about 77px. With 9px sides
  and the border it needs 97px, which is no margin at all; with 6px sides
  it needs 91px.
- Rows stand 12px apart and a name 4px above its settings, so a name
  reads with its own settings (craft floor, "Spacing"). The column's
  16px top margin stays, which keeps more space above the heading than
  below it.
- The stylesheet suite lives in the root project
  (`src/test/scala/distantlands/server`), where the other stylesheet
  suites are, although the slice changes frontend files only.
- `DESIGN.md` has no section for the consent column today. Task 3 adds
  "Consent Column" under "Components"; slice 3 extends it with the
  requests list. `.impeccable/design.json` already lags `DESIGN.md` (it
  still counts three overlays) and is left alone.

## Files

- Modify: `frontend/src/main/scala/distantlands/frontend/SitePowerLinks.scala`
  (`ConsentGridView`)
- Modify: `frontend/src/test/scala/distantlands/frontend/SitePowerGridSuite.scala`
- Modify: `frontend/styles.css` (the `.consent-grid*` rules near line 2326)
- Create: `src/test/scala/distantlands/server/StylesheetConsentGridSuite.scala`
- Modify docs: `DESIGN.md` (new "Consent Column" section), the spec's
  status line.

## Running things

- One frontend suite:
  `./sbtw "frontend/testOnly distantlands.frontend.SitePowerGridSuite"`.
- The consent overlay suites together:
  `./sbtw "frontend/testOnly distantlands.frontend.SitePowerGridSuite distantlands.frontend.BannerConsentGridSuite distantlands.frontend.BoardCardConsentGridSuite distantlands.frontend.ChronicleDetailsOverlaySuite distantlands.frontend.ConsentPromptPanelSuite"`.
- The stylesheet suite:
  `./sbtw "testOnly distantlands.server.StylesheetConsentGridSuite"`.
- Full gate at slice end, in the background or with a long timeout:
  `./sbtw test` (about 6800 tests), `./sbtw frontend/test` (about 1100
  tests), `python3 scripts/check-architecture.py`,
  `python3 scripts/check-markdown-links.py`.

## Re-inventory before each task

The inventory below was read at `94fcbb6d1`. Each task begins by running
its grep at HEAD. Treat what the grep shows as the list; handle a hit
missing from the task by the task's rule, and report it.

---

### Task 1: The heading and the row's structure

**Files:**
- Modify: `frontend/src/main/scala/distantlands/frontend/SitePowerLinks.scala:56-88` (`ConsentGridView`)
- Test: `frontend/src/test/scala/distantlands/frontend/SitePowerGridSuite.scala`

**Interfaces:**
- Consumes: `ConsentGrid(rows: Vector[ConsentGridRow], choose: Option[(String, String) => Unit])`
  and `ConsentGridRow(playerId: String, name: String, setting: String)`,
  unchanged.
- Produces: `ConsentGridView.render(grid: ConsentGrid): dom.Element`,
  same signature. Its tree becomes:
  `section.consent-grid` holding `h4.consent-grid-heading` and, for each
  row, `div.consent-grid-row[data-player-id]` holding
  `span.consent-grid-player` then `div.consent-grid-choices` holding the
  three `button.consent-grid-choice[data-setting]`. Task 2's rules and
  slice 3's requests list rely on these class names.

- [ ] **Step 1: Re-inventory**

Run:
```bash
git grep -n -e "Consent preferences" -e "consent-grid-row" -e "consent-grid-player" -- frontend/src src shared
```
Expected: the heading text in `SitePowerLinks.scala` and
`SitePowerGridSuite.scala` only; `consent-grid-row` in
`SitePowerLinks.scala` and `BannerConsentGridSuite.scala` (which reads
only the rows' `data-player-id` and their count, and needs no change);
`consent-grid-player` in `SitePowerLinks.scala` only. Another test that
asserts the old heading takes the new one in Step 2. Another reader of a
row's direct children is reported before it is changed.

- [ ] **Step 2: Write the failing tests**

In `frontend/src/test/scala/distantlands/frontend/SitePowerGridSuite.scala`,
change the heading assertion in the test "the overlay draws the grid; a
choice moves the pressed button and is sent once" from

```scala
    assertEquals(root.querySelector(".consent-grid-heading").textContent,
      "Consent preferences")
```

to

```scala
    assertEquals(root.querySelector(".consent-grid-heading").textContent,
      "Who may ignore this power")
```

and add this test directly after that test:

```scala
  test("a preference row names the player on its own line and holds the " +
      "three settings together under it"):
    val drawn = ConsentGridView.render(ConsentGrid(Vector(
      ConsentGridRow("blue", "Blue", "case-by-case"),
      ConsentGridRow("green", "Green", "never")), None))
    val rows = all(drawn, ".consent-grid-row")
    assertEquals(rows.map(_.getAttribute("data-player-id")),
      Vector("blue", "green"))
    rows.zip(Vector("Blue", "Green")).foreach { (row, name) =>
      assertEquals(row.children.toVector.map(_.getAttribute("class")),
        Vector("consent-grid-player", "consent-grid-choices"))
      assertEquals(row.children(0).textContent, name)
      val settings = row.children(1)
      assertEquals(settings.getAttribute("role"), "group")
      assertEquals(settings.getAttribute("aria-label"), name)
      assertEquals(settings.children.toVector.map(setting =>
          setting.getAttribute("class") -> setting.textContent),
        Vector("consent-grid-choice" -> "Never Allow",
          "consent-grid-choice" -> "Case-by-case",
          "consent-grid-choice" -> "Always Allow"))
    }
    assertEquals(all(drawn, "[aria-pressed='true']")
      .map(_.getAttribute("data-setting")), Vector("case-by-case", "never"))
```

- [ ] **Step 3: Run the suite to verify it fails**

Run: `./sbtw "frontend/testOnly distantlands.frontend.SitePowerGridSuite"`
Expected: two failures. The changed test fails on the heading
(`"Consent preferences"` is not `"Who may ignore this power"`). The new
test fails on the row's children: a row holds the name and the three
buttons, not the name and `consent-grid-choices`.

- [ ] **Step 4: Write the implementation**

In `frontend/src/main/scala/distantlands/frontend/SitePowerLinks.scala`,
replace the doc comment and body of `ConsentGridView` with:

```scala
/** The grid as the overlay draws it (reconsider design, slice 1): the
  * heading says what the viewer consents to, and each row is a player's
  * name on its own line over that player's three settings, which the
  * stylesheet lays in equal thirds. A choice is shown at once and sent;
  * the overlay ignores polls while open, so it keeps what was chosen, and
  * a choice already shown sends nothing. */
private[frontend] object ConsentGridView:
  private val settings = Vector("never" -> "Never Allow",
    "case-by-case" -> "Case-by-case", "always" -> "Always Allow")

  def render(grid: ConsentGrid): dom.Element =
    val section = element("section", "consent-grid")
    section.appendChild(text("h4", "consent-grid-heading",
      "Who may ignore this power"))
    grid.rows.foreach { row =>
      val line = element("div", "consent-grid-row")
      line.setAttribute("data-player-id", row.playerId)
      line.appendChild(text("span", "consent-grid-player", row.name))
      // The three settings stand together under the name, and are named
      // for the player they answer.
      val group = element("div", "consent-grid-choices")
      group.setAttribute("role", "group")
      group.setAttribute("aria-label", row.name)
      val choices = settings.map { (key, label) =>
        val choice = button(label, "consent-grid-choice")
        choice.setAttribute("data-setting", key)
        choice.setAttribute("aria-pressed", (key == row.setting).toString)
        choice.disabled = grid.choose.isEmpty
        key -> choice
      }
      choices.foreach { (key, choice) =>
        choice.onclick = _ =>
          if choice.getAttribute("aria-pressed") != "true" then
            choices.foreach((_, other) => other.setAttribute("aria-pressed",
              (other eq choice).toString))
            grid.choose.foreach(_(row.playerId, key))
        group.appendChild(choice)
      }
      line.appendChild(group)
      section.appendChild(line)
    }
    section
```

Only the doc comment, the heading's words, the `group` element and the
two `appendChild` targets differ from the code at `94fcbb6d1`.

- [ ] **Step 5: Run the consent overlay suites to verify they pass**

Run:
`./sbtw "frontend/testOnly distantlands.frontend.SitePowerGridSuite distantlands.frontend.BannerConsentGridSuite distantlands.frontend.BoardCardConsentGridSuite distantlands.frontend.ChronicleDetailsOverlaySuite distantlands.frontend.ConsentPromptPanelSuite"`
Expected: all pass, with no test changed outside `SitePowerGridSuite`.

- [ ] **Step 6: Commit**

```bash
git add frontend/src/main/scala/distantlands/frontend/SitePowerLinks.scala \
  frontend/src/test/scala/distantlands/frontend/SitePowerGridSuite.scala
git commit -m "feat(frontend): head the consent rows with what the granter consents to

The column's heading reads \"Who may ignore this power\". A row's three
settings now stand in one group under the player's name, named for that
player, so the stylesheet can lay them in thirds.

Co-Authored-By: Claude Fable 5.2 <noreply@anthropic.com>"
git log -3
```

---

### Task 2: The row's styles

**Files:**
- Modify: `frontend/styles.css` (the `.consent-grid*` rules near line 2326)
- Test: `src/test/scala/distantlands/server/StylesheetConsentGridSuite.scala` (create)

**Interfaces:**
- Consumes: Task 1's tree: `.consent-grid` > `.consent-grid-row` >
  `.consent-grid-player` and `.consent-grid-choices` >
  `.consent-grid-choice`.
- Produces: the rules `.consent-grid`, `.consent-grid-row`,
  `.consent-grid-player`, `.consent-grid-choices` and
  `.consent-grid-choices .consent-grid-choice`. Slice 3's requests list
  is laid out beside them.

- [ ] **Step 1: Write the failing suite**

Create `src/test/scala/distantlands/server/StylesheetConsentGridSuite.scala`:

```scala
package distantlands.server

import java.nio.file.{Files, Path}

/** The consent column's preference rows (reconsider design, slice 1): a
  * player's name on its own line, and that player's three settings below
  * it in equal thirds, so no setting wraps alone. */
class StylesheetConsentGridSuite extends munit.FunSuite:
  private val css = Files.readString(Path.of("frontend", "styles.css"))

  /** The declarations of the first rule whose selector is `selector`. */
  private def rule(selector: String)(using munit.Location): String =
    val at = css.indexOf(s"$selector {")
    assert(at >= 0, s"no rule $selector")
    css.substring(at, css.indexOf("}", at))

  /** The stylesheet's consent rules: from their opening comment to the
    * overlay's close button, which follows them. */
  private def consentRules(using munit.Location): String =
    val from = css.indexOf("/* The consent confirmation")
    val to = css.indexOf(".card-overlay-close {", from)
    assert(from >= 0 && to > from, "the consent rules were not found")
    css.substring(from, to)

  test("a row stacks the player's name over the settings"):
    val row = rule(".consent-grid-row")
    assert(row.contains("display: grid;"), row)
    assert(!row.contains("flex"), row)

  test("the three settings share one line in equal thirds"):
    val settings = rule(".consent-grid-choices")
    assert(settings.contains("display: grid;"), settings)
    assert(settings.contains(
      "grid-template-columns: repeat(3, minmax(0, 1fr));"), settings)
    assert(settings.contains("gap: 6px;"), settings)

  test("a setting keeps the compact Control face, with the side padding " +
      "a third leaves room for at 360px"):
    assert(consentRules.contains(
      ".overlay-confirm button, .consent-grid-choice { min-width: 32px;\n" +
      "  min-height: 30px; padding: 4px 9px;"), consentRules)
    assert(rule(".consent-grid-choices .consent-grid-choice")
      .contains("padding: 4px 6px;"))

  test("rows stand further apart than a name stands from its settings"):
    assert(rule(".consent-grid").contains("gap: 12px;"))
    assert(rule(".consent-grid-row").contains("gap: 4px;"))

  test("the column never outgrows the overlay, and a long name wraps"):
    assert(rule(".consent-grid")
      .contains("grid-template-columns: minmax(0, 1fr);"))
    assert(rule(".consent-grid-player").contains("overflow-wrap: anywhere;"))

  test("the column reads at the details column's size"):
    assert(rule(".card-overlay-details").contains("font-size: 0.82em;"))
    assert(rule(".consent-grid").contains("font-size: 0.82em;"))

  test("the pressed setting is the column's one selected thing"):
    val pressed = rule(".consent-grid-choice[aria-pressed=\"true\"]")
    assert(pressed.contains("border-color: var(--selection);"), pressed)
    assert(pressed.contains("background: var(--selection-fill);"), pressed)
    assert(!consentRules.contains("consent-amber"))

  test("a finger gets a 44px setting"):
    val coarse = css.substring(css.indexOf("@media (pointer: coarse) {"))
    assert(coarse.contains(
      ".consent-grid-choice { min-width: 44px; min-height: 44px; }"))

  test("the consent rules move nothing"):
    assert(!consentRules.contains("transition"))
    assert(!consentRules.contains("animation"))
```

- [ ] **Step 2: Run the suite to verify it fails**

Run: `./sbtw "testOnly distantlands.server.StylesheetConsentGridSuite"`
Expected: six failures and three passes.
- Failing: "a row stacks the player's name over the settings" (the row
  is `display: flex` today); "the three settings share one line in equal
  thirds" and "a setting keeps the compact Control face..." (no such
  rules); "rows stand further apart..." (the column's gap is 8px); "the
  column never outgrows the overlay..." and "the column reads at the
  details column's size" (no such declarations).
- Passing: the pressed setting, the 44px setting and the no-motion test.
  They pin what the slice must keep.

- [ ] **Step 3: Write the styles**

In `frontend/styles.css`, replace these four rules

```css
.consent-grid { margin-top: 16px; display: grid; gap: 8px; }
.consent-grid-heading { margin: 0; color: var(--brass-label); }
.consent-grid-row { display: flex; flex-wrap: wrap; align-items: center;
  gap: 6px; }
.consent-grid-player { flex: 1 1 6rem; min-width: 0; }
```

with

```css
.consent-grid { margin-top: 16px; display: grid;
  grid-template-columns: minmax(0, 1fr); gap: 12px; font-size: 0.82em; }
.consent-grid-heading { margin: 0; color: var(--brass-label); }
/* A row is a player's name over that player's three settings (reconsider
   design, slice 1). The settings are equal thirds of the column, so the
   three always share a line. A third sets a setting's width, so its side
   padding is 6px, which keeps every label on one line at 360px. */
.consent-grid-row { display: grid; gap: 4px; }
.consent-grid-player { overflow-wrap: anywhere; }
.consent-grid-choices { display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 6px; }
```

and add this rule directly after the rule
`.consent-grid-choice[aria-pressed="true"] { ... }`:

```css
.consent-grid-choices .consent-grid-choice { padding: 4px 6px; }
```

Leave the shared rule `.overlay-confirm button, .consent-grid-choice`,
its hover and focus rules and the `@media (pointer: coarse)` rule as they
are.

- [ ] **Step 4: Run the stylesheet suites to verify they pass**

Run:
`./sbtw "testOnly distantlands.server.StylesheetConsentGridSuite distantlands.server.StylesheetChronicleDetailsSuite distantlands.server.StylesheetTableEdgeSuite distantlands.server.StylesheetProportionsSuite"`
Expected: all pass.

- [ ] **Step 5: Commit**

```bash
git add frontend/styles.css \
  src/test/scala/distantlands/server/StylesheetConsentGridSuite.scala
git commit -m "style(consent): lay a preference row's settings in equal thirds

A row was one wrapping line, so at a narrow width the third setting
dropped to a line of its own. The player's name now has its own line and
the three settings share the next in equal thirds. The column reads at
the details column's size.

Co-Authored-By: Claude Fable 5.2 <noreply@anthropic.com>"
git log -3
```

---

### Task 3: The design system and the spec's status

**Files:**
- Modify: `DESIGN.md` (new section between "Hover Note" and
  "Inputs / Fields")
- Modify: `docs/superpowers/specs/2026-11/2026-11-20-consent-reconsider-design.md:3-5`
  (status)

- [ ] **Step 1: Describe the column in `DESIGN.md`**

Insert this section directly before the line `### Inputs / Fields`:

```markdown
### Consent Column
A power the viewer rules opens in the card overlay with their consent
column after the details (consent design, ruling 16; reconsider design).
It reads at the details column's size, `0.82em`.
- **Heading:** "Who may ignore this power", in Brass Label, 16px under
  what stands above it and 12px over the first row.
- **Preference row:** one per other player, 12px apart. The player's name
  has its own line. Under it, 4px down, stand that player's three
  settings, "Never Allow", "Case-by-case" and "Always Allow", in equal
  thirds of the column, 6px apart, so the three always share one line,
  at 360px too. The three are one group, named for the player.
- **Setting:** the compact Control face. In thirds it takes 6px of side
  padding, because the third sets its width. The setting in force is
  pressed (`aria-pressed="true"`): a Selection border on Selection Fill,
  the column's one selected thing. Without control the settings are
  disabled. Where the main pointer is coarse they grow to 44px.
- The column uses no amber: consent amber stays on the option a player
  may ask about.
```

- [ ] **Step 2: Update the spec's status line**

In `docs/superpowers/specs/2026-11/2026-11-20-consent-reconsider-design.md`
replace

```markdown
**Status:** designed 2026-11-20. It extends the
[consent system](../2026-10/2026-10-08-consent-system-design.md) and amends
its rulings 12 and 13. Not implemented.
```

with

```markdown
**Status:** designed 2026-11-20. It extends the
[consent system](../2026-10/2026-10-08-consent-system-design.md) and amends
its rulings 12 and 13. Slice 1 is implemented
([plan](../../plans/2026-11/2026-11-20-consent-reconsider-slice1.md));
slices 2 and 3 are not.
```

- [ ] **Step 3: Check the links and commit**

Run: `python3 scripts/check-markdown-links.py`
Expected: passed.

```bash
git add DESIGN.md \
  docs/superpowers/specs/2026-11/2026-11-20-consent-reconsider-design.md
git commit -m "docs(design): describe the consent column's preference rows

Co-Authored-By: Claude Fable 5.2 <noreply@anthropic.com>"
git log -3
```

---

### Task 4: The full gate and the browser check

- [ ] **Step 1: Run the full gate**

Run, in the background or with a long timeout:
`./sbtw test`, `./sbtw frontend/test`,
`python3 scripts/check-architecture.py`,
`python3 scripts/check-markdown-links.py`.
Expected: all pass. Report the test counts sbt prints.

- [ ] **Step 2: Start a scratch server**

`$DB` is a new, empty folder under
`/private/tmp/claude-501/-Users-roman-projects-distantlands/904ab515-4150-48ab-97c4-45a98d0b15e6/scratchpad`.
Never point the server at `var/distantlands` or `var/oathdigital`. Never
use `preview_start`. From the worktree, the build first and then the
server in the background:

```bash
./sbtw frontend/fastLinkJS
./sbtw "runMain distantlands.server.DistantLandsServer --port 8081 --database-path $DB/distantlands"
```

Wait until
`curl -s -o /dev/null -w "%{http_code}" http://localhost:8081/health/ready`
prints `200`.

- [ ] **Step 3: Reach a consent column**

Open `http://localhost:8081/` with the built-in browser tools, choose
New Chronicle and create a game. The column shows only to a player who
rules a waivable power (`ConsentPowers.ruledBy`). For each seat, load the
table with that seat's `playerId` and read `consentPreferences` in the
snapshot the page polls. Take the first route that works:

1. A seat's list is not empty in the opening position: open that power.
2. No seat's list has an entry: play ordinary moves, at most one round,
   until a player rules a site or card with a waivable power (the Narrow
   Pass, the Forbidden Forest, Passages, Secret Police, the Hall of
   Ministers, Ancient Bloodline, Relic Patrols, or the Darkest Secret's
   Festival for its holder).

If neither route shows the column, stop. Say plainly in the report that
the browser check was not made and why; do not claim it.

- [ ] **Step 4: Check the column once at each width**

At 1440×900, then at a 360×800 window, with the overlay open:

1. Read the overlay's text. The heading reads "Who may ignore this
   power", and each row reads the player's name and the three settings.
2. Measure, with one script in the page, for every `.consent-grid-row`:
   the name's box, and each `.consent-grid-choice`'s `left`, `top`,
   `width` and `height`, its `scrollWidth` against its `clientWidth`, and
   its computed `padding` and `font-size`. Also read
   `document.documentElement.scrollWidth` against `innerWidth`, and the
   `.card-overlay-body`'s `scrollWidth` against its `clientWidth`.
   Expected: the name's bottom is above the settings' top; the three
   settings share one `top`, have equal widths within 1px and equal
   heights (30px, or 44px where the window reports a coarse pointer); no
   label is cut or wrapped; neither the page nor the overlay's body
   scrolls sideways. At 360px also read the computed font size of a name
   and of a setting: each is 11px or more (R1). A name or a setting
   under 11px, or a "Case-by-case" that clips or wraps (R3), stops the
   work: report the measured sizes and widths and change nothing.
3. Click a setting that is not pressed: the pressed state moves, and the
   viewer's log shows the preference line. Tab to a setting: the Cream
   Focus outline shows.
4. Take a screenshot at each width. Give each file's full path in the
   report when the tool saves one; otherwise give the measured numbers
   and the overlay's text.

This is one bounded pass. Fix what it finds as one batch of test-first
commits, then run at most one confirming round. The app has one theme;
say so in the report.

- [ ] **Step 5: Stop the server**

```bash
pkill -f "DistantLandsServer --port 8081"
```

Confirm with `curl` that port 8081 no longer answers.

---

## Self-Review

- **Spec coverage.** The slice is the spec's "Preference rows" paragraph
  and slice 1.
  - "The heading reads 'Who may ignore this power'": Task 1.
  - "Each row shows the player's name on its own line and the three
    settings below it": Task 1 (structure) and Task 2 (the stacked row).
  - "in equal thirds, so no button wraps alone": Task 2.
  - "This holds at 360px": Task 2's padding and Task 4's measurement.
  - "Frontend" testing item "The preference row's structure and heading":
    Task 1's tests. "Browser. The overlay at desktop width and at
    360px, once, after the suites pass": Task 4.
  - `DESIGN.md` followed and updated: Tasks 2 and 3.
  - Not in this slice: the requests list, its live redraw and the prompt
    notice (slice 3); the ledger, the event, the intent, the desk and the
    log lines (slice 2); the amendment of rulings 12 and 13 in the
    consent design (when slice 2 lands).
- **Placeholder scan.** Every code step shows its code, and every docs
  step shows its text. Task 4 Step 3 names two routes and a stop rule,
  because which seat rules a waivable power is read at run time.
- **Type consistency.** `ConsentGridView.render(grid: ConsentGrid):
  dom.Element` keeps its signature. The class names
  `consent-grid-choices` (the new group) and `consent-grid-choice` (a
  setting) are spelled the same in Task 1's code and tests, Task 2's
  rules and suite, and Task 3's text.
- **Found and fixed in review.** Task 2 Step 2 first counted five
  failures where its list has six; it now says six failures and three
  passes.
