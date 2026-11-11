package distantlands.catalog

import distantlands.model.{FoundationFace, FoundationNumber, PowerId}

/** One face of a foundation setup card: the id rule sources and the Game
  * Log know it by, its printed name, and its text as the card prints it, in
  * the catalog's markup with a blank line between paragraphs. Gameplay
  * never reads `text`: behaviour lives in setup and in the procedures that
  * read the face in use, and follows the rulebook where it says more than
  * the card.
  */
final case class FoundationCardFace(id: PowerId, name: String, text: String):
  require(name.trim.nonEmpty, "a foundation face needs its printed name")
  require(text.trim.nonEmpty, "a foundation face needs its rules text")

/** A foundation setup card: its number and its two faces. */
final case class FoundationCard(number: FoundationNumber,
    normal: FoundationCardFace, altered: FoundationCardFace):
  def face(which: FoundationFace): FoundationCardFace = which match
    case FoundationFace.Normal => normal
    case FoundationFace.Altered => altered

/** The six foundation setup cards of New Foundations, I to VI (Foundations
  * design, slice 1). They are fixed, so they stand beside
  * [[ExecutableCatalog]] and not in it: no test swaps one.
  */
object FoundationCards:
  private def printed(number: String, slug: String, name: String,
      paragraphs: String*): FoundationCardFace =
    FoundationCardFace(PowerId(s"foundation.$number.$slug"), name,
      paragraphs.mkString("\n\n"))

  /** Both faces of V, and of VI, end with the same paragraph. */
  private def bannerGoes(banner: String): String =
    s"If the Oathkeeper goal is the $banner, give the banner to the " +
      "Chancellor, if any. Otherwise, place it in the shared bank."

  val all: Vector[FoundationCard] = Vector(
    FoundationCard(FoundationNumber.I,
      printed("i", "imperial-maps", "Imperial Maps",
        "The Chancellor fills the remaining empty site slots going " +
          "Hinterward.",
        "They must fill these slots with Recent Sites or Forgotten Sites " +
          "from the Atlas, but not both.",
        "**Bandits** are placed on the site equal to its capacity.",
        "Edifices start **ruined**.",
        "**Fill the site's empty relic slots** by drawing relics from the " +
          "relic deck."),
      printed("i", "wide-horizons", "Wide Horizons",
        "**Do not fill empty site slots!**",
        "In this game, players can spend 1 Supply to **Explore** if their " +
          "pawn is at the Hintermost site card (rulebook page 9).",
        "Explore works like a Recover action with a **difficulty of 4**.",
        "If you succeed, place a site on the next Hinterward slot, " +
          "populate it as described on the back of this card, and then " +
          "place your pawn on the site.",
        "_Keep this card out as a reminder._")),
    FoundationCard(FoundationNumber.II,
      printed("ii", "powerful-tribes", "Powerful Tribes",
        "Place [favor] [favor] [favor] on each suit's favor bank.",
        "If you have five or more players, add an extra [favor] to each " +
          "favor bank.",
        "Then, for each denizen on the map, add an extra [favor] to the " +
          "matching favor bank."),
      printed("ii", "movers-and-shakers", "Movers and Shakers",
        "Each player takes 5 more favor [favor] [favor] [favor] [favor] " +
          "[favor] from the shared bank. _Players usually start with 6._",
        "Then, for each denizen on the map, place [favor] on the matching " +
          "favor bank.")),
    FoundationCard(FoundationNumber.III,
      printed("iii", "quiet-ambitions", "Quiet Ambitions",
        "Each player draws 2 legacy cards, keeps one facedown in their " +
          "play area, and returns the other to the box."),
      printed("iii", "public-ambitions", "Public Ambitions",
        "Deal 4 legacy cards to a faceup row near the map.",
        "Everyone competes to activate these public legacies in the " +
          "Chronicle Phase. For these, the outgoing Chancellor breaks ties " +
          "between Imperials alone. Discard public legacies that aren't " +
          "activated.")),
    FoundationCard(FoundationNumber.IV,
      printed("iv", "ties-of-fellowship", "Ties of Fellowship",
        "Each player who has no starting advisers draws 3 denizens " +
          "facedown from the starting denizen stack.",
        "_You will choose one as an adviser later. Players sometimes carry " +
          "over advisers from the previous game._",
        "Then, deal 2 denizens from the starting denizen stack facedown to " +
          "each discard pile."),
      printed("iv", "teeming-world", "Teeming World",
        "Each player draws facedown denizens from the starting denizen " +
          "stack until they have three starting advisers.",
        "Discard the rest of the starting denizen stack as evenly as " +
          "possible, going from Cradle to Provinces to Hinterland. _The " +
          "world deck will only have Visions!_")),
    FoundationCard(FoundationNumber.V,
      printed("v", "mobs-favor", "Mob's Favor",
        "Set the People's Favor on its **Mob** side. Place [favor] on it " +
          "from the shared bank.",
        bannerGoes("People's Favor")),
      printed("v", "grand-council", "Grand Council",
        "Flip the People's Favor to its **Grand Council** side. Place " +
          "[favor] on it from the shared bank.",
        bannerGoes("People's Favor"))),
    FoundationCard(FoundationNumber.VI,
      printed("vi", "wandering-flame", "Wandering Flame",
        "Set the Darkest Secret on its **Wandering Flame** side. Place " +
          "[secret] on it from the shared bank.",
        bannerGoes("Darkest Secret")),
      printed("vi", "festival", "Festival",
        "Flip the Darkest Secret to its **Festival side**. Place [secret] " +
          "on it from the shared bank. Place the Festival marker nearby.",
        bannerGoes("Darkest Secret"))))

  private val byNumber: Map[FoundationNumber, FoundationCard] =
    all.map(card => card.number -> card).toMap
  private val byId: Map[PowerId, FoundationCardFace] =
    all.flatMap(card => Vector(card.normal, card.altered))
      .map(face => face.id -> face).toMap

  def card(number: FoundationNumber): FoundationCard = byNumber(number)

  /** The face of `number` that lies up when the card shows `which`. */
  def face(number: FoundationNumber, which: FoundationFace)
      : FoundationCardFace = card(number).face(which)

  /** The face that carries `id`, if `id` is a foundation face's. */
  def faceWithId(id: PowerId): Option[FoundationCardFace] = byId.get(id)
