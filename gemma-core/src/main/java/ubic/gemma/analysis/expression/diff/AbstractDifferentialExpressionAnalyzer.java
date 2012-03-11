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
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.basecode.dataStructure.matrix.DenseDoubleMatrix1D;
import ubic.basecode.math.MultipleTestCorrection;
import ubic.basecode.math.Rank;
import ubic.gemma.datastructure.matrix.ExpressionDataDoubleMatrix;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
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
     * )
     */
    @Override
    public abstract Collection<DifferentialExpressionAnalysis> run( ExpressionExperiment expressionExperiment );

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.analysis.expression.diff.DiffExAnalyzer#run(ubic.gemma.model.expression.experiment.ExpressionExperiment
     * , java.util.Collection)
     */
    @Override
    public abstract Collection<DifferentialExpressionAnalysis> run( ExpressionExperiment expressionExperiment,
            Collection<ExperimentalFactor> factors );

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
     * , ubic.gemma.model.expression.experiment.ExperimentalFactor)
     */
    @Override
    public abstract Collection<DifferentialExpressionAnalysis> run( ExpressionExperiment expressionExperiment,
            ExperimentalFactor... experimentalFactors );

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

    /**
     * Calls the Q value function in R.
     * 
     * @param pvalues Entries that are NaN or out of range [0,1] are ignored in the qvalue computation and rendered as
     *        NaN qvalues.
     * @return returns the qvalues (false discovery rates) for the pvalues using the method of Storey and Tibshirani;
     *         falls back on Benjamini-Hochberg if qvalue fails to return a result (this can happen if qvalue's fitting
     *         procedure fails to converge).
     */
    protected double[] getQValues( Double[] pvalues ) {

        if ( pvalues == null || pvalues.length == 0 ) {
            throw new IllegalArgumentException( "No pvalues provided" );
        }

        if ( rc == null || !rc.isConnected() ) {
            connectToR();
        }
        double[] qvalues = new double[pvalues.length];

        /* Create a list with only the p-values that are not Double.NaN */
        ArrayList<Double> pvaluesList = new ArrayList<Double>();
        for ( int i = 0; i < pvalues.length; i++ ) {
            qvalues[i] = Double.NaN; // initialize.

            Double pvalue = pvalues[i];
            if ( pvalue == null || pvalue < 0.0 || pvalue > 1.0 || Double.isNaN( pvalue ) ) continue;
            pvaluesList.add( pvalue );
        }

        if ( pvaluesList.isEmpty() ) {
            throw new IllegalArgumentException( "No pvalues were valid numbers, returning null qvalues" );
        }

        /* convert to primitive array */
        double[] pvaluesToUse = new double[pvaluesList.size()];
        int j = 0;
        for ( Double d : pvaluesList ) {
            pvaluesToUse[j] = d;
            j++;
        }

        boolean hasQValue = rc.loadLibrary( "qvalue" );
        if ( !hasQValue ) {
            List<String> stringListEval = rc.stringListEval( "Sys.getenv()" );
            log.info( StringUtils.join( stringListEval, "\n" ) );
            throw new IllegalStateException( "qvalue does not seem to be available" );
        }

        String pvalsName = "pvals_" + RandomStringUtils.randomAlphabetic( 10 );

        rc.assign( pvalsName, pvaluesToUse );
        String qvalueCommand = ( "qvalue(" + pvalsName + ")$qvalues" );

        double[] qvaluesFromR = null;
        Exception qve = null;
        try {
            qvaluesFromR = rc.doubleArrayEval( qvalueCommand );
        } catch ( Exception e ) {
            qve = e;
        }

        if ( qvaluesFromR == null ) {
            /*
             * Qvalue will return an error in several conditions. 1)if the vector contains NaNs [we handle that
             * already]; 2) p-values are out of range 0-1 [we handle that too]; 3) pi0 [proportion of unchanged genes]
             * <= 0; 4) other invalid arguments which we don't set anyway. So we try the other method.
             */

            qvalueCommand = "qvalue(" + pvalsName + ", pi0.method=\"bootstrap\")$qvalues";
            try {
                qvaluesFromR = rc.doubleArrayEval( qvalueCommand );
            } catch ( Exception e ) {
                log.error( e, e );
                qve = e;
            }

            if ( qvaluesFromR == null ) {
                String err = "";

                if ( qve != null ) {
                    err = "qvalue failed: " + qve.getMessage();
                } else {
                    err = "Null qvalues were returned from R. No details about the problem, but probably pi0 was <= 0. Tried both fitting methods. Last attempted command was: "
                            + qvalueCommand;
                }

                String path = savePvaluesForDebugging( pvaluesToUse );

                /*
                 * Fall back on Benjamni-Hochberg
                 */

                boolean hasMulttest = rc.loadLibrary( "globaltest" );
                if ( hasMulttest ) {
                    log.info( "qvalue failed; Falling back on Benjamini-Hochberg (error was: " + qve );
                    qvalueCommand = "p.adjust(" + pvalsName + ", \"BH\")";
                    qve = null;
                    try {
                        qvaluesFromR = rc.doubleArrayEval( qvalueCommand );
                    } catch ( Exception e ) {
                        log.error( e, e );
                        qve = e;
                    }
                } else {
                    throw new IllegalStateException( err
                            + ". Fallback to Benjamini-Hochberg failed due to missing library. "
                            + "The pvalues that caused the problem are saved in: " + path
                            + "; try running in R: \nlibrary(qvalue);\nx<-read.table(\"" + path
                            + "\", header=F);\nsummary(qvalues(x));\n" );
                }

                if ( qvaluesFromR == null ) {
                    if ( qve != null ) {
                        err = "p.adjust failed (fallback for qvalue, which also failed): " + qve.getMessage();
                    } else {
                        err = "Null qvalues were returned from R. No details about the problem, but probably pi0 was <= 0. Tried both fitting methods. Last attempted command was: "
                                + qvalueCommand;
                    }
                    throw new IllegalStateException( err + ". The pvalues that caused the problem are saved in: "
                            + path + "; try running in R: \nlibrary(qvalue);\nx<-read.table(\"" + path
                            + "\", header=F);\nsummary(qvalues(x));\n" );

                }

            }
        }

        if ( qvaluesFromR.length != pvaluesToUse.length ) {
            throw new IllegalStateException( "Number of q values and p values must match.  Qvalues - "
                    + qvaluesFromR.length + ": Pvalues - " + pvaluesToUse.length );
        }

        /* Add the Double.NaN back in */
        int k = 0;

        for ( int i = 0; i < qvalues.length; i++ ) {
            Double pvalue = pvalues[i];
            if ( pvalue == null || pvalue < 0.0 || pvalue > 1.0 || Double.isNaN( pvalue ) ) {
                qvalues[i] = Double.NaN;
            } else {
                qvalues[i] = qvaluesFromR[k];
                k++;
            }
        }
        return qvalues;
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
