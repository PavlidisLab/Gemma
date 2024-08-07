package ubic.gemma.persistence.service.common.auditAndSecurity;

import org.springframework.security.access.annotation.Secured;
import ubic.gemma.model.common.auditAndSecurity.Securable;
import ubic.gemma.persistence.service.BaseService;

import java.util.Collection;

/**
 * A base service for securable entities.
 * <p>
 * This interface provides sensible default {@link Secured} annotations for all methods defined and inherited from
 * {@link BaseService}.
 * @author poirigui
 * @see BaseService
 */
public interface SecurableBaseService<C extends Securable> extends BaseService<C>,
        SecurableBaseImmutableService<C> {

    @Override
    @Secured({ "GROUP_USER", "ACL_SECURABLE_COLLECTION_EDIT_IGNORE_TRANSIENT" })
    Collection<C> save( Collection<C> entities );

    @Override
    @Secured({ "GROUP_USER", "ACL_SECURABLE_EDIT_IGNORE_TRANSIENT" })
    C save( C entity );

    @Override
    @Secured({ "GROUP_USER", "ACL_SECURABLE_EDIT" })
    void update( C entity );

    @Override
    @Secured({ "GROUP_USER", "ACL_SECURABLE_COLLECTION_EDIT" })
    void update( Collection<C> entities );
}
