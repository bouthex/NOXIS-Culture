package com.noxisculture.entity.ai;

import com.noxisculture.entity.idle.NoxisKeepsakeAnimation;
import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Leer y escribir (servidor), solo para los poquitos Noxis lectores/escritores. De vez en cuando,
 * tranquilo y sin nada importante que hacer, busca un lugarcito tranquilo cerca (sin nadie al
 * lado), se sienta, saca su libro del inventario privado y lee o escribe un rato; después lo
 * cierra, lo guarda y sigue con su vida.
 *
 * <p>Todo es visual: el libro es un libro vanilla vacío que nunca sale de su inventario (lo que
 * se ve en sus manitos es solo una imagen) y nadie puede ver lo que "lee" o "escribe".</p>
 *
 * <p>Espera propia de 3 a 6 minutos; un solo lector/escritor activo por zona; comercio, miedo,
 * sueño, descansos y demás actividades lo cortan (vuelve suave a su postura). La antorcha no lo
 * impide: la guarda mientras lee y la vuelve a sacar después.</p>
 *
 * <p>Depuración: con {@code -Dnoxisculture.debugScholars=true} se anota en el registro por qué un
 * lector no empieza (como mucho una vez por minuto por Noxis).</p>
 */
public class NoxisScholarGoal<T extends PathfinderMob & NoxisKeeper> extends Goal {
    private static final double ZONE = 12.0D;
    private static final int SPOT_RADIUS = 6;
    private static final int COOLDOWN = 3600;          // 3 min ...
    private static final int COOLDOWN_RANDOM = 3600;   // ... a 6 min
    private static final int WALK_TIMEOUT = 200;
    private static final double SPEED = 0.35D;

    private enum Phase { WALK, OPEN, MAIN, CLOSE }

    private final T mob;
    private int nextCheckTick;
    private long nextAllowedTime = -1L;
    private @Nullable Vec3 spot;
    private Phase phase = Phase.WALK;
    private boolean writing;
    private int ticks;
    private int mainLength;
    private boolean done;

    public NoxisScholarGoal(T mob) {
        this.mob = mob;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
        this.nextCheckTick = mob.getRandom().nextInt(200);
    }

