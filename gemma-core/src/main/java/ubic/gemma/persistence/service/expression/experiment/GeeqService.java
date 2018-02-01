/*
 * The gemma project
 *
 * Copyright (c) 2018 University of British Columbia
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

import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.Geeq;
import ubic.gemma.model.expression.experiment.GeeqValueObject;
import ubic.gemma.persistence.service.BaseVoEnabledService;

public interface GeeqService extends BaseVoEnabledService<Geeq, GeeqValueObject> {

    /**
     * Resets the manual override of batch confound to false and recalculates the score.
     * @param eeId the id of the experiment to do this for.
     * @return the updated experiment
     */
    ExpressionExperiment resetBatchConfound( Long eeId );

    /**
     * Resets the manual override of batch effect to false and recalculates the score.
     * @param eeId the id of the experiment to do this for.
     * @return the updated experiment
     */
    ExpressionExperiment resetBatchEffect( Long eeId );

    /**
     * Calculates the GEEQ scores for the experiment with the given id.
     *
     * @param eeId the id of the experiment to calculate the scores for.
     * @return the updated experiment.
     */
    ExpressionExperiment calculateScore( Long eeId );

    /**
     * Reads manual override info from the given GEEQ Value Object and stores them with the experiment
     *
     * @param gqVo a GEEQ Value Object containing the necessary information.
     * @param eeId the id of the experiment to update the geeq information for.
     * @return the updated experiment.
     */
    ExpressionExperiment setManualOverrides( Long eeId, GeeqValueObject gqVo );

}
