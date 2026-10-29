package oathdigital.model

import oathdigital.gameplay.setup.FirstGameSetupFixture.chronicle

/** A Chronicle's name (Chronicle browsing design, "Model"). */
class ChronicleSuite extends munit.FunSuite:
  test("a name is trimmed and has 1 to 80 characters"):
    assertEquals(Chronicle.nameProblem("a" * 80), None)
    assertEquals(Chronicle.nameProblem(""), Some("a Chronicle needs a name"))
    assertEquals(Chronicle.nameProblem(" Ash"),
      Some("a Chronicle name has no leading or trailing spaces"))
    assertEquals(Chronicle.nameProblem("a" * 81),
      Some("a Chronicle name has at most 80 characters"))

  test("a Chronicle refuses a name with a problem"):
    intercept[IllegalArgumentException](chronicle.copy(name = "Ash "))
