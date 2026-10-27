package com.noxisculture.client.config;

import com.noxisculture.config.NoxisConfig;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

/**
 * Pantalla de configuración de Noxis Culture: la ÚNICA. Se abre desde Mod Menu (si está instalado),
 * desde el botón "Noxis Culture" de la pantalla de Opciones de Minecraft, o con el comando
 * {@code /noxisculture config}. Las tres vías abren esta misma pantalla y guardan el mismo archivo.
 *
 * <p>Estética sobria: fondo del juego, un panel oscuro con borde violeta, títulos en lila y los
 * botones normales de Minecraft. Dos pestañas para que entre en cualquier tamaño de interfaz:
 * <b>Comportamientos</b> (lo que hacen los Noxis: se aplica a la lógica del mundo) y <b>Efectos y
 * sonidos</b> (solo lo que ves y oyes en tu juego). Apagar un efecto visual nunca apaga un
 * comportamiento, y al revés.</p>
 *
 * <p>Se trabaja sobre una copia: "Guardar" aplica y escribe el archivo, "Cancelar" (o Esc) descarta
 * y "Restablecer" vuelve a los valores por defecto (los de siempre del mod).</p>
 */
public class NoxisConfigScreen extends Screen {
    private static final int[] FREQ_STEPS = {0, 25, 50, 100, 150, 200};
    private static final int[] PARTICLE_STEPS = {25, 50, 75, 100};
    private static final int BUTTON_W = 150;
    private static final int BUTTON_H = 20;
    private static final int ROW = 24;
    private static final int GAP = 8;
    private static final int COLOR_TITLE = 0xFFEBD9FF;
    private static final int COLOR_HEADER = 0xFFC79BFF;
    private static final int COLOR_NOTE = 0xFF9E92B0;
    private static final int PANEL = 0xC0120A1C;
    private static final int BORDER = 0xFF6B3AA8;

    private enum Tab { BEHAVIOR, EFFECTS }

    private final @Nullable Screen parent;
    private final NoxisConfig working;
    private final Tab tab;

    private int panelX0, panelY0, panelX1, panelY1, headerY, header2Y, col1X, col2X;
    private boolean twoColumns;

    public NoxisConfigScreen(@Nullable Screen parent) {
        this(parent, NoxisConfig.get().copy(), Tab.BEHAVIOR);
    }

    private NoxisConfigScreen(@Nullable Screen parent, NoxisConfig working, Tab tab) {
        super(Component.translatable("noxis_culture.config.title"));
        this.parent = parent;
        this.working = working;
        this.tab = tab;
    }

    private void reopen(NoxisConfig values, Tab newTab) {
        if (this.minecraft != null) this.minecraft.gui.setScreen(new NoxisConfigScreen(this.parent, values, newTab));
    }

    @Override
    protected void init() {
        int center = this.width / 2;
        this.twoColumns = this.width >= BUTTON_W * 2 + GAP + 24;
        this.col1X = this.twoColumns ? center - BUTTON_W - GAP / 2 : center - BUTTON_W / 2;
        this.col2X = this.twoColumns ? center + GAP / 2 : this.col1X;

        // ---- Pestañas
        int tabW = 120;
        int tabY = 28;
        Button behavior = Button.builder(Component.translatable("noxis_culture.config.section.behavior"),
                b -> this.reopen(this.working, Tab.BEHAVIOR)).pos(center - tabW - 2, tabY).size(tabW, BUTTON_H).build();
        Button effects = Button.builder(Component.translatable("noxis_culture.config.tab.effects"),
                b -> this.reopen(this.working, Tab.EFFECTS)).pos(center + 2, tabY).size(tabW, BUTTON_H).build();
        behavior.active = this.tab != Tab.BEHAVIOR;      // la pestaña abierta queda "apretada"
        effects.active = this.tab != Tab.EFFECTS;
        this.addRenderableWidget(behavior);
        this.addRenderableWidget(effects);

        int top = tabY + BUTTON_H + 12;
        this.headerY = top;
        int y0 = top + 14;
        int bottom;
        if (this.tab == Tab.BEHAVIOR) {
            Row[] rows = {
                    freq("animal_watching", () -> this.working.animalWatching, v -> this.working.animalWatching = v),
                    freq("humming", () -> this.working.humming, v -> this.working.humming = v),
                    toggle("hum_reactions", () -> this.working.humReactions, v -> this.working.humReactions = v),
                    freq("reading_writing", () -> this.working.readingWriting, v -> this.working.readingWriting = v),
                    freq("sky_gazing", () -> this.working.skyGazing, v -> this.working.skyGazing = v),
                    freq("quiet_moments", () -> this.working.quietMoments, v -> this.working.quietMoments = v),
                    freq("discoveries", () -> this.working.discoveries, v -> this.working.discoveries = v),
                    freq("painting", () -> this.working.painting, v -> this.working.painting = v),
            };
            bottom = this.layout(rows, y0);
            this.header2Y = -1;
        } else {
            Row[] visual = {
                    toggle("decorative_particles", () -> this.working.decorativeParticles, v -> this.working.decorativeParticles = v),
                    steps("particle_amount", PARTICLE_STEPS, () -> this.working.particleAmount, v -> this.working.particleAmount = v),
            };
            int afterVisual = this.layout(visual, y0);
            this.header2Y = afterVisual + 6;
            Row[] sound = {
                    toggle("ambient_sounds", () -> this.working.ambientSounds, v -> this.working.ambientSounds = v),
            };
            bottom = this.layout(sound, this.header2Y + 14);
        }

        this.panelX0 = Math.min(this.col1X, this.col2X) - 10;
        this.panelX1 = Math.max(this.col1X, this.col2X) + BUTTON_W + 10;
        this.panelY0 = top - 8;
        this.panelY1 = bottom + 4;

        // ---- Botones de abajo
        int by = Math.min(this.height - 26, bottom + 22);
        int bw = 98;
        this.addRenderableWidget(Button.builder(Component.translatable("noxis_culture.config.reset"),
                        b -> this.reopen(NoxisConfig.defaults(), this.tab))
                .pos(center - bw - bw / 2 - 6, by).size(bw, BUTTON_H)
                .tooltip(Tooltip.create(Component.translatable("noxis_culture.config.reset.tooltip"))).build());
        this.addRenderableWidget(Button.builder(Component.translatable("noxis_culture.config.cancel"), b -> this.close(false))
                .pos(center - bw / 2, by).size(bw, BUTTON_H).build());
        this.addRenderableWidget(Button.builder(Component.translatable("noxis_culture.config.save"), b -> this.close(true))
                .pos(center + bw / 2 + 6, by).size(bw, BUTTON_H).build());
    }

