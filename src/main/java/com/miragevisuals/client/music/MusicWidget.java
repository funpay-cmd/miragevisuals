package com.miragevisuals.client.music;

import com.miragevisuals.client.render.GlassPanelRenderer;
import com.miragevisuals.client.render.RoundedRectRenderer;
import com.miragevisuals.client.util.Easing;
import com.miragevisuals.client.util.MusicColors;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

/**
 * Main-menu music HUD widget.
 *
 * <p>Lives in the top-right corner of the {@code TitleScreen}. When the SMTC
 * bridge reports an active session it fades in as a small circular bubble
 * carrying a music-note glyph; hovering the bubble expands it into a
 * dark-glass pill that shows the current track, a progress bar and three
 * playback buttons (prev / play-pause / next).</p>
 *
 * <p>The widget owns its own animation state — there is no Fabric tick hook;
 * we drive everything off real time read in {@link #render}. That keeps the
 * animation smooth regardless of the title-screen tick cadence.</p>
 */
public final class MusicWidget {
    private static final MusicWidget INSTANCE = new MusicWidget();

    private static final long HIDE_DELAY_NANOS    = 350_000_000L;   // 0.35s hover-leave delay
    private static final long ANIM_DURATION_NANOS = 320_000_000L;   // 0.32s expand / collapse
    private static final long FADE_DURATION_NANOS = 380_000_000L;   // 0.38s show / hide

    private long lastNanos = System.nanoTime();

    /** 0 = fully collapsed (circle only), 1 = fully expanded (pill visible). */
    private float expansion = 0.0F;
    /** 0 = invisible, 1 = fully shown. */
    private float visibility = 0.0F;

    /** Hover bookkeeping for hysteresis-free collapsing. */
    private boolean wasHovered;
    private long hoverEndNanos;

    // Cached layout from the last render — used by mouseClicked().
    private int circleX, circleY;
    private int pillX, pillY, pillW, pillH;
    private boolean controlsHittable;
    private int prevBtnX, prevBtnY, prevBtnSize;
    private int playBtnX, playBtnY, playBtnSize;
    private int nextBtnX, nextBtnY, nextBtnSize;

    public static MusicWidget get() {
        return INSTANCE;
    }

    private MusicWidget() {
    }

    public void render(DrawContext ctx, int screenW, int screenH, int mouseX, int mouseY) {
        MusicState state = MusicService.get().currentState();
        long now = System.nanoTime();
        float dt = Math.min(0.1F, (now - lastNanos) / 1.0E9F);
        lastNanos = now;

        // Target visibility / expansion.
        boolean wantVisible = state.hasTrack();
        float targetVisibility = wantVisible ? 1.0F : 0.0F;

        // Hover hit-test against the *currently rendered* hull (circle + pill).
        boolean hoverNow = wantVisible && hoverHit(mouseX, mouseY);
        if (hoverNow) {
            wasHovered = true;
            hoverEndNanos = 0L;
        } else if (wasHovered) {
            if (hoverEndNanos == 0L) {
                hoverEndNanos = now;
            } else if (now - hoverEndNanos > HIDE_DELAY_NANOS) {
                wasHovered = false;
            }
        }
        float targetExpansion = wasHovered ? 1.0F : 0.0F;

        visibility = approach(visibility, targetVisibility,
            dt / (FADE_DURATION_NANOS / 1.0E9F));
        expansion = approach(expansion, targetExpansion,
            dt / (ANIM_DURATION_NANOS / 1.0E9F));

        if (visibility <= 0.001F) {
            controlsHittable = false;
            return;
        }

        // Apply easing for display.
        float vis = Easing.easeOutCubic(visibility);
        float exp = Easing.easeInOutSine(expansion);

        renderInternal(ctx, screenW, screenH, mouseX, mouseY, state, vis, exp);
    }

