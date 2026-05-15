package com.miragevisuals.client.mixin;

import com.miragevisuals.client.render.GlassPanelRenderer;
import com.miragevisuals.client.util.MirageColors;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.PressableWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Replaces the vanilla button background on the {@link TitleScreen} with a
 * dark-blue glass panel rendered by {@link GlassPanelRenderer}. Falls through
 * to the vanilla render on every other screen so we don't accidentally restyle
 * world-selection lists, options, etc.
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
            highlight = MirageColors.HIGHLIGHT_IDLE * 0.5F;
            textColor = MirageColors.TEXT_COLOR_DISABLED;
        } else if (hovered) {
            tintA = MirageColors.TINT_ALPHA_HOVER * this.alpha;
            tintMix = MirageColors.TINT_MIX_HOVER;
            highlight = MirageColors.HIGHLIGHT_HOVER;
            textColor = MirageColors.TEXT_COLOR_HOVER;
            // Hovered buttons read a touch brighter.
            tintR = Math.min(1.0F, tintR + 0.08F);
            tintG = Math.min(1.0F, tintG + 0.12F);
            tintB = Math.min(1.0F, tintB + 0.20F);
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

        this.drawMessage(context, client.textRenderer, applyAlpha(textColor, this.alpha));

        ci.cancel();
    }

    private static int applyAlpha(int argb, float alpha) {
        int a = (argb >>> 24) & 0xFF;
        int scaled = Math.round(a * Math.max(0.0F, Math.min(1.0F, alpha)));
        return (scaled << 24) | (argb & 0x00FFFFFF);
    }
}
