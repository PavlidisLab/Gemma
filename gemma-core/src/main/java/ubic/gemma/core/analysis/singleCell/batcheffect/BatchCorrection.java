package ubic.gemma.core.analysis.singleCell.batcheffect;

import ubic.gemma.core.datastructure.matrix.SingleCellDesignMatrix;
import ubic.gemma.core.datastructure.matrix.SingleCellExpressionDataMatrix;

interface BatchCorrection {

    /**
     * Perform a batch correction on the provided data matrix using the specified design matrix.
     */
    SingleCellExpressionDataMatrix<?> perform( SingleCellExpressionDataMatrix<?> dataMatrix, SingleCellDesignMatrix singleCellDesignMatrix );
}
