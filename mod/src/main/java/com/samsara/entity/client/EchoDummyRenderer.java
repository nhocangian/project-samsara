package com.samsara.entity.client;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.samsara.client.CamcorderClientManager;
import com.samsara.entity.EchoDummyEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.model.Dilation;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelPartBuilder;
import net.minecraft.client.model.ModelPartData;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.util.DefaultSkinHelper;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;

/**
 * Renderer for Echo Residues.
 * Renders the full mirror image of the player (including all Layer 2 outer parts)
 * with semi-transparent ghosting, visible only through the camcorder.
 * Uses an isolated, clean vanilla model hierarchy to guarantee immunity from
 * third-party player model mixins (such as repo-heads).
 */
public class EchoDummyRenderer extends LivingEntityRenderer<EchoDummyEntity, EchoDummyRenderer.EchoDummyModel> {
    private final EchoDummyModel defaultModel;
    private final EchoDummyModel slimModel;

    public EchoDummyRenderer(EntityRendererFactory.Context context) {
        super(context, new EchoDummyModel(createCleanModelPart(false)), 0.0f);
        this.defaultModel = this.model;
        this.slimModel = new EchoDummyModel(createCleanModelPart(true));
    }

    private static ModelPart createCleanModelPart(boolean slim) {
        ModelData modelData = new ModelData();
        ModelPartData root = modelData.getRoot();
        Dilation dilation = Dilation.NONE;

        // Head & Hat (Layer 1 & Layer 2) - Clean vanilla siblings
        root.addChild("head", ModelPartBuilder.create().uv(0, 0).cuboid(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F, dilation), ModelTransform.pivot(0.0F, 0.0F, 0.0F));
        root.addChild("hat", ModelPartBuilder.create().uv(32, 0).cuboid(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F, dilation.add(0.5F)), ModelTransform.pivot(0.0F, 0.0F, 0.0F));

        // Body & Jacket (Layer 1 & Layer 2)
        root.addChild("body", ModelPartBuilder.create().uv(16, 16).cuboid(-4.0F, 0.0F, -2.0F, 8.0F, 12.0F, 4.0F, dilation), ModelTransform.pivot(0.0F, 0.0F, 0.0F));
        root.addChild("jacket", ModelPartBuilder.create().uv(16, 32).cuboid(-4.0F, 0.0F, -2.0F, 8.0F, 12.0F, 4.0F, dilation.add(0.25F)), ModelTransform.pivot(0.0F, 0.0F, 0.0F));

        // Arms & Sleeves (Layer 1 & Layer 2)
        if (slim) {
            root.addChild("left_arm", ModelPartBuilder.create().uv(32, 48).cuboid(-1.0F, -2.0F, -2.0F, 3.0F, 12.0F, 4.0F, dilation), ModelTransform.pivot(5.0F, 2.5F, 0.0F));
            root.addChild("right_arm", ModelPartBuilder.create().uv(40, 16).cuboid(-2.0F, -2.0F, -2.0F, 3.0F, 12.0F, 4.0F, dilation), ModelTransform.pivot(-5.0F, 2.5F, 0.0F));
            root.addChild("left_sleeve", ModelPartBuilder.create().uv(48, 48).cuboid(-1.0F, -2.0F, -2.0F, 3.0F, 12.0F, 4.0F, dilation.add(0.25F)), ModelTransform.pivot(5.0F, 2.5F, 0.0F));
            root.addChild("right_sleeve", ModelPartBuilder.create().uv(40, 32).cuboid(-2.0F, -2.0F, -2.0F, 3.0F, 12.0F, 4.0F, dilation.add(0.25F)), ModelTransform.pivot(-5.0F, 2.5F, 0.0F));
        } else {
            root.addChild("left_arm", ModelPartBuilder.create().uv(32, 48).cuboid(-1.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F, dilation), ModelTransform.pivot(5.0F, 2.0F, 0.0F));
            root.addChild("right_arm", ModelPartBuilder.create().uv(40, 16).cuboid(-3.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F, dilation), ModelTransform.pivot(-5.0F, 2.0F, 0.0F));
            root.addChild("left_sleeve", ModelPartBuilder.create().uv(48, 48).cuboid(-1.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F, dilation.add(0.25F)), ModelTransform.pivot(5.0F, 2.0F, 0.0F));
            root.addChild("right_sleeve", ModelPartBuilder.create().uv(40, 32).cuboid(-3.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F, dilation.add(0.25F)), ModelTransform.pivot(-5.0F, 2.0F, 0.0F));
        }

        // Legs & Pants (Layer 1 & Layer 2)
        root.addChild("right_leg", ModelPartBuilder.create().uv(0, 16).cuboid(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, dilation), ModelTransform.pivot(-1.9F, 12.0F, 0.0F));
        root.addChild("right_pants", ModelPartBuilder.create().uv(0, 32).cuboid(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, dilation.add(0.25F)), ModelTransform.pivot(-1.9F, 12.0F, 0.0F));
        root.addChild("left_leg", ModelPartBuilder.create().uv(16, 48).cuboid(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, dilation), ModelTransform.pivot(1.9F, 12.0F, 0.0F));
        root.addChild("left_pants", ModelPartBuilder.create().uv(0, 48).cuboid(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, dilation.add(0.25F)), ModelTransform.pivot(1.9F, 12.0F, 0.0F));

        return root.createPart(64, 64);
    }

