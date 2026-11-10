package oathdigital.gameplay.setup

import oathdigital.catalog.ExecutableCatalog
import oathdigital.gameplay.operations.TheGrandScepterCard
import oathdigital.model._

/** The card accounting every Chronicle passes (Chronicle steps design, "Exit
  * and extraction"). Game start checks `knownAndUnique` before it deals;
  * the Chronicle Phase checks both before it journals the evolved record.
  * Denizens and edifices a Chronicle does not list are in the suited
  * reserves and the edifice deck, which are derived, so only sites and
  * relics can go missing. */
object ChronicleAccounting:
  /** Every id is a known card of the kind its field holds, and no card or
    * site is stored twice, across every field. */
  def knownAndUnique(catalog: ExecutableCatalog, chronicle: Chronicle)
      : Either[String, Unit] =
    val siteIds = catalog.sites.map(_.id).toSet
    val edificeIds = catalog.edifices.map(e => EdificeId(e.id.value)).toSet
    val denizenIds = catalog.denizens.map(d => DenizenId(d.id.value)).toSet
    val relicIds = catalog.relics.map(r => RelicId(r.id.value)).toSet
    val sites = (chronicle.atlasBox ++ chronicle.world).map(_.site)
    val items = (chronicle.atlasBox ++ chronicle.world).flatMap(_.items)
    val advisers = chronicle.lineages.flatMap(_.startingAdvisers).map(_.id)
    val keptRelics = chronicle.lineages.flatMap(_.startingRelics)
    val stored = items ++ advisers ++ keptRelics
    val denizens = chronicle.worldDeck ++ chronicle.dispossessed ++
      stored.collect { case id: DenizenId => id }
    val relics = chronicle.relicDeck ++ chronicle.reliquary ++
      stored.collect { case id: RelicId => id }
    val edifices = stored.collect { case id: EdificeId => id }
    val others = stored.filter {
      case _: DenizenId | _: RelicId | _: EdificeId => false
      case _ => true
    }
    first(Vector(
      sites.find(!siteIds(_)).map(id => s"unknown atlas site ${id.value}"),
      duplicate(sites).map(id => s"duplicate atlas site ${id.value}"),
      edifices.find(!edificeIds(_)).map(id => s"unknown edifice ${id.value}"),
      denizens.find(!denizenIds(_)).map(id => s"unknown denizen ${id.value}"),
      duplicate(chronicle.worldDeck)
        .map(id => s"duplicate world-deck denizen ${id.value}"),
      relics.find(!relicIds(_)).map(id => s"unknown relic ${id.value}"),
      duplicate(chronicle.relicDeck).map(id => s"duplicate relic ${id.value}"),
      others.headOption.map(id =>
        s"${id.value} is a ${id.kind}, which the Chronicle does not store"),
      duplicate(denizens).map(id => s"denizen ${id.value} is stored twice"),
      duplicate(relics).map(id => s"relic ${id.value} is stored twice"),
      duplicate(edifices).map(id => s"edifice ${id.value} is stored twice"),
      relics.find(_ == TheGrandScepterCard.id)
        .map(_ => "the Grand Scepter is not stored")))

  /** `evolved` holds exactly the sites and relics `baseline` holds. The
    * Chronicle Phase moves them; it never adds or loses one. */
  def conserves(baseline: Chronicle, evolved: Chronicle)
      : Either[String, Unit] =
    def sites(chronicle: Chronicle): Vector[SiteId] =
      (chronicle.atlasBox ++ chronicle.world).map(_.site)
    def relics(chronicle: Chronicle): Vector[RelicId] =
      chronicle.relicDeck ++ chronicle.reliquary ++
        (chronicle.atlasBox ++ chronicle.world).flatMap(_.items)
          .collect { case id: RelicId => id } ++
        chronicle.lineages.flatMap(_.startingRelics)
    val (beforeSites, afterSites) = (sites(baseline), sites(evolved).toSet)
    val (beforeRelics, afterRelics) = (relics(baseline), relics(evolved).toSet)
    first(Vector(
      beforeSites.find(!afterSites(_)).map(id => s"site ${id.value} is missing"),
      sites(evolved).find(!beforeSites.contains(_))
        .map(id => s"site ${id.value} was not in the baseline"),
      beforeRelics.find(!afterRelics(_))
        .map(id => s"relic ${id.value} is missing"),
      relics(evolved).find(!beforeRelics.contains(_))
        .map(id => s"relic ${id.value} was not in the baseline")))

  private def first(problems: Vector[Option[String]]): Either[String, Unit] =
    problems.flatten.headOption.toLeft(())

  private def duplicate[A](values: Vector[A]): Option[A] =
    val seen = scala.collection.mutable.HashSet.empty[A]
    values.find(value => !seen.add(value))
