package de.gsi.chart.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayDeque;
import java.util.Arrays;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Based on javafx.base/src/test/java/test/javafx/collections/ObservableListTest.java
 * 
 * @author Alexander Krimm
 */
class ObservableDequeTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(ObservableDequeTest.class);

    ObservableDeque<Integer> deque;
    MockListObserver<Integer> mlo;

    @BeforeEach
    public void setUp() throws Exception {
        deque = new ObservableDeque<>(new ArrayDeque<>());
        mlo = new MockListObserver<>();
        deque.addListener(mlo);
        deque.add(Integer.valueOf(1));
        deque.addAll(Integer.valueOf(1), Integer.valueOf(3));
        mlo.clear();
    }

    @Test
    void setupCorrect() {
        assertEquals(deque.size(), 3);
        assertEquals(deque.get(2), 3);
        assertEquals(deque.get(0), 1);
    }

    @Test
    void testClear() {
        assertEquals(deque.size(), 3);
        deque.clear();
        mlo.check1AddRemove(deque, Arrays.asList(Integer.valueOf(1), Integer.valueOf(1), Integer.valueOf(3)), 0, 0);
        assertEquals(deque.size(), 0);
    }
}
