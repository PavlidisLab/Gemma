package ubic.gemma.persistence.persister;

import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ubic.gemma.model.expression.designElement.CompositeSequence;

@Service
public class CompositeSequencePersister extends AbstractPersister<CompositeSequence> {

    @Autowired
    public CompositeSequencePersister( SessionFactory sessionFactory ) {
        super( sessionFactory );
    }

    @Override
    public <S extends CompositeSequence> S persist( S entity ) {
        return null;
    }
}
