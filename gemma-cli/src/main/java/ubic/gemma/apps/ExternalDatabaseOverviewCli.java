package ubic.gemma.apps;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.springframework.beans.factory.annotation.Autowired;
import ubic.gemma.cli.util.AbstractAuthenticatedCLI;
import ubic.gemma.model.common.auditAndSecurity.AuditEvent;
import ubic.gemma.model.common.description.ExternalDatabase;
import ubic.gemma.persistence.service.common.auditAndSecurity.AuditEventService;
import ubic.gemma.persistence.service.common.description.ExternalDatabaseService;

import javax.annotation.Nullable;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class ExternalDatabaseOverviewCli extends AbstractAuthenticatedCLI {

    @Autowired
    private ExternalDatabaseService externalDatabaseService;

    @Autowired
    private AuditEventService auditEventService;

    @Nullable
    @Override
    public String getCommandName() {
        return "listExternalDatabases";
    }

    @Nullable
    @Override
    public String getShortDesc() {
        return "Print an overview of all external databases used by Gemma";
    }

    @Override
    protected void buildOptions( Options options ) {
        addBatchOption( options );
    }

    @Override
    protected void processOptions( CommandLine commandLine ) {

    }

    @Override
    protected void doAuthenticatedWork() throws Exception {
        externalDatabaseService.loadAllWithAuditTrail().stream()
                .sorted( Comparator.comparing( ExternalDatabase::getLastUpdated, Comparator.nullsLast( Comparator.reverseOrder() ) ) )
                .forEachOrdered( ed -> addSuccessObject( ed.getName(), summarize( ed ) ) );
    }

    private String summarize( ExternalDatabase ed ) {
        return String.format( "Description: %s\nRelease Version: %s\nRelease URL: %s\nLast Updated: %s\nEvents:\n%s",
                ed.getDescription(),
                ed.getReleaseVersion(),
                ed.getReleaseUrl(),
                ed.getLastUpdated(),
                summarize( auditEventService.getEvents( ed ) ) );
    }

    private String summarize( List<AuditEvent> events ) {
        return events.stream()
                .sorted( Comparator.comparing( AuditEvent::getDate, Comparator.reverseOrder() ) ) // most recent events first
                .map( this::summarize )
                .collect( Collectors.joining( "\n" ) );
    }

    private String summarize( AuditEvent auditEvent ) {
        String s = String.format( "%s: %s", auditEvent.getDate(), auditEvent.getAction() );
        if ( auditEvent.getPerformer() != null ) {
            s += " by " + auditEvent.getPerformer().getUserName();
        }
        if ( auditEvent.getNote() != null ) {
            s += "\n\t" + auditEvent.getNote();
        }
        if ( auditEvent.getDetail() != null ) {
            s += "\n\t" + auditEvent.getDetail();
        }
        return s;
    }
}
