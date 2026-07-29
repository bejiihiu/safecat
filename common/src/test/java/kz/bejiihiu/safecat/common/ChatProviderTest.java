package kz.bejiihiu.safecat.common;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import kz.bejiihiu.safecat.api.ChatFormatEvent;
import kz.bejiihiu.safecat.api.ChatProvider;
import kz.bejiihiu.safecat.api.SafeCatEventBus;
import kz.bejiihiu.safecat.api.SafeCatRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ChatProviderTest {

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
  void registerAndRetrieveChatProvider() {
    ChatProvider p = mock(ChatProvider.class);
    when(p.getProviderId()).thenReturn("test:chat");
    registry.register(p);
    assertSame(p, registry.getChatProvider("test:chat"));
    assertTrue(registry.getChatProviders().contains(p));
  }

  @Test
  void getChatProvider_unknownId_returnsNull() {
    assertNull(registry.getChatProvider("nonexistent"));
  }

  @Test
  void getChatProviders_empty_returnsEmptyCollection() {
    assertTrue(registry.getChatProviders().isEmpty());
  }

  @Test
  void getPrefix_returnsFromHighestPriorityProvider() {
    ChatProvider low = chatProvider("low", 0, Optional.empty(), Optional.empty(), Optional.empty());
    ChatProvider high =
        chatProvider("high", 10, Optional.of("[High]"), Optional.empty(), Optional.empty());
    registry.register(low);
    registry.register(high);

    Optional<String> prefix = api.getPrefix(PLAYER).join();
    assertEquals(Optional.of("[High]"), prefix);
  }

  @Test
  void getPrefix_fallsBackWhenHighPriorityReturnsEmpty() {
    ChatProvider high =
        chatProvider("high", 10, Optional.empty(), Optional.empty(), Optional.empty());
    ChatProvider low =
        chatProvider("low", 0, Optional.of("[Low]"), Optional.empty(), Optional.empty());
    registry.register(high);
    registry.register(low);

    Optional<String> prefix = api.getPrefix(PLAYER).join();
    assertEquals(Optional.of("[Low]"), prefix);
  }

  @Test
  void getPrefix_noProviders_returnsEmpty() {
    Optional<String> prefix = api.getPrefix(PLAYER).join();
    assertEquals(Optional.empty(), prefix);
  }

  @Test
  void getSuffix_returnsFromHighestPriorityProvider() {
    ChatProvider low = chatProvider("low", 0, Optional.empty(), Optional.empty(), Optional.empty());
    ChatProvider high =
        chatProvider("high", 10, Optional.empty(), Optional.of("[Suffix]"), Optional.empty());
    registry.register(low);
    registry.register(high);

    Optional<String> suffix = api.getSuffix(PLAYER).join();
    assertEquals(Optional.of("[Suffix]"), suffix);
  }

  @Test
  void getSuffix_noProviders_returnsEmpty() {
    Optional<String> suffix = api.getSuffix(PLAYER).join();
    assertEquals(Optional.empty(), suffix);
  }

  @Test
  void getDisplayName_returnsFromHighestPriorityProvider() {
    ChatProvider high =
        chatProvider("high", 10, Optional.empty(), Optional.empty(), Optional.of("DisplayHigh"));
    ChatProvider low = chatProvider("low", 0, Optional.empty(), Optional.empty(), Optional.empty());
    registry.register(high);
    registry.register(low);

    Optional<String> displayName = api.getDisplayName(PLAYER).join();
    assertEquals(Optional.of("DisplayHigh"), displayName);
  }

  @Test
  void getDisplayName_noProviders_returnsEmpty() {
    Optional<String> displayName = api.getDisplayName(PLAYER).join();
    assertEquals(Optional.empty(), displayName);
  }

  @Test
  void format_appliesAllProvidersInOrder() {
    ChatProvider first = mock(ChatProvider.class);
    when(first.getProviderId()).thenReturn("first");
    when(first.priority()).thenReturn(10);
    when(first.format(anyString(), any())).thenAnswer(inv -> "{" + inv.getArgument(0) + "}");

    ChatProvider second = mock(ChatProvider.class);
    when(second.getProviderId()).thenReturn("second");
    when(second.priority()).thenReturn(0);
    when(second.format(anyString(), any())).thenAnswer(inv -> "[" + inv.getArgument(0) + "]");

    registry.register(second);
    registry.register(first);

    String result = api.format("hello", PLAYER);
    assertEquals("[{hello}]", result);
  }

  @Test
  void format_noProviders_returnsOriginalMessage() {
    String result = api.format("hello", PLAYER);
    assertEquals("hello", result);
  }

  @Test
  void format_firesChatFormatEvent() {
    ChatProvider p = mock(ChatProvider.class);
    when(p.getProviderId()).thenReturn("test");
    when(p.priority()).thenReturn(0);
    when(p.format(anyString(), any())).thenAnswer(inv -> inv.getArgument(0));
    registry.register(p);

    ChatFormatEvent[] captured = new ChatFormatEvent[1];
    eventBus.on(ChatFormatEvent.class, e -> captured[0] = e);

    api.format("hello", PLAYER);
    assertNotNull(captured[0]);
    assertEquals(PLAYER, captured[0].getPlayer());
    assertEquals("hello", captured[0].getMessage());
  }

  @Test
  void format_cancelledEvent_blocksFormatting() {
    eventBus.on(ChatFormatEvent.class, e -> e.setCancelled(true));
    ChatProvider p = mock(ChatProvider.class);
    when(p.getProviderId()).thenReturn("test");
    registry.register(p);

    String result = api.format("hello", PLAYER);
    assertEquals("hello", result);
    verify(p, never()).format(anyString(), any());
  }

  @Test
  void format_eventCanOverrideMessageAndFormat() {
    eventBus.on(
        ChatFormatEvent.class,
        e -> {
          e.setMessage("overridden");
          e.setFormat("&c");
        });

    ChatProvider p = mock(ChatProvider.class);
    when(p.getProviderId()).thenReturn("test");
    when(p.priority()).thenReturn(0);
    when(p.format(anyString(), any())).thenAnswer(inv -> inv.getArgument(0));
    registry.register(p);

    String result = api.format("hello", PLAYER);
    assertEquals("overridden", result);
  }

  @Test
  void getPrefix_providerThrowsException_fallsBack() {
    ChatProvider broken = mock(ChatProvider.class);
    when(broken.getProviderId()).thenReturn("broken");
    when(broken.priority()).thenReturn(10);
    when(broken.getPrefix(any()))
        .thenReturn(CompletableFuture.failedFuture(new RuntimeException("boom")));

    ChatProvider fallback = mock(ChatProvider.class);
    when(fallback.getProviderId()).thenReturn("fallback");
    when(fallback.priority()).thenReturn(0);
    when(fallback.getPrefix(any()))
        .thenReturn(CompletableFuture.completedFuture(Optional.of("[ok]")));

    registry.register(broken);
    registry.register(fallback);

    Optional<String> prefix = api.getPrefix(PLAYER).join();
    assertEquals(Optional.of("[ok]"), prefix);
  }

  @Test
  void initNotCalledOnRegister() {
    ChatProvider p = mock(ChatProvider.class);
    when(p.getProviderId()).thenReturn("test:init");
    registry.register(p);
    verify(p, never()).init(any(SafeCatRegistry.class));
  }

  private static ChatProvider chatProvider(
      String id,
      int priority,
      Optional<String> prefix,
      Optional<String> suffix,
      Optional<String> displayName) {
    ChatProvider p = mock(ChatProvider.class);
    when(p.getProviderId()).thenReturn(id);
    when(p.priority()).thenReturn(priority);
    when(p.getPrefix(any())).thenReturn(CompletableFuture.completedFuture(prefix));
    when(p.getSuffix(any())).thenReturn(CompletableFuture.completedFuture(suffix));
    when(p.getDisplayName(any())).thenReturn(CompletableFuture.completedFuture(displayName));
    return p;
  }
}
