package kz.bejiihiu.safecat.extension.luckperms;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import kz.bejiihiu.safecat.api.PermissionProvider;
import kz.bejiihiu.safecat.api.SafeCatAPI;
import kz.bejiihiu.safecat.api.SafeCatRegistry;
import kz.bejiihiu.safecat.extension.SafeCatExtension;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LuckPermsExtension implements SafeCatExtension {

  private static final Logger LOG = LoggerFactory.getLogger(LuckPermsExtension.class);
  private LuckPerms lp;

  @Override
  public String id() {
    return "luckperms";
  }

  @Override
  public String name() {
    return "LuckPerms Integration";
  }

  @Override
  public void init() {
    try {
      lp = LuckPermsProvider.get();
      SafeCatAPI.getInstance().registerAdapter(new LPPermissionProvider());
      LOG.info("LuckPerms integration active");
    } catch (Exception e) {
      LOG.warn("LuckPerms not available: {}", e.getMessage());
    }
  }

  private class LPPermissionProvider implements PermissionProvider {

    @Override
    public String getProviderId() {
      return "luckperms";
    }

    @Override
    public void init(SafeCatRegistry registry) {
      registry.register(this);
    }

    @Override
    public CompletableFuture<Boolean> hasPermission(UUID player, String permission) {
      return hasPermission(player, permission, null);
    }

    @Override
    public CompletableFuture<Boolean> hasPermission(
        UUID player, String permission, String context) {
      if (lp == null) {
        return CompletableFuture.completedFuture(false);
      }
      return lp.getUserManager()
          .loadUser(player)
          .thenApplyAsync(
              user ->
                  user.getCachedData().getPermissionData().checkPermission(permission).asBoolean());
    }
  }
}
