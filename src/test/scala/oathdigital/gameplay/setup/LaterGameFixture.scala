package oathdigital.gameplay.setup

import oathdigital.application.{ChronicleFirstGamePlan, FirstGameBootstrapConfig}
import oathdigital.model._

/** A later game's Chronicle built on [[FirstGameSetupFixture]]'s: its first
  * `world` atlas sites become the Empire's, each with one stored denizen
  * taken from the world deck, and the lineages take the given roles. */
object LaterGameFixture:
  def chronicle(chancellor: LineageId, citizens: LineageId*): Chronicle =
    chronicle(chancellor, citizens.toVector, 2)

  def chronicle(chancellor: LineageId, world: Int): Chronicle =
    chronicle(chancellor, Vector.empty, world)

  def chronicle(chancellor: LineageId, citizens: Vector[LineageId],
      world: Int): Chronicle =
    val base = FirstGameSetupFixture.chronicle
    val (empire, atlas) = base.atlasBox.splitAt(world)
    val (stored, deck) = base.worldDeck.splitAt(world)
    def lineage(id: LineageId, role: Role) =
      LineageState(id, None, role, Vector.empty, Vector.empty)
    base.copy(atlasBox = atlas, worldDeck = deck,
      world = empire.zip(stored).map((site, denizen) =>
        site.copy(items = site.items :+ denizen)),
      lineages = lineage(chancellor, Role.Chancellor) +:
        citizens.map(lineage(_, Role.Citizen)))

  /** The fixture's seats with `firstPlayer` first to act, dealt from the
    * Chronicle's decks in their own order. */
  def orders(chronicle: Chronicle, firstPlayer: PlayerId): SetupOrders =
    val participants = FirstGameSetupFixture.participants
    ChronicleFirstGamePlan.dealOrder(
      FirstGameBootstrapConfig(participants, firstPlayer),
      SetupDeal.drawers(chronicle, participants), chronicle.worldDeck,
      chronicle.relicDeck, identity)
