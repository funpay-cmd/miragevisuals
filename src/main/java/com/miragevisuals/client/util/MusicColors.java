package com.miragevisuals.client.util;

/**
 * Palette for the main-menu music widget. The look matches the reference
 * mock: a dark translucent glass panel with high-contrast white text and a
 * thin progress bar.
 */
public final class MusicColors {
    private MusicColors() {
    }

    // Dark translucent glass body.
    public static final float TINT_R = 0.10F;
    public static final float TINT_G = 0.10F;
    public static final float TINT_B = 0.12F;
    public static final float TINT_ALPHA = 0.78F;

    // Subtle whitish rim so the panel doesn't melt into the menu.
    public static final float BORDER_R = 0.95F;
    public static final float BORDER_G = 0.95F;
    public static final float BORDER_B = 1.00F;
    public static final float BORDER_ALPHA = 0.10F;

    // Top sheen — keep gentle to read as glass, not plastic.
    public static final float HIGHLIGHT = 0.35F;

    // Geometry of the two layout poses.
    public static final int MARGIN          = 12;
    public static final int CIRCLE_DIAMETER = 38;
    public static final int PILL_WIDTH      = 200;
    public static final int PILL_HEIGHT     = 88;
    public static final int PILL_GAP        = 8;   // vertical space between circle and pill
    public static final int PILL_RADIUS     = 22;
    public static final int CIRCLE_RADIUS   = CIRCLE_DIAMETER / 2;

    // Text.
    public static final int TEXT_PRIMARY    = 0xFFE6E6EE;
    public static final int TEXT_SECONDARY  = 0xFF9B9BA8;

    // Progress bar.
    public static final int PROGRESS_TRACK  = 0x66FFFFFF;
    public static final int PROGRESS_FILL   = 0xFFEFEFF5;

    // Icon / control button colours.
    public static final int ICON_PRIMARY    = 0xFFE2E2EC;
    public static final int ICON_DIMMED     = 0xFF7E7E8C;
}
