package distantlands.application

import java.security.SecureRandom

/**
 * The application-layer source of randomness for Chronicle setup: every
 * resolved order is recorded in the start event, so the engine and replay
 * stay RNG-free (2026-09-21 Chronicle design, "Randomness"). Production
 * shuffles from a `SecureRandom`.
 */
trait ChronicleRandomPort:
  def shuffle[A](values: Vector[A]): Vector[A]
object ChronicleRandomPort:
  val random: ChronicleRandomPort = new ChronicleRandomPort:
    private val rng = new scala.util.Random(new SecureRandom())
    def shuffle[A](values: Vector[A]): Vector[A] = rng.shuffle(values)
