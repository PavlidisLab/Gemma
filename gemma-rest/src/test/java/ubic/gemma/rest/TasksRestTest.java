package ubic.gemma.rest;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import ubic.gemma.rest.util.BaseJerseyIntegrationTest5;

import jakarta.ws.rs.core.Response;

import static ubic.gemma.rest.util.Assertions.assertThat;

@Tag("slow")
public class TasksRestTest extends BaseJerseyIntegrationTest5 {

    @Test
    public void testGetTaskStatusWithUnknownTaskIdIs404() {
        // The in-memory task store starts empty; an arbitrary id should always 404.
        assertThat( target( "/tasks/this-task-does-not-exist-" + System.nanoTime() ).request().get() )
                .hasStatus( Response.Status.NOT_FOUND );
    }
}
