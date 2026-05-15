package com.miragevisuals.client.util;

/**
 * Pastel lavender glass palette used by MirageVisuals widgets.
 *
 * <p>The look matches the reference mock: a soft, slightly desaturated
 * lavender body with a fuzzy outer halo and a clean (noise-free) interior.
 * Text contrasts against the light fill, so dark navy is used for labels.</p>
 */
public final class MirageColors {
    private MirageColors() {
    }

    // Pastel lavender tint of the glass body — slightly cooler / cleaner.
    public static final float TINT_R = 0.90F;
    public static final float TINT_G = 0.90F;
    public static final float TINT_B = 0.98F;

    // Translucency: very low so the dirt / panorama clearly bleeds through.
    // User asked twice for "more transparent" — these are intentionally aggressive.
    public static final float TINT_ALPHA_IDLE     = 0.28F;
    public static final float TINT_ALPHA_HOVER    = 0.42F;
    public static final float TINT_ALPHA_DISABLED = 0.14F;

    // Reserved for future shaders that mix tint with a scene sample.
    public static final float TINT_MIX_IDLE  = 0.45F;
    public static final float TINT_MIX_HOVER = 0.60F;

    // Optional rim — kept very subtle.
    public static final float BORDER_R = 0.85F;
    public static final float BORDER_G = 0.85F;
    public static final float BORDER_B = 0.95F;
    public static final float BORDER_ALPHA = 0.18F;

    // Sheen / glow disabled entirely — user explicitly asked "убери свечение".
    public static final float HIGHLIGHT_IDLE  = 0.0F;
    public static final float HIGHLIGHT_HOVER = 0.0F;

    public static final float CORNER_RADIUS = 10.0F;
    public static final float BORDER_WIDTH  = 0.0F; // no hard border; halo carries the edge

    // Black labels on the lighter body — matches the user's mock.
    public static final int TEXT_COLOR_IDLE     = 0xFF000000;
    public static final int TEXT_COLOR_HOVER    = 0xFF000000;
    public static final int TEXT_COLOR_DISABLED = 0xFF4A4A4A;
}
