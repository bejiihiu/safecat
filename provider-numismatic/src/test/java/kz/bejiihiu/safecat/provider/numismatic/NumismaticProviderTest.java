package kz.bejiihiu.safecat.provider.numismatic;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.util.UUID;
import kz.bejiihiu.safecat.api.EventReason;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class NumismaticProviderTest {

  private static final UUID PLAYER = UUID.randomUUID();
  private NumismaticProvider provider;

  @BeforeEach
  void setUp() {
    provider = new NumismaticProvider();
  }

  @Test
  void getBalance_initialIsZero() {
    var balance = provider.getBalance(PLAYER).join();
    assertEquals(0, BigDecimal.ZERO.compareTo(balance));
  }

  @Test
  void deposit_increasesBalance() {
    provider.deposit(PLAYER, BigDecimal.TEN, EventReason.ADMIN).join();
    var balance = provider.getBalance(PLAYER).join();
    assertEquals(0, BigDecimal.TEN.compareTo(balance));
  }

  @Test
  void withdraw_decreasesBalance() {
    provider.deposit(PLAYER, BigDecimal.TEN, EventReason.ADMIN).join();
    provider.withdraw(PLAYER, BigDecimal.ONE, EventReason.ADMIN).join();
    var balance = provider.getBalance(PLAYER).join();
    assertEquals(0, BigDecimal.valueOf(9).compareTo(balance));
  }

  @Test
  void withdraw_insufficient_returnsFailure() {
    var result = provider.withdraw(PLAYER, BigDecimal.TEN, EventReason.ADMIN).join();
    assertFalse(result.success());
  }

  @Test
  void withdraw_negativeAmount_returnsFailure() {
    var result = provider.withdraw(PLAYER, BigDecimal.valueOf(-5), EventReason.ADMIN).join();
    assertFalse(result.success());
  }

  @Test
  void deposit_negativeAmount_returnsFailure() {
    var result = provider.deposit(PLAYER, BigDecimal.valueOf(-5), EventReason.ADMIN).join();
    assertFalse(result.success());
  }

  @Test
  void multipleOperations() {
    provider.deposit(PLAYER, BigDecimal.valueOf(100), EventReason.ADMIN).join();
    provider.withdraw(PLAYER, BigDecimal.valueOf(30), EventReason.SHOP_PURCHASE).join();
    provider.deposit(PLAYER, BigDecimal.valueOf(20), EventReason.QUEST_REWARD).join();
    var balance = provider.getBalance(PLAYER).join();
    assertEquals(0, BigDecimal.valueOf(90).compareTo(balance));
  }
}
