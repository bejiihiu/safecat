package kz.bejiihiu.safecat.common;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import kz.bejiihiu.safecat.api.ChatProvider;
import kz.bejiihiu.safecat.api.CommandProvider;
import kz.bejiihiu.safecat.api.CommandRegistrationEvent;
import kz.bejiihiu.safecat.api.Currency;
import kz.bejiihiu.safecat.api.CurrencyProvider;
import kz.bejiihiu.safecat.api.PermissionProvider;
import kz.bejiihiu.safecat.api.RegisterCurrenciesEvent;
import kz.bejiihiu.safecat.api.RegisterProvidersEvent;
import kz.bejiihiu.safecat.api.SafeCatEventBus;
import kz.bejiihiu.safecat.api.SafeCatRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class SafeCatRegistryImpl implements SafeCatRegistry {

  private static final Logger LOG = LoggerFactory.getLogger(SafeCatRegistryImpl.class);

  private final ConcurrentHashMap<String, Currency> currencies = new ConcurrentHashMap<>();
  private final ConcurrentHashMap<String, List<CurrencyProvider>> providersByCurrency =
      new ConcurrentHashMap<>();
  private final CopyOnWriteArrayList<CurrencyProvider> allProviders = new CopyOnWriteArrayList<>();
  private final ConcurrentHashMap<String, CommandProvider> commands = new ConcurrentHashMap<>();
  private final ConcurrentHashMap<String, PermissionProvider> permissionProviders =
      new ConcurrentHashMap<>();
  private final ConcurrentHashMap<String, ChatProvider> chatProviders = new ConcurrentHashMap<>();

  public SafeCatRegistryImpl() {}

  public void initialize(SafeCatEventBus eventBus) {
    ClassLoader cl = Thread.currentThread().getContextClassLoader();
    if (cl == null) cl = ClassLoader.getSystemClassLoader();
    ServiceLoader<CurrencyProvider> currencyLoader = ServiceLoader.load(CurrencyProvider.class, cl);
    boolean hasCurrency = false;
    for (CurrencyProvider provider : currencyLoader) {
      hasCurrency = true;
      provider.init(this);
      register(provider);
    }
    if (!hasCurrency) LOG.warn("no CurrencyProvider implementations found");
    ServiceLoader<PermissionProvider> permLoader = ServiceLoader.load(PermissionProvider.class, cl);
    boolean hasPerm = false;
    for (PermissionProvider provider : permLoader) {
      hasPerm = true;
      provider.init(this);
      register(provider);
    }
    if (!hasPerm) LOG.warn("no PermissionProvider implementations found");
    ServiceLoader<ChatProvider> chatLoader = ServiceLoader.load(ChatProvider.class, cl);
    boolean hasChat = false;
    for (ChatProvider provider : chatLoader) {
      hasChat = true;
      provider.init(this);
      register(provider);
    }
    if (!hasChat) LOG.warn("no ChatProvider implementations found");
    // Events posted here reach zero subscribers — platform listeners haven't subscribed yet.
    // SafeCatCore.initialize() runs registry.init before setting up listeners, so these
    // events are effectively lost. Platform entries should re-fire init events after subscribing.
    eventBus.post(new RegisterCurrenciesEvent(this));
    eventBus.post(new RegisterProvidersEvent(this));
    eventBus.post(new CommandRegistrationEvent(this));
  }

  @Override
  public void register(Currency currency) {
    currencies.put(currency.id(), currency);
  }

  @Override
  public void register(CurrencyProvider provider) {
    allProviders.add(provider);
    providersByCurrency
        .computeIfAbsent(provider.getCurrencyId(), k -> new CopyOnWriteArrayList<>())
        .add(provider);
  }

  @Override
  public void register(PermissionProvider provider) {
    permissionProviders.put(provider.getProviderId(), provider);
  }

  @Override
  public void register(CommandProvider command) {
    commands.put(command.getName(), command);
  }

  @Override
  public PermissionProvider getPermissionProvider(String id) {
    return permissionProviders.get(id);
  }

  @Override
  public Collection<PermissionProvider> getPermissionProviders() {
    return List.copyOf(permissionProviders.values());
  }

  @Override
  public void register(ChatProvider provider) {
    chatProviders.put(provider.getProviderId(), provider);
  }

  @Override
  public ChatProvider getChatProvider(String id) {
    return chatProviders.get(id);
  }

  @Override
  public Collection<ChatProvider> getChatProviders() {
    return List.copyOf(chatProviders.values());
  }

  @Override
  public Collection<CommandProvider> getCommands() {
    return List.copyOf(commands.values());
  }

  @Override
  public void register(Object adapter) {
    if (adapter instanceof CurrencyProvider cp) {
      register(cp);
    }
    if (adapter instanceof PermissionProvider pp) {
      register(pp);
    }
    if (adapter instanceof ChatProvider chat) {
      register(chat);
    }
    if (adapter instanceof CommandProvider cmd) {
      register(cmd);
    }
  }

  @Override
  public Currency getCurrency(String id) {
    return currencies.get(id);
  }

  @Override
  public Set<Currency> getCurrencies() {
    return Set.copyOf(currencies.values());
  }

  public List<PermissionProvider> getPermissionProvidersSorted() {
    List<PermissionProvider> sorted = new ArrayList<>(permissionProviders.values());
    sorted.sort(Comparator.comparingInt(PermissionProvider::priority).reversed());
    return List.copyOf(sorted);
  }

  public List<CurrencyProvider> getProviders(String currencyId) {
    List<CurrencyProvider> list = providersByCurrency.get(currencyId);
    if (list == null || list.isEmpty()) {
      return List.of();
    }
    List<CurrencyProvider> sorted = new ArrayList<>(list);
    sorted.sort(Comparator.comparingInt(CurrencyProvider::priority).reversed());
    return List.copyOf(sorted);
  }

}
