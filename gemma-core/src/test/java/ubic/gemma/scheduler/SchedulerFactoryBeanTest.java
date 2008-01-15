/*
 * The Gemma project
 * 
 * Copyright (c) 2006 University of British Columbia
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
package ubic.gemma.scheduler;

import java.util.Date;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.quartz.Trigger;
import org.quartz.impl.StdScheduler;

import ubic.gemma.testing.BaseSpringContextTest;

/**
 * Tests the declarative configuration of triggers.
 * 
 * @author keshav
 * @version $Id$
 */
public class SchedulerFactoryBeanTest extends BaseSpringContextTest {
    private Log log = LogFactory.getLog( this.getClass() );

    /**
     * @throws Exception
     */
    public final void testChangeSchedule() throws Exception {
        StdScheduler scheduler = ( StdScheduler ) this.getBean( "schedulerFactoryBean" );
        boolean wasShutdown = false;
        if ( scheduler.isShutdown() ) {
            log.info( "Scheduler is shut down, cannot test it." );
            return;
        }

        String[] names = scheduler.getTriggerNames( null );
        for ( String name : names ) {
            log.info( name );
        }
        Trigger newTrigger = scheduler.getTrigger( "gene2CsUpdateTrigger", null );
        newTrigger.setStartTime( new Date() );
        scheduler.rescheduleJob( "gene2CsUpdateTrigger", null, newTrigger );

        if ( wasShutdown ) {
            scheduler.shutdown();
        } else {
            scheduler.start();
        }

    }

}
