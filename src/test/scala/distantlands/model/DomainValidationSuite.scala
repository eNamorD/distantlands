package distantlands.model

class DomainValidationSuite extends munit.FunSuite:
  import TestGameFixtures._
  import distantlands.testkit.Table

  test("campaign maps retain their structural identities and required slots"):
    // Patched with copy, not a Table step: the state is invalid on purpose.
    val wrongId = LineageId("wrong-state-id")
    val invalid = game.copy(
      campaign = game.campaign.copy(
        foundations = game.campaign.foundations - FoundationNumber.VI,
        lineages = Map(lineageId -> lineage.copy(id = wrongId))
      )
    )

    val problems = DomainValidation.validate(invalid)

    assert(problems.contains(DomainProblem.MissingFoundation(FoundationNumber.VI)))
    assert(
      problems.contains(DomainProblem.LineageKeyMismatch(lineageId, wrongId))
    )

  test("a site definition occupies only one place in the world"):
    // sites.head is on the map, so an Atlas copy of it is also
    // SiteInMapAndAtlas.
    val duplicateStoredSite = AtlasEntry.StoredSite(
      sites.head,
      Vector.empty,
      Vector.empty
    )
    val invalid = game.copy(
      campaign = game.campaign.copy(
        atlas = AtlasState(Vector(duplicateStoredSite, duplicateStoredSite))
      )
    )

    val problems = DomainValidation.validate(invalid)

    assert(problems.contains(DomainProblem.DuplicateAtlasSite(sites.head)))
    assert(problems.contains(DomainProblem.SiteInMapAndAtlas(sites.head)))

  test("holders and Exile forces reference current aggregate identities"):
    // Patched with copy, not a Table step: the state is invalid on purpose.
    val absentPlayer = PlayerId("absent-player")
    val absentLineage = LineageId("absent-lineage")
    val occupied = game.current.map.sites(sites.head).copy(
      forces = SiteForces.Occupied(ForceKind.Exile(absentLineage), 1)
    )
    val invalid = game.copy(
      current = game.current.copy(
        map = game.current.map.copy(
          sites = game.current.map.sites.updated(sites.head, occupied)
        ),
        banners = game.current.banners.copy(
          peoplesFavor =
            game.current.banners.peoplesFavor.copy(holder = Some(absentPlayer)),
          darkestSecret =
            game.current.banners.darkestSecret.copy(holder = Some(absentPlayer))
        ),
        title = game.current.title.copy(holder = Some(absentPlayer))
      )
    )

    val problems = DomainValidation.validate(invalid)

    assert(
      problems.contains(
        DomainProblem.UnknownForceLineage(sites.head, absentLineage)
      )
    )
    assert(
      problems.contains(DomainProblem.UnknownPeoplesFavorHolder(absentPlayer))
    )
    assert(
      problems.contains(DomainProblem.UnknownDarkestSecretHolder(absentPlayer))
    )
    assert(problems.contains(DomainProblem.UnknownTitleHolder(absentPlayer)))

  test("duplicate identity problems have deterministic ID order"):
    // The advisers are cleared so the copies repeat only the player ids, not
    // the fixture's adviser card.
    val alpha = player.copy(
      player = PlayerId("alpha"),
      advisers = Vector.empty
    )
    val zeta = player.copy(
      player = PlayerId("zeta"),
      advisers = Vector.empty
    )
    val invalid = game.copy(
      current = game.current.copy(
        players = Vector(zeta, alpha, zeta, alpha)
      )
    )

    val duplicates = DomainValidation
      .validate(invalid)
      .collect { case problem: DomainProblem.DuplicatePlayer => problem }

    assertEquals(
      duplicates,
      Vector(
        DomainProblem.DuplicatePlayer(PlayerId("alpha")),
        DomainProblem.DuplicatePlayer(PlayerId("zeta"))
      )
    )

  test("an Imperial player never holds the Usurper side of the title"):
    // Built with Table steps, then validated: the state breaks a rule
    // on purpose. Becoming Imperial flips the title back to its
    // Oathkeeper side (Empire slice 3), so no rule can reach it.
    val usurperCitizen = Table.start.imperial(Table.p1, Table.p2)
      .oathkeeper(Some(Table.p2), TitleSide.Usurper).ready.game
    assertEquals(imperialUsurpers(usurperCitizen),
      Vector(DomainProblem.ImperialUsurper(Table.p2)))

  test("a Chancellor holding the Usurper side is flagged outside the Chronicle"):
    val usurperChancellor = Table.start.imperial(Table.p2)
      .turn(Table.p2, Phase.Act)
      .oathkeeper(Some(Table.p2), TitleSide.Usurper).ready.game
    assertEquals(imperialUsurpers(usurperChancellor),
      Vector(DomainProblem.ImperialUsurper(Table.p2)))

  test("the Chronicle Phase may hold an Imperial player on the Usurper side"):
    // The Chronicle crowns the Usurper winner after the game has ended.
    val crowned = Table.start.imperial(Table.p2)
      .turn(Table.p2, Phase.Chronicle)
      .oathkeeper(Some(Table.p2), TitleSide.Usurper).ready.game
    assertEquals(imperialUsurpers(crowned), Vector.empty)

  test("an Imperial player may hold the Oathkeeper side"):
    val oathkeeperCitizen = Table.start.imperial(Table.p1, Table.p2)
      .oathkeeper(Some(Table.p2)).ready.game
    assertEquals(imperialUsurpers(oathkeeperCitizen), Vector.empty)

  test("the round's starter is a seated player"):
    // Built with a Table step, then validated: the state breaks a rule
    // on purpose.
    val unseated = Table.start.roundStarter(PlayerId("nobody")).ready.game
    assert(DomainValidation.validate(unseated).contains(
      DomainProblem.UnknownRoundStarter(PlayerId("nobody"))))

  private def imperialUsurpers(game: OathGame): Vector[DomainProblem] =
    DomainValidation.validate(game)
      .collect { case problem: DomainProblem.ImperialUsurper => problem }
