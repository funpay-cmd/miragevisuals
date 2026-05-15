package com.miragevisuals.client;

import com.miragevisuals.client.music.MusicService;
import com.miragevisuals.client.render.GlassPanelRenderer;
import com.miragevisuals.client.render.RoundedRectRenderer;
import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MirageVisualsClient implements ClientModInitializer {
    public static final String MOD_ID = "miragevisuals";
    public static final String NAME = "MirageVisuals";
    public static final Logger LOGGER = LoggerFactory.getLogger(NAME);

    @Override
    public void onInitializeClient() {
        LOGGER.info("MirageVisuals {} loading", "0.1.0");
        // Renderers initialize lazily on first draw call (needs a live GL context).
        RoundedRectRenderer.markPending();
        GlassPanelRenderer.markPending();
        // Spin up the SMTC bridge so the main-menu music widget can show the
        // user's current track. No-op on non-Windows; fails open otherwise.
        MusicService.get().start();
    }
}
