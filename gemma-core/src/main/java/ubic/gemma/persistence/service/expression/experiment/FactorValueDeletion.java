/*
 * The Gemma project
 * 
 * Copyright (c) 2009 University of British Columbia
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 * 
 */
/*
 * The Gemma project
 * 
 * Copyright (c) 2011 University of British Columbia
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

package ubic.gemma.persistence.service.expression.experiment;

import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.FactorValue;

import java.util.Collection;
import java.util.List;

/**
 * This interface needed to be extracted for factor value deletions in
 * order to be able to make the methods transactional
 *
 * @author tvrossum
 */
public interface FactorValueDeletion {

    @Transactional
    void deleteFactorValues( Collection<Long> factorValueIds );

    /**
     * Resolve factor values of an experiment by factor and displayed value (as given by
     * {@link ubic.gemma.model.expression.experiment.FactorValueUtils#getValue(FactorValue, String)}), ignoring case.
     *
     * @param factor name or ID of a factor of the experiment's design
     * @throws IllegalArgumentException if the factor is not found, a value matches nothing, or a value matches more
     *                                  than one factor value; the message lists the candidates with their IDs
     */
    List<Long> resolveFactorValueIds( ExpressionExperiment ee, String factor, Collection<String> values );

    /**
     * Describe what {@link #deleteFactorValues(ExpressionExperiment, Collection)} would do: each factor value with the
     * number of samples it is assigned to, and the differential expression analyses deleted with it.
     *
     * @throws IllegalArgumentException if a factor value does not exist or does not belong to the experiment's design
     */
    String describeDeletion( ExpressionExperiment ee, Collection<Long> factorValueIds );

    /**
     * Delete factor values of an experiment, with every differential expression analysis that uses their factor, and
     * record an {@code ExperimentalDesignUpdatedEvent} on the experiment.
     *
     * @throws IllegalArgumentException if a factor value does not exist or does not belong to the experiment's design
     */
    void deleteFactorValues( ExpressionExperiment ee, Collection<Long> factorValueIds );

}
