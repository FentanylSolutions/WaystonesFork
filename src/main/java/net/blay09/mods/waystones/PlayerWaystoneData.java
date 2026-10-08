package net.blay09.mods.waystones;

import java.util.ArrayList;
import java.util.List;

import net.blay09.mods.waystones.util.WaystoneCooldown;
import net.blay09.mods.waystones.util.WaystoneEntry;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraftforge.common.util.Constants;

public class PlayerWaystoneData {

    public static final String WAYSTONES = "Waystones";
    public static final String WAYSTONE_LIST = "WaystoneList";
    public static final String LAST_FREE_WARP = "LastFreeWarp";
    public static final String LAST_WARP_STONE_USE = "LastWarpStoneUse";
    public static final String LAST_SERVER_WAYSTONE = "LastWaystone";
    public static final String PINNED_WAYSTONES = "PinnedWaystone";

    private final WaystoneEntry[] entries;
    private final String lastServerWaystoneName;
    private final long lastFreeWarp;
    private final long lastWarpStoneUse;
    private final String[] pinnedWaystones;

    public PlayerWaystoneData(WaystoneEntry[] entries, String lastServerWaystoneName, long lastFreeWarp,
        long lastWarpStoneUse, String[] pinnedWaystones) {
        this.entries = entries;
        this.lastServerWaystoneName = lastServerWaystoneName;
        this.lastFreeWarp = lastFreeWarp;
        this.lastWarpStoneUse = lastWarpStoneUse;
        this.pinnedWaystones = pinnedWaystones;
    }

    public WaystoneEntry[] getWaystones() {
        return entries;
    }

    public long getLastFreeWarp() {
        return lastFreeWarp;
    }

    public long getLastWarpStoneUse() {
        return lastWarpStoneUse;
    }

    public static NBTTagCompound getWaystonesTag(EntityPlayer player) {
        return player.getEntityData()
            .getCompoundTag(EntityPlayer.PERSISTED_NBT_TAG)
            .getCompoundTag(WAYSTONES);
    }

    public static NBTTagCompound getOrCreateWaystonesTag(EntityPlayer player) {
        NBTTagCompound persistedTag = player.getEntityData()
            .getCompoundTag(EntityPlayer.PERSISTED_NBT_TAG);
        NBTTagCompound waystonesTag = persistedTag.getCompoundTag(WAYSTONES);
        persistedTag.setTag(WAYSTONES, waystonesTag);
        player.getEntityData()
            .setTag(EntityPlayer.PERSISTED_NBT_TAG, persistedTag);
        return waystonesTag;
    }

    public static PlayerWaystoneData fromPlayer(EntityPlayer player) {
        NBTTagCompound tagCompound = getWaystonesTag(player);
        NBTTagList tagList = tagCompound.getTagList(WAYSTONE_LIST, Constants.NBT.TAG_COMPOUND);
        WaystoneEntry[] entries = new WaystoneEntry[tagList.tagCount()];
        for (int i = 0; i < entries.length; i++) {
            entries[i] = WaystoneEntry.read(tagList.getCompoundTagAt(i));
        }
        String lastServerWaystoneName = tagCompound.getString(LAST_SERVER_WAYSTONE);
        long lastFreeWarp = tagCompound.getLong(LAST_FREE_WARP);
        long lastWarpStoneUse = tagCompound.getLong(LAST_WARP_STONE_USE);
        NBTTagList pinnedTagList = tagCompound.getTagList(PINNED_WAYSTONES, Constants.NBT.TAG_STRING);
        String[] pinnedWaystones = new String[pinnedTagList.tagCount()];
        for (int i = 0; i < pinnedWaystones.length; i++) {
            pinnedWaystones[i] = pinnedTagList.getStringTagAt(i);
        }
        return new PlayerWaystoneData(entries, lastServerWaystoneName, lastFreeWarp, lastWarpStoneUse, pinnedWaystones);
    }

    public static void store(EntityPlayer player, WaystoneEntry[] entries, String lastServerWaystoneName,
        long lastFreeWarp, long lastWarpStoneUse, String[] pinnedWaystones) {
        NBTTagCompound tagCompound = getOrCreateWaystonesTag(player);
        NBTTagList tagList = new NBTTagList();
        for (WaystoneEntry entry : entries) {
            tagList.appendTag(entry.writeToNBT());
        }
        tagCompound.setTag(WAYSTONE_LIST, tagList);
        tagCompound.setString(LAST_SERVER_WAYSTONE, lastServerWaystoneName);
        tagCompound.setLong(LAST_FREE_WARP, lastFreeWarp);
        tagCompound.setLong(LAST_WARP_STONE_USE, lastWarpStoneUse);
        NBTTagList pinnedList = new NBTTagList();
        for (String s : pinnedWaystones) {
            pinnedList.appendTag(new NBTTagString(s));
        }
        tagCompound.setTag(PINNED_WAYSTONES, pinnedList);
    }

    public static void setLastServerWaystone(EntityPlayer player, WaystoneEntry waystone) {
        getOrCreateWaystonesTag(player).setString(LAST_SERVER_WAYSTONE, waystone.getName());
    }

    public static void resetLastServerWaystone(EntityPlayer player) {
        getWaystonesTag(player).removeTag(LAST_SERVER_WAYSTONE);
    }

