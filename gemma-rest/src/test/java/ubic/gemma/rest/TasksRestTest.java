package ubic.gemma.rest;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import ubic.gemma.core.util.test.category.SlowTest;
import ubic.gemma.rest.util.BaseJerseyIntegrationTest;

import javax.ws.rs.core.Response;

import static ubic.gemma.rest.util.Assertions.assertThat;

@Category(SlowTest.class)
public class TasksRestTest extends BaseJerseyIntegrationTest {

    @Test
    public void testGetTaskStatusWithUnknownTaskIdIs404() {
        // The in-memory task store starts empty; an arbitrary id should always 404.
        assertThat( target( "/tasks/this-task-does-not-exist-" + System.nanoTime() ).request().get() )
                .hasStatus( Response.Status.NOT_FOUND );
    }
}
