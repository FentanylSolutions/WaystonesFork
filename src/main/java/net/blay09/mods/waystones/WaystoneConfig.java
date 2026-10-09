package net.blay09.mods.waystones;

import net.minecraftforge.common.config.Configuration;

import cpw.mods.fml.common.network.ByteBufUtils;
import io.netty.buffer.ByteBuf;

// TODO: Clean up this mess

// Static fields are not synced from the server, non-static are
public class WaystoneConfig {

    private static Configuration config;

    public static boolean debugMode;
    public static int teleportButtonX;
    public static int teleportButtonY;
    public static boolean disableParticles;
    public static boolean disableTeleportSound;

    public boolean teleportButton;
    public int teleportButtonCooldown;
    public boolean teleportButtonReturnOnly;
    public static boolean showCooldownOnWaystone;

    public boolean allowReturnScrolls;
    public boolean lootReturnScrolls;
    public boolean allowWarpStone;

    public int warpStoneCooldown;
    public int waystoneCooldown;

    public boolean interDimension;

    public boolean invulnerableWaystones;

    public boolean creativeModeOnly;
    public boolean setSpawnPoint;

    public boolean globalNoCooldown;
    public boolean globalInterDimension;

    public static boolean showNametag;
    public boolean enableWorldgen;
    public boolean villageNamesCompat;

    public int xpBaseCost;
    public int xpBlocksPerLevel;
    public int xpCrossDimCost;

    public static boolean menusPauseGame;

    public static int sortingMode;
    public static boolean flatInventoryIcon;
    public static String[] overlayClipBounds;
    public static boolean journeyMapWaypoints;
    public static boolean journeyMapWaypointRandomColor;
    public static String journeyMapWaypointColor;
    public static int journeyMapWaypointYOffset;
    public static boolean xaeroMinimapWaypoints;
    public static boolean xaeroMinimapWaypointRandomColor;
    public static int xaeroMinimapWaypointColor;
    public static int xaeroMinimapWaypointYOffset;

    public float waystoneLightLevel;
    public boolean disableWaystoneDrops;
    public String[] sandyWaystonePathBlocks;
    public String[] mossyWaystonePathBlocks;
    public String[] structureWaystoneRules;

    public static class Categories {

        public static final String general = "general";
        public static final String client = "client";
        public static final String journeyMap = "journeymap";
        public static final String worldgen = "worldgen";
        public static final String xaeroMinimap = "xaerominimap";
    }

