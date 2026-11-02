#!/usr/bin/env python3
"""Deterministic source-boundary checks for the modular monolith."""

from pathlib import Path
import re
import sys

ROOT = Path(__file__).resolve().parents[1]
PRODUCTION_ROOTS = (
    ROOT / "src/main/scala",
    ROOT / "frontend/src/main/scala",
    ROOT / "shared/src/main/scala",
)

# The hard limit docs/architecture/codebase-structure.md sets for a Scala
# source file. This is the only place the number is written: the
# "stays at or below" tests in BackendArchitectureSuite read this
# declaration, so keep the name and the assignment on one line.
MAX_SCALA_FILE_LINES = 800


def scala_sources(root: Path):
    return sorted(root.rglob("*.scala"))


def relative(path: Path) -> str:
    return path.relative_to(ROOT).as_posix()


errors = []
sources = [path for root in PRODUCTION_ROOTS for path in scala_sources(root)]

for path in sources:
    line_count = len(path.read_text(encoding="utf-8").splitlines())
    if line_count > MAX_SCALA_FILE_LINES:
        errors.append(f"{relative(path)}:{line_count}: production Scala file "
                      f"exceeds {MAX_SCALA_FILE_LINES} lines")

forbidden_imports = {
    "src/main/scala/oathdigital/model": (
        "application", "gameplay", "persistence", "serialization", "server"
    ),
    "src/main/scala/oathdigital/gameplay": (
        "application", "persistence", "presentation", "protocol",
        "serialization", "server"
    ),
    "src/main/scala/oathdigital/application": (
        "persistence", "serialization", "server"
    ),
    # Actions are the engine's procedures; powers contribute to them through
    # power windows (docs/architecture/gameplay-modules.md).
    "src/main/scala/oathdigital/gameplay/actions": ("gameplay.powers",),
    "src/main/scala/oathdigital/catalog": (
        "application", "gameplay", "persistence", "serialization", "server"
    ),
}
# Known exceptions, each a file and the one import it may keep. The attack
# line counts skull faces as the chosen plans score them, which only the
# plan cards know (ROADMAP "Campaign notes read the attack rules").
allowed_imports = {
    "src/main/scala/oathdigital/gameplay/actions/campaign/CampaignNotes.scala":
        "import oathdigital.gameplay.powers.campaign.AttackScore\n",
}
for prefix, packages in forbidden_imports.items():
    root = ROOT / prefix
    needles = tuple(f"import oathdigital.{package}" for package in packages)
    for path in scala_sources(root):
        text = path.read_text(encoding="utf-8")
        text = text.replace(allowed_imports.get(relative(path), "\0"), "")
        for needle in needles:
            if needle in text:
                errors.append(f"{relative(path)}: forbidden inward dependency: {needle}")

# The frontend renders projections over the shared protocol and never
# imports gameplay, application or server rules; the shared protocol
# imports nothing but itself (docs/architecture/codebase-structure.md).
# One exception: build.sbt grafts three `model` files into the Scala.js
# build, and `oathdigital.model.PlayerColor` from them is the one model
# name the frontend and the shared protocol reference, by import or fully
# qualified. Any other `oathdigital.<package>` reference, in an import or
# written out in full, is a violation.
GRAFTED_MODEL_FILES = ("Identity.scala", "PlayerColor.scala", "Resources.scala")
SHARED_MODEL_NAMES = {"PlayerColor"}
build_sbt = (ROOT / "build.sbt").read_text(encoding="utf-8")
for grafted in GRAFTED_MODEL_FILES:
    if f'"model" / "{grafted}"' not in build_sbt:
        errors.append(f"build.sbt: no longer grafts model/{grafted} into the "
                      "frontend; revisit the frontend gate's model exception")

qualified_reference = re.compile(
    r"\boathdigital\.([A-Za-z0-9_]+)(?:\.([A-Za-z0-9_]+))?")
# `import oathdigital.{application, model => m}` names packages inside the
# braces, where the pattern above cannot see them.
brace_group_import = re.compile(r"\bimport\s+oathdigital\.\{([^}]*)\}")


def outward_references(path: Path, allowed_packages: tuple[str, ...]):
    """Each (line number, package) naming a package outside `allowed_packages`,
    except the sanctioned model names."""
    for line_number, line in enumerate(path.read_text(encoding="utf-8").splitlines(), 1):
        for group in brace_group_import.finditer(line):
            for item in group.group(1).split(","):
                package = item.strip().split("=>")[0].strip().split(".")[0]
                if package and package not in allowed_packages:
                    yield line_number, package
        for match in qualified_reference.finditer(line):
            package, member = match.group(1), match.group(2)
            if package in allowed_packages:
                continue
            if package == "model" and member in SHARED_MODEL_NAMES:
                continue
            yield line_number, package


