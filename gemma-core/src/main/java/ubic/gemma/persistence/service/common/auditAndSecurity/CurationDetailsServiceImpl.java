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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.model.common.auditAndSecurity.AuditEvent;
import ubic.gemma.model.common.auditAndSecurity.curation.Curatable;
import ubic.gemma.model.common.auditAndSecurity.curation.CurationDetails;
import ubic.gemma.model.common.auditAndSecurity.eventType.NotTroubledStatusFlagEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.TroubledStatusFlagEvent;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignDao;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentDao;

import java.util.Collection;

@Service
public class CurationDetailsServiceImpl implements CurationDetailsService {

    @Autowired
    private CurationDetailsDao curationDetailsDao;

    @Autowired
    private ArrayDesignDao arrayDesignDao;

    @Autowired
    private ExpressionExperimentDao expressionExperimentDao;

    @Override
    @Transactional
    public void update( Curatable curatable, AuditEvent auditEvent ) {
        this.curationDetailsDao.update( curatable, auditEvent );

        /*
         * The logic below addresses the special relationship between ArrayDesigns and ExpressionExperiments.
         * To avoid us having to "reach through" to the ArrayDesign to check whether an Experiment is troubled,
         * the troubled status of the ArrayDesign affects the Troubled status of the Experiment. This denormlization
         * saves joins when querying troubled status of experiments.
         */

        /*
         * If we're updating an ArrayDesign, and this is a trouble event, update the associated experiments.
         */
        if ( ArrayDesign.class.isAssignableFrom( curatable.getClass() ) ) {

            if ( TroubledStatusFlagEvent.class.isAssignableFrom( auditEvent.getEventType().getClass() ) ) {

                /*
                 * set the trouble status for all the experiments
                 */
                Collection<ExpressionExperiment> ees = arrayDesignDao
                        .getExpressionExperiments( ( ArrayDesign ) curatable );
                for ( ExpressionExperiment ee : ees ) {
                    CurationDetails curationDetails = ee.getCurationDetails();
                    curationDetails.setTroubled( true );
                    curationDetailsDao.update( curationDetails );
                }

            } else if ( NotTroubledStatusFlagEvent.class.isAssignableFrom( auditEvent.getEventType().getClass() ) ) {

                /*
                 * unset the trouble status for all the experiments; be careful not to do this if
                 * the experiment is troubled independently of the array design.
                 */
                Collection<ExpressionExperiment> ees = arrayDesignDao
                        .getExpressionExperiments( ( ArrayDesign ) curatable );
                for ( ExpressionExperiment ee : ees ) {
                    CurationDetails curationDetails = ee.getCurationDetails();

                    if ( curationDetails.getLastTroubledEvent() == null ) {
                        curationDetails.setTroubled( false );
                        curationDetailsDao.update( curationDetails );
                    }
                }

            }

        }

        /*
         * If we're updating an experiment, only unset the trouble flag if all the array designs are NOT troubled.
         */
        if ( NotTroubledStatusFlagEvent.class.isAssignableFrom( auditEvent.getClass() ) && ExpressionExperiment.class
                .isAssignableFrom( curatable.getClass() ) ) {

            boolean troubledPlatform = false;
            ExpressionExperiment ee = ( ExpressionExperiment ) curatable;
            for ( ArrayDesign ad : expressionExperimentDao.getArrayDesignsUsed( ee ) ) {
                if ( ad.getCurationDetails().getTroubled() ) {
                    troubledPlatform = true;
                }
            }

            if ( !troubledPlatform ) {
                CurationDetails curationDetails = ee.getCurationDetails();
                curationDetails.setTroubled( false );
                curationDetailsDao.update( curationDetails );
            }

        }
    }

}
