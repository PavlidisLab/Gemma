package ubic.gemma.persistence.service.expression.arrayDesign;

import org.springframework.security.access.annotation.Secured;
import ubic.gemma.model.common.auditAndSecurity.curation.AbstractCuratableValueObject;
import ubic.gemma.model.common.auditAndSecurity.curation.Curatable;
import ubic.gemma.persistence.service.BaseService;
import ubic.gemma.persistence.service.FilteringVoEnabledService;
import ubic.gemma.persistence.util.Filters;
import ubic.gemma.persistence.util.Slice;
import ubic.gemma.persistence.util.Sort;
import ubic.gemma.persistence.util.Specification;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;

/**
 * Interface for curatable services.
 * <p>
 * For now, this mainly adds {@link Secured} annotations to safeguard methods inherited from {@link BaseService}.
 * @param <C>
 */
public interface CuratableService<C extends Curatable, VO extends AbstractCuratableValueObject<C>> extends BaseService<C>, FilteringVoEnabledService<C, VO> {

    @Override
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_READ_QUIET" })
    C find( Specification<C> spec );

    @Override
    @Secured({ "GROUP_USER", "AFTER_ACL_READ" })
    C findOrCreate( Specification<C> spec );

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
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_READ_QUIET" })
    C load( Long id );

    @Nonnull
    @Override
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_READ" })
    C loadOrFail( Long id ) throws NullPointerException;

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

    /**
     * {@inheritDoc}
     * <p>
     * TODO: add ACL_SECURABLE_EDIT, but this method accepts transient entities which do not have ACLs yet.
     * @deprecated do not use this until the issue with ACLs is resolved.
     */
    @Deprecated
    @Override
    @Secured({ "GROUP_USER" })
    Collection<C> save( Collection<C> entities );

    /**
     * {@inheritDoc}
     * <p>
     * TODO: add ACL_SECURABLE_EDIT, but this method accepts transient entities which do not have ACLs yet.
     * @deprecated do not use this until the issue with ACLs is resolved.
     */
    @Deprecated
    @Override
    @Secured({ "GROUP_USER" })
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

    /**
     * {@inheritDoc}
     * <p>
     * Only administrators are allowed to remove all entities in batch.
     */
    @Override
    @Secured({ "GROUP_ADMIN" })
    void removeAllInBatch();
}
