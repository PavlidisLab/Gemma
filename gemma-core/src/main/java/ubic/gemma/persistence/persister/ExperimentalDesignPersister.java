package ubic.gemma.persistence.persister;

import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ubic.gemma.model.expression.experiment.ExperimentalDesign;

@Service
public class ExperimentalDesignPersister extends AbstractPersister<ExperimentalDesign> {

    @Autowired
    public ExperimentalDesignPersister( SessionFactory sessionFactory ) {
        super( sessionFactory );
    }

    @Override
    public <S extends ExperimentalDesign> S persist( S entity ) {
        return null;
    }
}
