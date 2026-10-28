package dev.visualplus.screen;

import dev.visualplus.ModConfig;
import dev.visualplus.module.Module;
import dev.visualplus.module.ModuleManager;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public class ModulesScreen extends Screen {
    private final Screen parent;

    public ModulesScreen(Screen parent) {
        super(Text.literal("VisualPlus"));
        this.parent = parent;
    }

    private static Text label(Module m) {
        return Text.literal(m.name + ": " + (m.enabled ? "ON" : "OFF"));
    }

    @Override
    protected void init() {
        ModConfig c = ModConfig.get();
        int cx = width / 2, lx = cx - 155, rx = cx + 5, w = 150, step = 24, y = 40;

        int i = 0;
        for (Module m : ModuleManager.ALL) {
            int x = (i % 2 == 0) ? lx : rx;
            int yy = y + (i / 2) * step;
            addDrawableChild(ButtonWidget.builder(label(m), b -> {
                m.toggle();
                b.setMessage(label(m));
            }).dimensions(x, yy, w, 20).tooltip(Tooltip.of(Text.literal(m.description))).build());
            i++;
        }
        int rows = (ModuleManager.ALL.size() + 1) / 2;
        int by = y + rows * step + 6;

        addDrawableChild(new ValueSlider(lx, by, w, 20, "Fly speed", 0.5, 10.0, c.flySpeed, 1, v -> c.flySpeed = v));
        addDrawableChild(ButtonWidget.builder(Text.literal("Visual settings..."),
                b -> client.setScreen(new VisualScreen(this))).dimensions(rx, by, w, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Done"), b -> close())
                .dimensions(cx - 100, by + step + 6, 200, 20).build());
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);
        ctx.drawCenteredTextWithShadow(textRenderer, title, width / 2, 16, 0xFFFFFFFF);
        ctx.drawCenteredTextWithShadow(textRenderer,
                Text.literal("Bind keys for each module in Options > Controls > VisualPlus"), width / 2, 28, 0xFFAAAAAA);
    }

    @Override
    public boolean shouldPause() { return false; }

    @Override
    public void close() { client.setScreen(parent); }

    @Override
    public void removed() { ModConfig.save(); }
}
