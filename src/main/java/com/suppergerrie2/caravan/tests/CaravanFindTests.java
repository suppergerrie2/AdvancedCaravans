package com.suppergerrie2.caravan.tests;

import com.suppergerrie2.caravan.CaravanMod;
import com.suppergerrie2.caravan.entity.CaravanLeaderEntity;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.horse.Llama;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(CaravanMod.MODID)
public class CaravanFindTests {

    @GameTest(template = "test_template")
    @PrefixGameTestTemplate(false)
    public static void canClaimLlama(GameTestHelper helper) {
        helper.setDayTime(0);
        CaravanLeaderEntity caravanLeader = helper.spawn(CaravanMod.CARAVAN_LEADER.get(), 1, 2, 1);
        Llama llama = helper.spawn(EntityType.LLAMA, 1, 2, 6);
        llama.setTamed(true);

        helper.succeedWhen(() -> {
            if (caravanLeader.getCaravanHead().map(head -> head.equals(llama.getUUID())).orElse(false)) {
                helper.assertEntityProperty(llama, Llama::getLeashHolder, "leashHolder", caravanLeader);

                helper.succeed();
            } else {
                helper.fail("Caravan leader is not the head of the caravan");
            }
        });
    }

    @GameTest(template = "test_template")
    @PrefixGameTestTemplate(false)
    public static void cannotClaimUntamedLlama(GameTestHelper helper) {
        helper.setDayTime(0);
        CaravanLeaderEntity caravanLeader = helper.spawn(CaravanMod.CARAVAN_LEADER.get(), 1, 2, 1);
        Llama llama = helper.spawn(EntityType.LLAMA, 1, 2, 6);
        llama.setTamed(false);

        assertNotClaimed(helper, caravanLeader);
    }

    private static void assertNotClaimed(GameTestHelper helper, CaravanLeaderEntity caravanLeader) {
        helper.setDayTime(0);
        helper.onEachTick(() -> {
            if (caravanLeader.getCaravanHead().isPresent()) {
                helper.fail("Cannot claim caged llama");
            }
        });

        helper.runAtTickTime(100, () -> {
            if(caravanLeader.getCaravanHead().isPresent()) {
                helper.fail("Cannot claim caged llama");
            } else {
                helper.succeed();
            }
        });
    }

    @GameTest(template = "test_template")
    @PrefixGameTestTemplate(false)
    public static void onlyOneClaimsLlama(GameTestHelper helper) {
        helper.setDayTime(0);
        CaravanLeaderEntity[] caravanLeaders = new CaravanLeaderEntity[32];
        for (int i = 0; i < caravanLeaders.length; i++) {
            caravanLeaders[i] = helper.spawn(CaravanMod.CARAVAN_LEADER.get(), 1, 2, 1);
        }

        Llama llama = helper.spawn(EntityType.LLAMA, 1, 2, 6);
        llama.setTamed(true);

        helper.succeedWhen(() -> {
            int amountClaimed = 0;
            for (CaravanLeaderEntity caravanLeader : caravanLeaders) {
                if (caravanLeader.getCaravanHead().map(head -> head.equals(llama.getUUID())).orElse(false)) {
                    amountClaimed++;
                    helper.assertEntityProperty(llama, Llama::getLeashHolder, "leashHolder", caravanLeader);

                    helper.succeed();
                }
            }

            if (amountClaimed == 0) {
                helper.fail("No caravan leader claimed the llama");
            }

            if (amountClaimed > 1) {
                helper.fail("More than one caravan leader claimed the llama");
            }
        });
    }

    @GameTest(template = "test_template_cages", timeoutTicks = 100)
    @PrefixGameTestTemplate(false)
    public static void cannotClaimCaged(GameTestHelper helper) {
        helper.setDayTime(0);
        CaravanLeaderEntity caravanLeader = helper.spawn(CaravanMod.CARAVAN_LEADER.get(), 1, 2, 1);
        Llama llama = helper.spawn(EntityType.LLAMA, 1, 2, 6);
        llama.setTamed(true);

        assertNotClaimed(helper, caravanLeader);
    }

}
