package ubic.gemma.core.analysis.singleCell.batcheffect;

import org.springframework.util.Assert;
import ubic.gemma.core.datastructure.matrix.SingleCellDesignMatrix;
import ubic.gemma.core.datastructure.matrix.SingleCellExpressionDataMatrix;

class Harmony implements BatchCorrection {

    @Override
    public SingleCellExpressionDataMatrix<?> perform( SingleCellExpressionDataMatrix<?> dataMatrix, SingleCellDesignMatrix singleCellDesignMatrix ) {
        Assert.isTrue( dataMatrix.getBioAssays().equals( singleCellDesignMatrix.getBioAssays() ),
                "Assays in the data matrix must match exactly those of the design matrix." );
        // TODO: serialize both matrices to disk and call Harmony R package
        return dataMatrix;
    }
}
