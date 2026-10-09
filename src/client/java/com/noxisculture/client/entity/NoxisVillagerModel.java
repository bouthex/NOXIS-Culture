package com.noxisculture.client.entity;

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
        this.rightEar = this.head.getChild(PartNames.RIGHT_EAR);
        this.leftEar = this.head.getChild(PartNames.LEFT_EAR);
        this.eyesTired = this.head.getChild(EYES_TIRED);
        this.eyesClosed = this.head.getChild(EYES_CLOSED);
        this.eyesOpen = this.head.getChild(EYES_OPEN);
        this.eyesBig = this.head.getChild(EYES_BIG);
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
        float doze = rest * (Mth.sin(age * 0.05F) * 0.06F + (dozing ? 0.22F : 0.0F));
        this.head.yRot = state.yRot * Mth.DEG_TO_RAD + 0.3F * torchAnim;   // mira hacia la luz
        this.head.xRot = state.xRot * Mth.DEG_TO_RAD + life[0] + 0.25F * scared + 0.1F * torchAnim
                + 0.15F * rest + doze;
        // Con la antorcha ladea la cabecita (como esforzándose por ver); cansado, la deja caer de lado.
        this.head.zRot = roll * 1.4F + life[1] + NoxisMoodAnimator.headTremble(age, scared)
                + 0.2F * torchAnim + 0.12F * rest;
        this.head.x = -roll * 3.0F;
        this.head.z = -0.9F * torchAnim;                             // estira el cuello para ver mejor
        this.head.y = HEAD_Y - bounce - hop + 0.8F * scared + sitDrop;

        // ---- Piezas compartidas por todas las especies ----
        NoxisMoodAnimator.animateEars(this.rightEar, this.leftEar, age, happy, scared, EAR_BASE_ANGLE);
        NoxisMoodAnimator.animateHat(this.hat, HAT_Y, HAT_BASE_TILT, age, pos, amount, happy, scared);
        NoxisMoodAnimator.animateTail(this.tail, TAIL_BASE_ANGLE, age, happy, scared);
        // Cansado: orejitas caídas, sombrero que se resbala sobre los ojos, cola enroscada en el piso.
        float plopBounce = Mth.sin(Mth.clamp((rest - 0.2F) / 0.8F, 0.0F, 1.0F) * Mth.PI) * (this.restRising ? 0.0F : 1.0F);
        this.rightEar.zRot -= 0.55F * rest - 0.35F * plopBounce;
        this.leftEar.zRot += 0.55F * rest - 0.35F * plopBounce;
        this.hat.xRot = -0.05F + 0.2F * rest;
        this.tail.xRot = Mth.lerp(sit, this.tail.xRot, -0.15F);
        this.tail.yRot = Mth.lerp(sit, this.tail.yRot, 1.25F);
        // Curiosidad repentina (gesto compartido; solo aparece estando quieto y tranquilo).
        NoxisMoodAnimator.applyCuriosity(this.head, this.rightEar, this.leftEar,
                state.curious, state.curiousSide, state.curiousFlick);
        // Fascinación por un cristal: ladea la cabeza y para las orejitas (mirarlo lo hace el servidor).
        NoxisMoodAnimator.applyFascination(this.head, this.rightEar, this.leftEar,
                state.crystalAmount, state.crystalSide, state.crystalTwitch, state.crystalHop);
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
        boolean sleepy = torchAnim > 0.5F || rest > 0.5F;
        boolean slowBlink = sleepy && (t % 110) < 8;
        boolean normalBlink = (t % 83) < 3;
        boolean closed = slowBlink || (rest > 0.5F && dozing) || (!sleepy && normalBlink)
                || state.natureEyesClosed > 0.5F;                    // disfrutando el aroma
        this.eyesClosed.visible = closed;
        this.eyesTired.visible = sleepy && !closed;
        boolean open = !closed && !sleepy;
        // Pupilas agrandadas, fascinado con la flor: los ojos grandes reemplazan a los normales.
        boolean bigPupils = open && state.naturePupils > 0.5F;
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
        float hopState = state.crystalHop;
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

        // ---- Variante de ropa: capa de espalda (componente reutilizable por todas las especies Noxis) ----
        int capeStyle = state.variant - 1;          // 0 = sin capa; 1..3 = estilos de capa
        boolean hasCape = capeStyle >= 0;
        NoxisCape.animate(this.body, capeStyle, state.capeSwing, state.capeTurn, age, sit);
        if (hasCape) {
            // Sentado con capa: la colita sale derecha por su ojal (no hacia el costado).
            this.tail.yRot = Mth.lerp(sit, this.tail.yRot, 0.0F);
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
    }
}
