package ubic.gemma.persistence.service.common.auditAndSecurity;

import org.springframework.security.access.annotation.Secured;
import ubic.gemma.model.common.Identifiable;
import ubic.gemma.persistence.service.BaseService;

import javax.annotation.Nonnull;
import java.util.Collection;

/**
 * A {@link BaseService} for {@link gemma.gsec.model.Securable} entities with sensible defaults.
 * @see BaseService
 * @author poirigui
 */
public interface SecurableService<S extends Identifiable> extends BaseService<S> {

    @Override
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_READ_QUIET" })
    S find( S entity );

    @Override
    @Secured({ "GROUP_USER", "AFTER_ACL_READ" })
    S findOrCreate( S entity );

    @Override
    @Secured({ "GROUP_USER" })
    Collection<S> create( Collection<S> entities );

    @Override
    @Secured({ "GROUP_USER" })
    S create( S entity );

    @Override
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COLLECTION_READ" })
    Collection<S> load( Collection<Long> ids );

    @Override
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_READ_QUIET" })
    S load( Long id );

    @Nonnull
    @Override
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_READ" })
    S loadOrFail( Long id ) throws NullPointerException;

    @Override
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COLLECTION_READ" })
    Collection<S> loadAll();

    /**
     * {@inheritDoc}
     * <p>
     * TODO: add ACL_SECURABLE_COLLECTION_EDIT, but this method accepts transient entities which do not have ACLs yet.
     *       See <a href="https://github.com/PavlidisLab/Gemma/issues/861">#861</a> for more details.
     * @deprecated do not use this until the issue with ACLs is resolved.
     */
    @Deprecated
    @Override
    @Secured({ "GROUP_USER" })
    Collection<S> save( Collection<S> entities );

    /**
     * {@inheritDoc}
     * <p>
     * TODO: add ACL_SECURABLE_EDIT, but this method accepts transient entities which do not have ACLs yet.
     *       See <a href="https://github.com/PavlidisLab/Gemma/issues/861">#861</a> for more details.
     * @deprecated do not use this until the issue with ACLs is resolved.
     */
    @Deprecated
    @Override
    @Secured({ "GROUP_USER" })
    S save( S entity );

    @Override
    @Secured({ "GROUP_USER", "ACL_SECURABLE_EDIT" })
    void update( S entity );

    @Override
    @Secured({ "GROUP_USER", "ACL_SECURABLE_COLLECTION_EDIT" })
    void update( Collection<S> entities );

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
    void remove( S entity );

    @Override
    @Secured({ "GROUP_USER", "ACL_SECURABLE_COLLECTION_EDIT" })
    void remove( Collection<S> entities );

    /**
     * {@inheritDoc}
     * <p>
     * Only administrators are allowed to remove all entities in batch.
     */
    @Override
    @Secured({ "GROUP_ADMIN" })
    void removeAllInBatch();
}
