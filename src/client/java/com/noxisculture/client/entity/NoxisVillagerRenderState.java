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
    /** Duerme dentro de una pecera / tiene puesto el sombrero. */
    public boolean inBowl;
    public boolean hasHat = true;
    /** Colores del sombrero que lleva puesto (RGB): copa/ala y lazo. */
    public int hatCrownColor = com.noxisculture.item.NoxisHatColors.DEFAULT_CROWN;
    public int hatBandColor = com.noxisculture.item.NoxisHatColors.DEFAULT_BAND;
    /** Durmiendo en una pecera tapada por otra (se acurruca un poquito más). */
    public boolean bowlCovered;
    /** Animación de sacarse/ponerse el sombrero (0 = ninguna) y su tiempo en ticks. */
    public byte hatAnim;
    public float hatAnimTime;
    /** Saltito de la pecera (0 = ninguno) y su tiempo en ticks. */
    public byte bowlHop;
    public float bowlHopTime;
    /** Aburrimiento: gesto (0 = ninguno), si usa el lado izquierdo, su tiempo y cuánto se ve (0..1). */
    public byte boredType;
    public boolean boredMirrored;
    public float boredTime;
    public float boredWeight;
    /** Ojitos del aburrimiento: cerrados (satisfecho / parpadeo), pupilas grandes, entrecerrados. */
    public boolean boredEyesClosed;
    public boolean boredPupils;
    public boolean boredTired;
    /** Acomodarse / estirarse: gesto (byte completo), tiempo, cuánto se ve, ojitos, despierto (0..1) y globito. */
    public byte restGesture;
    public float restGestTime;
    public float restGestWeight;
    public int restEyes;
    public float restAwake;
    public float bubbleRest = -1.0F;
    /** Cariño con su amigo: gesto (papel + lado), tiempo, cuánto se ve, ojitos cerrados y pupilas grandes. */
    public byte affection;
    public float affectionTime;
    public float affectionWeight;
    public boolean affectionEyesClosed;
    public boolean affectionPupils;
    /** Curiosidad / travesura: peso y tiempo de cada estado (índice = NoxisPlayAnimation) y ojitos. */
    public final float[] playWeight = new float[com.noxisculture.entity.idle.NoxisPlayAnimation.COUNT];
    public final float[] playTime = new float[com.noxisculture.entity.idle.NoxisPlayAnimation.COUNT];
    public boolean playSly;
    public boolean playPupils;
    public boolean playLaugh;
    /** Melodía espontánea: estado (NoxisHumAnimation), cuánto se ve y la hora del mundo (para el compás). */
    public byte hum;
    public float humWeight;
    public float humBeatTime;
    public boolean humEyesHalf;
    /** Silbando: se ve la boquita en "o". */
    public boolean whistleMouth;
    /** Sentado mirando un animal (0-1). */
    public float watchSit;
    /** Gesto de la personalidad: cuál, tiempo, cuánto se ve y ojitos. */
    public byte persona;
    public float personaTime;
    public float personaWeight;
    public boolean personaPupils;
    public boolean personaHalf;
    public boolean personaClosed;
    /** Objetos: gesto (mirar / contemplar), tiempo, cuánto se ve, ojitos y si tiene el tesoro en las manitos. */
    public byte keepsake;
    public float keepsakeTime;
    public float keepsakeWeight;
    public boolean keepsakePupils;
    public boolean keepsakeClosed;
    public boolean keepsakeHolding;
    /** Leyendo / escribiendo: cuánto está sentado (usa la postura de sentarse de siempre) y ojitos concentrados. */
    public float scholarSit;
    /** Libro abierto 3D en las manitos / lápiz en la mano (lectores y escritores). */
    public boolean bookShown;
    public boolean pencilShown;
    public boolean scholarFocused;
    /** Reacción a sonidos: peso y tiempo de cada etapa (índice = NoxisListenAnimation), dirección y ojitos. */
    public final float[] listenWeight = new float[4];
    public final float[] listenTime = new float[4];
    public float listenDir;
    public float listenSide = 1.0F;
    public boolean listenPupils;
    public boolean listenClosed;
    public boolean listenNoBlink;
    /** Baile: cuánto se ve el baile y la caída, en qué tiempo de la canción va, estilo y ojitos. */
    public float danceWeight;
    public float danceFallWeight;
    public float danceFallTime;
    public float danceBeatRaw;
    public float danceBeatTicks = 10.0F;
    public byte danceStyle;
    public byte dancePrevStyle;
    public float danceStyleBlend = 1.0F;
    public float dancePairFallWeight;
    public float dancePairFallTime;
    public int dancePairRole;
    public float danceMirror = 1.0F;
    public int danceSeed;
    public boolean danceClosed;
    public boolean danceHalf;
    public boolean danceBig;
    /** Resonancia con el disco amarillo: cuánto se ve (0..1) y el pulso de la canción (en tiempos). */
    public float resonance;
    public float resonanceBeat;
    /** La flor que tiene en la mano (vacía si no tiene nada). */
    public final ItemStackRenderState heldFlower = new ItemStackRenderState();
    /** true mientras se está levantando (para no repetir el rebote del "plop" al revés). */
    public boolean restRising;
}
