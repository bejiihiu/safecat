package kz.bejiihiu.safecat;

import kz.bejiihiu.safecat.api.BalanceChangeEvent;
import kz.bejiihiu.safecat.api.BalanceRequestEvent;
import kz.bejiihiu.safecat.api.RegisterCurrenciesEvent;
import kz.bejiihiu.safecat.api.RegisterProvidersEvent;
import kz.bejiihiu.safecat.api.TransactionEvent;
import kz.bejiihiu.safecat.common.PlatformHelper;
import kz.bejiihiu.safecat.common.SafeCatCore;
import net.neoforged.bus.api.Event;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

@Mod(Constants.MOD_ID)
public class Safecat {

  public Safecat() {
    SafeCatCore.initialize().join();

    var bus = SafeCatCore.eventBus();
    bus.register(
        new Object() {
          @com.google.common.eventbus.Subscribe
          public void on(BalanceChangeEvent e) {
            NeoForge.EVENT_BUS.post(new NeoForgeBridgeEvent(e));
          }

          @com.google.common.eventbus.Subscribe
          public void on(BalanceRequestEvent e) {
            NeoForge.EVENT_BUS.post(new NeoForgeBridgeEvent(e));
          }

          @com.google.common.eventbus.Subscribe
          public void on(TransactionEvent e) {
            NeoForge.EVENT_BUS.post(new NeoForgeBridgeEvent(e));
          }

          @com.google.common.eventbus.Subscribe
          public void on(RegisterCurrenciesEvent e) {
            NeoForge.EVENT_BUS.post(new NeoForgeBridgeEvent(e));
          }

          @com.google.common.eventbus.Subscribe
          public void on(RegisterProvidersEvent e) {
            NeoForge.EVENT_BUS.post(new NeoForgeBridgeEvent(e));
          }
        });

    NeoForge.EVENT_BUS.addListener(this::onRegisterCommands);

    Constants.LOG.info("SafeCat initialized on NeoForge");
  }

  private void onRegisterCommands(RegisterCommandsEvent event) {
    PlatformHelper.registerCommands(event.getDispatcher());
  }

  public static final class NeoForgeBridgeEvent extends Event {
    private final kz.bejiihiu.safecat.api.Event wrapped;

    public NeoForgeBridgeEvent(kz.bejiihiu.safecat.api.Event event) {
      this.wrapped = event;
    }

    public kz.bejiihiu.safecat.api.Event unwrap() {
      return wrapped;
    }
  }
}
