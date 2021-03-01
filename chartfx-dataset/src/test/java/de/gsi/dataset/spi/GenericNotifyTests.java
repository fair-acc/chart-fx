package de.gsi.dataset.spi;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import de.gsi.dataset.DataSet;

public class GenericNotifyTests {
    @Test
    void notifyTests() { // NOPMD
        testDataSetSetterNotify(DoubleDataSet.class);
        testDataSetSetterNotify(DoubleErrorDataSet.class);
        testDataSetSetterNotify(FloatDataSet.class);
        testDataSetSetterNotify(LimitedIndexedTreeDataSet.class);
        testDataSetSetterNotify(MultiDimDoubleDataSet.class);
    }

    void testDataSetSetterNotify(Class<? extends DataSet> testDataSetClass) throws AssertionError {
        try {
            final DataSet testDataSet = testDataSetClass.getDeclaredConstructor(String.class).newInstance("test - " + testDataSetClass.getName());
            final AtomicInteger counter = new AtomicInteger();
            testDataSet.addListener(evt -> counter.getAndIncrement());
            testDataSet.set(new DoubleErrorDataSet("null dummy"));
            assertEquals(1, counter.get());
            testDataSet.set(new DoubleErrorDataSet("null dummy"), true);
            assertEquals(2, counter.get());
            testDataSet.set(new DoubleErrorDataSet("null dummy"), false);
            assertEquals(3, counter.get());
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | InstantiationException e) {
            throw new AssertionError("caught exception for " + testDataSetClass.getName(), e);
        }
    }
}
