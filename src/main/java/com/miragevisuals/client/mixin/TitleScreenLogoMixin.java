package com.miragevisuals.client.mixin;

import com.miragevisuals.client.util.MirageFonts;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.LogoDrawer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Replaces the vanilla "MINECRAFT / JAVA EDITION" logo with a custom
 * "MirageVisuals.lol" wordmark rendered in the bundled Krona One font.
 *
 * <p>We do this in two halves:</p>
 * <ol>
 *   <li>@{@link Redirect} on the {@link LogoDrawer#draw(DrawContext, int, float)}
 *       call inside {@link TitleScreen#render} so the textured logo never
 *       gets blitted to the framebuffer.</li>
 *   <li>@{@link Inject} at {@code RETURN} of {@code render} so the custom text
 *       lands on top of the panorama/dirt background, exactly where the
 *       vanilla logo used to sit.</li>
 * </ol>
 *
 * <p>The text is drawn at 3× scale to roughly match the visual weight of the
 * removed bitmap logo; the splash ("Funk soul brother!") and the version /
 * copyright lines are intentionally left untouched.</p>
 */
@Mixin(TitleScreen.class)
public abstract class TitleScreenLogoMixin extends Screen {
    // The bundled Krona One TTF is an all-caps display face, so the wordmark
    // is rendered in upper-case to guarantee every glyph exists in the font
    // — lower-case letters in Krona One don't have outlines and would render
    // as tofu boxes.
    private static final String LOGO_TEXT = "MIRAGEVISUALS.LOL";
    private static final float LOGO_SCALE = 3.0F;

    // Snapshotted from the redirected LogoDrawer.draw(…, float alpha) call so
    // that the @Inject below can fade the wordmark in lockstep with the
    // vanilla title-screen fade-in (buttons / splash / copyright). Without
    // this, the wordmark would pop in at full opacity on the very first frame.
    @Unique
    private float miragevisuals$logoAlpha = 1.0F;

    private TitleScreenLogoMixin() {
        super(null);
    }

    @Redirect(
        method = "render",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/LogoDrawer;draw(Lnet/minecraft/client/gui/DrawContext;IF)V"
        )
    )
    private void miragevisuals$skipVanillaLogo(LogoDrawer drawer, DrawContext ctx, int screenWidth, float alpha) {
        // No-op for the vanilla bitmap, but capture the title-screen fade alpha
        // so the wordmark in the @Inject below can match the fade.
        this.miragevisuals$logoAlpha = alpha;
    }

    @Inject(method = "render", at = @At("RETURN"))
    private void miragevisuals$renderCustomLogo(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        MinecraftClient mc = MinecraftClient.getInstance();
        TextRenderer tr = mc.textRenderer;
        if (tr == null) {
            return;
        }

        MutableText styled = Text.literal(LOGO_TEXT)
            .fillStyle(Style.EMPTY.withFont(MirageFonts.KRONA));
        int rawWidth = tr.getWidth(styled);

        int scaledX = (int) ((this.width / 2.0F) - (rawWidth * LOGO_SCALE) / 2.0F);
        int scaledY = 30;

        // Convert the screen coordinate back into the un-scaled coordinate space
        // we get inside the pushed/scaled matrix: divide by the scale factor.
        int textX = Math.round(scaledX / LOGO_SCALE);
        int textY = Math.round(scaledY / LOGO_SCALE);

        int alphaByte = Math.round(MathHelper.clamp(this.miragevisuals$logoAlpha, 0.0F, 1.0F) * 255.0F);
        if (alphaByte <= 0) {
            return;
        }
        int color = (alphaByte << 24) | 0x00FFFFFF;

        context.getMatrices().push();
        context.getMatrices().scale(LOGO_SCALE, LOGO_SCALE, 1.0F);
        context.drawText(tr, styled, textX, textY, color, true);
        context.getMatrices().pop();
    }
}
