package dev.visualplus;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.resource.Resource;
import net.minecraft.util.Identifier;

import java.io.InputStream;
import java.util.Optional;

/**
 * Tints the vanilla enchantment glint textures by swapping them in the texture manager.
 * Works for items, armor and entities because they all sample these two textures.
 */
public final class GlintManager {
    private static final Identifier[] TARGETS = {
            Identifier.ofVanilla("textures/misc/enchanted_glint_item.png"),
            Identifier.ofVanilla("textures/misc/enchanted_glint_entity.png")
    };
    private static boolean dirty = true;

    private GlintManager() {}

    public static void markDirty() { dirty = true; }

    public static void tick(MinecraftClient mc) {
        if (!dirty || mc.getResourceManager() == null) return;
        dirty = false;
        apply(mc);
    }

    private static void apply(MinecraftClient mc) {
        TextureManager tm = mc.getTextureManager();
        ModConfig.Glint g = ModConfig.get().glint;
        for (Identifier id : TARGETS) {
            try {
                if (g == ModConfig.Glint.DEFAULT) {
                    tm.destroyTexture(id); // vanilla texture is lazily re-loaded
                    continue;
                }
                Optional<Resource> res = mc.getResourceManager().getResource(id);
                if (res.isEmpty()) continue;
                int tr = (g.rgb >> 16) & 255, tg = (g.rgb >> 8) & 255, tb = g.rgb & 255;
                try (InputStream in = res.get().getInputStream(); NativeImage src = NativeImage.read(in)) {
                    NativeImage out = new NativeImage(src.getWidth(), src.getHeight(), false);
                    for (int y = 0; y < src.getHeight(); y++) {
                        for (int x = 0; x < src.getWidth(); x++) {
                            int argb = src.getColorArgb(x, y);
                            int a = (argb >>> 24) & 255;
                            int lum = Math.max((argb >> 16) & 255, Math.max((argb >> 8) & 255, argb & 255));
                            int nr = lum * tr / 255, ng = lum * tg / 255, nb = lum * tb / 255;
                            out.setColorArgb(x, y, (a << 24) | (nr << 16) | (ng << 8) | nb);
                        }
                    }
                    tm.registerTexture(id, new NativeImageBackedTexture(out));
                }
            } catch (Exception e) {
                System.err.println("[VisualPlus] Glint tint failed: " + e);
            }
        }
    }
}
