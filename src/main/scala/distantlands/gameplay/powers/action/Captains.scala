package oathdigital.gameplay.powers.action

import oathdigital.catalog.{Denizen, ExecutableCatalog, PrintedPower,
  SiteOnly}
import oathdigital.gameplay.PowerAccess
import oathdigital.model._

object CaptainsCard extends Denizen(DenizenId("115"), "Captains", Suit.Order) with SiteOnly:
  val power = PrintedPower(PowerId("denizen.captains"),
    persistent = false, cost = Cost(favor = 1),
    text = "**ACTION:** Campaign at any site you rule. Act as if your " +
      "pawn is there. Spend no Supply and add your warbands there " +
      "to your force.")
  val powers: Vector[PrintedPower] = Vector(power)

/** Captains (card 115), a site-only ACTION, one favor: "Campaign at any
  * site you rule. Act as if your pawn is there. Spend no Supply and add
  * your warbands there to your force." Its sites are the ones its user
  * rules as a player (catalog batch 8, slice 13, ruling 3), the pawn's
  * site among them. */
case object Captains extends CampaignFromSite(CaptainsCard.power, CaptainsCard.id):
  val siteDecisionId: String = "power.captains.site"
  private val notes = NoteSet(id)
  /** "Campaign at {site}.": the use's own line. */
  val campaignAt: NoteKey = notes.key(NoteKey.Used, Vector(
    NotePart.Text("Campaign at "), NotePart.Arg(0), NotePart.Text(".")))
  override val noteKeys: Vector[NoteKey] = notes.declared

  protected def name: String = CaptainsCard.name
  protected def noSite: String =
    "No site you rule where a Campaign could start"

  protected def sites(catalog: ExecutableCatalog, ready: ReadyGame,
      player: PlayerId): Vector[SiteId] =
    val ruled = PowerAccess.ruledSites(ready, player)
    ready.game.current.map.inPlay.filter(ruled.contains)
