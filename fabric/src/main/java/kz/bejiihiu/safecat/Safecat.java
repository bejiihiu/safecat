package kz.bejiihiu.safecat;

import java.util.function.Consumer;
import kz.bejiihiu.safecat.api.BalanceChangeEvent;
import kz.bejiihiu.safecat.api.TransactionEvent;
import kz.bejiihiu.safecat.common.PlatformHelper;
import kz.bejiihiu.safecat.common.SafeCatCore;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;

public class Safecat implements ModInitializer {

  public static final Event<Consumer<BalanceChangeEvent>> BALANCE_CHANGE_EVENT =
      EventFactory.createArrayBacked(
          Consumer.class,
          listeners ->
              event -> {
                for (Consumer<BalanceChangeEvent> listener : listeners) {
                  listener.accept(event);
                }
              });

  public static final Event<Consumer<TransactionEvent>> TRANSACTION_EVENT =
      EventFactory.createArrayBacked(
          Consumer.class,
          listeners ->
              event -> {
                for (Consumer<TransactionEvent> listener : listeners) {
                  listener.accept(event);
                }
              });

  @Override
  public void onInitialize() {
    SafeCatCore.initialize().join();

    var bus = SafeCatCore.eventBus();
    bus.register(
        new Object() {
          @com.google.common.eventbus.Subscribe
          public void on(BalanceChangeEvent e) {
            BALANCE_CHANGE_EVENT.invoker().accept(e);
          }

          @com.google.common.eventbus.Subscribe
          public void on(TransactionEvent e) {
            TRANSACTION_EVENT.invoker().accept(e);
          }
        });

    CommandRegistrationCallback.EVENT.register(
        (dispatcher, registryAccess, environment) -> PlatformHelper.registerCommands(dispatcher));

    Constants.LOG.info("SafeCat initialized on Fabric");
  }
}
