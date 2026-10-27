package com.terraformersmc.modmenu.api;

/**
 * STUB de compilación (no va dentro del .jar). Copia mínima de la interfaz pública de Mod Menu
 * (TerraformersMC) con solo el método que usa Noxis Culture. Así el mod compila sin descargar Mod Menu
 * y, si Mod Menu está instalado, en el juego se usa su interfaz real (mismo nombre y firma).
 * Si Mod Menu no está instalado, esta entrada nunca se carga.
 */
public interface ModMenuApi {
    default ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> null;
    }
}
