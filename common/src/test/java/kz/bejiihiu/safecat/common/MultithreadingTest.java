package kz.bejiihiu.safecat.common;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import kz.bejiihiu.safecat.api.Currency;
import kz.bejiihiu.safecat.api.CurrencyProvider;
import kz.bejiihiu.safecat.api.CurrencyType;
import kz.bejiihiu.safecat.api.EventReason;
import kz.bejiihiu.safecat.api.SafeCatEventBus;
import kz.bejiihiu.safecat.api.TransactionResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MultithreadingTest {

  private static final UUID PLAYER = UUID.randomUUID();
  private SafeCatRegistryImpl registry;
  private SafeCatEventBus eventBus;
  private SafeCatAPIImpl api;

  @BeforeEach
  void setUp() {
    eventBus = new SafeCatEventBus();
    registry = new SafeCatRegistryImpl();
    api = new SafeCatAPIImpl(registry, eventBus);
  }

  @Test
  void concurrentRegistryOperations() throws InterruptedException {
    int threadCount = 4;
    int opsPerThread = 100;
    try (ExecutorService exec = Executors.newFixedThreadPool(threadCount)) {
      CountDownLatch latch = new CountDownLatch(threadCount);
      AtomicInteger errors = new AtomicInteger();

      for (int t = 0; t < threadCount; t++) {
        final int threadId = t;
        exec.submit(
            () -> {
              try {
                for (int i = 0; i < opsPerThread; i++) {
                  String id = "currency:" + threadId + ":" + i;
                  registry.register(new Currency(id, id, "$", CurrencyType.TOKEN));
                  registry.register(new TestProvider(id));
                  registry.getCurrency(id);
                  registry.getProviders(id);
                }
              } catch (Exception e) {
                errors.incrementAndGet();
              } finally {
                latch.countDown();
              }
            });
      }

      latch.await();
    }
    assertEquals(0, errors.get());
    assertEquals(threadCount * opsPerThread, registry.getCurrencies().size());
  }

  @Test
  void concurrentApiCalls() throws InterruptedException {
    int providerCount = 20;
    for (int i = 0; i < providerCount; i++) {
      String id = "currency:" + i;
      registry.register(new Currency(id, id, "$", CurrencyType.TOKEN));
      registry.register(new TestProvider(id));
    }

    int requestCount = 200;
    try (ExecutorService exec = Executors.newFixedThreadPool(8)) {
      CountDownLatch latch = new CountDownLatch(requestCount);
      AtomicInteger errors = new AtomicInteger();

      for (int i = 0; i < requestCount; i++) {
        final int idx = i % providerCount;
        final String id = "currency:" + idx;
        exec.submit(
            () -> {
              try {
                api.getBalance(PLAYER, id).get();
                api.deposit(PLAYER, id, BigDecimal.ONE, EventReason.ADMIN).get();
                api.withdraw(PLAYER, id, BigDecimal.ONE, EventReason.ADMIN).get();
              } catch (Exception e) {
                errors.incrementAndGet();
              } finally {
                latch.countDown();
              }
            });
      }

      latch.await();
    }
    assertEquals(0, errors.get(), "no errors during concurrent API calls");
  }

  @Test
  void noConcurrentModificationException() throws InterruptedException {
    for (int i = 0; i < 10; i++) {
      String id = "currency:" + i;
      registry.register(new Currency(id, id, "$", CurrencyType.TOKEN));
      registry.register(new TestProvider(id));
    }

    try (ExecutorService exec = Executors.newFixedThreadPool(4)) {
      CountDownLatch latch = new CountDownLatch(2);
      AtomicInteger errors = new AtomicInteger();

      exec.submit(
          () -> {
            try {
              for (int i = 0; i < 500; i++) {
                registry.getCurrencies().forEach(c -> registry.getProviders(c.id()));
                registry.getProviders("currency:" + (i % 10));
              }
            } catch (Exception e) {
              errors.incrementAndGet();
            } finally {
              latch.countDown();
            }
          });

      exec.submit(
          () -> {
            try {
              for (int i = 0; i < 500; i++) {
                String id = "concurrent:" + i;
                registry.register(new Currency(id, id, "$", CurrencyType.TOKEN));
                registry.register(new TestProvider(id));
              }
            } catch (Exception e) {
              errors.incrementAndGet();
            } finally {
              latch.countDown();
            }
          });

      latch.await();
    }
    assertEquals(0, errors.get(), "no ConcurrentModificationException");
  }

  @Test
  void concurrentEventBusOperations() throws InterruptedException {
    int threadCount = 4;
    int eventsPerThread = 200;
    try (ExecutorService exec = Executors.newFixedThreadPool(threadCount)) {
      CountDownLatch latch = new CountDownLatch(threadCount);
      AtomicInteger handled = new AtomicInteger();

      for (int i = 0; i < 4; i++) {
        eventBus.on(TestEvent.class, e -> handled.incrementAndGet());
      }

      for (int t = 0; t < threadCount; t++) {
        exec.submit(
            () -> {
              try {
                for (int i = 0; i < eventsPerThread; i++) {
                  eventBus.post(new TestEvent("data"));
                }
              } finally {
                latch.countDown();
              }
            });
      }

      latch.await();
    }
    assertEquals(threadCount * eventsPerThread * 4, handled.get());
  }

  private static class TestEvent extends kz.bejiihiu.safecat.api.Event {
    private final String value;

    TestEvent(String value) {
      this.value = value;
    }
  }

  private record TestProvider(String currencyId) implements CurrencyProvider {
    @Override
    public String getCurrencyId() {
      return currencyId();
    }

    @Override
    public void init(kz.bejiihiu.safecat.api.SafeCatRegistry r) {}

    @Override
    public CompletableFuture<BigDecimal> getBalance(UUID p) {
      return CompletableFuture.completedFuture(BigDecimal.ZERO);
    }

    @Override
    public CompletableFuture<TransactionResult> withdraw(UUID p, BigDecimal a, EventReason r) {
      return CompletableFuture.completedFuture(TransactionResult.success(a, currencyId));
    }

    @Override
    public CompletableFuture<TransactionResult> deposit(UUID p, BigDecimal a, EventReason r) {
      return CompletableFuture.completedFuture(TransactionResult.success(a, currencyId));
    }
  }
}
