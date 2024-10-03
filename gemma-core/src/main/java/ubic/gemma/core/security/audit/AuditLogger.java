package ubic.gemma.core.security.audit;

import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.lang3.StringUtils;
import ubic.gemma.model.common.auditAndSecurity.AuditEvent;
import ubic.gemma.model.common.auditAndSecurity.Auditable;

/**
 * Logger for created {@link AuditEvent}.
 * @author poirigui
 */
@CommonsLog
public class AuditLogger {

    /**
     * Log a given audit event.
     */
    public void log( Auditable auditable, AuditEvent event ) {
        if ( !log.isInfoEnabled() ) {
            return;
        }
        log.info( String.format( "%s event%s on entity %s:%d on %s by %s%s%s",
                event.getAction(),
                event.getEventType() != null ? " of type " + event.getEventType().getClass().getName() : "",
                auditable.getClass(), auditable.getId(),
                event.getDate(),
                event.getPerformer() != null ? event.getPerformer().getUserName() : "[anonymous]",
                StringUtils.isNotBlank( event.getNote() ) ? ": " + event.getNote() : "",
                StringUtils.isNotBlank( event.getDetail() ) ? "\n" + event.getDetail() : "" ) );
    }
}
