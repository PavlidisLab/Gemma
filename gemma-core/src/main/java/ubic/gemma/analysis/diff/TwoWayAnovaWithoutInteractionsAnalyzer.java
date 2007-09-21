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
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import ubic.basecode.dataStructure.matrix.DoubleMatrixNamed;
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
public class TwoWayAnovaWithoutInteractionsAnalyzer extends AbstractAnalyzer {

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.analysis.diff.AbstractAnalyzer#getPValues(ubic.gemma.model.expression.experiment.ExpressionExperiment,
     *      java.util.Collection)
     */
    @Override
    public Map<DesignElement, Double> getPValues( ExpressionExperiment expressionExperiment,
            Collection<ExperimentalFactor> experimentalFactors ) {

        if ( experimentalFactors.size() != 2 )
            throw new RuntimeException( "Two way anova supports 2 experimental factors.  Received "
                    + experimentalFactors.size() + "." );

        Iterator iter = experimentalFactors.iterator();
        ExperimentalFactor experimentalFactorA = ( ExperimentalFactor ) iter.next();
        ExperimentalFactor experimentalFactorB = ( ExperimentalFactor ) iter.next();

        return twoWayAnova( expressionExperiment, experimentalFactorA, experimentalFactorB );
    }

    /**
     * @param expressionExperiment
     * @param experimentalFactors
     * @return
     */
    public Map<DesignElement, Double> twoWayAnova( ExpressionExperiment expressionExperiment,
            ExperimentalFactor experimentalFactorA, ExperimentalFactor experimentalFactorB ) {

        Collection factorValuesA = experimentalFactorA.getFactorValues();
        Collection factorValuesB = experimentalFactorB.getFactorValues();

        if ( factorValuesA.size() < 2 || factorValuesB.size() < 2 ) {
            throw new RuntimeException(
                    "Two way anova requires 2 or more factor values per experimental factor.  Received "
                            + factorValuesA.size() + " for either experimental factor " + experimentalFactorA.getName()
                            + " or experimental factor " + experimentalFactorB.getName() + "." );
        }

        Collection<BioMaterial> biomaterials = AnalyzerHelper
                .getBioMaterialsForBioAssaysWithoutReplicates( expressionExperiment );

        // TODO will need to select a quantitation type (see AbstractAnalyzerTest)
        ExpressionDataMatrix matrix = new ExpressionDataDoubleMatrix( expressionExperiment
                .getDesignElementDataVectors() );

        return twoWayAnova( matrix, experimentalFactorA, experimentalFactorB, biomaterials );
    }

    /**
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

        ArrayList<FactorValue> factorValues = new ArrayList<FactorValue>();
        factorValues.addAll( experimentalFactorA.getFactorValues() );
        factorValues.addAll( experimentalFactorB.getFactorValues() );

        List<String> rFactors = AnalyzerHelper.getRFactorsFromFactorValues( factorValues, samplesUsed );

        String facts = rc.assignStringList( rFactors );

        // R Call
        //
        // bdata<-read.table("/data.txt", header=T,row.names=1, sep="\t");
        // bfacts<-read.table("/classes.txt", header=T,row.names=1, sep="\t");
        //
        // apply(bdata, 1, aof);
        //
        // aof <- function(x) {
        // m<-data.frame(bfacts["area"], bfacts["treat"], x);
        // anova(aov(x ~ area + treat + area*treat, m))
        // }

        return null;
    }

}
