package ubic.gemma.core.loader.expression.singleCell;

import ubic.gemma.core.loader.expression.DataLoader;
import ubic.gemma.core.loader.expression.sequencing.SequencingDataLoader;
import ubic.gemma.core.loader.expression.sequencing.SequencingMetadata;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.CellLevelCharacteristics;
import ubic.gemma.model.expression.bioAssayData.CellTypeAssignment;
import ubic.gemma.model.expression.bioAssayData.SingleCellDimension;
import ubic.gemma.model.expression.bioAssayData.SingleCellExpressionDataVector;
import ubic.gemma.model.expression.designElement.CompositeSequence;

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
public interface SingleCellDataLoader extends DataLoader, SequencingDataLoader {

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
     * Load single-cell type assignments present in the data.
     */
    Set<CellTypeAssignment> getCellTypeAssignments( SingleCellDimension dimension ) throws IOException;

    /**
     * Load cell-level characteristics that are not cell type assignments present in the data.
     */
    Set<CellLevelCharacteristics> getOtherCellLevelCharacteristics( SingleCellDimension dimension ) throws IOException;

    Map<BioAssay, SequencingMetadata> getSequencingMetadata( SingleCellDimension dimension ) throws IOException;

    /**
     * Produces a stream of single-cell expression data vectors for the given {@link QuantitationType}.
     *
     * @param designElements   a collection of design elements for mapping of element names used in the dataset to {@link CompositeSequence}
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
    Stream<SingleCellExpressionDataVector> loadVectors( Collection<CompositeSequence> designElements, SingleCellDimension dimension, QuantitationType quantitationType ) throws IOException, IllegalArgumentException;
}
