/*
 * The gemma-core project
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

package ubic.gemma.core.analysis.preprocess;

import org.springframework.security.access.annotation.Secured;
import ubic.gemma.model.analysis.expression.ExpressionExperimentSet;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

/**
 * TODO Document Me
 *
 * @author paul
 */
public interface SplitExperimentService {

    /**
     * Split an experiment into multiple experiments based on a factor. The new experiments will automatically be given
     * short names to suit and the names will be appended with an indicator of the split.
     *
     * @param expressionExperiment     the experiment to split
     * @param splitOn                  the factor to split the experiment on
     * @param postProcess              post-process the experiments resulting from the split
     * @param deleteOriginalExperiment whether to delete the original experiment after splitting, otherwise it will only
     *                                 be marked as private
     * @return results of the split
     */
    @Secured({ "GROUP_ADMIN", "ACL_SECURABLE_EDIT" })
    ExpressionExperimentSet split( ExpressionExperiment expressionExperiment, ExperimentalFactor splitOn, boolean postProcess, boolean deleteOriginalExperiment );
}
