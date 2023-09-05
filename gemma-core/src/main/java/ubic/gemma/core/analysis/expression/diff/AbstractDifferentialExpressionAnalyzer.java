/*
 * The Gemma project
 *
 * Copyright (c) 2006-2010 University of British Columbia
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

import cern.colt.list.DoubleArrayList;
import cern.colt.matrix.DoubleMatrix1D;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ubic.basecode.dataStructure.matrix.DenseDoubleMatrix1D;
import ubic.basecode.math.MultipleTestCorrection;
import ubic.basecode.math.Rank;
import ubic.gemma.core.datastructure.matrix.ExpressionDataDoubleMatrix;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

import javax.annotation.Nullable;
import java.util.Collection;

/**
 * An abstract differential expression analyzer to be extended
 *
 * @author keshav
 */
public abstract class AbstractDifferentialExpressionAnalyzer extends AbstractAnalyzer implements DiffExAnalyzer {

    private final Log log = LogFactory.getLog( this.getClass() );

    @Override
    public abstract Collection<DifferentialExpressionAnalysis> run( ExpressionExperiment expressionExperiment,
            DifferentialExpressionAnalysisConfig config );

    @Override
    public abstract Collection<DifferentialExpressionAnalysis> run( ExpressionExperiment expressionExperiment,
            ExpressionDataDoubleMatrix dmatrix, DifferentialExpressionAnalysisConfig config );

    /**
     * @param pvalues pvalues
     * @return normalized ranks of the pvalues, or null if they were invalid/unusable.
     */
    double[] computeRanks( double[] pvalues ) {
        if ( pvalues == null ) {
            log.error( "Null pvalues" );
            return null;
        }
        if ( pvalues.length == 0 ) {
            log.error( "Empty pvalues array" );
            return null;
        }

        DoubleArrayList ranks = Rank.rankTransform( new DoubleArrayList( pvalues ) );

        if ( ranks == null ) {
            log.error( "Pvalue ranks could not be computed" );
            return null;
        }

        double[] normalizedRanks = new double[ranks.size()];
        for ( int i = 0; i < ranks.size(); i++ ) {
            normalizedRanks[i] = ranks.get( i ) / ranks.size();
        }
        return normalizedRanks;
    }

    /**
     * @param pvalues pvalues
     * @return Qvalues, or null if they could not be computed.
     */
    @Nullable
    double[] benjaminiHochberg( Double[] pvalues ) {
        DoubleMatrix1D benjaminiHochberg = MultipleTestCorrection
                .benjaminiHochberg( new DenseDoubleMatrix1D( ArrayUtils.toPrimitive( pvalues ) ) );
        return benjaminiHochberg != null ? benjaminiHochberg.toArray() : null;
    }

    DifferentialExpressionAnalysis initAnalysisEntity( BioAssaySet bioAssaySet,
            DifferentialExpressionAnalysisConfig config ) {

        if ( config == null ) {
            config = new DifferentialExpressionAnalysisConfig();
        }
        DifferentialExpressionAnalysis expressionAnalysis = config.toAnalysis();
        expressionAnalysis.setExperimentAnalyzed( bioAssaySet );
        return expressionAnalysis;
    }

    /**
     * Needed to convert NaN or infinity values to a value we can store in the database.
     *
     * @param e e
     * @return converted
     */
    Double nan2Null( Double e ) {
        boolean isNaN = ( e == null || Double.isNaN( e ) || e == Double.NEGATIVE_INFINITY
                || e == Double.POSITIVE_INFINITY );
        if ( isNaN ) {
            return null;
        }
        return e;
    }

}
