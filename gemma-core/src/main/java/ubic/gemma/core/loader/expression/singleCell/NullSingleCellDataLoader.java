package ubic.gemma.core.loader.expression.singleCell;

import ubic.gemma.core.loader.expression.sequencing.SequencingMetadata;
import ubic.gemma.core.loader.util.mapper.BioAssayMapper;
import ubic.gemma.core.loader.util.mapper.DesignElementMapper;
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
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Single-cell data loader used when no data is available.
 * <p>
 * This is useful to use as a base loader for an {@link AbstractDelegatingSingleCellDataLoader} implementation.
 * @author poirigui
 */
public class NullSingleCellDataLoader implements SingleCellDataLoader {

    @Override
    public void setBioAssayToSampleNameMapper( BioAssayMapper bioAssayToSampleNameMatcher ) {

    }

    @Override
    public void setDesignElementToGeneMapper( DesignElementMapper designElementToGeneMapper ) {

    }

    @Override
    public void setIgnoreUnmatchedSamples( boolean ignoreUnmatchedSamples ) {

    }

    @Override
    public void setIgnoreUnmatchedDesignElements( boolean ignoreUnmatchedDesignElements ) {

    }

    @Override
    public Set<String> getSampleNames() throws IOException {
        return Collections.emptySet();
    }

    @Override
    public SingleCellDimension getSingleCellDimension( Collection<BioAssay> bioAssays ) throws IOException, IllegalArgumentException {
        throw new UnsupportedOperationException( "The null loader does not support loading single-cell dimensions." );
    }

    @Override
    public Set<QuantitationType> getQuantitationTypes() throws IOException {
        return Collections.emptySet();
    }

    @Override
    public Set<CellTypeAssignment> getCellTypeAssignments( SingleCellDimension dimension ) throws IOException {
        return Collections.emptySet();
    }

    @Override
    public Set<CellLevelCharacteristics> getOtherCellLevelCharacteristics( SingleCellDimension dimension ) throws IOException {
        return Collections.emptySet();
    }

    @Override
    public Set<ExperimentalFactor> getFactors( Collection<BioAssay> samples, @Nullable Map<BioMaterial, Set<FactorValue>> factorValueAssignments ) throws IOException {
        return Collections.emptySet();
    }

    @Override
    public Map<BioMaterial, Set<Characteristic>> getSamplesCharacteristics( Collection<BioAssay> samples ) throws IOException {
        return Collections.emptyMap();
    }

    @Override
    public Set<String> getGenes() throws IOException {
        return Collections.emptySet();
    }

    @Override
    public Map<BioAssay, SequencingMetadata> getSequencingMetadata( Collection<BioAssay> samples ) throws IOException {
        return Collections.emptyMap();
    }

    @Override
    public Map<BioAssay, SequencingMetadata> getSequencingMetadata( SingleCellDimension dimension ) throws IOException {
        return Collections.emptyMap();
    }

    @Override
    public Stream<SingleCellExpressionDataVector> loadVectors( Collection<CompositeSequence> designElements, SingleCellDimension dimension, QuantitationType quantitationType ) throws IOException, IllegalArgumentException {
        return Stream.empty();
    }

    @Override
    public void close() throws IOException {

    }
}
