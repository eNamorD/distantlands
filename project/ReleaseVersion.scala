object ReleaseVersion {
  private val prerelease =
    "(0|[1-9][0-9]*)\\.(0|[1-9][0-9]*)\\.(0|[1-9][0-9]*)-(alpha|beta|rc)".r

  def resolve(overrideVersion: Option[String]): String = overrideVersion match {
    case None => "0.2.0-SNAPSHOT"
    case Some(value) if prerelease.pattern.matcher(value).matches() => value
    case Some(_) => sys.error(
      "DLQ_RELEASE_VERSION must be X.Y.Z-(alpha|beta|rc) without leading zeros"
    )
  }
}
