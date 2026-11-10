package oathdigital.frontend

class MemoryChronicleStorageSuite extends ChronicleStorageContract:
  protected def make(): ChronicleStorage = new MemoryChronicleStorage()
