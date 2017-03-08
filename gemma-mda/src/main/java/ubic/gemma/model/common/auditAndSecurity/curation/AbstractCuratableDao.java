package ubic.gemma.model.common.auditAndSecurity.curation;

import org.springframework.orm.hibernate3.support.HibernateDaoSupport;
import ubic.gemma.model.common.auditAndSecurity.AuditEvent;
import ubic.gemma.persistence.BaseDao;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

/**
 * Created by tesarst on 07/03/17.
 *
 * DAO covering methods common to all Curatable objects
 */
public abstract class AbstractCuratableDao<Curatable> extends HibernateDaoSupport implements BaseDao<Curatable> {

    protected void addEventsToMap( Map<Long, Collection<AuditEvent>> eventMap, Long id, AuditEvent event ) {
        if ( eventMap.containsKey( id ) ) {

            Collection<AuditEvent> events = eventMap.get( id );
            events.add( event );
        } else {
            Collection<AuditEvent> events = new ArrayList<>();
            events.add( event );
            eventMap.put( id, events );
        }
    }

}
