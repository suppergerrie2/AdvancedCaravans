package com.suppergerrie2.caravan;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.suppergerrie2.caravan.entity.CaravanLeaderEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.horse.Llama;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = CaravanMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class DebugRenderer {

    enum DebugMode {
        DISABLED,
        CARAVAN_LLAMAS;

        private static final DebugMode[] vals = values();

        public DebugMode next() {
            return vals[(this.ordinal() + 1) % vals.length];
        }
    }

    static DebugMode currentMode = DebugMode.DISABLED;

    @SubscribeEvent
    public static void onKeyPressed(InputEvent.Key event) {
        if (event.getAction() == InputConstants.RELEASE) return;
        if (!InputConstants.isKeyDown(Minecraft.getInstance().getWindow().getWindow(), GLFW.GLFW_KEY_F3)) return;

        switch (event.getKey()) {
            case GLFW.GLFW_KEY_Q -> Minecraft.getInstance().gui.getChat().addMessage(Component.literal("F3 + O = Cycle caravan debug modes"));
            case GLFW.GLFW_KEY_O -> {
                currentMode = currentMode.next();

                Minecraft.getInstance().gui.getChat().addMessage(Component.literal("Switched to mode %s".formatted(currentMode.toString())).withStyle(ChatFormatting.GREEN));
            }
            default -> {
                return;
            }
        }

        KeyMapping.set(InputConstants.getKey(event.getKey(), event.getScanCode()), false);
        Minecraft.getInstance().keyboardHandler.handledDebugKey = true;
    }

    @SubscribeEvent
    public static void renderDebugEvent(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_WEATHER || currentMode == DebugMode.DISABLED) {
            return;
        }

        switch (currentMode) {
            case CARAVAN_LLAMAS -> renderCaravanLlamas(event);
        }
    }

    private static void renderCaravanLlamas(RenderLevelStageEvent event) {
        for (Entity entity : Minecraft.getInstance().level.entitiesForRendering()) {
            if (!(entity instanceof CaravanLeaderEntity leader)) continue;

            leader.getCaravanHead().ifPresent(llamaUuid -> {
                Llama llama = (Llama) Minecraft.getInstance().level.getEntities().get(llamaUuid);

                if(llama == null){
                    net.minecraft.client.renderer.debug.DebugRenderer.renderFloatingText("!?!", leader.getX(), leader.getEyeY() + 0.5f, leader.getZ(), 0xFFFF0000);
                    return;
                }

                renderLine(leader.getEyePosition(), llama.getEyePosition(), 0xFF0000FF, event);
            });
        }
    }

    private static void renderLine(Vec3 start, Vec3 end, int color, RenderLevelStageEvent event) {

        start = start.subtract(event.getCamera().getPosition());
        end = end.subtract(event.getCamera().getPosition());

        RenderSystem.enableDepthTest();
        RenderSystem.setShader(GameRenderer::getRendertypeLinesShader);
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder bufferbuilder = tesselator.getBuilder();
        bufferbuilder.begin(VertexFormat.Mode.LINES, DefaultVertexFormat.POSITION_COLOR_NORMAL);
        RenderSystem.lineWidth(5.0F);

        for(Direction dir : Direction.values()) {
            bufferbuilder.vertex(start.x, start.y, start.z).color(color).normal((float) dir.getStepX(), (float) dir.getStepY(), (float) dir.getStepZ()).endVertex();
            bufferbuilder.vertex(end.x, end.y, end.z).color(color).normal((float) dir.getStepX(), (float) dir.getStepY(), (float) dir.getStepZ()).endVertex();
        }

        tesselator.end();

    }

}
