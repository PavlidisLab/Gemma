package ubic.gemma.persistence.service.common.measurement;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.model.common.measurement.Unit;
import ubic.gemma.persistence.service.AbstractService;

@Service
public class UnitServiceImpl extends AbstractService<Unit> implements UnitService {

    private final UnitDao unitDao;

    @Autowired
    public UnitServiceImpl( UnitDao unitDao ) {
        super( unitDao );
        this.unitDao = unitDao;
    }

    @Override
    @Transactional
    public void removeIfUnused( Unit unit ) {
        unitDao.removeIfUnused( unit );
    }
}
