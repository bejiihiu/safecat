package kz.bejiihiu.safecat.common;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import kz.bejiihiu.safecat.api.BalanceChangeEvent;
import kz.bejiihiu.safecat.api.BalanceRequestEvent;
import kz.bejiihiu.safecat.api.CurrencyProvider;
import kz.bejiihiu.safecat.api.EventReason;
import kz.bejiihiu.safecat.api.SafeCatEventBus;
import kz.bejiihiu.safecat.api.TransactionEvent;
import kz.bejiihiu.safecat.api.TransactionResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SafeCatAPIImplTest {

  private static final String CURRENCY_ID = "test:coin";
  private static final UUID PLAYER = UUID.randomUUID();

  private SafeCatRegistryImpl registry;
  private SafeCatEventBus eventBus;
  private SafeCatAPIImpl api;

  @BeforeEach
  void setUp() {
    eventBus = spy(new SafeCatEventBus());
    registry = new SafeCatRegistryImpl();
    api = new SafeCatAPIImpl(registry, eventBus);
  }

  @Test
  void getBalance_returnsFromHighestPriorityProvider() {
    CurrencyProvider low = provider(CURRENCY_ID, 0, BigDecimal.ONE);
    CurrencyProvider high = provider(CURRENCY_ID, 10, BigDecimal.TEN);
    registry.register(low);
    registry.register(high);

    BigDecimal balance = api.getBalance(PLAYER, CURRENCY_ID).join();
    assertEquals(0, BigDecimal.TEN.compareTo(balance));
  }

  @Test
  void getBalance_fallsBackWhenProviderDoesNotSupport() {
    CurrencyProvider noSupport = mock(CurrencyProvider.class);
    lenient().when(noSupport.getCurrencyId()).thenReturn(CURRENCY_ID);
    lenient().when(noSupport.priority()).thenReturn(10);
    when(noSupport.supports(PLAYER)).thenReturn(false);

    CurrencyProvider withSupport = mock(CurrencyProvider.class);
    lenient().when(withSupport.getCurrencyId()).thenReturn(CURRENCY_ID);
    lenient().when(withSupport.priority()).thenReturn(0);
    when(withSupport.supports(PLAYER)).thenReturn(true);
    when(withSupport.getBalance(PLAYER))
        .thenReturn(CompletableFuture.completedFuture(BigDecimal.ONE));

    registry.register(noSupport);
    registry.register(withSupport);

    BigDecimal balance = api.getBalance(PLAYER, CURRENCY_ID).join();
    assertEquals(0, BigDecimal.ONE.compareTo(balance));
  }

  @Test
  void getBalance_noProviders_returnsZero() {
    BigDecimal balance = api.getBalance(PLAYER, "unknown:currency").join();
    assertEquals(BigDecimal.ZERO, balance);
  }

  @Test
  void withdraw_followsPriorityChain() {
    CurrencyProvider high = mockProvider(CURRENCY_ID, 10);
    when(high.withdraw(PLAYER, BigDecimal.ONE, EventReason.SHOP_PURCHASE))
        .thenReturn(
            CompletableFuture.completedFuture(
                TransactionResult.success(BigDecimal.ONE, CURRENCY_ID)));
    CurrencyProvider low = mockProvider(CURRENCY_ID, 0);
    registry.register(low);
    registry.register(high);

    TransactionResult result =
        api.withdraw(PLAYER, CURRENCY_ID, BigDecimal.ONE, EventReason.SHOP_PURCHASE).join();
    assertTrue(result.success());
  }

  @Test
  void withdraw_skipsUnsupportedProvider() {
    CurrencyProvider noSupport = mock(CurrencyProvider.class);
    lenient().when(noSupport.getCurrencyId()).thenReturn(CURRENCY_ID);
    lenient().when(noSupport.priority()).thenReturn(10);
    when(noSupport.supports(PLAYER)).thenReturn(false);

    CurrencyProvider withSupport = mock(CurrencyProvider.class);
    lenient().when(withSupport.getCurrencyId()).thenReturn(CURRENCY_ID);
    lenient().when(withSupport.priority()).thenReturn(0);
    when(withSupport.supports(PLAYER)).thenReturn(true);
    when(withSupport.withdraw(PLAYER, BigDecimal.ONE, EventReason.SHOP_PURCHASE))
        .thenReturn(
            CompletableFuture.completedFuture(
                TransactionResult.success(BigDecimal.ONE, CURRENCY_ID)));

    registry.register(noSupport);
    registry.register(withSupport);

    TransactionResult result =
        api.withdraw(PLAYER, CURRENCY_ID, BigDecimal.ONE, EventReason.SHOP_PURCHASE).join();
    assertTrue(result.success());
  }

  @Test
  void withdraw_allProvidersFail_returnsFailure() {
    CurrencyProvider p = mockProvider(CURRENCY_ID, 0);
    when(p.withdraw(PLAYER, BigDecimal.ONE, EventReason.SHOP_PURCHASE))
        .thenReturn(
            CompletableFuture.completedFuture(TransactionResult.failure(CURRENCY_ID, "no funds")));
    registry.register(p);

    TransactionResult result =
        api.withdraw(PLAYER, CURRENCY_ID, BigDecimal.ONE, EventReason.SHOP_PURCHASE).join();
    assertFalse(result.success());
  }

  @Test
  void withdraw_noProviders_returnsFailure() {
    TransactionResult result =
        api.withdraw(PLAYER, "unknown:id", BigDecimal.ONE, EventReason.SHOP_PURCHASE).join();
    assertFalse(result.success());
  }

  @Test
  void withdraw_dispatchesBalanceChangeEvent() {
    CurrencyProvider p = mockProvider(CURRENCY_ID, 0);
    when(p.withdraw(PLAYER, BigDecimal.ONE, EventReason.SHOP_PURCHASE))
        .thenReturn(
            CompletableFuture.completedFuture(
                TransactionResult.success(BigDecimal.ONE, CURRENCY_ID)));
    registry.register(p);

    api.withdraw(PLAYER, CURRENCY_ID, BigDecimal.ONE, EventReason.SHOP_PURCHASE).join();

    verify(eventBus, times(1)).post(any(BalanceChangeEvent.class));
  }

  @Test
  void withdraw_cancelledEvent_preventsTransaction() {
    eventBus.on(BalanceChangeEvent.class, e -> e.setCancelled(true));
    CurrencyProvider p = mockProvider(CURRENCY_ID, 0);
    registry.register(p);

    TransactionResult result =
        api.withdraw(PLAYER, CURRENCY_ID, BigDecimal.ONE, EventReason.SHOP_PURCHASE).join();
    assertFalse(result.success());
    assertEquals("cancelled by event handler", result.message());
    verify(p, never()).withdraw(any(), any(), any());
  }

  @Test
  void withdraw_dispatchesTransactionEventOnSuccess() {
    CurrencyProvider p = mockProvider(CURRENCY_ID, 0);
    when(p.withdraw(PLAYER, BigDecimal.ONE, EventReason.SHOP_PURCHASE))
        .thenReturn(
            CompletableFuture.completedFuture(
                TransactionResult.success(BigDecimal.ONE, CURRENCY_ID)));
    registry.register(p);

    api.withdraw(PLAYER, CURRENCY_ID, BigDecimal.ONE, EventReason.SHOP_PURCHASE).join();

    verify(eventBus, times(1)).post(any(TransactionEvent.class));
  }

  @Test
  void deposit_followsPriorityChain() {
    CurrencyProvider p = mockProvider(CURRENCY_ID, 0);
    when(p.deposit(PLAYER, BigDecimal.TEN, EventReason.QUEST_REWARD))
        .thenReturn(
            CompletableFuture.completedFuture(
                TransactionResult.success(BigDecimal.TEN, CURRENCY_ID)));
    registry.register(p);

    TransactionResult result =
        api.deposit(PLAYER, CURRENCY_ID, BigDecimal.TEN, EventReason.QUEST_REWARD).join();
    assertTrue(result.success());
  }

  @Test
  void deposit_cancelledEvent_preventsTransaction() {
    eventBus.on(BalanceChangeEvent.class, e -> e.setCancelled(true));
    CurrencyProvider p = mockProvider(CURRENCY_ID, 0);
    registry.register(p);

    TransactionResult result =
        api.deposit(PLAYER, CURRENCY_ID, BigDecimal.TEN, EventReason.QUEST_REWARD).join();
    assertFalse(result.success());
  }

  @Test
  void deposit_dispatchesTransactionEventOnSuccess() {
    CurrencyProvider p = mockProvider(CURRENCY_ID, 0);
    when(p.deposit(PLAYER, BigDecimal.TEN, EventReason.QUEST_REWARD))
        .thenReturn(
            CompletableFuture.completedFuture(
                TransactionResult.success(BigDecimal.TEN, CURRENCY_ID)));
    registry.register(p);

    api.deposit(PLAYER, CURRENCY_ID, BigDecimal.TEN, EventReason.QUEST_REWARD).join();

    verify(eventBus, times(1)).post(any(TransactionEvent.class));
  }

  @Test
  void getBalance_dispatchesBalanceRequestEvent() {
    registry.register(provider(CURRENCY_ID, 0, BigDecimal.TEN));
    api.getBalance(PLAYER, CURRENCY_ID).join();
    verify(eventBus, atLeastOnce()).post(any(BalanceRequestEvent.class));
  }

  @Test
  void getBalance_balanceRequestEventCancelledWithBalance_returnsEventBalance() {
    eventBus.on(
        BalanceRequestEvent.class,
        e -> {
          if (e.getCurrencyId().equals(CURRENCY_ID) && e.getPlayer().equals(PLAYER)) {
            e.setBalance(BigDecimal.valueOf(42));
            e.setCancelled(true);
          }
        });
    BigDecimal balance = api.getBalance(PLAYER, CURRENCY_ID).join();
    assertEquals(0, BigDecimal.valueOf(42).compareTo(balance));
  }

  @Test
  void getBalance_balanceRequestEventCancelledWithoutBalance_fallsThrough() {
    eventBus.on(BalanceRequestEvent.class, e -> e.setCancelled(true));
    registry.register(provider(CURRENCY_ID, 0, BigDecimal.TEN));
    BigDecimal balance = api.getBalance(PLAYER, CURRENCY_ID).join();
    assertEquals(0, BigDecimal.TEN.compareTo(balance));
  }

  @Test
  void withdraw_transactionEventPayloadMatches() {
    CurrencyProvider p = mockProvider(CURRENCY_ID, 0);
    when(p.withdraw(PLAYER, BigDecimal.ONE, EventReason.SHOP_PURCHASE))
        .thenReturn(
            CompletableFuture.completedFuture(
                TransactionResult.success(BigDecimal.ONE, CURRENCY_ID)));
    registry.register(p);

    TransactionResult[] captured = new TransactionResult[1];
    eventBus.on(
        TransactionEvent.class,
        e -> {
          assertEquals(PLAYER, e.getTo());
          assertEquals(CURRENCY_ID, e.getCurrencyId());
          assertEquals(0, BigDecimal.ONE.negate().compareTo(e.getAmount()));
          assertEquals(EventReason.SHOP_PURCHASE, e.getReason());
          captured[0] = e.getResult();
        });

    TransactionResult result =
        api.withdraw(PLAYER, CURRENCY_ID, BigDecimal.ONE, EventReason.SHOP_PURCHASE).join();
    assertSame(captured[0], result);
  }

  @Test
  void deposit_transactionEventPayloadMatches() {
    CurrencyProvider p = mockProvider(CURRENCY_ID, 0);
    when(p.deposit(PLAYER, BigDecimal.TEN, EventReason.QUEST_REWARD))
        .thenReturn(
            CompletableFuture.completedFuture(
                TransactionResult.success(BigDecimal.TEN, CURRENCY_ID)));
    registry.register(p);

    TransactionResult[] captured = new TransactionResult[1];
    eventBus.on(
        TransactionEvent.class,
        e -> {
          assertEquals(PLAYER, e.getTo());
          assertEquals(CURRENCY_ID, e.getCurrencyId());
          assertEquals(0, BigDecimal.TEN.compareTo(e.getAmount()));
          assertEquals(EventReason.QUEST_REWARD, e.getReason());
          captured[0] = e.getResult();
        });

    TransactionResult result =
        api.deposit(PLAYER, CURRENCY_ID, BigDecimal.TEN, EventReason.QUEST_REWARD).join();
    assertSame(captured[0], result);
  }

  private static CurrencyProvider provider(String currencyId, int priority, BigDecimal balance) {
    return new CurrencyProvider() {
      @Override
      public String getCurrencyId() {
        return currencyId;
      }

      @Override
      public int priority() {
        return priority;
      }

      @Override
      public void init(kz.bejiihiu.safecat.api.SafeCatRegistry reg) {
        reg.register(
            new kz.bejiihiu.safecat.api.Currency(
                currencyId, currencyId, "$", kz.bejiihiu.safecat.api.CurrencyType.CUSTOM));
        reg.register(this);
      }

      @Override
      public CompletableFuture<BigDecimal> getBalance(UUID player) {
        return CompletableFuture.completedFuture(balance);
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
    };
  }

  private static CurrencyProvider mockProvider(String currencyId, int priority) {
    CurrencyProvider p = mock(CurrencyProvider.class);
    when(p.getCurrencyId()).thenReturn(currencyId);
    when(p.priority()).thenReturn(priority);
    when(p.supports(any())).thenReturn(true);
    return p;
  }
}
