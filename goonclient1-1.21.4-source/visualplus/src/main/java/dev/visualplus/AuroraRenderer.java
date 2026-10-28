package dev.visualplus;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import org.joml.Matrix4f;

/** Draws animated northern-lights curtains in a user-chosen colour. */
public final class AuroraRenderer {
    private static final int CURTAINS = 4;
    private static final int SEGMENTS = 56;
    private static final float SPAN = 2.0f; // radians covered by one curtain

    private AuroraRenderer() {}

    public static void register() {
        WorldRenderEvents.AFTER_TRANSLUCENT.register(AuroraRenderer::render);
    }

    private static void render(WorldRenderContext ctx) {
        ModConfig c = ModConfig.get();
        if (!c.auroraEnabled) return;
        ClientWorld w = MinecraftClient.getInstance().world;
        if (w == null || w.getRegistryKey() != World.OVERWORLD) return;
        MatrixStack ms = ctx.matrixStack();
        if (ms == null) return;

        float td = ctx.tickCounter().getTickDelta(false);
        float night = 1f;
        if (!c.auroraAlways) {
            float cosA = MathHelper.cos(w.getSkyAngle(td) * MathHelper.TAU);
            night = MathHelper.clamp(-cosA * 1.5f + 0.3f, 0f, 1f);
        }
        night *= 1f - w.getRainGradient(td);
        if (night <= 0.01f) return;

        float intensity = night * (float) c.auroraIntensity * 0.55f;
        float cr = c.auroraR / 255f, cg = c.auroraG / 255f, cb = c.auroraB / 255f;
        // top of the curtain drifts toward a lighter tint of the chosen colour
        float tr = MathHelper.lerp(0.35f, cr, 1f), tg = MathHelper.lerp(0.35f, cg, 1f), tb = MathHelper.lerp(0.35f, cb, 1f);
        float t = (w.getTime() + td) / 20f;
        Matrix4f m = ms.peek().getPositionMatrix();

        BufferBuilder buf = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        for (int k = 0; k < CURTAINS; k++) {
            float base = k * (MathHelper.TAU / CURTAINS) + t * 0.015f;
            float[] a = edge(k, 0f, base, t);
            for (int j = 1; j <= SEGMENTS; j++) {
                float[] b = edge(k, j / (float) SEGMENTS, base, t);
                float aa = a[4] * intensity, ab = b[4] * intensity;
                float a1 = a[2] + a[3] * 0.3f, b1 = b[2] + b[3] * 0.3f;
                float a2 = a[2] + a[3], b2 = b[2] + b[3];

                // lower half: fades in from nothing to full brightness
                buf.vertex(m, a[0], a[2], a[1]).color(cr, cg, cb, 0f);
                buf.vertex(m, a[0], a1, a[1]).color(cr, cg, cb, aa);
                buf.vertex(m, b[0], b1, b[1]).color(cr, cg, cb, ab);
                buf.vertex(m, b[0], b[2], b[1]).color(cr, cg, cb, 0f);
                // upper half: fades out toward the lighter top colour
                buf.vertex(m, a[0], a1, a[1]).color(cr, cg, cb, aa);
                buf.vertex(m, a[0], a2, a[1]).color(tr, tg, tb, 0f);
                buf.vertex(m, b[0], b2, b[1]).color(tr, tg, tb, 0f);
                buf.vertex(m, b[0], b1, b[1]).color(cr, cg, cb, ab);
                a = b;
            }
        }

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        BufferRenderer.drawWithGlobalProgram(buf.end());
        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    }

    /** Returns {x, z, baseY, height, strength} for one point along a curtain (camera-relative). */
    private static float[] edge(int k, float u, float base, float t) {
        float th = base + SPAN * (u - 0.5f);
        float r = 105f + 10f * k + 8f * MathHelper.sin(th * 3f + t * 0.35f + k);
        float y0 = 25f + 6f * MathHelper.sin(th * 2f + t * 0.25f + k * 1.7f);
        float h = 45f + 20f * MathHelper.sin(th * 4f + t * 0.6f + k * 2.3f);
        float str = MathHelper.sin(MathHelper.PI * u) * (0.55f + 0.45f * MathHelper.sin(th * 9f - t * 0.8f + k * 1.3f));
        return new float[]{r * MathHelper.cos(th), r * MathHelper.sin(th), y0, h, Math.max(0f, str)};
    }
}
