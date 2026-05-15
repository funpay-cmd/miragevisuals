package com.miragevisuals.client.music;

import com.miragevisuals.client.MirageVisualsClient;

/**
 * Process-wide singleton wrapping the {@link SmtcBridge}.
 *
 * <p>Started once from {@link MirageVisualsClient#onInitializeClient()};
 * everything that needs to know about the active track reads through
 * {@link #currentState()}.</p>
 */
public final class MusicService {
    private static final MusicService INSTANCE = new MusicService();

    private final SmtcBridge bridge = new SmtcBridge();
    private boolean started;

    private MusicService() {
    }

    public static MusicService get() {
        return INSTANCE;
    }

    public synchronized void start() {
        if (started) {
            return;
        }
        bridge.start();
        started = true;
        Runtime.getRuntime().addShutdownHook(new Thread(bridge::stop, "MirageVisuals-SMTC-Shutdown"));
    }

    public MusicState currentState() {
        return bridge.latest();
    }

    public boolean isSupported() {
        return bridge.isSupported();
    }

    public void playPauseToggle() {
        bridge.sendCommand("toggle");
    }

    public void next() {
        bridge.sendCommand("next");
    }

    public void prev() {
        bridge.sendCommand("prev");
    }
}
