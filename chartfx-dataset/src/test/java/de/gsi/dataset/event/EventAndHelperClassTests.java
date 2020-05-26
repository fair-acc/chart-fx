package de.gsi.dataset.event;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public class EventAndHelperClassTests {
    private final static Object payload = new Object();
    private static final EventSource testEventSource = new EventSource() {
        @Override
        public AtomicBoolean autoNotification() {
            return null;
        }

        @Override
        public List<EventListener> updateEventListener() {
            return null;
        }
    };

    @Test
    public void testEventSource() {
        assertNull(testEventSource.autoNotification());
        assertNull(testEventSource.updateEventListener());
    }

    @DisplayName("UpdateEvent class constructors")
    @ParameterizedTest(name = "event class - {0}")
    @ValueSource(classes = { AddedDataEvent.class, AddedDataEvent.class, InvalidatedEvent.class, RemovedDataEvent.class, //
                         UpdatedDataEvent.class, UpdatedMetaDataEvent.class, UpdateEvent.class })
    public void
    testConstructors(final Class<? extends UpdateEvent> eventClass) //
            throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException {
        Constructor<? extends UpdateEvent> constructor1 = eventClass.getConstructor(EventSource.class);
        assertNotNull(constructor1);
        assertEquals(1, constructor1.getParameterCount());
        UpdateEvent instance1 = constructor1.newInstance(testEventSource);
        assertEquals(testEventSource, instance1.getSource(), "event source equality");
        assertEquals(null, instance1.getMessage(), "event message equality");
        assertEquals(null, instance1.getPayLoad(), "payload equality");

        Constructor<? extends UpdateEvent> constructor2 = eventClass.getConstructor(EventSource.class, String.class);
        assertNotNull(constructor2);
        assertEquals(2, constructor2.getParameterCount());
        UpdateEvent instance2 = constructor2.newInstance(testEventSource, "test2");
        assertEquals(testEventSource, instance2.getSource(), "event source equality");
        assertEquals("test2", instance2.getMessage(), "event message equality");
        assertEquals(null, instance2.getPayLoad(), "payload equality");

        Constructor<? extends UpdateEvent> constructor3 = eventClass.getConstructor(EventSource.class, String.class, Object.class);
        assertNotNull(constructor3);
        assertEquals(3, constructor3.getParameterCount());
        UpdateEvent instance3 = constructor3.newInstance(testEventSource, "test3", payload);
        assertEquals(testEventSource, instance3.getSource(), "event source equality");
        assertEquals("test3", instance3.getMessage(), "event message equality");
        assertEquals(payload, instance3.getPayLoad(), "payload equality");
    }

    @DisplayName("UpdateAxisEvent class constructors")
    @ParameterizedTest(name = "event class - {0}")
    @ValueSource(classes = { AxisChangeEvent.class, AxisNameChangeEvent.class, AxisRangeChangeEvent.class })
    public void testAxisConstructors(final Class<? extends AxisChangeEvent> eventClass) //
            throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException {
        Constructor<? extends AxisChangeEvent> constructor0 = eventClass.getConstructor(EventSource.class);
        assertNotNull(constructor0);
        assertEquals(1, constructor0.getParameterCount());
        AxisChangeEvent instance0 = constructor0.newInstance(testEventSource);
        assertEquals(testEventSource, instance0.getSource(), "event source equality");
        assertEquals(-1, instance0.getDimension(), "event message equality");
        assertEquals(null, instance0.getMessage(), "event message equality");
        assertEquals(null, instance0.getPayLoad(), "payload equality");

        Constructor<? extends AxisChangeEvent> constructor1 = eventClass.getConstructor(EventSource.class, int.class);
        assertNotNull(constructor1);
        assertEquals(2, constructor1.getParameterCount());
        AxisChangeEvent instance1 = constructor1.newInstance(testEventSource, 5);
        assertEquals(testEventSource, instance1.getSource(), "event source equality");
        assertEquals(5, instance1.getDimension(), "axis id identity");
        assertEquals(null, instance1.getMessage(), "event message equality");
        assertEquals(null, instance1.getPayLoad(), "payload equality");

        Constructor<? extends AxisChangeEvent> constructor2 = eventClass.getConstructor(EventSource.class, String.class, int.class);
        assertNotNull(constructor2);
        assertEquals(3, constructor2.getParameterCount());
        AxisChangeEvent instance2 = constructor2.newInstance(testEventSource, "test2", 5);
        assertEquals(testEventSource, instance2.getSource(), "event source equality");
        assertEquals(5, instance2.getDimension(), "axis id identity");
        assertEquals("test2", instance2.getMessage(), "event message equality");
        assertEquals(null, instance2.getPayLoad(), "payload equality");

        Constructor<? extends AxisChangeEvent> constructor3 = eventClass.getConstructor(EventSource.class, String.class, Object.class, int.class);
        assertNotNull(constructor3);
        assertEquals(4, constructor3.getParameterCount());
        AxisChangeEvent instance3 = constructor3.newInstance(testEventSource, "test3", payload, 5);
        assertEquals(testEventSource, instance3.getSource(), "event source equality");
        assertEquals(5, instance3.getDimension(), "axis id identity");
        assertEquals("test3", instance3.getMessage(), "event message equality");
        assertEquals(payload, instance3.getPayLoad(), "payload equality");
    }

    @Test
    public void testBasicEventHelperCoverage() {
        assertNotNull(EventThreadHelper.getExecutorService());

        assertEquals(Math.max(4, Runtime.getRuntime().availableProcessors()), EventThreadHelper.getMaxThreads());
    }
}
