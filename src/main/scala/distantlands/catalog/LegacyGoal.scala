package distantlands.catalog

/** A legacy's goal: the white icon at the top of its ribbon (CR section
  * 12, and the Sun task card's legend). Resolve Legacies checks it. A
  * `MostOfSuit` goal reads the legacy's own suit. */
enum LegacyGoal(val key: String):
  /** "Rule the most sites." */
  case MostSites extends LegacyGoal("most-sites")
  /** "Hold Grand Scepter. If none, win." */
  case GrandScepter extends LegacyGoal("grand-scepter")
  /** "Hold People's Favor." */
  case PeoplesFavor extends LegacyGoal("peoples-favor")
  /** "Hold Darkest Secret." */
  case DarkestSecret extends LegacyGoal("darkest-secret")
  /** "Hold relic of highest value (under name)." */
  case HighestRelic extends LegacyGoal("highest-relic")
  /** "Rule most cards of suit (even advisers)." */
  case MostOfSuit extends LegacyGoal("most-of-suit")

object LegacyGoal:
  /** In printed id order: L01-L06 first, L31-L36 last. */
  val all: Vector[LegacyGoal] = Vector(MostSites, GrandScepter,
    PeoplesFavor, DarkestSecret, HighestRelic, MostOfSuit)
