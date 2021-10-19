package ubic.gemma.persistence.service;

import org.hibernate.Criteria;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import ubic.gemma.model.IdentifiableValueObject;
import ubic.gemma.model.analysis.expression.diff.ExpressionAnalysisResultSet;
import ubic.gemma.model.common.Identifiable;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.util.*;

import java.util.List;

/**
 * Partial implementation of {@link FilteringVoEnabledDao} based on the Hibernate {@link Criteria} API.
 *
 * @see ubic.gemma.persistence.util.ObjectFilterCriteriaUtils to obtain {@link org.hibernate.criterion.DetachedCriteria}
 * from a {@link Filters}.
 * @see ubic.gemma.persistence.util.AclCriteriaUtils for utilities to include ACL constraints on the VOs at the
 * database-level.
 */
public abstract class AbstractCriteriaFilteringVoEnabledDao<O extends Identifiable, VO extends IdentifiableValueObject<O>> extends AbstractFilteringVoEnabledDao<O, VO> {

    protected AbstractCriteriaFilteringVoEnabledDao( Class elementClass, SessionFactory sessionFactory ) {
        super( elementClass, sessionFactory );
    }

    /**
     * For the Criterion API, NULL refers to the root entity.
     * @return
     */
    @Override
    public String getObjectAlias() {
        return null;
    }

    protected abstract Criteria getLoadValueObjectsCriteria( Filters objectFilters, Sort sort );

    @Override
    public Slice<VO> loadValueObjectsPreFilter( Filters objectFilters, Sort sort, int offset, int limit ) {
        Criteria query = getLoadValueObjectsCriteria( objectFilters, sort );
        Criteria totalElementsQuery = getLoadValueObjectsCriteria( objectFilters, sort );

        // setup offset/limit
        if ( offset > 0 )
            query.setFirstResult( offset );
        if ( limit > 0 )
            query.setMaxResults( limit );

        //noinspection unchecked
        List<O> data = query.setResultTransformer( Criteria.DISTINCT_ROOT_ENTITY )
                .setCacheable( true )
                .list();

        Long totalElements = ( Long ) totalElementsQuery
                .setProjection( Projections.countDistinct( "id" ) )
                .uniqueResult();

        //noinspection unchecked
        return new Slice<>( super.loadValueObjects( data ), sort, offset, limit, totalElements );
    }

    @Override
    public List<VO> loadValueObjectsPreFilter( Filters objectFilters, Sort sort ) {
        Criteria query = getLoadValueObjectsCriteria( objectFilters, sort );

        //noinspection unchecked
        List<O> data = query.setResultTransformer( Criteria.DISTINCT_ROOT_ENTITY )
                .setCacheable( true )
                .list();

        //noinspection unchecked
        return super.loadValueObjects( data );
    }
}
