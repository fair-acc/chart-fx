package de.gsi.dataset.spi.financial.api.attrs;

public abstract class TypeKey<T> implements Comparable<TypeKey<T>> {
    private final Class<T> type;
    private final String name;

    protected TypeKey(Class<T> type, String name) {
        this.type = type;
        this.name = name;
    }

    public Class<T> getType() {
        return type;
    }
    public String getName() {
        return name;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof TypeKey<?>) ) {
            return false;
        }
        TypeKey<?> other = (TypeKey<?>) obj;
        /* equality is defined only by name */
        return getName().equals(other.getName());
    }

    @Override
    public int hashCode() {
        int _hashCode = 17;
        _hashCode += getName().hashCode();
        return _hashCode;
    }

    @Override
    public String toString() {
        return getName();
    }

    @Override
    public int compareTo(TypeKey<T> typeKey) {
        return this.toString().compareTo(typeKey.toString());
    }
}
