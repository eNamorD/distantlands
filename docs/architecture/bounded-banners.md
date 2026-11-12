# Bounded banners and Challenge

Design: [challenge walker design](../superpowers/specs/2026-09/2026-09-19-challenge-walker-design.md)

This slice implements Combined Rulebook pp. 26 and 32 and New Foundations
pp. 12 and 15 for the first-game banner faces, Mob and Wandering Flame, and
for the altered ones: an altered Foundation V starts the People's Favor on
its Grand Council face, and an altered Foundation VI the Darkest Secret on
its Festival face
([Foundations design](../superpowers/specs/2026-11-11-foundations-design.md),
slices 5 and 6). The face identities are typed state, and so is the site
the festival marker lies over (`DarkestSecretState.festival`).

`BannerRules` owns holder and resource lookup, the faceup-only comparison
input (favor, or faceup secrets only), least-stocked favor banks and sites,
and the one-at-a-time favor return. The People's Favor's ribbon
(`peoplesFavorRibbon`) reads the face, and Challenge, Favored Son, Campaign
Raid and Conspiracy all adjust the banner through it, including the printed
leftmost tied-bank rule. The Darkest Secret's right ribbon
(`darkestSecretRibbon`) reads the face too, for a Raid and the Conspiracy,
and both Festival ribbons end by removing the marker (`festivalRemoval`).
Only Wandering Flame site ties create a player decision. Nothing derives behaviour from component `rulesText`.

## Challenge

Challenge is a normal Act major action and runs as the `ChallengeProcedure`
walker tree. Its start gate refuses a start with no legal banner. A banner is
legal when the actor does not hold it, its holder (if any) shares the actor's
site, and the actor has strictly more relevant faceup resources than the
banner holds.

The walk opens with the modifier decision, spends exactly 1 Supply, then asks
for the banner and for a replacement amount strictly greater than the prior
amount. Both answers come before any resource moves. The ribbon then returns
the banner's resources (`ChallengeRibbon`):

- People's Favor on the Mob returns every favor to the least-favor banks,
  leftmost first. On the Grand Council the previous holder first takes back
  half, rounded down; an unheld banner's favor all goes to the banks.
- Darkest Secret first gives the previous holder the secrets beyond half
  (rounded down), or burns them when the banner was unclaimed (7.8.2 errata).
  It then places the half on the least-stocked sites one pass at a time. When
  fewer secrets remain than tied sites, the challenger picks which sites.
- Darkest Secret on the Festival burns one secret and gives the previous
  holder the rest, or burns them too when the banner was unclaimed. After
  the ribbon window, so a power that replaces the ribbon's resources there
  leaves it be, the festival marker leaves the map.

Last, the challenger pays the amount onto the banner and takes it by a
required `Take`. Each answer is a recorded walker step and every effect is
recorded core operations, so replay applies them without re-deriving them.

## Banner resources and face powers

Place Banner Resource (`PlaceBannerResourceProcedure`) is a 0-Supply minor
action for a banner's holder. It moves a positive amount of favor or faceup
secrets from the holder's own board onto the banner.

The Mob face is a contribution (`PeoplesFavorMob`): when its holder plays a
card to a site, they may first discard a card there. The Grand Council face is
another (`PeoplesFavorGrandCouncil`): its holder may also play a card to any
Imperial site, and may first discard a card there, but at no other site. The Wandering Flame face
has two free, unlimited actions (`WanderingFlameMove`, `WanderingFlamePlace`):
move the pawn to another site with a secret on it, or place a faceup secret on
the pawn's site. The Festival face is a contribution and a power-use
restriction (`DarkestSecretFestival`): at the start of its holder's Rest
(`RestStart`) the marker goes to the holder's pawn site, and while it lies
there the holder's enemies cannot place favor or secrets on the denizens at
that site or use their non-persistent powers, unless the holder consents.
`BannerFacePowers` registers them.

## Visibility

Banner faces, holders, and resource totals are public projections. Challenge
decisions, their legal bank and site choices, and the amount bounds are
visible only to the decision owner. Authenticated HTTP intents do not accept a
player identifier; development commands keep the loopback actor selector
check.
