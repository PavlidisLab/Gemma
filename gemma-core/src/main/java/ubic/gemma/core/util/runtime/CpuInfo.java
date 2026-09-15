package ubic.gemma.core.util.runtime;

/**
 * Information about a CPU.
 * <p>
 * Implemented as a Java record (formerly a Lombok {@code @Value} class). {@link #getFlags()} is
 * preserved as a {@code get}-prefixed alias for callers that use the JavaBean accessor convention.
 * Equality on the {@code flags} array uses reference equality (the record contract), matching the
 * prior Lombok behaviour for arrays.
 */
public record CpuInfo(String[] flags) {

    public String[] getFlags() {
        return flags;
    }
}
