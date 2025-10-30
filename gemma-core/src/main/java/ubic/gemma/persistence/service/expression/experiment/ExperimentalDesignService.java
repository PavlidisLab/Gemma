/*
 * The Gemma project.
 *
 * Copyright (c) 2006-2007 University of British Columbia
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
import ubic.gemma.model.expression.experiment.ExperimentalDesign;
import ubic.gemma.persistence.service.common.auditAndSecurity.SecurableBaseService;

import javax.annotation.Nullable;

/**
 * @author kelsey
 */
public interface ExperimentalDesignService extends SecurableBaseService<ExperimentalDesign> {

    /**
     * Load an experimental design with its experimental factors initialized.
     */
    @Nullable
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_READ_QUIET" })
    ExperimentalDesign loadWithExperimentalFactors( Long id );

    /**
     * Obtain a random experimental design that needs attention.
     * <p>
     * This operation is reserved to administrators.
     * @see ExperimentalDesignDao#getRandomExperimentalDesignThatNeedsAttention(ExperimentalDesign)
     */
    @Nullable
    @Secured({ "GROUP_ADMIN" })
    ExperimentalDesign getRandomExperimentalDesignThatNeedsAttention( ExperimentalDesign excludeDesign );
}
