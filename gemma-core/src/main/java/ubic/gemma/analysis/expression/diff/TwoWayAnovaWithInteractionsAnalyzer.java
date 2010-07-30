/*
 * The Gemma project
 * 
 * Copyright (c) 2007 University of British Columbia
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
package ubic.gemma.analysis.expression.diff;

import java.util.Collection;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

/**
 * A two way anova implementation with interactions as described by P. Pavlidis, Methods 31 (2003) 282-289.
 * 
 * @author keshav
 * @version $Id$
 * @see AbstractTwoWayAnovaAnalyzer
 */
@Service
@Scope(value = "prototype")
public class TwoWayAnovaWithInteractionsAnalyzer extends LinearModelAnalyzer {

    /*
     * (non-Javadoc)
     * 
     * @seeubic.gemma.analysis.expression.diff.LinearModelAnalyzer#run(ubic.gemma.model.expression.experiment.
     * ExpressionExperiment , ubic.gemma.analysis.expression.diff.DifferentialExpressionAnalysisConfig)
     */
    @Override
    public Collection<DifferentialExpressionAnalysis> run( ExpressionExperiment expressionExperiment,
            DifferentialExpressionAnalysisConfig config ) {

        if ( expressionExperiment.getExperimentalDesign().getExperimentalFactors().size() < 2 ) {
            throw new IllegalArgumentException( "Need two factors to run two-way anova " );
        }

        if ( config.getFactorsToInclude().isEmpty() ) {
            if ( expressionExperiment.getExperimentalDesign().getExperimentalFactors().size() != 2 ) {

                throw new IllegalArgumentException(
                        "You must specific the factors to use when the experiment has more than 2 factors" );
            }
            config.getFactorsToInclude().addAll( expressionExperiment.getExperimentalDesign().getExperimentalFactors() );
        }

        if ( config.getInteractionsToInclude().isEmpty() ) {
            config.addInteractionToInclude( config.getFactorsToInclude() );
        }

        return super.run( expressionExperiment, config );
    }

}
