/*
 * The Gemma project
 *
 * Copyright (c) 2012 University of British Columbia
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
package ubic.gemma.core.loader.expression;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.model.common.auditAndSecurity.eventType.AuditEventType;
import ubic.gemma.model.common.auditAndSecurity.eventType.ExpressionExperimentPlatformSwitchEvent;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.bioAssayData.RawExpressionDataVector;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.common.auditAndSecurity.AuditTrailService;
import ubic.gemma.persistence.service.expression.bioAssayData.RawExpressionDataVectorService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;

/**
 * @author Paul
 */
@Service
public class ExperimentPlatformSwitchHelperServiceImpl implements ExperimentPlatformSwitchHelperService {
    private static final Log log = LogFactory.getLog( ExperimentPlatformSwitchHelperServiceImpl.class );
    @Autowired
    private AuditTrailService auditTrailService;

    @Autowired
    private ExpressionExperimentService expressionExperimentService;

    @Autowired
    private RawExpressionDataVectorService rawVectorService;

    @Override
    @Transactional
    public void persist( ExpressionExperiment ee, ArrayDesign arrayDesign ) {
        expressionExperimentService.update( ee );
        rawVectorService.update( ee.getRawExpressionDataVectors() ); // no processed vectors at this point.

        // a redundant check, but there have been problems.
        for ( RawExpressionDataVector v : ee.getRawExpressionDataVectors() ) {
            if ( !arrayDesign.equals( v.getDesignElement().getArrayDesign() ) ) {
                throw new IllegalStateException( "A raw vector for QT =" + v.getQuantitationType()
                        + " was not correctly switched to the target platform " + arrayDesign );
            }
        }

        this.audit( ee, "Switch to use " + arrayDesign.getShortName() );

        ExperimentPlatformSwitchHelperServiceImpl.log
                .info( "Completing switching " + ee ); // flush of transaction happens after this, can take a while.
    }

    private void audit( ExpressionExperiment ee, String note ) {
        auditTrailService.addUpdateEvent( ee, ExpressionExperimentPlatformSwitchEvent.class, note );
    }

}
