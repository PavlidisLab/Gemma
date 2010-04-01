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
package ubic.gemma.analysis.expression.diff;

import java.io.File;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;

import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import cern.colt.list.DoubleArrayList;

import ubic.basecode.math.Rank;
import ubic.basecode.util.FileTools;
import ubic.gemma.analysis.preprocess.ExpressionDataMatrixBuilder;
import ubic.gemma.datastructure.matrix.ExpressionDataDoubleMatrix;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.description.VocabCharacteristic;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.FactorValue;

/**
 * An abstract differential expression analyzer to be extended by analyzers which will make use of R. For example, see
 * {@link OneWayAnovaAnalyzer}.
 * 
 * @author keshav
 * @version $Id$
 */
public abstract class AbstractDifferentialExpressionAnalyzer extends AbstractAnalyzer {

    private Log log = LogFactory.getLog( this.getClass() );
    private static Collection<String> controlGroupTerms = new HashSet<String>();

    static {
        /*
         * FIXME: make this external.
         */
        controlGroupTerms.add( "control group" );
        controlGroupTerms.add( "control_group" );
        controlGroupTerms.add( "http://purl.org/nbirn/birnlex/ontology/BIRNLex-Investigation.owl#birnlex_2201"
                .toLowerCase() );
    }

    /**
     * Peform an analysis where the factors are determined (or guessed) automatically. If this cannot be unambiguously
     * determined, an exception will be thrown.
     * 
     * @param expressionExperiment
     * @return ExpressionAnalysis
     */
    public abstract DifferentialExpressionAnalysis run( ExpressionExperiment expressionExperiment );

    /**
     * Perform an analysis using the specified factor(s)
     * 
     * @param expressionExperiment
     * @param factors
     * @return
     */
    public abstract DifferentialExpressionAnalysis run( ExpressionExperiment expressionExperiment,
            Collection<ExperimentalFactor> factors );

    /**
     * Calls the Q value function in R.
     * 
     * @param pvalues Entries that are NaN or out of range [0,1] are ignored in the qvalue computation and rendered as
     *        NaN qvalues.
     * @return returns the qvalues (false discovery rates) for the pvalues using the method of Storey and Tibshirani.
     */
    protected double[] getQValues( double[] pvalues ) {

        if ( pvalues == null || pvalues.length == 0 ) {
            throw new IllegalArgumentException( "No pvalues provided" );
        }

        if ( rc == null || !rc.isConnected() ) {
            connectToR();
        }

        /* Create a list with only the p-values that are not Double.NaN */
        ArrayList<Double> pvaluesList = new ArrayList<Double>();
        for ( int i = 0; i < pvalues.length; i++ ) {
            double pvalue = pvalues[i];
            if ( pvalue < 0.0 || pvalue > 1.0 || Double.isNaN( pvalue ) ) continue;
            pvaluesList.add( pvalue );
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
            throw new IllegalStateException( "qvalue does not seem to be available" );
        }

        String pvalsName = "pvals_" + RandomStringUtils.randomAlphabetic( 10 );

        rc.assign( pvalsName, pvaluesToUse );
        String qvalueCommand = ( "qvalue(" + pvalsName + ")$qvalues" );
        double[] qvaluesFromR = rc.doubleArrayEval( qvalueCommand.toString() );

        if ( qvaluesFromR == null ) {
            /*
             * Qvalue will return an error in several conditions. 1)if the vector contains NaNs [we handle that
             * already]; 2) p-values are out of range 0-1 [we handle that too]; 3) pi0 [proportion of unchanged genes]
             * <= 0; 4) other invalid arguments which we don't set anyway. I suspect the main cause of this is actually
             * #3. So we try the other method.
             */
            qvalueCommand = "qvalue(" + pvalsName + ", method=\"bootstrap\"" + ")$qvalues";
            qvaluesFromR = rc.doubleArrayEval( qvalueCommand.toString() );

            if ( qvaluesFromR == null ) {
                String err = "Null qvalues were returned from R. No details about the problem, but probably pi0 was <= 0. Tried both fitting methods. Last attempted command was: "
                        + qvalueCommand;

                String path = "";
                try {
                    path = savePvaluesForDebugging( pvaluesToUse );
                } catch ( IOException e ) {
                    throw new IllegalStateException( err + "; the pvalues could not be written to disk for debugging: "
                            + e.getMessage() );
                }
                throw new IllegalStateException( err + ". The pvalues that caused the problem are saved in: " + path
                        + "; try running in R: \nlibrary(qvalue);\nx<-read.table(\"" + path
                        + "\", header=F);\nsummary(qvalues(x));\n" );
            }
        }

        if ( qvaluesFromR.length != pvaluesToUse.length ) {
            throw new IllegalStateException( "Number of q values and p values must match.  Qvalues - "
                    + qvaluesFromR.length + ": Pvalues - " + pvaluesToUse.length );
        }

        /* Add the Double.NaN back in */
        int k = 0;
        double[] qvalues = new double[pvalues.length];
        for ( int i = 0; i < qvalues.length; i++ ) {
            double pvalue = pvalues[i];
            if ( pvalue < 0.0 || pvalue > 1.0 || Double.isNaN( pvalue ) ) {
                qvalues[i] = Double.NaN;
            } else {
                qvalues[i] = qvaluesFromR[k];
                k++;
            }
        }
        return qvalues;
    }

