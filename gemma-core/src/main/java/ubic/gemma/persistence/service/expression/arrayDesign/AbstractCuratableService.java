package ubic.gemma.persistence.service.expression.arrayDesign;

import ubic.gemma.model.IdentifiableValueObject;
import ubic.gemma.model.common.Identifiable;
import ubic.gemma.persistence.service.AbstractFilteringVoEnabledService;
import ubic.gemma.persistence.service.FilteringVoEnabledDao;

import java.util.Date;

/**
 * Base implementation for services dealing with {@link ubic.gemma.model.common.auditAndSecurity.curation.Curatable}
 * entities.
 * @param <O>
 * @param <VO>
 * @author poirigui
 * @see ubic.gemma.model.common.auditAndSecurity.curation.Curatable
 */
public abstract class AbstractCuratableService<O extends Identifiable, VO extends IdentifiableValueObject<O>> extends AbstractFilteringVoEnabledService<O, VO> {

    protected AbstractCuratableService( FilteringVoEnabledDao<O, VO> voDao ) {
        super( voDao );
    }
}
