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
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.rosuda.JRclient.REXP;

import ubic.basecode.dataStructure.matrix.DoubleMatrixNamed;
import ubic.gemma.datastructure.matrix.ExpressionDataDoubleMatrix;
import ubic.gemma.datastructure.matrix.ExpressionDataMatrix;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.designElement.DesignElement;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.FactorValue;

/**
 * A t-test implementation as described by P. Pavlidis, Methods 31 (2003) 282-289.
 * <p>
 * See http://www.bioinformatics.ubc.ca/pavlidis/lab/docs/reprints/anova-methods.pdf.
 * 
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
            throw new RuntimeException( "T-test supports 1 experimental factor.  Received "
                    + experimentalFactors.size() + "." );

        ExperimentalFactor experimentalFactor = experimentalFactors.iterator().next();

        Collection<FactorValue> factorValues = experimentalFactor.getFactorValues();
        if ( factorValues.size() != 2 )
            throw new RuntimeException( "T-test supports 2 factor values.  Received " + factorValues.size() + "." );

        Iterator<FactorValue> iter = factorValues.iterator();

        FactorValue factorValueA = iter.next();

        FactorValue factorValueB = iter.next();

        return tTest( expressionExperiment, factorValueA, factorValueB );
    }

    /**
     * Runs a t-test on the factor values.
     * 
     * @param expressionExperiment
     * @param factorValues
     * @return
     */
    public Map<DesignElement, Double> tTest( ExpressionExperiment expressionExperiment, FactorValue factorValueA,
            FactorValue factorValueB ) {

        Collection<BioMaterial> biomaterials = AnalyzerHelper
                .getBioMaterialsForBioAssaysWithoutReplicates( expressionExperiment );

        // TODO will need to select a quantitation type (see AbstractAnalyzerTest)
        ExpressionDataMatrix matrix = new ExpressionDataDoubleMatrix( expressionExperiment
                .getDesignElementDataVectors() );

        return tTest( matrix, factorValueA, factorValueB, biomaterials );
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
    @SuppressWarnings("unchecked")
    protected Map<DesignElement, Double> tTest( ExpressionDataMatrix matrix, FactorValue factorValueA,
            FactorValue factorValueB, Collection<BioMaterial> samplesUsed ) {

        Collection<FactorValue> factorValues = new ArrayList<FactorValue>();
        factorValues.add( factorValueA );
        factorValues.add( factorValueB );

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

        return pvaluesMap;
    }

}
