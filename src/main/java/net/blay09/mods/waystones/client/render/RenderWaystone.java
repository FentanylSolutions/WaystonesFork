package net.blay09.mods.waystones.client.render;

import java.util.Random;

import net.blay09.mods.waystones.PlayerWaystoneData;
import net.blay09.mods.waystones.WaystoneConfig;
import net.blay09.mods.waystones.WaystoneManager;
import net.blay09.mods.waystones.Waystones;
import net.blay09.mods.waystones.block.TileWaystone;
import net.blay09.mods.waystones.varinstances.VarInstanceClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IIcon;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.util.ForgeDirection;

import org.lwjgl.opengl.GL11;

public class RenderWaystone extends TileEntitySpecialRenderer {

    private static final ResourceLocation texture = new ResourceLocation(
        Waystones.MODID,
        "textures/entity/waystone.png");
    private static final ResourceLocation textureSandstone = new ResourceLocation(
        Waystones.MODID,
        "textures/entity/sandstone.png");
    private static final ResourceLocation textureMossy = new ResourceLocation(
        Waystones.MODID,
        "textures/entity/mossy.png");
    private static final ResourceLocation textureStonebrick = new ResourceLocation(
        Waystones.MODID,
        "textures/entity/stonebrick.png");
    private static final ResourceLocation textureMossyStonebrick = new ResourceLocation(
        Waystones.MODID,
        "textures/entity/mossystonebrick.png");
    private static final ResourceLocation textureNether = new ResourceLocation(
        Waystones.MODID,
        "textures/entity/netherbrick.png");
    private static final ResourceLocation textureEnd = new ResourceLocation(
        Waystones.MODID,
        "textures/entity/endstone.png");
    private static final ResourceLocation textureActive = new ResourceLocation(
        Waystones.MODID,
        "textures/entity/waystone_active.png");
    private static final ResourceLocation textureActiveSandstone = new ResourceLocation(
        Waystones.MODID,
        "textures/entity/sandstone_active.png");
    private static final ResourceLocation textureActiveMossy = new ResourceLocation(
        Waystones.MODID,
        "textures/entity/mossy_active.png");
    private static final ResourceLocation textureActiveStonebrick = new ResourceLocation(
        Waystones.MODID,
        "textures/entity/stonebrick_active.png");
    private static final ResourceLocation textureActiveNether = new ResourceLocation(
        Waystones.MODID,
        "textures/entity/netherbrick_active.png");
    private static final ResourceLocation textureActiveEnd = new ResourceLocation(
        Waystones.MODID,
        "textures/entity/endstone_active.png");

    // UV zoom factor for overlay textures
    private static final float LAVA_UV_SCALE = 3.0f;
    private static final float END_PORTAL_UV_SCALE = 3.0f;

    private static final ResourceLocation END_SKY_TEXTURE = new ResourceLocation("textures/environment/end_sky.png");
    private static final ResourceLocation END_PORTAL_TEXTURE = new ResourceLocation("textures/entity/end_portal.png");
    private static final Random END_PORTAL_RANDOM = new Random(31100L);

    private final ModelWaystone model = new ModelWaystone();

    // Pillar box in model-space units after ModelRenderer scale (0.0625f) has been applied.
    private static final float PILLAR_X_MIN = -10f * 0.0625f;
    private static final float PILLAR_X_MAX = 10f * 0.0625f;
    private static final float PILLAR_Z_MIN = -10f * 0.0625f;
    private static final float PILLAR_Z_MAX = 10f * 0.0625f;
    private static final float PILLAR_Y_TOP = -48f * 0.0625f;
    private static final float PILLAR_Y_BOTTOM = -18f * 0.0625f;

    // UV layout for pillar sides (derived from ModelRenderer(144, -2).addBox(..., 20, 30, 20))
    private static final float UV_U0 = 144f / 256f;
    private static final float UV_U1 = 164f / 256f;
    private static final float UV_U2 = 184f / 256f;
    private static final float UV_U3 = 204f / 256f;
    private static final float UV_U4 = 224f / 256f;
    private static final float UV_V_TOP = 18f / 256f;
    private static final float UV_V_BOTTOM = 48f / 256f;

