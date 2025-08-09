package ubic.gemma.apps;

import org.springframework.beans.factory.annotation.Autowired;
import ubic.gemma.core.analysis.service.ExpressionDataMatrixService;
import ubic.gemma.core.datastructure.matrix.ExpressionDataDoubleMatrix;
import ubic.gemma.core.datastructure.matrix.SingleCellExpressionDataMatrix;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.bioAssayData.DataVector;
import ubic.gemma.model.expression.bioAssayData.RawExpressionDataVector;
import ubic.gemma.model.expression.bioAssayData.SingleCellExpressionDataVector;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.expression.experiment.SingleCellExpressionExperimentService;

import javax.annotation.Nullable;

import static ubic.gemma.core.analysis.preprocess.detect.QuantitationTypeDetectionUtils.inferQuantitationType;

public class DetectQuantitationTypeCli extends ExpressionExperimentVectorsManipulatingCli<DataVector> {

    @Autowired
    private ExpressionDataMatrixService expressionDataMatrixService;

    @Autowired
    public SingleCellExpressionExperimentService singleCellExpressionExperimentService;

    public DetectQuantitationTypeCli() {
        super( DataVector.class );
    }

    @Nullable
    @Override
    public String getCommandName() {
        return "detectQuantitationType";
    }

    @Nullable
    @Override
    public String getShortDesc() {
        return "Detect quantitation type from data";
    }

    @Override
    protected void processExpressionExperimentVectors( ExpressionExperiment ee, QuantitationType qt, Class<? extends DataVector> vectorType ) throws Exception {
        if ( RawExpressionDataVector.class.isAssignableFrom( vectorType ) ) {
            detectRawQuantitationType( ee, qt );
        } else if ( SingleCellExpressionDataVector.class.isAssignableFrom( vectorType ) ) {
            detectSingleCellQuantitationType( ee, qt );
        } else {
            throw new UnsupportedOperationException( "Unsupported vector type " + vectorType );
        }
    }

    private void detectRawQuantitationType( ExpressionExperiment ee, QuantitationType quantitationType ) {
        log.info( "Loading data for " + quantitationType + "..." );
        ExpressionDataDoubleMatrix matrix = expressionDataMatrixService.getRawExpressionDataMatrix( ee, quantitationType );
        log.info( "Got data!" );
        if ( matrix != null ) {
            QuantitationType qt = inferQuantitationType( matrix );
            log.info( "Detected quantitation type: " + qt );
        }
    }

    private void detectSingleCellQuantitationType( ExpressionExperiment ee, QuantitationType quantitationType ) {
        log.info( "Loading data for " + quantitationType + "..." );
        SingleCellExpressionDataMatrix<?> matrix = singleCellExpressionExperimentService.getSingleCellExpressionDataMatrix( ee, quantitationType );
        log.info( "Got data!" );
        QuantitationType qt = inferQuantitationType( matrix );
        log.info( "Detected quantitation type: " + qt );
    }
}
