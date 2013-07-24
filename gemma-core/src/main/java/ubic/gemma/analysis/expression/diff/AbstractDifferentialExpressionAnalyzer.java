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
package ubic.gemma.analysis.expression.diff;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.basecode.dataStructure.matrix.DenseDoubleMatrix1D;
import ubic.basecode.math.MultipleTestCorrection;
import ubic.basecode.math.Rank;
import ubic.gemma.datastructure.matrix.ExpressionDataDoubleMatrix;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import cern.colt.list.DoubleArrayList;
import cern.colt.matrix.DoubleMatrix1D;

/**
 * An abstract differential expression analyzer to be extended by analyzers which will make use of R. For example, see
 * {@link OneWayAnovaAnalyzerImpl}.
 * 
 * @author keshav
 * @version $Id$
 */
public abstract class AbstractDifferentialExpressionAnalyzer extends AbstractAnalyzer implements DiffExAnalyzer {

    private Log log = LogFactory.getLog( this.getClass() );

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.analysis.expression.diff.DiffExAnalyzer#run(ubic.gemma.model.expression.experiment.ExpressionExperiment
     * , ubic.gemma.analysis.expression.diff.DifferentialExpressionAnalysisConfig)
     */
    @Override
    public abstract Collection<DifferentialExpressionAnalysis> run( ExpressionExperiment expressionExperiment,
            DifferentialExpressionAnalysisConfig config );

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.analysis.expression.diff.DiffExAnalyzer#run(ubic.gemma.model.expression.experiment.ExpressionExperiment
     * , ubic.gemma.datastructure.matrix.ExpressionDataDoubleMatrix,
     * ubic.gemma.analysis.expression.diff.DifferentialExpressionAnalysisConfig)
     */
    @Override
    public abstract Collection<DifferentialExpressionAnalysis> run( ExpressionExperiment expressionExperiment,
            ExpressionDataDoubleMatrix dmatrix, DifferentialExpressionAnalysisConfig config );

    /**
     * @param pvalues
     * @return normalized ranks of the pvalues, or null if they were invalid/unusable.
     */
    protected double[] computeRanks( double[] pvalues ) {
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
     * @param pvalues
     * @return Qvalues, or null if they could not be computed.
     */
    protected double[] benjaminiHochberg( Double[] pvalues ) {
        DoubleMatrix1D benjaminiHochberg = MultipleTestCorrection.benjaminiHochberg( new DenseDoubleMatrix1D(
                ArrayUtils.toPrimitive( pvalues ) ) );
        if ( benjaminiHochberg == null ) {
            return null;
        }
        return benjaminiHochberg.toArray();
    }

    protected DifferentialExpressionAnalysis initAnalysisEntity( BioAssaySet bioAssaySet ) {
        // TODO pass the DifferentialExpressionAnalysisConfig in (see LinkAnalysisService)
        /* Create the expression analysis and pack the results. */
        DifferentialExpressionAnalysisConfig config = new DifferentialExpressionAnalysisConfig();
        DifferentialExpressionAnalysis expressionAnalysis = config.toAnalysis();
        expressionAnalysis.setExperimentAnalyzed( bioAssaySet );
        return expressionAnalysis;
    }

    /**
     * Needed to convert NaN or infinity values to a value we can store in the database.
     * 
     * @param e
     * @return
     */
    protected Double nan2Null( Double e ) {
        boolean isNaN = e == null || Double.isNaN( e ) || e == Double.NEGATIVE_INFINITY
                || e == Double.POSITIVE_INFINITY;
        if ( isNaN ) {
            return null;
        }
        return e;
    }

    /**
     * Debugging tool. For example, if qvalue failed, save the pvalues to a temporary file for inspection.
     * 
     * @param pvaluesToUse
     * @return path to file where the pvalues were saved (a temporary file)
     * @throws IOException
     */
    protected String savePvaluesForDebugging( double[] pvaluesToUse ) {
        try {
            File f = File.createTempFile( "diffanalysis_", ".pvalues.txt" );
            FileWriter w = new FileWriter( f );
            for ( double d : pvaluesToUse ) {
                w.write( d + "\n" );
            }
            w.close();

            return f.getPath();
        } catch ( IOException e ) {
            throw new RuntimeException( e );
        }
    }

}
