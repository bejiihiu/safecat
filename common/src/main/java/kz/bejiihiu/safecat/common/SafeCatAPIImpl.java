package kz.bejiihiu.safecat.common;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.function.Supplier;
import kz.bejiihiu.safecat.api.BalanceChangeEvent;
import kz.bejiihiu.safecat.api.BalanceRequestEvent;
import kz.bejiihiu.safecat.api.ChatFormatEvent;
import kz.bejiihiu.safecat.api.ChatProvider;
import kz.bejiihiu.safecat.api.CommandProvider;
import kz.bejiihiu.safecat.api.Currency;
import kz.bejiihiu.safecat.api.CurrencyProvider;
import kz.bejiihiu.safecat.api.EventReason;
import kz.bejiihiu.safecat.api.PermissionCheckEvent;
import kz.bejiihiu.safecat.api.PermissionProvider;
import kz.bejiihiu.safecat.api.SafeCatAPI;
import kz.bejiihiu.safecat.api.SafeCatEventBus;
import kz.bejiihiu.safecat.api.TransactionEvent;
import kz.bejiihiu.safecat.api.TransactionResult;

public final class SafeCatAPIImpl implements SafeCatAPI {

  private static final UUID ZERO_UUID = new UUID(0, 0);

  private final SafeCatRegistryImpl registry;
  private final SafeCatEventBus eventBus;

  public SafeCatAPIImpl(SafeCatRegistryImpl registry, SafeCatEventBus eventBus) {
    this.registry = registry;
    this.eventBus = eventBus;
  }

  @Override
  public CompletableFuture<BigDecimal> getBalance(UUID player, String currencyId) {
    BalanceRequestEvent event = new BalanceRequestEvent(currencyId, player);
    eventBus.post(event);
    if (event.isCancelled() && event.getBalance() != null) {
      return CompletableFuture.completedFuture(event.getBalance());
    }

    List<CurrencyProvider> providers = registry.getProviders(currencyId);
    if (providers.isEmpty()) {
      return CompletableFuture.completedFuture(BigDecimal.ZERO);
    }
    return tryGetBalance(player, providers, 0);
  }

  private CompletableFuture<BigDecimal> tryGetBalance(
      UUID player, List<CurrencyProvider> providers, int idx) {
    return chainForCurrency(
        providers,
        idx,
        player,
        CurrencyProvider::getBalance,
        r -> true,
        () -> CompletableFuture.completedFuture(BigDecimal.ZERO));
  }

  @Override
  public CompletableFuture<TransactionResult> withdraw(
      UUID player, String currencyId, BigDecimal amount, EventReason reason) {
    if (amount.signum() <= 0) {
      return CompletableFuture.completedFuture(
          TransactionResult.failure(currencyId, "amount must be positive"));
    }
    BalanceChangeEvent event =
        new BalanceChangeEvent(
            player, currencyId, amount, reason, BalanceChangeEvent.Operation.WITHDRAW);
    eventBus.post(event);
    if (event.isCancelled()) {
      return CompletableFuture.completedFuture(
          TransactionResult.failure(currencyId, "cancelled by event handler"));
    }

    List<CurrencyProvider> providers = registry.getProviders(currencyId);
    if (providers.isEmpty()) {
      return CompletableFuture.completedFuture(
          TransactionResult.failure(currencyId, "no provider for currency: " + currencyId));
    }

    return tryTransaction(
        player,
        currencyId,
        reason,
        providers,
        0,
        (p, u) -> p.withdraw(u, amount, reason),
        amount.negate());
  }

  @Override
  public CompletableFuture<TransactionResult> deposit(
      UUID player, String currencyId, BigDecimal amount, EventReason reason) {
    if (amount.signum() <= 0) {
      return CompletableFuture.completedFuture(
          TransactionResult.failure(currencyId, "amount must be positive"));
    }
    BalanceChangeEvent event =
        new BalanceChangeEvent(
            player, currencyId, amount, reason, BalanceChangeEvent.Operation.DEPOSIT);
    eventBus.post(event);
    if (event.isCancelled()) {
      return CompletableFuture.completedFuture(
          TransactionResult.failure(currencyId, "cancelled by event handler"));
    }

    List<CurrencyProvider> providers = registry.getProviders(currencyId);
    if (providers.isEmpty()) {
      return CompletableFuture.completedFuture(
          TransactionResult.failure(currencyId, "no provider for currency: " + currencyId));
    }

    return tryTransaction(
        player, currencyId, reason, providers, 0, (p, u) -> p.deposit(u, amount, reason), amount);
  }

  private CompletableFuture<TransactionResult> tryTransaction(
      UUID player,
      String currencyId,
      EventReason reason,
      List<CurrencyProvider> providers,
      int idx,
      BiFunction<CurrencyProvider, UUID, CompletableFuture<TransactionResult>> operation,
      BigDecimal eventAmount) {
    return chainForCurrency(
        providers,
        idx,
        player,
        (p, u) ->
            operation
                .apply(p, u)
                .thenApply(
                    r -> {
                      if (r.success()) {
                        eventBus.post(
                            new TransactionEvent(
                                ZERO_UUID, player, currencyId, eventAmount, reason, r));
                      }
                      return r;
                    }),
        TransactionResult::success,
        () ->
            CompletableFuture.completedFuture(
                TransactionResult.failure(currencyId, "all providers failed")));
  }

