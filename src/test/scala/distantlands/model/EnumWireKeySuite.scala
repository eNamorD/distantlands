package distantlands.model

/** Keys that journaled events and the client protocol carry. Renaming one
  * breaks saved games or the frontend, so each is spelled out here.
  */
class EnumWireKeySuite extends munit.FunSuite:
  test("suit, region and major action keys stay as the wire spells them"):
    assertEquals(Suit.all.map(_.key),
      Vector("discord", "arcane", "order", "hearth", "beast", "nomad"))
    assertEquals(Region.all.map(_.key),
      Vector("cradle", "provinces", "hinterland"))
    assertEquals(MajorActionType.all.map(_.key), Vector("search", "travel",
      "campaign", "muster", "trade", "forge", "recover", "challenge",
      "explore"))

  test("triggered procedure keys stay as journaled walker events spell them"):
    assertEquals(TriggeredProcedureRef.all.map(_.key),
      Vector("oathkeeper", "setup", "forced-wake", "follow-up-offer",
        "wake-menu", "rest-menu", "act-menu", "chronicle-begin",
        "chronicle-sun", "chronicle-throne", "chronicle-world",
        "chronicle-beacon", "chronicle-stars", "chronicle-finish",
        "quest-first-wake", "round-end-arbiter", "game-end-conspirator"))

  test("every older triggered procedure key still decodes, and the first " +
      "Wake's is new (R32)"):
    Vector("oathkeeper", "setup", "forced-wake", "follow-up-offer",
        "wake-menu", "rest-menu", "act-menu", "chronicle-begin",
        "chronicle-sun", "chronicle-throne", "chronicle-world",
        "chronicle-beacon", "chronicle-stars", "chronicle-finish")
      .foreach(key => assertEquals(TriggeredProcedureRef.all
        .find(_.key == key).map(_.key), Some(key)))
    assertEquals(TriggeredProcedureRef.all.find(_.key == "quest-first-wake"),
      Some(TriggeredProcedureRef.QuestFirstWake))
    // Arbiter's question before the end die (Legacy in-play powers R11).
    assertEquals(TriggeredProcedureRef.all.find(_.key == "round-end-arbiter"),
      Some(TriggeredProcedureRef.Arbiter))


  test("Phase.Chronicle round-trips through its wire key"):
    assertEquals(Phase.fromKey("chronicle"), Some(Phase.Chronicle))
    assertEquals(Phase.Chronicle.key, "chronicle")

  test("Phase.Setup round-trips through its wire key"):
    assertEquals(Phase.fromKey("setup"), Some(Phase.Setup))
    assertEquals(Phase.Setup.key, "setup")
