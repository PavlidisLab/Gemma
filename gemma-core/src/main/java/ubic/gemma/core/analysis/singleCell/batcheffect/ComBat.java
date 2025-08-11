package ubic.gemma.core.analysis.singleCell.batcheffect;

import ubic.gemma.core.datastructure.matrix.SingleCellDesignMatrix;
import ubic.gemma.core.datastructure.matrix.SingleCellExpressionDataMatrix;

class ComBat implements BatchCorrection {
    @Override
    public SingleCellExpressionDataMatrix<?> perform( SingleCellExpressionDataMatrix<?> dataMatrix, SingleCellDesignMatrix singleCellDesignMatrix ) {
        // TODO: reuse ComBat implementation
        return dataMatrix;
    }
}