    private int bookSlot() {
        SimpleContainer inv = this.mob.getPrivateInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            if (inv.getItem(i).is(Items.BOOK)) return i;
        }
        return -1;
    }

    @Override
    public boolean canUse() {
        if (this.mob.tickCount < this.nextCheckTick) return false;
        RandomSource random = this.mob.getRandom();
        this.nextCheckTick = this.mob.tickCount + 100 + random.nextInt(60);
        long now = this.mob.level().getGameTime();
        int freq = com.noxisculture.config.NoxisConfig.get().readingWriting;
        if (!com.noxisculture.config.NoxisConfig.enabled(freq)) return false;
        if (this.nextAllowedTime < 0L) {
            this.nextAllowedTime = now + com.noxisculture.config.NoxisConfig.scaleCooldown(1200 + random.nextInt(2400), freq);   // el primero: 1 a 3 min
        }
        if (now < this.nextAllowedTime || !this.mob.isScholar()) return false;
        if (!this.mob.canStudy()) return this.why("ocupado con otra cosa o no está tranquilo");
        if (!this.mob.onGround() || this.mob.isInWater()) return this.why("no está en suelo firme");
        if (this.bookSlot() < 0) return this.why("no tiene su libro en el inventario");
        if (random.nextInt(2) != 0) return false;
        // Un solo lector/escritor activo por zona.
        boolean busy = !this.mob.level().getEntitiesOfClass(PathfinderMob.class, this.mob.getBoundingBox().inflate(ZONE, 4.0D, ZONE),
                e -> e != this.mob && e instanceof NoxisKeeper k && NoxisKeepsakeAnimation.isBook(k.getKeepsakeAnim())).isEmpty();
        if (busy) return this.why("ya hay otro leyendo cerca");
        this.spot = this.findQuietSpot();
        if (this.spot == null) return this.why("no encontró un lugar tranquilo");
        if (com.noxisculture.entity.custom.NoxisVillager.DEBUG_SCHOLARS) {
            com.noxisculture.NoxisCulture.LOGGER.info("[Noxis lectores] {} va a {}", this.mob.getUUID(), "leer o escribir");
        }
        return true;
    }

    private long lastWhy = -1L;

    /** Depuración: anota por qué no empezó (como mucho una vez por minuto). Siempre devuelve false. */
    private boolean why(String reason) {
        if (com.noxisculture.entity.custom.NoxisVillager.DEBUG_SCHOLARS) {
            long now = this.mob.level().getGameTime();
            if (this.lastWhy < 0L || now - this.lastWhy >= 1200L) {
                this.lastWhy = now;
                com.noxisculture.NoxisCulture.LOGGER.info("[Noxis lectores] {} no empieza: {}", this.mob.getUUID(), reason);
            }
        }
        return false;
    }

    /** Un lugarcito tranquilo cerca: piso firme, sin nadie a menos de 2,5 bloques, al que pueda llegar. */
    private @Nullable Vec3 findQuietSpot() {
        RandomSource random = this.mob.getRandom();
        BlockPos origin = this.mob.blockPosition();
        Vec3 best = null;
        int bestCrowd = Integer.MAX_VALUE;
        for (int i = 0; i < 10; i++) {
            BlockPos pos = origin.offset(random.nextInt(SPOT_RADIUS * 2 + 1) - SPOT_RADIUS, random.nextInt(3) - 1,
                    random.nextInt(SPOT_RADIUS * 2 + 1) - SPOT_RADIUS);
            if (!this.isStandable(pos)) continue;
            Vec3 c = Vec3.atBottomCenterOf(pos);
            int crowd = this.mob.level().getEntitiesOfClass(LivingEntity.class, new AABB(c, c).inflate(2.5D, 1.5D, 2.5D),
                    e -> e != this.mob && e.isAlive()).size();
            if (crowd >= bestCrowd || this.mob.getNavigation().createPath(pos, 0) == null) continue;
            best = c;
            bestCrowd = crowd;
            if (crowd == 0) break;
        }
        if (best == null && this.isQuietHere()) best = this.mob.position();     // ya está en un buen lugar
        return best;
    }

    private boolean isStandable(BlockPos pos) {
        var level = this.mob.level();
        return level.getBlockState(pos).isAir() && level.getBlockState(pos.above()).isAir()
                && level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), net.minecraft.core.Direction.UP)
                && level.getFluidState(pos).isEmpty();
    }

    private boolean isQuietHere() {
        return this.mob.level().getEntitiesOfClass(LivingEntity.class, this.mob.getBoundingBox().inflate(2.5D, 1.5D, 2.5D),
                e -> e != this.mob && e.isAlive()).isEmpty();
    }

    @Override
    public void start() {
        this.phase = Phase.WALK;
        this.ticks = 0;
        this.done = false;
        RandomSource random = this.mob.getRandom();
        this.writing = random.nextInt(5) < 2;                         // 3 de cada 5 veces lee; 2, escribe
        this.mainLength = this.writing ? 300 + random.nextInt(200) : 400 + random.nextInt(200);
    }

    @Override
    public boolean canContinueToUse() {
        if (this.done || this.mob.hurtTime > 0 || !this.mob.canStudy()) return false;
        return this.bookSlot() >= 0;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        this.ticks++;
        switch (this.phase) {
            case WALK -> {
                Vec3 s = this.spot;
                if (s == null || this.mob.position().distanceToSqr(s.x, this.mob.getY(), s.z) < 0.45D * 0.45D
                        || (this.ticks > WALK_TIMEOUT && this.isQuietHere())) {
                    this.enter(Phase.OPEN, this.writing ? NoxisKeepsakeAnimation.WRITE_OPEN : NoxisKeepsakeAnimation.READ_OPEN);
                } else if (this.ticks > WALK_TIMEOUT) {
                    this.done = true;                                   // no encontró su lugarcito: otra vez será
                } else if (this.ticks % 10 == 1 || this.mob.getNavigation().isDone()) {
                    this.mob.getNavigation().moveTo(s.x, s.y, s.z, SPEED);
                }
            }
            case OPEN -> {
                this.mob.getNavigation().stop();
                if (this.ticks == NoxisKeepsakeAnimation.BOOK_SHOW_TICK) {
                    this.mob.showKeepsake(new ItemStack(Items.BOOK));   // solo una imagen: el libro sigue guardado
                    this.mob.playSound(SoundEvents.BOOK_PAGE_TURN, 0.5F, 1.1F);
                }
                if (this.ticks >= NoxisKeepsakeAnimation.BOOK_OPEN_LENGTH) {
                    this.enter(Phase.MAIN, this.writing ? NoxisKeepsakeAnimation.WRITE_MAIN : NoxisKeepsakeAnimation.READ_MAIN);
                }
            }
            case MAIN -> {
                this.mob.getNavigation().stop();
                // Leyendo: da vuelta la página cada 5 renglones (el cliente mueve la patita en ese momento).
                if (!this.writing && this.ticks % NoxisKeepsakeAnimation.PAGE_TICKS == NoxisKeepsakeAnimation.PAGE_TICKS - 8) {
                    this.mob.playSound(SoundEvents.BOOK_PAGE_TURN, 0.4F, 1.0F + this.mob.getRandom().nextFloat() * 0.2F);
                }
                if (this.ticks >= this.mainLength) {
                    this.enter(Phase.CLOSE, this.writing ? NoxisKeepsakeAnimation.WRITE_CLOSE : NoxisKeepsakeAnimation.READ_CLOSE);
                }
            }
            case CLOSE -> {
                this.mob.getNavigation().stop();
                if (this.ticks == 4) this.mob.playSound(SoundEvents.BOOK_PUT, 0.45F, 1.1F);
                if (this.ticks == NoxisKeepsakeAnimation.BOOK_HIDE_TICK) this.mob.showKeepsake(ItemStack.EMPTY);
                if (this.ticks >= NoxisKeepsakeAnimation.BOOK_CLOSE_LENGTH) this.done = true;
            }
        }
    }

    /** Guarda el documento recién escrito (como mucho uno a la vez; si no entra, no se crea). */
    private void keepDocument() {
        SimpleContainer inv = this.mob.getPrivateInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            if (com.noxisculture.document.NoxisDocuments.isDocument(inv.getItem(i))) return;
        }
        int roll = this.mob.getRandom().nextInt(10);
        int kind = roll < 5 ? com.noxisculture.document.NoxisDocuments.NOTEBOOK
                : roll < 8 ? com.noxisculture.document.NoxisDocuments.SHEET : com.noxisculture.document.NoxisDocuments.BOOK;
        inv.addItem(com.noxisculture.document.NoxisDocuments.create(kind, this.mob.getUUID().toString(), this.mob.level().getGameTime()));
    }

    private void enter(Phase next, byte anim) {
        this.phase = next;
        this.ticks = 0;
        this.mob.getNavigation().stop();
        this.mob.setKeepsakeAnim(anim);
    }

    @Override
    public void stop() {
        // Terminó de escribir (no lo interrumpieron): a veces lo que escribió queda como un documento suyo
        // (un cuaderno, una hoja o un libro, con las páginas en blanco: el idioma llega en otra etapa).
        if (this.done && this.writing && this.phase == Phase.CLOSE && this.mob.getRandom().nextFloat() < 0.35F) {
            this.keepDocument();
        }
        this.mob.showKeepsake(ItemStack.EMPTY);
        this.mob.setKeepsakeAnim(NoxisKeepsakeAnimation.NONE);
        this.mob.getNavigation().stop();
        this.spot = null;
        this.nextAllowedTime = this.mob.level().getGameTime() + com.noxisculture.config.NoxisConfig.scaleCooldown(
                COOLDOWN + this.mob.getRandom().nextInt(COOLDOWN_RANDOM), com.noxisculture.config.NoxisConfig.get().readingWriting);
    }
}
