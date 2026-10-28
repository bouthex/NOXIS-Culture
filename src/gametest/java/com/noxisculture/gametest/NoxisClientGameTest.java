package com.noxisculture.gametest;

import com.mojang.blaze3d.platform.InputConstants;
import com.noxisculture.art.NoxisPaintings;
import com.noxisculture.block.ModBlocks;
import com.noxisculture.block.custom.NoxisDocumentBlock;
import com.noxisculture.block.custom.NoxisEaselBlock;
import com.noxisculture.block.custom.NoxisPaintingBlock;
import com.noxisculture.block.entity.NoxisDocumentBlockEntity;
import com.noxisculture.block.entity.NoxisEaselBlockEntity;
import com.noxisculture.client.document.NoxisDocumentScreen;
import com.noxisculture.document.NoxisDocuments;
import com.noxisculture.entity.custom.NoxisVillager;
import com.noxisculture.entity.personality.NoxisPersonality;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.world.TestWorldSave;
import net.minecraft.client.CameraType;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * Pruebas REALES dentro de Minecraft (cliente + mundo integrado) de la etapa de revisión artística:
 * pinturas (pared, identidad, pilas, recarga), atril (apoyar y retirar), documentos (colocar, leer,
 * hojas en capas, recoger), comandos de personalidad, el Noxis artista pintando, y capturas de todo
 * (en build/run/clientGameTest/screenshots). El resultado detallado queda en build/run/clientGameTest/noxis_report.txt.
 */
public class NoxisClientGameTest implements FabricClientGameTest {
    private final List<String> log = new ArrayList<>();
    private int failures;

    private void check(boolean ok, String what) {
        this.log.add((ok ? "OK    " : "FALLA ") + what);
        if (!ok) this.failures++;
    }

    private void note(String what) {
        this.log.add("      " + what);
    }

    private void section(String name, Runnable r) {
        try {
            r.run();
        } catch (Throwable t) {
            this.failures++;
            this.log.add("FALLA " + name + ": excepción " + t);
            t.printStackTrace();
        }
    }

    /** Apunta la cámara al centro de una cara de un bloque (no a su esquina). */
    private static void aim(ClientGameTestContext ctx, BlockPos pos, Direction face) {
        net.minecraft.world.phys.Vec3 t = net.minecraft.world.phys.Vec3.atCenterOf(pos)
                .add(face.getStepX() * 0.49, face.getStepY() * 0.49, face.getStepZ() * 0.49);
        float[] yp = ctx.computeOnClient(mc -> {
            net.minecraft.world.phys.Vec3 e = mc.player.getEyePosition();
            double dx = t.x - e.x, dy = t.y - e.y, dz = t.z - e.z;
            double h = Math.sqrt(dx * dx + dz * dz);
            return new float[]{(float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0), (float) -Math.toDegrees(Math.atan2(dy, h))};
        });
        ctx.getInput().lookAt(yp[0], yp[1]);
        ctx.waitTick();
    }

    private static ServerPlayer player(MinecraftServer s) {
        return s.getPlayerList().getPlayers().get(0);
    }

    private static ServerLevel level(MinecraftServer s) {
        return s.overworld();
    }

    private static final int Y = -60;   // superficie del mundo plano de prueba