    @Override
    public Identifier getTexture(EchoDummyEntity entity) {
        Optional<UUID> uuidOpt = entity.getPlayerUuid();
        if (uuidOpt.isPresent()) {
            UUID uuid = uuidOpt.get();
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.getNetworkHandler() != null) {
                PlayerListEntry entry = client.getNetworkHandler().getPlayerListEntry(uuid);
                if (entry != null) {
                    return entry.getSkinTexture();
                }
            }
            return DefaultSkinHelper.getTexture(uuid);
        }
        return DefaultSkinHelper.getTexture();
    }

    private boolean isSlim(EchoDummyEntity entity) {
        Optional<UUID> uuidOpt = entity.getPlayerUuid();
        if (uuidOpt.isPresent()) {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.getNetworkHandler() != null) {
                PlayerListEntry entry = client.getNetworkHandler().getPlayerListEntry(uuidOpt.get());
                if (entry != null) {
                    return "slim".equals(entry.getModel());
                }
            }
        }
        return false;
    }

    @Override
    public void render(EchoDummyEntity entity, float yaw, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
        // Tàn dư ký ức luân hồi (Echo Residue) vô hình với mắt thường.
        // Chỉ hiện hình khi người chơi nhìn qua ống kính máy quay (hoặc khi mang cờ ALWAYS_VISIBLE)!
        boolean throughCam = CamcorderClientManager.isFullyAimed() || entity.isAlwaysVisible();
        if (!throughCam) {
            return;
        }

        // Chọn model phù hợp với skin của người chơi (Slim hoặc Wide)
        this.model = isSlim(entity) ? this.slimModel : this.defaultModel;

        // Bật hiển thị toàn bộ các bộ phận layer 1 & layer 2 của skin
        this.model.setVisible(true);

        super.render(entity, yaw, tickDelta, matrices, vertexConsumers, light);
    }

    @Override
    protected boolean hasLabel(EchoDummyEntity entity) {
        // Chỉ bóng ma có tên (Nam_1998) mới hiện nhãn; render() đã chặn khi không nhìn qua máy quay
        return entity.hasCustomName() && entity.isCustomNameVisible();
    }

    @Nullable
    @Override
    protected RenderLayer getRenderLayer(EchoDummyEntity entity, boolean showBody, boolean translucent, boolean showOutline) {
        // Semi-transparent phantom layer
        return RenderLayer.getEntityTranslucent(getTexture(entity));
    }

    /**
     * Custom Biped model holding the full player skin layers (Layer 1 + Layer 2).
     * By extending BipedEntityModel instead of PlayerEntityModel, it completely bypasses
     * mod mixins targeting PlayerEntityModel (e.g. repo-heads altering hat coordinates/parenting).
     */
    public static class EchoDummyModel extends BipedEntityModel<EchoDummyEntity> {
        public final ModelPart jacket;
        public final ModelPart leftSleeve;
        public final ModelPart rightSleeve;
        public final ModelPart leftPants;
        public final ModelPart rightPants;

        public EchoDummyModel(ModelPart root) {
            super(root);
            this.jacket = root.getChild("jacket");
            this.leftSleeve = root.getChild("left_sleeve");
            this.rightSleeve = root.getChild("right_sleeve");
            this.leftPants = root.getChild("left_pants");
            this.rightPants = root.getChild("right_pants");
        }

        @Override
        public void setVisible(boolean visible) {
            super.setVisible(visible);
            this.jacket.visible = visible;
            this.leftSleeve.visible = visible;
            this.rightSleeve.visible = visible;
            this.leftPants.visible = visible;
            this.rightPants.visible = visible;
        }

        @Override
        protected Iterable<ModelPart> getBodyParts() {
            return Iterables.concat(super.getBodyParts(), ImmutableList.of(
                    this.leftPants,
                    this.rightPants,
                    this.leftSleeve,
                    this.rightSleeve,
                    this.jacket
            ));
        }

        @Override
        public void setAngles(EchoDummyEntity entity, float limbAngle, float limbDistance, float animationProgress, float headYaw, float headPitch) {
            super.setAngles(entity, limbAngle, limbDistance, animationProgress, headYaw, headPitch);

            if (entity.isCrouchingState()) {
                this.sneaking = true;
            }

            if (entity.isBowing()) {
                this.head.pitch = 0.55f; // Đầu cúi xuống tôn nghiêm
                this.body.pitch = 0.15f; // Lưng hơi khom
                this.rightArm.pitch = 0.25f;
                this.leftArm.pitch = 0.25f;
                this.hat.copyTransform(this.head);
            }

            // Đồng bộ hoá toàn bộ Layer 2 bám chặt lấy Layer 1
            this.jacket.copyTransform(this.body);
            this.leftSleeve.copyTransform(this.leftArm);
            this.rightSleeve.copyTransform(this.rightArm);
            this.leftPants.copyTransform(this.leftLeg);
            this.rightPants.copyTransform(this.rightLeg);
        }
    }
}
