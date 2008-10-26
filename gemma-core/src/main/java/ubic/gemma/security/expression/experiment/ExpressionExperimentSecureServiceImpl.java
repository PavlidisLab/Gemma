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

import ubic.gemma.model.common.AuditableServiceImpl;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentDao;
import ubic.gemma.security.RunAsManager;

/**
 * @author keshav
 * @version $Id$
 * @see ExpressionExperimentSecureService
 */
public class ExpressionExperimentSecureServiceImpl extends AuditableServiceImpl implements
        ExpressionExperimentSecureService {

    private Log log = LogFactory.getLog( this.getClass() );

    ExpressionExperimentDao expressionExperimentDao = null;

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.security.expression.experiment.ExpressionExperimentSecureService#loadExpressionExperimentsForUser()
     */
    public Collection<ExpressionExperiment> loadExpressionExperimentsForUser() {
        return expressionExperimentDao.loadAll();
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.security.expression.experiment.ExpressionExperimentSecureService#loadExpressionExperimentsForAnotherUser(org.springframework.security.userdetails.User)
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
        Collection<ExpressionExperiment> eeCol = expressionExperimentDao.loadAll();
        SecurityContextHolder.getContext().setAuthentication( auth );
        return eeCol;
    }

    /**
     * @param expressionExperimentDao
     */
    public void setExpressionExperimentDao( ExpressionExperimentDao expressionExperimentDao ) {
        this.expressionExperimentDao = expressionExperimentDao;
    }
}
