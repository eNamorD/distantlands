package oathdigital.application

import oathdigital.model.OptionPrice

/** The cost of an option, worded for display: what a player weighs when they
  * choose a battle plan or a modifier. It is one line in the table's cost
  * notation, the one a start's and a destination's badge use: each part a
  * count and its token, in a fixed order, joined by " · " ("Cost: 1 [favor]
  * · 1 [secret-burnt]"), and a part that costs nothing is not mentioned.
  */
private[application] object PriceDetails:
  def of(price: OptionPrice): Vector[String] =
    val parts = Vector(
      part(price.favor, "[favor]"),
      part(price.favorBurnt, "[favor-burnt]"),
      part(price.secrets, "[secret]"),
      if price.secretsX then "X [secret]" else "",
      part(price.secretsBurnt, "[secret-burnt]"),
      part(price.warbands, if price.warbands == 1 then "warband"
        else "warbands")).filter(_.nonEmpty)
    Option.when(parts.nonEmpty)(parts.mkString("Cost: ", " · ", "")).toVector

  private def part(count: Int, unit: String): String =
    if count == 0 then "" else s"$count $unit"

  /** What a pick paid, worded for its row: "1 favor", "2 secrets burnt",
    * "1 warband sacrificed", or "Free" when it paid nothing. */
  def paid(price: OptionPrice): Vector[String] =
    val parts = paidParts(price)
    if parts.isEmpty then Vector("Free") else parts

  /** What several picks paid together: "1 favor, 1 secret", or "Nothing". */
  def paidInAll(prices: Vector[OptionPrice]): String =
    val parts = paidParts(prices.foldLeft(OptionPrice())((total, price) =>
      OptionPrice(total.favor + price.favor, total.secrets + price.secrets,
        total.favorBurnt + price.favorBurnt,
        total.secretsBurnt + price.secretsBurnt,
        total.warbands + price.warbands)))
    if parts.isEmpty then "Nothing" else parts.mkString(", ")

  private def paidParts(price: OptionPrice): Vector[String] = Vector(
    amount(price.favor, "favor", "favor", ""),
    amount(price.secrets, "secret", "secrets", ""),
    amount(price.favorBurnt, "favor", "favor", " burnt"),
    amount(price.secretsBurnt, "secret", "secrets", " burnt"),
    amount(price.warbands, "warband", "warbands", " sacrificed"))
    .filter(_.nonEmpty)

  private def amount(count: Int, one: String, many: String, suffix: String)
      : String =
    if count == 0 then "" else s"$count ${if count == 1 then one else many}$suffix"
