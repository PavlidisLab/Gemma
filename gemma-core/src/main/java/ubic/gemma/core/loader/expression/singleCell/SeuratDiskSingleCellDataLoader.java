package ubic.gemma.core.loader.expression.singleCell;

import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.CellTypeAssignment;
import ubic.gemma.model.expression.bioAssayData.SingleCellDimension;
import ubic.gemma.model.expression.bioAssayData.SingleCellExpressionDataVector;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;

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
    public SingleCellDimension getSingleCellDimension( Collection<BioAssay> bioAssays ) {
        return null;
    }

    @Override
    public Set<QuantitationType> getQuantitationTypes() throws IOException {
        return null;
    }

    @Nullable
    @Override
    public Optional<CellTypeAssignment> getCellTypeAssignment() throws IOException {
        return Optional.empty();
    }

    @Override
    public Set<ExperimentalFactor> getFactors() throws IOException {
        return null;
    }

    @Override
    public Stream<SingleCellExpressionDataVector> loadVectors( Map<String, CompositeSequence> elementsMapping, SingleCellDimension dimension, QuantitationType quantitationType ) throws IOException {
        return null;
    }
}
