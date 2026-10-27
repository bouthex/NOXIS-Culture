package com.noxisculture.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.noxisculture.NoxisCulture;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.util.Mth;

/**
 * Configuración de Noxis Culture ({@code config/noxis_culture.json}).
 *
 * <p>Dos grupos:</p>
 * <ul>
 *   <li><b>Comportamientos</b> (los lee la lógica del mundo, o sea el servidor): en un mundo de un
 *   jugador o en LAN se usan los de tu juego; en un servidor dedicado, los del archivo del servidor.
 *   No se sincronizan: cada lado usa su propio archivo.</li>
 *   <li><b>Efectos y sonidos decorativos</b> (solo del cliente): partículas y sonidos ambientales que
 *   no comunican peligro. Las llamas, el daño, la pantalla violeta y los sonidos de encendido y
 *   apagado del fuego nunca se apagan desde acá.</li>
 * </ul>
 *
 * <p>Los valores por defecto conservan exactamente el comportamiento actual del mod. Todos los
 * números se validan (se recortan a su rango) al cargar y al cambiarlos.</p>
 */
public final class NoxisConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String FILE = "noxis_culture.json";

    /** Frecuencias en porcentaje: 0 = desactivado, 100 = normal, 200 = el doble. */
    public static final int MIN_FREQ = 0;
    public static final int MAX_FREQ = 200;
    public static final int MIN_PARTICLES = 25;
    public static final int MAX_PARTICLES = 100;

    // ---- Comportamientos (servidor)
    public int animalWatching = 100;
    public int humming = 100;
    public boolean humReactions = true;
    public int readingWriting = 100;
    // ---- Efectos (cliente)
    public boolean decorativeParticles = true;
    public int particleAmount = 100;
    // ---- Sonidos (cliente)
    public boolean ambientSounds = true;

    private static NoxisConfig instance = new NoxisConfig();
    private static boolean loaded;

    public static NoxisConfig get() {
        if (!loaded) load();
        return instance;
    }

    public static NoxisConfig defaults() {
        return new NoxisConfig();
    }

    private static Path path() {
        return FabricLoader.getInstance().getConfigDir().resolve(FILE);
    }

    /** Carga el archivo (si no existe o está roto, usa los valores por defecto y lo vuelve a escribir). */
    public static synchronized void load() {
        loaded = true;
        NoxisConfig cfg = new NoxisConfig();
        Path p = path();
        if (Files.exists(p)) {
            try (Reader r = Files.newBufferedReader(p, StandardCharsets.UTF_8)) {
                JsonObject o = JsonParser.parseReader(r).getAsJsonObject();
                if (o.has("animalWatching")) cfg.animalWatching = o.get("animalWatching").getAsInt();
                if (o.has("humming")) cfg.humming = o.get("humming").getAsInt();
                if (o.has("humReactions")) cfg.humReactions = o.get("humReactions").getAsBoolean();
                if (o.has("readingWriting")) cfg.readingWriting = o.get("readingWriting").getAsInt();
                if (o.has("decorativeParticles")) cfg.decorativeParticles = o.get("decorativeParticles").getAsBoolean();
                if (o.has("particleAmount")) cfg.particleAmount = o.get("particleAmount").getAsInt();
                if (o.has("ambientSounds")) cfg.ambientSounds = o.get("ambientSounds").getAsBoolean();
            } catch (Exception e) {
                NoxisCulture.LOGGER.warn("Noxis Culture: no se pudo leer {} ({}); se usan los valores por defecto.", p, e.toString());
                cfg = new NoxisConfig();
            }
        }
        cfg.validate();
        instance = cfg;
        save();
    }

    /** Guarda la configuración actual. */
    public static synchronized void save() {
        NoxisConfig cfg = instance;
        cfg.validate();
        JsonObject o = new JsonObject();
        o.addProperty("animalWatching", cfg.animalWatching);
        o.addProperty("humming", cfg.humming);
        o.addProperty("humReactions", cfg.humReactions);
        o.addProperty("readingWriting", cfg.readingWriting);
        o.addProperty("decorativeParticles", cfg.decorativeParticles);
        o.addProperty("particleAmount", cfg.particleAmount);
        o.addProperty("ambientSounds", cfg.ambientSounds);
        try {
            Files.createDirectories(path().getParent());
            try (Writer w = Files.newBufferedWriter(path(), StandardCharsets.UTF_8)) {
                GSON.toJson(o, w);
            }
        } catch (IOException e) {
            NoxisCulture.LOGGER.warn("Noxis Culture: no se pudo guardar la configuración ({}).", e.toString());
        }
    }

    /** Reemplaza todos los valores (lo usa la pantalla al guardar) y guarda. */
    public static synchronized void apply(NoxisConfig values) {
        values.validate();
        instance = values.copy();
        loaded = true;
        save();
    }

    public NoxisConfig copy() {
        NoxisConfig c = new NoxisConfig();
        c.animalWatching = this.animalWatching;
        c.humming = this.humming;
        c.humReactions = this.humReactions;
        c.readingWriting = this.readingWriting;
        c.decorativeParticles = this.decorativeParticles;
        c.particleAmount = this.particleAmount;
        c.ambientSounds = this.ambientSounds;
        return c;
    }

    public void validate() {
        this.animalWatching = Mth.clamp(this.animalWatching, MIN_FREQ, MAX_FREQ);
        this.humming = Mth.clamp(this.humming, MIN_FREQ, MAX_FREQ);
        this.readingWriting = Mth.clamp(this.readingWriting, MIN_FREQ, MAX_FREQ);
        this.particleAmount = Mth.clamp(this.particleAmount, MIN_PARTICLES, MAX_PARTICLES);
    }

    // ------------------------------------------------------------------ ayudas para los sistemas

    /**
     * Escala una espera según una frecuencia en porcentaje (100 = sin cambios, 200 = la mitad de
     * espera, 50 = el doble). Con 0 el comportamiento está desactivado: devolver un valor enorme es
     * solo un resguardo, porque los sistemas ya comprueban {@link #enabled(int)} antes.
     */
    public static int scaleCooldown(int ticks, int frequency) {
        if (frequency <= 0) return Integer.MAX_VALUE / 4;
        return Math.max(1, Math.round(ticks * 100.0F / frequency));
    }

    public static boolean enabled(int frequency) {
        return frequency > 0;
    }

    /** Partículas decorativas: ¿se dibuja esta? (según el interruptor y la cantidad). Solo cliente. */
    public static boolean decorativeParticle(net.minecraft.util.RandomSource random) {
        NoxisConfig c = get();
        return c.decorativeParticles && (c.particleAmount >= 100 || random.nextInt(100) < c.particleAmount);
    }

    /** Sonidos ambientales decorativos (burbujeo, flujo, crepitar). Solo cliente. */
    public static boolean ambientSounds() {
        return get().ambientSounds;
    }
}
