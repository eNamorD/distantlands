package oathdigital.gameplay

import oathdigital.model._

object PlayerResourceSources:
  def player(ready: ReadyGame, playerId: PlayerId): Either[String, PlayerState] =
    ready.game.current.player(playerId).toRight(
      s"unknown player ${playerId.value}")
