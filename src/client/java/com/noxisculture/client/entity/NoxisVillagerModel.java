package com.noxisculture.client.entity;

import com.noxisculture.entity.idle.NoxisAffectionAnimation;
import com.noxisculture.entity.idle.NoxisBoredAnimation;
import com.noxisculture.entity.idle.NoxisBowlHop;
import com.noxisculture.entity.idle.NoxisKeepsakeAnimation;
import com.noxisculture.entity.idle.NoxisPersonaAnimation;
import com.noxisculture.entity.idle.NoxisHumAnimation;
import com.noxisculture.entity.idle.NoxisListenAnimation;
import com.noxisculture.entity.idle.NoxisDanceAnimation;
import com.noxisculture.entity.idle.NoxisPlayAnimation;
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
    /** Copia que solo dibuja el libro abierto y el lápiz (lectores y escritores), con {@code noxis_props.png}. */
    public static final int PASS_PROPS = 6;
    private static final String PROP_BOOK = "prop_book";
    private static final String PROP_PENCIL = "prop_pencil";
    /** Globito de sueño (solo lo dibuja la copia {@link #PASS_SLEEP_BUBBLE}). */
    private static final String SLEEP_BUBBLE = "sleep_bubble";
    /** Mechoncito de pelo verde (solo se ve cuando no tiene sombrero). */
    private static final String HAIR_TUFT = "hair_tuft";
    /** Boquita en "o" para silbar (solo se ve mientras silba). */
    private static final String WHISTLE_MOUTH = "whistle_mouth";
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
    /** La raíz del modelo (para la caída torpe: todo el Noxis gira desde los pies). */
    private final ModelPart rootPart;
    private final ModelPart eyesBig;
    private final ModelPart sleepBubble;
    private final ModelPart hairTuft;
    private final ModelPart flowerAnchor;
    private final ModelPart propBook;
    private final ModelPart propPencil;
    private final ModelPart whistleMouth;

    public NoxisVillagerModel(ModelPart root) {
        this(root, PASS_NORMAL);
    }

    public NoxisVillagerModel(ModelPart root, int pass) {
        super(root);
        this.rootPart = root;
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
        this.whistleMouth = this.head.getChild(WHISTLE_MOUTH);
        this.flowerAnchor = root.getChild(FLOWER_ANCHOR);
        this.propBook = root.getChild(PROP_BOOK);
        this.propPencil = root.getChild(PROP_PENCIL);
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
        // Boquita de silbar: un cubito chiquito que asoma de la cara, debajo de los ojos (u=100 v=100).
        head.addOrReplaceChild(WHISTLE_MOUTH,
                CubeListBuilder.create().texOffs(100, 100).addBox(-0.5F, -2.7F, -4.6F, 1.0F, 1.0F, 0.6F),
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

        // ---- Libro abierto y lápiz (lectores / escritores): textura propia noxis_props.png ----
        // Modelados al doble de tamaño y achicados a la mitad en cada cuadro (pixel art más fino).
        PartDefinition book = root.addOrReplaceChild(PROP_BOOK, CubeListBuilder.create()
                        .texOffs(0, 24).addBox(-1.0F, 0.3F, -5.0F, 2.0F, 1.0F, 10.0F),          // lomo
                PartPose.ZERO);
        book.addOrReplaceChild("left", CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-7.0F, 0.0F, -5.0F, 7.0F, 1.0F, 10.0F)            // tapa
                        .texOffs(40, 0).addBox(-6.5F, -1.0F, -4.5F, 6.0F, 1.0F, 9.0F),          // páginas
                PartPose.rotation(0.0F, 0.0F, 0.22F));
        book.addOrReplaceChild("right", CubeListBuilder.create()
                        .texOffs(0, 12).addBox(0.0F, 0.0F, -5.0F, 7.0F, 1.0F, 10.0F)
                        .texOffs(40, 12).addBox(0.5F, -1.0F, -4.5F, 6.0F, 1.0F, 9.0F),
                PartPose.rotation(0.0F, 0.0F, -0.22F));
        root.addOrReplaceChild(PROP_PENCIL, CubeListBuilder.create()
                        .texOffs(56, 24).addBox(-1.0F, -4.0F, -1.0F, 2.0F, 1.0F, 2.0F)          // terminación oscura
                        .texOffs(30, 24).addBox(-1.0F, -3.0F, -1.0F, 2.0F, 7.0F, 2.0F)          // cuerpo de madera pintada
                        .texOffs(40, 24).addBox(-1.0F, 4.0F, -1.0F, 2.0F, 1.0F, 2.0F)           // madera afilada
                        .texOffs(50, 24).addBox(-0.5F, 5.0F, -0.5F, 1.0F, 1.0F, 1.0F),          // punta de grafito
                PartPose.ZERO);

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
        float sit = Math.max(Math.max(rest, state.watchSit), Math.max(state.natureSit, state.scholarSit));

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
        boolean sleepy = torchAnim > 0.5F || rest > 0.5F || state.boredTired || state.playSly || state.personaHalf || state.humEyesHalf || state.scholarFocused || state.danceHalf;
        boolean slowBlink = sleepy && (t % 110) < 8;
        boolean normalBlink = (t % 83) < 3;
        // Fascinado con un cristal no parpadea: lo mira con los ojos bien abiertos (escuchando, tampoco).
        if (state.crystalAmount > 0.5F || state.listenNoBlink) normalBlink = false;
        boolean closed = slowBlink || (rest > 0.5F && dozing) || (!sleepy && normalBlink)
                || state.natureEyesClosed > 0.5F
                || state.socialYawn > 0.5F || Math.abs(state.socialLean) > 0.5F     // bostezo / dormidos juntos
                || (state.socialDoze > 0.6F && (t % 30) < 12)
                || state.boredEyesClosed || state.affectionEyesClosed || state.playLaugh || state.personaClosed || state.keepsakeClosed || state.listenClosed || state.danceClosed;                     // cabeceo de sueño                    // disfrutando el aroma
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
        boolean bigPupils = open && (state.naturePupils > 0.5F || state.crystalAmount > 0.35F || state.boredPupils || state.affectionPupils || state.playPupils || state.personaPupils || state.keepsakePupils || state.listenPupils || state.danceBig);
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
        // ---- Objetos: mirar algo con curiosidad / sacar, contemplar y guardar un tesoro ----
        if (state.keepsake != NoxisKeepsakeAnimation.NONE && state.keepsakeWeight > 0.0F) {
            this.applyKeepsake(state.keepsake, state.keepsakeTime, state.keepsakeWeight, state.keepsakeHolding);
        }
        // ---- Gesto propio de la personalidad ----
        if (state.persona != NoxisPersonaAnimation.NONE && state.personaWeight > 0.0F) {
            this.applyPersona(state.persona, state.personaTime, state.personaWeight);
        }
        // ---- Melodías espontáneas: tararear o silbar, mecerse al compás o escuchar ----
        if (state.hum != NoxisHumAnimation.NONE && state.humWeight > 0.0F) {
            this.applyHum(state.hum, state.humWeight, state.humBeatTime);
        }
        // ---- Curiosidad y travesuras (seguir, observar, acercarse agachadito, ¡salto!, susto y juego) ----
        this.applyPlay(state.playWeight, state.playTime);
        // ---- Cariño con su amigo especial (coreografía compartida por los dos) ----
        if (state.affection != NoxisAffectionAnimation.NONE && state.affectionWeight > 0.0F) {
            this.applyAffection(state.affection, state.affectionTime, state.affectionWeight);
        }
        // ---- Sonidos del mundo: escuchar, ir a investigar y mirar qué era ----
        this.applyListen(state.listenWeight, state.listenTime, state.listenDir, state.listenSide,
                torchAnim > 0.5F, state.umbrellaAnim > 0.5F);
        // ---- Bailar con un disco (y la caída torpe) ----
        if (state.danceWeight > 0.0F || state.danceFallWeight > 0.0F || state.dancePairFallWeight > 0.0F) {
            this.applyDance(state, torchAnim > 0.5F, state.umbrellaAnim > 0.5F);
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

        // ---- Boquita de silbar (solo en el Noxis normal) ----
        this.whistleMouth.visible = this.pass == PASS_NORMAL && state.whistleMouth;
        // ---- Libro y lápiz: solo los dibuja la copia PASS_PROPS (con su textura) ----
        this.propBook.visible = this.pass == PASS_PROPS && state.bookShown;
        this.propPencil.visible = this.pass == PASS_PROPS && state.pencilShown;
        if (this.pass == PASS_PROPS) {
            this.head.visible = false;
            this.flowerAnchor.visible = false;
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
        boolean hatPass = this.pass >= PASS_HAT_CROWN && this.pass <= PASS_HAT_FIXED;
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

    // ================================================================== objetos personales

    private void applyKeepsake(byte a, float t, float w, boolean holding) {
        if (a == NoxisKeepsakeAnimation.INSPECT) {
            this.keepsakeInspect(t, w);
        } else if (a == NoxisKeepsakeAnimation.CONTEMPLATE) {
            this.keepsakeContemplate(t, w, holding);
        } else if (NoxisKeepsakeAnimation.isBook(a)) {
            this.bookPose(a, t, w, holding);
        }
    }

    // ---------------------------------------------------------------- leer y escribir

    /**
     * Leer y escribir (ya sentado con la postura de siempre). Abrir: busca el libro en su
     * costadito y lo trae adelante. Leer: libro en alto con las dos patitas, la cabeza recorre los
     * renglones de un lado al otro (y baja un poquito en cada uno), da vuelta la página con la
     * patita, cambia apenas de postura, y a veces se queda pensando mirando para arriba con la
     * patita en el mentón. Escribir: libro sobre la falda sostenido por una patita; la otra escribe
     * en garabatos rápidos de renglón en renglón, la cabeza gacha y los ojitos concentrados; a veces
     * se golpea el mentón pensando y vuelve a escribir con ganas. Cerrar: lo cierra, lo guarda en
     * su costadito y se levanta.
     */
    private void bookPose(byte a, float t, float w, boolean holding) {
        boolean writing = NoxisKeepsakeAnimation.isWriting(a);
        // Cuánto está "con el libro" (0 buscándolo / guardándolo, 1 leyendo o escribiendo).
        float withBook;
        float pocket = 0.0F;
        if (a == NoxisKeepsakeAnimation.READ_OPEN || a == NoxisKeepsakeAnimation.WRITE_OPEN) {
            pocket = NoxisBoredAnimation.env(t, 10, 15, 19, 24);
            withBook = NoxisBoredAnimation.smooth((t - 20.0F) / 9.0F);
        } else if (a == NoxisKeepsakeAnimation.READ_CLOSE || a == NoxisKeepsakeAnimation.WRITE_CLOSE) {
            withBook = 1.0F - NoxisBoredAnimation.smooth((t - 4.0F) / 6.0F);
            pocket = NoxisBoredAnimation.env(t, 8, 12, 15, 20);
        } else {
            withBook = 1.0F;
        }
        boolean main = a == NoxisKeepsakeAnimation.READ_MAIN || a == NoxisKeepsakeAnimation.WRITE_MAIN;
        float think = w * NoxisKeepsakeAnimation.thinking(a, t);
        // Busca / guarda el libro en el costadito.
        float p = w * pocket;
        this.rightArm.xRot = Mth.lerp(p, this.rightArm.xRot, 0.7F);
        this.rightArm.zRot = Mth.lerp(p, this.rightArm.zRot, 0.45F);
        this.head.yRot += 0.3F * p;
        this.head.xRot += 0.15F * p;
        float b = w * withBook;
        if (!writing) {
            // ---- Leer: libro en alto con las dos patitas ----
            float line = main ? (t % NoxisKeepsakeAnimation.LINE_TICKS) / NoxisKeepsakeAnimation.LINE_TICKS : 0.0F;
            float scan = line < 0.85F ? NoxisBoredAnimation.smooth(line / 0.85F) : 1.0F - NoxisBoredAnimation.smooth((line - 0.85F) / 0.15F);
            float lineNo = main ? (int) (t / NoxisKeepsakeAnimation.LINE_TICKS) % 5 : 0.0F;
            float pageT = main ? t % NoxisKeepsakeAnimation.PAGE_TICKS : 0.0F;
            float turn = NoxisBoredAnimation.env(pageT, NoxisKeepsakeAnimation.PAGE_TICKS - 14, NoxisKeepsakeAnimation.PAGE_TICKS - 9,
                    NoxisKeepsakeAnimation.PAGE_TICKS - 7, NoxisKeepsakeAnimation.PAGE_TICKS - 1);
            float reading = b * (1.0F - NoxisKeepsakeAnimation.thinking(a, t));
            this.rightArm.z -= 0.6F * b;
            this.leftArm.z -= 0.6F * b;
            this.rightArm.yScale *= 1.0F + 0.15F * b;
            this.leftArm.yScale *= 1.0F + 0.15F * b;
            this.rightArm.xRot = Mth.lerp(b, this.rightArm.xRot, -1.35F - 0.3F * turn);
            this.rightArm.zRot = Mth.lerp(b, this.rightArm.zRot, -0.3F - 0.55F * turn);
            this.leftArm.xRot = Mth.lerp(b, this.leftArm.xRot, -1.35F);
            this.leftArm.zRot = Mth.lerp(b, this.leftArm.zRot, 0.3F);
            // La mirada recorre los renglones.
            this.head.xRot += 0.2F * b + 0.025F * lineNo * reading;
            this.head.yRot += (-0.14F + 0.28F * scan) * reading;
            this.head.zRot += 0.05F * Mth.sin(t * 0.05F) * reading;
            this.body.zRot += 0.035F * Mth.sin(t * 0.021F) * b;            // se acomoda de a poquito
            this.rightEar.zRot += 0.1F * turn * w;
            this.tail.yRot += Mth.sin(t * 0.07F) * 0.3F * b;
            // Pensando: mira para arriba, ladea la cabeza y se apoya la patita en el mentón.
            this.head.xRot -= 0.45F * think;
            this.head.zRot += 0.3F * think;
            this.leftArm.xRot = Mth.lerp(think, this.leftArm.xRot, -2.05F);
            this.leftArm.zRot = Mth.lerp(think, this.leftArm.zRot, 0.55F);
            this.rightArm.xRot += 0.25F * think;                           // el libro baja un poquito
            this.leftEar.zRot -= 0.15F * think * Mth.sin(t * 0.6F);
        } else {
            // ---- Escribir: libro sobre la falda; una patita lo sostiene, la otra escribe ----
            float lineT = main ? (t % 36.0F) / 36.0F : 0.0F;
            float across = lineT < 0.88F ? NoxisBoredAnimation.smooth(lineT / 0.88F) : 1.0F - NoxisBoredAnimation.smooth((lineT - 0.88F) / 0.12F);
            float scribbling = b * (main ? 1.0F : 0.0F) * (1.0F - NoxisKeepsakeAnimation.thinking(a, t));
            float eureka = w * NoxisBoredAnimation.pulse(main ? t % 240.0F : 0.0F, 214.0F, 4.0F);
            this.leftArm.z -= 0.3F * b;
            this.leftArm.xRot = Mth.lerp(b, this.leftArm.xRot, -0.75F);
            this.leftArm.zRot = Mth.lerp(b, this.leftArm.zRot, 0.25F);
            this.rightArm.z -= 0.5F * b;
            this.rightArm.xRot = Mth.lerp(b, this.rightArm.xRot, -0.9F + 0.08F * Mth.sin(t * 1.5F) * scribbling);
            this.rightArm.zRot = Mth.lerp(b, this.rightArm.zRot, -0.3F + 0.1F * Mth.sin(t * 0.9F) * scribbling);
            this.rightArm.yRot = Mth.lerp(b, this.rightArm.yRot, (-0.25F + 0.5F * across) * scribbling);
            this.head.xRot += 0.42F * b + 0.03F * Mth.sin(t * 0.35F) * scribbling;   // cabeza gacha, siguiendo lo que escribe
            this.head.yRot += (-0.08F + 0.16F * across) * scribbling;
            this.tail.yRot += Mth.sin(t * 0.9F) * 0.15F * scribbling;
            // Pensando: se golpea el mentón con la patita mirando para arriba... ¡y vuelve con ganas!
            this.rightArm.xRot = Mth.lerp(think, this.rightArm.xRot, -2.1F + 0.12F * Mth.sin(t * 0.8F));
            this.rightArm.zRot = Mth.lerp(think, this.rightArm.zRot, -0.6F);
            this.head.xRot -= 0.6F * think;
            this.head.zRot -= 0.25F * think;
            this.rightEar.xRot -= 0.35F * eureka;
            this.leftEar.xRot -= 0.35F * eureka;
            this.playLift(0.6F * eureka);
        }
        // El libro abierto (y el lápiz, al escribir): modelos 3D propios, en sus manitos.
        // Solo se ven mientras tiene el libro (el libro real sigue guardado en su inventario).
        if (holding) {
            this.propBook.xScale = 0.5F;
            this.propBook.yScale = 0.5F;
            this.propBook.zScale = 0.5F;
            if (!writing) {
                // Leer: abierto entre las dos patitas, con las páginas hacia su carita.
                float[] r = handTip(this.rightArm);
                float[] l = handTip(this.leftArm);
                this.propBook.x = (r[0] + l[0]) * 0.5F;
                this.propBook.y = (r[1] + l[1]) * 0.5F + 1.1F;
                this.propBook.z = Math.min(r[2], l[2]) - 1.2F;
                this.propBook.xRot = -0.8F + 0.3F * think;
                this.propBook.yRot = 0.0F;
                this.propBook.zRot = 0.04F * Mth.sin(t * 0.05F);
            } else {
                // Escribir: abierto sobre la falda, justo debajo de la punta del lápiz (con la postura
                // de escribir de base, así el libro queda quieto mientras el lápiz se mueve por la página).
                ModelPart arm = this.rightArm;
                float[] nib = pointAlong(arm.x, arm.y, arm.z, -0.9F, 0.0F, -0.3F, 3.0F * arm.yScale + 3.0F);
                this.propBook.x = nib[0] + 1.75F;
                this.propBook.y = nib[1] + 0.5F;
                this.propBook.z = nib[2];
                this.propBook.xRot = -0.2F;
                this.propBook.yRot = 0.0F;
                this.propBook.zRot = 0.0F;
                float[] h = handTip(this.rightArm);
                this.propPencil.x = h[0];
                this.propPencil.y = h[1];
                this.propPencil.z = h[2];
                this.propPencil.xRot = this.rightArm.xRot;
                this.propPencil.yRot = this.rightArm.yRot;
                this.propPencil.zRot = this.rightArm.zRot;
                this.propPencil.xScale = 0.5F;
                this.propPencil.yScale = 0.5F;
                this.propPencil.zScale = 0.5F;
            }
        }
    }

    /**
     * Mirar un objeto: se inclina hacia él estirando el cuello, ladea la cabeza de un lado al otro,
     * lo olfatea (la naricita se mueve rápido), para las orejitas hacia adelante y levanta apenas
     * una patita, curioso.
     */
    private void keepsakeInspect(float t, float w) {
        float on = w * NoxisBoredAnimation.smooth(t / 8.0F);
        this.body.xRot += 0.15F * on;
        this.head.z -= 1.0F * on;
        this.head.y += 0.4F * on;
        this.head.xRot += 0.18F * on;
        this.head.zRot += on * 0.25F * Mth.sin(t * 0.1F);
        float sniff = NoxisBoredAnimation.env(t, 14, 17, 28, 32);
        this.head.xRot += on * 0.05F * Mth.sin(t * 1.8F) * sniff;
        this.rightEar.xRot -= 0.35F * on;
        this.leftEar.xRot -= 0.35F * on;
        this.rightArm.xRot = Mth.lerp(on, this.rightArm.xRot, -0.6F);
        this.rightArm.zRot = Mth.lerp(on, this.rightArm.zRot, -0.1F);
        this.tail.yRot = Mth.lerp(on, this.tail.yRot, 0.3F);
    }

    /**
     * Contemplar un tesoro: busca en su costadito (como en un bolsillo), lo saca y lo acuna con las
     * dos patitas delante del pecho; lo mira feliz meciéndose (con un saltito de alegría), lo acerca
     * a la carita con los ojitos cerrados, lo guarda con cuidado en el costadito y le da una
     * palmadita.
     */
    private void keepsakeContemplate(float t, float w, boolean holding) {
        float reach = NoxisBoredAnimation.env(t, 0, 6, 9, 15);
        float cradle = NoxisBoredAnimation.env(t, 10, 22, 88, 98);
        float look = NoxisBoredAnimation.env(t, 22, 30, 84, 92);
        float nuzzle = NoxisBoredAnimation.env(t, 58, 64, 72, 78);
        float stow = NoxisBoredAnimation.env(t, 90, 96, 102, 108);
        float pat = NoxisBoredAnimation.pulse(t, 103.0F, 2.5F);
        // Busca en el costadito / lo guarda.
        float pocket = w * Math.max(reach, stow);
        this.rightArm.xRot = Mth.lerp(pocket, this.rightArm.xRot, 0.7F + 0.25F * pat);
        this.rightArm.zRot = Mth.lerp(pocket, this.rightArm.zRot, 0.45F);
        this.head.yRot += 0.35F * pocket;
        this.head.xRot += 0.15F * pocket;
        // Lo acuna con las dos patitas.
        float c = w * cradle;
        this.rightArm.z -= 0.8F * c;
        this.leftArm.z -= 0.8F * c;
        this.rightArm.yScale *= 1.0F + 0.2F * c;
        this.leftArm.yScale *= 1.0F + 0.2F * c;
        this.rightArm.xRot = Mth.lerp(c, this.rightArm.xRot, -1.25F - 0.35F * nuzzle);
        this.rightArm.zRot = Mth.lerp(c, this.rightArm.zRot, -0.35F);
        this.leftArm.xRot = Mth.lerp(c, this.leftArm.xRot, -1.15F - 0.3F * nuzzle);
        this.leftArm.zRot = Mth.lerp(c, this.leftArm.zRot, 0.35F);
        // Lo mira feliz, meciéndose; un saltito de alegría.
        float l = w * look;
        this.head.xRot += 0.28F * l - 0.15F * w * nuzzle;
        this.head.zRot += l * 0.12F * Mth.sin(t * 0.12F) + 0.15F * w * nuzzle;
        this.body.zRot += l * 0.04F * Mth.sin(t * 0.12F);
        this.rightEar.zRot -= 0.2F * l;
        this.leftEar.zRot += 0.2F * l;
        this.tail.yRot += l * Mth.sin(t * 0.3F) * 0.4F;
        this.playLift(w * 0.6F * NoxisBoredAnimation.pulse(t, 44.0F, 3.0F));
        // El tesoro, en sus manitos (punta de la patita derecha, un poquito adelante).
        if (holding) {
            ModelPart arm = this.rightArm;
            float len = 3.0F * arm.yScale;
            float cx = Mth.cos(arm.xRot), sx = Mth.sin(arm.xRot);
            float cy = Mth.cos(arm.yRot), sy = Mth.sin(arm.yRot);
            float cz = Mth.cos(arm.zRot), sz = Mth.sin(arm.zRot);
            float y1 = len * cx;
            float z0 = len * sx;
            float x1 = sy * z0;
            float z1 = cy * z0;
            this.flowerAnchor.x = arm.x + x1 * cz - y1 * sz + 1.0F;
            this.flowerAnchor.y = arm.y + x1 * sz + y1 * cz + 0.8F;
            this.flowerAnchor.z = arm.z + z1 - 0.8F;
            this.flowerAnchor.xRot = 0.15F;
            this.flowerAnchor.zRot = 0.0F;
        }
    }

    /** Un punto a {@code len} del hombro siguiendo esas rotaciones (ZYX), como si fuera el brazo. */
    private static float[] pointAlong(float px, float py, float pz, float xr, float yr, float zr, float len) {
        float cx = Mth.cos(xr), sx = Mth.sin(xr);
        float cy = Mth.cos(yr), sy = Mth.sin(yr);
        float cz = Mth.cos(zr), sz = Mth.sin(zr);
        float y1 = len * cx;
        float z0 = len * sx;
        float x1 = sy * z0;
        float z1 = cy * z0;
        return new float[] {px + x1 * cz - y1 * sz, py + x1 * sz + y1 * cz, pz + z1};
    }

    /** Punta de la patita (pivote del hombro + rotación ZYX del brazo aplicada a (0, largo, 0)). */
    private static float[] handTip(ModelPart arm) {
        float len = 3.0F * arm.yScale;
        float cx = Mth.cos(arm.xRot), sx = Mth.sin(arm.xRot);
        float cy = Mth.cos(arm.yRot), sy = Mth.sin(arm.yRot);
        float cz = Mth.cos(arm.zRot), sz = Mth.sin(arm.zRot);
        float y1 = len * cx;
        float z0 = len * sx;
        float x1 = sy * z0;
        float z1 = cy * z0;
        return new float[] {arm.x + x1 * cz - y1 * sz, arm.y + x1 * sz + y1 * cz, arm.z + z1};
    }

    // ================================================================== personalidades

    private void applyPersona(byte g, float t, float w) {
        switch (g) {
            case NoxisPersonaAnimation.CURIOUS -> this.personaCurious(t, w);
            case NoxisPersonaAnimation.MISCHIEF -> this.personaMischief(t, w);
            case NoxisPersonaAnimation.ARTIST -> this.personaArtist(t, w);
            case NoxisPersonaAnimation.AFFECTION -> this.personaAffection(t, w);
            case NoxisPersonaAnimation.CALM -> this.personaCalm(t, w);
            default -> { }
        }
    }

    /**
     * Curioso: sigue con la mirada algo que flota (de un lado al otro, subiendo y bajando, con el
     * cuerpito acompañando); se queda mirándolo, ladea mucho la cabeza ("¿mrr?") y mueve una
     * orejita para adelante y otra para atrás, como preguntando; la colita en signo de pregunta.
     */
    private void personaCurious(float t, float w) {
        float track = NoxisBoredAnimation.env(t, 2, 8, 34, 42);
        float ask = NoxisBoredAnimation.env(t, 36, 42, 54, 62);
        float u = Mth.clamp((t - 4.0F) / 34.0F, 0.0F, 1.0F);
        float follow = Mth.sin(u * Mth.PI * 1.5F - Mth.HALF_PI);      // izquierda → derecha → centro
        this.head.yRot += w * 0.5F * follow * track;
        this.head.xRot += w * (-0.22F * (0.5F + 0.5F * Mth.sin(t * 0.32F)) * track - 0.08F * ask);
        this.body.yRot += w * 0.12F * follow * track;
        this.head.zRot += w * 0.38F * ask;
        float flip = Mth.sin(t * 0.45F);
        this.rightEar.xRot += w * (-0.45F * ask * Math.max(0.0F, flip) + 0.3F * ask * Math.max(0.0F, -flip));
        this.leftEar.xRot += w * (0.3F * ask * Math.max(0.0F, flip) - 0.45F * ask * Math.max(0.0F, -flip));
        this.rightEar.zRot += w * 0.12F * Mth.sin(t * 0.7F) * track;
        this.tail.xRot = Mth.lerp(w * 0.8F, this.tail.xRot, 0.35F);
        this.tail.yRot = Mth.lerp(w * 0.8F, this.tail.yRot, 0.45F);
    }

    /**
     * Travieso: hombritos arriba, se frota las patitas con picardía delante del pecho y mira rápido
     * a un lado y al otro como planeando algo; en la risita ("jejeje") el cuerpo se sacude un
     * poquito; la puntita de la colita no para.
     */
    private void personaMischief(float t, float w) {
        float on = NoxisBoredAnimation.env(t, 0, 8, 58, 68);
        float rub = Mth.sin(t * 1.3F) * NoxisBoredAnimation.env(t, 6, 10, 54, 60);
        float giggle = NoxisBoredAnimation.env(t, 22, 25, 36, 40);
        float k = w * on;
        this.rightArm.y -= 0.6F * k;
        this.leftArm.y -= 0.6F * k;
        this.head.y += 0.3F * k;
        // Manitos juntas delante de la pancita (bien adelante, que se vean frotándose).
        this.rightArm.x += 1.0F * k;
        this.leftArm.x -= 1.0F * k;
        this.rightArm.z -= 1.0F * k;
        this.leftArm.z -= 1.0F * k;
        this.rightArm.yScale *= 1.0F + 0.3F * k;
        this.leftArm.yScale *= 1.0F + 0.3F * k;
        this.rightArm.xRot = Mth.lerp(k, this.rightArm.xRot, -1.15F);
        this.leftArm.xRot = Mth.lerp(k, this.leftArm.xRot, -1.15F);
        this.rightArm.zRot = Mth.lerp(k, this.rightArm.zRot, -0.1F);
        this.leftArm.zRot = Mth.lerp(k, this.leftArm.zRot, 0.1F);
        this.rightArm.yRot = Mth.lerp(k, this.rightArm.yRot, -0.4F) + w * 0.28F * rub;
        this.leftArm.yRot = Mth.lerp(k, this.leftArm.yRot, 0.4F) - w * 0.28F * rub;
        this.rightArm.xRot += w * 0.1F * rub;
        this.leftArm.xRot -= w * 0.1F * rub;
        // Mira para un lado y para el otro, rápido (como si planeara algo).
        float look = 0.45F * (NoxisBoredAnimation.env(t, 10, 13, 18, 21) - NoxisBoredAnimation.env(t, 24, 27, 32, 35)
                + 0.6F * NoxisBoredAnimation.env(t, 40, 43, 47, 50));
        this.head.yRot += w * look;
        this.head.xRot += w * 0.1F * on;
        float shake = w * giggle * Mth.sin(t * 2.5F);
        this.body.y += 0.25F * shake;
        this.head.y += 0.3F * shake;
        this.head.zRot += 0.06F * shake;
        this.tail.yRot += w * Mth.sin(t * 1.6F) * 0.3F * on;
    }

    /**
     * Artista: encuadra algo con las dos patitas como un pintor (cabeza un poco atrás, ojitos
     * entornados); después, soñador, mira para arriba y pinta pinceladas suaves en el aire con una
     * patita mientras tararea, con el cuerpo meciéndose apenas.
     */
    private void personaArtist(float t, float w) {
        float frame = NoxisBoredAnimation.env(t, 4, 12, 26, 34);
        float dream = NoxisBoredAnimation.env(t, 30, 38, 70, 80);
        float stroke = Mth.sin((t - 30.0F) * 0.18F);
        float f = w * frame;
        this.rightArm.xRot = Mth.lerp(f, this.rightArm.xRot, -1.6F);
        this.leftArm.xRot = Mth.lerp(f, this.leftArm.xRot, -1.6F);
        this.rightArm.zRot = Mth.lerp(f, this.rightArm.zRot, 0.5F);
        this.leftArm.zRot = Mth.lerp(f, this.leftArm.zRot, -0.5F);
        this.rightArm.yScale *= 1.0F + 0.2F * f;
        this.leftArm.yScale *= 1.0F + 0.2F * f;
        this.head.xRot -= 0.12F * f;
        this.head.zRot += 0.15F * f;
        this.head.z += 0.4F * f;
        float d = w * dream;
        this.rightArm.xRot = Mth.lerp(d, this.rightArm.xRot, -1.4F + 0.2F * stroke);
        this.rightArm.zRot = Mth.lerp(d, this.rightArm.zRot, 0.6F + 0.4F * stroke);
        this.leftArm.xRot = Mth.lerp(d, this.leftArm.xRot, -0.5F);
        this.leftArm.zRot = Mth.lerp(d, this.leftArm.zRot, 0.15F);
        this.head.xRot -= 0.3F * d;
        this.head.zRot += d * 0.1F * Mth.sin(t * 0.1F);
        this.body.zRot += d * 0.04F * Mth.sin(t * 0.1F);
        this.rightEar.zRot -= 0.2F * d;
        this.leftEar.zRot += 0.2F * d;
        this.tail.yRot += w * Mth.sin(t * 0.12F) * 0.4F * (frame + dream);
    }

    /**
     * Cariñoso: saltito, abre los bracitos bien grandes ("¡eeey!"), y después llama con la patita
     * ("vení") con la cabeza ladeada, los ojitos cerrados de contento y la colita a mil.
     */
    private void personaAffection(float t, float w) {
        this.playLift(w * 1.2F * NoxisBoredAnimation.pulse(t, 5.0F, 3.0F));
        float open = NoxisBoredAnimation.env(t, 3, 8, 20, 26);
        float beckon = NoxisBoredAnimation.env(t, 22, 28, 48, 56);
        float o = w * open;
        this.rightArm.zRot = Mth.lerp(o, this.rightArm.zRot, 2.1F);
        this.leftArm.zRot = Mth.lerp(o, this.leftArm.zRot, -2.1F);
        this.rightArm.xRot = Mth.lerp(o, this.rightArm.xRot, -0.2F);
        this.leftArm.xRot = Mth.lerp(o, this.leftArm.xRot, -0.2F);
        this.rightArm.yScale *= 1.0F + 0.15F * o;
        this.leftArm.yScale *= 1.0F + 0.15F * o;
        this.rightEar.xRot -= 0.3F * o;
        this.leftEar.xRot -= 0.3F * o;
        float b = w * beckon;
        this.rightArm.xRot = Mth.lerp(b, this.rightArm.xRot, -1.3F + 0.45F * Mth.sin((t - 24.0F) * 0.7F));
        this.rightArm.zRot = Mth.lerp(b, this.rightArm.zRot, -0.1F);
        this.head.zRot += 0.2F * b;
        this.tail.yRot += w * Mth.sin(t * 0.9F) * 0.6F * (open + beckon);
    }

    /**
     * Tranquilo: se balancea despacito de lado a lado, respira hondo (sube y baja), afloja los
     * hombros y deja caer los bracitos, las orejitas se relajan, cierra los ojitos contento
     * (ronroneo) y enrosca la colita.
     */
    private void personaCalm(float t, float w) {
        float on = NoxisBoredAnimation.env(t, 0, 10, 66, 80);
        float sway = Mth.sin(t * 0.16F);
        float k = w * on;
        this.body.zRot += 0.07F * sway * k;
        this.head.zRot += 0.1F * Mth.sin(t * 0.16F + 0.6F) * k;
        this.rightArm.xRot = Mth.lerp(k, this.rightArm.xRot, 0.0F);
        this.leftArm.xRot = Mth.lerp(k, this.leftArm.xRot, 0.0F);
        this.rightArm.zRot = Mth.lerp(k, this.rightArm.zRot, 0.05F + 0.06F * sway);
        this.leftArm.zRot = Mth.lerp(k, this.leftArm.zRot, -0.05F + 0.06F * sway);
        this.rightArm.y += 0.4F * k;
        this.leftArm.y += 0.4F * k;
        float breath = NoxisBoredAnimation.env(t, 18, 28, 30, 40) - 0.6F * NoxisBoredAnimation.env(t, 34, 42, 48, 56);
        this.playLift(w * 0.5F * breath);
        this.rightEar.zRot -= 0.25F * k;
        this.leftEar.zRot += 0.25F * k;
        this.head.xRot += 0.1F * w * NoxisBoredAnimation.env(t, 36, 44, 66, 76);
        this.tail.yRot = Mth.lerp(k * 0.8F, this.tail.yRot, 1.0F);
        this.tail.xRot = Mth.lerp(k * 0.6F, this.tail.xRot, 0.2F);
    }

    // ================================================================== bailar

    private static float hash(int n) {
        float x = Mth.sin(n * 12.9898F) * 43758.547F;
        return x - Mth.floor(x);
    }

    private void danceLift(float amount) {
        this.head.y -= amount;
        this.body.y -= amount;
        this.rightArm.y -= amount;
        this.leftArm.y -= amount;
    }

    /**
     * Baile: cuatro pasos que cambian cada 8 tiempos (con medio tiempo de mezcla), siempre sobre el
     * pulso. {@code b}: tiempo de la canción (en tiempos); {@code m}: lado (±1); {@code seed}: para
     * que cada tiempo salga un poquito distinto (torpeza).
     */
    private void dance(float b, float w, int variant, float m, int seed) {
        float phrase = b / 8.0F;
        int pi = Mth.floor(phrase);
        float fp = phrase - pi;
        int move = Math.floorMod(pi + variant, 4);
        int prev = Math.floorMod(pi - 1 + variant, 4);
        float blend = NoxisBoredAnimation.smooth(fp * 16.0F);
        int n = Mth.floor(b);
        float f = b - n;
        float amp = 0.85F + 0.3F * hash(n * 7 + seed);
        float side = ((n & 1) == 0 ? 1.0F : -1.0F) * m;
        this.danceMove(prev, w * (1.0F - blend), b, f, amp, side, m, seed);
        this.danceMove(move, w * blend, b, f, amp, side, m, seed);
    }

    private void danceMove(int mv, float k, float b, float f, float amp, float side, float m, int seed) {
        if (k <= 0.0F) return;
        float lift = Mth.sin(Mth.PI * f) * amp;                 // abajo en el tiempo, arriba entre tiempos
        switch (mv) {
            case 0 -> {
                // Rebote de patitas: una patita y después la otra, bracitos al revés, orejitas que rebotan.
                this.danceLift(1.1F * lift * k);
                ModelPart leg = side > 0.0F ? this.rightLeg : this.leftLeg;
                leg.xRot -= 1.0F * lift * k;
                leg.y -= 0.6F * lift * k;
                float swing = 0.42F * side * Mth.cos(Mth.PI * f) * k;
                this.rightArm.xRot += swing - 0.2F * k;
                this.leftArm.xRot -= swing + 0.2F * k;
                this.rightArm.zRot += 0.35F * k;
                this.leftArm.zRot -= 0.35F * k;
                this.rightEar.zRot += 0.3F * side * lift * k;
                this.leftEar.zRot += 0.3F * side * lift * k;
                this.head.xRot += 0.16F * (1.0F - f) * (1.0F - f) * (1.0F - f) * k;
                this.head.zRot += 0.08F * side * k;
                this.tail.yRot += 0.5F * side * k;
            }
            case 1 -> {
                // Balanceo de lado: un vaivén cada dos tiempos, bracitos abiertos, cabeza al revés.
                float sw = Mth.sin(Mth.PI * b) * m;
                this.danceLift(0.4F * lift * k);
                this.body.zRot += 0.16F * sw * k;
                this.head.zRot -= 0.24F * sw * k;
                this.head.x += 1.2F * sw * k;
                this.rightArm.zRot = Mth.lerp(k, this.rightArm.zRot, 1.25F + 0.35F * sw);
                this.leftArm.zRot = Mth.lerp(k, this.leftArm.zRot, -1.25F + 0.35F * sw);
                this.rightArm.xRot = Mth.lerp(k, this.rightArm.xRot, -0.3F);
                this.leftArm.xRot = Mth.lerp(k, this.leftArm.xRot, -0.3F);
                this.rightEar.zRot += 0.25F * sw * k;
                this.leftEar.zRot += 0.25F * sw * k;
                this.tail.yRot += 0.7F * sw * k;
                this.rightLeg.zRot += 0.12F * sw * k;
                this.leftLeg.zRot += 0.12F * sw * k;
            }
            case 2 -> {
                // Paso torpe: giro de cadera a cada tiempo, la cabeza mira al otro lado, patitas que bombean.
                float tw = side * (0.5F - 0.5F * Mth.cos(Mth.PI * Math.min(1.0F, f * 2.0F)));
                this.body.yRot += 0.38F * tw * k;
                this.head.yRot -= 0.22F * tw * k;
                this.danceLift(0.9F * lift * k);
                float pump = 0.55F * side * Mth.sin(Mth.PI * f);
                this.rightArm.xRot = Mth.lerp(k, this.rightArm.xRot, -1.15F + pump);
                this.leftArm.xRot = Mth.lerp(k, this.leftArm.xRot, -1.15F - pump);
                this.rightArm.zRot = Mth.lerp(k, this.rightArm.zRot, 0.2F);
                this.leftArm.zRot = Mth.lerp(k, this.leftArm.zRot, -0.2F);
                this.rightLeg.xRot -= 0.5F * lift * k * (side > 0.0F ? 1.0F : 0.2F);
                this.leftLeg.xRot -= 0.5F * lift * k * (side < 0.0F ? 1.0F : 0.2F);
                this.rightEar.zRot -= 0.35F * lift * k;
                this.leftEar.zRot += 0.35F * lift * k;
                this.head.zRot += 0.06F * Mth.sin(b * 2.3F + seed) * k;
            }
            default -> {
                // ¡Manitos arriba!: bracitos arriba que se menean, cabeceo, orejitas de a una, colita a mil.
                this.danceLift(0.6F * lift * k);
                float wig = Mth.sin(Mth.PI * f) * side;
                this.rightArm.zRot = Mth.lerp(k, this.rightArm.zRot, 2.15F + 0.35F * wig);
                this.leftArm.zRot = Mth.lerp(k, this.leftArm.zRot, -2.15F + 0.35F * wig);
                this.rightArm.xRot = Mth.lerp(k, this.rightArm.xRot, -0.45F);
                this.leftArm.xRot = Mth.lerp(k, this.leftArm.xRot, -0.45F);
                this.head.xRot += (0.2F * (1.0F - f) * (1.0F - f) - 0.06F) * k;
                float flick = 0.4F * NoxisBoredAnimation.pulse(f, 0.15F, 0.2F) * k;
                if (side > 0.0F) this.rightEar.zRot -= flick;
                else this.leftEar.zRot += flick;
                this.tail.yRot += Mth.sin(b * Mth.PI * 2.0F) * 0.6F * k;
            }
        }
    }

    private void applyDance(NoxisVillagerRenderState state, boolean rightBusy, boolean leftBusy) {
        // El bracito que sostiene la antorcha o el paraguas no baila (la sigue sosteniendo firme).
        float rx = this.rightArm.xRot, ry = this.rightArm.yRot, rz = this.rightArm.zRot, rY = this.rightArm.y;
        float lx = this.leftArm.xRot, ly = this.leftArm.yRot, lz = this.leftArm.zRot, lY = this.leftArm.y;
        float rS = this.rightArm.yScale, lS = this.leftArm.yScale;
        this.rootXr = this.rootYr = this.rootZr = 0.0F;
        this.rootOx = this.rootOy = this.rootOz = 0.0F;
        this.pivotX = 0.0F;
        this.pivotY = 24.0F;
        this.pivotZ = 0.0F;
        // Al caerse, el baile se apaga enseguida.
        float falling = Math.max(state.danceFallWeight, state.dancePairFallWeight);
        float danceW = state.danceWeight * (1.0F - Mth.clamp(falling * 3.0F, 0.0F, 1.0F));
        if (danceW > 0.0F) {
            // Cambio de coreografía (por ejemplo, de solo a pareja): mezcla suave de una a la otra.
            float blend = state.danceStyleBlend;
            if (blend < 1.0F) this.danceRoutine(state.dancePrevStyle, danceW * (1.0F - blend), state);
            this.danceRoutine(state.danceStyle, danceW * blend, state);
        }
        if (state.danceFallWeight > 0.0F) {
            this.slip(state.danceFallTime, state.danceFallWeight, state.danceMirror);
        }
        if (state.dancePairFallWeight > 0.0F) {
            this.pairFall(state.dancePairFallTime, state.dancePairFallWeight, state.dancePairRole);
        }
        if (rightBusy) {
            this.rightArm.xRot = rx;
            this.rightArm.yRot = ry;
            this.rightArm.zRot = rz;
            this.rightArm.y = rY;
            this.rightArm.yScale = rS;
        }
        if (leftBusy) {
            this.leftArm.xRot = lx;
            this.leftArm.yRot = ly;
            this.leftArm.zRot = lz;
            this.leftArm.y = lY;
            this.leftArm.yScale = lS;
        }
        this.applyRoot();
    }

    // Raíz del modelo (todo el Noxis): giros y desplazamientos del baile alrededor de un pivote.
    private float rootXr, rootYr, rootZr, rootOx, rootOy, rootOz, pivotX, pivotY, pivotZ;

    /** Gira toda la raíz (orden ZYX, como el juego) alrededor del pivote y le suma el desplazamiento. */
    private void applyRoot() {
        if (this.rootXr == 0.0F && this.rootYr == 0.0F && this.rootZr == 0.0F
                && this.rootOx == 0.0F && this.rootOy == 0.0F && this.rootOz == 0.0F) return;
        float cx = Mth.cos(this.rootXr), sx = Mth.sin(this.rootXr);
        float cy = Mth.cos(this.rootYr), sy = Mth.sin(this.rootYr);
        float cz = Mth.cos(this.rootZr), sz = Mth.sin(this.rootZr);
        float x1 = this.pivotX;
        float y1 = this.pivotY * cx - this.pivotZ * sx;
        float z1 = this.pivotY * sx + this.pivotZ * cx;
        float x2 = x1 * cy + z1 * sy;
        float z2 = -x1 * sy + z1 * cy;
        float x3 = x2 * cz - y1 * sz;
        float y3 = x2 * sz + y1 * cz;
        this.rootPart.xRot = this.rootXr;
        this.rootPart.yRot = this.rootYr;
        this.rootPart.zRot = this.rootZr;
        this.rootPart.x = this.pivotX - x3 + this.rootOx;
        this.rootPart.y = this.pivotY - y3 + this.rootOy;
        this.rootPart.z = this.pivotZ - z2 + this.rootOz;
    }

    /**
     * Vueltas completas del baile: devuelve el mismo giro llevado al rango -π..π (una vuelta entera
     * vale 0). Así, cuando la vuelta termina, o cuando el baile se apaga o cambia de coreografía,
     * el Noxis queda mirando hacia donde miraba, sin "desgirar" para el lado contrario.
     */
    private static float turnAngle(float a) {
        float full = 2.0F * Mth.PI;
        return a - full * Math.round(a / full);
    }

    /** Mitad de la distancia entre los dos de la pareja (px): el centro de la ronda queda adelante. */
    private static final float PAIR_HALF = 6.8F;

    /** Los dos de la pareja giran juntos alrededor del centro (cada uno lo ve adelante suyo). */
    private void pairOrbit(float phi) {
        this.rootYr += phi;
        this.rootOx += -PAIR_HALF * Mth.sin(phi);
        this.rootOz += -PAIR_HALF + PAIR_HALF * Mth.cos(phi);
    }

    private void danceRoutine(byte style, float w, NoxisVillagerRenderState state) {
        if (w <= 0.0F) return;
        float m = NoxisDanceAnimation.mirrored(style) ? -1.0F : 1.0F;
        int role = NoxisDanceAnimation.mirrored(style) ? 1 : 0;
        // El retraso propio solo en los bailes solos (en pareja van exactamente juntos).
        float beat = state.danceBeatRaw - (NoxisDanceAnimation.isPair(style) ? 0.0F : NoxisDanceAnimation.lag(style) / state.danceBeatTicks);
        switch (NoxisDanceAnimation.routine(style)) {
            case NoxisDanceAnimation.SOLO_A -> this.dance(beat, w, NoxisDanceAnimation.variant(style), m, state.danceSeed);
            case NoxisDanceAnimation.SOLO_B -> this.soloB(beat, w, m, state.danceSeed);
            case NoxisDanceAnimation.PAIR_RING -> this.pairRing(beat, w, role);
            default -> this.pairHops(beat, w, role);
        }
    }

    // ---------------------------------------------------------------- baile solo B

    /**
     * Baile solo B (frases de 8 tiempos): pasito de pingüino (se corre de lado a lado, bracitos
     * duros que aletean, cabecita que va y viene), vueltita y aplausos (una vuelta en su lugar y
     * después aplaude en cada tiempo) y sentadillas felices (baja en cada tiempo y levanta un
     * bracito al cielo, primero uno y después el otro).
     */
    private void soloB(float b, float w, float m, int seed) {
        int ph = Mth.floor(b / 8.0F);
        float q = b - 8.0F * ph;
        int mv = Math.floorMod(ph, 3);
        int prev = Math.floorMod(ph - 1, 3);
        float bl = NoxisBoredAnimation.smooth(q * 2.0F);
        this.soloBMove(prev, w * (1.0F - bl), b, q + 8.0F, m);
        this.soloBMove(mv, w * bl, b, q, m);
    }

    private void soloBMove(int mv, float k, float b, float q, float m) {
        if (k <= 0.0F) return;
        int n = Mth.floor(b);
        float f = b - n;
        float side = ((n & 1) == 0 ? 1.0F : -1.0F) * m;
        switch (mv) {
            case 0 -> {
                this.rootOx += 1.8F * Mth.sin(Mth.PI * b / 2.0F) * m * k;
                this.body.zRot += 0.14F * side * k;
                this.head.zRot -= 0.1F * side * k;
                ModelPart leg = side > 0.0F ? this.rightLeg : this.leftLeg;
                leg.zRot += (side > 0.0F ? 1.0F : -1.0F) * 0.45F * Mth.sin(Mth.PI * f) * k;
                float flap = 0.45F + 0.35F * Math.abs(Mth.sin(2.0F * Mth.PI * b));
                this.rightArm.zRot = Mth.lerp(k, this.rightArm.zRot, flap);
                this.leftArm.zRot = Mth.lerp(k, this.leftArm.zRot, -flap);
                this.rightArm.xRot = Mth.lerp(k, this.rightArm.xRot, 0.15F);
                this.leftArm.xRot = Mth.lerp(k, this.leftArm.xRot, 0.15F);
                float peck = (1.0F - f) * (1.0F - f);
                this.head.z -= 1.1F * peck * k;
                this.rightEar.xRot -= 0.3F * peck * k;
                this.leftEar.xRot -= 0.3F * peck * k;
                this.tail.yRot += 0.5F * side * k;
                this.danceLift(0.35F * Mth.sin(Mth.PI * f) * k);
            }
            case 1 -> {
                float spin = q < 4.0F ? NoxisBoredAnimation.smooth(q / 4.0F) : 1.0F;
                this.rootYr += turnAngle(2.0F * Mth.PI * m * spin) * k;
                float sp = NoxisBoredAnimation.env(q, 0.0F, 0.5F, 3.5F, 4.2F);
                float cl = NoxisBoredAnimation.env(q, 3.8F, 4.3F, 7.6F, 8.0F);
                this.rightArm.zRot = Mth.lerp(k * sp, this.rightArm.zRot, 1.45F);
                this.leftArm.zRot = Mth.lerp(k * sp, this.leftArm.zRot, -1.45F);
                this.danceLift((0.9F * NoxisBoredAnimation.pulse(q, 0.5F, 0.5F) + 0.9F * NoxisBoredAnimation.pulse(q, 3.6F, 0.5F)) * k);
                this.rightEar.zRot -= 0.35F * sp * k;
                this.leftEar.zRot += 0.35F * sp * k;
                float clap = 1.0F - Math.abs(Mth.sin(Mth.PI * f));
                this.rightArm.xRot = Mth.lerp(k * cl, this.rightArm.xRot, -1.35F);
                this.leftArm.xRot = Mth.lerp(k * cl, this.leftArm.xRot, -1.35F);
                this.rightArm.yRot = Mth.lerp(k * cl, this.rightArm.yRot, -0.15F - 0.45F * clap);
                this.leftArm.yRot = Mth.lerp(k * cl, this.leftArm.yRot, 0.15F + 0.45F * clap);
                this.rightArm.zRot = Mth.lerp(k * cl, this.rightArm.zRot, -0.1F);
                this.leftArm.zRot = Mth.lerp(k * cl, this.leftArm.zRot, 0.1F);
                float c3 = clap * clap * clap;
                this.head.xRot += 0.14F * c3 * cl * k;
                this.rightEar.xRot -= 0.4F * c3 * clap * cl * k;
                this.leftEar.xRot -= 0.4F * c3 * clap * cl * k;
                this.tail.yRot += Mth.sin(b * Mth.PI * 2.0F) * 0.5F * cl * k;
            }
            default -> {
                float c = Mth.cos(Mth.PI * f);
                float dip = c * c;
                this.head.y += 1.6F * dip * k;
                this.body.y += 1.4F * dip * k;
                this.rightArm.y += 1.4F * dip * k;
                this.leftArm.y += 1.4F * dip * k;
                float up = Mth.sin(Mth.PI * Math.min(1.0F, f * 1.6F));
                ModelPart arm = side > 0.0F ? this.rightArm : this.leftArm;
                ModelPart other = side > 0.0F ? this.leftArm : this.rightArm;
                float s = side > 0.0F ? 1.0F : -1.0F;
                arm.zRot = Mth.lerp(k, arm.zRot, s * (1.0F + 1.75F * up));
                arm.xRot = Mth.lerp(k, arm.xRot, -0.25F);
                other.zRot = Mth.lerp(k, other.zRot, -s * 0.3F);
                other.xRot = Mth.lerp(k, other.xRot, -0.6F);
                this.head.zRot += 0.18F * side * up * k;
                this.rightEar.xRot += (0.4F * dip - 0.25F * up) * k;
                this.leftEar.xRot += (0.4F * dip - 0.25F * up) * k;
                this.tail.xRot = Mth.lerp(k, this.tail.xRot, 1.1F - 0.5F * dip);
            }
        }
    }

    // ---------------------------------------------------------------- bailes en pareja

    /**
     * Pareja 1, ronda de manitos: tomados de las dos manitos, 4 tiempos de vaivén juntos y después
     * una vuelta completa los dos alrededor del centro, con saltitos y los ojitos cerrados.
     * {@code role}: 0 = el que propuso, 1 = el compañero (frente a frente, se mueven en espejo).
     */
    private void pairRing(float b, float w, int role) {
        float ws = role == 0 ? 1.0F : -1.0F;
        int ph = Mth.floor(b / 8.0F);
        float q = b - 8.0F * ph;
        float f = b - Mth.floor(b);
        this.rightArm.xRot = Mth.lerp(w, this.rightArm.xRot, -1.45F);
        this.leftArm.xRot = Mth.lerp(w, this.leftArm.xRot, -1.45F);
        this.rightArm.zRot = Mth.lerp(w, this.rightArm.zRot, -0.12F);
        this.leftArm.zRot = Mth.lerp(w, this.leftArm.zRot, 0.12F);
        this.rightArm.yScale *= 1.0F + 0.3F * w;
        this.leftArm.yScale *= 1.0F + 0.3F * w;
        float sway = NoxisBoredAnimation.env(q, 0.0F, 0.5F, 3.6F, 4.2F);
        float spin = NoxisBoredAnimation.env(q, 3.8F, 4.3F, 7.6F, 8.0F);
        float s = Mth.sin(Mth.PI * b) * ws;
        this.body.zRot += 0.15F * s * sway * w;
        this.head.zRot += 0.22F * s * sway * w;
        this.rightEar.zRot += 0.25F * s * sway * w;
        this.leftEar.zRot += 0.25F * s * sway * w;
        this.danceLift(0.8F * Mth.sin(Mth.PI * f) * sway * w);
        this.tail.yRot += 0.6F * s * sway * w;
        float u = q < 4.0F ? 0.0F : NoxisBoredAnimation.smooth((q - 4.0F) / 4.0F);
        this.pairOrbit(turnAngle(2.0F * Mth.PI * u) * w);
        this.danceLift(0.9F * Math.abs(Mth.sin(Mth.PI * b)) * spin * w);
        this.head.xRot -= 0.18F * spin * w;
        this.rightEar.zRot -= 0.5F * spin * w;
        this.leftEar.zRot += 0.5F * spin * w;
        this.rightEar.xRot += 0.2F * spin * w;
        this.leftEar.xRot += 0.2F * spin * w;
        this.tail.xRot = Mth.lerp(spin * w, this.tail.xRot, 0.3F);
    }

    /**
     * Pareja 2: saltitos alternados (uno en el tiempo, el otro a contratiempo), pasos al costado
     * los dos hacia el mismo lado, un giro en su lugar al mismo tiempo y "¡ta-da!" con el bracito
     * de adentro arriba y un saltito.
     */
    private void pairHops(float b, float w, int role) {
        float ws = role == 0 ? 1.0F : -1.0F;
        int ph = Mth.floor(b / 8.0F);
        float q = b - 8.0F * ph;
        int n = Mth.floor(b);
        float f = b - n;
        float hopP = NoxisBoredAnimation.env(q, -0.2F, 0.2F, 1.8F, 2.1F);
        float stepP = NoxisBoredAnimation.env(q, 1.8F, 2.2F, 3.8F, 4.1F);
        float turnP = NoxisBoredAnimation.env(q, 3.8F, 4.2F, 5.8F, 6.1F);
        float taP = NoxisBoredAnimation.env(q, 5.8F, 6.2F, 7.7F, 8.2F);
        float hb = role == 0 ? Math.abs(Mth.sin(Mth.PI * b)) : Math.abs(Mth.cos(Mth.PI * b));
        this.danceLift(1.6F * hb * hopP * w);
        this.rightArm.zRot = Mth.lerp(hopP * w, this.rightArm.zRot, 0.3F + 1.6F * hb);
        this.leftArm.zRot = Mth.lerp(hopP * w, this.leftArm.zRot, -0.3F - 1.6F * hb);
        this.rightEar.xRot -= 0.4F * hb * hopP * w;
        this.leftEar.xRot -= 0.4F * hb * hopP * w;
        float st = Mth.sin(Mth.PI * Mth.clamp((q - 2.0F) / 2.0F, 0.0F, 1.0F));
        this.rootOx += 2.6F * ws * st * stepP * w;
        ModelPart leg = (n & 1) == 0 ? this.rightLeg : this.leftLeg;
        leg.zRot += ((n & 1) == 0 ? 1.0F : -1.0F) * 0.4F * Mth.sin(Mth.PI * f) * stepP * w;
        this.body.zRot += 0.12F * ws * st * stepP * w;
        this.head.zRot -= 0.15F * ws * st * stepP * w;
        this.rightArm.xRot += 0.4F * Mth.sin(Mth.PI * b) * stepP * w;
        this.leftArm.xRot -= 0.4F * Mth.sin(Mth.PI * b) * stepP * w;
        // Giro coordinado: una vuelta entera, con arranque y frenada suaves (q 4 a 6). La vuelta no
        // se multiplica por turnP: si no, al apagarse el giro "desgiraban" de golpe para el otro lado.
        this.rootYr += turnAngle(2.0F * Mth.PI * ws * NoxisBoredAnimation.smooth((q - 4.0F) / 2.0F)) * w;
        this.rightArm.zRot = Mth.lerp(turnP * w, this.rightArm.zRot, 1.3F);
        this.leftArm.zRot = Mth.lerp(turnP * w, this.leftArm.zRot, -1.3F);
        ModelPart inner = role == 0 ? this.rightArm : this.leftArm;
        ModelPart outer = role == 0 ? this.leftArm : this.rightArm;
        inner.zRot = Mth.lerp(taP * w, inner.zRot, (role == 0 ? 1.0F : -1.0F) * 2.4F);
        inner.xRot = Mth.lerp(taP * w, inner.xRot, -0.9F);
        outer.zRot = Mth.lerp(taP * w, outer.zRot, (role == 0 ? -1.0F : 1.0F) * 0.9F);
        this.danceLift(1.2F * NoxisBoredAnimation.pulse(q, 7.0F, 0.6F) * w);
        this.tail.yRot += Mth.sin(b * Mth.PI * 2.0F) * 0.6F * taP * w;
    }

    // ---------------------------------------------------------------- caídas

    /** Inclinación hacia atrás desde atrás de las patitas (ya sentado: la colita en el piso). */
    private void tipBack(float angle, float drop, float w) {
        this.rootXr += angle * w;
        this.rootOy += drop * w;
        this.pivotX = 0.0F;
        this.pivotY = 22.0F;
        this.pivotZ = 2.5F;
    }

    /**
     * Resbalón (baile solo): se entusiasma con un molinete, ¡resbala! (las patitas se van para
     * adelante, bracitos y orejitas arriba), cae sentado con un rebotito sobre la colita, rueda de
     * espaldas y se mece como tortuguita pedaleando, se sienta, se rasca la cabecita de vergüenza,
     * se levanta con un saltito y se sacude la colita mirando a los costados.
     */
    private void slip(float t, float w, float m) {
        float over = NoxisBoredAnimation.env(t, 0, 3, 7, 10);
        ModelPart kickLeg = m > 0.0F ? this.rightLeg : this.leftLeg;
        ModelPart armA = m > 0.0F ? this.rightArm : this.leftArm;
        float wind = -2.6F + 3.2F * NoxisBoredAnimation.smooth(t / 8.0F);
        armA.xRot = Mth.lerp(over * w, armA.xRot, wind);
        armA.zRot = Mth.lerp(over * w, armA.zRot, (m > 0.0F ? 1.0F : -1.0F) * 0.5F);
        this.body.xRot += 0.2F * over * w;
        kickLeg.xRot -= 1.2F * NoxisBoredAnimation.env(t, 2, 5, 8, 10) * w;
        float slipK = NoxisBoredAnimation.env(t, 8, 12, 66, 72);
        this.rightLeg.xRot = Mth.lerp(slipK * w, this.rightLeg.xRot, -1.75F);
        this.leftLeg.xRot = Mth.lerp(slipK * w, this.leftLeg.xRot, -1.75F);
        float flail = NoxisBoredAnimation.env(t, 8, 11, 20, 26);
        this.rightArm.zRot = Mth.lerp(flail * w, this.rightArm.zRot, 2.5F + 0.3F * Mth.sin(t * 1.3F));
        this.leftArm.zRot = Mth.lerp(flail * w, this.leftArm.zRot, -2.5F + 0.3F * Mth.sin(t * 1.3F + 1.0F));
        this.rightArm.xRot = Mth.lerp(flail * w, this.rightArm.xRot, -0.5F);
        this.leftArm.xRot = Mth.lerp(flail * w, this.leftArm.xRot, -0.5F);
        this.rightEar.xRot -= 0.6F * NoxisBoredAnimation.env(t, 10, 14, 20, 26) * w;
        this.leftEar.xRot -= 0.6F * NoxisBoredAnimation.env(t, 11, 15, 20, 26) * w;
        float a;
        float drop;
        if (t < 8.0F) {
            a = 0.0F;
            drop = 0.0F;
        } else if (t < 15.0F) {
            float u = (t - 8.0F) / 7.0F;
            a = -0.55F * u * u;
            drop = 2.0F * u * u;
        } else if (t < 19.0F) {
            float u = (t - 15.0F) / 4.0F;
            a = -0.55F + 0.12F * Mth.sin(Mth.PI * u);
            drop = 2.0F - 0.8F * Mth.sin(Mth.PI * u);
        } else if (t < 27.0F) {
            a = -0.55F - 0.8F * NoxisBoredAnimation.smooth((t - 19.0F) / 8.0F);
            drop = 2.0F;
        } else if (t < 48.0F) {
            a = -1.35F + 0.16F * Mth.sin((t - 27.0F) * 0.45F) * Math.max(0.0F, 1.0F - (t - 27.0F) / 18.0F);
            drop = 2.0F;
        } else if (t < 58.0F) {
            a = -1.35F + 1.0F * NoxisBoredAnimation.smooth((t - 48.0F) / 10.0F);
            drop = 2.0F;
        } else if (t < 66.0F) {
            a = -0.35F;
            drop = 2.0F;
        } else if (t < 74.0F) {
            float u = NoxisBoredAnimation.smooth((t - 66.0F) / 8.0F);
            a = -0.35F * (1.0F - u);
            drop = 2.0F * (1.0F - u);
        } else {
            a = 0.0F;
            drop = 0.0F;
        }
        this.tipBack(a, drop, w);
        float back = NoxisBoredAnimation.env(t, 20, 26, 46, 54);
        this.rightLeg.xRot -= 0.5F * back * w * (1.0F + 0.3F * Mth.sin(t * 0.9F));
        this.leftLeg.xRot -= 0.5F * back * w * (1.0F + 0.3F * Mth.sin(t * 0.9F + Mth.PI));
        this.rightArm.zRot = Mth.lerp(back * w, this.rightArm.zRot, 1.0F + 0.15F * Mth.sin(t * 0.4F));
        this.leftArm.zRot = Mth.lerp(back * w, this.leftArm.zRot, -1.0F - 0.15F * Mth.sin(t * 0.4F));
        this.head.zRot += 0.12F * Mth.sin((t - 30.0F) * 0.35F) * NoxisBoredAnimation.env(t, 30, 36, 44, 50) * w;
        this.tail.xRot = Mth.lerp(back * w, this.tail.xRot, 0.1F);
        float shy = NoxisBoredAnimation.env(t, 54, 58, 64, 68);
        ModelPart scratch = m > 0.0F ? this.leftArm : this.rightArm;
        scratch.zRot = Mth.lerp(shy * w, scratch.zRot, (m > 0.0F ? -1.0F : 1.0F) * 2.55F + 0.15F * Mth.sin(t * 1.5F));
        scratch.xRot = Mth.lerp(shy * w, scratch.xRot, -0.3F);
        this.head.zRot += (m > 0.0F ? -1.0F : 1.0F) * 0.15F * shy * w;
        this.head.xRot += 0.12F * shy * w;
        this.rightEar.xRot += 0.5F * shy * w;
        this.leftEar.xRot += 0.5F * shy * w;
        this.danceLift(1.0F * NoxisBoredAnimation.pulse(t, 73.0F, 2.5F) * w);
        float dust = NoxisBoredAnimation.env(t, 72, 74, 78, 80);
        ModelPart brush = m > 0.0F ? this.rightArm : this.leftArm;
        brush.xRot = Mth.lerp(dust * w, brush.xRot, 0.7F + 0.4F * Mth.sin((t - 72.0F) * 1.7F));
        this.head.yRot += 0.4F * Mth.sin((t - 72.0F) * 0.6F) * NoxisBoredAnimation.env(t, 72, 74, 79, 80) * w;
    }

    /**
     * Caída en pareja (una sola secuencia para los dos): la ronda se acelera de más, se sueltan y
     * trastabillan para atrás, caen sentados y ruedan de espaldas mareados, se sientan, se miran y
     * se ríen, se estiran las manitos para ayudarse a levantar y terminan con un saltito contento.
     */
    private void pairFall(float t, float w, int role) {
        float fast = NoxisBoredAnimation.env(t, 0, 2, 10, 13);
        this.rightArm.xRot = Mth.lerp(fast * w, this.rightArm.xRot, -1.5F);
        this.leftArm.xRot = Mth.lerp(fast * w, this.leftArm.xRot, -1.5F);
        this.rightArm.yScale *= 1.0F + 0.3F * fast * w;
        this.leftArm.yScale *= 1.0F + 0.3F * fast * w;
        this.pairOrbit(turnAngle(4.0F * Mth.PI * NoxisBoredAnimation.smooth(t / 12.0F)) * w);
        this.rightEar.zRot -= 0.7F * fast * w;
        this.leftEar.zRot += 0.7F * fast * w;
        this.head.xRot -= 0.25F * fast * w;
        float letGo = NoxisBoredAnimation.env(t, 11, 13, 18, 22);
        this.rightArm.zRot = Mth.lerp(letGo * w, this.rightArm.zRot, 2.3F + 0.3F * Mth.sin(t * 1.4F));
        this.leftArm.zRot = Mth.lerp(letGo * w, this.leftArm.zRot, -2.3F - 0.3F * Mth.sin(t * 1.4F));
        float back = t < 12.0F ? 0.0F : t < 18.0F ? NoxisBoredAnimation.smooth((t - 12.0F) / 6.0F)
                : t < 68.0F ? 1.0F : 1.0F - NoxisBoredAnimation.smooth((t - 68.0F) / 10.0F);
        this.rootOz += 2.5F * back * w;
        float a;
        float drop;
        if (t < 14.0F) {
            a = 0.0F;
            drop = 0.0F;
        } else if (t < 20.0F) {
            float u = (t - 14.0F) / 6.0F;
            a = -0.5F * u * u;
            drop = 2.0F * u * u;
        } else if (t < 24.0F) {
            float u = (t - 20.0F) / 4.0F;
            a = -0.5F + 0.1F * Mth.sin(Mth.PI * u);
            drop = 2.0F - 0.7F * Mth.sin(Mth.PI * u);
        } else if (t < 31.0F) {
            a = -0.5F - 0.8F * NoxisBoredAnimation.smooth((t - 24.0F) / 7.0F);
            drop = 2.0F;
        } else if (t < 48.0F) {
            a = -1.3F;
            drop = 2.0F;
        } else if (t < 57.0F) {
            a = -1.3F + 0.95F * NoxisBoredAnimation.smooth((t - 48.0F) / 9.0F);
            drop = 2.0F;
        } else if (t < 68.0F) {
            a = -0.35F;
            drop = 2.0F;
        } else if (t < 80.0F) {
            float u = NoxisBoredAnimation.smooth((t - 68.0F) / 12.0F);
            a = -0.35F * (1.0F - u);
            drop = 2.0F * (1.0F - u);
        } else {
            a = 0.0F;
            drop = 0.0F;
        }
        this.tipBack(a, drop, w);
        float legs = NoxisBoredAnimation.env(t, 15, 19, 72, 80);
        this.rightLeg.xRot = Mth.lerp(legs * w, this.rightLeg.xRot, -1.7F);
        this.leftLeg.xRot = Mth.lerp(legs * w, this.leftLeg.xRot, -1.7F);
        float lying = NoxisBoredAnimation.env(t, 24, 30, 46, 54);
        this.rightLeg.xRot -= 0.45F * lying * w;
        this.leftLeg.xRot -= 0.45F * lying * w;
        this.head.zRot += 0.2F * Mth.sin(t * 0.5F + role * 1.7F) * lying * w;
        this.head.xRot += 0.1F * Mth.cos(t * 0.5F + role * 1.7F) * lying * w;
        this.rightArm.zRot = Mth.lerp(lying * w, this.rightArm.zRot, 1.1F);
        this.leftArm.zRot = Mth.lerp(lying * w, this.leftArm.zRot, -1.1F);
        this.rightEar.zRot -= 0.3F * lying * w;
        this.leftEar.zRot += 0.3F * lying * w;
        float laugh = NoxisBoredAnimation.env(t, 56, 59, 66, 69);
        this.danceLift(0.35F * Math.abs(Mth.sin(t * 1.9F)) * laugh * w);
        this.rightArm.xRot = Mth.lerp(laugh * w, this.rightArm.xRot, -0.7F);
        this.leftArm.xRot = Mth.lerp(laugh * w, this.leftArm.xRot, -0.7F);
        this.rightArm.zRot = Mth.lerp(laugh * w, this.rightArm.zRot, -0.2F);
        this.leftArm.zRot = Mth.lerp(laugh * w, this.leftArm.zRot, 0.2F);
        this.rightEar.zRot += 0.15F * Mth.sin(t * 1.9F) * laugh * w;
        this.leftEar.zRot -= 0.15F * Mth.sin(t * 1.9F) * laugh * w;
        this.head.xRot -= 0.12F * laugh * w;
        float help = NoxisBoredAnimation.env(t, 66, 70, 78, 82);
        this.rightArm.xRot = Mth.lerp(help * w, this.rightArm.xRot, -1.5F);
        this.leftArm.xRot = Mth.lerp(help * w, this.leftArm.xRot, -1.5F);
        this.rightArm.yScale *= 1.0F + 0.3F * help * w;
        this.leftArm.yScale *= 1.0F + 0.3F * help * w;
        this.rightArm.zRot = Mth.lerp(help * w, this.rightArm.zRot, -0.12F);
        this.leftArm.zRot = Mth.lerp(help * w, this.leftArm.zRot, 0.12F);
        this.danceLift(1.1F * NoxisBoredAnimation.pulse(t, 84.0F, 2.5F) * w);
        this.rightEar.zRot -= 0.3F * NoxisBoredAnimation.pulse(t, 86.0F, 3.0F) * w;
        this.leftEar.zRot += 0.3F * NoxisBoredAnimation.pulse(t, 86.0F, 3.0F) * w;
        this.tail.yRot += Mth.sin(t * 0.9F) * 0.5F * NoxisBoredAnimation.env(t, 80, 82, 88, 90) * w;
    }

    // ================================================================== escuchar sonidos

    private void applyListen(float[] w, float[] t, float dir, float side, boolean rightBusy, boolean leftBusy) {
        if (w[NoxisListenAnimation.PERK] > 0.0F) {
            this.listenPerk(t[NoxisListenAnimation.PERK], w[NoxisListenAnimation.PERK], dir, side, rightBusy, leftBusy);
        }
        if (w[NoxisListenAnimation.INVESTIGATE] > 0.0F) {
            this.listenWalk(t[NoxisListenAnimation.INVESTIGATE], w[NoxisListenAnimation.INVESTIGATE], dir);
        }
        if (w[NoxisListenAnimation.PEEK] > 0.0F) {
            this.listenPeek(t[NoxisListenAnimation.PEEK], w[NoxisListenAnimation.PEEK], rightBusy, leftBusy);
        }
    }

    /**
     * Escuchar: respingo chiquito ("¡eh!"), orejitas paradas, adelantadas y giradas hacia el
     * sonido (la de ese lado gira más; la otra se echa atrás); la cabeza lo busca con un vaivén que
     * se apaga (el giro real lo hace el servidor); pausa de escucha con la cabeza ladeada, la patita
     * de ese lado junto a la carita, colita alerta y quieta y tiquitos de oreja; cierra con un
     * cabeceo ("ajá"). {@code dir}: hacia dónde está el sonido respecto de la cabeza (-1..1);
     * {@code side}: de qué lado quedó al empezar (+1 = su derecha).
     */
    private void listenPerk(float t, float w, float dir, float side, boolean rightBusy, boolean leftBusy) {
        float perk = NoxisBoredAnimation.env(t, 0, 3, 46, 56);
        float orient = NoxisBoredAnimation.env(t, 1, 6, 46, 56);
        float search = NoxisBoredAnimation.env(t, 8, 11, 20, 26);
        float hold = NoxisBoredAnimation.env(t, 16, 22, 44, 54);
        float sg = side >= 0.0F ? 1.0F : -1.0F;
        // 1) ¡eh!: un respingo chiquito.
        float jolt = NoxisBoredAnimation.pulse(t, 2.0F, 2.5F) * 0.6F * w;
        this.head.y -= jolt;
        this.body.y -= jolt * 0.6F;
        this.rightArm.y -= jolt * 0.6F;
        this.leftArm.y -= jolt * 0.6F;
        // 2) Orejitas: derechitas, más altas, adelantadas y giradas hacia el sonido.
        float k = w * perk;
        this.rightEar.zRot = Mth.lerp(k, this.rightEar.zRot, -0.04F);
        this.leftEar.zRot = Mth.lerp(k, this.leftEar.zRot, 0.04F);
        this.rightEar.xRot -= 0.3F * k;
        this.leftEar.xRot -= 0.3F * k;
        this.rightEar.y -= 0.9F * k;
        this.leftEar.y -= 0.9F * k;
        ModelPart near = sg > 0.0F ? this.rightEar : this.leftEar;
        ModelPart far = sg > 0.0F ? this.leftEar : this.rightEar;
        float a = Math.max(Math.min(1.0F, Math.abs(dir)), 0.6F * hold);
        near.yRot += w * orient * (1.05F * dir + 0.3F * sg * hold);
        far.yRot += w * orient * 0.6F * dir;
        far.xRot += w * orient * 0.55F * a;
        near.zRot -= sg * w * orient * 0.25F * a;
        float ticks = NoxisBoredAnimation.pulse(t, 28.0F, 1.6F) + NoxisBoredAnimation.pulse(t, 31.0F, 1.6F)
                + NoxisBoredAnimation.pulse(t, 38.0F, 1.6F);
        near.zRot -= sg * w * 0.28F * ticks;
        // 3) La cabeza busca: vaivén corto que se apaga.
        this.head.yRot += w * 0.2F * Mth.sin((t - 8.0F) * 0.75F) * search * Math.max(0.0F, 1.0F - (t - 8.0F) / 18.0F);
        // 4) Pausa de escucha: cabeza ladeada, quieto, apenas inclinado.
        float h = w * hold;
        this.head.zRot += 0.22F * sg * h;
        this.head.xRot -= 0.05F * h;
        this.body.zRot += 0.04F * sg * h;
        this.body.xRot += 0.05F * h;
        // La patita de ese lado, levantada junto a la carita (si ese bracito está libre).
        boolean busy = sg > 0.0F ? rightBusy : leftBusy;
        if (!busy) {
            ModelPart arm = sg > 0.0F ? this.rightArm : this.leftArm;
            float p = w * NoxisBoredAnimation.env(t, 14, 22, 44, 54);
            arm.zRot = Mth.lerp(p, arm.zRot, sg * 2.1F);
            arm.xRot = Mth.lerp(p, arm.xRot, -0.25F);
            arm.yRot = Mth.lerp(p, arm.yRot, -sg * 0.5F);
        }
        // Colita alerta: arriba y quieta.
        this.tail.xRot = Mth.lerp(w * perk * 0.85F, this.tail.xRot, 1.25F);
        this.tail.yRot = Mth.lerp(w * hold, this.tail.yRot, 0.0F);
        // 5) Ajá: cabeceo chiquito.
        this.head.xRot += w * 0.14F * NoxisBoredAnimation.pulse(t, 51.0F, 3.0F);
    }

    /** Ir a investigar: cuello estirado, orejitas bien adelante, patitas al frente; cada tanto para una orejita. */
    private void listenWalk(float t, float w, float dir) {
        this.rightEar.zRot = Mth.lerp(w, this.rightEar.zRot, -0.08F);
        this.leftEar.zRot = Mth.lerp(w, this.leftEar.zRot, 0.08F);
        this.rightEar.xRot -= 0.5F * w;
        this.leftEar.xRot -= 0.5F * w;
        this.rightEar.yRot += 0.35F * dir * w;
        this.leftEar.yRot += 0.35F * dir * w;
        this.head.z -= 0.9F * w;
        this.head.xRot += 0.06F * w;
        this.body.xRot += 0.12F * w;
        this.rightArm.xRot -= 0.45F * w;
        this.leftArm.xRot -= 0.45F * w;
        this.rightArm.zRot -= 0.12F * w;
        this.leftArm.zRot += 0.12F * w;
        this.tail.xRot = Mth.lerp(w * 0.8F, this.tail.xRot, 0.15F);
        this.tail.yRot = Mth.lerp(w * 0.8F, this.tail.yRot, 0.18F * Mth.sin(t * 0.25F));
        float check = NoxisBoredAnimation.env(t % 46.0F, 30, 33, 38, 42);
        this.head.zRot += 0.15F * check * w;
        this.rightEar.zRot += 0.25F * check * w;
    }

    /** ¿Y esto?: se inclina a mirar, ladea la cabeza a un lado y al otro, y saltito de "¡encontrado!". */
    private void listenPeek(float t, float w, boolean rightBusy, boolean leftBusy) {
        float on = NoxisBoredAnimation.env(t, 0, 6, 34, 42);
        float look = NoxisBoredAnimation.env(t, 2, 8, 30, 38);
        this.body.xRot += 0.2F * on * w;
        this.head.z -= 0.9F * on * w;
        this.head.xRot += 0.32F * look * w;
        float tl = NoxisBoredAnimation.env(t, 6, 10, 13, 17);
        float tr = NoxisBoredAnimation.env(t, 17, 21, 24, 28);
        this.head.zRot += 0.4F * (tl - tr) * w;
        float k = on * w;
        this.rightEar.zRot = Mth.lerp(k, this.rightEar.zRot, -0.06F);
        this.leftEar.zRot = Mth.lerp(k, this.leftEar.zRot, 0.06F);
        this.rightEar.xRot -= (0.55F * tl + 0.15F) * k;
        this.leftEar.xRot -= (0.55F * tr + 0.15F) * k;
        this.rightEar.yRot += 0.5F * tl * w;
        this.leftEar.yRot -= 0.5F * tr * w;
        if (!rightBusy) {
            this.rightArm.xRot = Mth.lerp(k, this.rightArm.xRot, -0.75F);
            this.rightArm.zRot = Mth.lerp(k, this.rightArm.zRot, -0.25F);
        }
        if (!leftBusy) {
            this.leftArm.xRot = Mth.lerp(k, this.leftArm.xRot, -0.75F);
            this.leftArm.zRot = Mth.lerp(k, this.leftArm.zRot, 0.25F);
        }
        float hop = NoxisBoredAnimation.pulse(t, 34.0F, 3.0F) * 0.9F * w;
        this.head.y -= hop;
        this.body.y -= hop;
        this.rightArm.y -= hop;
        this.leftArm.y -= hop;
        this.tail.yRot += w * Mth.sin(t * 0.9F) * 0.5F * NoxisBoredAnimation.env(t, 30, 32, 38, 42);
    }

    // ================================================================== curiosidad y travesuras

    private void applyPlay(float[] w, float[] t) {
        if (w[NoxisPlayAnimation.FOLLOW] > 0.0F) this.playFollow(t[NoxisPlayAnimation.FOLLOW], w[NoxisPlayAnimation.FOLLOW]);
        if (w[NoxisPlayAnimation.WATCH] > 0.0F) this.playWatch(t[NoxisPlayAnimation.WATCH], w[NoxisPlayAnimation.WATCH]);
        if (w[NoxisPlayAnimation.SNEAK] > 0.0F) this.playCrouch(t[NoxisPlayAnimation.SNEAK], w[NoxisPlayAnimation.SNEAK], false);
        if (w[NoxisPlayAnimation.CROUCH] > 0.0F) this.playCrouch(t[NoxisPlayAnimation.CROUCH], w[NoxisPlayAnimation.CROUCH], true);
        if (w[NoxisPlayAnimation.POUNCE] > 0.0F) this.playPounce(t[NoxisPlayAnimation.POUNCE], w[NoxisPlayAnimation.POUNCE]);
        if (w[NoxisPlayAnimation.CHEER] > 0.0F) this.playCheer(t[NoxisPlayAnimation.CHEER], w[NoxisPlayAnimation.CHEER]);
        if (w[NoxisPlayAnimation.STARTLED] > 0.0F) this.playStartled(t[NoxisPlayAnimation.STARTLED], w[NoxisPlayAnimation.STARTLED]);
        if (w[NoxisPlayAnimation.PLAYFUL] > 0.0F) this.playPlayful(t[NoxisPlayAnimation.PLAYFUL], w[NoxisPlayAnimation.PLAYFUL]);
        // Sentado mirando un animal: la misma atención que al observar, más suave (la postura sentada la pone "sit").
        if (w[NoxisPlayAnimation.SIT_WATCH] > 0.0F) this.playWatch(t[NoxisPlayAnimation.SIT_WATCH] * 0.7F, w[NoxisPlayAnimation.SIT_WATCH] * 0.6F);
    }

    // ================================================================== melodías espontáneas

    /**
     * Tararear / silbar: la cabecita marca el pulso (100 BPM = un pulso cada 12 ticks), se mece de lado
     * cada dos pulsos y las orejitas se mueven a tiempo; silbando, la cabeza un poco levantada.
     * Mecerse (otro Noxis): balanceo suave del cuerpo y los bracitos al mismo compás, con un rebote
     * mínimo en cada pulso. Escuchar: cabeza ladeada, orejitas paradas y patita en el pecho.
     */
    private void applyHum(byte s, float w, float gameTime) {
        float beat = (gameTime % NoxisHumAnimation.BEAT) / NoxisHumAnimation.BEAT;           // 0-1 dentro del pulso
        float bar = (gameTime % (NoxisHumAnimation.BEAT * 2)) / (NoxisHumAnimation.BEAT * 2);   // 0-1 cada dos pulsos
        float onBeat = Mth.sin(beat * Mth.PI);                                               // 0 → 1 → 0 en cada pulso
        float side = Mth.sin(bar * (2.0F * Mth.PI));
        if (NoxisHumAnimation.isWhistle(s)) {
            // SILBAR: boquita en "o" (pieza propia), cabeza un poco levantada que va y viene despacio,
            // una manito en la cintura y la otra marcando el compás; rebotito en cada pulso, ojos abiertos.
            this.head.xRot += w * (-0.16F + 0.05F * onBeat);
            this.head.yRot += w * 0.18F * Mth.sin(bar * (2.0F * Mth.PI) * 0.5F);
            this.rightEar.xRot -= w * 0.25F;
            this.leftEar.xRot -= w * 0.25F;
            this.leftArm.xRot = Mth.lerp(w, this.leftArm.xRot, 0.35F);
            this.leftArm.zRot = Mth.lerp(w, this.leftArm.zRot, -0.75F);
            this.rightArm.xRot = Mth.lerp(w, this.rightArm.xRot, -0.35F - 0.25F * onBeat);
            this.rightArm.zRot = Mth.lerp(w, this.rightArm.zRot, 0.2F);
            this.playLift(w * 0.3F * onBeat);
            this.tail.yRot += w * 0.45F * side;
        } else if (NoxisHumAnimation.isHumming(s)) {
            // TARAREAR: ojitos entornados, manitos juntas adelante, la cabecita marca el pulso y se mece.
            this.head.xRot += w * 0.07F * onBeat;
            this.head.zRot += w * 0.09F * side;
            this.body.zRot += w * 0.03F * side;
            this.rightEar.zRot -= w * 0.18F * onBeat * (side > 0 ? 1.0F : 0.4F);
            this.leftEar.zRot += w * 0.18F * onBeat * (side < 0 ? 1.0F : 0.4F);
            this.rightArm.xRot = Mth.lerp(w, this.rightArm.xRot, -0.55F);
            this.rightArm.zRot = Mth.lerp(w, this.rightArm.zRot, 0.35F);
            this.leftArm.xRot = Mth.lerp(w, this.leftArm.xRot, -0.55F);
            this.leftArm.zRot = Mth.lerp(w, this.leftArm.zRot, -0.35F);
            this.tail.yRot += w * 0.35F * side;
        } else if (NoxisHumAnimation.isSway(s)) {
            this.body.zRot += w * 0.08F * side;
            this.head.zRot += w * 0.12F * Mth.sin(bar * (2.0F * Mth.PI) + 0.5F);
            this.rightArm.zRot += w * (0.18F + 0.14F * side);
            this.leftArm.zRot -= w * (0.18F - 0.14F * side);
            this.playLift(w * 0.35F * onBeat);
            this.rightEar.zRot -= w * 0.12F * onBeat;
            this.leftEar.zRot += w * 0.12F * onBeat;
            this.tail.yRot += w * 0.5F * side;
        } else if (NoxisHumAnimation.isListen(s)) {
            this.head.zRot += w * 0.24F;
            this.head.z -= 0.3F * w;
            this.rightEar.xRot -= 0.35F * w;
            this.leftEar.xRot -= 0.35F * w;
            this.rightEar.zRot += w * 0.15F * onBeat;
            this.rightArm.xRot = Mth.lerp(w, this.rightArm.xRot, -0.75F);
            this.rightArm.zRot = Mth.lerp(w, this.rightArm.zRot, -0.25F);
            this.tail.yRot += w * 0.2F * side;
        }
    }

    private void playLift(float amount) {
        this.head.y -= amount;
        this.body.y -= amount;
        this.rightArm.y -= amount;
        this.leftArm.y -= amount;
    }

    /** Siguiéndolo: cada tanto ladea la cabecita (a un lado y después al otro) y mueve una orejita. */
    private void playFollow(float t, float w) {
        float ph = t % 70.0F;
        float tilt = NoxisBoredAnimation.env(ph, 0, 6, 16, 24);
        float dir = ((int) (t / 70.0F)) % 2 == 0 ? 1.0F : -1.0F;
        this.head.zRot += w * 0.22F * dir * tilt;
        float flick = NoxisBoredAnimation.pulse(ph, 40.0F, 2.0F);
        if (dir > 0) this.rightEar.zRot -= w * 0.3F * flick;
        else this.leftEar.zRot += w * 0.3F * flick;
    }

    /** Observándolo (el otro se frenó): se inclina apenas, patita en el pecho, cabeza que va y viene, orejitas atentas. */
    private void playWatch(float t, float w) {
        this.body.xRot += 0.08F * w;
        this.head.z -= 0.5F * w;
        this.head.zRot += w * 0.26F * Mth.sin(t * 0.09F);
        this.rightEar.xRot -= 0.3F * w;
        this.leftEar.xRot -= 0.3F * w;
        float ph = t % 40.0F;
        this.rightEar.zRot += w * 0.25F * NoxisBoredAnimation.pulse(ph, 10.0F, 2.0F);
        this.leftEar.zRot -= w * 0.25F * NoxisBoredAnimation.pulse(ph, 25.0F, 2.0F);
        this.rightArm.xRot = Mth.lerp(w, this.rightArm.xRot, -0.75F);
        this.rightArm.zRot = Mth.lerp(w, this.rightArm.zRot, -0.25F);
        this.tail.yRot += w * Mth.sin(t * 0.08F) * 0.3F;
    }

    /**
     * Agachadito: el cuerpo bajo, la cabeza baja y adelantada, las manitos adelante (en puntitas),
     * las orejitas planas para atrás y la colita estirada y quieta. Esperando (crouch): todavía más
     * abajo, moviendo la colita y la cola del cuerpo como gatito a punto de saltar.
     */
    private void playCrouch(float t, float w, boolean waiting) {
        float low = waiting ? 1.9F : 1.4F;
        this.body.y += low * w;
        this.head.y += (low + 0.2F) * w;
        this.rightArm.y += (low - 0.2F) * w;
        this.leftArm.y += (low - 0.2F) * w;
        this.head.z -= 0.5F * w;
        this.head.xRot += 0.08F * w;
        float step = waiting ? 0.0F : Mth.sin(t * 0.35F) * 0.15F;
        this.rightArm.xRot = Mth.lerp(w, this.rightArm.xRot, -0.85F + step);
        this.leftArm.xRot = Mth.lerp(w, this.leftArm.xRot, -0.85F - step);
        this.rightArm.zRot = Mth.lerp(w, this.rightArm.zRot, -0.1F);
        this.leftArm.zRot = Mth.lerp(w, this.leftArm.zRot, 0.1F);
        this.rightEar.xRot += 0.55F * w;
        this.leftEar.xRot += 0.55F * w;
        this.rightEar.zRot -= 0.25F * w;
        this.leftEar.zRot += 0.25F * w;
        this.tail.xRot = Mth.lerp(w, this.tail.xRot, 1.45F);
        this.tail.yRot = Mth.lerp(w, this.tail.yRot, 0.0F);
        if (waiting) {
            // Se prepara: meneíto de colita (como gatito antes de saltar).
            float wig = NoxisBoredAnimation.env(t, 4, 7, 30, 34);
            this.body.zRot += w * Mth.sin(t * 1.5F) * 0.09F * wig;
            this.tail.yRot += w * Mth.sin(t * 1.8F) * 0.35F * wig;
        } else {
            this.body.y += w * 0.25F * Math.abs(Mth.sin(t * 0.3F));       // pasitos en puntitas
        }
    }

    /** ¡Salta! Se aplasta, se estira para arriba con los bracitos bien abiertos y aterriza con un rebote. */
    private void playPounce(float t, float w) {
        float squash = NoxisBoredAnimation.env(t, 0, 2, 3, 5);
        float spring = NoxisBoredAnimation.env(t, 3, 5, 8, 11);
        float land = NoxisBoredAnimation.pulse(t, 12.0F, 2.5F);
        this.playLift(w * (-1.2F * squash + 2.4F * spring - 1.0F * land));
        this.rightArm.zRot = Mth.lerp(w * spring, this.rightArm.zRot, 2.3F);
        this.leftArm.zRot = Mth.lerp(w * spring, this.leftArm.zRot, -2.3F);
        this.rightArm.xRot = Mth.lerp(w * spring, this.rightArm.xRot, -0.3F);
        this.leftArm.xRot = Mth.lerp(w * spring, this.leftArm.xRot, -0.3F);
        this.rightEar.zRot += 0.22F * w * spring;
        this.leftEar.zRot -= 0.22F * w * spring;
        this.rightEar.xRot -= 0.3F * w * spring;
        this.leftEar.xRot -= 0.3F * w * spring;
        this.head.xRot -= 0.2F * w * spring;
        this.tail.xRot -= 0.8F * w * spring;
        this.rightLeg.xRot -= 0.4F * w * spring;
        this.leftLeg.xRot += 0.4F * w * spring;
    }

    /** Festeja la travesura: rebota riéndose, saluda con los bracitos arriba y mueve la colita. */
    private void playCheer(float t, float w) {
        float bounce = NoxisBoredAnimation.pulse(t, 4.0F, 3.0F) + NoxisBoredAnimation.pulse(t, 14.0F, 3.0F)
                + NoxisBoredAnimation.pulse(t, 24.0F, 3.0F);
        this.playLift(w * 1.2F * bounce);
        this.rightArm.zRot = Mth.lerp(w, this.rightArm.zRot, 1.9F + Mth.sin(t * 0.6F) * 0.35F);
        this.leftArm.zRot = Mth.lerp(w, this.leftArm.zRot, -1.9F + Mth.sin(t * 0.6F + Mth.PI) * 0.35F);
        this.rightArm.xRot = Mth.lerp(w, this.rightArm.xRot, -0.2F);
        this.leftArm.xRot = Mth.lerp(w, this.leftArm.xRot, -0.2F);
        this.head.zRot += w * Mth.sin(t * 0.5F) * 0.15F;
        this.head.xRot -= w * 0.12F;
        this.tail.yRot += w * Mth.sin(t * 0.9F) * 0.6F;
    }

    /** ¡Susto! Pega un saltito con las manitos arriba, la cabeza para atrás y la colita tiesa (después, el miedo de siempre). */
    private void playStartled(float t, float w) {
        float jolt = NoxisBoredAnimation.env(t, 0, 2, 6, 12);
        this.playLift(w * 2.0F * NoxisBoredAnimation.pulse(t, 3.0F, 3.0F));
        float j = w * jolt;
        this.rightArm.xRot = Mth.lerp(j, this.rightArm.xRot, -2.4F);
        this.leftArm.xRot = Mth.lerp(j, this.leftArm.xRot, -2.4F);
        this.rightArm.zRot = Mth.lerp(j, this.rightArm.zRot, 0.35F);
        this.leftArm.zRot = Mth.lerp(j, this.leftArm.zRot, -0.35F);
        this.head.xRot -= 0.25F * j;
        this.body.xRot -= 0.15F * j;
        this.rightEar.zRot += 0.22F * j;
        this.leftEar.zRot -= 0.22F * j;
        this.tail.xRot -= 0.9F * j;
    }

    /** Se recupera y juega: dos manotazos juguetones con la patita, cabeza ladeada, saltito y colita contenta. */
    private void playPlayful(float t, float w) {
        float on = NoxisBoredAnimation.env(t, 2, 6, 22, 28);
        float swat = NoxisBoredAnimation.pulse(t, 8.0F, 4.0F) + NoxisBoredAnimation.pulse(t, 18.0F, 4.0F);
        this.rightArm.xRot = Mth.lerp(w * on, this.rightArm.xRot, -1.3F - 0.45F * swat);
        this.rightArm.zRot = Mth.lerp(w * on, this.rightArm.zRot, -0.15F);
        this.leftArm.xRot = Mth.lerp(w * on, this.leftArm.xRot, -0.6F);
        this.head.zRot += w * 0.2F * on;
        this.playLift(w * 1.0F * NoxisBoredAnimation.pulse(t, 24.0F, 3.0F));
        this.tail.yRot += w * Mth.sin(t * 0.8F) * 0.55F;
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
        if (NoxisAffectionAnimation.isWalking(anim)) {
            this.affectionHandWalk(f, t, w);
            return;
        }
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

    /**
     * Caminando de la mano: el bracito de adentro queda estirado hacia el amigo, tomado a la altura
     * de las manitos y con un balanceo chiquito al paso; el de afuera sigue el vaivén normal de la
     * caminata (no se toca). La cabecita a veces se gira hacia el amigo y la colita se mueve contenta.
     */
    private void affectionHandWalk(float f, float t, float w) {
        ModelPart inner = f > 0 ? this.rightArm : this.leftArm;
        float sw = Mth.sin(t * 0.3F);
        inner.zRot = Mth.lerp(w, inner.zRot, f * 1.1F);
        inner.xRot = Mth.lerp(w, inner.xRot, -0.1F + 0.12F * sw);
        inner.yScale *= 1.0F + 0.12F * w;
        float glance = NoxisBoredAnimation.env(t % 160.0F, 100, 110, 130, 140);
        this.head.yRot += w * f * 0.35F * glance;
        this.tail.yRot += w * Mth.sin(t * 0.35F) * 0.35F;
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
