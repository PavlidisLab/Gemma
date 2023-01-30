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

import ubic.gemma.core.analysis.expression.diff.DifferentialExpressionFileUtils;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

import java.io.File;

/**
 * Helper functions for checking existence etc. of QC information.
 *
 * @author paul
 */
public class ExpressionExperimentQCUtils {

    public static boolean hasNodeDegreeDistFile( ExpressionExperiment ee ) {
        if ( ee == null )
            return false;
        // String shortName = ee.getShortName();
        // TODO implement
        // String analysisStoragePath = ConfigUtils.getAnalysisStoragePath() + File.separatorChar
        // + ExpressionDataSampleCorrelation.CORRMAT_DIR_NAME;
        // File f = new File( analysisStoragePath + File.separatorChar + shortName + "_corrmat" + ".txt" );
        // return f.exists() && f.canRead();
        return false;
    }

    public static boolean hasPvalueDistFiles( ExpressionExperiment ee ) {

        if ( ee == null )
            return false;

        String shortName = ee.getShortName();

        File directory = DifferentialExpressionFileUtils.getBaseDifferentialDirectory( shortName );
        if ( !directory.exists() ) {
            return false;
        }

        String[] fileNames = directory.list();
        if ( fileNames == null ) {
            return false;
        }
        String suffix = DifferentialExpressionFileUtils.PVALUE_DIST_SUFFIX;
        for ( String fileName : fileNames ) {
            if ( fileName.endsWith( suffix ) ) {
                return true;
            }
        }

        return false;
    }

    /**
     * @param expressionExperiment ee
     * @return How many factors (including batches) will be displayed in the PCA results?
     */
    public static int numFactors( ExpressionExperiment expressionExperiment ) {
        return 3; // FIXME
    }

}