for path in scala_sources(ROOT / "frontend/src/main/scala"):
    for line_number, package in outward_references(path, ("frontend", "protocol")):
        errors.append(f"{relative(path)}:{line_number}: frontend references {package}")

for path in scala_sources(ROOT / "shared/src/main/scala"):
    for line_number, package in outward_references(path, ("protocol",)):
        errors.append(
            f"{relative(path)}:{line_number}: shared protocol references {package}"
        )

printed_text = re.compile(r"\.text\b")
for path in scala_sources(ROOT / "src/main/scala/oathdigital/gameplay"):
    text = path.read_text(encoding="utf-8")
    if "rulesText" in text:
        errors.append(f"{relative(path)}: gameplay must not inspect catalog rulesText")
    if printed_text.search(text):
        errors.append(f"{relative(path)}: gameplay must not read printed power text")

legacy_patterns = {
    r"\boathdigital\.setup\b": "legacy setup package",
    r"\bSetupEventWire\b": "deleted setup wire",
    r"\bSetupState\b": "deleted bounded setup aggregate",
    r"\bBeginSetup\b": "deleted bounded setup command",
    r"\bBrowserMemory\w*\b": "deleted browser-memory stack",
}
for path in sources:
    text = path.read_text(encoding="utf-8")
    for pattern, label in legacy_patterns.items():
        if re.search(pattern, text):
            errors.append(f"{relative(path)}: contains {label}")

definition = re.compile(r"\s*(?:sealed trait|final case class)\s+([A-Za-z0-9_]+)")
shared_only = {
    "GameProjection", "SetupPlayerProjection", "CardDetailsProjection",
    "PlayerBoardProjection",
    "FirstGameBootstrapRequest", "BootstrapParticipantRequest",
}
for path in sources:
    for line_number, line in enumerate(path.read_text(encoding="utf-8").splitlines(), 1):
        match = definition.match(line)
        if match and match.group(1) in shared_only and "shared/src/main" not in relative(path):
            errors.append(
                f"{relative(path)}:{line_number}: duplicate shared DTO {match.group(1)}"
            )

command_definition = re.compile(
    r"\s*(?:sealed trait|final case class)\s+(GameIntent|GameCommand)(?:\s|\()"
)
for path in sources:
    rel = relative(path)
    if rel.endswith("application/GameCommands.scala") or "shared/src/main" in rel:
        continue
    for line_number, line in enumerate(path.read_text(encoding="utf-8").splitlines(), 1):
        if command_definition.match(line):
            errors.append(f"{rel}:{line_number}: duplicate command vocabulary")

