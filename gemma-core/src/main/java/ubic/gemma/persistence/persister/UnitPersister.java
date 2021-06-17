package ubic.gemma.persistence.persister;

import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.model.common.measurement.Unit;
import ubic.gemma.persistence.service.common.measurement.UnitDao;

@Service
public class UnitPersister extends AbstractPersister<Unit> {

    @Autowired
    private UnitDao unitDao;

    @Autowired
    public UnitPersister( SessionFactory sessionFactory ) {
        super( sessionFactory );
    }

    @Override
    @Transactional
    public Unit persist( Unit unit ) {
        if ( unit == null )
            return null;
        if ( !this.isTransient( unit ) )
            return unit;
        return this.unitDao.findOrCreate( unit );
    }
}
