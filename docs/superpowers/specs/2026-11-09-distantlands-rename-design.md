# Rename: Oath Digital to Distant Lands

**Status:** designed 2026-11-09. Not started.

## Goal and scope

The project's name should not use the word "Oath", out of caution about
the game's trademark. The project becomes **Distant Lands**, served at
`distantlands.quest`. The rename covers the brand, the code and build
identifiers, and every runtime name a host or browser sees.

It is a **hard cut**: the release that carries it reads no old name, moves
no old folder and copies no old browser storage. The project has one
host, the maintainer, and players already accept that alpha releases may
not keep saved games, so the release carries no migration code, no
startup check for old names and no host migration guide. The maintainer's
own cutover is a one-off checklist (below).

Out of scope: the game-model type names (see "Unchanged"), the GitHub
repository rename (the maintainer does it after merge), and writing the
next release's notes (written when that release is cut).

## Naming rule

- **Distant Lands** names this program: page titles, loading text, the
  Chronicle page header, CLI output, release titles, docs.
- **Oath** names the board game the players are playing. Text that means
  the game keeps it: the host heading "Create an Oath game", rules prose,
  card text.
- Identifiers use `distantlands` (lower case, one word), `DistantLands`
  where the platform convention is capitalised, and `DLQ_` for
  environment variables.

## Name map

### Code and build identifiers

| Old | New |
|---|---|
| Scala package `oathdigital.*`, main and test, in `src`, `shared`, `frontend`, and their folders | `distantlands.*` |
| `src/main/resources/META-INF/services/oathdigital.*` (file names and contents) | `distantlands.*` |
| Classpath frontend path `oathdigital/frontend/` (`build.sbt` resource generator, `ProductionFrontendRoutes`, `src/test/resources/oathdigital/frontend/`) | `distantlands/frontend/` |
| `application.conf` key `oathdigital.blocking-dispatcher` and its lookup in the server | `distantlands.blocking-dispatcher` |
| Logger names in `logback.xml` and `LoggerFactory.getLogger("oathdigital…")` | `distantlands…` |
| Dev system property `oathdigital.notes.strict` (`build.sbt`, `NoteCheck`) | `distantlands.notes.strict` |
| `OathServer` and `mainClass` | `DistantLandsServer` |
| sbt organization `dev.oathdigital` (so `lib/dev.oathdigital.*.jar`) | `quest.distantlands` |
| sbt modules `oathdigital-engine`, `oathdigital-frontend`; `maintainer := "Oath Digital"` | `distantlands-engine`, `distantlands-frontend`; `Distant Lands` |
| npm package `oathdigital-frontend-tests` (`package.json`, `package-lock.json`) | `distantlands-frontend-tests` |
| Universal archive `oathdigital-<version>`, executable `bin/oathdigital(.bat)`, `share/oathdigital/` | `distantlands-<version>`, `bin/distantlands(.bat)`, `share/distantlands/` |
| Docker image and daemon user `oathdigital` | `distantlands` |
| Launchers `Start Oath Digital.command`, `Start Oath Digital.bat`, `start-oathdigital.sh` | `Start Distant Lands.command`, `Start Distant Lands.bat`, `start-distantlands.sh` |
| Actor system `oathdigital-server`, HSQLDB pool `oathdigital-database` | `distantlands-server`, `distantlands-database` |
| CLI prefix `oathdigital:` and usage line `Usage: oathdigital …` | `distantlands:`, `Usage: distantlands …` |

### Runtime names

| Old | New |
|---|---|
| Every `OATH_*` environment variable (`PORT`, `HOST`, `DATABASE_PATH`, `PUBLIC_BASE_URL`, `MODE`, `LAUNCH`, `OPEN_BROWSER`, `CATALOG_PATH`, `RELEASE_VERSION`, `IMAGE_REFERENCE`, `SESSION_COOKIE_NAME`, `AUTHENTICATED_PUBLIC_ORIGIN`, `PROT`, `SMOKE_JOURNAL`, `SMOKE_OUT`, and the test-only `UNRELATED`) | `DLQ_*`, same suffix |
| Default database path `var/oathdigital` (`ServerConfig`) | `var/distantlands` |
| Docker data directory and volume `/var/lib/oathdigital` | `/var/lib/distantlands` |
| Desktop data folder: macOS `~/Library/Application Support/OathDigital`, Windows `%LOCALAPPDATA%\OathDigital`, Linux `$XDG_DATA_HOME/oathdigital` or `~/.local/share/oathdigital` | `DistantLands`, `DistantLands`, `distantlands` |
| Desktop settings file `oathdigital.properties`; template comment `/path/to/OathDigitalData/database` | `distantlands.properties`; `/path/to/DistantLandsData/database` |
| Seat cookie `oath_seat` | `distantlands_seat` |
| Browser storage `oath.chronicles`, `oath.chronicles.loaded`, `oath.chronicles.probe`, `oath.log.seen.<game>.<seat>`, `oath.table.sidebar-width` | `distantlands.` with the same suffix |

