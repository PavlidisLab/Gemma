/*
 * The Gemma project
 * 
 * Copyright (c) 2006 University of British Columbia
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
package ubic.gemma.analysis.diff;

import java.util.Collection;
import java.util.HashSet;

import ubic.gemma.model.analysis.Analysis;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.analysis.ExpressionAnalysis;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;

/**
 * A spring loaded differential expression service to run the differential expression analysis (and persist the results
 * using the appropriate data access objects).
 * 
 * @spring.bean id="differentialExpressionAnalysisService"
 * @spring.property name="expressionExperimentService" ref="expressionExperimentService"
 * @author keshav
 * @version $Id$
 */
public class DifferentialExpressionAnalysisService {

    ExpressionExperimentService expressionExperimentService = null;

    DifferentialExpressionAnalysis analysis = null;

    /**
     * @param expressionExperiment
     * @param quantitationType
     * @param bioAssayDimension
     */
    public void analyze( ExpressionExperiment expressionExperiment, QuantitationType quantitationType,
            BioAssayDimension bioAssayDimension ) {

        // TODO allow method to take in some config attributes, like whether or not to persist.

        analysis = new DifferentialExpressionAnalysis();

        analysis.analyze( expressionExperiment );

    }

    /**
     * @return
     */
    public Collection<ExpressionAnalysis> getExpressionAnalysis( ExpressionExperiment expressionExperiment ) {

        Collection<ExpressionAnalysis> expressionAnalyses = new HashSet<ExpressionAnalysis>();
        Collection<Analysis> analyses = expressionExperiment.getAnalyses();

        for ( Analysis analysis : analyses ) {
            if ( analysis instanceof ExpressionAnalysis ) {
                expressionAnalyses.add( ( ExpressionAnalysis ) analysis );
            }
        }
        return expressionAnalyses;
    }

    /**
     * @return
     */
    public Collection<ExpressionAnalysis> getExpressionAnalysis( String shortName ) {

        ExpressionExperiment ee = expressionExperimentService.findByShortName( shortName );

        return this.getExpressionAnalysis( ee );

    }

    /**
     * @return
     */
    public ExpressionAnalysis getExpressionAnalysis() {
        return analysis.getExpressionAnalysis();
    }

    /**
     * @param expressionExperimentService
     */
    public void setExpressionExperimentService( ExpressionExperimentService expressionExperimentService ) {
        this.expressionExperimentService = expressionExperimentService;
    }
}
