# Negotiation

Design: [negotiation walker design](../superpowers/specs/2026-09-19-negotiation-walker-design.md)

Negotiation implements the Combined Rulebook page 27 and New Foundations page 15 procedure. It is a persistent 0-Supply minor action started by the active player with one or more players at their site or within reach, and it takes no start selection, except the private marker Whispering Stone's queued Negotiation carries.

## Procedure

`NegotiationProcedure` (`gameplay/actions/negotiation/`) builds one tree:

```
Sequence(
  Decide(negotiators)              -- omitted when exactly one candidate exists
  Repeat(!closed) { Branch -> Decide(deal, co-owned) }
  Branch(agreed -> BuildOps(settle)))
```

The deal's `Repeat` and its settlement are `NegotiationDealTree`'s, driven by a `DealWalk`: the prefix of the deal's decision ids, the owner of its decision, its participants and rules, and its opening line. The Negotiate action walks one under the prefix `negotiation`. The fold, the decline line and the Deed Writer warband questions read `<prefix>.deal` and `<prefix>.warbands.<site>`, so a walk may hold several deals.

- The negotiator choice is a `ChooseMany(1, n, ...)` over the candidates: the other players at the actor's site, and those Traveling Negotiator, The Tribunal or Bandit Tribunal lets the actor reach (`NegotiationReach`). Its window is `NegotiationEligibility`.
- The deal is one `Decide` whose owner is the actor and whose co-owners are the other participants. Any owner may answer, in any order, and the `Repeat` re-asks it after each answer until the deal closes.
- A start is refused when there is no candidate (`NegotiationUnavailable`). The start reads the reach once, under the walk's power-use restrictions (City Wall, Giant Python, Bandit Forest), and stores the candidates and The Tribunal's users as walker start facts (`NegotiationStartFacts`). Every rebuild reads them back. There is no first-game gate: the action is available to any role and any Foundation face.
- A deal opens binding promises to every author when one participant may use The Tribunal (intact).

## The deal is a fold over the answers

Nothing about the deal is stored. `PendingTree.answered` is the only record, and `NegotiationDeal.fold` derives the deal from it on every command:

- `ProposeTerms(terms)` replaces that author's whole terms and clears every acceptance.
- `AcceptDeal` adds the answerer to the accepted set.
- `DeclineDeal` closes the deal.
- The deal is agreed when every participant has accepted and at least one term has substance (favor, secrets, a relic, a disclosure, a Citizenship offer, a promise or a binding promise). An empty deal cannot be accepted.

The `Negotiate` decision query is a snapshot rebuilt from live state and the fold on every command. It carries the participants, everyone's terms, who has accepted, each author's own bounds (recipients, favor held, relics held, information that may be promised) and who may accept now. `DecisionQueries.accepts` judges a proposal against the answerer's own bounds, which is why it receives the answerer.

A transfer row may hold advisers where `DealRules` allows them: Council Arbiter's faceup holder trades advisers with anyone, and a participant with access to Festival District opens them between every pair. The start stores who may use each card, under the power-use restrictions (`NegotiationStartFacts`). The bounds offer only the advisers no printed restriction refuses to give. Nobody may accept while the terms would leave a participant over their adviser limit (`DecisionQuery.Negotiate.overAdviserLimit`), read through `AdviserLimits`, which the registry fills with the powers' `AdviserLimit`. Settlement gives each adviser after the favor and relics, keeping its face.

A transfer row may hold sites where `DealRules` allows them: Deed Writer's faceup holder trades sites with anyone, under the power-use restrictions, and the start stores who may (`NegotiationStartFacts`). An author may give the sites their own-colour warbands hold, so every Imperial player may give every Imperial site, but never to another Imperial player or to a Vow of Wandering holder (`NegotiationSites`). Nobody may accept while a recipient would have fewer warbands on their board than sites they receive, counted after their own sites' warbands come back (`DecisionQuery.Negotiate.shortOfWarbands`). Settlement returns each given site's warbands to its old ruler, then asks each new ruler, site by site, how many to place (`<prefix>.warbands.<site>`). The placement line covers the generic warband line.

A transfer row may hold secrets where `DealRules.secretsOpen` allows them. The Negotiate action never opens them; The Gathering does (catalog batch 12, slice 6b). An author may give up to their faceup secrets, and settlement moves them faceup from board to board after the favor (`Move` with a faceup source side). A banner's secrets are never on the board, so they never move.

