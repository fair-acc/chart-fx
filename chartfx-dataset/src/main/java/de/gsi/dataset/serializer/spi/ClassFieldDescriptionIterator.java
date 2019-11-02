package de.gsi.dataset.serializer.spi;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * @author rstein
 * @param <E> generics handling
 */
public class ClassFieldDescriptionIterator<E extends ClassFieldDescription> implements Iterator<ClassFieldDescription> {
    public static int maxRecursionLevel = 10;
    private final List<ClassFieldDescription> list = new LinkedList<>();
    private final Iterator<ClassFieldDescription> iterator;

    public ClassFieldDescriptionIterator(ClassFieldDescription fieldObj) {
        parse(fieldObj, 0);
        iterator = list.iterator();
    }

    @Override
    public boolean hasNext() {
        return iterator.hasNext();
    }

    @Override
    public ClassFieldDescription next() {
        return iterator.next();
    }

    private void parse(ClassFieldDescription fieldObj, int recursionLevel) {
        if (recursionLevel > maxRecursionLevel) {
            throw new IllegalStateException("recursion error while scanning object structure: recursionLevel = '"
                    + recursionLevel + "' > " + ClassFieldDescriptionIterator.class.getSimpleName()
                    + ".maxRecursionLevel ='" + ClassFieldDescriptionIterator.maxRecursionLevel + "'");
        }
        list.add(fieldObj);
        for (ClassFieldDescription child : fieldObj.getChildren()) {
            parse(child, recursionLevel + 1);
        }
    }

    @Override
    public void remove() {
        iterator.remove();
    }

}