    public void reloadLocal(Configuration config) {
        debugMode = config.getBoolean("debugMode", Categories.general, false, "Additional logs");
        teleportButton = config.getBoolean(
            "teleportButton",
            Categories.general,
            false,
            "Should there be a button in the inventory to access the waystone menu?");
        teleportButtonCooldown = config.getInt(
            "teleportButtonCooldown",
            Categories.general,
            300,
            0,
            86400,
            "The cooldown between usages of the inventory teleport button in seconds. Independent of placed waystones and the Warp Stone item.");
        teleportButtonReturnOnly = config.getBoolean(
            "teleportButtonReturnOnly",
            Categories.general,
            false,
            "If true, the teleport button will only let you return to the last activated waystone, instead of allowing to choose.");

        allowReturnScrolls = config
            .getBoolean("allowReturnScrolls", Categories.general, true, "If true, return scrolls will be craftable.");
        lootReturnScrolls = config.getBoolean(
            "Spawn Return Scrolls as loot",
            Categories.general,
            true,
            "If true, return scrolls will be found in dungeons as loot.");
        allowWarpStone = config
            .getBoolean("allowWarpStone", Categories.general, true, "If true, the warp stone will be craftable.");

        teleportButtonX = config.getInt(
            "teleportButtonX",
            Categories.client,
            60,
            -100,
            250,
            "The x position of the warp button in the inventory.");
        teleportButtonY = config.getInt(
            "teleportButtonY",
            Categories.client,
            60,
            -100,
            250,
            "The y position of the warp button in the inventory.");
        disableParticles = config.getBoolean(
            "disableParticles",
            Categories.client,
            false,
            "If true, activated waystones will not emit particles.");
        disableTeleportSound = config.getBoolean(
            "disableTeleportSound",
            Categories.client,
            false,
            "If true, waystone teleports will not play the teleport sound.");
        menusPauseGame = config.getBoolean(
            "menusPauseGame",
            Categories.client,
            false,
            "If true, GUI menus pause the game in singleplayer.");

        warpStoneCooldown = config.getInt(
            "warpStoneCooldown",
            Categories.general,
            300,
            0,
            86400,
            "The cooldown between usages of the handheld Warp Stone item in seconds. Independent of placed waystones and the inventory teleport button.");
        waystoneCooldown = config.getInt(
            "waystoneCooldown",
            Categories.general,
            0,
            0,
            86400,
            "The cooldown between teleports from placed waystones in seconds. Independent of the Warp Stone item and inventory teleport button. Set to 0 to disable.");

        setSpawnPoint = config.getBoolean(
            "setSpawnPoint",
            Categories.general,
            false,
            "If true, the player's spawnpoint will be set to the last activated waystone.");
        interDimension = config
            .getBoolean("interDimension", Categories.general, true, "If true, all waystones work inter-dimensionally.");

        invulnerableWaystones = config.getBoolean(
            "Invulnerable Waystones",
            Categories.general,
            false,
            "If true, a waystone is invulnerable to all but the player who named them.");

        creativeModeOnly = config.getBoolean(
            "creativeModeOnly",
            Categories.general,
            false,
            "If true, waystones can only be placed in creative mode.");

        globalNoCooldown = config.getBoolean(
            "globalNoCooldown",
            Categories.general,
            false,
            "If true, waystones marked as global have no cooldown.");
        globalInterDimension = config.getBoolean(
            "globalInterDimension",
            Categories.general,
            true,
            "If true, waystones marked as global work inter-dimensionally.");

        showNametag = config.getBoolean(
            "showNametag",
            Categories.client,
            false,
            "If true, show a floating nametag with the Waystone's name, above it.");

        enableWorldgen = config
            .getBoolean("enableWorldgen", Categories.worldgen, true, "If true, generate a Waystone in each village.");

        villageNamesCompat = config.getBoolean(
            "villageNamesCompat",
            Categories.worldgen,
            true,
            "If true, village Waystones will take their name from Village Names.");

        xpBaseCost = config.getInt(
            "xpBaseCost",
            Categories.general,
            5,
            -1,
            Integer.MAX_VALUE,
            "The minimum amount of XP levels consumed when using a Waystone. Set to -1 to disable cost altogether.");

        xpBlocksPerLevel = config.getInt(
            "xpBlocksPerLevel",
            Categories.general,
            100,
            0,
            Integer.MAX_VALUE,
            "Each how many blocks consume one XP level.");

        xpCrossDimCost = config.getInt(
            "xpCrossDimCost",
            Categories.general,
            5,
            0,
            Integer.MAX_VALUE,
            "How many XP levels are consumed for teleporting to another dimension.");

        sortingMode = config.getInt(
            "sortingMode",
            Categories.client,
            0,
            0,
            1,
            "The Waystone sorting mode. Alphabetical: 0, Distance: 1.");

        flatInventoryIcon = config.getBoolean(
            "flatInventoryIcon",
            Categories.client,
            false,
            "If true, render waystones as 2D icons in the inventory instead of 3D models.");

        // Formula to calculate those:
        // Upper:
        // Take the Y of the first pixel row of the overlay, multiply by 2, substract 66
        // Lower:
        // Take the Y of the last pixel row of the overlay, multiply by 2, substract 66
        overlayClipBounds = config.getStringList(
            "overlayClipBounds",
            Categories.client,
            new String[] { "variant=stone;lower=-22;upper=-44", "variant=sandstone;lower=-22;upper=-38",
                "variant=mossy;lower=-24;upper=-44", "variant=stonebrick;lower=-26;upper=-44",
                "variant=mossy_stonebrick;lower=-26;upper=-44", "variant=nether;lower=-24;upper=-44",
                "variant=end;lower=-24;upper=-42" },
            "Per-variant overlay clip bounds in model pixel units. "
                + "Controls the lower and upper Y extents of the glow overlay on the pillar. "
                + "Format: variant=<name>;lower=<Y>;upper=<Y>");

        showCooldownOnWaystone = config.getBoolean(
            "showCooldownOnWaystone",
            Categories.client,
            true,
            "If true, Waystone glow texture will display the cooldown status.");

        journeyMapWaypoints = config.getBoolean(
            "journeyMapWaypoints",
            Categories.journeyMap,
            true,
            "If true, activating a Waystone adds or updates a JourneyMap waypoint for it.");
        journeyMapWaypointRandomColor = config.getBoolean(
            "journeyMapWaypointRandomColor",
            Categories.journeyMap,
            true,
            "If true, JourneyMap waypoint colors are derived from the Waystone name instead of using the fixed color.");
        journeyMapWaypointColor = config.getString(
            "journeyMapWaypointColor",
            Categories.journeyMap,
            "7FDBFF",
            "Fixed JourneyMap waypoint color as a hex RGB value. Accepts values like 7FDBFF or #7FDBFF.");
        journeyMapWaypointYOffset = config.getInt(
            "journeyMapWaypointYOffset",
            Categories.journeyMap,
            2,
            Integer.MIN_VALUE,
            Integer.MAX_VALUE,
            "Vertical offset applied to JourneyMap waypoints relative to the Waystone base block.");
        xaeroMinimapWaypoints = config.getBoolean(
            "xaeroMinimapWaypoints",
            Categories.xaeroMinimap,
            true,
            "If true, activating a Waystone adds or updates a Xaero's Minimap waypoint for it.");
        xaeroMinimapWaypointRandomColor = config.getBoolean(
            "xaeroMinimapWaypointRandomColor",
            Categories.xaeroMinimap,
            true,
            "If true, Xaero's Minimap waypoint colors are derived from the Waystone name instead of using the fixed color.");
        xaeroMinimapWaypointColor = config.getInt(
            "xaeroMinimapWaypointColor",
            Categories.xaeroMinimap,
            11,
            0,
            15,
            "Fixed Xaero's Minimap waypoint color index. Uses Xaero's 0-15 color palette.");
        xaeroMinimapWaypointYOffset = config.getInt(
            "xaeroMinimapWaypointYOffset",
            Categories.xaeroMinimap,
            2,
            Integer.MIN_VALUE,
            Integer.MAX_VALUE,
            "Vertical offset applied to Xaero's Minimap waypoints relative to the Waystone base block.");

        waystoneLightLevel = config.getFloat(
            "waystoneLightLevel",
            Categories.general,
            0.5f,
            0f,
            1f,
            "Light level emitted by waystones. 0 = none, 1 = maximum.");

        disableWaystoneDrops = config.getBoolean(
            "disableWaystoneDrops",
            Categories.general,
            false,
            "If true, waystones will not drop as an item when mined (including Silk Touch).");

        sandyWaystonePathBlocks = config.getStringList(
            "sandyWaystonePathBlocks",
            Categories.worldgen,
            new String[] { "minecraft:sandstone" },
            "List of path/surface blocks that should make village-generated Waystones use the sandy variant.");
        mossyWaystonePathBlocks = config.getStringList(
            "mossyWaystonePathBlocks",
            Categories.worldgen,
            new String[] {},
            "List of path/surface blocks that should make village-generated Waystones use the mossy variant.");
        structureWaystoneRules = config.getStringList(
            "structureWaystoneRules",
            Categories.worldgen,
            new String[] { "structure=village;chance=1;type=auto", "structure=temple_desert;chance=1;type=sandy",
                "structure=temple_jungle;chance=1;type=auto", "structure=stronghold;chance=1;type=auto",
                "structure=fortress;chance=1;type=nether", "structure=end_spike;chance=1;type=end",
                "structure=world_spawn;chance=1;type=stone;dimensionWhitelist=0" },
            "How waystones generate in structures. One rule per structure id. "
                + "Format: structure=<id>;chance=<0..1>;type=<auto|stone|sandy|mossy|stonebrick|mossystonebrick|nether|end>;"
                + "name=<override>;forceGlobal=<true|false>;autoActivateGlobal=<true|false>;"
                + "dimensionWhitelist=<*|0,-1,1>;biomeWhitelist=<*|2,17,21>");
    }

