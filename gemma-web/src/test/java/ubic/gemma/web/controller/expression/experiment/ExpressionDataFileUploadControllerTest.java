package ubic.gemma.web.controller.expression.experiment;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.json.JSONObject;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ubic.gemma.core.job.SubmittedTask;
import ubic.gemma.core.job.TaskResult;
import ubic.gemma.core.job.TaskRunningService;
import ubic.gemma.web.util.BaseWebIntegrationTest;

import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;
import static ubic.gemma.web.util.dwr.MockDwrRequestBuilders.dwr;
import static ubic.gemma.web.util.dwr.MockDwrResultHandlers.getCallback;
import static ubic.gemma.web.util.dwr.MockDwrResultMatchers.callback;

public class ExpressionDataFileUploadControllerTest extends BaseWebIntegrationTest {

    @Autowired
    private TaskRunningService taskRunningService;

    @Test
    public void testValidate() throws Exception {
        JSONObject command = new JSONObject();
        command.put( "shortName", "foo" );
        command.put( "name", "foo" );
        perform( dwr( ExpressionDataFileUploadController.class, "validate", command ) )
                .andExpect( callback().exist() )
                .andDo( getCallback( ( taskId ) -> {
                    SubmittedTask task = taskRunningService.getSubmittedTask( ( String ) taskId );
                    try {
                        TaskResult result = task.getResult();
                        SimpleExpressionExperimentCommandValidation answer = ( ( SimpleExpressionExperimentCommandValidation ) result.getAnswer() );
                        assertThat( answer )
                                .asInstanceOf( InstanceOfAssertFactories.type( SimpleExpressionExperimentCommandValidation.class ) )
                                .satisfies( c -> {
                                    System.out.println( c );
                                    assertThat( c.isValid() ).isTrue();
                                } );
                    } catch ( ExecutionException | InterruptedException e ) {
                        throw new RuntimeException( e );
                    }
                } ) );
    }
}