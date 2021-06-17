package ubic.gemma.persistence.persister;

import gemma.gsec.model.User;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.model.common.auditAndSecurity.AuditEvent;
import ubic.gemma.model.common.auditAndSecurity.AuditTrail;
import ubic.gemma.persistence.service.common.auditAndSecurity.AuditTrailDao;

@Service
public class AuditTrailPersister extends AbstractPersister<AuditTrail> {

    @Autowired
    private AuditTrailDao auditTrailDao;

    @Autowired
    private Persister<User> userPersister;

    @Autowired
    public AuditTrailPersister( SessionFactory sessionFactory ) {
        super( sessionFactory );
    }

    @Override
    @Transactional
    public AuditTrail persist( AuditTrail entity ) {
        if ( entity == null )
            return null;
        if ( !this.isTransient( entity ) )
            return entity;

        for ( AuditEvent event : entity.getEvents() ) {
            if ( event == null )
                continue; // legacy of ordered-list which could end up with gaps; should not be needed
            // any more
            // event.setPerformer( ( User ) persistPerson( event.getPerformer() ) );
            assert event.getPerformer() != null && !userPersister.isTransient( event.getPerformer() );
        }

        // events are persisted by composition.
        return auditTrailDao.create( entity );
    }
}
