package com.noxisculture.client.config;

import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Cómo se llega a la configuración SIN Mod Menu (con Mod Menu también funcionan):
 *
 * <ul>
 *   <li><b>Botón en Opciones</b>: en la pantalla de Opciones de Minecraft (desde el menú principal o
 *   desde el menú de pausa) aparece un botón chico "Noxis Culture" arriba a la izquierda. Minecraft no
 *   trae un lugar universal para la configuración de los mods, así que se agrega con la API de
 *   pantallas de Fabric (Fabric API, que el mod ya necesita).</li>
 *   <li><b>Comando</b>: {@code /noxisculture config} (comando del cliente: no necesita permisos ni que
 *   el servidor tenga el mod).</li>
 * </ul>
 * Las dos vías (y Mod Menu) abren la misma {@link NoxisConfigScreen}.
 */
public final class NoxisConfigAccess {
    private NoxisConfigAccess() {}

    /** El comando se ejecuta con el chat abierto: la pantalla se abre en el tick siguiente, ya cerrado el chat. */
    private static boolean openNextTick;

    public static void register() {
        ScreenEvents.AFTER_INIT.register(NoxisConfigAccess::afterInit);

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, buildContext) -> dispatcher.register(
                ClientCommands.literal("noxisculture")
                        .then(ClientCommands.literal("config").executes(context -> {
                            openNextTick = true;
                            return 1;
                        }))));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (openNextTick) {
                openNextTick = false;
                client.gui.setScreen(new NoxisConfigScreen(client.gui.screen()));
            }
        });
    }

    private static void afterInit(Minecraft client, Screen screen, int width, int height) {
        // La pantalla de Opciones de Minecraft (por nombre: así no depende de su paquete exacto).
        if (!"OptionsScreen".equals(screen.getClass().getSimpleName())) return;
        Button button = Button.builder(Component.translatable("noxis_culture.config.open_button"),
                        b -> client.gui.setScreen(new NoxisConfigScreen(screen)))
                .pos(6, 6).size(92, 20)
                .tooltip(Tooltip.create(Component.translatable("noxis_culture.config.open_button.tooltip")))
                .build();
        Screens.getWidgets(screen).add(button);
    }
}
