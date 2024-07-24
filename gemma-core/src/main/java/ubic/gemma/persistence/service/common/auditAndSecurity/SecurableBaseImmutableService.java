package ubic.gemma.persistence.service.common.auditAndSecurity;

import org.springframework.security.access.annotation.Secured;
import ubic.gemma.model.common.auditAndSecurity.Securable;
import ubic.gemma.persistence.service.BaseImmutableService;

import java.util.Collection;

public interface SecurableBaseImmutableService<C extends Securable> extends BaseImmutableService<C>,
        SecurableBaseReadOnlyService<C> {

    @Override
    @Secured({ "GROUP_USER", "AFTER_ACL_READ" })
    C findOrCreate( C entity );

    @Override
    @Secured({ "GROUP_USER" })
    Collection<C> create( Collection<C> entities );

    @Override
    @Secured({ "GROUP_USER" })
    C create( C entity );

    /**
     * {@inheritDoc}
     * <p>
     * Only administrator are allowed to remove entity by ID.
     */
    @Override
    @Secured({ "GROUP_ADMIN" })
    void remove( Long id );

    @Override
    @Secured({ "GROUP_USER", "ACL_SECURABLE_EDIT" })
    void remove( C entity );

    @Override
    // FIXME: this does not work with generics
    // @Secured({ "GROUP_USER", "ACL_SECURABLE_COLLECTION_EDIT" })
    void remove( Collection<C> entities );
}
