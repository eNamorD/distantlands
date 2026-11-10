package distantlands.testkit

import distantlands.gameplay.phases.rest.RoundEndRandomPort
import distantlands.model.PlayerId

/** Stubs of RoundEndRandomPort (canned answers, no record of calls). A stub
  * fails the test when asked for what it does not fix, so a test that
  * fixes the tied winner proves no die was rolled, and one that fixes the
  * die proves no winner was drawn. `RoundEndRandomPortSuite` holds them to
  * the port's promise. */
object RoundEndChoices:
  /** Picks the tied winner with `pick`; rolls no end die. */
  def picking(pick: Vector[PlayerId] => PlayerId): RoundEndRandomPort =
    new RoundEndRandomPort:
      def choose(candidates: Vector[PlayerId]): PlayerId = pick(candidates)
      def endDie(): Int =
        munit.Assertions.fail("this test rolls no end die")

  /** Rolls `face` on the end die; draws no tied winner. */
  def endDie(face: Int): RoundEndRandomPort = new RoundEndRandomPort:
    def choose(candidates: Vector[PlayerId]): PlayerId =
      munit.Assertions.fail("this test draws no tied winner")
    def endDie(): Int = face

  /** Always picks the first candidate. */
  val firstCandidate: RoundEndRandomPort = picking(_.head)

  /** Always picks the last candidate. */
  val lastCandidate: RoundEndRandomPort = picking(_.last)
