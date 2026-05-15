package com.miragevisuals.client.music;

import com.miragevisuals.client.MirageVisualsClient;

/**
 * Process-wide singleton wrapping the {@link SmtcBridge}.
 *
 * <p>Started once from {@link MirageVisualsClient#onInitializeClient()};
 * everything that needs to know about the active track reads through
 * {@link #currentState()}.</p>
 *
 * <p>A demo mode is available for local visual testing: set the system
 * property {@code -Dmiragevisuals.demoMusic=true} (or the env var
 * {@code MIRAGEVISUALS_DEMO_MUSIC=1}) and {@link #currentState()} will
 * synthesise a "Demo Track / MirageVisuals" state with a scrolling
 * progress bar so the widget renders without a real SMTC session.</p>
 */
public final class MusicService {
    private static final MusicService INSTANCE = new MusicService();

    private final SmtcBridge bridge = new SmtcBridge();
    private boolean started;
    private final boolean demo;
    private final long demoStartNanos = System.nanoTime();

    private MusicService() {
        this.demo = isDemoFlagSet();
    }

    public static MusicService get() {
        return INSTANCE;
    }

    public synchronized void start() {
        if (started) {
            return;
        }
        if (demo) {
            MirageVisualsClient.LOGGER.info(
                "MirageVisuals music service running in DEMO mode "
              + "(set -Dmiragevisuals.demoMusic=false to disable)");
        } else {
            bridge.start();
        }
        started = true;
        Runtime.getRuntime().addShutdownHook(new Thread(bridge::stop, "MirageVisuals-SMTC-Shutdown"));
    }

    public MusicState currentState() {
        if (demo) {
            long elapsedMs = (System.nanoTime() - demoStartNanos) / 1_000_000L;
            long durationMs = 213_000L;            // ~3:33
            long position = elapsedMs % durationMs;
            return new MusicState(
                true,
                "Demo Track",
                "MirageVisuals",
                "Mirage Sessions",
                position,
                durationMs,
                true,
                "demo"
            );
        }
        return bridge.latest();
    }

    public boolean isSupported() {
        return demo || bridge.isSupported();
    }

    public void playPauseToggle() {
        if (!demo) bridge.sendCommand("toggle");
    }

    public void next() {
        if (!demo) bridge.sendCommand("next");
    }

    public void prev() {
        if (!demo) bridge.sendCommand("prev");
    }

    private static boolean isDemoFlagSet() {
        String sys = System.getProperty("miragevisuals.demoMusic", "");
        if (!sys.isEmpty()) {
            return Boolean.parseBoolean(sys) || "1".equals(sys);
        }
        String env = System.getenv("MIRAGEVISUALS_DEMO_MUSIC");
        return env != null && (Boolean.parseBoolean(env) || "1".equals(env));
    }
}
