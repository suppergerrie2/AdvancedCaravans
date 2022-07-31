package com.suppergerrie2.caravan.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.suppergerrie2.caravan.CaravanMod;
import net.minecraft.client.model.VillagerModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

@OnlyIn(Dist.CLIENT)
public class CaravanLeaderProfessionLayer extends RenderLayer<CaravanLeaderEntity, VillagerModel<CaravanLeaderEntity>> {

    private static final ResourceLocation VILLAGER_BASE_SKIN = new ResourceLocation("minecraft", "textures/entity/villager/type/plains.png");
    private static final ResourceLocation CARAVAN_OVERLAY = new ResourceLocation(CaravanMod.MODID, "textures/entity/caravan_leader/caravan.png");

    public CaravanLeaderProfessionLayer(RenderLayerParent<CaravanLeaderEntity, VillagerModel<CaravanLeaderEntity>> pRenderer, ResourceManager pResourceManager, String pPath) {
        super(pRenderer);
    }

    @Override
    public void render(@NotNull PoseStack poseStack, @NotNull MultiBufferSource buffer, int packedLight, CaravanLeaderEntity livingEntity, float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        if (!livingEntity.isInvisible()) {
            VillagerModel<CaravanLeaderEntity> m = this.getParentModel();
            m.hatVisible(false);
            renderColoredCutoutModel(m, VILLAGER_BASE_SKIN, poseStack, buffer, packedLight, livingEntity, 1.0F, 1.0F, 1.0F);
            m.hatVisible(true);
            if (!livingEntity.isBaby()) {
                renderColoredCutoutModel(m, CARAVAN_OVERLAY, poseStack, buffer, packedLight, livingEntity, 1.0F, 1.0F, 1.0F);
            }
        }
    }

}