    private void renderInternal(DrawContext ctx, int screenW, int screenH,
                                int mouseX, int mouseY,
                                MusicState state, float vis, float exp) {
        MinecraftClient mc = MinecraftClient.getInstance();
        TextRenderer tr = mc.textRenderer;

        // Pill geometry (anchored top-right). The pill grows downward as
        // expansion goes 0 -> 1.
        int pillFullH = MusicColors.PILL_HEIGHT;
        int pillCurH = Math.round(pillFullH * exp);
        int pillRight = screenW - MusicColors.MARGIN;
        int pillTop = MusicColors.MARGIN + MusicColors.CIRCLE_DIAMETER + MusicColors.PILL_GAP;
        int pillLeft = pillRight - MusicColors.PILL_WIDTH;

        // Slight "pop-in" scale on the circle when it first appears.
        float circleScale = 0.7F + 0.3F * Easing.easeOutBack(vis);
        int circleD = Math.max(8, Math.round(MusicColors.CIRCLE_DIAMETER * circleScale));
        int circleRight = pillRight - 24;            // sits over the pill's right side
        int circleX0 = circleRight - circleD;
        int circleY0 = MusicColors.MARGIN
            + Math.round((MusicColors.CIRCLE_DIAMETER - circleD) / 2.0F);

        // Cache hit-test geometry for mouseClicked().
        this.circleX = circleX0;
        this.circleY = circleY0;
        this.pillX = pillLeft;
        this.pillY = pillTop;
        this.pillW = MusicColors.PILL_WIDTH;
        this.pillH = pillCurH;
        this.controlsHittable = exp > 0.85F;

        // --- Pill body (only when there's something to show).
        if (pillCurH > 4) {
            drawGlass(pillLeft, pillTop, MusicColors.PILL_WIDTH, pillCurH,
                MusicColors.PILL_RADIUS, vis * exp);
            if (exp > 0.55F) {
                renderPillContents(ctx, tr, state, pillLeft, pillTop,
                    MusicColors.PILL_WIDTH, pillCurH, vis,
                    Easing.clamp01((exp - 0.55F) / 0.45F));
            }
        }

        // --- Circle on top with the music-note glyph.
        drawGlass(circleX0, circleY0, circleD, circleD,
            circleD / 2.0F, vis);
        int iconSize = Math.max(8, (int) (circleD * 0.65F));
        int iconX = circleX0 + (circleD - iconSize) / 2;
        int iconY = circleY0 + (circleD - iconSize) / 2;
        MusicIcons.drawMusicNote(ctx, iconX, iconY, iconSize,
            applyAlpha(MusicColors.ICON_PRIMARY, vis));
    }

    private void renderPillContents(DrawContext ctx, TextRenderer tr, MusicState state,
                                    int px, int py, int pw, int ph,
                                    float vis, float reveal) {
        int innerX = px + 14;
        int innerY = py + 12;
        int innerW = pw - 28;

        String title = state.title.isEmpty() ? "Unknown track" : state.title;
        String artist = state.artist;

        int titleColor = applyAlpha(MusicColors.TEXT_PRIMARY, vis * reveal);
        String titleStr = trimToWidth(tr, title, innerW);
        ctx.drawText(tr, titleStr, innerX, innerY, titleColor, false);

        if (!artist.isEmpty()) {
            int artistColor = applyAlpha(MusicColors.TEXT_SECONDARY, vis * reveal);
            String artistStr = trimToWidth(tr, artist, innerW);
            ctx.drawText(tr, artistStr, innerX, innerY + tr.fontHeight + 2,
                artistColor, false);
        }

        // Progress bar.
        int barY = py + ph - 32;
        int barH = 3;
        int barX = innerX;
        int barW = innerW;
        int trackColor = applyAlpha(MusicColors.PROGRESS_TRACK, vis * reveal);
        int fillColor = applyAlpha(MusicColors.PROGRESS_FILL, vis * reveal);
        // Track.
        RoundedRectRenderer.draw(barX, barY, barW, barH, barH / 2.0F, trackColor);
        float p = state.progress();
        int fillW = Math.max(0, Math.round(barW * p));
        if (fillW > 1) {
            RoundedRectRenderer.draw(barX, barY, fillW, barH, barH / 2.0F, fillColor);
        }

        // Playback controls — three buttons centred.
        int btnSize = 14;
        int btnGap = 14;
        int controlsY = py + ph - 22;
        int totalW = btnSize * 3 + btnGap * 2;
        int controlsX = px + (pw - totalW) / 2;

        prevBtnX = controlsX;
        prevBtnY = controlsY;
        prevBtnSize = btnSize;
        playBtnX = controlsX + btnSize + btnGap;
        playBtnY = controlsY;
        playBtnSize = btnSize;
        nextBtnX = controlsX + (btnSize + btnGap) * 2;
        nextBtnY = controlsY;
        nextBtnSize = btnSize;

        int iconColor = applyAlpha(MusicColors.ICON_PRIMARY, vis * reveal);
        int dimColor = applyAlpha(MusicColors.ICON_DIMMED, vis * reveal);

        MusicIcons.drawPrev(ctx, prevBtnX, prevBtnY, btnSize, dimColor);
        if (state.isPlaying) {
            MusicIcons.drawPause(ctx, playBtnX, playBtnY, btnSize, iconColor);
        } else {
            MusicIcons.drawPlay(ctx, playBtnX, playBtnY, btnSize, iconColor);
        }
        MusicIcons.drawNext(ctx, nextBtnX, nextBtnY, btnSize, dimColor);
    }

