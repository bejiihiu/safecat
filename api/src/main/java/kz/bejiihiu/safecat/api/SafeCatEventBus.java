package kz.bejiihiu.safecat.api;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public class SafeCatEventBus {
  private final Map<Class<?>, CopyOnWriteArrayList<Consumer<?>>> handlers =
      new ConcurrentHashMap<>();

  public <T> void on(Class<T> type, Consumer<T> handler) {
    handlers.computeIfAbsent(type, k -> new CopyOnWriteArrayList<>()).add(handler);
  }

  @SuppressWarnings("unchecked")
  public <T> void post(T event) {
    var list = handlers.get(event.getClass());
    if (list != null) {
      for (var h : list) {
        ((Consumer<T>) h).accept(event);
      }
    }
  }

  // Supports @com.google.common.eventbus.Subscribe for loader modules.
  public void register(Object subscriber) {
    for (Method method : subscriber.getClass().getDeclaredMethods()) {
      for (var ann : method.getDeclaredAnnotations()) {
        if (ann.annotationType().getName().equals("com.google.common.eventbus.Subscribe")) {
          var params = method.getParameterTypes();
          if (params.length == 1) {
            registerMethod(subscriber, method, params[0]);
          }
          break;
        }
      }
    }
  }

  @SuppressWarnings("unchecked")
  private <T> void registerMethod(Object subscriber, Method method, Class<?> paramType) {
    method.setAccessible(true);
    on((Class<T>) paramType, event -> {
      try {
        method.invoke(subscriber, event);
      } catch (Exception e) {
        throw new RuntimeException("event handler failed", e);
      }
    });
  }
}
