package kz.bejiihiu.safecat.common;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import kz.bejiihiu.safecat.api.CurrencyProvider;
import kz.bejiihiu.safecat.api.EventReason;
import kz.bejiihiu.safecat.api.SafeCatEventBus;
import kz.bejiihiu.safecat.api.TransactionResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PriorityChainTest {

  private static final UUID PLAYER = UUID.randomUUID();
  private static final String CURRENCY_ID = "test:coin";

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
  void earliestProviderHandlesWithdraw() {
    CurrencyProvider earliest = mockProvider(CURRENCY_ID, 100);
    when(earliest.withdraw(PLAYER, BigDecimal.ONE, EventReason.SHOP_PURCHASE))
        .thenReturn(
            CompletableFuture.completedFuture(
                TransactionResult.success(BigDecimal.ONE, CURRENCY_ID)));
    CurrencyProvider normal = mockProvider(CURRENCY_ID, 50);
    CurrencyProvider late = mockProvider(CURRENCY_ID, 0);
    registry.register(late);
    registry.register(normal);
    registry.register(earliest);

    TransactionResult result =
        api.withdraw(PLAYER, CURRENCY_ID, BigDecimal.ONE, EventReason.SHOP_PURCHASE).join();

    assertTrue(result.success());
    verify(earliest).withdraw(PLAYER, BigDecimal.ONE, EventReason.SHOP_PURCHASE);
    verify(normal, never()).withdraw(any(), any(), any());
    verify(late, never()).withdraw(any(), any(), any());
  }

  @Test
  void earliestProviderHandlesDeposit() {
    CurrencyProvider earliest = mockProvider(CURRENCY_ID, 100);
    when(earliest.deposit(PLAYER, BigDecimal.TEN, EventReason.QUEST_REWARD))
        .thenReturn(
            CompletableFuture.completedFuture(
                TransactionResult.success(BigDecimal.TEN, CURRENCY_ID)));
    CurrencyProvider normal = mockProvider(CURRENCY_ID, 50);
    CurrencyProvider late = mockProvider(CURRENCY_ID, 0);
    registry.register(late);
    registry.register(normal);
    registry.register(earliest);

    TransactionResult result =
        api.deposit(PLAYER, CURRENCY_ID, BigDecimal.TEN, EventReason.QUEST_REWARD).join();

    assertTrue(result.success());
    verify(earliest).deposit(PLAYER, BigDecimal.TEN, EventReason.QUEST_REWARD);
    verify(normal, never()).deposit(any(), any(), any());
    verify(late, never()).deposit(any(), any(), any());
  }

  @Test
  void normalPicksUpWhenEarliestReturnsFailure() {
    CurrencyProvider earliest = mockProvider(CURRENCY_ID, 100);
    when(earliest.withdraw(PLAYER, BigDecimal.ONE, EventReason.SHOP_PURCHASE))
        .thenReturn(
            CompletableFuture.completedFuture(TransactionResult.failure(CURRENCY_ID, "no funds")));
    CurrencyProvider normal = mockProvider(CURRENCY_ID, 50);
    when(normal.withdraw(PLAYER, BigDecimal.ONE, EventReason.SHOP_PURCHASE))
        .thenReturn(
            CompletableFuture.completedFuture(
                TransactionResult.success(BigDecimal.ONE, CURRENCY_ID)));
    CurrencyProvider late = mockProvider(CURRENCY_ID, 0);
    registry.register(late);
    registry.register(normal);
    registry.register(earliest);

    TransactionResult result =
        api.withdraw(PLAYER, CURRENCY_ID, BigDecimal.ONE, EventReason.SHOP_PURCHASE).join();

    assertTrue(result.success());
    verify(earliest).withdraw(PLAYER, BigDecimal.ONE, EventReason.SHOP_PURCHASE);
    verify(normal).withdraw(PLAYER, BigDecimal.ONE, EventReason.SHOP_PURCHASE);
    verify(late, never()).withdraw(any(), any(), any());
  }

  @Test
  void latePicksUpWhenEarliestAndNormalReturnFailure() {
    CurrencyProvider earliest = mockProvider(CURRENCY_ID, 100);
    when(earliest.withdraw(PLAYER, BigDecimal.ONE, EventReason.SHOP_PURCHASE))
        .thenReturn(
            CompletableFuture.completedFuture(TransactionResult.failure(CURRENCY_ID, "no funds")));
    CurrencyProvider normal = mockProvider(CURRENCY_ID, 50);
    when(normal.withdraw(PLAYER, BigDecimal.ONE, EventReason.SHOP_PURCHASE))
        .thenReturn(
            CompletableFuture.completedFuture(
                TransactionResult.failure(CURRENCY_ID, "insufficient")));
    CurrencyProvider late = mockProvider(CURRENCY_ID, 0);
    when(late.withdraw(PLAYER, BigDecimal.ONE, EventReason.SHOP_PURCHASE))
        .thenReturn(
            CompletableFuture.completedFuture(
                TransactionResult.success(BigDecimal.ONE, CURRENCY_ID)));
    registry.register(late);
    registry.register(normal);
    registry.register(earliest);

    TransactionResult result =
        api.withdraw(PLAYER, CURRENCY_ID, BigDecimal.ONE, EventReason.SHOP_PURCHASE).join();

    assertTrue(result.success());
    verify(late).withdraw(PLAYER, BigDecimal.ONE, EventReason.SHOP_PURCHASE);
  }

  @Test
  void allProvidersFail_returnsFailureFallback() {
    CurrencyProvider earliest = mockProvider(CURRENCY_ID, 100);
    when(earliest.withdraw(PLAYER, BigDecimal.ONE, EventReason.SHOP_PURCHASE))
        .thenReturn(
            CompletableFuture.completedFuture(TransactionResult.failure(CURRENCY_ID, "no funds")));
    CurrencyProvider normal = mockProvider(CURRENCY_ID, 50);
    when(normal.withdraw(PLAYER, BigDecimal.ONE, EventReason.SHOP_PURCHASE))
        .thenReturn(
            CompletableFuture.completedFuture(
                TransactionResult.failure(CURRENCY_ID, "insufficient")));
    CurrencyProvider late = mockProvider(CURRENCY_ID, 0);
    when(late.withdraw(PLAYER, BigDecimal.ONE, EventReason.SHOP_PURCHASE))
        .thenReturn(
            CompletableFuture.completedFuture(TransactionResult.failure(CURRENCY_ID, "blocked")));
    registry.register(late);
    registry.register(normal);
    registry.register(earliest);

    TransactionResult result =
        api.withdraw(PLAYER, CURRENCY_ID, BigDecimal.ONE, EventReason.SHOP_PURCHASE).join();

    assertFalse(result.success());
    assertEquals("all providers failed", result.message());
  }

  @Test
  void unsupportedProviderSkippedInChain() {
    CurrencyProvider earliest = mockProvider(CURRENCY_ID, 100);
    when(earliest.supports(PLAYER)).thenReturn(false);
    CurrencyProvider normal = mockProvider(CURRENCY_ID, 50);
    when(normal.withdraw(PLAYER, BigDecimal.ONE, EventReason.SHOP_PURCHASE))
        .thenReturn(
            CompletableFuture.completedFuture(
                TransactionResult.success(BigDecimal.ONE, CURRENCY_ID)));
    CurrencyProvider late = mockProvider(CURRENCY_ID, 0);
    registry.register(late);
    registry.register(normal);
    registry.register(earliest);

    TransactionResult result =
        api.withdraw(PLAYER, CURRENCY_ID, BigDecimal.ONE, EventReason.SHOP_PURCHASE).join();

    assertTrue(result.success());
    verify(earliest, never()).withdraw(any(), any(), any());
    verify(normal).withdraw(PLAYER, BigDecimal.ONE, EventReason.SHOP_PURCHASE);
    verify(late, never()).withdraw(any(), any(), any());
  }

  private static CurrencyProvider mockProvider(String currencyId, int priority) {
    CurrencyProvider p = mock(CurrencyProvider.class);
    when(p.getCurrencyId()).thenReturn(currencyId);
    when(p.priority()).thenReturn(priority);
    when(p.supports(any())).thenReturn(true);
    return p;
  }
}
