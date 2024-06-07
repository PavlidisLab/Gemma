/*
 * The Gemma project
 *
 * Copyright (c) 2012 University of British Columbia
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
package ubic.gemma.persistence.service.expression.experiment;

import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.persister.ArrayDesignsForExperimentCache;

/**
 * Sets up the array designs before saving an experiment.
 *
 * @author paul
 */
public interface ExpressionExperimentPrePersistService {

    /**
     * Call this before calling the persister.
     *
     * @param ee experiment
     * @return cache
     */
    ArrayDesignsForExperimentCache prepare( ExpressionExperiment ee );

    /**
     * @param c  A cache that is already possibly partly populated.
     * @param ee experiment
     * @return cache
     */
    ArrayDesignsForExperimentCache prepare( ExpressionExperiment ee, ArrayDesignsForExperimentCache c );

}