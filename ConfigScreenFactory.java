package com.terraformersmc.modmenu.api;

import net.minecraft.client.gui.screens.Screen;

/** STUB de compilación (se excluye del .jar en build.gradle): ver {@link ModMenuApi}. */
@FunctionalInterface
public interface ConfigScreenFactory<S extends Screen> {
    S create(Screen parent);
}
