package com.suppergerrie2.caravan;

import com.suppergerrie2.caravan.entity.ai.CustomLlamaFollowCaravanGoal;
import net.minecraft.world.entity.ai.goal.LlamaFollowCaravanGoal;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.minecraft.world.entity.animal.horse.Llama;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.List;
import java.util.stream.Collectors;

public class Events {

    @SubscribeEvent
    public void onEntityJoinEvent(EntityJoinLevelEvent event) {
        // Replace the default LlamaFollowCaravanGoal with our custom one
        if (event.getEntity() instanceof Llama llama) {
            List<WrappedGoal> goalsToReplace = llama.goalSelector.getAvailableGoals().stream().filter(g -> g.getGoal() instanceof LlamaFollowCaravanGoal).toList();
            for (WrappedGoal g : goalsToReplace) {
                llama.goalSelector.removeGoal(g.getGoal());
                llama.goalSelector.addGoal(g.getPriority(), new CustomLlamaFollowCaravanGoal(llama, ((LlamaFollowCaravanGoal) g.getGoal()).speedModifier));
            }
        }
    }

    @SubscribeEvent
    public void onEntityLeaveEvent(EntityLeaveLevelEvent event) {
        if (event.getEntity() instanceof Llama llama) {
            // Fix for MC-210225
            if (llama.inCaravan()) {
                llama.leaveCaravan();
            }
        }
    }
}