    @Override
    public void runTest(ClientGameTestContext ctx) {
        System.setProperty("noxisculture.test.fastArt", "true");
        TestWorldSave save;
        int[] wallArt = new int[3];
        int[] easelArt = new int[1];
        try (TestSingleplayerContext sp = ctx.worldBuilder()
                .adjustSettings(c -> c.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE)).create()) {
            save = sp.getWorldSave();
            var srv = sp.getServer();
            srv.runCommand("gamerule send_command_feedback false");
            ctx.runOnClient(mc -> mc.options.chatVisibility().set(net.minecraft.world.entity.player.ChatVisiblity.HIDDEN));
            srv.runCommand("time set noon");
            srv.runCommand("gamerule advance_time false");
            srv.runCommand("gamerule spawn_mobs false");
            srv.runCommand("weather clear");
            srv.runCommand("gamemode survival @a");
            srv.runCommand("effect give @a minecraft:resistance infinite 10 true");
            srv.runCommand("tp @a 0.5 " + Y + " 0.5 -90 0");
            sp.getConnection().waitForChunksRender();
            ctx.waitTicks(10);

            // ------------------------------------------------ A. pinturas en la pared (clic real)
            section("A. pinturas", () -> {
            srv.runOnServer(s -> {
                ServerLevel l = level(s);
                for (int y = Y; y <= Y + 3; y++) for (int z = -4; z <= 4; z++) l.setBlockAndUpdate(new BlockPos(3, y, z), Blocks.STONE_BRICKS.defaultBlockState());
                player(s).getInventory().setItem(0, NoxisPaintings.blank().copyWithCount(3));
                player(s).getInventory().setSelectedSlot(0);
            });
            ctx.waitTicks(5);
            BlockPos[] walls = {new BlockPos(3, Y + 1, 0), new BlockPos(3, Y + 1, -2), new BlockPos(3, Y + 2, 2)};
            for (int i = 0; i < 3; i++) {
                aim(ctx, walls[i], Direction.WEST);
                ctx.getInput().pressKey(o -> o.keyUse);
                sp.getConnection().waitForServerboundPackets();
                ctx.waitTicks(3);
                final int k = i;
                BlockState st = srv.computeOnServer(s -> level(s).getBlockState(walls[k].west()));
                check(st.is(ModBlocks.NOXIS_PAINTING), "A" + i + " pintura colgada con clic derecho en la pared");
                if (st.is(ModBlocks.NOXIS_PAINTING)) {
                    check(st.getValue(NoxisPaintingBlock.FACING) == Direction.WEST, "A" + i + " orientación correcta (mira al jugador)");
                    wallArt[i] = st.getValue(NoxisPaintingBlock.ART);
                }
            }
            int left = srv.computeOnServer(s -> player(s).getMainHandItem().getCount());
            check(left == 0, "A: en supervivencia se gastan las 3 pinturas (quedan " + left + ")");
            check(wallArt[0] != wallArt[1] && wallArt[1] != wallArt[2] && wallArt[0] != wallArt[2],
                    "A: obras sorteadas distintas entre vecinas: " + wallArt[0] + ", " + wallArt[1] + ", " + wallArt[2]);
            // En el piso no se cuelga.
            srv.runOnServer(s -> player(s).getInventory().setItem(0, NoxisPaintings.blank().copyWithCount(2)));
            ctx.waitTicks(3);
            aim(ctx, new BlockPos(1, Y - 1, 0), Direction.UP);
            ctx.getInput().pressKey(o -> o.keyUse);
            sp.getConnection().waitForServerboundPackets();
            ctx.waitTicks(3);
            boolean floor = srv.computeOnServer(s -> level(s).getBlockState(new BlockPos(1, Y, 0)).is(ModBlocks.NOXIS_PAINTING));
            int count2 = srv.computeOnServer(s -> player(s).getMainHandItem().getCount());
            check(!floor && count2 == 2, "A: en el piso la acción falla y no gasta la pintura");
            check(srv.computeOnServer(s -> !NoxisPaintings.hasArt(player(s).getMainHandItem())), "A: las que quedan en la mano siguen sin descubrir");
            ctx.getInput().lookAt(walls[0]);
            ctx.waitTicks(5);
            ctx.takeScreenshot("A1_pinturas_pared_frente");
            srv.runCommand("tp @a 0.5 " + Y + " -3.5 -60 10");
            ctx.waitTicks(5);
            ctx.takeScreenshot("A2_pinturas_pared_angulo");
            srv.runCommand("tp @a 1.0 " + Y + " 0.5 -90 -12");
            ctx.waitTicks(5);
            ctx.takeScreenshot("A3_pintura_de_cerca");

            // Retirar: cae la MISMA obra; volver a colgarla da la misma obra.
            srv.runOnServer(s -> level(s).destroyBlock(walls[0].west(), true));
            ctx.waitTicks(5);
            int dropped = srv.computeOnServer(s -> {
                List<ItemEntity> items = level(s).getEntitiesOfClass(ItemEntity.class, new AABB(walls[0]).inflate(3));
                for (ItemEntity e : items) if (NoxisPaintings.isPainting(e.getItem())) {
                    int a = NoxisPaintings.artOf(e.getItem());
                    e.discard();
                    return a;
                }
                return -2;
            });
            check(dropped == wallArt[0], "A: al retirarla cae la misma obra (" + dropped + " = " + wallArt[0] + ")");
            srv.runCommand("tp @a 0.5 " + Y + " 0.5 -90 0");
            srv.runOnServer(s -> player(s).getInventory().setItem(0, NoxisPaintings.stack(wallArt[0])));
            ctx.waitTicks(3);
            aim(ctx, walls[0], Direction.WEST);
            ctx.getInput().pressKey(o -> o.keyUse);
            sp.getConnection().waitForServerboundPackets();
            ctx.waitTicks(3);
            int again = srv.computeOnServer(s -> {
                BlockState st = level(s).getBlockState(walls[0].west());
                return st.is(ModBlocks.NOXIS_PAINTING) ? st.getValue(NoxisPaintingBlock.ART) : -1;
            });
            check(again == wallArt[0], "A: colgarla de nuevo muestra la misma obra (" + again + ")");
            // Si se rompe la pared, la pintura cae (no queda flotando).
            srv.runOnServer(s -> level(s).setBlockAndUpdate(walls[2], Blocks.AIR.defaultBlockState()));
            ctx.waitTicks(3);
            boolean floating = srv.computeOnServer(s -> level(s).getBlockState(walls[2].west()).is(ModBlocks.NOXIS_PAINTING));
            int fell = srv.computeOnServer(s -> {
                for (ItemEntity e : level(s).getEntitiesOfClass(ItemEntity.class, new AABB(walls[2]).inflate(3)))
                    if (NoxisPaintings.isPainting(e.getItem())) { int a = NoxisPaintings.artOf(e.getItem()); e.discard(); return a; }
                return -2;
            });
            check(!floating && fell == wallArt[2], "A: sin pared no flota: cae como objeto con su obra (" + fell + ")");
            srv.runOnServer(s -> level(s).setBlockAndUpdate(walls[2], Blocks.STONE_BRICKS.defaultBlockState()));
            });

            // ------------------------------------------------ B. pilas e identidad (reglas de Minecraft)
            section("B. pilas", () -> {
            boolean[] stack = srv.computeOnServer(s -> new boolean[]{
                    ItemStack.isSameItemSameComponents(NoxisPaintings.blank(), NoxisPaintings.blank()),
                    ItemStack.isSameItemSameComponents(NoxisPaintings.stack(5), NoxisPaintings.stack(5)),
                    ItemStack.isSameItemSameComponents(NoxisPaintings.stack(5), NoxisPaintings.stack(6)),
                    ItemStack.isSameItemSameComponents(NoxisPaintings.blank(), NoxisPaintings.stack(5)),
                    NoxisPaintings.blank().getMaxStackSize() == 16});
            check(stack[0], "B: dos pinturas sin descubrir se apilan");
            check(stack[1], "B: dos pinturas de la misma obra se apilan");
            check(!stack[2], "B: obras distintas NO se apilan");
            check(!stack[3], "B: una sin descubrir no se mezcla con una descubierta");
            check(stack[4], "B: pila máxima de 16");
            // Dividir una pila de la misma obra conserva la obra en ambas mitades.
            boolean split = srv.computeOnServer(s -> {
                ItemStack a = NoxisPaintings.stack(7).copyWithCount(10);
                ItemStack b = a.split(4);
                return NoxisPaintings.artOf(a) == 7 && NoxisPaintings.artOf(b) == 7 && a.getCount() == 6 && b.getCount() == 4;
            });
            check(split, "B: dividir una pila conserva la obra de cada mitad");
            });

            // ------------------------------------------------ C. atril
            section("C. atril", () -> {
            srv.runCommand("tp @a 0.5 " + Y + " 0.5 0 20");
            srv.runOnServer(s -> player(s).getInventory().setItem(0, new ItemStack(ModBlocks.NOXIS_EASEL.asItem(), 2)));
            ctx.waitTicks(3);
            aim(ctx, new BlockPos(0, Y - 1, 3), Direction.UP);
            ctx.getInput().pressKey(o -> o.keyUse);
            sp.getConnection().waitForServerboundPackets();
            ctx.waitTicks(3);
            BlockPos easel = new BlockPos(0, Y, 3);
            BlockState es = srv.computeOnServer(s -> level(s).getBlockState(easel));
            check(es.is(ModBlocks.NOXIS_EASEL) && es.getValue(NoxisEaselBlock.FACING) == Direction.NORTH,
                    "C: atril colocado con el frente hacia el jugador");
            srv.runOnServer(s -> player(s).getInventory().setItem(0, NoxisPaintings.blank().copyWithCount(2)));
            ctx.waitTicks(3);
            aim(ctx, easel, Direction.NORTH);
            ctx.getInput().pressKey(o -> o.keyUse);
            sp.getConnection().waitForServerboundPackets();
            ctx.waitTicks(3);
            es = srv.computeOnServer(s -> level(s).getBlockState(easel));
            check(es.is(ModBlocks.NOXIS_EASEL) && es.getValue(NoxisEaselBlock.CANVAS) == NoxisEaselBlock.CANVAS_DONE,
                    "C: clic derecho con una pintura la apoya en el atril");
            easelArt[0] = es.getValue(NoxisEaselBlock.ART);
            check(srv.computeOnServer(s -> player(s).getMainHandItem().getCount()) == 1, "C: se gasta una sola pintura de la pila");
            ctx.waitTicks(5);
            ctx.takeScreenshot("C1_atril_frente");
            srv.runCommand("tp @a 2.5 " + Y + " 2.0 60 15");
            ctx.waitTicks(5);
            ctx.takeScreenshot("C2_atril_costado");
            srv.runCommand("tp @a 0.5 " + Y + " 6.0 180 15");
            ctx.waitTicks(5);
            ctx.takeScreenshot("C3_atril_atras");
            srv.runCommand("tp @a 0.5 " + (Y + 2) + " 1.5 0 55");
            ctx.waitTicks(5);
            ctx.takeScreenshot("C4_atril_arriba");
            // Retirar con la mano vacía (slot 0 vacío): vuelve la misma obra, el atril queda.
            srv.runCommand("tp @a 0.5 " + Y + " 0.5 0 20");
            srv.runOnServer(s -> {
                player(s).getInventory().setItem(1, player(s).getInventory().getItem(0));
                player(s).getInventory().setItem(0, ItemStack.EMPTY);
            });
            ctx.waitTicks(3);
            aim(ctx, easel, Direction.NORTH);
            ctx.getInput().pressKey(o -> o.keyUse);
            sp.getConnection().waitForServerboundPackets();
            ctx.waitTicks(3);
            es = srv.computeOnServer(s -> level(s).getBlockState(easel));
            int got = srv.computeOnServer(s -> {
                var inv = player(s).getInventory();
                for (int i = 0; i < inv.getContainerSize(); i++) if (NoxisPaintings.isPainting(inv.getItem(i)) && NoxisPaintings.hasArt(inv.getItem(i))) return NoxisPaintings.artOf(inv.getItem(i));
                return -1;
            });
            check(es.is(ModBlocks.NOXIS_EASEL) && es.getValue(NoxisEaselBlock.CANVAS) == NoxisEaselBlock.CANVAS_NONE, "C: al retirarla el atril queda colocado");
            check(got == easelArt[0], "C: el jugador recibe exactamente la obra del atril (" + got + " = " + easelArt[0] + ")");
            // Obra en curso de un Noxis: no se puede retirar.
            BlockPos wip = new BlockPos(-2, Y, 3);
            srv.runOnServer(s -> {
                level(s).setBlockAndUpdate(wip, ModBlocks.NOXIS_EASEL.defaultBlockState().setValue(NoxisEaselBlock.CANVAS, NoxisEaselBlock.CANVAS_COLOR).setValue(NoxisEaselBlock.ART, 19));
                if (level(s).getBlockEntity(wip) instanceof NoxisEaselBlockEntity be) be.startWork(UUID.randomUUID(), level(s).getGameTime(), true);
            });
            ctx.waitTicks(3);
            aim(ctx, wip, Direction.NORTH);
            ctx.getInput().pressKey(o -> o.keyUse);
            sp.getConnection().waitForServerboundPackets();
            ctx.waitTicks(3);
            check(srv.computeOnServer(s -> level(s).getBlockState(wip).getValue(NoxisEaselBlock.CANVAS)) == NoxisEaselBlock.CANVAS_COLOR,
                    "C: una obra que un Noxis está pintando no se puede retirar");
            // Etapas del lienzo, una al lado de la otra (para la captura).
            srv.runOnServer(s -> {
                for (int c = 1; c <= 4; c++) level(s).setBlockAndUpdate(new BlockPos(-5 + 2 * c, Y, 7),
                        ModBlocks.NOXIS_EASEL.defaultBlockState().setValue(NoxisEaselBlock.CANVAS, c).setValue(NoxisEaselBlock.ART, 17));
            });
            srv.runCommand("tp @a 0.0 " + Y + " 3.6 0 12");
            ctx.waitTicks(8);
            ctx.takeScreenshot("C5_atril_etapas_blanco_boceto_color_terminado");
            });

            // ------------------------------------------------ D. documentos
            section("D. documentos", () -> {
            srv.runCommand("tp @a -6.5 " + Y + " 0.5 0 35");
            ItemStack[] docs = srv.computeOnServer(s -> new ItemStack[]{
                    NoxisDocuments.create(NoxisDocuments.BOOK, "", 0L), NoxisDocuments.create(NoxisDocuments.NOTEBOOK, "", 0L),
                    NoxisDocuments.create(NoxisDocuments.SHEET, "", 0L), NoxisDocuments.create(NoxisDocuments.SHEET, "", 0L),
                    NoxisDocuments.create(NoxisDocuments.SHEET, "", 0L)});
            BlockPos[] spots = {new BlockPos(-7, Y, 2), new BlockPos(-6, Y, 2), new BlockPos(-5, Y, 2), new BlockPos(-5, Y, 2), new BlockPos(-5, Y, 2)};
            ctx.getInput().holdKey(o -> o.keyShift);
            ctx.waitTicks(3);
            for (int i = 0; i < docs.length; i++) {
                final ItemStack d = docs[i];
                srv.runOnServer(s -> player(s).getInventory().setItem(0, d.copy()));
                ctx.waitTicks(3);
                ctx.getInput().holdKey(o -> o.keyShift);
                ctx.waitTicks(2);
                aim(ctx, spots[i].below(), Direction.UP);
                note("D: agachado antes de usar: " + srv.computeOnServer(s -> player(s).isShiftKeyDown()));
                ctx.getInput().pressKey(o -> o.keyUse);
                sp.getConnection().waitForServerboundPackets();
                ctx.waitTicks(3);
            }
            ctx.getInput().releaseKey(o -> o.keyShift);
            ctx.waitTicks(2);
            check(srv.computeOnServer(s -> level(s).getBlockState(spots[0]).is(ModBlocks.NOXIS_DOCUMENT)), "D: libro apoyado (agachado + clic derecho)");
            check(srv.computeOnServer(s -> level(s).getBlockState(spots[1]).is(ModBlocks.NOXIS_DOCUMENT)), "D: cuaderno apoyado");
            int layers = srv.computeOnServer(s -> {
                BlockState st = level(s).getBlockState(spots[2]);
                return st.is(ModBlocks.NOXIS_DOCUMENT) ? st.getValue(NoxisDocumentBlock.LAYERS) : 0;
            });
            check(layers == 3, "D: tres hojas apiladas en el mismo lugar (capas = " + layers + ")");
            double h = srv.computeOnServer(s -> level(s).getBlockState(spots[2]).getCollisionShape(level(s), spots[2]).max(Direction.Axis.Y));
            check(Math.abs(h - 3 / 16.0) < 1e-6, "D: colisión de la pila a la altura real (" + h * 16 + " px)");
            int stored = srv.computeOnServer(s -> level(s).getBlockEntity(spots[2]) instanceof NoxisDocumentBlockEntity be ? be.count() : -1);
            check(stored == 3, "D: la pila guarda las 3 hojas exactas");
            srv.runCommand("tp @a -6.0 " + (Y + 0.5) + " 0.9 0 50");
            ctx.waitTicks(8);
            ctx.takeScreenshot("D1_documentos_en_el_suelo");
            srv.runCommand("tp @a -3.6 " + Y + " 3.6 120 40");
            ctx.waitTicks(8);
            ctx.takeScreenshot("D2_documentos_costado");
            // Leer el libro con clic derecho (mano vacía).
            srv.runCommand("tp @a -6.5 " + Y + " 0.5 0 35");
            srv.runOnServer(s -> player(s).getInventory().setItem(0, ItemStack.EMPTY));
            ctx.waitTicks(3);
            aim(ctx, spots[0].below(), Direction.UP);
            ctx.getInput().pressKey(o -> o.keyUse);
            ctx.waitTicks(3);
            boolean open = ctx.computeOnClient(mc -> mc.gui.screen() instanceof NoxisDocumentScreen);
            check(open, "D: clic derecho sobre el libro lo abre");
            ctx.takeScreenshot("D3_leer_libro");
            ctx.getInput().pressKey(InputConstants.KEY_ESCAPE);
            ctx.waitTicks(3);
            check(ctx.computeOnClient(mc -> mc.gui.screen() == null), "D: Esc cierra el libro y devuelve el control");
            // Leer cuaderno y hoja desde la mano.
            for (int i = 1; i <= 2; i++) {
                final ItemStack d = docs[i];
                srv.runOnServer(s -> player(s).getInventory().setItem(0, d.copy()));
                ctx.waitTicks(3);
                ctx.getInput().lookAt(0f, -60f);
                ctx.waitTick();
                ctx.getInput().pressKey(o -> o.keyUse);
                ctx.waitTicks(3);
                check(ctx.computeOnClient(mc -> mc.gui.screen() instanceof NoxisDocumentScreen), "D: " + (i == 1 ? "cuaderno" : "hoja") + " se lee desde la mano");
                ctx.takeScreenshot(i == 1 ? "D4_leer_cuaderno" : "D5_leer_hoja");
                ctx.clickScreenButton("gui.done");
                ctx.waitTicks(3);
                check(ctx.computeOnClient(mc -> mc.gui.screen() == null), "D: el botón Listo cierra la pantalla");
            }
            // Recoger la hoja de arriba (agachado, mano vacía): es la última que se apoyó.
            String topId = docs[4].get(net.minecraft.core.component.DataComponents.CUSTOM_DATA).copyTag().getStringOr("doc_id", "?");
            srv.runOnServer(s -> player(s).getInventory().setItem(0, ItemStack.EMPTY));
            ctx.waitTicks(3);
            ctx.getInput().holdKey(o -> o.keyShift);
            aim(ctx, spots[2].below(), Direction.UP);
            ctx.getInput().pressKey(o -> o.keyUse);
            sp.getConnection().waitForServerboundPackets();
            ctx.waitTicks(3);
            ctx.getInput().releaseKey(o -> o.keyShift);
            int layersAfter = srv.computeOnServer(s -> level(s).getBlockState(spots[2]).getValue(NoxisDocumentBlock.LAYERS));
            boolean gotTop = srv.computeOnServer(s -> {
                var inv = player(s).getInventory();
                for (int i = 0; i < inv.getContainerSize(); i++) {
                    ItemStack it = inv.getItem(i);
                    if (NoxisDocuments.isDocument(it) && topId.equals(it.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA).copyTag().getStringOr("doc_id", ""))) return true;
                }
                return false;
            });
            check(layersAfter == 2 && gotTop, "D: agachado se recoge la hoja de arriba (la misma, con su identidad)");
            });

