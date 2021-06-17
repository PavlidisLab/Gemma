package ubic.gemma.persistence.persister;

import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.model.expression.biomaterial.Compound;
import ubic.gemma.persistence.service.expression.biomaterial.CompoundDao;

@Service
public class CompoundPersister extends AbstractPersister<Compound> {

    @Autowired
    private CompoundDao compoundDao;

    @Autowired
    public CompoundPersister( SessionFactory sessionFactory ) {
        super( sessionFactory );
    }

    @Override
    @Transactional
    public Compound persist( Compound compound ) {
        if ( compound == null )
            return null;
        return compoundDao.findOrCreate( compound );
    }
}
