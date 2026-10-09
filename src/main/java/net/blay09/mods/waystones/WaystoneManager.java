package net.blay09.mods.waystones;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import net.blay09.mods.waystones.block.BlockWaystone;
import net.blay09.mods.waystones.block.TileWaystone;
import net.blay09.mods.waystones.network.NetworkHandler;
import net.blay09.mods.waystones.network.message.MessageJourneyMapWaypoint;
import net.blay09.mods.waystones.network.message.MessageTeleportEffect;
import net.blay09.mods.waystones.network.message.MessageWaystones;
import net.blay09.mods.waystones.util.BlockPos;
import net.blay09.mods.waystones.util.TeleporterNoPortalSeekBlock;
import net.blay09.mods.waystones.util.WaystoneEntry;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.common.util.ForgeDirection;

import com.google.common.collect.Maps;

import cpw.mods.fml.common.network.NetworkRegistry;

public class WaystoneManager {

    private static final Map<String, WaystoneEntry> serverWaystones = Maps.newHashMap();
    private static final Map<String, WaystoneEntry> knownWaystones = Maps.newHashMap();

    public static boolean playerActivatedWaystone(EntityPlayer player, TileWaystone waystone) {
        WaystoneEntry target = new WaystoneEntry(waystone);

        NBTTagCompound tagCompound = PlayerWaystoneData.getWaystonesTag(player);
        NBTTagList tagList = tagCompound.getTagList(PlayerWaystoneData.WAYSTONE_LIST, Constants.NBT.TAG_COMPOUND);
        for (int i = 0; i < tagList.tagCount(); i++) {
            WaystoneEntry entry = WaystoneEntry.read(tagList.getCompoundTagAt(i));
            if (entry.equals(target)) {
                return true;
            }
        }

        for (WaystoneEntry entry : getServerWaystones()) {
            if (entry.equals(target)) {
                return true;
            }
        }

        return false;
    }

    public static boolean playerKnowsAboutWaystone(EntityPlayer player, WaystoneEntry waystoneEntry) {
        if (waystoneEntry.isGlobal()) {
            return true;
        }
        boolean playerKnewAboutWaystone = false;
        WaystoneEntry[] entries = PlayerWaystoneData.fromPlayer(player)
            .getWaystones();
        for (WaystoneEntry entry : entries) {
            if (entry.equals(waystoneEntry)) {
                playerKnewAboutWaystone = true;
                break;
            }
        }
        return playerKnewAboutWaystone;
    }

    public static void activateWaystone(EntityPlayer player, TileWaystone waystone) {
        if (waystone.shouldForceGlobalOnActivation() && !waystone.getWaystoneName()
            .isEmpty()) {
            addServerWaystone(new WaystoneEntry(waystone));
            waystone.setForceGlobalOnActivation(false);
        }

        WaystoneEntry serverWaystone = getServerWaystone(waystone.getWaystoneName());
        if (serverWaystone != null) {
            PlayerWaystoneData.setLastServerWaystone(player, serverWaystone);
            sendPlayerWaystones(player);
            sendJourneyMapWaypoint(player, waystone);
            if (!playerKnowsAboutWaystone(player, serverWaystone)) {
                BlockWaystone.sendActivationChatMessage(player, waystone);
            }
            return;
        }
        serverWaystone = new WaystoneEntry(waystone);
        PlayerWaystoneData.resetLastServerWaystone(player);
        boolean playerKnows = playerKnowsAboutWaystone(player, serverWaystone);
        removePlayerWaystone(player, serverWaystone);
        addPlayerWaystone(player, waystone);
        sendPlayerWaystones(player);
        sendJourneyMapWaypoint(player, waystone);
        if (!playerKnows) {
            BlockWaystone.sendActivationChatMessage(player, waystone);
        }
    }

