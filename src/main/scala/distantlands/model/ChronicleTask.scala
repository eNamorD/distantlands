package oathdigital.model

/** The three task cards a player can hold in the Chronicle (CR p. 36). The
  * Sun and the Stars are resolved by no holder. */
enum ChronicleTask(val key: String):
  case Throne extends ChronicleTask("throne")
  case World extends ChronicleTask("world")
  case Beacon extends ChronicleTask("beacon")
object ChronicleTask:
  val all: Vector[ChronicleTask] = Vector(Throne, World, Beacon)
  def fromKey(key: String): Option[ChronicleTask] = all.find(_.key == key)
