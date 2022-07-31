package com.suppergerrie2.caravan;

import com.google.common.collect.ImmutableList;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.Pair;
import it.unimi.dsi.fastutil.objects.ObjectObjectImmutablePair;
import net.minecraft.core.GlobalPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.util.*;

public class CaravanBlockAssignmentManager extends SavedData {

    private static final Logger LOGGER = LogUtils.getLogger();

    private static final String DATA_NAME = "scaravan:caravan_block_assignment_manager";

    private final HashMap<GlobalPos, UUID> posToOwner = new HashMap<>();
    private final HashMap<UUID, Pair<GlobalPos, GlobalPos>> ownerToPositions = new HashMap<>();

    @Override
    public @NotNull CompoundTag save(@NotNull CompoundTag compoundTag) {
        ListTag listTag = new ListTag();

        for (Map.Entry<UUID, Pair<GlobalPos, GlobalPos>> posToOwnerEntry : ownerToPositions.entrySet()) {
            CompoundTag tag = new CompoundTag();

            Pair<GlobalPos, GlobalPos> sourceDestPair = posToOwnerEntry.getValue();

            if (sourceDestPair.first() != null)
                GlobalPos.CODEC.encodeStart(NbtOps.INSTANCE, posToOwnerEntry.getValue().first()).resultOrPartial(LOGGER::error).ifPresent(p -> tag.put("source", p));
            if (sourceDestPair.second() != null)
                GlobalPos.CODEC.encodeStart(NbtOps.INSTANCE, posToOwnerEntry.getValue().second()).resultOrPartial(LOGGER::error).ifPresent(p -> tag.put("dest", p));

            tag.putUUID("owner", posToOwnerEntry.getKey());
            listTag.add(tag);
        }

        compoundTag.put("posToOwner", listTag);

        return compoundTag;
    }

    private static @NotNull CaravanBlockAssignmentManager load(@NotNull CompoundTag compoundTag) {
        CaravanBlockAssignmentManager caravanBlockAssignmentManager = new CaravanBlockAssignmentManager();

        ListTag listTag = compoundTag.getList("posToOwner", Tag.TAG_COMPOUND);

        for (int i = 0; i < listTag.size(); i++) {
            CompoundTag tag = listTag.getCompound(i);

            Optional<GlobalPos> source = GlobalPos.CODEC.parse(NbtOps.INSTANCE, tag.getCompound("source")).resultOrPartial(LOGGER::trace);
            Optional<GlobalPos> dest = GlobalPos.CODEC.parse(NbtOps.INSTANCE, tag.getCompound("dest")).resultOrPartial(LOGGER::trace);
            UUID owner = tag.getUUID("owner");

            source.ifPresent(s -> caravanBlockAssignmentManager.posToOwner.put(s, owner));
            dest.ifPresent(d -> caravanBlockAssignmentManager.posToOwner.put(d, owner));
            caravanBlockAssignmentManager.ownerToPositions.put(owner, new ObjectObjectImmutablePair<>(source.orElse(null), dest.orElse(null)));
        }

        return caravanBlockAssignmentManager;
    }

    public static @NotNull CaravanBlockAssignmentManager getInstance(ServerLevel serverLevel) {
        return serverLevel.getDataStorage().computeIfAbsent(CaravanBlockAssignmentManager::load, CaravanBlockAssignmentManager::new, DATA_NAME);
    }

    public List<UUID> getAllOwners() {
        return ImmutableList.copyOf(ownerToPositions.keySet());
    }

    public List<GlobalPos> getAllPositions() {
        return ImmutableList.copyOf(posToOwner.keySet());
    }

    public static Optional<GlobalPos> getSource(ServerLevel level, UUID owner) {
        return getInstance(level).getSource(owner);
    }

    public Optional<GlobalPos> getSource(UUID owner) {
        return ownerToPositions.containsKey(owner) ? Optional.ofNullable(ownerToPositions.get(owner).first()) : Optional.empty();
    }

    public static Optional<GlobalPos> getDest(ServerLevel level, UUID owner) {
        return getInstance(level).getDest(owner);
    }

    public Optional<GlobalPos> getDest(UUID owner) {
        return ownerToPositions.containsKey(owner) ? Optional.ofNullable(ownerToPositions.get(owner).second()) : Optional.empty();
    }

    public static void addDest(ServerLevel level, GlobalPos pos, UUID owner) {
        getInstance(level).addDest(pos, owner);
    }

