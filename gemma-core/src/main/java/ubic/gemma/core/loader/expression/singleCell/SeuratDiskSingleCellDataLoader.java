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
import java.nio.file.Path;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Implementation of <a href="https://mojaveazure.github.io/seurat-disk/index.html">SeuratDisk</a>.
 *
 * @author poirigui
 */
public class SeuratDiskSingleCellDataLoader implements SingleCellDataLoader {

    private final Path path;

    public SeuratDiskSingleCellDataLoader( Path path ) {
        this.path = path;
    }

    @Override
    public void setSampleNameComparator( SampleNameComparator sampleNameComparator ) {

    }

    @Override
    public void setIgnoreUnmatchedSamples( boolean ignoreUnmatchedSamples ) {

    }

    @Override
    public void setIgnoreUnmatchedDesignElements( boolean ignoreUnmatchedDesignElements ) {

    }

    @Override
    public Set<String> getSampleNames() throws IOException {
        return null;
    }

    @Override
    public SingleCellDimension getSingleCellDimension( Collection<BioAssay> bioAssays ) {
        return null;
    }

    @Override
    public Set<QuantitationType> getQuantitationTypes() throws IOException {
        return null;
    }

    @Nullable
    @Override
    public Optional<CellTypeAssignment> getCellTypeAssignment( SingleCellDimension dimension ) {
        return Optional.empty();
    }

    @Override
    public Set<ExperimentalFactor> getFactors( Collection<BioMaterial> samples, @Nullable Map<BioMaterial, Set<FactorValue>> factorValueAssignments ) throws IOException {
        return null;
    }

    @Override
    public Map<BioMaterial, Set<Characteristic>> getSampleCharacteristics( Collection<BioMaterial> samples ) throws IOException {
        return null;
    }

    @Override
    public Set<String> getGenes() throws IOException {
        return null;
    }

    @Override
    public Stream<SingleCellExpressionDataVector> loadVectors( Map<String, CompositeSequence> elementsMapping, SingleCellDimension dimension, QuantitationType quantitationType ) throws IOException {
        return null;
    }
}
