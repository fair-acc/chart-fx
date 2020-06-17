open module de.gsi.chartfx.dataset {
    requires org.slf4j;

    requires it.unimi.dsi.fastutil;
    requires jdk.unsupported;

    exports de.gsi.dataset;
    exports de.gsi.dataset.event;
    exports de.gsi.dataset.locks;
    exports de.gsi.dataset.serializer;
    exports de.gsi.dataset.serializer.spi;
    exports de.gsi.dataset.serializer.spi.iobuffer;
    exports de.gsi.dataset.spi;
    exports de.gsi.dataset.spi.utils;
    exports de.gsi.dataset.testdata;
    exports de.gsi.dataset.testdata.spi;
    exports de.gsi.dataset.utils;
    exports de.gsi.dataset.utils.trees;
}