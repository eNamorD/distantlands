package oathdigital.gameplay.phases.rest

import oathdigital.model.PlayerId

/** The randomness of a round's end: the purple end die of a Stable Regime
  * check (CR p. 19), and the winner War Exhaustion picks in a game with no
  * Empire and no Oathkeeper. Production rolls; tests fix the answers. */
trait RoundEndRandomPort:
  def choose(candidates: Vector[PlayerId]): PlayerId
  /** One roll of the end die, from 1 to 6. */
  def endDie(): Int
object RoundEndRandomPort:
  val random: RoundEndRandomPort = new RoundEndRandomPort:
    private val rng = new scala.util.Random()
    def choose(candidates: Vector[PlayerId]): PlayerId =
      candidates(rng.nextInt(candidates.size))
    def endDie(): Int = rng.nextInt(6) + 1
