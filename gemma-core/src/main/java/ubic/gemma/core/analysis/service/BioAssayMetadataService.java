package ubic.gemma.core.analysis.service;

import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Set;

/**
 * Curator edits to the upstream-derived metadata on a sample: {@code libraryStrategy},
 * {@code librarySelection} and {@code extractedMolecule}.
 *
 * <h2>Why this exists</h2>
 * {@code GeoConverterImpl} is the only other writer of these three columns, and it writes them once, at
 * import, from what GEO declared. GEO under-declares: ribosome profiling is routinely submitted as
 * {@code OTHER}, and {@code GeoConverterImpl.effectiveLibStrategy} only rescues the cases whose titles
 * say so unambiguously. Everything it deliberately leaves alone — TRAP, polysome purification, the arms
 * where GEO declared {@code RNA-Seq} outright — had no route to the column at all, so the alternative was
 * direct SQL, which emits no audit event and validates nothing.
 *
 * <h2>Validation</h2>
 * {@code libraryStrategy} and {@code extractedMolecule} are checked against closed vocabularies.
 * 🛑 {@code librarySelection} is NOT, and deliberately so: {@code GeoSample.librarySelection} is kept as
 * the submitter's raw string ({@code cDNA}, {@code PCR}, {@code other}) precisely so that normalizing to
 * a closed set cannot drop values the enum does not know. It is trimmed and length-checked only.
 */
public interface BioAssayMetadataService {

    /** What {@link #setLibraryStrategy} will accept, for the 400 message and for callers that want to offer a menu. */
    Set<String> getValidLibraryStrategies();

    /**
     * Set {@code libraryStrategy} on the given samples.
     *
     * @param ee        the experiment the samples belong to; every assay must be one of its samples
     * @param bioAssays the samples to change
     * @param value     a {@code GeoLibraryStrategy} constant name, {@code MICROARRAY_ONE_COLOR} or
     *                  {@code MICROARRAY_TWO_COLOR}, or {@code null} to clear the field
     * @return the samples actually changed; empty when every sample already held {@code value}
     * @throws IllegalArgumentException if {@code value} is not a known strategy
     */
    Collection<BioAssay> setLibraryStrategy( ExpressionExperiment ee, Collection<BioAssay> bioAssays, @Nullable String value );

    /**
     * Set {@code librarySelection} on the given samples. Free text by design — see the class javadoc.
     *
     * @param value trimmed; {@code null} or blank clears the field
     * @return the samples actually changed
     * @throws IllegalArgumentException if {@code value} exceeds the column length
     */
    Collection<BioAssay> setLibrarySelection( ExpressionExperiment ee, Collection<BioAssay> bioAssays, @Nullable String value );

    /**
     * Set {@code extractedMolecule} on the given samples.
     *
     * @param value an {@link ubic.gemma.model.expression.bioAssay.ExtractedMolecule} constant name, or
     *              {@code null} to clear the field
     * @return the samples actually changed
     * @throws IllegalArgumentException if {@code value} is not a known molecule
     */
    Collection<BioAssay> setExtractedMolecule( ExpressionExperiment ee, Collection<BioAssay> bioAssays, @Nullable String value );
}