    private static void sendJourneyMapWaypoint(EntityPlayer player, TileWaystone waystone) {
        if (player instanceof EntityPlayerMP && waystone.getWorldObj() != null) {
            NetworkHandler.channel.sendTo(
                new MessageJourneyMapWaypoint(
                    waystone.getWaystoneName(),
                    waystone.getWorldObj().provider.dimensionId,
                    new BlockPos(waystone)),
                (EntityPlayerMP) player);
        }
    }

    public static void sendPlayerWaystones(EntityPlayer player) {
        if (player instanceof EntityPlayerMP) {
            PlayerWaystoneData waystoneData = PlayerWaystoneData.fromPlayer(player);
            NetworkHandler.channel.sendTo(
                new MessageWaystones(
                    waystoneData.getWaystones(),
                    getServerWaystones().toArray(new WaystoneEntry[getServerWaystones().size()]),
                    waystoneData.getLastServerWaystoneName(),
                    waystoneData.getLastFreeWarp(),
                    waystoneData.getLastWarpStoneUse(),
                    waystoneData.getLastWaystoneUse(),
                    waystoneData.getPinnedWaystones()),
                (EntityPlayerMP) player);
        }
    }

    public static void addPlayerWaystone(EntityPlayer player, TileWaystone waystone) {
        NBTTagCompound tagCompound = PlayerWaystoneData.getOrCreateWaystonesTag(player);
        NBTTagList tagList = tagCompound.getTagList(PlayerWaystoneData.WAYSTONE_LIST, Constants.NBT.TAG_COMPOUND);
        tagList.appendTag(new WaystoneEntry(waystone).writeToNBT());
        tagCompound.setTag(PlayerWaystoneData.WAYSTONE_LIST, tagList);
    }

    public static boolean removePlayerWaystone(EntityPlayer player, WaystoneEntry waystone) {
        NBTTagCompound tagCompound = PlayerWaystoneData.getWaystonesTag(player);
        NBTTagList tagList = tagCompound.getTagList(PlayerWaystoneData.WAYSTONE_LIST, Constants.NBT.TAG_COMPOUND);
        for (int i = 0; i < tagList.tagCount(); i++) {
            NBTTagCompound entryCompound = tagList.getCompoundTagAt(i);
            if (WaystoneEntry.read(entryCompound)
                .equals(waystone)) {
                tagList.removeTag(i);
                return true;
            }
        }
        return false;
    }

    public static boolean checkAndUpdateWaystone(EntityPlayer player, WaystoneEntry waystone) {
        WaystoneEntry serverEntry = getServerWaystone(waystone.getName());
        if (serverEntry != null) {
            if (getWaystoneInWorld(serverEntry) == null) {
                removeServerWaystone(serverEntry);
                PlayerWaystoneData.setWaystonePinned(player, waystone, false);
                return false;
            }
            if (removePlayerWaystone(player, waystone)) {
                sendPlayerWaystones(player);
            }
            return true;
        }
        NBTTagCompound tagCompound = PlayerWaystoneData.getWaystonesTag(player);
        NBTTagList tagList = tagCompound.getTagList(PlayerWaystoneData.WAYSTONE_LIST, Constants.NBT.TAG_COMPOUND);
        for (int i = 0; i < tagList.tagCount(); i++) {
            NBTTagCompound entryCompound = tagList.getCompoundTagAt(i);
            if (WaystoneEntry.read(entryCompound)
                .equals(waystone)) {
                TileWaystone tileEntity = getWaystoneInWorld(waystone);
                if (tileEntity != null) {
                    if (!entryCompound.getString("Name")
                        .equals(tileEntity.getWaystoneName())) {
                        entryCompound.setString("Name", tileEntity.getWaystoneName());
                        sendPlayerWaystones(player);
                    }
                    return true;
                } else {
                    removePlayerWaystone(player, waystone);
                    PlayerWaystoneData.setWaystonePinned(player, waystone, false);
                    sendPlayerWaystones(player);
                }
                return false;
            }
        }
        return false;
    }

