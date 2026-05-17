package ubic.gemma.persistence.service;

import org.hibernate.SessionFactory;
import ubic.gemma.model.common.Identifiable;
import ubic.gemma.model.common.IdentifiableValueObject;
import ubic.gemma.persistence.util.Filters;
import ubic.gemma.persistence.util.Slice;
import ubic.gemma.persistence.util.Sort;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Partial implementation of {@link FilteringVoEnabledDao}.
 * <p>
 * Pre-phase-2 this class was built on the {@code org.hibernate.Criteria} API which Hibernate 6 removed entirely.
 * For now this stub throws {@link UnsupportedOperationException} from every filtering method; subclasses are
 * expected to override every method they actually use (with HQL via {@link AbstractQueryFilteringVoEnabledDao}
 * or with JPA Criteria built locally). A proper JPA-Criteria reimplementation of the shared filtering machinery
 * can land in a follow-up.
 */
public abstract class AbstractCriteriaFilteringVoEnabledDao<O extends Identifiable, VO extends IdentifiableValueObject<O>>
        extends AbstractFilteringVoEnabledDao<O, VO> {

    private static final String UNSUPPORTED = "Criteria-based filtering is temporarily unavailable. Override the relevant load*/count method in your DAO.";

    protected AbstractCriteriaFilteringVoEnabledDao( Class<? extends O> elementClass, SessionFactory sessionFactory ) {
        super( null, elementClass, sessionFactory );
    }

    @Override
    public List<Long> loadIds( @Nullable Filters filters, @Nullable Sort sort ) {
        throw new UnsupportedOperationException( UNSUPPORTED );
    }

    @Override
    public List<O> load( @Nullable Filters filters, @Nullable Sort sort ) {
        throw new UnsupportedOperationException( UNSUPPORTED );
    }

    @Override
    public Slice<O> load( @Nullable Filters filters, @Nullable Sort sort, int offset, int limit ) {
        throw new UnsupportedOperationException( UNSUPPORTED );
    }

    @Override
    public Slice<VO> loadValueObjects( @Nullable Filters filters, @Nullable Sort sort, int offset, int limit ) {
        throw new UnsupportedOperationException( UNSUPPORTED );
    }

    @Override
    public List<VO> loadValueObjects( @Nullable Filters filters, @Nullable Sort sort ) {
        throw new UnsupportedOperationException( UNSUPPORTED );
    }

    @Override
    public long count( @Nullable Filters filters ) {
        throw new UnsupportedOperationException( UNSUPPORTED );
    }
}
