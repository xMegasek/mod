package pl.anarchiabot;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

/**
 * Zarzadza procesem bota (Node + mineflayer). Komunikacja: JSON, jedna linia = jedna wiadomosc.
 */
public class BotManager {
    private static final BotManager INSTANCE = new BotManager();

    public static BotManager get() {
        return INSTANCE;
    }

    private final List<String> log = new ArrayList<>();
    private Process process;
    private BufferedWriter writer;
    private volatile boolean starting = false;
    private volatile boolean connected = false;

    private final Path dir = FabricLoader.getInstance().getConfigDir().resolve("anarchiabot");

    public synchronized List<String> getLog() {
        return new ArrayList<>(log);
    }

    public synchronized void addLog(String line) {
        log.add(line);
        while (log.size() > 300) {
            log.remove(0);
        }
    }

    public boolean isRunning() {
        return process != null && process.isAlive();
    }

    public boolean isConnected() {
        return connected && isRunning();
    }

    public boolean isStarting() {
        return starting;
    }

    private void extract(String name) throws IOException {
        try (InputStream in = BotManager.class.getResourceAsStream("/bot/" + name)) {
            if (in == null) throw new IOException("Brak zasobu: " + name);
            Files.copy(in, dir.resolve(name), StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static boolean windows() {
        return System.getProperty("os.name").toLowerCase().contains("win");
    }

    /** Uruchamia proces bota (jesli trzeba, instaluje zaleznosci) i wysyla komende connect. */
    public void connect(String host, int port, String username, String version, boolean autoRules, String password) {
        if (starting) return;
        if (isRunning()) {
            addLog("[mod] Bot juz dziala. Najpierw go rozlacz.");
            return;
        }
        starting = true;
        Thread t = new Thread(() -> {
            try {
                Files.createDirectories(dir);
                extract("bot.js");
                extract("package.json");

                if (!Files.exists(dir.resolve("node_modules").resolve("mineflayer"))) {
                    addLog("[mod] Pierwsze uruchomienie: instaluje mineflayer (npm install)...");
                    ProcessBuilder npm = new ProcessBuilder(windows() ? "npm.cmd" : "npm", "install", "--no-audit", "--no-fund");
                    npm.directory(dir.toFile());
                    npm.redirectErrorStream(true);
                    Process p = npm.start();
                    try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
                        String l;
                        while ((l = r.readLine()) != null) addLog("[npm] " + l);
                    }
                    if (p.waitFor() != 0) {
                        addLog("[mod] BLAD: npm install nie powiodl sie. Zainstaluj Node.js (nodejs.org) i sprobuj ponownie.");
                        return;
                    }
                }

                ProcessBuilder pb = new ProcessBuilder(windows() ? "node.exe" : "node", "bot.js");
                pb.directory(dir.toFile());
                pb.redirectErrorStream(true);
                process = pb.start();
                writer = new BufferedWriter(new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8));

                Thread reader = new Thread(() -> readLoop(process), "AnarchiaBot-reader");
                reader.setDaemon(true);
                reader.start();

                JsonObject o = new JsonObject();
                o.addProperty("cmd", "connect");
                o.addProperty("host", host);
                o.addProperty("port", port);
                o.addProperty("username", username);
                if (version != null && !version.isBlank()) o.addProperty("version", version);
                o.addProperty("autoRules", autoRules);
                if (password != null && !password.isBlank()) o.addProperty("password", password);
                send(o);
            } catch (IOException e) {
                addLog("[mod] BLAD uruchamiania (czy jest zainstalowany Node.js?): " + e.getMessage());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                starting = false;
            }
        }, "AnarchiaBot-start");
        t.setDaemon(true);
        t.start();
    }

    private void readLoop(Process p) {
        try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = r.readLine()) != null) {
                try {
                    JsonObject o = JsonParser.parseString(line).getAsJsonObject();
                    String type = o.has("type") ? o.get("type").getAsString() : "log";
                    String text = o.has("text") ? o.get("text").getAsString() : "";
                    if (type.equals("state")) {
                        connected = text.equals("connected");
                        addLog("[bot] stan: " + text);
                    } else {
                        addLog(text);
                    }
                } catch (Exception ex) {
                    addLog(line);
                }
            }
        } catch (IOException ignored) {
        }
        connected = false;
        addLog("[mod] Proces bota zakonczony.");
    }

    public synchronized void send(JsonObject o) {
        if (!isRunning() || writer == null) {
            addLog("[mod] Bot nie dziala.");
            return;
        }
        try {
            writer.write(o.toString());
            writer.newLine();
            writer.flush();
        } catch (IOException e) {
            addLog("[mod] Blad wysylania do bota: " + e.getMessage());
        }
    }

    public void chat(String text) {
        JsonObject o = new JsonObject();
        o.addProperty("cmd", "chat");
        o.addProperty("text", text);
        send(o);
    }

    public void clickSlot(int slot) {
        JsonObject o = new JsonObject();
        o.addProperty("cmd", "click");
        o.addProperty("slot", slot);
        send(o);
    }

    public void simple(String cmd) {
        JsonObject o = new JsonObject();
        o.addProperty("cmd", cmd);
        send(o);
    }

    public void setAutoRules(boolean on) {
        JsonObject o = new JsonObject();
        o.addProperty("cmd", "autoRules");
        o.addProperty("value", on);
        send(o);
    }

    public synchronized void stop() {
        connected = false;
        if (process != null && process.isAlive()) {
            try {
                simple("disconnect");
            } catch (Exception ignored) {
            }
            process.destroy();
        }
        process = null;
        writer = null;
    }
}