  private List<ChatProvider> sortedChatProviders() {
    List<ChatProvider> sorted = new ArrayList<>(registry.getChatProviders());
    sorted.sort(Comparator.comparingInt(ChatProvider::priority).reversed());
    return sorted;
  }

  @Override
  public CompletableFuture<Optional<String>> getPrefix(UUID player) {
    List<ChatProvider> providers = sortedChatProviders();
    return tryAttribute(player, providers, 0, ChatProvider::getPrefix);
  }

  @Override
  public CompletableFuture<Optional<String>> getSuffix(UUID player) {
    List<ChatProvider> providers = sortedChatProviders();
    return tryAttribute(player, providers, 0, ChatProvider::getSuffix);
  }

  @Override
  public CompletableFuture<Optional<String>> getDisplayName(UUID player) {
    List<ChatProvider> providers = sortedChatProviders();
    return tryAttribute(player, providers, 0, ChatProvider::getDisplayName);
  }

  private CompletableFuture<Optional<String>> tryAttribute(
      UUID player,
      List<ChatProvider> providers,
      int idx,
      java.util.function.BiFunction<ChatProvider, UUID, CompletableFuture<Optional<String>>> fn) {
    if (idx >= providers.size()) {
      return CompletableFuture.completedFuture(Optional.empty());
    }
    ChatProvider provider = providers.get(idx);
    return fn.apply(provider, player)
        .thenCompose(
            result -> {
              if (result.isPresent()) {
                return CompletableFuture.completedFuture(result);
              }
              return tryAttribute(player, providers, idx + 1, fn);
            })
        .exceptionallyCompose(ex -> tryAttribute(player, providers, idx + 1, fn));
  }

  @Override
  public String format(String message, UUID player) {
    ChatFormatEvent event = new ChatFormatEvent(player, message, "");
    eventBus.post(event);
    if (event.isCancelled()) {
      return message;
    }
    String msg = event.getMessage();
    for (ChatProvider provider : sortedChatProviders()) {
      msg = provider.format(msg, player);
    }
    return msg;
  }

  @Override
  public CompletableFuture<Boolean> hasPermission(UUID player, String permission) {
    return hasPermission(player, permission, null);
  }

  @Override
  public CompletableFuture<Boolean> hasPermission(UUID player, String permission, String context) {
    PermissionCheckEvent event = new PermissionCheckEvent(player, permission, context);
    eventBus.post(event);
    if (event.isCancelled() && event.getResult() != null) {
      return CompletableFuture.completedFuture(event.getResult());
    }
    List<PermissionProvider> providers = registry.getPermissionProvidersSorted();
    return tryHasPermission(player, permission, context, providers, 0);
  }

  private CompletableFuture<Boolean> tryHasPermission(
      UUID player, String permission, String context, List<PermissionProvider> providers, int idx) {
    if (idx >= providers.size()) {
      return CompletableFuture.completedFuture(false);
    }
    PermissionProvider provider = providers.get(idx);
    return provider
        .hasPermission(player, permission, context)
        .thenCompose(
            result -> {
              if (result) {
                return CompletableFuture.completedFuture(true);
              }
              return tryHasPermission(player, permission, context, providers, idx + 1);
            })
        .exceptionallyCompose(
            ex -> tryHasPermission(player, permission, context, providers, idx + 1));
  }

  @Override
  public SafeCatEventBus getEventBus() {
    return eventBus;
  }

  @Override
  public void registerCommand(CommandProvider command) {
    registry.register(command);
  }

  @Override
  public Collection<CommandProvider> getCommands() {
    return registry.getCommands();
  }

  @Override
  public void registerAdapter(Object adapter) {
    registry.register(adapter);
  }

  @Override
  public Currency getCurrency(String id) {
    return registry.getCurrency(id);
  }

  @Override
  public Set<Currency> getCurrencies() {
    return registry.getCurrencies();
  }

  /**
   * Walks through currency providers in priority order, skipping unsupported ones. Each provider's
   * result is tested — if it passes, the chain stops. On exception or failed test the next provider
   * is tried. Falls back to default when exhausted.
   */
  private <T> CompletableFuture<T> chainForCurrency(
      List<CurrencyProvider> providers,
      int idx,
      UUID player,
      BiFunction<CurrencyProvider, UUID, CompletableFuture<T>> call,
      Predicate<T> isSuccess,
      Supplier<CompletableFuture<T>> fallback) {
    if (idx >= providers.size()) return fallback.get();
    CurrencyProvider p = providers.get(idx);
    if (!p.supports(player))
      return chainForCurrency(providers, idx + 1, player, call, isSuccess, fallback);
    return call.apply(p, player)
        .thenCompose(
            r ->
                isSuccess.test(r)
                    ? CompletableFuture.completedFuture(r)
                    : chainForCurrency(providers, idx + 1, player, call, isSuccess, fallback))
        .exceptionallyCompose(
            ex -> chainForCurrency(providers, idx + 1, player, call, isSuccess, fallback));
  }
}
