package dev.visualplus.screen;

import dev.visualplus.GlintManager;
import dev.visualplus.ModConfig;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

public class VisualScreen extends Screen {
    private final Screen parent;

    public VisualScreen(Screen parent) {
        super(Text.literal("VisualPlus"));
        this.parent = parent;
    }

    private static Text onOff(String label, boolean v) {
        return Text.literal(label + ": " + (v ? "ON" : "OFF"));
    }

    private ButtonWidget toggle(int x, int y, int w, String label, BooleanSupplier get, Consumer<Boolean> set) {
        return ButtonWidget.builder(onOff(label, get.getAsBoolean()), b -> {
            set.accept(!get.getAsBoolean());
            b.setMessage(onOff(label, get.getAsBoolean()));
        }).dimensions(x, y, w, 20).build();
    }

    @Override
    protected void init() {
        ModConfig c = ModConfig.get();
        int cx = width / 2, lx = cx - 155, rx = cx + 5, w = 150, step = 24, y = 40;

        // ---- left column
        addDrawableChild(new ValueSlider(lx, y, w, 20, "Swing speed", 0.25, 4.0, c.swingSpeed, 2, v -> c.swingSpeed = v));
        addDrawableChild(ButtonWidget.builder(Text.literal("Glint: " + c.glint.label), b -> {
            ModConfig.Glint[] all = ModConfig.Glint.values();
            c.glint = all[(c.glint.ordinal() + 1) % all.length];
            b.setMessage(Text.literal("Glint: " + c.glint.label));
            GlintManager.markDirty();
        }).dimensions(lx, y + step, w, 20).build());
        addDrawableChild(toggle(lx, y + step * 2, w, "Spotify HUD", () -> c.spotifyHud, v -> c.spotifyHud = v));
        addDrawableChild(ButtonWidget.builder(Text.literal("Spotify setup..."),
                b -> client.setScreen(new SpotifyScreen(this))).dimensions(lx, y + step * 3, w, 20).build());

        // ---- right column (aurora)
        addDrawableChild(toggle(rx, y, w, "Aurora sky", () -> c.auroraEnabled, v -> c.auroraEnabled = v));
        addDrawableChild(toggle(rx, y + step, w, "Aurora in daytime", () -> c.auroraAlways, v -> c.auroraAlways = v));
        addDrawableChild(new ValueSlider(rx, y + step * 2, w, 20, "Red", 0, 255, c.auroraR, 0, v -> c.auroraR = (int) Math.round(v)));
        addDrawableChild(new ValueSlider(rx, y + step * 3, w, 20, "Green", 0, 255, c.auroraG, 0, v -> c.auroraG = (int) Math.round(v)));
        addDrawableChild(new ValueSlider(rx, y + step * 4, w, 20, "Blue", 0, 255, c.auroraB, 0, v -> c.auroraB = (int) Math.round(v)));
        addDrawableChild(new ValueSlider(rx, y + step * 5, w, 20, "Aurora brightness", 0.1, 2.0, c.auroraIntensity, 2, v -> c.auroraIntensity = v));

        addDrawableChild(ButtonWidget.builder(Text.literal("Done"), b -> close())
                .dimensions(cx - 100, y + step * 6 + 12, 200, 20).build());
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);
        ModConfig c = ModConfig.get();
        ctx.drawCenteredTextWithShadow(textRenderer, title, width / 2, 16, 0xFFFFFFFF);
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("Menu key: Options > Controls > VisualPlus"),
                width / 2, 28, 0xFFAAAAAA);
        int cx = width / 2;
        int swatchY = 40 + 24 * 6 - 3;
        ctx.fill(cx + 5, swatchY, cx + 155, swatchY + 4, 0xFF000000 | (c.auroraR << 16) | (c.auroraG << 8) | c.auroraB);
    }

    @Override
    public boolean shouldPause() { return false; }

    @Override
    public void close() { client.setScreen(parent); }

    @Override
    public void removed() { ModConfig.save(); }
}
