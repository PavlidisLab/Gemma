/*
 * The Gemma project
 * 
 * Copyright (c) 2011 University of British Columbia
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

import java.util.Collection;
import java.util.Map;

import ubic.gemma.model.common.auditAndSecurity.AuditEvent;
import ubic.gemma.model.common.auditAndSecurity.AuditTrailService;
import ubic.gemma.model.common.auditAndSecurity.StatusService;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;
import ubic.gemma.util.AbstractSpringAwareCLI;
import ubic.gemma.util.EntityUtils;

/**
 * TODO Document Me
 * 
 * @author paul
 * @version $Id$
 */
public class StatusPopulatorCli extends AbstractSpringAwareCLI {

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.util.AbstractCLI#buildOptions()
     */
    @Override
    protected void buildOptions() {
        // nothing do to
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.util.AbstractCLI#doWork(java.lang.String[])
     */
    @Override
    protected Exception doWork( String[] args ) {

        processCommandLine( "Populate status", args );

        ExpressionExperimentService eeService = ( ExpressionExperimentService ) this
                .getBean( "expressionExperimentService" );
        ArrayDesignService adService = ( ArrayDesignService ) this.getBean( "arrayDesignService" );
        StatusService statusService = ( StatusService ) this.getBean( "statusService" );
        AuditTrailService atService = ( AuditTrailService ) this.getBean( "auditTrailService" );

        /*
         * Experiments trouble
         */
        Collection<ExpressionExperiment> ees = eeService.loadAll();
        Map<Long, ExpressionExperiment> eeidmap = EntityUtils.getIdMap( ees );
        Map<Long, AuditEvent> eeTroubles = eeService.getLastTroubleEvent( EntityUtils.getIds( ees ) );

        for ( Long id : eeTroubles.keySet() ) {
            ExpressionExperiment ee = eeidmap.get( id );
            ee.getStatus().setTroubled( true );
            statusService.update( ee.getStatus() );
        }

        /*
         * Experiments validated
         */
        Map<Long, AuditEvent> eeValidations = eeService.getLastValidationEvent( EntityUtils.getIds( ees ) );
        for ( Long id : eeValidations.keySet() ) {
            eeidmap.get( id ).getStatus().setValidated( true );
            statusService.update( eeidmap.get( id ).getStatus() );
        }

        /*
         * Array designs trouble
         */
        Collection<ArrayDesign> ads = adService.loadAll();
        Map<Long, AuditEvent> adTroubles = adService.getLastTroubleEvent( EntityUtils.getIds( ads ) );
        Map<Long, ArrayDesign> adidmap = EntityUtils.getIdMap( ads );
        for ( Long id : adTroubles.keySet() ) {
            adidmap.get( id ).getStatus().setTroubled( true );
            statusService.update( adidmap.get( id ).getStatus() );
        }

        /*
         * Last updates for everything
         */

        return null;
    }

    public static void main( String[] args ) {
        StatusPopulatorCli c = new StatusPopulatorCli();
        c.doWork( args );
    }

}
