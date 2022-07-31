package com.suppergerrie2.caravan;

import net.minecraft.world.entity.animal.horse.Llama;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import org.jetbrains.annotations.NotNull;

public class CaravanInventoryUtils {

    public static boolean caravanInventoryIsEmpty(Llama head) {
        if (head == null) return true;

        LazyOptional<IItemHandler> optionalInventory = head.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY);

        return optionalInventory.map(inventory -> {
            // Start at 2 since llamas have a swag slot and saddle slot
            for (int i = 2; i < inventory.getSlots(); i++) {
                if (!inventory.getStackInSlot(i).isEmpty()) return false;
            }

            return true;
        }).orElse(true) && caravanInventoryIsEmpty(head.caravanTail);
    }

    public static boolean caravanHasItems(Llama head) {
        if(head == null) return false;

        LazyOptional<IItemHandler> optionalInventory = head.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY);

        return optionalInventory.map(inventory -> {
            // Start at 2 since llamas have a swag slot and saddle slot
            for (int i = 2; i < inventory.getSlots(); i++) {
                if (!inventory.getStackInSlot(i).isEmpty()) return true;
            }

            return false;
        }).orElse(false) || caravanHasItems(head.caravanTail);
    }

    public static boolean caravanHasSpace(Llama head) {
        if (head == null) return false;

        LazyOptional<IItemHandler> optionalInventory = head.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY);

        return optionalInventory.map(inventory -> {
            // Start at 2 since llamas have a swag slot and saddle slot
            for (int i = 2; i < inventory.getSlots(); i++) {
                if (inventory.getStackInSlot(i).isEmpty()) return true;
            }

            return false;
        }).orElse(false) || caravanHasSpace(head.caravanTail);
    }

    public static ItemStack insertStackIntoCaravan(Llama head, ItemStack stack) {
        if(head == null) return stack;

        LazyOptional<IItemHandler> itemHandler = head.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY);

        if(!itemHandler.isPresent()) insertStackIntoCaravan(head.caravanTail, stack);
        IItemHandler handler = itemHandler.orElseThrow(() -> new IllegalStateException("Llama has no item handler"));

        stack = insertItemStackedIntoLlama(handler, stack, false);

        if(!stack.isEmpty()) stack = insertStackIntoCaravan(head.caravanTail, stack);

        return stack;
    }

    public static ItemStack extractStackFromCaravan(Llama head) {
        if(head == null) return ItemStack.EMPTY;

        LazyOptional<IItemHandler> itemHandler = head.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY);
        if(!itemHandler.isPresent()) return extractStackFromCaravan(head.caravanTail);
        IItemHandler handler = itemHandler.orElseThrow(() -> new IllegalStateException("Llama has no item handler"));

        ItemStack stack = extractItem(handler, false);

        if(stack.isEmpty()) stack = extractStackFromCaravan(head.caravanTail);

        return stack;
    }

    static ItemStack extractItem(IItemHandler handler, boolean simulate) {
        // Start at 2 since llamas have a swag slot and saddle slot
        for(int i = 2; i < handler.getSlots(); i++) {
            ItemStack stack = handler.getStackInSlot(i);
            if(!stack.isEmpty()) {
                if(!simulate) {
                   stack = handler.extractItem(i, handler.getSlotLimit(i), false);
                }
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }


    // These helper methods are from ItemHandlerHelper but modified to not insert into the first two slots

    @NotNull
    public static ItemStack insertItemStackedIntoLlama(IItemHandler inventory, @NotNull ItemStack stack, boolean simulate)
    {
        if (inventory == null || stack.isEmpty())
            return stack;

        // not stackable -> just insert into a new slot
        if (!stack.isStackable())
        {
            return insertItemIntoLlama(inventory, stack, simulate);
        }

        int sizeInventory = inventory.getSlots();

        // go through the inventory and try to fill up already existing items
        // Start at 2 since llamas have a swag slot and saddle slot
        for (int i = 2; i < sizeInventory; i++)
        {
            ItemStack slot = inventory.getStackInSlot(i);
            if (ItemHandlerHelper.canItemStacksStackRelaxed(slot, stack))
            {
                stack = inventory.insertItem(i, stack, simulate);

                if (stack.isEmpty())
                {
                    break;
                }
            }
        }

        // insert remainder into empty slots
        if (!stack.isEmpty())
        {
            // find empty slot
            // Start at 2 since llamas have a swag slot and saddle slot
            for (int i = 2; i < sizeInventory; i++)
            {
                if (inventory.getStackInSlot(i).isEmpty())
                {
                    stack = inventory.insertItem(i, stack, simulate);
                    if (stack.isEmpty())
                    {
                        break;
                    }
                }
            }
        }

        return stack;
    }

    @NotNull
    public static ItemStack insertItemIntoLlama(IItemHandler dest, @NotNull ItemStack stack, boolean simulate)
    {
        if (dest == null || stack.isEmpty())
            return stack;

        // Start at 2 since llamas have a swag slot and saddle slot
        for (int i = 2; i < dest.getSlots(); i++)
        {
            stack = dest.insertItem(i, stack, simulate);
            if (stack.isEmpty())
            {
                return ItemStack.EMPTY;
            }
        }

        return stack;
    }
}
