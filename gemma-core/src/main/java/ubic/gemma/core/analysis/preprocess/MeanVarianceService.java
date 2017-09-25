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
package ubic.gemma.core.analysis.preprocess;

import org.springframework.security.access.annotation.Secured;
import ubic.gemma.model.expression.bioAssayData.MeanVarianceRelation;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

/**
 * Responsible for returning the coordinates of the experiment's Mean-Variance relationship.
 *
 * @author ptan
 */
public interface MeanVarianceService {

    /**
     * Retrieve (and if necessary compute) the mean-variance relationship for the experiment
     *
     * @return MeanVarianceRelation
     */
    @Secured({ "GROUP_USER" })
    MeanVarianceRelation create( ExpressionExperiment ee, boolean forceRecompute );

    /**
     * Retrieve existing mean-variance relation. Returns null if it does not already exist.
     */
    MeanVarianceRelation find( ExpressionExperiment ee );

    /**
     * Creates the matrix, or loads
     *
     * @return MeanVarianceRelation
     */
    @Secured({ "GROUP_USER", "ACL_SECURABLE_READ" })
    MeanVarianceRelation findOrCreate( ExpressionExperiment ee );

    /**
     * @return true if the specified experiment already has a MeanVarianceRelation computed
     */
    boolean hasMeanVariance( ExpressionExperiment ee );

}
