package net.blay09.mods.waystones.network.message;

import net.blay09.mods.waystones.util.WaystoneEntry;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import io.netty.buffer.ByteBuf;

public class MessageWarpStone implements IMessage {

    private WaystoneEntry waystone;
    private boolean isFree;
    private WaystoneEntry sourceWaystone;

    public MessageWarpStone() {}

    public MessageWarpStone(WaystoneEntry waystone, boolean isFree, WaystoneEntry sourceWaystone) {
        this.waystone = waystone;
        this.isFree = isFree;
        this.sourceWaystone = sourceWaystone;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        waystone = WaystoneEntry.read(buf);
        isFree = buf.readBoolean();
        sourceWaystone = buf.readBoolean() ? WaystoneEntry.read(buf) : null;
    }

    @Override
    public void toBytes(ByteBuf buf) {
        waystone.write(buf);
        buf.writeBoolean(isFree);
        buf.writeBoolean(sourceWaystone != null);
        if (sourceWaystone != null) {
            sourceWaystone.write(buf);
        }
    }

    public WaystoneEntry getWaystone() {
        return waystone;
    }

    public boolean isFree() {
        return isFree;
    }

    public WaystoneEntry getSourceWaystone() {
        return sourceWaystone;
    }
}
