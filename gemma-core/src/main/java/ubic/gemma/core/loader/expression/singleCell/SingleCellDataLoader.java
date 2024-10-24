package ubic.gemma.core.loader.expression.singleCell;

import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.CellLevelCharacteristics;
import ubic.gemma.model.expression.bioAssayData.CellTypeAssignment;
import ubic.gemma.model.expression.bioAssayData.SingleCellDimension;
import ubic.gemma.model.expression.bioAssayData.SingleCellExpressionDataVector;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.FactorValue;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Common interface for all single cell data loaders.
 *
 * @author poirigui
 */
public interface SingleCellDataLoader {

    /**
     * Set the strategy used for comparing {@link BioAssay} to sample names from the data.
     */
    void setBioAssayToSampleNameMatcher( BioAssayToSampleNameMatcher sampleNameComparator );

    /**
     * Ignore unmatched samples from the data when creating the {@link SingleCellDimension} in {@link #getSingleCellDimension(Collection)}.
     * <p>
     * This defaults to true.
     */
    void setIgnoreUnmatchedSamples( boolean ignoreUnmatchedSamples );

    /**
     * Ignore unmatched design elements from the data when creating vectors in {@link #loadVectors(Map, SingleCellDimension, QuantitationType)}.
     * <p>
     * This defaults to true.
     * <p>
     * There's a <a href="https://github.com/PavlidisLab/Gemma/issues/973">discussions to make this default in false</a>
     * in general for sequencing data.
     */
    void setIgnoreUnmatchedDesignElements( boolean ignoreUnmatchedDesignElements );

    /**
     * Obtain the sample names present in the data.
     */
    Set<String> getSampleNames() throws IOException;

    /**
     * Load the single-cell dimension present in the data.
     * <p>
     * Not all samples might be present and thus the returned {@link SingleCellDimension} will have a expression data
     * for a subset of the data.
     *
     * @param bioAssays a set of bioassays to use when populating the dimension, not all bioassays may be used
     * @throws IllegalArgumentException if a sample present in the data cannot be matched to one of the supplied
     *                                  {@link BioAssay}, ignored if {@link #setIgnoreUnmatchedSamples(boolean)} is set
     *                                  to true.
     */
    SingleCellDimension getSingleCellDimension( Collection<BioAssay> bioAssays ) throws IOException, IllegalArgumentException;

    /**
     * Load quantitation types present in the data.
     */
    Set<QuantitationType> getQuantitationTypes() throws IOException;

    /**
     * Load single-cell type assignments present in the data.
     */
    Set<CellTypeAssignment> getCellTypeAssignments( SingleCellDimension dimension ) throws IOException;

    /**
     * Load cell-level characteristics that are not cell type assignments present in the data.
     */
    Set<CellLevelCharacteristics> getOtherCellLevelCharacteristics( SingleCellDimension dimension ) throws IOException;

    /**
     * Load experimental factors present in the data.
     * @param samples                samples to use when determining which factors to load
     * @param factorValueAssignments if non-null, the proposed assignment of factor values to samples are populated in
     *                               the mapping.
     * @return a set of factors present in the data
     */
    Set<ExperimentalFactor> getFactors( Collection<BioAssay> samples, @Nullable Map<BioMaterial, Set<FactorValue>> factorValueAssignments ) throws IOException;

    /**
     * Load samples characteristics present in the data.
     * @param samples to use when determining which characteristics to load
     * @return proposed characteristics grouped by sample
     */
    Map<BioMaterial, Set<Characteristic>> getSamplesCharacteristics( Collection<BioAssay> samples ) throws IOException;

    /**
     * Load gene identifiers present in the data.
     */
    Set<String> getGenes() throws IOException;

    /**
     * Produces a stream of single-cell expression data vectors for the given {@link QuantitationType}.
     *
     * @param elementsMapping  a mapping of element names used in the dataset to {@link CompositeSequence}
     * @param dimension        a dimension to use for creating vectors, may be loaded from the single-cell data with
     *                         {@link #getSingleCellDimension(Collection)}
     * @param quantitationType a quantitation type to extract from the data for, may be loaded from the single-cell data
     *                         with {@link #getQuantitationTypes()}
     * @throws IllegalArgumentException if a design element present in the data cannot be matched to one of the supplied
     *                                  elements, requires setting {@link #setIgnoreUnmatchedDesignElements(boolean)} to
     *                                  false
     * @return a stream of single-cell expression data vectors that must be closed when done, preferably using a
     * try-with-resource block.
     */
    Stream<SingleCellExpressionDataVector> loadVectors( Map<String, CompositeSequence> elementsMapping, SingleCellDimension dimension, QuantitationType quantitationType ) throws IOException, IllegalArgumentException;
}
