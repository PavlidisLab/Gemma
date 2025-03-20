package ubic.gemma.core.loader.expression.sequencing;

import lombok.Builder;
import lombok.Data;

import javax.annotation.Nullable;

/**
 * Represents sequencing metadata for a {@link ubic.gemma.model.expression.bioAssay.BioAssay}.
 * @author poirigui
 */
@Data
@Builder
public class SequencingMetadata {
    /**
     * The length of the reads.
     * <p>
     * If reads are paired, this is the sum of the length of each mate, not the fragment length.
     * <p>
     * This is {@code null} if unknown.
     */
    @Nullable
    Integer readLength;
    /**
     * The number of reads in the library.
     * <p>
     * This is {@code null} if unknown.
     */
    @Nullable
    Long readCount;
    /**
     * Whether the reads are paired.
     * <p>
     * This is {@code null} if unknown.
     */
    @Nullable
    Boolean isPaired;
}
