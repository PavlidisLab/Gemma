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
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.rosuda.JRclient.REXP;

import ubic.basecode.dataStructure.matrix.DoubleMatrixNamed;
import ubic.gemma.datastructure.matrix.ExpressionDataDoubleMatrix;
import ubic.gemma.datastructure.matrix.ExpressionDataMatrix;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.designElement.DesignElement;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.FactorValue;

/**
 * A two way anova without interactions implementation as described by P. Pavlidis, Methods 31 (2003) 282-289.
 * <p>
 * See http://www.bioinformatics.ubc.ca/pavlidis/lab/docs/reprints/anova-methods.pdf.
 * 
 * @author keshav
 * @version $Id$
 */
public class TwoWayAnovaWithoutInteractionsAnalyzer extends AbstractAnalyzer {

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.analysis.diff.AbstractAnalyzer#getPValues(ubic.gemma.model.expression.experiment.ExpressionExperiment,
     *      ubic.gemma.model.common.quantitationtype.QuantitationType,
     *      ubic.gemma.model.expression.bioAssayData.BioAssayDimension, java.util.Collection)
     */
    @Override
    public Map<DesignElement, Double> getPValues( ExpressionExperiment expressionExperiment,
            QuantitationType quantitationType, BioAssayDimension bioAssayDimension,
            Collection<ExperimentalFactor> experimentalFactors ) {

        if ( experimentalFactors.size() != 2 )
            throw new RuntimeException( "Two way anova supports 2 experimental factors.  Received "
                    + experimentalFactors.size() + "." );

        Iterator iter = experimentalFactors.iterator();
        ExperimentalFactor experimentalFactorA = ( ExperimentalFactor ) iter.next();
        ExperimentalFactor experimentalFactorB = ( ExperimentalFactor ) iter.next();

        return twoWayAnova( expressionExperiment, quantitationType, experimentalFactorA, experimentalFactorB );
    }

    /**
     * @param expressionExperiment
     * @param quantitationType
     * @param experimentalFactorA
     * @param experimentalFactorB
     * @return
     */
    public Map<DesignElement, Double> twoWayAnova( ExpressionExperiment expressionExperiment,
            QuantitationType quantitationType, ExperimentalFactor experimentalFactorA,
            ExperimentalFactor experimentalFactorB ) {

        Collection factorValuesA = experimentalFactorA.getFactorValues();
        Collection factorValuesB = experimentalFactorB.getFactorValues();

        if ( factorValuesA.size() < 2 || factorValuesB.size() < 2 ) {
            throw new RuntimeException(
                    "Two way anova requires 2 or more factor values per experimental factor.  Received "
                            + factorValuesA.size() + " for either experimental factor " + experimentalFactorA.getName()
                            + " or experimental factor " + experimentalFactorB.getName() + "." );
        }

        ExpressionDataMatrix matrix = new ExpressionDataDoubleMatrix( expressionExperiment
                .getDesignElementDataVectors() );

        Collection<BioMaterial> biomaterials = AnalyzerHelper.getBioMaterialsForBioAssays( matrix );

        return twoWayAnova( matrix, experimentalFactorA, experimentalFactorB, biomaterials );
    }

    /**
     * R Call:
     * <p>
     * apply(matrix,1,function(x){anova(aov(x~farea+ftreat))$Pr})
     * <p>
     * where area and treat are first transposed and then factor is called on each to give farea and ftreat.
     * 
     * @param matrix
     * @param experimentalFactorA
     * @param experimentalFactorB
     * @param samplesUsed
     * @return
     */
    public Map<DesignElement, Double> twoWayAnova( ExpressionDataMatrix matrix, ExperimentalFactor experimentalFactorA,
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

        double[] filteredPvalues = new double[pvalues.length / 2];// removes the NaN row

        for ( int i = 0, j = 0; j < filteredPvalues.length; i++ ) {
            if ( i % 3 < 2 ) {
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

        return pvaluesMap;
    }
}
