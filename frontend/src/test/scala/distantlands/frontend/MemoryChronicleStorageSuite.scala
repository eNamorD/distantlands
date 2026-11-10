package distantlands.frontend

class MemoryChronicleStorageSuite extends ChronicleStorageContract:
  protected def make(): ChronicleStorage = new MemoryChronicleStorage()
