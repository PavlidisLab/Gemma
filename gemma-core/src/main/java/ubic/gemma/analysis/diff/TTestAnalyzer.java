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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.rosuda.JRclient.REXP;

import ubic.basecode.dataStructure.matrix.DoubleMatrixNamed;
import ubic.basecode.util.MapUtils;
import ubic.gemma.datastructure.matrix.ExpressionDataDoubleMatrix;
import ubic.gemma.datastructure.matrix.ExpressionDataMatrix;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.designElement.DesignElement;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.FactorValue;

/**
 * @author keshav
 * @version $Id$
 */
public class TTestAnalyzer extends AbstractAnalyzer {

    private Log log = LogFactory.getLog( this.getClass() );

    public TTestAnalyzer() {
        super();
    }

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
            throw new RuntimeException( "T-test supports one experimental factor.  Received "
                    + experimentalFactors.size() + "." );

        ExperimentalFactor experimentalFactor = experimentalFactors.iterator().next();

        return tTest( expressionExperiment, experimentalFactor.getFactorValues() );
    }

    /**
     * Runs a t-test on the factor values.
     * 
     * @param expressionExperiment
     * @param factorValues
     * @return
     */
    public Map<DesignElement, Double> tTest( ExpressionExperiment expressionExperiment,
            Collection<FactorValue> factorValues ) {
        // TODO change signature to take factorValue1 and factorValue2 since we know this for a ttest

        Collection<BioMaterial> biomaterials = AnalyzerHelper.getBioMaterialsForBioAssays( expressionExperiment );

        ExpressionDataMatrix matrix = new ExpressionDataDoubleMatrix( expressionExperiment
                .getDesignElementDataVectors() );

        return tTest( matrix, factorValues, biomaterials );
    }

    /**
     * Makes the following R call:
     * <p>
     * apply(matrix, 1, function(x) {t.test(x ~ factor(t(facts)))$p.value}) <-- R Console
     * 
     * @param matrix
     * @param factorValues
     * @param samplesUsed
     * @return
     */
    protected Map<DesignElement, Double> tTest( ExpressionDataMatrix matrix, Collection<FactorValue> factorValues,
            Collection<BioMaterial> samplesUsed ) {

        if ( factorValues.size() != 2 )
            throw new RuntimeException( "Must have only two factor values per experimental factor." );

        List<String> rFactors = AnalyzerHelper.getRFactorsFromFactorValues( factorValues, samplesUsed );

        ExpressionDataDoubleMatrix dmatrix = ( ExpressionDataDoubleMatrix ) matrix;

        DoubleMatrixNamed namedMatrix = dmatrix.getNamedMatrix();

        String facts = rc.assignStringList( rFactors );

        String tfacts = "t(" + facts + ")";

        String factor = "factor(" + tfacts + ")";

        String matrixName = rc.assignMatrix( namedMatrix );
        StringBuffer command = new StringBuffer();

        command.append( "apply(" );
        command.append( matrixName );
        // command.append( ", 1, function(x) {t.test(x[1:3],x[4:6])}" ); <-- Useful Test
        command.append( ", 1, function(x) {t.test(x ~ " + factor + ")$p.value}" );
        command.append( ")" );

        log.debug( command.toString() );

        REXP regExp = rc.eval( command.toString() );

        double[] pvalues = ( double[] ) regExp.getContent();

        // TODO can you get the design elements from R?
        Map pvaluesMap = new HashMap<DesignElement, Double>();
        for ( int i = 0; i < matrix.rows(); i++ ) {
            DesignElement de = matrix.getDesignElementForRow( i );
            pvaluesMap.put( de, pvalues[i] );
        }

        Map sortedPvaluesMap = MapUtils.sortMapByValues( pvaluesMap );

        return sortedPvaluesMap;
    }

}
