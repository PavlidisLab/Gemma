package ubic.gemma.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import ubic.gemma.core.job.SubmittedTask;
import ubic.gemma.core.job.TaskRunningService;
import ubic.gemma.rest.util.ResponseDataObject;
import ubic.gemma.rest.util.ResponseErrorObject;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import static ubic.gemma.rest.util.Responders.respond;

/**
 * RESTful interface for polling background pipeline tasks submitted via the dispatch endpoints on
 * {@link DatasetsWebService} (preprocess, diagnostics, batch info fetch, differential analysis run/redo/remove).
 * <p>
 * Tasks are kept in memory only and evicted ~10 minutes after completion; once evicted the endpoint returns 404.
 */
@Service
@Path("/tasks")
@CommonsLog
public class TasksWebService {

    @Autowired
    private TaskRunningService taskRunningService;

    @GET
    @Path("/{taskId}")
    @Produces(MediaType.APPLICATION_JSON)
    @PreAuthorize("hasAuthority('GROUP_ADMIN')")
    @Operation(summary = "Retrieve the status of a submitted pipeline task",
            description = "Returns a snapshot of the named task's current state (`queued`, `running`, `completed`, "
                    + "`failed`, `cancelling`, `unknown`), its submission/start/finish timestamps, the experiment "
                    + "it operates on, and the most recent progress message. The task store is in-memory and is "
                    + "evicted roughly 10 minutes after completion, after which this endpoint returns 404.",
            security = { @SecurityRequirement(name = "basicAuth", scopes = { "GROUP_ADMIN" }),
                    @SecurityRequirement(name = "cookieAuth", scopes = { "GROUP_ADMIN" }) },
            responses = {
                    @ApiResponse(responseCode = "200", useReturnTypeSchema = true, content = @Content()),
                    @ApiResponse(responseCode = "404", description = "The task was never submitted or has already been evicted from the in-memory store.",
                            content = @Content(schema = @Schema(implementation = ResponseErrorObject.class))) })
    public ResponseDataObject<TaskStatusValueObject> getTaskStatus(
            @PathParam("taskId") String taskId
    ) {
        SubmittedTask task = taskRunningService.getSubmittedTask( taskId );
        if ( task == null ) {
            throw new NotFoundException( "No task with id " + taskId + " is currently tracked." );
        }
        return respond( new TaskStatusValueObject( task ) );
    }
}