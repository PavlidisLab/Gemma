package ubic.gemma.persistence.service.expression.bioAssayData;

import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;
import ubic.gemma.model.expression.bioAssayData.RawExpressionDataVector;

@Repository
public class RawAndProcessedExpressionDataVectorDaoImpl extends AbstractDesignElementDataVectorDao<DesignElementDataVector> implements RawAndProcessedExpressionDataVectorDao {

    @Autowired
    public RawAndProcessedExpressionDataVectorDaoImpl( SessionFactory sessionFactory ) {
        super( DesignElementDataVector.class, sessionFactory, sessionFactory.getClassMetadata( RawExpressionDataVector.class ) );
    }
}
