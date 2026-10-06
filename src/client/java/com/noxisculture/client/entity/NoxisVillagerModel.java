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
    private static final String RIGHT_LID = "right_lid";
    private static final String LEFT_LID = "left_lid";
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
    private final ModelPart rightLid;
    private final ModelPart leftLid;
    private final ModelPart leftArm;
    private final ModelPart rightLeg;
    private final ModelPart leftLeg;

    public NoxisVillagerModel(ModelPart root) {
        super(root);
        this.head = root.getChild(PartNames.HEAD);
        this.body = root.getChild(PartNames.BODY);
        this.hat = this.head.getChild(HAT);
        this.rightEar = this.head.getChild(PartNames.RIGHT_EAR);
        this.leftEar = this.head.getChild(PartNames.LEFT_EAR);
        this.rightLid = this.head.getChild(RIGHT_LID);
        this.leftLid = this.head.getChild(LEFT_LID);
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
        // Párpados 3D: bajan sobre los ojos para entrecerrarlos (esfuerzo) o casi cerrarlos (sueño).
        head.addOrReplaceChild(RIGHT_LID,
                CubeListBuilder.create().texOffs(0, 40).addBox(0.0F, 0.0F, -0.5F, 3.0F, 1.0F, 1.0F),
                PartPose.offset(-4.0F, -6.0F, -4.0F));
        head.addOrReplaceChild(LEFT_LID,
                CubeListBuilder.create().texOffs(0, 40).addBox(0.0F, 0.0F, -0.5F, 3.0F, 1.0F, 1.0F),
                PartPose.offset(1.0F, -6.0F, -4.0F));
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

        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(NoxisVillagerRenderState state) {
        super.setupAnim(state);
        float age = state.ageInTicks;
        float pos = state.walkAnimationPos;
        float amount = state.walkAnimationSpeed;
        float happy = state.happyAnim;
        float scared = state.scaredAnim;
        float torchAnim = state.torchAnim;
        float rest = state.restAnim;

        // ---- Bamboleo cartoon al caminar ----
        float[] waddle = NoxisMoodAnimator.waddle(pos, amount, happy);
        float roll = waddle[0];
        float bounce = waddle[1];
        float hop = Math.abs(Mth.sin(age * 0.3F)) * 0.6F * happy * (1.0F - amount);
        float sitDrop = 2.0F * rest; // al sentarse, todo baja

        this.body.zRot = roll;
        this.body.xRot = 0.12F * torchAnim - 0.08F * rest; // se inclina para ver / se recuesta un poquito
        this.body.y = HEAD_Y - bounce - hop + sitDrop;

        // ---- Cabeza ----
        float[] life = NoxisMoodAnimator.headLife(age, pos, amount, happy);
        int t = (int) age;
        boolean dozing = (t % 160) < 22;                            // cabeceo de sueño de vez en cuando
        float doze = rest * (Mth.sin(age * 0.05F) * 0.06F + (dozing ? 0.22F : 0.0F));
        this.head.yRot = state.yRot * Mth.DEG_TO_RAD + 0.3F * torchAnim;   // mira hacia la luz
        this.head.xRot = state.xRot * Mth.DEG_TO_RAD + life[0] + 0.25F * scared + 0.15F * rest + doze;
        this.head.zRot = roll * 1.4F + life[1] + NoxisMoodAnimator.headTremble(age, scared) + 0.12F * rest;
        this.head.x = -roll * 3.0F;
        this.head.z = -0.9F * torchAnim;                             // estira el cuello para ver mejor
        this.head.y = HEAD_Y - bounce - hop + 0.8F * scared + sitDrop;

        // ---- Piezas compartidas por todas las especies ----
        NoxisMoodAnimator.animateEars(this.rightEar, this.leftEar, age, happy, scared, EAR_BASE_ANGLE);
        NoxisMoodAnimator.animateHat(this.hat, HAT_Y, HAT_BASE_TILT, age, pos, amount, happy, scared);
        NoxisMoodAnimator.animateTail(this.tail, TAIL_BASE_ANGLE, age, happy, scared);
        // Cansado: orejitas caídas, sombrero que se resbala sobre los ojos, cola enroscada en el piso.
        this.rightEar.zRot -= 0.55F * rest;
        this.leftEar.zRot += 0.55F * rest;
        this.hat.xRot = -0.05F + 0.2F * rest;
        this.tail.xRot = Mth.lerp(rest, this.tail.xRot, -0.15F);
        this.tail.yRot = Mth.lerp(rest, this.tail.yRot, 1.25F);

        // ---- Párpados (expresión) ----
        // esfuerzo por ver con la antorcha: entrecierra; cansado: casi cierra y parpadea lento.
        boolean blink = (t % 70) < 4 || dozing;
        float lids = Math.max(2.0F * torchAnim, rest * (blink ? 3.0F : 2.0F));
        this.rightLid.visible = lids > 0.05F;
        this.leftLid.visible = lids > 0.05F;
        this.rightLid.yScale = lids;
        this.leftLid.yScale = lids;

        // ---- Patitas: pasitos, o estiradas hacia adelante al sentarse ----
        this.rightLeg.xRot = Mth.lerp(rest, Mth.cos(pos * 1.2F) * 1.1F * amount, -1.45F);
        this.leftLeg.xRot = Mth.lerp(rest, Mth.cos(pos * 1.2F + Mth.PI) * 1.1F * amount, -1.45F);
        this.rightLeg.yRot = 0.25F * rest;
        this.leftLeg.yRot = -0.25F * rest;
        this.rightLeg.y = 22.0F + 1.0F * rest;
        this.leftLeg.y = 22.0F + 1.0F * rest;

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
        rightX = Mth.lerp(rest, rightX, -0.55F);
        leftX = Mth.lerp(rest, leftX, -0.55F);
        rightZ = Mth.lerp(rest, rightZ, -0.15F);
        leftZ = Mth.lerp(rest, leftZ, 0.15F);
        // Antorcha: brazo derecho ESTIRADO al costado y adelante, apenas arriba del hombro
        // (como sosteniendo un farol para ver), y el izquierdo adelante, con cuidado.
        float sway = Mth.sin(age * 0.12F) * 0.05F + Mth.cos(pos * 1.2F) * 0.08F * amount;
        rightX = Mth.lerp(torchAnim, rightX, -0.6F + sway);
        rightZ = Mth.lerp(torchAnim, rightZ, 1.9F + roll);
        leftX = Mth.lerp(torchAnim, leftX, -0.5F + Mth.cos(pos * 1.2F) * 0.3F * amount);
        leftZ = Mth.lerp(torchAnim, leftZ, -0.25F);
        this.rightArm.xRot = rightX;
        this.rightArm.zRot = rightZ;
        this.leftArm.xRot = leftX;
        this.leftArm.zRot = leftZ;

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
    }
}
