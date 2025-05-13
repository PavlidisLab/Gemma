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
package ubic.gemma.core.analysis.expression.diff;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.core.analysis.service.ExpressionDataMatrixService;
import ubic.gemma.core.datastructure.matrix.ExpressionDataDoubleMatrix;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentSubSet;
import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * A differential expression analysis tool that executes the appropriate analysis based on the number of experimental
 * factors and factor values, as well as the block design.
 * <p>
 * Implementations of the selected analyses; t-test, one way anova, and two-way anova with and without interactions are
 * based on the details of the paper written by P. Pavlidis, Methods 31 (2003) 282-289.
 * <p>
 * See http://www.bioinformatics.ubc.ca/pavlidis/lab/docs/reprints/anova-methods.pdf.
 *
 * @author keshav
 */
@Service
class AnalysisSelectionAndExecutionService {

    private static final Log log = LogFactory.getLog( AnalysisSelectionAndExecutionService.class );

    @Autowired
    private ExpressionExperimentService expressionExperimentService;
    @Autowired
    private DiffExAnalyzer diffExAnalyzer;
    @Autowired
    private ExpressionDataMatrixService expressionDataMatrixService;

    @Transactional(readOnly = true)
    public Collection<DifferentialExpressionAnalysis> analyze( ExpressionExperiment expressionExperiment,
            DifferentialExpressionAnalysisConfig config ) {
        AnalysisType analyzer = DiffExAnalyzerUtils.determineAnalysisType( expressionExperiment, config );

        if ( analyzer == null ) {
            throw new RuntimeException( "Could not locate an appropriate analyzer" );
        }

        ExpressionDataDoubleMatrix dmatrix = expressionDataMatrixService
                .getProcessedExpressionDataMatrix( expressionExperiment, true );

        // if this is a subset analysis, see if we can reuse an existing set of subsets
        if ( config.getSubsetFactor() != null ) {
            Map<FactorValue, ExpressionExperimentSubSet> subsets = expressionExperimentService.getSubSetsByFactorValue( expressionExperiment, config.getSubsetFactor(), dmatrix.getBestBioAssayDimension() );
            if ( subsets != null ) {
                log.info( String.format( "%s already has subsets for %s, reusing them:\n\t%s", expressionExperiment,
                        config.getSubsetFactor(),
                        subsets.entrySet().stream().map( e -> e.getKey() + " -> " + e.getValue() ).collect( Collectors.joining( "\n\t" ) ) ) );
                return diffExAnalyzer.run( expressionExperiment, subsets, dmatrix, config );
            } else {
                log.debug( expressionExperiment + " does not have subsets for " + config.getSubsetFactor() + " they will be created by the analyzer." );
            }
        }

        return diffExAnalyzer.run( expressionExperiment, dmatrix, config );
    }

    @Transactional(readOnly = true)
    public DifferentialExpressionAnalysis analyze( ExpressionExperimentSubSet subset,
            DifferentialExpressionAnalysisConfig config ) {
        AnalysisType analyzer = DiffExAnalyzerUtils.determineAnalysisType( subset, config );

        if ( analyzer == null ) {
            throw new RuntimeException( "Could not locate an appropriate analyzer" );
        }

        ExpressionDataDoubleMatrix dmatrix = expressionDataMatrixService
                .getProcessedExpressionDataMatrix( subset.getSourceExperiment(), true );

        return diffExAnalyzer.run( subset, dmatrix, config );
    }
}
