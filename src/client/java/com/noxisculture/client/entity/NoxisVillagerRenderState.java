package com.noxisculture.client.entity;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;

/** Datos visuales copiados de la entidad en cada frame. */
public class NoxisVillagerRenderState extends LivingEntityRenderState {
    /** 0..1: cuánto de "feliz" mostrar (transición suave). */
    public float happyAnim;
    /** 0..1: cuánto de "miedo" mostrar (transición suave). */
    public float scaredAnim;
    /** 0..1: cuánto levantó la antorcha (animación de sacarla). */
    public float torchAnim;
    /** 0..1: cuánto está sentado descansando. */
    public float restAnim;
    /** 0..1: cuánto sacó el paraguas (lluvia). */
    public float umbrellaAnim;
    /** Variante de ropa (0 = sin capa, 1..3 = capa con su estilo). */
    public byte variant;
    /** Física de la capa: movimiento (0..1) y giro con retraso (-1..1). */
    public float capeSwing;
    public float capeTurn;
    /** Curiosidad: intensidad (0..1), lado (+1/-1) y golpecito de oreja (0..1). */
    public float curious;
    public float curiousSide = 1.0F;
    public float curiousFlick;
    /** Fascinación por los cristales: pose (0..1), brillo extra de ojos, bracitos, saltito, orejas y lado. */
    public float crystalAmount;
    public float crystalGlow;
    public float crystalCheer;
    public float crystalHop;
    public float crystalTwitch;
    public float crystalSide = 1.0F;
    /** Naturaleza: mirar alrededor, agacharse, sostener/acercar la flor, olfatear, ojitos, orejas, ladeo, pupilas, sentarse. */
    public float natureSurvey;
    public float natureLean;
    public float natureHold;
    public float natureRaise;
    public float natureSniff;
    public float natureEyesClosed;
    public float natureEars;
    public float natureTilt;
    public float naturePupils;
    public float natureSit;
    /** Regalo: bracito estirado ofreciendo la flor. */
    public float natureOffer;
    public float natureSide = 1.0F;
    /** Saludo y descanso en compañía: ladeo, orejas, patita, asentir, saltito, cabeceo, bostezo, apoyo. */
    public float socialTilt;
    public float socialSide = 1.0F;
    public float socialEars;
    public float socialWave;
    public float socialNod;
    public float socialHop;
    public float socialDoze;
    public float socialYawn;
    public float socialLean;
    /** La flor que tiene en la mano (vacía si no tiene nada). */
    public final ItemStackRenderState heldFlower = new ItemStackRenderState();
    /** true mientras se está levantando (para no repetir el rebote del "plop" al revés). */
    public boolean restRising;
}
