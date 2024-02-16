package ubic.gemma.core.loader.expression.singleCell;

import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.CellTypeLabelling;
import ubic.gemma.model.expression.bioAssayData.SingleCellDimension;
import ubic.gemma.model.expression.bioAssayData.SingleCellExpressionDataVector;

import java.io.IOException;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Common interface for all single cell data loaders.
 *
 * @author poirigui
 */
public interface SingleCellDataLoader {

    /**
     * Load the single-cell dimension present in the data.
     * <p>
     * Not all samples might be present and thus the returned {@link SingleCellDimension} will have a expression data
     * for a subset of the data.
     *
     * @param bioAssays a set of bioassays to use when populating the dimension, not all bioassays may be used
     * @throws IllegalArgumentException if a sample present in the data cannot be matched to one of the supplied
     *                                  {@link BioAssay}
     */
    SingleCellDimension getSingleCellDimension( Collection<BioAssay> bioAssays ) throws IOException;

    /**
     * Load quantitation types present in the data.
     */
    Set<QuantitationType> getQuantitationTypes() throws IOException;

    /**
     * Load single-cell type labelling present in the data.
     */
    Optional<CellTypeLabelling> getCellTypeLabelling() throws IOException;

    /**
     * Produces a stream of single-cell expression data vectors for the given {@link QuantitationType}.
     * <p>
     * Make sure to close the stream when done, preferably using a try-with-resource block.
     *
     * @param platform         a platform to use when mapping vectors to probes/genes
     * @param dimension        a dimension to use for creating vectors, may be loaded from the single-cell data with
     *                         {@link #getSingleCellDimension(Collection)}
     * @param quantitationType a quantitation type to extract from the data for, may be loaded from the single-cell data
     *                         with {@link #getQuantitationTypes()}
     */
    Stream<SingleCellExpressionDataVector> loadVectors( ArrayDesign platform, SingleCellDimension dimension, QuantitationType quantitationType ) throws IOException;
}
