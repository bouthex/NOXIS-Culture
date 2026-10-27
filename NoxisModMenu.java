package com.noxisculture.client.config;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

/** Entrada para Mod Menu (opcional): el botón "Configurar" abre la pantalla de Noxis Culture. */
public class NoxisModMenu implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return NoxisConfigScreen::new;
    }
}
