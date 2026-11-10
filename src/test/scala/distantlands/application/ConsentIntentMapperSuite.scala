package oathdigital.application

import oathdigital.model._
import oathdigital.protocol.{GameIntent, WalkerStartArgWire}

/** The consent intents map to their model intents; the gateway binds the
  * sender as the actor (TrustedSeatRoutesSuite). */
class ConsentIntentMapperSuite extends munit.FunSuite:
  private def failure(path: String, message: String) =
    Left(GameIntentMappingFailure(path, message))

  test("a consent request maps to its model intent"):
    assertEquals(GameIntentMapper.map(GameIntent.RequestConsent(
      "travel.destination", WalkerStartArgWire("site", "site:deep-woods"))),
      Right(PlayerIntent.RequestConsent("travel.destination",
        DecisionOptionRef.Site(SiteId("site:deep-woods")))))

  test("an answer maps to its choice"):
    assertEquals(GameIntentMapper.map(GameIntent.AnswerConsent("never")),
      Right(PlayerIntent.AnswerConsent(ConsentChoice.Never)))
    assertEquals(GameIntentMapper.map(GameIntent.AnswerConsent("maybe")),
      failure("$.intent.choice", "invalid consent choice 'maybe'"))

  test("a preference maps its power, requester and setting"):
    assertEquals(GameIntentMapper.map(GameIntent.SetConsentPreference(
      "site.narrow-pass.pass", "blue", "case-by-case")),
      Right(PlayerIntent.SetConsentPreference(
        PowerId("site.narrow-pass.pass"), PlayerId("blue"),
        ConsentSetting.CaseByCase)))
    assertEquals(GameIntentMapper.map(GameIntent.SetConsentPreference(
      "site.narrow-pass.pass", "blue", "sometimes")),
      failure("$.intent.setting", "invalid consent setting 'sometimes'"))
