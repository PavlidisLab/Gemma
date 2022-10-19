package ubic.gemma.persistence.service;

import org.hibernate.SessionFactory;
import ubic.gemma.model.IdentifiableValueObject;
import ubic.gemma.model.common.Identifiable;
import ubic.gemma.persistence.util.EntityUtils;
import ubic.gemma.persistence.util.Filters;
import ubic.gemma.persistence.util.ObjectFilter;
import ubic.gemma.persistence.util.Sort;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Base implementation for {@link FilteringVoEnabledDao}.
 *
 * @param <O> the entity type
 * @param <VO> the corresponding VO type
 * @author poirigui
 */
public abstract class AbstractFilteringVoEnabledDao<O extends Identifiable, VO extends IdentifiableValueObject<O>> extends AbstractVoEnabledDao<O, VO> implements FilteringVoEnabledDao<O, VO> {

    private final String objectAlias;

    protected AbstractFilteringVoEnabledDao( @Nullable String objectAlias, Class<? extends O> elementClass, SessionFactory sessionFactory ) {
        super( elementClass, sessionFactory );
        this.objectAlias = objectAlias;
    }

    @Nullable
    @Override
    public final String getObjectAlias() {
        return objectAlias;
    }

    @Override
    public Class<? extends O> getElementClass() {
        return elementClass;
    }

    /**
     * {@inheritDoc}
     * <p/>
     * For consistency, this is redefined in terms of {@link #loadValueObjectsPreFilter(Filters, Sort)}.
     */
    @Override
    public final VO loadValueObject( O entity ) {
        return loadValueObjectsPreFilter( Filters.singleFilter( new ObjectFilter( getObjectAlias(), getIdPropertyName(), Long.class, ObjectFilter.Operator.eq, entity.getId() ) ), null ).stream()
                .findFirst()
                .orElse( null );
    }

    /**
     * {@inheritDoc}
     * <p/>
     * For consistency, this is redefined in terms of {@link #loadValueObjectsPreFilter(Filters, Sort)}.
     */
    @Override
    public final VO loadValueObjectById( Long id ) {
        return loadValueObjectsPreFilter( Filters.singleFilter( new ObjectFilter( getObjectAlias(), getIdPropertyName(), Long.class, ObjectFilter.Operator.eq, id ) ), null ).stream()
                .findFirst()
                .orElse( null );
    }

    /**
     * {@inheritDoc}
     * <p/>
     * For consistency, this is redefined in terms of {@link #loadValueObjectsPreFilter(Filters, Sort)}.
     */
    @Override
    public final List<VO> loadValueObjects( Collection<O> entities ) {
        if ( entities.isEmpty() ) {
            return Collections.emptyList();
        }
        return loadValueObjectsPreFilter( Filters.singleFilter( new ObjectFilter( getObjectAlias(), getIdPropertyName(), Long.class, ObjectFilter.Operator.in, EntityUtils.getIds( entities ) ) ), null );
    }

    /**
     * {@inheritDoc}
     * <p/>
     * For consistency, this is redefined in terms of {@link #loadValueObjectsPreFilter(Filters, Sort)}.
     */
    @Override
    public final List<VO> loadValueObjectsByIds( Collection<Long> ids ) {
        if ( ids.isEmpty() ) {
            return Collections.emptyList();
        }
        return loadValueObjectsPreFilter( Filters.singleFilter( new ObjectFilter( getObjectAlias(), getIdPropertyName(), Long.class, ObjectFilter.Operator.in, ids ) ), null );
    }

    /**
     * {@inheritDoc}
     * <p/>
     * For consistency, this is redefined in terms of {@link #loadValueObjectsPreFilter(Filters, Sort)}.
     */
    @Override
    public final List<VO> loadAllValueObjects() {
        return loadValueObjectsPreFilter( null, null );
    }

    private String getIdPropertyName() {
        return getSessionFactory().getClassMetadata( elementClass ).getIdentifierPropertyName();
    }
}