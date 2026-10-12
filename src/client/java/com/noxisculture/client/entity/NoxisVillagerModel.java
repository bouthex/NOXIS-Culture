package com.noxisculture.client.entity;

import com.noxisculture.entity.idle.NoxisAffectionAnimation;
import com.noxisculture.entity.idle.NoxisBoredAnimation;
import com.noxisculture.entity.idle.NoxisBowlHop;
import com.noxisculture.entity.idle.NoxisRestGesture;
import com.noxisculture.entity.idle.NoxisHatAnimation;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartNames;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

/**
 * Modelo v3 del Noxis (estilo Ribbits + gato negro):
 * cabeza enorme 10x8x8, cuerpito 6x5x5, patitas de 2 px, orejitas que asoman
 * por el ala, colita enrulada, sombrero de copa ALTO (12 px) con una ramita,
 * y un poncho de hojas/musgo con la gema como broche.
 *
 * Jerarquía pensada para las variantes futuras:
 *  - "hat" es un grupo propio: guerreros/sabios podrán cambiar o quitar el sombrero.
 *  - los brazos tienen pivote en el hombro: el sabio sostendrá su bastón 3D con right_arm.
 * Textura 64x64 -> textures/entity/noxis_villager.png
 */
public class NoxisVillagerModel extends EntityModel<NoxisVillagerRenderState> {
    public static final String HAT = "hat";
    private static final String HAT_BRIM = "hat_brim";
    private static final String HAT_CROWN = "hat_crown";
    private static final String HAT_BAND = "hat_band";
    private static final String TAIL = "tail";
    private static final String TAIL_TIP = "tail_tip";
    private static final String HAT_SPRIG = "hat_sprig";
    /** La gema del lazo: nunca se tiñe (se dibuja aparte, con sus colores originales). */
    private static final String HAT_GEM = "hat_gem";
    private static final String PONCHO = "poncho";
    private static final String TORCH = "torch";
    private static final String EYES_TIRED = "eyes_tired";
    private static final String EYES_CLOSED = "eyes_closed";
    private static final String EYES_OPEN = "eyes_open";
    /** Ojos con pupilas agrandadas (contemplando una flor): armados píxel a píxel con píxeles que ya existen. */
    private static final String EYES_BIG = "eyes_big";
    /** Punto invisible en la manito donde se dibuja la flor. */
    private static final String FLOWER_ANCHOR = "flower_anchor";

    /** Qué dibuja esta copia del modelo: el Noxis normal o solo los ojitos (brillo de la fascinación). */
    public static final int PASS_NORMAL = 0;
    public static final int PASS_GLOW_EYES = 1;
    /** Copia que solo dibuja el globito de sueño (con su propia textura). */
    public static final int PASS_SLEEP_BUBBLE = 2;
    /**
     * Copias que solo dibujan el sombrero, con la textura-máscara {@code noxis_hat_mask.png}:
     * copa y ala (se tiñen con su color), lazo (con el suyo) y lo fijo (ramita y gema, sin teñir).
     */
    public static final int PASS_HAT_CROWN = 3;
    public static final int PASS_HAT_BAND = 4;
    public static final int PASS_HAT_FIXED = 5;
    /** Globito de sueño (solo lo dibuja la copia {@link #PASS_SLEEP_BUBBLE}). */
    private static final String SLEEP_BUBBLE = "sleep_bubble";
    /** Mechoncito de pelo verde (solo se ve cuando no tiene sombrero). */
    private static final String HAIR_TUFT = "hair_tuft";
    private static final String UMBRELLA = "umbrella";
    private static final String UMBRELLA_CANOPY = "umbrella_canopy";
    private static final float HAT_Y = -8.0F;
    private static final float TAIL_BASE_ANGLE = 0.9F;

    private static final float HEAD_Y = 17.0F;
    private static final float HAT_BASE_TILT = 0.08F;
    private static final float EAR_BASE_ANGLE = 0.22F;

    private final ModelPart head;
    private final ModelPart body;
    private final ModelPart hat;
    private final ModelPart hatBrim;
    private final ModelPart hatCrown;
    private final ModelPart hatBand;
    private final ModelPart hatSprig;
    private final ModelPart hatGem;
    private final ModelPart rightEar;
    private final ModelPart leftEar;
    private final ModelPart tail;
    private final ModelPart rightArm;
    private final ModelPart torch;
    private final ModelPart eyesTired;
    private final ModelPart eyesClosed;
    private final ModelPart eyesOpen;
    private final ModelPart umbrella;
    private final ModelPart umbrellaCanopy;
    private boolean restRising;
    private final ModelPart leftArm;
    private final ModelPart rightLeg;
    private final ModelPart leftLeg;

    private final int pass;
    private final ModelPart eyesBig;
    private final ModelPart sleepBubble;
    private final ModelPart hairTuft;
    private final ModelPart flowerAnchor;

    public NoxisVillagerModel(ModelPart root) {
        this(root, PASS_NORMAL);
    }