    /**
     * @param pvaluesToUse
     * @return
     * @throws IOException
     */
    private String savePvaluesForDebugging( double[] pvaluesToUse ) throws IOException {
        File f = File.createTempFile( "", "pvalues.txt" );
        FileWriter w = new FileWriter( f );
        for ( double d : pvaluesToUse ) {
            w.write( d + "\n" );
        }

        return f.getPath();
    }

    /**
     * @param pvalues
     * @param expressionExperiment
     * @param effects ordered (for 2 way anova)
     */
    protected void writePValuesHistogram( double[] pvalues, ExpressionExperiment expressionExperiment,
            ArrayList<ExperimentalFactor> effects ) {

        File dir = DifferentialExpressionFileUtils.getBaseDifferentialDirectory( expressionExperiment.getShortName() );

        FileTools.createDir( dir.toString() );

        File[] oldFiles = dir.listFiles( new FilenameFilter() {
            public boolean accept( File d, String name ) {
                return name.endsWith( DifferentialExpressionFileUtils.PVALUE_DIST_SUFFIX );
            }
        } );
        for ( File file : oldFiles ) {
            file.delete();
        }

        String histFileName = expressionExperiment.getShortName() + DifferentialExpressionFileUtils.PVALUE_DIST_SUFFIX;

        Collection<Histogram> hists = generateHistograms( histFileName, effects, 100, 0, 1, pvalues );

        if ( hists == null || hists.isEmpty() ) {
            log.error( "Could not generate histogram.  Not writing to file" );
            return;
        }

        for ( Histogram hist : hists ) {
            String path = dir + File.separator + hist.getName();

            File outputFile = new File( path );
            try {
                FileWriter out = new FileWriter( outputFile, false ); // false = clobber.
                out.write( "# Differential Expression distribution\n" );
                out.write( "# date=" + ( new Date() ) + "\n" );
                out.write( "# exp=" + expressionExperiment + " " + expressionExperiment.getShortName() + "\n" );
                out.write( "Bin\tCount\n" );
                hist.writeToFile( out );
                out.close();
            } catch ( IOException e ) {
                throw new RuntimeException( e );
            }
        }
    }

    /**
     * @param histFileName
     * @param effects ordered
     * @param numBins
     * @param min
     * @param max
     * @param pvalues
     * @return
     */
    protected abstract Collection<Histogram> generateHistograms( String histFileName,
            ArrayList<ExperimentalFactor> effects, int numBins, int min, int max, double[] pvalues );

    /**
     * Returns the preferred {@link QuantitationType}.
     * 
     * @param vectors
     * @return the first quantitation type that has 'isPreferred' == true, or null if none is found.
     */
    protected QuantitationType getPreferredQuantitationType( Collection<DesignElementDataVector> vectors ) {
        QuantitationType qt = null;
        for ( DesignElementDataVector vector : vectors ) {
            qt = vector.getQuantitationType();
            if ( qt.getIsPreferred() ) {
                return qt;
            }
        }
        log
                .error( "Could not determine the preferred quantitation type.  Not sure what type to associate with the analysis result." );
        return null;
    }

    /**
     * Creates the matrix using the vectors. Masks the data for two color arrays.
     * 
     * @param vectorsToUse
     * @return
     */
    protected ExpressionDataDoubleMatrix createMaskedMatrix( Collection<DesignElementDataVector> vectorsToUse ) {

        ExpressionDataMatrixBuilder builder = new ExpressionDataMatrixBuilder( vectorsToUse );

        ExpressionDataDoubleMatrix dmatrix = builder.getProcessedData();

        return dmatrix;
    }

    /**
     * @param pvalues
     * @return normalized ranks of the pvalues.
     */
    protected double[] computeRanks( double[] pvalues ) {
        if ( pvalues == null ) throw new IllegalArgumentException( "Null pvalues" );
        if ( pvalues.length == 0 ) throw new IllegalArgumentException( "Empty pvalues array" );
        DoubleArrayList pvalDal = new DoubleArrayList( pvalues );
        DoubleArrayList ranks = Rank.rankTransform( pvalDal );
        double[] normalizedRanks = new double[ranks.size()];
        for ( int i = 0; i < ranks.size(); i++ ) {
            normalizedRanks[i] = ranks.get( i ) / ranks.size();
        }
        return normalizedRanks;
    }

    protected FactorValue determineControlGroup( Collection<FactorValue> factorValues ) {
        FactorValue control = null;

        for ( FactorValue factorValue : factorValues ) {
            for ( Characteristic c : factorValue.getCharacteristics() ) {
                if ( c instanceof VocabCharacteristic ) {
                    String valueUri = ( ( VocabCharacteristic ) c ).getValueUri();
                    if ( StringUtils.isNotBlank( valueUri ) && controlGroupTerms.contains( valueUri.toLowerCase() ) ) {

                        if ( control != null ) {
                            log.warn( "More than one control group found, cannot choose between " + valueUri );
                            return null;
                        }

                        control = factorValue;
                    }
                } else if ( StringUtils.isNotBlank( c.getValue() )
                        && controlGroupTerms.contains( c.getValue().toLowerCase() ) ) {
                    if ( control != null ) {
                        log.warn( "More than one control group found, cannot choose between " + c.getValue() );
                        return null;
                    }

                    control = factorValue;
                }

            }
        }

        return control;
    }
}