    public static WaystoneConfig read(ByteBuf buf) {
        WaystoneConfig config = new WaystoneConfig();
        config.teleportButton = buf.readBoolean();
        config.teleportButtonCooldown = buf.readInt();
        config.teleportButtonReturnOnly = buf.readBoolean();
        config.warpStoneCooldown = buf.readInt();
        config.waystoneCooldown = buf.readInt();
        config.interDimension = buf.readBoolean();
        config.globalInterDimension = buf.readBoolean();
        config.creativeModeOnly = buf.readBoolean();
        config.setSpawnPoint = buf.readBoolean();
        config.enableWorldgen = buf.readBoolean();
        config.villageNamesCompat = buf.readBoolean();
        config.xpBaseCost = buf.readInt();
        config.xpBlocksPerLevel = buf.readInt();
        config.xpCrossDimCost = buf.readInt();
        config.allowReturnScrolls = buf.readBoolean();
        config.lootReturnScrolls = buf.readBoolean();
        config.allowWarpStone = buf.readBoolean();
        config.globalNoCooldown = buf.readBoolean();
        config.waystoneLightLevel = buf.readFloat();
        config.disableWaystoneDrops = buf.readBoolean();
        config.invulnerableWaystones = buf.readBoolean();
        int sandyPathBlockCount = buf.readInt();
        config.sandyWaystonePathBlocks = new String[sandyPathBlockCount];
        for (int i = 0; i < sandyPathBlockCount; i++) {
            config.sandyWaystonePathBlocks[i] = ByteBufUtils.readUTF8String(buf);
        }
        int mossyPathBlockCount = buf.readInt();
        config.mossyWaystonePathBlocks = new String[mossyPathBlockCount];
        for (int i = 0; i < mossyPathBlockCount; i++) {
            config.mossyWaystonePathBlocks[i] = ByteBufUtils.readUTF8String(buf);
        }
        int structureRuleCount = buf.readInt();
        config.structureWaystoneRules = new String[structureRuleCount];
        for (int i = 0; i < structureRuleCount; i++) {
            config.structureWaystoneRules[i] = ByteBufUtils.readUTF8String(buf);
        }
        return config;
    }

