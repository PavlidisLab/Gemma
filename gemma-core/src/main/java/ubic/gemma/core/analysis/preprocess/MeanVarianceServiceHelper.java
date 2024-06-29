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
package ubic.gemma.core.analysis.preprocess;

import ubic.gemma.core.datastructure.matrix.ExpressionDataDoubleMatrix;
import ubic.gemma.core.lang.Nullable;
import ubic.gemma.model.expression.bioAssayData.MeanVarianceRelation;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

/**
 * @author ptan
 */
public interface MeanVarianceServiceHelper {

    /**
     * @param ee  the experiment
     * @param mvr the relation
     */
    void createMeanVariance( ExpressionExperiment ee, MeanVarianceRelation mvr );

    /**
     * @param ee the experiment
     * @return ExpressionDataDoubleMatrix of expression intensities or null if no intensities are available
     */
    @Nullable
    ExpressionDataDoubleMatrix getIntensities( ExpressionExperiment ee );
}
