package io.fair_acc.dataset.event;

/**
 * 
 * @author rstein
 *
 */
public interface EventListener {

    /**
     * This method needs to be provided by an implementation of {@code UpdateListener}. It is called if an
     * {@link EventSource} has been modified/updated.
     * <p>
     * In general is is considered bad practice to modify the observed value in this method.
     *
     * @param event The {@code UpdateEvent} issued by the modified {@code UpdateSource}
     */
    void handle(UpdateEvent event);
}