    public static WaystoneEntry getLastWaystone(EntityPlayer player) {
        NBTTagCompound tagCompound = getWaystonesTag(player);
        NBTTagList tagList = tagCompound.getTagList(WAYSTONE_LIST, Constants.NBT.TAG_COMPOUND);
        WaystoneEntry lastServerWaystone = WaystoneManager
            .getServerWaystone(tagCompound.getString(LAST_SERVER_WAYSTONE));
        if (lastServerWaystone != null) {
            return lastServerWaystone;
        }
        if (tagList.tagCount() > 0) {
            return WaystoneEntry.read(tagList.getCompoundTagAt(tagList.tagCount() - 1));
        }
        return null;
    }

    public static boolean canFreeWarp(EntityPlayer player) {
        return getFreeWarpCooldownRemaining(player) == 0L;
    }

    public static boolean canUseWarpStone(EntityPlayer player) {
        return player.capabilities.isCreativeMode || getWarpStoneCooldownRemaining(player) == 0L;
    }

    public static long getFreeWarpCooldownRemaining(EntityPlayer player) {
        return WaystoneCooldown.getRemainingMillis(
            getLastFreeWarp(player),
            Waystones.getConfig().teleportButtonCooldown,
            System.currentTimeMillis());
    }

    public static long getWarpStoneCooldownRemaining(EntityPlayer player) {
        return WaystoneCooldown.getRemainingMillis(
            getLastWarpStoneUse(player),
            Waystones.getConfig().warpStoneCooldown,
            System.currentTimeMillis());
    }

    public static float getWarpStoneCooldownProgress(EntityPlayer player) {
        return WaystoneCooldown.getProgress(
            getLastWarpStoneUse(player),
            Waystones.getConfig().warpStoneCooldown,
            System.currentTimeMillis());
    }

    public static boolean shouldIgnoreWarpStoneCooldown(WaystoneEntry waystone) {
        return waystone != null && waystone.isGlobal() && Waystones.getConfig().globalNoCooldown;
    }

    public static boolean canUseWarpStone(EntityPlayer player, WaystoneEntry waystone) {
        return shouldIgnoreWarpStoneCooldown(waystone) || canUseWarpStone(player);
    }

    public static void setLastFreeWarp(EntityPlayer player, long lastFreeWarp) {
        PlayerWaystoneData.getOrCreateWaystonesTag(player)
            .setLong(PlayerWaystoneData.LAST_FREE_WARP, lastFreeWarp);
    }

    public static long getLastFreeWarp(EntityPlayer player) {
        return PlayerWaystoneData.getWaystonesTag(player)
            .getLong(PlayerWaystoneData.LAST_FREE_WARP);
    }

    public static void setLastWarpStoneUse(EntityPlayer player, long lastWarpStone) {
        PlayerWaystoneData.getOrCreateWaystonesTag(player)
            .setLong(PlayerWaystoneData.LAST_WARP_STONE_USE, lastWarpStone);
    }

    public static long getLastWarpStoneUse(EntityPlayer player) {
        return PlayerWaystoneData.getWaystonesTag(player)
            .getLong(PlayerWaystoneData.LAST_WARP_STONE_USE);
    }

    public String getLastServerWaystoneName() {
        return lastServerWaystoneName;
    }

    // Pinned Waystones handling
    public static boolean isWaystonePinned(EntityPlayer player, WaystoneEntry waystone) {
        NBTTagCompound tagCompound = getWaystonesTag(player);
        NBTTagList pinnedList = tagCompound.getTagList(PINNED_WAYSTONES, Constants.NBT.TAG_STRING);

        String waystoneName = waystone.getName();
        for (int i = 0; i < pinnedList.tagCount(); i++) {
            if (pinnedList.getStringTagAt(i)
                .equals(waystoneName)) {
                return true;
            }
        }
        return false;
    }

    public static void setWaystonePinned(EntityPlayer player, WaystoneEntry waystone, boolean isPinned) {
        NBTTagCompound tagCompound = getOrCreateWaystonesTag(player);
        NBTTagList pinnedList = tagCompound.getTagList(PINNED_WAYSTONES, Constants.NBT.TAG_STRING);

        String waystoneName = waystone.getName();

        if (isPinned) {
            boolean alreadyPinned = false;
            for (int i = 0; i < pinnedList.tagCount(); i++) {
                if (pinnedList.getStringTagAt(i)
                    .equals(waystoneName)) {
                    alreadyPinned = true;
                    break;
                }
            }
            if (!alreadyPinned) {
                pinnedList.appendTag(new NBTTagString(waystoneName));
            }
        } else {
            for (int i = 0; i < pinnedList.tagCount(); i++) {
                if (pinnedList.getStringTagAt(i)
                    .equals(waystoneName)) {
                    pinnedList.removeTag(i);
                    break;
                }
            }
        }

        tagCompound.setTag(PINNED_WAYSTONES, pinnedList);
    }

    public static List<String> getPinnedWaystoneNames(EntityPlayer player) {
        NBTTagCompound tagCompound = getWaystonesTag(player);
        NBTTagList pinnedList = tagCompound.getTagList(PINNED_WAYSTONES, Constants.NBT.TAG_STRING);

        List<String> pinnedNames = new ArrayList<>();
        for (int i = 0; i < pinnedList.tagCount(); i++) {
            pinnedNames.add(pinnedList.getStringTagAt(i));
        }
        return pinnedNames;
    }

    public String[] getPinnedWaystones() {
        return pinnedWaystones;
    }
}
