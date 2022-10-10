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
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.BaseService;

import javax.annotation.Nullable;
import java.util.Collection;

/**
 * @author kelsey
 */
public interface ExperimentalDesignService extends BaseService<ExperimentalDesign> {

    @Override
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_READ" })
    ExperimentalDesign find( ExperimentalDesign experimentalDesign );

    @Override
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_READ" })
    ExperimentalDesign load( @Nullable Long id );

    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_READ" })
    ExperimentalDesign loadWithExperimentalFactors( Long id );

    @Override
    @Secured({ "GROUP_ADMIN" })
    Collection<ExperimentalDesign> loadAll();

    @Override
    @Secured({ "GROUP_USER", "ACL_SECURABLE_EDIT" })
    void update( Collection<ExperimentalDesign> entities );

    @Override
    @Secured({ "GROUP_USER", "ACL_SECURABLE_EDIT" })
    void update( ExperimentalDesign experimentalDesign );

    /**
     * Gets the expression experiment for the specified experimental design object
     *
     * @param experimentalDesign experimental design
     * @return experiment the given design belongs to
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_READ" })
    ExpressionExperiment getExpressionExperiment( ExperimentalDesign experimentalDesign );
}
