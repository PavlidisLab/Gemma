package ubic.gemma.persistence.service.expression.bioAssayData;

import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVector;
import ubic.gemma.model.expression.bioAssayData.RawExpressionDataVector;
import ubic.gemma.model.expression.bioAssayData.BulkExpressionDataVector;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

import java.util.ArrayList;
import java.util.Collection;

@Repository
public class RawAndProcessedExpressionDataVectorDaoImpl extends AbstractDesignElementDataVectorDao<BulkExpressionDataVector> implements RawAndProcessedExpressionDataVectorDao {

    @Autowired
    private RawExpressionDataVectorDao rawExpressionDataVectorDao;

    @Autowired
    private ProcessedExpressionDataVectorDao processedExpressionDataVectorDao;

    @Autowired
    public RawAndProcessedExpressionDataVectorDaoImpl( SessionFactory sessionFactory ) {
        super( BulkExpressionDataVector.class, sessionFactory );
    }

    @Override
    public BulkExpressionDataVector load( Long id ) {
        throw new UnsupportedOperationException( "Use a specific expression vector DAO to load by ID." );
    }

    @Override
    public Collection<BulkExpressionDataVector> load( Collection<Long> ids ) {
        throw new UnsupportedOperationException( "Use a specific expression vector DAO to load by IDs." );
    }

    @Override
    public BulkExpressionDataVector find( BulkExpressionDataVector entity ) {
        if ( entity instanceof RawExpressionDataVector ) {
            return rawExpressionDataVectorDao.find( ( RawExpressionDataVector ) entity );
        } else if ( entity instanceof ProcessedExpressionDataVector ) {
            return processedExpressionDataVectorDao.find( ( ProcessedExpressionDataVector ) entity );
        } else {
            throw new UnsupportedOperationException( "Only raw and processed vectors can be used with this service." );
        }
    }

    @Override
    public Collection<BulkExpressionDataVector> findByExpressionExperiment( ExpressionExperiment ee ) {
        // BulkExpressionDataVector is not a JPA entity (the inheritance is plain Java, not <subclass/>),
        // so the AbstractDao.findByProperty Criteria query against it fails on Hibernate 6's stricter
        // JPA Metamodel. Issue two HQL queries against the real entities and merge.
        Collection<BulkExpressionDataVector> result = new ArrayList<>();
        result.addAll( this.getSessionFactory().getCurrentSession()
                .createQuery( "from RawExpressionDataVector v where v.expressionExperiment = :ee", RawExpressionDataVector.class )
                .setParameter( "ee", ee )
                .list() );
        result.addAll( this.getSessionFactory().getCurrentSession()
                .createQuery( "from ProcessedExpressionDataVector v where v.expressionExperiment = :ee", ProcessedExpressionDataVector.class )
                .setParameter( "ee", ee )
                .list() );
        return result;
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
