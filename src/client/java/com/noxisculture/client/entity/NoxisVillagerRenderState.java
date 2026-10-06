package com.noxisculture.client.entity;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

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
    /** true mientras se está levantando (para no repetir el rebote del "plop" al revés). */
    public boolean restRising;
}
