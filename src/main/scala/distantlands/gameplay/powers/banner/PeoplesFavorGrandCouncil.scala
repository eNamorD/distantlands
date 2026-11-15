package distantlands.gameplay.powers.banner

import distantlands.gameplay.SiteRulers
import distantlands.gameplay.actions.{CardPlayRules, DiscardPermission,
  PlaySites}
import distantlands.gameplay.actions.cardplay.CardPlayProcedure
import distantlands.gameplay.powerresolver.{Contribution, ContributingPower,
  PowerCtx, Transform}
import distantlands.model._

/** Grand Council (the Banner of the People's Favor, Grand Council face):
  * "When playing a card, you may play it to any Imperial site on the map,
  * and you may discard a denizen from that site first." (Foundations
  * design, slice 5).
  *
  * A persistent rule of the banner's holder, automatic, at the
  * `SearchPlayAdviser` window beside the Mob's, so it reaches every card
  * play the engine plans: a Search's, the facedown-adviser play's and a
  * power's own. It opens every Imperial site as a place to play the card,
  * after the pawn site, and adds one discard that holds only at an
  * Imperial site, the pawn site included when it is one. The generic
  * discard rules decide which cards may go: a ruined edifice may, an
  * intact edifice or a locked card may not. A full Imperial site takes the
  * play through that discard.
  *
  * An Imperial site is one the Empire's warbands hold
  * (`SiteRulers.isImperialSite`), read on the board the play is planned
  * on. The Grand Mask changes who rules the cards there, not the site.
  *
  * Its line, "May discard a card at {site} first.", travels in the
  * permission, and card play writes it for the discard it allowed, as for
  * the Mob's.
  */
object PeoplesFavorGrandCouncil extends ContributingPower:
  val id: PowerId = PowerId("banner.peoples-favor.grand-council")
  def source: RuleSourceRef = RuleSourceRef.Banner(Banner.PeoplesFavor.key)

  /** The player plays a card while holding the banner, Grand Council face
    * up. */
  override def applicable(ctx: PowerCtx): Boolean =
    val favor = ctx.state.game.current.banners.peoplesFavor
    favor.active == PeoplesFavorFace.GrandCouncil &&
      favor.holder.contains(ctx.actor)

  /** Whether the Empire's warbands hold `site` on the board the play is
    * planned on. */
  val imperialSite: (ReadyGame, SiteId) => Boolean =
    (ready, site) => SiteRulers.isImperialSite(ready, site)

  private val notes = NoteSet(id)
  notes.include(CardPlayRules.notes)
  override def noteKeys: Vector[NoteKey] = notes.declared

  def contributions: Map[PowerWindow, Vector[Contribution]] =
    Map(PowerWindow.SearchPlayAdviser -> Vector(Transform((ctx, children) =>
      ctx.operation match
        case tree: CardPlayProcedure.PlacementTree =>
          tree.adjust(children)(_.openSites(PlaySites(imperialSite))
            .permitSiteDiscard(DiscardPermission.lined(id,
              PowerSourceRef.Banner(Banner.PeoplesFavor),
              appliesAt = imperialSite)))
        case _ => children)))
