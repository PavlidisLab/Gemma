package ubic.gemma.core.datastructure.matrix;

import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.biomaterial.BioMaterial;

import java.util.Collection;

/**
 * Design matrix for multi-assay bulk expression data.
 * <p>
 * A multi-assay bulk design matrix may have more than one {@link BioAssay} per {@link BioMaterial}.
 * @author poirigui
 * @see MultiAssayBulkExpressionDataMatrix
 */
public interface MultiAssayBulkDesignMatrix extends BulkDesignMatrix {

    /**
     * Obtain all the bioassays for a given row in the design matrix.
     */
    Collection<BioAssay> getBioAssaysForRow( int row );
}
