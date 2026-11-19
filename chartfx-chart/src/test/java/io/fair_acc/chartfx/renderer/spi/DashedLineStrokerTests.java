package io.fair_acc.chartfx.renderer.spi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Tests the dash cutting of {@link DashedLineStroker} without a scene
 */
class DashedLineStrokerTests {
    private static final double EPS = 1e-9;

    @Test
    void cutsPatternAlongTheLine() {
        var lines = stroke(new double[] { 10, 5 }, 0, 1000, 1000, new double[] { 0, 40 }, new double[] { 0, 0 });
        assertLines(lines, new double[][] { { 0, 10 }, { 15, 25 }, { 30, 40 } });
    }

    @Test
    void repeatsOddArrays() {
        var lines = stroke(new double[] { 10 }, 0, 1000, 1000, new double[] { 0, 40 }, new double[] { 0, 0 });
        assertLines(lines, new double[][] { { 0, 10 }, { 20, 30 } });
    }

    @Test
    void honorsTheDashOffset() {
        var lines = stroke(new double[] { 10, 5 }, 3, 1000, 1000, new double[] { 0, 40 }, new double[] { 0, 0 });
        assertLines(lines, new double[][] { { 0, 7 }, { 12, 22 }, { 27, 37 } });
        var negative = stroke(new double[] { 10, 5 }, -3, 1000, 1000, new double[] { 0, 40 }, new double[] { 0, 0 });
        assertLines(negative, new double[][] { { 3, 13 }, { 18, 28 }, { 33, 40 } });
    }

    @Test
    void keepsPhaseAcrossSegments() {
        var whole = stroke(new double[] { 10, 5 }, 0, 1000, 1000, new double[] { 0, 40 }, new double[] { 0, 0 });
        var split = stroke(new double[] { 10, 5 }, 0, 1000, 1000, new double[] { 0, 7, 12, 40 }, new double[] { 0, 0, 0, 0 });
        assertEquals(covered(whole), covered(split), EPS);
        assertTrue(split.stream().allMatch(l -> isInsideOnPiece(l[0], l[2], 10, 15)));
    }

    @Test
    void measuresAlongTheSlope() {
        // 50 long, so a 10/10 pattern draws 0-10, 20-30 and 40-50 of it
        var lines = stroke(new double[] { 10, 10 }, 0, 1000, 1000, new double[] { 0, 30 }, new double[] { 0, 40 });
        assertEquals(3, lines.size());
        assertEquals(30, covered(lines), EPS);
        assertEquals(6, lines.get(0)[2], EPS);
        assertEquals(8, lines.get(0)[3], EPS);
    }

    @Test
    void dashesVerticalSegments() {
        var lines = stroke(new double[] { 10, 5 }, 0, 1000, 1000, new double[] { 3, 3 }, new double[] { 40, 0 });
        assertEquals(3, lines.size());
        assertEquals(30, covered(lines), EPS);
        assertTrue(lines.stream().allMatch(l -> l[0] == 3 && l[2] == 3));
    }

    @Test
    void dashesSteepZigZagsEvenly() {
        // a fast command trace: each segment is 1 px wide and 100 px tall
        int n = 200;
        double[] x = new double[n], y = new double[n];
        for (int i = 0; i < n; i++) {
            x[i] = i;
            y[i] = i % 2 == 0 ? 100 : 200;
        }
        var lines = stroke(new double[] { 10, 10 }, 0, 1000, 1000, x, y);
        double total = (n - 1) * Math.hypot(1, 100);
        assertEquals(total / 2, covered(lines), 10);
        assertTrue(lines.stream().allMatch(l -> Math.hypot(l[2] - l[0], l[3] - l[1]) <= 10 + EPS));
    }

    @Test
    void restartsAtTheFirstPoint() {
        double shift = 3.7;
        var before = stroke(new double[] { 10, 5 }, 0, 1000, 1000, new double[] { 20, 60 }, new double[] { 0, 0 });
        var after = stroke(new double[] { 10, 5 }, 0, 1000, 1000, new double[] { 20 + shift, 60 + shift }, new double[] { 0, 0 });
        assertEquals(before.size(), after.size());
        for (int i = 0; i < before.size(); i++) {
            assertEquals(before.get(i)[0] + shift, after.get(i)[0], EPS);
            assertEquals(before.get(i)[2] + shift, after.get(i)[2], EPS);
        }
    }

