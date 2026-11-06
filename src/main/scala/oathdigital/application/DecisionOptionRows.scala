package oathdigital.application

import oathdigital.catalog.ExecutableCatalog
import oathdigital.gameplay.actions.BannerRules
import oathdigital.gameplay.actions.cardplay.CardPlayProcedure
import oathdigital.gameplay.powers.ConsentPowers
import oathdigital.model._
import oathdigital.protocol.projection.{CardDetailsProjection,
  ConsentAlternativeProjection, ConsentAskProjection,
  DecisionOptionProjection}

/** The option-row module: answers "how does this one [[DecisionOption]]
  * read to this viewer" -- its label, its card, its state and reason, and
  * whom an askable option asks -- or `None` when the option's game object
  * is absent from authoritative state.
  *
  * Its interface is two members, [[project]] and [[shown]]; everything
  * else here (the wrapper unwinding, the consent asks, the Replace and
  * Bury wording, the card disclosure check) is implementation, which is
  * what makes the module deep: three callers cross this seam and none of
  * them needs to know how a row is worded. [[WalkerDecisionProjector]]
  * describes every option of a parked query through it, and reads
  * [[shown]] for an `Inspect` query's cards and a card play's subject;
  * [[ConsentProjector]] names the option a consent request is about; and
  * [[PlanAidProjector]] lists the plans an Imperial ally may lend. Before
  * this module existed, the plan-aid panel depended on the whole walker
  * projector -- walker powers, tree source and all -- just to label an
  * option; now it depends on the catalog and presentation this needs.
  *
  * The dependencies are passed in once: the catalog (a site's capacity),
  * the presentation (every label and card detail, and the disclosure rule
  * `identifiesCard`), and the consent powers (which asks are paid ones,
  * and how a waivable power is named). The game-log wording of a hidden
  * option's reason ([[gamelog.NoteReasons]]) is built from the first two.
  * Pure over `ready`: the same option on the same state reads the same to
  * the same viewer, which is also what makes the module testable through
  * its interface alone (`DecisionOptionRowsSuite`, `SlotOptionRowsSuite`).
  */