    public void addDest(GlobalPos globalPos, UUID owner) {
        if (posToOwner.containsKey(globalPos) && !posToOwner.get(globalPos).equals(owner)) {
            LOGGER.warn("Assigning an owner to a block that was already owned.");
        }

        setDirty();

        posToOwner.put(globalPos, owner);

        if (ownerToPositions.containsKey(owner)) {
            Pair<GlobalPos, GlobalPos> positions = ownerToPositions.get(owner);

            if (positions.second() != null) {
                LOGGER.warn("Assigning a destination to an owner that already has a destination.");
            }

            ownerToPositions.put(owner, new ObjectObjectImmutablePair<>(positions.first(), globalPos));
        } else {
            ownerToPositions.put(owner, new ObjectObjectImmutablePair<>(null, globalPos));
        }
    }

    public static void addSource(ServerLevel level, GlobalPos pos, UUID owner) {
        getInstance(level).addSource(pos, owner);
    }

    public void addSource(GlobalPos globalPos, UUID owner) {
        if (posToOwner.containsKey(globalPos) && !posToOwner.get(globalPos).equals(owner)) {
            LOGGER.warn("Assigning an owner to a block that was already owned.");
        }

        setDirty();

        posToOwner.put(globalPos, owner);

        if (ownerToPositions.containsKey(owner)) {
            Pair<GlobalPos, GlobalPos> positions = ownerToPositions.get(owner);

            if (positions.first() != null) {
                LOGGER.warn("Assigning a destination to an owner that already has a destination.");
            }

            ownerToPositions.put(owner, new ObjectObjectImmutablePair<>(globalPos, positions.second()));
        } else {
            ownerToPositions.put(owner, new ObjectObjectImmutablePair<>(globalPos, null));
        }
    }

    public static void removeDest(ServerLevel level, GlobalPos pos, UUID owner) {
        getInstance(level).removeDest(pos, owner);
    }

    public void removeDest(@NotNull GlobalPos globalPos, @NotNull UUID owner) {
        Pair<GlobalPos, GlobalPos> sourceDestPair = ownerToPositions.get(owner);
        if (sourceDestPair != null && globalPos.equals(sourceDestPair.second())) {
            posToOwner.remove(globalPos);
            setDirty();
        }

        if (ownerToPositions.containsKey(owner)) {
            Pair<GlobalPos, GlobalPos> posPair = ownerToPositions.get(owner);
            if (!globalPos.equals(posPair.second())) return;

            if (posPair.first() == null) ownerToPositions.remove(owner);
            else ownerToPositions.put(owner, new ObjectObjectImmutablePair<>(posPair.first(), null));

            setDirty();
        }
    }

    public static void removeSource(ServerLevel level, GlobalPos pos, UUID owner) {
        getInstance(level).removeSource(pos, owner);
    }

    public void removeSource(@NotNull GlobalPos globalPos, @NotNull UUID owner) {
        Pair<GlobalPos, GlobalPos> sourceDestPair = ownerToPositions.get(owner);
        if (sourceDestPair != null && globalPos.equals(sourceDestPair.first())) {
            posToOwner.remove(globalPos);
            setDirty();
        }

        if (ownerToPositions.containsKey(owner)) {
            Pair<GlobalPos, GlobalPos> posPair = ownerToPositions.get(owner);
            if (!globalPos.equals(posPair.first())) return;

            if (posPair.second() == null) ownerToPositions.remove(owner);
            else ownerToPositions.put(owner, new ObjectObjectImmutablePair<>(null, posPair.second()));
            setDirty();
        }
    }

    public boolean hasPosition(GlobalPos clickedPos) {
        return posToOwner.containsKey(clickedPos);
    }


    public boolean hasUUID(UUID uuid) {
        return ownerToPositions.containsKey(uuid);
    }

    public @NotNull Optional<UUID> getOwner(@NotNull GlobalPos globalPos) {
        return Optional.ofNullable(posToOwner.get(globalPos));
    }

    public interface CaravanBlockAddTarget {
        void addTarget(ServerLevel level, GlobalPos pos, UUID caravanLeaderId);
    }

    public interface CaravanBlockRemoveTarget {
        void removeTarget(ServerLevel level, GlobalPos pos, UUID caravanLeaderId);
    }

    public interface CaravanBlockGetTarget {
        Optional<GlobalPos> getTarget(ServerLevel level, UUID caravanLeaderId);
    }
}
