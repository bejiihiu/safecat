package kz.bejiihiu.safecat.common.extension;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;
import kz.bejiihiu.safecat.extension.SafeCatExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Loads SafeCat extensions from {@code config/safecat/extensions/}.
 *
 * <p>Scans the directory for {@code *.jar} files, loads each with an isolated {@link
 * URLClassLoader}, discovers {@link SafeCatExtension} implementations via {@link ServiceLoader},
 * and calls {@link SafeCatExtension#init()}.
 */
public final class ExtensionLoader {

  private static final Logger LOG = LoggerFactory.getLogger(ExtensionLoader.class);
  private static final String EXTENSIONS_DIR = "config/safecat/extensions";

  private final List<SafeCatExtension> extensions = new ArrayList<>();
  private final List<URLClassLoader> classLoaders = new ArrayList<>();

  public void loadAll() {
    Path dir = Paths.get(EXTENSIONS_DIR);
    if (!Files.isDirectory(dir)) {
      try {
        Files.createDirectories(dir);
      } catch (IOException e) {
        LOG.warn("could not create extensions directory: {}", dir, e);
      }
      LOG.info("extensions directory created at {}", dir.toAbsolutePath());
      return;
    }

    List<Path> jars = new ArrayList<>();
    try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, "*.jar")) {
      for (Path entry : stream) {
        jars.add(entry);
      }
    } catch (IOException e) {
      LOG.warn("error scanning extensions directory", e);
    }

    if (jars.isEmpty()) {
      LOG.debug("no extension jars found in {}", dir.toAbsolutePath());
      return;
    }

    for (Path jar : jars) {
      loadJar(jar);
    }

    LOG.info("loaded {} extension(s)", extensions.size());
  }

  private void loadJar(Path jar) {
    URL jarUrl;
    try {
      jarUrl = jar.toUri().toURL();
    } catch (MalformedURLException e) {
      LOG.warn("invalid jar path: {}", jar, e);
      return;
    }
    URLClassLoader cl = new URLClassLoader(new URL[] {jarUrl}, getClass().getClassLoader());
    classLoaders.add(cl);

    try {
      ServiceLoader<SafeCatExtension> sl = ServiceLoader.load(SafeCatExtension.class, cl);
      boolean found = false;
      for (SafeCatExtension ext : sl) {
        found = true;
        extensions.add(ext);
        try {
          ext.init();
          LOG.info("extension loaded: {} ({})", ext.name(), ext.id());
        } catch (Exception e) {
          LOG.warn("extension {} failed to init: {}", ext.id(), e.getMessage());
        }
      }
      if (!found) {
        LOG.warn("no SafeCatExtension implementations in {}", jar.getFileName());
      }
    } finally {
      try {
        cl.close();
      } catch (IOException e) {
        LOG.warn("failed to close classloader for {}", jar.getFileName(), e);
      }
    }
  }
}
