/*
 * The Gemma project
 * 
 * Copyright (c) 2011 University of British Columbia
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package ubic.gemma.analysis.expression;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import ubic.gemma.model.analysis.expression.coexpression.ProbeCoexpressionAnalysis;
import ubic.gemma.model.analysis.expression.coexpression.ProbeCoexpressionAnalysisService;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisService;
import ubic.gemma.model.analysis.expression.pca.PrincipalComponentAnalysisService;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

/**
 * Utility methods for dealing with analyses.
 * 
 * @author paul
 * @version $Id$
 */
@Component
public class AnalysisUtilServiceImpl implements AnalysisUtilService {

    private static Log log = LogFactory.getLog( AnalysisUtilServiceImpl.class );

    @Autowired
    private DifferentialExpressionAnalysisService differentialExpressionAnalysisService;

    @Autowired
    private ProbeCoexpressionAnalysisService coexpressionAnalysisService;

    @Autowired
    private PrincipalComponentAnalysisService principalComponentAnalysisService;

    /* (non-Javadoc)
     * @see ubic.gemma.analysis.expression.AnalysisUtilService#deleteOldAnalyses(ubic.gemma.model.expression.experiment.ExpressionExperiment)
     */
    @Override
    public boolean deleteOldAnalyses( ExpressionExperiment expExp ) {

        boolean removedAll = true;
        log.info( "Removing old analyses for " + expExp );
        if ( principalComponentAnalysisService.loadForExperiment( expExp ) != null ) {
            try {
                principalComponentAnalysisService.removeForExperiment( expExp );
            } catch ( Exception e ) {
                log.warn( "Could not delete pca for: " + expExp );
                removedAll = false;
            }
        }

        for ( DifferentialExpressionAnalysis diff : differentialExpressionAnalysisService.findByInvestigation( expExp ) ) {
            try {
                differentialExpressionAnalysisService.delete( diff );
            } catch ( Exception e ) {
                log.warn( "Could not delete analysis: " + diff + ": " + e.getMessage() );
                removedAll = false;
            }
        }
        for ( ProbeCoexpressionAnalysis coex : coexpressionAnalysisService.findByInvestigation( expExp ) ) {
            try {
                coexpressionAnalysisService.delete( coex );
            } catch ( Exception e ) {
                log.warn( "Could not delete analysis: " + coex + ": " + e.getMessage() );
                removedAll = false;
            }
        }
        return removedAll;
    }
}
