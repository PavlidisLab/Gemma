package ubic.gemma.rest;

import io.swagger.v3.oas.models.OpenAPI;
import org.junit.After;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.AccessDecisionManager;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.context.support.WithSecurityContextTestExecutionListener;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import ubic.gemma.core.context.TestComponent;
import ubic.gemma.core.job.SubmittedTask;
import ubic.gemma.core.job.TaskCommand;
import ubic.gemma.core.job.TaskRunningService;
import ubic.gemma.core.tasks.analysis.expression.PreprocessTaskCommand;
import ubic.gemma.core.util.BuildInfo;
import ubic.gemma.core.util.test.TestPropertyPlaceholderConfigurer;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.rest.analytics.AnalyticsProvider;
import ubic.gemma.rest.util.BaseJerseyTest;
import ubic.gemma.rest.util.JacksonConfig;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Collections;
import java.util.Date;
import java.util.concurrent.Future;

import static org.apache.commons.lang3.concurrent.ConcurrentUtils.constantFuture;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static ubic.gemma.rest.util.Assertions.assertThat;

@ContextConfiguration
@TestExecutionListeners({ WithSecurityContextTestExecutionListener.class })
public class TasksWebServiceTest extends BaseJerseyTest {

    @Configuration
    @TestComponent
    @Import(JacksonConfig.class)
    public static class TasksWebServiceContextConfiguration {

        @Bean
        public static TestPropertyPlaceholderConfigurer placeholderConfigurer() {
            return new TestPropertyPlaceholderConfigurer( "gemma.hosturl=http://localhost:8080" );
        }

        @Bean
        public TaskRunningService taskRunningService() {
            return mock( TaskRunningService.class );
        }

        @Bean
        public TasksWebService tasksWebService() {
            return new TasksWebService();
        }

        @Bean
        public AnalyticsProvider analyticsProvider() {
            return mock();
        }

        @Bean
        public AccessDecisionManager accessDecisionManager() {
            return mock();
        }

        @Bean
        public Future<OpenAPI> openApi() {
            return constantFuture( mock() );
        }

        @Bean
        public BuildInfo buildInfo() {
            return mock();
        }
    }

    @Autowired
    private TaskRunningService taskRunningService;

    @After
    public void resetMocks() {
        reset( taskRunningService );
    }

    @Test
    @WithMockUser(authorities = "GROUP_ADMIN")
    public void testGetTaskStatus() {
        Date submittedAt = new Date( 1_700_000_000_000L );
        Date startedAt = new Date( 1_700_000_001_000L );
        SubmittedTask task = mock( SubmittedTask.class );
        when( task.getTaskId() ).thenReturn( "abc-123" );
        when( task.getStatus() ).thenReturn( SubmittedTask.Status.RUNNING );
        when( task.getSubmissionTime() ).thenReturn( submittedAt );
        when( task.getStartTime() ).thenReturn( startedAt );
        when( task.getFinishTime() ).thenReturn( null );
        when( task.getLastProgressUpdates() ).thenReturn( "step 2/5" );
        when( task.getProgressUpdates() ).thenReturn( null );
        // taskCommand wired to a known command type so step + experimentId get populated
        ExpressionExperiment ee = ExpressionExperiment.Factory.newInstance();
        ee.setId( 42L );
        TaskCommand cmd = new PreprocessTaskCommand( ee );
        when( task.getTaskCommand() ).thenReturn( cmd );

        when( taskRunningService.getSubmittedTask( "abc-123" ) ).thenReturn( task );

        assertThat( target( "/tasks/abc-123" ).request().get() )
                .hasStatus( Response.Status.OK )
                .hasMediaTypeCompatibleWith( MediaType.APPLICATION_JSON_TYPE )
                .entity()
                .hasFieldOrPropertyWithValue( "data.taskId", "abc-123" )
                .hasFieldOrPropertyWithValue( "data.status", "running" )
                .hasFieldOrPropertyWithValue( "data.message", "step 2/5" )
                .hasFieldOrPropertyWithValue( "data.experimentId", 42 )
                .hasFieldOrPropertyWithValue( "data.step", "preprocess" );

        verify( taskRunningService ).getSubmittedTask( "abc-123" );
    }

    @Test
    @WithMockUser(authorities = "GROUP_ADMIN")
    public void testGetTaskStatusForCompletedTask() {
        SubmittedTask task = mock( SubmittedTask.class );
        when( task.getTaskId() ).thenReturn( "abc-123" );
        when( task.getStatus() ).thenReturn( SubmittedTask.Status.COMPLETED );
        when( task.getLastProgressUpdates() ).thenReturn( "" );
        when( task.getProgressUpdates() ).thenReturn( null );
        when( taskRunningService.getSubmittedTask( "abc-123" ) ).thenReturn( task );

        assertThat( target( "/tasks/abc-123" ).request().get() )
                .hasStatus( Response.Status.OK )
                .entity()
                .hasFieldOrPropertyWithValue( "data.status", "completed" );
    }

    @Test
    @WithMockUser(authorities = "GROUP_ADMIN")
    public void testGetTaskStatusFallsBackToProgressQueueWhenLastIsEmpty() {
        SubmittedTask task = mock( SubmittedTask.class );
        when( task.getTaskId() ).thenReturn( "abc-123" );
        when( task.getStatus() ).thenReturn( SubmittedTask.Status.RUNNING );
        when( task.getLastProgressUpdates() ).thenReturn( "" );
        java.util.Queue<String> updates = new java.util.LinkedList<>();
        updates.add( "first" );
        updates.add( "last" );
        when( task.getProgressUpdates() ).thenReturn( updates );
        when( taskRunningService.getSubmittedTask( "abc-123" ) ).thenReturn( task );

        assertThat( target( "/tasks/abc-123" ).request().get() )
                .hasStatus( Response.Status.OK )
                .entity()
                .hasFieldOrPropertyWithValue( "data.message", "last" );
    }

    @Test
    @WithMockUser(authorities = "GROUP_ADMIN")
    public void testGetTaskStatusReportsUnknownWhenStatusIsNull() {
        SubmittedTask task = mock( SubmittedTask.class );
        when( task.getTaskId() ).thenReturn( "abc-123" );
        when( task.getStatus() ).thenReturn( null );
        when( task.getLastProgressUpdates() ).thenReturn( "" );
        when( task.getProgressUpdates() ).thenReturn( new java.util.LinkedList<>() );
        when( taskRunningService.getSubmittedTask( "abc-123" ) ).thenReturn( task );

        assertThat( target( "/tasks/abc-123" ).request().get() )
                .hasStatus( Response.Status.OK )
                .entity()
                .hasFieldOrPropertyWithValue( "data.status", "unknown" );
    }

    @Test
    @WithMockUser(authorities = "GROUP_ADMIN")
    public void testGetTaskStatusWithUnknownTaskIdIs404() {
        when( taskRunningService.getSubmittedTask( "evicted-task" ) ).thenReturn( null );

        assertThat( target( "/tasks/evicted-task" ).request().get() )
                .hasStatus( Response.Status.NOT_FOUND )
                .hasMediaTypeCompatibleWith( MediaType.APPLICATION_JSON_TYPE );
    }
}
