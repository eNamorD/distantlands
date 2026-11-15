package distantlands.catalog

/** A legacy's goal: the white icon at the top of its ribbon (CR section
  * 12, and the Sun task card's legend). Resolve Legacies checks it. A
  * `MostOfSuit` goal reads the legacy's own suit. `key` names it on the
  * wire and in the frontend's styles; `printed` is its words as the rules'
  * Sun list words it, which the game shows in place of an icon (Legacies
  * implementation rulings R34, R35 and R42). */
enum LegacyGoal(val key: String, val printed: String):
  case MostSites extends LegacyGoal("most-sites", "Rule the most sites.")
  case GrandScepter extends LegacyGoal("grand-scepter",
    "Hold the Grand Scepter. If none, win.")
  case PeoplesFavor extends LegacyGoal("peoples-favor",
    "Hold the People's Favor.")
  case DarkestSecret extends LegacyGoal("darkest-secret",
    "Hold the Darkest Secret.")
  case HighestRelic extends LegacyGoal("highest-relic",
    "Hold the relic of the highest value (shown under name).")
  case MostOfSuit extends LegacyGoal("most-of-suit",
    "Rule the most cards of this suit (even advisers).")
