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
import java.io.IOException;
import java.io.Writer;
import java.util.Collection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.gemma.analysis.preprocess.ExpressionDataMatrixBuilder;
import ubic.gemma.datastructure.matrix.ExpressionDataDoubleMatrix;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

/**
 * An abstract differential expression analyzer to be extended by analyzers which will make use of R. For example, see
 * {@link OneWayAnovaAnalyzer}.
 * 
 * @author keshav
 * @version $Id$
 */
public abstract class AbstractDifferentialExpressionAnalyzer extends AbstractAnalyzer {

    private Log log = LogFactory.getLog( this.getClass() );

    /**
     * @param expressionExperiment
     * @return ExpressionAnalysis
     */
    public abstract DifferentialExpressionAnalysis getDifferentialExpressionAnalysis(
            ExpressionExperiment expressionExperiment );

    /**
     * @param pvalues
     * @return returns the qvalues for the pvalues
     */
    protected double[] getQValues( double[] pvalues ) {

        if ( rc == null ) {
            connectToR();
        }
        boolean hasQValue = rc.loadLibrary( "qvalue" );
        if ( !hasQValue ) {
            throw new IllegalStateException( "qvalue does not seem to be available" );
        }

        StringBuffer qvalueCommand = new StringBuffer();
        String pvalsName = "pvals";
        rc.assign( pvalsName, pvalues );
        qvalueCommand.append( "qvalue(" + pvalsName + ")$qvalues" );
        double[] qvalues = rc.doubleArrayEval( qvalueCommand.toString() );

        if ( qvalues == null ) {
            throw new IllegalStateException( "Null qvalues.  Check the R side." );
        }

        if ( qvalues.length != pvalues.length ) {
            throw new IllegalStateException( "Number of q values and p values must match.  Qvalues - " + qvalues.length
                    + ": Pvalues - " + pvalues.length );
        }

        return qvalues;
    }

    /**
     * Save the raw pvalues to a file.
     * 
     * @param pvalues
     * @param location
     * @param file
     * @throws IOException
     */
    protected static void writeRawPValues( Double[] pvalues, File location, String file ) throws IOException {
        File f = new File( location, file );

        Writer writer = new FileWriter( file );
        for ( int i = 0; i < pvalues.length; i++ ) {
            writer.write( pvalues[i].toString() );

            if ( i < pvalues.length - 1 ) writer.write( "\t" );
        }
    }

    /**
     * Returns the preferred {@link QuantitationType}.
     * 
     * @param vectors
     * @return
     */
    protected QuantitationType getPreferredQuantitationType( Collection<DesignElementDataVector> vectors ) {
        // FIXME could be slow?
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

        ExpressionDataDoubleMatrix dmatrix = builder.getMaskedPreferredData();

        return dmatrix;
    }
}
