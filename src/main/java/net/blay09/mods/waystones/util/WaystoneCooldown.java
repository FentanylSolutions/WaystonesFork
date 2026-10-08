package net.blay09.mods.waystones.util;

public final class WaystoneCooldown {

    private WaystoneCooldown() {}

    public static long toClientTime(long lastUse, long serverTime, long clientTime) {
        // Zero means the player has never used this teleport type.
        return lastUse == 0 ? 0 : clientTime - Math.max(0L, serverTime - lastUse);
    }

    public static long getRemainingMillis(long lastUse, int cooldownSeconds, long now) {
        if (cooldownSeconds <= 0 || lastUse == 0) {
            return 0L;
        }
        return Math.max(0L, cooldownSeconds * 1000L - Math.max(0L, now - lastUse));
    }

    public static float getProgress(long lastUse, int cooldownSeconds, long now) {
        if (cooldownSeconds <= 0) {
            return 1f;
        }
        return 1f - (float) getRemainingMillis(lastUse, cooldownSeconds, now) / (cooldownSeconds * 1000L);
    }
}
