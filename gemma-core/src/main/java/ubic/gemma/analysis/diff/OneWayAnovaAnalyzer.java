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
public class OneWayAnovaAnalyzer extends AbstractAnalyzer {

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.analysis.diff.AbstractAnalyzer#getPValues(ubic.gemma.model.expression.experiment.ExpressionExperiment,
     *      java.util.Collection)
     */
    @Override
    public HashMap<DesignElement, Double> getPValues( ExpressionExperiment expressionExperiment,
            Collection<ExperimentalFactor> experimentalFactors ) {
        // TODO Auto-generated method stub
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.analysis.diff.AbstractAnalyzer#getSignificantGenes(java.util.Collection)
     */
    @Override
    public Collection<DesignElement> getSignificantGenes( Collection<ExperimentalFactor> experimentalFactors ) {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * @param matrix
     * @param factorValues
     * @param samplesUsed
     * @return
     */
    public HashMap<DesignElement, Double> oneWayAnova( ExpressionDataMatrix matrix,
            Collection<FactorValue> factorValues, Collection<BioMaterial> samplesUsed ) {

        // TODO implement me
        // R Call
        return null;
    }

}
