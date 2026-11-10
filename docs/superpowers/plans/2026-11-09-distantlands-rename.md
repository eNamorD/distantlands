# Rename to Distant Lands Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Rename the project from Oath Digital (`oathdigital`) to Distant
Lands (`distantlands`) across code, build, runtime names and docs, as a
hard cut.

**Architecture:** Six content commits on branch `rename-distantlands`,
each green on its own: a pure folder move, a mechanical package rewrite,
the server type, the runtime names, the build and remaining identifiers,
then UI text and docs. The rewrites are `perl -pi` substitutions over
`git grep` file lists, run in a fixed order. A last commit lists the
package-rewrite commit in `.git-blame-ignore-revs`.

**Tech Stack:** Scala 3, Scala.js, sbt via `./sbtw`, munit, jsdom,
sbt-native-packager (Universal and Docker), HSQLDB, POSIX sh, Python 3
check scripts.

**Spec:** [Rename design](../specs/2026-11-09-distantlands-rename-design.md).
Read its name map and "Unchanged" list before every task.

## Global Constraints

- Brand text is `Distant Lands`; identifiers are `distantlands`,
  `DistantLands` where the platform capitalises, and environment
  variables use the prefix `DLQ_`.
- Text that means the board game keeps "Oath": the host heading becomes
  `Create an Oath game`.
- Hard cut: no fallback that reads an old name, no startup check for old
  names, no host migration guide.
- Never rename: `OathState`, `OathEvent`, `OathViolation`,
  `OathTransition`, `OathRules`, `OathRulesWalker`, `OathDriver`,
  `OathGame`, `Oathkeeper*`, `OathsOfLoyaltyCard`, IDs such as
  `chronicle.swear-oath`, the README attribution notice.
- Never edit: `docs/operations/release-notes/0.1.0-alpha.*.md`, the spec,
  this plan.
- sbt organization is `quest.distantlands`; cookie is `distantlands_seat`;
  storage keys start `distantlands.`.
- Every commit passes the gate below.

**The gate** (run from the repository root; `npm ci` once first if
`node_modules` is missing):

```sh
./sbtw compile test frontend/test
python3 scripts/check-architecture.py
python3 scripts/check-markdown-links.py
git diff --check
```

**Exclusions** used by every rewrite (define once per shell):

```sh
SPEC=':!docs/superpowers/specs/2026-11-09-distantlands-rename-design.md'
PLAN=':!docs/superpowers/plans/2026-11-09-distantlands-rename.md'
NOTES=':!docs/operations/release-notes/0.1.0-alpha.*'
SCRATCH=${SCRATCH:-$(mktemp -d)}   # outside the repository; reuse one directory for the whole plan
```

`git grep -lzI` lists text files only (binary fonts are skipped) and
`xargs -0 -r` does nothing when the list is empty.

---

### Task 1: Move the Scala source folders

**Files:**
- Move: `{src,shared/src,frontend/src}/{main,test}/scala/oathdigital/` to
  `…/distantlands/`
