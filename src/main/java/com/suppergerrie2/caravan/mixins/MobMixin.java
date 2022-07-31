package com.suppergerrie2.caravan.mixins;

import com.suppergerrie2.caravan.entity.CaravanLeaderEntity;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mob.class)
public class MobMixin {

    @Inject(method = "dropLeash", at = @At("HEAD"), cancellable = true)
    public void onLeashHolderSet(boolean pBroadcastPacket, boolean pDropLeash, CallbackInfo ci) {
        // Prevent the leash from dropping when the leash holder is a caravan leader entity to prevent duping leashes TODO: Find a non mixin way to do this
        if(pDropLeash && ((Mob) (Object) this).getLeashHolder() instanceof CaravanLeaderEntity) {
            ((Mob) (Object) this).dropLeash(pBroadcastPacket, false);
            ci.cancel();
        }
    }

}
