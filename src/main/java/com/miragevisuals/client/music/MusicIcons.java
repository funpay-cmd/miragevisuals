package com.miragevisuals.client.music;

import net.minecraft.client.gui.DrawContext;

/**
 * Tiny procedural icon set drawn with {@link DrawContext#fill(int, int, int, int, int)}.
 *
 * <p>Each icon is parameterised by its bounding box and is rendered as a
 * series of horizontal scanlines. The look matches the rest of the
 * Minecraft UI (chunky pixel-style) without depending on external textures.</p>
 */
public final class MusicIcons {
    private MusicIcons() {
    }

    /** Filled "play" triangle pointing right. */
    public static void drawPlay(DrawContext ctx, int x, int y, int size, int color) {
        int half = size / 2;
        for (int i = 0; i < size; i++) {
            int width = i <= half ? i : (size - i);
            // Anchor on the left edge — the triangle grows then shrinks vertically.
            ctx.fill(x, y + i, x + width, y + i + 1, color);
        }
    }

    /** Two vertical bars — pause icon. */
    public static void drawPause(DrawContext ctx, int x, int y, int size, int color) {
        int barW = Math.max(2, size / 4);
        int gap = Math.max(1, size / 6);
        int left = x + (size / 2) - barW - (gap / 2);
        int right = x + (size / 2) + (gap / 2);
        ctx.fill(left, y, left + barW, y + size, color);
        ctx.fill(right, y, right + barW, y + size, color);
    }

    /** Two right-pointing triangles + a bar — next icon. */
    public static void drawNext(DrawContext ctx, int x, int y, int size, int color) {
        int triSize = (size * 7) / 10;
        int barW = Math.max(2, size / 8);
        int triYOff = (size - triSize) / 2;
        // Left triangle (pointing right).
        drawRightTriangle(ctx, x, y + triYOff, triSize, color);
        // Right triangle, shifted halfway across.
        drawRightTriangle(ctx, x + triSize / 2, y + triYOff, triSize, color);
        // Trailing vertical bar.
        int barX = x + triSize / 2 + triSize - 1;
        ctx.fill(barX, y + triYOff, barX + barW, y + triYOff + triSize, color);
    }

    /** Two left-pointing triangles + a leading bar — previous icon. */
    public static void drawPrev(DrawContext ctx, int x, int y, int size, int color) {
        int triSize = (size * 7) / 10;
        int barW = Math.max(2, size / 8);
        int triYOff = (size - triSize) / 2;
        // Leading vertical bar.
        ctx.fill(x, y + triYOff, x + barW, y + triYOff + triSize, color);
        int triX = x + barW;
        // Right then left triangles relative to bar.
        drawLeftTriangle(ctx, triX, y + triYOff, triSize, color);
        drawLeftTriangle(ctx, triX + triSize / 2, y + triYOff, triSize, color);
    }

    /** Single quaver-style music note. */
    public static void drawMusicNote(DrawContext ctx, int x, int y, int size, int color) {
        // Note head: filled diamond approximated as stepped rectangles.
        int headW = Math.max(6, size * 5 / 12);
        int headH = Math.max(4, size * 4 / 12);
        int headX = x + size / 6;
        int headY = y + size - headH - 1;

        // Slight oval-ish head via two stacked rows.
        ctx.fill(headX + 1, headY, headX + headW - 1, headY + 1, color);
        ctx.fill(headX, headY + 1, headX + headW, headY + headH - 1, color);
        ctx.fill(headX + 1, headY + headH - 1, headX + headW - 1, headY + headH, color);

        // Stem rising from the right side of the head up to the flag.
        int stemW = Math.max(1, size / 14);
        int stemX = headX + headW - stemW;
        int stemTop = y + 1;
        int stemBot = headY + 1;
        ctx.fill(stemX, stemTop, stemX + stemW, stemBot, color);

        // Flag: a small curved bracket at the top of the stem.
        int flagW = Math.max(3, size / 4);
        int flagH = Math.max(2, size / 6);
        ctx.fill(stemX, stemTop, stemX + flagW, stemTop + 1, color);
        ctx.fill(stemX + flagW - 1, stemTop, stemX + flagW, stemTop + flagH, color);
        ctx.fill(stemX, stemTop + flagH - 1, stemX + flagW - 1, stemTop + flagH, color);
    }

    private static void drawRightTriangle(DrawContext ctx, int x, int y, int size, int color) {
        int half = size / 2;
        for (int i = 0; i < size; i++) {
            int w = i <= half ? i : (size - i);
            if (w <= 0) continue;
            ctx.fill(x, y + i, x + w, y + i + 1, color);
        }
    }

    private static void drawLeftTriangle(DrawContext ctx, int x, int y, int size, int color) {
        int half = size / 2;
        for (int i = 0; i < size; i++) {
            int w = i <= half ? i : (size - i);
            if (w <= 0) continue;
            ctx.fill(x + (size - w), y + i, x + size, y + i + 1, color);
        }
    }
}
