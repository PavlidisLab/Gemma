package ubic.gemma.persistence.service.common.auditAndSecurity;

import org.springframework.security.access.annotation.Secured;
import ubic.gemma.model.common.auditAndSecurity.Securable;
import ubic.gemma.persistence.service.BaseService;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * A base service for securable entities.
 * <p>
 * This interface provides sensible default {@link Secured} annotations for all methods defined and inherited from
 * {@link BaseService}.
 * @author poirigui
 * @see gemma.gsec.model.Securable
 * @see BaseService
 */
public interface SecurableBaseService<C extends Securable> extends BaseService<C> {

    @Override
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_READ_QUIET" })
    C find( C entity );

    @Override
    @Secured({ "GROUP_USER", "AFTER_ACL_READ" })
    C findOrCreate( C entity );

    @Override
    @Secured({ "GROUP_USER" })
    Collection<C> create( Collection<C> entities );

    @Override
    @Secured({ "GROUP_USER" })
    C create( C entity );

    @Override
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COLLECTION_READ" })
    Collection<C> load( Collection<Long> ids );

    @Override
    @Nullable
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_READ_QUIET" })
    C load( Long id );

    @Nonnull
    @Override
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_READ" })
    C loadOrFail( Long id ) throws NullPointerException;

    @Nonnull
    @Override
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_READ" })
    <T extends Exception> C loadOrFail( Long id, Supplier<T> exceptionSupplier ) throws T;

    @Nonnull
    @Override
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_READ" })
    <T extends Exception> C loadOrFail( Long id, Function<String, T> exceptionSupplier ) throws T;

    @Nonnull
    @Override
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_READ" })
    <T extends Exception> C loadOrFail( Long id, Function<String, T> exceptionSupplier, String message ) throws T;

    @Override
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COLLECTION_READ" })
    Collection<C> loadAll();

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
    @Secured({ "GROUP_USER", "ACL_SECURABLE_EDIT" })
    void update( Collection<C> entities );

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
}
