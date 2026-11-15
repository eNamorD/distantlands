package distantlands.gameplay.operations

import distantlands.gameplay.cards.BrokenPeaksSite
import distantlands.model._
import distantlands.testkit.Table
import distantlands.testkit.Table.p2

/** A banner turned to its other face (Legacy in-play powers ruling 20). */
class FlipBannerSuite extends munit.FunSuite:
  private val executor = new OperationExecutor()
  private def flipped(table: Table, banner: Banner): ReadyGame =
    executor.execute(table.ready, FlipBanner(banner))
      .getOrElse(fail("the flip must apply"))

  /** p2 holds the People's Favor on the Mob with 2 favor, and the Darkest
    * Secret on the Wandering Flame with 1 secret. */
  private val held: Table = Table.start
    .peoplesFavor(Some(p2), 2, face = PeoplesFavorFace.Mob)
    .darkestSecret(Some(p2), 1, face = DarkestSecretFace.WanderingFlame)

  test("the People's Favor turns between the Mob and the Grand Council, " +
      "its holder and favor kept"):
    val once = flipped(held, Banner.PeoplesFavor)
    assertEquals(once.game.current.banners.peoplesFavor,
      PeoplesFavorState(PeoplesFavorFace.GrandCouncil, Some(p2), 2))
    assertEquals(flipped(Table.from(once), Banner.PeoplesFavor)
      .game.current.banners.peoplesFavor.active, PeoplesFavorFace.Mob)

  test("flipping to the Festival places no marker"):
    assertEquals(flipped(held, Banner.DarkestSecret).game.current.banners
      .darkestSecret, DarkestSecretState(DarkestSecretFace.Festival,
        Some(p2), 1, None))

  test("flipping away from the Festival takes its marker off"):
    val marked = held.darkestSecret(Some(p2), 1,
      face = DarkestSecretFace.Festival).festival(BrokenPeaksSite)
    assertEquals(flipped(marked, Banner.DarkestSecret).game.current.banners
      .darkestSecret, DarkestSecretState(DarkestSecretFace.WanderingFlame,
        Some(p2), 1, None))
