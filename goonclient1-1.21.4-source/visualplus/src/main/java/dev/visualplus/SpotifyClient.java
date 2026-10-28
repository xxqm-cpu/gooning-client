package dev.visualplus;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/** Minimal Spotify Web API client (Authorization Code + PKCE, no client secret needed). */
public final class SpotifyClient {
    public static final String REDIRECT = "http://127.0.0.1:8888/callback";

    public static volatile String title = "";
    public static volatile String artist = "";
    public static volatile boolean playing;
    public static volatile long progressMs, durationMs, progressStamp;
    public static volatile String status = "Not connected";
    public static volatile Identifier art;

    private static final HttpClient HTTP = HttpClient.newHttpClient();
    private static String accessToken, refreshToken, artUrl = "";
    private static long expiresAt;
    private static int artCounter;

    private SpotifyClient() {}

    private static Path tokenFile() {
        return FabricLoader.getInstance().getConfigDir().resolve("visualplus_spotify.json");
    }

    public static void start() {
        try {
            if (Files.exists(tokenFile())) {
                JsonObject o = JsonParser.parseString(Files.readString(tokenFile())).getAsJsonObject();
                refreshToken = o.get("refresh_token").getAsString();
                status = "Connecting...";
            }
        } catch (Exception ignored) {}
        ScheduledExecutorService ex = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "visualplus-spotify");
            t.setDaemon(true);
            return t;
        });
        ex.scheduleWithFixedDelay(SpotifyClient::poll, 2, 3, TimeUnit.SECONDS);
    }

    public static boolean isConnected() { return refreshToken != null; }

    public static long currentProgress() {
        if (!playing) return progressMs;
        return Math.min(durationMs, progressMs + (System.currentTimeMillis() - progressStamp));
    }

    public static void disconnect() {
        refreshToken = null;
        accessToken = null;
        title = "";
        artist = "";
        playing = false;
        status = "Not connected";
        try { Files.deleteIfExists(tokenFile()); } catch (Exception ignored) {}
    }

    // ---------------------------------------------------------------- auth
    public static void connect() {
        final String clientId = ModConfig.get().spotifyClientId.trim();
        if (clientId.isEmpty()) { status = "Enter your Client ID first"; return; }
        new Thread(() -> {
            try (ServerSocket ss = new ServerSocket(8888, 1, InetAddress.getByName("127.0.0.1"))) {
                ss.setSoTimeout(180_000);
                byte[] rnd = new byte[48];
                new SecureRandom().nextBytes(rnd);
                String verifier = Base64.getUrlEncoder().withoutPadding().encodeToString(rnd);
                byte[] hash = MessageDigest.getInstance("SHA-256").digest(verifier.getBytes(StandardCharsets.US_ASCII));
                String challenge = Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
                String state = Long.toHexString(new SecureRandom().nextLong());

                String url = "https://accounts.spotify.com/authorize?response_type=code"
                        + "&client_id=" + enc(clientId)
                        + "&scope=" + enc("user-read-currently-playing user-read-playback-state")
                        + "&redirect_uri=" + enc(REDIRECT)
                        + "&code_challenge_method=S256&code_challenge=" + challenge
                        + "&state=" + state;
                status = "Waiting for browser login...";
                Util.getOperatingSystem().open(URI.create(url));

                String code;
                try (Socket s = ss.accept()) {
                    BufferedReader br = new BufferedReader(new InputStreamReader(s.getInputStream(), StandardCharsets.UTF_8));
                    String line = br.readLine();
                    Map<String, String> q = new HashMap<>();
                    String target = line.split(" ")[1];
                    int qi = target.indexOf('?');
                    if (qi >= 0) {
                        for (String pair : target.substring(qi + 1).split("&")) {
                            String[] kv = pair.split("=", 2);
                            q.put(kv[0], kv.length > 1 ? URLDecoder.decode(kv[1], StandardCharsets.UTF_8) : "");
                        }
                    }
                    boolean ok = q.containsKey("code") && state.equals(q.get("state"));
                    String body = ok ? "<h2>Connected! You can close this tab and go back to Minecraft.</h2>"
                                     : "<h2>Login failed. Go back to Minecraft and try again.</h2>";
                    OutputStream os = s.getOutputStream();
                    os.write(("HTTP/1.1 200 OK\r\nContent-Type: text/html\r\nConnection: close\r\n\r\n" + body)
                            .getBytes(StandardCharsets.UTF_8));
                    os.flush();
                    if (!ok) throw new IOException("login was denied or state mismatch");
                    code = q.get("code");
                }

                JsonObject j = tokenRequest("grant_type=authorization_code&code=" + enc(code)
                        + "&redirect_uri=" + enc(REDIRECT)
                        + "&client_id=" + enc(clientId)
                        + "&code_verifier=" + enc(verifier));
                applyToken(j);
                status = "Connected";
            } catch (Exception e) {
                status = "Connect failed: " + e.getMessage();
            }
        }, "visualplus-spotify-auth").start();
    }

    private static String enc(String s) { return URLEncoder.encode(s, StandardCharsets.UTF_8); }

    private static JsonObject tokenRequest(String form) throws Exception {
        HttpRequest req = HttpRequest.newBuilder(URI.create("https://accounts.spotify.com/api/token"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(form)).build();
        HttpResponse<String> r = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
        if (r.statusCode() != 200) throw new IOException("HTTP " + r.statusCode() + " " + r.body());
        return JsonParser.parseString(r.body()).getAsJsonObject();
    }

    private static void applyToken(JsonObject j) throws IOException {
        accessToken = j.get("access_token").getAsString();
        expiresAt = System.currentTimeMillis() + j.get("expires_in").getAsLong() * 1000L;
        if (j.has("refresh_token")) {
            refreshToken = j.get("refresh_token").getAsString();
            JsonObject o = new JsonObject();
            o.addProperty("refresh_token", refreshToken);
            Files.writeString(tokenFile(), o.toString());
        }
    }

    private static void ensureToken() throws Exception {
        if (accessToken != null && System.currentTimeMillis() < expiresAt - 30_000) return;
        applyToken(tokenRequest("grant_type=refresh_token&refresh_token=" + enc(refreshToken)
                + "&client_id=" + enc(ModConfig.get().spotifyClientId.trim())));
    }

    // ---------------------------------------------------------------- polling
    private static void poll() {
        try {
            if (refreshToken == null) return;
            ensureToken();
            HttpRequest req = HttpRequest.newBuilder(URI.create("https://api.spotify.com/v1/me/player/currently-playing"))
                    .header("Authorization", "Bearer " + accessToken).GET().build();
            HttpResponse<String> r = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
            int sc = r.statusCode();
            if (sc == 204) { title = ""; playing = false; status = "Connected - nothing playing"; return; }
            if (sc == 401) { accessToken = null; return; }
            if (sc != 200) { status = "Spotify error " + sc; return; }

            JsonObject o = JsonParser.parseString(r.body()).getAsJsonObject();
            JsonElement itemEl = o.get("item");
            if (itemEl == null || itemEl.isJsonNull()) { title = ""; return; }
            JsonObject item = itemEl.getAsJsonObject();

            String name = item.get("name").getAsString();
            StringBuilder sb = new StringBuilder();
            if (item.has("artists")) {
                for (JsonElement a : item.getAsJsonArray("artists")) {
                    if (sb.length() > 0) sb.append(", ");
                    sb.append(a.getAsJsonObject().get("name").getAsString());
                }
            }
            durationMs = item.get("duration_ms").getAsLong();
            progressMs = o.has("progress_ms") && !o.get("progress_ms").isJsonNull() ? o.get("progress_ms").getAsLong() : 0;
            progressStamp = System.currentTimeMillis();
            playing = o.get("is_playing").getAsBoolean();
            artist = sb.toString();
            title = name;
            status = "Connected";

            if (item.has("album")) {
                JsonArray imgs = item.getAsJsonObject("album").getAsJsonArray("images");
                if (imgs != null && imgs.size() > 0) {
                    String url = imgs.get(imgs.size() - 1).getAsJsonObject().get("url").getAsString();
                    if (!url.equals(artUrl)) { artUrl = url; loadArt(url); }
                }
            }
        } catch (Exception e) {
            status = "Error: " + e.getMessage();
        }
    }

    private static void loadArt(String url) throws Exception {
        byte[] data = HTTP.send(HttpRequest.newBuilder(URI.create(url)).GET().build(),
                HttpResponse.BodyHandlers.ofByteArray()).body();
        BufferedImage src = ImageIO.read(new ByteArrayInputStream(data));
        if (src == null) return;
        BufferedImage dst = new BufferedImage(36, 36, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = dst.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(src, 0, 0, 36, 36, null);
        g.dispose();
        ByteArrayOutputStream bo = new ByteArrayOutputStream();
        ImageIO.write(dst, "png", bo);
        NativeImage ni = NativeImage.read(new ByteArrayInputStream(bo.toByteArray()));
        MinecraftClient mc = MinecraftClient.getInstance();
        mc.execute(() -> {
            Identifier id = Identifier.of("visualplus", "spotify_art_" + (artCounter++));
            mc.getTextureManager().registerTexture(id, new NativeImageBackedTexture(ni));
            Identifier old = art;
            art = id;
            if (old != null) mc.getTextureManager().destroyTexture(old);
        });
    }
}
