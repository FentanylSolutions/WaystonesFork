package net.blay09.mods.waystones.client.gui;

import java.util.List;

import net.blay09.mods.waystones.PlayerWaystoneData;
import net.blay09.mods.waystones.Waystones;
import net.blay09.mods.waystones.util.ClientUtil;
import net.blay09.mods.waystones.util.DimensionUtil;
import net.blay09.mods.waystones.util.WaystoneEntry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.resources.I18n;

import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

public class WaystoneList {

    private final Minecraft mc;
    Tessellator tessellator;
    List<GuiButtonWaystone> entries;

    public int x, y, width, height;
    private final int slotHeight;
    private final int gapHeight;

    private float scrollAmount = 0;
    private float maxScroll = 0;

    final private int padding = 2;
    final private int scrollBarWidth = 5;
    final private int brightColor = 0xFFAAAAAA;
    final private int backgroundColor = 0x7F000000;

    private int startClickX;
    private int startClickY;
    private boolean isDragging;

    private final Gui parentGui;

    private List<String> pinnedNames = PlayerWaystoneData.getPinnedWaystoneNames(Minecraft.getMinecraft().thePlayer);

    public WaystoneList(Minecraft mc, int x, int y, int width, int height, int slotHeight, int gapHeight,
        List<GuiButtonWaystone> entries, Gui parentGui) {
        this.mc = mc;
        this.tessellator = Tessellator.instance;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.slotHeight = slotHeight;
        this.gapHeight = gapHeight;
        this.entries = entries;
        this.startClickX = -1;
        this.startClickY = -1;
        this.isDragging = false;
        this.parentGui = parentGui;
        recalcScroll();
    }

    public void update() {
        pinnedNames = PlayerWaystoneData.getPinnedWaystoneNames(Minecraft.getMinecraft().thePlayer);
        for (GuiButtonWaystone btn : entries) {
            if (pinnedNames.contains(btn.realName)) {
                btn.pinned = true;
            }
        }
        recalcScroll();
    }

    int getContentHeight() {
        return this.entries.size() * this.slotHeight + Math.max(0, this.entries.size() - 1) * this.gapHeight;
    }

