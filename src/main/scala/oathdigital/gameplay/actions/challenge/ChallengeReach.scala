package oathdigital.gameplay.actions.challenge

import oathdigital.gameplay.powerresolver.ModifierPicks
import oathdigital.model._

/** What the Challenge modifiers change about a Challenge (catalog batch
  * 10, slice 11), read from the walk's picks, which a park keeps and
  * replay rebuilds, as `RecoverProcedure` reads Drowned Library's:
  *
  *  - School of Vines: a banner an enemy holds needs no pawn at its
  *    holder's site.
  *  - Sigil of the Eye: the Darkest Secret needs none, whoever holds it;
  *    Sigil of the Heart: the People's Favor.
  */
object ChallengeReach:
  /** Why School of Vines or a Sigil cannot be picked: the picks so far
    * already reach every banner it would. */
  val noNewBanner: String = "Brings no new banner into reach"

  /** Whether `picks` let `actor` challenge `banner`, which `holder` holds,
    * without standing at the holder's pawn site. */
  def unbound(ready: ReadyGame, actor: PlayerId, picks: Vector[CardId],
      banner: Banner, holder: PlayerId): Boolean =
    (picks.contains(SchoolOfVinesCard.id) &&
      Imperials.enemies(ready, actor, holder)) ||
      (banner match
        case Banner.DarkestSecret => picks.contains(SigilOfTheEyeCard.id)
        case Banner.PeoplesFavor => picks.contains(SigilOfTheHeartCard.id))

  /** Whether picking `card` beside the picks among `answered` brings a
    * banner into reach. */
  def widens(ready: ReadyGame, actor: PlayerId, answered: Vector[Answered],
      card: CardId): Boolean =
    val picks = ModifierPicks.cards(answered)
    ChallengeProcedure.legalBanners(ready, actor, picks :+ card).size >
      ChallengeProcedure.legalBanners(ready, actor, picks).size
