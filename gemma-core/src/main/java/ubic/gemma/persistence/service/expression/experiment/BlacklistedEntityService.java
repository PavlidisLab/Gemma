package ubic.gemma.persistence.service.expression.experiment;

import ubic.gemma.model.expression.BlacklistedEntity;
import ubic.gemma.model.expression.BlacklistedValueObject;
import ubic.gemma.persistence.service.BaseVoEnabledService;

public interface BlacklistedEntityService extends BaseVoEnabledService<BlacklistedEntity, BlacklistedValueObject> {

    boolean isBlacklisted( String accession );

    BlacklistedEntity findByAccession( String accession );
}
