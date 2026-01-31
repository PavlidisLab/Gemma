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

import org.springframework.security.access.annotation.Secured;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.Geeq;
import ubic.gemma.model.expression.experiment.GeeqValueObject;
import ubic.gemma.persistence.service.BaseVoEnabledService;

public interface GeeqService extends BaseVoEnabledService<Geeq, GeeqValueObject> {

    /**
     * Modes for filling GEEQ scores.
     * <p>
     * These are in lowercase as they used to be identified by name this way with string constants.
     */
    enum ScoreMode {
        all,
        batch,
        reps,
        pub
    }

    /**
     * Calculates the GEEQ score in the given mode for the experiment with the given id.
     *
     * @param ee   the id of the experiment to calculate the scores for.
     * @param mode either run all scores, or only re-score batch effect, batch confound or replicates.
     */
    @Secured({ "GROUP_USER", "ACL_SECURABLE_EDIT" })
    Geeq calculateScore( ExpressionExperiment ee, ScoreMode mode );
}
