package kz.bejiihiu.safecat.common.extension;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ExtensionDownloader {

  private static final Logger LOG = LoggerFactory.getLogger(ExtensionDownloader.class);
  private static final String EXTENSIONS_DIR = "config/safecat/extensions";
  private static final String RELEASES_API =
      "https://api.github.com/repos/bejiihiu/safecat/releases/latest";

  private final Path extensionsDir;

  public ExtensionDownloader() {
    this.extensionsDir = Paths.get(EXTENSIONS_DIR);
  }

  public void downloadMissing() {
    if (!Files.isDirectory(extensionsDir)) {
      try {
        Files.createDirectories(extensionsDir);
      } catch (IOException e) {
        LOG.warn("could not create extensions directory", e);
        return;
      }
    }

    downloadIfMissing("safecat-extension-luckperms");
  }

  private void downloadIfMissing(String artifactName) {
    Path target = extensionsDir.resolve(artifactName + ".jar");
    if (Files.exists(target)) {
      return;
    }

    String downloadUrl = fetchDownloadUrl(artifactName);
    if (downloadUrl == null) return;

    try {
      HttpClient client = HttpClient.newHttpClient();
      HttpRequest request = HttpRequest.newBuilder(URI.create(downloadUrl)).build();
      HttpResponse<InputStream> response =
          client.send(request, HttpResponse.BodyHandlers.ofInputStream());
      if (response.statusCode() != 200) {
        LOG.warn("failed to download {}: HTTP {}", artifactName, response.statusCode());
        return;
      }
      Files.copy(response.body(), target, StandardCopyOption.REPLACE_EXISTING);
      LOG.info("downloaded {} -> {}", artifactName, target);
    } catch (IOException | InterruptedException e) {
      LOG.warn("failed to download {}: {}", artifactName, e.getMessage());
    }
  }

  private String fetchDownloadUrl(String artifactName) {
    try {
      HttpClient client = HttpClient.newHttpClient();
      HttpRequest request =
          HttpRequest.newBuilder(URI.create(RELEASES_API))
              .header("Accept", "application/json")
              .build();
      HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
      if (response.statusCode() != 200) {
        LOG.warn("failed to fetch latest release: HTTP {}", response.statusCode());
        return null;
      }

      String body = response.body();
      String marker = "\"" + artifactName + "-";
      int idx = body.indexOf(marker);
      if (idx == -1) {
        LOG.warn("artifact {} not found in latest release", artifactName);
        return null;
      }

      int urlStart = body.lastIndexOf("\"browser_download_url\":\"", idx);
      if (urlStart == -1) return null;
      urlStart += "\"browser_download_url\":\"".length();
      int urlEnd = body.indexOf("\"", urlStart);
      return body.substring(urlStart, urlEnd);
    } catch (IOException | InterruptedException e) {
      LOG.warn("failed to fetch release info: {}", e.getMessage());
      return null;
    }
  }
}
