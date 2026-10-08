package net.blay09.mods.waystones.network.message;

import net.blay09.mods.waystones.util.WaystoneCooldown;
import net.blay09.mods.waystones.util.WaystoneEntry;

import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import io.netty.buffer.ByteBuf;

public class MessageWaystones implements IMessage {

    private WaystoneEntry[] entries;
    private WaystoneEntry[] serverEntries;
    private String lastServerWaystoneName;
    private long lastFreeWarp;
    private long lastWarpStoneUse;
    private String[] pinnedWaystones;

    public MessageWaystones() {}

    public MessageWaystones(WaystoneEntry[] entries, WaystoneEntry[] serverEntries, String lastServerWaystoneName,
        long lastFreeWarp, long lastWarpStoneUse, String[] pinnedWaystones) {
        this.entries = entries;
        this.serverEntries = serverEntries;
        this.lastFreeWarp = lastFreeWarp;
        this.lastWarpStoneUse = lastWarpStoneUse;
        this.lastServerWaystoneName = lastServerWaystoneName;
        this.pinnedWaystones = pinnedWaystones;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        entries = new WaystoneEntry[buf.readByte()];
        for (int i = 0; i < entries.length; i++) {
            entries[i] = WaystoneEntry.read(buf);
        }
        serverEntries = new WaystoneEntry[buf.readByte()];
        for (int i = 0; i < serverEntries.length; i++) {
            serverEntries[i] = WaystoneEntry.read(buf);
            serverEntries[i].setGlobal(true);
        }
        lastServerWaystoneName = ByteBufUtils.readUTF8String(buf);
        lastFreeWarp = buf.readLong();
        lastWarpStoneUse = buf.readLong();
        pinnedWaystones = new String[buf.readByte()];
        for (int i = 0; i < pinnedWaystones.length; i++) {
            pinnedWaystones[i] = ByteBufUtils.readUTF8String(buf);
        }
        // Compare elapsed time on the server with the client's own clock, not absolute timestamps.
        long serverTime = buf.readLong();
        long clientTime = System.currentTimeMillis();
        lastFreeWarp = WaystoneCooldown.toClientTime(lastFreeWarp, serverTime, clientTime);
        lastWarpStoneUse = WaystoneCooldown.toClientTime(lastWarpStoneUse, serverTime, clientTime);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeByte(entries.length);
        for (WaystoneEntry entry : entries) {
            entry.write(buf);
        }
        buf.writeByte(serverEntries.length);
        for (WaystoneEntry entry : serverEntries) {
            entry.write(buf);
        }
        ByteBufUtils.writeUTF8String(buf, lastServerWaystoneName);
        buf.writeLong(lastFreeWarp);
        buf.writeLong(lastWarpStoneUse);
        buf.writeByte(pinnedWaystones.length);
        for (String s : pinnedWaystones) {
            ByteBufUtils.writeUTF8String(buf, s);
        }
        buf.writeLong(System.currentTimeMillis());
    }

    public WaystoneEntry[] getEntries() {
        return entries;
    }

    public WaystoneEntry[] getServerEntries() {
        return serverEntries;
    }

    public String getLastServerWaystoneName() {
        return lastServerWaystoneName;
    }

    public long getLastFreeWarp() {
        return lastFreeWarp;
    }

    public long getLastWarpStoneUse() {
        return lastWarpStoneUse;
    }

    public String[] getPinnedWaystones() {
        return pinnedWaystones;
    }
}
