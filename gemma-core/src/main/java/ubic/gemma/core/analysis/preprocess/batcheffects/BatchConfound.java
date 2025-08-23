/*
 * The Gemma project
 *
 * Copyright (c) 2011 University of British Columbia
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package ubic.gemma.core.analysis.preprocess.batcheffects;

import lombok.Value;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentSubSet;

/**
 * Represents a summary of a batch effect confound.
 *
 * @author paul
 */
@Value
public class BatchConfound {

    /**
     * Experiment or subset this confound is applicable to.
     */
    BioAssaySet bioAssaySet;
    /**
     * Factor being confounded with the batches.
     */
    ExperimentalFactor factor;
    double chiSquare;
    int df;
    double pValue;
    /**
     * Number of batches.
     */
    int numBatches;

    @Override
    public String toString() {
        String name;
        if ( bioAssaySet instanceof ExpressionExperimentSubSet ) {
            name = ( ( ExpressionExperimentSubSet ) bioAssaySet ).getSourceExperiment().getShortName();
        } else {
            name = "Subset " + bioAssaySet.getName() + " of " + ( ( ExpressionExperiment ) bioAssaySet ).getShortName();
        }
        return String.format( "%d\t%s\t%d\t%s\t%s\t%d\t%s\t%d", bioAssaySet.getId(), name, factor.getId(),
                factor.getCategory() != null ? factor.getCategory().getCategory() : factor.getName(),
                String.format( "%.2f", chiSquare ), df, String.format( "%.2g", pValue ), numBatches );
    }

}