    private void drawGlass(int x, int y, int w, int h, float radius, float alphaScale) {
        if (alphaScale <= 0.0F) return;
        float tA = MusicColors.TINT_ALPHA * alphaScale;
        float bA = MusicColors.BORDER_ALPHA * alphaScale;
        boolean ok = GlassPanelRenderer.drawPanel(
            x, y, w, h, radius,
            MusicColors.TINT_R, MusicColors.TINT_G, MusicColors.TINT_B, tA, 0.5F,
            MusicColors.HIGHLIGHT,
            1.0F,
            MusicColors.BORDER_R, MusicColors.BORDER_G, MusicColors.BORDER_B, bA
        );
        if (!ok) {
            // Fallback so we don't go invisible if the shader path is unavailable.
            int argb = ((int) (tA * 255) << 24) | 0x121214;
            RoundedRectRenderer.draw(x, y, w, h, radius, argb);
        }
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (visibility < 0.5F || button != 0) {
            return false;
        }
        // Circle: toggle play/pause.
        if (insideCircle((int) mouseX, (int) mouseY)) {
            MusicService.get().playPauseToggle();
            return true;
        }
        if (!controlsHittable) {
            // Pill not yet expanded — clicking elsewhere on the hover region
            // shouldn't pass through to the underlying menu either.
            return insidePill((int) mouseX, (int) mouseY);
        }
        if (inside(prevBtnX, prevBtnY, prevBtnSize, prevBtnSize, mouseX, mouseY)) {
            MusicService.get().prev();
            return true;
        }
        if (inside(playBtnX, playBtnY, playBtnSize, playBtnSize, mouseX, mouseY)) {
            MusicService.get().playPauseToggle();
            return true;
        }
        if (inside(nextBtnX, nextBtnY, nextBtnSize, nextBtnSize, mouseX, mouseY)) {
            MusicService.get().next();
            return true;
        }
        return insidePill((int) mouseX, (int) mouseY);
    }

    private boolean hoverHit(int mouseX, int mouseY) {
        return insideCircle(mouseX, mouseY) || insidePill(mouseX, mouseY);
    }

    private boolean insideCircle(int mx, int my) {
        int cd = MusicColors.CIRCLE_DIAMETER;
        return mx >= circleX && mx < circleX + cd
            && my >= circleY && my < circleY + cd;
    }

    private boolean insidePill(int mx, int my) {
        if (expansion < 0.05F) return false;
        return mx >= pillX && mx < pillX + pillW
            && my >= pillY && my < pillY + pillH;
    }

    private static boolean inside(int x, int y, int w, int h, double mx, double my) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    private static float approach(float current, float target, float step) {
        if (step <= 0.0F) return current;
        if (target > current) {
            return Math.min(target, current + step);
        }
        return Math.max(target, current - step);
    }

    private static int applyAlpha(int argb, float scale) {
        int a = (argb >>> 24) & 0xFF;
        int scaled = Math.round(a * Math.max(0.0F, Math.min(1.0F, scale)));
        return (scaled << 24) | (argb & 0x00FFFFFF);
    }

    private static String trimToWidth(TextRenderer tr, String s, int maxWidth) {
        if (tr.getWidth(s) <= maxWidth) return s;
        String ellipsis = "…";
        int ellipsisW = tr.getWidth(ellipsis);
        int end = s.length();
        while (end > 0 && tr.getWidth(s.substring(0, end)) + ellipsisW > maxWidth) {
            end--;
        }
        return s.substring(0, Math.max(0, end)) + ellipsis;
    }
}
