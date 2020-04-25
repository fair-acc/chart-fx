package de.gsi.chart.utils;

public interface PaletteQuantizer {
    int[] getColor(int i);

    int getColorCount();

    int getTransparentIndex();

    int lookup(int r, int g, int b);

    int lookup(int r, int g, int b, int a);
}