package ubic.gemma.apps;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import ubic.gemma.core.analysis.service.ExpressionDataMatrixService;
import ubic.gemma.core.datastructure.matrix.ExpressionDataDoubleMatrix;
import ubic.gemma.core.datastructure.matrix.SingleCellExpressionDataMatrix;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.bioAssayData.RawExpressionDataVector;
import ubic.gemma.model.expression.bioAssayData.SingleCellExpressionDataVector;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.common.quantitationtype.NonUniqueQuantitationTypeByNameException;
import ubic.gemma.persistence.service.common.quantitationtype.QuantitationTypeService;
import ubic.gemma.persistence.service.expression.experiment.SingleCellExpressionExperimentService;

import javax.annotation.Nullable;

import static ubic.gemma.core.analysis.preprocess.detect.QuantitationTypeDetectionUtils.inferQuantitationType;

public class DetectQuantitationTypeCli extends ExpressionExperimentManipulatingCLI {

    @Autowired
    public QuantitationTypeService quantitationTypeService;

    @Autowired
    private ExpressionDataMatrixService expressionDataMatrixService;

    @Autowired
    public SingleCellExpressionExperimentService singleCellExpressionExperimentService;

    @Nullable
    private String quantitationTypeName;

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
    protected void buildExperimentOptions( Options options ) {
        options.addOption( "qtName", "quantitation-type-name", true, "Name of the quantitation type to process." );
    }

    @Override
    protected void processExperimentOptions( CommandLine commandLine ) throws ParseException {
        quantitationTypeName = commandLine.getOptionValue( "qtName" );
    }

    @Override
    protected void processExpressionExperiment( ExpressionExperiment expressionExperiment ) {
        if ( quantitationTypeName != null ) {
            QuantitationType qt;
            try {
                qt = quantitationTypeService.findByNameAndVectorType( expressionExperiment, quantitationTypeName, RawExpressionDataVector.class );
            } catch ( NonUniqueQuantitationTypeByNameException e ) {
                throw new RuntimeException( e );
            }
            if ( qt != null ) {
                detectRawQuantitationType( expressionExperiment, qt );
            }
        } else {
            quantitationTypeService.findByExpressionExperiment( expressionExperiment, RawExpressionDataVector.class )
                    .forEach( qt -> detectRawQuantitationType( expressionExperiment, qt ) );
        }
        if ( quantitationTypeName != null ) {
            QuantitationType qt;
            try {
                qt = quantitationTypeService.findByNameAndVectorType( expressionExperiment, quantitationTypeName, SingleCellExpressionDataVector.class );
            } catch ( NonUniqueQuantitationTypeByNameException e ) {
                throw new RuntimeException( e );
            }
            if ( qt != null ) {
                detectRawQuantitationType( expressionExperiment, qt );
            }
        } else {
            quantitationTypeService.findByExpressionExperiment( expressionExperiment, SingleCellExpressionDataVector.class )
                    .forEach( qt -> detectSingleCellQuantitationType( expressionExperiment, qt ) );
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
