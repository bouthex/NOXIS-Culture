package com.noxisculture.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.noxisculture.entity.ModEntityTypes;
import com.noxisculture.entity.custom.NoxisVillager;
import com.noxisculture.entity.personality.NoxisPersonality;
import java.util.Arrays;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.phys.Vec3;

/**
 * Comandos de prueba (requieren permisos de operador, nivel 2, como /summon):
 * <pre>
 * /noxis personality &lt;noxis&gt;                         muestra su personalidad (y si es lector/escritor)
 * /noxis personality &lt;noxis&gt; set &lt;personalidad&gt;     la cambia (se guarda con el Noxis)
 * /noxis summon &lt;personalidad&gt; [posición]             hace aparecer un Noxis con esa personalidad
 * </pre>
 * Personalidades: curious, mischievous, artist, affectionate, calm. No tocan las probabilidades
 * normales de aparición: solo actúan sobre el Noxis indicado o el que crean.
 */
public final class NoxisCommands {
    private NoxisCommands() {}

    private static final DynamicCommandExceptionType UNKNOWN = new DynamicCommandExceptionType(
            id -> Component.translatable("commands.noxis_culture.personality.unknown", id));
    private static final SimpleCommandExceptionType NOT_NOXIS = new SimpleCommandExceptionType(
            Component.translatable("commands.noxis_culture.not_noxis"));
    private static final SimpleCommandExceptionType FAILED = new SimpleCommandExceptionType(
            Component.translatable("commands.noxis_culture.summon.failed"));

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> register(dispatcher));
    }

    private static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("noxis")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("personality")
                        .then(Commands.argument("noxis", EntityArgument.entity())
                                .executes(NoxisCommands::show)
                                .then(Commands.literal("set")
                                        .then(Commands.argument("personality", StringArgumentType.word())
                                                .suggests((c, b) -> SharedSuggestionProvider.suggest(ids(), b))
                                                .executes(NoxisCommands::set)))))
                .then(Commands.literal("summon")
                        .then(Commands.argument("personality", StringArgumentType.word())
                                .suggests((c, b) -> SharedSuggestionProvider.suggest(ids(), b))
                                .executes(c -> summon(c, c.getSource().getPosition()))
                                .then(Commands.argument("pos", Vec3Argument.vec3())
                                        .executes(c -> summon(c, Vec3Argument.getVec3(c, "pos")))))));
    }

    private static String[] ids() {
        return Arrays.stream(NoxisPersonality.values()).map(NoxisPersonality::id).toArray(String[]::new);
    }

    private static NoxisPersonality parse(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        String id = StringArgumentType.getString(c, "personality");
        for (NoxisPersonality p : NoxisPersonality.values()) if (p.id().equals(id)) return p;
        throw UNKNOWN.create(id);
    }

    private static NoxisVillager target(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        Entity e = EntityArgument.getEntity(c, "noxis");
        if (e instanceof NoxisVillager v) return v;
        throw NOT_NOXIS.create();
    }

    public static Component name(NoxisPersonality p) {
        return Component.translatable("noxis_culture.personality." + p.id());
    }

    private static int show(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        NoxisVillager v = target(c);
        NoxisPersonality p = v.getPersonality();
        Component reader = Component.translatable(v.isScholar() ? "commands.noxis_culture.yes" : "commands.noxis_culture.no");
        c.getSource().sendSuccess(() -> Component.translatable("commands.noxis_culture.personality.show",
                v.getDisplayName(), name(p), reader), false);
        return p.ordinal() + 1;
    }

    private static int set(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        NoxisVillager v = target(c);
        NoxisPersonality p = parse(c);
        v.setPersonality(p);
        c.getSource().sendSuccess(() -> Component.translatable("commands.noxis_culture.personality.set",
                v.getDisplayName(), name(p)), true);
        return 1;
    }

    private static int summon(CommandContext<CommandSourceStack> c, Vec3 pos) throws CommandSyntaxException {
        NoxisPersonality p = parse(c);
        ServerLevel level = c.getSource().getLevel();
        NoxisVillager v = ModEntityTypes.NOXIS_VILLAGER.create(level, EntitySpawnReason.COMMAND);
        if (v == null) throw FAILED.create();
        v.snapTo(pos.x, pos.y, pos.z, level.getRandom().nextFloat() * 360.0F, 0.0F);
        v.finalizeSpawn(level, level.getCurrentDifficultyAt(v.blockPosition()), EntitySpawnReason.COMMAND, null);
        v.setPersonality(p);
        if (!level.tryAddFreshEntityWithPassengers(v)) throw FAILED.create();
        c.getSource().sendSuccess(() -> Component.translatable("commands.noxis_culture.summon.success", name(p)), true);
        return 1;
    }
}
