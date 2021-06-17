package ubic.gemma.persistence.persister.expression;

import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentSubSet;
import ubic.gemma.persistence.persister.AbstractPersister;
import ubic.gemma.persistence.persister.Persister;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentSubSetDao;

@Service
public class ExpressionExperimentSubSetPersister extends AbstractPersister<ExpressionExperimentSubSet> {

    @Autowired
    private ExpressionExperimentSubSetDao expressionExperimentSubSetDao;

    @Autowired
    private Persister<ExpressionExperiment> expressionExperimentPersister;

    @Autowired
    public ExpressionExperimentSubSetPersister( SessionFactory sessionFactory ) {
        super( sessionFactory );
    }

    @Override
    @Transactional
    public ExpressionExperimentSubSet persist( ExpressionExperimentSubSet entity ) {
        if ( !this.isTransient( entity ) )
            return entity;

        if ( entity.getBioAssays().size() == 0 ) {
            throw new IllegalArgumentException( "Cannot make a subset with no bioassays" );
        } else if ( this.expressionExperimentPersister.isTransient( entity.getSourceExperiment() ) ) {
            throw new IllegalArgumentException(
                    "Subsets are only supported for expression experiments that are already persistent" );
        }

        return expressionExperimentSubSetDao.findOrCreate( entity );
    }
}
