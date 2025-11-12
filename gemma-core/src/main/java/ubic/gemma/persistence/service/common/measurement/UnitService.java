package ubic.gemma.persistence.service.common.measurement;

import ubic.gemma.model.common.measurement.Unit;
import ubic.gemma.persistence.service.BaseImmutableService;

public interface UnitService extends BaseImmutableService<Unit> {

    /**
     * Remove the given unit if unused.
     */
    void removeIfUnused( Unit unit );
}
