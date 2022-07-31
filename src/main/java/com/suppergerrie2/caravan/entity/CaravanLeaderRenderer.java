package com.suppergerrie2.caravan.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.VillagerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.CrossedArmsItemLayer;
import net.minecraft.client.renderer.entity.layers.CustomHeadLayer;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public class CaravanLeaderRenderer extends MobRenderer<CaravanLeaderEntity, VillagerModel<CaravanLeaderEntity>> {
    private static final ResourceLocation VILLAGER_BASE_SKIN = new ResourceLocation("textures/entity/villager/villager.png");

    public CaravanLeaderRenderer(EntityRendererProvider.Context context) {
        super(context, new CaravanLeaderModel(context.bakeLayer(ModelLayers.VILLAGER)), 0.5F);
        this.addLayer(new CustomHeadLayer<>(this, context.getModelSet(), context.getItemInHandRenderer()));
        this.addLayer(new CaravanLeaderProfessionLayer(this, context.getResourceManager(), "villager")); // TODO: This only works for villagers, not for CaravanLeaders
        this.addLayer(new CrossedArmsItemLayer<>(this, context.getItemInHandRenderer()));
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull CaravanLeaderEntity entity) {
        return VILLAGER_BASE_SKIN;
    }

    @Override
    protected void scale(CaravanLeaderEntity livingEntity, @NotNull PoseStack matrixStack, float partialTickTime) {
        float scale = 0.9375F;
        if (livingEntity.isBaby()) {
            scale *= 0.5F;
            this.shadowRadius = 0.25F;
        } else {
            this.shadowRadius = 0.5F;
        }

        matrixStack.scale(scale, scale, scale);
    }
}
