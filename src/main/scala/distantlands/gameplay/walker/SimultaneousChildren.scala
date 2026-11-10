package distantlands.gameplay.walker

import distantlands.model.{Answered, Decide, Operation, Sequence, Simultaneous}

/** The children of a [[Simultaneous]] node: each is a `Decide`, or a
  * `Sequence` that starts with one, and a child is open until the walk's
  * answers hold its decision id. */
private[walker] object SimultaneousChildren:
  /** The decision `child` opens with. */
  def decideOf(child: Operation): Option[Decide] = child match
    case decide: Decide => Some(decide)
    case sequence: Sequence if sequence.window.isEmpty =>
      sequence.children.headOption.collect { case decide: Decide => decide }
    case _ => None

  /** The resume cursor from `child` down to its decision. */
  def entry(child: Operation): Vector[String] = child match
    case _: Decide => Vector.empty
    case _ => Vector("0")

  /** Why `node` cannot be walked, or `None` when it can. */
  def malformed(node: Simultaneous): Option[String] =
    val decides = node.children.map(decideOf)
    val found = decides.flatten
    if node.children.isEmpty then Some("it has no children")
    else if decides.contains(None) then
      Some("a child does not start with a Decide")
    else if found.exists(_.window.nonEmpty) then
      Some("a child's Decide has a window")
    else if found.exists(_.coOwners.nonEmpty) then
      Some("a child's Decide has co-owners")
    else if found.map(_.decisionId).distinct.size < found.size then
      Some("two children share a decision id")
    else if found.map(_.owner).distinct.size < found.size then
      Some("two children share an owner")
    else if found.map(_.query.heading).distinct.size > 1 then
      Some("the children's headings differ")
    else None

  /** The open children, each with its index and decision, in child order. */
  def open(node: Simultaneous, answered: Vector[Answered])
      : Vector[(Int, Decide)] =
    val ids = answered.map(_.decisionId).toSet
    node.children.zipWithIndex.flatMap((child, index) =>
      decideOf(child).filterNot(decide => ids(decide.decisionId))
        .map(index -> _))
