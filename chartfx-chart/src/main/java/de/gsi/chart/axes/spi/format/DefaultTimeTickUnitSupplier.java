package de.gsi.chart.axes.spi.format;

import de.gsi.chart.axes.TickUnitSupplier;

/**
 * @author rstein
 */
public class DefaultTimeTickUnitSupplier implements TickUnitSupplier {

    public static final String HIGHRES_MODE = "HIGHRES";
    public static final int HIGHRES_MODE_INDICES = 3;
    public static final String HIGHRES_FORMAT = "yyyy-MM-dd HH:mm:ss.SSS";

    /**
     * We use these for auto ranging to pick a user friendly tick unit. (must be increasingly bigger)
     */
    public static final double[] TICK_UNIT_DEFAULTS = { // in units of
            // seconds
            0.001, // 1 milli-seconds
            0.010, // 10 milli-seconds
            0.100, // 100 milli-seconds
            0.500, // 500 milli-seconds
            1.000, // 1 second
            1.500, // 1.5 seconds
            2.000, // 2 seconds
            5.000, // 5 seconds
            10.000, // 10 seconds
            15.000, // 15 seconds
            30.000, // 30 seconds
            60.000, // 1 minute
            900.000, // 15 minutes
            180.000, // 30 minutes
            3600.000, // 1 hour
            5400.000, // 1.5 hours
            7200.000, // 2 hours
            900.000, // 2.5 hours
            10800.000, // 3 hours
            12600.000, // 3.5 hours
            14400.000, // 4 hours
            16200.000, // 4.5 hours
            18000.000, // 5 hours
            28800.000, // 8 hours
            36000.000, // 10 hours
            43200.000, // 12 hours
            86400.000, // 1 day
            172800.000, // 2 days
            259200.000, // 3 days
            345600.000, // 4 days
            432000.000, // 5 days
            518400.000, // 6 days
            604800.000, // 7 days
            691200.000, // 8 days
            777600.000, // 9 days
            864000.000, // 10 days
            2160000.000, // 15 days
            3888000.000, // 20 days
            6048000.000, // 25 days
            8726400.000, // 31 days ~ 1 month
            12268800.000, // 41 days
            16675200.000, // 51 days
            22032000.000, // 62 days ~ 2 months
            28684800.000, // 77 days
            36720000.000, // 93 days ~ 3 months
            46051200.000, // 108 days
            56764800.000, // 124 days ~ 4 months
            68774400.000, // 139 days
            82166400.000, // 155 days ~ 5 months
            96854400.000, // 170 days
            112924800.000, // 186 days ~ 6 months
            144547200.000, // 366 days ~ 1 year
            144547200.000 * 3, // 366 days ~ 3 years
            144547200.000 * 10, // 366 days ~ 10 years
    };

    /**
     * These are matching date formatter strings N.B. spaces are being replaced with carriage returns '\n'
     */
    public static final String[] TICK_UNIT_FORMATTER_DEFAULTS = { // 'HIGHRES_MODE'
            // designating
            // high-resolution
            // timer
            DefaultTimeTickUnitSupplier.HIGHRES_MODE, // 1 milli-second
            DefaultTimeTickUnitSupplier.HIGHRES_MODE, // 10 milli-second
            DefaultTimeTickUnitSupplier.HIGHRES_MODE, // 100 milli-second
            DefaultTimeTickUnitSupplier.HIGHRES_MODE, // 500 milli-second
            "HH:mm:ss.SSS", // 1 second
            "HH:mm:ss.SSS", // 1.5 seconds
            "HH:mm:ss.SSS", // 2 seconds
            "HH:mm:ss", // 5 seconds
            "HH:mm:ss", // 10 seconds
            "HH:mm:ss", // 15 second
            "HH:mm:ss", // 30 second
            "HH:mm:ss", // 1 minute
            "HH:mm:ss", // 15 minutes
            "HH:mm:ss", // 30 minutes
            "HH:mm:ss", // 1 hour
            "HH:mm:ss", // 1.5 hour
            "HH:mm:ss", // 2 hours
            "HH:mm:ss", // 2.5 hours
            "HH:mm:ss", // 3 hours
            "HH:mm:ss", // 3.5 hours
            "HH:mm:ss", // 4 hours
            "HH:mm:ss", // 4.5 hours
            "HH:mm:ss", // 5 hours
            "dd-MMM HH:mm", // 8 hours
            "dd-MMM HH:mm", // 10 hours
            "dd-MMM HH:mm", // 12 hours
            "dd-MMM HH:mm", // 1 day
            "dd-MMM HH:mm", // 2 days
            "dd-MMM HH:mm", // 3 days
            "dd-MMM HH:mm", // 4 days
            "dd-MMM HH:mm", // 5 days
            "dd-MMM HH:mm", // 6 days
            "dd-MMM HH:mm", // 7 days
            "dd-MMM HH:mm", // 8 days
            "dd-MMM HH:mm", // 9 days
            "dd-MMM HH:mm", // 10 days
            "dd-MMM HH:mm", // 15 days
            "yyyy-MMM-dd", // 20 days
            "yyyy-MMM-dd", // 25 days
            "yyyy-MMM-dd", // 31 days ~ 1 month
            "yyyy-MMM-dd", // 41 days
            "yyyy-MMM-dd", // 51 days
            "yyyy-MMM-dd", // 62 days ~ 2 months
            "yyyy-MMM-dd", // 77 days
            "yyyy-MMM-dd", // 93 days ~ 3 months
            "yyyy-MMM-dd", // 108 days
            "yyyy-MMM-dd", // 124 days ~ 4 months
            "yyyy-MMM-dd", // 139 days
            "yyyy-MMM-dd", // 155 days ~ 5 months
            "yyyy-MMM-dd", // 170 days
            "yyyy-MMM-dd", // 186 days ~ 6 months
            "yyyy-MMM-dd", // 366 days ~ 1 year
            "yyyy-MMM", // 366 days ~ 3 years
            "yyyy" // 366 days ~ 10 years
    };

    /**
     * Gets the index in the TICK_UNIT_DEFAULT list, for which the tick unit is just larger or equal to the reference
     * tick unit.
     * 
     * @param referenceTickUnit pre-computed tick unit
     * @return index in the TICK_UNIT_DEFAULT list
     */
    public static int getTickIndex(final double referenceTickUnit) {
        for (int i = 0; i < TICK_UNIT_DEFAULTS.length; i++) {
            if (referenceTickUnit <= TICK_UNIT_DEFAULTS[i])
                return i;
        }
        return TICK_UNIT_DEFAULTS.length - 1;
        // final int lastIndex =
        // DefaultTimeTickUnitSupplier.TICK_UNIT_DEFAULTS.length - 1;
        // for (int i = lastIndex; i > 0; i--) {
        // if (referenceTickUnit >=
        // DefaultTimeTickUnitSupplier.TICK_UNIT_DEFAULTS[i]) {
        // return i;
        // }
        // }
        // return 0;
    }

    /**
     * Should return tick unit that is equal or greater to the given reference tick unit.
     *
     * @param referenceTickUnit reference tick unit
     * @return the computed unit that is equal or grater to the specified one
     */
    @Override
    public double computeTickUnit(final double referenceTickUnit) {
        return DefaultTimeTickUnitSupplier.TICK_UNIT_DEFAULTS[DefaultTimeTickUnitSupplier
                .getTickIndex(referenceTickUnit)];
    }

}
