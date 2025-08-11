package ubic.gemma.core.analysis.singleCell.batcheffect;

import org.rosuda.REngine.REXP;
import org.rosuda.REngine.REXPNull;
import org.springframework.util.Assert;
import ubic.gemma.core.datastructure.matrix.SingleCellDesignMatrix;
import ubic.gemma.core.datastructure.matrix.SingleCellExpressionDataMatrix;
import ubic.gemma.core.util.r.RClient;
import ubic.gemma.core.util.r.REngineFactory;

/**
 * Perform batch correction using the <a href="">Harmony algorithm</a>.
 * <p>
 * Requirements: an R engine with the Harmony R package installed.
 * @author poirigui
 */
class Harmony implements BatchCorrection {

    private final REngineFactory rEngineFactory;

    Harmony( REngineFactory rEngineFactory ) {
        this.rEngineFactory = rEngineFactory;
    }

    @Override
    public SingleCellExpressionDataMatrix<?> perform( SingleCellExpressionDataMatrix<?> dataMatrix, SingleCellDesignMatrix singleCellDesignMatrix ) {
        Assert.isTrue( dataMatrix.getBioAssays().equals( singleCellDesignMatrix.getBioAssays() ),
                "Assays in the data matrix must match exactly those of the design matrix." );
        try ( RClient rEngine = new RClient( rEngineFactory ) ) {
            // TODO: serialize both matrices to disk and call Harmony R package
            rEngine.parseAndEval( "library(harmony);" );
            // rEngine.assignDataFrame( "dataMatrix", toDataFrame( dataMatrix ) );
            // rEngine.assignDataFrame( "designMatrix", toDataFrame( singleCellDesignMatrix ) );
            //language=R
            return fromDataFrame( rEngine.parseAndEval( "harmony::HarmonyMatrix(dataMatrix, designMatrix);" ) );
        }
    }

    private REXP toDataFrame( SingleCellExpressionDataMatrix<?> dataMatrix ) {
        // Convert the SingleCellExpressionDataMatrix to an REXP object
        return new REXPNull();
    }

    private REXP toDataFrame( SingleCellDesignMatrix singleCellDesignMatrix ) {
        return new REXPNull();
    }

    private SingleCellExpressionDataMatrix<?> fromDataFrame( REXP rexp ) {
        return null;
    }
}
