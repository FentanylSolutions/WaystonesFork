package net.blay09.mods.waystones.client.gui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import net.blay09.mods.waystones.PlayerWaystoneData;
import net.blay09.mods.waystones.WaystoneConfig;
import net.blay09.mods.waystones.WaystoneManager;
import net.blay09.mods.waystones.Waystones;
import net.blay09.mods.waystones.block.TileWaystone;
import net.blay09.mods.waystones.network.NetworkHandler;
import net.blay09.mods.waystones.network.message.MessageUnlearnWaystone;
import net.blay09.mods.waystones.network.message.MessageWarpStone;
import net.blay09.mods.waystones.util.ClientUtil;
import net.blay09.mods.waystones.util.WaystoneCooldown;
import net.blay09.mods.waystones.util.WaystoneEntry;
import net.blay09.mods.waystones.util.WaystoneXpCost;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.config.Configuration;

import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

public class GuiWarpStone extends GuiScreen {

    public final TileWaystone currentWaystone;
    public final TeleportSource source;
    private boolean isGlobal;
    private WaystoneEntry[] entries;
    private final boolean isFree;
    private GuiTextField searchBar;
    private WaystoneList waystoneList;
    private List<GuiButtonWaystone> lstEntries;

    private int helpButtonX;
    private int helpButtonY;
    private final int helpButtonSize = 20;

    private int deleteButtonX;
    private int deleteButtonY;
    private final int deleteButtonSize = 16;

    private int renameButtonX;
    private int renameButtonY;
    private final int renameButtonSize = 16;

    private int configButtonX;
    private int configButtonY;
    private final int configButtonSize = 20;

    private int sortButtonX;
    private int sortButtonY;
    private final int sortButtonSize = 20;

    public boolean hoveringOverRemoveButtonInList = false;
    public boolean hoveringOverPinButtonInList = false;
    public boolean hoveringOverUnpinButtonInList = false;

    public static ResourceLocation menuResourceLocation = new ResourceLocation(
        Waystones.MODID,
        "textures/gui/menu.png");

    public enum TeleportSource {
        WAYSTONE,
        GUI_BUTTON,
        WARPSTONE
    }

    public GuiWarpStone(TileWaystone currentWaystone, WaystoneEntry[] entries, boolean isFree, TeleportSource source) {
        this.currentWaystone = currentWaystone;
        this.isGlobal = false;
        // currentWaystone is null when invoked from scroll or warpstone
        if (currentWaystone != null) {
            // We need this, because only WaystoneEntries we get from the server are marked with global
            WaystoneEntry tmp = new WaystoneEntry(currentWaystone);
            for (WaystoneEntry entry : WaystoneManager.getServerWaystones()) {
                if (tmp.equals(entry)) {
                    this.isGlobal = entry.isGlobal();
                }
            }
        }
        this.entries = entries;
        this.isFree = isFree;
        this.source = source;
        lstEntries = new ArrayList<>();
    }

    @Override
    public void initGui() {
        searchBar = new GuiTextField(fontRendererObj, width / 2 - 100, height / 2 - 55, 200, 20);
        searchBar.setEnabled(true);
        searchBar.setMaxStringLength(1000);
        searchBar.setText("");

        waystoneList = new WaystoneList(mc, width / 2 - 92, height / 2 - 27, 179, 105, 20, 2, lstEntries, this);

        this.sortButtonX = waystoneList.x - 25;
        this.sortButtonY = waystoneList.y;

        this.configButtonX = waystoneList.x - 25;
        this.configButtonY = waystoneList.y + 23;

        this.helpButtonX = waystoneList.x - 25;
        this.helpButtonY = waystoneList.y + 45;

        updateList();
    }

    public void refreshEntries() {
        this.entries = WaystoneEntry.getCombinedWaystones(Minecraft.getMinecraft().thePlayer, true);
        updateList();
    }

