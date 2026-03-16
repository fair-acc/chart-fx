package io.fair_acc.chartfx.axes.spi.format;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import io.fair_acc.dataset.spi.fastutil.DoubleArrayList;

class DefaultFormatterTest {

    @Test
    void formatAndParsingWorks() {
        DefaultFormatter formatter = new DefaultFormatter();
        formatter.updateFormatter(DoubleArrayList.wrap(new double[]{1e-5, 10}), 1.);
        
        final double value = 0.01;
        String formatted = formatter.toString(value);
        assertEquals("1E-2", formatted);
        
        Number parsed = formatter.fromString(formatted);
        assertEquals(value, parsed.doubleValue());
    }
    
    @Test
    void formatAndParsingWorksWithExponentialSeparator() {
        DefaultFormatter formatter = new DefaultFormatter() {
            {
                formatter.setExponentialSeparator('\u202F');
            }
        };
        formatter.updateFormatter(DoubleArrayList.wrap(new double[]{1e-5, 10}), 1.);
        
        final double value = 0.01;
        String formatted = formatter.toString(value);
        assertEquals("1\u202FE-2", formatted);
        
        Number parsed = formatter.fromString(formatted);
        assertEquals(value, parsed.doubleValue());
    }
    
}
