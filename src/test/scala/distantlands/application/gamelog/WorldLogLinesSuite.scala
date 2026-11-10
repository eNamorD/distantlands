package distantlands.application.gamelog

import distantlands.application.GamePresentationProjector
import distantlands.gameplay.{OathRules, WalkerDiceFixture}
import distantlands.gameplay.actions.ProcedureNotes
import distantlands.gameplay.chronicle.{ChronicleWalk, ProtectAndDispossess}
import distantlands.gameplay.powers.WalkerPowerCatalog
import distantlands.gameplay.setup.FirstGameSetupFixture.catalog
import distantlands.model._
import distantlands.protocol.projection.LogSpan
import distantlands.testkit.Outcomes
import distantlands.testkit.Table.{p1, p2}

/** What each seat reads of the World's lines (CR p. 38): the kept adviser
  * only its keeper, the faceup dispossessed cards everyone, the facedown
  * stack nobody. */
class WorldLogLinesSuite extends munit.FunSuite:
  private val rules = new OathRules(catalog,
    walkerPowerCatalog = WalkerPowerCatalog.default(catalog),
    walkerDice = WalkerDiceFixture.reversingShuffle)
  private val presentation = new GamePresentationProjector(catalog)
  private val words = new LogWords(catalog, LogScripts.presentation)
  private val lines = new NoteLines(words, NoteWordings.default(catalog),
    LogScripts.formatter.procedures.noteKind)

  private lazy val parkedOnKeep: ReadyGame = Outcomes.ready(
    ChronicleWalk.worldParkedOnKeepWithSecondStack(rules))

  private def spoken(note: LogNote, viewer: PlayerId)(using munit.Location)
      : Vector[LogSpan] =
    lines.spoken(ProcedureNotes.id(TriggeredProcedureRef.ChronicleWorld),
      note, Vector(parkedOnKeep), Some(viewer)).getOrElse(fail("no line"))

  /** Whether `spans` name `card`: a card span, or a face in a card list,
    * carrying its id. A hidden card carries none. */
  private def names(spans: Vector[LogSpan], card: CardId): Boolean =
    spans.exists {
      case LogSpan.Card(id, _) => id == card.value
      case LogSpan.Cards(cards) => cards.exists(face =>
        !face.hidden && face.cardId == card.value)
      case _ => false
    }

  test("only the World holder reads which adviser they kept"):
    val card = parkedOnKeep.game.current.temporaryHands(p2).head
    val note = ProtectAndDispossess.keptAdviser.sourceless(NoteArg.Player(p2),
      NoteArg.Card(card))
    assert(presentation.identifiesAt(parkedOnKeep, Some(p2), card))
    assert(names(spoken(note, p2), card))
    assert(!names(spoken(note, p1), card))

  test("the dispossessed line names the faceup cards and hides the " +
      "facedown stack"):
    val six = parkedOnKeep.game.current.cardGathering
    val note = ProtectAndDispossess.dispossessed.sourceless(NoteArg.Cards(six))
    val read = spoken(note, p1)
    assert(six.take(3).forall(names(read, _)), read)
    assert(!six.drop(3).exists(names(read, _)), read)
