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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.security.Authentication;
import org.springframework.security.context.SecurityContextHolder;
import org.springframework.security.userdetails.User;

import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;
import ubic.gemma.security.RunAsManager;

/**
 * An service to provide security sepecific actions on {@link ExpressionExperiment}s.
 * <p>
 * The global security provided by Spring Security still applies here, but there are certain security related tasks
 * (such as filtering out public objects for a user) that cannot be done by Spring Security directly so we do it here.
 * 
 * @author keshav
 * @version $Id$
 */
public class ExpressionExperimentSecureService {

    private Log log = LogFactory.getLog( this.getClass() );

    ExpressionExperimentService expressionExperimentService = null;

    public void setExpressionExperimentService( ExpressionExperimentService expressionExperimentService ) {
        this.expressionExperimentService = expressionExperimentService;
    }

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
    public Collection<ExpressionExperiment> loadExpressionExperimentsForAnotherUser( User u ) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        RunAsManager r = new RunAsManager();

        Authentication runAs = r.buildRunAs( null, auth, u.getUsername() );

        if ( runAs == null ) {
            log.debug( "RunAsManager did not change Authentication object" );
        } else {
            log.debug( "Switching to RunAs Authentication: " + runAs );
            SecurityContextHolder.getContext().setAuthentication( runAs );
        }
        Collection<ExpressionExperiment> eeCol = expressionExperimentService.loadAll();
        SecurityContextHolder.getContext().setAuthentication( auth );
        return eeCol;
    }

}
