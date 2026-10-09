package net.blay09.mods.waystones.client.gui;

import net.blay09.mods.waystones.PlayerWaystoneData;
import net.blay09.mods.waystones.WaystoneManager;
import net.blay09.mods.waystones.Waystones;
import net.blay09.mods.waystones.network.NetworkHandler;
import net.blay09.mods.waystones.network.message.MessagePinWaystone;
import net.blay09.mods.waystones.util.ClientUtil;
import net.blay09.mods.waystones.util.WaystoneEntry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

public class GuiButtonWaystone extends GuiButton {

    public static ResourceLocation orbResourceLocation = new ResourceLocation(
        Waystones.MODID,
        "textures/gui/xporb.png");

    public int sideButtonSize;
    public int crossButtonX;
    public int buttonY;
    public int pinButtonX;

    private final WaystoneEntry waystone;
    private final int xpCost;

    private final GuiWarpStone parentGui;
    public final String realName;
    public boolean pinned;

    public GuiButtonWaystone(int id, int x, int y, WaystoneEntry waystone, int xpCost, GuiWarpStone parentGui) {
        super(id, x, y, (waystone.isGlobal() ? EnumChatFormatting.YELLOW : "") + waystone.getName());
        this.width = 179;
        String finalName = waystone.getName();
        while (Minecraft.getMinecraft().fontRenderer.getStringWidth(finalName) > this.width - 90) {
            finalName = finalName.substring(0, finalName.length() - 1);
        }
        if (finalName.length() != waystone.getName()
            .length()) {
            finalName += "...";
        }
        this.realName = waystone.getName();
        this.displayString = (waystone.isGlobal() ? EnumChatFormatting.YELLOW : "") + finalName;
        this.waystone = waystone;
        this.xpCost = xpCost;
        this.parentGui = parentGui;

        sideButtonSize = 16;
        crossButtonX = this.xPosition + 2;
        buttonY = this.yPosition + 2;
        pinButtonX = crossButtonX + 18;

        this.pinned = false;
    }

    public WaystoneEntry getWaystone() {
        return waystone;
    }

    @Override
    public void drawButton(Minecraft mc, int mouseX, int mouseY) {
        refreshEnabledState();
        super.drawButton(mc, mouseX, mouseY);

        if (!parentGui.isFree() && Waystones.getConfig().xpBaseCost > -1
            && !Minecraft.getMinecraft().thePlayer.capabilities.isCreativeMode) {
            // Cost
            int color = (Minecraft.getMinecraft().thePlayer.experienceLevel >= xpCost) ? 0x36A336 : 0xFF5555;// 0x55FF55
                                                                                                             // :
                                                                                                             // 0xc75454;
            String s = String.valueOf(xpCost);

            int bakColor = ClientUtil.backupGLColor();
            if (!this.enabled) {
                color = ClientUtil.dimColor(color);
            }
            mc.fontRenderer
                .drawString(s, xPosition + width - 15 - mc.fontRenderer.getStringWidth(s), yPosition + 6, color, true);
            ClientUtil.restoreGLColor(bakColor);

            // XP orb
            ClientUtil.drawScaledTexture(orbResourceLocation, xPosition + width - 13, yPosition + 6, 8, 8, 8, 8);
        }

        GL11.glColor4f(1, 1, 1, 1);
        buttonY = this.yPosition + 2;
        crossButtonX = this.xPosition + 2;
        pinButtonX = crossButtonX + 18;

        if (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT)
            && ClientUtil.mouseOverArea(mouseX, mouseY, this.xPosition, this.yPosition, this.width, this.height)) {
            Minecraft.getMinecraft()
                .getTextureManager()
                .bindTexture(GuiWarpStone.menuResourceLocation);

            // Cross button
            if (ClientUtil.mouseOverArea(mouseX, mouseY, crossButtonX, buttonY, sideButtonSize, sideButtonSize)
                && !waystone.isGlobal()) {
                parentGui.hoveringOverRemoveButtonInList = true;
                Gui.func_152125_a(
                    crossButtonX,
                    buttonY,
                    116,
                    168,
                    16,
                    16,
                    sideButtonSize,
                    sideButtonSize,
                    256.0F,
                    256.0F);
                // Drawing hovering text leaks a ton of stuff, we draw it in the parentgui last
            } else {
                if (waystone.isGlobal()) {
                    GL11.glColor4f(0.5f, 0.5f, 0.5f, 1);
                }
                Gui.func_152125_a(
                    crossButtonX,
                    buttonY,
                    100,
                    168,
                    16,
                    16,
                    sideButtonSize,
                    sideButtonSize,
                    256.0F,
                    256.0F);
                if (waystone.isGlobal()) {
                    GL11.glColor4f(1, 1, 1, 1);
                }
            }

            // Pin button
            if (!this.pinned) {
                if (ClientUtil.mouseOverArea(mouseX, mouseY, pinButtonX, buttonY, sideButtonSize, sideButtonSize)) {
                    parentGui.hoveringOverPinButtonInList = true;
                    Gui.func_152125_a(
                        pinButtonX,
                        buttonY,
                        116,
                        152,
                        16,
                        16,
                        sideButtonSize,
                        sideButtonSize,
                        256.0F,
                        256.0F);
                } else {
                    Gui.func_152125_a(
                        pinButtonX,
                        buttonY,
                        100,
                        152,
                        16,
                        16,
                        sideButtonSize,
                        sideButtonSize,
                        256.0F,
                        256.0F);
                }
            } else {
                if (ClientUtil.mouseOverArea(mouseX, mouseY, pinButtonX, buttonY, sideButtonSize, sideButtonSize)) {
                    parentGui.hoveringOverUnpinButtonInList = true;
                    Gui.func_152125_a(
                        pinButtonX,
                        buttonY,
                        148,
                        152,
                        16,
                        16,
                        sideButtonSize,
                        sideButtonSize,
                        256.0F,
                        256.0F);
                } else {
                    Gui.func_152125_a(
                        pinButtonX,
                        buttonY,
                        132,
                        152,
                        16,
                        16,
                        sideButtonSize,
                        sideButtonSize,
                        256.0F,
                        256.0F);
                }
            }
        }

