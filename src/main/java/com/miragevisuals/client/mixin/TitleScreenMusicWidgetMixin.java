package com.miragevisuals.client.mixin;

import com.miragevisuals.client.music.MusicWidget;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Hosts the {@link MusicWidget} on the title screen.
 *
 * <p>The widget paints itself in the top-right corner above the vanilla
 * buttons (RETURN/TAIL render injection so it sits on top of the menu, but
 * still under any modal popups Minecraft draws afterwards).</p>
 */
@Mixin(TitleScreen.class)
public abstract class TitleScreenMusicWidgetMixin extends Screen {
    private TitleScreenMusicWidgetMixin() {
        super(null);
    }

    @Inject(method = "render", at = @At("RETURN"))
    private void miragevisuals$renderMusicWidget(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        MusicWidget.get().render(context, this.width, this.height, mouseX, mouseY);
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void miragevisuals$mouseClicked(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        if (MusicWidget.get().mouseClicked(mouseX, mouseY, button)) {
            cir.setReturnValue(true);
        }
    }
}
