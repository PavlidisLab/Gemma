package ubic.gemma.web.controller.expression.experiment;

import java.io.File;

import ubic.gemma.analysis.expression.diff.DifferentialExpressionFileUtils;
import ubic.gemma.analysis.stats.ExpressionDataSampleCorrelation;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.util.ConfigUtils;

/**
 * TODO Document Me
 * 
 * @author paul
 * @version $Id$
 */
public class ExpressionExperimentQCUtils {

    /**
     * @param ee
     * @return
     */
    public static boolean hasCorrDistFile( ExpressionExperiment ee ) {

        if ( ee == null ) return false;
        String shortName = ee.getShortName();
        String analysisStoragePath = ConfigUtils.getAnalysisStoragePath();
        String suffix = ".correlDist.txt";
        File f = new File( analysisStoragePath + File.separatorChar + shortName + suffix );
        return f.exists() && f.canRead();
    }

    /**
     * @param ee
     * @return
     */
    public static boolean hasCorrMatFile( ExpressionExperiment ee ) {

        if ( ee == null ) return false;
        String shortName = ee.getShortName();
        String analysisStoragePath = ConfigUtils.getAnalysisStoragePath() + File.separatorChar
                + ExpressionDataSampleCorrelation.CORRMAT_DIR_NAME;
        File f = new File( analysisStoragePath + File.separatorChar + shortName + "_corrmat" + ".txt" );
        return f.exists() && f.canRead();
    }

    public static boolean hasPvalueDistFiles( ExpressionExperiment ee ) {

        if ( ee == null ) return false;

        String shortName = ee.getShortName();

        File directory = DifferentialExpressionFileUtils.getBaseDifferentialDirectory( shortName );
        if ( !directory.exists() ) {
            return false;
        }

        String[] fileNames = directory.list();
        String suffix = DifferentialExpressionFileUtils.PVALUE_DIST_SUFFIX;
        for ( String fileName : fileNames ) {
            if ( fileName.endsWith( suffix ) ) {
                return true;
            }
        }

        return false;
    }

}
