package ubic.gemma.core.util.runtime;

/**
 * Information about system memory.
 * <p>
 * Implemented as a Java record (formerly a Lombok {@code @Value} class). {@link #getAvailableMemory()}
 * is preserved as a {@code get}-prefixed alias for callers that use the JavaBean accessor convention.
 *
 * @author poirigui
 */
public record MemInfo(long availableMemory) {

    public long getAvailableMemory() {
        return availableMemory;
    }
}
