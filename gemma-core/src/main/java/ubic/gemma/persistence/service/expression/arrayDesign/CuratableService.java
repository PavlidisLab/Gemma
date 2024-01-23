package ubic.gemma.persistence.service.expression.arrayDesign;

import org.springframework.security.access.annotation.Secured;
import ubic.gemma.model.common.auditAndSecurity.curation.AbstractCuratableValueObject;
import ubic.gemma.model.common.auditAndSecurity.curation.Curatable;
import ubic.gemma.persistence.service.BaseService;
import ubic.gemma.persistence.service.FilteringVoEnabledService;
import ubic.gemma.persistence.util.Filters;
import ubic.gemma.persistence.util.Slice;
import ubic.gemma.persistence.util.Sort;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Interface for curatable services.
 * <p>
 * For now, this mainly adds {@link Secured} annotations to safeguard methods inherited from {@link BaseService}.
 * @author poirigui
 */
public interface CuratableService<C extends Curatable, VO extends AbstractCuratableValueObject<C>> extends BaseService<C>, FilteringVoEnabledService<C, VO> {

    @Override
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_READ_QUIET" })
    C find( C curatable );

    @Override
    @Secured({ "GROUP_USER", "AFTER_ACL_READ" })
    C findOrCreate( C curatable );

    @Override
    @Secured({ "GROUP_USER" })
    Collection<C> create( Collection<C> entities );

    @Override
    @Secured({ "GROUP_USER" })
    C create( C curatable );

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

    // The methods below using Filters do not require AFTER_ACL_COLLECTION_READ because the ACL filtering is performed
    // in the query itself.

    @Override
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY" })
    List<Long> loadIds( @Nullable Filters filters, @Nullable Sort sort );

    @Override
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY" })
    List<C> load( @Nullable Filters filters, @Nullable Sort sort );

    @Override
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY" })
    Slice<C> load( @Nullable Filters filters, @Nullable Sort sort, int offset, int limit );

    @Override
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY" })
    List<VO> loadValueObjects( @Nullable Filters filters, @Nullable Sort sort );

    @Override
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY" })
    Slice<VO> loadValueObjects( @Nullable Filters filters, @Nullable Sort sort, int offset, int limit );

    @Override
    @Nullable
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_VALUE_OBJECT_READ" })
    VO loadValueObject( C entity );

    @Override
    @Nullable
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_VALUE_OBJECT_READ" })
    VO loadValueObjectById( Long entityId );

    @Override
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_VALUE_OBJECT_COLLECTION_READ" })
    List<VO> loadValueObjects( Collection<C> entities );

    @Override
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_VALUE_OBJECT_COLLECTION_READ" })
    List<VO> loadValueObjectsByIds( Collection<Long> ids );

    @Override
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_VALUE_OBJECT_COLLECTION_READ" })
    List<VO> loadAllValueObjects();

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
    void remove( C expressionExperiment );
}
