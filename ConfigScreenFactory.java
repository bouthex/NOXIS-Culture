package com.terraformersmc.modmenu.api;

import net.minecraft.client.gui.screens.Screen;

/** STUB de compilación (no va dentro del .jar): ver {@link ModMenuApi}. */
@FunctionalInterface
public interface ConfigScreenFactory<S extends Screen> {
    S create(Screen parent);
}