            // ------------------------------------------------ E. objetos en la mano (1ª y 3ª persona), inventario y suelo
            section("E. objetos", () -> {
            srv.runCommand("tp @a 0.5 " + Y + " -6.5 180 0");
            ItemStack[] held = srv.computeOnServer(s -> new ItemStack[]{NoxisPaintings.blank(), new ItemStack(ModBlocks.NOXIS_EASEL.asItem()),
                    NoxisDocuments.create(0, "", 0L), NoxisDocuments.create(1, "", 0L), NoxisDocuments.create(2, "", 0L)});
            String[] names = {"pintura", "atril", "libro", "cuaderno", "hoja"};
            for (int i = 0; i < held.length; i++) {
                final ItemStack it = held[i];
                srv.runOnServer(s -> {
                    player(s).getInventory().setItem(0, it.copy());
                    player(s).setItemInHand(InteractionHand.OFF_HAND, it.copy());
                });
                ctx.waitTicks(6);
                ctx.runOnClient(mc -> mc.options.setCameraType(CameraType.FIRST_PERSON));
                ctx.waitTicks(2);
                ctx.takeScreenshot("E" + i + "a_mano_1ra_" + names[i]);
                ctx.runOnClient(mc -> mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT));
                ctx.waitTicks(3);
                ctx.takeScreenshot("E" + i + "b_mano_3ra_" + names[i]);
                ctx.runOnClient(mc -> mc.options.setCameraType(CameraType.FIRST_PERSON));
            }
            srv.runOnServer(s -> {
                var inv = player(s).getInventory();
                for (int i = 0; i < held.length; i++) inv.setItem(i, held[i].copy());
                inv.setItem(5, NoxisPaintings.stack(3).copyWithCount(4));
                player(s).setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
            });
            ctx.waitTicks(3);
            ctx.setScreen(() -> new InventoryScreen(ctx.computeOnClient(mc -> mc.player)));
            ctx.waitTicks(3);
            ctx.takeScreenshot("E9_inventario");
            ctx.setScreen(() -> null);
            srv.runOnServer(s -> {
                for (int i = 0; i < held.length; i++) {
                    ItemEntity e = new ItemEntity(level(s), 0.5 + i * 0.8 - 1.6, Y + 0.2, -8.5, held[i].copy());
                    e.setDeltaMovement(0, 0, 0);
                    e.setNeverPickUp();
                    level(s).addFreshEntity(e);
                }
            });
            ctx.waitTicks(25);
            srv.runCommand("tp @a 0.5 " + (Y + 0.6) + " -6.6 180 35");
            ctx.waitTicks(5);
            ctx.takeScreenshot("E10_objetos_en_el_suelo");
            });

            // ------------------------------------------------ F. comandos de personalidad
            section("F. comandos", () -> {
            srv.runCommand("tp @a 12.5 " + Y + " 0.5 -90 10");
            srv.runCommand("noxis summon artist 16.5 " + Y + " 0.5");
            ctx.waitTicks(5);
            NoxisPersonality p = srv.computeOnServer(s -> {
                List<NoxisVillager> v = level(s).getEntitiesOfClass(NoxisVillager.class, new AABB(16.5, Y, 0.5, 16.5, Y, 0.5).inflate(2));
                return v.isEmpty() ? null : v.get(0).getPersonality();
            });
            check(p == NoxisPersonality.ARTIST, "F: /noxis summon artist crea un Noxis Artista (" + p + ")");
            srv.runCommand("noxis personality @e[type=noxis_culture:noxis_villager,limit=1,sort=nearest] set calm");
            ctx.waitTicks(2);
            NoxisPersonality p2 = srv.computeOnServer(s -> level(s).getEntitiesOfClass(NoxisVillager.class, new AABB(16.5, Y, 0.5, 16.5, Y, 0.5).inflate(3)).get(0).getPersonality());
            check(p2 == NoxisPersonality.CALM, "F: /noxis personality ... set calm la cambia (" + p2 + ")");
            srv.runCommand("noxis personality @e[type=noxis_culture:noxis_villager,limit=1,sort=nearest] set artist");
            ctx.waitTicks(2);
            });

            // ------------------------------------------------ G. el Noxis artista pinta en un atril real
            section("G. el Noxis", () -> {
            srv.runCommand("tp @a 4.5 " + (Y + 1) + " -12.5 -45 20");
            UUID artist = srv.computeOnServer(s -> level(s).getEntitiesOfClass(NoxisVillager.class, new AABB(16.5, Y, 0.5, 16.5, Y, 0.5).inflate(3)).get(0).getUUID());
            int waited = 0;
            BlockPos found = null;
            while (waited < 4000 && found == null) {
                ctx.waitTicks(100);
                waited += 100;
                if (waited % 1000 == 0) note("G: " + waited + " ticks, libre para pintar: " + srv.computeOnServer(s -> {
                    var vs = level(s).getEntitiesOfClass(NoxisVillager.class, new AABB(16.5, Y, 0.5, 16.5, Y, 0.5).inflate(20));
                    return vs.isEmpty() ? "sin artista" : vs.get(0).canDoAmbient() + " pos " + vs.get(0).blockPosition().toShortString();
                }));
                found = srv.computeOnServer(s -> {
                    ServerLevel l = level(s);
                    for (BlockPos q : BlockPos.betweenClosed(new BlockPos(4, Y, -14), new BlockPos(30, Y + 2, 14))) {
                        if (l.getBlockState(q).is(ModBlocks.NOXIS_EASEL) && l.getBlockEntity(q) instanceof NoxisEaselBlockEntity be && artist.equals(be.getOwner())) return q.immutable();
                    }
                    return null;
                });
            }
            check(found != null, "G: el Artista arma su atril y empieza una obra (esperado " + waited + " ticks)");
            int[] artArt = {-1};
            if (found != null) {
                final BlockPos fe = found;
                artArt[0] = srv.computeOnServer(s -> level(s).getBlockState(fe).getValue(NoxisEaselBlock.ART));
                srv.runCommand("tp @a " + (fe.getX() + 3.5) + " " + Y + " " + (fe.getZ() + 3.5) + " 135 15");
                ctx.waitTicks(60);
                ctx.takeScreenshot("G1_artista_pintando");
                int prog = 0;
                int w2 = 0;
                while (w2 < 6000) {
                    ctx.waitTicks(100);
                    w2 += 100;
                    prog = srv.computeOnServer(s -> level(s).getBlockEntity(fe) instanceof NoxisEaselBlockEntity be ? be.getProgress() : -1);
                    BlockState fs = srv.computeOnServer(s -> level(s).getBlockState(fe));
                    if (!fs.is(ModBlocks.NOXIS_EASEL) || fs.getValue(NoxisEaselBlock.CANVAS) == NoxisEaselBlock.CANVAS_DONE) break;
                    if (w2 == 1000) ctx.takeScreenshot("G2_obra_en_curso");
                }
                BlockState fs = srv.computeOnServer(s -> level(s).getBlockState(fe));
                note("G: tras " + (waited + w2) + " ticks: atril=" + fs + " avance=" + prog);
                final int theArt = artArt[0];
                int copies = srv.computeOnServer(s -> {
                    int n = 0;
                    for (NoxisVillager v : level(s).getEntitiesOfClass(NoxisVillager.class, new AABB(fe).inflate(40))) {
                        var inv = v.getPrivateInventory();
                        for (int i = 0; i < inv.getContainerSize(); i++)
                            if (NoxisPaintings.isPainting(inv.getItem(i)) && NoxisPaintings.artOf(inv.getItem(i)) == theArt) n += inv.getItem(i).getCount();
                    }
                    for (BlockPos q : BlockPos.betweenClosed(fe.offset(-20, -2, -20), fe.offset(20, 3, 20))) {
                        BlockState bs = level(s).getBlockState(q);
                        if (bs.is(ModBlocks.NOXIS_PAINTING) && bs.getValue(NoxisPaintingBlock.ART) == theArt && q.getX() > 4) n++;
                        if (bs.is(ModBlocks.NOXIS_EASEL) && bs.getValue(NoxisEaselBlock.CANVAS) > 0 && bs.getValue(NoxisEaselBlock.ART) == theArt && q.getX() > 4) n++;
                    }
                    return n;
                });
                note("G: obra " + theArt + ": copias en el mundo (zona del artista) + inventarios = " + copies);
                check(copies == 1, "G: la obra existe una sola vez (" + copies + ")");
                ctx.takeScreenshot("G3_artista_despues");
            }
            });
        }

        // ------------------------------------------------ H. recargar el mundo
        try (TestSingleplayerContext sp = save.open()) {
            sp.getConnection().waitForChunksRender();
            var srv = sp.getServer();
            int[] after = srv.computeOnServer(s -> {
                int[] r = new int[2];
                BlockState a = level(s).getBlockState(new BlockPos(2, Y + 1, 0));
                r[0] = a.is(ModBlocks.NOXIS_PAINTING) ? a.getValue(NoxisPaintingBlock.ART) : -1;
                r[1] = -1;
                var inv = player(s).getInventory();
                for (int i = 0; i < inv.getContainerSize(); i++) if (NoxisPaintings.isPainting(inv.getItem(i)) && inv.getItem(i).getCount() == 4) r[1] = NoxisPaintings.artOf(inv.getItem(i));
                return r;
            });
            check(after[0] == wallArt[0], "H: tras recargar, la pintura de la pared conserva su obra (" + after[0] + ")");
            check(after[1] == 3, "H: tras recargar, la pila de 4 pinturas de la obra 3 conserva su obra (" + after[1] + ")");
            int sheetsAfter = srv.computeOnServer(s -> level(s).getBlockEntity(new BlockPos(-5, Y, 2)) instanceof NoxisDocumentBlockEntity be ? be.count() : -1);
            check(sheetsAfter == 2, "H: tras recargar, la pila de hojas guarda sus 2 hojas (" + sheetsAfter + ")");
            int wipAfter = srv.computeOnServer(s -> level(s).getBlockEntity(new BlockPos(-2, Y, 3)) instanceof NoxisEaselBlockEntity be && be.getOwner() != null ? 1 : 0);
            check(wipAfter == 1, "H: tras recargar, el atril recuerda la obra en curso y su dueño");
        }

        try {
            Path out = Path.of("noxis_report.txt");
            Files.write(out, this.log);
            System.out.println("[NoxisTest] Informe en " + out.toAbsolutePath());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        for (String l : this.log) System.out.println("[NoxisTest] " + l);
        if (this.failures > 0) throw new AssertionError(this.failures + " comprobaciones fallaron (ver noxis_report.txt)");
    }
}
