package io.fair_acc.math.functions;

import java.util.ArrayList;
import java.util.List;

import io.fair_acc.dataset.spi.utils.DoublePoint;

public class EllipseFunction extends AbstractFunction implements FunctionND {

    public EllipseFunction(final String name, final int nparm) {
        super(name, nparm);
    }

    @Override
    public String getID() {
        return "EllipseFunction@+" + System.currentTimeMillis();
    }

    @Override
    public int getInputDimension() {
        return 1;
    }

    @Override
    public int getOutputDimension() {
        return 2;
    }

    @Override
    public double[] getValue(final double[] x) {
        throw new IllegalArgumentException("not implemented");
    }

    @Override
    public double getValue(final double[] x, final int i) {
        return 0;
    }

    public static List<DoublePoint> calculateEllipse(final double centerX, final double centerY, final double halfAxisA,
            final double halfAxisB, final double angle, final double steps) {
        final int dsteps = steps == 0 ? (int) (360.0 / 32) : (int) (360.0 / steps);

        final ArrayList<DoublePoint> points = new ArrayList<>();

        // Angle is given by Degree Value
        final double beta = -angle * (Math.PI / 180); // (Math.PI/180) converts Degree Value into Radians
        final double sinbeta = Math.sin(beta);
        final double cosbeta = Math.cos(beta);

        for (int i = 0; i < 360; i += dsteps) {
            final double alpha = i * (Math.PI / 180);
            final double sinalpha = Math.sin(alpha);
            final double cosalpha = Math.cos(alpha);

            final double posX = centerX + (halfAxisA * cosalpha * cosbeta - halfAxisB * sinalpha * sinbeta);
            final double posY = centerY + (halfAxisA * cosalpha * sinbeta + halfAxisB * sinalpha * cosbeta);

            points.add(new DoublePoint(posX, posY));
        }

        return points;
    }

}