    float getCooldownProgress(TileWaystone tileWaystone) {
        if (Minecraft.getMinecraft().thePlayer.capabilities.isCreativeMode || !WaystoneConfig.showCooldownOnWaystone) {
            return 1f;
        }
        if (!tileWaystone.hasWorldObj()) {
            return 1f; // fully charged if not in world
        }
        if (PlayerWaystoneData
            .shouldIgnoreCooldown(WaystoneManager.getServerWaystone(tileWaystone.getWaystoneName()))) {
            return 1f;
        }

        return PlayerWaystoneData.getWaystoneCooldownProgress(Minecraft.getMinecraft().thePlayer);
    }

    @Override
    public void renderTileEntityAt(TileEntity tileEntity, double x, double y, double z, float partialTicks) {
        TileWaystone tileWaystone = (TileWaystone) tileEntity;
        boolean stoneIsKnown = WaystoneManager.getKnownWaystone(tileWaystone.getWaystoneName()) != null
            || WaystoneManager.getServerWaystone(tileWaystone.getWaystoneName()) != null;
        boolean stoneIsGlobal = WaystoneManager.getServerWaystone(tileWaystone.getWaystoneName()) != null
            && WaystoneManager.getServerWaystone(tileWaystone.getWaystoneName())
                .isGlobal();
        bindTexture(getBaseTexture(tileWaystone.getVariant()));

        float angle = tileEntity.hasWorldObj()
            ? WaystoneManager.getRotationYaw(ForgeDirection.getOrientation(tileEntity.getBlockMetadata()))
            : 0f;
        final float prevBrightX = OpenGlHelper.lastBrightnessX;
        final float prevBrightY = OpenGlHelper.lastBrightnessY;
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        GL11.glPushMatrix();
        try {
            GL11.glEnable(GL11.GL_LIGHTING);
            GL11.glColor4f(1f, 1f, 1f, 1f);
            GL11.glTranslated(x + 0.5, y, z + 0.5);
            GL11.glRotatef(angle, 0f, 1f, 0f);
            GL11.glRotatef(-180f, 1f, 0f, 0f);
            GL11.glScalef(0.5f, 0.5f, 0.5f);
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            model.renderAll();
            if (tileWaystone.hasWorldObj() && stoneIsKnown) {
                GL11.glDisable(GL11.GL_CULL_FACE); // render all faces

                // Render active pillar overlay with emissive glow, clipped by cooldown progress
                float progress = getCooldownProgress(tileWaystone);
                if (progress > 0f) {
                    float glowIntensity = 1f;

                    // Emissive rendering: disable lighting and lightmap so symbols
                    // render at constant brightness regardless of ambient light
                    GL11.glDisable(GL11.GL_LIGHTING);
                    Minecraft.getMinecraft().entityRenderer.disableLightmap(0);

                    bindTexture(getOverlayTexture(tileWaystone.getVariant()));
                    GL11.glEnable(GL11.GL_POLYGON_OFFSET_FILL);
                    GL11.glPolygonOffset(-1.0f, -1.0f);
                    GL11.glDepthFunc(GL11.GL_LEQUAL);

                    // Glow blend: overlay adds light to the underlying stone
                    // alpha=1 symbols add (intensity,intensity,intensity), alpha=0 areas add nothing
                    GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
                    GL11.glColor4f(glowIntensity, glowIntensity, glowIntensity, 1f);

                    // Reveal glow from bottom to top as progress goes 0 -> 1
                    VarInstanceClient.OverlayClipBounds clipBounds = Waystones.varInstanceClient
                        .getOverlayClipBounds(tileWaystone.getVariant());
                    float pillarBottom = clipBounds.lower;
                    float pillarTop = clipBounds.upper;
                    float clipY = pillarBottom + progress * (pillarTop - pillarBottom);

                    int variant = tileWaystone.getVariant();
                    if (variant == TileWaystone.VARIANT_NETHER) {
                        renderNetherLavaOverlay(glowIntensity, clipY);
                    } else if (variant == TileWaystone.VARIANT_END) {
                        renderEndPortalOverlay(glowIntensity, clipY);
                    } else {
                        renderPillarClipped(clipY);
                    }

                    GL11.glDisable(GL11.GL_POLYGON_OFFSET_FILL);
                    GL11.glDepthFunc(GL11.GL_LEQUAL);
                    GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
                    GL11.glColor4f(1f, 1f, 1f, 1f);
                    GL11.glEnable(GL11.GL_LIGHTING);
                    Minecraft.getMinecraft().entityRenderer.enableLightmap(0);
                }

                GL11.glEnable(GL11.GL_CULL_FACE);
            }
            GL11.glDisable(GL11.GL_BLEND);
        } finally {
            Minecraft.getMinecraft().entityRenderer.enableLightmap(0);
            OpenGlHelper.setActiveTexture(OpenGlHelper.lightmapTexUnit);
            GL11.glEnable(GL11.GL_TEXTURE_2D);
            OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, prevBrightX, prevBrightY);
            OpenGlHelper.setActiveTexture(OpenGlHelper.defaultTexUnit);
            GL11.glEnable(GL11.GL_TEXTURE_2D);
            GL11.glColor4f(1f, 1f, 1f, 1f);
            GL11.glPopMatrix();
            GL11.glPopAttrib();
        }

