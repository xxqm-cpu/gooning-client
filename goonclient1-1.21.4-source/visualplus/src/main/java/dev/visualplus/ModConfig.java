package dev.visualplus;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Files;
import java.nio.file.Path;

public class ModConfig {
    public enum Glint {
        DEFAULT("Default", 0),
        RED("Red", 0xFF2020),
        PINK("Pink", 0xFF69B4),
        WHITE("White", 0xFFFFFF),
        BLACK("Black", 0x101010);

        public final String label;
        public final int rgb;
        Glint(String label, int rgb) { this.label = label; this.rgb = rgb; }
    }

    // Modules
    public double flySpeed = 2.0;
    public java.util.List<String> enabledModules = new java.util.ArrayList<>();

    public void setModule(String name, boolean on) {
        enabledModules.remove(name);
        if (on) enabledModules.add(name);
    }

    // Combat visuals
    public double swingSpeed = 1.0;
    public Glint glint = Glint.DEFAULT;

    // Aurora sky
    public boolean auroraEnabled = true;
    public boolean auroraAlways = false;
    public int auroraR = 40, auroraG = 255, auroraB = 140;
    public double auroraIntensity = 1.0;

    // Spotify
    public boolean spotifyHud = true;
    public int hudX = 6, hudY = 6;
    public String spotifyClientId = "";

    private static ModConfig instance = new ModConfig();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static Path path() {
        return FabricLoader.getInstance().getConfigDir().resolve("visualplus.json");
    }

    public static ModConfig get() { return instance; }

    public static void load() {
        try {
            Path p = path();
            if (Files.exists(p)) {
                ModConfig c = GSON.fromJson(Files.readString(p), ModConfig.class);
                if (c != null) {
                    if (c.glint == null) c.glint = Glint.DEFAULT;
                    if (c.enabledModules == null) c.enabledModules = new java.util.ArrayList<>();
                    instance = c;
                }
            }
        } catch (Exception e) {
            System.err.println("[VisualPlus] Could not load config: " + e);
        }
    }

    public static void save() {
        try {
            Files.writeString(path(), GSON.toJson(instance));
        } catch (Exception e) {
            System.err.println("[VisualPlus] Could not save config: " + e);
        }
    }
}
