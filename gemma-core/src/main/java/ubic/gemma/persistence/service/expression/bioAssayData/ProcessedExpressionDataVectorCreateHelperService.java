/*
 * The Gemma project
 *
 * Copyright (c) 2013 University of British Columbia
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
package ubic.gemma.persistence.service.expression.bioAssayData;

import ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVector;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

import java.util.Collection;

/**
 * @author Paul
 */
interface ProcessedExpressionDataVectorCreateHelperService {

    int replaceProcessedDataVectors( ExpressionExperiment ee,
            Collection<ProcessedExpressionDataVector> vecs );

    void reorderByDesign( ExpressionExperiment ee );

    /**
     * If possible, update the ranks for the processed data vectors. For data sets with only ratio expression values
     * provided, ranks will not be computable.
     *
     * @param ee the experiment
     * @return processed vectors with updated ranks
     */
    void updateRanks( ExpressionExperiment ee );
}