        if (WaystoneConfig.showNametag && tileWaystone.hasWorldObj() && stoneIsKnown) {
            renderWaystoneName(tileWaystone, x + 0.5, y + 2.5, z + 0.5, stoneIsGlobal);
        }
    }

    private void renderWaystoneName(TileWaystone tile, double x, double y, double z, boolean isGlobal) {
        FontRenderer fontRenderer = Minecraft.getMinecraft().fontRenderer;
        String name = (isGlobal ? EnumChatFormatting.YELLOW : "") + tile.getWaystoneName();

        GL11.glPushAttrib(
            GL11.GL_ENABLE_BIT | GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT | GL11.GL_TEXTURE_BIT);
        GL11.glPushMatrix();
        try {
            GL11.glTranslated(x, y, z);

            // Face the player
            GL11.glRotatef(-RenderManager.instance.playerViewY, 0.0F, 1.0F, 0.0F);
            GL11.glRotatef(RenderManager.instance.playerViewX, 1.0F, 0.0F, 0.0F);

            float scale = 0.01666667F * 1.6F; // adjust size
            GL11.glScalef(-scale, -scale, scale);

            GL11.glDisable(GL11.GL_LIGHTING);
            GL11.glDepthMask(false);
            GL11.glDisable(GL11.GL_DEPTH_TEST);
            GL11.glEnable(GL11.GL_BLEND);
            OpenGlHelper.glBlendFunc(770, 771, 1, 0);
            GL11.glDisable(GL11.GL_TEXTURE_2D);

            int width = fontRenderer.getStringWidth(name) / 2;
            Tessellator tess = Tessellator.instance;
            tess.startDrawingQuads();
            tess.setColorRGBA_F(0f, 0f, 0f, 0.25f);
            tess.addVertex(-width - 1, -1, 0);
            tess.addVertex(-width - 1, 8, 0);
            tess.addVertex(width + 1, 8, 0);
            tess.addVertex(width + 1, -1, 0);
            tess.draw();
            GL11.glEnable(GL11.GL_TEXTURE_2D);

            // Vanilla-like two-pass text: through-walls darker pass, then normal pass.
            fontRenderer.drawString(name, -width, 0, 553648127);
            GL11.glEnable(GL11.GL_DEPTH_TEST);
            GL11.glDepthMask(true);
            fontRenderer.drawString(name, -width, 0, 0xFFFFFF);
            GL11.glEnable(GL11.GL_LIGHTING);
            GL11.glDisable(GL11.GL_BLEND);
        } finally {
            GL11.glPopMatrix();
            GL11.glColor4f(1f, 1f, 1f, 1f);
            GL11.glPopAttrib();
        }
    }

    private void renderNetherLavaOverlay(float glowIntensity, float clipY) {
        // Pass 1: write overlay alpha shape into depth buffer
        // Polygon offset is already active (-1, -1) from the caller, so this writes
        // depth D_offset where overlay alpha > 0, leaving base depth D0 elsewhere.
        bindTexture(textureActiveNether);
        GL11.glEnable(GL11.GL_ALPHA_TEST);
        GL11.glAlphaFunc(GL11.GL_GREATER, 0.0f);
        GL11.glColorMask(false, false, false, false);
        GL11.glDisable(GL11.GL_BLEND);
        renderPillarClipped(clipY);

        // Pass 2: draw animated lava only where the depth mask was written.
        // Uses pre-computed vertex UVs mapped to the lava atlas icon instead of
        // the GL texture matrix, for compatibility with shader mods.
        GL11.glColorMask(true, true, true, true);
        GL11.glDepthFunc(GL11.GL_EQUAL);
        GL11.glDisable(GL11.GL_ALPHA_TEST);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        GL11.glColor4f(glowIntensity, glowIntensity, glowIntensity, 1f);

        Minecraft.getMinecraft()
            .getTextureManager()
            .bindTexture(TextureMap.locationBlocksTexture);
        IIcon lavaIcon = Blocks.lava.getIcon(0, 0);
        renderPillarClippedDirectUVs(
            clipY,
            lavaIcon.getMinU(),
            lavaIcon.getMaxU(),
            lavaIcon.getMinV(),
            lavaIcon.getMaxV());

        GL11.glDepthFunc(GL11.GL_LEQUAL);
    }

    private void renderEndPortalOverlay(float glowIntensity, float clipY) {
        // Pass 1: write overlay alpha shape into depth buffer
        bindTexture(textureActiveEnd);
        GL11.glEnable(GL11.GL_ALPHA_TEST);
        GL11.glAlphaFunc(GL11.GL_GREATER, 0.0f);
        GL11.glColorMask(false, false, false, false);
        GL11.glDisable(GL11.GL_BLEND);
        renderPillarClipped(clipY);

        // Pass 2: draw end portal layers only where the depth mask was written.
        // Uses pre-computed vertex UVs instead of the GL texture matrix for
        // shader mod compatibility.
        GL11.glColorMask(true, true, true, true);
        GL11.glDepthFunc(GL11.GL_EQUAL);
        GL11.glDisable(GL11.GL_ALPHA_TEST);
        GL11.glEnable(GL11.GL_BLEND);

        END_PORTAL_RANDOM.setSeed(31100L);
        float timeOffset = (float) (Minecraft.getSystemTime() % 700000L) / 700000.0f;

        for (int i = 0; i < 16; i++) {
            float layerDepth = (float) (16 - i);
            float scale = i == 0 ? 0.125f : 0.5f;
            float brightness = 1.0f / (layerDepth + 1.0f);

            if (i == 0) {
                bindTexture(END_SKY_TEXTURE);
                brightness = 0.1f;
                GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            }
            if (i == 1) {
                bindTexture(END_PORTAL_TEXTURE);
                GL11.glBlendFunc(GL11.GL_ONE, GL11.GL_ONE);
            }

            float r = END_PORTAL_RANDOM.nextFloat() * 0.5f + 0.1f;
            float g = END_PORTAL_RANDOM.nextFloat() * 0.5f + 0.4f;
            float b = END_PORTAL_RANDOM.nextFloat() * 0.5f + 0.5f;
            if (i == 0) {
                r = g = b = 1.0f;
            }

            GL11.glColor4f(
                r * brightness * glowIntensity,
                g * brightness * glowIntensity,
                b * brightness * glowIntensity,
                1.0f);

            float rotation = (float) (i * i * 4321 + i * 9) * 2.0f;
            renderPillarClippedTransformedUVs(clipY, scale, rotation, timeOffset);
        }

        GL11.glDepthFunc(GL11.GL_LEQUAL);
    }

    private static void renderPillarClipped(float clipY) {
        float yMin = Math.max(PILLAR_Y_TOP, Math.min(PILLAR_Y_BOTTOM, clipY));
        float yMax = PILLAR_Y_BOTTOM;
        if (yMin >= yMax) {
            return;
        }

        float t = (yMin - PILLAR_Y_TOP) / (PILLAR_Y_BOTTOM - PILLAR_Y_TOP);
        float vMin = UV_V_TOP + (UV_V_BOTTOM - UV_V_TOP) * t;

        Tessellator tess = Tessellator.instance;
        tess.startDrawingQuads();

        // +X face
        tess.addVertexWithUV(PILLAR_X_MAX, yMin, PILLAR_Z_MAX, UV_U3, vMin);
        tess.addVertexWithUV(PILLAR_X_MAX, yMin, PILLAR_Z_MIN, UV_U2, vMin);
        tess.addVertexWithUV(PILLAR_X_MAX, yMax, PILLAR_Z_MIN, UV_U2, UV_V_BOTTOM);
        tess.addVertexWithUV(PILLAR_X_MAX, yMax, PILLAR_Z_MAX, UV_U3, UV_V_BOTTOM);

        // -X face
        tess.addVertexWithUV(PILLAR_X_MIN, yMin, PILLAR_Z_MIN, UV_U1, vMin);
        tess.addVertexWithUV(PILLAR_X_MIN, yMin, PILLAR_Z_MAX, UV_U0, vMin);
        tess.addVertexWithUV(PILLAR_X_MIN, yMax, PILLAR_Z_MAX, UV_U0, UV_V_BOTTOM);
        tess.addVertexWithUV(PILLAR_X_MIN, yMax, PILLAR_Z_MIN, UV_U1, UV_V_BOTTOM);

        // -Z face
        tess.addVertexWithUV(PILLAR_X_MAX, yMin, PILLAR_Z_MIN, UV_U2, vMin);
        tess.addVertexWithUV(PILLAR_X_MIN, yMin, PILLAR_Z_MIN, UV_U1, vMin);
        tess.addVertexWithUV(PILLAR_X_MIN, yMax, PILLAR_Z_MIN, UV_U1, UV_V_BOTTOM);
        tess.addVertexWithUV(PILLAR_X_MAX, yMax, PILLAR_Z_MIN, UV_U2, UV_V_BOTTOM);

        // +Z face
        tess.addVertexWithUV(PILLAR_X_MIN, yMin, PILLAR_Z_MAX, UV_U4, vMin);
        tess.addVertexWithUV(PILLAR_X_MAX, yMin, PILLAR_Z_MAX, UV_U3, vMin);
        tess.addVertexWithUV(PILLAR_X_MAX, yMax, PILLAR_Z_MAX, UV_U3, UV_V_BOTTOM);
        tess.addVertexWithUV(PILLAR_X_MIN, yMax, PILLAR_Z_MAX, UV_U4, UV_V_BOTTOM);

        tess.draw();
    }

    /**
     * Renders the pillar with UVs directly mapped to a texture atlas icon.
     * Each face maps to the full icon UV range. Shader-compatible (no texture matrix).
     */
    private static void renderPillarClippedDirectUVs(float clipY, float uMin, float uMax, float vMin, float vMax) {
        float yLow = Math.max(PILLAR_Y_TOP, Math.min(PILLAR_Y_BOTTOM, clipY));
        float yHigh = PILLAR_Y_BOTTOM;
        if (yLow >= yHigh) {
            return;
        }

        // Zoom into center of the icon by shrinking the UV range
        float uMid = (uMin + uMax) * 0.5f;
        float vMid = (vMin + vMax) * 0.5f;
        float uHalf = (uMax - uMin) * 0.5f / LAVA_UV_SCALE;
        float vHalf = (vMax - vMin) * 0.5f / LAVA_UV_SCALE;
        uMin = uMid - uHalf;
        uMax = uMid + uHalf;
        vMin = vMid - vHalf;
        vMax = vMid + vHalf;

        float t = (yLow - PILLAR_Y_TOP) / (PILLAR_Y_BOTTOM - PILLAR_Y_TOP);
        float vClipped = vMin + (vMax - vMin) * t;

        Tessellator tess = Tessellator.instance;
        tess.startDrawingQuads();

        // +X face
        tess.addVertexWithUV(PILLAR_X_MAX, yLow, PILLAR_Z_MAX, uMax, vClipped);
        tess.addVertexWithUV(PILLAR_X_MAX, yLow, PILLAR_Z_MIN, uMin, vClipped);
        tess.addVertexWithUV(PILLAR_X_MAX, yHigh, PILLAR_Z_MIN, uMin, vMax);
        tess.addVertexWithUV(PILLAR_X_MAX, yHigh, PILLAR_Z_MAX, uMax, vMax);

        // -X face
        tess.addVertexWithUV(PILLAR_X_MIN, yLow, PILLAR_Z_MIN, uMax, vClipped);
        tess.addVertexWithUV(PILLAR_X_MIN, yLow, PILLAR_Z_MAX, uMin, vClipped);
        tess.addVertexWithUV(PILLAR_X_MIN, yHigh, PILLAR_Z_MAX, uMin, vMax);
        tess.addVertexWithUV(PILLAR_X_MIN, yHigh, PILLAR_Z_MIN, uMax, vMax);

        // -Z face
        tess.addVertexWithUV(PILLAR_X_MAX, yLow, PILLAR_Z_MIN, uMax, vClipped);
        tess.addVertexWithUV(PILLAR_X_MIN, yLow, PILLAR_Z_MIN, uMin, vClipped);
        tess.addVertexWithUV(PILLAR_X_MIN, yHigh, PILLAR_Z_MIN, uMin, vMax);
        tess.addVertexWithUV(PILLAR_X_MAX, yHigh, PILLAR_Z_MIN, uMax, vMax);

        // +Z face
        tess.addVertexWithUV(PILLAR_X_MIN, yLow, PILLAR_Z_MAX, uMax, vClipped);
        tess.addVertexWithUV(PILLAR_X_MAX, yLow, PILLAR_Z_MAX, uMin, vClipped);
        tess.addVertexWithUV(PILLAR_X_MAX, yHigh, PILLAR_Z_MAX, uMin, vMax);
        tess.addVertexWithUV(PILLAR_X_MIN, yHigh, PILLAR_Z_MAX, uMax, vMax);

        tess.draw();
    }

    /**
     * Renders the pillar with pre-computed end portal UV transforms.
     * Replicates the texture matrix transform (rotate, scale, translate) per vertex
     * for shader mod compatibility.
     */
    private static void renderPillarClippedTransformedUVs(float clipY, float uvScale, float rotationDeg,
        float timeOffset) {
        float yLow = Math.max(PILLAR_Y_TOP, Math.min(PILLAR_Y_BOTTOM, clipY));
        float yHigh = PILLAR_Y_BOTTOM;
        if (yLow >= yHigh) {
            return;
        }

        // Normalized V: 0 at pillar top, 1 at pillar bottom
        float vTop = (yLow - PILLAR_Y_TOP) / (PILLAR_Y_BOTTOM - PILLAR_Y_TOP);
        float rad = (float) Math.toRadians(rotationDeg);
        float cosA = (float) Math.cos(rad);
        float sinA = (float) Math.sin(rad);

        // Scale base UVs toward center (0.5, 0.5) to zoom in on the texture
        float invZoom = 1.0f / END_PORTAL_UV_SCALE;
        float u0 = 0.5f - 0.5f * invZoom;
        float u1 = 0.5f + 0.5f * invZoom;
        float v0 = 0.5f + (vTop - 0.5f) * invZoom;
        float v1 = 0.5f + 0.5f * invZoom;

        // Pre-compute transformed UVs for the 4 unique corner positions:
        // A=(u1,v0) B=(u0,v0) C=(u0,v1) D=(u1,v1)
        float au = endPortalU(u1, v0, uvScale, cosA, sinA);
        float av = endPortalV(u1, v0, uvScale, cosA, sinA, timeOffset);
        float bu = endPortalU(u0, v0, uvScale, cosA, sinA);
        float bv = endPortalV(u0, v0, uvScale, cosA, sinA, timeOffset);
        float cu = endPortalU(u0, v1, uvScale, cosA, sinA);
        float cv = endPortalV(u0, v1, uvScale, cosA, sinA, timeOffset);
        float du = endPortalU(u1, v1, uvScale, cosA, sinA);
        float dv = endPortalV(u1, v1, uvScale, cosA, sinA, timeOffset);

        Tessellator tess = Tessellator.instance;
        tess.startDrawingQuads();

        // +X face
        tess.addVertexWithUV(PILLAR_X_MAX, yLow, PILLAR_Z_MAX, au, av);
        tess.addVertexWithUV(PILLAR_X_MAX, yLow, PILLAR_Z_MIN, bu, bv);
        tess.addVertexWithUV(PILLAR_X_MAX, yHigh, PILLAR_Z_MIN, cu, cv);
        tess.addVertexWithUV(PILLAR_X_MAX, yHigh, PILLAR_Z_MAX, du, dv);

        // -X face
        tess.addVertexWithUV(PILLAR_X_MIN, yLow, PILLAR_Z_MIN, au, av);
        tess.addVertexWithUV(PILLAR_X_MIN, yLow, PILLAR_Z_MAX, bu, bv);
        tess.addVertexWithUV(PILLAR_X_MIN, yHigh, PILLAR_Z_MAX, cu, cv);
        tess.addVertexWithUV(PILLAR_X_MIN, yHigh, PILLAR_Z_MIN, du, dv);

        // -Z face
        tess.addVertexWithUV(PILLAR_X_MAX, yLow, PILLAR_Z_MIN, au, av);
        tess.addVertexWithUV(PILLAR_X_MIN, yLow, PILLAR_Z_MIN, bu, bv);
        tess.addVertexWithUV(PILLAR_X_MIN, yHigh, PILLAR_Z_MIN, cu, cv);
        tess.addVertexWithUV(PILLAR_X_MAX, yHigh, PILLAR_Z_MIN, du, dv);

        // +Z face
        tess.addVertexWithUV(PILLAR_X_MIN, yLow, PILLAR_Z_MAX, au, av);
        tess.addVertexWithUV(PILLAR_X_MAX, yLow, PILLAR_Z_MAX, bu, bv);
        tess.addVertexWithUV(PILLAR_X_MAX, yHigh, PILLAR_Z_MAX, cu, cv);
        tess.addVertexWithUV(PILLAR_X_MIN, yHigh, PILLAR_Z_MAX, du, dv);

        tess.draw();
    }

    /** Computes transformed U for the end portal effect: center, rotate, un-center, scale. */
    private static float endPortalU(float u, float v, float scale, float cosA, float sinA) {
        return ((u - 0.5f) * cosA - (v - 0.5f) * sinA + 0.5f) * scale;
    }

    /** Computes transformed V for the end portal effect: center, rotate, un-center, scale, time scroll. */
    private static float endPortalV(float u, float v, float scale, float cosA, float sinA, float timeOffset) {
        return ((u - 0.5f) * sinA + (v - 0.5f) * cosA + 0.5f) * scale + timeOffset;
    }

    private static ResourceLocation getBaseTexture(int variant) {
        switch (variant) {
            case TileWaystone.VARIANT_SANDSTONE:
                return textureSandstone;
            case TileWaystone.VARIANT_MOSSY:
                return textureMossy;
            case TileWaystone.VARIANT_STONEBRICK:
                return textureStonebrick;
            case TileWaystone.VARIANT_MOSSY_STONEBRICK:
                return textureMossyStonebrick;
            case TileWaystone.VARIANT_NETHER:
                return textureNether;
            case TileWaystone.VARIANT_END:
                return textureEnd;
            default:
                return texture;
        }
    }

    private static ResourceLocation getOverlayTexture(int variant) {
        switch (variant) {
            case TileWaystone.VARIANT_SANDSTONE:
                return textureActiveSandstone;
            case TileWaystone.VARIANT_MOSSY:
                return textureActiveMossy;
            case TileWaystone.VARIANT_STONEBRICK:
            case TileWaystone.VARIANT_MOSSY_STONEBRICK:
                return textureActiveStonebrick;
            case TileWaystone.VARIANT_NETHER:
                return textureActiveNether;
            case TileWaystone.VARIANT_END:
                return textureActiveEnd;
            default:
                return textureActive;
        }
    }
}
