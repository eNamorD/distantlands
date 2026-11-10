package oathdigital.gameplay.aid

import oathdigital.model._
import oathdigital.model.OathEvent.{PlanAidCleared, PlanAidSaved}

/** Evolves the Imperial Tactics facts into `ReadyGame.planAid`. The desk
  * validated each offer when it was saved, so replay only records it. */
object PlanAidEvolution:
  def evolve(ready: ReadyGame, event: PlanAidEvent)
      : Either[OathViolation, ReadyGame] = event match
    case PlanAidSaved(ally, window, offer) =>
      Right(ready.copy(planAid = ready.planAid.saved(ally, window, offer)))
    case PlanAidCleared => Right(ready.copy(planAid = ready.planAid.cleared))
