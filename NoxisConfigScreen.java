package com.noxisculture.client.config;

import com.noxisculture.config.NoxisConfig;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

/**
 * Pantalla de configuración de Noxis Culture (se abre desde Mod Menu, si está instalado).
 *
 * <p>Estética sobria: fondo oscuro, un panel con borde violeta, títulos de sección en lila y los
 * botones normales de Minecraft (legibles con cualquier escala de interfaz). Dos columnas:
 * comportamientos a la izquierda; efectos y sonidos a la derecha. En pantallas angostas pasa a una
 * sola columna.</p>
 *
 * <p>Se trabaja sobre una copia: "Guardar" aplica y escribe el archivo, "Cancelar" (o Esc) descarta
 * y "Restablecer" vuelve a los valores por defecto (que son los de siempre del mod).</p>
 */
public class NoxisConfigScreen extends Screen {
    private static final int[] FREQ_STEPS = {0, 25, 50, 100, 150, 200};
    private static final int[] PARTICLE_STEPS = {25, 50, 75, 100};
    private static final int BUTTON_W = 150;
    private static final int BUTTON_H = 20;
    private static final int ROW = 24;
    private static final int COLOR_TITLE = 0xFFEBD9FF;
    private static final int COLOR_HEADER = 0xFFC79BFF;
    private static final int COLOR_NOTE = 0xFF9E92B0;
    private static final int PANEL = 0xC0120A1C;
    private static final int BORDER = 0xFF6B3AA8;

    private final @Nullable Screen parent;
    private NoxisConfig working;

    // Posiciones calculadas en init (para dibujar los títulos de sección).
    private int leftX, rightX, top, headerBehaviorY, headerVisualY, headerSoundY, panelX0, panelY0, panelX1, panelY1;

    public NoxisConfigScreen(@Nullable Screen parent) {
        this(parent, NoxisConfig.get().copy());
    }

    private NoxisConfigScreen(@Nullable Screen parent, NoxisConfig working) {
        super(Component.translatable("noxis_culture.config.title"));
        this.parent = parent;
        this.working = working;
    }

    @Override
    protected void init() {
        boolean twoColumns = this.width >= BUTTON_W * 2 + 40;
        int center = this.width / 2;
        this.top = 34;
        this.leftX = twoColumns ? center - BUTTON_W - 6 : center - BUTTON_W / 2;
        this.rightX = twoColumns ? center + 6 : this.leftX;

        // ---- Comportamientos de los Noxis
        int y = this.top + 14;
        this.headerBehaviorY = this.top;
        this.freqButton(this.leftX, y, "animal_watching", () -> this.working.animalWatching, v -> this.working.animalWatching = v);
        y += ROW;
        this.freqButton(this.leftX, y, "humming", () -> this.working.humming, v -> this.working.humming = v);
        y += ROW;
        this.toggleButton(this.leftX, y, "hum_reactions", () -> this.working.humReactions, v -> this.working.humReactions = v);
        y += ROW;
        this.freqButton(this.leftX, y, "reading_writing", () -> this.working.readingWriting, v -> this.working.readingWriting = v);
        y += ROW;
        int leftBottom = y;

        // ---- Efectos visuales y sonidos
        int ry = twoColumns ? this.top : leftBottom + 6;
        this.headerVisualY = ry;
        ry += 14;
        this.toggleButton(this.rightX, ry, "decorative_particles", () -> this.working.decorativeParticles, v -> this.working.decorativeParticles = v);
        ry += ROW;
        this.stepButton(this.rightX, ry, "particle_amount", PARTICLE_STEPS, () -> this.working.particleAmount, v -> this.working.particleAmount = v);
        ry += ROW + 6;
        this.headerSoundY = ry;
        ry += 14;
        this.toggleButton(this.rightX, ry, "ambient_sounds", () -> this.working.ambientSounds, v -> this.working.ambientSounds = v);
        ry += ROW;

        int contentBottom = Math.max(leftBottom, ry);
        this.panelX0 = Math.min(this.leftX, this.rightX) - 10;
        this.panelX1 = Math.max(this.leftX, this.rightX) + BUTTON_W + 10;
        this.panelY0 = this.top - 8;
        this.panelY1 = contentBottom + 4;

        // ---- Botones de abajo
        int by = Math.min(this.height - 28, contentBottom + 30);
        int bw = 98;
        this.addRenderableWidget(Button.builder(Component.translatable("noxis_culture.config.reset"), b -> {
            // Restablecer: misma pantalla con los valores por defecto (todavía sin guardar).
            if (this.minecraft != null) this.minecraft.gui.setScreen(new NoxisConfigScreen(this.parent, NoxisConfig.defaults()));
        }).pos(center - bw - bw / 2 - 6, by).size(bw, BUTTON_H)
                .tooltip(Tooltip.create(Component.translatable("noxis_culture.config.reset.tooltip"))).build());
        this.addRenderableWidget(Button.builder(Component.translatable("noxis_culture.config.cancel"), b -> this.close(false))
                .pos(center - bw / 2, by).size(bw, BUTTON_H).build());
        this.addRenderableWidget(Button.builder(Component.translatable("noxis_culture.config.save"), b -> this.close(true))
                .pos(center + bw / 2 + 6, by).size(bw, BUTTON_H).build());
    }

