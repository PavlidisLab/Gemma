/*
 * The Gemma project
 * 
 * Copyright (c) 2007 University of British Columbia
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
package ubic.gemma.apps;

import ubic.gemma.loader.expression.ExpressionExperimentPlatformSwitchService;
import ubic.gemma.model.common.auditAndSecurity.AuditTrailService;
import ubic.gemma.model.common.auditAndSecurity.eventType.AuditEventType;
import ubic.gemma.model.common.auditAndSecurity.eventType.ExpressionExperimentPlatformSwitchEvent;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

/**
 * Switch the array design used to the merged one.
 * 
 * @author pavlidis
 * @version $Id$
 */
public class ExpressionExperimentPlatformSwitchCli extends AbstractGeneExpressionExperimentManipulatingCLI {

    @Override
    protected Exception doWork( String[] args ) {

        Exception exp = processCommandLine( "Switch EE to merged Array Design", args );
        if ( exp != null ) {
            return exp;
        }

        ExpressionExperimentPlatformSwitchService serv = ( ExpressionExperimentPlatformSwitchService ) this
                .getBean( "expressionExperimentPlatformSwitchService" );

        ExpressionExperiment ee = this.locateExpressionExperiment( this.getExperimentShortName() );

        if ( ee == null ) return null;

        this.eeService.thawLite( ee );

        serv.assignArrayDesignTo( ee );

        AuditTrailService auditEventService = ( AuditTrailService ) this.getBean( "auditTrailService" );
        AuditEventType type = ExpressionExperimentPlatformSwitchEvent.Factory.newInstance();
        auditEventService.addUpdateEvent( ee, type, "Switched to use merged array Design " );

        return null;
    }

    /**
     * @param args
     */
    public static void main( String[] args ) {
        ExpressionExperimentPlatformSwitchCli p = new ExpressionExperimentPlatformSwitchCli();
        Exception e = p.doWork( args );
        if ( e != null ) {
            log.fatal( e, e );
        }
    }

}
