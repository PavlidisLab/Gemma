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
package ubic.gemma.core.analysis.expression;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import ubic.gemma.model.analysis.expression.coexpression.CoexpressionAnalysis;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.analysis.expression.coexpression.CoexpressionAnalysisService;
import ubic.gemma.persistence.service.analysis.expression.diff.DifferentialExpressionAnalysisService;
import ubic.gemma.persistence.service.analysis.expression.pca.PrincipalComponentAnalysisService;
import ubic.gemma.persistence.service.analysis.expression.sampleCoexpression.SampleCoexpressionAnalysisService;

/**
 * Utility methods for dealing with analyses.
 *
 * @author paul
 */
@Component
public class AnalysisUtilServiceImpl implements AnalysisUtilService {

    private static final Log log = LogFactory.getLog( AnalysisUtilServiceImpl.class );

    @Autowired
    private DifferentialExpressionAnalysisService differentialExpressionAnalysisService;

    @Autowired
    private CoexpressionAnalysisService coexpressionAnalysisService;

    @Autowired
    private PrincipalComponentAnalysisService principalComponentAnalysisService;

    @Autowired
    private SampleCoexpressionAnalysisService sampleCoexpressionAnalysisService;

    @Override
    public boolean deleteOldAnalyses( ExpressionExperiment expExp ) {

        boolean removedAll = true;
        AnalysisUtilServiceImpl.log.info( "Removing old analyses for " + expExp );
        if ( principalComponentAnalysisService.loadForExperiment( expExp ) != null ) {
            try {
                principalComponentAnalysisService.removeForExperiment( expExp );
            } catch ( Exception e ) {
                AnalysisUtilServiceImpl.log.warn( "Could not remove PCA for: " + expExp );
                removedAll = false;
            }
        }

        if ( sampleCoexpressionAnalysisService.hasAnalysis( expExp ) ) {
            try {
                sampleCoexpressionAnalysisService.removeForExperiment( expExp );
            } catch ( Exception e ) {
                AnalysisUtilServiceImpl.log.warn( "Could not remove sample correlations for: " + expExp );
                removedAll = false;
            }
        }

        for ( DifferentialExpressionAnalysis diff : differentialExpressionAnalysisService
                .findByExperiment( expExp, true ) ) {
            try {
                differentialExpressionAnalysisService.remove( diff );
            } catch ( Exception e ) {
                AnalysisUtilServiceImpl.log.warn( "Could not remove analysis: " + diff + ": " + e.getMessage() );
                removedAll = false;
            }
        }
        for ( CoexpressionAnalysis coex : coexpressionAnalysisService.findByExperimentAnalyzed( expExp ) ) {
            try {
                coexpressionAnalysisService.remove( coex );
            } catch ( Exception e ) {
                AnalysisUtilServiceImpl.log.warn( "Could not remove analysis: " + coex + ": " + e.getMessage() );
                removedAll = false;
            }
        }

        log.info( "Done deleting old analyses (as much as possible)" );
        return removedAll;
    }
}
