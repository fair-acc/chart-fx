package de.gsi.chart.utils;

import java.lang.ref.Reference;

import javafx.scene.image.WritableImage;

import de.gsi.dataset.utils.CacheCollection;

/**
 * Implements WritableImage cache collection to minimise memory re-allocation.
 *  
 * <p> 
 * N.B. This is useful to re-use short-lived data storage container to minimise the amount of garbage to be collected. This is used in situation replacing e.g.
 * <pre>
 *  {@code
 *      public returnValue frequentlyExecutedFunction(...) {
 *          final WritableImage storage = new WritableImage(width, heigth); // allocate new memory block (costly)
 *          // [...] do short-lived computation on storage
 *          // storage is implicitly finalised by garbage collector (costly)
 *      }
 *  }
 * </pre>
 * with
 * <pre>
 *  {@code
 *      // ...
 *      private final WritableImageCache cache = new WritableImageCache();
 *      // ...
 *      
 *      public returnValue frequentlyExecutedFunction(...) {
 *          final byte[] storage = cache.getImage(width, height); // return previously allocated image (cheap) or allocated new if necessary 
 *          // [...] do short-lived computation on storage
 *          cache.add(storage); // return object to cache
 *      }
 *  }
 * </pre>
 *  
 * @author rstein
 *
 */
public class WritableImageCache extends CacheCollection<WritableImage> {
    private static final WritableImageCache SELF = new WritableImageCache();

    public WritableImage getImage(final int requiredWidth, final int requiredHeight) {
        synchronized (contents) {
            WritableImage bestFit = null;

            for (final Reference<WritableImage> candidate : contents) {
                final WritableImage localRef = candidate.get();
                if (localRef == null) {
                    continue;
                }
                final int localWidth = (int) localRef.getWidth();
                final int localHeight = (int) localRef.getHeight();

                if (localWidth == requiredWidth && localHeight == requiredHeight) {
                    bestFit = localRef;
                    break;
                }
            }

            if (bestFit == null) {
                // could not find any cached, return new WritableImage
                bestFit = new WritableImage(requiredWidth, requiredHeight);
                return bestFit;
            }

            remove(bestFit);
            return bestFit;
        }
    }

    public static WritableImageCache getInstance() {
        return SELF;
    }
}
