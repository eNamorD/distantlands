package distantlands.application

import distantlands.protocol.GameIntent

class GameIntentMapperUndoSuite extends munit.FunSuite:
  test("an Undo intent maps to Undo"):
    assertEquals(GameIntentMapper.map(GameIntent.Undo), Right(PlayerIntent.Undo))

  test("a Redo intent maps to Redo"):
    assertEquals(GameIntentMapper.map(GameIntent.Redo), Right(PlayerIntent.Redo))
