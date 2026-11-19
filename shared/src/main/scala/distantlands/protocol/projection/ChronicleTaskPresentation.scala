package distantlands.protocol.projection

/** One printed side of a Chronicle task card: its label ("Front" or
  * "Back") and its lines, one a paragraph, in the rules text markup the
  * inspector renders. */
final case class ChronicleTaskFace(label: String, lines: Vector[String])

/** A Chronicle task card as the card data words it (Chronicle Details
  * design, ruling D4 and slice 2): its title and its faces, front first.
  * Static, beside [[OathkeeperPresentation]]: the text is the same in every
  * game, so the projection does not carry it.
  *
  * The text is transcribed from the New Foundations card data
  * (`OATH-CHRON01a-NF` to `OATH-CHRON05-NF`). Each icon the data marks as a
  * symbol is left out and its words kept, and the fronts' "Now, flip this
  * card over." is left out, since the "Back" line stands where the flip
  * happens (implementation ruling R6). */
final case class ChronicleTaskPresentation(title: String,
    faces: Vector[ChronicleTaskFace]):
  /** What the inspector reads: the front's lines, then each later face's
    * label and lines. */
  def lines: Vector[String] = faces match
    case front +: rest =>
      front.lines ++ rest.flatMap(face => face.label +: face.lines)
    case _ => Vector.empty

object ChronicleTaskPresentation:
  private def front(lines: String*) = ChronicleTaskFace("Front", lines.toVector)
  private def back(lines: String*) = ChronicleTaskFace("Back", lines.toVector)

  val sun: ChronicleTaskPresentation = ChronicleTaskPresentation("The Sun",
    Vector(
      front(
        "**Resolve Legacies.** Reveal facedown relics held by players. " +
          "Dormant legacies become active if their goal is met. If not, " +
          "discard them. All Imperials rule Imperial sites for legacy " +
          "goals. Ties between only Imperials are friendly. All other ties " +
          "are unfriendly. Active legacies of Exiles become dormant if goal " +
          "is not met. Players discard down to 3 active legacies. Shuffle " +
          "discarded legacies into legacy deck.",
        "Hold People's Favor.",
        "Hold Darkest Secret.",
        "Rule the most sites.",
        "Hold Grand Scepter. _If none, win._",
        "Rule most cards of suit _(even advisers)_.",
        "Hold relic of highest value _(under name)_."),
      back(
        "**Crown Chancellor.** The winner becomes the Chancellor and takes " +
          "the Grand Scepter. **If an Exile won,** their newly dormant " +
          "legacies become active. Old Imperials, even in the box, become " +
          "Exiles. Discard the Reliquary. Remove Imperial warbands from " +
          "map, then mark winner's sites with Imperial warbands. **If a " +
          "Citizen won,** old Chancellor turns Citizen.",
        "**Assign Tasks.** The winner takes the Throne task card. The " +
          "player with the People's Favor takes the World task card. The " +
          "player with the Darkest Secret takes the Beacon task card. **If " +
          "no one holds a banner,** the winner may give its task to anyone. " +
          "**If a player holds both the Beacon and the World tasks,** they " +
          "must give away one of them.")))

  val throne: ChronicleTaskPresentation = ChronicleTaskPresentation(
    "The Throne", Vector(
      front(
        "**Offer Citizenship.** You may offer Citizenship to Exiles: Their " +
          "board flips. Their newly dormant legacies become active. You " +
          "take rule of their sites.",
        "**Shape Empire.** You may remove warbands from sites you rule, " +
          "except your last site. If you rule none, replace the warbands " +
          "on your site with one of yours. Then, discard all denizens " +
          "**you don't rule** except intact edifices. _Discarded ruined " +
          "edifices go to the bottom of the edifice deck in the box._",
        "**Build Edifice.** Draw the topmost edifice of each suit. You may " +
          "bury a denizen at a site you rule to replace it with the " +
          "matching drawn edifice. _Burying ignores the locked restriction " +
          "and places the card on the bottom of its respective deck._ " +
          "Reshuffle the edifice deck.",
        "**Stock Reliquary.** Add all your relic cards to the Reliquary " +
          "facedown. Discard relics in excess of five."),
      back(
        "**Scatter Relics.** Set aside the Grand Scepter and Reliquary. " +
          "Shuffle together discarded relics and relics held by players. " +
          "Deal relics from this pile facedown to fill all empty relic " +
          "slots at sites, going Hinterward. Return remaining relics to the " +
          "relic deck.",
        "**Score Lineage Points.** The winner scores 3 points + extra " +
          "points for their legacies: each legacy scores 1 point per " +
          "matching card **at sites** they rule. Then, they may give points " +
          "to other players, spending 2 points for each point given. " +
          "Finally, record scores, circle the Chancellor's score, and " +
          "underline each Citizen's score.",
        "**Swear an Oath.** If a Vision won, set the Oathkeeper goal to " +
          "match it. Otherwise, choose an Oathkeeper goal that is different " +
          "from the current one.")))

  val world: ChronicleTaskPresentation = ChronicleTaskPresentation(
    "The World", Vector(front(
      "**Separate Visions.** Set aside all 5 Vision cards.",
      "**Gather Denizens.** Shuffle all denizens from the discard piles and " +
        "players' advisers into a stack. Return all but 9 denizens from " +
        "this stack to the world deck. _Don't look at these cards._ Split " +
        "these 9 denizens into three stacks of 3 denizens each. Flip two of " +
        "these stacks faceup.",
      "**Protect and Dispossess.** Choose one faceup denizen stack to " +
        "protect: Secretly choose 1 denizen as a facedown starting adviser " +
        "you keep for the next game, and add the other 2 denizens facedown " +
        "to the world deck. Place the other two stacks, 6 denizens in " +
        "total, behind the Dispossessed divider in the box.")))

  val beacon: ChronicleTaskPresentation = ChronicleTaskPresentation(
    "The Beacon", Vector(front(
      "**Introduce Newcomers.** Choose 3 different suits. Draw 3 of the " +
        "first, then 2 of the second, and then 1 of the third from the box. " +
        "_Don't draw from the Dispossessed or Edifices sections._",
      "**Welcome a Stranger.** Examine the drawn cards. Keep one as a " +
        "facedown starting adviser for the next game, and add the other " +
        "five to the world deck.",
      "**Shape Foundation.** You may flip one altered foundation to its " +
        "normal side unless an active legacy is altering it.")))

  val stars: ChronicleTaskPresentation = ChronicleTaskPresentation(
    "The Stars", Vector(front(
      "**Write in the Stars.** The winner draws and names a constellation " +
        "on the back of the era scorepad.",
      "**Archive Rival Sites.** **At each site not ruled by the winner,** " +
        "remove any pieces and stack cards there on the site. Stack these " +
        "sites with their cards going Hinterward, and place the Empire " +
        "divider on top of them. Slide all of this into the Atlas box on " +
        "the Recent Sites side, on top of the sites already in the Atlas " +
        "box.",
      "**Clean Up.** Return all pieces to their banks. Consolidate Empire " +
        "sites going Cradleward.",
      "**Play Again or Store.** To store: Stack the Empire going " +
        "Hinterward and slide it on top of the Empire divider in the Atlas " +
        "box. Store each player's pieces and cards. _Grand Scepter and " +
        "Reliquary are stored separately._ Return Visions and the denizen " +
        "stack from the map to the World Deck section in the box.")))

  /** The five cards in the order the Chronicle Phase uses them. */
  val all: Vector[ChronicleTaskPresentation] =
    Vector(sun, throne, world, beacon, stars)
