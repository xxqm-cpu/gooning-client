package dev.visualplus.screen;

import dev.visualplus.ModConfig;
import dev.visualplus.SpotifyClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

public class SpotifyScreen extends Screen {
    private final Screen parent;
    private TextFieldWidget idField;

    public SpotifyScreen(Screen parent) {
        super(Text.literal("Spotify setup"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        ModConfig c = ModConfig.get();
        int cx = width / 2;
        idField = new TextFieldWidget(textRenderer, cx - 120, 110, 240, 20, Text.literal("Client ID"));
        idField.setMaxLength(64);
        idField.setText(c.spotifyClientId);
        idField.setChangedListener(s -> c.spotifyClientId = s.trim());
        addDrawableChild(idField);

        addDrawableChild(ButtonWidget.builder(Text.literal("Connect"), b -> {
            ModConfig.save();
            SpotifyClient.connect();
        }).dimensions(cx - 120, 140, 118, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Disconnect"), b -> SpotifyClient.disconnect())
                .dimensions(cx + 2, 140, 118, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Back"), b -> close())
                .dimensions(cx - 100, 190, 200, 20).build());
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);
        int cx = width / 2;
        ctx.drawCenteredTextWithShadow(textRenderer, title, cx, 16, 0xFFFFFFFF);
        String[] lines = {
                "1. Go to developer.spotify.com/dashboard and create an app",
                "2. Add this Redirect URI: " + SpotifyClient.REDIRECT,
                "3. Paste the app's Client ID below and press Connect",
        };
        for (int i = 0; i < lines.length; i++) {
            ctx.drawCenteredTextWithShadow(textRenderer, lines[i], cx, 36 + i * 12, 0xFFCCCCCC);
        }
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("Status: " + SpotifyClient.status), cx, 168, 0xFF1DB954);
    }

    @Override
    public boolean shouldPause() { return false; }

    @Override
    public void close() { client.setScreen(parent); }

    @Override
    public void removed() { ModConfig.save(); }
}
