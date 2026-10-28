package dev.visualplus.screen;

import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.text.Text;

import java.util.function.DoubleConsumer;

public class ValueSlider extends SliderWidget {
    private final String label;
    private final double min, max;
    private final int decimals;
    private final DoubleConsumer onChange;

    public ValueSlider(int x, int y, int w, int h, String label, double min, double max, double value,
                       int decimals, DoubleConsumer onChange) {
        super(x, y, w, h, Text.empty(), (value - min) / (max - min));
        this.label = label;
        this.min = min;
        this.max = max;
        this.decimals = decimals;
        this.onChange = onChange;
        updateMessage();
    }

    private double actual() { return min + (max - min) * this.value; }

    @Override
    protected void updateMessage() {
        setMessage(Text.literal(label + ": " + String.format("%." + decimals + "f", actual())));
    }

    @Override
    protected void applyValue() { onChange.accept(actual()); }
}
