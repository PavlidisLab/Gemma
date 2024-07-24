package ubic.gemma.persistence.service.common.auditAndSecurity;

import org.springframework.security.access.annotation.Secured;
import ubic.gemma.model.common.auditAndSecurity.Securable;
import ubic.gemma.persistence.service.BaseReadOnlyService;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.function.Function;
import java.util.function.Supplier;

public interface SecurableBaseReadOnlyService<C extends Securable> extends BaseReadOnlyService<C> {

    @Override
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_READ_QUIET" })
    C find( C entity );

    @Nonnull
    @Override
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_READ" })
    C findOrFail( C entity ) throws NullPointerException;

    @Override
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COLLECTION_READ" })
    Collection<C> load( Collection<Long> ids );

    @Override
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
}
