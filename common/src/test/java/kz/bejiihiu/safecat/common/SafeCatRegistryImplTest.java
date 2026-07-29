package kz.bejiihiu.safecat.common;

import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import kz.bejiihiu.safecat.api.Currency;
import kz.bejiihiu.safecat.api.CurrencyProvider;
import kz.bejiihiu.safecat.api.CurrencyType;
import kz.bejiihiu.safecat.api.EventReason;
import kz.bejiihiu.safecat.api.RegisterCurrenciesEvent;
import kz.bejiihiu.safecat.api.RegisterProvidersEvent;
import kz.bejiihiu.safecat.api.SafeCatEventBus;
import kz.bejiihiu.safecat.api.SafeCatRegistry;
import kz.bejiihiu.safecat.api.TransactionResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SafeCatRegistryImplTest {

  private SafeCatEventBus eventBus;

  record TestProvider(String currencyId, int priority) implements CurrencyProvider {
    @Override
    public String getCurrencyId() {
      return currencyId();
    }

    @Override
    public void init(SafeCatRegistry registry) {
      registry.register(new Currency(currencyId, currencyId, "$", CurrencyType.CUSTOM));
      registry.register(this);
    }

    @Override
    public CompletableFuture<BigDecimal> getBalance(UUID player) {
      return CompletableFuture.completedFuture(BigDecimal.TEN);
    }

    @Override
    public CompletableFuture<TransactionResult> withdraw(
        UUID player, BigDecimal amount, EventReason reason) {
      return CompletableFuture.completedFuture(TransactionResult.success(amount, currencyId));
    }

    @Override
    public CompletableFuture<TransactionResult> deposit(
        UUID player, BigDecimal amount, EventReason reason) {
      return CompletableFuture.completedFuture(TransactionResult.success(amount, currencyId));
    }
  }

  @BeforeEach
  void setUp() {
    eventBus = new SafeCatEventBus();
  }

  private SafeCatRegistryImpl newRegistry() {
    return new SafeCatRegistryImpl();
  }

  @Test
  void registerAndRetrieveCurrency() {
    SafeCatRegistryImpl registry = newRegistry();
    Currency c = new Currency("test:coin", "Test Coin", "T", CurrencyType.TOKEN);
    registry.register(c);
    assertSame(c, registry.getCurrency("test:coin"));
    assertTrue(registry.getCurrencies().contains(c));
  }

  @Test
  void registerAndRetrieveProviders() {
    SafeCatRegistryImpl registry = newRegistry();
    TestProvider p1 = new TestProvider("test:coin", 1);
    TestProvider p2 = new TestProvider("test:coin", 2);
    registry.register(p1);
    registry.register(p2);
    List<CurrencyProvider> providers = registry.getProviders("test:coin");
    assertEquals(2, providers.size());
    assertSame(p2, providers.get(0));
    assertSame(p1, providers.get(1));
  }

  @Test
  void getProvidersForUnknownCurrency_returnsEmpty() {
    SafeCatRegistryImpl registry = newRegistry();
    assertTrue(registry.getProviders("unknown:currency").isEmpty());
  }

  @Test
  void providerInitRegistersItself() {
    SafeCatRegistryImpl registry = newRegistry();
    TestProvider p = new TestProvider("test:token", 0);
    p.init(registry);
    assertNotNull(registry.getCurrency("test:token"));
    assertFalse(registry.getProviders("test:token").isEmpty());
  }

  @Test
  void concurrentRegistration() throws InterruptedException {
    SafeCatRegistryImpl registry = newRegistry();
    int threads = 10;
    Thread[] workers = new Thread[threads];
    for (int i = 0; i < threads; i++) {
      final String id = "currency:" + i;
      workers[i] =
          new Thread(
              () -> {
                registry.register(new Currency(id, id, "$", CurrencyType.TOKEN));
                registry.register(new TestProvider(id, 0));
              });
      workers[i].start();
    }
    for (Thread w : workers) w.join();
    assertEquals(threads, registry.getCurrencies().size());
  }

  @Test
  void initialize_firesRegisterCurrenciesEvent() {
    eventBus.on(
        RegisterCurrenciesEvent.class,
        e -> e.register(new Currency("event:coin", "Event Coin", "E", CurrencyType.CUSTOM)));
    SafeCatRegistryImpl registry = newRegistry();
    registry.initialize(eventBus);
    assertNotNull(registry.getCurrency("event:coin"));
  }

  @Test
  void initialize_firesRegisterProvidersEvent() {
    eventBus.on(
        RegisterProvidersEvent.class, e -> e.register(new TestProvider("event:provider", 0)));
    SafeCatRegistryImpl registry = newRegistry();
    registry.initialize(eventBus);
    assertFalse(registry.getProviders("event:provider").isEmpty());
  }
}
