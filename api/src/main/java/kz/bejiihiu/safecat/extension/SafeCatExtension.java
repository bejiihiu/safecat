package kz.bejiihiu.safecat.extension;

/**
 * An extension that integrates SafeCat with a third-party mod.
 *
 * <p>Extensions are loaded from {@code config/safecat/extensions/} by the extension loader. Each
 * extension JAR must contain a {@code
 * META-INF/services/kz.bejiihiu.safecat.extension.SafeCatExtension} file pointing to the
 * implementation class.
 *
 * <p>Implementations should use reflection to interact with the target mod — this keeps the
 * extension JAR dependency-free and allows it to load even when the target mod is absent.
 *
 * <p>Typical flow:
 *
 * <ol>
 *   <li>Check if the target mod is loaded (e.g. {@code
 *       FabricLoader.getInstance().isModLoaded("luckperms")})
 *   <li>If absent, log a warning and return — {@code init()} becomes a no-op
 *   <li>If present, use reflection to obtain the mod's API and register providers via {@code
 *       SafeCatAPI.getInstance().registerAdapter(...)}
 * </ol>
 */
public interface SafeCatExtension {

  /** Unique identifier (e.g. "luckperms", "realeconomy"). */
  String id();

  /** Human-readable name (e.g. "LuckPerms Integration"). */
  String name();

  /**
   * Called when the extension is loaded. Register providers or do nothing if the target mod is not
   * present.
   */
  void init();
}
