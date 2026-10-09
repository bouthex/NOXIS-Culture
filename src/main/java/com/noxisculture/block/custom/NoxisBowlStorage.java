package com.noxisculture.block.custom;

import com.noxisculture.block.ModBlocks;
import com.noxisculture.entity.ai.NoxisBowlSleeper;
import com.noxisculture.entity.idle.NoxisHatAnimation;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

/**
 * Guarda el Noxis que duerme en una pecera dentro del ítem de la pecera (y lo vuelve a sacar
 * al colocarla), como las abejas en una colmena.
 *
 * <p>Se guardan TODOS sus datos (especie, variante y capa, salud, nivel de comercio, ofertas,
 * nombre, si tiene sombrero...) con el guardado normal de la entidad, en los datos del ítem
 * ({@code minecraft:custom_data}, clave {@value #KEY}). El Noxis original se quita del mundo en
 * el mismo momento en que se guarda, así nunca hay dos. Al volver a colocarlo recibe un
 * identificador nuevo (no se repite aunque se copie el ítem en creativo).</p>
 *
 * <p>Sirve para todas las especies Noxis, actuales y futuras ({@link NoxisBowlSleeper}).</p>
 */
public final class NoxisBowlStorage {
    public static final String KEY = "noxis_culture_sleeper";
    private static final double FLOOR = 2.0D / 16.0D;

    private NoxisBowlStorage() {
    }

    /**
     * Busca el Noxis que duerme en la pecera de {@code pos}; si lo hay, lo guarda en un ítem de
     * pecera nuevo y lo quita del mundo. Devuelve null si no había nadie durmiendo adentro.
     */
    public static @Nullable ItemStack capture(ServerLevel level, BlockPos pos) {
        List<Mob> sleepers = level.getEntitiesOfClass(Mob.class, new AABB(pos).inflate(1.0D),
                e -> e.isAlive() && e instanceof NoxisBowlSleeper s && s.isInBowl() && pos.equals(s.getBowlPos()));
        if (sleepers.isEmpty()) return null;
        Mob mob = sleepers.get(0);
        NoxisBowlSleeper sleeper = (NoxisBowlSleeper) mob;
        // El sombrero queda afuera, donde lo dejó (el jugador lo puede juntar): ya no lo busca ahí.
        sleeper.setHatPos(null);
        sleeper.setHatAnim(NoxisHatAnimation.NONE);
        sleeper.setBowlHop(false);
        sleeper.setBowlPos(null);                     // así al quitarlo no toca el bloque

        TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, level.registryAccess());
        mob.saveWithoutId(output);
        CompoundTag data = output.buildResult();
        data.remove("UUID");                        // al volver tendrá uno nuevo

        CompoundTag sleeperTag = new CompoundTag();
        sleeperTag.putString("id", BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType()).toString());
        sleeperTag.put("data", data);
        CompoundTag root = new CompoundTag();
        root.put(KEY, sleeperTag);

        mob.discard();

        ItemStack stack = new ItemStack(ModBlocks.NOXIS_BOWL);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(root));
        stack.set(DataComponents.ITEM_NAME, Component.translatable("block.noxis_culture.noxis_bowl.occupied"));
        return stack;
    }

    /** Si el ítem trae un Noxis guardado, lo hace aparecer durmiendo dentro de la pecera colocada. */
    public static void release(ServerLevel level, BlockPos pos, ItemStack stack) {
        CustomData custom = stack.get(DataComponents.CUSTOM_DATA);
        if (custom == null) return;
        CompoundTag root = custom.copyTag();
        CompoundTag sleeperTag = root.getCompoundOrEmpty(KEY);
        String id = sleeperTag.getStringOr("id", "");
        if (id.isEmpty()) return;
        Identifier typeId = Identifier.tryParse(id);
        if (typeId == null) return;
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getValue(typeId);
        if (type == null) return;
        Entity entity = type.create(level, EntitySpawnReason.LOAD);
        if (!(entity instanceof Mob mob) || !(entity instanceof NoxisBowlSleeper sleeper)) {
            if (entity != null) entity.discard();
            return;
        }
        mob.load(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(),
                sleeperTag.getCompoundOrEmpty("data")));
        mob.setPos(pos.getX() + 0.5D, pos.getY() + FLOOR, pos.getZ() + 0.5D);
        mob.setYRot(level.getRandom().nextFloat() * 360.0F);
        mob.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
        sleeper.setHatPos(null);
        sleeper.setBowlPos(pos);
        sleeper.setInBowl(true);
        BlockState state = level.getBlockState(pos);
        if (state.is(ModBlocks.NOXIS_BOWL)) {
            level.setBlock(pos, state.setValue(NoxisBowlBlock.OCCUPIED, true), Block.UPDATE_ALL);
        }
        level.addFreshEntity(mob);
    }

    /** ¿Este ítem de pecera trae un Noxis adentro? */
    public static boolean hasSleeper(ItemStack stack) {
        CustomData custom = stack.get(DataComponents.CUSTOM_DATA);
        return custom != null && !custom.copyTag().getCompoundOrEmpty(KEY).isEmpty();
    }
}
