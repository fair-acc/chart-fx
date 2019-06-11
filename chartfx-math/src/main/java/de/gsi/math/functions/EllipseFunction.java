package de.gsi.math.functions;

import java.util.ArrayList;

import de.gsi.dataset.spi.utils.DoublePoint;

public class EllipseFunction extends AbstractFunction implements FunctionND {

    public EllipseFunction(final String name, final int nparm) {
        super(name, nparm);
        // TODO Auto-generated constructor stub
    }

    @Override
    public double getValue(final double[] x, final int i) {
        return 0;
    }

    @Override
    public double[] getValue(final double[] x) {
        // TODO Auto-generated method stub
        return null;
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
    public String getID() {
        return "EllipseFunction@+" + System.currentTimeMillis();
    }

    public ArrayList<DoublePoint> calculateEllipse(final double x, final double y, final double a, final double b,
            final double angle, double steps) {
        if (steps == 0) {
            steps = 36;
        }

        final ArrayList<DoublePoint> points = new ArrayList<>();

        // Angle is given by Degree Value
        final double beta = -angle * (Math.PI / 180); // (Math.PI/180) converts Degree Value into Radians
        final double sinbeta = Math.sin(beta);
        final double cosbeta = Math.cos(beta);

        for (int i = 0; i < 360; i += 360 / steps) {
            final double alpha = i * (Math.PI / 180);
            final double sinalpha = Math.sin(alpha);
            final double cosalpha = Math.cos(alpha);

            final double X = x + (a * cosalpha * cosbeta - b * sinalpha * sinbeta);
            final double Y = y + (a * cosalpha * sinbeta + b * sinalpha * cosbeta);

            points.add(new DoublePoint(X, Y));
        }

        return points;
    }

}
