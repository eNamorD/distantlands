package oathdigital.frontend

class ChronicleFilesSuite extends munit.FunSuite:
  test("a download is named after the Chronicle, with unsafe characters " +
      "replaced"):
    assertEquals(ChronicleFiles.filename("Ashen Crown"),
      "Ashen Crown.chronicle.json")
    assertEquals(ChronicleFiles.filename("a/b:c?"), "a-b-c-.chronicle.json")
