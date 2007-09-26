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

        Collection<FactorValue> factorValuesA = experimentalFactorA.getFactorValues();
        Collection<FactorValue> factorValuesB = experimentalFactorB.getFactorValues();

        /* separating the biomaterials according to the experimental factor */
        Collection<BioMaterial> samplesUsedA = new ArrayList<BioMaterial>();
        Collection<BioMaterial> samplesUsedB = new ArrayList<BioMaterial>();

        for ( BioMaterial m : samplesUsed ) {
            Collection<FactorValue> fvs = m.getFactorValues();
            for ( FactorValue fv : fvs ) {
                log.debug( fv.getValue() + " in experimental factor: " + fv.getExperimentalFactor() );
                if ( fv.getExperimentalFactor() == experimentalFactorA ) samplesUsedA.add( m );
                if ( fv.getExperimentalFactor() == experimentalFactorB ) samplesUsedB.add( m );

            }
        }

        List<String> rFactorsA = AnalyzerHelper.getRFactorsFromFactorValues( factorValuesA, samplesUsedA );
        List<String> rFactorsB = AnalyzerHelper.getRFactorsFromFactorValues( factorValuesB, samplesUsedB );

        String factsA = rc.assignStringList( rFactorsA );
        String factsB = rc.assignStringList( rFactorsB );

        String tfactsA = "t(" + factsA + ")";
        String factorA = "factor(" + tfactsA + ")";

        String tfactsB = "t(" + factsB + ")";
        String factorB = "factor(" + tfactsB + ")";

        String matrixName = rc.assignMatrix( namedMatrix );
        StringBuffer command = new StringBuffer();

        command.append( "apply(" );
        command.append( matrixName );
        command.append( ", 1, function(x) {anova(aov(x ~ " + factorA + "+" + factorB + "+" + "))$Pr}" );
        command.append( ")" );

        log.debug( command.toString() );

        REXP regExp = rc.eval( command.toString() );

        // R Call
        // The call is: apply(matrix,1,function(x){anova(aov(x~farea+ftreat+farea*ftreat))})
        // where area and treat are first transposed and then factor is called on each to give
        // farea and ftreat.
        // TODO
        // farea and ftreat are just vectors. You'll have to group the biomaterials by experimental factors, so either
        // send in two collections of biomaterials, or
        // separate them here.

        return null;
    }
}