private[application] final class DecisionOptionRows(
    catalog: ExecutableCatalog, presentation: GamePresentationProjector,
    consentPowers: ConsentPowers = ConsentPowers.default):
  private lazy val reasons = new gamelog.NoteReasons(catalog, presentation)
  private lazy val consentNames = new ConsentNames(presentation,
    consentPowers)

  /** One option, as its stable reference plus display detail.
    *
    * A button carries the query's own declarative label and nothing else --
    * it has no game object whose name could be resolved, which is the only
    * reason the model holds any prompt copy at all. Every other variant is
    * presented from authoritative state: a card through
    * [[GamePresentationProjector.cardDetails]], inheriting the disclosure
    * rules every other card projection already follows, and a player or
    * site through the same label accessors the rest of this layer uses.
    * That is what keeps game-object naming out of gameplay: an option
    * declares a reference, and the name is resolved here.
    *
    * `None` means "absent from authoritative state", which the caller turns
    * into a suppressed decision. A `Deck` is a closed four-case enum, a
    * favor bank is a closed six-case enum, and a button is its own identity,
    * so none can be absent.
    *
    * `index` is the caller's one [[CardIndex]] over `ready`, shared by every
    * option of a query: a Forge partition asks about three denizens and a
    * Recover pick about every site relic. `details` are the lines the caller
    * already knows about the option (what answering it records); the
    * option's own wrappers add theirs after them.
    */
  def project(ready: ReadyGame, viewer: Option[PlayerId],
      index: Option[CardIndex], option: DecisionOption,
      details: Vector[String] = Vector.empty): Option[DecisionOptionProjection] =
    val ref = option.ref
    def row(label: String, card: Option[CardDetailsProjection] = None,
        extra: Vector[String] = Vector.empty, badge: Option[String] = None) =
      Some(DecisionOptionProjection(ref.kind, ref.wireId, label, card,
        details ++ extra, badge))
    option match
      case DecisionOption.Priced(inner, price) => project(ready,
        viewer, index, inner, details ++ PriceDetails.of(price))
      // The badge is set after the wrapped option is described, so it survives
      // whichever order a caller wraps price and badge in.
      case DecisionOption.Badged(inner, badge) => project(ready,
        viewer, index, inner, details).map(_.copy(badge = Some(badge)))
      // Imperial Tactics: whose plan it is, as a detail line, since the
      // badge already says which side's plan it is.
      case DecisionOption.Lent(inner, owner) => project(ready,
        viewer, index, inner,
        details :+ s"${presentation.playerLabel(owner)}'s plan")
      // The band is set after the wrapped option is described, for the reason
      // the badge is: a caller may wrap band and price in either order.
      case DecisionOption.Grouped(inner, group) => project(ready,
        viewer, index, inner, details).map(_.copy(group = Some(group.key)))
      // Unavailable, but its rulers may be asked (consent design,
      // "Projection"): shown as askable, with whom to ask.
      case DecisionOption.Unavailable(inner,
          reason @ OptionReason.Askable(_, _, asks, _)) =>
        project(ready, viewer, index, inner, details).map(_.copy(
          state = "askable", reason = Some(reasonText(ready, viewer, reason)),
          consent = Some(askProjection(asks))))
      case DecisionOption.Unavailable(inner, reason) => project(ready,
        viewer, index, inner, details).map(_.copy(state = "unavailable",
          reason = Some(reasonText(ready, viewer, reason))))
      // `OptionWarnings` never warns on an option that cannot be chosen.
      case DecisionOption.Warned(inner, reason) => project(ready,
        viewer, index, inner, details).map(_.copy(state = "warning",
          reason = Some(reasonText(ready, viewer, reason))))
      // Named after the wrapped option is described, so the label says what
      // the play discards and where the card goes.
      case DecisionOption.Replacing(inner, card) =>
        val replaced = shown(ready, viewer, index, card)
        project(ready, viewer, index, inner, details).map(row =>
          row.copy(label = replacingLabel(inner.ref, replaced.name),
            replaces = Some(replaced)))
      case DecisionOption.Burying(inner, card) =>
        val buried = shown(ready, viewer, index, card)
        val name = if buried.hidden then s"a ${buried.name.toLowerCase}"
          else buried.name
        project(ready, viewer, index, inner, details).map(row =>
          row.copy(label = buryingLabel(inner.ref, name),
            replaces = Some(buried), buries = true))
      case DecisionOption.Button(_, label) => row(label)
      case DecisionOption.Player(player) =>
        if ready.game.current.players.exists(_.player == player.id) then
          row(presentation.playerLabel(player.id)) else None
      case DecisionOption.Site(site) =>
        if ready.game.current.map.sites.contains(site.id) then
          row(presentation.siteLabel(site.id)) else None
      case DecisionOption.Denizen(denizen) =>
        card(ready, viewer, index, denizen.id)
          .flatMap(details => row(details.name, Some(details)))
      case DecisionOption.Relic(relic) =>
        card(ready, viewer, index, relic.id)
          .flatMap(details => row(details.name, Some(details)))
      case DecisionOption.Vision(vision) =>
        card(ready, viewer, index, vision.id)
          .flatMap(details => row(details.name, Some(details)))
      case DecisionOption.Edifice(edifice) =>
        index.flatMap(_.stateOf(edifice.id)).collect {
          case state: EdificeState => state
        }.flatMap(state => row(presentation.edificeLabel(state.id, state.side),
          Some(presentation.edificeCardDetails(state))))
      case DecisionOption.RelicSlot(slot) =>
        if ready.game.current.players.find(_.player == slot.owner)
            .exists(_.relics.isDefinedAt(slot.slot)) then
          row(s"${presentation.playerLabel(slot.owner)} facedown relic")
        else None
      // The relic when the viewer may identify it, else its back: a slot
      // never discloses a facedown relic.
      case DecisionOption.SiteRelicSlot(slot) =>
        ready.game.current.map.sites.get(slot.site)
          .flatMap(_.relics.lift(slot.index)).flatMap(held =>
            card(ready, viewer, index, held.id) match
              case Some(details) => row(details.name, Some(details))
              case None => row(
                s"Facedown relic at ${presentation.siteLabel(slot.site)}",
                Some(presentation.hiddenCard("relic"))))
      // The card when the viewer may identify it, else its back: a slot
      // never discloses a facedown adviser, but the panel still draws a card.
      case DecisionOption.AdviserSlot(slot) =>
        ready.game.current.players.find(_.player == slot.owner)
          .flatMap(_.advisers.lift(slot.slot)).flatMap { held =>
            val owner = presentation.playerLabel(slot.owner)
            card(ready, viewer, index, held.id) match
              case Some(details) => row(details.name, Some(details),
                extra = Vector(s"$owner's adviser"))
              case None => row(s"$owner's facedown adviser ${slot.slot + 1}",
                Some(presentation.hiddenCard(presentation.cardKind(held.id))))
          }
      case DecisionOption.Banner(held) =>
        // The banner is one object on the table and the board says who holds
        // it, so the option names the banner alone -- and counts what that
        // banner actually takes.
        val current = ready.game.current
        row(BannerRules.displayName(held.banner),
          extra = Vector(s"Currently ${BannerRules.resources(current,
            held.banner)} ${BannerRules.resourceName(held.banner)}"))
      case DecisionOption.Deck(deck) =>
        row(s"${presentation.safeLabel(deck.id.key)} deck")
      case DecisionOption.FavorBank(bank) =>
        row(presentation.safeLabel(bank.suit.key))
      case DecisionOption.SiteSlot(slot) =>
        val fits = catalog.site(slot.site).exists(slot.index < _.capacity)
        if fits && ready.game.current.map.sites.contains(slot.site) then
          row(s"Empty slot at ${presentation.siteLabel(slot.site)}")
        else None
      case DecisionOption.AdviserPlay(play) =>
        if ready.game.current.players.exists(_.player == play.owner) then
          row(s"Adviser slot ${play.index + 1}, ${facing(play.faceup)}")
            .map(_.copy(variant = Some(facing(play.faceup).capitalize)))
        else None
      case DecisionOption.RevealedVision(spot) =>
        if ready.game.current.players.exists(_.player == spot.owner) then
          row("Revealed Vision")
        else None
      // A pile names itself: a card play discards to it and a Search draws
      // from it, so the label says neither.
      case DecisionOption.DiscardPile(pile) =>
        row(SearchSource.name(SearchSource.RegionalDiscard(pile.region)))

  /** A card as its viewer may see it: its face when they may identify it,
    * else the redacted back [[GamePresentationProjector.hiddenCard]] gives an
    * unidentifiable board slot. */
  def shown(ready: ReadyGame, viewer: Option[PlayerId],
      index: Option[CardIndex], id: CardId): CardDetailsProjection =
    index.flatMap(_.get(id)) match
      case Some(located) =>
        val orientation = orientationOf(located.state)
        if presentation.identifiesCard(ready, viewer, id, orientation,
            located.location.container) then
          located.state match
            case Some(edifice: EdificeState) =>
              presentation.edificeCardDetails(edifice)
            case _ => presentation.cardDetails(id, orientation, hidden = false)
        else presentation.hiddenCard(presentation.cardKind(id))
      case None => presentation.hiddenCard(presentation.cardKind(id))

  /** Whom an askable option asks: its required asks to ignore a power,
    * each alternative on its own, and each required ask to pay on its own
    * (catalog batch 8, slice 15). */
  private def askProjection(asks: Vector[ConsentAsk]): ConsentAskProjection =
    def paying(ask: ConsentAsk) = ask.powers.forall(consentPowers.paying)
    def one(ask: ConsentAsk) = ConsentAlternativeProjection(
      ask.granter.value, ask.powers.map(consentNames.power), paying(ask))
    val (payers, ignoring) = asks.filterNot(_.alternative).partition(paying)
    ConsentAskProjection(ignoring.map(_.granter.value),
      ignoring.flatMap(_.powers).distinct.map(consentNames.power),
      asks.filter(_.alternative).map(one), payers.map(one))

  /** A Replace option's label: the card it discards, and where the played
    * card goes. */
  private def replacingLabel(ref: DecisionOptionRef, name: String): String =
    ref match
      case DecisionOptionRef.SiteSlot(site, _) =>
        s"Replace $name at ${presentation.siteLabel(site)}"
      case DecisionOptionRef.AdviserPlay(_, _, faceup) =>
        s"Replace $name, ${facing(faceup)}"
      case _: DecisionOptionRef.RevealedVision =>
        s"Replace $name as revealed Vision"
      case _ => s"Replace $name"

  /** A bury option's label: the card it buries, and where. */
  private def buryingLabel(ref: DecisionOptionRef, name: String): String =
    CardPlayProcedure.siteOf(ref).fold(s"Bury $name")(site =>
      s"Bury $name at ${presentation.siteLabel(site)}")

  private def facing(faceup: Boolean): String =
    if faceup then "faceup" else "facedown"

  /** A card option's presentation, or `None` when the card is nowhere in
    * authoritative state OR is there but this viewer may not be told which
    * card it is. The orientation comes from the card's own located state
    * rather than being assumed by the caller, so a facedown site relic and a
    * faceup denizen each present as what they are.
    *
    * Both rejections are the same failure to the caller -- the decision is
    * suppressed -- and that is the point. An option cannot be redacted the
    * way a board slot can: its reference is the card's identity, the client
    * answers by sending that reference back, and `DecisionOptionProjection`
    * carries the real id even when its card details are withheld. So
    * projecting a `hiddenCard` beside a live reference would hide the name
    * and disclose the identity in the same breath. Offering nothing is the
    * only honest answer, and it makes the spec's "builders are responsible
    * for constructing targets that exist in their authoritative state"
    * enforceable rather than advisory.
    *
    * The entitlement question itself is not answered here. It is
    * [[GamePresentationProjector.identifiesCard]], the same rule
    * `playerBoards` redacts by, so the walker path inherits the layer's
    * disclosure handling instead of asserting `hidden = false` over it.
    * `viewer` is the decision's own owner -- `project` has already gated on
    * that -- which is why a permissive answer here is still owner-private.
    */
  private def card(ready: ReadyGame, viewer: Option[PlayerId],
      index: Option[CardIndex], id: CardId): Option[CardDetailsProjection] =
    index.flatMap(_.get(id)).filter(located => presentation.identifiesCard(
      ready, viewer, id, orientationOf(located.state),
      located.location.container)).map(located => presentation.cardDetails(id,
        orientationOf(located.state), hidden = false))

  /** An unavailable option's reason as player text: a builder's own words,
    * or the note of the power that hid it, worded as the Game Log words it.
    */
  private def reasonText(ready: ReadyGame, viewer: Option[PlayerId],
      reason: OptionReason): String = reason match
    case OptionReason.Written(text) => text
    case OptionReason.Noted(power, note) =>
      reasons.text(power, note, ready, viewer).getOrElse("Not on offer now")
    case OptionReason.Askable(power, note, _, _) =>
      reasons.text(power, note, ready, viewer).getOrElse("Not on offer now")

  private def orientationOf(state: Option[CardState]): Option[Orientation] =
    GamePresentationProjector.orientationOf(state)
