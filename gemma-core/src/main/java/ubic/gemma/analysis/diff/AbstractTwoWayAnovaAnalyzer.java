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
import java.util.Iterator;
import java.util.Map;

import ubic.gemma.datastructure.matrix.ExpressionDataDoubleMatrix;
import ubic.gemma.datastructure.matrix.ExpressionDataMatrix;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.analysis.ExpressionAnalysis;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.designElement.DesignElement;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

/**
 * A two way anova base class as described by P. Pavlidis, Methods 31 (2003) 282-289.
 * <p>
 * See http://www.bioinformatics.ubc.ca/pavlidis/lab/docs/reprints/anova-methods.pdf.
 * <p>
 * For specific implementations with and without interactions, see the {@link TwoWayAnovaWithInteractionsAnalyzer} and
 * {@link TwoWayAnovaWithoutInteractionsAnalyzer} respectively.
 * 
 * @author keshav
 * @version $Id$
 */
public abstract class AbstractTwoWayAnovaAnalyzer extends AbstractAnalyzer {

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.analysis.diff.AbstractAnalyzer#getPValues(ubic.gemma.model.expression.experiment.ExpressionExperiment,
     *      ubic.gemma.model.common.quantitationtype.QuantitationType,
     *      ubic.gemma.model.expression.bioAssayData.BioAssayDimension, java.util.Collection)
     */
    @Override
    public ExpressionAnalysis getExpressionAnalysis( ExpressionExperiment expressionExperiment,
            QuantitationType quantitationType, BioAssayDimension bioAssayDimension ) {

        Collection<ExperimentalFactor> experimentalFactors = expressionExperiment.getExperimentalDesign()
                .getExperimentalFactors();

        if ( experimentalFactors.size() != 2 )
            throw new RuntimeException( "Two way anova supports 2 experimental factors.  Received "
                    + experimentalFactors.size() + "." );

        Iterator iter = experimentalFactors.iterator();
        ExperimentalFactor experimentalFactorA = ( ExperimentalFactor ) iter.next();
        ExperimentalFactor experimentalFactorB = ( ExperimentalFactor ) iter.next();

        return twoWayAnova( expressionExperiment, quantitationType, bioAssayDimension, experimentalFactorA,
                experimentalFactorB );
    }

    /**
     * @param expressionExperiment
     * @param quantitationType
     * @param bioAssayDimension
     * @param experimentalFactorA
     * @param experimentalFactorB
     * @return
     */
    public ExpressionAnalysis twoWayAnova( ExpressionExperiment expressionExperiment,
            QuantitationType quantitationType, BioAssayDimension bioAssayDimension,
            ExperimentalFactor experimentalFactorA, ExperimentalFactor experimentalFactorB ) {

        Collection factorValuesA = experimentalFactorA.getFactorValues();
        Collection factorValuesB = experimentalFactorB.getFactorValues();

        if ( factorValuesA.size() < 2 || factorValuesB.size() < 2 ) {
            throw new RuntimeException(
                    "Two way anova requires 2 or more factor values per experimental factor.  Received "
                            + factorValuesA.size() + " for either experimental factor " + experimentalFactorA.getName()
                            + " or experimental factor " + experimentalFactorB.getName() + "." );
        }

        ExpressionDataMatrix matrix = new ExpressionDataDoubleMatrix( expressionExperiment
                .getDesignElementDataVectors(), bioAssayDimension, quantitationType );

        Collection<BioMaterial> biomaterials = AnalyzerHelper.getBioMaterialsForBioAssays( matrix );

        return twoWayAnova( matrix, experimentalFactorA, experimentalFactorB, biomaterials );
    }

    /**
     * Two be implemented by the two way anova analyzer.
     * 
     * @param matrix
     * @param experimentalFactorA
     * @param experimentalFactorB
     * @param samplesUsed
     * @return
     */
    public abstract ExpressionAnalysis twoWayAnova( ExpressionDataMatrix matrix,
            ExperimentalFactor experimentalFactorA, ExperimentalFactor experimentalFactorB,
            Collection<BioMaterial> samplesUsed );

}
