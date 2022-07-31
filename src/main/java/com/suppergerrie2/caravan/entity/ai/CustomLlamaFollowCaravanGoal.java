package com.suppergerrie2.caravan.entity.ai;

import com.suppergerrie2.caravan.entity.CaravanLeaderEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.goal.LlamaFollowCaravanGoal;
import net.minecraft.world.entity.animal.horse.Llama;
import net.minecraft.world.level.pathfinder.Path;

import java.util.List;

// Delegates most of the logic to the original class, but adds a check to make sure the llama only follows llamas with the same colour, or no colour.
public class CustomLlamaFollowCaravanGoal extends LlamaFollowCaravanGoal {

    public CustomLlamaFollowCaravanGoal(Llama llama, double speedModifier) {
        super(llama, speedModifier);
    }

    boolean isValidLlamaToFollow(Llama candidate) {

        // Find the head of the caravan
        while(candidate.getCaravanHead() != null) {
            candidate = candidate.getCaravanHead();
        }

        if(candidate.getLeashHolder() == null || !(candidate.getLeashHolder() instanceof CaravanLeaderEntity)) {
            return true;
        }

        // Check if the head of the caravan has the same colour as the llama, or does not have a colour assigned
        return candidate.getSwag() == null || candidate.getSwag() == this.llama.getSwag();
    }

    @Override
    public boolean canUse() {
        // Copy from vanilla, except a check is added for the colour of the llama swag
        if (!this.llama.isLeashed() && !this.llama.inCaravan()) {
            List<Entity> list = this.llama.level.getEntities(this.llama, this.llama.getBoundingBox().inflate(9.0D, 4.0D, 9.0D), e -> {
                EntityType<?> entityType = e.getType();
                return entityType == EntityType.LLAMA || entityType == EntityType.TRADER_LLAMA;
            });

            Llama llama = null;
            double minDistance = Double.MAX_VALUE;

            // Find a llama that is already in a caravan but does not have anyone following it
            for(Entity entity : list) {
                Llama candidateLlama = (Llama)entity;
                if (candidateLlama.inCaravan() && !candidateLlama.hasCaravanTail() && isValidLlamaToFollow(candidateLlama)) {
                    double distance = this.llama.distanceToSqr(candidateLlama);
                    if (distance <= minDistance) {
                        Path path = this.llama.getNavigation().createPath(candidateLlama, 0);
                        if(path == null || path.getDistToTarget() > 1.0f) continue;

                        minDistance = distance;
                        llama = candidateLlama;
                    }
                }
            }

            // If no llama was found search for a llama that is leashed and does not have any llamas following it
            // This will become the head of the caravan
            if (llama == null) {
                for(Entity entity : list) {
                    Llama candidateLlama = (Llama)entity;
                    if (candidateLlama.isLeashed() && !candidateLlama.hasCaravanTail() && isValidLlamaToFollow(candidateLlama)) {
                        double distance = this.llama.distanceToSqr(candidateLlama);
                        if (distance <= minDistance) {
                            Path path = this.llama.getNavigation().createPath(candidateLlama, 0);
                            if(path == null || path.getDistToTarget() > 1.0f) continue;

                            minDistance = distance;
                            llama = candidateLlama;
                        }
                    }
                }
            }

            if (llama == null) {
                return false;
            } else if (minDistance < 4.0D) {
                return false;
            } else if (!llama.isLeashed() && !this.firstIsLeashed(llama, 1)) {
                return false;
            } else {
                this.llama.joinCaravan(llama);
                return true;
            }
        } else {
            return false;
        }
    }

    @Override
    public boolean canContinueToUse() {
        return super.canContinueToUse() && isValidLlamaToFollow(this.llama.getCaravanHead());
    }
}
