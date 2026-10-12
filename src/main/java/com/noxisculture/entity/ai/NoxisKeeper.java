package com.noxisculture.entity.ai;

import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Noxis con inventario privado y objetos favoritos (todas las especies, actuales y futuras).
 * El inventario es interno: ningún jugador puede abrirlo; se guarda con el Noxis.
 */
public interface NoxisKeeper extends NoxisCurious {
    /** Su inventario privado (chiquito). */
    SimpleContainer getPrivateInventory();

    /** ¿Este tipo de objeto se lo regalaron alguna vez? (lo recuerda con cariño). */
    boolean isGiftedItem(Item item);

    /** ¿Lo aprecia? (flores, cristales, Noxus, regalos). Lo demás no lo guarda. */
    boolean appreciates(ItemStack stack);

    /** ¿Está libre para mirar objetos o contemplar los suyos? (tranquilo, sin otra actividad). */
    boolean canHandleKeepsakes();

    /** Como {@link #canHandleKeepsakes()}, pero admite su propia fascinación (mirando un cristal). */
    boolean canHandleKeepsakesWhileFascinated();

    /** Gesto de objetos en curso ({@code NoxisKeepsakeAnimation}, sincronizado). */
    void setKeepsakeAnim(byte anim);

    byte getKeepsakeAnim();

    /** Lo que se ve en sus manitos mientras contempla (solo una imagen: el objeto sigue guardado). */
    void showKeepsake(ItemStack stack);

    /** Reacción de fascinación (la de los cristales) mientras mira algo cristalino. */
    void setFascinated(boolean fascinated);
}
