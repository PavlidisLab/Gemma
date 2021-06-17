package ubic.gemma.persistence.persister;

import org.hibernate.FlushMode;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.model.common.Auditable;
import ubic.gemma.model.common.auditAndSecurity.AuditTrail;

/**
 * Use this as a base class for {@link Auditable} entities.
 * @param <T>
 */
public abstract class AuditablePersister<T extends Auditable> extends AbstractPersister<T> {

    @Autowired
    private Persister<AuditTrail> auditTrailPersister;

    @Autowired
    public AuditablePersister( SessionFactory sessionFactory ) {
        super( sessionFactory );
    }

    @Override
    @Transactional
    public <S extends T> S persist( S entity ) {
        try {

            this.getSessionFactory().getCurrentSession().setFlushMode( FlushMode.COMMIT );

            if ( entity.getAuditTrail() == null ) {
                entity.setAuditTrail( AuditTrail.Factory.newInstance() );
            }

            entity.setAuditTrail( auditTrailPersister.persist( entity.getAuditTrail() ) );

            return this.persistAuditable( entity );
        } finally {
            this.getSessionFactory().getCurrentSession().setFlushMode( FlushMode.AUTO );
        }

    }

    protected abstract <S extends T> S persistAuditable( S entity );
}
