package com.noxisculture.client.document;

import com.noxisculture.NoxisCulture;
import com.noxisculture.document.NoxisDocuments;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.PageButton;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

/**
 * Pantalla de lectura de un documento Noxis: un libro abierto (dos páginas), una hoja de cuaderno o
 * una hoja suelta, con el aspecto de cada uno. Por ahora las páginas están EN BLANCO a propósito: el
 * idioma de los Noxis llega en una etapa futura (ver {@code NoxisDocuments}). No muestra texto
 * inventado, letras ni símbolos.
 *
 * <p>Se cierra con "Listo" o con Esc, como cualquier pantalla de Minecraft; no toma el control del
 * jugador ni cambia nada en el mundo (solo mira).</p>
 */
public class NoxisDocumentScreen extends Screen {
    private static final Identifier TEXTURE = NoxisCulture.id("textures/gui/noxis_document.png");
    /** Zonas de la textura (256x256): libro abierto arriba; hoja de cuaderno y hoja suelta abajo. */
    private static final int BOOK_W = 232, BOOK_H = 152;
    private static final int NOTE_U = 0, NOTE_V = 152, NOTE_W = 116, NOTE_H = 104;
    private static final int SHEET_U = 120, SHEET_V = 152, SHEET_W = 96, SHEET_H = 104;

    private final int kind;
    private final int spreads;
    private int spread;
    private PageButton forward;
    private PageButton back;

    public NoxisDocumentScreen(ItemStack document) {
        super(document.getHoverName());
        this.kind = NoxisDocuments.kindOf(document);
        int pages = NoxisDocuments.pageCount(document);
        this.spreads = this.kind == NoxisDocuments.BOOK ? Math.max(1, (pages + 1) / 2) : Math.max(1, pages);
    }

    public static void open(ItemStack document) {
        Minecraft mc = Minecraft.getInstance();
        mc.gui.setScreen(new NoxisDocumentScreen(document));
    }

    private int w() {
        return switch (this.kind) {
            case NoxisDocuments.NOTEBOOK -> NOTE_W;
            case NoxisDocuments.SHEET -> SHEET_W;
            default -> BOOK_W;
        };
    }

    private int h() {
        return switch (this.kind) {
            case NoxisDocuments.NOTEBOOK -> NOTE_H;
            case NoxisDocuments.SHEET -> SHEET_H;
            default -> BOOK_H;
        };
    }

    /** Las hojas chicas se dibujan al doble para que se lean como un objeto en primer plano. */
    private int scale() {
        return this.kind == NoxisDocuments.BOOK ? 1 : (this.height >= 2 * this.h() + 40 ? 2 : 1);
    }

    private int left() {
        return (this.width - this.w() * this.scale()) / 2;
    }

    private int top() {
        return Math.max(4, (this.height - this.h() * this.scale() - 28) / 2);
    }

    @Override
    protected void init() {
        int bottom = this.top() + this.h() * this.scale();
        this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> this.onClose())
                .pos((this.width - 200) / 2, Math.min(this.height - 24, bottom + 6)).width(200).build());
        int l = this.left();
        int r = l + this.w() * this.scale();
        this.back = this.addRenderableWidget(new PageButton(l + 8, bottom - 18, false, b -> this.turn(-1), true));
        this.forward = this.addRenderableWidget(new PageButton(r - 31, bottom - 18, true, b -> this.turn(1), true));
        this.updateButtons();
    }

    private void turn(int delta) {
        this.spread = Math.max(0, Math.min(this.spreads - 1, this.spread + delta));
        this.updateButtons();
    }

    private void updateButtons() {
        this.back.visible = this.spread > 0;
        this.forward.visible = this.spread < this.spreads - 1;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractBackground(graphics, mouseX, mouseY, a);
        int u = this.kind == NoxisDocuments.NOTEBOOK ? NOTE_U : this.kind == NoxisDocuments.SHEET ? SHEET_U : 0;
        int v = this.kind == NoxisDocuments.BOOK ? 0 : NOTE_V;
        int s = this.scale();
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, this.left(), this.top(), u, v,
                this.w() * s, this.h() * s, this.w(), this.h(), 256, 256);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
