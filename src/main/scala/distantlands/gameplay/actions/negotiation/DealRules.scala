package distantlands.gameplay.actions.negotiation

import distantlands.model.PlayerId

/** A kind of term an author may put into a row for one recipient (catalog
  * batch 12, "The deal model"). Secrets and sites join with the slices
  * that move them. */
enum TermKind:
  case Favor, Relic, Adviser

/** Who may put what into one deal (catalog batch 12, "The deal model"):
  * the one place that says it. Built when the deal opens, from the
  * Negotiation's start facts ([[NegotiationStartFacts.rules]]).
  *
  * `adviserTraders` may give and receive advisers with anyone (Council
  * Arbiter, ruling 14). `advisersOpen` lets every pair trade them
  * (Festival District, ruling 15). `bindingPromises` opens binding
  * promises to every author (The Tribunal, ruling 11).
  */
final case class DealRules(adviserTraders: Vector[PlayerId] = Vector.empty,
    advisersOpen: Boolean = false, bindingPromises: Boolean = false):
  def kinds(author: PlayerId, recipient: PlayerId): Set[TermKind] =
    val printed: Set[TermKind] = Set(TermKind.Favor, TermKind.Relic)
    if advisersOpen || adviserTraders.contains(author) ||
        adviserTraders.contains(recipient) then printed + TermKind.Adviser
    else printed

  /** The participants `author` may give advisers, in participant order. */
  def adviserRecipients(author: PlayerId,
      participants: Vector[PlayerId]): Vector[PlayerId] =
    participants.filter(recipient => recipient != author &&
      kinds(author, recipient).contains(TermKind.Adviser))
