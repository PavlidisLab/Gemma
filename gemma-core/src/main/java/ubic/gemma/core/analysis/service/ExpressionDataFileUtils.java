package ubic.gemma.core.analysis.service;

import ubic.basecode.util.FileTools;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentSubSet;

public class ExpressionDataFileUtils {

    // for single-cell vectors
    private static final String SC_DATA_SUFFIX = ".scdata";
    public static final String MEX_SC_DATA_SUFFIX = SC_DATA_SUFFIX;
    public static final String TABULAR_SC_DATA_SUFFIX = SC_DATA_SUFFIX + ".tsv.gz";

    // for bulk (raw or processed vectors)
    private static final String BULK_DATA_SUFFIX = ".data";
    public static final String TABULAR_BULK_DATA_FILE_SUFFIX = BULK_DATA_SUFFIX + ".txt.gz";
    public static final String JSON_BULK_DATA_FILE_SUFFIX = BULK_DATA_SUFFIX + ".json.gz";

    /**
     * Obtain a filename for writing the processed data.
     */
    public static String getDataOutputFilename( ExpressionExperiment ee, boolean filtered, String suffix ) {
        return ee.getId() + "_" + FileTools.cleanForFileName( ee.getShortName() ) + "_expmat" + ( filtered ? "" : ".unfilt" ) + suffix;
    }

    /**
     * Obtain the filename for writing a specific QT.
     */
    public static String getDataOutputFilename( ExpressionExperiment ee, QuantitationType type, String suffix ) {
        return ee.getId() + "_" + FileTools.cleanForFileName( ee.getShortName() ) + "_" + type.getId() + "_" + FileTools.cleanForFileName( type.getName() ) + "_expmat.unfilt" + suffix;
    }

    /**
     * Obtain the filename for writing coexpression data.
     */
    public static String getCoexpressionDataFilename( ExpressionExperiment ee ) {
        return ee.getId() + "_" + FileTools.cleanForFileName( ee.getShortName() ) + "_coExp" + TABULAR_BULK_DATA_FILE_SUFFIX;
    }

    public static String getDesignFileName( ExpressionExperiment ee ) {
        return ee.getId() + "_" + FileTools.cleanForFileName( ee.getShortName() ) + "_expdesign" + TABULAR_BULK_DATA_FILE_SUFFIX;
    }

    public static String getDiffExArchiveFileName( DifferentialExpressionAnalysis diff ) {
        BioAssaySet experimentAnalyzed = diff.getExperimentAnalyzed();

        ExpressionExperiment ee;
        if ( experimentAnalyzed instanceof ExpressionExperiment ) {
            ee = ( ExpressionExperiment ) experimentAnalyzed;
        } else if ( experimentAnalyzed instanceof ExpressionExperimentSubSet ) {
            ExpressionExperimentSubSet subset = ( ExpressionExperimentSubSet ) experimentAnalyzed;
            ee = subset.getSourceExperiment();
        } else {
            throw new UnsupportedOperationException( "Don't know about " + experimentAnalyzed.getClass().getName() );
        }

        return experimentAnalyzed.getId() + "_" + FileTools.cleanForFileName( ee.getShortName() ) + "_diffExpAnalysis"
                // might be for a non-persistent diff ex. analysis
                + ( diff.getId() != null ? "_" + diff.getId() : "" )
                + ".zip";
    }
}
