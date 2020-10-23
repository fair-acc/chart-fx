package de.gsi.serializer.spi;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import de.gsi.serializer.FieldDescription;

public class CmwLightSerialiserTests {
    @Test
    public void testCmwData() {
        final CmwLightSerialiser serialiser = new CmwLightSerialiser(FastByteBuffer.wrap(new byte[] {
                7, 0, 0, 0, 2, 0, 0, 0, 48, 0, 4, 1, 0, 0, 0, 0, 0, 0, 0, 2, 0, 0, 0, 49, 0, 7, 1, 0, 0, 0, 0, 2, 0, 0,
                0, 50, 0, 1, 5, 2, 0, 0, 0, 51, 0, 8, 1, 0, 0, 0, 2, 0, 0, 0, 98, 0, 4, 114, 0, 0, 0, 0, 0, 0, 0, 2, 0,
                0, 0, 55, 0, 1, 0, 2, 0, 0, 0, 100, 0, 7, 1, 0, 0, 0, 0, 2, 0, 0, 0, 102, 0, 7, 1, 0, 0, 0, 0 }));
        final FieldDescription fieldDescription = serialiser.parseIoStream(true).getChildren().get(0);
        // fieldDescription.printFieldStructure();
        assertEquals(1L, ((WireDataFieldDescription) fieldDescription.findChildField("0")).data());
        assertEquals("", ((WireDataFieldDescription) fieldDescription.findChildField("1")).data());
        assertEquals((byte) 5, ((WireDataFieldDescription) fieldDescription.findChildField("2")).data());
        assertEquals(114L, ((WireDataFieldDescription) fieldDescription.findChildField("3").findChildField("b")).data());
        assertEquals((byte) 0, ((WireDataFieldDescription) fieldDescription.findChildField("7")).data());
        assertEquals("", ((WireDataFieldDescription) fieldDescription.findChildField("d")).data());
        assertEquals("", ((WireDataFieldDescription) fieldDescription.findChildField("f")).data());
    }
}
