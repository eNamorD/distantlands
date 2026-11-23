package distantlands.application

import distantlands.catalog.ExecutableCatalog
import distantlands.gameplay.operations.TheGrandScepterCard
import distantlands.model._

sealed trait ChronicleGeneratorFailure extends Product with Serializable
object ChronicleGeneratorFailure:
  final case class WrongSiteCount(actual: Int) extends ChronicleGeneratorFailure
  final case class TooFewSuitDenizens(suit: Suit, denizens: Int)
      extends ChronicleGeneratorFailure
  final case class InvariantViolated(detail: String) extends ChronicleGeneratorFailure
  final case class InvalidName(reason: String) extends ChronicleGeneratorFailure

/**
 * Produces a random first-game Chronicle (2026-09-21 Chronicle design, "The
 * first-game generator"): all 24 sites shuffled into the atlas box, each
 * Homeland carrying the lowest-id edifice of its suit; a 60-denizen world
 * deck, 10 random denizens per suit; 12 dispossessed denizens, 2 per suit,
 * drawn from what the 60 left behind; and the full ordinary relic deck.
 * `random` shuffles both decks. Self-validates the counts before returning.
 */
object FirstGameChronicleGenerator:
  import ChronicleGeneratorFailure._

  def generate(catalog: ExecutableCatalog, name: String,
      random: ChronicleRandomPort)
      : Either[ChronicleGeneratorFailure, Chronicle] =
    for
      _ <- Chronicle.nameProblem(name).map(InvalidName(_)).toLeft(())
      atlas <- atlasBox(catalog, random)
      pools <- denizenPools(catalog, random)
      (worldPool, dispossessedPool) = pools
      relicPool = catalog.relics.filter(_.id != TheGrandScepterCard.id)
        .map(_.id)
      chronicle = Chronicle(
        name,
        atlas,
        worldDeck = random.shuffle(worldPool),
        relicDeck = random.shuffle(relicPool),
        era = EraState(20, Vector.empty),
        oathkeeperGoal = OathkeeperGoal.Supremacy,
        dispossessed = dispossessedPool)
      _ <- validate(catalog, chronicle)
    yield chronicle

  private def atlasBox(catalog: ExecutableCatalog, random: ChronicleRandomPort)
      : Either[ChronicleGeneratorFailure, Vector[StoredSite]] =
    val sites = catalog.sites.map(_.id)
    if sites.size != 24 then Left(WrongSiteCount(sites.size))
    else Right(random.shuffle(sites).map { siteId =>
      catalog.site(siteId).get.homeland match {
        case None => StoredSite(siteId)
        case Some(suit) =>
          StoredSite(siteId, Vector(edificeForHomeland(catalog, suit)))
      }
    })

  /** The lowest-id edifice of `suit`. */
  private def edificeForHomeland(catalog: ExecutableCatalog, suit: Suit)
      : EdificeId =
    EdificeId(catalog.edifices.filter(_.suit == suit).map(_.id.value).min)

  private val PerSuit = 10
  private val DispossessedPerSuit = 2

  /** Per suit: 10 random denizens go into the world deck, and 2 of the
    * denizens left out are dispossessed. */
  private def denizenPools(catalog: ExecutableCatalog,
      random: ChronicleRandomPort)
      : Either[ChronicleGeneratorFailure, (Vector[DenizenId], Vector[DenizenId])] =
    Suit.all.foldLeft[Either[ChronicleGeneratorFailure,
        (Vector[DenizenId], Vector[DenizenId])]](Right(Vector.empty -> Vector.empty)):
      (acc, suit) =>
      acc.flatMap { case (worldPool, dispossessedPool) =>
        val suited = catalog.denizens.filter(_.suit == suit)
          .map(d => DenizenId(d.id.value))
        if suited.size < PerSuit + DispossessedPerSuit then
          Left(TooFewSuitDenizens(suit, suited.size))
        else
          val world = random.shuffle(suited).take(PerSuit)
          val left = random.shuffle(suited.filterNot(world.contains))
          Right((worldPool ++ world,
            dispossessedPool ++ left.take(DispossessedPerSuit)))
      }

  private def validate(catalog: ExecutableCatalog, chronicle: Chronicle)
      : Either[ChronicleGeneratorFailure, Unit] =
    def perSuitCount(ids: Vector[DenizenId]): Map[Suit, Int] =
      Suit.all.map(suit => suit -> ids.count(id => catalog.suitOf(id).contains(suit))).toMap

    if chronicle.worldDeck.size != 60 then
      Left(InvariantViolated(
        s"world deck must have 60 denizens, has ${chronicle.worldDeck.size}"))
    else if chronicle.worldDeck.distinct.size != 60 then
      Left(InvariantViolated("world deck denizens must be unique"))
    else if perSuitCount(chronicle.worldDeck).values.exists(_ != 10) then
      Left(InvariantViolated("world deck must have 10 denizens per suit"))
    else if chronicle.dispossessed.size != 12 then
      Left(InvariantViolated(
        s"dispossessed must have 12 denizens, has ${chronicle.dispossessed.size}"))
    else if chronicle.dispossessed.distinct.size != 12 then
      Left(InvariantViolated("dispossessed denizens must be unique"))
    else if perSuitCount(chronicle.dispossessed).values.exists(_ != 2) then
      Left(InvariantViolated("dispossessed must have 2 denizens per suit"))
    else if (chronicle.worldDeck.toSet intersect chronicle.dispossessed.toSet).nonEmpty then
      Left(InvariantViolated("world deck and dispossessed must not overlap"))
    else Right(())
