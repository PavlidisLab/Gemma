package ubic.gemma.core.datastructure.matrix;

import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.CellLevelCharacteristics;
import ubic.gemma.model.expression.bioAssayData.SingleCellDimension;
import ubic.gemma.model.expression.experiment.ExperimentalDesign;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.FactorValue;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author poirigui
 */
public interface SingleCellDesignMatrix extends DesignMatrix {

    static SingleCellDesignMatrix from( SingleCellDimension dimension, ExperimentalDesign experimentalDesign, Collection<CellLevelCharacteristics> clcs ) {
        List<ExperimentalFactor> factors = experimentalDesign.getExperimentalFactors().stream()
                .sorted( ExperimentalFactor.COMPARATOR )
                .collect( Collectors.toList() );
        List<CellLevelCharacteristics> clcsSorted = clcs.stream()
                .sorted( CellLevelCharacteristics.COMPARATOR )
                .collect( Collectors.toList() );
        return new SingleCellDesignMatrixImpl( dimension, dimension.getBioAssays(), factors, clcsSorted );
    }

    /**
     * Obtain the list of cell IDs in the design matrix.
     * <p>
     * The list is not necessarily unique and has to be combined with {@link #getBioAssays()} to form a unique
     * identifier.
     */
    List<String> getCellIds();

    @Nullable
    List<FactorValue> getRow( BioAssay bioAssay, String cellId );

    /**
     * @throws IndexOutOfBoundsException if the row index is out of bounds.
     */
    String getCellIdForRow( int row );

    int getRowIndex( BioAssay bioAssay, String cellId );
}
