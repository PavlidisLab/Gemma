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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.rosuda.JRclient.REXP;

import ubic.basecode.dataStructure.matrix.DoubleMatrixNamed;
import ubic.gemma.datastructure.matrix.ExpressionDataDoubleMatrix;
import ubic.gemma.datastructure.matrix.ExpressionDataMatrix;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.designElement.DesignElement;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.FactorValue;

/**
 * @author keshav
 * @version $Id$
 */
public class OneWayAnovaAnalyzer extends AbstractAnalyzer {

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.analysis.diff.AbstractAnalyzer#getPValues(ubic.gemma.model.expression.experiment.ExpressionExperiment,
     *      java.util.Collection)
     */
    @Override
    public Map<DesignElement, Double> getPValues( ExpressionExperiment expressionExperiment,
            Collection<ExperimentalFactor> experimentalFactors ) {

        if ( experimentalFactors.size() != 1 )
            throw new RuntimeException( "One way anova supports one experimental factor.  Received "
                    + experimentalFactors.size() + "." );

        ExperimentalFactor experimentalFactor = experimentalFactors.iterator().next();

        return oneWayAnova( expressionExperiment, experimentalFactor.getFactorValues() );
    }

    /**
     * @param expressionExperiment
     * @param factorValues
     * @return
     */
    public Map<DesignElement, Double> oneWayAnova( ExpressionExperiment expressionExperiment,
            Collection<FactorValue> factorValues ) {
        Collection<BioMaterial> biomaterials = new ArrayList<BioMaterial>();

        Collection<BioAssay> allAssays = expressionExperiment.getBioAssays();

        for ( BioAssay assay : allAssays ) {
            Collection<BioMaterial> samplesUsed = assay.getSamplesUsed();
            biomaterials.addAll( samplesUsed );
        }

        ExpressionDataMatrix matrix = new ExpressionDataDoubleMatrix( expressionExperiment
                .getDesignElementDataVectors() );

        return oneWayAnova( matrix, factorValues, biomaterials );
    }

    /**
     * @param matrix
     * @param factorValues
     * @param samplesUsed
     * @return
     */
    public Map<DesignElement, Double> oneWayAnova( ExpressionDataMatrix matrix, Collection<FactorValue> factorValues,
            Collection<BioMaterial> samplesUsed ) {

        ExpressionDataDoubleMatrix dmatrix = ( ExpressionDataDoubleMatrix ) matrix;

        DoubleMatrixNamed namedMatrix = dmatrix.getNamedMatrix();

        List<String> rFactors = AnalyzerHelper.getRFactorsFromFactorValues( factorValues, samplesUsed );

        String facts = rc.assignStringList( rFactors );

        String tfacts = "t(" + facts + ")";

        String factor = "factor(" + tfacts + ")";

        String matrixName = rc.assignMatrix( namedMatrix );
        StringBuffer command = new StringBuffer();

        command.append( "apply(" );
        command.append( matrixName );
        command.append( ", 1, function(x) {anova(aov(x ~ " + factor + "))$Pr}" );
        command.append( ")" );

        log.info( command.toString() );

        REXP regExp = rc.eval( command.toString() );

        double[] pvalues = ( double[] ) regExp.getContent();

        double[] filteredPvalues = new double[pvalues.length / 2];// removes the NaN row
        for ( int i = 0, j = 0; i < pvalues.length; i++ ) {
            if ( i % 2 == 0 ) {
                filteredPvalues[j] = pvalues[i];
                j++;
            }
        }

        // TODO can you get the design elements from R?
        Map pvaluesMap = new HashMap<DesignElement, Double>();
        for ( int i = 0; i < matrix.rows(); i++ ) {
            DesignElement de = matrix.getDesignElementForRow( i );
            pvaluesMap.put( de, filteredPvalues[i] );
        }

        return pvaluesMap;
    }
}
