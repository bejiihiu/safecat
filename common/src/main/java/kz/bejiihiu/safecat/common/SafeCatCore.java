package kz.bejiihiu.safecat.common;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import kz.bejiihiu.safecat.api.SafeCatAPI;
import kz.bejiihiu.safecat.api.SafeCatEventBus;
import kz.bejiihiu.safecat.common.extension.ExtensionDownloader;
import kz.bejiihiu.safecat.common.extension.ExtensionLoader;

public final class SafeCatCore {

  private static final AtomicBoolean initialized = new AtomicBoolean(false);
  private static volatile SafeCatConfig config;
  private static volatile SafeCatRegistryImpl registry;
  private static volatile SafeCatEventBus eventBus;
  private static volatile SafeCatAPIImpl api;

  private SafeCatCore() {}

  // Package-private for testing only.
  static void reset() {
    initialized.set(false);
    config = null;
    registry = null;
    eventBus = null;
    api = null;
    SafeCatAPI.resetInstance();
  }

  public static CompletableFuture<Void> initialize() {
    if (!initialized.compareAndSet(false, true)) {
      return CompletableFuture.completedFuture(null);
    }
    return CompletableFuture.runAsync(
        () -> {
          config = SafeCatConfig.load();
          eventBus = new SafeCatEventBus();
          registry = new SafeCatRegistryImpl();
          registry.initialize(eventBus);
          api = new SafeCatAPIImpl(registry, eventBus);
          SafeCatAPI.setInstance(api);
          new SafecatCommand(); // Registers itself via constructor.
          new ExtensionDownloader().downloadMissing();
          new ExtensionLoader().loadAll(); // Load extensions from config/safecat/extensions/.
        });
  }

  public static SafeCatConfig config() {
    var c = config;
    if (c == null) throw new IllegalStateException("SafeCat not initialized yet");
    return c;
  }

  public static SafeCatRegistryImpl registry() {
    var r = registry;
    if (r == null) throw new IllegalStateException("SafeCat not initialized yet");
    return r;
  }

  public static SafeCatEventBus eventBus() {
    var eb = eventBus;
    if (eb == null) throw new IllegalStateException("SafeCat not initialized yet");
    return eb;
  }

  public static SafeCatAPIImpl api() {
    var a = api;
    if (a == null) throw new IllegalStateException("SafeCat not initialized yet");
    return a;
  }
}
