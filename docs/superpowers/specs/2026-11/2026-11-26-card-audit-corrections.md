# Card audit corrections (2026-11-26)

Every card's declared cost, text, battle plan side, suit, defense, value and
site data was compared with its printed image: the bgs-cards images for
relics, edifices, legacies, sites and unrevised denizens, and the rev1 print
sheets for the 93 revised denizens. This file records what changed. Where it
disagrees with an earlier rulings file, this file wins.

Owner rulings behind it:

- bgs-cards is the source of truth and is newer than the rev1 print sheets.
- Second Wind follows its print: an attacker's plan only.
- Keep is a defender's battle plan that its defender chooses.

## Costs

The old component catalog recorded every burnt-favor relic cost as burnt
secrets. The earlier rulings copied that.

| Card | Printed cost | Was |
|---|---|---|
| R36 Barbed Net | 3 favor burnt | 3 secrets burnt |
| R09 Dowsing Sticks | 1 secret placed, 2 favor burnt | 1 secret placed, 2 secrets burnt |
| R21 Crystal Vial | 1 secret placed, 1 favor burnt | 1 secret placed, 1 secret burnt |
| R27 Fearsome Shield | 2 favor burnt | 2 secrets burnt |
| R35 Black Sword | 2 favor burnt | 2 secrets burnt |
| R46 Demon Tail | 3 favor burnt | 3 secrets burnt |
| R33 Wine of Welcome | 1 secret placed | free |
| 71 Cracking Ground | 1 secret burnt | free |
| 96 Enchantress | 1 secret burnt | 1 secret placed |

Consequences:

- Cracking Ground costs, so a bandit defender no longer applies it.
- Enchantress carries no new secret into the swap.
- Wine of Welcome and Mushrooms each place a secret, so picking both needs
  two faceup secrets.

## Battle plan sides

| Card | Printed | Was |
|---|---|---|
| 225 Unstable Summon | defender; collects X defense dice | attacker; X attack dice |
| 20 Disgraced Captain | attacker; +4 attack dice | either side; ±4 |
| 16 Second Wind | attacker | either side (batch 8, slice 14, ruling 11, withdrawn) |
| 5 Keep | defender's plan, chosen | an automatic rule of the site card |

Keep is offered in a Raid to the defender of Keep's site. Chosen, it
replaces the Raid's consequences and relocation once the defender is
defeated, as it did before. Not chosen, the Raid runs as usual.

With Second Wind an attacker's plan, no power queues a defender's off-turn
Travel any more. The tests of that path were removed with it.

## Behaviour read from the text

- **179 Threatening Roar** discards Nomad and *other* Beast cards. Played to
  a site, it stays.
- **95 Relic Thief** rolls defense dice equal to the relic's defense, "(min
  1)": a relic of defense 0 rolls one die.

## Wording

These cards now carry the bgs-cards wording. Their behaviour already matched
it: Giant Python, Salt the Earth, Truthful Harp, Grand Mask, Horned Mask,
Cracked Horn, Bandit Crown, The Grand Scepter, Hall of Ministers, Bandit
Forest, Iron Hand, Peacemaker, Great Architect, Royal Line, Rival, The
Standard Bearer, The Ear, Oaths of Loyalty, Light Fingers and Secret
Society.

"Denizens" replaces "cards" on six of them. Edifices are denizens (owner
ruling), so nothing else at a site is a card and the set is the same.

## Open

- **R03 Brass Horse.** The print sheet says "a different site"; bgs-cards
  says "a site". Whether the pawn may stay where it stands is undecided, so
  the card keeps its old text and behaviour.
- **Denizens revised on the print sheets but absent from the bgs-cards New
  Foundations set** (60 cards). bgs-cards lists them only in its base-game
  files, and its text differs from the print sheets on 12: in substance on
  Bandit Paymaster, Bandit Prince and Spinning Bee, in wording on the rest.
  They keep the print-sheet text until the owner rules which source governs
  them.
- **bgs-cards text that disagrees with its own card image**, on unrevised
  denizens: Elders (text "Gain favor", image "Gain secret"), Extra
  Provisions (text "+2", image "+"), Walled Garden (text "±", image "+"),
  Ancient Binding, Pressgangs, Levelers. The code follows the images.
- The reference component catalog is corrected for the relic costs only. It
  is not kept in sync with the card classes.
