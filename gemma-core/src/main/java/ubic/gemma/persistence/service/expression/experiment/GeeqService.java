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

import ubic.gemma.model.expression.experiment.Geeq;
import ubic.gemma.model.expression.experiment.GeeqAdminValueObject;
import ubic.gemma.model.expression.experiment.GeeqValueObject;
import ubic.gemma.persistence.service.BaseVoEnabledService;

public interface GeeqService extends BaseVoEnabledService<Geeq, GeeqValueObject> {
    String OPT_MODE_ALL = "all";
    String OPT_MODE_BATCH = "batch";
    String OPT_MODE_REPS = "reps";
    String OPT_MODE_PUB = "pub";

    /**
     * Calculates the GEEQ score in the given mode for the experiment with the given id.
     *
     * @param eeId the id of the experiment to calculate the scores for.
     * @param mode either run all scores, or only re-score batch effect, batch confound or replicates.
     */
    void calculateScore( Long eeId, String mode );

    /**
     * Reads manual override info from the given GEEQ Value Object and stores them with the experiment
     *
     * @param eeId the id of the experiment to update the geeq information for.
     * @param gqVo a GEEQ Value Object containing the necessary information.
     */
    void setManualOverrides( Long eeId, GeeqAdminValueObject gqVo );

}
