/*
 * The Gemma project
 * 
 * Copyright (c) 2010 University of British Columbia
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package ubic.gemma.web.controller.expression.experiment;

import java.io.File;

import ubic.gemma.analysis.expression.diff.DifferentialExpressionFileUtils;
import ubic.gemma.analysis.stats.ExpressionDataSampleCorrelation;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.util.ConfigUtils;

/**
 * Helper functions for checking existence etc. of QC information.
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
        File f = new File( analysisStoragePath + File.separatorChar
                + ExpressionDataSampleCorrelation.cleanStringForPath( shortName ) + suffix );
        return f.exists() && f.canRead();
    }

    /**
     * @param ee
     * @return
     */
    public static boolean hasCorrMatFile( ExpressionExperiment ee ) {

        if ( ee == null ) return false;
        String analysisStoragePath = ConfigUtils.getAnalysisStoragePath() + File.separatorChar
                + ExpressionDataSampleCorrelation.CORRMAT_DIR_NAME;
        File f = new File( analysisStoragePath + File.separatorChar
                + ExpressionDataSampleCorrelation.getMatrixFileBaseName( ee ) + ".txt" );
        return f.exists() && f.canRead();
    }

    /**
     * @param ee
     * @return
     */
    public static boolean hasNodeDegreeDistFile( ExpressionExperiment ee ) {
        if ( ee == null ) return false;
        String shortName = ee.getShortName();
        // TODO implement
        // String analysisStoragePath = ConfigUtils.getAnalysisStoragePath() + File.separatorChar
        // + ExpressionDataSampleCorrelation.CORRMAT_DIR_NAME;
        // File f = new File( analysisStoragePath + File.separatorChar + shortName + "_corrmat" + ".txt" );
        // return f.exists() && f.canRead();
        return false;
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
