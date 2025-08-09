package ubic.gemma.core.datastructure.matrix;

import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.experiment.FactorValue;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Interface representing a design matrix for bulk data.
 * @author poirigui
 */
public interface BulkDesignMatrix extends DesignMatrix {

    /**
     * Get the row of factor values corresponding to a specific bioassay.
     * @return the row, or null if the bioassay is not present in the matrix.
     */
    @Nullable
    List<FactorValue> getRow( BioAssay bioAssay );

    @Nullable
    List<FactorValue> getRow( BioMaterial bioMaterial );

    int getRowIndex( BioAssay bioAssay );

    int getRowIndex( BioMaterial bioMaterial );
}
