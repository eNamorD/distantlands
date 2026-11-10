package distantlands.application.chronicle

import distantlands.model.Chronicle

/** The application's boundary to canonical Chronicle JSON (Chronicle
  * browsing design, "ChronicleJsonCodec"). `serialization` implements it;
  * `application` may not import that package, so it finds the
  * implementation through `ServiceLoader`, as it finds `GameEventCodec`. */
trait ChronicleJson:
  /** Canonical: one key order, no whitespace, so the digest is stable. */
  def encode(chronicle: Chronicle): String
  /** The reason is shown to the player who sent the text. */
  def decode(json: String): Either[String, Chronicle]

object ChronicleJson:
  lazy val default: ChronicleJson =
    val providers = java.util.ServiceLoader.load(classOf[ChronicleJson])
      .iterator()
    if providers.hasNext then providers.next()
    else throw new IllegalStateException("no ChronicleJson adapter is installed")
