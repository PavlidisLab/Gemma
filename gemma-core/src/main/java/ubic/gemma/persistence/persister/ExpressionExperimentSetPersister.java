package ubic.gemma.persistence.persister;

import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.model.analysis.expression.ExpressionExperimentSet;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.persistence.service.analysis.expression.ExpressionExperimentSetDao;

import java.util.Collection;
import java.util.HashSet;

@Service
public class ExpressionExperimentSetPersister extends AbstractPersister<ExpressionExperimentSet> {

    @Autowired
    private ExpressionExperimentSetDao expressionExperimentSetDao;

    @Autowired
    private Persister<BioAssaySet> bioAssaySetPersister;

    @Autowired
    public ExpressionExperimentSetPersister( SessionFactory sessionFactory ) {
        super( sessionFactory );
    }

    @Override
    @Transactional
    public ExpressionExperimentSet persist( ExpressionExperimentSet entity ) {
        if ( !this.isTransient( entity ) )
            return entity;

        Collection<BioAssaySet> setMembers = new HashSet<>();

        for ( BioAssaySet baSet : entity.getExperiments() ) {
            if ( bioAssaySetPersister.isTransient( baSet ) ) {
                baSet = bioAssaySetPersister.persist( baSet );
            }
            setMembers.add( baSet );
        }
        entity.getExperiments().clear();
        entity.getExperiments().addAll( setMembers );

        return expressionExperimentSetDao.create( entity );
    }
}
