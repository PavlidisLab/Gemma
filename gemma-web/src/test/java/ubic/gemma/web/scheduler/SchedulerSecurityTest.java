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

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Test;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import ubic.gemma.core.analysis.report.WhatsNewService;
import ubic.gemma.persistence.service.TableMaintenanceUtil;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.web.util.BaseWebIntegrationTest;

import java.lang.reflect.InvocationTargetException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

/**
 * Tests security of methods run by Quartz.
 *
 * @author keshav
 */
public class SchedulerSecurityTest extends BaseWebIntegrationTest {

    @Autowired
    private ExpressionExperimentService expressionExperimentService;

    @Autowired
    private WhatsNewService whatsNewService;

    @Autowired
    private TableMaintenanceUtil tableMaintenanceUtil;

    @Autowired
    @Qualifier("groupAgentSecurityContext")
    private SecurityContext securityContext;

    /*
     * Tests whether we can run a secured method that has been granted to GROUP_AGENT
     *
     */
    @Test
    public void runSecuredMethodOnSchedule() throws Exception {

        String jobName = "job_" + RandomStringUtils.randomAlphabetic( 10 );

        SecureMethodInvokingJobDetailFactoryBean jobDetail = new SecureMethodInvokingJobDetailFactoryBean( this.securityContext );
        jobDetail.setTargetMethod( "generateWeeklyReport" );
        jobDetail.setTargetObject( whatsNewService ); // access should be ok for GROUP_AGENT.
        jobDetail.setConcurrent( false );
        jobDetail.setBeanName( jobName );
        jobDetail.afterPropertiesSet(); // needed when we do this programatically.

        jobDetail.invoke();

    }

    /*
     * Tests whether we can run a secured method that has been granted to both GROUP_AGENT _and_ GROUP_USER.
     *
     */
    @Test
    public void runSecuredMethodOnScheduleMultiGroup() throws Exception {

        String jobName = "job_" + RandomStringUtils.randomAlphabetic( 10 );

        SecureMethodInvokingJobDetailFactoryBean jobDetail = new SecureMethodInvokingJobDetailFactoryBean( this.securityContext );
        jobDetail.setTargetMethod( "findByUpdatedLimit" );
        jobDetail.setArguments( new Object[] { 10 } );
        jobDetail.setTargetObject( expressionExperimentService ); // access should be ok for GROUP_AGENT.
        jobDetail.setConcurrent( false );
        jobDetail.setBeanName( jobName );
        jobDetail.afterPropertiesSet(); // needed when we do this programatically.

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
        SecureMethodInvokingJobDetailFactoryBean jobDetail = new SecureMethodInvokingJobDetailFactoryBean( this.securityContext );
        jobDetail.setTargetMethod( "remove" );
        jobDetail.setArguments( new Object[] { null } );
        jobDetail.setTargetObject( expressionExperimentService ); // no access
        jobDetail.setConcurrent( false );
        jobDetail.setBeanName( jobName );
        jobDetail.afterPropertiesSet(); // needed when we do this programatically.
        jobDetail.invoke();

    }

    @Component
    public static class TestSecureJob extends SecureQuartzJobBean {

        private TableMaintenanceUtil tableMaintenanceUtil;

        @Override
        protected void executeAs( JobExecutionContext context ) {
            assertNotNull( tableMaintenanceUtil );
            assertNotNull( SecurityContextHolder.getContext().getAuthentication() );
            assertTrue( SecurityContextHolder.getContext().getAuthentication().isAuthenticated() );
            assertTrue( SecurityContextHolder.getContext().getAuthentication().getAuthorities()
                    .contains( new SimpleGrantedAuthority( "GROUP_AGENT" ) ) );
            context.setResult( "Hello world!" );
        }

        @SuppressWarnings("unused")
        public void setTableMaintenanceUtil( TableMaintenanceUtil tableMaintenanceUtil ) {
            this.tableMaintenanceUtil = tableMaintenanceUtil;
        }
    }

    @Autowired
    private TestSecureJob testSecureJob;

    @Test
    public void testSecureJob() throws JobExecutionException {
        JobExecutionContext context = mock();
        JobDataMap jdm = new JobDataMap();
        jdm.put( "tableMaintenanceUtil", tableMaintenanceUtil );
        jdm.put( "securityContext", securityContext );
        when( context.getScheduler() ).thenReturn( mock() );
        when( context.getMergedJobDataMap() ).thenReturn( jdm );
        SecurityContext previousContext = SecurityContextHolder.getContext();
        testSecureJob.execute( context );
        assertThat( SecurityContextHolder.getContext() ).isSameAs( previousContext );
        verify( context ).setResult( "Hello world!" );
    }
}