    @Test
    void clipsSegmentsFarOutsideThePlot() {
        var lines = assertTimeoutPreemptively(Duration.ofSeconds(1),
                () -> stroke(new double[] { 1, 1 }, 0, 100, 100, new double[] { -1e12, 1e12 }, new double[] { 50, 50 }));
        assertTrue(lines.size() <= 53, "lines: " + lines.size());
        assertTrue(lines.stream().allMatch(l -> l[0] >= -2 && l[2] <= 102));
    }

    @Test
    void keepsPhaseAcrossClippedSegments() {
        // the first segment runs far above the plot, the second one is visible
        double[] x = { 10, 10, 50 }, y = { -1000, 50, 50 };
        var clipped = stroke(new double[] { 10, 5 }, 0, 100, 100, x, y);
        var unclipped = stroke(new double[] { 10, 5 }, 0, 1e6, 1e6, x, y);
        var visible = unclipped.stream().filter(l -> l[1] == 50 && l[3] == 50).toList();
        var clippedVisible = clipped.stream().filter(l -> l[1] == 50 && l[3] == 50).toList();
        assertEquals(visible.size(), clippedVisible.size());
        for (int i = 0; i < visible.size(); i++) {
            assertEquals(visible.get(i)[0], clippedVisible.get(i)[0], EPS);
            assertEquals(visible.get(i)[2], clippedVisible.get(i)[2], EPS);
        }
    }

    @Test
    void skipsNonFiniteInput() {
        assertTimeoutPreemptively(Duration.ofSeconds(1), () -> {
            assertEquals(0, stroke(new double[] { 10, 5 }, Double.NEGATIVE_INFINITY, 100, 100, new double[] { 0, 40 }, new double[] { 0, 0 }).size());
            assertEquals(0, stroke(new double[] { 10, 5 }, Double.NaN, 100, 100, new double[] { 0, 40 }, new double[] { 0, 0 }).size());
            assertEquals(0, stroke(new double[] { 10, 5 }, 0, 100, 100, new double[] { 0, Double.POSITIVE_INFINITY }, new double[] { 0, 0 }).size());
            assertEquals(0, stroke(new double[] { 10, 5 }, 0, 100, 100, new double[] { 0, 40 }, new double[] { 0, Double.NaN }).size());
        });
    }

    @Test
    void drawsPointsInsideADash() {
        assertEquals(1, stroke(new double[] { 10, 5 }, 7, 100, 100, new double[] { 3, 3 }, new double[] { 7, 7 }).size());
        assertEquals(0, stroke(new double[] { 10, 5 }, 12, 100, 100, new double[] { 3, 3 }, new double[] { 7, 7 }).size());
    }

    private static List<double[]> stroke(double[] dashes, double offset, double width, double height, double[] x, double[] y) {
        List<double[]> lines = new ArrayList<>();
        DashedLineStroker.strokeDashes((x0, y0, x1, y1) -> lines.add(new double[] { x0, y0, x1, y1 }),
                dashes, offset, width, height, x, y, x.length);
        return lines;
    }

    private static void assertLines(List<double[]> lines, double[][] expectedX) {
        assertEquals(expectedX.length, lines.size());
        for (int i = 0; i < expectedX.length; i++) {
            assertEquals(expectedX[i][0], lines.get(i)[0], EPS);
            assertEquals(expectedX[i][1], lines.get(i)[2], EPS);
        }
    }

    private static double covered(List<double[]> lines) {
        return lines.stream().mapToDouble(l -> Math.hypot(l[2] - l[0], l[3] - l[1])).sum();
    }

    private static boolean isInsideOnPiece(double from, double to, double on, double period) {
        double start = Math.floor(from / period) * period;
        return from >= start - EPS && to <= start + on + EPS;
    }
}
