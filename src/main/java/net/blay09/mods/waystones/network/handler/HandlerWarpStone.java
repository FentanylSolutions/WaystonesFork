package net.blay09.mods.waystones.network.handler;

import net.blay09.mods.waystones.PlayerWaystoneData;
import net.blay09.mods.waystones.WaystoneManager;
import net.blay09.mods.waystones.Waystones;
import net.blay09.mods.waystones.block.TileWaystone;
import net.blay09.mods.waystones.network.message.MessageWarpStone;
import net.blay09.mods.waystones.util.BlockPos;
import net.blay09.mods.waystones.util.WaystoneEntry;
import net.blay09.mods.waystones.util.WaystoneXpCost;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatComponentTranslation;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;

public class HandlerWarpStone implements IMessageHandler<MessageWarpStone, IMessage> {

    @Override
    public IMessage onMessage(final MessageWarpStone message, final MessageContext ctx) {
        Waystones.proxy.addScheduledTask(() -> {
            EntityPlayerMP player = ctx.getServerHandler().playerEntity;
            WaystoneEntry target = WaystoneManager.resolveWarpTarget(player, message.getWaystone());
            WaystoneEntry source = message.getSourceWaystone();

            // Verify the source before selecting its independent cooldown.
            if (source != null) {
                if (message.isFree() || !isValidWaystoneSource(player, source)) {
                    return;
                }
            } else if (!message.isFree()) {
                ItemStack heldItem = player.getHeldItem();
                if (heldItem == null || heldItem.getItem() != Waystones.itemWarpStone) {
                    return;
                }
            }

            // Free warp validation
            if (message.isFree()) {
                if (!Waystones.getConfig().teleportButton || Waystones.getConfig().teleportButtonReturnOnly
                    || !PlayerWaystoneData.canFreeWarp(player)) {
                    return;
                }
            }

            if (target == null) {
                player.addChatMessage(new ChatComponentTranslation("waystones:waystoneBroken"));
                return;
            }

            // XP cost and cooldown enforcement
            if (!message.isFree() && !player.capabilities.isCreativeMode) {
                boolean canWarp = source != null ? PlayerWaystoneData.canUseWaystone(player, target)
                    : PlayerWaystoneData.canUseWarpStone(player, target);
                if (!canWarp) {
                    player.addChatMessage(new ChatComponentTranslation("gui.waystones:warpStone.cantWarpWaystone"));
                    return;
                }

                if (Waystones.getConfig().xpBaseCost > -1) {
                    int cost = WaystoneXpCost.getXpCost(player, target);

                    if (player.experienceLevel < cost) {
                        player.addChatMessage(new ChatComponentTranslation("gui.waystones:notEnoughXp", cost));
                        return;
                    }

                    player.addExperienceLevel(-cost);
                }
            }

            // Teleport
            if (WaystoneManager.teleportToWaystone(player, target)) {
                if (!player.capabilities.isCreativeMode) {
                    if (message.isFree()) {
                        PlayerWaystoneData.setLastFreeWarp(player, System.currentTimeMillis());
                    } else if (!PlayerWaystoneData.shouldIgnoreCooldown(target)) {
                        if (source != null) {
                            PlayerWaystoneData.setLastWaystoneUse(player, System.currentTimeMillis());
                        } else {
                            PlayerWaystoneData.setLastWarpStoneUse(player, System.currentTimeMillis());
                        }
                    }
                }
            }

            // Sync waystones
            WaystoneManager.sendPlayerWaystones(player);
        });
        return null;
    }

    private boolean isValidWaystoneSource(EntityPlayerMP player, WaystoneEntry source) {
        if (source.getDimensionId() != player.worldObj.provider.dimensionId) {
            return false;
        }
        BlockPos pos = source.getPos();
        if (player.getDistanceSq(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) > 64.0
            || !player.worldObj.blockExists(pos.getX(), pos.getY(), pos.getZ())) {
            return false;
        }
        TileEntity tile = player.worldObj.getTileEntity(pos.getX(), pos.getY(), pos.getZ());
        return tile instanceof TileWaystone && WaystoneManager.playerActivatedWaystone(player, (TileWaystone) tile);
    }
}
