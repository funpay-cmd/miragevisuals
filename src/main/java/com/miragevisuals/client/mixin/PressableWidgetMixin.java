package com.miragevisuals.client.mixin;

import com.miragevisuals.client.render.GlassPanelRenderer;
import com.miragevisuals.client.util.MirageColors;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.PressableWidget;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Replaces the vanilla button background on the {@link TitleScreen} with a
 * translucent lavender-glass panel rendered by {@link GlassPanelRenderer},
 * then redraws the label in a black ink colour without a shadow so the
 * text reads cleanly against the pale glass.
 *
 * <p>We deliberately do NOT swap the button font to Krona One: vanilla
 * button labels can contain arbitrary localised text (Cyrillic, CJK, etc.)
 * which Krona One does not cover, and forcing the font would render those
 * characters as tofu boxes. The custom wordmark (where we know the exact
 * Latin upper-case string) handles the Krona styling instead.</p>
 *
 * <p>Falls through to the vanilla render on every other screen.</p>
 */
@Mixin(PressableWidget.class)
public abstract class PressableWidgetMixin extends ClickableWidget {
    @Shadow protected abstract void drawMessage(DrawContext context, TextRenderer textRenderer, int color);

    private PressableWidgetMixin() {
        // Mixins don't actually call this; required only to satisfy the abstract parent.
        super(0, 0, 0, 0, null);
    }

    @Inject(method = "renderWidget", at = @At("HEAD"), cancellable = true)
    private void miragevisuals$renderGlassBackground(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (!(client.currentScreen instanceof TitleScreen)) {
            return;
        }
        if (client.textRenderer == null) {
            return;
        }

        boolean hovered = this.isHovered() || this.isFocused();
        boolean enabled = this.active;

        float tintR = MirageColors.TINT_R;
        float tintG = MirageColors.TINT_G;
        float tintB = MirageColors.TINT_B;
        float tintA;
        float tintMix;
        float highlight;
        int textColor;

        if (!enabled) {
            tintA = MirageColors.TINT_ALPHA_DISABLED * this.alpha;
            tintMix = MirageColors.TINT_MIX_IDLE;
            highlight = 0.0F;
            textColor = MirageColors.TEXT_COLOR_DISABLED;
        } else if (hovered) {
            tintA = MirageColors.TINT_ALPHA_HOVER * this.alpha;
            tintMix = MirageColors.TINT_MIX_HOVER;
            highlight = MirageColors.HIGHLIGHT_HOVER;
            textColor = MirageColors.TEXT_COLOR_HOVER;
        } else {
            tintA = MirageColors.TINT_ALPHA_IDLE * this.alpha;
            tintMix = MirageColors.TINT_MIX_IDLE;
            highlight = MirageColors.HIGHLIGHT_IDLE;
            textColor = MirageColors.TEXT_COLOR_IDLE;
        }

        float borderA = MirageColors.BORDER_ALPHA * this.alpha;

        boolean drewGlass = GlassPanelRenderer.drawPanel(
            this.getX(), this.getY(), this.getWidth(), this.getHeight(),
            MirageColors.CORNER_RADIUS,
            tintR, tintG, tintB, tintA, tintMix,
            highlight,
            MirageColors.BORDER_WIDTH,
            MirageColors.BORDER_R, MirageColors.BORDER_G, MirageColors.BORDER_B, borderA
        );
        if (!drewGlass) {
            // Shader path unavailable (compile error, missing GL features, …);
            // let vanilla draw its original textured button so users still see something.
            return;
        }

        drawBlackLabel(context, client.textRenderer, applyAlpha(textColor, this.alpha));

        ci.cancel();
    }

    private void drawBlackLabel(DrawContext context, TextRenderer tr, int color) {
        Text msg = this.getMessage();
        if (msg == null) {
            return;
        }
        int textW = tr.getWidth(msg);
        int x = this.getX() + (this.getWidth() - textW) / 2;
        int y = this.getY() + (this.getHeight() - tr.fontHeight) / 2 + 1;
        // No shadow — black ink on the pale glass body reads cleanly without it.
        context.drawText(tr, msg, x, y, color, false);
    }

    private static int applyAlpha(int argb, float alpha) {
        int a = (argb >>> 24) & 0xFF;
        int scaled = Math.round(a * Math.max(0.0F, Math.min(1.0F, alpha)));
        return (scaled << 24) | (argb & 0x00FFFFFF);
    }
}
