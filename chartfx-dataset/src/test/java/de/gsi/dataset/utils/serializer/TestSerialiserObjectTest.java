package de.gsi.dataset.utils.serializer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.gsi.dataset.utils.serializer.helper.MyGenericClass;

/**
 * Simple tests to verify that the equals and hashCode functions of
 * 'MyGenericClass' work as expected
 * 
 * @author rstein
 */
public class TestSerialiserObjectTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(TestSerialiserObjectTest.class);

    @Test
    public void testIdentityGenericObject() {
        final MyGenericClass rootObject1 = new MyGenericClass();
        final MyGenericClass rootObject2 = new MyGenericClass();
        MyGenericClass.setVerboseChecks(false);

        assertEquals(rootObject1, rootObject2);

        rootObject1.modifyValues();
        assertNotEquals(rootObject1, rootObject2);
        rootObject2.modifyValues();
        assertEquals(rootObject1, rootObject2);

        rootObject1.boxedPrimitives.modifyValues();
        assertNotEquals(rootObject1, rootObject2);
        rootObject2.boxedPrimitives.modifyValues();
        assertEquals(rootObject1, rootObject2);

        rootObject1.arrays.modifyValues();
        assertNotEquals(rootObject1, rootObject2);
        rootObject2.arrays.modifyValues();
        assertEquals(rootObject1, rootObject2);

        rootObject1.objArrays.modifyValues();
        assertNotEquals(rootObject1, rootObject2);
        rootObject2.objArrays.modifyValues();
        assertEquals(rootObject1, rootObject2);
        LOGGER.atInfo().addArgument(this.getClass().getSimpleName())
                .log("{} - testIdentityGenericObject() - completed successfully");
    }

}