    public NoxisVillagerModel(ModelPart root, int pass) {
        super(root);
        this.pass = pass;
        this.head = root.getChild(PartNames.HEAD);
        this.body = root.getChild(PartNames.BODY);
        this.hat = this.head.getChild(HAT);
        this.hatBrim = this.hat.getChild(HAT_BRIM);
        this.hatCrown = this.hat.getChild(HAT_CROWN);
        this.hatBand = this.hat.getChild(HAT_BAND);
        this.hatSprig = this.hat.getChild(HAT_SPRIG);
        this.hatGem = this.hat.getChild(HAT_GEM);
        this.rightEar = this.head.getChild(PartNames.RIGHT_EAR);
        this.leftEar = this.head.getChild(PartNames.LEFT_EAR);
        this.eyesTired = this.head.getChild(EYES_TIRED);
        this.eyesClosed = this.head.getChild(EYES_CLOSED);
        this.eyesOpen = this.head.getChild(EYES_OPEN);
        this.eyesBig = this.head.getChild(EYES_BIG);
        this.sleepBubble = this.head.getChild(SLEEP_BUBBLE);
        this.hairTuft = this.head.getChild(HAIR_TUFT);
        this.flowerAnchor = root.getChild(FLOWER_ANCHOR);
        this.umbrella = root.getChild(UMBRELLA);
        this.umbrellaCanopy = this.umbrella.getChild(UMBRELLA_CANOPY);
        this.tail = root.getChild(PartNames.BODY).getChild(TAIL);
        this.rightArm = root.getChild(PartNames.RIGHT_ARM);
        this.torch = root.getChild(TORCH);
        this.leftArm = root.getChild(PartNames.LEFT_ARM);
        this.rightLeg = root.getChild(PartNames.RIGHT_LEG);
        this.leftLeg = root.getChild(PartNames.LEFT_LEG);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // ---- Cabeza ----
        PartDefinition head = root.addOrReplaceChild(PartNames.HEAD,
                CubeListBuilder.create().texOffs(0, 0).addBox(-5.0F, -8.0F, -4.0F, 10.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, HEAD_Y, 0.0F));

        // Orejitas: asoman por encima del ala, a los costados de la copa (forma triangular por textura).
        head.addOrReplaceChild(PartNames.RIGHT_EAR,
                CubeListBuilder.create().texOffs(0, 28).addBox(-1.5F, -3.0F, -0.5F, 3.0F, 3.0F, 1.0F),
                PartPose.offsetAndRotation(-4.5F, -9.0F, 0.0F, 0.0F, 0.0F, -EAR_BASE_ANGLE));
        // Expresiones: capas finitas pegadas a la cara (filas 1-4), dibujadas píxel por píxel.
        // Se muestran/ocultan según el estado. Tapan también el brillo de los ojos de abajo.
        head.addOrReplaceChild(EYES_TIRED,
                CubeListBuilder.create().texOffs(0, 59).addBox(-5.0F, -7.0F, -4.05F, 10.0F, 4.0F, 0.0F),
                PartPose.ZERO);
        // Ojos normales: también en capa propia, a la MISMA profundidad que las otras
        // expresiones. Debajo la cara es solo piel, así de costado nunca se asoma el amarillo.
        head.addOrReplaceChild(EYES_OPEN,
                CubeListBuilder.create().texOffs(40, 59).addBox(-5.0F, -7.0F, -4.05F, 10.0F, 4.0F, 0.0F),
                PartPose.ZERO);
        head.addOrReplaceChild(EYES_CLOSED,
                CubeListBuilder.create().texOffs(20, 59).addBox(-5.0F, -7.0F, -4.05F, 10.0F, 4.0F, 0.0F),
                PartPose.ZERO);
        // Ojos con pupilas agrandadas (al contemplar una flor). Reemplazan a los ojos normales
        // mientras dura la contemplación: están en EL MISMO plano que ellos (z = -4.05), armados con
        // cuadraditos de 1x1 que no se superponen entre sí, así que desde cualquier ángulo quedan
        // pegados a la cara, sin separarse ni titilar. Cada píxel sale de la textura que ya existe
        // (pupila, párpado, brillito y amarillo del propio ojo) y su cara de atrás cae en un píxel
        // transparente. La pupila de siempre queda en su lugar (mirada bizca) y suma un píxel.
        head.addOrReplaceChild(EYES_BIG, bigEyes(), PartPose.ZERO);
        // Globito de sueño: un cubito de 4 px que sale de la naricita, a un costado de la carita.
        // Usa su propia textura (noxis_sleep_bubble.png, en u=64 v=64): cada cara es un círculo
        // celeste con brillito, así desde cualquier ángulo se ve redondito.
        // Mechoncito verde (sin sombrero): tallito con un rulito, con píxeles verdes que ya existen.
        head.addOrReplaceChild(HAIR_TUFT,
                CubeListBuilder.create()
                        .texOffs(27, 30).addBox(-0.5F, -10.0F, -0.5F, 1.0F, 2.0F, 1.0F)
                        .texOffs(28, 31).addBox(0.5F, -10.8F, -0.5F, 1.0F, 1.0F, 1.0F)
                        .texOffs(30, 31).addBox(-1.5F, -9.0F, -0.5F, 1.0F, 1.0F, 1.0F),
                PartPose.ZERO);
        head.addOrReplaceChild(SLEEP_BUBBLE,
                CubeListBuilder.create().texOffs(64, 64).addBox(-2.0F, -2.0F, -4.0F, 4.0F, 4.0F, 4.0F),
                PartPose.offset(1.2F, -2.2F, -4.1F));
        head.addOrReplaceChild(PartNames.LEFT_EAR,
                CubeListBuilder.create().texOffs(0, 28).mirror().addBox(-1.5F, -3.0F, -0.5F, 3.0F, 3.0F, 1.0F),
                PartPose.offsetAndRotation(4.5F, -9.0F, 0.0F, 0.0F, 0.0F, EAR_BASE_ANGLE));

        // ---- Sombrero de copa (grupo propio, apoyado sobre la cabeza y levemente inclinado) ----
        PartDefinition hat = head.addOrReplaceChild(HAT, CubeListBuilder.create(),
                PartPose.offsetAndRotation(0.0F, HAT_Y, 0.0F, -0.05F, 0.0F, HAT_BASE_TILT));
        hat.addOrReplaceChild(HAT_BRIM,
                CubeListBuilder.create().texOffs(0, 46).addBox(-6.0F, -1.0F, -6.0F, 12.0F, 1.0F, 12.0F),
                PartPose.ZERO);
        hat.addOrReplaceChild(HAT_CROWN,
                CubeListBuilder.create().texOffs(36, 0).addBox(-3.5F, -13.0F, -3.5F, 7.0F, 12.0F, 7.0F),
                PartPose.ZERO);
        // Cinta inflada: sobresale apenas de la copa y da relieve real en 3D.
        hat.addOrReplaceChild(HAT_BAND,
                CubeListBuilder.create().texOffs(36, 19)
                        .addBox(-3.5F, -3.0F, -3.5F, 7.0F, 2.0F, 7.0F, new CubeDeformation(0.25F)),
                PartPose.ZERO);
        // Ramita con hoja metida en la cinta (toque "naturaleza").
        hat.addOrReplaceChild(HAT_SPRIG,
                CubeListBuilder.create().texOffs(48, 30).addBox(0.0F, -4.0F, -1.0F, 1.0F, 4.0F, 2.0F),
                PartPose.offsetAndRotation(3.2F, -2.0F, 1.0F, 0.0F, 0.0F, 0.35F));
        // Gema del lazo (al frente, en el centro): se dibuja sin teñir.
        hat.addOrReplaceChild(HAT_GEM,
                CubeListBuilder.create().texOffs(62, 62).addBox(-0.5F, -3.0F, -3.8F, 1.0F, 1.0F, 0.0F),
                PartPose.ZERO);

        // ---- Cuerpo + colita ----
        PartDefinition body = root.addOrReplaceChild(PartNames.BODY,
                CubeListBuilder.create().texOffs(0, 16).addBox(-3.0F, 0.0F, -2.5F, 6.0F, 5.0F, 5.0F),
                PartPose.offset(0.0F, HEAD_Y, 0.0F));
        // Poncho de hojas: apenas más grande que el cuerpo, borde inferior en zigzag (por textura).
        body.addOrReplaceChild(PONCHO,
                CubeListBuilder.create().texOffs(20, 30).addBox(-3.5F, -0.2F, -3.0F, 7.0F, 3.0F, 6.0F),
                PartPose.ZERO);
        // ===== Capa (variante): pequeña capa de espalda con el emblema del Nixil =====
        NoxisCape.addTo(body);
        PartDefinition tail = body.addOrReplaceChild(TAIL,
                CubeListBuilder.create().texOffs(0, 34).addBox(-0.5F, -0.5F, 0.0F, 1.0F, 1.0F, 4.0F),
                PartPose.offsetAndRotation(0.0F, 3.5F, 2.5F, TAIL_BASE_ANGLE, 0.0F, 0.0F));
        tail.addOrReplaceChild(TAIL_TIP,
                CubeListBuilder.create().texOffs(12, 34).addBox(-0.5F, -0.5F, 0.0F, 1.0F, 1.0F, 2.0F),
                PartPose.offsetAndRotation(0.0F, 0.0F, 4.0F, 0.9F, 0.0F, 0.0F));

        // ---- Bracitos y patitas ----
        root.addOrReplaceChild(PartNames.RIGHT_ARM,
                CubeListBuilder.create().texOffs(22, 16).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 3.0F, 2.0F),
                PartPose.offset(-4.0F, 17.5F, 0.0F));
        // Antorcha: pieza propia que se ubica en la mano en cada cuadro y SIEMPRE apunta hacia arriba
        // (como un farol sostenido a un costado), así nunca queda tapada por la cabeza o el sombrero.
        root.addOrReplaceChild(TORCH,
                CubeListBuilder.create()
                        .texOffs(56, 30).addBox(-0.5F, -6.0F, -0.5F, 1.0F, 6.0F, 1.0F)
                        .texOffs(56, 38).addBox(-1.0F, -8.0F, -1.0F, 2.0F, 2.0F, 2.0F),
                PartPose.offset(-6.0F, 17.0F, -1.5F));
        root.addOrReplaceChild(PartNames.LEFT_ARM,
                CubeListBuilder.create().texOffs(22, 16).mirror().addBox(-1.0F, 0.0F, -1.0F, 2.0F, 3.0F, 2.0F),
                PartPose.offset(4.0F, 17.5F, 0.0F));
        root.addOrReplaceChild(PartNames.RIGHT_LEG,
                CubeListBuilder.create().texOffs(22, 22).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 2.0F, 2.0F),
                PartPose.offset(-1.5F, 22.0F, 0.0F));
        root.addOrReplaceChild(PartNames.LEFT_LEG,
                CubeListBuilder.create().texOffs(22, 22).mirror().addBox(-1.0F, 0.0F, -1.0F, 2.0F, 2.0F, 2.0F),
                PartPose.offset(1.5F, 22.0F, 0.0F));

        // ---- Paraguas (pieza propia: se ubica en la mano izquierda en cada cuadro) ----
        // Mango con ganchito, varilla larga y una cúpula escalonada de 20 px: bien por encima
        // del sombrero de copa para que nunca choquen. Gema en la punta (brilla).
        PartDefinition umbrella = root.addOrReplaceChild(UMBRELLA, CubeListBuilder.create()
                        .texOffs(110, 64).addBox(-0.5F, -25.0F, -0.5F, 1.0F, 26.0F, 1.0F)     // varilla
                        .texOffs(116, 64).addBox(-0.5F, 1.0F, -0.5F, 1.0F, 2.0F, 1.0F)        // mango
                        .texOffs(116, 70).addBox(-0.5F, 2.0F, 0.5F, 1.0F, 1.0F, 2.0F),        // ganchito
                PartPose.offset(7.0F, 17.5F, -1.0F));
        umbrella.addOrReplaceChild(UMBRELLA_CANOPY, CubeListBuilder.create()
                        .texOffs(0, 64).addBox(-10.0F, -25.0F, -10.0F, 20.0F, 1.0F, 20.0F)    // borde
                        .texOffs(0, 86).addBox(-7.5F, -27.0F, -7.5F, 15.0F, 2.0F, 15.0F)      // cúpula media
                        .texOffs(64, 86).addBox(-4.5F, -29.0F, -4.5F, 9.0F, 2.0F, 9.0F)       // cúpula alta
                        .texOffs(82, 64).addBox(-2.0F, -30.0F, -2.0F, 4.0F, 1.0F, 4.0F)       // tapa
                        .texOffs(100, 64).addBox(-1.0F, -32.0F, -1.0F, 2.0F, 2.0F, 2.0F),     // gema
                PartPose.ZERO);

        // Punto (sin cubos) donde se dibuja la flor en la mano; se mueve con la manito.
        root.addOrReplaceChild(FLOWER_ANCHOR, CubeListBuilder.create(), PartPose.ZERO);

        return LayerDefinition.create(mesh, 128, 128);
    }

    /** Fila de píxeles de los ojos grandes: (u, v) de la textura de cada tipo de píxel. */
    private static final int[] LID = {43, 60};
    private static final int[] DARK = {48, 61};
    private static final int[] SHINE = {43, 61};
    private static final int[] YELLOW = {43, 62};

    private static CubeListBuilder bigEyes() {
        // x de cada columna (ojo derecho: -4..-2, ojo izquierdo: 1..3) y filas y = -6, -5, -4.
        int[][][] right = {
                {LID, LID, LID},
                {DARK, DARK, SHINE},
                {DARK, DARK, YELLOW}};
        int[][][] left = {
                {LID, LID, LID},
                {SHINE, DARK, DARK},
                {YELLOW, DARK, DARK}};
        CubeListBuilder builder = CubeListBuilder.create();
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                int[] r = right[row][col];
                builder.texOffs(r[0], r[1]).addBox(-4.0F + col, -6.0F + row, -4.05F, 1.0F, 1.0F, 0.0F);
                int[] l = left[row][col];
                builder.texOffs(l[0], l[1]).addBox(1.0F + col, -6.0F + row, -4.05F, 1.0F, 1.0F, 0.0F);
            }
        }
        return builder;
    }

    /**
     * Tamaño del globito de sueño. Dormido: respira lento (se infla y desinfla). Al despertarse
     * (levantándose): se infla un poquito de golpe y ¡plop!, desaparece. Despierto: nada.
     */
    public static float sleepBubbleScale(float rest, boolean rising, float age) {
        if (rest < 0.6F) return 0.0F;
        if (rising) {
            float p = (1.0F - rest) / 0.4F;                  // 0 recién despierto .. 1 desaparece
            return p < 0.7F ? 0.75F + 0.3F * p / 0.7F : 0.0F;
        }
        float settle = Mth.clamp((rest - 0.6F) / 0.4F, 0.0F, 1.0F);
        float breath = 0.5F + 0.5F * Mth.sin(age * 0.11F);
        return settle * (0.3F + 0.45F * breath);
    }

    /** Curva con pequeño sobrepaso: hace que el "plop" al sentarse rebote un poquito. */
    private static float easeOutBack(float x) {
        float c1 = 1.70158F;
        float c3 = c1 + 1.0F;
        float k = x - 1.0F;
        return 1.0F + c3 * k * k * k + c1 * k * k;
    }

    /** Dónde se dibuja la flor (para {@link NoxisHeldFlowerLayer}). */
    public ModelPart flowerAnchor() {
        return this.flowerAnchor;
    }

    @Override
    public void setupAnim(NoxisVillagerRenderState state) {
        this.restRising = state.restRising;
        super.setupAnim(state);
        float age = state.ageInTicks;
        float pos = state.walkAnimationPos;
        float amount = state.walkAnimationSpeed;
        float happy = state.happyAnim;
        float scared = state.scaredAnim;
        float torchAnim = state.torchAnim;
        float rest = state.restAnim;
        // Sentarse: el descanso o la contemplación de la naturaleza usan la misma postura
        // (la carita de sueño, las orejas caídas y el sombrero son SOLO del descanso).
        float sit = Math.max(rest, state.natureSit);

        // ---- Bamboleo cartoon al caminar ----
        float[] waddle = NoxisMoodAnimator.waddle(pos, amount, happy);
        float roll = waddle[0];
        float bounce = waddle[1];
        float hop = Math.abs(Mth.sin(age * 0.3F)) * 0.6F * happy * (1.0F - amount);
        // Sentarse en dos tiempos: primero dobla las patitas, después "plop" con un rebotito.
        float legPhase = Mth.clamp(sit * 1.6F, 0.0F, 1.0F);
        float plop = easeOutBack(Mth.clamp((sit - 0.2F) / 0.8F, 0.0F, 1.0F));
        float sitDrop = 2.0F * plop;

        this.body.zRot = roll;
        this.body.xRot = 0.12F * torchAnim - 0.08F * sit; // se inclina para ver / se recuesta un poquito
        this.body.y = HEAD_Y - bounce - hop + sitDrop;

        // ---- Cabeza ----
        float[] life = NoxisMoodAnimator.headLife(age, pos, amount, happy);
        int t = (int) age;
        boolean dozing = (t % 160) < 22;                            // cabeceo de sueño de vez en cuando
        // Al despertarse estirándose, la cabeza deja la postura de sueño (awake = 0 durmiendo).
        float sleepLook = rest * (1.0F - state.restAwake);
        float doze = sleepLook * (Mth.sin(age * 0.05F) * 0.06F + (dozing ? 0.22F : 0.0F));
        this.head.yRot = state.yRot * Mth.DEG_TO_RAD + 0.3F * torchAnim;   // mira hacia la luz
        this.head.xRot = state.xRot * Mth.DEG_TO_RAD + life[0] + 0.25F * scared + 0.1F * torchAnim
                + 0.15F * sleepLook + doze;
        // Con la antorcha ladea la cabecita (como esforzándose por ver); cansado, la deja caer de lado.
        this.head.zRot = roll * 1.4F + life[1] + NoxisMoodAnimator.headTremble(age, scared)
                + 0.2F * torchAnim + 0.12F * sleepLook;
        this.head.x = -roll * 3.0F;
        this.head.z = -0.9F * torchAnim;                             // estira el cuello para ver mejor
        this.head.y = HEAD_Y - bounce - hop + 0.8F * scared + sitDrop;

        // ---- Piezas compartidas por todas las especies ----
        NoxisMoodAnimator.animateEars(this.rightEar, this.leftEar, age, happy, scared, EAR_BASE_ANGLE);
        NoxisMoodAnimator.animateHat(this.hat, HAT_Y, HAT_BASE_TILT, age, pos, amount, happy, scared);
        NoxisMoodAnimator.animateTail(this.tail, TAIL_BASE_ANGLE, age, happy, scared);
        // Cansado: orejitas caídas, sombrero que se resbala sobre los ojos, cola enroscada en el piso.
        float plopBounce = Mth.sin(Mth.clamp((rest - 0.2F) / 0.8F, 0.0F, 1.0F) * Mth.PI) * (this.restRising ? 0.0F : 1.0F);
        this.rightEar.zRot -= 0.55F * sleepLook - 0.35F * plopBounce;
        this.leftEar.zRot += 0.55F * sleepLook - 0.35F * plopBounce;
        this.hat.xRot = -0.05F + 0.2F * sleepLook;
        this.tail.xRot = Mth.lerp(sit, this.tail.xRot, -0.15F);
        this.tail.yRot = Mth.lerp(sit, this.tail.yRot, 1.25F);
        // Curiosidad repentina (gesto compartido; solo aparece estando quieto y tranquilo).
        NoxisMoodAnimator.applyCuriosity(this.head, this.rightEar, this.leftEar,
                state.curious, state.curiousSide, state.curiousFlick);
        // Fascinación por un cristal: ladea la cabeza y para las orejitas (mirarlo lo hace el servidor).
        NoxisMoodAnimator.applyFascination(this.head, this.rightEar, this.leftEar,
                state.crystalAmount, state.crystalSide, state.crystalTwitch, state.crystalHop, age);
        // Saludo y descanso en compañía (a quién mira lo decide el servidor).
        NoxisMoodAnimator.applySocialHead(this.head, this.body, this.rightEar, this.leftEar, age,
                state.socialTilt, state.socialSide, state.socialEars, state.socialNod, state.socialDoze,
                state.socialYawn, state.socialLean, sit);
        // Naturaleza: mirar flores, agacharse, contemplar y olfatear (mirarlas lo hace el servidor).
        NoxisMoodAnimator.applyNature(this.head, this.body, this.rightEar, this.leftEar, age,
                state.natureSurvey, state.natureLean, state.natureHold, state.natureRaise, state.natureSniff,
                state.natureEars, state.natureTilt, state.natureSide);

        // ---- Párpados (expresión) ----
        // esfuerzo por ver con la antorcha: entrecierra; cansado: casi cierra y parpadea lento.
        // ---- Expresión de los ojos ----
        //  · Antorcha de noche: ojitos cansados pero vigilando (2 px amarillos + pupila bizca).
        //  · Sentado: los mismos ojitos de sueño, que se cierran al cabecear.
        //  · Siempre: parpadeo normal cada tanto; cansado, parpadeo LENTO con cabeceo.
        boolean sleepy = torchAnim > 0.5F || rest > 0.5F || state.boredTired;
        boolean slowBlink = sleepy && (t % 110) < 8;
        boolean normalBlink = (t % 83) < 3;
        // Fascinado con un cristal no parpadea: lo mira con los ojos bien abiertos.
        if (state.crystalAmount > 0.5F) normalBlink = false;
        boolean closed = slowBlink || (rest > 0.5F && dozing) || (!sleepy && normalBlink)
                || state.natureEyesClosed > 0.5F
                || state.socialYawn > 0.5F || Math.abs(state.socialLean) > 0.5F     // bostezo / dormidos juntos
                || (state.socialDoze > 0.6F && (t % 30) < 12)
                || state.boredEyesClosed || state.affectionEyesClosed;                     // cabeceo de sueño                    // disfrutando el aroma
        // Ojitos al acomodarse / despertarse (abre despacito, bosteza con los ojos cerrados).
        if (state.restEyes == NoxisRestGesture.EYES_CLOSED) {
            closed = true;
        } else if (state.restEyes == NoxisRestGesture.EYES_TIRED) {
            closed = false;
            sleepy = true;
        } else if (state.restEyes == NoxisRestGesture.EYES_OPEN) {
            sleepy = torchAnim > 0.5F;
            closed = normalBlink && !sleepy;
        }
        this.eyesClosed.visible = closed;
        this.eyesTired.visible = sleepy && !closed;
        boolean open = !closed && !sleepy;
        // Pupilas agrandadas, fascinado con la flor: los ojos grandes reemplazan a los normales.
        // También al fascinarse con un cristal (mismas pupilas grandes, mismo plano que el ojo).
        boolean bigPupils = open && (state.naturePupils > 0.5F || state.crystalAmount > 0.35F || state.boredPupils || state.affectionPupils);
        this.eyesOpen.visible = open && !bigPupils;
        this.eyesBig.visible = bigPupils;
        // Con el parpadeo lento, la cabecita se le cae un poquito (cabeceo de sueño).
        if (slowBlink) {
            this.head.xRot += 0.12F * torchAnim;
        }

        // ---- Patitas: pasitos, o estiradas hacia adelante al sentarse ----
        // Sentado: patitas hacia adelante, un poquito para arriba y abiertas en V, asomando
        // delante de la pancita (se mueven hacia adelante para que el cuerpo no las tape).
        float kick = Mth.sin(age * 0.15F) * 0.08F * sit;   // las balancea suavecito
        this.rightLeg.xRot = Mth.lerp(legPhase, Mth.cos(pos * 1.2F) * 1.1F * amount, -1.65F + kick);
        this.leftLeg.xRot = Mth.lerp(legPhase, Mth.cos(pos * 1.2F + Mth.PI) * 1.1F * amount, -1.65F - kick);
        this.rightLeg.yRot = 0.35F * legPhase;
        this.leftLeg.yRot = -0.35F * legPhase;
        this.rightLeg.x = -1.5F - 0.8F * legPhase;
        this.leftLeg.x = 1.5F + 0.8F * legPhase;
        this.rightLeg.z = -2.2F * legPhase;
        this.leftLeg.z = -2.2F * legPhase;
        this.rightLeg.y = 22.0F + 1.0F * plop;
        this.leftLeg.y = 22.0F + 1.0F * plop;

        // ---- Brazos ----
        float armY = 17.5F - bounce - hop + sitDrop;
        this.rightArm.y = armY;
        this.leftArm.y = armY;
        this.rightArm.x = -4.0F - roll * 2.5F;
        this.leftArm.x = 4.0F - roll * 2.5F;
        this.rightArm.z = 0.0F;
        float wave = Mth.sin(age * 0.5F) * 0.3F * happy;
        float rightX = Mth.cos(pos * 1.2F + Mth.PI) * 0.9F * amount - 1.3F * scared;
        float leftX = Mth.cos(pos * 1.2F) * 0.9F * amount - 1.3F * scared;
        float rightZ = 0.15F + roll + (0.9F + wave) * happy - 0.5F * scared;
        float leftZ = -0.15F + roll - (0.9F - wave) * happy + 0.5F * scared;
        // Sentado: manitos apoyadas sobre la pancita.
        float armsPhase = sit * sit; // las manitos llegan a la pancita al final
        rightX = Mth.lerp(armsPhase, rightX, -0.55F);
        leftX = Mth.lerp(armsPhase, leftX, -0.55F);
        rightZ = Mth.lerp(armsPhase, rightZ, -0.15F);
        leftZ = Mth.lerp(armsPhase, leftZ, 0.15F);
        // Antorcha: brazo derecho ESTIRADO al costado y adelante, apenas arriba del hombro
        // (como sosteniendo un farol para ver), y el izquierdo adelante, con cuidado.
        float sway = Mth.sin(age * 0.12F) * 0.05F + Mth.cos(pos * 1.2F) * 0.08F * amount;
        rightX = Mth.lerp(torchAnim, rightX, -0.6F + sway);
        rightZ = Mth.lerp(torchAnim, rightZ, 1.9F + roll);
        leftX = Mth.lerp(torchAnim, leftX, -0.5F + Mth.cos(pos * 1.2F) * 0.3F * amount);
        leftZ = Mth.lerp(torchAnim, leftZ, -0.25F);
        // Paraguas: brazo izquierdo estirado al costado (apenas adelante), sosteniéndolo firme.
        float umb = state.umbrellaAnim;
        float umbSway = Mth.sin(age * 0.1F) * 0.04F + Mth.cos(pos * 1.2F) * 0.06F * amount;
        leftX = Mth.lerp(umb, leftX, -0.3F + umbSway);
        leftZ = Mth.lerp(umb, leftZ, -1.62F + roll);   // brazo apenas más abierto y un pelín más alto
        this.rightArm.xRot = rightX;
        this.rightArm.zRot = rightZ;
        this.leftArm.xRot = leftX;
        this.leftArm.zRot = leftZ;

        // ---- Paraguas en la mano izquierda ----
        this.umbrella.visible = umb > 0.02F;
        if (this.umbrella.visible) {
            float yy = 3.0F * Mth.cos(leftX);
            float zz = 3.0F * Mth.sin(leftX);
            // La manito agarra la varilla por su lado de afuera; con la antorcha (cabeza hacia
            // adelante y ladeada hacia este lado) se separa un poco más para no rozar la cabeza.
            float apart = (0.6F + 0.8F * torchAnim) * umb;
            this.umbrella.x = this.leftArm.x - yy * Mth.sin(leftZ) + apart;
            this.umbrella.y = this.leftArm.y + yy * Mth.cos(leftZ);
            this.umbrella.z = this.leftArm.z + zz;
            // Apenas inclinado hacia la cabeza para cubrirla, sin tocar el ala del sombrero.
            this.umbrella.zRot = -0.08F + 0.07F * torchAnim + roll * 0.4F; // más derecho si la cabeza se inclina
            this.umbrella.xRot = -0.05F + umbSway;
            // La cúpula gira despacito sobre la varilla (detalle tierno).
            this.umbrellaCanopy.yRot = Mth.sin(age * 0.04F) * 0.35F;
            float scale = Mth.clamp(umb * 1.15F, 0.0F, 1.0F);
            this.umbrella.xScale = scale;
            this.umbrella.yScale = scale;
            this.umbrella.zScale = scale;
        }
        // Con lluvia, las orejitas van hacia atrás (a los gatos no les gusta mojarse).
        this.rightEar.xRot += 0.5F * umb;
        this.leftEar.xRot += 0.5F * umb;

        // ---- Antorcha en la mano ----
        this.torch.visible = torchAnim > 0.02F;
        if (this.torch.visible) {
            // Punta del brazo (mano) = pivote del hombro + rotación ZYX del brazo aplicada a (0, 3, 0).
            float armLen = 3.0F;
            float yy = armLen * Mth.cos(rightX);
            float zz = armLen * Mth.sin(rightX);
            float xr = -yy * Mth.sin(rightZ);
            float yr = yy * Mth.cos(rightZ);
            this.torch.x = this.rightArm.x + xr;
            this.torch.y = this.rightArm.y + yr;
            this.torch.z = this.rightArm.z + zz;
            this.torch.zRot = -0.2F + roll * 0.5F;          // la punta se inclina hacia afuera
            this.torch.xRot = -0.15F + sway;                 // y un poquito hacia adelante, iluminando el camino
            float scale = Mth.clamp(torchAnim * 1.15F, 0.0F, 1.0F);
            this.torch.xScale = scale;
            this.torch.yScale = scale;
            this.torch.zScale = scale;
        }

        // ---- Fascinación: festejo con bracitos arriba, colita contenta y saltitos cartoon ----
        NoxisMoodAnimator.applyCheer(this.rightArm, this.leftArm, this.tail, state.crystalCheer, age);
        // ---- Naturaleza: la flor en la manito ----
        NoxisMoodAnimator.applyFlowerArms(this.rightArm, this.leftArm, state.natureLean, state.natureHold,
                state.natureRaise, state.natureOffer);
        {
            // Punta de la mano (igual que la antorcha) y, un poquito más adelante, la flor.
            float ax = this.rightArm.xRot;
            float az = this.rightArm.zRot;
            float yy = 3.0F * Mth.cos(ax);
            float zz = 3.0F * Mth.sin(ax);
            float raise = state.natureRaise;
            this.flowerAnchor.x = this.rightArm.x - yy * Mth.sin(az) + 1.6F * raise;
            this.flowerAnchor.y = this.rightArm.y + yy * Mth.cos(az) + 1.8F * raise;   // pétalos a la altura de la naricita
            this.flowerAnchor.z = this.rightArm.z + zz - 1.3F - 1.4F * raise;
            this.flowerAnchor.zRot = -0.25F * (1.0F - raise);
            this.flowerAnchor.xRot = 0.12F * raise;
        }
        NoxisMoodAnimator.applySocialArms(this.rightArm, this.leftArm, age, state.socialWave, state.socialYawn, sit);
        float hopState = state.crystalHop + state.socialHop;
        if (hopState > 0.0F) {
            // En el aire: todo el Noxis sube (las patitas también, un poquito recogidas).
            float jump = 2.6F * hopState;
            this.head.y -= jump;
            this.body.y -= jump;
            this.rightArm.y -= jump;
            this.leftArm.y -= jump;
            this.rightLeg.y -= jump;
            this.leftLeg.y -= jump;
            this.rightLeg.xRot -= 0.35F * hopState;
            this.leftLeg.xRot += 0.35F * hopState;
        } else if (hopState < 0.0F) {
            // Agachadita antes de saltar y "aplastón" al aterrizar: el cuerpo baja, las patitas quedan.
            float squash = -2.2F * hopState;
            this.head.y += squash;
            this.body.y += squash * 0.8F;
            this.rightArm.y += squash * 0.8F;
            this.leftArm.y += squash * 0.8F;
        }

        // ---- Aburrimiento: pataditas, acomodarse el sombrero o jugar con sus patitas ----
        this.rightArm.xScale = 1.0F;
        this.rightArm.yScale = 1.0F;
        this.rightArm.zScale = 1.0F;
        this.leftArm.xScale = 1.0F;
        this.leftArm.yScale = 1.0F;
        this.leftArm.zScale = 1.0F;
        this.leftArm.z = 0.0F;
        if (state.boredType != NoxisBoredAnimation.NONE && state.boredWeight > 0.0F) {
            this.applyBoredom(state.boredType, state.boredMirrored, state.boredTime, state.boredWeight);
        }
        // ---- Cariño con su amigo especial (coreografía compartida por los dos) ----
        if (state.affection != NoxisAffectionAnimation.NONE && state.affectionWeight > 0.0F) {
            this.applyAffection(state.affection, state.affectionTime, state.affectionWeight);
        }

        // ---- Variante de ropa: capa de espalda (componente reutilizable por todas las especies Noxis) ----
        int capeStyle = state.variant - 1;          // 0 = sin capa; 1..3 = estilos de capa
        boolean hasCape = capeStyle >= 0;
        NoxisCape.animate(this.body, capeStyle, state.capeSwing, state.capeTurn, age, sit);
        // ---- Durmiendo en la pecera: bien apretadito para no tocar el vidrio ----
        if (state.inBowl) {
            if (hasCape) {
                // La parte de abajo de la capa queda acostada en el piso de la pecera.
                this.body.getChild(NoxisCape.capeName(capeStyle)).getChild(NoxisCape.CAPE_LOWER).xRot = 1.6F;
            }
            // Orejitas plegadas hacia atrás, como gatito acurrucado.
            this.rightEar.zRot += 0.35F;
            this.leftEar.zRot -= 0.35F;
            this.rightEar.xRot += 1.25F;
            this.leftEar.xRot += 1.25F;
            // Cabecita quieta, apenas inclinada (sin cabeceos que la acerquen al vidrio).
            this.head.yRot = 0.0F;
            this.head.zRot = 0.0F;
            this.head.xRot = 0.15F + 0.03F * Mth.sin(age * 0.05F);
            this.head.x = 0.0F;
            this.head.z = 0.0F;
            if (state.bowlCovered) {
                // Tapada: baja la cabecita y pliega más las orejitas (no toca la base de arriba).
                this.head.y += 2.0F;
                this.rightEar.xRot += 0.3F;
                this.leftEar.xRot += 0.3F;
            }
        }
        // ---- Sacarse / ponerse el sombrero (gesto compartido por todas las especies) ----
        if (state.hatAnim != NoxisHatAnimation.NONE) {
            this.applyHatAnimation(state.hatAnim, state.hatAnimTime);
        }
        // ---- Saltito para entrar / salir de la pecera ----
        this.setSqueeze(1.0F, 1.0F);
        if (state.bowlHop != NoxisBowlHop.NONE) {
            this.applyBowlHop(state.bowlHop, state.bowlHopTime);
        }
        // ---- Acomodarse para descansar / estirarse al despertar ----
        if (state.restGesture != NoxisRestGesture.NONE && state.restGestWeight > 0.0F) {
            this.applyRestGesture(state.restGesture, state.restGestTime, state.restGestWeight, state.inBowl);
        }
        // ---- Sombrero o mechoncito ----
        this.hat.visible = state.hasHat;
        this.hairTuft.visible = !state.hasHat;
        if (!state.hasHat) {
            // Sin el ala del sombrero, las orejitas bajan un píxel y quedan pegadas a la cabeza.
            this.rightEar.y += 1.0F;
            this.leftEar.y += 1.0F;
        }
        if (hasCape) {
            // Sentado con capa: la colita sale derecha por su ojal (no hacia el costado).
            this.tail.yRot = Mth.lerp(sit, this.tail.yRot, 0.0F);
        }

        // ---- Globito de sueño: se infla y desinfla con la respiración mientras duerme ----
        float bubble = sleepBubbleScale(state.bubbleRest >= 0.0F ? state.bubbleRest : rest, this.restRising, age);
        this.sleepBubble.visible = this.pass == PASS_SLEEP_BUBBLE && bubble > 0.02F;
        this.sleepBubble.xScale = bubble;
        this.sleepBubble.yScale = bubble;
        this.sleepBubble.zScale = state.inBowl ? bubble * 0.25F : bubble;   // en la pecera: casi plano
        if (this.pass == PASS_SLEEP_BUBBLE) {
            // Solo el globito: los ojitos tampoco se dibujan en esta pasada.
            this.eyesOpen.visible = false;
            this.eyesTired.visible = false;
            this.eyesClosed.visible = false;
            this.eyesBig.visible = false;
        }

        if (this.pass != PASS_NORMAL) {
            // Copia del brillo: la cabeza queda (su zona de la textura emisiva está vacía) con
            // sus ojitos; todo lo demás se oculta para que el brillo no toque nada más.
            this.body.visible = false;
            this.rightArm.visible = false;
            this.leftArm.visible = false;
            this.rightLeg.visible = false;
            this.leftLeg.visible = false;
            this.torch.visible = false;
            this.umbrella.visible = false;
            this.hat.visible = false;
            this.rightEar.visible = false;
            this.leftEar.visible = false;
        }

        // ---- Sombrero: lo dibuja NoxisHatLayer con sus colores (copa, lazo y gema por separado) ----
        boolean hatPass = this.pass >= PASS_HAT_CROWN;
        this.head.skipDraw = hatPass;                         // de la cabeza, solo el sombrero
        if (hatPass) {
            this.eyesOpen.visible = false;
            this.eyesTired.visible = false;
            this.eyesClosed.visible = false;
            this.eyesBig.visible = false;
            this.sleepBubble.visible = false;
            this.hairTuft.visible = false;
            this.hat.visible = state.hasHat;
            this.hatBrim.visible = this.pass == PASS_HAT_CROWN;
            this.hatCrown.visible = this.pass == PASS_HAT_CROWN;
            this.hatBand.visible = this.pass == PASS_HAT_BAND;
            this.hatSprig.visible = this.pass == PASS_HAT_FIXED;
            this.hatGem.visible = this.pass == PASS_HAT_FIXED;
        } else if (this.pass == PASS_NORMAL) {
            this.hat.visible = false;
        }
    }

    /**
     * Sacarse o ponerse el sombrero: los bracitos suben a buscarlo, el sombrero sube de la
     * cabeza con un bamboleo y se va hacia adelante y abajo hasta el piso (o al revés, desde el
     * piso hasta acomodarse en la cabeza). Se suma encima de la pose que ya tenga.
     */
    private void applyHatAnimation(byte type, float t) {
        float[] c = NoxisHatAnimation.curves(type, t);
        float lift = c[0];
        float carry = c[1];
        float relax = c[2];
        float wob = c[3];
        float forward = NoxisHatAnimation.smooth(carry * 1.8F);              // primero hacia adelante
        float down = NoxisHatAnimation.smooth((carry - 0.35F) / 0.65F);       // ...después hacia el piso
        this.hat.y += -4.0F * lift * (1.0F - down) + 15.0F * down;
        this.hat.z += -11.0F * forward;
        this.hat.xRot += 0.25F * forward;
        this.hat.zRot += wob;
        float k = 1.0F - relax;
        this.head.xRot += 0.35F * carry * k;                                  // agachadito para dejarlo/levantarlo
        float up = lift * (1.0F - carry);
        float rx = this.rightArm.xRot;
        float rz = this.rightArm.zRot;
        float lx = this.leftArm.xRot;
        float lz = this.leftArm.zRot;
        this.rightArm.xRot = Mth.lerp(k, rx, Mth.lerp(carry, Mth.lerp(lift, rx, -2.4F), -1.2F));
        this.leftArm.xRot = Mth.lerp(k, lx, Mth.lerp(carry, Mth.lerp(lift, lx, -2.4F), -1.2F));
        this.rightArm.zRot = Mth.lerp(k, rz, rz + 0.9F * up - 0.2F * carry);
        this.leftArm.zRot = Mth.lerp(k, lz, lz - 0.9F * up + 0.2F * carry);
    }

    /**
     * Saltito de la pecera: se agacha preparándose, se suelta con un "boing", recoge las patitas
     * y levanta los bracitos en el aire, se aprieta (más angostito y alto) para pasar por el
     * cuellito de la pecera y aterriza con un rebotito. Se suma encima de la pose que ya tenga.
     */
    private void applyBowlHop(byte type, float t) {
        float[] c = NoxisBowlHop.curves(type, t);
        float crouch = c[0];
        float tuck = c[1];
        float squeeze = c[2];
        float arms = c[3];
        float land = c[4];
        float down = 2.0F * crouch + 1.4F * land;               // agachadito / aplastón al aterrizar
        this.head.y += down;
        this.body.y += down * 0.75F;
        this.rightArm.y += down * 0.75F;
        this.leftArm.y += down * 0.75F;
        this.head.xRot += 0.15F * crouch;                        // mira la abertura, concentrado
        // Patitas recogidas en el aire.
        this.rightLeg.xRot = Mth.lerp(tuck, this.rightLeg.xRot, -1.35F);
        this.leftLeg.xRot = Mth.lerp(tuck, this.leftLeg.xRot, -1.35F);
        this.rightLeg.y -= 1.5F * tuck;
        this.leftLeg.y -= 1.5F * tuck;
        // Bracitos arriba (como zambulléndose).
        this.rightArm.xRot = Mth.lerp(arms, this.rightArm.xRot, -2.9F);
        this.leftArm.xRot = Mth.lerp(arms, this.leftArm.xRot, -2.9F);
        this.rightArm.zRot = Mth.lerp(arms, this.rightArm.zRot, 0.25F);
        this.leftArm.zRot = Mth.lerp(arms, this.leftArm.zRot, -0.25F);
        // Apretadito para pasar por el cuellito.
        float narrow = 1.0F - 0.4F * squeeze;
        this.setSqueeze(narrow, 1.0F + 0.15F * squeeze);
        this.rightArm.x *= narrow;
        this.leftArm.x *= narrow;
        this.rightLeg.x *= narrow;
        this.leftLeg.x *= narrow;
    }

    /**
     * Gestos de aburrimiento (coreografías propias, no reutilizan otras animaciones). Se suman
     * encima de la postura que ya tenga; {@code w} (0..1) permite volver suave si se corta.
     * {@code mirrored}: usa la pata / el brazo izquierdo en vez del derecho.
     */
    private void applyBoredom(byte type, boolean mirrored, float t, float w) {
        float s = mirrored ? -1.0F : 1.0F;
        switch (type) {
            case NoxisBoredAnimation.TAP -> this.boredTap(s, t, w);
            case NoxisBoredAnimation.HAT -> this.boredHat(s, t, w);
            case NoxisBoredAnimation.PAWS -> this.boredPaws(s, t, w);
            default -> { }
        }
    }

    /**
     * Pataditas: baja la mirada, pone las manitos en la cintura, levanta una patita y da tres
     * golpecitos contra el suelo (con rebote de todo el cuerpo y colita impaciente); pausa, mira
     * hacia un costado con ojitos de aburrido y las orejitas caídas, y vuelve a su postura.
     */
    private void boredTap(float s, float t, float w) {
        float down = NoxisBoredAnimation.env(t, 0, 10, 38, 48);
        float lift = NoxisBoredAnimation.env(t, 8, 13, 33, 38);
        float hips = NoxisBoredAnimation.env(t, 2, 12, 58, 70);
        float look = NoxisBoredAnimation.env(t, 42, 50, 62, 74);
        float fidget = NoxisBoredAnimation.env(t, 6, 12, 40, 50);
        float hit = 0.0F;
        for (int h : NoxisBoredAnimation.TAP_HITS) hit = Math.max(hit, NoxisBoredAnimation.pulse(t, h, 2.2F));
        ModelPart leg = s > 0 ? this.rightLeg : this.leftLeg;
        // Cabeza: mira la patita; después, hacia el otro costado (aburrido).
        this.head.xRot += w * (0.34F * down - 0.12F * look);
        this.head.yRot += w * (-0.62F * s * look);
        this.head.zRot += w * (0.12F * s * look);
        // El peso pasa a la otra patita.
        float shift = w * 0.45F * s * lift;
        this.head.x += shift;
        this.body.x += shift;
        this.rightArm.x += shift;
        this.leftArm.x += shift;
        // La patita sale adelante (que se vea bajo la pancita), se levanta con la puntita para
        // arriba y baja de golpe contra el suelo en cada golpecito.
        float foot = w * lift * (1.0F - hit);
        leg.z -= 1.6F * w * lift;
        leg.xRot -= 0.9F * foot;
        leg.y -= 1.0F * foot;
        // Cada golpecito sacude un poquito todo el cuerpo y las orejitas.
        float bump = w * hit * lift;
        this.head.y += 0.4F * bump;
        this.body.y += 0.3F * bump;
        this.rightArm.y += 0.3F * bump;
        this.leftArm.y += 0.3F * bump;
        this.rightEar.zRot -= 0.22F * bump;
        this.leftEar.zRot += 0.22F * bump;
        // Al mirar al costado, orejitas caídas de aburrimiento.
        this.rightEar.zRot -= w * 0.3F * look;
        this.leftEar.zRot += w * 0.3F * look;
        this.rightEar.xRot += w * 0.25F * look;
        this.leftEar.xRot += w * 0.25F * look;
        // Manitos en la cintura, impaciente.
        float h = w * hips;
        this.rightArm.xRot = Mth.lerp(h, this.rightArm.xRot, 0.35F);
        this.leftArm.xRot = Mth.lerp(h, this.leftArm.xRot, 0.35F);
        this.rightArm.zRot = Mth.lerp(h, this.rightArm.zRot, 0.65F);
        this.leftArm.zRot = Mth.lerp(h, this.leftArm.zRot, -0.65F);
        // Colita impaciente.
        this.tail.yRot += w * Mth.sin(t * 0.65F) * 0.5F * fidget;
        this.tail.xRot -= w * 0.2F * fidget;
    }

    /**
     * Acomodarse el sombrero: estira un bracito por el costado hasta el borde del ala (en
     * puntitas, hombrito arriba, cabeza apenas ladeada y la orejita de ese lado aplastada), lo
     * agarra y lo baja de su lado, le da un tironcito y lo gira de un lado al otro hasta dejarlo
     * justo; lo suelta (el sombrero vuelve a su lugar con un bamboleo) y termina satisfecho:
     * mentón arriba, pecho inflado, ojitos cerrados, orejas paradas, colita contenta y un saltito.
     */
    private void boredHat(float s, float t, float w) {
        float reach = NoxisBoredAnimation.env(t, 0, 12, 46, 56);
        float grip = NoxisBoredAnimation.env(t, 12, 16, 44, 48);
        float u = Mth.clamp((t - 18.0F) / 24.0F, 0.0F, 1.0F);
        float tug = Mth.sin(u * Mth.PI);
        float wig = Mth.sin(u * Mth.PI * 3.0F) * tug;
        float sat = NoxisBoredAnimation.env(t, 50, 55, 63, 72);
        float settle = t >= 46.0F && t < 58.0F ? Mth.sin((t - 46.0F) * 1.3F) * (1.0F - (t - 46.0F) / 12.0F) : 0.0F;
        float hop = NoxisBoredAnimation.pulse(t, 56.0F, 3.0F);
        ModelPart arm = s > 0 ? this.rightArm : this.leftArm;
        ModelPart other = s > 0 ? this.leftArm : this.rightArm;
        ModelPart ear = s > 0 ? this.rightEar : this.leftEar;
        ModelPart otherEar = s > 0 ? this.leftEar : this.rightEar;
        float r = w * reach;
        float g = w * grip;
        // Bracito por el costado hasta el borde del ala; acompaña cada movimiento del sombrero.
        arm.xRot = Mth.lerp(r, arm.xRot, -0.57F - 0.1F * tug * grip);
        arm.zRot = Mth.lerp(r, arm.zRot, s * (2.39F + 0.1F * wig * grip - 0.1F * tug * grip));
        arm.yScale = 1.0F + 0.35F * r;                       // estira el bracito (cartoon)
        arm.y -= 1.7F * r;                                    // hombrito arriba
        arm.x -= 0.9F * s * r;
        // La otra manito, abierta al costado para hacer equilibrio.
        other.zRot = Mth.lerp(r, other.zRot, -s * 0.4F);
        other.xRot = Mth.lerp(r, other.xRot, -0.2F);
        // En puntitas; al final, un saltito contento.
        float tip = 0.5F * r - 0.8F * w * hop;
        this.head.y -= tip;
        this.body.y -= tip;
        this.rightArm.y -= tip;
        this.leftArm.y -= tip;
        // Cabeza apenas ladeada hacia la mano; al final, mentón arriba, orgulloso.
        this.head.zRot += w * (-0.12F * s * reach + 0.04F * s * wig * grip);
        this.head.xRot += w * (0.15F * reach + 0.05F * grip * tug - 0.3F * sat);
        this.body.xRot -= w * 0.12F * sat;
        // El sombrero va con la manito: se ladea y baja de ese lado, tironcito y giro; al soltarlo
        // vuelve a su lugar con un bamboleo.
        this.hat.y += 1.2F * g;
        this.hat.zRot += w * (-0.45F * s * grip - 0.1F * s * tug * grip + 0.12F * settle);
        this.hat.yRot += 0.18F * wig * g;
        this.hat.xRot += 0.08F * tug * g;
        // Orejitas: la de ese lado aplastada por la manito; la otra se mueve; al final, bien paradas.
        ear.zRot -= s * 0.55F * g;
        otherEar.zRot += s * 0.14F * Mth.sin(t * 0.6F) * g;
        this.rightEar.zRot += w * 0.2F * sat;
        this.leftEar.zRot -= w * 0.2F * sat;
        this.rightEar.xRot -= w * 0.2F * sat;
        this.leftEar.xRot -= w * 0.2F * sat;
        // Colita contenta.
        this.tail.xRot -= w * 0.5F * sat;
        this.tail.yRot += w * Mth.sin(t * 0.8F) * 0.5F * sat;
    }

    /**
     * Jugar con sus patitas: levanta una patita al lado de la carita, gira la cabeza hacia ella y
     * la mira con curiosidad (pupilas grandes), moviéndola y ladeando la cabeza de un lado al otro;
     * después la baja delante de la pancita, suma la otra y se da palmaditas jugando (rebotes,
     * colita y orejitas que acompañan); pausa con las patitas juntas, un parpadeo, y vuelve.
     */
    private void boredPaws(float s, float t, float w) {
        float look = NoxisBoredAnimation.env(t, 0, 8, 86, 96);
        float up = NoxisBoredAnimation.env(t, 6, 16, 82, 92);
        float p2 = NoxisBoredAnimation.env(t, 40, 48, 80, 90);
        float mix = NoxisBoredAnimation.smooth((t - 40.0F) / 8.0F);       // de la carita a la pancita
        float inspect = NoxisBoredAnimation.env(t, 16, 22, 36, 44);
        float play = NoxisBoredAnimation.env(t, 46, 50, 70, 74);
        float pause = NoxisBoredAnimation.env(t, 72, 76, 82, 86);
        float osc = Mth.sin((t - 46.0F) * 0.8F) * play;
        float turn = Mth.sin((t - 16.0F) * 0.3F) * inspect;
        ModelPart first = s > 0 ? this.rightArm : this.leftArm;
        ModelPart second = s > 0 ? this.leftArm : this.rightArm;
        // Cabeza: gira hacia la patita levantada; después mira las patitas que juegan.
        this.head.yRot += w * s * 0.25F * up * (1.0F - mix);
        this.head.xRot += w * (0.12F * up * (1.0F - mix) + 0.22F * mix * look + 0.05F * osc);
        this.head.zRot += w * (0.22F * Mth.sin((t - 16.0F) * 0.2F) * inspect + 0.2F * s * pause);
        // Primera patita: al lado de la carita (hombrito arriba) y después delante de la pancita.
        float k1 = w * up;
        first.x += k1 * s * Mth.lerp(mix, -0.3F, 1.4F);
        first.y += k1 * Mth.lerp(mix, -1.4F, 0.0F);
        first.z = Mth.lerp(k1, first.z, Mth.lerp(mix, -1.4F, -2.0F));
        first.xRot = Mth.lerp(k1, first.xRot, Mth.lerp(mix, -0.43F, -1.11F) + 0.3F * osc);
        first.zRot = Mth.lerp(k1, first.zRot, s * Mth.lerp(mix, 2.03F + 0.12F * turn, -0.905F));
        first.yRot = Mth.lerp(k1, first.yRot, s * 0.6F * turn * (1.0F - mix));   // la gira y la mira
        first.yScale = 1.0F + k1 * (Mth.lerp(mix, 1.15F, 1.34F) - 1.0F);
        // La otra patita se suma para jugar.
        float k2 = w * p2;
        second.x -= k2 * s * 1.4F;
        second.z = Mth.lerp(k2, second.z, -2.0F);
        second.xRot = Mth.lerp(k2, second.xRot, -1.11F - 0.3F * osc);
        second.zRot = Mth.lerp(k2, second.zRot, s * 0.905F);
        second.yRot = Mth.lerp(k2, second.yRot, 0.0F);
        second.yScale = 1.0F + 0.34F * k2;
        // Rebotecitos, colita y orejitas jugando.
        float bounce = w * 0.3F * Math.abs(osc);
        this.body.y += bounce;
        this.head.y += bounce;
        this.rightArm.y += bounce;
        this.leftArm.y += bounce;
        this.tail.yRot += w * Mth.sin(t * 0.5F) * 0.55F * play;
        this.rightEar.zRot += w * 0.16F * osc;
        this.leftEar.zRot += w * 0.16F * osc;
        // Orejitas paradas de curiosidad mientras la mira.
        this.rightEar.xRot -= w * 0.3F * inspect;
        this.leftEar.xRot -= w * 0.3F * inspect;
        this.rightEar.zRot += w * Mth.sin(t * 0.9F) * 0.12F * inspect;
        this.leftEar.zRot -= w * Mth.sin(t * 0.9F + 1.7F) * 0.12F * inspect;
    }

    // ================================================================== cariño entre amigos

    /**
     * Gestos de cariño entre amigos: tiernos e inocentes, siempre uno AL LADO del otro mirando
     * para adelante (nunca cara a cara). Los dos lo hacen a la vez, cada uno con su papel.
     * {@code f} = +1 si el amigo está a su derecha, -1 si está a su izquierda.
     */
    private void applyAffection(byte anim, float t, float w) {
        boolean lead = NoxisAffectionAnimation.roleOf(anim) == NoxisAffectionAnimation.LEAD;
        float f = NoxisAffectionAnimation.friendOnRight(anim) ? 1.0F : -1.0F;
        switch (NoxisAffectionAnimation.gestureOf(anim)) {
            case NoxisAffectionAnimation.HANDS -> this.affectionHands(f, t, w);
            case NoxisAffectionAnimation.CHEEK -> this.affectionCheek(lead, f, t, w);
            case NoxisAffectionAnimation.HUG -> {
                if (lead) this.affectionHug(f, t, w);
                else this.affectionHugBack(f, t, w);
            }
            default -> { }
        }
    }

    private void affectionHop(float amount) {
        this.head.y -= amount;
        this.body.y -= amount;
        this.rightArm.y -= amount;
        this.leftArm.y -= amount;
    }

    /** Toda la parte de arriba se corre un poquito hacia el amigo. */
    private void affectionShift(float dx, float headExtra) {
        this.body.x += dx;
        this.rightArm.x += dx;
        this.leftArm.x += dx;
        this.head.x += dx + headExtra;
    }

    /**
     * Agarrarse de las manitos: se miran, estiran el bracito de adentro y se dan la mano;
     * balancean las manitos juntas (con un saltito contento), se sueltan y se miran contentos.
     */
    private void affectionHands(float f, float t, float w) {
        float look = NoxisBoredAnimation.env(t, 0, 8, 20, 28) + NoxisBoredAnimation.env(t, 62, 68, 86, 94);
        float reach = NoxisBoredAnimation.env(t, 8, 20, 58, 68);
        float hold = NoxisBoredAnimation.env(t, 20, 24, 56, 60);
        float sw = Mth.sin((t - 22.0F) * 0.24F) * hold;
        ModelPart inner = f > 0 ? this.rightArm : this.leftArm;
        ModelPart outer = f > 0 ? this.leftArm : this.rightArm;
        this.head.yRot += w * (f * 0.22F * look + f * 0.08F * hold);
        this.head.zRot += w * -f * 0.1F * look;
        float k = w * reach;
        inner.zRot = Mth.lerp(k, inner.zRot, f * 1.15F);
        inner.xRot = Mth.lerp(k, inner.xRot, -0.15F + 0.3F * sw);
        inner.yScale *= 1.0F + 0.15F * k;
        outer.xRot += w * 0.25F * sw;
        this.body.zRot += w * -f * 0.03F * sw;
        this.tail.yRot += w * Mth.sin(t * 0.5F) * 0.4F * (hold + look);
        this.rightEar.xRot -= w * 0.2F * look;
        this.leftEar.xRot -= w * 0.2F * look;
        this.affectionHop(w * 0.7F * (NoxisBoredAnimation.pulse(t, 42.0F, 3.0F) + NoxisBoredAnimation.pulse(t, 82.0F, 3.0F)));
    }

    /**
     * Cabecitas de costado: se miran, se inclinan apenas hacia el otro y apoyan un costado de la
     * cabeza contra el del otro un instante (las caras siguen mirando adelante), con un
     * frotadito suave; los sombreros se ladean para afuera con el empujoncito. Se separan, se
     * miran y uno da un saltito.
     */
    private void affectionCheek(boolean lead, float f, float t, float w) {
        float look = NoxisBoredAnimation.env(t, 0, 6, 12, 18) + NoxisBoredAnimation.env(t, 66, 72, 84, 90);
        float lean = NoxisBoredAnimation.env(t, 12, 26, 60, 72);
        float rub = NoxisBoredAnimation.env(t, 28, 34, 54, 60);
        float ro = Mth.sin((t - 28.0F) * 0.4F) * rub;
        ModelPart innerEar = f > 0 ? this.rightEar : this.leftEar;
        ModelPart outerEar = f > 0 ? this.leftEar : this.rightEar;
        this.head.yRot += w * f * 0.2F * look;
        float l = w * lean;
        this.affectionShift(-f * 0.5F * l, -f * 0.4F * l);
        this.head.zRot += -f * 0.12F * l + w * 0.04F * ro * -f;
        this.head.y += (lead ? 0.9F : -0.3F) * l;
        this.body.zRot += -f * 0.05F * l;
        this.hat.zRot += f * 0.3F * l;
        this.hat.x += f * 0.8F * l;
        this.hat.y += (lead ? 0.2F : -0.4F) * l;
        innerEar.xRot += 0.5F * l;
        outerEar.zRot += (f > 0 ? 0.15F : -0.15F) * l * Mth.sin(t * 0.4F);
        this.rightArm.xRot = Mth.lerp(l, this.rightArm.xRot, -0.35F);
        this.leftArm.xRot = Mth.lerp(l, this.leftArm.xRot, -0.35F);
        this.tail.yRot += w * Mth.sin(t * 0.35F) * 0.5F * lean;
        if (!lead) this.affectionHop(w * 0.6F * NoxisBoredAnimation.pulse(t, 78.0F, 3.0F));
    }

    /**
     * Abrazo (el que vino): lo mira, abre bien los bracitos con un saltito, y lo abraza por el
     * costado (un bracito por la espalda del amigo y el otro por delante), con apretoncitos
     * suaves; después lo suelta y lo mira contento.
     */
    private void affectionHug(float f, float t, float w) {
        float look = NoxisBoredAnimation.env(t, 0, 6, 12, 20) + NoxisBoredAnimation.env(t, 66, 72, 86, 94);
        float open = NoxisBoredAnimation.env(t, 8, 16, 20, 28);
        float hug = NoxisBoredAnimation.env(t, 22, 30, 60, 70);
        float sq = Mth.sin((t - 30.0F) * 0.3F) * NoxisBoredAnimation.env(t, 30, 34, 56, 60);
        ModelPart inner = f > 0 ? this.rightArm : this.leftArm;
        ModelPart outer = f > 0 ? this.leftArm : this.rightArm;
        this.head.yRot += w * f * 0.25F * look;
        // Abre los bracitos.
        float o = w * open;
        this.rightArm.zRot = Mth.lerp(o, this.rightArm.zRot, 2.0F);
        this.leftArm.zRot = Mth.lerp(o, this.leftArm.zRot, -2.0F);
        this.rightArm.xRot = Mth.lerp(o, this.rightArm.xRot, -0.25F);
        this.leftArm.xRot = Mth.lerp(o, this.leftArm.xRot, -0.25F);
        this.rightArm.yScale *= 1.0F + 0.2F * o;
        this.leftArm.yScale *= 1.0F + 0.2F * o;
        this.rightEar.xRot -= 0.3F * o;
        this.leftEar.xRot -= 0.3F * o;
        this.affectionHop(w * 0.6F * NoxisBoredAnimation.pulse(t, 14.0F, 3.0F));
        // Lo abraza por el costado.
        float h = w * hug;
        inner.zRot = Mth.lerp(h, inner.zRot, f * 1.25F);
        inner.xRot = Mth.lerp(h, inner.xRot, 0.75F);
        inner.yScale *= 1.0F + 0.45F * h;
        outer.zRot = Mth.lerp(h, outer.zRot, -f * 0.55F);
        outer.xRot = Mth.lerp(h, outer.xRot, -1.25F);
        outer.yRot = Mth.lerp(h, outer.yRot, f * 0.5F);
        outer.yScale *= 1.0F + 0.45F * h;
        outer.z -= 0.6F * h;
        this.affectionShift(-f * 1.0F * h, -f * 0.1F * h);
        this.head.zRot += -f * 0.1F * h;
        this.head.y += 0.8F * h + 0.2F * sq * h;
        this.body.y += 0.15F * sq * h;
        this.hat.zRot += f * 0.28F * h;
        this.hat.x += f * 0.7F * h;
        this.tail.yRot += w * Mth.sin(t * 0.5F) * 0.45F * (hug + look);
    }

    /**
     * Abrazo (el que lo recibe): para las orejitas al sentirlo, le devuelve el abrazo con el
     * bracito de su lado, mueve la colita y, al final, da un saltito contento.
     */
    private void affectionHugBack(float f, float t, float w) {
        float notice = NoxisBoredAnimation.env(t, 18, 24, 62, 70);
        float ret = NoxisBoredAnimation.env(t, 34, 42, 58, 66);
        float look = NoxisBoredAnimation.env(t, 66, 72, 86, 94);
        ModelPart inner = f > 0 ? this.rightArm : this.leftArm;
        float n = w * notice;
        this.rightEar.xRot -= 0.45F * n;
        this.leftEar.xRot -= 0.45F * n;
        this.rightEar.zRot += 0.2F * n;
        this.leftEar.zRot -= 0.2F * n;
        this.head.yRot += w * f * 0.2F * (look + 0.5F * notice);
        this.hat.zRot += f * 0.15F * n;
        this.hat.y -= 0.4F * n;
        this.hat.x += f * 0.3F * n;
        float k = w * ret;
        inner.zRot = Mth.lerp(k, inner.zRot, f * 1.2F);
        inner.xRot = Mth.lerp(k, inner.xRot, 0.6F);
        inner.yScale *= 1.0F + 0.35F * k;
        this.tail.yRot += w * Mth.sin(t * 0.6F) * 0.5F * (ret + look);
        this.affectionHop(w * 0.6F * NoxisBoredAnimation.pulse(t, 80.0F, 3.0F));
    }

    // ================================================================== descanso: acomodarse y estirarse

    private void applyRestGesture(byte gesture, float t, float w, boolean inBowl) {
        switch (NoxisRestGesture.typeOf(gesture)) {
            case NoxisRestGesture.SETTLE -> this.restSettle(t, w, inBowl, NoxisRestGesture.lead(gesture));
            case NoxisRestGesture.STRETCH_SEATED -> this.restStretch(t - NoxisRestGesture.lead(gesture), w, true,
                    NoxisRestGesture.hasHat(gesture));
            case NoxisRestGesture.STRETCH_STAND -> this.restStretch(t, w, false, NoxisRestGesture.hasHat(gesture));
            default -> { }
        }
    }

    /**
     * Acomodarse para descansar: mira el lugar (de pie, recorriéndolo con la vista), junta las
     * patitas y los bracitos y baja suave el cuerpo; se sienta con la transición de siempre y,
     * ya sentado, se acomoda con un balanceo de lado a lado que se va calmando (colita que se
     * enrosca, orejitas que se relajan) y un suspiro final. En la pecera solo hace el balanceo.
     */
    private void restSettle(float t, float w, boolean inBowl, int seed) {
        float side = (seed & 1) == 0 ? 1.0F : -1.0F;
        float look = NoxisBoredAnimation.env(t, 0, 6, 12, 20);
        float prep = NoxisBoredAnimation.env(t, 3, 10, 13, 24);
        float wig = NoxisBoredAnimation.env(t, 28, 32, 48, 56);
        float decay = 1.0F - Mth.clamp((t - 28.0F) / 30.0F, 0.0F, 1.0F);
        float osc = Mth.sin((t - 28.0F) * 0.62F) * wig * decay * side;
        float sigh = NoxisBoredAnimation.env(t, 48, 52, 54, 60);
        float k = inBowl ? 0.55F : 1.0F;                 // en la pecera, más chiquito (no toca el vidrio)
        // Mira el lugar donde se va a acomodar.
        this.head.xRot += w * 0.32F * look;
        this.head.yRot += w * 0.24F * Mth.sin(t * 0.38F) * look;
        // Junta las patitas y los bracitos, y baja suave el cuerpo.
        float p = w * prep;
        this.rightLeg.x += 0.5F * p;
        this.leftLeg.x -= 0.5F * p;
        this.rightArm.xRot = Mth.lerp(p, this.rightArm.xRot, -0.45F);
        this.leftArm.xRot = Mth.lerp(p, this.leftArm.xRot, -0.45F);
        this.rightArm.zRot = Mth.lerp(p, this.rightArm.zRot, -0.12F);
        this.leftArm.zRot = Mth.lerp(p, this.leftArm.zRot, 0.12F);
        float lower = 0.9F * p;
        this.head.y += lower;
        this.body.y += lower;
        this.rightArm.y += lower;
        this.leftArm.y += lower;
        this.tail.yRot += 0.6F * p * side;
        // Ya sentado: se balancea buscando la postura cómoda.
        float o = w * k * osc;
        this.body.zRot += 0.11F * o;
        this.head.zRot += 0.09F * o;
        this.body.x += 0.35F * o;
        this.head.x += 0.25F * o;
        this.rightArm.x += 0.3F * o;
        this.leftArm.x += 0.3F * o;
        this.tail.yRot += 0.5F * o;
        float flick = NoxisBoredAnimation.pulse(t, 31.0F, 2.0F) + NoxisBoredAnimation.pulse(t, 43.0F, 2.0F);
        this.rightEar.zRot -= w * k * 0.2F * flick;
        this.leftEar.zRot += w * k * 0.2F * flick;
        // Suspiro final: baja apenas la cabecita y el cuerpo se afloja.
        this.head.xRot += w * k * 0.08F * sigh;
        this.body.y += w * k * 0.2F * sigh;
        this.head.y += w * k * 0.25F * sigh;
    }

    /**
     * Estirarse al despertar. Sentado: abre los ojitos despacio y levanta la cabeza; se encoge un
     * poquito (anticipación); estira los dos bracitos arriba y un poco afuera, arquea el cuerpo,
     * estira las patitas para adelante y para la colita (con un temblorcito en el punto máximo);
     * bosteza con la cabeza para atrás; baja los brazos, se levanta, se sacude suave y, si tiene
     * sombrero, le da un toquecito para dejarlo derecho. De pie (al salir de la pecera): lo mismo,
     * pero estira una patita para atrás y después la otra, como un gatito.
     */
    private void restStretch(float u, float w, boolean seated, boolean hat) {
        // Tiempos (sentado / de pie).
        float antic = seated ? NoxisBoredAnimation.env(u, 10, 16, 18, 22) : NoxisBoredAnimation.env(u, 0, 5, 7, 10);
        float big = seated ? NoxisBoredAnimation.env(u, 18, 26, 40, 50) : NoxisBoredAnimation.env(u, 7, 14, 28, 36);
        float peak = seated ? NoxisBoredAnimation.env(u, 26, 29, 36, 40) : NoxisBoredAnimation.env(u, 14, 17, 24, 28);
        float yawn = seated ? NoxisBoredAnimation.env(u, 32, 36, 42, 47) : NoxisBoredAnimation.env(u, 22, 26, 32, 37);
        float shakeStart = seated ? 64.0F : 40.0F;
        float shake = NoxisBoredAnimation.env(u, shakeStart, shakeStart + 3, shakeStart + 12, shakeStart + 16);
        float shakeOsc = Mth.sin((u - shakeStart) * 1.7F) * shake;
        float hatStart = seated ? 80.0F : 60.0F;
        float hatTap = hat ? NoxisBoredAnimation.env(u, hatStart, hatStart + 4, hatStart + 8, hatStart + 13) : 0.0F;
        float lift = seated ? NoxisBoredAnimation.smooth(u / 10.0F) * (1.0F - NoxisBoredAnimation.smooth((u - 50.0F) / 8.0F)) : 0.0F;

        // Abre los ojitos y levanta la cabeza (sentado, recién despierto).
        this.head.xRot -= w * 0.06F * lift;
        // Anticipación: se encoge, manitos al pecho, orejitas para atrás.
        float a = w * antic;
        this.head.y += 0.6F * a;
        this.body.y += 0.45F * a;
        this.rightArm.y += 0.45F * a;
        this.leftArm.y += 0.45F * a;
        this.head.xRot += 0.15F * a;
        this.rightEar.xRot += 0.4F * a;
        this.leftEar.xRot += 0.4F * a;
        this.rightArm.xRot = Mth.lerp(a, this.rightArm.xRot, -0.9F);
        this.leftArm.xRot = Mth.lerp(a, this.leftArm.xRot, -0.9F);
        this.rightArm.zRot = Mth.lerp(a, this.rightArm.zRot, -0.2F);
        this.leftArm.zRot = Mth.lerp(a, this.leftArm.zRot, 0.2F);
        // Estirón: bracitos arriba y un poco afuera (con temblorcito), cuerpo arqueado, más alto.
        float b = w * big;
        float tremble = Mth.sin(u * 2.3F) * 0.05F * peak;
        this.rightArm.xRot = Mth.lerp(b, this.rightArm.xRot, -0.15F);
        this.leftArm.xRot = Mth.lerp(b, this.leftArm.xRot, -0.15F);
        this.rightArm.zRot = Mth.lerp(b, this.rightArm.zRot, 2.25F + tremble);
        this.leftArm.zRot = Mth.lerp(b, this.leftArm.zRot, -2.25F + tremble);
        this.rightArm.yScale *= 1.0F + 0.45F * b;
        this.leftArm.yScale *= 1.0F + 0.45F * b;
        this.body.xRot -= 0.22F * b;
        this.head.xRot -= 0.3F * b + 0.18F * w * yawn;
        this.head.y -= 0.9F * b;
        this.body.y -= 0.5F * b;
        this.rightArm.y -= 1.2F * b;
        this.leftArm.y -= 1.2F * b;
        // Orejitas bien paradas y colita estirada para arriba.
        this.rightEar.zRot += 0.22F * b + 0.06F * Mth.sin(u * 2.0F) * peak;
        this.leftEar.zRot -= 0.22F * b + 0.06F * Mth.sin(u * 2.0F + 1.3F) * peak;
        this.tail.xRot -= 0.9F * b;
        this.tail.yRot = Mth.lerp(b, this.tail.yRot, 0.0F);
        if (seated) {
            // Patitas estiradas para adelante, con las puntitas arriba.
            this.rightLeg.xRot -= 0.35F * b;
            this.leftLeg.xRot -= 0.35F * b;
            this.rightLeg.z -= 0.4F * b;
            this.leftLeg.z -= 0.4F * b;
        } else {
            // De pie: en puntitas y estira una patita para atrás, después la otra.
            float legR = NoxisBoredAnimation.env(u, 12, 16, 19, 23);
            float legL = NoxisBoredAnimation.env(u, 20, 24, 27, 31);
            this.rightLeg.xRot += 0.75F * w * legR;
            this.leftLeg.xRot += 0.75F * w * legL;
            this.rightLeg.z += 0.6F * w * legR;
            this.leftLeg.z += 0.6F * w * legL;
            this.body.xRot += 0.12F * w * (legR + legL);
        }
        // Bostezo: cabeza atrás, ojitos apretados (los pone el modelo), cabecita estirada, orejas atrás.
        float y = w * yawn;
        this.head.yScale *= 1.0F + 0.07F * y;
        this.head.xScale *= 1.0F - 0.03F * y;
        this.rightEar.xRot += 0.35F * y;
        this.leftEar.xRot += 0.35F * y;
        // Sacudida suave al final (cuerpo, cabeza, orejas y colita).
        float sh = w * shakeOsc;
        this.body.zRot += 0.1F * sh;
        this.head.zRot -= 0.13F * sh;
        this.rightEar.zRot += 0.3F * sh;
        this.leftEar.zRot += 0.3F * sh;
        this.tail.yRot += 0.5F * sh;
        this.rightArm.zRot += 0.18F * sh;
        this.leftArm.zRot += 0.18F * sh;
        // Toquecito al sombrero: una manito sube rápido al costado, el sombrero salta apenas y
        // vuelve derecho; la cabeza acompaña con un cabeceo.
        if (hatTap > 0.0F) {
            float h = w * hatTap;
            float pop = NoxisBoredAnimation.pulse(u, hatStart + 6.0F, 3.0F);
            this.rightArm.xRot = Mth.lerp(h, this.rightArm.xRot, -0.7F);
            this.rightArm.zRot = Mth.lerp(h, this.rightArm.zRot, 2.15F);
            this.rightArm.yScale *= 1.0F + 0.3F * h;
            this.rightArm.y -= 1.4F * h;
            this.hat.y -= 1.2F * w * pop;
            this.hat.yRot += 0.25F * w * pop;
            this.head.xRot += 0.1F * w * pop;
            this.head.zRot += 0.08F * h;
        }
    }

    /** Escala de la cabeza y el cuerpo (1 = normal; el saltito de la pecera los aprieta). */
    private void setSqueeze(float wide, float tall) {
        this.head.xScale = wide;
        this.head.zScale = wide;
        this.head.yScale = tall;
        this.body.xScale = wide;
        this.body.zScale = wide;
        this.body.yScale = tall;
    }
}
