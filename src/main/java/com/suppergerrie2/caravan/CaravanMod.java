package com.suppergerrie2.caravan;

import com.mojang.logging.LogUtils;
import com.suppergerrie2.caravan.blocks.CaravanTargetBlock;
import com.suppergerrie2.caravan.blocks.CaravanTargetBlockEntity;
import com.suppergerrie2.caravan.entity.CaravanLeaderEntity;
import com.suppergerrie2.caravan.entity.ai.sensors.CaravanTargetSensor;
import com.suppergerrie2.caravan.entity.ai.sensors.LlamaSearcherSensor;
import com.suppergerrie2.caravan.worldgen.VillageModifier;
import net.minecraft.core.GlobalPos;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.sensing.Sensor;
import net.minecraft.world.entity.ai.sensing.SensorType;
import net.minecraft.world.entity.animal.horse.Llama;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.entity.schedule.Schedule;
import net.minecraft.world.entity.schedule.ScheduleBuilder;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.DoubleHighBlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.slf4j.Logger;

import java.util.Optional;

@Mod(CaravanMod.MODID)
public class CaravanMod {

    public static final String MODID = "scaravan";
    private static final Logger LOGGER = LogUtils.getLogger();

    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, MODID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, MODID);
    public static final DeferredRegister<Schedule> SCHEDULES = DeferredRegister.create(ForgeRegistries.SCHEDULES, MODID);
    public static final DeferredRegister<Activity> ACTIVITIES = DeferredRegister.create(ForgeRegistries.ACTIVITIES, MODID);
    public static final DeferredRegister<SensorType<?>> SENSOR_TYPES = DeferredRegister.create(ForgeRegistries.SENSOR_TYPES, MODID);
    public static final DeferredRegister<MemoryModuleType<?>> MEMORY_MODULE_TYPES = DeferredRegister.create(ForgeRegistries.MEMORY_MODULE_TYPES, MODID);

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, MODID);

    public static final RegistryObject<Block> CARAVAN_SOURCE_BLOCK = BLOCKS.register("caravan_source", () -> new CaravanTargetBlock(CaravanBlockAssignmentManager::addSource, CaravanBlockAssignmentManager::removeSource, CaravanBlockAssignmentManager::getSource));
    public static final RegistryObject<Block> CARAVAN_DEST_BLOCK = BLOCKS.register("caravan_dest", () -> new CaravanTargetBlock(CaravanBlockAssignmentManager::addDest, CaravanBlockAssignmentManager::removeDest, CaravanBlockAssignmentManager::getDest));

    public static final RegistryObject<Item> CARAVAN_SOURCE_ITEM = ITEMS.register("caravan_source", () ->
            new DoubleHighBlockItem(CARAVAN_SOURCE_BLOCK.get(), new Item.Properties().tab(CreativeModeTab.TAB_MISC)));
    public static final RegistryObject<Item> CARAVAN_DEST_ITEM = ITEMS.register("caravan_dest", () ->
            new DoubleHighBlockItem(CARAVAN_DEST_BLOCK.get(), new Item.Properties().tab(CreativeModeTab.TAB_MISC)));

    public static final RegistryObject<BlockEntityType<?>> CARAVAN_TARGET_BLOCK_ENTITY = BLOCK_ENTITIES.register("caravan_target", () -> BlockEntityType.Builder.of(CaravanTargetBlockEntity::new, CARAVAN_SOURCE_BLOCK.get(), CARAVAN_DEST_BLOCK.get()).build(null));
    public static final RegistryObject<EntityType<CaravanLeaderEntity>> CARAVAN_LEADER = ENTITY_TYPES.register("caravan_leader", () ->
            EntityType.Builder.of(CaravanLeaderEntity::new, MobCategory.MISC)
                    .sized(0.6F, 1.95F)
                    .clientTrackingRange(10)
                    .build("caravan_leader"));

    public static final RegistryObject<MemoryModuleType<Llama>> NEARBY_UNCLAIMED_LLAMA = MEMORY_MODULE_TYPES.register("unclaimed_nearby_llama", () -> new MemoryModuleType<>(Optional.empty()));
    public static final RegistryObject<MemoryModuleType<GlobalPos>> CARAVAN_SOURCE_TARGET = MEMORY_MODULE_TYPES.register("caravan_source_target", () -> new MemoryModuleType<>(Optional.empty()));
    public static final RegistryObject<MemoryModuleType<GlobalPos>> CARAVAN_DEST_TARGET = MEMORY_MODULE_TYPES.register("caravan_dest_target", () -> new MemoryModuleType<>(Optional.empty()));

    public static final RegistryObject<Activity> LEAD_CARAVAN = ACTIVITIES.register("lead_caravan", () -> new Activity("lead_caravan"));
    public static final RegistryObject<Schedule> CARAVAN_SCHEDULE = SCHEDULES.register("caravan", Schedule::new);
    public static final RegistryObject<SensorType<? extends Sensor<? super LivingEntity>>> FIND_UNCLAIMED_LLAMA = SENSOR_TYPES.register("unclaimed_llama_sensor", () -> new SensorType<>(LlamaSearcherSensor::new));
    public static final RegistryObject<SensorType<? extends Sensor<? super LivingEntity>>> SOURCE_SENSOR = SENSOR_TYPES.register("source_sensor", () -> new SensorType<>(() -> new CaravanTargetSensor<>(CARAVAN_SOURCE_TARGET.get(), CaravanBlockAssignmentManager::getSource)));
    public static final RegistryObject<SensorType<? extends Sensor<? super LivingEntity>>> DEST_SENSOR = SENSOR_TYPES.register("dest_sensor", () -> new SensorType<>(() -> new CaravanTargetSensor<>(CARAVAN_DEST_TARGET.get(), CaravanBlockAssignmentManager::getDest)));

    public static final RegistryObject<Item> DEBUG_ITEM = ITEMS.register("debug_item", () ->
            new DebugItem(new Item.Properties()));

    public CaravanMod() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        ENTITY_TYPES.register(modEventBus);
        ACTIVITIES.register(modEventBus);
        SCHEDULES.register(modEventBus);
        MEMORY_MODULE_TYPES.register(modEventBus);
        SENSOR_TYPES.register(modEventBus);
        BLOCK_ENTITIES.register(modEventBus);

        modEventBus.register(this);
        MinecraftForge.EVENT_BUS.register(new Events());
        VillageModifier.init();
    }

    @SubscribeEvent
    public void onAttributeCreate(EntityAttributeCreationEvent event) {
        event.put(CARAVAN_LEADER.get(), CaravanLeaderEntity.createAttributes().build());
    }

    @SubscribeEvent
    public void onCommonSetup(FMLCommonSetupEvent event) {
        new ScheduleBuilder(CARAVAN_SCHEDULE.get()).changeActivityAt(10, Activity.IDLE).changeActivityAt(1000, LEAD_CARAVAN.get()).changeActivityAt(11000, Activity.IDLE).changeActivityAt(12000, Activity.REST).build();
    }

}