    public static TileWaystone getWaystoneInWorld(WaystoneEntry waystone) {
        World targetWorld = MinecraftServer.getServer()
            .worldServerForDimension(waystone.getDimensionId());
        int x = waystone.getPos()
            .getX();
        int y = waystone.getPos()
            .getY();
        int z = waystone.getPos()
            .getZ();
        if (!targetWorld.blockExists(x, y, z)) {
            targetWorld.getChunkProvider()
                .loadChunk(x >> 4, z >> 4);
        }
        TileEntity tileEntity = targetWorld.getTileEntity(
            waystone.getPos()
                .getX(),
            waystone.getPos()
                .getY(),
            waystone.getPos()
                .getZ());
        if (tileEntity instanceof TileWaystone) {
            Waystones.debug("getWaystoneInWorld found waystone " + ((TileWaystone) tileEntity).getWaystoneName());
            return (TileWaystone) tileEntity;
        }
        Waystones.debug("getWaystoneInWorld didnt find waystone");
        return null;
    }

    public static boolean isDimensionWarpAllowed(WaystoneEntry waystone) {
        return waystone.isGlobal() ? Waystones.getConfig().globalInterDimension : Waystones.getConfig().interDimension;
    }

    public static WaystoneEntry resolveWarpTarget(EntityPlayer player, WaystoneEntry waystone) {
        if (waystone == null) {
            return null;
        }

        WaystoneEntry serverEntry = getServerWaystone(waystone.getName());
        if (serverEntry != null) {
            return serverEntry;
        }

        WaystoneEntry[] playerEntries = PlayerWaystoneData.fromPlayer(player)
            .getWaystones();
        for (WaystoneEntry entry : playerEntries) {
            if (entry.equals(waystone)) {
                return entry;
            }
        }

        return null;
    }

    public static boolean teleportToWaystone(EntityPlayer player, WaystoneEntry waystone) {
        if (!checkAndUpdateWaystone(player, waystone)) {
            ChatComponentTranslation chatComponent = new ChatComponentTranslation("waystones:waystoneBroken");
            chatComponent.getChatStyle()
                .setColor(EnumChatFormatting.RED);
            player.addChatComponentMessage(chatComponent);
            return false;
        }

        WaystoneEntry resolvedWaystone = resolveWarpTarget(player, waystone);
        if (resolvedWaystone == null) {
            ChatComponentTranslation chatComponent = new ChatComponentTranslation("waystones:waystoneBroken");
            chatComponent.getChatStyle()
                .setColor(EnumChatFormatting.RED);
            player.addChatComponentMessage(chatComponent);
            return false;
        }

        World targetWorld = MinecraftServer.getServer()
            .worldServerForDimension(resolvedWaystone.getDimensionId());
        int x = resolvedWaystone.getPos()
            .getX();
        int y = resolvedWaystone.getPos()
            .getY();
        int z = resolvedWaystone.getPos()
            .getZ();
        ForgeDirection facing = ForgeDirection.getOrientation(targetWorld.getBlockMetadata(x, y, z));
        BlockPos targetPos = getSafeTeleportPosition(targetWorld, player, resolvedWaystone.getPos(), facing);
        boolean dimensionWarp = resolvedWaystone.getDimensionId() != player.getEntityWorld().provider.dimensionId;
        if (!player.capabilities.isCreativeMode && dimensionWarp && !isDimensionWarpAllowed(resolvedWaystone)) {
            player.addChatComponentMessage(new ChatComponentTranslation("waystones:noDimensionWarp"));
            return false;
        }

        sendTeleportEffect(player.worldObj, new BlockPos(player), false);
        player.addPotionEffect(new PotionEffect(Potion.blindness.getId(), 20, 3));
        if (dimensionWarp) {
            MinecraftServer.getServer()
                .getConfigurationManager()
                .transferPlayerToDimension(
                    (EntityPlayerMP) player,
                    resolvedWaystone.getDimensionId(),
                    new TeleporterNoPortalSeekBlock(
                        net.minecraftforge.common.DimensionManager.getWorld(resolvedWaystone.getDimensionId())));
        }
        player.rotationYaw = getRotationYaw(facing);
        player.setPositionAndUpdate(targetPos.getX() + 0.5, targetPos.getY(), targetPos.getZ() + 0.5);
        sendTeleportEffect(player.worldObj, targetPos, true);
        return true;
    }

