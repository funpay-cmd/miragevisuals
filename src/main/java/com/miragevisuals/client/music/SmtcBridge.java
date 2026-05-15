package com.miragevisuals.client.music;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.miragevisuals.client.MirageVisualsClient;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Windows-only bridge to <code>Windows.Media.Control.*</code> via a small
 * PowerShell helper that lives under
 * {@code assets/miragevisuals/smtc/smtc_poll.ps1}.
 *
 * <p>On {@link #start()} the helper is extracted to a stable cache directory
 * and launched once; its stdout (one JSON line per poll) is parsed on a
 * daemon thread into a {@link MusicState} which callers can read via
 * {@link #latest()}. Playback commands fire a one-shot helper invocation.</p>
 */
public final class SmtcBridge {
    private static final String POLL_RESOURCE = "/assets/miragevisuals/smtc/smtc_poll.ps1";
    private static final String CMD_RESOURCE  = "/assets/miragevisuals/smtc/smtc_cmd.ps1";

    private final AtomicReference<MusicState> latest = new AtomicReference<>(MusicState.EMPTY);
    private Process pollProcess;
    private Thread readerThread;
    private Path pollScript;
    private Path cmdScript;
    private boolean disabled;

    public boolean isSupported() {
        return System.getProperty("os.name", "").toLowerCase().contains("win");
    }

    public MusicState latest() {
        return latest.get();
    }

    public synchronized void start() {
        if (disabled || pollProcess != null) {
            return;
        }
        if (!isSupported()) {
            MirageVisualsClient.LOGGER.info("SMTC bridge disabled: non-Windows OS");
            disabled = true;
            return;
        }
        try {
            Path cache = cacheDir();
            pollScript = extract(POLL_RESOURCE, cache.resolve("smtc_poll.ps1"));
            cmdScript  = extract(CMD_RESOURCE,  cache.resolve("smtc_cmd.ps1"));

            ProcessBuilder pb = new ProcessBuilder(
                "powershell.exe",
                "-NoProfile",
                "-ExecutionPolicy", "Bypass",
                "-File", pollScript.toString()
            );
            pb.redirectErrorStream(true);
            pollProcess = pb.start();

            readerThread = new Thread(this::pumpStdout, "MirageVisuals-SMTC-Reader");
            readerThread.setDaemon(true);
            readerThread.start();
            MirageVisualsClient.LOGGER.info("SMTC bridge started, poll pid={}", pollProcess.pid());
        } catch (Throwable t) {
            MirageVisualsClient.LOGGER.warn("SMTC bridge failed to start; the music widget will not show real data", t);
            disabled = true;
            stopQuiet();
        }
    }

    public synchronized void stop() {
        stopQuiet();
        disabled = true;
    }

    private void stopQuiet() {
        Process p = pollProcess;
        pollProcess = null;
        if (p != null) {
            try {
                p.destroy();
            } catch (Throwable ignored) {
            }
        }
        Thread t = readerThread;
        readerThread = null;
        if (t != null) {
            t.interrupt();
        }
    }

    public void sendCommand(String cmd) {
        if (disabled || cmdScript == null) {
            return;
        }
        if (!("play".equals(cmd) || "pause".equals(cmd) || "toggle".equals(cmd)
              || "next".equals(cmd) || "prev".equals(cmd))) {
            return;
        }
        try {
            ProcessBuilder pb = new ProcessBuilder(
                "powershell.exe",
                "-NoProfile",
                "-ExecutionPolicy", "Bypass",
                "-File", cmdScript.toString(),
                "-cmd", cmd
            );
            pb.redirectErrorStream(true);
            pb.redirectOutput(ProcessBuilder.Redirect.DISCARD);
            pb.start();
        } catch (IOException ex) {
            MirageVisualsClient.LOGGER.debug("SMTC command failed", ex);
        }
    }

    private void pumpStdout() {
        Process p = pollProcess;
        if (p == null) {
            return;
        }
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) {
                    continue;
                }
                if ("null".equals(line)) {
                    latest.set(MusicState.EMPTY);
                    continue;
                }
                MusicState parsed = parse(line);
                if (parsed != null) {
                    latest.set(parsed);
                }
            }
        } catch (IOException ex) {
            MirageVisualsClient.LOGGER.debug("SMTC stdout reader stopped", ex);
        }
    }

    private MusicState parse(String json) {
        try {
            JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
            return new MusicState(
                true,
                str(obj, "title"),
                str(obj, "artist"),
                str(obj, "albumTitle"),
                num(obj, "positionMs"),
                num(obj, "durationMs"),
                obj.has("isPlaying") && !obj.get("isPlaying").isJsonNull() && obj.get("isPlaying").getAsBoolean(),
                str(obj, "source")
            );
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private static String str(JsonObject o, String key) {
        return o.has(key) && !o.get(key).isJsonNull() ? o.get(key).getAsString() : "";
    }

    private static long num(JsonObject o, String key) {
        if (!o.has(key) || o.get(key).isJsonNull()) return 0L;
        try {
            return o.get(key).getAsLong();
        } catch (NumberFormatException ex) {
            return 0L;
        }
    }

    private Path cacheDir() throws IOException {
        String env = System.getenv("LOCALAPPDATA");
        Path base = env != null && !env.isEmpty()
            ? Paths.get(env, "MirageVisuals", "smtc")
            : Paths.get(System.getProperty("user.home"), ".miragevisuals", "smtc");
        Files.createDirectories(base);
        return base;
    }

    private Path extract(String resource, Path target) throws IOException {
        try (InputStream in = SmtcBridge.class.getResourceAsStream(resource)) {
            if (in == null) {
                throw new IOException("Bundled SMTC script missing: " + resource);
            }
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        }
        return target;
    }
}
