package dev.visualplus;

import dev.visualplus.module.Module;
import dev.visualplus.module.ModuleManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.util.math.MathHelper;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

/** Classic "array list" of enabled modules in the top-right corner. */
public final class ModuleHud {
    private ModuleHud() {}

    public static void render(DrawContext ctx, RenderTickCounter tick) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.options.hudHidden || mc.currentScreen != null) return;
        List<Module> on = new ArrayList<>();
        for (Module m : ModuleManager.ALL) if (m.enabled) on.add(m);
        on.sort((a, b) -> mc.textRenderer.getWidth(b.name) - mc.textRenderer.getWidth(a.name));
        int sw = ctx.getScaledWindowWidth();
        long now = System.currentTimeMillis();
        for (int i = 0; i < on.size(); i++) {
            String n = on.get(i).name;
            float hue = ((now / 20L + i * 12L) % 360L) / 360f;
            int col = Color.HSBtoRGB(hue, 0.6f, 1f) | 0xFF000000;
            ctx.drawTextWithShadow(mc.textRenderer, n, sw - mc.textRenderer.getWidth(n) - 4, 4 + i * 10, col);
        }
    }
}
