package ubic.gemma.core.security.audit;

import com.fasterxml.jackson.databind.util.ISO8601DateFormat;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.lang3.StringUtils;
import ubic.gemma.model.common.auditAndSecurity.AuditEvent;
import ubic.gemma.model.common.auditAndSecurity.Auditable;

import java.text.DateFormat;

/**
 * Logger for created {@link AuditEvent}.
 * @author poirigui
 */
@CommonsLog
public class AuditLogger {

    /**
     * Date format consistent with the logging.
     */
    private static final DateFormat dateFormat = new ISO8601DateFormat();

    /**
     * Log a given audit event.
     */
    public void log( Auditable auditable, AuditEvent event ) {
        if ( !log.isInfoEnabled() ) {
            return;
        }
        log.info( String.format( "%s | %s event%s on entity %s:%d by %s%s%s",
                dateFormat.format( event.getDate() ),
                event.getAction(),
                event.getEventType() != null ? " of type " + event.getEventType().getClass().getName() : "",
                auditable.getClass().getName(), auditable.getId(),
                event.getPerformer() != null ? event.getPerformer().getUserName() : "[anonymous]",
                StringUtils.isNotBlank( event.getNote() ) ? ": " + event.getNote() : "",
                StringUtils.isNotBlank( event.getDetail() ) ? "\n" + event.getDetail() : "" ) );
    }
}