    // ------------------------------------------------------------------ controles

    private void freqButton(int x, int y, String key, IntSupplier get, IntConsumer set) {
        this.stepButton(x, y, key, FREQ_STEPS, get, set);
    }

    /** Botón que recorre una lista de valores (porcentajes): cada clic pasa al siguiente (y vuelve a empezar). */
    private void stepButton(int x, int y, String key, int[] steps, IntSupplier get, IntConsumer set) {
        Button button = Button.builder(this.label(key, get.getAsInt(), steps == FREQ_STEPS), b -> {
            int current = get.getAsInt();
            int idx = 0;
            for (int i = 0; i < steps.length; i++) if (steps[i] <= current) idx = i;
            int next = steps[(idx + 1) % steps.length];
            set.accept(next);
            b.setMessage(this.label(key, next, steps == FREQ_STEPS));
        }).pos(x, y).size(BUTTON_W, BUTTON_H)
                .tooltip(Tooltip.create(Component.translatable("noxis_culture.config.option." + key + ".tooltip"))).build();
        this.addRenderableWidget(button);
    }

    private void toggleButton(int x, int y, String key, java.util.function.BooleanSupplier get, java.util.function.Consumer<Boolean> set) {
        Button button = Button.builder(this.toggleLabel(key, get.getAsBoolean()), b -> {
            boolean next = !get.getAsBoolean();
            set.accept(next);
            b.setMessage(this.toggleLabel(key, next));
        }).pos(x, y).size(BUTTON_W, BUTTON_H)
                .tooltip(Tooltip.create(Component.translatable("noxis_culture.config.option." + key + ".tooltip"))).build();
        this.addRenderableWidget(button);
    }

    private Component label(String key, int value, boolean frequency) {
        Component v;
        if (frequency && value == 0) v = Component.translatable("noxis_culture.config.value.off");
        else if (frequency && value == 100) v = Component.translatable("noxis_culture.config.value.normal");
        else v = Component.literal(value + "%");
        return Component.translatable("noxis_culture.config.option." + key, v);
    }

    private Component toggleLabel(String key, boolean on) {
        return Component.translatable("noxis_culture.config.option." + key,
                Component.translatable(on ? "noxis_culture.config.value.on" : "noxis_culture.config.value.off"));
    }

    private void close(boolean save) {
        if (save) NoxisConfig.apply(this.working);
        if (this.minecraft != null) this.minecraft.gui.setScreen(this.parent);
    }

    @Override
    public void onClose() {
        this.close(false);
    }

    // ------------------------------------------------------------------ dibujo

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        // (El fondo con desenfoque lo dibuja el juego antes de llamar a este método.)
        // Panel oscuro con borde violeta.
        graphics.fill(this.panelX0, this.panelY0, this.panelX1, this.panelY1, PANEL);
        graphics.fill(this.panelX0, this.panelY0, this.panelX1, this.panelY0 + 1, BORDER);
        graphics.fill(this.panelX0, this.panelY1 - 1, this.panelX1, this.panelY1, BORDER);
        graphics.fill(this.panelX0, this.panelY0, this.panelX0 + 1, this.panelY1, BORDER);
        graphics.fill(this.panelX1 - 1, this.panelY0, this.panelX1, this.panelY1, BORDER);
        graphics.centeredText(this.font, this.title.getString(), this.width / 2, 12, COLOR_TITLE);
        graphics.text(this.font, Component.translatable("noxis_culture.config.section.behavior").getString(), this.leftX + 2, this.headerBehaviorY + 2, COLOR_HEADER);
        graphics.text(this.font, Component.translatable("noxis_culture.config.section.visual").getString(), this.rightX + 2, this.headerVisualY + 2, COLOR_HEADER);
        graphics.text(this.font, Component.translatable("noxis_culture.config.section.sound").getString(), this.rightX + 2, this.headerSoundY + 2, COLOR_HEADER);
        int noteY = this.panelY1 + 6;
        if (noteY + 10 < this.height - 30) {
            graphics.centeredText(this.font, Component.translatable("noxis_culture.config.note").getString(), this.width / 2, noteY, COLOR_NOTE);
        }
        super.extractRenderState(graphics, mouseX, mouseY, delta);
    }
}
