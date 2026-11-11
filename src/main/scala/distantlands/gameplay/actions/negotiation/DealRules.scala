package distantlands.gameplay.actions.negotiation

import distantlands.model.PlayerId

/** A kind of term an author may put into a row for one recipient (catalog
  * batch 12, "The deal model"). Secrets join with slice 6. */
enum TermKind:
  case Favor, Relic, Adviser, Site

/** Who may put what into one deal (catalog batch 12, "The deal model"):
  * the one place that says it. Built when the deal opens, from the
  * Negotiation's start facts ([[NegotiationStartFacts.rules]]).
  *
  * `adviserTraders` may give and receive advisers with anyone (Council
  * Arbiter, ruling 14). `advisersOpen` lets every pair trade them
  * (Festival District, ruling 15). `bindingPromises` opens binding
  * promises to every author (The Tribunal, ruling 11). `siteTraders` may
  * give and receive sites with anyone (Deed Writer, ruling 18).
  * `privateDeal` keeps the terms from players outside the deal (Whispering
  * Stone, ruling 23).
  */
final case class DealRules(adviserTraders: Vector[PlayerId] = Vector.empty,
    advisersOpen: Boolean = false, bindingPromises: Boolean = false,
    siteTraders: Vector[PlayerId] = Vector.empty,
    privateDeal: Boolean = false):
  def kinds(author: PlayerId, recipient: PlayerId): Set[TermKind] =
    val pair = Set(author, recipient)
    Set[TermKind](TermKind.Favor, TermKind.Relic) ++
      Option.when(advisersOpen || adviserTraders.exists(pair))(
        TermKind.Adviser) ++
      Option.when(siteTraders.exists(pair))(TermKind.Site)

  /** The participants `author` may give advisers, in participant order. */
  def adviserRecipients(author: PlayerId,
      participants: Vector[PlayerId]): Vector[PlayerId] =
    participants.filter(recipient => recipient != author &&
      kinds(author, recipient).contains(TermKind.Adviser))

  /** The participants `author` may give sites, in participant order,
    * before ruling 19 and the Vow of Wandering narrow them
    * ([[NegotiationSites.mayReceive]]). */
  def siteRecipients(author: PlayerId,
      participants: Vector[PlayerId]): Vector[PlayerId] =
    participants.filter(recipient => recipient != author &&
      kinds(author, recipient).contains(TermKind.Site))
