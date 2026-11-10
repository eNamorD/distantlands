package distantlands.gameplay.powers.poweruse

import distantlands.gameplay.cards.{AncientCitySite, DeepWoodsSite}
import distantlands.gameplay.powerresolver.ModifierPicks
import distantlands.gameplay.powers.{SearchFixture, WalkerPowerCatalog}
import distantlands.gameplay.powers.campaign.WrestlersCard
import distantlands.gameplay.powers.search.{MushroomsCard, NewsFromAfarCard}
import distantlands.gameplay.setup.FirstGameSetupFixture.catalog
import distantlands.model._
import distantlands.model.OathState.Ready
import distantlands.testkit.{MenuPlay, ParkedNode, Table}
import distantlands.testkit.Table.{p1, p2}
import distantlands.testkit.Unwrap.accepted

/** Spell Breaker in the modifier picker (catalog batch 8, slice 10). */
class SpellBreakerModifierSuite extends munit.FunSuite:
  import SearchFixture.rules

  private val breaker = SpellBreakerCard.id
  private val mushrooms = MushroomsCard.id
  private val woods = DeepWoodsSite.id
  private val mushroomsRef = DecisionOptionRef.Denizen(mushrooms)
  private val decisionId =
    ModifierPicks.decisionId(PowerWindow.SearchModifierSelection)

  /** p1 at Ancient City beside Mushrooms, with 5 Supply, a faceup secret
    * and a card in the Cradle discard pile for Mushrooms to draw, and Spell
    * Breaker at Deep Woods, which `ruled` gives its ruler: p2 by
    * default. */
  private def mushroomsBesideBreaker(
      ruled: Table => Table = _.warbandsAt(woods, p2, 1)): ReadyGame =
    ruled(Table.start.pawn(p1, AncientCitySite)
      .denizen(mushrooms, at = AncientCitySite).supply(p1, 5)
      .discarded(Region.Cradle, WrestlersCard.id)
      .denizen(breaker, at = woods)).ready

  /** The modifier decision's Mushrooms option, once p1 starts a Search. */
  private def mushroomsPick(ready: ReadyGame)(using munit.Location)
      : DecisionOption =
    val begun = accepted(MenuPlay.started(rules, Ready(ready), p1,
      ActionRef.Search))
    ParkedNode.of(begun.state, catalog, WalkerPowerCatalog.default(catalog))
      .toOption.flatten.collect {
        case ParkedNode.Decision(_, decide, _)
            if decide.decisionId == decisionId => decide.query
      }.collect { case one: DecisionQuery.ChooseOne => one.options }
      .flatMap(_.find(_.ref == mushroomsRef))
      .getOrElse(fail("the modifier decision does not list Mushrooms"))

  test("an enemy's secret-costing modifier is askable of the ruler"):
    assert(mushroomsPick(mushroomsBesideBreaker()).askable)

  test("under bandits it shows Spell Breaker's reason"):
    // News from Afar, faceup and affordable, keeps the pass asking: a pass
    // with nothing to pick or ask about is not asked.
    val news = NewsFromAfarCard.id
    assertEquals(mushroomsPick(mushroomsBesideBreaker(_.bandits(woods, 2)
      .favor(p1, 2).adviser(p1, news))),
      DecisionOption.Unavailable(DecisionOption.Denizen(mushroomsRef),
        OptionReason.Noted(SpellBreaker.id,
          SpellBreaker.plain(PowerSourceRef.Card(breaker)))))

  test("a grant remembered this turn lets p1 pick it"):
    val ready = mushroomsBesideBreaker()
    val covers = ConsentKey(SpellBreaker.id, p1, "power.use", mushroomsRef)
    val granted = ready.copy(consent = ConsentState(
      answers = Map(covers -> ConsentAnswer.Granted.by(p2)),
      turn = Some(ConsentTurn.of(ready))))
    assert(mushroomsPick(granted).available)

