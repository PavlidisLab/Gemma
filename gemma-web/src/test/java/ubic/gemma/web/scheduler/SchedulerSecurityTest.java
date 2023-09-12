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
package ubic.gemma.web.scheduler;

import gemma.gsec.authentication.ManualAuthenticationService;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ubic.gemma.core.analysis.report.WhatsNewService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.web.util.BaseSpringWebTest;

import java.lang.reflect.InvocationTargetException;

/**
 * Tests security of methods run by Quartz.
 *
 * @author keshav
 */
public class SchedulerSecurityTest extends BaseSpringWebTest {

    @Autowired
    private ExpressionExperimentService expressionExperimentService;

    @Autowired
    private WhatsNewService whatsNewService;

    @Autowired
    private ManualAuthenticationService manualAuthenticationService;

    /*
     * Tests whether we can run a secured method that has been granted to GROUP_AGENT
     *
     */
    @Test
    public void runSecuredMethodOnSchedule() throws Exception {

        String jobName = "job_" + RandomStringUtils.randomAlphabetic( 10 );

        SecureMethodInvokingJobDetailFactoryBean jobDetail = new SecureMethodInvokingJobDetailFactoryBean();
        jobDetail.setTargetMethod( "generateWeeklyReport" );
        jobDetail.setTargetObject( whatsNewService ); // access should be ok for GROUP_AGENT.
        jobDetail.setConcurrent( false );
        jobDetail.setBeanName( jobName );
        jobDetail.afterPropertiesSet(); // needed when we do this programatically.
        jobDetail.setManualAuthenticationService( this.manualAuthenticationService );

        jobDetail.invoke();

    }

    /*
     * Tests whether we can run a secured method that has been granted to both GROUP_AGENT _and_ GROUP_USER.
     *
     */
    @Test
    public void runSecuredMethodOnScheduleMultiGroup() throws Exception {

        String jobName = "job_" + RandomStringUtils.randomAlphabetic( 10 );

        SecureMethodInvokingJobDetailFactoryBean jobDetail = new SecureMethodInvokingJobDetailFactoryBean();
        jobDetail.setTargetMethod( "findByUpdatedLimit" );
        jobDetail.setArguments( new Object[] { 10 } );
        jobDetail.setTargetObject( expressionExperimentService ); // access should be ok for GROUP_AGENT.
        jobDetail.setConcurrent( false );
        jobDetail.setBeanName( jobName );
        jobDetail.afterPropertiesSet(); // needed when we do this programatically.
        jobDetail.setManualAuthenticationService( this.manualAuthenticationService );

        jobDetail.invoke();

    }

    /*
     * Confirm that we can't run methods that GROUP_AGENT doesn't have access to, namely deleting experiments.
     *
     */
    @Test(expected = InvocationTargetException.class)
    public void runUnauthorizedMethodOnSchedule() throws Exception {

        String jobName = "testJobDetail";

        /*
         * Mimics configuration in xml.
         */
        SecureMethodInvokingJobDetailFactoryBean jobDetail = new SecureMethodInvokingJobDetailFactoryBean();
        jobDetail.setTargetMethod( "remove" );
        jobDetail.setArguments( new Object[] { null } );
        jobDetail.setTargetObject( expressionExperimentService ); // no access
        jobDetail.setConcurrent( false );
        jobDetail.setBeanName( jobName );
        jobDetail.afterPropertiesSet(); // needed when we do this programatically.
        jobDetail.setManualAuthenticationService( this.manualAuthenticationService );
        jobDetail.invoke();

    }

}