- Modify: `build.sbt:269-271` (frontend's list of shared root sources),
  `scripts/check-architecture.py`, `src/test/scala/…/gameplay/BackendArchitectureSuite.scala`,
  `src/test/scala/…/application/PendingWalkerInvariantSuite.scala`,
  comments and docs that cite source paths

**Interfaces:**
- Produces: every Scala source lives under `…/scala/distantlands/`.
  Package declarations still say `oathdigital` until Task 2.

- [ ] **Step 1: Run the gate on the untouched branch**

Run the gate. Expected: PASS. A failure here is not this plan's; stop
and report it.

- [ ] **Step 2: Move the folders**

```sh
for d in src/main/scala src/test/scala shared/src/main/scala shared/src/test/scala frontend/src/main/scala frontend/src/test/scala; do
  git mv "$d/oathdigital" "$d/distantlands"
done
```

- [ ] **Step 3: Rewrite strings that name source paths**

```sh
git grep -lzIP 'scala/oathdigital(?!\w)' -- . "$SPEC" "$PLAN" "$NOTES" |
  xargs -0 -r perl -pi -e 's#scala/oathdigital(?!\w)#scala/distantlands#g'
perl -pi -e 's#shared / "oathdigital" / "model"#shared / "distantlands" / "model"#g' build.sbt
```

- [ ] **Step 4: Check nothing still names the old folders**

```sh
git grep -nIP 'scala/oathdigital(?!\w)|shared / "oathdigital"' -- . "$SPEC" "$PLAN" "$NOTES"
```

Expected: no output.

- [ ] **Step 5: Run the gate**

Expected: PASS. The architecture script and the path-reading suites now
read the moved folders.

- [ ] **Step 6: Commit**

```sh
git add -A
git commit -m "refactor: move Scala sources from oathdigital/ to distantlands/"
```

---

### Task 2: Rewrite the package to `distantlands`

**Files:**
- Move: `src/main/resources/META-INF/services/oathdigital.application.GameEventCodec`,
  `src/main/resources/META-INF/services/oathdigital.application.chronicle.ChronicleJson`,
  `src/test/resources/oathdigital/` to their `distantlands` names
- Modify: every `.scala` file (package and import lines), `build.sbt`
  (`mainClass`, the `notes.strict` property, the resource generator's
  `"oathdigital" / "frontend"`), `src/main/resources/application.conf`,
  `ProductionFrontendRoutes.scala`, `ProductionFrontendRoutesSuite.scala`,
  `NoteCheck.scala`

**Interfaces:**
- Consumes: Task 1's folders.
- Produces: package root `distantlands`; ServiceLoader files
  `META-INF/services/distantlands.application.GameEventCodec` and
  `…/distantlands.application.chronicle.ChronicleJson` naming
  `distantlands.serialization.GameEventCodecAdapter` and
  `distantlands.serialization.ChronicleJsonCodec`; classpath frontend at
  `distantlands/frontend/`; config key `distantlands.blocking-dispatcher`;
  system property `distantlands.notes.strict`; main class
  `distantlands.server.OathServer` (renamed in Task 3).

This commit holds code identity only. The pattern skips
`oathdigital.properties`, `oathdigital.bat` and `oathdigital.sh`, and
anything after `.`, `-` or a word character (such as `dev.oathdigital`
and `start-oathdigital.sh`); later tasks own those.

- [ ] **Step 1: Move the resource files**

```sh
git mv src/main/resources/META-INF/services/oathdigital.application.GameEventCodec \
  src/main/resources/META-INF/services/distantlands.application.GameEventCodec
git mv src/main/resources/META-INF/services/oathdigital.application.chronicle.ChronicleJson \
  src/main/resources/META-INF/services/distantlands.application.chronicle.ChronicleJson
git mv src/test/resources/oathdigital src/test/resources/distantlands
```

- [ ] **Step 2: Rewrite qualified names and the classpath path**

```sh
PKG='(?<![\w.-])oathdigital\.(?!(?:properties|bat|sh)\b)'
git grep -lzIP "$PKG" -- . ':!docs' ':!*.md' ':!vendor' ':!reference' |
  xargs -0 -r perl -pi -e "s/$PKG/distantlands./g"
git grep -lzI 'oathdigital/frontend' -- . ':!docs' ':!*.md' |
  xargs -0 -r perl -pi -e 's#oathdigital/frontend#distantlands/frontend#g'
perl -pi -e 's#"oathdigital" / "frontend"#"distantlands" / "frontend"#' build.sbt
```

- [ ] **Step 3: Check the result**

```sh
git grep -nIP "$PKG" -- . ':!docs' ':!*.md' ':!vendor' ':!reference'
git grep -nI 'oathdigital/frontend' -- . ':!docs' ':!*.md'
cat src/main/resources/META-INF/services/*
grep -n 'blocking-dispatcher' src/main/resources/application.conf src/main/scala/distantlands/server/OathServer.scala
```

Expected: the first two print nothing. The service files print
`distantlands.serialization.GameEventCodecAdapter` and
`distantlands.serialization.ChronicleJsonCodec`. Both dispatcher lines
say `distantlands.blocking-dispatcher`.

- [ ] **Step 4: Run the gate, plus the production bundle**

Run the gate, then `./sbtw frontend/fullOptJS Compile/resources`.
Expected: PASS. `ProductionFrontendRoutesSuite` proves the classpath path,
and the chronicle and event codec suites prove the ServiceLoader files
(a wrong file name fails them with no provider found).

- [ ] **Step 5: Commit**

```sh
git add -A
git commit -m "refactor: rename the Scala package from oathdigital to distantlands"
```

---

### Task 3: Rename `OathServer` to `DistantLandsServer`

**Files:**
- Move: `src/main/scala/distantlands/server/OathServer.scala` to
  `src/main/scala/distantlands/server/DistantLandsServer.scala`
- Modify: `build.sbt` (`mainClass`), any file that names `OathServer`

**Interfaces:**
- Produces: `object DistantLandsServer` with the same members;
  `Compile / mainClass := Some("distantlands.server.DistantLandsServer")`.

- [ ] **Step 1: Move and rename**

```sh
git mv src/main/scala/distantlands/server/OathServer.scala \
  src/main/scala/distantlands/server/DistantLandsServer.scala
git grep -lzIw OathServer -- . ':!docs' ':!*.md' |
  xargs -0 -r perl -pi -e 's/\bOathServer\b/DistantLandsServer/g'
```

- [ ] **Step 2: Check**

Run: `git grep -nIw OathServer -- . ':!docs' ':!*.md'`
Expected: no output. `README.md` still names it; Task 6 rewrites it.

- [ ] **Step 3: Run the gate, then `./sbtw "show Compile/mainClass"`**

Expected: PASS, and `Some(distantlands.server.DistantLandsServer)`.

- [ ] **Step 4: Commit**

```sh
git add -A
git commit -m "refactor: rename OathServer to DistantLandsServer"
```

---

### Task 4: Rename the runtime names

**Files:**
- Modify (main): `ServerConfig.scala` (`DefaultDatabasePath`, usage line),
  `DesktopPaths.scala`, `DesktopSettings.scala`, `DesktopLaunchProfile.scala`,
  `DesktopConsole.scala`, `DistantLandsServer.scala`, `GameApiRoutes.scala`
  (`cookieName`), `HsqldbDatabaseOwner.scala` (pool name),
  `ChronicleShelf.scala`, `ChronicleStorage.scala`, `LogMarker.scala`,
  `SidebarDivider.scala`, `project/ReleaseVersion.scala`, `build.sbt`,
  `packaging/desktop/*`, `scripts/*.sh`, `scripts/*.ps1`,
  `.github/workflows/alpha-release.yml`
- Test: `ServerConfigSuite`, `DesktopPathsSuite`, `DesktopSettingsSuite`,
  `DesktopLaunchProfileSuite`, `DesktopConsoleSuite`, `TrustedSeatRoutesSuite`,
  `testkit/HttpTestServer.scala`, `BrowserChronicleStorageSuite`,
  `ChronicleStorageContract`, `GameLogOverlaySuite`, `LogMarkerSuite`,
  `LogStoreContract`, `PhonePaneSwitchSuite`

**Interfaces:**
- Produces: environment variables `DLQ_*`; default database path
  `var/distantlands`; desktop data folder `DistantLands` (macOS, Windows)
  and `distantlands` (Linux); settings file `distantlands.properties`;
  cookie `distantlands_seat`; storage keys `distantlands.chronicles`,
  `distantlands.chronicles.loaded`, `distantlands.chronicles.probe`,
  `distantlands.log.seen.<game>.<seat>`, `distantlands.table.sidebar-width`;
  actor system `distantlands-server`; pool `distantlands-database`; CLI
  prefix `distantlands: `; console text `Distant Lands`.

The tests change first and must fail against the old production names.

- [ ] **Step 1: Write the substitution scripts**

Save as `$SCRATCH/runtime.pl`:

```perl
s/\bOATH_/DLQ_/g;
s/oath_seat/distantlands_seat/g;
s/(?<=")oath\.(?=chronicles|log\.|table\.)/distantlands./g;
s#/var/lib/oathdigital#/var/lib/distantlands#g;
s#var/oathdigital#var/distantlands#g;
s/OathDigitalData/DistantLandsData/g;
s/"OathDigital"/"DistantLands"/g;
s/oathdigital\.properties/distantlands.properties/g;
s/oathdigital-(server|database)/distantlands-$1/g;
s/oathdigital: /distantlands: /g;
s/Usage: oathdigital/Usage: distantlands/g;
```

Save as `$SCRATCH/brand.pl`, for server-side console text in Scala:

```perl
s/\ban Oath Digital\b/a Distant Lands/g;
s/\bAn Oath Digital\b/A Distant Lands/g;
s/Oath Digital's/Distant Lands'/g;
s/Oath Digital/Distant Lands/g;
```

`RUNTIME` below is the pattern that finds files for `runtime.pl`:

```sh
RUNTIME='\bOATH_|oath_seat|"oath\.|var/oathdigital|OathDigital|oathdigital\.properties|oathdigital-(server|database)|oathdigital: |Usage: oathdigital'
```

- [ ] **Step 2: Apply them to the tests only**

```sh
git grep -lzIP "$RUNTIME" -- src/test shared/src/test frontend/src/test |
  xargs -0 -r perl -pi "$SCRATCH/runtime.pl"
git grep -lzI 'Oath Digital' -- src/test/scala |
  xargs -0 -r perl -pi "$SCRATCH/brand.pl"
perl -pi -e 's/oathdigital/distantlands/g; s/OathDigital/DistantLands/g' \
  src/test/scala/distantlands/server/DesktopPathsSuite.scala
```

- [ ] **Step 3: Run the tests and watch them fail**

Run: `./sbtw test frontend/test`
Expected: FAIL. At least `ServerConfigSuite`, `DesktopPathsSuite`,
`DesktopSettingsSuite`, `DesktopConsoleSuite` and `LogMarkerSuite` fail,
because they now expect the new names. Record the failing suites. A suite
that passes either takes its key as a parameter (the storage contracts)
or does not pin the name; both are fine.

- [ ] **Step 4: Apply it to everything else outside the docs**

```sh
git grep -lzIP "$RUNTIME" -- . ':!docs' ':!*.md' ':!vendor' ':!reference' ':!.impeccable' |
  xargs -0 -r perl -pi "$SCRATCH/runtime.pl"
git grep -lzI 'Oath Digital' -- src/main/scala |
  xargs -0 -r perl -pi "$SCRATCH/brand.pl"
perl -pi -e 's/oathdigital/distantlands/g; s/OathDigital/DistantLands/g' \
  src/main/scala/distantlands/server/DesktopPaths.scala
```

`src/main/scala` is the server only; the frontend's UI text is Task 6's,
and build, launcher and script text is Task 5's.

- [ ] **Step 5: Check the runtime names are gone outside the docs**

```sh
git grep -nIP '\bOATH_|oath_seat|"oath\.|var/oathdigital|/var/lib/oathdigital|OathDigital|oathdigital\.properties|oathdigital-(server|database)|oathdigital: ' \
  -- . ':!docs' ':!*.md' ':!vendor' ':!reference' ':!.impeccable'
```

Expected: no output.

- [ ] **Step 6: Run the gate**

Expected: PASS, including every suite that failed in Step 3.

- [ ] **Step 7: Commit**

```sh
git add -A
git commit -m "feat!: rename runtime names to Distant Lands

Environment variables use DLQ_ instead of OATH_. The default database
path, desktop data folder, settings file, seat cookie and browser storage
keys use distantlands. Nothing reads the old names."
```

---

### Task 5: Build, packaging, CI and remaining identifiers

**Files:**
- Move: `packaging/desktop/Start Oath Digital.command`,
  `packaging/desktop/Start Oath Digital.bat`,
  `packaging/desktop/start-oathdigital.sh`
- Modify: `build.sbt`, `packaging/desktop/*`, `scripts/package-bundled-runtime.sh`,
  `scripts/smoke-*.sh`, `scripts/smoke-bundled-distribution.ps1`,
  `scripts/verify-alpha-release.sh`, `.github/workflows/alpha-release.yml`,
  `package.json`, `package-lock.json`, test temp-directory prefixes such as
  `GameApplicationServiceReopenSuite` and `MembershipAuthorizationServiceSuite`

**Interfaces:**
- Produces: organization `quest.distantlands`; modules
  `distantlands-engine`, `distantlands-frontend`; archive
  `distantlands-<version>`; executable `bin/distantlands`; `share/distantlands/`;
  Docker image and user `distantlands`; launchers `Start Distant Lands.command`,
  `Start Distant Lands.bat`, `start-distantlands.sh`; npm package
  `distantlands-frontend-tests`.

- [ ] **Step 1: Move the launchers**

```sh
git mv "packaging/desktop/Start Oath Digital.command" "packaging/desktop/Start Distant Lands.command"
git mv "packaging/desktop/Start Oath Digital.bat" "packaging/desktop/Start Distant Lands.bat"
git mv packaging/desktop/start-oathdigital.sh packaging/desktop/start-distantlands.sh
```

`git mv` keeps the executable bit, and `.gitattributes` (`*.bat text
eol=crlf`) still matches the `.bat` file.

- [ ] **Step 2: Rewrite every remaining name outside the docs and UI text**

```sh
UI=(':!frontend/index.html' ':!frontend/production-index.html'
  ':!frontend/src/main/scala/distantlands/frontend/ChroniclePage.scala'
  ':!frontend/src/main/scala/distantlands/frontend/TrustedHostUi.scala')
git grep -lzIiE 'oath ?digital|oathdigital' \
  -- . ':!docs' ':!*.md' ':!vendor' ':!reference' ':!.impeccable' "${UI[@]}" |
  xargs -0 -r perl -pi -e '
    s/dev\.oathdigital/quest.distantlands/g;
    s/\ban Oath Digital\b/a Distant Lands/g;
    s/\bAn Oath Digital\b/A Distant Lands/g;
    s/Oath Digital'"'"'s/Distant Lands'"'"'/g;
    s/Oath Digital/Distant Lands/g;
    s/OathDigital/DistantLands/g;
    s/oathdigital/distantlands/g;'
```

Run this in `bash` or `zsh`; both support the `UI` array.

- [ ] **Step 3: Check**

```sh
git grep -nIiE 'oath ?digital|oathdigital' -- . ':!docs' ':!*.md' ':!vendor' ':!reference' ':!.impeccable'
grep -n 'organization\|packageName\|executableScriptName\|daemonUser\|maintainer' build.sbt
```

Expected: the first command prints only lines in the four UI files.
`build.sbt` shows `quest.distantlands`, `distantlands-${version.value}`,
`distantlands`, and `Distant Lands`.

- [ ] **Step 4: Run the gate**

Expected: PASS.

- [ ] **Step 5: Build the packages**

```sh
./sbtw Universal/packageZipTarball Docker/stage
sh scripts/verify-alpha-release.sh self-test
ls target/universal/distantlands-*.tgz
tar -tzf target/universal/distantlands-*.tgz | grep -E 'bin/distantlands$|Start Distant Lands|start-distantlands.sh|lib/quest\.distantlands\.'
```

Expected: both sbt tasks pass `verifyPackageMappings`, the self-test
passes, and `tar` lists the executable, all three launchers and the
`quest.distantlands` jars.

- [ ] **Step 6: Smoke the bundled distribution**

This needs Java 21. Find it with `/usr/libexec/java_home -v 21`. If no
Java 21 exists, skip this step and record it as not run in the PR.

```sh
J21=$(/usr/libexec/java_home -v 21)
JAVA_HOME="$J21" PATH="$J21/bin:$PATH" sh scripts/verify-alpha-release.sh bundled \
  "$(ls target/universal/distantlands-*.tgz)" macos-arm64 "$SCRATCH/bundled"
```

Expected: the bundled archive builds and its smoke test passes. If
`docker info` succeeds, also run `./sbtw smokeContainer`. Expected: PASS.

- [ ] **Step 7: Commit**

```sh
git add -A
git commit -m "build: rename packages, launchers and CI artifacts to distantlands"
```

---

### Task 6: UI text and docs

**Files:**
- Modify: `frontend/index.html`, `frontend/production-index.html`,
  `frontend/src/main/scala/distantlands/frontend/ChroniclePage.scala`,
  `frontend/src/main/scala/distantlands/frontend/TrustedHostUi.scala`,
  every `*.md` except the spec, this plan and
  `docs/operations/release-notes/0.1.0-alpha.*.md`,
  `.impeccable/design.json`,
  `reference/catalog-ingestion/archive/ingestion-component-catalog.schema.json`

**Interfaces:**
- Consumes: every name from Tasks 2 to 5.

- [ ] **Step 1: Apply the naming rule to the UI**

```sh
perl -pi -e 's/Create an Oath Digital game/Create an Oath game/g' \
  frontend/src/main/scala/distantlands/frontend/TrustedHostUi.scala
perl -pi -e 's/Oath Digital/Distant Lands/g' \
  frontend/index.html frontend/production-index.html \
  frontend/src/main/scala/distantlands/frontend/ChroniclePage.scala
```

- [ ] **Step 2: Rewrite the docs**

```sh
git grep -lzIiE 'oath ?digital|oathdigital|OATH_|oath_seat|oath\.(chronicles|log|table)|OathServer' \
  -- '*.md' .impeccable/design.json reference/catalog-ingestion/archive/ingestion-component-catalog.schema.json \
     "$SPEC" "$PLAN" "$NOTES" |
  xargs -0 -r perl -pi -e '
    s/Create an Oath Digital game/Create an Oath game/g;
    s/\ban Oath Digital\b/a Distant Lands/g;
    s/\bAn Oath Digital\b/A Distant Lands/g;
    s/dev\.oathdigital/quest.distantlands/g;
    s/\bOathServer\b/DistantLandsServer/g;
    s/OathDigital/DistantLands/g;
    s/Oath Digital'"'"'s/Distant Lands'"'"'/g;
    s/Oath Digital/Distant Lands/g;
    s/oath_seat/distantlands_seat/g;
    s/\bOATH_/DLQ_/g;
    s/(?<![\w.])oath\.(?=chronicles|log\.seen|table\.sidebar)/distantlands./g;
    s/oathdigital/distantlands/g;'
```

- [ ] **Step 3: Read the prose changes**

```sh
git diff -U0 -- '*.md' | grep '^+' | grep -n 'Distant Lands' | head -80
git diff -- README.md
```

Fix by hand any sentence the rewrite made awkward. Check that README's
attribution notice still names *Oath: New Foundations*, Cole Wehrle and
Buried Giant Studios.

- [ ] **Step 4: Run the gate**

Expected: PASS.

- [ ] **Step 5: Commit**

```sh
git add -A
git commit -m "docs: rename Oath Digital to Distant Lands"
```

---

### Task 7: Final verification

**Files:** none changed, unless a check finds a miss. Fix a miss in a new
commit named for what it fixes, then rerun this task.

- [ ] **Step 1: Leftover grep**

```sh
git grep -il -E 'oath ?digital|oathdigital|oath-digital|OATH_|oath_seat|"oath\.'
```

Expected: only `docs/operations/release-notes/0.1.0-alpha.*.md`, the spec
and this plan.

- [ ] **Step 2: Start a fresh server**

```sh
./sbtw Universal/stage
mkdir -p "$SCRATCH/e2e/fresh"
DLQ_PORT=18081 DLQ_DATABASE_PATH="$SCRATCH/e2e/fresh/database" \
  target/universal/stage/bin/distantlands
```

Run the server in the background. Expected: it logs `distantlands:`
lines, no `oathdigital` line, and listens on port 18081.

- [ ] **Step 3: Check it in the browser**

Open `http://localhost:18081` in the built-in browser. Check that the
page title and loading text say `Distant Lands` and that the host heading
says `Create an Oath game`. Create a game and open one seat link. Then:

```sh
curl -si "<the seat link>" | grep -i '^set-cookie'
```

Expected: `Set-Cookie: distantlands_seat=…`. In the browser, run
`Object.keys(localStorage)` on the table page. Expected: every key
starts with `distantlands.`, and no key starts with `oath.`. Stop the
server.

- [ ] **Step 4: Open a database the old code made**

Copy the dev database. Never move or edit the original, and leave its
`.lck` file behind. HSQLDB names its files after the last path segment,
so the copies take the new name:

```sh
mkdir -p "$SCRATCH/e2e/old"
for ext in script properties lobs log; do
  [ -f "var/oathdigital.$ext" ] && cp "var/oathdigital.$ext" "$SCRATCH/e2e/old/distantlands.$ext"
done
DLQ_PORT=18082 DLQ_DATABASE_PATH="$SCRATCH/e2e/old/distantlands" \
  target/universal/stage/bin/distantlands
```

Expected: the server starts, and the games the old database holds are
listed at `http://localhost:18082`. If `var/oathdigital.script` does not
exist, record this step as not run. Stop the server.

- [ ] **Step 5: Record results**

Write each check's outcome (passed, failed, or not run with the reason)
for the PR description.

---

### Task 8: Blame-ignore and pull request

**Files:**
- Modify: `.git-blame-ignore-revs`

- [ ] **Step 1: List the package-rewrite commit**

Run this only once the branch history is final; a rebase changes the
hash.

```sh
HASH=$(git log --format=%H --grep='^refactor: rename the Scala package from oathdigital to distantlands$' refs/heads/0.2.0-alpha..HEAD)
printf '\n# Mechanical package rename to distantlands; see docs/superpowers/specs/2026-11-09-distantlands-rename-design.md\n%s\n' "$HASH" >> .git-blame-ignore-revs
cat .git-blame-ignore-revs
```

Expected: one new 40-character hash under the new comment.

- [ ] **Step 2: Commit**

```sh
git add .git-blame-ignore-revs
git commit -m "chore: ignore the package rename in git blame"
```

- [ ] **Step 3: Pull request**

Ask the maintainer before pushing. On a yes, push `rename-distantlands`
and open a PR into `0.2.0-alpha` whose description carries Task 7's
results and the spec's "After merge" steps.
