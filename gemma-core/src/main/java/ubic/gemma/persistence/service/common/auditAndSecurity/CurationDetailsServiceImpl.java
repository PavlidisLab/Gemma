/*
 * The Gemma project
 *
 * Copyright (c) 2011 University of British Columbia
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
package ubic.gemma.persistence.service.common.auditAndSecurity;

import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.model.common.auditAndSecurity.AuditAction;
import ubic.gemma.model.common.auditAndSecurity.AuditEvent;
import ubic.gemma.model.common.auditAndSecurity.curation.Curatable;
import ubic.gemma.model.common.auditAndSecurity.curation.CurationDetails;
import ubic.gemma.model.common.auditAndSecurity.eventType.CurationDetailsEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.NotTroubledStatusFlagEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.TroubledStatusFlagEvent;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentDao;

@Service
@CommonsLog
public class CurationDetailsServiceImpl implements CurationDetailsService {

    @Autowired
    private CurationDetailsDao curationDetailsDao;

    @Autowired
    private ExpressionExperimentDao expressionExperimentDao;

    @Override
    @Transactional
    public void updateCurationDetailsFromAuditEvent( Curatable curatable, AuditEvent auditEvent ) {
        if ( curatable.getId() == null ) {
            throw new IllegalArgumentException( "Cannot update curation details for a transient entity." );
        }

        if ( auditEvent.getAction() != AuditAction.UPDATE ) {
            throw new IllegalArgumentException( "Only update audit action can be used to update curation details." );
        }

        CurationDetails curationDetails = curatable.getCurationDetails();

        // Update the lastUpdated property to match the event date
        curationDetails.setLastUpdated( auditEvent.getDate() );

        // Update other curationDetails properties, if the event updates them.
        if ( auditEvent.getEventType() != null
                && CurationDetailsEvent.class.isAssignableFrom( auditEvent.getEventType().getClass() ) ) {
            CurationDetailsEvent eventType = ( CurationDetailsEvent ) auditEvent.getEventType();
            eventType.updateCurationDetails( curationDetails, auditEvent );
        }

        /*
         * The logic below addresses the special relationship between ArrayDesigns and ExpressionExperiments.
         * To avoid us having to "reach through" to the ArrayDesign to check whether an Experiment is troubled,
         * the troubled status of the ArrayDesign affects the Troubled status of the Experiment. This denormlization
         * saves joins when querying troubled status of experiments.
         */

        if ( curatable instanceof ArrayDesign ) {
            updateArrayDesign( ( ArrayDesign ) curatable, auditEvent );
        }

        if ( curatable instanceof ExpressionExperiment ) {
            updateExpressionExperiment( ( ExpressionExperiment ) curatable, auditEvent );
        }

        curatable.setCurationDetails( curationDetailsDao.save( curationDetails ) );
    }

    /**
     * If we're updating an ArrayDesign, and this is a trouble event, update the associated experiments.
     */
    private void updateArrayDesign( ArrayDesign curatable, AuditEvent auditEvent ) {
        if ( isTroubledEvent( auditEvent ) ) {
            expressionExperimentDao.updateTroubledByArrayDesign( curatable, true );
        } else if ( isNotTroubledEvent( auditEvent ) ) {
            /*
             * unset the trouble status for all the experiments; be careful not to do this if
             * the experiment is troubled independently of the array design.
             */
            expressionExperimentDao.updateTroubledByArrayDesign( curatable, false );
        }
    }

    /**
     * If we're marking an experiment as non-troubled, but it still uses a troubled platform, restore the troubled
     * status.
     */
    private void updateExpressionExperiment( ExpressionExperiment ee, AuditEvent auditEvent ) {
        if ( isNotTroubledEvent( auditEvent ) && expressionExperimentDao.countTroubledPlatforms( ee ) > 0 ) {
            ee.getCurationDetails().setTroubled( true );
        }
    }

    private static boolean isTroubledEvent( AuditEvent auditEvent ) {
        return auditEvent.getEventType() instanceof TroubledStatusFlagEvent;
    }

    private static boolean isNotTroubledEvent( AuditEvent auditEvent ) {
        return auditEvent.getEventType() instanceof NotTroubledStatusFlagEvent;
    }
}