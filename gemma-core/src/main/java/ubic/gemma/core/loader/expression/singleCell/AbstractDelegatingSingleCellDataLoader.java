package ubic.gemma.core.loader.expression.singleCell;

import ubic.gemma.core.loader.expression.sequencing.AbstractDelegatingSequencingDataLoader;
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

public abstract class AbstractDelegatingSingleCellDataLoader extends AbstractDelegatingSequencingDataLoader implements SingleCellDataLoader {

    private final SingleCellDataLoader delegate;

    protected AbstractDelegatingSingleCellDataLoader( SingleCellDataLoader delegate ) {
        super( delegate );
        this.delegate = delegate;
    }

    @Override
    public SingleCellDimension getSingleCellDimension( Collection<BioAssay> bioAssays ) throws IOException, IllegalArgumentException {
        return delegate.getSingleCellDimension( bioAssays );
    }

    @Override
    public Set<CellTypeAssignment> getCellTypeAssignments( SingleCellDimension dimension ) throws IOException {
        return delegate.getCellTypeAssignments( dimension );
    }

    @Override
    public Set<CellLevelCharacteristics> getOtherCellLevelCharacteristics( SingleCellDimension dimension ) throws IOException {
        return delegate.getOtherCellLevelCharacteristics( dimension );
    }

    @Override
    public Map<BioAssay, SequencingMetadata> getSequencingMetadata( SingleCellDimension dimension ) throws IOException {
        return delegate.getSequencingMetadata( dimension );
    }

    @Override
    public Stream<SingleCellExpressionDataVector> loadVectors( Collection<CompositeSequence> designElements, SingleCellDimension dimension, QuantitationType quantitationType ) throws IOException, IllegalArgumentException {
        return delegate.loadVectors( designElements, dimension, quantitationType );
    }
}
