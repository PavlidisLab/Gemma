/*
 * The Gemma project
 * 
 * Copyright (c) 2008 University of British Columbia
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
package ubic.gemma.security.expression.experiment;

import java.util.Collection;

import org.springframework.security.userdetails.User;

import ubic.gemma.model.common.AuditableService;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

/**
 * * An service to provide security sepecific actions on {@link ExpressionExperiment}s.
 * <p>
 * The global security provided by Spring Security still applies here, but there are certain security related tasks
 * (such as filtering out public objects for a user) that cannot be done by Spring Security directly so we do it here.
 * 
 * @author keshav
 * @version $Id$
 */
public interface ExpressionExperimentSecureService extends AuditableService {

    /**
     * Returns the {@link ExpressionExperiment}s for the currently logged in {@link User}.
     * <p>
     * This method does not completely abstract away security.
     * <p>
     * see AclAfterCollectionPublicExpressionExperimentFilter for processConfigAttribute.
     * <p>
     * 
     * @return
     */
    public Collection<ExpressionExperiment> loadExpressionExperimentsForUser();

    /**
     * Returns the {@link ExpressionExperiment}s for the {@link User} u. This would generally be used by an
     * administrator to see the {@link ExpressionExperiment}s for a specific {@link User} u.
     * <p>
     * This method does not completely abstract away security.
     * <p>
     * see AclAfterCollectionPublicExpressionExperimentFilter for processConfigAttribute.
     * <p>
     * 
     * @param u
     * @return
     */
    public Collection<ExpressionExperiment> loadExpressionExperimentsForAnotherUser( User u );

}
