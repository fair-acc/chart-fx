package de.gsi.dataset.serializer.spi;

public class ProtocolInfo extends WireDataFieldDescription {
    private final WireDataFieldDescription fieldHeader;
    private final String producerName;
    private final byte versionMajor;
    private final byte versionMinor;
    private final byte versionMicro;

    ProtocolInfo(final WireDataFieldDescription fieldDescription, final String producer, final byte major, final byte minor, final byte micro) {
        super(null, fieldDescription.hashCode(), fieldDescription.getFieldName(), fieldDescription.getDataType(), fieldDescription.getFieldStart(), fieldDescription.getDataStartOffset(), fieldDescription.getDataSize());
        this.fieldHeader = fieldDescription;
        producerName = producer;
        versionMajor = major;
        versionMinor = minor;
        versionMicro = micro;
    }

    @Override
    public boolean equals(final Object obj) {
        if (!(obj instanceof ProtocolInfo)) {
            return false;
        }
        final ProtocolInfo other = (ProtocolInfo) obj;
        return other.isCompatible();
    }

    public WireDataFieldDescription getFieldHeader() {
        return fieldHeader;
    }

    public String getProducerName() {
        return producerName;
    }

    public byte getVersionMajor() {
        return versionMajor;
    }

    public byte getVersionMicro() {
        return versionMicro;
    }

    public byte getVersionMinor() {
        return versionMinor;
    }

    @Override
    public int hashCode() {
        return producerName.hashCode();
    }

    public boolean isCompatible() {
        // N.B. no API changes within the same 'major.minor'- version
        // micro.version tracks possible benin additions & internal bug-fixes
        return getVersionMajor() <= BinarySerialiser.VERSION_MAJOR && getVersionMinor() <= BinarySerialiser.VERSION_MINOR;
    }

    @Override
    public String toString() {
        return super.toString() + String.format(" serialiser: %s-v%d.%d.%d", getProducerName(), getVersionMajor(), getVersionMinor(), getVersionMicro());
    }
}