    public void updateList() {
        buttonList.removeIf(guiButton -> guiButton instanceof GuiButtonWaystone);

        // int baseY = height / 2 - 60;
        int baseY = height / 2 - 30;
        int y = 0;
        lstEntries = new ArrayList<>();
        for (int i = 0; i < entries.length; i++) {
            if (WaystoneEntry.tileAndEntryShareCoords(entries[i], currentWaystone)) {
                continue;
            }
            if (!this.searchBar.getText()
                .isEmpty()
                && !entries[i].getName()
                    .toLowerCase()
                    .startsWith(
                        this.searchBar.getText()
                            .toLowerCase())) {
                continue;
            }
            int xpCost = -1;
            if (!isFree && !Minecraft.getMinecraft().thePlayer.capabilities.isCreativeMode) {
                xpCost = WaystoneXpCost.getXpCost(currentWaystone, entries[i], Minecraft.getMinecraft().thePlayer);
            }
            GuiButtonWaystone btnWaystone = new GuiButtonWaystone(
                2 + i,
                width / 2 - 100,
                baseY + y,
                entries[i],
                xpCost,
                this);

            btnWaystone.refreshEnabledState();

            lstEntries.add(btnWaystone);
            y += 22;
        }

        waystoneList.entries = lstEntries;
        waystoneList.update();
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button instanceof GuiButtonWaystone) {
            teleportTo(((GuiButtonWaystone) button).getWaystone());
        }
    }

    public void teleportTo(WaystoneEntry waystone) {
        WaystoneEntry origin = currentWaystone != null ? new WaystoneEntry(currentWaystone) : null;
        NetworkHandler.channel.sendToServer(new MessageWarpStone(waystone, isFree, origin));
        mc.displayGuiScreen(null);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        hoveringOverRemoveButtonInList = false;
        hoveringOverPinButtonInList = false;
        hoveringOverUnpinButtonInList = false;

        drawWorldBackground(0);
        super.drawScreen(mouseX, mouseY, partialTicks);
        GL11.glColor4f(1f, 1f, 1f, 1f);
        boolean hoveringDeleteButton = false;
        boolean hoveringRenameButton = false;
        GL11.glPushMatrix();
        GL11.glTranslatef((float) width / 2, (float) height / 2 - 110, 0);
        float scale = 1.5f;
        GL11.glScalef(scale, scale, scale);
        if (this.source == TeleportSource.WAYSTONE && currentWaystone != null) {
            drawCenteredString(
                fontRendererObj,
                (isGlobal ? EnumChatFormatting.YELLOW.toString() : "") + EnumChatFormatting.UNDERLINE
                    + currentWaystone.getWaystoneName(),
                3,
                0,
                0xFFFFFF); // Draw at scaled coords

            // Drawing the Waystone icon & delete button
            GL11.glColor4f(1, 1, 1, 1);
            Minecraft.getMinecraft()
                .getTextureManager()
                .bindTexture(menuResourceLocation);
            int strWidth = fontRendererObj.getStringWidth(currentWaystone.getWaystoneName());
            float transformedMouseX = (mouseX - width / 2f) / scale;
            float transformedMouseY = (mouseY - (height / 2f - 110)) / scale;

            if (isGlobal || !Keyboard.isKeyDown(Keyboard.KEY_LSHIFT)) {
                // Waystone icon
                Gui.func_152125_a(-strWidth / 2 - 7, -2, 19, 75, 20, 17, 15, 12, 256.0F, 256.0F);
            }

            // Switch to 1x scale for buttons
            GL11.glScalef(1.0f / scale, 1.0f / scale, 1.0f / scale);

            // Hover size in 1.5x coord system (matches 16px button at 1x scale)
            int hoverSize = (int) (deleteButtonSize / scale);

            if (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) && !isGlobal) { // remove button
                // Position in 1.5x coord system (for hover detection)
                this.deleteButtonX = -strWidth / 2 - 10;
                this.deleteButtonY = 0;
                // Draw position multiplied by scale (converts to 1x space)
                int drawDeleteX = (int) (this.deleteButtonX * scale);
                int drawDeleteY = (int) (this.deleteButtonY * scale);
                if (!ClientUtil.mouseOverArea(
                    (int) transformedMouseX,
                    (int) transformedMouseY,
                    this.deleteButtonX,
                    this.deleteButtonY,
                    hoverSize,
                    hoverSize)) {
                    Gui.func_152125_a(
                        drawDeleteX,
                        drawDeleteY,
                        100,
                        168,
                        16,
                        16,
                        deleteButtonSize,
                        deleteButtonSize,
                        256.0F,
                        256.0F);
                } else {
                    hoveringDeleteButton = true;
                    Gui.func_152125_a(
                        drawDeleteX,
                        drawDeleteY,
                        116,
                        168,
                        16,
                        16,
                        deleteButtonSize,
                        deleteButtonSize,
                        256.0F,
                        256.0F);
                }
            }

            // Edit name button
            if (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT)) {
                // Position in 1.5x coord system (for hover detection)
                this.renameButtonX = -strWidth / 2 - 22;
                if (isGlobal) {
                    this.renameButtonX = -strWidth / 2 - 10;
                }
                this.renameButtonY = 0;
                // Draw position multiplied by scale (converts to 1x space)
                int drawRenameX = (int) (this.renameButtonX * scale);
                int drawRenameY = (int) (this.renameButtonY * scale);

                if (!ClientUtil.mouseOverArea(
                    (int) transformedMouseX,
                    (int) transformedMouseY,
                    this.renameButtonX,
                    this.renameButtonY,
                    hoverSize,
                    hoverSize)) {
                    Gui.func_152125_a(
                        drawRenameX,
                        drawRenameY,
                        36,
                        40,
                        16,
                        16,
                        renameButtonSize,
                        renameButtonSize,
                        256.0F,
                        256.0F);
                } else {
                    hoveringRenameButton = true;
                    Gui.func_152125_a(
                        drawRenameX,
                        drawRenameY,
                        52,
                        40,
                        16,
                        16,
                        renameButtonSize,
                        renameButtonSize,
                        256.0F,
                        256.0F);
                }
            }
        } else if (this.source == TeleportSource.WARPSTONE) {
            drawCenteredString(fontRendererObj, I18n.format("gui.waystones:warpStone.title"), 3, 0, 0xFFFFFF);
        } else if (this.source == TeleportSource.GUI_BUTTON) {
            drawCenteredString(fontRendererObj, I18n.format("gui.waystones:guiButton.title"), 3, 0, 0xFFFFFF);
        }
        GL11.glPopMatrix();

        drawRect(width / 2 - 50, height / 2 - 50, width / 2 + 50, height / 2 + 50, 0xFFFFFF);
        if (canWarpTo(null) || hasCooldownBypassWaystone()) {
            drawCenteredString(
                fontRendererObj,
                I18n.format("gui.waystones:warpStone.selectDestination"),
                width / 2,
                height / 2 - 85,
                0xAAAAAA);
        } else {
            String cantWarpStr = I18n.format("gui.waystones:warpStone.cantWarpWaystone");
            int cantWarpStrWidth = fontRendererObj.getStringWidth(cantWarpStr);
            ClientUtil.drawSolidRect(
                Tessellator.instance,
                (double) width / 2 - (double) cantWarpStrWidth / 2 - 2,
                (double) height / 2 - 88,
                cantWarpStrWidth + 4,
                14,
                0x32FFFFFF);
            float progress = getCooldownProgress();
            ClientUtil.drawSolidRect(
                Tessellator.instance,
                (double) width / 2 - (double) cantWarpStrWidth / 2 - 2,
                (double) height / 2 - 88,
                (cantWarpStrWidth + 4) * progress,
                14,
                0xFFFFFFFF);
            fontRendererObj.drawString(
                cantWarpStr,
                width / 2 - fontRendererObj.getStringWidth(cantWarpStr) / 2,
                height / 2 - 85,
                0xe33042,
                true);
        }

        this.searchBar.drawTextBox();
        ClientUtil.drawTextFieldHint(this.searchBar, I18n.format("gui.waystones:searchWaystones"), fontRendererObj);

        waystoneList.draw(mouseX, mouseY, this.width, this.height);

        // Help button
        boolean hoveringHelpButton = ClientUtil.mouseOverArea(
            mouseX,
            mouseY,
            this.helpButtonX,
            this.helpButtonY,
            this.helpButtonSize,
            this.helpButtonSize);
        Minecraft.getMinecraft()
            .getTextureManager()
            .bindTexture(menuResourceLocation);
        GL11.glColor4f(1, 1, 1, 1);
        if (hoveringHelpButton) {
            Gui.func_152125_a(
                this.helpButtonX,
                this.helpButtonY,
                20,
                132,
                20,
                20,
                this.helpButtonSize,
                this.helpButtonSize,
                256.0F,
                256.0F);
        } else {
            Gui.func_152125_a(
                this.helpButtonX,
                this.helpButtonY,
                0,
                132,
                20,
                20,
                this.helpButtonSize,
                this.helpButtonSize,
                256.0F,
                256.0F);
        }

        // Sort Button
        boolean hoveringSortButton = ClientUtil.mouseOverArea(
            mouseX,
            mouseY,
            this.sortButtonX,
            this.sortButtonY,
            this.sortButtonSize,
            this.sortButtonSize);
        Minecraft.getMinecraft()
            .getTextureManager()
            .bindTexture(menuResourceLocation);
        GL11.glColor4f(1, 1, 1, 1);

        if (hoveringSortButton) {
            int offset = WaystoneConfig.sortingMode == 0 ? 80 : 100;
            Gui.func_152125_a(
                this.sortButtonX,
                this.sortButtonY,
                offset,
                92,
                20,
                20,
                this.sortButtonSize,
                this.sortButtonSize,
                256.0F,
                256.0F);
        } else {
            int offset = WaystoneConfig.sortingMode == 0 ? 20 : 40;
            Gui.func_152125_a(
                this.sortButtonX,
                this.sortButtonY,
                offset,
                92,
                20,
                20,
                this.sortButtonSize,
                this.sortButtonSize,
                256.0F,
                256.0F);
        }

        // Config button
        boolean hoveringConfigButton = ClientUtil.mouseOverArea(
            mouseX,
            mouseY,
            this.configButtonX,
            this.configButtonY,
            this.configButtonSize,
            this.configButtonSize);
        Minecraft.getMinecraft()
            .getTextureManager()
            .bindTexture(menuResourceLocation);
        GL11.glColor4f(1, 1, 1, 1);
        if (hoveringConfigButton) {
            Gui.func_152125_a(
                this.configButtonX,
                this.configButtonY,
                140,
                0,
                20,
                20,
                this.configButtonSize,
                this.configButtonSize,
                256.0F,
                256.0F);
        } else {
            Gui.func_152125_a(
                this.configButtonX,
                this.configButtonY,
                60,
                0,
                20,
                20,
                this.configButtonSize,
                this.configButtonSize,
                256.0F,
                256.0F);
        }

        // Drawing hovers at the end because they mess with the GL state
        if (hoveringHelpButton) {
            drawHoveringText(
                Arrays.asList(
                    I18n.format("gui.waystones:helpTooltip")
                        .split("\\R")),
                mouseX,
                mouseY,
                fontRendererObj);
        }
        if (hoveringConfigButton) {
            drawHoveringText(
                Collections.singletonList(I18n.format("gui.waystones:configTitle")),
                mouseX,
                mouseY,
                fontRendererObj);
        }
        if (hoveringSortButton) {
            String sortKey = WaystoneConfig.sortingMode == 0 ? "gui.waystones:sortingModeAlphabetical"
                : "gui.waystones:sortingModeDistance";
            drawHoveringText(
                Collections.singletonList(I18n.format("gui.waystones:sortingMode", I18n.format(sortKey))),
                mouseX,
                mouseY,
                fontRendererObj);
        }
        if (hoveringDeleteButton) {
            drawHoveringText(
                Collections.singletonList(I18n.format("gui.waystones:forget")),
                mouseX,
                mouseY,
                fontRendererObj);
        }
        if (hoveringRenameButton) {
            drawHoveringText(
                Collections.singletonList(I18n.format("gui.waystones:rename")),
                mouseX,
                mouseY,
                fontRendererObj);
        }
        if (hoveringOverRemoveButtonInList) {
            drawHoveringText(
                Collections.singletonList(I18n.format("gui.waystones:forget")),
                mouseX,
                mouseY,
                fontRendererObj);
        }
        if (hoveringOverPinButtonInList) {
            drawHoveringText(
                Collections.singletonList(I18n.format("gui.waystones:pin")),
                mouseX,
                mouseY,
                fontRendererObj);
        }
        if (hoveringOverUnpinButtonInList) {
            drawHoveringText(
                Collections.singletonList(I18n.format("gui.waystones:unpin")),
                mouseX,
                mouseY,
                fontRendererObj);
        }
    }

    public boolean isFree() {
        return isFree;
    }

    public boolean canWarpTo(WaystoneEntry waystone) {
        if (isFree) {
            return PlayerWaystoneData.canFreeWarp(Minecraft.getMinecraft().thePlayer);
        }
        return source == TeleportSource.WAYSTONE
            ? PlayerWaystoneData.canUseWaystone(Minecraft.getMinecraft().thePlayer, waystone)
            : PlayerWaystoneData.canUseWarpStone(Minecraft.getMinecraft().thePlayer, waystone);
    }

    private float getCooldownProgress() {
        if (isFree) {
            return WaystoneCooldown.getProgress(
                PlayerWaystoneData.getLastFreeWarp(Minecraft.getMinecraft().thePlayer),
                Waystones.getConfig().teleportButtonCooldown,
                System.currentTimeMillis());
        }
        return source == TeleportSource.WAYSTONE
            ? PlayerWaystoneData.getWaystoneCooldownProgress(Minecraft.getMinecraft().thePlayer)
            : PlayerWaystoneData.getWarpStoneCooldownProgress(Minecraft.getMinecraft().thePlayer);
    }

    private boolean hasCooldownBypassWaystone() {
        for (GuiButtonWaystone btn : lstEntries) {
            if (canWarpTo(btn.getWaystone())) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) {
        if (!this.searchBar.isFocused() && keyCode == mc.gameSettings.keyBindInventory.getKeyCode()) {
            mc.displayGuiScreen(null);
            return;
        }

        if (keyCode == Waystones.varInstanceClient.closeGuiKey.getKeyCode()
            && Waystones.varInstanceClient.closeGuiKey.getKeyCode() != Keyboard.KEY_NONE) {
            mc.displayGuiScreen(null);
            return;
        }

        super.keyTyped(typedChar, keyCode);

        this.searchBar.textboxKeyTyped(typedChar, keyCode);
        if (this.searchBar.isFocused()) {
            updateList();
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        super.mouseClicked(mouseX, mouseY, mouseButton);

        // Delete button
        float scale = 1.5f;
        float transformedMouseX = (mouseX - width / 2f) / scale;
        float transformedMouseY = (mouseY - (height / 2f - 110)) / scale;
        int hoverSize = (int) (deleteButtonSize / scale);
        if (!isGlobal && Keyboard.isKeyDown(Keyboard.KEY_LSHIFT)) {
            if (ClientUtil.mouseOverArea(
                (int) transformedMouseX,
                (int) transformedMouseY,
                this.deleteButtonX,
                this.deleteButtonY,
                hoverSize,
                hoverSize)) {
                openRemoveDialog(new WaystoneEntry(currentWaystone), true);
            }
        }
        if (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT)) {
            if (ClientUtil.mouseOverArea(
                (int) transformedMouseX,
                (int) transformedMouseY,
                this.renameButtonX,
                this.renameButtonY,
                hoverSize,
                hoverSize)) {
                Waystones.debug("Clicked rename button");
                Minecraft.getMinecraft()
                    .displayGuiScreen(new GuiWaystoneName(currentWaystone, true, this));
            }
        }

        // Sort button
        if (ClientUtil.mouseOverArea(
            mouseX,
            mouseY,
            this.sortButtonX,
            this.sortButtonY,
            this.sortButtonSize,
            this.sortButtonSize)) {
            WaystoneConfig.sortingMode = (WaystoneConfig.sortingMode + 1) % 2;
            refreshEntries();
            Configuration config = WaystoneConfig.getRawConfig();
            config.get(WaystoneConfig.Categories.client, "Sorting mode", 0)
                .set(WaystoneConfig.sortingMode);
            config.save();
        }

        // Config button
        if (ClientUtil.mouseOverArea(
            mouseX,
            mouseY,
            this.configButtonX,
            this.configButtonY,
            this.configButtonSize,
            this.configButtonSize)) {
            Minecraft.getMinecraft()
                .displayGuiScreen(new GuiFactory.ConfigGui(this));
        }

        this.searchBar.mouseClicked(mouseX, mouseY, mouseButton);
        this.waystoneList.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return WaystoneConfig.menusPauseGame;
    }

    public void openRemoveDialog(WaystoneEntry waystone, boolean closeOnRemoval) {
        Minecraft.getMinecraft()
            .displayGuiScreen(
                new GuiSimpleYesNo(
                    this,
                    closeOnRemoval,
                    false,
                    I18n.format(
                        "gui.waystones:removeWaystoneDialog",
                        waystone.isGlobal() ? EnumChatFormatting.YELLOW : "",
                        waystone.getName()),
                    yes -> {
                        if (yes) {
                            removeEntry(waystone);
                        }
                    }));
    }

    public void removeEntry(WaystoneEntry entry) {
        this.entries = Arrays.stream(entries)
            .filter(x -> !x.equals(entry))
            .toArray(WaystoneEntry[]::new);
        updateList();
        NetworkHandler.channel.sendToServer(new MessageUnlearnWaystone(entry));
    }
}
