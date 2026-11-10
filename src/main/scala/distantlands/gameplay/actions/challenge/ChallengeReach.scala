package oathdigital.gameplay.actions.challenge

import oathdigital.gameplay.actions.BannerRules
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
  *  - Magician's Code: 2 secrets, gained and placed with the rest, for
  *    the Darkest Secret only.
  */
object ChallengeReach:
  /** Why School of Vines or a Sigil cannot be picked: the picks so far,
    * with Magician's Code if it could still be picked, already reach every
    * banner it would. */
  val noNewBanner: String = "Brings no new banner into reach"

  /** The secrets Magician's Code gains and places. */
  val CodeSecrets: Int = 2

  /** Why Magician's Code cannot be picked: 2 more secrets do not outbid
    * the Darkest Secret. */
  val codeTooFew: String =
    "The Darkest Secret is out of reach, even with 2 more secrets"

  /** Why Magician's Code cannot be picked: the Darkest Secret's holder is
    * away, and no pick that could still be made lifts that. */
  val codeAway: String = "The Darkest Secret's holder is not at your site"

  /** Why Magician's Code cannot be picked: the actor holds the banner. */
  val codeHeld: String = "You hold the Darkest Secret"

  /** The secrets Magician's Code adds to a Challenge of `banner`: 2 for
    * the Darkest Secret while it is picked, else none. */
  def bonus(picks: Vector[CardId], banner: Banner): Int =
    if banner == Banner.DarkestSecret && picks.contains(MagiciansCodeCard.id)
    then CodeSecrets else 0

  /** Why Magician's Code cannot be picked beside the picks among
    * `answered` and `assumed`, the reach cards the actor could still pick,
    * or `None` when the Darkest Secret is then in reach. Assuming them lets
    * the Code be picked first when a Sigil of the Eye or School of Vines
    * must also be. */
  def codeReason(ready: ReadyGame, actor: PlayerId,
      answered: Vector[Answered], assumed: Vector[CardId]): Option[String] =
    val picks = ModifierPicks.cards(answered) ++ assumed :+ MagiciansCodeCard.id
    val current = ready.game.current
    val banner = Banner.DarkestSecret
    def outbids = current.player(actor).exists(player =>
      BannerRules.playerResources(ready, player, banner) + CodeSecrets >
        BannerRules.resources(current, banner))
    if ChallengeProcedure.legalBanners(ready, actor, picks).contains(banner)
    then None
    else if BannerRules.holder(current, banner).contains(actor)
    then Some(codeHeld)
    else if outbids then Some(codeAway)
    else Some(codeTooFew)

  /** Whether `picks` let `actor` challenge `banner`, which `holder` holds,
    * without standing at the holder's pawn site. */
  def unbound(ready: ReadyGame, actor: PlayerId, picks: Vector[CardId],
      banner: Banner, holder: PlayerId): Boolean =
    (picks.contains(SchoolOfVinesCard.id) &&
      Imperials.enemies(ready, actor, holder)) ||
      (banner match
        case Banner.DarkestSecret => picks.contains(SigilOfTheEyeCard.id)
        case Banner.PeoplesFavor => picks.contains(SigilOfTheHeartCard.id))

  /** Whether picking `card` beside the picks among `answered` and
    * `assumed` brings a banner into reach. `assumed` is Magician's Code
    * when it could still be picked, so a Sigil of the Eye or School of
    * Vines may be picked first when both it and the Code are needed. */
  def widens(ready: ReadyGame, actor: PlayerId, answered: Vector[Answered],
      card: CardId, assumed: Vector[CardId]): Boolean =
    val picks = ModifierPicks.cards(answered) ++ assumed
    ChallengeProcedure.legalBanners(ready, actor, picks :+ card).size >
      ChallengeProcedure.legalBanners(ready, actor, picks).size
