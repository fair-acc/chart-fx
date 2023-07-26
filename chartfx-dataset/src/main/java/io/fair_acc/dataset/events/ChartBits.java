package io.fair_acc.dataset.events;

import java.util.function.IntSupplier;

/**
 * @author ennerf
 */
public enum ChartBits implements IntSupplier {
    AxisLayout, // size needs to be evaluated (e.g. labels may be larger)
    AxisCanvas, // needs to be drawn
    AxisRange, // anything related to min/max, tick marks, etc.
    AxisTickLabelText, // the tick label display w/ unit scaling
    AxisLabelText, // display name or units
    AxisDescriptionRange,
    AxisDescriptionName,

    ChartLayout,
    ChartCanvas,
    ChartAxes,
    ChartRenderers,
    ChartDataSets,
    ChartLegend,
    ChartPlugins,
    ChartPluginState,
    DataSetVisibility,
    DataSetData,
    DataSetRange,
    DataSetName,
    DataSetMetaData,
    DataSetPermutation,
    DataViewWindow, // TODO: WindowMinimisedEvent/WindowMaximisedEvent/... necessary?
    DataSetMeasurement;

    private static final ChartBits[] AllBits = ChartBits.values();
    public static final int KnownMask = BitState.mask(AllBits);
    public static final int AxisMask = BitState.mask(AxisLayout, AxisCanvas, AxisRange, AxisTickLabelText, AxisLabelText);
    public static final int DataSetMask = BitState.mask(ChartDataSets, DataSetVisibility, DataSetData, DataSetRange, DataSetMetaData, DataSetPermutation);

    public static StateListener printer() {
        return PRINTER;
    }

    public static StateListener printerWithStackTrace() {
        return STACK_TRACE_PRINTER;
    }

    private static final StateListener PRINTER = BitState.createDebugPrinter(AllBits);
    private static final StateListener STACK_TRACE_PRINTER = BitState.createDebugPrinterWithStackTrace(AllBits);

    @Override
    public int getAsInt() {
        return bit;
    }

    public boolean isSet(int mask) {
        return BitState.isSet(mask, bit);
    }

    final int bit = 1 << ordinal();

}
