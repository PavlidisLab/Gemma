package ubic.gemma.core.loader.expression.singleCell;

import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.bioAssay.BioAssay;
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
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

public abstract class AbstractDelegatingSingleCellDataLoader implements SingleCellDataLoader {

    private final SingleCellDataLoader delegate;

    protected AbstractDelegatingSingleCellDataLoader( SingleCellDataLoader delegate ) {
        this.delegate = delegate;
    }

    @Override
    public void setBioAssayToSampleNameMatcher( BioAssayToSampleNameMatcher sampleNameComparator ) {
        delegate.setBioAssayToSampleNameMatcher( sampleNameComparator );
    }

    @Override
    public void setIgnoreUnmatchedSamples( boolean ignoreUnmatchedSamples ) {
        delegate.setIgnoreUnmatchedSamples( ignoreUnmatchedSamples );
    }

    @Override
    public void setIgnoreUnmatchedDesignElements( boolean ignoreUnmatchedDesignElements ) {
        delegate.setIgnoreUnmatchedDesignElements( ignoreUnmatchedDesignElements );

    }

    @Override
    public Set<String> getSampleNames() throws IOException {
        return delegate.getSampleNames();
    }

    @Override
    public SingleCellDimension getSingleCellDimension( Collection<BioAssay> bioAssays ) throws IOException, IllegalArgumentException {
        return delegate.getSingleCellDimension( bioAssays );
    }

    @Override
    public Set<QuantitationType> getQuantitationTypes() throws IOException {
        return delegate.getQuantitationTypes();
    }

    @Override
    public Optional<CellTypeAssignment> getCellTypeAssignment( SingleCellDimension dimension ) throws IOException {
        return delegate.getCellTypeAssignment( dimension );
    }

    @Override
    public Set<ExperimentalFactor> getFactors( Collection<BioAssay> samples, @Nullable Map<BioMaterial, Set<FactorValue>> factorValueAssignments ) throws IOException {
        return delegate.getFactors( samples, factorValueAssignments );
    }

    @Override
    public Map<BioMaterial, Set<Characteristic>> getSamplesCharacteristics( Collection<BioAssay> samples ) throws IOException {
        return delegate.getSamplesCharacteristics( samples );
    }

    @Override
    public Set<String> getGenes() throws IOException {
        return delegate.getGenes();
    }

    @Override
    public Stream<SingleCellExpressionDataVector> loadVectors( Map<String, CompositeSequence> elementsMapping, SingleCellDimension dimension, QuantitationType quantitationType ) throws IOException, IllegalArgumentException {
        return delegate.loadVectors( elementsMapping, dimension, quantitationType );
    }
}
