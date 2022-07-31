package com.suppergerrie2.caravan.tests;

import com.suppergerrie2.caravan.CaravanMod;
import com.suppergerrie2.caravan.entity.CaravanLeaderEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.horse.Llama;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.items.CapabilityItemHandler;

@GameTestHolder(CaravanMod.MODID)
public class CaravanFollowTest {

    @GameTest(setupTicks = 50)
    @PrefixGameTestTemplate(false)
    public static void testOnlySameColourFollows(GameTestHelper testHelper) {
        CaravanLeaderEntity caravanLeader = testHelper.getEntities(CaravanMod.CARAVAN_LEADER.get(), new BlockPos(2, 2, 2), 2).get(0);
        Llama differentColour = spawnLlamaWithCarpet(testHelper, Items.PURPLE_CARPET);
        Llama sameColour1 = spawnLlamaWithCarpet(testHelper, Items.MAGENTA_CARPET);
        Llama sameColour2 = spawnLlamaWithCarpet(testHelper, Items.MAGENTA_CARPET);
        Llama noColour = testHelper.spawn(EntityType.LLAMA, new BlockPos(2, 2, 2));

        testHelper.runAtTickTime(0, () -> {
            if (caravanLeader.getCaravanHead().isPresent()) {
                testHelper.succeed();
            } else {
                testHelper.fail("Caravan leader should have claimed llama");
            }
        });

        testHelper.succeedWhen(() -> {
            testHelper.assertEntityProperty(sameColour1, llama -> llama.getCaravanHead() != null && llama.getCaravanHead().getSwag() == llama.getSwag(), "Llama should have the same colour as the caravan leader");
            testHelper.assertEntityProperty(sameColour2, llama -> llama.getCaravanHead() != null && llama.getCaravanHead().getSwag() == llama.getSwag(), "Llama should have the same colour as the caravan leader");
        });

        testHelper.failIfEver(() -> {
            if(differentColour.getCaravanHead() != null || noColour.getCaravanHead() != null) {
                testHelper.fail("Llama should not be part of the caravan");
            }

            testHelper.succeed();
        });
    }

    @GameTest(setupTicks = 50)
    @PrefixGameTestTemplate(false)
    public static void testAllColoursFollowNonColoured(GameTestHelper testHelper) {
        CaravanLeaderEntity caravanLeader = testHelper.getEntities(CaravanMod.CARAVAN_LEADER.get(), new BlockPos(2, 2, 2), 2).get(0);
        Llama differentColour = spawnLlamaWithCarpet(testHelper, Items.PURPLE_CARPET);
        Llama sameColour1 = spawnLlamaWithCarpet(testHelper, Items.MAGENTA_CARPET);
        Llama sameColour2 = spawnLlamaWithCarpet(testHelper, Items.MAGENTA_CARPET);
        Llama noColour = testHelper.spawn(EntityType.LLAMA, new BlockPos(2, 2, 2));

        testHelper.runAtTickTime(0, () -> {
            if (caravanLeader.getCaravanHead().isPresent()) {
                testHelper.succeed();
            } else {
                testHelper.fail("Caravan leader should have claimed llama");
            }
        });

        testHelper.succeedWhen(() -> {
            testHelper.assertEntityProperty(sameColour1, llama -> llama.getCaravanHead() != null && llama.getCaravanHead().getSwag() == llama.getSwag(), "Llama should have the same colour as the caravan leader");
            testHelper.assertEntityProperty(sameColour2, llama -> llama.getCaravanHead() != null && llama.getCaravanHead().getSwag() == llama.getSwag(), "Llama should have the same colour as the caravan leader");
            testHelper.assertEntityProperty(differentColour, llama -> llama.getCaravanHead() != null && llama.getCaravanHead().getSwag() == llama.getSwag(), "Llama should have the same colour as the caravan leader");
            testHelper.assertEntityProperty(noColour, llama -> llama.getCaravanHead() != null && llama.getCaravanHead().getSwag() == llama.getSwag(), "Llama should have the same colour as the caravan leader");
        });
    }

    @GameTest(setupTicks = 75)
    @PrefixGameTestTemplate(false)
    public static void testCannotFollowUnreachable(GameTestHelper testHelper) {
        CaravanLeaderEntity caravanLeader = testHelper.getEntities(CaravanMod.CARAVAN_LEADER.get(), new BlockPos(2, 2, 2), 2).get(0);
        Llama head = testHelper.getEntities(EntityType.LLAMA, new BlockPos(3,1,3), 2).get(0);
        Llama notTail = testHelper.getEntities(EntityType.LLAMA, new BlockPos(3,1,7), 2).get(0);

        testHelper.runAtTickTime(0, () -> {
            if (caravanLeader.getCaravanHead().isPresent()) {
                testHelper.succeed();
            } else {
                testHelper.fail("Caravan leader should have claimed llama");
            }
        });

        testHelper.failIfEver(() -> {
            if(notTail.inCaravan()) {
                testHelper.fail("Llama should not be part of the caravan");
            }

            if(caravanLeader.getCaravanHead().isPresent() && caravanLeader.getCaravanHead().get().equals(notTail.getUUID())) {
                testHelper.fail("Llama should not be part of the caravan");
            }

            testHelper.succeed();
        });
    }

    private static Llama spawnLlamaWithCarpet(GameTestHelper testHelper, Item magentaCarpet) {
        Llama llama = testHelper.spawn(EntityType.LLAMA, 2, 2, 3);
        llama.setTamed(true);
        llama.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null).ifPresent(iItemHandler -> {
            iItemHandler.insertItem(1, new ItemStack(magentaCarpet), false);
        });

        return llama;
    }

}
