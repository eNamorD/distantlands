# Chronicle Details implementation rulings

Rulings the owner's orchestrator made while the
[Chronicle Details design](2026-11-18-chronicle-details-design.md) was
implemented, on questions the design and its slice plans did not settle.
Each entry gives the question, the decision and the reason, the confidence
when the orchestrator marked it low, and the slice and files it touches.
The design's own rulings D1-D4 are not repeated here.

## Slice 1

**R1. Where unseated lineages come from.**
Question: the design reads roles and legacies from `campaign.lineages`,
which holds the seated lineages only; a lineage nobody seated lives in
`campaign.baseline.lineages`, its in-game role in `campaign.boxRoles`.
Decision: add `CampaignState.standingLineages`: the seated lineages plus
the baseline lineages missing from `campaign.lineages`, each boxed one
with `boxRoles.getOrElse(id, role)`. `ChronicleCrown.roles` switches to
it with no behaviour change, its suites unchanged, and the projection
reads it.
Reason: one rule in one place; a second copy in the projector would
drift from the crown's.
Slice/files: 1; `model/GameState.scala`,
`gameplay/chronicle/ChronicleCrown.scala`,
`application/GameProjection.scala`.

**R2. Naming the Recent site's cards outside the overlay.**
Decision: `identifiesCard` names the denizens and edifices stored at the
Atlas's Recent site to every viewer, and never names relics. Its doc
comment cites the NF rulebook's Atlas privacy line. If a golden log
changes, stop and report the diff; do not re-record.
Reason: the rulebook makes exactly those cards public.
Slice/files: 1; `application/GamePresentationProjector.scala`.

**R3. The side a stored edifice shows.**
Decision: intact.
Reason: the rulebook stores intact edifices and starts them intact.
Slice/files: 1; `GamePresentationProjector.atlasSite`.

**R4. The case class default.**
Decision: `chronicleDetails = None` is the last `GameProjection` parameter
in Scala, and the key is required on the wire. A lineage's `role` and
`atlasRecent` are required and nullable.
Reason: this matches `foundations`; the wire rule is what matters.
Slice/files: 1; `GameProjectionDto.scala`, `GameProjectionCodec.scala`,
`ChronicleDetailsCodec.scala`.

**R5. Lineage order.**
Decision: lineages sort by their seat's colour when seated, otherwise by
the colour the lineage id names, then any left by id.
Reason: the design's "PlayerColor order" needs a colour for unseated and
test lineages.
Slice/files: 1; `application/GameProjection.scala`.
