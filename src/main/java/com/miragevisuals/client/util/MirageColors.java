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

    // Pastel lavender tint of the glass body.
    public static final float TINT_R = 0.86F;
    public static final float TINT_G = 0.86F;
    public static final float TINT_B = 0.96F;

    public static final float TINT_ALPHA_IDLE = 0.78F;
    public static final float TINT_ALPHA_HOVER = 0.92F;
    public static final float TINT_ALPHA_DISABLED = 0.42F;

    // Reserved for future shaders that mix tint with a scene sample.
    public static final float TINT_MIX_IDLE = 0.55F;
    public static final float TINT_MIX_HOVER = 0.70F;

    // Optional rim — kept very subtle so it doesn't fight the halo.
    public static final float BORDER_R = 0.78F;
    public static final float BORDER_G = 0.80F;
    public static final float BORDER_B = 0.95F;
    public static final float BORDER_ALPHA = 0.55F;

    public static final float HIGHLIGHT_IDLE = 0.50F;
    public static final float HIGHLIGHT_HOVER = 0.80F;

    public static final float CORNER_RADIUS = 10.0F;
    public static final float BORDER_WIDTH = 0.0F; // no hard border; halo carries the edge

    // Dark navy text reads well on the pale lavender body.
    public static final int TEXT_COLOR_IDLE = 0xFF2A2A55;
    public static final int TEXT_COLOR_HOVER = 0xFF101034;
    public static final int TEXT_COLOR_DISABLED = 0xFF7878A0;
}
