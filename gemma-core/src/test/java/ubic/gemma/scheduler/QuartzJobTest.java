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

import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerFactory;
import org.quartz.Trigger;
import org.quartz.TriggerUtils;

import ubic.gemma.testing.BaseSpringContextTest;

/**
 * A class used to test Quartz functionality.
 * 
 * @author keshav
 * @version $Id$
 */
public class QuartzJobTest extends BaseSpringContextTest {

    /**
     * Tests executing the job ExpressionExperimentIndexerJob
     */
    public void testExecuteInExpressionExperimentIndexerJob() {

        SchedulerFactory schedulerFactory = new org.quartz.impl.StdSchedulerFactory();

        Scheduler sched;

        boolean fail = false;
        try {
            sched = schedulerFactory.getScheduler();
            sched.start();

            JobDetail jobDetail = new JobDetail( "ExpressionExperimentIndexerJob", // job name
                    null, // job group (you can also specify 'null' to use the default group)
                    ExpressionExperimentIndexerJob.class ); // the java class to execute

            Trigger trigger = TriggerUtils.makeImmediateTrigger( 0, 0 );
            // trigger.setStartTime( new Date() );
            trigger.setName( "Trigger Created In " + this.getClass().getName() );

            sched.scheduleJob( jobDetail, trigger );

        } catch ( Exception e ) {
            fail = true;
            e.printStackTrace();
        } finally {
            assertFalse( fail );
        }
    }

}