        if (!(Keyboard.isKeyDown(Keyboard.KEY_LSHIFT)
            && ClientUtil.mouseOverArea(mouseX, mouseY, this.xPosition, this.yPosition, this.width, this.height))
            && this.pinned) {
            Minecraft.getMinecraft()
                .getTextureManager()
                .bindTexture(GuiWarpStone.menuResourceLocation);
            Gui.func_152125_a(crossButtonX, buttonY, 164, 152, 16, 16, sideButtonSize, sideButtonSize, 256.0F, 256.0F);
        }
    }

    public void refreshEnabledState() {
        if (Minecraft.getMinecraft().thePlayer == null) {
            return;
        }

        this.enabled = true;
        if (!Minecraft.getMinecraft().thePlayer.capabilities.isCreativeMode) {
            if (waystone.getDimensionId() != Minecraft.getMinecraft().theWorld.provider.dimensionId
                && !WaystoneManager.isDimensionWarpAllowed(waystone)) {
                this.enabled = false;
            }

            if (!parentGui.isFree() && Waystones.getConfig().xpBaseCost > -1
                && Minecraft.getMinecraft().thePlayer.experienceLevel < xpCost) {
                this.enabled = false;
            }
        }
        if (!parentGui.canWarpTo(waystone)) {
            this.enabled = false;
        }
    }

    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT)) {
            if (ClientUtil.mouseOverArea(mouseX, mouseY, crossButtonX, buttonY, sideButtonSize, sideButtonSize)) {
                if (waystone.isGlobal()) {
                    return;
                }
                Waystones.debug("Clicked on " + waystone.getName() + " cross button");
                parentGui.openRemoveDialog(waystone, false);
                return;
            }
            if (ClientUtil.mouseOverArea(mouseX, mouseY, pinButtonX, buttonY, sideButtonSize, sideButtonSize)) {
                Waystones.debug("Clicked on " + waystone.getName() + " pin button");
                this.pinned = !this.pinned;
                PlayerWaystoneData.setWaystonePinned(Minecraft.getMinecraft().thePlayer, waystone, this.pinned);
                parentGui.refreshEntries();
                NetworkHandler.channel.sendToServer(new MessagePinWaystone(this.realName, this.pinned));
                return;
            }
        }

        if (!this.enabled) {
            return;
        }

        parentGui.teleportTo(waystone);
    }
}
