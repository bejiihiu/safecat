package kz.bejiihiu.safecat;

import kz.bejiihiu.safecat.api.BalanceChangeEvent;
import kz.bejiihiu.safecat.api.BalanceRequestEvent;
import kz.bejiihiu.safecat.api.RegisterCurrenciesEvent;
import kz.bejiihiu.safecat.api.RegisterProvidersEvent;
import kz.bejiihiu.safecat.api.TransactionEvent;
import kz.bejiihiu.safecat.commands.CommandsModule;
import kz.bejiihiu.safecat.common.PlatformHelper;
import kz.bejiihiu.safecat.common.SafeCatCore;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.fml.common.Mod;

@Mod(Constants.MOD_ID)
public class Safecat {

  public Safecat() {
    SafeCatCore.initialize().join();
    CommandsModule.register();

    var bus = SafeCatCore.eventBus();
    bus.register(
        new Object() {
          @com.google.common.eventbus.Subscribe
          public void on(BalanceChangeEvent e) {
            MinecraftForge.EVENT_BUS.post(new ForgeBridgeEvent(e));
          }

          @com.google.common.eventbus.Subscribe
          public void on(BalanceRequestEvent e) {
            MinecraftForge.EVENT_BUS.post(new ForgeBridgeEvent(e));
          }

          @com.google.common.eventbus.Subscribe
          public void on(TransactionEvent e) {
            MinecraftForge.EVENT_BUS.post(new ForgeBridgeEvent(e));
          }

          @com.google.common.eventbus.Subscribe
          public void on(RegisterCurrenciesEvent e) {
            MinecraftForge.EVENT_BUS.post(new ForgeBridgeEvent(e));
          }

          @com.google.common.eventbus.Subscribe
          public void on(RegisterProvidersEvent e) {
            MinecraftForge.EVENT_BUS.post(new ForgeBridgeEvent(e));
          }
        });

    MinecraftForge.EVENT_BUS.addListener(this::onRegisterCommands);

    Constants.LOG.info("SafeCat initialized on Forge");
  }

  private void onRegisterCommands(RegisterCommandsEvent event) {
    PlatformHelper.registerCommands(event.getDispatcher());
  }

  public static final class ForgeBridgeEvent extends Event {
    private final kz.bejiihiu.safecat.api.Event wrapped;

    public ForgeBridgeEvent(kz.bejiihiu.safecat.api.Event event) {
      this.wrapped = event;
    }

    public kz.bejiihiu.safecat.api.Event unwrap() {
      return wrapped;
    }
  }
}