## Settlement and disclosure

Settlement is one recorded step. It validates that every author can still afford their terms and still holds their offered relics, then emits disclosure `Peek`s first, while the cards are still where they were disclosed, and the favor, secret and relic transfers second, in participant order. If the state changed so that a term is no longer affordable, the final accept is rejected with `InsufficientFavor`, `InsufficientSecrets` or `NegotiationUnavailable` and the deal stays open. A decline applies nothing. Either close runs the action boundary.

Disclosures are binding deal terms rather than an immediate sharing command. Completion grants selected recipients persistent adviser, held-relic, or site-relic knowledge. Adviser and held-relic knowledge follows card identity; site-relic knowledge remains site-scoped. Any future move into a shuffled or otherwise randomized opaque zone must explicitly clear applicable knowledge.

## Private deals

Whispering Stone pays 1 secret onto the relic and queues a Negotiation `Run`, sourced on the relic, whose one start argument is the private marker (`NegotiationProcedure.privateArgs`). The Negotiation accepts the marker only from such an entry (`queuedPrivately`). Its candidates are then every other player, its negotiators question takes exactly one, and `DealRules.privateDeal` marks the `Negotiate` query private. Every other rule of the deal holds unchanged, its When Negotiating powers and their blockers included.

A viewer outside a private deal is projected its participants and accepted marks only. The opening line reads "Negotiated with p3, in private." The settlement's lines are public, but each promise line is a `LogNote` read only by the participants (`LogNote.readers`). The formatter numbers every line before it drops one for a viewer outside its readers, so the other lines keep their keys.

## The Gathering

The Gathering's When Played power (`gameplay/powers/whenplayed/`) asks
each player whose pawn is elsewhere, in the next round's order, whether
to put it on the card's site, and moves a yes with a plain `Move`. Then
`GatheringDeals` loops one open question, `gathering.open`, co-owned by
every player at the site:

```
Repeat(two or more at the site, one of them not passed) {
  Branch -> Decide(gathering.open)          -- OpenDeal, PassGathering, UnpassGathering
  Branch(OpenDeal -> NegotiationDealTree under gathering.<n>)
}
```

- Each deal is a `DealWalk` with every printed kind open
  (`advisersOpen`, `secretsOpen`), site terms under Deed Writer and binding
  promises under The Tribunal. Its decision is co-owned by its
  participants and by the players at the site outside it, its
  bystanders, who may only pass or take back a pass.
- The opener may name a player elsewhere whom the Negotiate action's
  reach lets them reach. The reach and each deal's rules are read live,
  under the walk's power-use restrictions (`PowerCtx.useRestrictions`):
  nothing is stored, since no state changes while a question or a deal
  is parked.
- Passes are folded from every answer under `gathering.`; opening a deal
  clears the opener's pass only. The loop ends when every player at the
  site has passed.

## Visibility

Owners get the full deal with editing inputs, computed from their own bounds. Every other viewer, including the public view, gets a read-only deal through the walker waiting projection: the participants, who has accepted, favor amounts, relic counts, faceup relic details, and each disclosure's kind and recipient. Only an author sees the identity of a facedown relic they offer or of information they promise. A private deal shows a viewer outside it only who negotiates and who has accepted (see Private deals).

## Unsupported Negotiation rules

The one `When Negotiating` handler the engine does not support, High Priest's (a legacy), is ignored, not blocking, and the start records nothing about it. The engine does not infer mechanics from rules text.

## Journal

Each answer is a `WalkerStepRecorded` carrying a `ChoicePayload`. Negotiation has no events of its own.

## Deferred

High Priest's power, "You may give secrets", comes with the legacies (roadmap **Phase - Chronicle steps**). Banners are never terms: New Foundations forbids giving them. The engine records promises and binding promises but enforces none, as the rules leave their penalties to the players.

No power contributes at the `NegotiationEligibility` or `NegotiationSettlement` windows. The reach cards (Traveling Negotiator, The Tribunal, Bandit Tribunal) read `NegotiationReach`, and the term-kind cards (Council Arbiter, Festival District, Deed Writer) and The Gathering set `DealRules`.