    /** Ubica las filas en una o dos columnas; devuelve dónde termina. */
    private int layout(Row[] rows, int y0) {
        int perColumn = this.twoColumns ? (rows.length + 1) / 2 : rows.length;
        for (int i = 0; i < rows.length; i++) {
            int col = i / perColumn;
            int x = col == 0 ? this.col1X : this.col2X;
            int y = y0 + (i % perColumn) * ROW;
            this.addRenderableWidget(rows[i].build(x, y));
        }
        return y0 + perColumn * ROW;
    }

    // ------------------------------------------------------------------ controles

    @FunctionalInterface
    private interface Row {
        Button build(int x, int y);
    }

    private Row freq(String key, IntSupplier get, IntConsumer set) {
        return this.steps(key, FREQ_STEPS, get, set);
    }

    /** Botón que recorre una lista de valores (porcentajes): cada clic pasa al siguiente (y vuelve a empezar). */
    private Row steps(String key, int[] steps, IntSupplier get, IntConsumer set) {
        boolean frequency = steps == FREQ_STEPS;
        return (x, y) -> Button.builder(this.label(key, get.getAsInt(), frequency), b -> {
            int current = get.getAsInt();
            int idx = 0;
            for (int i = 0; i < steps.length; i++) if (steps[i] <= current) idx = i;
            int next = steps[(idx + 1) % steps.length];
            set.accept(next);
            b.setMessage(this.label(key, next, frequency));
        }).pos(x, y).size(BUTTON_W, BUTTON_H)
                .tooltip(Tooltip.create(Component.translatable("noxis_culture.config.option." + key + ".tooltip"))).build();
    }

    private Row toggle(String key, BooleanSupplier get, Consumer<Boolean> set) {
        return (x, y) -> Button.builder(this.toggleLabel(key, get.getAsBoolean()), b -> {
            boolean next = !get.getAsBoolean();
            set.accept(next);
            b.setMessage(this.toggleLabel(key, next));
        }).pos(x, y).size(BUTTON_W, BUTTON_H)
                .tooltip(Tooltip.create(Component.translatable("noxis_culture.config.option." + key + ".tooltip"))).build();
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
        // (El fondo lo dibuja el juego antes de llamar a este método.)
        graphics.fill(this.panelX0, this.panelY0, this.panelX1, this.panelY1, PANEL);
        graphics.fill(this.panelX0, this.panelY0, this.panelX1, this.panelY0 + 1, BORDER);
        graphics.fill(this.panelX0, this.panelY1 - 1, this.panelX1, this.panelY1, BORDER);
        graphics.fill(this.panelX0, this.panelY0, this.panelX0 + 1, this.panelY1, BORDER);
        graphics.fill(this.panelX1 - 1, this.panelY0, this.panelX1, this.panelY1, BORDER);
        graphics.centeredText(this.font, this.title.getString(), this.width / 2, 10, COLOR_TITLE);
        String header = this.tab == Tab.BEHAVIOR ? "noxis_culture.config.section.behavior" : "noxis_culture.config.section.visual";
        graphics.text(this.font, Component.translatable(header).getString(), this.col1X + 2, this.headerY + 2, COLOR_HEADER);
        if (this.header2Y >= 0) {
            graphics.text(this.font, Component.translatable("noxis_culture.config.section.sound").getString(), this.col1X + 2, this.header2Y + 2, COLOR_HEADER);
        }
        int noteY = this.panelY1 + 4;
        String note = this.tab == Tab.BEHAVIOR ? "noxis_culture.config.note" : "noxis_culture.config.note_effects";
        if (noteY + 10 < this.height - 28) {
            graphics.centeredText(this.font, Component.translatable(note).getString(), this.width / 2, noteY, COLOR_NOTE);
        }
        super.extractRenderState(graphics, mouseX, mouseY, delta);
    }
}
