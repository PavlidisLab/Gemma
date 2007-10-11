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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.rosuda.JRclient.REXP;

import ubic.basecode.dataStructure.matrix.DoubleMatrixNamed;
import ubic.gemma.datastructure.matrix.ExpressionDataDoubleMatrix;
import ubic.gemma.datastructure.matrix.ExpressionDataMatrix;
import ubic.gemma.model.expression.analysis.ExpressionAnalysis;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.designElement.DesignElement;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.FactorValue;

/**
 * A two way anova implementation without interactions as described by P. Pavlidis, Methods 31 (2003) 282-289.
 * <p>
 * See http://www.bioinformatics.ubc.ca/pavlidis/lab/docs/reprints/anova-methods.pdf.
 * <p>
 * R Call:
 * <p>
 * apply(matrix,1,function(x){anova(aov(x~farea+ftreat))$Pr})
 * <p>
 * where area and treat are first transposed and then factor is called on each to give farea and ftreat.
 * 
 * @author keshav
 * @version $Id$
 * @see AbstractTwoWayAnovaAnalyzer
 */
public class TwoWayAnovaWithoutInteractionsAnalyzer extends AbstractTwoWayAnovaAnalyzer {

    /**
     * See class level javadoc for R Call.
     * 
     * @see AbstractTwoWayAnovaAnalyzer
     * @param matrix
     * @param experimentalFactorA
     * @param experimentalFactorB
     * @param samplesUsed
     * @return
     */
    @Override
    public ExpressionAnalysis twoWayAnova( ExpressionDataMatrix matrix, ExperimentalFactor experimentalFactorA,
            ExperimentalFactor experimentalFactorB, Collection<BioMaterial> samplesUsed ) {

        ExpressionDataDoubleMatrix dmatrix = ( ExpressionDataDoubleMatrix ) matrix;

        DoubleMatrixNamed namedMatrix = dmatrix.getNamedMatrix();

        Collection<FactorValue> factorValuesA = experimentalFactorA.getFactorValues();
        Collection<FactorValue> factorValuesB = experimentalFactorB.getFactorValues();

        List<String> rFactorsA = AnalyzerHelper.getRFactorsFromFactorValuesForTwoWayAnova( factorValuesA, samplesUsed );
        List<String> rFactorsB = AnalyzerHelper.getRFactorsFromFactorValuesForTwoWayAnova( factorValuesB, samplesUsed );

        String factsA = rc.assignStringList( rFactorsA );
        String factsB = rc.assignStringList( rFactorsB );

        String tfactsA = "t(" + factsA + ")";
        String tfactsB = "t(" + factsB + ")";

        String factorA = "factor(" + tfactsA + ")";
        String factorB = "factor(" + tfactsB + ")";

        String matrixName = rc.assignMatrix( namedMatrix );
        StringBuffer command = new StringBuffer();

        command.append( "apply(" );
        command.append( matrixName );
        command.append( ", 1, function(x) {anova(aov(x ~ " + factorA + "+" + factorB + "))$Pr}" );
        command.append( ")" );

        log.debug( command.toString() );

        REXP regExp = rc.eval( command.toString() );

        double[] pvalues = ( double[] ) regExp.getContent();

        double[] filteredPvalues = new double[( pvalues.length / 4 ) * 3];// removes the NaN row

        for ( int i = 0, j = 0; j < filteredPvalues.length; i++ ) {
            if ( i % 4 < 3 ) {
                filteredPvalues[j] = pvalues[i];
                j++;
            }
        }

        // TODO Use the ExpressionAnalysisResult
        Map<DesignElement, Double> pvaluesMap = new HashMap<DesignElement, Double>();
        for ( int i = 0; i < matrix.rows(); i++ ) {
            DesignElement de = matrix.getDesignElementForRow( i );
            pvaluesMap.put( de, filteredPvalues[i] );
        }

        return null;
    }

}