Saved data needs nothing: game events carry their own wire type tags and
Chronicle JSON carries a numeric `"format"`, and neither holds a package
name. A database moved by hand opens unchanged, within the alpha rule in
[releases](../../operations/releases.md#versions-and-compatibility).

### Unchanged

- Game-model types that name the game being modelled: `OathState`,
  `OathEvent`, `OathViolation`, `OathTransition`, `OathRules`,
  `OathRulesWalker`, `OathDriver`, `OathGame`.
- Rules terms and card content: `Oathkeeper*`, `OathsOfLoyaltyCard`, IDs
  such as `chronicle.swear-oath` and `denizen.true-oath`.
- The README's attribution and non-affiliation notice for *Oath: New
  Foundations* and Buried Giant Studios.
- Published history: git history, tags, GitHub releases, existing ghcr
  images, and the published release notes
  `docs/operations/release-notes/0.1.0-alpha.*.md`, which describe the
  `oathdigital-…` files people downloaded.
- This spec and its implementation plan, which must name the old names.

## Delivery

One branch, `rename-distantlands`, cut from `0.2.0-alpha`, one PR back
into `0.2.0-alpha`. Six commits, then one bookkeeping commit. Every commit
passes, on its own:

```sh
./sbtw compile test frontend/test
python3 scripts/check-architecture.py
python3 scripts/check-markdown-links.py
git diff --check
```

The mechanical commits come from throwaway scripts kept outside the
repository, so the diff is what gets reviewed.

1. **Move source folders.** `git mv` each
   `{src,shared,frontend}/src/{main,test}/scala/oathdigital` to
   `…/distantlands`. Scala does not tie folders to packages, so nothing
   else changes except strings that name source *files*: the shared-source
   list in `build.sbt`, the paths in `scripts/check-architecture.py`, the
   path-reading tests (`BackendArchitectureSuite`,
   `PendingWalkerInvariantSuite`), and comments that cite paths.
2. **Rewrite packages** (listed in `.git-blame-ignore-revs`). `package`,
   `import` and fully qualified `oathdigital.` become `distantlands.`,
   together with the ServiceLoader files, the classpath frontend path, the
   dispatcher key, logger names and the `notes.strict` property. The script
   leaves every runtime name alone, `oathdigital.properties` included, so
   this commit holds code identity only.
3. **Rename the server type.** `OathServer` becomes `DistantLandsServer`:
   file, references, `mainClass`.
4. **Runtime names.** Everything in the runtime table, across code,
   tests, `build.sbt`, launchers, scripts and CI, plus the actor system and
   pool names and the CLI prefix. Tests that pin these names (Chronicle
   storage contract, log marker, desktop paths, server config, packaging
   checks) assert the new ones.
5. **Build, packaging and CI.** Organization, modules, archive, executable,
   Docker image and user, `share/`, the three launchers (`git mv` and their
   text), `scripts/package-bundled-runtime.sh` (jar glob, folder checks),
   smoke scripts, `.github/workflows/alpha-release.yml` artifact names and
   release title, `package.json`, `package-lock.json`.
6. **Text and docs.** The naming rule applied to UI text
   (`frontend/index.html`, `frontend/production-index.html`,
   `ChroniclePage`, `TrustedHostUi`), then `README.md`, `PRODUCT.md`,
   `DESIGN.md`, `CONTEXT.md`, `claude.md`, `.impeccable` and all of
   `docs/`, `docs/superpowers` included, except what "Unchanged" lists.
7. **Bookkeeping**, once history is final: add commit 2's hash to
   `.git-blame-ignore-revs`. It comes last because a rebase changes it.

## Verification

All one-off, during the PR; nothing stays in the code.

1. The per-commit checks above.
2. After commit 5: `./sbtw Universal/packageZipTarball` and Docker
   staging, which run the packaging checks in `build.sbt`, then
   `scripts/smoke-bundled-distribution.sh` against the extracted
   `distantlands-<version>` directory.
3. After commit 6, this returns only the allowlist:

   ```sh
   git grep -il -E 'oath ?digital|oathdigital|oath-digital|OATH_|oath_seat|"oath\.'
   ```

   Allowlist: `.git-blame-ignore-revs`,
   `docs/operations/release-notes/0.1.0-alpha.*.md`, this spec, and its
   plan.
4. End to end, in a browser: start the packaged server with `DLQ_*`
   variables on a fresh data folder, create a game, open a seat link, and
   confirm the `distantlands_seat` cookie and `distantlands.*` storage keys.
   Then start it on a copy of a database from the current branch, moved to
   the new path, and confirm its games load.

## After merge

**Repository** (maintainer): rename `eNamorD/oathdigital` to
`distantlands` in GitHub settings, then
`git remote set-url origin git@github.com:eNamorD/distantlands.git`.
GitHub redirects the old URLs. The next release publishes to
`ghcr.io/enamord/distantlands`; old images stay where they are.

**Maintainer's server cutover**, once, at the first release with the
rename:

- Stop the server and back up the database directory, per the
  [data policy](../../operations/data-policy.md).
- Rename every `OATH_*` variable to `DLQ_*`.
- Move the data to its new path. HSQLDB names its files after the last
  segment of the database path, so the default path's files
  `var/oathdigital.{script,properties,lobs,log}` are renamed to
  `var/distantlands.*` (the `.tmp` directory too); the server must be
  stopped, and the `.lck` file is left behind. Desktop and Docker database
  paths end in `database`, so for them only the folder moves: rename the
  desktop data folder, or remount the Docker volume at
  `/var/lib/distantlands`. Rename the desktop settings file
  `oathdigital.properties` to `distantlands.properties` if present.
- Point deployments at `ghcr.io/enamord/distantlands`.

**Next release's notes** carry a short "Renamed to Distant Lands" section
for players: export Chronicles from the browser shelf before upgrading and
import them after (the storage keys change, and a new domain is a new
browser origin), and reopen seat links, since old seat cookies are not
recognised.
