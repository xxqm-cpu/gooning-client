package dev.visualplus;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.util.Identifier;

public final class SpotifyHud {
    private SpotifyHud() {}

    public static void render(DrawContext ctx, RenderTickCounter tick) {
        ModConfig c = ModConfig.get();
        MinecraftClient mc = MinecraftClient.getInstance();
        if (!c.spotifyHud || mc.options.hudHidden || SpotifyClient.title.isEmpty()) return;

        int x = c.hudX, y = c.hudY, w = 160, h = 44;
        ctx.fill(x, y, x + w, y + h, 0xAA000000);

        Identifier art = SpotifyClient.art;
        int tx = x + 4;
        if (art != null) {
            ctx.drawTexture(RenderLayer::getGuiTextured, art, x + 4, y + 4, 0f, 0f, 36, 36, 36, 36);
            tx = x + 44;
        }
        int maxW = x + w - 4 - tx;
        ctx.drawTextWithShadow(mc.textRenderer, mc.textRenderer.trimToWidth(SpotifyClient.title, maxW), tx, y + 6, 0xFFFFFFFF);
        ctx.drawTextWithShadow(mc.textRenderer, mc.textRenderer.trimToWidth(SpotifyClient.artist, maxW), tx, y + 18, 0xFFB3B3B3);

        long dur = Math.max(1, SpotifyClient.durationMs);
        float frac = Math.min(1f, SpotifyClient.currentProgress() / (float) dur);
        int bx = tx, bw = x + w - 4 - tx, by = y + 33;
        ctx.fill(bx, by, bx + bw, by + 3, 0xFF535353);
        ctx.fill(bx, by, bx + (int) (bw * frac), by + 3, 0xFF1DB954);
    }
}
