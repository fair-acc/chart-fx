package de.gsi.chart.renderer;

public enum ContourType {
    CONTOUR, // marching-square based contour plotting algorithm
    CONTOUR_FAST, // experimental contour plotting algorithm
    CONTOUR_HEXAGON, // hexagon-map based contour plotting algorithm
    HEATMAP, // 2D orthogonal projection based plotting algorithm
    HEATMAP_HEXAGON // hexagon-based plotting algorithm
}
