package ubic.gemma.persistence.service.expression.bioAssayData;

import org.springframework.lang.Nullable;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.designElement.CompositeSequence;

import java.util.Map;

/**
 * The processing pipeline's state between the read and the write, carried across a stretch with no transaction
 * open.
 *
 * <h2>Why it exists as a type</h2>
 *
 * <p>Quantile normalization takes 44 minutes on a large experiment (GSE260875, 34,330 x 1,090, measured
 * 2026-09-17) and touches no entity: it reads {@link #getData()} and {@link #getReferenceColumns()}, both plain
 * arrays. Everything before it is reads and everything after it is writes, so the three belong in separate
 * transactions — and something has to carry the middle. This is that thing.</p>
 *
 * <p>🛑 <b>Its entity fields are DETACHED</b> once the read transaction commits. {@link #getDimension()} and the
 * {@link CompositeSequence} keys of {@link #getData()} are real rows that nothing is tracking any more, and
 * {@link #getProcessedQt()} is transient until the write step creates it. Read what is already initialized;
 * navigate nothing new. The write step re-associates them.</p>
 *
 * @author gemma
 */
class ComputedProcessedData {

    private final Map<CompositeSequence, double[]> data;
    private final BioAssayDimension dimension;
    private final QuantitationType processedQt;
    private final Map<CompositeSequence, int[]> numberOfCells;
    @Nullable
    private final boolean[] referenceColumns;

    ComputedProcessedData( Map<CompositeSequence, double[]> data, BioAssayDimension dimension,
            QuantitationType processedQt, Map<CompositeSequence, int[]> numberOfCells,
            @Nullable boolean[] referenceColumns ) {
        this.data = data;
        this.dimension = dimension;
        this.processedQt = processedQt;
        this.numberOfCells = numberOfCells;
        this.referenceColumns = referenceColumns;
    }

    Map<CompositeSequence, double[]> getData() {
        return data;
    }

    BioAssayDimension getDimension() {
        return dimension;
    }

    QuantitationType getProcessedQt() {
        return processedQt;
    }

    Map<CompositeSequence, int[]> getNumberOfCells() {
        return numberOfCells;
    }

    /**
     * Which columns define the quantile reference distribution, or {@code null} when every column does.
     * <p>
     * Read in the read step, because it comes from {@code BioAssay.getIsOutlier()} on the dimension's assays and
     * that is a navigation the normalization step must not have to make.
     */
    @Nullable
    boolean[] getReferenceColumns() {
        return referenceColumns;
    }
}
