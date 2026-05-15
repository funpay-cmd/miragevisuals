package com.miragevisuals.client.util;

/**
 * Small set of single-purpose easing functions used by the MirageVisuals
 * widgets. All take and return values in {@code [0, 1]}.
 */
public final class Easing {
    private Easing() {
    }

    public static float clamp01(float v) {
        return v < 0.0F ? 0.0F : (v > 1.0F ? 1.0F : v);
    }

    public static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    /** Smooth ease-out: fast start, slow end. */
    public static float easeOutCubic(float t) {
        t = clamp01(t);
        float inv = 1.0F - t;
        return 1.0F - inv * inv * inv;
    }

    /** Symmetric ease in/out (cosine). */
    public static float easeInOutSine(float t) {
        t = clamp01(t);
        return -(float) (Math.cos(Math.PI * t) - 1.0) / 2.0F;
    }

    /** Slight overshoot at the end — good for "pop-in" transitions. */
    public static float easeOutBack(float t) {
        t = clamp01(t);
        float c1 = 1.70158F;
        float c3 = c1 + 1.0F;
        float inv = t - 1.0F;
        return 1.0F + c3 * inv * inv * inv + c1 * inv * inv;
    }
}
