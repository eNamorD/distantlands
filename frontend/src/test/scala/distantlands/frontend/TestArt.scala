package distantlands.frontend

import distantlands.protocol.art.ArtCatalog

/** Game art states for the render suites. Each returns the store the
  * preference is kept in. A suite that uses one calls `GameArt.reset()`
  * after each test. */
private[frontend] object TestArt:
  /** Game art on, with a manifest listing exactly `paths`. */
  def listing(paths: String*): MemoryChronicleStorage =
    val store = new MemoryChronicleStorage()
    GameArt.install(GameArt.Manifest.Listed(paths.toSet), store)
    store

  /** Game art on, with a manifest listing every catalog path. */
  def listingWholeCatalog(): MemoryChronicleStorage =
    listing(ArtCatalog.allPaths*)
