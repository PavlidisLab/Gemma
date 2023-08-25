package ubic.gemma.persistence.service.expression.bioAssayData;

import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;
import ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVector;
import ubic.gemma.model.expression.bioAssayData.RawExpressionDataVector;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.persistence.util.Specification;

import java.util.Collection;

@Repository
public class RawAndProcessedExpressionDataVectorDaoImpl extends AbstractDesignElementDataVectorDao<DesignElementDataVector> implements RawAndProcessedExpressionDataVectorDao {

    @Autowired
    private RawExpressionDataVectorDao rawExpressionDataVectorDao;

    @Autowired
    private ProcessedExpressionDataVectorDao processedExpressionDataVectorDao;

    @Autowired
    public RawAndProcessedExpressionDataVectorDaoImpl( SessionFactory sessionFactory ) {
        super( DesignElementDataVector.class, sessionFactory, sessionFactory.getClassMetadata( RawExpressionDataVector.class ) );
    }

    @Override
    public DesignElementDataVector load( Long id ) {
        throw new UnsupportedOperationException( "Use a specific expression vector DAO to load by ID." );
    }

    @Override
    public Collection<DesignElementDataVector> load( Collection<Long> ids ) {
        throw new UnsupportedOperationException( "Use a specific expression vector DAO to load by IDs." );
    }

    @Override
    public DesignElementDataVector find( Specification<DesignElementDataVector> spec ) {
        if ( spec.getEntity() instanceof RawExpressionDataVector ) {
            return rawExpressionDataVectorDao.find( ( Specification<RawExpressionDataVector> ) spec );
        } else if ( spec.getEntity() instanceof ProcessedExpressionDataVector ) {
            return processedExpressionDataVectorDao.find( ( Specification<ProcessedExpressionDataVector> ) spec );
        } else {
            throw new UnsupportedOperationException( "Only raw and processed vectors can be used with this service." );
        }
    }

    @Override
    public int removeByCompositeSequence( CompositeSequence cs ) {
        int removed = 0;
        removed += this.getSessionFactory().getCurrentSession()
                .createQuery( "delete RawExpressionDataVector dedv where dedv.designElement = :cs" )
                .setParameter( "cs", cs )
                .executeUpdate();
        removed += this.getSessionFactory().getCurrentSession()
                .createQuery( "delete ProcessedExpressionDataVector dedv where dedv.designElement = :cs" )
                .setParameter( "cs", cs )
                .executeUpdate();
        return removed;
    }
}