    private static BlockPos getSafeTeleportPosition(World world, EntityPlayer player, BlockPos waystonePos,
        ForgeDirection preferredFacing) {
        BlockPos preferred = waystonePos.offset(preferredFacing);
        if (canStandWithoutColliding(world, player, preferred)) {
            return preferred;
        }

        ForgeDirection[] sides = new ForgeDirection[] { ForgeDirection.NORTH, ForgeDirection.SOUTH, ForgeDirection.WEST,
            ForgeDirection.EAST };
        for (ForgeDirection side : sides) {
            if (side == preferredFacing) {
                continue;
            }
            BlockPos candidate = waystonePos.offset(side);
            if (canStandWithoutColliding(world, player, candidate)) {
                return candidate;
            }
        }

        return preferred;
    }

    private static boolean canStandWithoutColliding(World world, EntityPlayer player, BlockPos pos) {
        double x = pos.getX() + 0.5;
        double y = pos.getY();
        double z = pos.getZ() + 0.5;
        AxisAlignedBB bb = AxisAlignedBB.getBoundingBox(
            x - player.width / 2.0,
            y,
            z - player.width / 2.0,
            x + player.width / 2.0,
            y + player.height,
            z + player.width / 2.0);
        List<?> collisions = world.getCollidingBoundingBoxes(player, bb);
        return collisions == null || collisions.isEmpty();
    }

    public static void sendTeleportEffect(World world, BlockPos pos, boolean playSound) {
        NetworkHandler.channel.sendToAllAround(
            new MessageTeleportEffect(pos, playSound),
            new NetworkRegistry.TargetPoint(world.provider.dimensionId, pos.getX(), pos.getY(), pos.getZ(), 64));
    }

    public static float getRotationYaw(ForgeDirection facing) {
        switch (facing) {
            case NORTH:
                return 180f;
            case SOUTH:
                return 0f;
            case WEST:
                return 90f;
            case EAST:
                return -90f;
        }
        return 0f;
    }

    public static void addServerWaystone(WaystoneEntry entry) {
        entry.setGlobal(true);
        serverWaystones.put(entry.getName(), entry);
        GlobalWaystoneData.save(
            MinecraftServer.getServer()
                .getEntityWorld());
    }

    public static void removeServerWaystone(WaystoneEntry entry) {
        serverWaystones.remove(entry.getName());
        GlobalWaystoneData.save(
            MinecraftServer.getServer()
                .getEntityWorld());
    }

    public static void setServerWaystones(WaystoneEntry[] entries) {
        serverWaystones.clear();
        for (WaystoneEntry entry : entries) {
            entry.setGlobal(true);
            serverWaystones.put(entry.getName(), entry);
        }
    }

    public static void setKnownWaystones(WaystoneEntry[] entries) {
        knownWaystones.clear();
        for (WaystoneEntry entry : entries) {
            knownWaystones.put(entry.getName(), entry);
        }
    }

    public static WaystoneEntry getKnownWaystone(String name) {
        return knownWaystones.get(name);
    }

    public static Collection<WaystoneEntry> getServerWaystones() {
        return serverWaystones.values();
    }

    public static WaystoneEntry getServerWaystone(String name) {
        return serverWaystones.get(name);
    }

    public static Collection<WaystoneEntry> getKnownWaystones() {
        return knownWaystones.values();
    }

    public static void removeKnownWaystone(String name) {
        knownWaystones.remove(name);
    }

    public static void addKnownWaystone(WaystoneEntry entry) {
        knownWaystones.put(entry.getName(), entry);
    }

    public static void removeServerWaystoneByName(String name) {
        serverWaystones.remove(name);
    }

    public static void addServerWaystoneDirectly(WaystoneEntry entry) {
        serverWaystones.put(entry.getName(), entry);
    }
}
