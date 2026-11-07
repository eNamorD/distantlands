package oathdigital.gameplay.powers.whenplayed

import oathdigital.gameplay.actions.BannerRules
import oathdigital.gameplay.powers.NoteText
import oathdigital.gameplay.powers.banner.SecretTestamentCard
import oathdigital.gameplay.powers.targeting.{LostTongue, LostTongueCard}
import oathdigital.model._
import oathdigital.testkit.{Look, Outcomes, Table}
import oathdigital.testkit.Outcomes.assertWireRoundTrips
import oathdigital.testkit.Table.{p1, p2}

/** Favored Son (card 235), WHEN PLAYED: take the People's Favor, and
  * return its favor by its left ribbon (catalog batch 10, slice 11). */
class FavoredSonSuite extends munit.FunSuite:
  import WhenPlayedHarness._

  private val power = registered[FavoredSon.type]
  private val son = power.cardId

  private val took = NoteText.Said("took", "Took the People's Favor.",
    covers = false)

  /** The favor banks after the People's Favor's `favor` returned from
    * `ready`'s banks, leftmost least first, as a Challenge returns it. */
  private def banksAfterRibbon(ready: ReadyGame, favor: Int)
      : Map[Suit, Int] = BannerRules.addFavor(ready.banks.favor,
    BannerRules.raidFavorReturn(ready.banks.favor, favor))

  test("takes the People's Favor from its holder and returns its favor to " +
      "the banks"):
    val ready = Table.start.adviser(p1, son)
      .peoplesFavor(Some(p2), favor = 3).ready
    val done = Outcomes.finished(play(ready, power, son))
    assertEquals(Look(done.treeless).peoplesFavorHolder, Some(p1))
    assertEquals(Look(done.treeless).favorOnPeoplesFavor, 0)
    assertEquals(done.treeless.banks.favor, banksAfterRibbon(ready, 3))
    assertEquals(said(power, done.events), Vector(took))
    assertJournalRebuilds(ready, done.events, done.treeless)
    assertWireRoundTrips(done.events)

  test("takes an unheld People's Favor from the shared bank"):
    val ready = Table.start.adviser(p1, son).peoplesFavor(None, favor = 2)
      .ready
    val done = Outcomes.finished(play(ready, power, son))
    assertEquals(Look(done.treeless).peoplesFavorHolder, Some(p1))
    assertEquals(Look(done.treeless).favorOnPeoplesFavor, 0)
    assertEquals(done.treeless.banks.favor, banksAfterRibbon(ready, 2))
    assertEquals(said(power, done.events), Vector(took))

  test("a user who already holds the People's Favor changes nothing"):
    val ready = Table.start.adviser(p1, son)
      .peoplesFavor(Some(p1), favor = 2).ready
    val done = Outcomes.finished(play(ready, power, son))
    assertEquals(ops(done.events), Vector.empty)
    assertEquals(Look(done.treeless).favorOnPeoplesFavor, 2)
    assertEquals(said(power, done.events), Vector(NoteText.Said("held",
      "Already held the People's Favor.", covers = false)))

  test("when Lost Tongue refuses the take, the People's Favor and its " +
      "favor stay"):
    // p2 holds the People's Favor and Lost Tongue faceup; p1 rules no
    // Nomad card.
    val ready = Table.start.adviser(p1, son).adviser(p2, LostTongueCard)
      .peoplesFavor(Some(p2), favor = 3).ready
    val done = Outcomes.finished(playBeside(ready, power, son, LostTongue))
    assertEquals(Look(done.treeless).peoplesFavorHolder, Some(p2))
    assertEquals(Look(done.treeless).favorOnPeoplesFavor, 3)
    assertEquals(said(power, done.events), Vector(NoteText.Said("none",
      "The People's Favor did not move.", covers = false)))

  test("Secret Testament leaves the take alone: the favor goes to the " +
      "banks, not to its holder"):
    val ready = Table.start.adviser(p1, son).relic(p2, SecretTestamentCard)
      .peoplesFavor(Some(p2), favor = 3).ready
    val done = Outcomes.finished(fireWithEveryPower(ready, hook(son)))
    assertEquals(Look(done.treeless).peoplesFavorHolder, Some(p1))
    assertEquals(Look(done.treeless).favor(p2), Look(ready).favor(p2))
    assertEquals(done.treeless.banks.favor, banksAfterRibbon(ready, 3))
