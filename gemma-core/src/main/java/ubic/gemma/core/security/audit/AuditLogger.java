package ubic.gemma.core.security.audit;

import com.fasterxml.jackson.databind.util.StdDateFormat;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import ubic.gemma.model.common.auditAndSecurity.AuditEvent;
import ubic.gemma.model.common.auditAndSecurity.Auditable;

import java.text.DateFormat;

/**
 * Logger for created {@link AuditEvent}.
 * @author poirigui
 */
@Slf4j
public class AuditLogger {

    /**
     * Date format consistent with the logging.
     */
    private static final DateFormat dateFormat = new StdDateFormat();

    /**
     * Log a given audit event.
     */
    public void log( Auditable auditable, AuditEvent event ) {
        if ( !log.isInfoEnabled() ) {
            return;
        }
        log.info( format( auditable, event ) );
    }

    /**
     * Render the log line for an audit event.
     * <p>
     * The line carries the auditable's {@code toString()} alongside its class and id. For a
     * {@link ubic.gemma.model.common.auditAndSecurity.AuditAction#DELETE DELETE} this is the only
     * surviving description of what was deleted: the audit trail and every event on it are
     * cascade-removed with the entity ({@code AbstractAuditable.auditTrail} and
     * {@code AuditTrail.events} are both mapped {@code CascadeType.ALL}), so the id in this line
     * resolves to nothing the moment the row is gone. {@code toString()} supplies the short name
     * and name, which stay meaningful afterwards.
     */
    String format( Auditable auditable, AuditEvent event ) {
        String dateStr;
        // StdDateFormat is not thread-safe; synchronize on the shared static instance.
        synchronized ( dateFormat ) {
            dateStr = dateFormat.format( event.getDate() );
        }
        return String.format( "%s | %s event%s on entity %s:%d [%s] by %s%s%s",
                dateStr,
                event.getAction(),
                event.getEventType() != null ? " of type " + event.getEventType().getClass().getName() : "",
                auditable.getClass().getName(), auditable.getId(), auditable,
                event.getPerformer() != null ? event.getPerformer().getUserName() : "[anonymous]",
                StringUtils.isNotBlank( event.getNote() ) ? ": " + event.getNote() : "",
                StringUtils.isNotBlank( event.getDetail() ) ? "\n" + event.getDetail() : "" );
    }
}
