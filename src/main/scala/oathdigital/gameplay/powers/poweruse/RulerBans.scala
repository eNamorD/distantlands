package oathdigital.gameplay.powers.poweruse

import oathdigital.gameplay.{CardRule, SiteRulers}
import oathdigital.gameplay.powerresolver.{PowerUseRefusal, Waiver}
import oathdigital.model._

/** The ban a site card holds over its ruler's enemies (Spell Breaker,
  * Forest Council, City Wall; catalog batch 8, slice 10). The ruler is the
  * card's ruler (`CardRule.rulerOf`), when that is a player or bandits:
  * nobody is
  * restricted at an unruled site. The Empire's sites resolve to its
  * governor. Every player but a player ruler is an enemy, and every player
  * is an enemy of bandits.
  * Only a player ruler, or the Bandit Crown's holder for a bandit ruler, may
  * consent (consent design, ruling 4; catalog batch 11, ruling 5). */
private[powers] object RulerBans:
  def ruler(ready: ReadyGame, card: DenizenId): Option[SiteRuler] =
    CardRule.rulerOf(ready, card)

  def enemy(ruler: SiteRuler, player: PlayerId): Boolean =
    ruler != SiteRuler.Player(player)

  /** The ban's waiver, for the consent grid: its granter is whoever
    * `SiteRulers.consentGranter` names for `card`, and `asked` is its reason
    * when asked. */
  def waiver(card: DenizenId, asked: NoteKey): Waiver =
    val source = PowerSourceRef.Card(card)
    Waiver(SiteRulers.consentGranter(_, card), source,
      (_, _) => asked(source))

  /** `power`'s refusal of `requester`, waivable under `covers` by whoever
    * `SiteRulers.consentGranter` names. */
  def refusal(ready: ReadyGame, power: PowerId, card: DenizenId,
      requester: PlayerId, covers: ConsentKey, plain: NoteKey,
      asked: NoteKey, code: String): PowerUseRefusal =
    PowerUseRefusal(power, plain(PowerSourceRef.Card(card)),
      reason(ready, power, card, requester, covers, plain, asked, code))

  /** `power`'s refusal of `requester` on the operation path, waivable under
    * `covers` by whoever `SiteRulers.consentGranter` names. Under bandits
    * its waiver names the Bandit Crown's holder, if any; with none, an
    * option it hides shows `plain`. */
  def reason(ready: ReadyGame, power: PowerId, card: DenizenId,
      requester: PlayerId, covers: ConsentKey, plain: NoteKey,
      asked: NoteKey, code: String): OperationReason =
    val source = PowerSourceRef.Card(card)
    OperationReason(code, s"${power.value} refuses ${requester.value}",
      OperationReasonKind.Impossible, Some(OperationWaiver(power,
        SiteRulers.consentGranter(ready, card), covers, asked(source),
        plain(source))))
