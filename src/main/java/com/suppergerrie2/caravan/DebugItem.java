package com.suppergerrie2.caravan;

import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.UUID;

public class DebugItem extends Item {

    public DebugItem(Properties properties) {
        super(properties);
    }

    @Override
    public @NotNull InteractionResult useOn(UseOnContext context) {
        if (context.getLevel().isClientSide) {
            return InteractionResult.SUCCESS;
        }

        if (context.getPlayer().isShiftKeyDown()) {
            var manager = CaravanBlockAssignmentManager.getInstance((ServerLevel) context.getLevel());
            var pos = context.getClickedPos().relative(context.getClickedFace());

            GlobalPos globalPos = GlobalPos.of(context.getLevel().dimension(), pos);
            Optional<UUID> ownerOptional = manager.getOwner(globalPos);
            ownerOptional.ifPresent(owner -> {
                manager.removeSource(globalPos, owner);
                manager.removeDest(globalPos, owner);
            });
        }

        var manager = CaravanBlockAssignmentManager.getInstance((ServerLevel) context.getLevel());

        for (UUID owner : manager.getAllOwners()) {
            context.getPlayer().sendSystemMessage(Component.literal("Owner: " + owner));

            manager.getSource(owner).ifPresent(source -> context.getPlayer().sendSystemMessage(Component.literal("    Source: " + source.pos())));
            manager.getDest(owner).ifPresent(dest -> context.getPlayer().sendSystemMessage(Component.literal("    Dest: " + dest.pos())));
        }

        for (GlobalPos position : manager.getAllPositions()) {
            manager.getOwner(position).ifPresentOrElse(
                    owner -> context.getPlayer().sendSystemMessage(Component.literal("Position: " + position.pos() + " Owner: " + owner)),
                    () -> context.getPlayer().sendSystemMessage(Component.literal("Position: " + position.pos())));
        }

        return super.useOn(context);
    }
}
