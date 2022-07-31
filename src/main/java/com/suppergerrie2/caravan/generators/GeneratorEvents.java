package com.suppergerrie2.caravan.generators;

import com.suppergerrie2.caravan.CaravanMod;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = CaravanMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class GeneratorEvents {

    @SubscribeEvent
    public static void gatherDataEvent(GatherDataEvent event) {
        event.getGenerator().addProvider(event.includeClient(), new DefaultLanguageProvider(event.getGenerator()));
        event.getGenerator().addProvider(event.includeClient(), new CaravanItemModelProvider(event.getGenerator(), event.getExistingFileHelper()));
        event.getGenerator().addProvider(event.includeClient(), new CaravanBlockStateProvider(event.getGenerator(), event.getExistingFileHelper()));
        event.getGenerator().addProvider(event.includeServer(), new CaravanLootTableProvider(event.getGenerator()));
    }

}