    public void write(ByteBuf buf) {
        buf.writeBoolean(teleportButton);
        buf.writeInt(teleportButtonCooldown);
        buf.writeBoolean(teleportButtonReturnOnly);
        buf.writeInt(warpStoneCooldown);
        buf.writeInt(waystoneCooldown);
        buf.writeBoolean(interDimension);
        buf.writeBoolean(globalInterDimension);
        buf.writeBoolean(creativeModeOnly);
        buf.writeBoolean(setSpawnPoint);
        buf.writeBoolean(enableWorldgen);
        buf.writeBoolean(villageNamesCompat);
        buf.writeInt(xpBaseCost);
        buf.writeInt(xpBlocksPerLevel);
        buf.writeInt(xpCrossDimCost);
        buf.writeBoolean(allowReturnScrolls);
        buf.writeBoolean(lootReturnScrolls);
        buf.writeBoolean(allowWarpStone);
        buf.writeBoolean(globalNoCooldown);
        buf.writeFloat(waystoneLightLevel);
        buf.writeBoolean(disableWaystoneDrops);
        buf.writeBoolean(invulnerableWaystones);
        String[] sandyBlocks = sandyWaystonePathBlocks != null ? sandyWaystonePathBlocks : new String[0];
        buf.writeInt(sandyBlocks.length);
        for (String sandyPathBlock : sandyBlocks) {
            ByteBufUtils.writeUTF8String(buf, sandyPathBlock);
        }
        String[] mossyBlocks = mossyWaystonePathBlocks != null ? mossyWaystonePathBlocks : new String[0];
        buf.writeInt(mossyBlocks.length);
        for (String mossyPathBlock : mossyBlocks) {
            ByteBufUtils.writeUTF8String(buf, mossyPathBlock);
        }
        String[] structureRules = structureWaystoneRules != null ? structureWaystoneRules : new String[0];
        buf.writeInt(structureRules.length);
        for (String structureRule : structureRules) {
            ByteBufUtils.writeUTF8String(buf, structureRule);
        }
    }

    public static Configuration getRawConfig() {
        return config;
    }

    public static void setConfig(Configuration config) {
        WaystoneConfig.config = config;
    }
}
