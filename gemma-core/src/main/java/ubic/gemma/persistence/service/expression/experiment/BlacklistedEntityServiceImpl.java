package ubic.gemma.persistence.service.expression.experiment;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.model.expression.BlacklistedEntity;
import ubic.gemma.model.expression.BlacklistedValueObject;
import ubic.gemma.persistence.service.AbstractVoEnabledService;

@Service
public class BlacklistedEntityServiceImpl extends AbstractVoEnabledService<BlacklistedEntity, BlacklistedValueObject> implements BlacklistedEntityService {

    private final BlacklistedEntityDao blacklistedEntityDao;

    @Autowired
    public BlacklistedEntityServiceImpl( BlacklistedEntityDao voDao ) {
        super( voDao );
        this.blacklistedEntityDao = voDao;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isBlacklisted( String accession ) {
        return blacklistedEntityDao.isBlacklisted( accession );
    }

    @Override
    @Transactional(readOnly = true)
    public BlacklistedEntity findByAccession( String accession ) {
        return blacklistedEntityDao.findByAccession( accession );
    }
}