# Powers read their cards from card objects (ROADMAP standing rules; the
# card classes design of 2026-10-01). A power takes a `catalog:
# ExecutableCatalog` parameter, or offers a `forCatalog` constructor, only
# to look other cards up: another card's suit, a site's capacity, a relic's
# defense. Each file that still does is listed here with its reason; a new
# one is a violation until it earns an entry.
POWERS_ROOT = "src/main/scala/oathdigital/gameplay/powers"
catalog_parameter = re.compile(r"catalog: ExecutableCatalog|def forCatalog\b")
powers_reading_the_catalog = {
    "ActionModifiers.scala": "registry: threads the catalog to the powers below that read it",
    "AdviserLimit.scala": "helper: the adviser limit reads the holder's advisers' suits",
    "CatalogResolution.scala": "helper: the catalog form serves powers without a card object",
    "ConsentPowers.scala": "helper: builds the walker powers",
    "DeclaredNotes.scala": "helper: builds the walker and phase powers for their notes",
    "PhasePowerCatalog.scala": "registry: threads the catalog to the powers below that read it",
    "PowerImplementationStatus.scala": "helper: builds every power to count the implemented ones",
    "RuledCards.scala": "helper: the cards a ruler rules, by their effective suit",
    "SiteRoom.scala": "helper: a site's capacity, from the catalog",
    "WalkerPowerCatalog.scala": "registry: threads the catalog to the powers below that read it",
    "action/ArmedMob.scala": "reads other cards' suits",
    "action/Assassin.scala": "reads other cards' suits",
    "action/BallotBox.scala": "reads other cards' suits",
    "action/BedOfRoots.scala": "reads other cards' suits",
    "action/BrassHorse.scala": "reads other cards' suits",
    "action/CrystalVial.scala": "reads other cards' suits",
    "action/DarkEnforcer.scala": "reads other cards' suits",
    "action/GiantPython.scala": "reads other cards' suits",
    "action/GreatFeast.scala": "reads other cards' suits",
    "action/Homesteaders.scala": "reads a site's capacity",
    "action/HonorGuard.scala": "reads other cards' suits",
    "action/MemoryOfNature.scala": "reads other cards' suits",
    "action/MovementPowers.scala": "registry: threads the catalog to the powers below that read it",
    "action/Oracle.scala": "plays a card through the card-play procedure",
    "action/OtherActionPowers.scala": "registry: threads the catalog to the powers below that read it",
    "action/RelicThief.scala": "reads another relic's defense",
    "action/Resettle.scala": "reads other cards' suits",
    "action/RovingTerror.scala": "reads other cards' suits",
    "action/SecondChance.scala": "reads other cards' suits",
    "action/SelfActionPowers.scala": "registry: threads the catalog to the powers below that read it",
    "action/SingingMask.scala": "reads other cards' suits",
    "action/SiteCards.scala": "helper: the cards at a site, by suit",
    "action/SiteSlotMoves.scala": "helper: site capacity and the cards at a site",
    "action/TamingCharm.scala": "reads other cards' suits",
    "action/TargetPowers.scala": "registry: threads the catalog to the powers below that read it",
    "action/WildAllies.scala": "reads other cards' suits",
    "action/WorldActionPowers.scala": "registry: threads the catalog to the powers below that read it",
    "campaign/BattleAxes.scala": "reads other cards' suits",
    "campaign/BeastTamer.scala": "reads other cards' suits",
    "campaign/CrackedSage.scala": "reads other cards' suits",
    "campaign/DisgracedCaptain.scala": "reads other cards' suits",
    "campaign/EnemyPlans.scala": "helper: the suit of the enemy's plan card",
    "campaign/FavorBySuit.scala": "helper: counts cards by suit",
    "campaign/GleamingArmor.scala": "prices the enemy's plan card (PlanSurcharge)",
    "campaign/GreatCrusade.scala": "reads other cards' suits",
    "campaign/InsectSwarm.scala": "prices the enemy's plan card (PlanSurcharge)",
    "campaign/KindredWarriors.scala": "reads other cards' suits",
    "campaign/MilitaryParade.scala": "reads other cards' suits",
    "campaign/NatureWorship.scala": "reads other cards' suits",
    "campaign/PlanContext.scala": "helper: the cards a side rules, by suit",
    "campaign/PlanDiscard.scala": "helper: the catalog form discards any ruled card (Wild Mounts)",
    "campaign/PlanRestrictions.scala": "registry: threads the catalog to the powers below that read it",
    "campaign/PlanRewrites.scala": "registry: threads the catalog to the powers below that read it",
    "campaign/PlanRules.scala": "registry: threads the catalog to the powers below that read it",
    "campaign/PlanSurcharge.scala": "prices the enemy's plan card",
    "campaign/PledgeOfDefense.scala": "reads other cards' suits (the ruled Nomad cards)",
    "campaign/RivalKhan.scala": "reads other cards' suits",
    "campaign/SaltTheEarth.scala": "reads other cards' suits",
    "campaign/SignalTrees.scala": "reads other cards' suits",
    "campaign/SimplePlans.scala": "registry: threads the catalog to the powers below that read it",
    "campaign/TributeSpoils.scala": "reads other cards' suits",
    "campaign/TrueNames.scala": "reads other cards' suits",
    "campaign/WalledGarden.scala": "reads other cards' suits",
    "campaign/WildMounts.scala": "reads other cards' suits",
    "cardplay/CardPlayTriggers.scala": "registry: threads the catalog to the powers below that read it",
    "cardplay/FamilyWagon.scala": "reads the other advisers' suits (SuitOnlyAdvisers)",
    "cardplay/SaddleMakers.scala": "reads other cards' suits",
    "cardplay/SuitOnlyAdvisers.scala": "reads the other advisers' suits",
    "cardplay/VowOfUnionAdvisers.scala": "reads the other advisers' suits (SuitOnlyAdvisers)",
    "cardplay/WildCry.scala": "reads other cards' suits",
    "economy/ActingSite.scala": "reads the suits of the cards at the chosen site",
    "economy/AnimalPlaymates.scala": "reads the suit of the card mustered from (SuitSupplyWaiver)",
    "economy/Birdsong.scala": "reads the suit of the card traded from (SuitSupplyWaiver)",
    "economy/CupOfPlenty.scala": "reads other cards' suits",
    "economy/Curfew.scala": "reads other cards' suits",
    "economy/Defame.scala": "reads other cards' suits",
    "economy/Downtrodden.scala": "reads other cards' suits",
    "economy/EconomySources.scala": "helper: the cards a Trade or Muster draws on, by suit",
    "economy/FriendlyFamiliar.scala": "reads other cards' suits",
    "economy/MapLibrary.scala": "reads other cards' suits",
    "economy/MountedLibrary.scala": "reads other cards' suits",
    "economy/MovingMarket.scala": "reads a site's capacity",
    "economy/SkilledMerchants.scala": "reads other cards' suits",
    "economy/SmallFriends.scala": "reads other cards' suits",
    "economy/SuitSupplyWaiver.scala": "helper: the suit of the traded card",
    "economy/VowOfBeastkin.scala": "reads other cards' suits",
    "poweruse/ForestCouncil.scala": "reads other cards' suits",
    "recover/CatacombsContribution.scala": "reads its site's relic slots",
    "rest/LeagueTreatyContribution.scala": "reads other cards' suits",
    "rest/SilverTongue.scala": "reads other cards' suits",
    "search/ForestWarden.scala": "reads other cards' suits",
    "search/NewGrowth.scala": "reads other cards' suits",
    "search/SpinningBee.scala": "reads other cards' suits",
    "setup/ProvingGroundsRules.scala": "reads other cards' suits",
    "targeting/Diplomat.scala": "reads other cards' suits",
    "targeting/FortressRules.scala": "reads other cards' suits",
    "targeting/LostTongue.scala": "reads other cards' suits",
    "targeting/TargetProtections.scala": "registry: threads the catalog to the powers below that read it",
    "travel/ForbiddenForest.scala": "reads the traveller's advisers' suits",
    "travel/ForestPaths.scala": "reads other cards' suits",
    "travel/Hospitality.scala": "reads other cards' suits",
    "travel/TravelModifiers.scala": "registry: threads the catalog to the powers below that read it",
    "wake/HornedMask.scala": "reads other cards' suits",
    "wake/Hunger.scala": "reads other cards' suits",
    "whenplayed/ARoundOfAle.scala": "runs the Rest cleanup, which reads suits",
    "whenplayed/AnimalHost.scala": "reads other cards' suits",
    "whenplayed/Blackmail.scala": "builds Lost Tongue, which reads other cards' suits",
    "whenplayed/CallForHelp.scala": "reads other cards' suits",
    "whenplayed/Dazzle.scala": "reads other cards' suits",
    "whenplayed/Dissent.scala": "reads other cards' suits",
    "whenplayed/FabledFeast.scala": "reads other cards' suits",
    "whenplayed/GreatHerd.scala": "reads other cards' suits",
    "whenplayed/RegionDiscard.scala": "helper: the cards of a region, by suit",
    "whenplayed/Riots.scala": "reads other cards' suits",
    "whenplayed/ThreateningRoar.scala": "reads other cards' suits",
    "whenplayed/TownMeeting.scala": "reads other cards' suits",
    "whenplayed/TrueOath.scala": "reads other cards' suits",
    "whenplayed/TwinBrother.scala": "reads other cards' suits",
    "whenplayed/WhenPlayedPowers.scala": "registry: threads the catalog to the powers below that read it",
    "whenplayed/WizardsConclave.scala": "reads other cards' suits",
}
for path in scala_sources(ROOT / POWERS_ROOT):
    rel = relative(path)[len(POWERS_ROOT) + 1:]
    if catalog_parameter.search(path.read_text(encoding="utf-8")):
        if rel not in powers_reading_the_catalog:
            errors.append(f"{relative(path)}: a power takes the catalog or offers "
                          "forCatalog; read the card object, or list the file "
                          "with its reason in check-architecture.py")
    elif rel in powers_reading_the_catalog:
        errors.append(f"{relative(path)}: listed as reading the catalog but no "
                      "longer does; drop its entry from check-architecture.py")

if errors:
    print("architecture check failed:")
    for error in sorted(set(errors)):
        print(f"- {error}")
    sys.exit(1)

print(f"architecture check passed: {len(sources)} production Scala files")
