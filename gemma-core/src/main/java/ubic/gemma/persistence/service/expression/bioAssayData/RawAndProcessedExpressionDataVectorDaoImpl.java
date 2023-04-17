package ubic.gemma.persistence.service.expression.bioAssayData;

import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;
import ubic.gemma.model.expression.bioAssayData.RawExpressionDataVector;
import ubic.gemma.model.expression.designElement.CompositeSequence;

@Repository
public class RawAndProcessedExpressionDataVectorDaoImpl extends AbstractDesignElementDataVectorDao<DesignElementDataVector> implements RawAndProcessedExpressionDataVectorDao {

    @Autowired
    public RawAndProcessedExpressionDataVectorDaoImpl( SessionFactory sessionFactory ) {
        super( DesignElementDataVector.class, sessionFactory, sessionFactory.getClassMetadata( RawExpressionDataVector.class ) );
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
