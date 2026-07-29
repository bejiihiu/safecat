package kz.bejiihiu.safecat.common;

import static org.junit.jupiter.api.Assertions.*;

import kz.bejiihiu.safecat.api.SafeCatAPI;
import org.junit.jupiter.api.Test;

class SafeCatCoreTest {

  @Test
  void initializeSetsUpSingleton() {
    SafeCatCore.initialize().join();
    assertNotNull(SafeCatAPI.getInstance());
    assertNotNull(SafeCatCore.config());
    assertNotNull(SafeCatCore.registry());
    assertNotNull(SafeCatCore.eventBus());
    assertNotNull(SafeCatCore.api());
    SafeCatCore.reset();
    SafeCatAPI.resetInstance();
  }
}
