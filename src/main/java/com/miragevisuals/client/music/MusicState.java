package com.miragevisuals.client.music;

/**
 * Immutable snapshot of the currently playing track.
 *
 * <p>{@code position}/{@code duration} are in milliseconds; values of 0 mean
 * the underlying source did not provide timeline data. {@code present=false}
 * means there is no active media session at all and the widget should hide.</p>
 */
public final class MusicState {
    public static final MusicState EMPTY = new MusicState(false, "", "", "", 0L, 0L, false, "");

    public final boolean present;
    public final String title;
    public final String artist;
    public final String albumTitle;
    public final long positionMs;
    public final long durationMs;
    public final boolean isPlaying;
    public final String source;
    public final long updatedNanos;

    public MusicState(boolean present, String title, String artist, String albumTitle,
                      long positionMs, long durationMs, boolean isPlaying, String source) {
        this.present = present;
        this.title = title == null ? "" : title;
        this.artist = artist == null ? "" : artist;
        this.albumTitle = albumTitle == null ? "" : albumTitle;
        this.positionMs = positionMs;
        this.durationMs = durationMs;
        this.isPlaying = isPlaying;
        this.source = source == null ? "" : source;
        this.updatedNanos = System.nanoTime();
    }

    public float progress() {
        if (durationMs <= 0L) {
            return 0.0F;
        }
        return Math.max(0.0F, Math.min(1.0F, (float) positionMs / (float) durationMs));
    }

    public boolean hasTrack() {
        return present && (!title.isEmpty() || !artist.isEmpty());
    }
}
