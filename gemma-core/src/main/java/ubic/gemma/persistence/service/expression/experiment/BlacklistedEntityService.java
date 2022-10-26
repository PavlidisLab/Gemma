package ubic.gemma.persistence.service.expression.experiment;

import ubic.gemma.model.expression.BlacklistedEntity;
import ubic.gemma.model.expression.BlacklistedValueObject;
import ubic.gemma.persistence.service.BaseVoEnabledService;

public interface BlacklistedEntityService extends BaseVoEnabledService<BlacklistedEntity, BlacklistedValueObject> {

    /**
     * @see BlacklistedEntityDao#isBlacklisted(String)
     */
    boolean isBlacklisted( String accession );

    /**
     * @see BlacklistedEntityDao#findByAccession(String)
     */
    BlacklistedEntity findByAccession( String accession );
}