    public void draw(int mouseX, int mouseY, int windowWidth, int windowHeight) {
        // Background
        ClientUtil.drawSolidRect(
            this.tessellator,
            this.x - this.padding,
            this.y - this.padding,
            this.width + this.padding * 2,
            this.height + this.padding * 2,
            backgroundColor);

        if (Mouse.isButtonDown(1)) {
            Waystones.debug("[Debug] Right Click at " + mouseX + ";" + mouseY);
        }

        // Scrollbar calcs
        int viewportHeight = height;
        int trackHeight = height;
        int thumbHeight = Math
            .min(Math.max((int) ((viewportHeight / (float) getContentHeight()) * trackHeight), 10), trackHeight);

        float howMuchCanScroll = getContentHeight() - this.height;
        float scrolledRatio = 0;
        if (howMuchCanScroll < 0) {
            howMuchCanScroll = 0;
        } else {
            scrolledRatio = scrollAmount / howMuchCanScroll;
        }
        int thumbPositionY = (int) (this.y + (this.height - thumbHeight) * scrolledRatio);
        int scrollbarPosX = this.x + this.width + this.padding;

        // --- CLIP TO WIDGET BOUNDS ---
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        applyScissor();

        float yOffset = this.y - scrollAmount;

        for (int i = 0; i < entries.size(); i++) {
            float entryY = yOffset + i * (slotHeight + gapHeight);

            GuiButtonWaystone entry = entries.get(i);
            entry.xPosition = this.x;
            entry.yPosition = (int) entryY;
            if (entryY + slotHeight < y || entryY > y + height) {
                continue;
            }

            if (mouseX < x || mouseX > x + width || mouseY < y || mouseY > y + height) {
                entry.drawButton(mc, -1, -1);
            } else {
                entry.drawButton(mc, mouseX, mouseY);
            }
        }

        GL11.glDisable(GL11.GL_SCISSOR_TEST);

        // Border
        ClientUtil.drawBorderedBox(
            this.x - this.padding,
            this.y - this.padding,
            this.width + this.padding * 2 + this.scrollBarWidth,
            this.height + this.padding * 2,
            1,
            0x00000000,
            brightColor);

        // Scrollbar background
        ClientUtil.drawSolidRect(
            this.tessellator,
            this.x + this.width + this.padding,
            this.y - this.padding,
            scrollBarWidth,
            this.height + this.padding * 2,
            backgroundColor);

        // Scrollbar
        ClientUtil
            .drawSolidRect(this.tessellator, scrollbarPosX, thumbPositionY, scrollBarWidth, thumbHeight, 0xFF808080);
        ClientUtil.drawSolidRect(
            this.tessellator,
            scrollbarPosX,
            thumbPositionY,
            scrollBarWidth - 1,
            thumbHeight - 1,
            0xFFC0C0C0);

        if (mouseX >= scrollbarPosX && mouseX <= scrollbarPosX + scrollBarWidth
            && mouseY >= thumbPositionY
            && mouseY <= thumbPositionY + thumbHeight) {
            if (Mouse.isButtonDown(0)) {
                if (!isDragging) {
                    isDragging = true;
                    startClickX = mouseX;
                    startClickY = mouseY;
                }
            }
        }
        if (Mouse.isButtonDown(0) && isDragging) {
            // scrollAmount += mouseY - startClickY;
            float delta = mouseY - startClickY;

            howMuchCanScroll = this.height - thumbHeight;
            if (howMuchCanScroll < 0) {
                howMuchCanScroll = 0;
            }
            scrolledRatio = delta / howMuchCanScroll;
            scrollAmount += (getContentHeight() - this.height) * scrolledRatio;

            clampScroll();
            startClickY = mouseY;
        } else {
            isDragging = false;
            startClickX = -1;
            startClickY = -1;
        }

        // Detailed Waystone info
        int index = getHoveredButton(mouseX, mouseY);
        if (index > -1) {
            int bakColor = ClientUtil.backupGLColor();
            WaystoneEntry entry = this.entries.get(index)
                .getWaystone();
            if (entry.getDimensionId() == Minecraft.getMinecraft().thePlayer.getEntityWorld().provider.dimensionId) {
                int distance = (int) Math.sqrt(
                    Math.pow(
                        Minecraft.getMinecraft().thePlayer.posX - entry.getPos()
                            .getX(),
                        2)
                        + Math.pow(
                            Minecraft.getMinecraft().thePlayer.posY - entry.getPos()
                                .getY(),
                            2)
                        + Math.pow(
                            Minecraft.getMinecraft().thePlayer.posZ - entry.getPos()
                                .getZ(),
                            2));
                this.parentGui.drawCenteredString(
                    mc.fontRenderer,
                    I18n.format(
                        "gui.waystones:bottomInfo1",
                        distance,
                        entry.getPos()
                            .getX(),
                        entry.getPos()
                            .getY(),
                        entry.getPos()
                            .getZ()),
                    this.x + this.width / 2,
                    this.y + this.height + 15,
                    brightColor);
            } else {
                this.parentGui.drawCenteredString(
                    mc.fontRenderer,
                    DimensionUtil.idToName(entry.getDimensionId()),
                    this.x + this.width / 2,
                    this.y + this.height + 15,
                    brightColor);
            }
            ClientUtil.restoreGLColor(bakColor);
        }

        if (Mouse.isButtonDown(0)) {
            // Pass
        } else {
            while (Mouse.next()) {
                int dwheel = ClientUtil.getMouseDwheel();
                if (dwheel != 0) {
                    handleScroll(mouseX, mouseY, dwheel);
                }
            }
        }
    }

    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (mouseButton != 0) {
            return;
        }

        int index = getHoveredButton(mouseX, mouseY);
        if (index == -1) {
            return;
        }

        if (index >= 0 && index < entries.size()) {
            GuiButtonWaystone button = entries.get(index);
            button.mouseClicked(mouseX, mouseY, mouseButton);
        }
    }

    int getHoveredButton(int mouseX, int mouseY) {
        if (!isMouseOver(mouseX, mouseY)) {
            return -1;
        }
        int res = (int) ((mouseY - y + scrollAmount) / (slotHeight + this.gapHeight));
        if (res >= this.entries.size()) {
            return -1;
        }
        return res;
    }

    public void handleScroll(int mouseX, int mouseY, int dWheel) {
        if (!isMouseOver(mouseX, mouseY)) {
            return;
        }
        if (dWheel < 0 && scrollAmount >= maxScroll) {
            clampScroll();
            return;
        }
        if (dWheel > 0 && scrollAmount <= 0) {
            clampScroll();
            return;
        }
        scrollAmount -= (float) (Integer.signum(dWheel) * slotHeight) / 2;
        clampScroll();
    }

    private boolean isMouseOver(int mouseX, int mouseY) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    private void clampScroll() {
        if (scrollAmount < 0) scrollAmount = 0;
        if (scrollAmount > maxScroll) scrollAmount = maxScroll;
    }

    private void recalcScroll() {
        maxScroll = Math.max(0, getContentHeight() - height);
        clampScroll();
    }

    private void applyScissor() {
        ScaledResolution sr = new ScaledResolution(mc, mc.displayWidth, mc.displayHeight);

        int scale = sr.getScaleFactor();
        int screenHeight = sr.getScaledHeight();

        int scissorX = x * scale;
        int scissorY = (screenHeight - (y + height)) * scale;
        int scissorW = width * scale;
        int scissorH = height * scale;

        GL11.glScissor(scissorX, scissorY, scissorW, scissorH);
    }
}
