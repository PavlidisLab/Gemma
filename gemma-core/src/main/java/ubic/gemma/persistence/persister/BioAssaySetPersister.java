package ubic.gemma.persistence.persister;

import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ubic.gemma.model.expression.experiment.BioAssaySet;

@Service
public class BioAssaySetPersister extends AbstractPersister<BioAssaySet> {

    @Autowired
    public BioAssaySetPersister( SessionFactory sessionFactory ) {
        super( sessionFactory );
    }

    @Override
    public <S extends BioAssaySet> S persist( S entity ) {
        return null;
    }
}
